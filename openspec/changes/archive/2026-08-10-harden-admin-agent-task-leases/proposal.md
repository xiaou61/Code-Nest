## Why

Durable administrator-agent tasks can currently be recovered from a stale-task scan even when
their lease was renewed after the scan, and long planner or tool calls have no independent lease
heartbeat. These races can create unnecessary recovery, delayed progress, or duplicate read-only
execution under production latency.

## What Changes

- Give every task claim a unique lease token, including repeated claims by the same Worker process.
- Keep an active task lease renewed while one bounded graph cycle is blocked in planning or tool
  execution, and stop accepting progress after renewal proves the lease is lost.
- Fence stale-task recovery with the same heartbeat cutoff used to discover the task so a recently
  renewed lease cannot be recovered from an old scan result.
- Isolate recovery per task so a conflict or failure for one stale task cannot roll back successful
  recovery of other tasks in the batch.
- Add bounded-cardinality lease renewal and recovery-conflict metrics without emitting a durable
  event for every heartbeat.
- Preserve existing task APIs, confirmation semantics, MySQL source-of-truth behavior, bounded
  one-tool graph cycles, and default-disabled Worker rollout.

## Capabilities

### New Capabilities

- `admin-agent-task-lease-reliability`: Defines renewable, uniquely fenced task leases and
  conflict-isolated stale recovery for durable administrator-agent tasks.

### Modified Capabilities

- None.

## Impact

- Backend: Worker claim ownership, lease heartbeat coordination, task state/recovery services,
  MyBatis recovery compare-and-set statements, and task runtime metrics.
- Database: no schema change; existing task lease and heartbeat columns remain authoritative.
- API: no HTTP contract change.
- Tests: long-cycle renewal, unique claim tokens, stale-scan/heartbeat races, recovery transaction
  isolation, and bounded-cardinality metrics.
