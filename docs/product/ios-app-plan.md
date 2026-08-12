# AMYGO Field iOS — 开发计划（摘自 PRD v1.2 附录 E）

完整条文见 `docs/product/PRD_MVP_v1.1.txt` 附录 E。此处为工程速查。

## 定位
- 现场运营 / 场馆值守伴侣 App（iOS 17+）
- Web Console = 多站点主控；iOS = 现场发起与轻干预
- 调用同一 Control Plane API；禁止低层遥控

## 分期
| 里程碑 | 窗口 | 要点 |
|---|---|---|
| iOS-M0 | S3–S4 | 工程/CI/TestFlight/OIDC/只读列表 |
| iOS-M1 | S5–S6 | 配送闭环 + 推送 |
| iOS-M2 | S6–S7 | 清洁/酒店 |
| iOS-M3 | S8–S10 | Show 值守 |
| iOS-GA | 2027-02 Pilot | Pilot 证据包 |

## 仓库建议（尚未创建）
`app-ios2026/` 或 `amygo-field-ios/` — 待 M0 开工时建仓，不放入本单体强制依赖。
