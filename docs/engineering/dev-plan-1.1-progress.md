# 开发计划 1.1 · 进度看板（2026-08-12）

对应：`DEV_ADDENDUM` §11.1 四周启动计划 + PRD §1.1 六项核心决策。  
仓库实现进度（不含正式厂商沙箱账号依赖项）。

## 1. 四周启动计划 + 加码周

| 周期 | 目标 | 状态 | 证据 / 说明 |
|---|---|---|---|
| Day 0–2 | 工程启动 | **完成** | 新仓、CI、Cursor Rules、可构建；README |
| Week 1 | 契约与模拟骨架 | **完成** | `week1-summary.md` |
| Week 2 | 完整纵向闭环硬化 | **完成** | `week2-summary.md` |
| Week 3 | PUDU 接入起步 | **部分完成（Mock 门）** | **缺**正式沙箱实机 |
| Week 4 | 真实链路硬化 | **部分完成（Mock 证据门）** | **缺**实机回调 |
| Week 5 | Reconnect / 离线策略 | **完成（Mock）** | `week5-summary.md` |
| Week 6 | CLEANING + Tracing | **完成（Mock）** | `week6-summary.md` |
| Week 7 | HOTEL + Watermark 持久化 | **完成（Mock）** | `week7-summary.md` |

## 2. PRD §1.1 六项核心决策 · 落地进度

| 编号 | 决策 | 落地状态 |
|---|---|---|
| D1 | 商用服务 + 宇树表演 | **进行中**：配送+清洁+酒店 Mock；Show 原型 |
| D2 | 云端高层 / 本地安全 | **已遵守** |
| D3 | 双执行平面 | **骨架**：Service 闭环；Show 仍进程内 |
| D4 | 三厂商接入 | **Mock**；Supported 待正式文档 |
| D5 | Legacy 不阻断 | **已执行** |
| D6 | 12 月 Demo / 2 月 Pilot | **Demo 增强中**；Pilot 未开始 |

## 3. Issue 摘要

| Issue | 状态 |
|---|---|
| RAAS-001～011 | **完成** |
| RAAS-012 PUDU 沙箱 | **阻塞** |
| RAAS-013 KEENON 资料 | **开放**（Mock 已立） |
| RAAS-014 Unitree Show | **原型完成（Mock）** |
| RAAS-015 Tracing | **基本完成**（Logging 导出；OTLP 可选） |

## 4. 下一步建议

1. PUDU 正式沙箱 → Beta/HIL（关键路径）  
2. Show Agent 迁 Edge 脚手架  
3. **Customer iOS 独立仓**（`amygo-customer-ios`，Cust-M0）— 用户请求服务；Ops 移动端后置，现用 Web  
4. OTLP → Tempo/Jaeger  
5. 酒店电梯/门禁联动（有楼宇 API 后）  
6. Ops iOS（`amygo-ops-ios`）在 Web 稳态后再开工
