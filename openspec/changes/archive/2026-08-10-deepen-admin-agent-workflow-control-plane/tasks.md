## 1. Specification and Tests

- [x] 1.1 Add focused tests for durable event ordering, owner isolation, and bounded cursor pagination.
- [x] 1.2 Add focused tests for `WAITING_INPUT`, `PAUSED`, resume, cancellation races, and event/state atomicity.
- [x] 1.3 Add focused graph tests for explicit conditional routing and the one-tool-step cycle bound.

## 2. Durable Control Plane

- [x] 2.1 Add the v2.5.10 idempotent database migration and consolidated schema updates for workflow context and task events.
- [x] 2.2 Add task-event domain, mapper, response DTOs, and bounded cursor reads.
- [x] 2.3 Extend task lifecycle transitions so required state changes and events commit atomically.
- [x] 2.4 Add bounded, validated workflow context persistence for owner-supplied planner input.

## 3. APIs and Runtime

- [x] 3.1 Add owner-only event, pause, resume, and input-submission endpoints with audit-safe request bounds.
- [x] 3.2 Extend planning context and planner prompt inputs without allowing operator input to bypass tool validation.
- [x] 3.3 Replace the two-node graph with an explicit, bounded conditional workflow.
- [x] 3.4 Preserve worker-disabled rollout defaults, cancellation/confirmation behavior, and normal chat independence.

## 4. Verification

- [x] 4.1 Validate the OpenSpec change strictly.
- [x] 4.2 Run focused task/control-plane tests and the `xiaou-system` Maven test reactor.
- [x] 4.3 Run SQL/XML and diff checks; record any non-blocking verification limits.

Verification limit: the idempotent SQL migration and consolidated schema were checked statically;
no live MySQL or production migration was applied in this change.
