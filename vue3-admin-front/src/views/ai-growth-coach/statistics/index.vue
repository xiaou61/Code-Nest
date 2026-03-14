<template>
  <div class="ai-growth-coach-statistics-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-header">
        <div>
          <h2>AI成长教练统计</h2>
          <p>查看快照生成、规则兜底、对话使用和动作完成情况。</p>
        </div>
        <el-button type="primary" :loading="loading" @click="loadStatistics">刷新数据</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">总快照数</div>
          <div class="metric-value">{{ statistics.totalSnapshots || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">今日快照数</div>
          <div class="metric-value success">{{ statistics.todaySnapshots || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">规则兜底数</div>
          <div class="metric-value warning">{{ statistics.fallbackSnapshots || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">动作完成率</div>
          <div class="metric-value primary">{{ formatRate(statistics.actionDoneRate) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="metric-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">重排总次数</div>
          <div class="metric-value">{{ statistics.totalReplans || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">今日重排次数</div>
          <div class="metric-value success">{{ statistics.todayReplans || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">重排兜底率</div>
          <div class="metric-value warning">{{ formatRate(statistics.replanFallbackRate) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">平均压缩率</div>
          <div class="metric-value primary">{{ formatRate(statistics.avgCompressionRate) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>对话使用情况</span>
          </template>
          <div class="stat-list">
            <div class="stat-item">
              <label>对话会话数</label>
              <strong>{{ statistics.totalChatSessions || 0 }}</strong>
            </div>
            <div class="stat-item">
              <label>消息总数</label>
              <strong>{{ statistics.totalMessages || 0 }}</strong>
            </div>
            <div class="stat-item">
              <label>平均每会话消息数</label>
              <strong>{{ statistics.avgMessagesPerSession || 0 }}</strong>
            </div>
            <div class="stat-item">
              <label>失败快照数</label>
              <strong>{{ statistics.failedSnapshots || 0 }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>场景分布</span>
          </template>
          <el-table :data="sceneRows" v-loading="loading" border>
            <el-table-column prop="label" label="场景" min-width="140" />
            <el-table-column prop="count" label="快照数" min-width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import aiGrowthCoachAdminApi from '@/api/aiGrowthCoach'

const loading = ref(false)
const statistics = reactive({})

const sceneRows = computed(() => {
  const distribution = statistics.sceneDistribution || {}
  return [
    { label: '学习场景', count: distribution.LEARNING || 0 },
    { label: '求职场景', count: distribution.CAREER || 0 },
    { label: '综合场景', count: distribution.HYBRID || 0 }
  ]
})

const formatRate = (value) => `${Number(value || 0).toFixed(2)}%`

const loadStatistics = async () => {
  loading.value = true
  try {
    const res = await aiGrowthCoachAdminApi.getStatistics()
    Object.assign(statistics, res || {})
  } catch (error) {
    console.error('加载AI成长教练统计失败', error)
    ElMessage.error('加载AI成长教练统计失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.ai-growth-coach-statistics-page {
  padding: 20px;
}

.hero-card,
.table-card {
  margin-bottom: 16px;
}

.hero-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.hero-header h2 {
  margin: 0 0 8px;
}

.hero-header p {
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

.metric-value.success {
  color: #67c23a;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-value.primary {
  color: #409eff;
}

.stat-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.stat-item {
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: #f9fbff;
}

.stat-item label {
  display: block;
  color: #909399;
  margin-bottom: 8px;
}

.stat-item strong {
  font-size: 24px;
  color: #1f2d3d;
}
</style>
