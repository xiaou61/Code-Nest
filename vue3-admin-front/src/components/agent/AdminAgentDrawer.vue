<template>
  <el-button
    class="admin-agent__trigger"
    type="primary"
    circle
    size="large"
    :icon="Cpu"
    @click="drawerVisible = true"
  />

  <el-drawer
    v-model="drawerVisible"
    title="管理员智能体"
    size="460px"
    append-to-body
    destroy-on-close
  >
    <section class="admin-agent">
      <header class="admin-agent__intro">
        <p>统一后端智能体</p>
        <h2>只通过 /admin/agent/chat 工作</h2>
        <span>前端只负责展示、输入和确认，规划、策略、执行与审计都在后端。</span>
      </header>

      <div class="admin-agent__examples" aria-label="示例指令">
        <button
          v-for="example in examples"
          :key="example"
          type="button"
          :disabled="running"
          @click="submitExample(example)"
        >
          {{ example }}
        </button>
      </div>

      <main ref="messageListRef" class="admin-agent__messages">
        <article
          v-for="message in messages"
          :key="message.id"
          class="admin-agent__message"
          :class="`admin-agent__message--${message.role}`"
        >
          <p class="admin-agent__message-meta">
            {{ message.role === 'user' ? '你' : 'Agent' }}
            <el-tag v-if="message.status" size="small" :type="statusTagType(message.status)">
              {{ statusText(message.status) }}
            </el-tag>
          </p>
          <p class="admin-agent__message-text">{{ message.text }}</p>

          <section v-if="message.plan?.length" class="admin-agent__block">
            <h3>执行计划</h3>
            <ol>
              <li v-for="(step, index) in message.plan" :key="`${message.id}-plan-${index}`">
                <strong>{{ step.title || `步骤 ${index + 1}` }}</strong>
                <span>{{ step.detail || step.status || '-' }}</span>
              </li>
            </ol>
          </section>

          <section v-if="message.diff?.length" class="admin-agent__block">
            <h3>预览差异</h3>
            <div
              v-for="(item, index) in message.diff"
              :key="`${message.id}-diff-${index}`"
              class="admin-agent__diff"
            >
              <strong>{{ item.field || '变更项' }}</strong>
              <span>{{ item.description || formatValue(item.afterValue) }}</span>
            </div>
          </section>

          <section v-if="message.artifacts?.length" class="admin-agent__block">
            <h3>结构化结果</h3>
            <details
              v-for="(artifact, index) in message.artifacts"
              :key="`${message.id}-artifact-${index}`"
            >
              <summary>{{ artifact.title || artifact.type || `结果 ${index + 1}` }}</summary>
              <pre>{{ formatValue(artifact.data) }}</pre>
            </details>
          </section>

          <section v-if="message.confirmation" class="admin-agent__confirmation">
            <strong>需要强确认</strong>
            <span>请输入：{{ message.confirmation.requiredText }}</span>
          </section>
        </article>
      </main>

      <section v-if="pendingConfirmation" class="admin-agent__pending">
        <div>
          <strong>待确认动作</strong>
          <span>{{ pendingConfirmation.prompt || `请输入：${pendingConfirmation.requiredText}` }}</span>
        </div>
        <el-input
          v-model="confirmationText"
          :placeholder="pendingConfirmation.requiredText"
          :disabled="running"
          clearable
        />
        <div class="admin-agent__pending-actions">
          <el-button :disabled="running" @click="cancelPending">取消</el-button>
          <el-button type="danger" :loading="running" @click="confirmPending">确认执行</el-button>
        </div>
      </section>

      <footer class="admin-agent__composer">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="例如：查最近5条操作日志"
          :disabled="running"
          @keydown.enter.exact.prevent="handleSubmit"
        />
        <el-button type="primary" :icon="Promotion" :loading="running" @click="handleSubmit">
          发送
        </el-button>
      </footer>
    </section>
  </el-drawer>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import { Cpu, Promotion } from '@element-plus/icons-vue'
import { agentChatApi } from '@/api/agentChat'

const examples = [
  '查最近5条操作日志',
  '清理30天前操作日志'
]

const drawerVisible = ref(false)
const running = ref(false)
const draft = ref('')
const confirmationText = ref('')
const pendingConfirmation = ref(null)
const messageListRef = ref(null)
const sessionId = ref(createSessionId())
const messages = ref([
  {
    id: 'welcome',
    role: 'agent',
    text: '我现在只连接后端统一智能体接口。你可以查询后台状态；写入动作会先由后端生成预览并要求强确认。',
    status: 'answered',
    plan: [],
    diff: [],
    artifacts: [],
    confirmation: null
  }
])

async function handleSubmit() {
  const content = draft.value.trim()
  if (!content || running.value) return

  draft.value = ''
  pushMessage({ role: 'user', text: content })
  await sendToAgent({ message: content })
}

async function confirmPending() {
  if (!pendingConfirmation.value || running.value) return

  const content = confirmationText.value.trim()
  pushMessage({ role: 'user', text: `确认：${content || '(空)'}` })
  await sendToAgent({
    auditId: pendingConfirmation.value.auditId,
    confirmationText: content
  })
  confirmationText.value = ''
}

async function cancelPending() {
  if (!pendingConfirmation.value || running.value) return

  pushMessage({ role: 'user', text: '取消' })
  await sendToAgent({
    auditId: pendingConfirmation.value.auditId,
    message: '取消'
  })
  confirmationText.value = ''
}

async function sendToAgent(payload) {
  running.value = true
  try {
    const response = await agentChatApi.sendMessage({
      sessionId: sessionId.value,
      ...payload
    })
    pushAgentResponse(response)
  } catch (error) {
    pendingConfirmation.value = null
    pushMessage({
      role: 'agent',
      text: error?.message || '智能体接口暂时不可用',
      status: 'error',
      plan: [],
      diff: [],
      artifacts: [],
      confirmation: null
    })
  } finally {
    running.value = false
  }
}

function pushAgentResponse(response = {}) {
  pushMessage({
    role: 'agent',
    text: response.answer || '后端没有返回可展示内容。',
    status: response.status || 'answered',
    plan: Array.isArray(response.plan) ? response.plan : [],
    diff: Array.isArray(response.diff) ? response.diff : [],
    artifacts: Array.isArray(response.artifacts) ? response.artifacts : [],
    confirmation: response.confirmation || null
  })

  pendingConfirmation.value = response.status === 'confirm_required' && response.confirmation
    ? response.confirmation
    : null
}

function pushMessage(message) {
  messages.value.push({
    id: `${Date.now()}-${messages.value.length}`,
    status: '',
    plan: [],
    diff: [],
    artifacts: [],
    confirmation: null,
    ...message
  })
  scrollToBottom()
}

function submitExample(example) {
  draft.value = example
  handleSubmit()
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

function formatValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'string') return value
  return JSON.stringify(value, null, 2)
}

function statusText(status) {
  const textMap = {
    answered: '已回答',
    confirm_required: '待确认',
    executed: '已执行',
    cancelled: '已取消',
    rejected: '已拒绝',
    error: '异常'
  }
  return textMap[status] || status
}

function statusTagType(status) {
  if (status === 'executed' || status === 'answered') return 'success'
  if (status === 'confirm_required') return 'warning'
  if (status === 'error' || status === 'rejected') return 'danger'
  return 'info'
}

function createSessionId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return `admin-agent-${crypto.randomUUID()}`
  }
  return `admin-agent-${Date.now()}`
}
</script>

<style scoped>
.admin-agent__trigger {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 20;
  box-shadow: 0 14px 34px color-mix(in srgb, var(--cn-color-brand-primary) 28%, transparent);
}

.admin-agent {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto auto;
  gap: var(--cn-space-4);
  height: 100%;
  min-height: 0;
}

.admin-agent__intro {
  display: grid;
  gap: var(--cn-space-1);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--cn-color-brand-primary) 18%, transparent), transparent 36%),
    var(--cn-color-bg-surface-muted);
}

.admin-agent__intro p,
.admin-agent__intro h2,
.admin-agent__intro span,
.admin-agent__message-meta,
.admin-agent__message-text,
.admin-agent__block h3 {
  margin: 0;
}

.admin-agent__intro p {
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 750;
}

.admin-agent__intro h2 {
  color: var(--cn-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.admin-agent__intro span {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.admin-agent__examples {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.admin-agent__examples button {
  appearance: none;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  padding: 7px 10px;
}

.admin-agent__examples button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.admin-agent__messages {
  display: grid;
  align-content: start;
  gap: var(--cn-space-3);
  min-height: 0;
  overflow-y: auto;
  padding-right: var(--cn-space-1);
}

.admin-agent__message {
  display: grid;
  gap: var(--cn-space-2);
  max-width: 92%;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.admin-agent__message--user {
  justify-self: end;
  background: var(--cn-color-brand-soft);
}

.admin-agent__message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.admin-agent__message-text {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.admin-agent__block,
.admin-agent__confirmation,
.admin-agent__pending {
  display: grid;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-sm);
  background: var(--cn-color-bg-surface-muted);
}

.admin-agent__block h3 {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.admin-agent__block ol {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
  padding-left: 18px;
}

.admin-agent__block li,
.admin-agent__diff {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.admin-agent__block li strong,
.admin-agent__diff strong,
.admin-agent__confirmation strong,
.admin-agent__pending strong {
  color: var(--cn-color-text-primary);
}

.admin-agent__block li span,
.admin-agent__diff span,
.admin-agent__confirmation span,
.admin-agent__pending span {
  display: block;
  color: var(--cn-color-text-secondary);
}

.admin-agent__block details {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-sm);
  padding: var(--cn-space-2);
  background: var(--cn-color-bg-surface);
}

.admin-agent__block summary {
  cursor: pointer;
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 700;
}

.admin-agent__block pre {
  max-height: 180px;
  overflow: auto;
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  white-space: pre-wrap;
}

.admin-agent__confirmation,
.admin-agent__pending {
  border: 1px solid color-mix(in srgb, var(--cn-color-danger) 28%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-danger) 8%, var(--cn-color-bg-surface));
}

.admin-agent__pending-actions,
.admin-agent__composer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.admin-agent__composer {
  align-items: flex-end;
}

.admin-agent__composer :deep(.el-textarea) {
  flex: 1;
}

@media (max-width: 768px) {
  .admin-agent__trigger {
    right: 16px;
    bottom: 16px;
  }
}
</style>
