<template>
  <CnPage class="plan-page" max-width="1180px" full-height>
    <CnPageHeader
      title="计划打卡执行面板"
      description="把每日任务、打卡状态和长期趋势集中到同一个视图，持续稳定推进目标。"
      eyebrow="PLAN CHECK-IN"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">进行中 {{ stats.totalPlans || 0 }}</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>今日打卡 {{ stats.todayCheckins || 0 }}</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>最长连续 {{ stats.maxStreak || 0 }} 天</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建计划</el-button>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="进行中" :value="stats.totalPlans || 0" description="当前活跃计划" tone="brand" />
      <CnStatCard title="今日已打卡" :value="stats.todayCheckins || 0" description="今日完成任务数" tone="success" />
      <CnStatCard title="累计打卡" :value="stats.totalCheckins || 0" description="历史打卡总次数" tone="info" />
      <CnStatCard title="最长连续" :value="stats.maxStreak || 0" unit="天" description="最长连续执行记录" tone="warning" />
    </div>

    <CnSection title="今日任务" description="优先处理今天需要完成的计划任务。" divided>
      <template #actions>
        <CnStatusTag type="neutral" size="sm" subtle>{{ todayTasks.length }} 个任务</CnStatusTag>
      </template>

      <div v-loading="todayLoading" class="today-task-shell">
        <CnEmptyState
          v-if="!todayLoading && todayTasks.length === 0"
          title="今日暂无任务"
          description="可以先创建一个计划，系统会根据计划生成每日打卡任务。"
          icon="TD"
          surface="transparent"
          size="sm"
        />

        <div v-else class="task-list">
          <article
            v-for="task in todayTasks"
            :key="task.planId"
            class="task-card"
            :class="{ 'is-completed': isTaskChecked(task) }"
          >
            <div class="task-main">
              <div class="task-copy">
                <div class="task-title">
                  <CnStatusTag :type="getPlanTypeTone(task.planType)" size="sm" subtle>
                    {{ getPlanTypeText(task.planType) }}
                  </CnStatusTag>
                  <h3>{{ task.planName || '未命名任务' }}</h3>
                </div>
                <p class="task-target">目标：{{ task.targetValue || 0 }} {{ task.targetUnit || '' }}</p>
                <p v-if="task.dailyStartTime || task.dailyEndTime" class="task-time">
                  <el-icon><Clock /></el-icon>
                  {{ task.dailyStartTime || '--:--' }} - {{ task.dailyEndTime || '--:--' }}
                </p>
              </div>

              <div class="streak-card">
                <span class="streak-label">连续</span>
                <strong>{{ task.currentStreak || 0 }}</strong>
                <span class="streak-label">天</span>
              </div>
            </div>

            <div class="task-action">
              <el-button v-if="!isTaskChecked(task)" type="primary" :icon="Check" @click="openCheckinDialog(task)">
                打卡
              </el-button>
              <CnStatusTag v-else type="success" size="lg">
                <el-icon><SuccessFilled /></el-icon>
                已完成
              </CnStatusTag>
            </div>
          </article>
        </div>
      </div>
    </CnSection>

    <CnSection title="我的计划" description="查看计划执行情况，继续编辑、暂停、恢复或查看打卡记录。" divided>
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建计划</el-button>
      </template>

      <CnFilterForm
        v-model="filters"
        :fields="filterFields"
        :columns="3"
        :loading="planLoading"
        @search="handleFilterSearch"
        @reset="handleFilterReset"
      >
        <template #status="{ value }">
          <el-select
            :model-value="value"
            placeholder="全部状态"
            clearable
            @update:model-value="(next) => handleSelectFilterChange('status', next)"
          >
            <el-option label="进行中" :value="1" />
            <el-option label="已暂停" :value="2" />
            <el-option label="已完成" :value="3" />
          </el-select>
        </template>

        <template #planType="{ value }">
          <el-select
            :model-value="value"
            placeholder="全部类型"
            clearable
            @update:model-value="(next) => handleSelectFilterChange('planType', next)"
          >
            <el-option label="刷题计划" :value="1" />
            <el-option label="学习计划" :value="2" />
            <el-option label="阅读计划" :value="3" />
            <el-option label="运动计划" :value="4" />
            <el-option label="其他计划" :value="5" />
          </el-select>
        </template>

        <template #keyword="{ value }">
          <el-input
            :model-value="String(value || '')"
            placeholder="搜索计划名称"
            clearable
            :prefix-icon="Search"
            @update:model-value="(next) => updateFilter('keyword', next)"
            @clear="handleFilterSearch"
            @keyup.enter="handleFilterSearch"
          />
        </template>
      </CnFilterForm>

      <div v-loading="planLoading" class="plan-list-shell">
        <CnEmptyState
          v-if="!planLoading && planList.length === 0"
          title="暂无计划"
          description="点击新建计划，创建你的第一个每日打卡目标。"
          icon="PL"
          surface="transparent"
          size="sm"
        />

        <div v-else class="plan-list">
          <article v-for="plan in planList" :key="plan.id" class="plan-card">
            <div class="plan-card__top">
              <div class="plan-title">
                <CnStatusTag :type="getPlanTypeTone(plan.planType)" size="sm" subtle>
                  {{ getPlanTypeText(plan.planType) }}
                </CnStatusTag>
                <h3>{{ plan.planName || '未命名计划' }}</h3>
                <CnStatusTag :type="getStatusTone(plan.status)" size="sm">
                  {{ getStatusText(plan.status) }}
                </CnStatusTag>
              </div>

              <el-dropdown trigger="click" @command="(command) => handlePlanAction(command, plan)">
                <el-button link :icon="MoreFilled" class="more-button" aria-label="计划操作" />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      <el-icon><Edit /></el-icon>
                      编辑
                    </el-dropdown-item>
                    <el-dropdown-item command="records">
                      <el-icon><Document /></el-icon>
                      打卡记录
                    </el-dropdown-item>
                    <el-dropdown-item v-if="Number(plan.status) === 1" command="pause">
                      <el-icon><VideoPause /></el-icon>
                      暂停
                    </el-dropdown-item>
                    <el-dropdown-item v-if="Number(plan.status) === 2" command="resume">
                      <el-icon><VideoPlay /></el-icon>
                      恢复
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <p v-if="plan.planDesc" class="plan-desc">{{ plan.planDesc }}</p>

            <div class="plan-meta">
              <span>
                <el-icon><Calendar /></el-icon>
                {{ formatDate(plan.startDate) }} - {{ plan.endDate ? formatDate(plan.endDate) : '长期' }}
              </span>
              <span>
                <el-icon><Aim /></el-icon>
                {{ plan.targetValue || 0 }} {{ plan.targetUnit || '' }}
              </span>
            </div>

            <div class="plan-stats">
              <div>
                <strong>{{ plan.totalCheckinDays || 0 }}</strong>
                <span>累计打卡</span>
              </div>
              <div>
                <strong>{{ plan.currentStreak || 0 }}</strong>
                <span>当前连续</span>
              </div>
              <div>
                <strong>{{ plan.maxStreak || 0 }}</strong>
                <span>最长连续</span>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div v-if="total > pageSize" class="pagination">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </CnSection>

    <PlanFormDialog v-model="showFormDialog" :plan-data="editingPlan" @success="onPlanSaved" />

    <CheckinDialog v-model="showCheckinDialog" :task="checkinTask" @success="onCheckinSuccess" />

    <CheckinRecordDialog v-model="showRecordDialog" :plan-id="recordPlanId" :plan-name="recordPlanName" />
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Aim,
  Calendar,
  Check,
  Clock,
  Delete,
  Document,
  Edit,
  MoreFilled,
  Plus,
  Search,
  SuccessFilled,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnFilterField,
  type CnTone
} from '@/design-system'
import planApi from '@/api/plan'
import PlanFormDialog from './components/PlanFormDialog.vue'
import CheckinDialog from './components/CheckinDialog.vue'
import CheckinRecordDialog from './components/CheckinRecordDialog.vue'

interface StatsOverview extends Record<string, unknown> {
  totalPlans?: number
  todayCheckins?: number
  totalCheckins?: number
  maxStreak?: number
}

interface TodayTask extends Record<string, unknown> {
  planId: number | string
  planName?: string
  planType?: number | string
  targetValue?: number | string
  targetUnit?: string
  dailyStartTime?: string
  dailyEndTime?: string
  currentStreak?: number
  todayChecked?: boolean | number
}

interface PlanItem extends Record<string, unknown> {
  id: number | string
  planName?: string
  planDesc?: string
  planType?: number | string
  status?: number | string
  startDate?: string
  endDate?: string
  targetValue?: number | string
  targetUnit?: string
  totalCheckinDays?: number
  currentStreak?: number
  maxStreak?: number
}

interface PlanListResponse {
  records?: PlanItem[]
  total?: number
}

type PlanAction = 'edit' | 'records' | 'pause' | 'resume' | 'delete'

const stats = ref<StatsOverview>({})
const todayTasks = ref<TodayTask[]>([])
const todayLoading = ref(false)

const planList = ref<PlanItem[]>([])
const planLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const createFilters = () => ({
  status: '',
  planType: '',
  keyword: ''
})

const filters = ref<Record<string, unknown>>(createFilters())

const filterFields: CnFilterField[] = [
  { prop: 'status', label: '状态', type: 'custom', slot: 'status' },
  { prop: 'planType', label: '类型', type: 'custom', slot: 'planType' },
  { prop: 'keyword', label: '计划名称', type: 'custom', slot: 'keyword' }
]

const showFormDialog = ref(false)
const editingPlan = ref<PlanItem | null>(null)
const showCheckinDialog = ref(false)
const checkinTask = ref<TodayTask | null>(null)
const showRecordDialog = ref(false)
const recordPlanId = ref<number | null>(null)
const recordPlanName = ref('')

const nullableFilter = (value: unknown) => {
  return value === '' || value === undefined ? null : value
}

const updateFilter = (prop: string, value: unknown) => {
  filters.value = {
    ...filters.value,
    [prop]: value ?? ''
  }
}

const handleSelectFilterChange = (prop: string, value: unknown) => {
  updateFilter(prop, value)
  handleFilterSearch()
}

const loadStats = async () => {
  try {
    const response = (await planApi.getStatsOverview()) as StatsOverview
    stats.value = response || {}
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const loadTodayTasks = async () => {
  todayLoading.value = true
  try {
    const response = (await planApi.getTodayTasks()) as TodayTask[]
    todayTasks.value = response || []
  } catch (error) {
    console.error('加载今日任务失败:', error)
  } finally {
    todayLoading.value = false
  }
}

const loadPlanList = async () => {
  planLoading.value = true
  try {
    const response = (await planApi.getPlanList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: nullableFilter(filters.value.status),
      planType: nullableFilter(filters.value.planType),
      keyword: (filters.value.keyword as string) || ''
    })) as PlanListResponse
    planList.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('加载计划列表失败:', error)
  } finally {
    planLoading.value = false
  }
}

const handleFilterSearch = () => {
  pageNum.value = 1
  loadPlanList()
}

const handleFilterReset = () => {
  filters.value = createFilters()
  pageNum.value = 1
  loadPlanList()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  loadPlanList()
}

const openCreateDialog = () => {
  editingPlan.value = null
  showFormDialog.value = true
}

const openCheckinDialog = (task: TodayTask) => {
  checkinTask.value = task
  showCheckinDialog.value = true
}

const handlePlanAction = async (command: unknown, plan: PlanItem) => {
  switch (command as PlanAction) {
    case 'edit':
      editingPlan.value = plan
      showFormDialog.value = true
      break
    case 'records':
      recordPlanId.value = Number(plan.id)
      recordPlanName.value = plan.planName || ''
      showRecordDialog.value = true
      break
    case 'pause':
      await handlePausePlan(plan)
      break
    case 'resume':
      await handleResumePlan(plan)
      break
    case 'delete':
      await handleDeletePlan(plan)
      break
  }
}

const handlePausePlan = async (plan: PlanItem) => {
  try {
    await ElMessageBox.confirm('确定要暂停该计划吗？暂停后将不再生成提醒', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await planApi.pausePlan(plan.id)
    ElMessage.success('计划已暂停')
    loadPlanList()
    loadTodayTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('暂停计划失败:', error)
      ElMessage.error('暂停失败')
    }
  }
}

const handleResumePlan = async (plan: PlanItem) => {
  try {
    await planApi.resumePlan(plan.id)
    ElMessage.success('计划已恢复')
    loadPlanList()
    loadTodayTasks()
  } catch (error) {
    console.error('恢复计划失败:', error)
    ElMessage.error('恢复失败')
  }
}

const handleDeletePlan = async (plan: PlanItem) => {
  try {
    await ElMessageBox.confirm('确定要删除该计划吗？删除后数据无法恢复', '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await planApi.deletePlan(plan.id)
    ElMessage.success('计划已删除')
    loadPlanList()
    loadTodayTasks()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除计划失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const onPlanSaved = () => {
  loadPlanList()
  loadTodayTasks()
  loadStats()
}

const onCheckinSuccess = () => {
  loadTodayTasks()
  loadPlanList()
  loadStats()
}

const isTaskChecked = (task: TodayTask) => {
  return task.todayChecked === true || Number(task.todayChecked) === 1
}

const getPlanTypeText = (type: unknown) => {
  const typeMap: Record<string, string> = {
    1: '刷题',
    2: '学习',
    3: '阅读',
    4: '运动',
    5: '其他',
    99: '其他'
  }
  return typeMap[String(type)] || '其他'
}

const getPlanTypeTone = (type: unknown): CnTone => {
  const toneMap: Record<string, CnTone> = {
    1: 'info',
    2: 'brand',
    3: 'success',
    4: 'warning',
    5: 'neutral',
    99: 'neutral'
  }
  return toneMap[String(type)] || 'neutral'
}

const getStatusText = (status: unknown) => {
  const statusMap: Record<string, string> = {
    1: '进行中',
    2: '已暂停',
    3: '已完成'
  }
  return statusMap[String(status)] || '未知'
}

const getStatusTone = (status: unknown): CnTone => {
  const toneMap: Record<string, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'info'
  }
  return toneMap[String(status)] || 'neutral'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(() => {
  loadStats()
  loadTodayTasks()
  loadPlanList()
})
</script>

<style scoped>
.plan-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.today-task-shell,
.plan-list-shell {
  min-height: 180px;
}

.task-list,
.plan-list {
  display: grid;
  gap: var(--cn-space-4);
}

.task-card,
.plan-card {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.task-card:hover,
.plan-card:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-sm);
}

.task-card.is-completed {
  border-color: color-mix(in srgb, var(--cn-color-success) 28%, var(--cn-color-border));
  background: color-mix(in srgb, var(--cn-color-success-soft) 36%, var(--cn-card-bg));
}

.task-main,
.plan-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.task-main {
  align-items: center;
  flex: 1;
}

.task-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.task-copy {
  min-width: 0;
}

.task-title,
.plan-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  min-width: 0;
}

.task-title h3,
.plan-title h3 {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.35;
}

.task-target,
.task-time,
.plan-desc,
.plan-meta {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.task-target {
  margin: var(--cn-space-2) 0 0;
}

.task-time,
.plan-meta span {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.task-time {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-tertiary);
}

.streak-card {
  display: grid;
  justify-items: center;
  min-width: 76px;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.streak-card strong {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 26px;
  line-height: 1;
}

.streak-label {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.task-action {
  flex-shrink: 0;
}

.task-action :deep(.cn-status-tag__label) {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.plan-list-shell {
  margin-top: var(--cn-space-5);
}

.more-button {
  color: var(--cn-color-text-tertiary);
}

.plan-desc {
  margin: var(--cn-space-3) 0 0;
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-3);
}

.plan-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.plan-stats div {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.plan-stats strong {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 22px;
  line-height: 1.1;
}

.plan-stats span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-5);
  overflow-x: auto;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .summary-grid,
  .plan-stats {
    grid-template-columns: 1fr;
  }

  .task-card,
  .task-main,
  .plan-card__top {
    display: grid;
  }

  .task-action :deep(.el-button) {
    width: 100%;
  }
}
</style>
