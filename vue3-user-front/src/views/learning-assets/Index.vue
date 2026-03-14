<template>
  <div class="learning-assets-page">
    <el-card class="hero-card" shadow="never">
      <div class="hero-header">
        <div>
          <div class="hero-eyebrow">Learning Assets</div>
          <h1>我的学习资产</h1>
          <p>把博客、帖子、作品和面试报告沉淀成可复习、可训练、可追踪的学习资产。</p>
        </div>
        <div class="hero-actions">
          <el-button @click="loadRecords">刷新列表</el-button>
          <el-button type="primary" @click="router.push('/learning-cockpit')">
            返回学习驾驶舱
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="page-grid">
      <el-card class="record-panel" shadow="never">
        <template #header>
          <div class="panel-header">
            <span>转化记录</span>
          </div>
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

        <el-table
          v-loading="recordsLoading"
          :data="recordList.records"
          highlight-current-row
          @current-change="handleRecordSelect"
          @row-click="handleRecordSelect"
        >
          <el-table-column label="来源内容" min-width="220">
            <template #default="{ row }">
              <div class="record-title">{{ row.sourceTitle }}</div>
              <div class="record-sub">{{ sourceTypeText(row.sourceType) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" min-width="120">
            <template #default="{ row }">
              <el-tag :type="recordTagType(row.status)">{{ row.statusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="已发布" min-width="90">
            <template #default="{ row }">
              {{ row.publishedCandidates }}/{{ row.totalCandidates }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发起时间" min-width="170" />
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="filters.pageNum"
            v-model:page-size="filters.pageSize"
            :page-sizes="[5, 10, 20]"
            :total="recordList.total || 0"
            layout="total, sizes, prev, pager, next"
            @current-change="loadRecords"
            @size-change="handleSizeChange"
          />
        </div>
      </el-card>

      <el-card class="detail-panel" shadow="never" v-loading="detailLoading">
        <template #header>
          <div class="panel-header">
            <span>记录详情</span>
            <div v-if="currentRecord" class="detail-actions">
              <el-button @click="handleRetry">重新转化</el-button>
              <el-button type="success" @click="handleConfirm">保存选择</el-button>
              <el-button type="primary" @click="handlePublish">发布资产</el-button>
            </div>
          </div>
        </template>

        <el-empty v-if="!currentRecord" description="选择左侧记录查看候选资产" />

        <template v-else>
          <div class="detail-summary">
            <div class="summary-top">
              <div>
                <div class="summary-title">{{ currentRecord.sourceTitle }}</div>
                <div class="summary-meta">
                  <el-tag size="small">{{ sourceTypeText(currentRecord.sourceType) }}</el-tag>
                  <el-tag :type="recordTagType(currentRecord.status)" size="small">
                    {{ currentRecord.statusText }}
                  </el-tag>
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
              <div class="summary-block" v-if="sourceSnapshotSummary">
                <div class="block-label">来源摘要</div>
                <div class="block-text">{{ sourceSnapshotSummary }}</div>
              </div>
              <div class="summary-block" v-if="currentRecord.failReason">
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
              <el-button
                v-if="publishedPlanIds.length"
                @click="router.push('/plan')"
              >
                查看今日计划
              </el-button>
              <el-button
                v-if="reviewingCandidateCount"
                @click="router.push('/notification')"
              >
                查看通知中心
              </el-button>
            </div>
          </div>

          <div class="candidate-toolbar">
            <span>候选资产</span>
            <div class="toolbar-hint">可勾选保留项，确认后发布到闪卡、计划或审核池；已丢弃项也可重新恢复。</div>
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
                    <el-tag size="small">{{ assetTypeText(candidate.assetType) }}</el-tag>
                    <el-tag :type="candidateTagType(candidate.status)" size="small">
                      {{ candidate.statusText }}
                    </el-tag>
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
                  <el-tag
                    v-for="tag in splitTags(candidate.tags)"
                    :key="`${candidate.id}-${tag}`"
                    size="small"
                    type="info"
                  >
                    {{ tag }}
                  </el-tag>
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
      </el-card>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import learningAssetApi from '@/api/learningAssets'

const route = useRoute()
const router = useRouter()

const recordsLoading = ref(false)
const detailLoading = ref(false)
const editorVisible = ref(false)
const savingCandidate = ref(false)
const discardingCandidateId = ref(null)
const currentRecord = ref(null)
const editingCandidateId = ref(null)
const selectedCandidateIds = ref([])
const lastPublishResult = ref(null)

const filters = reactive({
  sourceType: '',
  status: '',
  pageNum: 1,
  pageSize: 10
})

const recordList = reactive({
  total: 0,
  records: []
})

const editorForm = reactive({
  title: '',
  tags: '',
  difficulty: '',
  contentJson: ''
})

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
  const ids = new Set()
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
    return `当前记录已有 ${currentRecord.value.publishedCandidates} 项学习资产成功发布，可直接进入学习或计划中心。`
  }
  return ''
})

const hasQuickActions = computed(() => {
  return Boolean(publishedFlashcardDeckId.value || publishedPlanIds.value.length || reviewingCandidateCount.value)
})

const sourceTypeText = (type) => {
  const map = {
    blog: '博客文章',
    community: '社区帖子',
    codepen: 'CodePen 作品',
    mock_interview: '模拟面试报告'
  }
  return map[type] || type
}

const assetTypeText = (type) => {
  const map = {
    flashcard: '闪卡卡组',
    knowledge_node: '知识节点候选',
    practice_plan: '练习清单',
    interview_question: '面试题草稿'
  }
  return map[type] || type
}

const recordTagType = (status) => {
  const map = {
    PENDING_CONFIRM: 'warning',
    REVIEWING: 'info',
    PUBLISHED: 'success',
    PARTIAL_PUBLISHED: 'primary',
    FAILED: 'danger'
  }
  return map[status] || ''
}

const candidateTagType = (status) => {
  const map = {
    DRAFT: '',
    SELECTED: 'warning',
    REVIEWING: 'info',
    PUBLISHED: 'success',
    DISCARDED: 'danger',
    REJECTED: 'danger'
  }
  return map[status] || ''
}

const splitTags = (tags) => {
  return String(tags || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const isCandidateLocked = (candidate) => {
  return ['PUBLISHED', 'REVIEWING'].includes(candidate.status)
}

const canEditCandidate = (candidate) => {
  return ['DRAFT', 'SELECTED'].includes(candidate.status)
}

const canDiscardCandidate = (candidate) => {
  return ['DRAFT', 'SELECTED'].includes(candidate.status)
}

const formatCandidatePreview = (candidate) => {
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

const initSelectedCandidates = (detail) => {
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
    const res = await learningAssetApi.getRecords({
      sourceType: filters.sourceType || undefined,
      status: filters.status || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    })
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

const loadRecordDetail = async (recordId) => {
  if (!recordId) return
  if (currentRecord.value?.recordId !== recordId) {
    lastPublishResult.value = null
  }
  detailLoading.value = true
  try {
    const detail = await learningAssetApi.getRecordDetail(recordId)
    currentRecord.value = detail
    initSelectedCandidates(detail)
  } catch (error) {
    console.error('加载记录详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleRecordSelect = (row) => {
  if (row?.recordId) {
    router.replace({
      path: route.path,
      query: {
        ...route.query,
        recordId: row.recordId
      }
    })
    loadRecordDetail(row.recordId)
  }
}

const handleSearch = () => {
  filters.pageNum = 1
  loadRecords()
}

const handleSizeChange = () => {
  filters.pageNum = 1
  loadRecords()
}

const toggleCandidate = (candidateId, checked) => {
  if (checked) {
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
  if (!editableCandidates.length) {
    return
  }
  currentRecord.value = await learningAssetApi.confirm(currentRecord.value.recordId, selectedCandidateIds.value)
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
    currentRecord.value = await learningAssetApi.confirm(currentRecord.value.recordId, selectedCandidateIds.value)
    initSelectedCandidates(currentRecord.value)
    ElMessage.success('候选项已确认')
    await loadRecords()
  } catch (error) {
    console.error('确认候选项失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleDiscard = async (candidate) => {
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
    const detail = await learningAssetApi.discardCandidate(candidate.id)
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

const restoreCandidateSelection = (candidateId) => {
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
    const result = await learningAssetApi.publish(currentRecord.value.recordId, selectedCandidateIds.value)
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
    const detail = await learningAssetApi.retry(currentRecord.value.recordId)
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

const openEditor = (candidate) => {
  editingCandidateId.value = candidate.id
  editorForm.title = candidate.title
  editorForm.tags = candidate.tags || ''
  editorForm.difficulty = candidate.difficulty || ''
  editorForm.contentJson = candidate.contentJson || ''
  editorVisible.value = true
}

const saveCandidate = async () => {
  if (!editingCandidateId.value) return
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
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(180deg, #f6fbff 0%, #f4f7fb 100%);
}

.hero-card {
  margin-bottom: 20px;
  border-radius: 20px;
  border: 1px solid #e6eef7;
  background:
    radial-gradient(circle at top right, rgba(67, 160, 255, 0.18), transparent 35%),
    radial-gradient(circle at bottom left, rgba(54, 209, 153, 0.18), transparent 25%),
    #ffffff;
}

.hero-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hero-eyebrow {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #67a4ff;
  margin-bottom: 10px;
}

.hero-header h1 {
  margin: 0 0 10px;
  font-size: 30px;
  color: #1f2d3d;
}

.hero-header p {
  margin: 0;
  max-width: 720px;
  line-height: 1.7;
  color: #5f6f81;
}

.hero-actions {
  display: flex;
  gap: 12px;
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(320px, 42%) 1fr;
  gap: 20px;
}

.record-panel,
.detail-panel {
  min-height: 720px;
  border-radius: 18px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.publish-alert {
  margin-top: 4px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.record-title {
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.5;
}

.record-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #8b97a6;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-summary {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 20px;
  padding: 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #e8f1fb;
}

.summary-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.summary-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2d3d;
  margin-bottom: 8px;
}

.summary-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-stats {
  display: flex;
  gap: 12px;
}

.stat-box {
  min-width: 88px;
  padding: 12px 14px;
  border-radius: 14px;
  background: white;
  border: 1px solid #e6eef7;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: #7a8aa0;
  margin-bottom: 8px;
}

.summary-body {
  display: grid;
  gap: 12px;
}

.summary-block {
  padding: 14px 16px;
  background: white;
  border-radius: 12px;
  border: 1px solid #eef3f8;
}

.block-label {
  font-size: 12px;
  color: #8b97a6;
  margin-bottom: 8px;
}

.block-label.danger {
  color: #e85b6c;
}

.block-text {
  line-height: 1.7;
  color: #1f2d3d;
  white-space: pre-wrap;
}

.candidate-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #1f2d3d;
}

.toolbar-hint {
  font-size: 12px;
  font-weight: 400;
  color: #8b97a6;
}

.candidate-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.candidate-card {
  display: block;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid #e7edf5;
  background: white;
  transition: all 0.25s ease;
}

.candidate-card.active {
  border-color: #76a9ff;
  box-shadow: 0 10px 30px rgba(118, 169, 255, 0.12);
}

.candidate-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.candidate-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.candidate-meta {
  flex: 1;
  min-width: 0;
}

.candidate-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
  line-height: 1.5;
}

.candidate-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}

.confidence {
  font-size: 12px;
  color: #7a8aa0;
}

.candidate-body {
  padding-left: 26px;
  margin-top: 14px;
}

.candidate-preview {
  line-height: 1.7;
  color: #445365;
  white-space: pre-wrap;
}

.candidate-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.candidate-target,
.candidate-note {
  margin-top: 12px;
  font-size: 13px;
  color: #7a8aa0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 1200px) {
  .page-grid {
    grid-template-columns: 1fr;
  }

  .record-panel,
  .detail-panel {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .learning-assets-page {
    padding: 16px;
  }

  .hero-header,
  .summary-top,
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions,
  .detail-actions,
  .quick-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .filter-row {
    grid-template-columns: 1fr;
  }

  .summary-stats {
    width: 100%;
  }

  .stat-box {
    flex: 1;
  }

  .candidate-body {
    padding-left: 0;
  }
}
</style>
