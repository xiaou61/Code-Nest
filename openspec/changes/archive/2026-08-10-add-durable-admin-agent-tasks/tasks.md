## 1. Persistence Contract

- [x] 1.1 Add idempotent MySQL migration and consolidated schema entries for administrator task and task-step records with ownership, status, ordering, audit, lease, cancellation, and recovery indexes.
- [x] 1.2 Add task and step domain models, MyBatis mappers, XML mappings, and focused mapper/service persistence tests for conditional state transitions.
- [x] 1.3 Add task runtime properties for enablement, batch size, lease duration, poll delay, maximum steps, and bounded persisted result sizes.

## 2. Shared Agent Runtime Integration

- [x] 2.1 Extract reusable current-operator resolution from the chat controller and cover role/permission refresh behavior.
- [x] 2.2 Add a server-validated registered-tool execution entry point to the chat orchestrator while preserving the existing chat endpoint behavior.
- [x] 2.3 Add task DTOs and owner-scoped task response conversion with bounded step result data.

## 3. Planning and Graph Execution

- [x] 3.1 Define the structured iterative task-planning prompt and output contract, including schema filtering, missing input handling, confidence, and safe fallback behavior.
- [x] 3.2 Implement the LangGraph4j single-cycle task graph and planner/step fingerprint protections.
- [x] 3.3 Write focused graph tests for ordered read-only execution, planner completion, step-limit exhaustion, and duplicate-call rejection.

## 4. Durable Task Lifecycle

- [x] 4.1 Implement task creation, claiming, heartbeats, ordered step persistence, terminal outcomes, and owner-only list/detail access.
- [x] 4.2 Implement write-preview pause, owner confirmation, audited resume, and confirmation mismatch handling without duplicating the chat safety path.
- [x] 4.3 Implement cooperative cancellation and pending preview cancellation, ensuring no later step begins after a successful cancellation.
- [x] 4.4 Implement stale-task recovery, terminal audit reconciliation, read-only retry, and `REQUIRES_REVIEW` classification for ambiguous writes.
- [x] 4.5 Add the scheduled worker and task metrics with a disabled-by-default production rollout configuration.

## 5. HTTP Surface and Verification

- [x] 5.1 Add authenticated create, list, detail, confirm, and cancel task endpoints with controller tests for validation and ownership.
- [x] 5.2 Add lifecycle tests covering multi-step success, write confirmation pause/resume, cross-operator denial, cancellation, restart recovery, and unchanged single-chat behavior.
- [x] 5.3 Update deployment configuration and release documentation with migration, enablement, monitoring, and rollback guidance.
- [x] 5.4 Run OpenSpec strict validation and targeted system-module tests/build; record any unverified production-only behavior.

## Verification Notes

- `openspec validate add-durable-admin-agent-tasks --strict` passed on 2026-08-05.
- `python scripts/release_manifest.py validate` passed for release and schema version `v2.5.9`.
- `mvn -pl xiaou-system -am test` passed across the 18-module reactor; `xiaou-system` reported 290 tests, 0 failures, 0 errors, and 3 skipped tests. The subsequently added Spring constructor-wiring test also passed in isolation.
- MyBatis XML contract tests verify task claim, task transition, step transition, lease ownership, and owner-scoped cancellation predicates.
- Production-only behavior remains unverified: the migration has not been applied to a real production MySQL instance, multi-instance lease contention has not been exercised against production MySQL, and the disabled-by-default Worker has not been enabled under live load.
