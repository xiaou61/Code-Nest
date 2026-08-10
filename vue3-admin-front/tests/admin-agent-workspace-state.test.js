import test from 'node:test'
import assert from 'node:assert/strict'

import {
  MAX_MESSAGES_PER_SESSION,
  MAX_WORKSPACE_SESSIONS,
  agentMessageFromResponse,
  createWorkspaceState,
  deriveSessionTitle,
  normalizeWorkspaceState,
  sessionToMarkdown,
} from '../src/components/agent/agentWorkspaceState.js'

const NOW = '2026-08-05T08:00:00.000Z'

test('workspace starts with one selected bounded session', () => {
  const state = createWorkspaceState({
    idFactory: () => 'session-1',
    now: () => NOW,
  })

  assert.equal(state.currentSessionId, 'session-1')
  assert.equal(state.sessions.length, 1)
  assert.deepEqual(state.sessions[0], {
    id: 'session-1',
    title: '新会话',
    createdAt: NOW,
    updatedAt: NOW,
    messages: [],
    pendingConfirmation: null,
  })
})

test('workspace normalization rejects invalid state and bounds persisted history', () => {
  const sessions = Array.from({ length: MAX_WORKSPACE_SESSIONS + 5 }, (_, sessionIndex) => ({
    id: `session-${sessionIndex}`,
    title: `  会话 ${sessionIndex}  `,
    createdAt: NOW,
    updatedAt: new Date(Date.parse(NOW) + sessionIndex * 1000).toISOString(),
    messages: Array.from({ length: MAX_MESSAGES_PER_SESSION + 8 }, (_, messageIndex) => ({
      id: `message-${sessionIndex}-${messageIndex}`,
      role: messageIndex % 2 === 0 ? 'user' : 'agent',
      text: `message ${messageIndex}`,
      createdAt: NOW,
    })),
    pendingConfirmation: sessionIndex === 0 ? { auditId: 'audit-1', requiredText: '确认执行' } : null,
  }))

  const normalized = normalizeWorkspaceState({
    currentSessionId: 'missing-session',
    sessions,
  }, {
    idFactory: () => 'fallback-session',
    now: () => NOW,
  })

  assert.equal(normalized.sessions.length, MAX_WORKSPACE_SESSIONS)
  assert.equal(normalized.sessions[0].id, `session-${MAX_WORKSPACE_SESSIONS + 4}`)
  assert.equal(normalized.sessions[0].messages.length, MAX_MESSAGES_PER_SESSION)
  assert.equal(normalized.currentSessionId, normalized.sessions[0].id)
})

test('agent response mapping preserves operational details for the inspector', () => {
  const message = agentMessageFromResponse({
    status: 'confirm_required',
    answer: '已生成写入预览',
    traceId: 'trace-1',
    toolName: 'system.operation-log.clean',
    riskLevel: 'medium',
    riskCategory: 'WRITE',
    plan: [{ title: '预览', status: 'done', detail: '计算删除范围' }],
    diff: [{ field: 'retentionDays', beforeValue: 60, afterValue: 30 }],
    artifacts: [{ type: 'preview', title: '预览结果', data: { affected: 12 } }],
    nextActions: ['确认后执行'],
    trace: [{ stage: 'policy.checked', status: 'done', detail: 'confirmation required' }],
    confirmation: { auditId: 'audit-1', requiredText: '确认执行' },
  }, {
    idFactory: () => 'message-1',
    now: () => NOW,
  })

  assert.equal(message.id, 'message-1')
  assert.equal(message.role, 'agent')
  assert.equal(message.text, '已生成写入预览')
  assert.equal(message.traceId, 'trace-1')
  assert.equal(message.toolName, 'system.operation-log.clean')
  assert.equal(message.plan.length, 1)
  assert.equal(message.diff.length, 1)
  assert.equal(message.artifacts.length, 1)
  assert.equal(message.nextActions.length, 1)
  assert.equal(message.trace.length, 1)
  assert.equal(message.confirmation.auditId, 'audit-1')
})

test('session titles and markdown exports remain readable and deterministic', () => {
  assert.equal(
    deriveSessionTitle('  请帮我\n查询最近的操作日志并给出风险分析结论  '),
    '请帮我 查询最近的操作日志并给出风险分析结论',
  )

  const markdown = sessionToMarkdown({
    title: '运行日志检查',
    createdAt: NOW,
    messages: [
      { role: 'user', text: '查最近3条操作日志', createdAt: NOW },
      {
        role: 'agent',
        text: '已完成查询',
        status: 'answered',
        toolName: 'system.operation-log.list',
        artifacts: [{ title: '日志', data: [{ id: 1, action: 'LOGIN' }] }],
        createdAt: NOW,
      },
    ],
  })

  assert.match(markdown, /^# 运行日志检查/m)
  assert.match(markdown, /system\.operation-log\.list/)
  assert.match(markdown, /"action": "LOGIN"/)
  assert.doesNotMatch(markdown, /\[object Object\]/)
})
