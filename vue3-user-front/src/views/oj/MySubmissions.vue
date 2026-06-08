<template>
  <CnPage class="oj-submissions" surface="transparent" max-width="1280px">
    <CnPageHeader
      class="cn-learn-reveal"
      title="我的提交记录"
      description="集中查看每次判题结果、耗时与内存占用，快速回到题目或查看提交详情。"
      eyebrow="OJ Submissions"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          共 {{ ojStore.submissionsTotal }} 条
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          已通过 {{ acceptedCount }}
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">
          当前页 {{ queryParams.pageNum }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj')">
          <el-icon><ArrowLeft /></el-icon>
          返回题目列表
        </el-button>
        <el-button type="primary" :loading="ojStore.submissionsLoading" @click="loadData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <section class="submission-stats cn-learn-reveal" aria-label="提交数据概览">
      <CnStatCard
        title="提交总数"
        :value="ojStore.submissionsTotal"
        unit="次"
        description="我的历史提交记录"
        tone="brand"
        trend="flat"
        :loading="ojStore.submissionsLoading"
        trend-text="实时"
      />
      <CnStatCard
        title="本页通过"
        :value="acceptedCount"
        unit="次"
        description="当前页 Accepted 记录"
        tone="success"
        trend="flat"
        :loading="ojStore.submissionsLoading"
        :trend-text="acceptedRateText"
      />
      <CnStatCard
        title="等待判题"
        :value="pendingCount"
        unit="次"
        description="等待或正在判题的提交"
        tone="info"
        trend="flat"
        :loading="ojStore.submissionsLoading"
        trend-text="队列"
      />
      <CnStatCard
        title="平均耗时"
        :value="averageTimeText"
        description="当前页有耗时记录的提交"
        tone="warning"
        trend="flat"
        :loading="ojStore.submissionsLoading"
        trend-text="性能"
      />
    </section>

    <CnSection
      class="submission-section cn-learn-reveal"
      title="提交列表"
      :description="tableDescription"
      surface="panel"
      divided
    >
      <template #actions>
        <CnStatusTag v-if="ojStore.submissions.length > 0" type="neutral" size="sm">
          每页 {{ queryParams.pageSize }} 条
        </CnStatusTag>
      </template>

      <CnDataTable
        :columns="submissionColumns"
        :data="ojStore.submissions"
        :loading="ojStore.submissionsLoading"
        :pagination="pagination"
        empty-title="暂无提交记录"
        empty-description="开始刷题并提交代码后，这里会显示你的判题历史。"
        empty-icon="OJ"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #createTime="{ row }">
          <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
        </template>

        <template #problem="{ row }">
          <button class="problem-link" type="button" @click="goToProblem(row.problemId)">
            <span>{{ row.problemTitle || `#${row.problemId}` }}</span>
          </button>
        </template>

        <template #language="{ row }">
          <CnStatusTag type="neutral" size="sm" :dot="false" subtle>
            {{ getLanguageLabel(row.language) }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusType(row.status)" size="sm">
            {{ getStatusLabel(row.status) }}
          </CnStatusTag>
        </template>

        <template #timeUsed="{ row }">
          <span class="metric-cell">{{ formatTime(row.timeUsed) }}</span>
        </template>

        <template #memoryUsed="{ row }">
          <span class="metric-cell">{{ formatMemory(row.memoryUsed) }}</span>
        </template>

        <template #actions="{ row }">
          <el-button type="primary" size="small" text @click="goToSubmission(row.id)">
            详情
          </el-button>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnPagination, CnTableColumn, CnTone } from '@/design-system'
import { useOjStore } from '@/stores/oj'
import { useRevealMotion } from '@/utils/reveal-motion'

interface SubmissionRecord extends Record<string, unknown> {
  id: number | string
  problemId: number | string
  problemTitle?: string
  language?: string
  status?: string
  timeUsed?: number | null
  memoryUsed?: number | null
  createTime?: string
}

const router = useRouter()
const ojStore = useOjStore()
useRevealMotion('.oj-submissions .cn-learn-reveal')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 15
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '我的提交' }
]

const submissionColumns: CnTableColumn<SubmissionRecord>[] = [
  { prop: 'createTime', label: '提交时间', width: 180, slot: 'createTime' },
  { prop: 'problemTitle', label: '题目', minWidth: 220, slot: 'problem', showOverflowTooltip: true },
  { prop: 'language', label: '语言', width: 120, align: 'center', slot: 'language' },
  { prop: 'status', label: '状态', width: 140, align: 'center', slot: 'status' },
  { prop: 'timeUsed', label: '耗时', width: 100, align: 'center', slot: 'timeUsed' },
  { prop: 'memoryUsed', label: '内存', width: 100, align: 'center', slot: 'memoryUsed' },
  { label: '操作', width: 90, align: 'center', slot: 'actions' }
]

const pagination = computed<CnPagination | null>(() => {
  if (ojStore.submissionsTotal <= 0) return null
  return {
    page: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    total: ojStore.submissionsTotal,
    pageSizes: [15, 30, 50],
    layout: 'total, sizes, prev, pager, next'
  }
})

const acceptedCount = computed(() => {
  return ojStore.submissions.filter((item: SubmissionRecord) => item.status === 'accepted').length
})

const pendingCount = computed(() => {
  return ojStore.submissions.filter((item: SubmissionRecord) => ['pending', 'judging'].includes(String(item.status))).length
})

const acceptedRateText = computed(() => {
  const total = ojStore.submissions.length
  if (!total) return '0%'
  return `${Math.round((acceptedCount.value / total) * 100)}%`
})

const averageTimeText = computed(() => {
  const times = ojStore.submissions
    .map((item: SubmissionRecord) => Number(item.timeUsed))
    .filter((time: number) => Number.isFinite(time) && time > 0)

  if (!times.length) return '-'
  const average = times.reduce((sum: number, time: number) => sum + time, 0) / times.length
  return `${Math.round(average)}ms`
})

const tableDescription = computed(() => {
  return `当前第 ${queryParams.pageNum} 页，每页 ${queryParams.pageSize} 条，按提交时间倒序展示。`
})

const languageMap: Record<string, string> = {
  java: 'Java',
  cpp: 'C++',
  c: 'C',
  python: 'Python3',
  go: 'Go',
  javascript: 'JavaScript'
}

const statusMap: Record<string, { label: string; type: CnTone }> = {
  pending: { label: '等待判题', type: 'info' },
  judging: { label: '判题中', type: 'info' },
  accepted: { label: '通过', type: 'success' },
  wrong_answer: { label: '答案错误', type: 'danger' },
  time_limit_exceeded: { label: '超时', type: 'warning' },
  memory_limit_exceeded: { label: '超内存', type: 'warning' },
  runtime_error: { label: '运行错误', type: 'danger' },
  compile_error: { label: '编译错误', type: 'danger' },
  system_error: { label: '系统错误', type: 'danger' }
}

const getLanguageLabel = (lang?: string) => {
  if (!lang) return '-'
  return languageMap[lang] || lang
}

const getStatusLabel = (status?: string) => {
  if (!status) return '-'
  return statusMap[status]?.label || status
}

const getStatusType = (status?: string): CnTone => {
  if (!status) return 'neutral'
  return statusMap[status]?.type || 'info'
}

const formatTime = (time?: number | null) => {
  return time != null ? `${time}ms` : '-'
}

const formatMemory = (kb?: number | null) => {
  if (!kb) return '-'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

const formatDateTime = (value?: string) => {
  return value || '-'
}

const loadData = () => {
  ojStore.fetchMySubmissions(queryParams, { forceRefresh: true })
}

const handleSizeChange = (size: number) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadData()
}

const handleCurrentChange = (page: number) => {
  queryParams.pageNum = page
  loadData()
}

const goToProblem = (problemId: SubmissionRecord['problemId']) => {
  router.push(`/oj/problem/${problemId}`)
}

const goToSubmission = (submissionId: SubmissionRecord['id']) => {
  router.push(`/oj/submission/${submissionId}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.oj-submissions {
  min-height: calc(100vh - 68px);
}

.submission-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.submission-section :deep(.cn-section__body) {
  padding: 0;
}

.time-cell,
.metric-cell {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.metric-cell {
  font-weight: 650;
}

.problem-link {
  display: inline-flex;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--cn-color-brand-primary);
  cursor: pointer;
  font: inherit;
  font-weight: 650;
  line-height: 1.5;
  text-align: left;
}

.problem-link span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.problem-link:hover {
  color: var(--cn-color-brand-hover);
  text-decoration: underline;
}

@media (max-width: 1024px) {
  .submission-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .submission-stats {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
