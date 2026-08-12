# Runbook: Mid-mission disconnect + Reconnect / Resync (Mock)

## Symptom
- Robot `connectivityStatus=OFFLINE`
- Task either `FAILED` (`fail_on_disconnect`) or `NEEDS_INTERVENTION` (`hold_on_disconnect`)
- Event `robot.connectivity.changed` + `task.failed` with reason `disconnect_mid_mission`

## Config
```yaml
raas:
  ops:
    offline-policy: fail_on_disconnect   # or hold_on_disconnect
  pudu-mock:
    fault-mode: disconnect_mid_mission   # test injection only
```

| Policy | Task outcome | Next step |
|---|---|---|
| `fail_on_disconnect` (default) | `FAILED` | Reconnect → **restart** (new Task) |
| `hold_on_disconnect` | `NEEDS_INTERVENTION` | Reconnect → operator **fail** → **restart** |

## Operator steps
1. Verify site network / Edge health.
2. Call **Reconnect**:
   - `POST /api/v1/robots/{robotId}/reconnect`
   - Headers: `X-Tenant-Id`, `X-Site-Id`, `X-Actor-Id`
3. Confirm robot `connectivityStatus=ONLINE` and Audit `robot.reconnected`.
4. Close / recover the mission:
   - Fail path: `POST .../tasks/{id}/restart`
   - Hold path: `POST .../fail` then `POST .../restart`
5. Capture Audit + events for SAT evidence.

## Console
Ops → Robots → **Reconnect** on OFFLINE rows.

## Evidence
- `Week4DisconnectDuplicateIntegrationTest` (fail policy default)
- `Week5ReconnectResyncIntegrationTest` (hold + reconnect)

## Non-goals
- Not a real PUDU webhook replay / Edge mTLS path
- Does not invent vendor reconnect APIs
