<template>
  <CnPage class="mock-interview-history" surface="transparent" max-width="1280px" full-height>
    <CnPageHeader
      title="面试历史"
      description="复盘过往 AI 模拟面试，继续未完成会话，查看评分报告并清理无效记录。"
      eyebrow="Interview History"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">记录 {{ total }}</CnStatusTag>
        <CnStatusTag type="success" size="sm">本页完成 {{ completedCount }}</CnStatusTag>
        <CnStatusTag type="warning" size="sm">进行中 {{ runningCount }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回入口
        </el-button>
        <el-button :loading="loading" @click="fetchHistory">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <section class="history-summary-grid" aria-label="面试历史概览">
      <CnStatCard
        title="历史记录"
        :value="total"
        unit="条"
        description="当前筛选条件下的模拟面试记录"
        tone="brand"
        trend="flat"
        trend-text="记录"
        :loading="loading"
      />
      <CnStatCard
        title="本页完成"
        :value="completedCount"
        unit="场"
        description="当前页已完成并可查看报告的面试"
        tone="success"
        trend="flat"
        trend-text="完成"
        :loading="loading"
      />
      <CnStatCard
        title="本页平均分"
        :value="averageScore"
        description="按当前页已有分数计算"
        tone="warning"
        trend="flat"
        trend-text="表现"
        :loading="loading"
      />
      <CnStatCard
        title="进行中"
        :value="runningCount"
        unit="场"
        description="可以继续作答的未完成会话"
        tone="info"
        trend="flat"
        trend-text="待办"
        :loading="loading"
      />
    </section>

    <CnSection
      title="筛选条件"
      description="按面试方向和会话状态快速定位历史记录。"
      surface="panel"
      divided
    >
      <CnFilterForm
        v-model="filters"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        search-text="查询"
        reset-text="重置"
        @search="handleFilter"
        @reset="resetFilter"
      />
    </CnSection>

    <CnSection
      class="history-section"
      title="历史记录"
      description="点击已完成或已中断记录可进入报告；进行中的记录需要先继续面试。"
      surface="panel"
      divided
    >
      <template #actions>
        <CnStatusTag type="neutral" size="sm" :dot="false">
          第 {{ pageNum }} 页 / 每页 {{ pageSize }} 条
        </CnStatusTag>
      </template>

      <CnDataTable
        :columns="historyColumns"
        :data="historyList"
        :loading="loading"
        :pagination="historyPagination"
        row-key="id"
        empty-title="暂无面试记录"
        empty-description="当前筛选条件下没有模拟面试历史。"
        empty-icon="MI"
        @row-click="handleRowClick"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #direction="{ row }">
          <div class="direction-cell">
            <CnStatusTag type="brand" size="sm" :dot="false">
              {{ row.direction || '未知方向' }}
            </CnStatusTag>
            <strong>{{ row.directionName || row.direction || '未命名方向' }}</strong>
          </div>
        </template>

        <template #detail="{ row }">
          <div class="detail-cell">
            <span>
              <em>难度</em>
              {{ row.levelName || getLevelText(row.level) }}
            </span>
            <span>
              <em>题数</em>
              {{ row.questionCount || 0 }} 题
            </span>
            <span>
              <em>创建</em>
              {{ formatDate(row.createTime) }}
            </span>
          </div>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ getStatusText(row.status) }}
          </CnStatusTag>
        </template>

        <template #score="{ row }">
          <div v-if="hasScore(row.totalScore)" class="score-cell">
            <strong :class="getScoreClass(row.totalScore)">{{ row.totalScore }}</strong>
            <span>分</span>
          </div>
          <CnStatusTag v-else type="neutral" size="sm" :dot="false">未完成</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="row-actions">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click.stop="continueInterview(row)"
            >
              <el-icon><VideoPlay /></el-icon>
              继续面试
            </el-button>
            <el-button
              v-else
              type="primary"
              size="small"
              text
              @click.stop="viewReport(row)"
            >
              <el-icon><Document /></el-icon>
              查看报告
            </el-button>
            <el-button type="danger" size="small" text @click.stop="deleteInterview(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Delete,
  Document,
  Refresh,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { mockInterviewApi } from '@/api/mockInterview'
import type { CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface InterviewDirection {
  directionCode: string
  directionName: string
}

interface InterviewHistoryItem {
  id?: number | string
  sessionId?: number | string
  direction?: string
  directionName?: string
  level?: number
  levelName?: string
  questionCount?: number
  createTime?: string
  status: number
  totalScore?: number | null
}

interface InterviewHistoryResponse {
  records?: InterviewHistoryItem[]
  total?: number
}

const router = useRouter()

const loading = ref(false)
const directions = ref<InterviewDirection[]>([])
const historyList = ref<InterviewHistoryItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filters = ref<Record<string, unknown>>({
  direction: '',
  status: null
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: 'AI 模拟面试', to: '/mock-interview' },
  { label: '面试历史' }
]

const filterFields = computed<CnFilterField[]>(() => [
  {
    prop: 'direction',
    label: '面试方向',
    type: 'select',
    placeholder: '全部方向',
    options: directions.value.map(direction => ({
      label: direction.directionName,
      value: direction.directionCode
    }))
  },
  {
    prop: 'status',
    label: '面试状态',
    type: 'select',
    placeholder: '全部状态',
    options: [
      { label: '进行中', value: 0 },
      { label: '已完成', value: 1 },
      { label: '已中断', value: 2 }
    ]
  }
])

const historyColumns: CnTableColumn<InterviewHistoryItem>[] = [
  { label: '面试方向', slot: 'direction', minWidth: 180 },
  { label: '面试信息', slot: 'detail', minWidth: 280 },
  { label: '状态', slot: 'status', width: 120, align: 'center' },
  { label: '分数', slot: 'score', width: 120, align: 'center' },
  { label: '操作', slot: 'actions', width: 210, fixed: 'right', align: 'right' }
]

const normalizedDirection = computed(() => String(filters.value.direction || ''))

const normalizedStatus = computed<number | null>(() => {
  const status = filters.value.status
  if (status === null || status === undefined || status === '') return null
  return Number(status)
})

const completedCount = computed(() => {
  return historyList.value.filter(item => item.status === 1).length
})

const runningCount = computed(() => {
  return historyList.value.filter(item => item.status === 0).length
})

const averageScore = computed(() => {
  const scores = historyList.value
    .map(item => item.totalScore)
    .filter(hasScore)
  if (!scores.length) return 0
  return Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length)
})

const historyPagination = computed<CnPagination | null>(() => {
  if (total.value <= 0) return null
  return {
    page: pageNum.value,
    pageSize: pageSize.value,
    total: total.value,
    pageSizes: [10, 20, 30],
    layout: 'total, sizes, prev, pager, next, jumper'
  }
})

const getSessionId = (item: InterviewHistoryItem) => {
  return item.id || item.sessionId
}

const hasScore = (score: unknown): score is number => {
  return typeof score === 'number' && Number.isFinite(score)
}

const getStatusTone = (status: number): CnTone => {
  const tones: Record<number, CnTone> = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return tones[status] || 'neutral'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = {
    0: '进行中',
    1: '已完成',
    2: '已中断'
  }
  return texts[status] || '未知'
}

const getLevelText = (level?: number) => {
  const texts: Record<number, string> = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return level ? texts[level] || '未知' : '未知'
}

const getScoreClass = (score: number) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-medium'
  return 'score-low'
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return date.toLocaleString('zh-CN')
}

const goBack = () => {
  router.push('/mock-interview')
}

const fetchDirections = async () => {
  try {
    const data = (await mockInterviewApi.getDirections()) as InterviewDirection[]
    directions.value = data || []
  } catch (error) {
    console.error('获取方向列表失败', error)
  }
}

const fetchHistory = async () => {
  loading.value = true
  try {
    const data = (await mockInterviewApi.getHistory({
      direction: normalizedDirection.value,
      status: normalizedStatus.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })) as InterviewHistoryResponse

    historyList.value = (data?.records || []).map(item => ({
      ...item,
      id: getSessionId(item)
    }))
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取历史记录失败', error)
    ElMessage.error('获取历史记录失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pageNum.value = 1
  fetchHistory()
}

const resetFilter = () => {
  filters.value = {
    direction: '',
    status: null
  }
  pageNum.value = 1
  fetchHistory()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  fetchHistory()
}

const handlePageChange = (page: number) => {
  pageNum.value = page
  fetchHistory()
}

const continueInterview = (item: InterviewHistoryItem) => {
  const sessionId = getSessionId(item)
  if (!sessionId) return
  router.push({
    path: '/mock-interview/session',
    query: { sessionId }
  })
}

const viewReport = (item: InterviewHistoryItem) => {
  if (item.status === 0) {
    ElMessage.warning('面试尚未完成，请先完成面试')
    return
  }
  const sessionId = getSessionId(item)
  if (!sessionId) return
  router.push({
    path: '/mock-interview/report',
    query: { sessionId }
  })
}

const handleRowClick = (row: unknown) => {
  viewReport(row as InterviewHistoryItem)
}

const deleteInterview = async (item: InterviewHistoryItem) => {
  const sessionId = getSessionId(item)
  if (!sessionId) return
  try {
    await ElMessageBox.confirm('确定要删除这条面试记录吗？删除后无法恢复。', '提示', {
      type: 'warning'
    })

    await mockInterviewApi.deleteInterview(sessionId)
    ElMessage.success('删除成功')
    fetchHistory()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchDirections()
  fetchHistory()
})
</script>

<style scoped>
.mock-interview-history {
  min-height: calc(100vh - 74px);
}

.history-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.history-section {
  min-width: 0;
}

.history-section :deep(.el-table__row) {
  cursor: pointer;
}

.direction-cell {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-2);
  justify-items: start;
}

.direction-cell strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-cell {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.detail-cell span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.detail-cell em {
  color: var(--cn-color-text-tertiary);
  font-style: normal;
  font-weight: 650;
}

.score-cell {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  min-width: 72px;
  gap: 4px;
}

.score-cell strong {
  font-family: var(--cn-font-heading);
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
}

.score-cell span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.score-high {
  color: var(--cn-color-success);
}

.score-medium {
  color: var(--cn-color-warning);
}

.score-low {
  color: var(--cn-color-danger);
}

.row-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-1);
}

@media (max-width: 1180px) {
  .history-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .history-summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .row-actions {
    justify-content: flex-start;
  }
}
</style>
