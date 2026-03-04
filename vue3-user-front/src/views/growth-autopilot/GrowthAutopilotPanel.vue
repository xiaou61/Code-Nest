<template>
  <div class="growth-autopilot-panel">
      <section class="hero cn-learn-hero cn-wave-reveal">
        <div class="hero-content">
          <span class="hero-eyebrow">
            <el-icon><MagicStick /></el-icon>
            Growth Autopilot MVP
          </span>
          <h1>成长闭环自动驾驶</h1>
          <p>把周目标自动拆解成今日任务包，实时纠偏，一键重排，持续推进学习闭环。</p>
        </div>
        <div class="hero-actions">
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
        </div>
      </section>

      <section class="summary-grid">
        <article class="summary-card">
          <div class="summary-label">本周完成率</div>
          <div class="summary-value">{{ summary.completionRate || 0 }}%</div>
          <el-progress :percentage="summary.completionRate || 0" :show-text="false" :stroke-width="8" />
        </article>
        <article class="summary-card">
          <div class="summary-label">任务完成</div>
          <div class="summary-value">{{ summary.completedTasks || 0 }} / {{ summary.totalTasks || 0 }}</div>
          <p class="summary-desc">今日完成 {{ summary.todayCompleted || 0 }} / {{ summary.todayTasks || 0 }}</p>
        </article>
        <article class="summary-card">
          <div class="summary-label">逾期任务</div>
          <div class="summary-value" :class="{ danger: (summary.overdueTasks || 0) > 0 }">{{ summary.overdueTasks || 0 }}</div>
          <p class="summary-desc">建议优先处理逾期与 P1 任务</p>
        </article>
        <article class="summary-card">
          <div class="summary-label">风险等级</div>
          <div class="summary-value risk">
            <el-tag :type="riskTagType(summary.riskLevel)" effect="light">{{ riskText(summary.riskLevel) }}</el-tag>
          </div>
          <p class="summary-desc">{{ summary.riskText || '执行节奏稳定' }}</p>
        </article>
      </section>

      <section v-if="hasPlan" class="filter-wrap cn-learn-panel">
        <div class="filter-item">
          <span class="filter-label">模块</span>
          <el-select v-model="filters.moduleKey" size="small" style="width: 150px">
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
          <el-select v-model="filters.status" size="small" style="width: 140px">
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
      </section>

      <section v-if="!hasPlan" class="empty-wrap cn-learn-panel">
        <el-empty description="本周还没有自动驾驶计划">
          <el-button type="primary" @click="openGenerateDialog">立即生成</el-button>
        </el-empty>
      </section>

      <section v-else class="main-grid">
        <div class="main-left">
          <article class="panel cn-learn-panel">
            <header class="panel-head">
              <h3>本周任务时间线</h3>
              <span>{{ dashboard.weekStart }} 至 {{ dashboard.weekEnd }}</span>
            </header>
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
                      <el-tag size="small" :type="moduleTagType(task.moduleKey)" effect="plain">{{ task.moduleName }}</el-tag>
                      <el-tag size="small" :type="statusTagType(task.status)" effect="light">{{ task.statusText }}</el-tag>
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
          </article>
        </div>

        <div class="main-right">
          <article class="panel cn-learn-panel">
            <header class="panel-head">
              <h3>周进度趋势</h3>
              <span>按天完成率</span>
            </header>
            <div v-if="!dailyProgress.length" class="empty-inline">暂无趋势数据</div>
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
                <el-tag effect="plain">待完成 {{ statusSummary.todoTasks || 0 }}</el-tag>
                <el-tag type="success" effect="plain">已完成 {{ statusSummary.doneTasks || 0 }}</el-tag>
                <el-tag type="danger" effect="plain">已错过 {{ statusSummary.missedTasks || 0 }}</el-tag>
              </div>
              <div class="score-row">
                分值进度 {{ statusSummary.doneScore || 0 }} / {{ statusSummary.targetScore || 0 }}
              </div>
            </template>
          </article>

          <article class="panel cn-learn-panel">
            <header class="panel-head">
              <h3>模块进度</h3>
              <span>{{ dashboard.targetProfile?.targetRole || '通用' }} · {{ dashboard.targetProfile?.weeklyHours || 0 }}h</span>
            </header>
            <div v-if="!moduleProgress.length" class="empty-inline">暂无模块任务</div>
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
          </article>

          <article class="panel cn-learn-panel">
            <header class="panel-head">
              <h3>推荐下一步</h3>
              <span>系统建议</span>
            </header>
            <div v-if="!quickActions.length" class="empty-inline">暂无建议</div>
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
          </article>

          <article class="panel cn-learn-panel">
            <header class="panel-head">
              <h3>事件日志</h3>
              <span>最近操作</span>
            </header>
            <div v-if="!events.length" class="empty-inline">暂无事件</div>
            <ul v-else class="event-list">
              <li v-for="(event, index) in events" :key="`${event.createTime}-${index}`">
                <span class="event-type">{{ event.eventLabel }}</span>
                <p>{{ event.detail }}</p>
                <small>{{ event.createTime }}</small>
              </li>
            </ul>
          </article>
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
              style="width: 100%"
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
              style="width: 100%"
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

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Plus, RefreshRight, Refresh, Check } from '@element-plus/icons-vue'
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

const moduleTagType = (moduleKey) => {
  const map = {
    oj: 'primary',
    interview: 'success',
    flashcard: 'warning',
    plan: 'info',
    mock: 'danger',
    points: ''
  }
  return map[moduleKey] || 'info'
}

const statusTagType = (status) => {
  if (status === 'done') return 'success'
  if (status === 'missed') return 'danger'
  return 'info'
}

const riskTagType = (riskLevel) => {
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
  width: 100%;
}

.hero {
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.hero-content h1 {
  margin: 8px 0;
  color: #1c3760;
}

.hero-content p {
  margin: 0;
  color: #5b7498;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(31, 111, 235, 0.1);
  color: #1f61c4;
  font-size: 12px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.summary-card {
  border: 1px solid #dbe8fb;
  border-radius: 14px;
  background: linear-gradient(165deg, #ffffff 0%, #f5faff 100%);
  padding: 14px;
  box-shadow: 0 12px 24px rgba(25, 66, 124, 0.08);
}

.summary-label {
  color: #6f86a8;
  font-size: 12px;
}

.summary-value {
  margin: 6px 0 10px;
  font-size: 26px;
  color: #1f5fb4;
  font-weight: 700;
}

.summary-value.risk {
  font-size: 18px;
}

.summary-value.danger {
  color: #d14b58;
}

.summary-desc {
  margin: 0;
  color: #637ea5;
  font-size: 13px;
}

.empty-wrap {
  padding: 20px 0;
}

.filter-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  color: #6c83a6;
  font-size: 12px;
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
  gap: 12px;
}

.main-left,
.main-right {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel {
  padding: 14px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.panel-head h3 {
  margin: 0;
  color: #1f3a63;
}

.panel-head span {
  color: #7d8faa;
  font-size: 12px;
}

.day-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.day-card {
  border-radius: 12px;
  border: 1px solid #dce9fb;
  background: #f8fbff;
  padding: 10px;
  min-height: 180px;
}

.day-card.today {
  border-color: #8bb8ff;
  box-shadow: inset 0 0 0 1px #b9d4ff;
  background: linear-gradient(180deg, #f7fbff 0%, #edf5ff 100%);
}

.day-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.day-head .date {
  font-weight: 700;
  color: #214674;
}

.day-head .weekday {
  color: #6986ad;
  font-size: 12px;
}

.day-empty,
.empty-inline {
  color: #86a0c2;
  font-size: 13px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-item {
  border-radius: 10px;
  background: #fff;
  border: 1px solid #dce8f9;
  padding: 8px;
}

.task-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.task-item h4 {
  margin: 6px 0 4px;
  font-size: 13px;
  color: #203d66;
}

.task-item p {
  margin: 0;
  color: #637fa4;
  font-size: 12px;
  line-height: 1.5;
}

.task-meta {
  margin-top: 6px;
  display: flex;
  gap: 8px;
  color: #6d86a9;
  font-size: 12px;
}

.task-actions {
  margin-top: 6px;
  display: flex;
  gap: 10px;
}

.module-progress-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-progress-item {
  border: 1px solid #dbe8fb;
  border-radius: 10px;
  background: #f8fbff;
  padding: 10px;
}

.module-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.module-row strong {
  color: #234268;
}

.module-row span {
  color: #6e88aa;
  font-size: 12px;
}

.module-action {
  margin-top: 4px;
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.trend-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.trend-bar-bg {
  width: 100%;
  height: 90px;
  border-radius: 10px;
  background: linear-gradient(180deg, #f4f9ff 0%, #e8f1ff 100%);
  border: 1px solid #d9e7fb;
  display: flex;
  align-items: flex-end;
  padding: 4px;
}

.trend-bar-value {
  width: 100%;
  border-radius: 7px;
  background: linear-gradient(180deg, #2fa4f6 0%, #1f6feb 100%);
  transition: height 0.3s ease;
}

.trend-bar-item small {
  color: #6884aa;
  font-size: 12px;
}

.trend-bar-item span {
  color: #264a73;
  font-size: 12px;
}

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.score-row {
  color: #6380a6;
  font-size: 12px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.action-item {
  display: flex;
  gap: 10px;
  border: 1px solid #dce8fb;
  border-radius: 10px;
  background: #f8fbff;
  padding: 10px;
}

.action-order {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1f6feb, #22a0f0);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
}

.action-main h4 {
  margin: 0 0 4px;
  color: #204167;
  font-size: 14px;
}

.action-main p {
  margin: 0;
  color: #607b9f;
  font-size: 13px;
  line-height: 1.6;
}

.event-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.event-list li {
  border: 1px solid #dce8fb;
  border-radius: 10px;
  background: #f8fbff;
  padding: 10px;
}

.event-type {
  display: inline-block;
  font-size: 12px;
  border-radius: 999px;
  padding: 2px 8px;
  color: #1f63c3;
  background: #e9f2ff;
}

.event-list p {
  margin: 6px 0 4px;
  color: #26486f;
  font-size: 13px;
}

.event-list small {
  color: #7a8faa;
  font-size: 12px;
}

.generate-form {
  padding: 4px 6px 0;
}

.slider-wrap {
  width: 100%;
}

.slider-wrap span {
  color: #58779e;
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
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }

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
