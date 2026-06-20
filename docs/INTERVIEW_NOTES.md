# Interview Notes

## One-minute explanation

TaskForge is a distributed job processing system. The API accepts work and stores it as a job instead of executing it inside the request thread. Worker nodes poll the database for ready jobs, claim them with a pessimistic lock, process them in a thread pool, and update the job lifecycle. Failed jobs are retried with exponential backoff and are moved to a dead-letter state when they cross the attempt limit. The system also records worker heartbeats, publishes lifecycle events to RabbitMQ, stores queue stats in Redis, and exposes Prometheus metrics for Grafana.

## Principles used

- Producer-consumer pattern: API produces jobs, workers consume jobs.
- Queue-based architecture: work is persisted before execution.
- Asynchronous processing: clients get `202 Accepted` while work continues in the background.
- Horizontal scaling: more backend containers can be started as more worker nodes.
- Fault tolerance: jobs stay in PostgreSQL and stale locks can be released.
- Retry with exponential backoff: repeated failures are delayed before another attempt.
- Dead-letter queue: permanently failing jobs are isolated for debugging or manual requeue.
- Observability: metrics endpoint and dashboard show job status counts and worker health.
- Auditability: timeline events show when a job was created, claimed, retried, completed, or moved to dead-letter state.
- Event-driven integration: lifecycle events are published to RabbitMQ so other services can subscribe without coupling to the database.
- Fast operational reads: Redis stores the latest queue stats snapshot for dashboard-style monitoring.

## Why PostgreSQL locking matters

Multiple worker nodes may poll at the same time. The repository uses pessimistic locking while selecting ready jobs so two workers do not claim the same job. This gives at-least-once processing semantics, so job handlers should be idempotent in a production version.

## Tradeoffs

- This version uses PostgreSQL as the durable queue, which is simple and reliable for moderate scale.
- RabbitMQ is used for lifecycle event publishing, while PostgreSQL remains the source of truth for durable job claiming.
- For very high throughput, Redis Streams, RabbitMQ work queues, or Kafka could replace the PostgreSQL queue.
- The processor simulates job handlers; production handlers would call email, image, export, or webhook services.

## Worker heartbeat

Each worker node updates a heartbeat row with `workerId`, `threadCount`, `lastClaimedJobs`, `status`, and `lastSeenAt`. The dashboard marks a worker alive if the heartbeat is recent. This is useful because distributed systems need to know not only whether jobs are moving, but which workers are healthy.

## Job timeline

Every important lifecycle transition writes an event: `CREATED`, `CLAIMED`, `SUCCEEDED`, `FAILED`, `DEAD`, `REQUEUED`, or `STALE_LOCK_RELEASED`. This makes debugging easier because a user can inspect why a job failed, which worker claimed it, and how many attempts happened.

## Monitoring stack

Docker Compose starts Prometheus and Grafana. Prometheus scrapes Spring Boot Actuator metrics from the API node and worker containers at `/actuator/prometheus`. Grafana is provisioned with a TaskForge overview dashboard showing HTTP throughput, latency, JVM threads, and process uptime.

## Resume bullets

- Built TaskForge, a Java Spring Boot distributed job processing system with PostgreSQL-backed job claiming, RabbitMQ lifecycle events, Redis queue stats, worker heartbeats, and concurrent worker execution.
- Implemented asynchronous job submission, priority scheduling, retry with exponential backoff, stale-lock recovery, job timeline events, dead-letter queue handling, and multi-container worker deployment.
- Added REST APIs, Swagger docs, Docker Compose health checks, Prometheus/Grafana monitoring, lifecycle tests, metrics, and a React dashboard for queue, worker, Redis, and job timeline monitoring.
- Designed the system around producer-consumer, queue-based architecture, fault tolerance, and horizontal worker scaling.
