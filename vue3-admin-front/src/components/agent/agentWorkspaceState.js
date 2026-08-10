export const WORKSPACE_STORAGE_KEY = 'code-nest.admin-agent.workspace.v1'
export const MAX_WORKSPACE_SESSIONS = 20
export const MAX_MESSAGES_PER_SESSION = 100

const TITLE_LIMIT = 32
const DEFAULT_SESSION_TITLE = '新会话'

export function createWorkspaceState(options = {}) {
  const session = createWorkspaceSession(options)
  return {
    currentSessionId: session.id,
    sessions: [session],
  }
}

export function createWorkspaceSession(options = {}) {
  const timestamp = nowValue(options)
  return {
    id: idValue(options, 'admin-agent'),
    title: DEFAULT_SESSION_TITLE,
    createdAt: timestamp,
    updatedAt: timestamp,
    messages: [],
    pendingConfirmation: null,
  }
}

export function normalizeWorkspaceState(value, options = {}) {
  if (!value || typeof value !== 'object' || !Array.isArray(value.sessions)) {
    return createWorkspaceState(options)
  }

  const seenSessionIds = new Set()
  const sessions = value.sessions
    .map((session) => normalizeSession(session, options))
    .filter((session) => {
      if (!session || seenSessionIds.has(session.id)) return false
      seenSessionIds.add(session.id)
      return true
    })
    .sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt))
    .slice(0, MAX_WORKSPACE_SESSIONS)

  if (!sessions.length) {
    return createWorkspaceState(options)
  }

  const requestedSessionId = cleanText(value.currentSessionId)
  const currentSessionId = sessions.some((session) => session.id === requestedSessionId)
    ? requestedSessionId
    : sessions[0].id

  return { currentSessionId, sessions }
}

export function userMessageFromText(text, options = {}) {
  return baseMessage({
    id: idValue(options, 'message'),
    role: 'user',
    text: cleanText(text),
    createdAt: nowValue(options),
  })
}

export function agentMessageFromResponse(response = {}, options = {}) {
  const safeResponse = response && typeof response === 'object' ? response : {}
  return baseMessage({
    id: idValue(options, 'message'),
    role: 'agent',
    text: cleanText(safeResponse.answer)
      || cleanText(safeResponse.errorMessage)
      || '后端没有返回可展示内容。',
    createdAt: nowValue(options),
    status: cleanText(safeResponse.status) || 'answered',
    traceId: cleanText(safeResponse.traceId),
    auditId: cleanText(safeResponse.auditId),
    idempotencyKey: cleanText(safeResponse.idempotencyKey),
    toolName: cleanText(safeResponse.toolName),
    riskLevel: cleanText(safeResponse.riskLevel),
    riskCategory: cleanText(safeResponse.riskCategory),
    errorCode: cleanText(safeResponse.errorCode),
    errorMessage: cleanText(safeResponse.errorMessage),
    plan: cloneArray(safeResponse.plan),
    diff: cloneArray(safeResponse.diff),
    artifacts: cloneArray(safeResponse.artifacts),
    nextActions: cloneArray(safeResponse.nextActions),
    trace: cloneArray(safeResponse.trace),
    confirmation: normalizeConfirmation(safeResponse.confirmation),
  })
}

export function deriveSessionTitle(message) {
  const normalized = cleanText(message).replace(/\s+/g, ' ')
  if (!normalized) return DEFAULT_SESSION_TITLE
  if (normalized.length <= TITLE_LIMIT) return normalized
  return `${normalized.slice(0, TITLE_LIMIT - 1)}…`
}

export function sessionToMarkdown(session = {}) {
  const title = cleanText(session.title) || DEFAULT_SESSION_TITLE
  const lines = [`# ${title}`, '']

  for (const message of safeArray(session.messages)) {
    const role = message?.role === 'user' ? '管理员' : '智能体'
    lines.push(`## ${role}`)
    lines.push('')
    lines.push(cleanText(message?.text) || '-')

    const metadata = []
    if (cleanText(message?.status)) metadata.push(`状态: ${message.status}`)
    if (cleanText(message?.toolName)) metadata.push(`工具: ${message.toolName}`)
    if (cleanText(message?.traceId)) metadata.push(`Trace: ${message.traceId}`)
    if (metadata.length) {
      lines.push('')
      lines.push(metadata.join(' | '))
    }

    appendJsonSection(lines, '执行计划', message?.plan)
    appendJsonSection(lines, '变更差异', message?.diff)
    appendJsonSection(lines, '结构化结果', message?.artifacts)
    appendJsonSection(lines, '运行追踪', message?.trace)
    lines.push('')
  }

  return `${lines.join('\n').trim()}\n`
}

function normalizeSession(session, options) {
  if (!session || typeof session !== 'object') return null
  const id = cleanText(session.id)
  if (!id) return null

  const fallbackTime = nowValue(options)
  const messages = safeArray(session.messages)
    .map((message) => normalizeMessage(message, fallbackTime))
    .filter(Boolean)
    .slice(-MAX_MESSAGES_PER_SESSION)

  return {
    id,
    title: cleanText(session.title) || DEFAULT_SESSION_TITLE,
    createdAt: validTimestamp(session.createdAt, fallbackTime),
    updatedAt: validTimestamp(session.updatedAt, fallbackTime),
    messages,
    pendingConfirmation: normalizeConfirmation(session.pendingConfirmation),
  }
}

function normalizeMessage(message, fallbackTime) {
  if (!message || typeof message !== 'object') return null
  const role = message.role === 'user' ? 'user' : message.role === 'agent' ? 'agent' : ''
  if (!role) return null

  return baseMessage({
    ...cloneValue(message),
    id: cleanText(message.id) || `${role}-${fallbackTime}`,
    role,
    text: cleanText(message.text),
    createdAt: validTimestamp(message.createdAt, fallbackTime),
    confirmation: normalizeConfirmation(message.confirmation),
  })
}

function baseMessage(message) {
  return {
    id: cleanText(message.id),
    role: message.role === 'user' ? 'user' : 'agent',
    text: cleanText(message.text),
    createdAt: validTimestamp(message.createdAt, new Date().toISOString()),
    status: cleanText(message.status),
    traceId: cleanText(message.traceId),
    auditId: cleanText(message.auditId),
    idempotencyKey: cleanText(message.idempotencyKey),
    toolName: cleanText(message.toolName),
    riskLevel: cleanText(message.riskLevel),
    riskCategory: cleanText(message.riskCategory),
    errorCode: cleanText(message.errorCode),
    errorMessage: cleanText(message.errorMessage),
    plan: cloneArray(message.plan),
    diff: cloneArray(message.diff),
    artifacts: cloneArray(message.artifacts),
    nextActions: cloneArray(message.nextActions),
    trace: cloneArray(message.trace),
    confirmation: normalizeConfirmation(message.confirmation),
  }
}

function normalizeConfirmation(value) {
  if (!value || typeof value !== 'object') return null
  const auditId = cleanText(value.auditId)
  if (!auditId) return null
  return {
    auditId,
    confirmationId: cleanText(value.confirmationId),
    requiredText: cleanText(value.requiredText),
    prompt: cleanText(value.prompt),
  }
}

function appendJsonSection(lines, title, value) {
  const list = safeArray(value)
  if (!list.length) return
  lines.push('')
  lines.push(`### ${title}`)
  lines.push('')
  lines.push('```json')
  lines.push(JSON.stringify(list, null, 2))
  lines.push('```')
}

function idValue(options, prefix) {
  if (typeof options.idFactory === 'function') return cleanText(options.idFactory())
  if (globalThis.crypto?.randomUUID) return `${prefix}-${globalThis.crypto.randomUUID()}`
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function nowValue(options) {
  const value = typeof options.now === 'function' ? options.now() : new Date().toISOString()
  return validTimestamp(value, new Date().toISOString())
}

function validTimestamp(value, fallback) {
  const normalized = cleanText(value)
  return normalized && Number.isFinite(Date.parse(normalized)) ? normalized : fallback
}

function cleanText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

function safeArray(value) {
  return Array.isArray(value) ? value : []
}

function cloneArray(value) {
  return safeArray(value).map(cloneValue)
}

function cloneValue(value) {
  if (value === undefined) return null
  try {
    return JSON.parse(JSON.stringify(value))
  } catch {
    return String(value)
  }
}
