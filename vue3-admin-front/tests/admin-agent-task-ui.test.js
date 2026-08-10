import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const repositoryRoot = resolve(projectRoot, '..')
const taskApiSource = readFileSync(resolve(projectRoot, 'src/api/agentTask.js'), 'utf8')
const taskWorkspaceSource = readFileSync(
  resolve(projectRoot, 'src/components/agent/AgentTaskWorkspace.vue'),
  'utf8',
)
const drawerSource = readFileSync(
  resolve(projectRoot, 'src/components/agent/AdminAgentDrawer.vue'),
  'utf8',
)
const controllerSource = readFileSync(
  resolve(repositoryRoot, 'xiaou-system/src/main/java/com/xiaou/system/controller/AgentTaskController.java'),
  'utf8',
)

test('durable task API maps every owner-scoped controller route', () => {
  assert.match(taskApiSource, /request\.post\('\/admin\/agent\/tasks', data, config\)/)
  assert.match(taskApiSource, /request\.get\('\/admin\/agent\/tasks', params, config\)/)
  assert.match(taskApiSource, /request\.get\(`\$\{taskPath\(taskId\)\}`, \{\}, config\)/)
  assert.match(taskApiSource, /request\.get\(`\$\{taskPath\(taskId\)\}\/events`, params, config\)/)
  assert.match(taskApiSource, /request\.post\(`\$\{taskPath\(taskId\)\}\/pause`, data, config\)/)
  assert.match(taskApiSource, /request\.post\(`\$\{taskPath\(taskId\)\}\/resume`, \{\}, config\)/)
  assert.match(taskApiSource, /request\.post\(`\$\{taskPath\(taskId\)\}\/input`, data, config\)/)
  assert.match(taskApiSource, /request\.post\(`\$\{taskPath\(taskId\)\}\/confirm`, data, config\)/)
  assert.match(taskApiSource, /request\.post\(`\$\{taskPath\(taskId\)\}\/cancel`, data, config\)/)
  assert.match(taskApiSource, /encodeURIComponent\(String\(taskId \?\? ''\)\.trim\(\)\)/)

  for (const route of ['/{taskId}/events', '/{taskId}/pause', '/{taskId}/resume', '/{taskId}/input', '/{taskId}/confirm', '/{taskId}/cancel']) {
    assert.match(controllerSource, new RegExp(`@(?:Get|Post)Mapping\\(\\"${route.replaceAll('/', '\\/').replace('{', '\\{').replace('}', '\\}')}\\"\\)`))
  }
})

test('agent workspace exposes explicit chat and durable task modes without replacing chat', () => {
  assert.match(drawerSource, /const workspaceMode = ref\('chat'\)/)
  assert.match(drawerSource, /<el-radio-group[^>]*v-model="workspaceMode"/)
  assert.match(drawerSource, /<el-radio-button label="chat">对话<\/el-radio-button>/)
  assert.match(drawerSource, /<el-radio-button label="tasks">持久任务<\/el-radio-button>/)
  assert.match(drawerSource, /<AgentTaskWorkspace/)
  assert.match(drawerSource, /:active="workspaceVisible && workspaceMode === 'tasks'"/)
  assert.match(drawerSource, /v-if="workspaceMode === 'chat'"/)
  assert.match(drawerSource, /agentChatApi\.sendMessage/)
})

test('task workspace owns incremental polling and state-aware interventions', () => {
  assert.match(taskWorkspaceSource, /import \{ agentTaskApi \} from '@\/api\/agentTask'/)
  assert.match(taskWorkspaceSource, /setInterval\(refreshSelectedTask, TASK_POLL_INTERVAL_MS\)/)
  assert.match(taskWorkspaceSource, /clearInterval\(pollTimer\)/)
  assert.match(taskWorkspaceSource, /refreshGeneration/)
  assert.match(taskWorkspaceSource, /after: eventCursor\.value/)
  assert.match(taskWorkspaceSource, /mergeTaskEvents/)
  assert.match(taskWorkspaceSource, /parseTaskInputJson/)
  assert.match(taskWorkspaceSource, /confirmationText\.value === selectedTask\.value\?\.confirmation\?\.requiredText/)
  assert.match(taskWorkspaceSource, /agentTaskApi\.pauseTask/)
  assert.match(taskWorkspaceSource, /promptReason\('暂停任务', '暂停原因（可选）', 500\)/)
  assert.match(taskWorkspaceSource, /agentTaskApi\.resumeTask/)
  assert.match(taskWorkspaceSource, /agentTaskApi\.submitInput/)
  assert.match(taskWorkspaceSource, /agentTaskApi\.confirmTask/)
  assert.match(taskWorkspaceSource, /agentTaskApi\.cancelTask/)
  assert.match(taskWorkspaceSource, /promptReason\('取消任务', '取消原因（可选）', 1000\)/)
})
