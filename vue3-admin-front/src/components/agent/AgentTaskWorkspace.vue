<template>
  <section class="agent-task-workspace" aria-label="持久化任务工作台">
    <aside class="agent-task__rail">
      <header class="agent-task__rail-header">
        <div>
          <span class="agent-task__eyebrow">DURABLE TASKS</span>
          <h2>持久任务</h2>
        </div>
        <el-tooltip content="刷新任务" placement="bottom">
          <el-button
            text
            :icon="Refresh"
            :loading="listLoading"
            aria-label="刷新任务"
            @click="loadTasks"
          />
        </el-tooltip>
      </header>

      <form class="agent-task__create" @submit.prevent="createTask">
        <el-input
          v-model="taskGoal"
          type="textarea"
          :rows="3"
          resize="none"
          maxlength="4000"
          show-word-limit
          aria-label="任务目标"
          placeholder="输入需要分步骤完成的目标"
        />
        <el-button
          type="primary"
          native-type="submit"
          :loading="taskCreating"
          :disabled="!taskGoal.trim()"
          :icon="Plus"
        >
          创建任务
        </el-button>
      </form>

      <div class="agent-task__filter-row">
        <el-select v-model="statusFilter" aria-label="任务状态筛选" placeholder="全部状态">
          <el-option label="全部状态" value="" />
          <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <span class="agent-task__count">{{ tasks.length }}</span>
      </div>

      <div v-if="listError" class="agent-task__error" role="alert">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ listError }}</span>
        <el-button link type="danger" @click="loadTasks">重试</el-button>
      </div>

      <nav v-loading="listLoading" class="agent-task__list" aria-label="最近任务">
        <button
          v-for="task in tasks"
          :key="task.taskId"
          type="button"
          class="agent-task__list-item"
          :class="{ 'is-active': task.taskId === selectedTaskId }"
          @click="selectTask(task.taskId)"
        >
          <span class="agent-task__list-title">{{ task.goal || '未命名任务' }}</span>
          <span class="agent-task__list-meta">
            <el-tag size="small" effect="plain" :type="taskStatusMeta(task.status).type">
              {{ taskStatusMeta(task.status).label }}
            </el-tag>
            <time>{{ formatTime(task.updatedTime || task.createdTime) }}</time>
          </span>
        </button>
        <div v-if="!listLoading && !tasks.length" class="agent-task__empty-list">
          <el-icon><List /></el-icon>
          <span>暂无任务</span>
        </div>
      </nav>
    </aside>

    <main class="agent-task__detail" aria-live="polite">
      <template v-if="selectedTask">
        <header class="agent-task__detail-header">
          <div class="agent-task__detail-heading">
            <div class="agent-task__status-line">
              <el-tag :type="selectedStatusMeta.type" effect="light">{{ selectedStatusMeta.label }}</el-tag>
              <span v-if="selectedTask.taskId" class="agent-task__task-id">{{ selectedTask.taskId }}</span>
            </div>
            <h1>{{ selectedTask.goal || '未命名任务' }}</h1>
            <time>{{ formatTime(selectedTask.createdTime) }}</time>
          </div>
          <div class="agent-task__detail-actions">
            <el-tooltip content="刷新详情" placement="bottom">
              <el-button text :icon="Refresh" :loading="detailLoading" aria-label="刷新详情" @click="refreshSelectedTask" />
            </el-tooltip>
            <el-button v-if="capabilities.canPause" :loading="actionLoading" :icon="VideoPause" @click="pauseSelectedTask">
              暂停
            </el-button>
            <el-button v-if="capabilities.canResume" :loading="actionLoading" :icon="VideoPlay" @click="resumeSelectedTask">
              恢复
            </el-button>
            <el-button v-if="capabilities.canCancel" :loading="actionLoading" :icon="CircleClose" type="danger" plain @click="cancelSelectedTask">
              取消
            </el-button>
          </div>
        </header>

        <section class="agent-task__progress-band" aria-label="任务进度">
          <div class="agent-task__progress-copy">
            <strong>{{ progress.completed }} / {{ progress.total }} 步</strong>
            <span>{{ progress.percent }}%</span>
          </div>
          <el-progress :percentage="progress.percent" :show-text="false" :stroke-width="8" />
        </section>

        <div v-if="detailError" class="agent-task__error agent-task__error--detail" role="alert">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ detailError }}</span>
          <el-button link type="danger" @click="refreshSelectedTask">重试</el-button>
        </div>

        <section v-if="capabilities.canConfirm" class="agent-task__intervention agent-task__intervention--confirm" aria-label="强确认">
          <div class="agent-task__intervention-heading">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>{{ selectedTask.confirmation?.prompt || '确认执行待处理写步骤' }}</strong>
            </div>
          </div>
          <el-input
            v-model="confirmationText"
            :placeholder="selectedTask.confirmation?.requiredText"
            aria-label="任务确认文本"
            clearable
          />
          <el-button
            type="danger"
            :loading="actionLoading"
            :disabled="!confirmationMatches"
            @click="confirmSelectedTask"
          >
            确认执行
          </el-button>
        </section>

        <section v-if="capabilities.canSubmitInput" class="agent-task__intervention" aria-label="补充任务输入">
          <div class="agent-task__intervention-heading">
            <el-icon><EditPen /></el-icon>
            <div>
              <strong>需要补充输入</strong>
            </div>
          </div>
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="4"
            resize="none"
            maxlength="4000"
            aria-label="任务补充输入 JSON"
            placeholder="例如 {&quot;environment&quot;:&quot;staging&quot;}"
          />
          <div class="agent-task__intervention-footer">
            <span v-if="inputError" class="agent-task__field-error" role="alert">{{ inputError }}</span>
            <el-button type="primary" :loading="actionLoading" :disabled="!inputText.trim()" @click="submitTaskInput">
              提交输入
            </el-button>
          </div>
        </section>

        <section v-if="terminalReason" class="agent-task__terminal" :class="`is-${selectedTask.status.toLowerCase()}`">
          <strong>{{ selectedTask.status === 'REQUIRES_REVIEW' ? '需要人工复核' : '任务结果' }}</strong>
          <span>{{ terminalReason }}</span>
        </section>

        <section class="agent-task__section" aria-labelledby="agent-task-steps-title">
          <header class="agent-task__section-header">
            <h2 id="agent-task-steps-title">执行步骤</h2>
            <span>{{ selectedTask.steps.length }}</span>
          </header>
          <el-table
            v-if="selectedTask.steps.length"
            class="agent-task__steps-table"
            :data="selectedTask.steps"
            size="small"
            table-layout="fixed"
          >
            <el-table-column prop="stepOrder" label="#" width="56" />
            <el-table-column label="工具" min-width="170">
              <template #default="scope">
                <strong>{{ scope.row.toolName || '-' }}</strong>
                <small>{{ scope.row.inputSummary || '' }}</small>
              </template>
            </el-table-column>
            <el-table-column label="风险" width="100">
              <template #default="scope">
                <el-tag size="small" effect="plain">{{ scope.row.riskLevel || scope.row.riskCategory || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="结果" min-width="200">
              <template #default="scope">
                <span class="agent-task__result">{{ scope.row.resultSummary || scope.row.errorMessage || '-' }}</span>
                <small v-if="scope.row.traceId || scope.row.auditId">{{ scope.row.traceId || scope.row.auditId }}</small>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="selectedTask.steps.length" class="agent-task__steps-mobile">
            <article v-for="step in selectedTask.steps" :key="`mobile-${step.stepOrder}`" class="agent-task__step-card">
              <header>
                <span>#{{ step.stepOrder }}</span>
                <strong>{{ step.toolName || '-' }}</strong>
                <el-tag size="small" effect="plain">{{ step.status || '-' }}</el-tag>
              </header>
              <p>{{ step.resultSummary || step.errorMessage || '-' }}</p>
              <small>
                {{ step.riskLevel || step.riskCategory || '-' }}
                <template v-if="step.traceId || step.auditId"> · {{ step.traceId || step.auditId }}</template>
              </small>
            </article>
          </div>
          <div v-else class="agent-task__empty-section">尚未产生执行步骤</div>
        </section>

        <section class="agent-task__section" aria-labelledby="agent-task-events-title">
          <header class="agent-task__section-header">
            <h2 id="agent-task-events-title">运行时间线</h2>
            <span v-if="events.length">{{ events.length }}</span>
          </header>
          <div v-if="eventError" class="agent-task__error agent-task__error--inline" role="alert">
            <span>{{ eventError }}</span>
            <el-button link type="danger" @click="refreshEvents">重试</el-button>
          </div>
          <el-timeline v-if="events.length" class="agent-task__timeline">
            <el-timeline-item v-for="event in events" :key="event.cursor" :timestamp="formatTime(event.createdTime)">
              <div class="agent-task__event-title">
                <strong>{{ eventLabel(event) }}</strong>
                <code>#{{ event.cursor }}</code>
              </div>
              <p v-if="event.fromStatus || event.toStatus" class="agent-task__event-status">
                {{ event.fromStatus || '-' }} → {{ event.toStatus || '-' }}
              </p>
              <p v-if="event.detail && Object.keys(event.detail).length" class="agent-task__event-detail">
                {{ formatEventDetail(event.detail) }}
              </p>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="agent-task__empty-section">暂无运行事件</div>
        </section>
      </template>

      <div v-else class="agent-task__detail-empty">
        <el-icon><DataAnalysis /></el-icon>
        <h2>选择一个任务</h2>
      </div>
    </main>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  CircleClose,
  DataAnalysis,
  EditPen,
  List,
  Plus,
  Refresh,
  VideoPause,
  VideoPlay,
  WarningFilled,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agentTaskApi } from '@/api/agentTask'
import {
  isTerminalTaskStatus,
  mergeTaskEvents,
  normalizeTask,
  parseTaskInputJson,
  taskCapabilities,
  taskProgress,
  taskStatusMeta,
} from './agentTaskWorkspaceState.js'

const TASK_POLL_INTERVAL_MS = 3000
const TASK_LIST_LIMIT = 50
const EVENT_PAGE_LIMIT = 50

const props = defineProps({
  active: {
    type: Boolean,
    default: false,
  },
  sessionId: {
    type: String,
    default: '',
  },
})

const statusOptions = [
  { value: 'QUEUED', label: '排队中' },
  { value: 'RUNNING', label: '执行中' },
  { value: 'WAITING_CONFIRMATION', label: '等待确认' },
  { value: 'WAITING_INPUT', label: '等待输入' },
  { value: 'PAUSED', label: '已暂停' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'FAILED', label: '执行失败' },
  { value: 'REQUIRES_REVIEW', label: '需要复核' },
]

const taskGoal = ref('')
const statusFilter = ref('')
const tasks = ref([])
const selectedTaskId = ref('')
const selectedTask = ref(null)
const events = ref([])
const eventCursor = ref(null)
const listLoading = ref(false)
const listError = ref('')
const detailLoading = ref(false)
const detailError = ref('')
const eventError = ref('')
const taskCreating = ref(false)
const actionLoading = ref(false)
const inputText = ref('')
const inputError = ref('')
const confirmationText = ref('')
let pollTimer = null
let refreshGeneration = 0

const selectedStatusMeta = computed(() => taskStatusMeta(selectedTask.value?.status))
const capabilities = computed(() => taskCapabilities(selectedTask.value?.status))
const progress = computed(() => taskProgress(selectedTask.value))
const terminalReason = computed(() => (
  selectedTask.value?.terminalReason
  || selectedTask.value?.cancelReason
  || ''
))
const confirmationMatches = computed(() => Boolean(
  selectedTask.value?.confirmation?.requiredText
  && confirmationText.value === selectedTask.value?.confirmation?.requiredText,
))

watch(() => props.active, (active) => {
  if (active) {
    loadTasks()
    startPolling()
  } else {
    stopPolling()
  }
})

watch(statusFilter, () => {
  if (props.active) loadTasks()
})

watch(() => selectedTask.value?.status, (status) => {
  if (isTerminalTaskStatus(status)) stopPolling()
  else if (props.active) startPolling()
})

onMounted(() => {
  if (props.active) loadTasks()
})

onBeforeUnmount(() => {
  stopPolling()
})

async function loadTasks() {
  listLoading.value = true
  listError.value = ''
  try {
    const response = await agentTaskApi.listTasks({
      status: statusFilter.value || undefined,
      limit: TASK_LIST_LIMIT,
    }, { silent: true })
    const data = response?.data ?? response
    const nextTasks = Array.isArray(data) ? data : data?.tasks || data?.records || []
    tasks.value = nextTasks.map(normalizeTask)
    const selectedStillExists = tasks.value.some((task) => task.taskId === selectedTaskId.value)
    if (!selectedStillExists && tasks.value[0]?.taskId) {
      await selectTask(tasks.value[0].taskId)
    } else if (selectedTaskId.value) {
      await refreshSelectedTask()
    }
  } catch (error) {
    listError.value = error?.message || '任务列表暂时不可用'
  } finally {
    listLoading.value = false
  }
}

async function createTask() {
  const goal = taskGoal.value.trim()
  if (!goal || taskCreating.value) return
  taskCreating.value = true
  try {
    const response = await agentTaskApi.createTask({ goal, sessionId: props.sessionId || undefined })
    const created = normalizeTask(response?.data ?? response)
    taskGoal.value = ''
    if (created.taskId) {
      selectedTaskId.value = created.taskId
      await loadTasks()
      await selectTask(created.taskId)
    }
    ElMessage.success('任务已创建')
  } catch (error) {
    ElMessage.error(error?.message || '任务创建失败')
  } finally {
    taskCreating.value = false
  }
}

async function selectTask(taskId) {
  if (!taskId || taskId === selectedTaskId.value && selectedTask.value) return
  selectedTaskId.value = taskId
  selectedTask.value = null
  events.value = []
  eventCursor.value = null
  confirmationText.value = ''
  inputText.value = ''
  inputError.value = ''
  stopPolling()
  refreshGeneration += 1
  await refreshSelectedTask()
  startPolling()
}

async function refreshSelectedTask() {
  const taskId = selectedTaskId.value
  if (!taskId) return
  const generation = refreshGeneration
  detailLoading.value = true
  detailError.value = ''
  try {
    const response = await agentTaskApi.getTask(taskId, { silent: true })
    if (generation !== refreshGeneration || taskId !== selectedTaskId.value) return
    const task = normalizeTask(response?.data ?? response)
    selectedTask.value = task
    tasks.value = tasks.value.map((item) => item.taskId === task.taskId ? task : item)
    await refreshEvents(generation, taskId)
    if (isTerminalTaskStatus(task.status)) stopPolling()
  } catch (error) {
    if (generation === refreshGeneration && taskId === selectedTaskId.value) {
      detailError.value = error?.message || '任务详情暂时不可用'
    }
  } finally {
    if (generation === refreshGeneration) detailLoading.value = false
  }
}

async function refreshEvents(generation = refreshGeneration, taskId = selectedTaskId.value) {
  if (!taskId) return
  eventError.value = ''
  try {
    const response = await agentTaskApi.getEvents(taskId, {
      after: eventCursor.value || undefined,
      limit: EVENT_PAGE_LIMIT,
    }, { silent: true })
    if (generation !== refreshGeneration || taskId !== selectedTaskId.value) return
    const data = response?.data ?? response ?? {}
    const merged = mergeTaskEvents(events.value, data.events || [])
    events.value = merged.events
    eventCursor.value = data.nextCursor ?? merged.nextCursor
  } catch (error) {
    if (generation === refreshGeneration && taskId === selectedTaskId.value) {
      eventError.value = error?.message || '运行时间线暂时不可用'
    }
  }
}

function startPolling() {
  stopPolling()
  if (!props.active || !selectedTaskId.value || isTerminalTaskStatus(selectedTask.value?.status)) return
  pollTimer = setInterval(refreshSelectedTask, TASK_POLL_INTERVAL_MS)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function pauseSelectedTask() {
  const reason = await promptReason('暂停任务', '暂停原因（可选）', 500)
  if (reason === null) return
  await mutateTask(() => agentTaskApi.pauseTask(selectedTaskId.value, { reason }))
}

async function resumeSelectedTask() {
  await mutateTask(() => agentTaskApi.resumeTask(selectedTaskId.value))
}

async function cancelSelectedTask() {
  const reason = await promptReason('取消任务', '取消原因（可选）', 1000)
  if (reason === null) return
  await mutateTask(() => agentTaskApi.cancelTask(selectedTaskId.value, { reason }))
}

async function submitTaskInput() {
  inputError.value = ''
  let input
  try {
    input = parseTaskInputJson(inputText.value)
  } catch (error) {
    inputError.value = error.message
    return
  }
  await mutateTask(() => agentTaskApi.submitInput(selectedTaskId.value, { input }))
  inputText.value = ''
}

async function confirmSelectedTask() {
  if (!confirmationMatches.value) return
  await mutateTask(() => agentTaskApi.confirmTask(selectedTaskId.value, {
    confirmationText: confirmationText.value,
  }))
  confirmationText.value = ''
}

async function mutateTask(operation) {
  if (actionLoading.value) return
  actionLoading.value = true
  try {
    const response = await operation()
    const task = normalizeTask(response?.data ?? response)
    if (task.taskId) {
      selectedTask.value = task
      tasks.value = tasks.value.map((item) => item.taskId === task.taskId ? task : item)
    }
    await refreshEvents()
    if (isTerminalTaskStatus(task.status)) stopPolling()
    else startPolling()
  } catch (error) {
    ElMessage.error(error?.message || '任务操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function promptReason(title, placeholder, maxLength) {
  try {
    const result = await ElMessageBox.prompt('', title, {
      inputPlaceholder: placeholder,
      inputValidator: (value) => (
        value && value.length > maxLength ? `原因不能超过${maxLength}个字符` : true
      ),
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    return result.value?.trim() || ''
  } catch {
    return null
  }
}

function eventLabel(event) {
  const labels = {
    TASK_CREATED: '任务已创建',
    TASK_CLAIMED: '任务已领取',
    TASK_STEP_STARTED: '步骤已开始',
    TASK_STEP_COMPLETED: '步骤已完成',
    TASK_STEP_FAILED: '步骤执行失败',
    TASK_WAITING_CONFIRMATION: '等待强确认',
    TASK_WAITING_INPUT: '等待补充输入',
    TASK_PAUSED: '任务已暂停',
    TASK_RESUMED: '任务已恢复',
    TASK_COMPLETED: '任务已完成',
    TASK_CANCELLED: '任务已取消',
    TASK_FAILED: '任务失败',
    TASK_REQUIRES_REVIEW: '需要人工复核',
  }
  return labels[event.eventType] || event.eventType || '任务事件'
}

function formatEventDetail(detail) {
  try {
    return JSON.stringify(detail)
  } catch {
    return ''
  }
}

function formatTime(value) {
  if (!value) return '-'
  const timestamp = Date.parse(value)
  if (!Number.isFinite(timestamp)) return String(value)
  return new Date(timestamp).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.agent-task-workspace {
  display: grid;
  grid-template-columns: minmax(250px, 300px) minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: 100%;
  background: #f7f8fa;
}

.agent-task__rail {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 20px 16px;
  background: #fff;
  border-right: 1px solid #e7e9ed;
}

.agent-task__rail-header,
.agent-task__detail-header,
.agent-task__section-header,
.agent-task__filter-row,
.agent-task__progress-copy,
.agent-task__intervention-footer,
.agent-task__detail-actions,
.agent-task__status-line,
.agent-task__list-meta,
.agent-task__event-title {
  display: flex;
  align-items: center;
}

.agent-task__rail-header,
.agent-task__detail-header,
.agent-task__section-header,
.agent-task__filter-row,
.agent-task__progress-copy,
.agent-task__intervention-footer,
.agent-task__event-title {
  justify-content: space-between;
}

.agent-task__eyebrow {
  display: block;
  color: #909399;
  font-size: 10px;
  letter-spacing: 1px;
}

.agent-task__rail h2,
.agent-task__detail h1,
.agent-task__section h2,
.agent-task__detail-empty h2 {
  margin: 0;
  color: #1f2329;
}

.agent-task__rail h2 {
  margin-top: 3px;
  font-size: 18px;
}

.agent-task__create {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.agent-task__create .el-button {
  justify-self: start;
}

.agent-task__filter-row {
  gap: 8px;
  margin: 18px 0 10px;
}

.agent-task__filter-row .el-select {
  min-width: 0;
  flex: 1;
}

.agent-task__count {
  min-width: 28px;
  color: #909399;
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.agent-task__error {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 10px;
  color: #b42318;
  background: #fff1f0;
  border: 1px solid #ffd6d2;
  border-radius: 6px;
  font-size: 12px;
}

.agent-task__error span {
  min-width: 0;
  flex: 1;
}

.agent-task__list {
  min-height: 0;
  flex: 1;
  overflow: auto;
}

.agent-task__list-item {
  display: block;
  width: 100%;
  padding: 11px 10px;
  color: inherit;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #f0f1f3;
  cursor: pointer;
}

.agent-task__list-item:hover,
.agent-task__list-item.is-active {
  background: #f0f6ff;
}

.agent-task__list-title {
  display: -webkit-box;
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.agent-task__list-meta {
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #909399;
  font-size: 11px;
}

.agent-task__list-meta time {
  white-space: nowrap;
}

.agent-task__empty-list,
.agent-task__empty-section,
.agent-task__detail-empty {
  display: grid;
  place-items: center;
  gap: 8px;
  color: #a8abb2;
  text-align: center;
}

.agent-task__empty-list {
  min-height: 140px;
  font-size: 12px;
}

.agent-task__detail {
  min-width: 0;
  min-height: 0;
  padding: 24px clamp(20px, 4vw, 52px);
  overflow: auto;
}

.agent-task__detail-header {
  align-items: flex-start;
  gap: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e7e9ed;
}

.agent-task__detail-heading {
  min-width: 0;
  flex: 1;
}

.agent-task__status-line {
  gap: 9px;
}

.agent-task__task-id,
.agent-task__detail-heading time {
  color: #909399;
  font-size: 12px;
}

.agent-task__detail h1 {
  margin-top: 9px;
  font-size: 21px;
  font-weight: 650;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.agent-task__detail-heading time {
  display: block;
  margin-top: 7px;
}

.agent-task__detail-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 7px;
}

.agent-task__progress-band {
  padding: 17px 0 20px;
}

.agent-task__progress-copy {
  margin-bottom: 8px;
  color: #606266;
  font-size: 12px;
}

.agent-task__progress-copy strong {
  color: #303133;
}

.agent-task__error--detail {
  margin-bottom: 16px;
}

.agent-task__intervention,
.agent-task__terminal {
  padding: 15px 16px;
  margin-bottom: 18px;
  border: 1px solid #dfe3e8;
  border-radius: 6px;
  background: #fff;
}

.agent-task__intervention--confirm {
  border-color: #f3c78b;
  background: #fffaf1;
}

.agent-task__intervention-heading {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  color: #606266;
}

.agent-task__intervention-heading > .el-icon {
  color: #e6a23c;
  font-size: 18px;
}

.agent-task__intervention-heading strong,
.agent-task__intervention-heading span {
  display: block;
}

.agent-task__intervention-heading strong {
  color: #303133;
  font-size: 13px;
}

.agent-task__intervention-heading span {
  margin-top: 4px;
  font-size: 12px;
}

.agent-task__intervention > .el-button {
  margin-top: 11px;
}

.agent-task__intervention-footer {
  gap: 10px;
  margin-top: 9px;
}

.agent-task__field-error {
  color: #f56c6c;
  font-size: 12px;
}

.agent-task__terminal {
  display: flex;
  gap: 10px;
  align-items: baseline;
  color: #606266;
  font-size: 13px;
}

.agent-task__terminal strong {
  color: #303133;
  white-space: nowrap;
}

.agent-task__terminal.is-failed,
.agent-task__terminal.is-requires_review {
  border-color: #fbc4c4;
  background: #fef0f0;
}

.agent-task__section {
  padding: 20px 0;
  border-top: 1px solid #e7e9ed;
}

.agent-task__section-header {
  margin-bottom: 12px;
}

.agent-task__section-header h2 {
  font-size: 15px;
}

.agent-task__section-header > span {
  color: #909399;
  font-size: 12px;
}

.agent-task__section :deep(.el-table) {
  background: transparent;
}

.agent-task__section :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.agent-task__section :deep(.el-table td.el-table__cell),
.agent-task__section :deep(.el-table th.el-table__cell) {
  background: transparent;
  border-bottom-color: #edf0f3;
}

.agent-task__section :deep(.el-table .cell) {
  overflow-wrap: anywhere;
}

.agent-task__steps-mobile {
  display: none;
}

.agent-task__step-card {
  padding: 12px;
  border: 1px solid #e7e9ed;
  border-radius: 6px;
  background: #fff;
}

.agent-task__step-card + .agent-task__step-card {
  margin-top: 8px;
}

.agent-task__step-card header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 11px;
}

.agent-task__step-card header strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #303133;
  font-size: 12px;
}

.agent-task__step-card p {
  margin: 9px 0 0;
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.agent-task__step-card small {
  margin-top: 8px;
  color: #909399;
  overflow-wrap: anywhere;
  white-space: normal;
}

.agent-task__section small {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: #909399;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-task__result {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-task__error--inline {
  margin-bottom: 12px;
}

.agent-task__timeline {
  padding-left: 6px;
}

.agent-task__event-title {
  gap: 8px;
}

.agent-task__event-title code {
  color: #909399;
  font-size: 11px;
}

.agent-task__event-status,
.agent-task__event-detail {
  margin: 5px 0 0;
  color: #909399;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.agent-task__detail-empty {
  height: 100%;
  min-height: 340px;
}

.agent-task__detail-empty .el-icon {
  color: #c0c4cc;
  font-size: 34px;
}

.agent-task__detail-empty h2 {
  font-size: 17px;
}

.agent-task__detail-empty span {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 860px) {
  .agent-task-workspace {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(220px, 36%) minmax(0, 1fr);
  }

  .agent-task__rail {
    border-right: 0;
    border-bottom: 1px solid #e7e9ed;
  }

  .agent-task__list {
    max-height: 190px;
  }

  .agent-task__detail {
    padding: 20px;
  }
}

@media (max-width: 560px) {
  .agent-task__detail-header {
    display: block;
  }

  .agent-task__detail-actions {
    justify-content: flex-start;
    margin-top: 14px;
  }

  .agent-task__detail h1 {
    font-size: 18px;
  }

  .agent-task__steps-table {
    display: none;
  }

  .agent-task__steps-mobile {
    display: grid;
  }

  .agent-task__terminal {
    display: block;
  }

  .agent-task__terminal strong,
  .agent-task__terminal span {
    display: block;
  }

  .agent-task__terminal span {
    margin-top: 5px;
  }
}
</style>
