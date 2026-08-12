# Week 4 Demo Checklist

## Prep
- [ ] `control-plane` bootRun on :8080
- [ ] Console on :5173 (LAN optional)
- [ ] `./gradlew test` green (Week4 fault + Show tests)

## Demo script
1. **Happy path**: Create delivery → SUCCEEDED (Simulator/PUDU mock with fault=none).
2. **Timeout UNKNOWN**: set `raas.pudu-mock.fault-mode=timeout_unknown`, restart, create task on PUDU-only site → `NEEDS_INTERVENTION`, show `/commands/unknown`.
3. **Disconnect**: `disconnect_mid_mission` → FAILED + robot OFFLINE.
4. **Show Agent**: create show → preflight → arm → start (UNKNOWN) → reconcile → startCount=1 → abort.
5. **Metrics**: open `/actuator/prometheus` and `/api/v1/ops/metrics`.

## Exit criteria
- [ ] No blind retry of unknown commands
- [ ] Show Start at-most-once under disconnect
- [ ] Fault-injection evidence from automated tests attached
