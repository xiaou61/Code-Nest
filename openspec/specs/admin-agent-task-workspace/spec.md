# admin-agent-task-workspace Specification

## Purpose

Provide administrators with a product-native surface for creating, monitoring, and safely
controlling durable multi-step agent tasks while preserving the existing audited backend lifecycle.

## Requirements

### Requirement: Explicit durable-task workspace mode
The administrator agent workspace SHALL expose durable tasks as an explicit mode alongside the
existing synchronous chat mode, and switching modes MUST NOT discard chat sessions or alter the
single-chat execution contract.

#### Scenario: Operator switches to durable tasks
- **WHEN** an administrator selects the durable-task mode
- **THEN** the workspace displays the task surface while retaining all existing chat session state

#### Scenario: Operator returns to chat
- **WHEN** an administrator returns from durable-task mode to chat mode
- **THEN** the previously selected chat session and its messages remain available

### Requirement: Durable task creation and recent-task discovery
The task workspace SHALL let the current administrator create a task from a non-empty goal and
SHALL provide a bounded recent-task list that can be filtered by lifecycle status and refreshed
without reloading the page.

#### Scenario: Task is created successfully
- **WHEN** an administrator submits a valid task goal
- **THEN** the workspace creates the owner-scoped task, refreshes the recent-task list, selects the
  new task, and clears the submitted goal

#### Scenario: Task list cannot be loaded
- **WHEN** the recent-task request fails
- **THEN** the workspace presents an error state and a retry action without clearing a previously
  selected task detail

### Requirement: Inspectable task progress and persisted steps
The task workspace SHALL display the selected task's lifecycle status, bounded progress, terminal
reason when present, and persisted steps with their order, tool, risk, outcome, trace, and error
fields supplied by the backend.

#### Scenario: Task detail is selected
- **WHEN** an administrator selects an owned task from the recent list
- **THEN** the workspace loads and displays the latest task detail and ordered persisted steps

#### Scenario: Task reaches a terminal state
- **WHEN** the selected task becomes completed, cancelled, failed, or requires review
- **THEN** the workspace displays the terminal status and any available terminal or cancellation
  reason without offering an invalid state transition

### Requirement: Incremental task-event timeline
The task workspace SHALL read task events by stable cursor, merge new events without duplicates,
and poll task detail and events only while the workspace is active and the selected task is
non-terminal.

#### Scenario: New events arrive during an active task
- **WHEN** polling returns events after the last received cursor
- **THEN** the workspace appends events in cursor order exactly once and advances the cursor

#### Scenario: Polling is no longer required
- **WHEN** the workspace becomes inactive, the selected task changes, or the selected task becomes
  terminal
- **THEN** the previous polling schedule is stopped before another schedule can start

### Requirement: State-aware lifecycle controls
The task workspace SHALL expose pause, resume, and cancel commands only for states accepted by the
backend lifecycle and SHALL refresh both task state and events after a successful command.

#### Scenario: Running task is paused
- **WHEN** an administrator pauses a queued or running task and the backend accepts the transition
- **THEN** the workspace shows the returned paused state and its new timeline event

#### Scenario: Terminal task is inspected
- **WHEN** an administrator views a terminal task
- **THEN** pause, resume, input, confirmation, and cancel commands are unavailable

### Requirement: Safe human-intervention forms
The task workspace SHALL provide bounded structured input for `WAITING_INPUT` tasks and exact
strong-confirmation input for `WAITING_CONFIRMATION` tasks, and MUST NOT execute or infer a
destructive action in the browser.

#### Scenario: Missing input is submitted
- **WHEN** an administrator submits a valid non-empty JSON object for a task waiting for input
- **THEN** the workspace sends that object as planner context and displays the returned task state

#### Scenario: Strong confirmation text does not match
- **WHEN** the entered confirmation text differs from the backend-provided required text
- **THEN** the confirmation command remains unavailable and no confirmation request is sent

#### Scenario: Strong confirmation is accepted
- **WHEN** the entered confirmation text exactly matches the backend-provided required text and the
  administrator submits it
- **THEN** the workspace calls the owner-scoped confirmation endpoint and refreshes task detail and
  events from the returned lifecycle state
