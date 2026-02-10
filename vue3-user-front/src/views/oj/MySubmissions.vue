<template>
  <div class="my-submissions">
    <div class="page-header">
      <el-button text @click="$router.push('/oj')">
        <el-icon><ArrowLeft /></el-icon>
        返回题目列表
      </el-button>
      <h2>我的提交记录</h2>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="ojStore.submissionsLoading"
        :data="ojStore.submissions"
        style="width: 100%"
      >
        <el-table-column prop="createTime" label="提交时间" width="180" />
        <el-table-column label="题目" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/oj/problem/${row.problemId}`" class="problem-link">
              {{ row.problemTitle || `#${row.problemId}` }}
            </router-link>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="120" align="center">
          <template #default="{ row }">
            <span>{{ getLanguageLabel(row.language) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small" effect="dark">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.timeUsed != null ? row.timeUsed + 'ms' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内存" width="100" align="center">
          <template #default="{ row }">
            <span>{{ formatMemory(row.memoryUsed) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="$router.push(`/oj/submission/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty
      v-if="!ojStore.submissionsLoading && ojStore.submissions.length === 0"
      description="暂无提交记录"
      :image-size="120"
    />

    <div class="pagination-wrapper" v-if="ojStore.submissionsTotal > 0">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[15, 30, 50]"
        :total="ojStore.submissionsTotal"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useOjStore } from '@/stores/oj'

const ojStore = useOjStore()
const queryParams = reactive({ pageNum: 1, pageSize: 15 })

const getLanguageLabel = (lang) => {
  const map = { java: 'Java', cpp: 'C++', c: 'C', python: 'Python3', go: 'Go', javascript: 'JavaScript' }
  return map[lang] || lang
}

const getStatusLabel = (status) => {
  const map = {
    pending: '等待判题', judging: '判题中', accepted: '通过',
    wrong_answer: '答案错误', time_limit_exceeded: '超时',
    memory_limit_exceeded: '超内存', runtime_error: '运行错误',
    compile_error: '编译错误', system_error: '系统错误'
  }
  return map[status] || status
}

const getStatusTagType = (status) => {
  if (status === 'accepted') return 'success'
  if (['wrong_answer', 'runtime_error', 'compile_error'].includes(status)) return 'danger'
  if (['time_limit_exceeded', 'memory_limit_exceeded'].includes(status)) return 'warning'
  if (['pending', 'judging'].includes(status)) return 'info'
  return 'info'
}

const formatMemory = (kb) => {
  if (!kb) return '-'
  return kb > 1024 ? `${(kb / 1024).toFixed(1)}MB` : `${kb}KB`
}

const loadData = () => {
  ojStore.fetchMySubmissions(queryParams, { forceRefresh: true })
}

const handleSizeChange = () => { queryParams.pageNum = 1; loadData() }
const handleCurrentChange = () => { loadData() }

onMounted(() => loadData())
</script>

<style scoped>
.my-submissions {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.table-card {
  border-radius: 12px;
}

.problem-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
}

.problem-link:hover {
  text-decoration: underline;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
