# AMYGO iOS Apps — 开发计划

完整 Field/Ops 条文见 `docs/product/PRD_MVP_v1.1.txt` 附录 E。  
**2026-08-12 决策：** 拆成两个独立 App；先做客户请求端。

## 双 App 分工

| App | 仓库 | 用户 | 职责 | 现状 |
|---|---|---|---|---|
| **Customer**（先做） | 本地 `../amygo-customer-ios` → 远端建议 `amygo2026/amygo-customer-ios` | 终端顾客 / 住客 / 下单人 | 选场景、发起服务请求、跟踪自己的任务 | **Cust-M0 已本地建仓** |
| **Ops / Field**（后做） | 待建 `amygo-ops-ios` | 店长、礼宾、班组长、Show 值守 | 机队监控、任务干预、告警、Show 预检 | **现阶段用 Web Console**（`console-web`） |

共同约束：
- 只调 Control Plane 高层 API；禁止速度/转向/关节/力矩等低层遥控
- 同源 OpenAPI 契约（`contracts/openapi/control-plane-v0.1.yaml`）
- 写操作带 actor，并落 Audit

## Customer App 分期（当前优先）

| 里程碑 | 窗口 | 要点 |
|---|---|---|
| **Cust-M0** | 现在 | 独立仓、SwiftUI 壳、Staging/Mock API、服务目录、发起请求、我的任务只读 |
| Cust-M1 | 随后 | 配送请求闭环 + 状态时间线 + 取消 |
| Cust-M2 | 随后 | 清洁 / 酒店送物请求 |
| Cust-M3 | Pilot 前 | 登录（OIDC）、推送、站点扫码/选桌 |
| Cust-GA | 2027-02 Pilot | 与 Pilot 证据包对齐 |

## Ops App 分期（Web 先行）

| 里程碑 | 要点 |
|---|---|
| Ops-Web（当前） | Fleet Dashboard + Ops Console |
| Ops-M0 | 独立仓脚手架（对齐原附录 E iOS-M0） |
| Ops-M1+ | 配送干预、清洁/酒店、Show 值守（原 iOS-M1～M3） |

## 仓库边界

- **不要**把 iOS 工程塞进 `amygo` 单体
- Customer / Ops 两个仓各自 CI、TestFlight、Bundle ID
- 共享的只有后端契约与文档链接，不共享可执行 App 代码
