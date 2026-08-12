# Week 1 开发总结（2026-08-11）

关联：PRD v1.1 + 开发附加文档 V1.0｜仓库 [amygo2026/amygo-raas](https://github.com/amygo2026/amygo-raas)

## 1. 目标与结果

Week 1 目标（契约与模拟骨架）已完成到可进入 G0/A1 评审的程度：

| 目标 | 结果 |
|---|---|
| Legacy 自动盘点 | 已生成 `docs/legacy/` 10 份报告 |
| Domain / 状态机冻结 | Task 状态机单测 + `docs/engineering/contracts-week1.md` |
| Adapter / Command / Event 契约 | JSON Schema + OpenAPI stub + Adapter TCK |
| DB schema / Outbox / Audit | Flyway `V1__baseline.sql` + JDBC Outbox/Audit |
| Simulator + 故障注入 | `fault-mode`: none/fail_on_submit/expire/duplicate_events/out_of_order |
| 纵向闭环 | Create Task → Scheduler → Simulator → Event → SUCCEEDED（集成测试通过） |

## 2. 本周交付物

### 文档
- `docs/legacy/modules.md`
- `docs/legacy/spring-endpoints.md`
- `docs/legacy/database-entities.md`
- `docs/legacy/vcm-protocol.md`
- `docs/legacy/websocket-events.md`
- `docs/legacy/android-api-calls.md`
- `docs/legacy/dependency-graph.md`
- `docs/legacy/security-findings.md`
- `docs/legacy/behavior-baseline.md`
- `docs/legacy/migration-matrix.md`
- `docs/engineering/contracts-week1.md`
- `docs/engineering/week1-summary.md`（本文）

### 契约
- `contracts/events/command-envelope-v1.json`
- `contracts/events/robot-event-v1.json`
- `contracts/errors/standard-errors-v1.json`
- `contracts/openapi/control-plane-v0.1.yaml`

### 代码
- Control Plane：任务状态机、调度租约、Simulator Adapter、事件投影、Audit/Outbox 持久化
- Console：任务创建与机器人/事件观察
- 测试：`VerticalLoopIntegrationTest`、`AdapterTckTest`、`TaskStateMachineTest`

## 3. 架构决策（本周确认）

1. **新仓开发**：不以 fms2026 改名演进；Legacy 仅 EXTRACT/WRAP。
2. **模块化单体 + 进程内 Simulator**：先验证契约，再替换真实 Adapter。
3. **云端命令仅高层意图**：禁止速度/转向/关节等低层控制进入通用 Command。
4. **租户隔离**：业务 API 以请求头 `X-Tenant-Id` / `X-Site-Id` 为上下文（后续换 OIDC claims）。
5. **本地/CI 使用 H2 PostgreSQL mode + Flyway**；生产切 PostgreSQL 时复用同一 migration。

## 4. 验证证据

```bash
cd control-plane && ./gradlew test
```

预期覆盖：
- 配送任务纵向闭环到 `SUCCEEDED`
- Adapter 幂等、过期拒绝、能力不支持、故障模式拒绝
- Task 非法迁移抛错；终态不可回退

## 5. 明确未做（符合 Week 1 边界）

- 未接入真实 PUDU/KEENON/Unitree SDK
- 未实现 Show 编排域完整 API
- 未上 Redis 租约/分布式 Outbox publisher
- 未完成 Legacy Characterization Test 自动化套件（仅列出候选）
- Console 仍为 MVP 观察台，非完整运营后台

## 6. 风险与开放问题

| 风险 | 状态 | 下一步 |
|---|---|---|
| PUDU 正式接口/账号未到位 | 开放 | Week 3 接入门；缺资料则保持 Simulator |
| KEENON 合作文档缺口 | 开放 | 只做 Mock/Port，不猜字段 |
| H2 vs PostgreSQL 方言差异 | 可控 | Staging 用真实 PostgreSQL 跑 migration |
| 事件乱序策略仍偏保守 | 可控 | Week 2 补 sequence watermark 与指标 |

## 7. Week 2 建议（完整纵向闭环硬化）

1. Task API 取消/失败/重启恢复的自动化压测（目标 1000 次模拟任务）
2. Outbox publisher + 重启不丢/不重放保障
3. Console 状态页：任务时间线、机器人多维状态、Audit 查询
4. Adapter TCK 纳入 CI required check
5. 开始 PUDU 沙箱认证与设备绑定设计（仍不编造 API）

## 8. G0 / A1 检查结论（建议）

- **A0 开工门**：通过（仓库、CI、Rules、初版 Secret 策略文档位）
- **A1 Contract 门**：建议评审通过（契约已版本化；Simulator + TCK 基线可用）
- **进入 Week 2**：可以

---

变更回滚：回退本周提交即可；Flyway V1 为基线首版，无生产数据迁移风险。
