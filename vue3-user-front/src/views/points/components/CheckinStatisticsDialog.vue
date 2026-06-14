<template>
  <el-dialog
    class="checkin-statistics-dialog"
    :model-value="modelValue"
    title="打卡统计"
    width="760px"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <div v-loading="loading" class="statistics-dialog">
      <CnEmptyState
        v-if="!loading && !statisticsData"
        title="暂无打卡统计"
        description="完成打卡后，系统会生成连续天数、累计天数和月度走势。"
        icon="ST"
      />

      <template v-else-if="statisticsData">
        <div class="overview-stats">
          <CnStatCard
            title="连续打卡"
            :value="statisticsData.continuousDays || 0"
            unit="天"
            description="当前连续打卡天数"
            tone="brand"
          />
          <CnStatCard
            title="累计打卡"
            :value="statisticsData.totalCheckinDays || 0"
            unit="天"
            description="历史累计打卡天数"
            tone="success"
          />
          <CnStatCard
            title="打卡积分"
            :value="statisticsData.totalPointsEarned || 0"
            unit="分"
            description="通过打卡获得的积分"
            tone="warning"
          />
        </div>

        <CnSection title="最近几个月打卡情况" compact surface="plain">
          <div v-if="(statisticsData.monthlyStats || []).length === 0" class="muted-empty">
            暂无月度打卡数据
          </div>

          <div v-else class="monthly-list">
            <article
              v-for="month in statisticsData.monthlyStats || []"
              :key="month.yearMonth"
              class="month-item"
            >
              <div class="month-header">
                <strong>{{ formatMonth(month.yearMonth) }}</strong>
                <span>{{ month.checkinDays }}/{{ month.totalDays }} 天</span>
              </div>

              <div class="month-progress">
                <el-progress
                  :percentage="getProgressPercent(month.checkinDays, month.totalDays)"
                  :stroke-width="10"
                  :show-text="false"
                />
                <span class="progress-percent">
                  {{ getProgressPercent(month.checkinDays, month.totalDays) }}%
                </span>
              </div>

              <p class="month-points">
                本月打卡获得
                <strong>{{ month.pointsEarned || 0 }}</strong>
                积分
              </p>
            </article>
          </div>
        </CnSection>

        <CnSection title="打卡成就" compact surface="plain">
          <div class="achievement-list">
            <article
              v-for="achievement in achievements"
              :key="achievement.id"
              class="achievement-item"
              :class="{ 'is-achieved': achievement.achieved }"
            >
              <div class="achievement-copy">
                <strong>{{ achievement.name }}</strong>
                <span>{{ achievement.description }}</span>
              </div>
              <CnStatusTag :type="achievement.achieved ? 'success' : 'neutral'" size="sm">
                {{ achievement.achieved ? '已达成' : '未达成' }}
              </CnStatusTag>
            </article>
          </div>
        </CnSection>
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CnEmptyState, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import pointsApi from '@/api/points'

interface MonthlyStat {
  yearMonth?: string
  checkinDays?: number
  totalDays?: number
  pointsEarned?: number
}

interface StatisticsData {
  continuousDays?: number
  totalCheckinDays?: number
  totalPointsEarned?: number
  monthlyStats?: MonthlyStat[]
}

interface Achievement {
  id: string
  name: string
  description: string
  achieved: boolean
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const statisticsData = ref<StatisticsData | null>(null)
const loading = ref(false)

const achievements = computed<Achievement[]>(() => {
  if (!statisticsData.value) return []

  const { continuousDays = 0, totalCheckinDays = 0 } = statisticsData.value

  return [
    {
      id: 'first_checkin',
      name: '初来乍到',
      description: '完成第一次打卡',
      achieved: totalCheckinDays >= 1
    },
    {
      id: 'week_warrior',
      name: '一周打卡王',
      description: '连续打卡 7 天',
      achieved: continuousDays >= 7
    },
    {
      id: 'month_master',
      name: '月度大师',
      description: '连续打卡 30 天',
      achieved: continuousDays >= 30
    },
    {
      id: 'century_club',
      name: '百日俱乐部',
      description: '累计打卡 100 天',
      achieved: totalCheckinDays >= 100
    },
    {
      id: 'persistent_pro',
      name: '坚持达人',
      description: '连续打卡 100 天',
      achieved: continuousDays >= 100
    },
    {
      id: 'year_legend',
      name: '年度传奇',
      description: '累计打卡 365 天',
      achieved: totalCheckinDays >= 365
    }
  ]
})

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue) {
      loadStatisticsData()
    }
  }
)

const loadStatisticsData = async () => {
  loading.value = true

  try {
    const response = (await pointsApi.getCheckinStatistics(3)) as StatisticsData
    statisticsData.value = response
  } catch (error) {
    console.error('加载打卡统计失败:', error)
    statisticsData.value = null
  } finally {
    loading.value = false
  }
}

const handleVisibleChange = (visible: boolean) => {
  emit('update:modelValue', visible)
}

const formatMonth = (yearMonth?: string) => {
  if (!yearMonth) return ''
  const [year, month] = yearMonth.split('-')
  return `${year}年${Number.parseInt(month, 10)}月`
}

const getProgressPercent = (checkinDays = 0, totalDays = 0) => {
  if (!totalDays || totalDays === 0) return 0
  return Math.round((checkinDays / totalDays) * 100)
}
</script>

<style lang="scss" scoped>
.statistics-dialog {
  display: grid;
  gap: var(--cn-space-4);
  min-height: 320px;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.muted-empty {
  padding: var(--cn-space-6);
  border: 1px dashed var(--cn-color-border);
  border-radius: var(--cn-radius-card);
  color: var(--cn-color-text-secondary);
  text-align: center;
}

.monthly-list,
.achievement-list {
  display: grid;
  gap: var(--cn-space-3);
}

.month-item,
.achievement-item {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.month-header,
.month-progress,
.achievement-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.month-header {
  color: var(--cn-color-text-primary);
  font-size: 14px;

  span {
    color: var(--cn-color-text-secondary);
  }
}

.month-progress {
  margin-top: var(--cn-space-3);

  :deep(.el-progress) {
    flex: 1;
  }
}

.progress-percent {
  min-width: 42px;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
  text-align: right;
}

.month-points {
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;

  strong {
    color: var(--cn-color-success);
  }
}

.achievement-item.is-achieved {
  border-color: color-mix(in srgb, var(--cn-color-success) 28%, var(--cn-color-border-subtle));
  background: var(--cn-color-success-soft);
}

.achievement-copy {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;

  strong {
    color: var(--cn-color-text-primary);
    font-size: 14px;
  }

  span {
    color: var(--cn-color-text-secondary);
    font-size: 13px;
    line-height: 1.5;
  }
}

:deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}

@media (max-width: 760px) {
  .overview-stats {
    grid-template-columns: 1fr;
  }

  .achievement-item {
    align-items: flex-start;
  }
}
</style>
