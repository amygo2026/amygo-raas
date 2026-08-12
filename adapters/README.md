# adapters/

Runtime adapter implementations currently live in `control-plane` modules:

- `ai.amygo.raas.adapter.simulator` — Simulator (support: Simulator)
- `ai.amygo.raas.adapter.pudu` — PUDU Mock (support: Mock)
- `ai.amygo.raas.adapter.keenon` — KEENON Mock (support: Mock)
- `ai.amygo.raas.adapter.AdapterRouter` — routes by `robot.adapterType`

This directory is reserved for future extractable artifacts (`adapter-sdk`, `adapter-tck`, real vendor modules) once formal docs/sandbox exist. **Do not invent vendor APIs here.**
