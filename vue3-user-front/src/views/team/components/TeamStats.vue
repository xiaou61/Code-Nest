<template>
  <div class="team-stats">
    <div class="stats-header">
      <div class="stats-tabs">
        <el-button-group>
          <el-button
            :type="period === 'weekly' ? 'primary' : 'default'"
            @click="period = 'weekly'"
          >
            本周
          </el-button>
          <el-button
            :type="period === 'monthly' ? 'primary' : 'default'"
            @click="period = 'monthly'"
          >
            本月
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="stats-content" v-loading="loading">
      <div class="stats-overview">
        <div class="stat-card">
          <div class="stat-icon checkin">
            <el-icon><Check /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.totalCheckins || 0 }}</div>
            <div class="stat-label">总打卡次数</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon duration">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatDuration(periodDuration) }}</div>
            <div class="stat-label">{{ periodLabel }}学习时长</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon active">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ overview.todayCheckinCount || 0 }}</div>
            <div class="stat-label">今日打卡人数</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon rate">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ formatRate(overview.todayCheckinRate) }}</div>
            <div class="stat-label">今日打卡率</div>
          </div>
        </div>
      </div>

      <div class="stats-summary">
        <span class="summary-item">成员 {{ overview.memberCount || 0 }}</span>
        <span class="summary-item">任务 {{ overview.taskCount || 0 }}</span>
        <span class="summary-item">进行中 {{ overview.activeTaskCount || 0 }}</span>
        <span class="summary-item">讨论 {{ overview.discussionCount || 0 }}</span>
      </div>

      <div class="daily-stats" v-if="dailyData.length > 0">
        <h3>{{ periodLabel }}每日打卡</h3>
        <div class="daily-chart">
          <div
            v-for="(day, index) in dailyData"
            :key="index"
            class="day-bar"
            :style="{ height: getBarHeight(day.checkinCount || 0) + '%' }"
            :title="getDayTitle(day)"
          >
            <span v-if="day.checkinCount > 0" class="day-count">{{ day.checkinCount }}</span>
          </div>
        </div>
        <div class="daily-labels">
          <span v-for="(day, index) in dailyData" :key="index">
            {{ formatDayLabel(day.date, index) }}
          </span>
        </div>
      </div>

      <div class="contribution-rank" v-if="topMembers.length > 0">
        <h3>{{ periodLabel }}打卡排行</h3>
        <div class="rank-list">
          <div
            v-for="(member, index) in topMembers"
            :key="member.userId"
            class="rank-item"
          >
            <span class="rank-pos">{{ index + 1 }}</span>
            <div class="member-avatar">
              <img v-if="member.userAvatar" :src="member.userAvatar" />
              <span v-else>{{ member.userName?.charAt(0) || '用' }}</span>
            </div>
            <span class="member-name">{{ member.userName }}</span>
            <span class="member-count">{{ getMemberCheckinCount(member) }}次</span>
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: getProgressWidth(member) + '%' }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { Check, Timer, User, TrendCharts } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  teamId: { type: [String, Number], required: true }
})

const period = ref('weekly')
const loading = ref(false)
const overview = ref({})
const dailyData = ref([])
const topMembers = ref([])

const periodLabel = computed(() => period.value === 'weekly' ? '本周' : '本月')

const maxCount = computed(() => {
  if (topMembers.value.length === 0) return 1
  return Math.max(...topMembers.value.map(getMemberCheckinCount), 1)
})

const maxDaily = computed(() => {
  if (dailyData.value.length === 0) return 1
  return Math.max(...dailyData.value.map(item => item.checkinCount || 0), 1)
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

watch(() => props.teamId, () => {
  loadStats()
})

const loadStats = async () => {
  loading.value = true
  try {
    const now = new Date()
    const [overviewResponse, periodResponse, rankResponse] = await Promise.all([
      teamApi.getTeamStats(props.teamId),
      period.value === 'weekly'
        ? teamApi.getWeeklyStats(props.teamId)
        : teamApi.getMonthlyStats(props.teamId, now.getFullYear(), now.getMonth() + 1),
      teamApi.getCheckinRank(props.teamId, {
        type: period.value === 'weekly' ? 'weekly' : 'monthly',
        limit: 5
      })
    ])

    overview.value = overviewResponse || {}
    dailyData.value = period.value === 'weekly'
      ? (periodResponse?.weeklyStats || [])
      : (periodResponse?.monthlyStats || [])
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

function getMemberCheckinCount(member) {
  return member.checkinCount || member.weeklyCheckins || member.monthlyCheckins || member.totalCheckins || 0
}

const formatDuration = (minutes) => {
  if (!minutes) return '0h'
  const hours = Math.floor(minutes / 60)
  const remain = minutes % 60
  if (hours > 0 && remain > 0) return `${hours}h ${remain}m`
  if (hours > 0) return `${hours}h`
  return `${minutes}m`
}

const formatRate = (rate) => `${Math.round(rate || 0)}%`

const getBarHeight = (count) => Math.max((count / maxDaily.value) * 100, 5)

const getProgressWidth = (member) => (getMemberCheckinCount(member) / maxCount.value) * 100

const formatDayLabel = (date, index) => {
  if (!date) return ''
  const day = String(date).split('-').pop() || ''
  if (dailyData.value.length <= 7 || index % 2 === 0) {
    return day.replace(/^0/, '')
  }
  return ''
}

const getDayTitle = (day) => {
  return `${day.date}: ${day.checkinCount || 0}次打卡，学习时长 ${formatDuration(day.totalDuration || 0)}`
}
</script>

<style lang="scss" scoped>
.team-stats {
  min-height: 300px;
}

.stats-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fc;
  border-radius: 12px;

  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 22px;
      color: white;
    }

    &.checkin { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
    &.duration { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
    &.active { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
    &.rate { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
  }

  .stat-info {
    .stat-value {
      font-size: 20px;
      font-weight: bold;
      color: #333;
    }

    .stat-label {
      font-size: 13px;
      color: #999;
      margin-top: 2px;
    }
  }
}

.stats-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;

  .summary-item {
    padding: 6px 12px;
    background: #f5f7fa;
    border-radius: 999px;
    font-size: 13px;
    color: #606266;
  }
}

.daily-stats {
  margin-bottom: 24px;

  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    margin: 0 0 16px 0;
  }
}

.daily-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 120px;
  padding: 0 8px;
  background: #f8f9fc;
  border-radius: 8px;

  .day-bar {
    flex: 1;
    max-width: 30px;
    margin: 0 2px;
    background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
    border-radius: 4px 4px 0 0;
    position: relative;
    min-height: 4px;
    transition: height 0.3s;

    .day-count {
      position: absolute;
      top: -20px;
      left: 50%;
      transform: translateX(-50%);
      font-size: 11px;
      color: #666;
    }
  }
}

.daily-labels {
  display: flex;
  justify-content: space-between;
  padding: 8px 8px 0;

  span {
    flex: 1;
    max-width: 30px;
    text-align: center;
    font-size: 11px;
    color: #999;
  }
}

.contribution-rank {
  h3 {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    margin: 0 0 16px 0;
  }
}

.rank-list {
  .rank-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 0;

    & + .rank-item {
      border-top: 1px solid #f0f0f0;
    }
  }

  .rank-pos {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f0f0f0;
    border-radius: 50%;
    font-size: 12px;
    font-weight: 500;
    color: #666;
  }

  .member-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    overflow: hidden;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    span {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      color: white;
      font-size: 14px;
    }
  }

  .member-name {
    flex: 1;
    font-size: 14px;
    color: #333;
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .member-count {
    font-size: 13px;
    color: #999;
    width: 50px;
    text-align: right;
  }

  .progress-bar {
    width: 100px;
    height: 6px;
    background: #f0f0f0;
    border-radius: 3px;
    overflow: hidden;

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      border-radius: 3px;
      transition: width 0.3s;
    }
  }
}
</style>
