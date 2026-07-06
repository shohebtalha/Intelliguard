# Observability

IntelliGuard uses three layers of observability:

- Request correlation with `X-Trace-Id`.
- Application metrics through Spring Actuator and Micrometer Prometheus.
- Provisioned Grafana dashboard and Prometheus alert rules.
- CI-enforced security scanning through CodeQL and dependency review.

## Trace IDs

Every API request receives a trace ID. If the client sends `X-Trace-Id`, the server preserves it; otherwise the server generates one. The same value is returned in the response header and added to SLF4J MDC as `traceId`.

This enables incident workflows such as:

1. Analyst reports a failed transaction decision.
2. Support captures the `X-Trace-Id` from the browser/network log.
3. Engineers search backend logs by `traceId`.
4. The trace links API request, fraud decision, audit log creation, outbox publish, and downstream case creation.

## Metrics

Prometheus scrapes:

- `/actuator/prometheus`
- JVM memory, CPU, GC, HTTP latency, and Spring application metrics
- Future custom fraud metrics:
  - `intelliguard_outbox_pending`
  - `intelliguard_cases_open`
  - `intelliguard_model_fallbacks_15m`
  - `intelliguard_fraud_decisions_total{decision,tenant,ml_used}`
  - `intelliguard_fraud_decision_latency_seconds`
  - Future: `case_resolution_time_seconds`
  - Future: `model_inference_latency_ms`

## Provisioned Files

- Prometheus config: `prometheus.yml`
- Prometheus alert rules: `prometheus-rules.yml`
- Grafana datasource: `grafana/provisioning/datasources/prometheus.yml`
- Grafana dashboard provider: `grafana/provisioning/dashboards/dashboards.yml`
- Grafana dashboard: `grafana/dashboards/intelliguard-overview.json`

## Grafana Dashboards To Provision

- API health: request rate, error rate, p95/p99 latency.
- Fraud operations: approve/review/block rate, case backlog, case SLA.
- Eventing: outbox pending count, publish failures, Kafka publish latency.
- ML: model availability, inference latency, fraud score distribution.
- Infrastructure: JVM heap, GC pause, DB pool usage, Redis latency.

## Alert Policy

| Alert | Signal | Action |
| --- | --- | --- |
| API error budget burn | 5xx rate or p99 latency breach | Page on-call |
| Fraud scoring degraded | ML unavailable or rule-only fallback spike | Notify fraud platform owner |
| Outbox stuck | Pending outbox events older than threshold | Page backend owner |
| Case SLA breach | High-priority cases unresolved beyond SLA | Notify fraud operations |
| Audit chain failure | Audit hash verification mismatch | Security incident |
