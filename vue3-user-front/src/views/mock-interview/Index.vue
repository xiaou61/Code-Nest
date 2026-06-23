<template>
  <CnPage class="mock-interview-index" surface="transparent" max-width="1280px" full-height>
    <CnPageHeader
      title="AI 模拟面试"
      description="选择技术方向后进入配置页，由 AI 面试官完成出题、追问、评分和报告沉淀。"
      eyebrow="AI Interview"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">累计 {{ stats.totalInterviews || 0 }} 场</CnStatusTag>
        <CnStatusTag type="success" size="sm">最高分 {{ stats.highestScore || 0 }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">方向 {{ directions.length }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goToHistory">
          <el-icon><Clock /></el-icon>
          历史记录
        </el-button>
        <el-button type="primary" :disabled="!selectedDirection" @click="goToConfig">
          <el-icon><VideoPlay /></el-icon>
          开始 AI 面试
        </el-button>
      </template>
    </CnPageHeader>

    <section class="stats-grid" aria-label="模拟面试统计">
      <CnStatCard
        title="累计面试"
        :value="stats.totalInterviews || 0"
        unit="场"
        description="已创建的模拟面试会话"
        tone="brand"
        trend="flat"
        trend-text="累计"
      />
      <CnStatCard
        title="平均分数"
        :value="stats.avgScore || 0"
        description="已完成面试的平均得分"
        tone="warning"
        trend="flat"
        trend-text="表现"
      />
      <CnStatCard
        title="最高分"
        :value="stats.highestScore || 0"
        description="历史报告中的最佳成绩"
        tone="success"
        trend="up"
        trend-text="峰值"
      />
      <CnStatCard
        title="连续练习"
        :value="stats.interviewStreak || 0"
        unit="天"
        description="持续模拟面试的连续天数"
        tone="info"
        trend="flat"
        trend-text="习惯"
      />
    </section>

    <CnSection
      title="选择面试方向"
      description="先选一个方向，下一步会进入面试配置，继续选择难度、题量和面试风格。"
      surface="panel"
      divided
    >
      <template #actions>
        <CnStatusTag v-if="selectedDirection" type="success" size="sm">
          已选择 {{ selectedDirectionName }}
        </CnStatusTag>
        <CnStatusTag v-else type="warning" size="sm">待选择</CnStatusTag>
      </template>

      <div v-loading="loading" class="direction-grid">
        <CnEmptyState
          v-if="!loading && directions.length === 0"
          title="暂无面试方向"
          description="当前没有可用方向，请稍后刷新或联系管理员配置题库。"
          icon="AI"
          surface="transparent"
        >
          <template #actions>
            <el-button plain @click="fetchDirections">重新加载</el-button>
          </template>
        </CnEmptyState>

        <button
          v-for="direction in directions"
          v-else
          :key="direction.directionCode"
          class="direction-item"
          :class="{ 'is-selected': selectedDirection === direction.directionCode }"
          type="button"
          @click="selectDirection(direction)"
        >
          <span class="direction-icon">
            <el-icon :size="26">
              <component :is="getDirectionIcon(direction.directionCode)" />
            </el-icon>
          </span>
          <span class="direction-info">
            <strong>{{ direction.directionName }}</strong>
            <em>{{ direction.description || '技术面试' }}</em>
          </span>
          <span v-if="selectedDirection === direction.directionCode" class="direction-check">
            <el-icon><Check /></el-icon>
          </span>
        </button>
      </div>

      <div class="start-section">
        <el-button
          type="primary"
          size="large"
          class="start-btn"
          :disabled="!selectedDirection"
          @click="goToConfig"
        >
          <el-icon><VideoPlay /></el-icon>
          开始 AI 面试
        </el-button>
        <span v-if="!selectedDirection" class="start-tip">请先选择面试方向</span>
      </div>
    </CnSection>

    <CnSection title="功能特点" description="把真实面试中的问答、追问、评价和复盘固定成可重复练习的闭环。" surface="panel" divided>
      <div class="feature-grid">
        <article v-for="feature in features" :key="feature.title" class="feature-item">
          <span class="feature-icon">
            <el-icon><component :is="feature.icon" /></el-icon>
          </span>
          <strong>{{ feature.title }}</strong>
          <em>{{ feature.description }}</em>
        </article>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Aim,
  Check,
  Clock,
  Connection,
  Cpu,
  DataBoard,
  DataLine,
  InfoFilled,
  Monitor,
  Setting,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { mockInterviewApi } from '@/api/mockInterview'

interface InterviewDirection {
  directionCode: string
  directionName: string
  description?: string
}

interface InterviewStats {
  totalInterviews: number
  completedInterviews: number
  avgScore: number
  highestScore: number
  totalQuestions: number
  correctQuestions: number
  correctRate: number
  interviewStreak: number
  maxStreak: number
  completionRate: number
}

const router = useRouter()

const loading = ref(false)
const directions = ref<InterviewDirection[]>([])
const selectedDirection = ref<string | null>(null)
const stats = reactive<InterviewStats>({
  totalInterviews: 0,
  completedInterviews: 0,
  avgScore: 0,
  highestScore: 0,
  totalQuestions: 0,
  correctQuestions: 0,
  correctRate: 0,
  interviewStreak: 0,
  maxStreak: 0,
  completionRate: 0
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: 'AI 模拟面试' }
]

const features = [
  {
    title: 'AI 智能出题',
    description: '根据方向、难度和题量生成面试题。',
    icon: Aim
  },
  {
    title: '实时评价反馈',
    description: '回答后即时评分并给出改进建议。',
    icon: InfoFilled
  },
  {
    title: '追问深入考察',
    description: '围绕回答继续追问，模拟真实压力。',
    icon: Connection
  },
  {
    title: '详细面试报告',
    description: '沉淀多维评分和下一步学习建议。',
    icon: DataLine
  }
]

const selectedDirectionName = computed(() => {
  return directions.value.find(direction => direction.directionCode === selectedDirection.value)?.directionName || selectedDirection.value
})

const getDirectionIcon = (directionCode: string) => {
  const iconMap: Record<string, unknown> = {
    java: Cpu,
    frontend: Monitor,
    python: DataBoard,
    go: Connection,
    fullstack: Setting,
    database: DataBoard,
    devops: Setting,
    algorithm: DataLine
  }
  return iconMap[directionCode] || Cpu
}

const selectDirection = (direction: InterviewDirection) => {
  selectedDirection.value = direction.directionCode
}

const goToConfig = () => {
  if (!selectedDirection.value) {
    ElMessage.warning('请先选择面试方向')
    return
  }
  router.push({
    path: '/mock-interview/config',
    query: { direction: selectedDirection.value }
  })
}

const goToHistory = () => {
  router.push('/mock-interview/history')
}

const fetchDirections = async () => {
  loading.value = true
  try {
    const data = (await mockInterviewApi.getDirections()) as InterviewDirection[]
    directions.value = data || []
  } catch (error) {
    console.error('获取方向列表失败', error)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const data = (await mockInterviewApi.getStats()) as Partial<InterviewStats>
    if (data) {
      Object.assign(stats, data)
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
  }
}

onMounted(() => {
  fetchDirections()
  fetchStats()
})
</script>

<style scoped>
.mock-interview-index {
  min-height: calc(100vh - 74px);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.direction-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
  min-height: 180px;
}

.direction-item {
  position: relative;
  display: grid;
  min-width: 0;
  gap: var(--cn-space-3);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.direction-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateY(-2px);
}

.direction-item.is-selected {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 48%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-brand-soft) 78%, var(--cn-color-bg-surface));
}

.direction-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-info));
  color: white;
  box-shadow: 0 12px 24px color-mix(in srgb, var(--cn-color-brand-primary) 20%, transparent);
}

.direction-info {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-2);
}

.direction-info strong {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 700;
}

.direction-info em {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-style: normal;
  line-height: 1.65;
}

.direction-check {
  position: absolute;
  top: var(--cn-space-3);
  right: var(--cn-space-3);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-success);
  color: white;
}

.start-section {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-5);
  padding-top: var(--cn-space-5);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.start-btn {
  min-width: 220px;
  min-height: 48px;
  font-size: 16px;
  font-weight: 700;
}

.start-tip {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.feature-item {
  display: grid;
  justify-items: start;
  min-width: 0;
  gap: var(--cn-space-3);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.feature-item strong {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.feature-item em {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-style: normal;
  line-height: 1.65;
}

@media (max-width: 1180px) {
  .stats-grid,
  .direction-grid,
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .direction-grid,
  .feature-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .start-section {
    justify-content: stretch;
  }

  .start-btn {
    width: 100%;
  }
}
</style>
