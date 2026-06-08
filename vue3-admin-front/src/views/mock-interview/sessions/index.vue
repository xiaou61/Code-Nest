<template>
  <CnPage class="mock-interview-sessions-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="模拟面试运营台"
      description="查看模拟面试核心指标、筛选会话并快速质检问答质量。"
      eyebrow="Mock Interview Sessions"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">模拟面试</CnStatusTag>
        <CnStatusTag type="neutral">总会话 {{ stats.totalSessions || 0 }}</CnStatusTag>
        <CnStatusTag type="success">完成 {{ stats.completedSessions || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">进行中 {{ stats.ongoingSessions || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button @click="handleReset">重置筛选</el-button>
        <el-button type="primary" :loading="loading || statsLoading" @click="loadAllData">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <div class="mock-stat-grid" v-loading="statsLoading">
      <CnStatCard title="总会话数" :value="stats.totalSessions || 0" description="平台累计模拟面试会话" tone="brand" />
      <CnStatCard title="完成率" :value="toPercent(stats.completionRate)" unit="%" :description="`完成 ${stats.completedSessions || 0} 场`" tone="success" />
      <CnStatCard title="平均分" :value="toNumber(stats.avgScore, 2)" :description="`活跃用户 ${stats.activeUsers || 0}`" tone="info" />
      <CnStatCard
        title="平均时长"
        :value="stats.avgDurationMinutes || 0"
        unit="分钟"
        :description="`进行中 ${stats.ongoingSessions || 0} / 中断 ${stats.interruptedSessions || 0}`"
        tone="warning"
      />
    </div>

    <CnSection title="方向分布" description="按模拟面试方向查看会话数量、平均分和完成率。" divided>
      <CnDataTable
        :columns="distributionColumns"
        :data="directionDistributions"
        :loading="statsLoading"
        :pagination="null"
        row-key="direction"
        empty-title="暂无方向统计数据"
        empty-description="当前筛选条件下没有方向分布数据。"
      >
        <template #avgScore="{ row }">{{ toNumber(row.avgScore, 2) }}</template>
        <template #completionRate="{ row }">
          <CnStatusTag type="success" size="sm">{{ toPercent(row.completionRate) }}%</CnStatusTag>
        </template>
      </CnDataTable>
    </CnSection>

    <CnSection title="筛选条件" description="按用户、方向、状态、时间和分数范围筛选会话。" divided>
      <CnFilterForm
        :model-value="filters"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #minScore="{ value, setValue }">
          <el-input-number
            :model-value="value"
            :min="0"
            :max="100"
            :step="1"
            controls-position="right"
            placeholder="最低分"
            @update:model-value="(next) => setValue(next)"
          />
        </template>

        <template #maxScore="{ value, setValue }">
          <el-input-number
            :model-value="value"
            :min="0"
            :max="100"
            :step="1"
            controls-position="right"
            placeholder="最高分"
            @update:model-value="(next) => setValue(next)"
          />
        </template>
      </CnFilterForm>
    </CnSection>

    <CnSection title="会话列表" :description="`共 ${pagination.total} 条会话`" divided>
      <CnDataTable
        :columns="sessionColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="会话数据" description="详情抽屉用于查看每场面试的问答、评分和追问链路。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">当前页 {{ tableData.length }} 条</CnStatusTag>
            </template>
            <el-button type="primary" :loading="loading" @click="loadSessions">刷新列表</el-button>
          </CnToolbar>
        </template>

        <template #questionMode="{ row }">{{ questionModeText(row.questionMode) }}</template>
        <template #status="{ row }">
          <CnStatusTag :type="statusTone(row.status)" size="sm">{{ statusText(row.status) }}</CnStatusTag>
        </template>
        <template #totalScore="{ row }">
          <CnStatusTag :type="scoreTone(row.totalScore)" size="sm">{{ row.totalScore ?? '-' }}</CnStatusTag>
        </template>
        <template #actions="{ row }">
          <el-button type="primary" link size="small" @click="openDetail(row)">查看详情</el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-drawer v-model="detailVisible" title="会话详情" size="65%" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="sessionDetail">
          <el-descriptions :column="3" border class="detail-descriptions">
            <el-descriptions-item label="会话ID">{{ sessionDetail.session.id }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ sessionDetail.session.userId }}</el-descriptions-item>
            <el-descriptions-item label="方向">{{ sessionDetail.session.directionName || sessionDetail.session.direction }}</el-descriptions-item>
            <el-descriptions-item label="难度">{{ sessionDetail.session.levelName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="风格">{{ sessionDetail.session.styleName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <CnStatusTag :type="statusTone(sessionDetail.session.status)" size="sm">
                {{ statusText(sessionDetail.session.status) }}
              </CnStatusTag>
            </el-descriptions-item>
            <el-descriptions-item label="总分">{{ sessionDetail.session.totalScore ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ sessionDetail.session.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ sessionDetail.session.endTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="qa-metrics">
            <CnStatusTag type="brand">已回答: {{ sessionDetail.answeredCount || 0 }}</CnStatusTag>
            <CnStatusTag type="warning">已跳过: {{ sessionDetail.skippedCount || 0 }}</CnStatusTag>
            <CnStatusTag type="info">待回答: {{ sessionDetail.pendingCount || 0 }}</CnStatusTag>
            <CnStatusTag type="success">追问: {{ sessionDetail.followUpCount || 0 }}</CnStatusTag>
          </div>

          <CnEmptyState
            v-if="!sessionDetail.qaList?.length"
            title="暂无问答数据"
            description="该会话暂未记录问答明细。"
            size="sm"
            surface="plain"
          />
          <el-collapse v-else>
            <el-collapse-item
              v-for="item in sessionDetail.qaList"
              :key="item.id"
              :title="`第 ${item.questionOrder} 题（${qaStatusText(item.status)}）`"
              :name="item.id"
            >
              <div class="qa-block">
                <p><strong>问题：</strong>{{ item.questionContent }}</p>
                <p><strong>回答：</strong>{{ item.userAnswer || '（未作答）' }}</p>
                <p><strong>得分：</strong>{{ item.score ?? '-' }}</p>
                <p><strong>知识点：</strong>{{ item.knowledgePoints || '-' }}</p>
                <p><strong>参考答案：</strong>{{ item.referenceAnswer || '-' }}</p>
                <div v-if="parseFeedback(item.aiFeedback)" class="feedback">
                  <p><strong>AI反馈：</strong></p>
                  <p>优点：{{ parseFeedback(item.aiFeedback)?.strengths?.join('、') || '无' }}</p>
                  <p>改进：{{ parseFeedback(item.aiFeedback)?.improvements?.join('、') || '无' }}</p>
                </div>
              </div>

              <div v-if="item.followUps?.length" class="follow-up-block">
                <h4>追问链路（{{ item.followUps.length }}）</h4>
                <CnSection v-for="follow in item.followUps" :key="follow.id" compact surface="plain" class="follow-up-item">
                  <p><strong>追问：</strong>{{ follow.questionContent }}</p>
                  <p><strong>回答：</strong>{{ follow.userAnswer || '（未作答）' }}</p>
                  <p><strong>得分：</strong>{{ follow.score ?? '-' }}</p>
                </CnSection>
              </div>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </el-drawer>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import mockInterviewAdminApi from '@/api/mockInterview'
import {
  CnDataTable,
  CnFilterForm,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface DirectionRecord {
  id: number | string
  directionName: string
  directionCode: string
  [key: string]: unknown
}

interface DirectionDistribution {
  directionName?: string
  direction?: string
  sessionCount?: number
  avgScore?: number | string
  completionRate?: number | string
  [key: string]: unknown
}

interface StatsOverview {
  totalSessions?: number
  completionRate?: number | string
  completedSessions?: number
  avgScore?: number | string
  activeUsers?: number
  avgDurationMinutes?: number
  ongoingSessions?: number
  interruptedSessions?: number
  directionDistributions?: DirectionDistribution[]
  [key: string]: unknown
}

interface SessionRecord {
  id: number | string
  userId?: number | string
  direction?: string
  directionName?: string
  levelName?: string
  styleName?: string
  questionCount?: number
  questionMode?: number
  status: number
  answeredCount?: number
  totalScore?: number | string | null
  createTime?: string
  endTime?: string
  [key: string]: unknown
}

interface QaRecord {
  id: number | string
  questionOrder?: number
  status?: number
  questionContent?: string
  userAnswer?: string
  score?: number | string
  knowledgePoints?: string
  referenceAnswer?: string
  aiFeedback?: string | FeedbackPayload
  followUps?: QaRecord[]
  [key: string]: unknown
}

interface SessionDetail {
  session: SessionRecord
  answeredCount?: number
  skippedCount?: number
  pendingCount?: number
  followUpCount?: number
  qaList?: QaRecord[]
  [key: string]: unknown
}

interface FeedbackPayload {
  strengths?: string[]
  improvements?: string[]
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '模拟面试' }, { label: '会话管理' }]

const loading = ref(false)
const statsLoading = ref(false)
const detailLoading = ref(false)
const directions = ref<DirectionRecord[]>([])
const tableData = ref<SessionRecord[]>([])
const stats = ref<StatsOverview>({})
const detailVisible = ref(false)
const sessionDetail = ref<SessionDetail | null>(null)

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  userId: null as string | number | null,
  direction: '',
  status: null as number | null,
  minScore: null as number | null,
  maxScore: null as number | null,
  timeRange: [] as string[]
})

const directionDistributions = computed(() => stats.value.directionDistributions || [])
const directionOptions = computed(() => directions.value.map((item) => ({ label: item.directionName, value: item.directionCode })))

const filterFields = computed<CnFilterField[]>(() => [
  { prop: 'userId', label: '用户ID', type: 'input', placeholder: '请输入用户ID' },
  { prop: 'direction', label: '面试方向', type: 'select', placeholder: '请选择方向', options: directionOptions.value },
  {
    prop: 'status',
    label: '会话状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '进行中', value: 0 },
      { label: '已完成', value: 1 },
      { label: '已中断', value: 2 }
    ]
  },
  { prop: 'timeRange', label: '时间范围', type: 'datetimerange', placeholder: '开始时间' },
  { prop: 'minScore', label: '最低分', type: 'custom', slot: 'minScore' },
  { prop: 'maxScore', label: '最高分', type: 'custom', slot: 'maxScore' }
])

const distributionColumns: CnTableColumn<DirectionDistribution>[] = [
  { prop: 'directionName', label: '方向', minWidth: 140, showOverflowTooltip: true },
  { prop: 'direction', label: '代码', minWidth: 120, showOverflowTooltip: true },
  { prop: 'sessionCount', label: '会话数', minWidth: 100, align: 'center' },
  { prop: 'avgScore', label: '平均分', minWidth: 100, align: 'center', slot: 'avgScore' },
  { prop: 'completionRate', label: '完成率', minWidth: 120, align: 'center', slot: 'completionRate' }
]

const sessionColumns: CnTableColumn<SessionRecord>[] = [
  { prop: 'id', label: '会话ID', minWidth: 90 },
  { prop: 'userId', label: '用户ID', minWidth: 90 },
  { prop: 'directionName', label: '方向', minWidth: 120, showOverflowTooltip: true },
  { prop: 'levelName', label: '难度', minWidth: 100, showOverflowTooltip: true },
  { prop: 'styleName', label: '风格', minWidth: 100, showOverflowTooltip: true },
  { prop: 'questionCount', label: '题数', minWidth: 80, align: 'center' },
  { prop: 'questionMode', label: '模式', minWidth: 90, align: 'center', slot: 'questionMode' },
  { prop: 'status', label: '状态', minWidth: 100, align: 'center', slot: 'status' },
  { prop: 'answeredCount', label: '已回答主题', minWidth: 110, align: 'center' },
  { prop: 'totalScore', label: '总分', minWidth: 80, align: 'center', slot: 'totalScore' },
  { prop: 'createTime', label: '创建时间', minWidth: 170, showOverflowTooltip: true },
  { prop: 'endTime', label: '结束时间', minWidth: 170, showOverflowTooltip: true },
  { label: '操作', width: 110, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50]
}))

const statusText = (status?: number) => ({ 0: '进行中', 1: '已完成', 2: '已中断' })[Number(status)] || '未知'
const statusTone = (status?: number): CnTone => ({ 0: 'warning', 1: 'success', 2: 'info' })[Number(status)] as CnTone || 'neutral'
const qaStatusText = (status?: number) => ({ 0: '待回答', 1: '已回答', 2: '已跳过' })[Number(status)] || '未知'
const questionModeText = (mode?: number) => (mode === 2 ? 'AI出题' : '本地题库')

const scoreTone = (value?: number | string | null): CnTone => {
  const score = Number(value ?? 0)
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'neutral'
}

const toPercent = (value?: number | string) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const toNumber = (value?: number | string, digits = 0) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(digits) : (0).toFixed(digits)
}

const parseFeedback = (raw?: string | FeedbackPayload): FeedbackPayload | null => {
  if (!raw) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch {
    return null
  }
}

const buildQuery = () => {
  const [startTime, endTime] = filters.timeRange || []
  const rawUserId = filters.userId !== null && filters.userId !== undefined ? String(filters.userId).trim() : ''
  const userId = /^\d+$/.test(rawUserId) ? Number(rawUserId) : undefined
  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    userId,
    direction: filters.direction || undefined,
    status: filters.status,
    minScore: filters.minScore,
    maxScore: filters.maxScore,
    startTime,
    endTime
  }
}

const loadDirections = async () => {
  try {
    directions.value = await mockInterviewAdminApi.getDirections()
  } catch {
    ElMessage.error('加载方向配置失败')
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const [startTime, endTime] = filters.timeRange || []
    stats.value = await mockInterviewAdminApi.getStatsOverview({ startTime, endTime })
  } catch {
    ElMessage.error('加载运营指标失败')
  } finally {
    statsLoading.value = false
  }
}

const loadSessions = async () => {
  loading.value = true
  try {
    const res = await mockInterviewAdminApi.getSessions(buildQuery())
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch {
    ElMessage.error('加载会话列表失败')
  } finally {
    loading.value = false
  }
}

const loadAllData = async () => {
  await Promise.all([loadStats(), loadSessions()])
}

const openDetail = async (row: SessionRecord) => {
  detailVisible.value = true
  detailLoading.value = true
  sessionDetail.value = null
  try {
    sessionDetail.value = await mockInterviewAdminApi.getSessionDetail(row.id)
  } catch {
    ElMessage.error('加载会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(filters, value)
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadAllData()
}

const handleReset = async () => {
  Object.assign(filters, {
    userId: null,
    direction: '',
    status: null,
    minScore: null,
    maxScore: null,
    timeRange: []
  })
  pagination.pageNum = 1
  await loadAllData()
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadSessions()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadSessions()
}

onMounted(async () => {
  await loadDirections()
  await loadAllData()
})
</script>

<style scoped>
.mock-interview-sessions-page {
  min-height: 100%;
}

.mock-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.detail-descriptions {
  margin-bottom: var(--cn-space-4);
}

.qa-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-4);
}

.qa-block p,
.follow-up-item p {
  margin: 8px 0;
  line-height: 1.6;
  word-break: break-word;
}

.follow-up-block {
  margin-top: var(--cn-space-4);
}

.follow-up-block h4 {
  margin: 0 0 var(--cn-space-3);
}

.follow-up-item {
  margin-bottom: var(--cn-space-3);
}

@media (max-width: 1180px) {
  .mock-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .mock-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
