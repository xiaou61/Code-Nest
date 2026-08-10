# admin-agent-task-lease-reliability Specification

## Purpose

Keep durable administrator-agent task ownership correct during slow execution and concurrent
recovery so only the current lease holder can advance a task.

## Requirements

### Requirement: Every task claim has unique fenced ownership

The system SHALL assign a fresh opaque lease token to every successful task claim. A later claim,
including one by the same Worker process, MUST use a different token, and task progress MUST be
accepted only while the supplied token is the current lease owner.

#### Scenario: The same Worker process reclaims a task

- **WHEN** a task is released or recovered and the same Worker process successfully claims it again
- **THEN** the new claim uses a different lease token and results from the earlier claim cannot
  advance the task

#### Scenario: An old lease holder submits a late result

- **WHEN** a previous lease holder submits a step or task transition after another claim owns the task
- **THEN** the transition is rejected and no step, task state, or durable event from that late result
  is committed

### Requirement: Active long-running work renews its lease

The system SHALL renew the current task lease while a bounded graph cycle or confirmed tool
execution remains in flight. Renewal MUST occur before the configured lease expiry under normal
scheduler operation, MUST stop when the guarded work returns, and MUST NOT append a durable task
event for each heartbeat.

#### Scenario: A graph cycle lasts longer than one renewal interval

- **WHEN** planning or tool execution remains active beyond the lease renewal interval
- **THEN** the current lease heartbeat advances and stale recovery cannot take ownership solely
  because the cycle is slow

#### Scenario: Heartbeat proves ownership was lost

- **WHEN** a heartbeat compare-and-set reports that the task is no longer owned by the guarded lease
- **THEN** the old lease cannot commit later progress or start a subsequent tool step

### Requirement: Stale recovery is atomically fenced by the scan cutoff

The system SHALL capture one heartbeat cutoff for a stale-task scan and SHALL commit each recovery
transition only if the task is still running under the observed lease and its stored heartbeat is
not newer than that cutoff. A task renewed after discovery MUST remain unchanged by that scan.

#### Scenario: Heartbeat wins after stale-task discovery

- **WHEN** a scan discovers a stale task and its current lease is renewed before recovery commits
- **THEN** recovery loses its compare-and-set, leaves the task and step unchanged, and records a
  bounded recovery conflict outcome

#### Scenario: Lease remains stale through recovery

- **WHEN** a discovered task remains under the same expired lease through the recovery transaction
- **THEN** the existing read-only replay, confirmation reconciliation, or manual-review rule is
  applied exactly once

### Requirement: Recovery failures are isolated per task

The system SHALL recover each stale task in an independent transaction. A compare-and-set conflict
or runtime failure for one task MUST NOT roll back a successful recovery for another task in the
same bounded scan.

#### Scenario: One task conflicts while another can be recovered

- **WHEN** a stale scan contains two tasks and the first task is renewed concurrently
- **THEN** the first task is reported as a conflict while the second task recovery commits normally

#### Scenario: One task recovery throws an exception

- **WHEN** recovery of one scanned task fails because its persistence or audit dependency throws
- **THEN** the failure is recorded with bounded cardinality and remaining scanned tasks are still
  attempted

### Requirement: Lease reliability is observable with bounded cardinality

The system SHALL expose counters for lease-renewal outcomes and stale-recovery outcomes using only
a fixed outcome label set. Metrics MUST NOT use task identifiers, lease tokens, operator identifiers,
tool names, exception messages, or other unbounded values as labels.

#### Scenario: Operators inspect lease metrics

- **WHEN** lease renewals and stale recoveries succeed, conflict, lose ownership, or fail
- **THEN** counters distinguish those fixed outcomes without exposing per-task identifiers

### Requirement: Existing workflow and chat contracts remain compatible

The system SHALL preserve the existing durable-task HTTP contracts, confirmation and audit rules,
one-tool-per-cycle bound, MySQL source of truth, default-disabled Worker setting, and independent
single-tool administrator chat behavior.

#### Scenario: A normal task completes without lease contention

- **WHEN** a queued durable task is claimed and completes under normal latency
- **THEN** its externally visible task, step, event, and confirmation behavior remains unchanged
