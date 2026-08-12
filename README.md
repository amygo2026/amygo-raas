# AMYGO RaaS (`amygo-raas`)

Physical Service & Show Control Plane — MVP v1.0 development repository.

## Product intent

Customer submits a physical-world task; platform selects site/robot/capability/path; execution is observable, recoverable, and auditable.

First verticals:

- Commercial service robots (PUDU / KEENON): cleaning, restaurant delivery, hotel delivery
- Unitree show orchestration (venue-local execution)

Legacy `fms2026` / `app-android2026` are knowledge assets, not the code base.

## Repo layout

```
control-plane/     Java 21 + Spring Boot modular monolith (API + in-proc simulator loop)
console-web/       React + TypeScript ops console (MVP)
adapters/          Vendor adapters (simulator first)
contracts/         Command/Event schemas
docs/              PRD, engineering addendum, legacy, ADRs
.cursor/rules/     Cursor engineering guardrails
```

## Day-0 vertical loop (implemented)

`Console/API → Create Task → Scheduler lease → Simulator Adapter → Events → Task SUCCEEDED → Audit`

### Run backend

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
cd control-plane
./gradlew bootRun
```

API: `http://localhost:8080`

- `POST /api/v1/tasks`
- `GET /api/v1/tasks`
- `GET /api/v1/robots`
- `GET /api/v1/events`
- `GET /api/v1/audit`

### Run console

```bash
cd console-web
npm install
npm run dev
```

### Test

```bash
cd control-plane
./gradlew test
```

## Documents

- `docs/product/` — PRD v1.1
- `docs/engineering/` — development addendum V1.0
- `docs/engineering/contracts-week1.md` — frozen contracts
- `docs/engineering/week1-summary.md` — Week 1 development summary
- `docs/legacy/` — Legacy inventory + migration matrix

## Week 1 status

Completed: Legacy inventory, contract freeze, Flyway/Outbox/Audit baseline, Simulator fault injection, Adapter TCK. See `docs/engineering/week1-summary.md`.

## Non-goals (enforced)

- No cloud low-level teleop (speed/steer/joints/torque)
- No invented vendor APIs
- No vendor DTO leakage into domain
