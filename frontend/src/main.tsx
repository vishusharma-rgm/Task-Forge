import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import { Activity, BarChart3, CheckCircle2, Clock3, Gauge, GitBranch, ListRestart, Play, RefreshCw, Send, Server, ShieldAlert, Zap } from "lucide-react";
import "./styles.css";

type JobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "DEAD";
type JobType = "EMAIL" | "IMAGE_RESIZE" | "DATA_EXPORT" | "WEBHOOK";

type Job = {
  id: string;
  type: JobType;
  status: JobStatus;
  priority: number;
  attempts: number;
  maxAttempts: number;
  payload: string;
  lastError: string | null;
  lockedBy: string | null;
  nextRunAt: string;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

type Metrics = {
  jobsByStatus: Record<JobStatus, number>;
  workerThreads: number;
  nodeId: string;
};

type WorkerHeartbeat = {
  workerId: string;
  status: string;
  threadCount: number;
  lastClaimedJobs: number;
  lastSeenAt: string;
  alive: boolean;
  secondsSinceLastSeen: number;
};

type JobEvent = {
  id: string;
  jobId: string;
  type: "CREATED" | "CLAIMED" | "SUCCEEDED" | "FAILED" | "DEAD" | "REQUEUED" | "STALE_LOCK_RELEASED";
  workerId: string | null;
  message: string | null;
  attempt: number;
  createdAt: string;
};

type RedisQueueStats = {
  key: string;
  values: Record<string, string>;
  updatedAt: string | null;
};

type QueueSample = {
  time: string;
  depth: number;
  succeeded: number;
  failed: number;
};

const statusOrder: JobStatus[] = ["PENDING", "RUNNING", "SUCCEEDED", "FAILED", "DEAD"];
const jobTypes: JobType[] = ["EMAIL", "IMAGE_RESIZE", "DATA_EXPORT", "WEBHOOK"];
const configuredApiBase = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, "");
const apiBase = configuredApiBase ?? (window.location.port === "5173" ? "http://localhost:8080" : "");

function apiPath(path: string) {
  return `${apiBase}${path}`;
}

function App() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [workers, setWorkers] = useState<WorkerHeartbeat[]>([]);
  const [events, setEvents] = useState<JobEvent[]>([]);
  const [redisStats, setRedisStats] = useState<RedisQueueStats | null>(null);
  const [queueSamples, setQueueSamples] = useState<QueueSample[]>([]);
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<JobStatus | "ALL">("ALL");
  const [type, setType] = useState<JobType>("EMAIL");
  const [priority, setPriority] = useState(50);
  const [payload, setPayload] = useState('{"to":"student@example.com","subject":"Welcome"}');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function refresh() {
    const statusQuery = statusFilter === "ALL" ? "" : `?status=${statusFilter}`;
    try {
      const [jobsRes, metricsRes] = await Promise.all([
        fetch(apiPath(`/api/jobs${statusQuery}`)),
        fetch(apiPath("/api/metrics"))
      ]);
      if (!jobsRes.ok || !metricsRes.ok) {
        throw new Error("Backend API is not reachable");
      }
      setMessage((current) => current === "Backend API is not reachable" || current === "Load failed" ? "" : current);
      setJobs(await jobsRes.json());
      const metricsSnapshot: Metrics = await metricsRes.json();
      setMetrics(metricsSnapshot);
      setQueueSamples((samples) => {
        const nextSample = {
          time: new Date().toISOString(),
          depth: metricsSnapshot.jobsByStatus.PENDING + metricsSnapshot.jobsByStatus.RUNNING,
          succeeded: metricsSnapshot.jobsByStatus.SUCCEEDED,
          failed: metricsSnapshot.jobsByStatus.FAILED + metricsSnapshot.jobsByStatus.DEAD
        };
        const lastSample = samples[samples.length - 1];
        if (nextSample.depth === 0 && lastSample?.depth === 0) {
          return samples;
        }
        return [...samples.slice(-17), nextSample];
      });
      const workersRes = await fetch(apiPath("/api/workers"));
      if (workersRes.ok) {
        setWorkers(await workersRes.json());
      }
      const redisRes = await fetch(apiPath("/api/metrics/redis"));
      if (redisRes.ok) {
        setRedisStats(await redisRes.json());
      }
      if (selectedJobId) {
        await refreshTimeline(selectedJobId);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Backend API is not reachable");
    }
  }

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, 3000);
    return () => window.clearInterval(timer);
  }, [statusFilter, selectedJobId]);

  async function refreshTimeline(jobId: string) {
    const response = await fetch(apiPath(`/api/jobs/${jobId}/events`));
    if (response.ok) {
      setEvents(await response.json());
    }
  }

  async function selectJob(jobId: string) {
    setSelectedJobId(jobId);
    await refreshTimeline(jobId);
  }

  async function submitJob(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setMessage("");
    try {
      JSON.parse(payload);
      const response = await fetch(apiPath("/api/jobs"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ type, priority, payload })
      });
      if (!response.ok) {
        throw new Error((await response.json()).message ?? "Job submission failed");
      }
      setMessage("Job accepted by queue");
      await refresh();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Invalid request");
    } finally {
      setLoading(false);
    }
  }

  async function requeue(id: string) {
    await fetch(apiPath(`/api/jobs/${id}/requeue`), { method: "POST" });
    await refresh();
  }

  const totals = useMemo(() => {
    if (!metrics) return { all: 0, queueDepth: 0, failed: 0, succeeded: 0, successRate: 0 };
    const all = Object.values(metrics.jobsByStatus).reduce((sum, count) => sum + count, 0);
    const succeeded = metrics.jobsByStatus.SUCCEEDED;
    const failed = metrics.jobsByStatus.FAILED + metrics.jobsByStatus.DEAD;
    const completed = succeeded + failed;
    return {
      all,
      queueDepth: metrics.jobsByStatus.PENDING + metrics.jobsByStatus.RUNNING,
      failed,
      succeeded,
      successRate: completed === 0 ? 0 : Math.round((succeeded / completed) * 100)
    };
  }, [metrics]);

  const selectedJob = jobs.find((job) => job.id === selectedJobId) ?? null;
  const aliveWorkers = workers.filter((worker) => worker.alive).length;

  return (
    <main className="app-shell">
      <section className="topbar">
        <div className="brand-lockup">
          <div className="brand-mark"><Zap size={24} /></div>
          <div>
            <h1>TaskForge</h1>
          </div>
        </div>
        <button className="icon-button" onClick={refresh} aria-label="Refresh dashboard" title="Refresh dashboard">
          <RefreshCw size={18} />
        </button>
      </section>

      <section className="metrics-grid">
        <Metric icon={<Activity />} label="Total Jobs" value={totals.all} />
        <Metric icon={<Play />} label="Queue Depth" value={totals.queueDepth} />
        <Metric icon={<CheckCircle2 />} label="Success Rate" value={`${totals.successRate}%`} />
        <Metric icon={<ShieldAlert />} label="Failed Jobs" value={totals.failed} />
        <Metric icon={<Server />} label="Active Workers" value={aliveWorkers} />
        <Metric icon={<Gauge />} label="Worker Threads" value={metrics?.workerThreads ?? 0} />
      </section>

      <section className="charts-grid">
        <section className="panel chart-panel">
          <div className="panel-heading">
            <h2>Queue Length</h2>
            <BarChart3 size={18} />
          </div>
          <SparkBars samples={queueSamples.map((sample) => sample.depth)} />
        </section>

        <section className="panel chart-panel">
          <div className="panel-heading">
            <h2>Success vs Failed</h2>
            <CheckCircle2 size={18} />
          </div>
          <OutcomeChart succeeded={totals.succeeded} failed={totals.failed} />
        </section>

        <section className="panel chart-panel">
          <div className="panel-heading">
            <h2>Worker Utilization</h2>
            <Server size={18} />
          </div>
          <WorkerUtilization workers={workers} />
        </section>
      </section>

      <section className="workspace-grid">
        <form className="panel submit-panel" onSubmit={submitJob}>
          <div className="panel-heading">
            <h2>Submit Job</h2>
            <Send size={18} />
          </div>

          <label>
            Type
            <select value={type} onChange={(event) => setType(event.target.value as JobType)}>
              {jobTypes.map((jobType) => <option key={jobType}>{jobType}</option>)}
            </select>
          </label>

          <label>
            Priority: {priority}
            <input type="range" min="0" max="100" value={priority} onChange={(event) => setPriority(Number(event.target.value))} />
          </label>

          <label>
            Payload
            <textarea value={payload} onChange={(event) => setPayload(event.target.value)} spellCheck={false} />
          </label>

          <button className="primary-button" disabled={loading}>
            {loading ? "Submitting..." : "Queue job"}
          </button>
          {message && <p className="form-message">{message}</p>}
        </form>

        <div className="operations-column">
          <section className="panel jobs-panel">
            <div className="panel-heading">
              <h2>Jobs</h2>
              <ListRestart size={18} />
            </div>
            <div className="tabs">
              {["ALL", ...statusOrder].map((status) => (
                <button
                  key={status}
                  className={statusFilter === status ? "active" : ""}
                  onClick={() => setStatusFilter(status as JobStatus | "ALL")}
                >
                  {status}
                </button>
              ))}
            </div>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Attempts</th>
                    <th>Next run</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => (
                    <tr
                      key={job.id}
                      className={job.id === selectedJobId ? "selected-row" : ""}
                      onClick={() => selectJob(job.id)}
                    >
                      <td>
                        <strong>{job.type}</strong>
                        <span>{job.id.slice(0, 8)}</span>
                      </td>
                      <td><StatusBadge status={job.status} /></td>
                      <td>{job.priority}</td>
                      <td>{job.attempts}/{job.maxAttempts}</td>
                      <td>{formatTime(job.nextRunAt)}</td>
                      <td>
                        {(job.status === "FAILED" || job.status === "DEAD") && (
                          <button className="text-button" onClick={() => requeue(job.id)}>Requeue</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="details-grid">
            <section className="panel">
              <div className="panel-heading">
                <h2>Workers</h2>
                <Server size={18} />
              </div>
              <div className="worker-list">
                {workers.map((worker) => (
                  <div className="worker-row" key={worker.workerId}>
                    <div>
                      <strong>{worker.workerId}</strong>
                      <span>{worker.threadCount} threads · claimed {worker.lastClaimedJobs}</span>
                    </div>
                    <span className={worker.alive ? "live-dot live" : "live-dot stale"}>
                      {worker.alive ? "ALIVE" : "STALE"}
                    </span>
                    <small>{worker.secondsSinceLastSeen}s ago</small>
                  </div>
                ))}
                {workers.length === 0 && <p className="empty-state">No worker heartbeat yet</p>}
              </div>
            </section>

            <section className="panel">
              <div className="panel-heading">
                <h2>Redis Queue Stats</h2>
                <Gauge size={18} />
              </div>
              {redisStats && redisStats.key !== "disabled" ? (
                <div className="stats-list">
                  {statusOrder.map((status) => (
                    <div className="stat-row" key={status}>
                      <span>{status}</span>
                      <strong>{redisStats.values[status] ?? "0"}</strong>
                    </div>
                  ))}
                  <div className="stat-row">
                    <span>Updated</span>
                    <strong>{redisStats.updatedAt ? `${formatTime(redisStats.updatedAt)}` : "pending"}</strong>
                  </div>
                </div>
              ) : (
                <p className="empty-state">Redis stats disabled in local mode</p>
              )}
            </section>

            <section className="panel">
              <div className="panel-heading">
                <h2>Job Timeline</h2>
                <GitBranch size={18} />
              </div>
              {selectedJob ? (
                <>
                  <div className="selected-job">
                    <strong>{selectedJob.type}</strong>
                    <span>{selectedJob.id}</span>
                  </div>
                  <div className="timeline">
                    {events.map((event) => (
                      <div className={`timeline-row event-${event.type.toLowerCase().replace(/_/g, "-")}`} key={event.id}>
                        <div className="timeline-icon"><Clock3 size={14} /></div>
                        <div>
                          <strong>{event.type}</strong>
                          <p>{event.message ?? "No message"}{event.workerId ? ` · ${event.workerId}` : ""}</p>
                          <span>attempt {event.attempt} · {formatTime(event.createdAt)}</span>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <p className="empty-state">Select a job to inspect its lifecycle</p>
              )}
            </section>
          </section>
        </div>
      </section>
    </main>
  );
}

function Metric({ icon, label, value }: { icon: React.ReactNode; label: string; value: string | number }) {
  return (
    <div className="metric">
      <div className="metric-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusBadge({ status }: { status: JobStatus }) {
  return <span className={`status status-${status.toLowerCase()}`}>{status}</span>;
}

function SparkBars({ samples }: { samples: number[] }) {
  if (samples.length === 0 || samples.every((value) => value === 0)) {
    return <p className="empty-state chart-empty">No queued work in the current window</p>;
  }
  const normalized = samples;
  const max = Math.max(...normalized, 1);
  return (
    <div className="spark-bars">
      {normalized.map((value, index) => (
        <div className="spark-bar" key={`${value}-${index}`} style={{ height: `${Math.max(8, (value / max) * 100)}%` }}>
          <span>{value}</span>
        </div>
      ))}
    </div>
  );
}

function OutcomeChart({ succeeded, failed }: { succeeded: number; failed: number }) {
  const total = Math.max(succeeded + failed, 1);
  const successWidth = (succeeded / total) * 100;
  const failedWidth = (failed / total) * 100;
  return (
    <div className="outcome-chart">
      <div className="stacked-bar">
        <div className="stack-success" style={{ width: `${successWidth}%` }} />
        <div className="stack-failed" style={{ width: `${failedWidth}%` }} />
      </div>
      <div className="legend-row">
        <span><i className="legend success" />Succeeded {succeeded}</span>
        <span><i className="legend failed" />Failed {failed}</span>
      </div>
    </div>
  );
}

function WorkerUtilization({ workers }: { workers: WorkerHeartbeat[] }) {
  if (workers.length === 0) {
    return <p className="empty-state">No worker data yet</p>;
  }
  return (
    <div className="utilization-list">
      {workers.map((worker) => {
        const utilization = Math.min(100, Math.round((worker.lastClaimedJobs / Math.max(worker.threadCount, 1)) * 100));
        return (
          <div className="utilization-row" key={worker.workerId}>
            <div>
              <strong>{worker.workerId}</strong>
              <span>{utilization}% busy</span>
            </div>
            <div className="utilization-track">
              <div style={{ width: `${utilization}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date(value));
}

createRoot(document.getElementById("root")!).render(<App />);
