# Week 2 开发总结（2026-08-12）

关联：PRD v1.1 + 开发附加文档 V1.0｜A2 纵向闭环门

## 1. 目标与结果

| A2 / Week 2 目标 | 结果 |
|---|---|
| Task 取消 / 失败 / 重启恢复 | `POST .../cancel|fail|restart`；终态不复活，restart 新建 Task |
| ~1000 次模拟任务 | `Week2HardeningIntegrationTest.stress_one_thousand_simulated_tasks` 通过 |
| Outbox publisher + 不丢/不重放 | `OutboxPublisher` + claim/mark；sink 按 outbox id 幂等 |
| Event sequence watermark | `EventSequenceGuard` 缓冲乱序、丢弃 late |
| Console 状态页 | Timeline / 全维机器人 / Audit / cancel·fail·restart |
| Adapter TCK CI required job | `.github/workflows/ci.yml` → `adapter-tck` |
| PUDU 沙箱认证/绑定设计 | `docs/engineering/pudu-sandbox-auth-binding-design.md`（无臆造 API） |

## 2. 行为差异（相对 Week 1）

- 事件按 `(source, robotId, sequence)` 有序应用；缺口缓冲，迟到丢弃。
- Audit 动作额外写入 Outbox；定时 publisher 投递到进程内幂等 sink。
- Restart 创建新 Task（`attemptNo+1`，`payload.restartedFromTaskId`）。
- Simulator 支持 `raas.simulator.progress-delay-ms`（压测可设 0）。

## 3. 变更文件（主要）

- Control Plane：`MissionApplicationService`、`MissionController`、`EventSequenceGuard`、`OutboxPublisher`、`OutboxRepository`、`SimulatorRobotAdapter`、`Task`、`InMemoryStore`
- Migration：`V2__outbox_and_watermark.sql`
- Tests：`Week2HardeningIntegrationTest`、`EventSequenceGuardTest`
- Console：`console-web/src/App.tsx`
- Contracts：`contracts/openapi/control-plane-v0.1.yaml` → 0.2.0
- CI / Docs：`ci.yml`、`pudu-sandbox-auth-binding-design.md`、本文

## 4. 测试证据

```bash
cd control-plane && ./gradlew test
# 含 VerticalLoop / AdapterTck / EventSequenceGuard / Week2Hardening（含 1000 任务）
```

## 5. 开放问题

- 业务态仍主要在内存；进程重启后 Task/Robot 不持久（Outbox/Audit 在 H2 mem 亦随进程消失）。Staging 需 PostgreSQL + 业务表仓储。
- Outbox sink 仍为进程内；生产需替换为消息总线且保持幂等消费者。
- PUDU 正式 API/账号未到；不得进入 Supported。

## 6. 回滚

回退本周提交；Flyway V2 仅增加 outbox 列与 watermark 表，无生产数据迁移风险（尚未上生产）。
