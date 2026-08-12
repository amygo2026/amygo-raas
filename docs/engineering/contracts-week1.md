# Core contracts (Week 1 freeze)

Status: **Frozen for Sprint 1 entry** (changes require version bump + compatibility tests)

## 1. RobotAdapter

```java
public interface RobotAdapter {
  AdapterDescriptor descriptor();
  Set<String> capabilities(String robotId);
  RobotSnapshot getSnapshot(String robotId);
  CommandReceipt submit(CommandEnvelope command);
  CommandReceipt cancel(CommandEnvelope command);
  void subscribe(Consumer<RobotEvent> listener);
}
```

Support levels: `Simulator` | `Beta` | `Supported` | `Blocked`

## 2. CommandEnvelope v1

Required fields: `commandId`, `correlationId`, `tenantId`, `siteId`, `robotId`, `commandType`, `idempotencyKey`, `issuedAt`, `expiresAt`, `actor`, `payload`

Allowed cloud command types (MVP service):
- `DELIVERY_START` / `DELIVER`
- `CLEAN` / `CLEANING_START`
- `RETURN_TO_DOCK`
- `PAUSE` / `RESUME` / `CANCEL`
- `OPEN_COMPARTMENT` (capability-gated)

Forbidden as cloud commands: speed, steering, joint angles, torque, continuous gait streams.

Receipt statuses: `RECEIVED` | `ACCEPTED` | `REJECTED` | `UNKNOWN`

## 3. RobotEvent v1

Required: `eventId`, `eventType`, `schemaVersion`, `tenantId`, `siteId`, `robotId`, `sequence`, `occurredAt`, `receivedAt`, `source`, `payload`

Processing rules:
- Dedupe by `eventId`
- Detect reorder via `(source, robotId, sequence)`
- Business transitions only on allowed edges

## 4. Task state machine

```
DRAFT → QUEUED → ASSIGNED → DISPATCHING → RUNNING
                                         ├→ SUCCEEDED
                                         ├→ FAILED
                                         ├→ CANCELED
                                         ├→ SUSPENDED
                                         └→ NEEDS_INTERVENTION
```

Invariants:
- One active assignment per task
- One primary lease per robot
- Terminal states do not return to RUNNING
- Task status ≠ Robot status

Objects: `Task 1—N Assignment 1—N ExecutionAttempt 1—N Command/Event`

## 5. Robot multi-dimensional status

| Dimension | Values |
|---|---|
| connectivity | ONLINE / DEGRADED / OFFLINE |
| operational | AVAILABLE / BUSY / PAUSED / ERROR / MAINTENANCE |
| mission | IDLE / ACCEPTING / EXECUTING / RETURNING |
| battery | NORMAL / LOW / CRITICAL / CHARGING |
| localization | LOCALIZED / LOST / UNKNOWN |
| safety | NORMAL / PROTECTIVE_STOP / E_STOP |
| maintenance | OK / DUE / BLOCKED |

Schedulable requires: ONLINE + AVAILABLE + LOCALIZED + safety NORMAL + battery≠CRITICAL + maintenance≠BLOCKED + no active lease.

## 6. Standard errors

See `contracts/errors/standard-errors-v1.json`.

## 7. Site mapping

Tasks use Station/Zone IDs; adapters map to vendor place/target IDs. Raw coordinates are display/diagnostics only unless explicitly validated.
