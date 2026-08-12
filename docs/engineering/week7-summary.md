# Week 7 开发总结（2026-08-12）

关联：HOTEL_DELIVERY 纵向 + event_sequence_watermark 持久化

## 1. HOTEL_DELIVERY

| 项 | 结果 |
|---|---|
| taskType | `HOTEL_DELIVERY`（payload: roomNumber / floor / compartment） |
| 能力门槛 | 调度要求 `compartment`；优先 `sim.hotel.v1` |
| 命令 | `HOTEL_DELIVERY_START`（Simulator / PUDU / KEENON Mock 已映射） |
| Console | Ops「创建酒店配送」；首页「触发演示酒店配送」 |

## 2. Watermark 持久化

| 项 | 结果 |
|---|---|
| 表 | 既有 Flyway V2 `event_sequence_watermark` |
| 接线 | `WatermarkRepository` + `EventSequenceGuard` 启动加载 / 推进时 upsert |
| 指标 | `/ops/metrics.sequence.persisted` |

## 3. 测试

```bash
cd control-plane && ./gradlew test
# Week7HotelWatermarkIntegrationTest
# EventSequenceGuardTest
```

## 4. 开放问题

- 酒店电梯/门禁设施联动未做（无厂商/楼宇 API）
- Watermark 进程崩溃恢复：内存 gap-buffer 不持久；仅 last_sequence 持久
- Adapter 序号现从持久 watermark 续号，避免 H2/进程复用时 late-drop
- 厂商沙箱仍阻塞

## 5. 回滚

回退本周提交；无新 Flyway。
