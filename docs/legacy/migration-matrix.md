# Migration matrix

Disposition tags: KEEP | EXTRACT | WRAP | REWRITE | RETIRE

| Asset | Path / class area | Disposition | Target in amygo-raas | Notes |
|---|---|---|---|---|
| appserver auth/JWT patterns | com-amygo-appserver | EXTRACT + REWRITE | identity | Keep experience, new OIDC |
| Order lifecycle rules | appserver OrderService | EXTRACT + REWRITE | service-mission | Map to Task/Assignment/Attempt |
| infoInitial availability | appserver OrderController | EXTRACT | tenant-site + scheduler | Fence/hours/capacity |
| FMS RBAC | fms Administrator/Role/Menu/Resource | EXTRACT + REWRITE | identity + console-web | New UI |
| FMS fence/region | SystemVariableController | EXTRACT + REWRITE | tenant-site | Station/Zone model |
| FMS track/status | CarStatus* | EXTRACT + REWRITE | robot-profile + events | Multi-dim robot state |
| FMS remote steer/accel | CarRemoteControl* | RETIRE (cloud) / WRAP if needed | none / legacy-zeus only | Forbidden as cloud command |
| VCM Netty protocol | com-amygo-vcm | WRAP | adapters/legacy-zeus | P1 |
| VCM low-level vehicle cmds | IPCVehicle* | RETIRE from cloud contract | — | Local/vendor only |
| persis entities | com-amygo-persis | EXTRACT + REWRITE | db-migration | New schema + legacy_id |
| website portal/cms | com-website-* | KEEP | outside MVP | Continue separately |
| Amygo Retrofit APIs | NetwordLibrary | EXTRACT | contracts/openapi inspiration | New Task API |
| Amygo order UX | app panels | EXTRACT | future requester app | Not MVP frontend |
| AmygoPad | AmygoPad | EXTRACT | field assist later | |
| SmartControl UDP teleop | SmartControl | RETIRE as cloud path | remote-assist concept only | Safety boundary |
| Eureka discovery | discovery-eureka (bitbucket variant) | RETIRE | — | Not needed for modular monolith |

## Explicit non-migrations
- Do not rename Car→Robot or Order→Task in Legacy code.
- Do not reuse Thymeleaf ops UI.
- Do not expose Zeus private protocol as platform kernel.
