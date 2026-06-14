<template>
  <div class="team-stats">
    <div class="stats-header">
      <el-segmented v-model="period" :options="periodOptions" />
    </div>

    <div v-loading="loading" class="stats-content">
      <div class="stats-overview">
        <CnStatCard title="总打卡次数" :value="overview.totalCheckins || 0" description="小组累计打卡" tone="brand" />
        <CnStatCard title="学习时长" :value="formatDuration(periodDuration)" :description="`${periodLabel}累计`" tone="info" />
        <CnStatCard title="今日打卡人数" :value="overview.todayCheckinCount || 0" description="今日参与成员" tone="success" />
        <CnStatCard title="今日打卡率" :value="formatRate(overview.todayCheckinRate)" description="按成员数估算" tone="warning" />
      </div>

      <div class="stats-summary">
        <CnStatusTag type="neutral" size="sm">成员 {{ overview.memberCount || 0 }}</CnStatusTag>
        <CnStatusTag type="neutral" size="sm" subtle>任务 {{ overview.taskCount || 0 }}</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>进行中 {{ overview.activeTaskCount || 0 }}</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>讨论 {{ overview.discussionCount || 0 }}</CnStatusTag>
      </div>

      <CnEmptyState
        v-if="!loading && dailyData.length === 0 && topMembers.length === 0"
        title="暂无统计数据"
        description="小组成员完成打卡后，趋势和排行会显示在这里。"
        icon="ST"
        size="sm"
        surface="transparent"
      />

      <section v-if="dailyData.length > 0" class="daily-stats">
        <h3>{{ periodLabel }}每日打卡</h3>
        <div class="daily-chart">
          <div
            v-for="(day, index) in dailyData"
            :key="`${day.date || index}`"
            class="day-bar"
            :style="{ '--bar-size': `${getBarHeight(day.checkinCount || 0)}%` }"
            :title="getDayTitle(day)"
          >
            <span v-if="Number(day.checkinCount || 0) > 0" class="day-count">{{ day.checkinCount }}</span>
          </div>
        </div>
        <div class="daily-labels">
          <span v-for="(day, index) in dailyData" :key="`${day.date || index}-label`">
            {{ formatDayLabel(day.date, index) }}
          </span>
        </div>
      </section>

      <section v-if="topMembers.length > 0" class="contribution-rank">
        <h3>{{ periodLabel }}打卡排行</h3>
        <div class="rank-list">
          <article v-for="(member, index) in topMembers" :key="member.userId || index" class="rank-item">
            <span class="rank-pos">{{ index + 1 }}</span>
            <div class="member-avatar">
              <img v-if="member.userAvatar" :src="member.userAvatar" :alt="member.userName || '成员头像'" />
              <span v-else>{{ member.userName?.charAt(0) || '用' }}</span>
            </div>
            <span class="member-name">{{ member.userName || '匿名成员' }}</span>
            <span class="member-count">{{ getMemberCheckinCount(member) }} 次</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ '--progress-size': `${getProgressWidth(member)}%` }" />
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { CnEmptyState, CnStatCard, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'

type Period = 'weekly' | 'monthly'

interface TeamOverview {
  totalCheckins?: number
  todayCheckinCount?: number
  todayCheckinRate?: number
  memberCount?: number
  taskCount?: number
  activeTaskCount?: number
  discussionCount?: number
}

interface DailyStat {
  date?: string
  checkinCount?: number
  totalDuration?: number
}

interface RankMember {
  userId?: number | string
  userAvatar?: string
  userName?: string
  checkinCount?: number
  weeklyCheckins?: number
  monthlyCheckins?: number
  totalCheckins?: number
}

interface PeriodResponse {
  weeklyStats?: DailyStat[]
  monthlyStats?: DailyStat[]
}

const props = defineProps<{
  teamId: string | number
}>()

const period = ref<Period>('weekly')
const periodOptions = [
  { label: '本周', value: 'weekly' },
  { label: '本月', value: 'monthly' }
]
const loading = ref(false)
const overview = ref<TeamOverview>({})
const dailyData = ref<DailyStat[]>([])
const topMembers = ref<RankMember[]>([])

const periodLabel = computed(() => (period.value === 'weekly' ? '本周' : '本月'))

const maxCount = computed(() => {
  if (topMembers.value.length === 0) return 1
  return Math.max(...topMembers.value.map(getMemberCheckinCount), 1)
})

const maxDaily = computed(() => {
  if (dailyData.value.length === 0) return 1
  return Math.max(...dailyData.value.map((item) => item.checkinCount || 0), 1)
})

const periodDuration = computed(() => {
  return dailyData.value.reduce((total, item) => total + (item.totalDuration || 0), 0)
})

onMounted(() => {
  loadStats()
})

watch(period, () => {
  loadStats()
})

watch(
  () => props.teamId,
  () => {
    loadStats()
  }
)

const loadStats = async () => {
  loading.value = true
  try {
    const now = new Date()
    const [overviewResponse, periodResponse, rankResponse] = await Promise.all([
      teamApi.getTeamStats(props.teamId) as Promise<TeamOverview>,
      (period.value === 'weekly'
        ? teamApi.getWeeklyStats(props.teamId)
        : teamApi.getMonthlyStats(props.teamId, now.getFullYear(), now.getMonth() + 1)) as Promise<PeriodResponse>,
      teamApi.getCheckinRank(props.teamId, {
        type: period.value === 'weekly' ? 'weekly' : 'monthly',
        limit: 5
      }) as Promise<RankMember[]>
    ])

    overview.value = overviewResponse || {}
    dailyData.value = period.value === 'weekly' ? periodResponse?.weeklyStats || [] : periodResponse?.monthlyStats || []
    topMembers.value = rankResponse || []
  } catch (error) {
    console.error('加载统计数据失败:', error)
    overview.value = {}
    dailyData.value = []
    topMembers.value = []
  } finally {
    loading.value = false
  }
}

function getMemberCheckinCount(member: RankMember) {
  return member.checkinCount || member.weeklyCheckins || member.monthlyCheckins || member.totalCheckins || 0
}

const formatDuration = (minutes?: number) => {
  if (!minutes) return '0h'
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  if (hours > 0 && remain > 0) return `${hours}h ${remain}m`
  if (hours > 0) return `${hours}h`
  return `${minutes}m`
}

const formatRate = (rate?: number) => `${Math.round(rate || 0)}%`

const getBarHeight = (count: number) => Math.max((count / maxDaily.value) * 100, 5)

const getProgressWidth = (member: RankMember) => (getMemberCheckinCount(member) / maxCount.value) * 100

const formatDayLabel = (date: string | undefined, index: number) => {
  if (!date) return ''
  const day = String(date).split('-').pop() || ''
  if (dailyData.value.length <= 7 || index % 2 === 0) {
    return day.replace(/^0/, '')
  }
  return ''
}

const getDayTitle = (day: DailyStat) => {
  return `${day.date}: ${day.checkinCount || 0} 次打卡，学习时长 ${formatDuration(day.totalDuration || 0)}`
}
</script>

<style scoped>
.team-stats {
  min-height: 300px;
}

.stats-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--cn-space-5);
}

.stats-content {
  display: grid;
  gap: var(--cn-space-5);
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.stats-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.daily-stats,
.contribution-rank {
  display: grid;
  gap: var(--cn-space-4);
}

.daily-stats h3,
.contribution-rank h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.daily-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 128px;
  padding: var(--cn-space-4) var(--cn-space-3) 0;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.day-bar {
  position: relative;
  flex: 1;
  max-width: 30px;
  height: var(--bar-size);
  min-height: 4px;
  margin: 0 var(--cn-space-1);
  border-radius: var(--cn-radius-sm) var(--cn-radius-sm) 0 0;
  background: var(--cn-color-brand-primary);
  transition: height var(--cn-motion-base) var(--cn-ease-out);
}

.day-count {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  color: var(--cn-color-text-secondary);
  font-size: 11px;
  font-weight: 650;
}

.daily-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 var(--cn-space-3);
}

.daily-labels span {
  flex: 1;
  max-width: 30px;
  color: var(--cn-color-text-tertiary);
  font-size: 11px;
  text-align: center;
}

.rank-list {
  display: grid;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3) 0;
}

.rank-item + .rank-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.rank-pos,
.member-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--cn-radius-pill);
  flex-shrink: 0;
}

.rank-pos {
  width: 26px;
  height: 26px;
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.member-avatar {
  width: 34px;
  height: 34px;
  overflow: hidden;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 14px;
  font-weight: 800;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-count {
  width: 58px;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
  text-align: right;
}

.progress-bar {
  width: 100px;
  height: 7px;
  overflow: hidden;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
}

.progress-fill {
  width: var(--progress-size);
  height: 100%;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-primary);
  transition: width var(--cn-motion-base) var(--cn-ease-out);
}

@media (max-width: 900px) {
  .stats-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stats-overview {
    grid-template-columns: 1fr;
  }

  .rank-item {
    flex-wrap: wrap;
  }

  .progress-bar {
    width: 100%;
  }
}
</style>
