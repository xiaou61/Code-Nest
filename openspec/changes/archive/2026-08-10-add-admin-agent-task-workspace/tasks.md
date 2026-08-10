## 1. Contract and State Tests

- [x] 1.1 Add frontend API contract tests covering every durable-task route and backend controller parity.
- [x] 1.2 Add pure state-helper tests for task normalization, bounded progress, terminal detection, permitted actions, JSON input validation, and cursor event merging.
- [x] 1.3 Add workspace integration contract tests for explicit chat/task modes, task component wiring, active polling ownership, and preserved synchronous chat behavior.

## 2. Task Data Layer

- [x] 2.1 Add a dedicated owner-scoped durable-task API client for create, list, detail, events, pause, resume, input, confirm, and cancel operations.
- [x] 2.2 Add framework-independent task workspace helpers for state labels, control capabilities, progress, safe normalization, structured input, and event deduplication.

## 3. Durable Task Workspace

- [x] 3.1 Add a responsive task workspace with bounded goal creation, status filtering, refresh/error/empty states, and recent-task selection.
- [x] 3.2 Add selected-task lifecycle summary, terminal reasons, ordered persisted step details, and trace/audit metadata.
- [x] 3.3 Add cursor-based event timeline refresh with one generation-fenced polling loop that stops while inactive or terminal.
- [x] 3.4 Add state-aware pause, resume, cancel, missing-input, and exact strong-confirmation controls using backend-authoritative mutation results.
- [x] 3.5 Integrate an explicit chat/task mode control into the existing full-screen administrator-agent workspace without resetting chat state.

## 4. Verification

- [x] 4.1 Strictly validate the OpenSpec change and run the complete administrator frontend contract suite.
- [x] 4.2 Run the administrator frontend production build and inspect responsive layout behavior and final diff for regressions.
