## Context

See `proposal.md` and `specs/admin-agent-task-lease-reliability/spec.md`. MySQL task rows are the
durable ownership authority. A Worker currently uses one process-level owner value for every claim,
heartbeats only at graph state boundaries, and discovers stale rows before recovering them through
one batch transaction. The existing task and step CAS writes correctly reject most stale results,
but the recovery transition does not re-check the discovery cutoff atomically.

## Goals / Non-Goals

**Goals:**

- Make lease ownership resistant to same-process reclaim and stale-scan races without changing the
  database schema.
- Keep leases alive across slow LLM, tool, and confirmation calls while retaining MySQL as the sole
  source of ownership truth.
- Give every stale task its own transaction and observable bounded outcome.
- Keep callers behind small lease-management and recovery-coordination interfaces.

**Non-Goals:**

- Exactly-once execution of arbitrary external systems; existing audited write and idempotency rules
  remain the authority for side effects.
- Persistent LangGraph checkpoints, parallel steps, subagents, automatic remediation, or a new queue.
- Heartbeat events in the durable timeline or task/lease identifiers in metric tags.
- HTTP, frontend, or database-column changes.

## Decisions

### 1. Use a unique claim token rather than a process-stable owner as the fencing identity

The Worker keeps a stable instance identifier for diagnostics but creates a fresh bounded token for
each claim. Existing `lease_owner` CAS predicates then provide generation fencing without adding a
`lease_version` column. Confirmation already creates a fresh owner token and continues to do so.

Alternative considered: add a numeric lease generation column. It provides equivalent fencing but
requires a migration and adds another value to every transition. A unique token has the same
collision-resistant ownership semantics within the existing schema.

### 2. Put renewal lifecycle behind one lease manager

A lease manager creates claim tokens and runs a supplied bounded operation while a daemon scheduled
executor periodically calls the existing heartbeat CAS. The interval is derived from the normalized
lease duration and remains comfortably below expiry. The scheduled renewal is cancelled in `finally`
and executor shutdown follows the Spring bean lifecycle.

The Worker guards its claimed graph-cycle loop. The confirmation runtime guards only the in-flight
confirmed execution, because that path is not owned by the Worker. State transitions remain the
final ownership check; a renewal failure never grants permission to commit a result.

Alternative considered: heartbeat only before and after each graph node. Rejected because the
planner and registered tool call are opaque blocking operations and can individually exceed the
lease duration.

### 3. Re-check staleness in the recovery task transition SQL

Each scan captures one `staleBefore` timestamp. Recovery reads and classifies the step using existing
safety rules, then its final task transition includes status, owner, and
`COALESCE(heartbeat_at, claimed_at, updated_time) <= staleBefore` predicates. If that CAS fails, the
transaction is marked rollback-only so any preceding step update and event are also discarded.

Alternative considered: re-read the heartbeat immediately before the existing transition. Rejected
because renewal can still race between the read and write.

### 4. Coordinate the batch outside per-task `REQUIRES_NEW` transactions

A recovery coordinator obtains the bounded stale ID list and invokes the proxied state module once
per task. Each task recovery uses `REQUIRES_NEW`; the coordinator converts `RECOVERED`,
`REQUIRES_REVIEW`, `CONFLICT`, and thrown failures into aggregate counts and continues scanning.
This also prevents self-invocation from silently defeating Spring transaction propagation.

Alternative considered: use one transaction for the complete scan. Rejected because one rollback-only
CAS conflict can undo unrelated successful recoveries and remote audit reconciliation lengthens the
batch transaction.

### 5. Extend existing low-cardinality task metrics

Lease renewal uses a fixed outcome set (`success`, `lost`, `error`). Stale recovery uses
`recovered`, `requires_review`, `conflict`, and `error`. Heartbeats do not create timeline events,
because their volume would obscure causal task events and increase storage without adding state
information.

## Risks / Trade-offs

- [The scheduler cannot renew during a process pause or database outage] -> Existing lease expiry,
  stale-result CAS checks, and safe recovery rules remain the fallback.
- [A heartbeat races with a terminal transition and reports ownership lost] -> The terminal CAS is
  authoritative; the renewal outcome is operational telemetry and cannot overwrite state.
- [A read-only external call can still finish after genuine lease loss] -> Its stale result cannot
  commit; only operations declared read-only are eligible for automatic replay.
- [Per-task transactions add transaction setup overhead] -> Recovery batches are already bounded and
  correctness isolation outweighs the small stale-path cost.

## Migration Plan

1. Deploy the code with the durable task Worker still disabled by default; no SQL migration is needed.
2. Run task lifecycle and recovery tests, then enable one Worker instance in a controlled environment.
3. Observe renewal and recovery outcome counters for unexpected `lost`, `conflict`, or `error` rates.
4. Roll back by disabling the Worker and deploying the previous code; existing task rows remain
   compatible because the persistence schema is unchanged.
