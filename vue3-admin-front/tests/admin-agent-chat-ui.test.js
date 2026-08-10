import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const agentChatApiSource = readFileSync(resolve(projectRoot, 'src/api/agentChat.js'), 'utf8')
const drawerSource = readFileSync(resolve(projectRoot, 'src/components/agent/AdminAgentDrawer.vue'), 'utf8')
const layoutSource = readFileSync(resolve(projectRoot, 'src/layout/index.vue'), 'utf8')

test('admin agent frontend should only call the unified backend chat endpoint', () => {
  assert.match(
    agentChatApiSource,
    /sendMessage\(data, config = \{\}\) \{\s*return request\.post\('\/admin\/agent\/chat', data, config\)\s*\}/
  )
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/planner/)
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/policy/)
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/audit/)
})

test('admin agent workspace should remain a single-endpoint UI without frontend runtime orchestration', () => {
  assert.match(drawerSource, /import \{ agentChatApi \} from '@\/api\/agentChat'/)
  assert.match(drawerSource, /agentChatApi\.sendMessage/)
  assert.match(drawerSource, /<el-dialog/)
  assert.match(drawerSource, /fullscreen/)
  assert.match(drawerSource, /:global\(\.admin-agent-dialog\) \{[\s\S]*padding: 0;[\s\S]*border: 0;[\s\S]*border-radius: 0;/)
  assert.match(drawerSource, /:global\(\.admin-agent-dialog \.el-dialog__header\) \{[\s\S]*border-bottom: 0;/)
  assert.match(drawerSource, /admin-agent__sessions/)
  assert.match(drawerSource, /admin-agent__conversation/)
  assert.match(drawerSource, /admin-agent__inspector/)
  assert.match(drawerSource, /aria-live="polite"/)
  assert.match(drawerSource, /new AbortController\(\)/)
  assert.match(drawerSource, /signal: activeController\.signal/)
  assert.match(drawerSource, /sessionToMarkdown/)
  assert.match(drawerSource, /WORKSPACE_STORAGE_KEY/)
  assert.match(drawerSource, /auditId: pendingConfirmation\.value\.auditId/)
  assert.match(drawerSource, /confirmationText: content/)
  assert.match(drawerSource, /message: '取消'/)

  assert.doesNotMatch(drawerSource, /@\/agent/)
  assert.doesNotMatch(drawerSource, /createAdminAgent/)
  assert.doesNotMatch(drawerSource, /agentPlannerApi|agentPolicyApi|agentAuditApi/)
  assert.doesNotMatch(drawerSource, /\/admin\/agent\/planner|\/admin\/agent\/policy|\/admin\/agent\/audit/)
})

test('admin layout should globally mount the unified workspace entry', () => {
  assert.match(layoutSource, /<AdminAgentDrawer\s*\/>/)
  assert.match(layoutSource, /import AdminAgentDrawer from '@\/components\/agent\/AdminAgentDrawer\.vue'/)
  assert.doesNotMatch(layoutSource, /@\/agent/)
})
