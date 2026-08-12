# Week 5 开发总结（2026-08-12）

关联：PRD / DEV_ADDENDUM｜Reconnect / Resync + 离线策略 v0（Mock）

## 1. 目标与结果

| 目标 | 结果 |
|---|---|
| 闭合 Week4 断网半环 | `POST /api/v1/robots/{id}/reconnect`：markOnline → adapter snapshot → Audit `robot.reconnected` → scheduleNext |
| 离线策略显式化 | `raas.ops.offline-policy`: `fail_on_disconnect`（默认，兼容 Week4）/ `hold_on_disconnect` → `NEEDS_INTERVENTION` |
| 连通性事件 | Mission 层应用 `robot.connectivity.changed`；不再只依赖 Adapter 侧写库 |
| Console | Ops / 指挥中心对 OFFLINE 机器人提供 **Reconnect** |
| 契约 / Runbook | OpenAPI + `docs/runbooks/reconnect-resync.md` |

## 2. 配置

```yaml
raas:
  ops:
    offline-policy: fail_on_disconnect   # or hold_on_disconnect
```

`/api/v1/ops/metrics` 增加 `offlinePolicy`。

## 3. 测试证据

```bash
cd control-plane && ./gradlew test
# Week5ReconnectResyncIntegrationTest
# Week4DisconnectDuplicateIntegrationTest（默认 fail 策略仍绿）
```

## 4. 开放问题

- 真实厂商 webhook 重放 / Edge mTLS 未做
- PUDU 故障模式全局，同进程内难在 hold 后对同一 Mock 机验证 SUCCEEDED（用 SIMULATOR 站点旁路验证）
- Tracing（RAAS-015）仍未上；Show→Edge 仍待独立切片

## 5. 回滚

回退本周提交；无新 Flyway。
