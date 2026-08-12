# PUDU 沙箱认证与设备绑定设计（Week 2 · 文档 only）

状态：**设计草案 / 开放问题清单** — **不编造** PUDU API 字段、URL、错误码。  
正式接口/样例/账号到位前，实现边界仅限 Port + Mock + Simulator。

## 1. 目标

为 Week 3「PUDU 接入起步」准备：

1. 沙箱认证方式（谁发令牌、如何轮换、如何吊销）
2. 设备绑定模型（tenant/site ↔ vendor device）
3. 状态拉取 vs Webhook 的接入假设
4. Adapter TCK 进入 Beta 前的门禁

## 2. 已知约束（来自 PRD / 工程附加文档）

- Domain **不得**依赖 PUDU DTO / SDK 类型；仅 Adapter 模块可持有厂商类型。
- 云端命令仅为高层意图（配送/清洁/回充/舱门等），禁止速度/关节等低层控制。
- 所有命令携带：`commandId`、`correlationId`、`tenantId`、`siteId`、`robotId`、`expiresAt`、`idempotencyKey`、`actor`。
- 缺正式文档时只做 Port / Mock / 开放问题，返回 `CAPABILITY_NOT_SUPPORTED` 显式拒绝。

## 3. 建议 Port 草图（无厂商字段）

```text
PuduAuthPort
  - authenticate(sandboxCredentialsRef) -> AccessTokenHandle
  - refresh(handle) -> AccessTokenHandle

PuduDeviceBindingPort
  - listBoundDevices(tenantId, siteId) -> List<BoundDeviceRef>
  - bind(tenantId, siteId, vendorDeviceRef, robotId) -> Binding
  - unbind(...)

PuduMissionPort (extends RobotAdapter semantics)
  - submit(CommandEnvelope) / cancel(CommandEnvelope)
  - subscribe(events) or poll(checkpoint)
```

`BoundDeviceRef` / `AccessTokenHandle` 为 **平台内部** 类型；映射表放在 `adapters/pudu`。

## 4. 设备绑定设计原则

| 概念 | 平台侧 | 厂商侧（待证实） |
|---|---|---|
| Robot | `robot.id` + `adapter_type=PUDU` + `model_profile` | vendor device id / sn（来源：正式文档） |
| Site mapping | `tenant_id` + `site_id` | 门店/场地编码（来源：正式文档） |
| Binding | `robot_vendor_binding` 表（待 V3+） | OAuth client / device secret（来源：正式文档） |

绑定变更必须写 **Audit Log**；禁止请求体 `tenantId` 作为授权依据。

## 5. 认证开放问题（阻塞 Week 3 实接）

- [ ] 沙箱是 OAuth2 Client Credentials、API Key，还是设备证书？
- [ ] Token 有效期与刷新策略？是否支持多环境（sandbox/prod）隔离？
- [ ] 凭证存放：Secret Manager / KMS？旋转 Runbook？
- [ ] 是否有 IP allowlist / mTLS？
- [ ] 测试账号与设备白名单谁审批？

**未回答前**：`PuduRobotAdapter` 不得合并为 Supported；CI 仅跑 Simulator TCK。

## 6. 事件通道开放问题

- [ ] Webhook 签名算法与重放窗口？
- [ ] 是否提供 sequence / cursor 便于乱序与补拉？
- [ ] 断网后的补发保证（at-least-once？）
- [ ] 与平台 `EventSequenceGuard` 的 `(source, robotId, sequence)` 如何对齐？

在答案明确前，保持 Simulator 的 watermark / buffer 行为作为控制面契约。

## 7. 设备绑定流程（产品级，待厂商确认）

```text
Ops 选择 Site → 输入/扫描 Vendor Device Ref（正式字段名 TBD）
  → Adapter 校验沙箱可达
  → 写 binding + Audit
  → Robot Registry 显示 ONLINE/UNKNOWN（以厂商状态 API 为准）
```

失败路径：凭证无效、设备已被其他 tenant 绑定、能力矩阵不匹配 → 显式错误码（平台标准错误 + Adapter 映射）。

## 8. Week 3 进入条件（建议）

1. 正式接口文档或可执行样例（含认证与至少一种配送任务）
2. 沙箱账号 + ≥1 台测试设备
3. `adapters/pudu` 通过 Adapter TCK（可先 Beta）
4. 绑定/解绑 Audit 与租户隔离测试通过

## 9. 明确不做（本设计）

- 不猜测 REST path / JSON 字段名
- 不在 Domain 引入 `com.pudu.*`
- 不把 Zeus/FMS Legacy 协议当作 PUDU 协议

## 10. 回滚

删除本文件不影响运行时；后续若落地 migration/binding 表，按 Flyway 版本回滚说明执行。
