# Week 6 开发总结（2026-08-12）

关联：CLEANING 纵向 + RAAS-015 Tracing（Micrometer → OpenTelemetry）

## 1. CLEANING 纵向

| 项 | 结果 |
|---|---|
| `POST /tasks` `taskType` | `DELIVERY` / `CLEANING` / `HOTEL_DELIVERY`；未知类型 → 400 `CAPABILITY_NOT_SUPPORTED` |
| 调度 | 按 required capability 选机；优先匹配 `sim.cleaning.v1` 等 profile |
| 命令 | `CLEANING` → `CLEANING_START` |
| Simulator 能力 | 按 modelProfile 收窄（cleaning 机无 delivery 等） |
| Console | Ops「创建清洁」；首页「触发演示清洁」 |

## 2. RAAS-015 Tracing

| 项 | 结果 |
|---|---|
| 依赖 | `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-logging` |
| 业务 span | `raas.mission.create` / `raas.mission.dispatch` / `raas.robot.reconnect` |
| 日志 | pattern 含 `traceId` / `spanId` |
| 导出 | LoggingSpanExporter（无 Collector）；`/ops/metrics.tracing` 暴露 bridge/采样 |
| 采样 | `management.tracing.sampling.probability: 1.0`（本地 Demo） |

## 3. 测试

```bash
cd control-plane && ./gradlew test
# Week6CleaningTracingIntegrationTest
```

## 4. 开放问题

- OTLP Collector / Tempo / Jaeger 未接（换 SpanExporter 即可）
- HOTEL_DELIVERY 仅映射到 delivery 命令，未做独立酒店设施联动
- 厂商沙箱仍阻塞

## 5. 回滚

回退本周提交；无新 Flyway。
