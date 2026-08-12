# Runbook: Tracing (RAAS-015)

## What is enabled
- Micrometer Observation → OpenTelemetry bridge
- Business spans: `raas.mission.create`, `raas.mission.dispatch`, `raas.robot.reconnect`
- HTTP spans via Spring Boot auto-instrumentation
- Logs include `traceId` / `spanId` in level pattern
- Default exporter: **LoggingSpanExporter** (stdout) — no collector required

## Verify
```bash
curl -s http://localhost:8080/api/v1/ops/metrics | jq .tracing
# { "bridge": "micrometer-otel", "samplingProbability": 1.0 }

# Create a task and watch control-plane logs for span dumps / traceId
```

## Ops upgrade (optional)
Replace logging exporter with OTLP (Tempo/Jaeger/Collector):
1. Add `io.opentelemetry:opentelemetry-exporter-otlp`
2. Remove or override `LoggingSpanExporter` bean
3. Set `management.otlp.tracing.endpoint=http://collector:4318/v1/traces`

## Evidence
`Week6CleaningTracingIntegrationTest.ops_metrics_exposes_tracing_bridge`
