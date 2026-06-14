<template>
  <div class="growth-autopilot-panel">
      <CnPageHeader
        title="成长闭环自动驾驶"
        eyebrow="Growth Autopilot MVP"
        description="把周目标自动拆解成今日任务包，实时纠偏，一键重排，持续推进学习闭环。"
      >
        <template #meta>
          <CnStatusTag :type="hasPlan ? 'success' : 'warning'" size="sm">
            {{ hasPlan ? '本周计划运行中' : '等待生成计划' }}
          </CnStatusTag>
          <CnStatusTag v-if="hasPlan" :type="riskTone(summary.riskLevel)" size="sm">
            {{ riskText(summary.riskLevel) }}
          </CnStatusTag>
          <CnStatusTag v-if="dashboard.generatedAt" type="info" size="sm" subtle>
            {{ dashboard.generatedAt }}
          </CnStatusTag>
        </template>
        <template #actions>
          <el-button type="primary" :loading="loading.generate" @click="openGenerateDialog">
            <el-icon><Plus /></el-icon>
            {{ hasPlan ? '重新生成周计划' : '生成本周计划' }}
          </el-button>
          <el-button plain :loading="loading.completeToday" :disabled="!hasPlan" @click="handleCompleteToday">
            <el-icon><Check /></el-icon>
            完成今日待办
          </el-button>
          <el-button plain :loading="loading.replan" :disabled="!hasPlan" @click="handleReplan">
            <el-icon><RefreshRight /></el-icon>
            一键重排
          </el-button>
          <el-button plain :loading="loading.dashboard" @click="loadDashboard">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </template>
      </CnPageHeader>

      <section class="summary-grid">
        <CnStatCard
          title="本周完成率"
          :value="summary.completionRate || 0"
          unit="%"
          description="按自动驾驶任务完成情况计算"
          tone="brand"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="任务完成"
          :value="`${summary.completedTasks || 0} / ${summary.totalTasks || 0}`"
          :description="`今日完成 ${summary.todayCompleted || 0} / ${summary.todayTasks || 0}`"
          tone="success"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="逾期任务"
          :value="summary.overdueTasks || 0"
          description="建议优先处理逾期与 P1 任务"
          :tone="(summary.overdueTasks || 0) > 0 ? 'danger' : 'neutral'"
          :loading="loading.dashboard"
        />
        <CnStatCard
          title="风险等级"
          :value="riskText(summary.riskLevel)"
          :description="summary.riskText || '执行节奏稳定'"
          :tone="riskTone(summary.riskLevel)"
          :loading="loading.dashboard"
        />
      </section>

      <CnSection v-if="hasPlan" class="filter-section" surface="plain" compact>
        <div class="filter-wrap">
        <div class="filter-item">
          <span class="filter-label">模块</span>
          <el-select v-model="filters.moduleKey" size="small" class="module-filter-select">
            <el-option label="全部模块" value="all" />
            <el-option
              v-for="item in moduleProgress"
              :key="item.moduleKey"
              :label="item.moduleName"
              :value="item.moduleKey"
            />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">状态</span>
          <el-select v-model="filters.status" size="small" class="status-filter-select">
            <el-option label="全部状态" value="all" />
            <el-option label="待完成" value="todo" />
            <el-option label="已完成" value="done" />
            <el-option label="已错过" value="missed" />
          </el-select>
        </div>
        <div class="filter-item keyword-item">
          <el-input v-model.trim="filters.keyword" size="small" clearable placeholder="筛选任务关键词" />
        </div>
        <div class="filter-item switch-item">
          <el-switch v-model="filters.onlyToday" inline-prompt active-text="仅看今日" inactive-text="全周" />
        </div>
        </div>
      </CnSection>

      <CnEmptyState
        v-if="!hasPlan"
        class="empty-wrap"
        title="本周还没有自动驾驶计划"
        description="生成后会自动拆解每日任务包，并根据完成情况给出重排建议。"
        icon="GA"
        surface="panel"
      >
        <template #actions>
          <el-button type="primary" @click="openGenerateDialog">立即生成</el-button>
        </template>
      </CnEmptyState>

      <section v-else class="main-grid">
        <div class="main-left">
          <CnSection
            class="timeline-section"
            title="本周任务时间线"
            :description="`${dashboard.weekStart || '--'} 至 ${dashboard.weekEnd || '--'}`"
            divided
          >
            <div class="day-grid" v-loading="loading.dashboard">
              <div
                v-for="bucket in filteredDayBuckets"
                :key="bucket.date"
                class="day-card"
                :class="{ today: bucket.today }"
              >
                <div class="day-head">
                  <span class="date">{{ shortDate(bucket.date) }}</span>
                  <span class="weekday">{{ bucket.dayLabel }}</span>
                </div>
                <div v-if="!bucket.tasks?.length" class="day-empty">暂无任务</div>
                <div v-else class="task-list">
                  <div v-for="task in bucket.tasks" :key="task.taskId" class="task-item">
                    <div class="task-top">
                      <CnStatusTag :type="moduleTone(task.moduleKey)" size="sm">
                        {{ task.moduleName || '模块' }}
                      </CnStatusTag>
                      <CnStatusTag :type="statusTone(task.status)" size="sm">
                        {{ task.statusText || '待完成' }}
                      </CnStatusTag>
                    </div>
                    <h4>{{ task.title }}</h4>
                    <p>{{ task.description }}</p>
                    <div class="task-meta">
                      <span>{{ task.plannedMinutes }} 分钟</span>
                      <span>{{ task.priority }}</span>
                      <span>+{{ task.taskScore }} 分</span>
                    </div>
                    <div class="task-actions">
                      <el-button link type="primary" @click="goRoute(task.routePath)">去执行</el-button>
                      <el-button
                        link
                        type="success"
                        :disabled="task.status === 'done'"
                        :loading="loading.completeTaskId === task.taskId"
                        @click="handleCompleteTask(task)"
                      >
                        标记完成
                      </el-button>
                      <el-button
                        link
                        type="warning"
                        :disabled="task.status !== 'todo' || !canPostpone(task)"
                        :loading="loading.postponeTaskId === task.taskId"
                        @click="handlePostponeTask(task)"
                      >
                        顺延一天
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </CnSection>
        </div>

        <div class="main-right">
          <CnSection title="周进度趋势" description="按天完成率" compact divided>
            <CnEmptyState
              v-if="!dailyProgress.length"
              title="暂无趋势数据"
              description="完成任务后会生成每日进度趋势。"
              icon="TR"
              size="sm"
              surface="transparent"
            />
            <template v-else>
              <div class="trend-chart">
                <div v-for="item in dailyProgress" :key="item.date" class="trend-bar-item">
                  <div class="trend-bar-bg">
                    <div class="trend-bar-value" :style="{ height: `${Math.max(item.completionRate || 0, 4)}%` }"></div>
                  </div>
                  <small>{{ item.dayLabel }}</small>
                  <span>{{ item.completedTasks }}/{{ item.totalTasks }}</span>
                </div>
              </div>
              <div class="status-row">
                <CnStatusTag type="info" size="sm">待完成 {{ statusSummary.todoTasks || 0 }}</CnStatusTag>
                <CnStatusTag type="success" size="sm">已完成 {{ statusSummary.doneTasks || 0 }}</CnStatusTag>
                <CnStatusTag type="danger" size="sm">已错过 {{ statusSummary.missedTasks || 0 }}</CnStatusTag>
              </div>
              <div class="score-row">
                分值进度 {{ statusSummary.doneScore || 0 }} / {{ statusSummary.targetScore || 0 }}
              </div>
            </template>
          </CnSection>

          <CnSection
            title="模块进度"
            :description="`${dashboard.targetProfile?.targetRole || '通用'} · ${dashboard.targetProfile?.weeklyHours || 0}h`"
            compact
            divided
          >
            <CnEmptyState
              v-if="!moduleProgress.length"
              title="暂无模块任务"
              description="生成计划后会按能力模块展示完成情况。"
              icon="MO"
              size="sm"
              surface="transparent"
            />
            <div v-else class="module-progress-list">
              <div v-for="item in moduleProgress" :key="item.moduleKey" class="module-progress-item">
                <div class="module-row">
                  <strong>{{ item.moduleName }}</strong>
                  <span>{{ item.completedTasks }}/{{ item.totalTasks }}</span>
                </div>
                <el-progress :percentage="item.completionRate" :show-text="false" :stroke-width="8" />
                <div class="module-action">
                  <el-button link type="primary" @click="goRoute(item.routePath)">进入模块</el-button>
                </div>
              </div>
            </div>
          </CnSection>

          <CnSection title="推荐下一步" description="系统建议" compact divided>
            <CnEmptyState
              v-if="!quickActions.length"
              title="暂无建议"
              description="任务推进后会根据当前风险给出下一步。"
              icon="NX"
              size="sm"
              surface="transparent"
            />
            <div v-else class="action-list">
              <div v-for="(item, idx) in quickActions" :key="`${item.title}-${idx}`" class="action-item">
                <div class="action-order">{{ idx + 1 }}</div>
                <div class="action-main">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.description }}</p>
                  <el-button link type="primary" @click="goRoute(item.routePath)">去处理</el-button>
                </div>
              </div>
            </div>
          </CnSection>

          <CnSection title="事件日志" description="最近操作" compact divided>
            <CnEmptyState
              v-if="!events.length"
              title="暂无事件"
              description="生成、重排和完成动作会记录在这里。"
              icon="EV"
              size="sm"
              surface="transparent"
            />
            <ul v-else class="event-list">
              <li v-for="(event, index) in events" :key="`${event.createTime}-${index}`">
                <CnStatusTag type="brand" size="sm">{{ event.eventLabel || '事件' }}</CnStatusTag>
                <p>{{ event.detail }}</p>
                <small>{{ event.createTime }}</small>
              </li>
            </ul>
          </CnSection>
        </div>
      </section>

      <el-dialog v-model="generateDialogVisible" title="生成自动驾驶计划" width="520px">
        <el-form label-width="100px" class="generate-form">
          <el-form-item label="目标岗位">
            <el-select
              v-model="generateForm.targetRole"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入岗位"
              class="full-width-control"
            >
              <el-option v-for="item in roleOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="每周投入">
            <div class="slider-wrap">
              <el-slider v-model="generateForm.weeklyHours" :min="3" :max="40" :step="1" />
              <span>{{ generateForm.weeklyHours }} 小时/周</span>
            </div>
          </el-form-item>
          <el-form-item label="周起始日期">
            <el-date-picker
              v-model="generateForm.weekStart"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="默认当前周"
              class="full-width-control"
            />
          </el-form-item>
          <el-alert
            title="系统会按模块权重自动拆解任务，并给出可执行的每日任务包。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>
        <template #footer>
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading.generate" @click="handleGenerate">开始生成</el-button>
        </template>
      </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, RefreshRight, Refresh, Check } from '@element-plus/icons-vue'
import { CnEmptyState, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { growthAutopilotApi } from '@/api/growthAutopilot'

const router = useRouter()

const roleOptions = ['后端开发', '前端开发', '全栈开发', '算法工程师', '测试开发', '产品经理', '运维开发']
const loading = reactive({
  dashboard: false,
  generate: false,
  replan: false,
  completeToday: false,
  completeTaskId: null,
  postponeTaskId: null
})

const dashboard = ref({
  hasPlan: false,
  weekStart: '',
  weekEnd: '',
  generatedAt: '',
  targetProfile: {},
  summary: {},
  moduleProgress: [],
  dayBuckets: [],
  dailyProgress: [],
  statusSummary: {},
  quickActions: [],
  events: []
})

const generateDialogVisible = ref(false)
const generateForm = reactive({
  targetRole: '',
  weeklyHours: 8,
  weekStart: ''
})
const filters = reactive({
  moduleKey: 'all',
  status: 'all',
  keyword: '',
  onlyToday: false
})

const hasPlan = computed(() => Boolean(dashboard.value?.hasPlan))
const summary = computed(() => dashboard.value?.summary || {})
const moduleProgress = computed(() => dashboard.value?.moduleProgress || [])
const dayBuckets = computed(() => dashboard.value?.dayBuckets || [])
const dailyProgress = computed(() => dashboard.value?.dailyProgress || [])
const statusSummary = computed(() => dashboard.value?.statusSummary || {})
const quickActions = computed(() => dashboard.value?.quickActions || [])
const events = computed(() => dashboard.value?.events || [])

const filteredDayBuckets = computed(() => {
  const keyword = (filters.keyword || '').toLowerCase()
  return dayBuckets.value
    .map((bucket) => {
      if (filters.onlyToday && !bucket.today) {
        return { ...bucket, tasks: [] }
      }
      const tasks = (bucket.tasks || []).filter((task) => {
        if (filters.moduleKey !== 'all' && task.moduleKey !== filters.moduleKey) return false
        if (filters.status !== 'all' && task.status !== filters.status) return false
        if (keyword) {
          const content = `${task.title || ''} ${task.description || ''} ${task.moduleName || ''}`.toLowerCase()
          return content.includes(keyword)
        }
        return true
      })
      return { ...bucket, tasks }
    })
    .filter((bucket) => !filters.onlyToday || bucket.today)
})

const moduleTone = (moduleKey) => {
  const map = {
    oj: 'brand',
    interview: 'success',
    flashcard: 'warning',
    plan: 'info',
    mock: 'danger',
    points: 'neutral'
  }
  return map[moduleKey] || 'info'
}

const statusTone = (status) => {
  if (status === 'done') return 'success'
  if (status === 'missed') return 'danger'
  return 'info'
}

const riskTone = (riskLevel) => {
  if (riskLevel === 'high') return 'danger'
  if (riskLevel === 'medium') return 'warning'
  return 'success'
}

const riskText = (riskLevel) => {
  if (riskLevel === 'high') return '高风险'
  if (riskLevel === 'medium') return '中风险'
  return '低风险'
}

const shortDate = (dateText) => {
  if (!dateText || dateText.length < 10) return dateText || '--'
  return dateText.slice(5)
}

const clampWeeklyHours = (value) => Math.max(3, Math.min(40, Number(value) || 8))

const canPostpone = (task) => {
  if (!task?.taskDate || !dashboard.value?.weekEnd) return false
  return task.taskDate < dashboard.value.weekEnd
}

const loadDashboard = async () => {
  loading.dashboard = true
  try {
    const data = await growthAutopilotApi.getDashboard()
    dashboard.value = data || {
      hasPlan: false,
      summary: {},
      moduleProgress: [],
      dayBuckets: [],
      dailyProgress: [],
      statusSummary: {},
      quickActions: [],
      events: []
    }
  } catch (error) {
    console.error('加载自动驾驶看板失败', error)
    ElMessage.error('加载自动驾驶看板失败，请稍后重试')
  } finally {
    loading.dashboard = false
  }
}

const openGenerateDialog = () => {
  generateForm.targetRole = dashboard.value?.targetProfile?.targetRole || ''
  generateForm.weeklyHours = clampWeeklyHours(dashboard.value?.targetProfile?.weeklyHours || 8)
  generateForm.weekStart = dashboard.value?.weekStart || ''
  generateDialogVisible.value = true
}

const handleGenerate = async () => {
  loading.generate = true
  try {
    const data = await growthAutopilotApi.generate({
      targetRole: generateForm.targetRole,
      weeklyHours: clampWeeklyHours(generateForm.weeklyHours),
      weekStart: generateForm.weekStart || undefined
    })
    dashboard.value = data || dashboard.value
    generateDialogVisible.value = false
    ElMessage.success('自动驾驶周计划生成成功')
  } catch (error) {
    console.error('生成自动驾驶计划失败', error)
    ElMessage.error('生成失败，请稍后重试')
  } finally {
    loading.generate = false
  }
}

const handleReplan = async () => {
  if (!hasPlan.value) {
    ElMessage.warning('请先生成本周计划')
    return
  }
  loading.replan = true
  try {
    const data = await growthAutopilotApi.replan({
      weekStart: dashboard.value.weekStart || undefined
    })
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已按当前进度完成重排')
  } catch (error) {
    console.error('任务重排失败', error)
    ElMessage.error('任务重排失败，请稍后重试')
  } finally {
    loading.replan = false
  }
}

const handleCompleteToday = async () => {
  if (!hasPlan.value) {
    ElMessage.warning('请先生成本周计划')
    return
  }
  loading.completeToday = true
  try {
    const data = await growthAutopilotApi.completeToday(dashboard.value.weekStart || undefined)
    dashboard.value = data || dashboard.value
    ElMessage.success('今日待办已批量完成')
  } catch (error) {
    console.error('批量完成今日任务失败', error)
    ElMessage.error('批量完成失败，请稍后重试')
  } finally {
    loading.completeToday = false
  }
}

const handleCompleteTask = async (task) => {
  if (!task?.taskId || task.status === 'done') return
  loading.completeTaskId = task.taskId
  try {
    const data = await growthAutopilotApi.completeTask(task.taskId)
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已完成')
  } catch (error) {
    console.error('任务完成失败', error)
    ElMessage.error('任务完成失败，请稍后重试')
  } finally {
    loading.completeTaskId = null
  }
}

const handlePostponeTask = async (task) => {
  if (!task?.taskId || !canPostpone(task) || task.status !== 'todo') return
  loading.postponeTaskId = task.taskId
  try {
    const data = await growthAutopilotApi.postponeTask(task.taskId)
    dashboard.value = data || dashboard.value
    ElMessage.success('任务已顺延一天')
  } catch (error) {
    console.error('任务顺延失败', error)
    ElMessage.error('任务顺延失败，请稍后重试')
  } finally {
    loading.postponeTaskId = null
  }
}

const goRoute = (path) => {
  if (!path) return
  router.push(path)
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.growth-autopilot-panel {
  display: grid;
  gap: var(--cn-space-5);
  width: 100%;
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.empty-wrap {
  min-height: 280px;
}

.filter-wrap {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.module-filter-select {
  width: 150px;
}

.status-filter-select {
  width: 140px;
}

.full-width-control {
  width: 100%;
}

.filter-label {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.keyword-item {
  flex: 1;
  min-width: 180px;
}

.switch-item {
  margin-left: auto;
}

.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--cn-space-4);
}

.main-left,
.main-right {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-4);
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.day-card {
  min-width: 0;
  min-height: 180px;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.day-card.today {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 38%, var(--cn-color-border-subtle));
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--cn-color-brand-primary) 18%, transparent);
  background: color-mix(in srgb, var(--cn-color-brand-soft) 52%, var(--cn-color-bg-surface));
}

.day-head {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.day-head .date {
  font-weight: 700;
  color: var(--cn-color-text-primary);
}

.day-head .weekday {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.day-empty,
.empty-inline {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.task-item {
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface);
}

.task-top {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-2);
  min-width: 0;
}

.task-item h4 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  font-size: 13px;
  color: var(--cn-color-text-primary);
  line-height: 1.5;
}

.task-item p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.task-meta {
  margin-top: var(--cn-space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.task-actions {
  margin-top: var(--cn-space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.module-progress-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.module-progress-item {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.module-row {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-2);
}

.module-row strong {
  color: var(--cn-color-text-primary);
}

.module-row span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.module-action {
  margin-top: var(--cn-space-1);
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.trend-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--cn-space-1);
  min-width: 0;
}

.trend-bar-bg {
  width: 100%;
  height: 90px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  display: flex;
  align-items: flex-end;
  padding: var(--cn-space-1);
}

.trend-bar-value {
  width: 100%;
  border-radius: calc(var(--cn-radius-control) - 2px);
  background: var(--cn-color-brand-primary);
  transition: height var(--cn-motion-base) var(--cn-ease-out);
}

.trend-bar-item small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.trend-bar-item span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-2);
}

.score-row {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.action-item {
  display: flex;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.action-order {
  width: 26px;
  height: 26px;
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-brand-primary);
  color: var(--cn-button-primary-color);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
}

.action-main h4 {
  margin: 0 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.action-main p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.event-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.event-list li {
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
}

.event-list p {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.event-list small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.generate-form {
  padding: 4px 6px 0;
}

.slider-wrap {
  width: 100%;
}

.slider-wrap span {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-grid {
    grid-template-columns: 1fr;
  }

  .day-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
  }

  .switch-item {
    margin-left: 0;
    justify-content: flex-start;
  }

  .summary-grid,
  .day-grid {
    grid-template-columns: 1fr;
  }

  .trend-chart {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
