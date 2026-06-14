<template>
  <CnPage class="learning-assets-page" surface="transparent" max-width="1440px" full-height>
    <CnPageHeader
      title="我的学习资产"
      description="把博客、帖子、作品和面试报告沉淀成可复习、可训练、可追踪的学习资产。"
      eyebrow="Learning Assets"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">记录 {{ recordList.total || 0 }}</CnStatusTag>
        <CnStatusTag v-if="currentRecord" :type="recordTone(currentRecord.status)" size="sm">
          {{ currentRecord.statusText || currentRecord.status }}
        </CnStatusTag>
        <CnStatusTag v-if="currentRecord" type="success" size="sm">
          已发布 {{ currentRecord.publishedCandidates || 0 }}/{{ currentRecord.totalCandidates || 0 }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :loading="recordsLoading" @click="loadRecords">
          <el-icon><Refresh /></el-icon>
          刷新列表
        </el-button>
        <el-button type="primary" @click="router.push('/learning-cockpit')">
          <el-icon><DataAnalysis /></el-icon>
          返回学习驾驶舱
        </el-button>
      </template>
    </CnPageHeader>

    <section class="asset-summary-grid" aria-label="学习资产概览">
      <CnStatCard
        title="转化记录"
        :value="recordList.total || 0"
        unit="条"
        description="当前筛选条件下的转化记录"
        tone="brand"
        trend="flat"
        trend-text="记录"
        :loading="recordsLoading"
      />
      <CnStatCard
        title="候选资产"
        :value="currentRecord?.totalCandidates || 0"
        unit="项"
        description="当前记录生成的可发布候选项"
        tone="info"
        trend="flat"
        trend-text="候选"
        :loading="detailLoading"
      />
      <CnStatCard
        title="已发布"
        :value="currentRecord?.publishedCandidates || 0"
        unit="项"
        description="已落地到闪卡、计划或审核池"
        tone="success"
        trend="up"
        trend-text="落地"
        :loading="detailLoading"
      />
      <CnStatCard
        title="审核中"
        :value="reviewingCandidateCount"
        unit="项"
        description="需要后续在通知中心跟进"
        tone="warning"
        trend="flat"
        trend-text="审核"
        :loading="detailLoading"
      />
    </section>

    <div class="page-grid">
      <CnSection class="record-panel" title="转化记录" description="按来源和状态筛选资产转化记录。" surface="panel" divided>
        <template #actions>
          <CnStatusTag type="neutral" size="sm" :dot="false">{{ recordList.records.length }} 条</CnStatusTag>
        </template>

        <div class="filter-row">
          <el-select v-model="filters.sourceType" clearable placeholder="来源类型">
            <el-option label="博客文章" value="blog" />
            <el-option label="社区帖子" value="community" />
            <el-option label="CodePen 作品" value="codepen" />
            <el-option label="模拟面试报告" value="mock_interview" />
          </el-select>
          <el-select v-model="filters.status" clearable placeholder="状态">
            <el-option label="待确认" value="PENDING_CONFIRM" />
            <el-option label="审核中" value="REVIEWING" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="部分发布" value="PARTIAL_PUBLISHED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-button type="primary" @click="handleSearch">筛选</el-button>
        </div>

        <CnDataTable
          :columns="recordColumns"
          :data="recordList.records"
          :loading="recordsLoading"
          row-key="recordId"
          empty-title="暂无转化记录"
          empty-description="当前筛选条件下还没有学习资产转化记录。"
          empty-icon="LA"
          :pagination="recordPagination"
          @row-click="handleRecordSelect"
          @page-change="handlePageChange"
          @page-size-change="handleSizeChange"
        >
          <template #source="{ row }">
            <div class="record-title">{{ row.sourceTitle }}</div>
            <div class="record-sub">{{ sourceTypeText(row.sourceType) }}</div>
          </template>
          <template #status="{ row }">
            <CnStatusTag :type="recordTone(row.status)" size="sm">
              {{ row.statusText || row.status }}
            </CnStatusTag>
          </template>
          <template #published="{ row }">
            {{ row.publishedCandidates }}/{{ row.totalCandidates }}
          </template>
        </CnDataTable>
      </CnSection>

      <CnSection class="detail-panel" title="记录详情" description="确认候选资产后发布到闪卡、计划或审核流程。" surface="panel" divided>
        <template #actions>
          <div v-if="currentRecord" class="detail-actions">
            <el-button @click="handleRetry">重新转化</el-button>
            <el-button type="success" @click="handleConfirm">保存选择</el-button>
            <el-button type="primary" @click="handlePublish">发布资产</el-button>
          </div>
        </template>

        <div v-loading="detailLoading" class="detail-body">
          <CnEmptyState
            v-if="!currentRecord"
            title="请选择转化记录"
            description="从左侧选择一条记录后，可以查看候选资产、编辑内容并发布。"
            icon="REC"
            surface="transparent"
          />

          <template v-else>
            <div class="detail-summary">
              <div class="summary-top">
                <div>
                  <div class="summary-title">{{ currentRecord.sourceTitle }}</div>
                  <div class="summary-meta">
                    <CnStatusTag type="neutral" size="sm" :dot="false">
                      {{ sourceTypeText(currentRecord.sourceType) }}
                    </CnStatusTag>
                    <CnStatusTag :type="recordTone(currentRecord.status)" size="sm">
                      {{ currentRecord.statusText || currentRecord.status }}
                    </CnStatusTag>
                  </div>
                </div>
                <div class="summary-stats">
                  <div class="stat-box">
                    <span class="stat-label">候选总数</span>
                    <strong>{{ currentRecord.totalCandidates || 0 }}</strong>
                  </div>
                  <div class="stat-box">
                    <span class="stat-label">已发布</span>
                    <strong>{{ currentRecord.publishedCandidates || 0 }}</strong>
                  </div>
                </div>
              </div>

              <div class="summary-body">
                <div class="summary-block">
                  <div class="block-label">转化摘要</div>
                  <div class="block-text">{{ currentRecord.summaryText || '暂无摘要' }}</div>
                </div>
                <div v-if="sourceSnapshotSummary" class="summary-block">
                  <div class="block-label">来源摘要</div>
                  <div class="block-text">{{ sourceSnapshotSummary }}</div>
                </div>
                <div v-if="currentRecord.failReason" class="summary-block">
                  <div class="block-label danger">失败原因</div>
                  <div class="block-text">{{ currentRecord.failReason }}</div>
                </div>
              </div>

              <el-alert
                v-if="publishFeedbackText"
                class="publish-alert"
                type="success"
                :closable="false"
                :title="publishFeedbackText"
              />

              <div v-if="hasQuickActions" class="quick-actions">
                <el-button
                  v-if="publishedFlashcardDeckId"
                  type="primary"
                  @click="router.push(`/flashcard/study/${publishedFlashcardDeckId}`)"
                >
                  立即学习闪卡
                </el-button>
                <el-button v-if="publishedPlanIds.length" @click="router.push('/plan')">
                  查看今日计划
                </el-button>
                <el-button v-if="reviewingCandidateCount" @click="router.push('/notification')">
                  查看通知中心
                </el-button>
              </div>
            </div>

            <div class="candidate-toolbar">
              <span>候选资产</span>
              <div class="toolbar-hint">勾选保留项，确认后发布到闪卡、计划或审核池；已丢弃项也可恢复。</div>
            </div>

            <div class="candidate-list">
              <label
                v-for="candidate in currentRecord.candidates"
                :key="candidate.id"
                class="candidate-card"
                :class="{ active: selectedCandidateIds.includes(candidate.id) }"
              >
                <div class="candidate-head">
                  <el-checkbox
                    :model-value="selectedCandidateIds.includes(candidate.id)"
                    :disabled="isCandidateLocked(candidate)"
                    @change="toggleCandidate(candidate.id, $event)"
                  />
                  <div class="candidate-meta">
                    <div class="candidate-title">{{ candidate.title }}</div>
                    <div class="candidate-sub">
                      <CnStatusTag type="neutral" size="sm" :dot="false">
                        {{ assetTypeText(candidate.assetType) }}
                      </CnStatusTag>
                      <CnStatusTag :type="candidateTone(candidate.status)" size="sm">
                        {{ candidate.statusText || candidate.status }}
                      </CnStatusTag>
                      <span v-if="candidate.confidenceScore" class="confidence">
                        置信度 {{ Math.round(candidate.confidenceScore * 100) }}%
                      </span>
                    </div>
                  </div>
                  <div class="candidate-actions">
                    <el-button
                      v-if="candidate.status === 'DISCARDED'"
                      text
                      @click.prevent="restoreCandidateSelection(candidate.id)"
                    >
                      恢复选择
                    </el-button>
                    <el-button
                      v-if="canEditCandidate(candidate)"
                      text
                      type="primary"
                      @click.prevent="openEditor(candidate)"
                    >
                      编辑
                    </el-button>
                    <el-button
                      v-if="canDiscardCandidate(candidate)"
                      text
                      type="danger"
                      :loading="discardingCandidateId === candidate.id"
                      @click.prevent="handleDiscard(candidate)"
                    >
                      丢弃
                    </el-button>
                  </div>
                </div>

                <div class="candidate-body">
                  <div class="candidate-preview">{{ formatCandidatePreview(candidate) }}</div>
                  <div v-if="candidate.tags" class="candidate-tags">
                    <CnStatusTag
                      v-for="tag in splitTags(candidate.tags)"
                      :key="`${candidate.id}-${tag}`"
                      type="info"
                      size="sm"
                      :dot="false"
                      subtle
                    >
                      {{ tag }}
                    </CnStatusTag>
                  </div>
                  <div v-if="candidate.targetId" class="candidate-target">
                    已落地到 {{ candidate.targetModule }} / ID: {{ candidate.targetId }}
                  </div>
                  <div v-if="candidate.reviewNote" class="candidate-note">
                    备注：{{ candidate.reviewNote }}
                  </div>
                  <div v-if="candidate.status === 'DISCARDED'" class="candidate-note">
                    当前已丢弃，你可以点击“恢复选择”重新加入本次发布。
                  </div>
                </div>
              </label>
            </div>
          </template>
        </div>
      </CnSection>
    </div>

    <el-dialog v-model="editorVisible" title="编辑候选资产" width="640px">
      <el-form label-position="top">
        <el-form-item label="标题">
          <el-input v-model="editorForm.title" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editorForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="难度建议">
          <el-input v-model="editorForm.difficulty" placeholder="例如：初级 / 中级 / 高级" />
        </el-form-item>
        <el-form-item label="结构化内容 JSON">
          <el-input
            v-model="editorForm.contentJson"
            type="textarea"
            :rows="10"
            placeholder="请保持 JSON 格式"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingCandidate" @click="saveCandidate">
            保存修改
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Refresh } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import learningAssetApi from '@/api/learningAssets'
import type { CnPagination, CnTableColumn, CnTone } from '@/design-system'

type RecordStatus = 'PENDING_CONFIRM' | 'REVIEWING' | 'PUBLISHED' | 'PARTIAL_PUBLISHED' | 'FAILED' | string
type CandidateStatus = 'DRAFT' | 'SELECTED' | 'REVIEWING' | 'PUBLISHED' | 'DISCARDED' | 'REJECTED' | string

interface LearningAssetRecord {
  recordId: number
  sourceType: string
  sourceTitle: string
  sourceSnapshot?: string
  status: RecordStatus
  statusText?: string
  totalCandidates?: number
  publishedCandidates?: number
  summaryText?: string
  failReason?: string
  createTime?: string
  candidates?: LearningAssetCandidate[]
}

interface LearningAssetCandidate {
  id: number
  title: string
  assetType: string
  status: CandidateStatus
  statusText?: string
  confidenceScore?: number
  tags?: string
  targetId?: number | string
  targetModule?: string
  reviewNote?: string
  contentJson?: string
  difficulty?: string
}

interface RecordList {
  total: number
  records: LearningAssetRecord[]
}

interface PublishResult {
  publishedCount?: number
  reviewingCount?: number
}

const route = useRoute()
const router = useRouter()

const recordsLoading = ref(false)
const detailLoading = ref(false)
const editorVisible = ref(false)
const savingCandidate = ref(false)
const discardingCandidateId = ref<number | null>(null)
const currentRecord = ref<LearningAssetRecord | null>(null)
const editingCandidateId = ref<number | null>(null)
const selectedCandidateIds = ref<number[]>([])
const lastPublishResult = ref<PublishResult | null>(null)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '学习驾驶舱', to: '/learning-cockpit' },
  { label: '学习资产' }
]

const filters = reactive({
  sourceType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

const recordList = reactive<RecordList>({
  total: 0,
  records: []
})

const editorForm = reactive({
  title: '',
  tags: '',
  difficulty: '',
  contentJson: ''
})

const recordColumns: CnTableColumn<LearningAssetRecord>[] = [
  { label: '来源内容', slot: 'source', minWidth: 220 },
  { label: '状态', slot: 'status', minWidth: 120 },
  { label: '已发布', slot: 'published', minWidth: 92, align: 'center' },
  { prop: 'createTime', label: '发起时间', minWidth: 170, showOverflowTooltip: true }
]

const recordPagination = computed<CnPagination>(() => ({
  page: filters.pageNum,
  pageSize: filters.pageSize,
  total: recordList.total || 0,
  pageSizes: [5, 10, 20],
  layout: 'total, sizes, prev, pager, next'
}))

const sourceSnapshotSummary = computed(() => {
  if (!currentRecord.value?.sourceSnapshot) return ''
  try {
    const snapshot = JSON.parse(currentRecord.value.sourceSnapshot)
    return snapshot.summary || snapshot.content || ''
  } catch (error) {
    return ''
  }
})

const publishedFlashcardDeckId = computed(() => {
  const flashcardCandidate = (currentRecord.value?.candidates || []).find(candidate =>
    candidate.assetType === 'flashcard' && candidate.status === 'PUBLISHED' && candidate.targetId
  )
  return flashcardCandidate?.targetId || null
})

const publishedPlanIds = computed(() => {
  const ids = new Set<number | string>()
  ;(currentRecord.value?.candidates || []).forEach(candidate => {
    if (candidate.assetType === 'practice_plan' && candidate.status === 'PUBLISHED' && candidate.targetId) {
      ids.add(candidate.targetId)
    }
  })
  return Array.from(ids)
})

const reviewingCandidateCount = computed(() => {
  return (currentRecord.value?.candidates || []).filter(candidate => candidate.status === 'REVIEWING').length
})

const publishFeedbackText = computed(() => {
  if (lastPublishResult.value) {
    return `最近一次发布：${lastPublishResult.value.publishedCount || 0} 项已落地，${lastPublishResult.value.reviewingCount || 0} 项进入审核。`
  }
  if (reviewingCandidateCount.value > 0) {
    return `当前记录有 ${reviewingCandidateCount.value} 项候选资产正在审核中，审核结果会同步到通知中心。`
  }
  if ((currentRecord.value?.publishedCandidates || 0) > 0) {
    return `当前记录已有 ${currentRecord.value?.publishedCandidates} 项学习资产成功发布，可直接进入学习或计划中心。`
  }
  return ''
})

const hasQuickActions = computed(() => {
  return Boolean(publishedFlashcardDeckId.value || publishedPlanIds.value.length || reviewingCandidateCount.value)
})

const sourceTypeText = (type: string) => {
  const map: Record<string, string> = {
    blog: '博客文章',
    community: '社区帖子',
    codepen: 'CodePen 作品',
    mock_interview: '模拟面试报告'
  }
  return map[type] || type
}

const assetTypeText = (type: string) => {
  const map: Record<string, string> = {
    flashcard: '闪卡卡组',
    knowledge_node: '知识节点候选',
    practice_plan: '练习清单',
    interview_question: '面试题草稿'
  }
  return map[type] || type
}

const recordTone = (status: RecordStatus): CnTone => {
  const map: Record<string, CnTone> = {
    PENDING_CONFIRM: 'warning',
    REVIEWING: 'info',
    PUBLISHED: 'success',
    PARTIAL_PUBLISHED: 'brand',
    FAILED: 'danger'
  }
  return map[status] || 'neutral'
}

const candidateTone = (status: CandidateStatus): CnTone => {
  const map: Record<string, CnTone> = {
    DRAFT: 'neutral',
    SELECTED: 'warning',
    REVIEWING: 'info',
    PUBLISHED: 'success',
    DISCARDED: 'danger',
    REJECTED: 'danger'
  }
  return map[status] || 'neutral'
}

const splitTags = (tags?: string) => {
  return String(tags || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const isCandidateLocked = (candidate: LearningAssetCandidate) => {
  return ['PUBLISHED', 'REVIEWING'].includes(candidate.status)
}

const canEditCandidate = (candidate: LearningAssetCandidate) => {
  return ['DRAFT', 'SELECTED'].includes(candidate.status)
}

const canDiscardCandidate = (candidate: LearningAssetCandidate) => {
  return ['DRAFT', 'SELECTED'].includes(candidate.status)
}

const formatCandidatePreview = (candidate: LearningAssetCandidate) => {
  try {
    const payload = JSON.parse(candidate.contentJson || '{}')
    if (candidate.assetType === 'flashcard') {
      return `${payload.frontContent || ''} / ${payload.backContent || ''}`
    }
    if (candidate.assetType === 'practice_plan') {
      return `${payload.planName || candidate.title} · 目标 ${payload.targetValue || 1}${payload.targetUnit || '次'}`
    }
    if (candidate.assetType === 'knowledge_node') {
      return payload.summary || candidate.title
    }
    if (candidate.assetType === 'interview_question') {
      return payload.answer || candidate.title
    }
    return candidate.contentJson
  } catch (error) {
    return candidate.contentJson
  }
}

const initSelectedCandidates = (detail: LearningAssetRecord) => {
  const candidates = detail.candidates || []
  const hasExplicitSelection = candidates.some(item =>
    ['SELECTED', 'PUBLISHED', 'REVIEWING'].includes(item.status)
  )
  selectedCandidateIds.value = candidates
    .filter(item => {
      if (hasExplicitSelection) {
        return ['SELECTED', 'PUBLISHED', 'REVIEWING'].includes(item.status)
      }
      return ['DRAFT', 'SELECTED', 'PUBLISHED', 'REVIEWING'].includes(item.status)
    })
    .map(item => item.id)
}

const loadRecords = async () => {
  recordsLoading.value = true
  try {
    const res = (await learningAssetApi.getRecords({
      sourceType: filters.sourceType || undefined,
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    })) as RecordList
    recordList.total = res.total || 0
    recordList.records = res.records || []

    const routeRecordId = Number(route.query.recordId)
    if (routeRecordId) {
      const matched = recordList.records.find(item => item.recordId === routeRecordId)
      if (matched) {
        await loadRecordDetail(matched.recordId)
        return
      }
    }

    if (!currentRecord.value && recordList.records.length) {
      await loadRecordDetail(recordList.records[0].recordId)
    }
  } catch (error) {
    console.error('加载转化记录失败', error)
  } finally {
    recordsLoading.value = false
  }
}

const loadRecordDetail = async (recordId: number) => {
  if (!recordId) return
  if (currentRecord.value?.recordId !== recordId) {
    lastPublishResult.value = null
  }
  detailLoading.value = true
  try {
    const detail = (await learningAssetApi.getRecordDetail(recordId)) as LearningAssetRecord
    currentRecord.value = detail
    initSelectedCandidates(detail)
  } catch (error) {
    console.error('加载记录详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleRecordSelect = (row: unknown) => {
  const record = row as LearningAssetRecord | undefined
  if (record?.recordId) {
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        recordId: record.recordId
      }
    })
    loadRecordDetail(record.recordId)
  }
}

const handleSearch = () => {
  filters.pageNum = 1
  loadRecords()
}

const handlePageChange = (page: number) => {
  filters.pageNum = page
  loadRecords()
}

const handleSizeChange = (size: number) => {
  filters.pageSize = size
  filters.pageNum = 1
  loadRecords()
}

const toggleCandidate = (candidateId: number, checked: string | number | boolean) => {
  if (Boolean(checked)) {
    if (!selectedCandidateIds.value.includes(candidateId)) {
      selectedCandidateIds.value = [...selectedCandidateIds.value, candidateId]
    }
    return
  }
  selectedCandidateIds.value = selectedCandidateIds.value.filter(id => id !== candidateId)
}

const ensureConfirmedSelection = async () => {
  if (!currentRecord.value) return
  const editableCandidates = (currentRecord.value.candidates || []).filter(item =>
    ['DRAFT', 'SELECTED', 'DISCARDED'].includes(item.status)
  )
  if (!editableCandidates.length) return
  currentRecord.value = (await learningAssetApi.confirm(
    currentRecord.value.recordId,
    selectedCandidateIds.value
  )) as LearningAssetRecord
  initSelectedCandidates(currentRecord.value)
}

const handleConfirm = async () => {
  if (!currentRecord.value) return
  if (!selectedCandidateIds.value.length) {
    ElMessage.warning('请至少选择一个候选项')
    return
  }
  detailLoading.value = true
  try {
    currentRecord.value = (await learningAssetApi.confirm(
      currentRecord.value.recordId,
      selectedCandidateIds.value
    )) as LearningAssetRecord
    initSelectedCandidates(currentRecord.value)
    ElMessage.success('候选项已确认')
    await loadRecords()
  } catch (error) {
    console.error('确认候选项失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleDiscard = async (candidate: LearningAssetCandidate) => {
  if (!candidate?.id) return
  try {
    await ElMessageBox.confirm(
      '丢弃后该候选项不会进入本次发布，但后续仍可恢复重新选择。',
      '确认丢弃',
      {
        confirmButtonText: '确认丢弃',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch (error) {
    return
  }

  discardingCandidateId.value = candidate.id
  try {
    const detail = (await learningAssetApi.discardCandidate(candidate.id)) as LearningAssetRecord
    currentRecord.value = detail
    initSelectedCandidates(detail)
    selectedCandidateIds.value = selectedCandidateIds.value.filter(id => id !== candidate.id)
    lastPublishResult.value = null
    ElMessage.success('候选项已丢弃，可随时恢复')
    await loadRecords()
  } catch (error) {
    console.error('丢弃候选项失败', error)
  } finally {
    discardingCandidateId.value = null
  }
}

const restoreCandidateSelection = (candidateId: number) => {
  if (!selectedCandidateIds.value.includes(candidateId)) {
    selectedCandidateIds.value = [...selectedCandidateIds.value, candidateId]
  }
  ElMessage.success('已恢复到待确认列表，记得保存选择或直接发布')
}

const handlePublish = async () => {
  if (!currentRecord.value) return
  if (!selectedCandidateIds.value.length) {
    ElMessage.warning('请至少选择一个候选项')
    return
  }
  detailLoading.value = true
  try {
    await ensureConfirmedSelection()
    if (!currentRecord.value) return
    const result = (await learningAssetApi.publish(
      currentRecord.value.recordId,
      selectedCandidateIds.value
    )) as PublishResult
    lastPublishResult.value = result
    ElMessage.success(`发布完成：${result.publishedCount || 0} 个直接落地，${result.reviewingCount || 0} 个进入审核`)
    await loadRecordDetail(currentRecord.value.recordId)
    await loadRecords()
  } catch (error) {
    console.error('发布学习资产失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleRetry = async () => {
  if (!currentRecord.value) return
  detailLoading.value = true
  try {
    const detail = (await learningAssetApi.retry(currentRecord.value.recordId)) as LearningAssetRecord
    ElMessage.success('已重新生成候选资产')
    lastPublishResult.value = null
    currentRecord.value = detail
    initSelectedCandidates(detail)
    await loadRecords()
  } catch (error) {
    console.error('重新转化失败', error)
  } finally {
    detailLoading.value = false
  }
}

const openEditor = (candidate: LearningAssetCandidate) => {
  editingCandidateId.value = candidate.id
  editorForm.title = candidate.title
  editorForm.tags = candidate.tags || ''
  editorForm.difficulty = candidate.difficulty || ''
  editorForm.contentJson = candidate.contentJson || ''
  editorVisible.value = true
}

const saveCandidate = async () => {
  if (!editingCandidateId.value || !currentRecord.value) return
  savingCandidate.value = true
  try {
    JSON.parse(editorForm.contentJson || '{}')
    await learningAssetApi.updateCandidate(editingCandidateId.value, {
      title: editorForm.title,
      tags: editorForm.tags,
      difficulty: editorForm.difficulty,
      contentJson: editorForm.contentJson
    })
    ElMessage.success('候选资产已更新')
    editorVisible.value = false
    await loadRecordDetail(currentRecord.value.recordId)
    await loadRecords()
  } catch (error) {
    if (error instanceof SyntaxError) {
      ElMessage.error('结构化内容必须是合法 JSON')
    } else {
      console.error('更新候选资产失败', error)
    }
  } finally {
    savingCandidate.value = false
  }
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.learning-assets-page {
  min-height: calc(100vh - 74px);
}

.asset-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.82fr) minmax(0, 1.18fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.record-panel,
.detail-panel {
  min-width: 0;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.record-title {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 650;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-sub {
  margin-top: 4px;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.detail-body {
  min-height: 420px;
}

.detail-summary {
  display: grid;
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-5);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 82%, var(--cn-color-brand-soft));
}

.summary-top {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
}

.summary-title {
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 750;
  line-height: 1.4;
}

.summary-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.summary-stats {
  display: flex;
  flex-shrink: 0;
  gap: var(--cn-space-3);
}

.stat-box {
  min-width: 92px;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.stat-label {
  display: block;
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.stat-box strong {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  line-height: 1;
}

.summary-body {
  display: grid;
  gap: var(--cn-space-3);
}

.summary-block {
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.block-label {
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 700;
}

.block-label.danger {
  color: var(--cn-color-danger);
}

.block-text {
  color: var(--cn-color-text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.publish-alert {
  margin-top: var(--cn-space-1);
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

.candidate-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-weight: 700;
}

.toolbar-hint {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 400;
}

.candidate-list {
  display: grid;
  gap: var(--cn-space-3);
}

.candidate-card {
  display: block;
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  color: inherit;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.candidate-card.active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 38%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
}

.candidate-card:hover {
  transform: translateY(-1px);
}

.candidate-head {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-3);
}

.candidate-meta {
  flex: 1;
  min-width: 0;
}

.candidate-title {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.5;
}

.candidate-sub {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.confidence {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.candidate-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-1);
}

.candidate-body {
  margin-top: var(--cn-space-3);
  padding-left: 28px;
}

.candidate-preview {
  color: var(--cn-color-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.candidate-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.candidate-target,
.candidate-note {
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-3);
}

@media (max-width: 1200px) {
  .page-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .asset-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .asset-summary-grid,
  .filter-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-top,
  .candidate-toolbar,
  .candidate-head {
    display: grid;
  }

  .summary-stats {
    width: 100%;
  }

  .stat-box {
    flex: 1;
  }

  .detail-actions,
  .candidate-actions {
    justify-content: flex-start;
  }

  .candidate-body {
    padding-left: 0;
  }
}
</style>
