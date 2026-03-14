<template>
  <div class="learning-assets-review-page">
    <el-card shadow="never" class="overview-card">
      <div class="overview-header">
        <div>
          <h2>学习资产审核台</h2>
          <p>审核知识节点候选与面试题草稿，决定哪些内容进入平台公共资产池。</p>
        </div>
        <div class="actions">
          <el-button @click="handleReset">重置筛选</el-button>
          <el-button type="primary" :loading="loading" @click="loadCandidates">刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="filter-bar">
        <el-select v-model="filters.assetType" clearable placeholder="资产类型" style="width: 220px">
          <el-option label="知识节点候选" value="knowledge_node" />
          <el-option label="面试题草稿" value="interview_question" />
        </el-select>
        <el-button type="primary" @click="handleSearch">筛选</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="candidateId" label="候选ID" min-width="100" />
        <el-table-column label="资产类型" min-width="130">
          <template #default="{ row }">
            {{ assetTypeText(row.assetType) }}
          </template>
        </el-table-column>
        <el-table-column prop="title" label="候选标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sourceTitle" label="来源内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" min-width="170" />
        <el-table-column fixed="right" label="操作" min-width="140">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">
              查看审核
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="filters.pageNum"
          v-model:page-size="filters.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadCandidates"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="候选资产审核" size="62%" destroy-on-close>
      <div v-loading="detailLoading" class="detail-wrap">
        <template v-if="detail">
          <el-descriptions :column="2" border class="detail-base">
            <el-descriptions-item label="候选ID">{{ detail.candidateId }}</el-descriptions-item>
            <el-descriptions-item label="资产类型">{{ assetTypeText(detail.assetType) }}</el-descriptions-item>
            <el-descriptions-item label="来源类型">{{ sourceTypeText(detail.sourceType) }}</el-descriptions-item>
            <el-descriptions-item label="来源ID">{{ detail.sourceId }}</el-descriptions-item>
            <el-descriptions-item label="来源标题" :span="2">{{ detail.sourceTitle }}</el-descriptions-item>
            <el-descriptions-item label="标签" :span="2">
              <div class="tag-list">
                <el-tag
                  v-for="tag in splitTags(detail.tags)"
                  :key="`${detail.candidateId}-${tag}`"
                  size="small"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </el-descriptions-item>
          </el-descriptions>

          <el-card shadow="never" class="section-card">
            <template #header>
              <span>来源快照</span>
            </template>
            <div class="snapshot-box">{{ snapshotSummary }}</div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <div class="section-head">
                <span>候选内容</span>
                <el-button type="primary" text :loading="saving" @click="handleSave">
                  保存编辑
                </el-button>
              </div>
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
          </el-card>

          <el-card shadow="never" class="section-card">
            <template #header>
              <span>审核操作</span>
            </template>

            <el-form label-position="top">
              <template v-if="detail.assetType === 'knowledge_node'">
                <el-form-item label="目标知识图谱">
                  <el-select v-model="approveForm.mapId" placeholder="请选择图谱" style="width: 100%">
                    <el-option
                      v-for="item in knowledgeMaps"
                      :key="item.id"
                      :label="item.title"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="父节点 ID">
                  <el-input-number v-model="approveForm.parentId" :min="0" :step="1" style="width: 100%" />
                </el-form-item>
              </template>

              <template v-if="detail.assetType === 'interview_question'">
                <el-form-item label="目标题单">
                  <el-select v-model="approveForm.questionSetId" placeholder="请选择题单" style="width: 100%">
                    <el-option
                      v-for="item in questionSets"
                      :key="item.id"
                      :label="item.title"
                      :value="item.id"
                    />
                  </el-select>
                </el-form-item>
              </template>

              <el-form-item label="审核备注">
                <el-input
                  v-model="approveForm.note"
                  type="textarea"
                  :rows="3"
                  placeholder="通过或驳回时写一点备注，方便回溯"
                />
              </el-form-item>

              <el-divider content-position="left">合并到已有内容</el-divider>
              <el-form-item label="目标已有内容 ID">
                <el-input-number
                  v-model="mergeForm.existingTargetId"
                  :min="1"
                  :step="1"
                  style="width: 100%"
                  placeholder="输入已有知识节点或面试题 ID"
                />
              </el-form-item>
              <el-form-item label="合并备注">
                <el-input
                  v-model="mergeForm.note"
                  type="textarea"
                  :rows="2"
                  placeholder="记录为什么合并到已有内容"
                />
              </el-form-item>
            </el-form>

            <div class="review-actions">
              <el-button type="danger" :loading="rejecting" @click="handleReject">
                驳回候选
              </el-button>
              <el-button type="warning" :loading="merging" @click="handleMerge">
                合并到已有内容
              </el-button>
              <el-button type="primary" :loading="approving" @click="handleApprove">
                审核通过并发布
              </el-button>
            </div>
          </el-card>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import learningAssetAdminApi from '@/api/learningAssets'
import { getKnowledgeMapList } from '@/api/knowledge'
import { interviewApi } from '@/api/interview'

const loading = ref(false)
const detailLoading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const merging = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const knowledgeMaps = ref([])
const questionSets = ref([])
const tableData = ref([])

const pagination = reactive({
  total: 0
})

const filters = reactive({
  assetType: '',
  pageNum: 1,
  pageSize: 10
})

const approveForm = reactive({
  mapId: null,
  parentId: 0,
  questionSetId: null,
  note: ''
})

const editForm = reactive({
  title: '',
  tags: '',
  difficulty: '',
  contentJson: ''
})

const mergeForm = reactive({
  existingTargetId: null,
  note: ''
})

const parseSourceSnapshot = (sourceSnapshot) => {
  if (!sourceSnapshot) return null
  try {
    return JSON.parse(sourceSnapshot)
  } catch (error) {
    return null
  }
}

const snapshotSummary = computed(() => {
  const snapshot = parseSourceSnapshot(detail.value?.sourceSnapshot)
  if (!snapshot) return detail.value?.sourceSnapshot || '暂无来源快照'
  try {
    return snapshot.summary || snapshot.content || JSON.stringify(snapshot, null, 2)
  } catch (error) {
    return detail.value?.sourceSnapshot || '暂无来源快照'
  }
})

const assetTypeText = (type) => {
  const map = {
    knowledge_node: '知识节点候选',
    interview_question: '面试题草稿'
  }
  return map[type] || type
}

const sourceTypeText = (type) => {
  const map = {
    blog: '博客文章',
    community: '社区帖子',
    codepen: 'CodePen 作品',
    mock_interview: '模拟面试报告'
  }
  return map[type] || type
}

const splitTags = (tags) => {
  return String(tags || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const loadOptions = async () => {
  try {
    const [mapRes, questionRes] = await Promise.all([
      getKnowledgeMapList({ pageNum: 1, pageSize: 100, status: 1 }),
      interviewApi.getQuestionSets({ pageNum: 1, pageSize: 100, status: 1 })
    ])
    knowledgeMaps.value = mapRes.records || []
    questionSets.value = questionRes.records || []
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
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('加载审核列表失败', error)
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await learningAssetAdminApi.getCandidateDetail(row.candidateId)
    editForm.title = detail.value.title || ''
    editForm.tags = detail.value.tags || ''
    editForm.difficulty = detail.value.difficulty || ''
    editForm.contentJson = detail.value.contentJson || '{}'
    approveForm.mapId = knowledgeMaps.value[0]?.id || null
    approveForm.parentId = 0
    approveForm.questionSetId = questionSets.value[0]?.id || null
    approveForm.note = ''
    mergeForm.existingTargetId = detail.value.targetId || null
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

const handleSizeChange = () => {
  filters.pageNum = 1
  loadCandidates()
}

onMounted(async () => {
  await loadOptions()
  await loadCandidates()
})
</script>

<style scoped>
.learning-assets-review-page {
  min-height: 100vh;
  padding: 20px;
  background: #f5f7fb;
}

.overview-card,
.table-card {
  margin-bottom: 16px;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.overview-header h2 {
  margin: 0 0 8px;
}

.overview-header p {
  margin: 0;
  color: #909399;
}

.actions {
  display: flex;
  gap: 10px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-base,
.section-card {
  margin-bottom: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.snapshot-box,
.content-json {
  margin: 0;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.candidate-form {
  margin-top: 4px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .overview-header,
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions,
  .review-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
