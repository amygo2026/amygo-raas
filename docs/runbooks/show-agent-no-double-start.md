# Runbook: Show Agent Start — no double execution

## Symptom
First `POST /shows/{id}/start` returns `UNKNOWN` (disconnect simulation); `startCount` stays 0.

## Operator steps
1. Do **not** mint a new idempotency key.
2. Call `POST /shows/{id}/start/reconcile` with the **same** `idempotencyKey`.
3. Confirm `startCount == 1` and status `RUNNING`.
4. Abort if venue unsafe: `POST /shows/{id}/abort`.

## Evidence
`Week4ShowAgentPrototypeTest.show_start_unknown_then_reconcile_is_at_most_once`
