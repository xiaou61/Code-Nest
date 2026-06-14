<template>
  <CnPage class="learning-assets-review-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="学习资产审核台"
      description="审核知识节点候选与面试题草稿，决定哪些内容进入平台公共资产池。"
      eyebrow="Learning Assets Review"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">候选 {{ pagination.total }} 条</CnStatusTag>
        <CnStatusTag type="success">图谱 {{ knowledgeMaps.length }} 个</CnStatusTag>
        <CnStatusTag type="info">题单 {{ questionSets.length }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button @click="handleReset">重置筛选</el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadCandidates">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按候选资产类型筛选待审核内容。" divided>
      <div class="filter-bar">
        <el-select v-model="filters.assetType" clearable placeholder="资产类型" class="filter-control">
          <el-option label="知识节点候选" value="knowledge_node" />
          <el-option label="面试题草稿" value="interview_question" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">筛选</el-button>
      </div>
    </CnSection>

    <CnSection title="候选资产列表" :description="`共 ${pagination.total} 条候选记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="candidateId"
        empty-title="暂无候选资产"
        empty-description="当前筛选条件下没有待审核候选资产。"
        empty-icon="AR"
        @page-change="handlePageChange"
        @page-size-change="handleSizeChange"
      >
        <template #assetType="{ row }">
          <CnStatusTag :type="row.assetType === 'knowledge_node' ? 'brand' : 'success'" size="sm">
            {{ assetTypeText(row.assetType) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <el-button type="primary" link @click="openDetail(row)">查看审核</el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-drawer v-model="detailVisible" title="候选资产审核" size="62%" destroy-on-close>
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <el-descriptions :column="2" border class="detail-base">
            <el-descriptions-item label="候选ID">{{ detail.candidateId }}</el-descriptions-item>
            <el-descriptions-item label="资产类型">
              <CnStatusTag :type="detail.assetType === 'knowledge_node' ? 'brand' : 'success'" size="sm">
                {{ assetTypeText(detail.assetType) }}
              </CnStatusTag>
            </el-descriptions-item>
            <el-descriptions-item label="来源类型">{{ sourceTypeText(detail.sourceType) }}</el-descriptions-item>
            <el-descriptions-item label="来源ID">{{ detail.sourceId }}</el-descriptions-item>
            <el-descriptions-item label="来源标题" :span="2">{{ detail.sourceTitle }}</el-descriptions-item>
            <el-descriptions-item label="标签" :span="2">
              <div class="tag-list">
                <CnStatusTag v-for="tag in splitTags(detail.tags)" :key="`${detail.candidateId}-${tag}`" type="info" size="sm">
                  {{ tag }}
                </CnStatusTag>
                <CnStatusTag v-if="!splitTags(detail.tags).length" type="neutral" size="sm" subtle>暂无标签</CnStatusTag>
              </div>
            </el-descriptions-item>
          </el-descriptions>

          <CnSection title="来源快照" surface="plain" divided>
            <div class="snapshot-box">{{ snapshotSummary }}</div>
          </CnSection>

          <CnSection title="候选内容" surface="plain" divided>
            <template #actions>
              <el-button type="primary" link :loading="saving" @click="handleSave">保存编辑</el-button>
            </template>
            <el-form label-position="top" class="candidate-form">
              <el-form-item label="候选标题">
                <el-input v-model="editForm.title" maxlength="255" show-word-limit />
              </el-form-item>
              <el-form-item label="标签">
                <el-input v-model="editForm.tags" placeholder="多个标签请用逗号分隔" />
              </el-form-item>
              <el-form-item label="难度建议">
                <el-input v-model="editForm.difficulty" placeholder="例如：初级 / 中级 / 高级" />
              </el-form-item>
              <el-form-item label="结构化内容 JSON">
                <el-input v-model="editForm.contentJson" type="textarea" :rows="10" />
              </el-form-item>
            </el-form>
          </CnSection>

          <CnSection title="审核操作" surface="plain" divided>
            <el-form label-position="top">
              <template v-if="detail.assetType === 'knowledge_node'">
                <el-form-item label="目标知识图谱">
                  <el-select v-model="approveForm.mapId" placeholder="请选择图谱" class="full-width">
                    <el-option v-for="item in knowledgeMaps" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                </el-form-item>
                <el-form-item label="父节点 ID">
                  <el-input-number v-model="approveForm.parentId" :min="0" :step="1" class="full-width" />
                </el-form-item>
              </template>

              <template v-if="detail.assetType === 'interview_question'">
                <el-form-item label="目标题单">
                  <el-select v-model="approveForm.questionSetId" placeholder="请选择题单" class="full-width">
                    <el-option v-for="item in questionSets" :key="item.id" :label="item.title" :value="item.id" />
                  </el-select>
                </el-form-item>
              </template>

              <el-form-item label="审核备注">
                <el-input v-model="approveForm.note" type="textarea" :rows="3" placeholder="通过或驳回时写一点备注，方便回溯" />
              </el-form-item>

              <el-divider content-position="left">合并到已有内容</el-divider>
              <el-form-item label="目标已有内容 ID">
                <el-input-number
                  v-model="mergeForm.existingTargetId"
                  :min="1"
                  :step="1"
                  class="full-width"
                  placeholder="输入已有知识节点或面试题 ID"
                />
              </el-form-item>
              <el-form-item label="合并备注">
                <el-input v-model="mergeForm.note" type="textarea" :rows="2" placeholder="记录为什么合并到已有内容" />
              </el-form-item>
            </el-form>

            <div class="review-actions">
              <el-button type="danger" :loading="rejecting" @click="handleReject">驳回候选</el-button>
              <el-button type="warning" :loading="merging" @click="handleMerge">合并到已有内容</el-button>
              <el-button type="primary" :loading="approving" @click="handleApprove">审核通过并发布</el-button>
            </div>
          </CnSection>
        </template>
      </div>
    </el-drawer>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import learningAssetAdminApi from '@/api/learningAssets'
import { getKnowledgeMapList } from '@/api/knowledge'
import { interviewApi } from '@/api/interview'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn } from '@/design-system'

interface CandidateRecord extends Record<string, unknown> {
  candidateId: number
  assetType: string
  title?: string
  sourceTitle?: string
  createTime?: string
}

interface CandidateDetail extends CandidateRecord {
  sourceType?: string
  sourceId?: number
  sourceSnapshot?: string
  tags?: string
  difficulty?: string
  contentJson?: string
  targetId?: number
}

interface KnowledgeMapOption {
  id: number
  title: string
}

interface QuestionSetOption {
  id: number
  title: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '学习资产' }, { label: '审核台' }]

const loading = ref(false)
const detailLoading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const merging = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const detail = ref<CandidateDetail | null>(null)
const knowledgeMaps = ref<KnowledgeMapOption[]>([])
const questionSets = ref<QuestionSetOption[]>([])
const tableData = ref<CandidateRecord[]>([])

const pagination = reactive({
  total: 0
})

const filters = reactive({
  assetType: '',
  pageNum: 1,
  pageSize: 10
})

const approveForm = reactive({
  mapId: null as number | null,
  parentId: 0,
  questionSetId: null as number | null,
  note: ''
})

const editForm = reactive({
  title: '',
  tags: '',
  difficulty: '',
  contentJson: ''
})

const mergeForm = reactive({
  existingTargetId: null as number | null,
  note: ''
})

const tableColumns: CnTableColumn<CandidateRecord>[] = [
  { prop: 'candidateId', label: '候选ID', minWidth: 100 },
  { prop: 'assetType', label: '资产类型', minWidth: 140, slot: 'assetType' },
  { prop: 'title', label: '候选标题', minWidth: 220, showOverflowTooltip: true },
  { prop: 'sourceTitle', label: '来源内容', minWidth: 220, showOverflowTooltip: true },
  { prop: 'createTime', label: '提交时间', minWidth: 170, showOverflowTooltip: true },
  { label: '操作', width: 140, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: filters.pageNum,
  pageSize: filters.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50]
}))

const parseSourceSnapshot = (sourceSnapshot?: string) => {
  if (!sourceSnapshot) return null
  try {
    return JSON.parse(sourceSnapshot)
  } catch {
    return null
  }
}

const snapshotSummary = computed(() => {
  const snapshot = parseSourceSnapshot(detail.value?.sourceSnapshot)
  if (!snapshot) return detail.value?.sourceSnapshot || '暂无来源快照'
  try {
    return snapshot.summary || snapshot.content || JSON.stringify(snapshot, null, 2)
  } catch {
    return detail.value?.sourceSnapshot || '暂无来源快照'
  }
})

const assetTypeText = (type?: string) => {
  const map: Record<string, string> = {
    knowledge_node: '知识节点候选',
    interview_question: '面试题草稿'
  }
  return type ? map[type] || type : '-'
}

const sourceTypeText = (type?: string) => {
  const map: Record<string, string> = {
    blog: '博客文章',
    community: '社区帖子',
    codepen: 'CodePen 作品',
    mock_interview: '模拟面试报告'
  }
  return type ? map[type] || type : '-'
}

const splitTags = (tags?: string) => {
  return String(tags || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

const loadOptions = async () => {
  try {
    const [mapRes, questionRes] = await Promise.all([
      getKnowledgeMapList({ pageNum: 1, pageSize: 100, status: 1 }),
      interviewApi.getQuestionSets({ pageNum: 1, pageSize: 100, status: 1 })
    ])
    knowledgeMaps.value = Array.isArray(mapRes?.records) ? mapRes.records : []
    questionSets.value = Array.isArray(questionRes?.records) ? questionRes.records : []
  } catch (error) {
    console.error('加载审核辅助数据失败', error)
  }
}

const loadCandidates = async () => {
  loading.value = true
  try {
    const res = await learningAssetAdminApi.getCandidates({
      assetType: filters.assetType || undefined,
      pageNum: filters.pageNum,
      pageSize: filters.pageSize
    })
    tableData.value = Array.isArray(res?.records) ? res.records : []
    pagination.total = Number(res?.total) || 0
  } catch (error) {
    console.error('加载审核列表失败', error)
  } finally {
    loading.value = false
  }
}

const openDetail = async (row: CandidateRecord) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await learningAssetAdminApi.getCandidateDetail(row.candidateId)
    editForm.title = detail.value?.title || ''
    editForm.tags = detail.value?.tags || ''
    editForm.difficulty = detail.value?.difficulty || ''
    editForm.contentJson = detail.value?.contentJson || '{}'
    approveForm.mapId = knowledgeMaps.value[0]?.id || null
    approveForm.parentId = 0
    approveForm.questionSetId = questionSets.value[0]?.id || null
    approveForm.note = ''
    mergeForm.existingTargetId = detail.value?.targetId || null
    mergeForm.note = ''
  } catch (error) {
    console.error('加载候选详情失败', error)
  } finally {
    detailLoading.value = false
  }
}

const handleSave = async () => {
  if (!detail.value) return
  saving.value = true
  try {
    JSON.parse(editForm.contentJson || '{}')
    await learningAssetAdminApi.updateCandidate(detail.value.candidateId, {
      title: editForm.title,
      tags: editForm.tags,
      difficulty: editForm.difficulty,
      contentJson: editForm.contentJson
    })
    detail.value = await learningAssetAdminApi.getCandidateDetail(detail.value.candidateId)
    ElMessage.success('审核候选已更新')
  } catch (error) {
    console.error('保存审核候选失败', error)
    ElMessage.error('保存审核候选失败')
  } finally {
    saving.value = false
  }
}

const handleApprove = async () => {
  if (!detail.value) return
  if (detail.value.assetType === 'knowledge_node' && !approveForm.mapId) {
    ElMessage.warning('请选择目标知识图谱')
    return
  }
  if (detail.value.assetType === 'interview_question' && !approveForm.questionSetId) {
    ElMessage.warning('请选择目标面试题单')
    return
  }

  approving.value = true
  try {
    const targetId = await learningAssetAdminApi.approve(detail.value.candidateId, {
      mapId: approveForm.mapId,
      parentId: approveForm.parentId,
      questionSetId: approveForm.questionSetId,
      note: approveForm.note
    })
    ElMessage.success(`审核通过，已发布到目标模块，ID: ${targetId}`)
    detailVisible.value = false
    await loadCandidates()
  } catch (error) {
    console.error('审核通过失败', error)
  } finally {
    approving.value = false
  }
}

const handleMerge = async () => {
  if (!detail.value) return
  if (!mergeForm.existingTargetId) {
    ElMessage.warning('请输入目标已有内容 ID')
    return
  }
  merging.value = true
  try {
    const targetId = await learningAssetAdminApi.merge(detail.value.candidateId, {
      existingTargetId: mergeForm.existingTargetId,
      note: mergeForm.note
    })
    ElMessage.success(`已合并到已有内容，目标 ID: ${targetId}`)
    detailVisible.value = false
    await loadCandidates()
  } catch (error) {
    console.error('合并候选失败', error)
    ElMessage.error('合并候选失败')
  } finally {
    merging.value = false
  }
}

const handleReject = async () => {
  if (!detail.value) return
  if (!approveForm.note.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  try {
    await learningAssetAdminApi.reject(detail.value.candidateId, {
      note: approveForm.note
    })
    ElMessage.success('已驳回该候选资产')
    detailVisible.value = false
    await loadCandidates()
  } catch (error) {
    console.error('驳回候选失败', error)
  } finally {
    rejecting.value = false
  }
}

const handleSearch = () => {
  filters.pageNum = 1
  loadCandidates()
}

const handleReset = async () => {
  filters.assetType = ''
  filters.pageNum = 1
  filters.pageSize = 10
  await loadCandidates()
}

const handleSizeChange = (size: number) => {
  filters.pageSize = size
  filters.pageNum = 1
  loadCandidates()
}

const handlePageChange = (page: number) => {
  filters.pageNum = page
  loadCandidates()
}

onMounted(async () => {
  await loadOptions()
  await loadCandidates()
})
</script>

<style scoped>
.learning-assets-review-page {
  min-height: 100%;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
}

.filter-control {
  width: 220px;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-4);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.snapshot-box {
  margin: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.candidate-form {
  margin-top: var(--cn-space-1);
}

.review-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-3);
}

.full-width {
  width: 100%;
}

@media (max-width: 768px) {
  .filter-control {
    width: 100%;
  }

  .review-actions {
    justify-content: flex-start;
  }
}
</style>
