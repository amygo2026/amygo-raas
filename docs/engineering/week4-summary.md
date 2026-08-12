# Week 4 开发总结（2026-08-12）

关联：PRD v1.2 + 开发附加文档 V1.0｜真实链路硬化（Mock 证据门）

## 1. 目标与结果

| Week 4 目标 | 结果 |
|---|---|
| PUDU 超时/断网/回调 | Mock：`timeout_unknown` / `disconnect_mid_mission` / `duplicate_callback`；UNKNOWN → `NEEDS_INTERVENTION`，禁止盲重试 |
| Unitree Show Agent 原型 | `UnitreeShowAgentMock` + `/api/v1/shows/*`（Preflight/Arm/Start/Reconcile/Abort）；无 SDK/关节命令 |
| 监控 | Actuator 暴露 `prometheus`/`metrics`；`/ops/metrics` 含 unknownCommands |
| Runbook / Demo | `docs/runbooks/*` + `week4-demo-checklist.md` |

## 2. 配置

```yaml
raas.pudu-mock.fault-mode: none | timeout_unknown | disconnect_mid_mission | duplicate_callback
raas.unitree-show.simulate-disconnect-on-start: false|true
```

## 3. 测试证据

```bash
cd control-plane && ./gradlew test
# Week4FaultInjectionIntegrationTest
# Week4DisconnectDuplicateIntegrationTest
# Week4ShowAgentPrototypeTest
```

## 4. 开放问题

- 正式 PUDU Webhook/查询 API 未到；Reconnect 生产路径未做
- Show Agent 仍在 control-plane 进程内，后续迁 `edge/venue-show-controller`
- 未接真实 Unitree SDK2

## 5. 回滚

回退本周提交即可；无新 Flyway（本周未加 V4）。
