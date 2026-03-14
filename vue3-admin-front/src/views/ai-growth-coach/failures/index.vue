<template>
  <div class="ai-growth-coach-failures-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-header">
        <div>
          <h2>AI成长教练失败案例观测</h2>
          <p>集中查看最近生成失败和规则兜底的快照，方便排查工作流问题与策略空洞。</p>
        </div>
        <el-button type="primary" :loading="loading" @click="loadFailures">刷新列表</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="metric-row">
      <el-col :span="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">观测记录数</div>
          <div class="metric-value">{{ failures.length }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">规则兜底数</div>
          <div class="metric-value warning">{{ fallbackCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">生成失败数</div>
          <div class="metric-value danger">{{ failedCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-header">
          <span>最近失败 / 兜底快照</span>
          <span class="table-tip">默认展示最近 50 条记录</span>
        </div>
      </template>

      <el-table :data="failures" v-loading="loading" border>
        <el-table-column prop="snapshotId" label="快照ID" min-width="110" />
        <el-table-column prop="userId" label="用户ID" min-width="110" />
        <el-table-column label="场景" min-width="120">
          <template #default="{ row }">
            {{ formatScene(row.scene) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" min-width="120">
          <template #default="{ row }">
            <el-tag :type="row.fallbackOnly ? 'warning' : 'danger'" effect="light">
              {{ row.fallbackOnly ? '规则兜底' : '生成失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">
              {{ row.status || 'UNKNOWN' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="headline" label="摘要标题" min-width="240" show-overflow-tooltip />
        <el-table-column label="失败原因" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            {{ resolveReason(row) }}
          </template>
        </el-table-column>
        <el-table-column prop="generatedAt" label="生成时间" min-width="180" />
      </el-table>

      <el-empty
        v-if="!loading && !failures.length"
        description="当前没有失败或兜底记录"
        :image-size="72"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import aiGrowthCoachAdminApi from '@/api/aiGrowthCoach'

const loading = ref(false)
const failures = ref([])

const fallbackCount = computed(() => failures.value.filter(item => item?.fallbackOnly).length)
const failedCount = computed(() => failures.value.filter(item => !item?.fallbackOnly).length)

const formatScene = (scene) => {
  const key = String(scene || '').toUpperCase()
  const map = {
    LEARNING: '学习场景',
    CAREER: '求职场景',
    HYBRID: '综合场景'
  }
  return map[key] || scene || '未知场景'
}

const statusTagType = (status) => {
  const map = {
    READY: 'success',
    FAILED: 'danger',
    GENERATING: 'warning',
    EXPIRED: 'info'
  }
  return map[String(status || '').toUpperCase()] || 'info'
}

const resolveReason = (row) => row.failReason || (row.fallbackOnly ? '工作流异常后已走规则兜底' : '未记录失败原因')

const loadFailures = async () => {
  loading.value = true
  try {
    const res = await aiGrowthCoachAdminApi.getFailures()
    failures.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载AI成长教练失败案例失败', error)
    ElMessage.error('加载AI成长教练失败案例失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadFailures()
})
</script>

<style scoped>
.ai-growth-coach-failures-page {
  padding: 20px;
}

.hero-card,
.table-card {
  margin-bottom: 16px;
}

.hero-header,
.table-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.hero-header h2 {
  margin: 0 0 8px;
}

.hero-header p,
.table-tip {
  margin: 0;
  color: #909399;
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  border-radius: 16px;
}

.metric-label {
  color: #909399;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-value.danger {
  color: #f56c6c;
}
</style>
