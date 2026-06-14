<template>
  <CnPage class="contest-detail" surface="transparent" max-width="1440px">
    <CnPageHeader
      class="cn-learn-reveal"
      :title="contest.title || '赛事详情'"
      :description="contest.description || '暂无赛事介绍'"
      eyebrow="Contest Detail"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="contest.contestType === 'weekly' ? 'success' : 'warning'" size="sm" :dot="false">
          {{ getContestTypeLabel(contest.contestType) }}
        </CnStatusTag>
        <CnStatusTag :type="getContestStatusTone(contest.status)" size="sm">
          {{ getContestStatusLabel(contest.status) }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm">
          {{ problemRows.length }} 题
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">
          {{ contest.participantCount || 0 }} 人参赛
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj/contests')">
          <el-icon><ArrowLeft /></el-icon>
          返回赛事中心
        </el-button>
        <el-button
          type="success"
          plain
          :loading="joining"
          :disabled="!canJoinContest"
          @click="handleJoinContest"
        >
          {{ canJoinContest ? '报名赛事' : '不可报名' }}
        </el-button>
        <el-button type="primary" :disabled="problemRows.length === 0" @click="startContest">
          开始做题
        </el-button>
      </template>
    </CnPageHeader>

    <section class="summary-grid cn-learn-reveal" aria-label="赛事概览">
      <CnStatCard
        title="开始时间"
        :value="contest.startTime || '--'"
        description="赛事开放答题时间"
        tone="brand"
        trend="flat"
        :loading="loading"
        trend-text="Start"
      />
      <CnStatCard
        title="结束时间"
        :value="contest.endTime || '--'"
        description="赛事截止提交时间"
        tone="warning"
        trend="flat"
        :loading="loading"
        trend-text="End"
      />
      <CnStatCard
        title="题目数量"
        :value="problemRows.length"
        unit="题"
        description="当前赛事题单"
        tone="success"
        trend="flat"
        :loading="loading || problemsLoading"
        trend-text="题单"
      />
      <CnStatCard
        title="榜单记录"
        :value="rankingRows.length"
        unit="条"
        description="当前实时榜单数据"
        tone="info"
        trend="flat"
        :loading="loading || rankingLoading"
        trend-text="排名"
      />
    </section>

    <div class="content-grid">
      <CnSection
        class="contest-panel cn-learn-reveal"
        title="赛事题单"
        description="按题单顺序进入比赛题目。"
        surface="panel"
        divided
      >
        <CnDataTable
          :columns="problemColumns"
          :data="problemRows"
          :loading="problemsLoading || loading"
          empty-title="赛事题单为空"
          empty-description="当前赛事还没有配置题目。"
          empty-icon="OJ"
        >
          <template #difficulty="{ row }">
            <CnStatusTag :type="getDifficultyTone(row.difficulty)" size="sm" :dot="false">
              {{ getDifficultyLabel(row.difficulty) }}
            </CnStatusTag>
          </template>

          <template #actions="{ row }">
            <el-button type="primary" size="small" text @click="goProblem(row)">去做题</el-button>
          </template>
        </CnDataTable>
      </CnSection>

      <CnSection
        class="contest-panel cn-learn-reveal"
        title="实时榜单"
        description="按解题数、罚时和最后 AC 时间排序。"
        surface="panel"
        divided
      >
        <template #actions>
          <el-button text :loading="rankingLoading" @click="loadContestRanking">
            <el-icon><Refresh /></el-icon>
            刷新榜单
          </el-button>
        </template>

        <CnDataTable
          :columns="rankingColumns"
          :data="rankingRows"
          :loading="rankingLoading || loading"
          row-key="userId"
          empty-title="暂无榜单数据"
          empty-description="比赛开始并产生提交后，这里会展示实时排名。"
          empty-icon="TOP"
        >
          <template #rank="{ row }">
            <span class="rank-badge" :class="getRankClass(row.rank)">
              {{ row.rank }}
            </span>
          </template>

          <template #nickname="{ row }">
            <span class="contest-user" :class="{ 'is-current': row.isCurrentUser }">
              <span>{{ row.nickname }}</span>
              <CnStatusTag v-if="row.isCurrentUser" type="info" size="sm" :dot="false" subtle>我</CnStatusTag>
            </span>
          </template>

          <template #ratingChange="{ row }">
            <CnStatusTag :type="getRatingChangeTone(row.ratingChangeType)" size="sm" :dot="false" subtle>
              {{ row.ratingChangeText }}
            </CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnTableColumn, CnTone } from '@/design-system'
import { useUserStore } from '@/stores/user'
import { ojApi } from '@/api/oj'
import {
  adaptContestItem,
  adaptContestRanking,
  getContestStatusLabel,
  getContestStatusTag,
  getContestTypeLabel
} from '@/utils/oj-contest-adapter'
import { useRevealMotion } from '@/utils/reveal-motion'

interface ContestRecord extends Record<string, unknown> {
  id: number | string | null
  title: string
  description?: string
  contestType: 'weekly' | 'challenge'
  status: number
  startTime?: string
  endTime?: string
  problemCount: number
  participantCount: number
  problemIds: Array<number | string>
}

interface ContestProblemRow extends Record<string, unknown> {
  id: number | string
  title: string
  difficulty: string
}

interface ContestRankingRow extends Record<string, unknown> {
  userId: number
  nickname: string
  rank: number
  solvedText: string
  penaltyText: string
  performanceText: string
  ratingChangeText: string
  ratingChangeType: string
  ratingAfterText: string | number
  lastAcText: string
  isCurrentUser: boolean
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
useRevealMotion('.contest-detail .cn-learn-reveal')

const loading = ref(false)
const problemsLoading = ref(false)
const rankingLoading = ref(false)
const joining = ref(false)
const problemRows = ref<ContestProblemRow[]>([])
const rankingRows = ref<ContestRankingRow[]>([])
const contest = ref<ContestRecord>(adaptContestItem({}) as ContestRecord)

const contestId = computed(() => Number(route.params.id))

const currentUserId = computed(() => {
  const id = userStore.userInfo?.id ?? userStore.userInfo?.userId
  return Number.isFinite(Number(id)) ? Number(id) : null
})

const canJoinContest = computed(() => {
  return contest.value.status === 1 || contest.value.status === 2
})

const breadcrumbs = computed(() => [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '赛事中心', to: '/oj/contests' },
  { label: contest.value.title || '赛事详情' }
])

const problemColumns: CnTableColumn<ContestProblemRow>[] = [
  { type: 'index', label: '#', width: 64, align: 'center' },
  { prop: 'id', label: '题号', width: 90, align: 'center' },
  { prop: 'title', label: '题目', minWidth: 220, showOverflowTooltip: true },
  { prop: 'difficulty', label: '难度', width: 120, align: 'center', slot: 'difficulty' },
  { label: '操作', width: 110, align: 'center', slot: 'actions' }
]

const rankingColumns: CnTableColumn<ContestRankingRow>[] = [
  { prop: 'rank', label: '排名', width: 80, align: 'center', slot: 'rank' },
  { prop: 'nickname', label: '选手', minWidth: 160, slot: 'nickname', showOverflowTooltip: true },
  { prop: 'solvedText', label: '解题数', width: 100, align: 'center' },
  { prop: 'penaltyText', label: '罚时', width: 110, align: 'center' },
  { prop: 'performanceText', label: '表现分', width: 100, align: 'center' },
  { prop: 'ratingChangeText', label: '评分变化', width: 110, align: 'center', slot: 'ratingChange' },
  { prop: 'ratingAfterText', label: '预估评分', width: 100, align: 'center' },
  { prop: 'lastAcText', label: '最后 AC 时间', minWidth: 180 }
]

const getContestStatusTone = (status: number): CnTone => {
  const tagType = getContestStatusTag(status)
  if (['success', 'warning', 'danger', 'info'].includes(tagType)) return tagType as CnTone
  return 'neutral'
}

const getDifficultyTone = (difficulty: string): CnTone => {
  return ({ easy: 'success', medium: 'warning', hard: 'danger' }[difficulty] || 'info') as CnTone
}

const getDifficultyLabel = (difficulty: string) => {
  return ({ easy: '简单', medium: '中等', hard: '困难' }[difficulty] || '未知')
}

const getRatingChangeTone = (type: string): CnTone => {
  if (['success', 'warning', 'danger', 'info'].includes(type)) return type as CnTone
  return 'neutral'
}

const getRankClass = (rank: number) => {
  if (rank === 1) return 'rank-badge--gold'
  if (rank === 2) return 'rank-badge--silver'
  if (rank === 3) return 'rank-badge--bronze'
  return ''
}

const loadProblemRows = async (problemIds: Array<number | string> = []) => {
  if (!Array.isArray(problemIds) || problemIds.length === 0) {
    problemRows.value = []
    return
  }

  problemsLoading.value = true
  try {
    const items = await Promise.all(problemIds.map(async (problemId) => {
      try {
        const data = await ojApi.getProblemDetail(problemId)
        return {
          id: data?.id || problemId,
          title: data?.title || `题目 ${problemId}`,
          difficulty: data?.difficulty || ''
        }
      } catch {
        return {
          id: problemId,
          title: `题目 ${problemId}`,
          difficulty: ''
        }
      }
    }))
    problemRows.value = items
  } finally {
    problemsLoading.value = false
  }
}

const loadContestRanking = async () => {
  rankingLoading.value = true
  try {
    const data = await ojApi.getContestRanking(contestId.value)
    rankingRows.value = adaptContestRanking(data, { currentUserId: currentUserId.value }) as ContestRankingRow[]
  } catch {
    rankingRows.value = []
  } finally {
    rankingLoading.value = false
  }
}

const loadContestDetail = async () => {
  loading.value = true
  try {
    const detail = await ojApi.getContestDetail(contestId.value)
    contest.value = adaptContestItem(detail) as ContestRecord
    await Promise.all([
      loadProblemRows(contest.value.problemIds || []),
      loadContestRanking()
    ])
  } catch {
    ElMessage.error('赛事不存在或已下线')
    router.push('/oj/contests')
  } finally {
    loading.value = false
  }
}

const handleJoinContest = async () => {
  if (!canJoinContest.value) {
    ElMessage.warning('当前赛事不可报名')
    return
  }
  joining.value = true
  try {
    await ojApi.joinContest(contestId.value)
    ElMessage.success('报名成功')
    loadContestDetail()
  } catch (error) {
    const message = String((error as Error)?.message || '')
    if (/duplicate|已报名/i.test(message)) {
      ElMessage.info('你已报名该赛事')
      return
    }
  } finally {
    joining.value = false
  }
}

const goProblem = (problem: ContestProblemRow) => {
  router.push({
    path: `/oj/problem/${problem.id}`,
    query: { contestId: String(contestId.value) }
  })
}

const startContest = () => {
  const first = problemRows.value[0]
  if (!first) {
    ElMessage.warning('赛事题单为空')
    return
  }
  goProblem(first)
}

onMounted(() => {
  if (!Number.isFinite(contestId.value) || contestId.value <= 0) {
    router.push('/oj/contests')
    return
  }
  loadContestDetail()
})
</script>

<style scoped>
.contest-detail {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.15fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.contest-panel :deep(.cn-section__body) {
  padding: 0;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 28px;
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

.contest-user {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  max-width: 100%;
  min-width: 0;
}

.contest-user > span:first-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contest-user.is-current {
  color: var(--cn-color-brand-primary);
  font-weight: 700;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 680px) {
  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
