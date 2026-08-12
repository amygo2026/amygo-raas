# Runbook: PUDU Mock submit timeout → UNKNOWN

## Symptom
Task stuck in `NEEDS_INTERVENTION`; `/api/v1/commands/unknown` lists the command; Audit has `command.unknown`.

## Cause
Adapter returned `CommandReceiptStatus.UNKNOWN` (e.g. `raas.pudu-mock.fault-mode=timeout_unknown`). Control plane **must not** re-submit the same `idempotencyKey`.

## Operator steps
1. Confirm task status and unknown command list.
2. Prefer **fail** or **cancel** the task, then **restart** (new Task / attempt) if business still needs delivery.
3. Do **not** manually replay the same command id.
4. After formal PUDU docs: replace Mock with Beta adapter that implements status query / webhook reconcile.

## Evidence
`Week4FaultInjectionIntegrationTest.pudu_timeout_unknown_does_not_blind_retry_or_succeed`
