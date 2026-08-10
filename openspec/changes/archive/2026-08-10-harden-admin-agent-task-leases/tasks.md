## 1. Lease Contract Tests

- [x] 1.1 Add focused tests that every Worker claim uses a unique bounded lease token and that guarded work renews an active lease until completion.
- [x] 1.2 Add state/mapper tests proving a heartbeat newer than the scan cutoff defeats stale recovery and rolls back any preceding step transition.
- [x] 1.3 Add coordinator tests proving recovery conflicts and exceptions are counted independently while later stale tasks are still attempted.

## 2. Renewable Lease Ownership

- [x] 2.1 Add a lifecycle-managed lease module for per-claim token creation and scheduled heartbeat renewal around bounded operations.
- [x] 2.2 Guard the Worker graph-cycle loop and confirmed execution path with lease renewal while preserving final state CAS checks.
- [x] 2.3 Add fixed-cardinality lease renewal metrics and verify heartbeat activity never appends durable timeline events.

## 3. Fenced Recovery

- [x] 3.1 Add a stale-cutoff-aware MyBatis transition that atomically checks task status, lease owner, and persisted heartbeat age.
- [x] 3.2 Route every stale recovery outcome through the cutoff-aware task transition and roll back preceding step changes on conflict.
- [x] 3.3 Move batch scanning into a recovery coordinator and execute each task recovery through an independent `REQUIRES_NEW` transaction.
- [x] 3.4 Extend recovery results and metrics with fixed `conflict` and `error` outcomes.

## 4. Verification

- [x] 4.1 Strictly validate the OpenSpec change and run focused lease, recovery, worker, runtime, mapper, and metrics tests.
- [x] 4.2 Run the complete `xiaou-system` Maven reactor tests and compile checks.
- [x] 4.3 Run XML/diff checks and refresh the CodeGraph index after adding the new cross-file modules.
