# AMYGO RaaS (`amygo-raas`)

Physical Service & Show Control Plane — MVP v1.0 development repository.

## Product intent

Customer submits a physical-world task; platform selects site/robot/capability/path; execution is observable, recoverable, and auditable.

First verticals:

- Commercial service robots (PUDU / KEENON): cleaning, restaurant delivery, hotel delivery
- Unitree show orchestration (venue-local execution)

Legacy `fms2026` / `app-android2026` are knowledge assets, not the code base.

## Plan 1.1 progress

See **[docs/engineering/dev-plan-1.1-progress.md](docs/engineering/dev-plan-1.1-progress.md)**  
(Day0–Week2+Week5–7 done; Week3/4 Mock gate; Hotel + watermark persist; PRD §1.1 D1–D6 status).

## Repo layout

```
control-plane/     Java 21 + Spring Boot modular monolith (API + in-proc simulator loop)
console-web/       React + TypeScript — Home DEMO · Ops · Fleet Dashboard
adapters/          Vendor adapters (simulator / pudu mock / keenon mock)
contracts/         Command/Event schemas
docs/              PRD, engineering, runbooks, legacy
.cursor/rules/     Cursor engineering guardrails
```

## Console routes

| Path | Purpose |
|---|---|
| `/` | Home — DEMO CTAs + plan 1.1 progress strip |
| `/ops` | Ops console (tasks / robots / audit) |
| `/demo/command-center` | Mock Fleet Dashboard (KPIs + floor map + live feeds) |

### Run backend

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
cd control-plane
./gradlew bootRun
```

API: `http://localhost:8080`

### Run console

```bash
cd console-web
npm install
npm run dev
```

Open `http://localhost:5173/` (LAN: `http://<host-ip>:5173/`).

### Test

```bash
cd control-plane
./gradlew test
```

## Documents

- `docs/product/PRD_MVP_v1.1.txt` — PRD (+ §1.1 progress note, iOS appendix E)
- `docs/engineering/dev-plan-1.1-progress.md` — four-week + decision progress
- `docs/engineering/week1-summary.md` … `week4-summary.md`
- `docs/runbooks/` — timeout / disconnect / show start
- `docs/legacy/` — Legacy inventory + migration matrix

## Non-goals (enforced)

- No cloud low-level teleop (speed/steer/joints/torque)
- No invented vendor APIs
- No vendor DTO leakage into domain
- Simulator/Mock must not be labeled Supported / production
