<template>
  <el-tooltip content="打开管理员智能体" placement="left">
    <el-button
      class="admin-agent__trigger"
      type="primary"
      circle
      size="large"
      :icon="Cpu"
      aria-label="打开管理员智能体"
      @click="openWorkspace"
    />
  </el-tooltip>

  <el-dialog
    v-model="workspaceVisible"
    class="admin-agent-dialog"
    title="智能体工作台"
    fullscreen
    append-to-body
    :show-close="false"
    :close-on-click-modal="false"
    @opened="handleWorkspaceOpened"
  >
    <template #header>
      <header class="admin-agent__topbar">
        <div class="admin-agent__brand">
          <span class="admin-agent__brand-icon"><el-icon><Cpu /></el-icon></span>
        <div>
          <h1>智能体工作台</h1>
          <span>统一运行时</span>
        </div>
      </div>

        <el-radio-group v-model="workspaceMode" class="admin-agent__mode-switch" size="small" aria-label="工作台模式">
          <el-radio-button label="chat">对话</el-radio-button>
          <el-radio-button label="tasks">持久任务</el-radio-button>
        </el-radio-group>

        <div class="admin-agent__topbar-actions">
          <el-tooltip v-if="workspaceMode === 'chat' && isCompact" content="会话列表" placement="bottom">
            <el-button
              text
              :icon="Menu"
              aria-label="打开会话列表"
              @click="toggleSessionPanel"
            />
          </el-tooltip>
          <el-tooltip v-if="workspaceMode === 'chat'" content="执行详情" placement="bottom">
            <el-button
              text
              :type="inspectorVisible ? 'primary' : ''"
              :icon="DataAnalysis"
              aria-label="切换执行详情"
              @click="toggleInspector"
            />
          </el-tooltip>
          <el-tooltip v-if="workspaceMode === 'chat'" content="导出当前会话" placement="bottom">
            <el-button
              text
              :icon="Download"
              :disabled="!messages.length"
              aria-label="导出当前会话"
              @click="exportCurrentSession"
            />
          </el-tooltip>
          <el-tooltip content="关闭" placement="bottom">
            <el-button text :icon="Close" aria-label="关闭智能体工作台" @click="workspaceVisible = false" />
          </el-tooltip>
        </div>
      </header>
    </template>

    <section
      class="admin-agent__workspace"
      :class="{ 'admin-agent__workspace--inspector-hidden': !inspectorVisible && !isCompact, 'is-task-mode': workspaceMode === 'tasks' }"
    >
      <template v-if="workspaceMode === 'chat'">
      <aside
        class="admin-agent__sessions"
        :class="{ 'is-open': sessionPanelOpen }"
        aria-label="会话列表"
      >
        <header class="admin-agent__sessions-header">
          <strong>会话</strong>
          <el-tooltip content="新建会话" placement="right">
            <el-button
              type="primary"
              circle
              size="small"
              :icon="Plus"
              aria-label="新建会话"
              @click="createNewSession"
            />
          </el-tooltip>
        </header>

        <el-input
          v-model="sessionSearch"
          class="admin-agent__session-search"
          :prefix-icon="Search"
          clearable
          aria-label="搜索会话"
          placeholder="搜索会话"
        />

        <nav class="admin-agent__session-list" aria-label="历史会话">
          <div
            v-for="session in filteredSessions"
            :key="session.id"
            class="admin-agent__session-item"
            :class="{ 'is-active': session.id === currentSession?.id }"
          >
            <button type="button" class="admin-agent__session-main" @click="selectSession(session.id)">
              <span class="admin-agent__session-title">{{ session.title }}</span>
              <span class="admin-agent__session-meta">
                <time>{{ formatSessionTime(session.updatedAt) }}</time>
                <i v-if="runningSessionId === session.id" class="admin-agent__running-dot" />
                <span v-else>{{ session.messages.length }} 条</span>
              </span>
            </button>
            <el-tooltip content="删除会话" placement="right">
              <el-button
                class="admin-agent__session-delete"
                text
                :icon="Delete"
                :disabled="runningSessionId === session.id"
                :aria-label="`删除会话 ${session.title}`"
                @click="removeSession(session)"
              />
            </el-tooltip>
          </div>
        </nav>

        <footer class="admin-agent__sessions-footer">
          <span>{{ sessions.length }} / {{ MAX_WORKSPACE_SESSIONS }}</span>
          <span v-if="pendingSessionCount">待确认 {{ pendingSessionCount }}</span>
        </footer>
      </aside>

      <main class="admin-agent__conversation">
        <header class="admin-agent__conversation-header">
          <div>
            <h2>{{ currentSession?.title || '新会话' }}</h2>
            <span>{{ conversationSummary }}</span>
          </div>
          <el-tag v-if="runningCurrentSession" type="primary" effect="plain">处理中</el-tag>
          <el-tag v-else-if="pendingConfirmation" type="warning" effect="plain">待确认</el-tag>
        </header>

        <section
          ref="messageListRef"
          class="admin-agent__messages"
          role="log"
          aria-label="智能体会话"
          aria-live="polite"
        >
          <div v-if="!messages.length" class="admin-agent__empty">
            <span class="admin-agent__empty-icon"><el-icon><ChatDotRound /></el-icon></span>
            <h2>新会话</h2>
            <div class="admin-agent__quick-actions" aria-label="常用任务">
              <el-button
                v-for="action in quickActions"
                :key="action.message"
                :icon="action.icon"
                :disabled="running"
                @click="submitQuickAction(action.message)"
              >
                {{ action.label }}
              </el-button>
            </div>
          </div>

          <article
            v-for="message in messages"
            :key="message.id"
            class="admin-agent__message"
            :class="[
              `admin-agent__message--${message.role}`,
              { 'is-selected': selectedMessage?.id === message.id }
            ]"
            :tabindex="message.role === 'agent' ? 0 : undefined"
            @click="selectInspectorMessage(message)"
            @keydown.enter="selectInspectorMessage(message)"
          >
            <header class="admin-agent__message-header">
              <div>
                <strong>{{ message.role === 'user' ? '你' : 'Agent' }}</strong>
                <time>{{ formatMessageTime(message.createdAt) }}</time>
              </div>
              <el-tag
                v-if="message.status"
                size="small"
                :type="statusTagType(message.status)"
                effect="plain"
              >
                {{ statusText(message.status) }}
              </el-tag>
            </header>

            <div
              v-if="message.role === 'agent'"
              class="admin-agent__message-text markdown-content"
              v-html="renderMessage(message.text)"
            />
            <p v-else class="admin-agent__message-text">{{ message.text }}</p>

            <footer v-if="message.role === 'agent'" class="admin-agent__message-footer">
              <span v-if="message.toolName" class="admin-agent__tool-name">
                <el-icon><Tools /></el-icon>{{ message.toolName }}
              </span>
              <span v-else />
              <div class="admin-agent__message-actions">
                <el-tooltip content="复制回复" placement="top">
                  <el-button
                    text
                    :icon="CopyDocument"
                    :aria-label="`复制回复 ${message.id}`"
                    @click.stop="copyMessage(message)"
                  />
                </el-tooltip>
                <el-tooltip content="重试" placement="top">
                  <el-button
                    text
                    :icon="Refresh"
                    :disabled="running"
                    :aria-label="`重试回复 ${message.id}`"
                    @click.stop="retryMessage(message)"
                  />
                </el-tooltip>
              </div>
            </footer>
          </article>

          <article v-if="runningCurrentSession" class="admin-agent__message admin-agent__message--agent admin-agent__thinking">
            <header class="admin-agent__message-header"><strong>Agent</strong></header>
            <div class="admin-agent__thinking-bars" aria-label="正在处理">
              <span /><span /><span />
            </div>
          </article>
        </section>

        <section v-if="pendingConfirmation" class="admin-agent__pending" aria-live="assertive">
          <div class="admin-agent__pending-title">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>待确认动作</strong>
              <span>{{ pendingConfirmation.prompt || pendingConfirmation.requiredText }}</span>
            </div>
          </div>
          <el-input
            v-model="confirmationText"
            :placeholder="pendingConfirmation.requiredText"
            :disabled="running"
            aria-label="确认文本"
            clearable
          />
          <div class="admin-agent__pending-actions">
            <el-button :disabled="running" @click="cancelPending">取消</el-button>
            <el-button
              type="danger"
              :loading="running"
              :disabled="!confirmationMatches"
              @click="confirmPending"
            >
              确认执行
            </el-button>
          </div>
        </section>

        <footer class="admin-agent__composer">
          <el-input
            ref="composerRef"
            v-model="draft"
            type="textarea"
            :rows="3"
            resize="none"
            maxlength="1000"
            aria-label="管理员请求"
            placeholder="输入管理员请求"
            :disabled="running || Boolean(pendingConfirmation)"
            @keydown.enter.exact.prevent="handleSubmit"
          />
          <div class="admin-agent__composer-actions">
            <span>{{ draft.length }} / 1000</span>
            <el-button
              v-if="runningCurrentSession"
              :icon="VideoPause"
              aria-label="停止等待"
              @click="stopRequest"
            >
              停止
            </el-button>
            <el-button
              v-else
              type="primary"
              :icon="Promotion"
              :disabled="!canSubmit"
              @click="handleSubmit"
            >
              发送
            </el-button>
          </div>
        </footer>
      </main>

      <AgentWorkspaceInspector
        v-if="inspectorVisible || isCompact"
        class="admin-agent__inspector"
        :class="{ 'is-open': inspectorPanelOpen }"
        :message="selectedMessage"
      />

      <button
        v-if="isCompact && (sessionPanelOpen || inspectorPanelOpen)"
        type="button"
        class="admin-agent__panel-backdrop"
        aria-label="关闭侧边面板"
        @click="closeCompactPanels"
      />
      </template>

      <AgentTaskWorkspace
        v-else
        class="admin-agent__task-workspace"
        :active="workspaceVisible && workspaceMode === 'tasks'"
        :session-id="currentSession?.id || ''"
      />
    </section>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  ChatDotRound,
  Close,
  CopyDocument,
  Cpu,
  DataAnalysis,
  Delete,
  Download,
  Menu,
  Plus,
  Promotion,
  Refresh,
  Search,
  Tools,
  VideoPause,
  WarningFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentChatApi } from '@/api/agentChat'
import { renderMarkdown } from '@/utils/markdown'
import AgentTaskWorkspace from './AgentTaskWorkspace.vue'
import AgentWorkspaceInspector from './AgentWorkspaceInspector.vue'
import {
  MAX_MESSAGES_PER_SESSION,
  MAX_WORKSPACE_SESSIONS,
  WORKSPACE_STORAGE_KEY,
  agentMessageFromResponse,
  createWorkspaceSession,
  createWorkspaceState,
  deriveSessionTitle,
  normalizeWorkspaceState,
  sessionToMarkdown,
  userMessageFromText,
} from './agentWorkspaceState.js'

const quickActions = [
  { label: '运行时状态', message: '查询后端智能体运行时状态', icon: DataAnalysis },
  { label: '最近操作日志', message: '查最近5条操作日志', icon: Search },
  { label: '工具目录', message: '列出当前所有可用工具及风险级别', icon: Tools },
]

const workspaceVisible = ref(false)
const workspaceMode = ref('chat')
const workspaceState = ref(loadWorkspaceState())
const sessionSearch = ref('')
const draft = ref('')
const confirmationText = ref('')
const messageListRef = ref(null)
const composerRef = ref(null)
const selectedMessageId = ref('')
const runningSessionId = ref('')
const inspectorVisible = ref(true)
const sessionPanelOpen = ref(false)
const inspectorPanelOpen = ref(false)
const isCompact = ref(false)
let activeController = null
let storageWarningShown = false

const sessions = computed(() => workspaceState.value.sessions)
const currentSession = computed(() => (
  sessions.value.find((session) => session.id === workspaceState.value.currentSessionId)
  || sessions.value[0]
  || null
))
const messages = computed(() => currentSession.value?.messages || [])
const pendingConfirmation = computed(() => currentSession.value?.pendingConfirmation || null)
const pendingSessionCount = computed(() => sessions.value.filter((session) => session.pendingConfirmation).length)
const running = computed(() => Boolean(runningSessionId.value))
const runningCurrentSession = computed(() => runningSessionId.value === currentSession.value?.id)
const canSubmit = computed(() => Boolean(draft.value.trim()) && !running.value && !pendingConfirmation.value)
const confirmationMatches = computed(() => {
  const requiredText = pendingConfirmation.value?.requiredText || ''
  return Boolean(requiredText) && confirmationText.value.trim() === requiredText
})
const filteredSessions = computed(() => {
  const keyword = sessionSearch.value.trim().toLowerCase()
  if (!keyword) return sessions.value
  return sessions.value.filter((session) => session.title.toLowerCase().includes(keyword))
})
const selectedMessage = computed(() => {
  const selected = messages.value.find((message) => message.id === selectedMessageId.value)
  if (selected?.role === 'agent') return selected
  return [...messages.value].reverse().find((message) => message.role === 'agent') || null
})
const conversationSummary = computed(() => {
  const agentMessages = messages.value.filter((message) => message.role === 'agent')
  const tools = new Set(agentMessages.map((message) => message.toolName).filter(Boolean))
  if (!messages.value.length) return '空会话'
  return `${messages.value.length} 条消息 · ${tools.size} 个工具`
})

watch(workspaceState, (value) => {
  if (typeof localStorage === 'undefined') return
  try {
    localStorage.setItem(WORKSPACE_STORAGE_KEY, JSON.stringify(normalizeWorkspaceState(value)))
  } catch {
    if (!storageWarningShown) {
      storageWarningShown = true
      ElMessage.warning('会话记录已达到浏览器存储上限')
    }
  }
}, { deep: true })

watch(() => workspaceState.value.currentSessionId, () => {
  selectedMessageId.value = ''
  confirmationText.value = ''
  draft.value = ''
  scrollToBottom()
})

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  activeController?.abort()
  window.removeEventListener('resize', updateViewport)
})

function loadWorkspaceState() {
  if (typeof localStorage === 'undefined') return createWorkspaceState()
  try {
    const stored = localStorage.getItem(WORKSPACE_STORAGE_KEY)
    return stored ? normalizeWorkspaceState(JSON.parse(stored)) : createWorkspaceState()
  } catch {
    return createWorkspaceState()
  }
}

function openWorkspace() {
  workspaceVisible.value = true
}

function handleWorkspaceOpened() {
  scrollToBottom()
  nextTick(() => composerRef.value?.focus?.())
}

function createNewSession() {
  const session = createWorkspaceSession()
  workspaceState.value.sessions = [session, ...sessions.value].slice(0, MAX_WORKSPACE_SESSIONS)
  workspaceState.value.currentSessionId = session.id
  closeCompactPanels()
  nextTick(() => composerRef.value?.focus?.())
}

async function removeSession(session) {
  try {
    await ElMessageBox.confirm(`确认删除会话“${session.title}”？`, '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  const remaining = sessions.value.filter((item) => item.id !== session.id)
  if (!remaining.length) remaining.push(createWorkspaceSession())
  workspaceState.value.sessions = remaining
  if (workspaceState.value.currentSessionId === session.id) {
    workspaceState.value.currentSessionId = remaining[0].id
  }
}

function selectSession(sessionId) {
  workspaceState.value.currentSessionId = sessionId
  closeCompactPanels()
}

async function handleSubmit() {
  const content = draft.value.trim()
  if (!content || !canSubmit.value) return
  draft.value = ''
  await sendUserMessage(content)
}

async function submitQuickAction(message) {
  if (running.value) return
  await sendUserMessage(message)
}

async function sendUserMessage(content) {
  const targetSessionId = currentSession.value?.id
  if (!targetSessionId) return
  appendMessage(targetSessionId, userMessageFromText(content))
  await sendToAgent({ message: content }, targetSessionId)
}

async function confirmPending() {
  if (!pendingConfirmation.value || running.value || !confirmationMatches.value) return
  const targetSessionId = currentSession.value.id
  const content = confirmationText.value.trim()
  appendMessage(targetSessionId, userMessageFromText(`确认：${content}`))
  await sendToAgent({
    auditId: pendingConfirmation.value.auditId,
    confirmationText: content,
  }, targetSessionId)
  confirmationText.value = ''
}

async function cancelPending() {
  if (!pendingConfirmation.value || running.value) return
  const targetSessionId = currentSession.value.id
  appendMessage(targetSessionId, userMessageFromText('取消'))
  await sendToAgent({
    auditId: pendingConfirmation.value.auditId,
    message: '取消',
  }, targetSessionId)
  confirmationText.value = ''
}

async function sendToAgent(payload, targetSessionId) {
  if (running.value) return
  const targetSession = findSession(targetSessionId)
  if (!targetSession) return

  activeController = new AbortController()
  runningSessionId.value = targetSessionId
  try {
    const response = await agentChatApi.sendMessage({
      sessionId: targetSessionId,
      ...payload,
    }, {
      signal: activeController.signal,
      silent: true,
    })
    const message = agentMessageFromResponse(response)
    appendMessage(targetSessionId, message)
    setPendingConfirmation(
      targetSessionId,
      response?.status === 'confirm_required' ? response.confirmation : null,
    )
    if (currentSession.value?.id === targetSessionId) {
      selectedMessageId.value = message.id
      inspectorVisible.value = true
      scrollToBottom()
    }
  } catch (error) {
    if (isCancelled(error)) return
    const message = agentMessageFromResponse({
      status: 'error',
      answer: error?.message || '智能体接口暂时不可用',
      errorCode: error?.code,
      errorMessage: error?.message,
    })
    appendMessage(targetSessionId, message)
    setPendingConfirmation(targetSessionId, null)
    if (currentSession.value?.id === targetSessionId) selectedMessageId.value = message.id
  } finally {
    activeController = null
    runningSessionId.value = ''
  }
}

function stopRequest() {
  activeController?.abort()
  ElMessage.info('已停止等待本次响应')
}

function appendMessage(sessionId, message) {
  const session = findSession(sessionId)
  if (!session) return
  session.messages = [...session.messages, message].slice(-MAX_MESSAGES_PER_SESSION)
  session.updatedAt = new Date().toISOString()
  if (message.role === 'user' && (session.title === '新会话' || session.messages.length <= 1)) {
    session.title = deriveSessionTitle(message.text)
  }
  workspaceState.value.sessions = [
    session,
    ...sessions.value.filter((item) => item.id !== sessionId),
  ].slice(0, MAX_WORKSPACE_SESSIONS)
}

function setPendingConfirmation(sessionId, confirmation) {
  const session = findSession(sessionId)
  if (session) session.pendingConfirmation = confirmation || null
}

function findSession(sessionId) {
  return sessions.value.find((session) => session.id === sessionId)
}

function selectInspectorMessage(message) {
  if (message.role !== 'agent') return
  selectedMessageId.value = message.id
  inspectorVisible.value = true
  if (isCompact.value) {
    inspectorPanelOpen.value = true
    sessionPanelOpen.value = false
  }
}

async function copyMessage(message) {
  try {
    await copyText(message.text || '')
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function copyText(value) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const copied = document.execCommand('copy')
  textarea.remove()
  if (!copied) throw new Error('copy failed')
}

async function retryMessage(message) {
  const index = messages.value.findIndex((item) => item.id === message.id)
  const previousUserMessage = [...messages.value.slice(0, index)]
    .reverse()
    .find((item) => item.role === 'user')
  if (previousUserMessage) await sendUserMessage(previousUserMessage.text)
}

function exportCurrentSession() {
  if (!currentSession.value || !messages.value.length) return
  const blob = new Blob([sessionToMarkdown(currentSession.value)], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${safeFileName(currentSession.value.title)}.md`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function renderMessage(value) {
  return renderMarkdown(value || '')
}

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

function toggleSessionPanel() {
  sessionPanelOpen.value = !sessionPanelOpen.value
  inspectorPanelOpen.value = false
}

function toggleInspector() {
  if (isCompact.value) {
    inspectorPanelOpen.value = !inspectorPanelOpen.value
    sessionPanelOpen.value = false
  } else {
    inspectorVisible.value = !inspectorVisible.value
  }
}

function closeCompactPanels() {
  sessionPanelOpen.value = false
  inspectorPanelOpen.value = false
}

function updateViewport() {
  isCompact.value = window.innerWidth <= 1040
  if (!isCompact.value) closeCompactPanels()
}

function formatSessionTime(value) {
  const timestamp = Date.parse(value)
  if (!Number.isFinite(timestamp)) return '-'
  const date = new Date(timestamp)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function formatMessageTime(value) {
  const timestamp = Date.parse(value)
  if (!Number.isFinite(timestamp)) return ''
  return new Date(timestamp).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function statusText(status) {
  const textMap = {
    answered: '已回答',
    confirm_required: '待确认',
    executed: '已执行',
    cancelled: '已取消',
    rejected: '已拒绝',
    error: '异常',
  }
  return textMap[status] || status
}

function statusTagType(status) {
  if (status === 'executed' || status === 'answered') return 'success'
  if (status === 'confirm_required') return 'warning'
  if (status === 'error' || status === 'rejected') return 'danger'
  return 'info'
}

function isCancelled(error) {
  return error?.code === 'ERR_CANCELED'
    || error?.cause?.code === 'ERR_CANCELED'
    || error?.message === 'canceled'
}

function safeFileName(value) {
  return (value || '智能体会话').replace(/[\\/:*?"<>|]/g, '-').slice(0, 48)
}
</script>

<style scoped>
.admin-agent__trigger {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 20;
  width: 48px;
  height: 48px;
  box-shadow: 0 14px 32px color-mix(in srgb, var(--cn-color-brand-primary) 24%, transparent);
}

:global(.admin-agent-dialog) {
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 0;
  overflow: hidden;
  background: var(--cn-color-bg-page);
}

:global(.admin-agent-dialog .el-dialog__header) {
  margin: 0;
  padding: 0;
  border-bottom: 0;
}

:global(.admin-agent-dialog .el-dialog__body) {
  height: calc(100vh - 64px);
  padding: 0;
  overflow: hidden;
}

.admin-agent__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  height: 64px;
  padding: 0 var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.admin-agent__brand,
.admin-agent__topbar-actions,
.admin-agent__brand > div {
  display: flex;
  align-items: center;
  min-width: 0;
}

.admin-agent__brand {
  gap: var(--cn-space-3);
}

.admin-agent__brand > div {
  align-items: flex-start;
  flex-direction: column;
}

.admin-agent__brand-icon,
.admin-agent__empty-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.admin-agent__brand h1,
.admin-agent__brand span,
.admin-agent__conversation-header h2,
.admin-agent__conversation-header span,
.admin-agent__message-text,
.admin-agent__message-header strong,
.admin-agent__message-header time,
.admin-agent__empty h2 {
  margin: 0;
}

.admin-agent__brand h1 {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  line-height: 1.25;
}

.admin-agent__brand span {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.admin-agent__topbar-actions {
  gap: var(--cn-space-1);
}

.admin-agent__mode-switch {
  flex: 0 0 auto;
}

.admin-agent__topbar-actions :deep(.el-button) {
  width: 36px;
  height: 36px;
  margin: 0;
}

.admin-agent__workspace {
  position: relative;
  display: grid;
  grid-template-columns: 272px minmax(0, 1fr) 340px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--cn-color-bg-page);
}

.admin-agent__workspace.is-task-mode {
  display: block;
  overflow: hidden;
}

.admin-agent__task-workspace {
  height: 100%;
}

.admin-agent__workspace--inspector-hidden {
  grid-template-columns: 272px minmax(0, 1fr);
}

.admin-agent__sessions {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  gap: var(--cn-space-3);
  min-width: 0;
  min-height: 0;
  padding: var(--cn-space-4) var(--cn-space-3) var(--cn-space-3);
  border-right: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.admin-agent__sessions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  padding: 0 var(--cn-space-1);
}

.admin-agent__sessions-header strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.admin-agent__session-search :deep(.el-input__wrapper) {
  border-radius: var(--cn-radius-control);
  box-shadow: 0 0 0 1px var(--cn-color-border-subtle) inset;
}

.admin-agent__session-list {
  display: grid;
  align-content: start;
  gap: 4px;
  min-height: 0;
  overflow-y: auto;
}

.admin-agent__session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--cn-radius-control);
}

.admin-agent__session-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.admin-agent__session-item.is-active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 25%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.admin-agent__session-main {
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 10px var(--cn-space-2);
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.admin-agent__session-main:focus-visible,
.admin-agent__message:focus-visible {
  outline: 2px solid var(--cn-color-brand-primary);
  outline-offset: -2px;
}

.admin-agent__session-title {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-agent__session-meta {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 10px;
}

.admin-agent__running-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cn-color-brand-primary);
  animation: admin-agent-pulse 1.2s ease-in-out infinite;
}

.admin-agent__session-delete {
  width: 30px;
  height: 30px;
  opacity: 0;
}

.admin-agent__session-item:hover .admin-agent__session-delete,
.admin-agent__session-delete:focus-visible {
  opacity: 1;
}

.admin-agent__sessions-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 28px;
  padding: var(--cn-space-2) var(--cn-space-1) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-tertiary);
  font-size: 10px;
}

.admin-agent__conversation {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  min-width: 0;
  min-height: 0;
  background: var(--cn-color-bg-page);
}

.admin-agent__conversation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-height: 68px;
  padding: var(--cn-space-3) var(--cn-space-5);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.admin-agent__conversation-header > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.admin-agent__conversation-header h2 {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-agent__conversation-header span {
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
}

.admin-agent__messages {
  display: grid;
  align-content: start;
  gap: var(--cn-space-4);
  min-height: 0;
  overflow-y: auto;
  padding: var(--cn-space-6) clamp(var(--cn-space-4), 4vw, var(--cn-space-10));
}

.admin-agent__empty {
  display: grid;
  place-items: center;
  align-self: center;
  gap: var(--cn-space-3);
  min-height: 320px;
  text-align: center;
}

.admin-agent__empty-icon {
  width: 48px;
  height: 48px;
  font-size: 22px;
}

.admin-agent__empty h2 {
  color: var(--cn-color-text-primary);
  font-size: 18px;
}

.admin-agent__quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-2);
  width: min(100%, 620px);
}

.admin-agent__quick-actions :deep(.el-button) {
  min-width: 0;
  margin: 0;
  overflow: hidden;
}

.admin-agent__quick-actions :deep(.el-button > span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-agent__message {
  display: grid;
  gap: var(--cn-space-3);
  width: min(86%, 760px);
  padding: var(--cn-space-3) var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-xs);
}

.admin-agent__message--user {
  justify-self: end;
  width: min(78%, 680px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
}

.admin-agent__message--agent {
  justify-self: start;
  cursor: pointer;
}

.admin-agent__message--agent.is-selected {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 52%, var(--cn-color-border-subtle));
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--cn-color-brand-primary) 9%, transparent);
}

.admin-agent__message-header,
.admin-agent__message-header > div,
.admin-agent__message-footer,
.admin-agent__message-actions,
.admin-agent__tool-name {
  display: flex;
  align-items: center;
}

.admin-agent__message-header,
.admin-agent__message-footer {
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.admin-agent__message-header > div,
.admin-agent__message-actions,
.admin-agent__tool-name {
  gap: var(--cn-space-2);
}

.admin-agent__message-header strong {
  color: var(--cn-color-text-primary);
  font-size: 12px;
}

.admin-agent__message-header time {
  color: var(--cn-color-text-tertiary);
  font-size: 10px;
}

.admin-agent__message-text {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.72;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.admin-agent__message-text.markdown-content :deep(> :first-child) {
  margin-top: 0;
}

.admin-agent__message-text.markdown-content :deep(> :last-child) {
  margin-bottom: 0;
}

.admin-agent__message-text.markdown-content {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  font-size: 14px;
}

.admin-agent__message-text.markdown-content :deep(.markdown-heading) {
  margin: var(--cn-space-4) 0 var(--cn-space-2);
  padding-bottom: 0;
  border-bottom: 0;
  font-size: 15px;
}

.admin-agent__message-text.markdown-content :deep(.markdown-paragraph) {
  margin: var(--cn-space-2) 0;
  line-height: 1.72;
  text-align: left;
}

.admin-agent__message-text.markdown-content :deep(.markdown-list) {
  margin: var(--cn-space-2) 0;
}

.admin-agent__message-footer {
  min-height: 28px;
  padding-top: var(--cn-space-2);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.admin-agent__tool-name {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-agent__message-actions {
  opacity: 0;
}

.admin-agent__message:hover .admin-agent__message-actions,
.admin-agent__message:focus-within .admin-agent__message-actions {
  opacity: 1;
}

.admin-agent__message-actions :deep(.el-button) {
  width: 28px;
  height: 28px;
  margin: 0;
}

.admin-agent__thinking {
  width: 104px;
  cursor: default;
}

.admin-agent__thinking-bars {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 18px;
}

.admin-agent__thinking-bars span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cn-color-brand-primary);
  animation: admin-agent-thinking 1s ease-in-out infinite;
}

.admin-agent__thinking-bars span:nth-child(2) { animation-delay: 120ms; }
.admin-agent__thinking-bars span:nth-child(3) { animation-delay: 240ms; }

.admin-agent__pending {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(220px, 0.8fr) auto;
  align-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) var(--cn-space-5);
  border-top: 1px solid color-mix(in srgb, var(--cn-color-danger) 28%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-danger) 7%, var(--cn-color-bg-surface));
}

.admin-agent__pending-title,
.admin-agent__pending-actions {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
}

.admin-agent__pending-title > .el-icon {
  color: var(--cn-color-danger);
  font-size: 20px;
}

.admin-agent__pending-title > div {
  display: grid;
  gap: 2px;
}

.admin-agent__pending-title strong {
  color: var(--cn-color-text-primary);
  font-size: 12px;
}

.admin-agent__pending-title span {
  color: var(--cn-color-text-secondary);
  font-size: 11px;
}

.admin-agent__composer {
  display: grid;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3) var(--cn-space-5) var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.admin-agent__composer :deep(.el-textarea__inner) {
  min-height: 72px !important;
  border-radius: var(--cn-radius-control);
  line-height: 1.6;
}

.admin-agent__composer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.admin-agent__composer-actions > span {
  color: var(--cn-color-text-tertiary);
  font-size: 10px;
}

.admin-agent__inspector {
  min-width: 0;
  min-height: 0;
}

.admin-agent__panel-backdrop {
  display: none;
}

@keyframes admin-agent-thinking {
  0%, 100% { opacity: 0.35; transform: translateY(1px); }
  50% { opacity: 1; transform: translateY(-2px); }
}

@keyframes admin-agent-pulse {
  0%, 100% { opacity: 0.45; }
  50% { opacity: 1; }
}

@media (max-width: 1180px) {
  .admin-agent__workspace {
    grid-template-columns: 232px minmax(0, 1fr) 300px;
  }

  .admin-agent__workspace--inspector-hidden {
    grid-template-columns: 232px minmax(0, 1fr);
  }

  .admin-agent__messages {
    padding-inline: var(--cn-space-5);
  }
}

@media (max-width: 1040px) {
  .admin-agent__workspace,
  .admin-agent__workspace--inspector-hidden {
    grid-template-columns: minmax(0, 1fr);
  }

  .admin-agent__sessions,
  .admin-agent__inspector {
    position: absolute;
    z-index: 3;
    top: 0;
    bottom: 0;
    display: none;
    width: min(86vw, 340px);
    box-shadow: 18px 0 44px color-mix(in srgb, var(--cn-color-text-primary) 18%, transparent);
  }

  .admin-agent__sessions {
    left: 0;
  }

  .admin-agent__inspector {
    right: 0;
    width: min(90vw, 390px);
    box-shadow: -18px 0 44px color-mix(in srgb, var(--cn-color-text-primary) 18%, transparent);
  }

  .admin-agent__sessions.is-open,
  .admin-agent__inspector.is-open {
    display: grid;
  }

  .admin-agent__panel-backdrop {
    position: absolute;
    z-index: 2;
    inset: 0;
    display: block;
    border: 0;
    background: color-mix(in srgb, var(--cn-color-text-primary) 38%, transparent);
  }
}

@media (max-width: 720px) {
  .admin-agent__trigger {
    right: 16px;
    bottom: 16px;
  }

  .admin-agent__topbar {
    padding-inline: var(--cn-space-3);
  }

  .admin-agent__mode-switch :deep(.el-radio-button__inner) {
    padding-inline: 8px;
  }

  .admin-agent__brand span {
    display: none;
  }

  .admin-agent__brand h1 {
    font-size: 14px;
  }

  .admin-agent__conversation-header,
  .admin-agent__composer {
    padding-inline: var(--cn-space-3);
  }

  .admin-agent__messages {
    gap: var(--cn-space-3);
    padding: var(--cn-space-4) var(--cn-space-3);
  }

  .admin-agent__message,
  .admin-agent__message--user {
    width: min(96%, 760px);
    padding: var(--cn-space-3);
  }

  .admin-agent__quick-actions {
    grid-template-columns: 1fr;
    width: min(100%, 320px);
  }

  .admin-agent__pending {
    grid-template-columns: 1fr;
    padding: var(--cn-space-3);
  }

  .admin-agent__pending-actions {
    justify-content: flex-end;
  }

  .admin-agent__message-actions {
    opacity: 1;
  }

  .admin-agent__session-delete {
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .admin-agent__running-dot,
  .admin-agent__thinking-bars span {
    animation: none;
  }
}
</style>
