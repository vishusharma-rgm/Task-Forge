# TaskForge API

Base URL: `http://localhost:8080`

## Submit a job

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"EMAIL","priority":80,"payload":"{\"to\":\"student@example.com\"}"}'
```

## Submit a failing job

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{"type":"WEBHOOK","priority":20,"payload":"{\"fail\":true}"}'
```

## List jobs

```bash
curl http://localhost:8080/api/jobs
curl "http://localhost:8080/api/jobs?status=DEAD"
```

## Requeue a failed or dead job

```bash
curl -X POST http://localhost:8080/api/jobs/<job-id>/requeue
```

## Metrics

```bash
curl http://localhost:8080/api/metrics
```

## Redis queue stats

```bash
curl http://localhost:8080/api/metrics/redis
```

## Worker heartbeats

```bash
curl http://localhost:8080/api/workers
```

## Job timeline

```bash
curl http://localhost:8080/api/jobs/<job-id>/events
```

## OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Operations UIs

- RabbitMQ management: `http://localhost:15672` with `taskforge` / `taskforge`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` with `admin` / `taskforge`
