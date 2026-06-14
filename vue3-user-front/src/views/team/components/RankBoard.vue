<template>
  <div class="rank-board">
    <div class="rank-header">
      <el-segmented v-model="activeTab" :options="rankOptions">
        <template #default="{ item }">
          <span class="rank-option">
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </span>
        </template>
      </el-segmented>
    </div>

    <CnSection v-if="myRank" class="my-rank" surface="plain" compact>
      <div class="my-rank__content">
        <div>
          <CnStatusTag type="brand" size="sm" subtle>我的排名</CnStatusTag>
          <strong>#{{ myRank.rank || '-' }}</strong>
        </div>
        <span>{{ getRankValue(myRank) }}{{ getRankLabel() }}</span>
      </div>
    </CnSection>

    <div v-loading="loading" class="rank-list">
      <CnEmptyState
        v-if="rankList.length === 0 && !loading"
        title="暂无排行数据"
        description="成员完成打卡、学习或贡献后，排行榜会自动更新。"
        icon="RK"
        size="sm"
        surface="transparent"
      />

      <article v-for="(item, index) in rankList" :key="item.userId || index" class="rank-item" :class="{ 'top-three': index < 3 }">
        <div class="rank-position">
          <span class="rank-number">{{ index + 1 }}</span>
        </div>

        <div class="rank-avatar">
          <img v-if="item.userAvatar" :src="item.userAvatar" :alt="item.userName || '成员头像'" />
          <span v-else>{{ item.userName?.charAt(0) || '用' }}</span>
        </div>

        <div class="rank-info">
          <div class="rank-name">{{ item.userName || '匿名成员' }}</div>
          <CnStatusTag v-if="Number(item.memberRole) === 1" type="warning" size="sm" :dot="false" subtle>
            组长
          </CnStatusTag>
        </div>

        <div class="rank-value">
          <span class="value">{{ getRankValue(item) }}</span>
          <span class="label">{{ getRankLabel() }}</span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Check, Odometer, Timer, TrendCharts } from '@element-plus/icons-vue'
import { CnEmptyState, CnSection, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'

type RankTab = 'checkin' | 'streak' | 'duration' | 'contribution'

interface RankItem {
  userId?: number | string
  userAvatar?: string
  userName?: string
  memberRole?: number | string
  rank?: number | string
  checkinCount?: number
  streakDays?: number
  totalDuration?: number
  contribution?: number
  count?: number
}

const props = defineProps<{
  teamId: string | number
}>()

const rankOptions = [
  { value: 'checkin', label: '打卡榜', icon: Check },
  { value: 'streak', label: '连续榜', icon: Odometer },
  { value: 'duration', label: '时长榜', icon: Timer },
  { value: 'contribution', label: '贡献榜', icon: TrendCharts }
]

const activeTab = ref<RankTab>('checkin')
const rankList = ref<RankItem[]>([])
const myRank = ref<RankItem | null>(null)
const loading = ref(false)

onMounted(() => {
  loadRankData()
})

watch(activeTab, () => {
  loadRankData()
})

watch(
  () => props.teamId,
  () => {
    loadRankData()
  }
)

const loadRankData = async () => {
  loading.value = true
  try {
    let response: RankItem[] | undefined
    switch (activeTab.value) {
      case 'checkin':
        response = (await teamApi.getCheckinRank(props.teamId, { type: 'total', limit: 20 })) as RankItem[]
        break
      case 'streak':
        response = (await teamApi.getStreakRank(props.teamId)) as RankItem[]
        break
      case 'duration':
        response = (await teamApi.getDurationRank(props.teamId, { type: 'total', limit: 20 })) as RankItem[]
        break
      case 'contribution':
        response = (await teamApi.getContributionRank(props.teamId)) as RankItem[]
        break
    }
    rankList.value = response || []

    try {
      myRank.value = (await teamApi.getMyRank(props.teamId, activeTab.value)) as RankItem
    } catch {
      myRank.value = null
    }
  } catch (error) {
    console.error('加载排行榜失败:', error)
    rankList.value = []
  } finally {
    loading.value = false
  }
}

const getRankValue = (item: RankItem) => {
  switch (activeTab.value) {
    case 'checkin':
      return item.checkinCount || item.count || 0
    case 'streak':
      return item.streakDays || item.count || 0
    case 'duration':
      return formatDuration(item.totalDuration || item.count || 0)
    case 'contribution':
      return item.contribution || item.count || 0
    default:
      return item.count || 0
  }
}

const getRankLabel = () => {
  switch (activeTab.value) {
    case 'checkin':
      return '次'
    case 'streak':
      return '天'
    case 'duration':
      return ''
    case 'contribution':
      return '分'
    default:
      return ''
  }
}

const formatDuration = (minutes: number) => {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h${mins}m` : `${hours}小时`
}
</script>

<style scoped>
.rank-board {
  display: grid;
  gap: var(--cn-space-5);
  min-height: 300px;
}

.rank-header {
  display: flex;
  justify-content: flex-start;
  overflow-x: auto;
}

.rank-option {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.my-rank {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.my-rank__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.my-rank__content > div {
  display: grid;
  gap: var(--cn-space-2);
}

.my-rank strong {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 26px;
  line-height: 1;
}

.my-rank__content > span {
  color: var(--cn-color-brand-primary);
  font-size: 15px;
  font-weight: 750;
}

.rank-list {
  display: grid;
  gap: var(--cn-space-2);
}

.rank-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  transition:
    background-color var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out);
}

.rank-item:hover {
  border-color: var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
}

.rank-item.top-three {
  background: color-mix(in srgb, var(--cn-color-brand-soft) 36%, var(--cn-color-bg-surface));
}

.rank-position {
  width: 34px;
  flex-shrink: 0;
  text-align: center;
}

.rank-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 750;
}

.top-three .rank-number {
  background: var(--cn-color-brand-primary);
  color: var(--cn-color-text-inverse);
}

.rank-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 800;
}

.rank-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rank-info {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  flex: 1;
  min-width: 0;
}

.rank-name {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-value {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: var(--cn-space-1);
  flex-shrink: 0;
  min-width: 72px;
  text-align: right;
}

.rank-value .value {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 19px;
  font-weight: 800;
}

.rank-value .label {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

@media (max-width: 640px) {
  .my-rank__content,
  .rank-item {
    align-items: flex-start;
  }

  .my-rank__content {
    display: grid;
  }
}
</style>
