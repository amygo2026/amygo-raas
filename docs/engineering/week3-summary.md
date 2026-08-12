# Week 3 开发总结（2026-08-12）

关联：PRD v1.2（含 iOS 计划）+ 开发附加文档 V1.0｜PUDU 接入起步（Mock 门）

## 1. 目标与结果

| Week 3 目标 | 结果 |
|---|---|
| PUDU 认证/绑定/配送起步 | **Mock 路径**：`PuduMockRobotAdapter` + 设备绑定 API；**无正式沙箱账号前不标记 Supported** |
| Adapter TCK / 路由 | `AdapterRouter` 按 `robot.adapterType` 分发；CI 仍跑 Simulator TCK |
| KEENON Mock | `KeenonMockRobotAdapter`（Mock） |
| 不编造厂商 API | vendor_device_ref 不透明字符串；缺文档能力显式 `CAPABILITY_NOT_SUPPORTED` |
| 产品 PRD | **v1.2** 增加附录 E：iOS Field App 开发计划 |

## 2. 行为差异

- Demo 机器人增加 `robot-pudu-mock-01`、`robot-keenon-mock-01`
- `POST/GET/DELETE /api/v1/bindings`；绑定默认 `MOCK_BOUND` 并写 Audit/Outbox
- `GET /api/v1/adapters` 返回各 Adapter 支持级别

## 3. 测试证据

```bash
cd control-plane && ./gradlew test
# 含 Week3VendorMockIntegrationTest（PUDU mock 配送闭环 + 绑定审计）
```

## 4. 开放问题（阻断真实 Supported）

- PUDU/KEENON 正式接口、沙箱账号、设备白名单仍未到位
- 业务态仍内存为主；Staging 需 PostgreSQL 仓储

## 5. 回滚

回退本周提交；Flyway V3 仅新增 `robot_vendor_binding`，无生产数据。

## 6. 下一步（Week 4 / 有账号后）

正式沙箱联调、超时/断网/回调、Unitree Show Agent 原型、监控 Runbook；并行启动 iOS-M0 脚手架（见 PRD 附录 E）。
