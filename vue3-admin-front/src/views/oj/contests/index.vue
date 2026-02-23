<template>
  <div class="contest-management">
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>赛事管理</h2>
          <p>管理 OJ 赛事的创建、发布状态与题目编排</p>
        </div>
        <div class="action-section">
          <el-button type="primary" @click="router.push('/oj/contests/create')">
            <el-icon><Plus /></el-icon>
            新增赛事
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card class="search-card" shadow="never">
      <el-row :gutter="16" class="search-row">
        <el-col :span="8">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入赛事标题关键词"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.contestType" placeholder="赛事类型" clearable @change="handleSearch">
            <el-option label="周赛" value="weekly" />
            <el-option label="挑战赛" value="challenge" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="草稿" :value="0" />
            <el-option label="即将开始" :value="1" />
            <el-option label="进行中" :value="2" />
            <el-option label="已结束" :value="3" />
          </el-select>
        </el-col>
        <el-col :span="8" class="search-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="contestList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="赛事标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="dark" :type="getTypeTag(row.contestType)">
              {{ getTypeLabel(row.contestType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTag(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间窗口" min-width="280">
          <template #default="{ row }">
            <div class="time-range">
              <span>{{ row.startTime || '-' }}</span>
              <span>~</span>
              <span>{{ row.endTime || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="problemCount" label="题目数" width="100" align="center" />
        <el-table-column prop="participantCount" label="参赛人数" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="router.push(`/oj/contests/${row.id}/edit`)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="getStatusAction(row)"
              :type="getStatusAction(row).type"
              size="small"
              @click="handleStatusChange(row)"
            >
              <el-icon><SwitchButton /></el-icon>
              {{ getStatusAction(row).label }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search, SwitchButton } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const router = useRouter()

const loading = ref(false)
const contestList = ref([])
const total = ref(0)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  contestType: '',
  status: null
})

const getTypeTag = (type) => ({ weekly: 'success', challenge: 'warning' }[type] || 'info')
const getTypeLabel = (type) => ({ weekly: '周赛', challenge: '挑战赛' }[type] || (type || '未知'))
const getStatusTag = (status) => ({ 0: 'info', 1: 'warning', 2: 'success', 3: '' }[status] || 'info')
const getStatusLabel = (status) => ({ 0: '草稿', 1: '即将开始', 2: '进行中', 3: '已结束' }[status] || `状态${status}`)

const normalizeContest = (item) => {
  const problemList = item.problemIdList || item.problemIds || item.problems || []
  return {
    id: item.id ?? item.contestId ?? '',
    title: item.title || item.name || '',
    contestType: item.contestType || item.type || '',
    status: Number(item.status ?? 0),
    startTime: item.startTime || item.beginTime || '',
    endTime: item.endTime || item.finishTime || '',
    problemCount: Number(item.problemCount ?? (Array.isArray(problemList) ? problemList.length : 0)),
    participantCount: Number(item.participantCount ?? item.joinCount ?? 0),
    createTime: item.createTime || item.createdAt || ''
  }
}

const resolveListData = (data) => {
  if (Array.isArray(data)) {
    return { records: data, total: data.length }
  }
  if (!data || typeof data !== 'object') {
    return { records: [], total: 0 }
  }
  return {
    records: data.records || data.list || [],
    total: Number(data.total || 0)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm }
    if (!params.keyword) delete params.keyword
    if (!params.contestType) delete params.contestType
    if (params.status === null || params.status === undefined) delete params.status
    const data = await ojApi.getContestList(params)
    const pageData = resolveListData(data)
    contestList.value = (pageData.records || []).map(normalizeContest)
    total.value = pageData.total || 0
  } catch (error) {
    console.error('加载赛事列表失败:', error)
  } finally {
    loading.value = false
  }
}

const getStatusAction = (row) => {
  if (row.status === 0) {
    return { label: '发布', targetStatus: 1, type: 'success' }
  }
  if (row.status === 1) {
    return { label: '下线', targetStatus: 0, type: 'warning' }
  }
  return null
}

const handleStatusChange = (row) => {
  const action = getStatusAction(row)
  if (!action) {
    ElMessage.warning('当前状态不支持直接切换')
    return
  }
  ElMessageBox.confirm(`确定${action.label}赛事「${row.title}」吗？`, '状态变更', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await ojApi.updateContestStatus(row.id, action.targetStatus)
    ElMessage.success(`${action.label}成功`)
    loadData()
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除赛事「${row.title}」吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await ojApi.deleteContest(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.contestType = ''
  searchForm.status = null
  searchForm.pageNum = 1
  loadData()
}

const handleSizeChange = () => {
  searchForm.pageNum = 1
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.contest-management {
  display: grid;
  gap: 14px;
}

.header-card,
.search-card,
.table-card {
  margin-bottom: 0;
  border-radius: 14px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title-section h2 {
  margin: 0 0 6px;
  font-size: 23px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.title-section p {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 14px;
}

.action-section :deep(.el-button) {
  min-width: 120px;
}

.search-row :deep(.el-select),
.search-row :deep(.el-input) {
  width: 100%;
}

.search-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--cn-text-secondary);
}

.table-card :deep(.el-card__body) {
  padding-top: 8px;
}

.table-card :deep(.el-button--small) {
  min-width: 64px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .search-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .title-section h2 {
    font-size: 20px;
  }
}
</style>
