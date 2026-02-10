<template>
  <div class="oj-statistics">
    <div class="page-header">
      <el-button text @click="$router.push('/oj')">
        <el-icon><ArrowLeft /></el-icon>
        返回题目列表
      </el-button>
      <h2>做题统计</h2>
    </div>

    <div class="stats-content" v-loading="ojStore.statisticsLoading">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalSubmissions || 0 }}</div>
          <div class="stat-label">总提交次数</div>
        </div>
        <div class="stat-card accent">
          <div class="stat-value">{{ stats.acceptedProblems || 0 }}</div>
          <div class="stat-label">通过题目</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.attemptedProblems || 0 }}</div>
          <div class="stat-label">尝试题目</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ passRate }}%</div>
          <div class="stat-label">通过率</div>
        </div>
      </div>

      <!-- 难度分布 -->
      <el-card shadow="never" class="chart-card">
        <template #header>
          <span class="card-title">难度分布</span>
        </template>
        <div class="difficulty-bars">
          <div class="bar-item">
            <div class="bar-header">
              <el-tag type="success" size="small" effect="dark">简单</el-tag>
              <span class="bar-count">{{ stats.easyAccepted || 0 }} 题</span>
            </div>
            <el-progress
              :percentage="getPercent(stats.easyAccepted)"
              :stroke-width="20"
              color="#10b981"
              :show-text="false"
            />
          </div>
          <div class="bar-item">
            <div class="bar-header">
              <el-tag type="warning" size="small" effect="dark">中等</el-tag>
              <span class="bar-count">{{ stats.mediumAccepted || 0 }} 题</span>
            </div>
            <el-progress
              :percentage="getPercent(stats.mediumAccepted)"
              :stroke-width="20"
              color="#f59e0b"
              :show-text="false"
            />
          </div>
          <div class="bar-item">
            <div class="bar-header">
              <el-tag type="danger" size="small" effect="dark">困难</el-tag>
              <span class="bar-count">{{ stats.hardAccepted || 0 }} 题</span>
            </div>
            <el-progress
              :percentage="getPercent(stats.hardAccepted)"
              :stroke-width="20"
              color="#ef4444"
              :show-text="false"
            />
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useOjStore } from '@/stores/oj'

const ojStore = useOjStore()
const stats = computed(() => ojStore.statistics)

const passRate = computed(() => {
  const a = stats.value.acceptedProblems || 0
  const t = stats.value.attemptedProblems || 0
  return t > 0 ? ((a / t) * 100).toFixed(1) : '0.0'
})

const getPercent = (count) => {
  const total = (stats.value.easyAccepted || 0) + (stats.value.mediumAccepted || 0) + (stats.value.hardAccepted || 0)
  if (!total || !count) return 0
  return Math.round((count / total) * 100)
}

onMounted(() => {
  ojStore.fetchStatistics({ forceRefresh: true })
})
</script>

<style scoped>
.oj-statistics {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.stat-card.accent {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

.stat-card.accent .stat-value,
.stat-card.accent .stat-label {
  color: #fff;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #1f2937;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.chart-card {
  border-radius: 12px;
}

.card-title {
  font-weight: 600;
  color: #374151;
}

.difficulty-bars {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bar-count {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

@media (max-width: 600px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
