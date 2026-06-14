<template>
  <CnPage class="oj-statistics" surface="transparent" max-width="1080px">
    <CnPageHeader
      class="cn-learn-reveal"
      title="做题统计"
      description="跟踪个人提交、通过题数与难度分布，判断下一阶段训练重心。"
      eyebrow="OJ Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          提交 {{ stats.totalSubmissions || 0 }}
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          通过 {{ stats.acceptedProblems || 0 }}
        </CnStatusTag>
        <CnStatusTag type="warning" size="sm">
          通过率 {{ passRate }}%
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj')">
          <el-icon><ArrowLeft /></el-icon>
          返回题目列表
        </el-button>
        <el-button type="primary" :loading="ojStore.statisticsLoading" @click="loadStatistics">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <section class="stats-grid cn-learn-reveal" aria-label="做题数据概览">
      <CnStatCard
        title="总提交次数"
        :value="stats.totalSubmissions || 0"
        unit="次"
        description="包含所有题目的提交记录"
        tone="brand"
        trend="flat"
        :loading="ojStore.statisticsLoading"
        trend-text="累计"
      />
      <CnStatCard
        title="通过题目"
        :value="stats.acceptedProblems || 0"
        unit="题"
        description="已经 Accepted 的题目数量"
        tone="success"
        trend="flat"
        :loading="ojStore.statisticsLoading"
        trend-text="已掌握"
      />
      <CnStatCard
        title="尝试题目"
        :value="stats.attemptedProblems || 0"
        unit="题"
        description="有过提交记录的题目数量"
        tone="info"
        trend="flat"
        :loading="ojStore.statisticsLoading"
        trend-text="覆盖"
      />
      <CnStatCard
        title="通过率"
        :value="`${passRate}%`"
        description="通过题目 / 尝试题目"
        tone="warning"
        trend="flat"
        :loading="ojStore.statisticsLoading"
        trend-text="质量"
      />
    </section>

    <CnSection
      class="difficulty-section cn-learn-reveal"
      title="难度分布"
      :description="difficultyDescription"
      surface="panel"
      divided
    >
      <template #actions>
        <CnStatusTag type="neutral" size="sm">
          总计 {{ difficultyTotal }} 题
        </CnStatusTag>
      </template>

      <div class="difficulty-bars" v-loading="ojStore.statisticsLoading">
        <div
          v-for="item in difficultyItems"
          :key="item.key"
          class="difficulty-item"
          :class="`difficulty-item--${item.tone}`"
        >
          <div class="difficulty-header">
            <CnStatusTag :type="item.tone" size="sm" :dot="false">
              {{ item.label }}
            </CnStatusTag>
            <span class="difficulty-count">{{ item.count }} 题</span>
          </div>
          <el-progress
            :percentage="item.percent"
            :stroke-width="18"
            :color="item.color"
            :show-text="false"
          />
        </div>
      </div>

      <CnEmptyState
        v-if="!ojStore.statisticsLoading && difficultyTotal === 0"
        title="暂无做题统计"
        description="完成题目提交后，这里会展示不同难度的通过分布。"
        icon="OJ"
        surface="transparent"
      >
        <template #actions>
          <el-button type="primary" @click="router.push('/oj')">去刷题</el-button>
        </template>
      </CnEmptyState>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnTone } from '@/design-system'
import { useOjStore } from '@/stores/oj'
import { useRevealMotion } from '@/utils/reveal-motion'

interface OjStatistics {
  totalSubmissions?: number
  acceptedProblems?: number
  attemptedProblems?: number
  easyAccepted?: number
  mediumAccepted?: number
  hardAccepted?: number
}

const router = useRouter()
const ojStore = useOjStore()
useRevealMotion('.oj-statistics .cn-learn-reveal')

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '做题统计' }
]

const stats = computed<OjStatistics>(() => ojStore.statistics || {})

const passRate = computed(() => {
  const accepted = stats.value.acceptedProblems || 0
  const attempted = stats.value.attemptedProblems || 0
  return attempted > 0 ? ((accepted / attempted) * 100).toFixed(1) : '0.0'
})

const difficultyTotal = computed(() => {
  return (stats.value.easyAccepted || 0) + (stats.value.mediumAccepted || 0) + (stats.value.hardAccepted || 0)
})

const getPercent = (count?: number) => {
  if (!difficultyTotal.value || !count) return 0
  return Math.round((count / difficultyTotal.value) * 100)
}

const difficultyItems = computed(() => {
  const items: Array<{
    key: string
    label: string
    count: number
    percent: number
    tone: CnTone
    color: string
  }> = [
    {
      key: 'easy',
      label: '简单',
      count: stats.value.easyAccepted || 0,
      percent: getPercent(stats.value.easyAccepted),
      tone: 'success',
      color: 'var(--cn-color-success)'
    },
    {
      key: 'medium',
      label: '中等',
      count: stats.value.mediumAccepted || 0,
      percent: getPercent(stats.value.mediumAccepted),
      tone: 'warning',
      color: 'var(--cn-color-warning)'
    },
    {
      key: 'hard',
      label: '困难',
      count: stats.value.hardAccepted || 0,
      percent: getPercent(stats.value.hardAccepted),
      tone: 'danger',
      color: 'var(--cn-color-danger)'
    }
  ]

  return items
})

const difficultyDescription = computed(() => {
  if (!difficultyTotal.value) return '目前还没有已通过题目的难度分布。'
  return `已通过题目中，简单 ${difficultyItems.value[0].percent}%、中等 ${difficultyItems.value[1].percent}%、困难 ${difficultyItems.value[2].percent}%。`
})

const loadStatistics = () => {
  ojStore.fetchStatistics({ forceRefresh: true })
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.oj-statistics {
  min-height: calc(100vh - 68px);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.difficulty-bars {
  display: grid;
  gap: var(--cn-space-5);
  min-height: 160px;
}

.difficulty-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
}

.difficulty-count {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.difficulty-item :deep(.el-progress-bar__outer) {
  background: var(--cn-color-bg-surface-muted);
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
