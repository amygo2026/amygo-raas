# Legacy asset inventory (initial)

Generated from prior analysis of `../fms2026` and `../app-android2026`.
Phase 1 is read-only: no Legacy business code changes in this repo.

## Modules

| Legacy module | Path | Disposition | Target |
|---|---|---|---|
| com-amygo-appserver | fms2026/com-amygo/com-amygo-appserver | EXTRACT + REWRITE | identity, service-mission |
| com-amygo-fms | fms2026/com-amygo/com-amygo-fms | EXTRACT + REWRITE | tenant-site, robot-profile, console-web, audit |
| com-amygo-vcm | fms2026/com-amygo/com-amygo-vcm | WRAP + EXTRACT | adapters/legacy-zeus (P1) |
| com-amygo-persis | fms2026/com-amygo/com-amygo-persis | EXTRACT + REWRITE | new schema + mapping |
| com-website-* | fms2026/com-website | KEEP (outside MVP) | continue hosting separately |
| Amygo app | app-android2026/Amygo | EXTRACT experience / REWRITE later | requester app (post-MVP) |
| AmygoPad | app-android2026/AmygoPad | EXTRACT | field assist patterns |
| SmartControl | app-android2026/SmartControl | RETIRE as cloud control / EXTRACT as remote-assist concept | not cloud low-level teleop |

Detailed reports will be expanded under this folder (`spring-endpoints.md`, `vcm-protocol.md`, etc.).
