## Context

See `proposal.md` and `specs/admin-agent-task-workspace/spec.md`. The v2.5.9 administrator-agent
workspace is a full-screen Vue component centered on synchronous chat sessions stored in browser
local storage. The backend now separately exposes owner-scoped durable-task endpoints for creation,
bounded listing, detail, cursor events, pause, resume, input, confirmation, and cancellation.

The main constraints are preserving the chat path, reflecting the backend state machine instead of
reimplementing it, stopping background polling when it is not useful, and keeping destructive tool
execution exclusively behind backend policy, preview, audit, and confirmation controls.

## Goals / Non-Goals

**Goals:**

- Add a coherent durable-task mode within the existing workspace shell.
- Keep all task API routes and state-derived UI behavior centralized and independently testable.
- Use bounded, incremental reads and one polling owner for the selected task.
- Make every operator intervention explicit, state-aware, and followed by authoritative refresh.
- Keep the task surface usable on desktop and compact viewports.

**Non-Goals:**

- Moving chat history or task state into a new frontend store.
- Adding server-sent events, WebSockets, graph editing, parallel tasks, or optimistic state
  transitions that are not confirmed by the backend.
- Exposing raw tool inputs, model hidden reasoning, or browser-side policy decisions.
- Changing backend APIs, database schema, Worker enablement, or release version metadata.

## Decisions

### 1. Add a task mode inside the existing full-screen workspace

The workspace header receives a two-option mode control. Chat keeps its existing three-column
layout and component state. Task mode renders a dedicated task component in the same content area,
so the global entry point, dialog behavior, responsive shell, and administrator context remain
unchanged.

Alternative considered: add a standalone routed page. Rejected because task supervision is part of
the agent workflow, would duplicate the workspace shell, and would make chat-to-task context harder
to preserve.

### 2. Keep task transport in a dedicated API module

A separate task API module will map each existing controller operation one-to-one. This prevents
the synchronous chat client from becoming a mixed orchestration surface and makes route parity easy
to validate with source-level contract tests.

Alternative considered: add all methods to the chat API object. Rejected because chat and durable
tasks have different lifecycles and only share authentication and the workspace shell.

### 3. Put state-machine presentation rules in pure helpers

Terminal-state detection, permitted actions, task normalization, progress bounding, and cursor
event merging will live in a framework-independent helper module. Components consume the helper
output but always treat the backend response as authoritative after a mutation.

Alternative considered: scatter status comparisons through template expressions. Rejected because
that makes invalid controls and polling leaks more likely as states evolve.

### 4. Use one selected-task polling loop with cursor accumulation

The task component owns at most one timer. Each refresh fetches current detail and only events after
the stored cursor. Selecting another task resets event state and generation-fences late responses;
closing the workspace, switching mode, reaching a terminal state, or unmounting clears the timer.
Polling failures retain the last good data and surface a retry action.

Alternative considered: repeatedly fetch the full event history. Rejected because event logs are
append-only and already expose stable cursor pagination.

### 5. Use backend-confirmed mutations and exact intervention forms

Pause, resume, and cancel use the narrow lifecycle endpoints and update the view from the returned
task. Missing input accepts only a parsed, non-array JSON object. Confirmation remains disabled
until the typed text exactly matches `confirmation.requiredText`. After every accepted mutation,
the component performs an event refresh rather than manufacturing timeline entries.

Alternative considered: optimistic local transitions. Rejected because ownership, leases, and
concurrent Worker progress can make an apparently valid browser transition lose its backend CAS.

### 6. Use dense operational layout rather than nested cards

Task mode uses a bounded left rail for filters and recent tasks plus an unframed detail region with
status header, action bar, step table, and event timeline. Compact viewports stack the rail above
detail and keep controls wrapping within their container. Existing Element Plus controls and icons
provide keyboard and accessible-name behavior.

## Risks / Trade-offs

- [Polling adds avoidable load] -> Poll only the selected non-terminal task while active, use cursor
  reads, and keep the interval bounded.
- [A late response overwrites a newly selected task] -> Fence async detail/event results by the
  selected task identifier and refresh generation.
- [Frontend state rules drift from the backend] -> Keep rules minimal, cover every known state in
  pure helper tests, and always accept backend mutation results as authoritative.
- [Structured input is syntactically valid but semantically wrong] -> Submit it only as bounded
  planner context; backend schema filtering and policy remain mandatory before any tool call.
- [The combined workspace becomes visually dense] -> Preserve explicit modes and responsive layout
  so chat and task supervision are never shown simultaneously on compact viewports.

## Migration Plan

1. Deploy the frontend after the existing durable-task backend and migrations are available.
2. Keep the task Worker default-disabled behavior unchanged; the UI will still show queued tasks.
3. Verify API route parity, task contracts, responsive build, and polling stop conditions before
   enabling the Worker in production.
4. Roll back by deploying the previous frontend; persisted tasks and backend APIs remain intact.
