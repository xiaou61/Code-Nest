<template>
  <div class="problem-management">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>题目管理</h2>
          <p>管理 OJ 判题系统的题目，支持增删改查和状态控制</p>
        </div>
        <div class="action-section">
          <el-button type="primary" @click="$router.push('/oj/problems/create')">
            <el-icon><Plus /></el-icon>
            新增题目
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索区 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="16" class="search-row">
        <el-col :span="6">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入题目标题"
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
          <el-select v-model="searchForm.difficulty" placeholder="难度" clearable @change="handleSearch">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
            <el-option label="公开" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="10" class="search-actions">
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

    <!-- 表格 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="problemList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getDifficultyTag(row.difficulty)" size="small" effect="dark">
              {{ getDifficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="提交数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.submitCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="acceptedCount" label="通过数" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.acceptedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/oj/problems/${row.id}/edit`)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const loading = ref(false)
const problemList = ref([])
const total = ref(0)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  difficulty: null,
  status: null
})

const getDifficultyTag = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')
const getDifficultyLabel = (d) => ({ easy: '简单', medium: '中等', hard: '困难' }[d] || d)

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm }
    if (!params.keyword) delete params.keyword
    if (params.difficulty === null) delete params.difficulty
    if (params.status === null) delete params.status
    const data = await ojApi.getProblemList(params)
    problemList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('加载题目列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.difficulty = null
  searchForm.status = null
  searchForm.pageNum = 1
  loadData()
}
const handleSizeChange = () => { searchForm.pageNum = 1; loadData() }
const handleCurrentChange = () => { loadData() }

const handleStatusChange = async (row) => {
  try {
    await ojApi.updateProblem(row.id, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除题目「${row.title}」吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await ojApi.deleteProblem(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.problem-management {
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

.table-card :deep(.el-card__body) {
  padding-top: 8px;
}

.table-card :deep(.el-switch) {
  --el-switch-on-color: #1f6feb;
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
