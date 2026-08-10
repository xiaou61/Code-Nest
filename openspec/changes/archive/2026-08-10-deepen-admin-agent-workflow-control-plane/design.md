## Context

See `proposal.md` and `specs/admin-agent-workflow-control-plane/spec.md`. The current durable
task runtime persists task and step state in MySQL, executes a two-node LangGraph4j graph per
Worker cycle, and reuses the audited chat execution path. It has strong confirmation and recovery
semantics, but no durable causal timeline or distinct input/pause control states.

## Goals / Non-Goals

**Goals:**

- Keep MySQL task state authoritative while making workflow progress inspectable and replayable.
- Use LangGraph4j conditional edges for explicit bounded control flow rather than hiding routes
  in a single runtime method.
- Allow only the owner to safely pause, resume, or supply information without weakening policy or
  audit controls.
- Preserve current cancellation, confirmation, stale-recovery, and chat compatibility behavior.

**Non-Goals:**

- Graph-native durable checkpoints, graph state replay, or replacing MySQL with a checkpoint
  saver.
- Parallel task steps, subagents, autonomous remediation, generic arbitrary workflow editing,
  or persistence of model hidden reasoning.
- Server-sent events or a frontend workflow canvas in this increment; the owner event API is a
  polling contract that can support either later.

## Decisions

### 1. Add an append-only MySQL task-event table

`sys_agent_task_event` will contain an auto-increment event cursor, task identifier, event type,
optional step order, actor type and identifier, from/to statuses, bounded JSON detail, and
timestamp. Reads are ordered by event cursor, which is stable and suitable for pagination.

Each required event insertion occurs in the same transaction as the relevant task or step
transition. This gives event and state atomicity without introducing a second durable state store
or distributed transaction. Event payloads contain safe state summaries and bounded structured
facts, never raw model chain-of-thought or unbounded tool results.

Alternative considered: store a mutable JSON timeline on `sys_agent_task`. Rejected because it
would lose append-only causality, produce large row rewrites, and make cursor reads unreliable.

### 2. Persist a bounded operator-supplied workflow context

`sys_agent_task` gains a bounded `workflow_context_json` field. When the planner reports missing
fields, the task enters `WAITING_INPUT`. Owner input is schema-neutral at persistence time but
strictly bounded in keys, depth, and serialized size; it is passed to the planner as supplemental
context only. It is never sent directly to a tool or used to bypass the existing tool schema,
policy, preview, audit, or confirmation path.

Alternative considered: append operator input to the natural-language goal. Rejected because it
would erase structured provenance and make later validation/audit ambiguous.

### 3. Add explicit `WAITING_INPUT` and `PAUSED` lifecycle states

`WAITING_INPUT` is entered only when planning identifies missing information. `PAUSED` is an
owner-requested cooperative stop for a queued or running task. Resume moves only `PAUSED` tasks to
`QUEUED`; submitting input moves only `WAITING_INPUT` tasks to `QUEUED`. A waiting confirmation
remains governed by its existing audit continuation/cancel behavior and is not repurposed as a
general pause.

Conditional MyBatis transitions and expected lease checks remain the concurrency gate. A Worker
that has already started a tool cannot claim rollback; it observes the failed transition and starts
no later step.

### 4. Use a bounded conditional LangGraph4j cycle

The task graph becomes:

`load_context -> plan_next_step -> policy_guard -> execute_or_finish -> observe -> route`.

Conditional edges route only to allowed nodes or `END`: inactive/pause/cancel outcomes stop;
missing input routes to a persistence-only wait node; planner completion routes to task completion;
an executable call routes through policy guarding and the existing registered-call orchestrator.
The Worker still invokes one compiled graph cycle at a time and owns the maximum-step loop. This
preserves durable checkpoints at state boundaries and avoids an in-memory graph loop surviving a
restart.

The implementation uses the supported `StateGraph.addConditionalEdges` and `AsyncEdgeAction`
pattern from LangGraph4j. No `MemorySaver` is configured because it would duplicate, rather than
strengthen, MySQL persistence.

### 5. Expose a narrow owner-only control-plane HTTP surface

The durable task controller receives event list, pause, resume, and input endpoints. Inputs use
validated DTOs with strict request bounds. Every endpoint obtains the current operator and scopes
the lookup/mutation to the task owner. Responses return bounded task/event DTOs only.

Alternative considered: expose generic state-transition endpoints. Rejected because callers could
manufacture unsafe lifecycle transitions or bypass confirmation semantics.

## Risks / Trade-offs

- [A high event volume grows the task database] -> Events use bounded details and cursor scans;
  retention/archival can be operated later without changing correctness.
- [Operator input contains unsafe tool parameters] -> Input remains planner context; every tool
  call is independently filtered and policy-checked.
- [Conditional routing accidentally starts a later step after a pause] -> The graph reloads state
  and every start uses existing CAS/lease checks; tests cover pause/cancel races.
- [Event state and task state diverge] -> Required events insert in the same transaction; an event
  write failure rolls back the transition.
- [Worker restart loses graph memory] -> The graph holds only one-cycle data and reloads from
  MySQL on every invocation.

## Migration Plan

1. Apply the idempotent v2.5.10 migration that adds workflow context and the event table/indexes.
2. Deploy with the Worker disabled, then verify owner event reads and task state transitions.
3. Enable one Worker instance and observe event volume, queue depth, route outcomes, and recovery
   metrics before wider enablement.
4. Roll back by disabling the Worker and new control actions. Keep task and event records for
   audit; do not delete historic state.
