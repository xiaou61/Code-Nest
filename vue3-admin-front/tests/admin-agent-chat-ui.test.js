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
    /sendMessage\(data\) \{\s*return request\.post\('\/admin\/agent\/chat', data\)\s*\}/
  )
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/planner/)
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/policy/)
  assert.doesNotMatch(agentChatApiSource, /\/admin\/agent\/audit/)
})

test('admin agent drawer should remain a thin chat UI without frontend runtime orchestration', () => {
  assert.match(drawerSource, /import \{ agentChatApi \} from '@\/api\/agentChat'/)
  assert.match(drawerSource, /agentChatApi\.sendMessage/)
  assert.match(drawerSource, /auditId: pendingConfirmation\.value\.auditId/)
  assert.match(drawerSource, /confirmationText: content/)
  assert.match(drawerSource, /message: '取消'/)

  assert.doesNotMatch(drawerSource, /@\/agent/)
  assert.doesNotMatch(drawerSource, /createAdminAgent/)
  assert.doesNotMatch(drawerSource, /agentPlannerApi|agentPolicyApi|agentAuditApi/)
  assert.doesNotMatch(drawerSource, /\/admin\/agent\/planner|\/admin\/agent\/policy|\/admin\/agent\/audit/)
})

test('admin layout should mount only the thin drawer component', () => {
  assert.match(layoutSource, /<AdminAgentDrawer\s*\/>/)
  assert.match(layoutSource, /import AdminAgentDrawer from '@\/components\/agent\/AdminAgentDrawer\.vue'/)
  assert.doesNotMatch(layoutSource, /@\/agent/)
})
