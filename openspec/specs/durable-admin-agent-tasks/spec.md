# durable-admin-agent-tasks Specification

## Purpose

Provide administrators with durable, bounded multi-step agent tasks that can be safely inspected, paused for approval, resumed after interruption, and cancelled without bypassing the existing administrator-agent controls.

## Requirements

### Requirement: Administrators can create owned agent tasks
The system SHALL allow an authenticated administrator to create a durable task from a non-empty operational goal. A created task MUST be owned by the initiating administrator, scoped to its supplied session when present, assigned a unique task identifier, and returned with its initial lifecycle status.

#### Scenario: Create a valid task
- **WHEN** an authenticated administrator submits a non-empty task goal
- **THEN** the system creates an owned task in `QUEUED` or `RUNNING` status and returns its task identifier and status

#### Scenario: Reject an empty task goal
- **WHEN** an administrator submits an empty or whitespace-only task goal
- **THEN** the system rejects the request without creating a task

### Requirement: Tasks execute a bounded sequential plan
The system SHALL execute planned tool steps sequentially and SHALL enforce a server-configured maximum step count. It MUST persist a task-step record with the selected tool, input summary, lifecycle status, result summary, and trace reference for every attempted step. The system MUST NOT execute tool steps in parallel for a task.

#### Scenario: Execute multiple read-only steps
- **WHEN** a task plan contains permitted read-only steps within the configured limit
- **THEN** the system executes the steps in plan order, persists each completed step, and marks the task `COMPLETED` after the final successful step

#### Scenario: Stop at the step limit
- **WHEN** a planner proposes more steps than the configured task limit
- **THEN** the system executes no more than the configured limit and records that the task stopped because its execution budget was exhausted

### Requirement: Existing agent policy and write confirmation remain mandatory
The system SHALL evaluate every task step using the same input validation, permission, role, tenant, risk, preview, audit, and strong-confirmation rules as the existing administrator chat runtime. A write-risk step MUST NOT execute before its preview is created and the owning administrator supplies the required confirmation text.

#### Scenario: Pause at a write-risk step
- **WHEN** a sequential task reaches an allowed write-risk step
- **THEN** the system creates the preview and audit record, records the step as waiting for confirmation, and marks the task `WAITING_CONFIRMATION` without executing the write

#### Scenario: Resume a confirmed write-risk step
- **WHEN** the owning administrator supplies the exact confirmation text for a waiting task step
- **THEN** the system executes the approved step through the existing audit flow and continues only with later planned steps that remain allowed

#### Scenario: Reject a confirmation from another administrator
- **WHEN** an administrator other than the task owner attempts to confirm a waiting task
- **THEN** the system rejects the request and leaves the task and pending write unchanged

### Requirement: Tasks can be inspected only by their owner
The system SHALL provide task list and task-detail retrieval for authenticated administrators. Task detail MUST include the task lifecycle state, planned/executed step records, terminal reason when present, and pending confirmation metadata when applicable. An administrator MUST NOT retrieve another administrator's task through these endpoints.

#### Scenario: Inspect an owned task
- **WHEN** a task owner requests that task's detail
- **THEN** the system returns the task and its ordered step history

#### Scenario: Block cross-operator task inspection
- **WHEN** a different administrator requests a task identifier they do not own
- **THEN** the system returns a not-found or authorization-safe rejection without exposing task details

### Requirement: Cancellation prevents future task work
The system SHALL allow a task owner to cancel a `QUEUED`, `RUNNING`, or `WAITING_CONFIRMATION` task. Cancellation MUST persist the requester and reason, transition the task to `CANCELLED`, and prevent every unstarted step from executing. Cancellation MUST NOT claim to roll back an already completed external side effect.

#### Scenario: Cancel a task before a later step begins
- **WHEN** the owner cancels a task before its next planned step starts
- **THEN** the system records `CANCELLED` and does not execute that step or any later step

#### Scenario: Cancel a waiting confirmation
- **WHEN** the owner cancels a task waiting for write confirmation
- **THEN** the system marks the task cancelled and does not execute the pending write

### Requirement: Interrupted tasks have an explicit recovery state
The system SHALL persist task and step state before and after each execution transition so it can evaluate unfinished tasks after a restart. A task interrupted before a read-only step completes MAY resume from the latest completed step. A task with an ambiguous confirmed write MUST transition to `REQUIRES_REVIEW` and MUST NOT automatically replay that write.

#### Scenario: Recover an interrupted read-only task
- **WHEN** the service restarts while a task has persisted completed read-only steps and an unstarted later step
- **THEN** the task can resume from the next unstarted step without repeating completed steps

#### Scenario: Protect an ambiguous write after interruption
- **WHEN** a task is interrupted after write confirmation but before its final execution result is durably recorded
- **THEN** the system marks the task `REQUIRES_REVIEW` and does not automatically rerun the write step

### Requirement: Existing single-tool chat remains compatible
The system SHALL preserve the behavior and response contract of the existing administrator-agent chat endpoint. Creating or executing a durable task MUST NOT cause a normal single-tool chat request to execute additional tools.

#### Scenario: Use the existing chat endpoint
- **WHEN** an administrator sends a normal request to the existing chat endpoint
- **THEN** the system continues to resolve and handle only that request's single tool action using its existing safety behavior
