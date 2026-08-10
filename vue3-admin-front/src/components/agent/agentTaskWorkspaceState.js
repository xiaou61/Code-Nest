export const MAX_TASK_EVENTS = 200
export const MAX_TASK_INPUT_FIELDS = 20
export const MAX_TASK_INPUT_BYTES = 4000
export const MAX_TASK_STEPS = 10

const TERMINAL_STATUSES = new Set([
  'COMPLETED',
  'CANCELLED',
  'FAILED',
  'REQUIRES_REVIEW',
])

const CANCELLABLE_STATUSES = new Set([
  'QUEUED',
  'RUNNING',
  'WAITING_CONFIRMATION',
  'WAITING_INPUT',
  'PAUSED',
])

const STATUS_META = {
  QUEUED: { label: '排队中', type: 'info' },
  RUNNING: { label: '执行中', type: 'primary' },
  WAITING_CONFIRMATION: { label: '等待确认', type: 'warning' },
  WAITING_INPUT: { label: '等待输入', type: 'warning' },
  PAUSED: { label: '已暂停', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  FAILED: { label: '执行失败', type: 'danger' },
  REQUIRES_REVIEW: { label: '需要复核', type: 'danger' },
}

const UNKNOWN_STATUS_META = { label: '未知', type: 'info' }

export function taskStatusMeta(status) {
  const normalizedStatus = normalizeStatus(status)
  return STATUS_META[normalizedStatus] || UNKNOWN_STATUS_META
}

export function isTerminalTaskStatus(status) {
  return TERMINAL_STATUSES.has(normalizeStatus(status))
}

export function taskCapabilities(status) {
  const normalizedStatus = normalizeStatus(status)
  const isTerminal = isTerminalTaskStatus(normalizedStatus)
  return {
    isTerminal,
    canPause: normalizedStatus === 'QUEUED' || normalizedStatus === 'RUNNING',
    canResume: normalizedStatus === 'PAUSED',
    canCancel: !isTerminal && CANCELLABLE_STATUSES.has(normalizedStatus),
    canSubmitInput: normalizedStatus === 'WAITING_INPUT',
    canConfirm: normalizedStatus === 'WAITING_CONFIRMATION',
  }
}

export function normalizeTask(value) {
  const source = isPlainObject(value) ? value : {}
  const status = normalizeStatus(source.status)
  const maxSteps = Math.min(MAX_TASK_STEPS, positiveInteger(source.maxSteps, 1))
  const completedSteps = boundedInteger(source.completedSteps, 0, maxSteps)
  const steps = Array.isArray(source.steps)
    ? source.steps
      .filter(isPlainObject)
      .map(normalizeStep)
      .sort((left, right) => left.stepOrder - right.stepOrder)
    : []

  return {
    taskId: text(source.taskId),
    goal: text(source.goal),
    sessionId: text(source.sessionId),
    status,
    statusText: text(source.statusText) || taskStatusMeta(status).label,
    maxSteps,
    completedSteps,
    currentStepOrder: boundedInteger(source.currentStepOrder, 0, maxSteps),
    terminalCode: text(source.terminalCode),
    terminalReason: text(source.terminalReason),
    cancelReason: text(source.cancelReason),
    confirmation: normalizeConfirmation(source.confirmation),
    steps,
    cancelledAt: text(source.cancelledAt),
    completedAt: text(source.completedAt),
    createdTime: text(source.createdTime),
    updatedTime: text(source.updatedTime),
  }
}

export function taskProgress(task) {
  const normalizedTask = normalizeTask(task)
  const total = normalizedTask.maxSteps
  const completed = Math.min(normalizedTask.completedSteps, total)
  return {
    completed,
    total,
    percent: total > 0 ? Math.round((completed / total) * 100) : 0,
  }
}

export function parseTaskInputJson(value) {
  let parsed
  try {
    parsed = JSON.parse(String(value ?? '').trim())
  } catch {
    throw new Error('请输入有效的 JSON 对象')
  }
  if (!isPlainObject(parsed)) {
    throw new Error('补充输入必须是 JSON 对象')
  }
  const keys = Object.keys(parsed)
  if (!keys.length) {
    throw new Error('补充输入不能为空')
  }
  if (keys.length > MAX_TASK_INPUT_FIELDS) {
    throw new Error(`补充输入最多包含 ${MAX_TASK_INPUT_FIELDS} 个字段`)
  }
  if (JSON.stringify(parsed).length > MAX_TASK_INPUT_BYTES) {
    throw new Error(`补充输入不能超过 ${MAX_TASK_INPUT_BYTES} 个字符`)
  }
  return parsed
}

export function mergeTaskEvents(existingEvents, incomingEvents) {
  const byCursor = new Map()
  for (const event of [...safeArray(existingEvents), ...safeArray(incomingEvents)]) {
    if (!isPlainObject(event)) continue
    const cursor = Number(event.cursor)
    if (!Number.isSafeInteger(cursor) || cursor <= 0 || byCursor.has(cursor)) continue
    byCursor.set(cursor, { ...event, cursor })
  }
  const sorted = [...byCursor.values()].sort((left, right) => left.cursor - right.cursor)
  const events = sorted.slice(-MAX_TASK_EVENTS)
  return {
    events,
    nextCursor: events.at(-1)?.cursor ?? null,
  }
}

function normalizeStep(value) {
  return {
    stepOrder: positiveInteger(value.stepOrder, 1),
    toolName: text(value.toolName),
    inputSummary: text(value.inputSummary),
    riskLevel: text(value.riskLevel),
    riskCategory: text(value.riskCategory),
    status: normalizeStatus(value.status),
    auditId: text(value.auditId),
    traceId: text(value.traceId),
    resultSummary: text(value.resultSummary),
    resultJson: text(value.resultJson),
    errorMessage: text(value.errorMessage),
    startedAt: text(value.startedAt),
    completedAt: text(value.completedAt),
  }
}

function normalizeConfirmation(value) {
  if (!isPlainObject(value)) return null
  const auditId = text(value.auditId)
  const requiredText = text(value.requiredText)
  const prompt = text(value.prompt)
  if (!auditId && !requiredText && !prompt) return null
  return { auditId, requiredText, prompt }
}

function normalizeStatus(value) {
  return text(value).toUpperCase()
}

function positiveInteger(value, fallback) {
  const normalized = Math.trunc(Number(value))
  return Number.isFinite(normalized) && normalized > 0 ? normalized : fallback
}

function boundedInteger(value, minimum, maximum) {
  const normalized = Math.trunc(Number(value))
  if (!Number.isFinite(normalized)) return minimum
  return Math.min(maximum, Math.max(minimum, normalized))
}

function text(value) {
  return value == null ? '' : String(value).trim()
}

function safeArray(value) {
  return Array.isArray(value) ? value : []
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
}
