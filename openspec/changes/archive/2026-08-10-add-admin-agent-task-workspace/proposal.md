## Why

The durable administrator-agent task backend now supports multi-step execution, event history,
pause/resume, missing-input recovery, confirmation, and cancellation, but v2.5.9 exposes none of
that lifecycle in the administrator workspace. Operators must call the HTTP API manually and
cannot safely supervise long-running tasks from the product UI.

## What Changes

- Add a durable-task mode to the existing administrator-agent workspace without replacing the
  synchronous chat workflow.
- Let an operator create a bounded background task from a natural-language goal and immediately
  select the returned task.
- Add a status-filtered recent-task list with explicit loading, empty, error, and manual-refresh
  states.
- Add a task detail surface for lifecycle status, progress, terminal reason, and persisted step
  outcomes.
- Add an incremental, cursor-based event timeline that polls only while the selected task is
  non-terminal and never duplicates previously received events.
- Add state-aware pause, resume, cancel, structured-input, and strong-confirmation controls that
  expose only transitions allowed by the backend state machine.
- Keep task requests owner-scoped through the existing authenticated backend endpoints and keep
  all destructive work behind the existing preview, audit, and exact-confirmation contract.

## Capabilities

### New Capabilities

- `admin-agent-task-workspace`: Allows administrators to create, monitor, and safely control
  durable agent tasks through the existing full-screen workspace.

### Modified Capabilities

None.

## Impact

- Frontend: administrator-agent API client, workspace mode navigation, task list/detail/timeline
  components, state helpers, polling lifecycle, and responsive styling.
- API: consumes the existing `/admin/agent/tasks` owner-scoped endpoints; no backend contract or
  database migration is introduced.
- Safety: state-aware controls mirror backend lifecycle rules; strong confirmation remains exact
  and cancellation remains cooperative.
- Tests: frontend task state helpers, API/controller route parity, workspace integration contracts,
  and production build verification.
