import test from 'node:test'
import assert from 'node:assert/strict'

import {
  MAX_TASK_EVENTS,
  MAX_TASK_INPUT_BYTES,
  MAX_TASK_STEPS,
  isTerminalTaskStatus,
  mergeTaskEvents,
  normalizeTask,
  parseTaskInputJson,
  taskCapabilities,
  taskProgress,
  taskStatusMeta,
} from '../src/components/agent/agentTaskWorkspaceState.js'

test('task normalization keeps bounded ordered operational detail', () => {
  const task = normalizeTask({
    taskId: ' task-1 ',
    goal: ' inspect release health ',
    status: 'RUNNING',
    maxSteps: 3,
    completedSteps: 7,
    steps: [
      { stepOrder: 2, toolName: 'second', status: 'PENDING' },
      { stepOrder: 1, toolName: 'first', status: 'COMPLETED' },
      null,
    ],
  })

  assert.equal(task.taskId, 'task-1')
  assert.equal(task.goal, 'inspect release health')
  assert.equal(task.status, 'RUNNING')
  assert.equal(task.maxSteps, 3)
  assert.deepEqual(task.steps.map((step) => step.stepOrder), [1, 2])
  assert.deepEqual(taskProgress(task), {
    completed: 3,
    total: 3,
    percent: 100,
  })
  assert.equal(normalizeTask({ maxSteps: MAX_TASK_STEPS + 10 }).maxSteps, MAX_TASK_STEPS)
})

test('task lifecycle capabilities mirror backend accepted transitions', () => {
  assert.deepEqual(taskCapabilities('RUNNING'), {
    isTerminal: false,
    canPause: true,
    canResume: false,
    canCancel: true,
    canSubmitInput: false,
    canConfirm: false,
  })
  assert.equal(taskCapabilities('PAUSED').canResume, true)
  assert.equal(taskCapabilities('WAITING_INPUT').canSubmitInput, true)
  assert.equal(taskCapabilities('WAITING_CONFIRMATION').canConfirm, true)

  for (const status of ['COMPLETED', 'CANCELLED', 'FAILED', 'REQUIRES_REVIEW']) {
    assert.equal(isTerminalTaskStatus(status), true)
    assert.deepEqual(taskCapabilities(status), {
      isTerminal: true,
      canPause: false,
      canResume: false,
      canCancel: false,
      canSubmitInput: false,
      canConfirm: false,
    })
  }
  assert.deepEqual(taskStatusMeta('unexpected'), { label: '未知', type: 'info' })
})

test('structured task input accepts only a bounded non-empty JSON object', () => {
  assert.deepEqual(parseTaskInputJson('{"environment":"staging","limit":5}'), {
    environment: 'staging',
    limit: 5,
  })

  assert.throws(() => parseTaskInputJson('[]'), /JSON 对象/)
  assert.throws(() => parseTaskInputJson('{}'), /不能为空/)
  assert.throws(() => parseTaskInputJson('{invalid'), /有效的 JSON/)
  const tooManyFields = Object.fromEntries(Array.from({ length: 21 }, (_, index) => [`k${index}`, index]))
  assert.throws(() => parseTaskInputJson(JSON.stringify(tooManyFields)), /20/)
  assert.throws(() => parseTaskInputJson(JSON.stringify({ value: 'x'.repeat(MAX_TASK_INPUT_BYTES) })), /4000/)
})

test('task events merge by stable cursor without duplicates and remain bounded', () => {
  const existing = [{ cursor: 2, eventType: 'TASK_STARTED' }, { cursor: 1, eventType: 'TASK_CREATED' }]
  const incoming = [
    { cursor: 2, eventType: 'DUPLICATE' },
    { cursor: 4, eventType: 'STEP_COMPLETED' },
    { cursor: 3, eventType: 'STEP_STARTED' },
  ]

  const merged = mergeTaskEvents(existing, incoming)
  assert.deepEqual(merged.events.map((event) => event.cursor), [1, 2, 3, 4])
  assert.equal(merged.events[1].eventType, 'TASK_STARTED')
  assert.equal(merged.nextCursor, 4)

  const oversized = Array.from({ length: MAX_TASK_EVENTS + 5 }, (_, index) => ({
    cursor: index + 1,
    eventType: 'EVENT',
  }))
  const bounded = mergeTaskEvents([], oversized)
  assert.equal(bounded.events.length, MAX_TASK_EVENTS)
  assert.equal(bounded.events[0].cursor, 6)
})
