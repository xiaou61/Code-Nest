<template>
  <CnSection v-if="hasReviewTasks" class="review-reminder-card" title="今日待复习" surface="panel" compact>
    <template #actions>
      <el-button text type="primary" @click="goToReview">查看全部</el-button>
    </template>

    <div class="stats-row">
      <div class="stat-item overdue" v-if="stats.overdueCount > 0">
        <CnStatusTag type="danger" size="sm">逾期</CnStatusTag>
        <span class="stat-label">紧急复习（已逾期）</span>
        <span class="stat-value">{{ stats.overdueCount }} 题</span>
      </div>
      <div class="stat-item today" v-if="stats.todayCount > 0">
        <CnStatusTag type="warning" size="sm">今日</CnStatusTag>
        <span class="stat-label">今日待复习</span>
        <span class="stat-value">{{ stats.todayCount }} 题</span>
      </div>
      <div class="stat-item week" v-if="stats.weekCount > 0">
        <CnStatusTag type="success" size="sm">本周</CnStatusTag>
        <span class="stat-label">本周待复习</span>
        <span class="stat-value">{{ stats.weekCount }} 题</span>
      </div>
    </div>

    <div class="action-row">
      <el-button type="primary" @click="startReview" :disabled="!hasReviewTasks">
        开始复习
      </el-button>
      <span class="learned-info">
        已学习 {{ stats.totalLearned || 0 }} 题
      </span>
    </div>
  </CnSection>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CnSection, CnStatusTag } from '@/design-system'
import { interviewApi } from '@/api/interview'

const router = useRouter()

interface ReviewStats {
  overdueCount: number
  todayCount: number
  weekCount: number
  totalLearned: number
}

const stats = ref<ReviewStats>({
  overdueCount: 0,
  todayCount: 0,
  weekCount: 0,
  totalLearned: 0
})

const hasReviewTasks = computed(() => {
  return stats.value.overdueCount > 0 || stats.value.todayCount > 0 || stats.value.weekCount > 0
})

const fetchStats = async () => {
  try {
    const res = await interviewApi.getReviewStats()
    // request拦截器已经提取了data
    if (res) {
      stats.value = res as ReviewStats
    }
  } catch (error) {
    console.error('获取复习统计失败', error)
  }
}

const goToReview = () => {
  router.push('/interview/review')
}

const startReview = () => {
  // 优先复习逾期的题目
  if (stats.value.overdueCount > 0) {
    router.push('/interview/review?type=overdue')
  } else if (stats.value.todayCount > 0) {
    router.push('/interview/review?type=today')
  } else {
    router.push('/interview/review?type=week')
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.review-reminder-card {
  margin-bottom: var(--cn-space-4);
}

.stats-row {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) var(--cn-space-4);
  background: var(--cn-color-bg-surface-muted);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
}

.stat-label {
  flex: 1;
  font-size: 14px;
  color: var(--cn-color-text-secondary);
}

.stat-value {
  font-weight: 600;
  font-size: 15px;
  color: var(--cn-color-text-primary);
}

.action-row {
  display: flex;
  align-items: center;
  gap: var(--cn-space-4);
}

.learned-info {
  font-size: 13px;
  color: var(--cn-color-text-tertiary);
}

@media (max-width: 768px) {
  .stat-item {
    align-items: flex-start;
    padding: var(--cn-space-3);
  }

  .stat-label {
    font-size: 13px;
  }

  .action-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
