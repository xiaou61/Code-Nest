# admin-agent-workflow-control-plane Specification

## Purpose

Give durable administrator-agent tasks a bounded, observable workflow control plane so an
operator can understand, pause, supply required input, resume, and audit progress safely.

## Requirements

### Requirement: Durable task events provide an ordered causal timeline

The system SHALL append a bounded, immutable task event whenever a durable task changes
lifecycle state, a step enters a waiting or terminal state, recovery classifies an interrupted
task, or an operator intervenes. Events MUST identify the task, event type, timestamp, optional
step, actor category, and a safe structured summary. A state mutation that requires an event
MUST NOT commit if its event cannot be persisted.

#### Scenario: A task exposes its lifecycle timeline

- **WHEN** an owner creates a task and the Worker claims and completes a read-only step
- **THEN** the task timeline returns ordered creation, claim, step, and terminal events without
  hidden model reasoning or unbounded tool payloads

#### Scenario: Event persistence fails during a transition

- **WHEN** a task transition cannot append its required durable event
- **THEN** the corresponding task or step state transition is not committed

### Requirement: Owners can retrieve task workflow events safely

The system SHALL provide an owner-scoped, cursor-paginated task-event feed. The feed MUST be
ordered by a stable event cursor, return only events for the requested owned task, and bound the
number and size of returned event details.

#### Scenario: Owner reads later workflow events

- **WHEN** a task owner requests events after a previously returned event cursor
- **THEN** the system returns only later events in stable order and provides a cursor for the
  next page when more events exist

#### Scenario: Another administrator requests a task timeline

- **WHEN** an administrator who does not own the task requests its events
- **THEN** the system returns an authorization-safe rejection and exposes no task event data

### Requirement: Human intervention uses explicit controlled states

The system SHALL distinguish a task waiting for information from a task intentionally paused.
Only the task owner MAY pause a cancellable active task, resume a paused task, or submit bounded
structured input for a task waiting for information. These operations MUST append task events and
MUST NOT bypass existing permission, preview, audit, or strong-confirmation requirements.

#### Scenario: Planner requires missing information

- **WHEN** task planning determines that required information is missing
- **THEN** the task enters `WAITING_INPUT`, records the missing-input reason in its timeline, and
  starts no tool step

#### Scenario: Owner supplies missing information

- **WHEN** the owner submits valid bounded structured input for a `WAITING_INPUT` task
- **THEN** the input is durably associated with the task, an intervention event is recorded, and
  the task returns to `QUEUED` for a later Worker cycle

#### Scenario: Owner pauses a task before another step starts

- **WHEN** the owner pauses a `QUEUED` or `RUNNING` task before its next step begins
- **THEN** the task enters `PAUSED`, records the intervention, and no later step starts until the
  owner resumes it

### Requirement: Task routing remains bounded and safety-gated

The system SHALL route each task invocation through explicit context loading, planning, policy
guarding, execution, observation, and terminal-or-wait outcome handling. One invocation MUST NOT
execute more than one registered tool step. The system MUST stop routing when the task is
cancelled, paused, waiting for confirmation, waiting for input, failed, completed, or requires
review.

#### Scenario: A planned tool call passes through policy control

- **WHEN** a task planner proposes a registered tool call with complete input
- **THEN** the system validates the registered call and current operator access before executing
  at most one step through the existing audited execution path

#### Scenario: A write step requires confirmation

- **WHEN** the guarded step requires strong confirmation
- **THEN** the task enters `WAITING_CONFIRMATION`, records the wait outcome, and does not execute
  the write until the owner confirms through the existing audit flow

#### Scenario: A task is paused or cancelled during routing

- **WHEN** a route observes that its task is `PAUSED` or `CANCELLED`
- **THEN** the invocation ends without planning or starting another tool step

### Requirement: Existing administrator chat remains independent

The system SHALL preserve the existing single-tool administrator chat contract. Workflow events
and human-intervention controls MUST apply only to durable task APIs and MUST NOT add extra tool
steps or task records to a normal chat request.

#### Scenario: Administrator uses normal chat

- **WHEN** an administrator sends a normal request to the existing chat endpoint
- **THEN** the request retains its existing single-tool behavior and creates no durable workflow
  event
