## Why

The durable administrator-agent task runtime can safely execute sequential steps, but its
control flow is still mostly implicit in service methods and a polling Worker. Operators
cannot distinguish every pause reason, inspect an append-only causal timeline, or safely
resume a task after supplying missing information.

## What Changes

- Add a durable, append-only task-event timeline for lifecycle transitions, tool-step
  outcomes, confirmation waits, recovery decisions, and operator interventions.
- Add controlled `WAITING_INPUT` and `PAUSED` task states, with owner-only pause, resume,
  and input-submission actions that preserve the existing confirmation and audit rules.
- Expand the LangGraph4j task graph into a bounded conditional workflow with explicit
  context loading, planning, policy guarding, execution, observation, and terminal routing.
- Expose owner-scoped task-event and control-plane APIs for timeline polling and operator
  intervention without exposing model hidden reasoning or unbounded result payloads.
- Preserve MySQL task and step records as the source of truth; graph memory remains scoped to
  one invocation and is never used as a durable checkpoint.
- Preserve existing `/admin/agent/chat` behavior and the default-disabled Worker rollout.

## Capabilities

### New Capabilities

- `admin-agent-workflow-control-plane`: Provides durable workflow events, bounded conditional
  routing, explicit human-intervention states, and owner-scoped control APIs for
  administrator-agent tasks.

### Modified Capabilities

- None.

## Impact

- Backend: task state model, MyBatis persistence, transactional state transitions,
  LangGraph4j task orchestration, metrics, and administrator APIs.
- Database: an append-only task-event table and idempotent migration/schema updates.
- API: owner-only event-feed, pause, resume, and input-submission endpoints under
  `/admin/agent/tasks`.
- Tests: event ordering, owner isolation, conditional routing, human-state transitions,
  cancellation races, and recovery compatibility.
