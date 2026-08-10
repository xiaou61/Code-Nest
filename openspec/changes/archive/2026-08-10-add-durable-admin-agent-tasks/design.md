## Context

See `proposal.md` for motivation and `specs/durable-admin-agent-tasks/spec.md` for the behavior contract. The existing chat runtime owns tool registration, planner validation, policy checks, write previews, audit state, strong confirmation, metrics, and session recording. It is synchronous and handles one resolved tool call per request. The repository already uses MyBatis state transitions for durable SRE queues and LangGraph4j for bounded AI graphs.

The main constraint is that a durable task must not fork the safety implementation. In particular, task execution cannot call a tool directly after an LLM plan, and a confirmed write whose final result was not recorded is an ambiguous side effect that must never be replayed automatically.

## Goals / Non-Goals

**Goals:**

- Make MySQL the source of truth for task and step lifecycle state.
- Run a bounded sequential `plan -> execute -> observe` graph while persisting every transition.
- Reuse the existing registered-call execution and audited confirmation paths.
- Support safe multi-instance claiming, cooperative cancellation, and deterministic recovery classification.
- Re-resolve the administrator's current roles and permissions before execution or confirmation.

**Non-Goals:**

- Exactly-once delivery for arbitrary third-party side effects; ambiguous writes stop for review.
- Parallel steps, subagents, arbitrary code execution, or autonomous production remediation.
- Replacing the existing chat API or browser workspace in this change.
- Treating planner prose or hidden reasoning as persisted business state.

## Decisions

### 1. Persist tasks and steps in separate MySQL tables

`sys_agent_task` stores ownership, goal, session, lifecycle status, step budget, lease timestamps, pending audit, terminal reason, cancellation metadata, and timestamps. `sys_agent_task_step` stores ordered tool calls, filtered input JSON, risk metadata, audit/trace references, status, result summary, bounded result JSON, errors, and timestamps.

Task statuses are `QUEUED`, `RUNNING`, `WAITING_CONFIRMATION`, `COMPLETED`, `CANCELLED`, `FAILED`, and `REQUIRES_REVIEW`. Step statuses are `PENDING`, `RUNNING`, `WAITING_CONFIRMATION`, `CONFIRMING`, `COMPLETED`, `FAILED`, `CANCELLED`, and `REQUIRES_REVIEW`.

MySQL conditional updates provide compare-and-set transitions. This follows the existing SRE queue pattern and supports multiple application instances. Redis and in-memory session repositories are not suitable as the task source of truth because they do not provide the required durable transition and recovery queries.

### 2. Use LangGraph4j for one persisted execution cycle

An administrator-task graph contains `plan_next_step` and `execute_or_finish` nodes. One graph invocation advances at most one tool step. A scheduled worker claims a queued task and repeatedly invokes the graph until the task completes, pauses, fails, is cancelled, or exhausts its configured step limit.

The graph state carries only the current cycle's task identifier and planner decision. Task history is loaded from MySQL at the beginning of each cycle. This makes database checkpoints authoritative and avoids relying on an in-memory graph checkpoint after a restart.

Alternative considered: one large in-memory graph run. Rejected because a process restart would lose state between tool calls and write confirmation cannot remain blocked inside one request.

### 3. Add an iterative structured task planner

The planner receives the original goal, bounded summaries of persisted completed steps, remaining step budget, and the current registered tool catalog. It returns one structured decision: execute one registered tool or complete the task. Candidate input is filtered to the selected tool schema, required fields are checked, confidence is bounded, and duplicate completed tool/input fingerprints are rejected.

If AI planning is unavailable, the first cycle may reuse the existing deterministic single-tool resolver. After a fallback step completes, the task completes instead of guessing additional work. This preserves availability without fabricating a multi-step plan.

Alternative considered: split the user's text on conjunctions. Rejected because punctuation is not a reliable operational plan and could change the meaning of write requests.

### 4. Reuse the existing orchestrator for every resolved call

The chat orchestrator gains a registered-call entry point that accepts a server-validated `AgentToolCall` and then uses the existing policy, preview, audit, confirmation, execution, metrics, trace, and session-recording behavior. Normal chat continues through its current planner entry point.

The task graph never invokes `AgentTool.execute` directly. A read-only `answered` response completes the step. A `confirm_required` response persists its audit metadata, changes the step to `WAITING_CONFIRMATION`, and changes the task to `WAITING_CONFIRMATION`.

Alternative considered: duplicate policy and audit handling inside the task service. Rejected because the two paths would drift and could create a task-only authorization bypass.

### 5. Confirmation is a task state transition before an audit transition

The owner confirmation endpoint first atomically moves the task and step from `WAITING_CONFIRMATION` to `RUNNING` and `CONFIRMING`. It then calls the existing audited continuation path with the persisted audit identifier and submitted confirmation text.

- Exact confirmation and successful execution complete the step and requeue the task for its next cycle.
- A mismatch restores the task and step to their waiting states while the audit remains `PREVIEW`.
- A terminal audit response is reconciled into the step without replaying the tool.
- A persisted `CONFIRMED` audit with no terminal result moves the task and step to `REQUIRES_REVIEW`.

This ordering makes cancellation and confirmation race through one task-state compare-and-set gate.

### 6. Recovery distinguishes read-only work from ambiguous writes

The worker periodically scans stale `RUNNING` tasks using a configurable lease. A stale read-only `RUNNING` step is reset to `PENDING` and its task to `QUEUED`; completed steps are never repeated. A stale `CONFIRMING` step is reconciled from its audit:

- `PREVIEW`: restore `WAITING_CONFIRMATION`.
- `EXECUTED` or `FAILED`: persist the terminal audit result and continue or fail.
- `CONFIRMED`: mark `REQUIRES_REVIEW`.
- Missing or invalid audit: mark `REQUIRES_REVIEW`.

Any stale non-read-only step without a provably terminal audit is also marked `REQUIRES_REVIEW`.

### 7. Cancellation is cooperative and terminal

The owner can atomically cancel `QUEUED`, `RUNNING`, or `WAITING_CONFIRMATION` tasks. The worker checks task state before planning, before tool execution, and after every completed step. A pending `PREVIEW` audit is cancelled after the task transition succeeds. An already running tool call cannot be forcibly rolled back; its result may finish, but no later task step can start.

### 8. Resolve current operator access at execution time

Operator construction is moved into a shared resolver used by chat, task controllers, and the worker. Tasks persist the owner identifier and display name, but do not persist permission snapshots. Each execution and confirmation uses current roles and permissions so revoked access takes effect before the next step.

### 9. Expose owner-scoped task APIs and runtime metrics

The backend adds create, list, detail, confirm, and cancel endpoints under `/admin/agent/tasks`. Every lookup includes the current operator identifier. Responses expose bounded step results and pending confirmation metadata, not planner hidden reasoning.

Metrics cover queue depth, task duration, lifecycle outcomes, step outcomes, confirmation waits, cancellations, stale recoveries, and review-required transitions. Tool metrics continue to be emitted by the existing orchestrator.

## Risks / Trade-offs

- [A read-only tool is incorrectly classified but has side effects] -> Only tools declared `READONLY` are retryable; tool definition tests and review remain required.
- [Long tool execution exceeds its lease and is reclaimed] -> Use a lease longer than configured AI/tool timeouts, heartbeat at graph boundaries, and never recover a non-read-only running step automatically.
- [Planner loops on the same call] -> Persist and reject normalized tool/input fingerprints and enforce the hard step limit.
- [Task tables are not migrated before the worker starts] -> Keep the worker disabled by default and enable it only after applying the migration.
- [Permissions change while a task is queued] -> Re-resolve roles and permissions for every execution cycle.
- [Cancellation arrives while a read-only call is already running] -> Treat cancellation as preventing future work; do not claim that in-flight work was rolled back.
- [Task result JSON grows without bound] -> Persist bounded summaries and capped artifact JSON rather than full model or transport payloads.

## Migration Plan

1. Apply the idempotent SQL migration for task and step tables and update the consolidated MySQL schema.
2. Deploy the application with the task worker disabled; existing chat behavior remains active.
3. Verify task table access, planner configuration, and task runtime readiness.
4. Enable the task worker on one instance, observe queue/recovery metrics, then enable it across remaining instances.
5. Roll back by disabling the worker and task endpoints. Keep task records for audit; no existing chat schema needs to be reverted.

## Open Questions

- Retention and archival duration for completed task/step records can be selected operationally later; it does not affect lifecycle behavior.
