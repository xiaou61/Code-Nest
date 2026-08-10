## Why

The administrator agent currently resolves and executes at most one registered tool per request. It has strong policy, preview, confirmation, and audit controls, but it cannot durably carry a multi-step operational goal across process restarts, confirmation pauses, or operator cancellation.

## What Changes

- Introduce durable administrator-agent tasks with persisted task and step state, operator/session ownership, bounded execution, and traceable outcomes.
- Add a sequential plan-execute-observe loop that can run multiple registered tools toward one goal while enforcing a hard step limit.
- Pause tasks before every write-risk step and reuse the existing preview, strong-confirmation, permission, tenant, audit, and idempotency controls.
- Add resumable task execution after confirmation or recoverable interruption, without automatically replaying an ambiguous write operation.
- Add server-side cancellation and task status/detail APIs; cancellation stops future steps and records the final reason.
- Preserve the existing `/admin/agent/chat` single-tool contract for compatibility.
- Exclude multi-agent delegation, MCP/A2A, file/RAG, workflow canvas, autonomous remediation, and parallel tool execution from this change.

## Capabilities

### New Capabilities

- `durable-admin-agent-tasks`: Persist, execute, pause, resume, cancel, and inspect bounded multi-step administrator-agent tasks without weakening existing safety controls.

### Modified Capabilities

None.

## Impact

- Backend: administrator-agent orchestration, task planning, policy/audit integration, task and step persistence, and metrics.
- API: new authenticated administrator task endpoints and DTOs; the existing chat endpoint remains compatible.
- Database: new task and task-step tables plus indexes for ownership, status, and recovery scans.
- Tests: task lifecycle, bounded execution, write confirmation pause/resume, cancellation, ownership, and crash-recovery behavior.
- Operations: database migration and additional task-runtime health/metrics signals.
