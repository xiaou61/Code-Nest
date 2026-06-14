<template>
  <CnPage class="oj-ranking" surface="transparent" max-width="1080px">
    <CnPageHeader
      class="cn-learn-reveal"
      title="排行榜"
      description="查看全站与本周刷题表现，按通过题数和提交次数观察训练进度。"
      eyebrow="OJ Ranking"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          {{ rankTypeLabel }}
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          榜单 {{ rankingList.length }} 人
        </CnStatusTag>
        <CnStatusTag v-if="currentUserRank" type="info" size="sm">
          我的排名 {{ currentUserRank }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj')">
          <el-icon><ArrowLeft /></el-icon>
          返回题目列表
        </el-button>
        <el-button type="primary" :loading="loading" @click="loadRanking">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <section class="ranking-stats cn-learn-reveal" aria-label="排行榜概览">
      <CnStatCard
        title="榜单人数"
        :value="rankingList.length"
        unit="人"
        description="当前榜单返回的用户数量"
        tone="brand"
        trend="flat"
        :loading="loading"
        trend-text="实时"
      />
      <CnStatCard
        title="榜首通过"
        :value="topAcceptedCount"
        unit="题"
        description="当前榜单第一名通过题数"
        tone="success"
        trend="flat"
        :loading="loading"
        trend-text="标杆"
      />
      <CnStatCard
        title="平均通过"
        :value="averageAcceptedCount"
        unit="题"
        description="当前榜单人均通过题数"
        tone="info"
        trend="flat"
        :loading="loading"
        trend-text="参考"
      />
      <CnStatCard
        title="我的排名"
        :value="currentUserRank || '-'"
        description="根据本地登录用户 ID 匹配"
        tone="warning"
        trend="flat"
        :loading="loading"
        trend-text="定位"
      />
    </section>

    <CnSection
      class="ranking-section cn-learn-reveal"
      title="排名列表"
      :description="rankingDescription"
      surface="panel"
      divided
    >
      <template #actions>
        <el-radio-group v-model="rankType" size="small" @change="loadRanking">
          <el-radio-button value="all">总榜</el-radio-button>
          <el-radio-button value="weekly">周榜</el-radio-button>
        </el-radio-group>
      </template>

      <CnDataTable
        :columns="rankingColumns"
        :data="rankingList"
        :loading="loading"
        row-key="userId"
        empty-title="暂无排名数据"
        empty-description="榜单生成后会展示用户排名、通过题数与提交次数。"
        empty-icon="TOP"
      >
        <template #rank="{ row }">
          <span class="rank-badge" :class="getRankClass(row.rank)">
            {{ row.rank }}
          </span>
        </template>

        <template #user="{ row }">
          <div class="user-cell" :class="{ 'is-current': isCurrentUser(row) }">
            <el-avatar :size="34" :src="row.avatar" class="user-avatar">
              {{ getAvatarFallback(row.nickname) }}
            </el-avatar>
            <div class="user-copy">
              <span class="user-name">{{ row.nickname || '匿名用户' }}</span>
              <CnStatusTag v-if="isCurrentUser(row)" type="info" size="sm" :dot="false" subtle>
                我
              </CnStatusTag>
            </div>
          </div>
        </template>

        <template #acceptedCount="{ row }">
          <span class="accepted-count">{{ row.acceptedCount || 0 }}</span>
        </template>

        <template #submissionCount="{ row }">
          <span class="muted-count">{{ row.submissionCount || 0 }}</span>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnTableColumn } from '@/design-system'
import { ojApi } from '@/api/oj'
import { useRevealMotion } from '@/utils/reveal-motion'

interface RankingRecord extends Record<string, unknown> {
  userId: number | string
  rank: number
  avatar?: string
  nickname?: string
  acceptedCount?: number
  submissionCount?: number
}

type RankType = 'all' | 'weekly'

const router = useRouter()
useRevealMotion('.oj-ranking .cn-learn-reveal')

const rankType = ref<RankType>('all')
const rankingList = ref<RankingRecord[]>([])
const loading = ref(false)
const currentUserId = ref<number | string | null>(null)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '排行榜' }
]

const rankingColumns: CnTableColumn<RankingRecord>[] = [
  { prop: 'rank', label: '排名', width: 90, align: 'center', slot: 'rank' },
  { prop: 'nickname', label: '用户', minWidth: 240, slot: 'user', showOverflowTooltip: true },
  { prop: 'acceptedCount', label: '通过题数', width: 130, align: 'center', slot: 'acceptedCount' },
  { prop: 'submissionCount', label: '提交次数', width: 130, align: 'center', slot: 'submissionCount' }
]

const rankTypeLabel = computed(() => (rankType.value === 'weekly' ? '周榜' : '总榜'))

const topAcceptedCount = computed(() => {
  return rankingList.value[0]?.acceptedCount || 0
})

const averageAcceptedCount = computed(() => {
  if (!rankingList.value.length) return 0
  const total = rankingList.value.reduce((sum, item) => sum + (item.acceptedCount || 0), 0)
  return Math.round(total / rankingList.value.length)
})

const currentUserRank = computed(() => {
  if (!currentUserId.value) return ''
  const current = rankingList.value.find((item) => String(item.userId) === String(currentUserId.value))
  return current?.rank || ''
})

const rankingDescription = computed(() => {
  return `当前查看 ${rankTypeLabel.value}，共 ${rankingList.value.length} 位用户。`
})

const readCurrentUserId = () => {
  const keys = ['user', 'userInfo']
  for (const key of keys) {
    const userStr = localStorage.getItem(key) || sessionStorage.getItem(key)
    if (!userStr) continue
    try {
      const user = JSON.parse(userStr)
      currentUserId.value = user.id || user.userId || null
      if (currentUserId.value) return
    } catch {
      currentUserId.value = null
    }
  }
}

const loadRanking = async () => {
  loading.value = true
  try {
    rankingList.value = await ojApi.getRanking(rankType.value) || []
  } catch {
    rankingList.value = []
  } finally {
    loading.value = false
  }
}

const isCurrentUser = (row: RankingRecord) => {
  return Boolean(currentUserId.value) && String(row.userId) === String(currentUserId.value)
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-badge--gold'
  if (rank === 2) return 'rank-badge--silver'
  if (rank === 3) return 'rank-badge--bronze'
  return ''
}

const getAvatarFallback = (nickname?: string) => {
  return (nickname || '?').slice(0, 1)
}

readCurrentUserId()

onMounted(() => {
  loadRanking()
})
</script>

<style scoped>
.oj-ranking {
  min-height: calc(100vh - 68px);
}

.ranking-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.ranking-section :deep(.cn-section__body) {
  padding: 0;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 34px;
  height: 30px;
  padding: 0 var(--cn-space-2);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.rank-badge--gold {
  border-color: color-mix(in srgb, var(--cn-color-warning) 38%, var(--cn-color-border-subtle));
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.rank-badge--silver {
  border-color: color-mix(in srgb, var(--cn-color-info) 30%, var(--cn-color-border-subtle));
  background: var(--cn-color-info-soft);
  color: var(--cn-color-info);
}

.rank-badge--bronze {
  border-color: color-mix(in srgb, var(--cn-color-danger) 24%, var(--cn-color-border-subtle));
  background: var(--cn-color-danger-soft);
  color: var(--cn-color-danger);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
}

.user-avatar {
  flex-shrink: 0;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 74%, var(--cn-color-info));
  color: white;
  font-size: 14px;
  font-weight: 700;
}

.user-copy {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.user-name {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell.is-current .user-name {
  color: var(--cn-color-brand-primary);
}

.accepted-count {
  color: var(--cn-color-success);
  font-size: 16px;
  font-weight: 800;
}

.muted-count {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  font-weight: 650;
}

@media (max-width: 1024px) {
  .ranking-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .ranking-stats {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
