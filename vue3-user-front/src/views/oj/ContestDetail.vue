<template>
  <div class="contest-detail cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <section class="cn-learn-hero cn-wave-reveal">
        <div class="cn-learn-hero__content">
          <el-button text class="back-btn" @click="router.push('/oj/contests')">
            <el-icon><ArrowLeft /></el-icon>
            返回赛事中心
          </el-button>
          <span class="cn-learn-hero__eyebrow">Contest Detail</span>
          <h1 class="cn-learn-hero__title">{{ contest.title || '赛事详情' }}</h1>
          <p class="cn-learn-hero__desc">{{ contest.description || '暂无赛事介绍' }}</p>
        </div>
        <div class="cn-learn-hero__meta">
          <span class="cn-learn-chip">{{ getContestTypeLabel(contest.contestType) }}</span>
          <span class="cn-learn-chip">{{ getContestStatusLabel(contest.status) }}</span>
          <span class="cn-learn-chip">{{ contest.problemCount || 0 }} 题</span>
          <span class="cn-learn-chip">{{ contest.participantCount || 0 }} 人参赛</span>
          <div class="hero-actions">
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
          </div>
        </div>
      </section>

      <div class="summary-row cn-learn-reveal cn-learn-panel">
        <div class="summary-item">
          <el-icon><Calendar /></el-icon>
          <span class="summary-label">比赛时间</span>
          <span>{{ contest.startTime || '--' }} ~ {{ contest.endTime || '--' }}</span>
        </div>
        <div class="summary-item">
          <el-icon><List /></el-icon>
          <span class="summary-label">题目数量</span>
          <span>{{ problemRows.length }} 题</span>
        </div>
        <div class="summary-item">
          <el-icon><UserFilled /></el-icon>
          <span class="summary-label">参赛人数</span>
          <span>{{ contest.participantCount || 0 }} 人</span>
        </div>
        <div class="summary-item">
          <el-icon><Trophy /></el-icon>
          <span class="summary-label">当前排名数据</span>
          <span>{{ rankingRows.length }} 条</span>
        </div>
      </div>

      <div class="content-grid">
        <section class="cn-learn-panel cn-learn-reveal panel-block">
          <div class="panel-header">
            <h3>赛事题单</h3>
          </div>
          <el-table v-loading="problemsLoading || loading" :data="problemRows" style="width: 100%">
            <el-table-column label="#" type="index" width="60" />
            <el-table-column prop="id" label="题号" width="90" />
            <el-table-column prop="title" label="题目" min-width="220" />
            <el-table-column label="难度" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="dark" :type="getDifficultyTag(row.difficulty)">
                  {{ getDifficultyLabel(row.difficulty) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link @click="goProblem(row)">去做题</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !problemsLoading && problemRows.length === 0" description="赛事题单为空" :image-size="90" />
        </section>

        <section class="cn-learn-panel cn-learn-reveal panel-block">
          <div class="panel-header">
            <h3>实时榜单</h3>
            <el-button text @click="loadContestRanking" :loading="rankingLoading">刷新榜单</el-button>
          </div>
          <el-table
            v-loading="rankingLoading || loading"
            :data="rankingRows"
            style="width: 100%"
            :row-class-name="rankingRowClassName"
          >
            <el-table-column label="排名" width="70" align="center">
              <template #default="{ row }">
                <span v-if="row.rank === 1">🥇</span>
                <span v-else-if="row.rank === 2">🥈</span>
                <span v-else-if="row.rank === 3">🥉</span>
                <span v-else>{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="选手" min-width="160" />
            <el-table-column prop="solvedText" label="解题数" width="100" align="center" />
            <el-table-column prop="penaltyText" label="罚时" width="110" align="center" />
            <el-table-column prop="lastAcText" label="最后 AC 时间" min-width="180" />
          </el-table>
          <el-empty v-if="!loading && !rankingLoading && rankingRows.length === 0" description="暂无榜单数据" :image-size="90" />
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Calendar, List, Trophy, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ojApi } from '@/api/oj'
import {
  adaptContestItem,
  adaptContestRanking,
  getContestStatusLabel,
  getContestTypeLabel
} from '@/utils/oj-contest-adapter'
import { useRevealMotion } from '@/utils/reveal-motion'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
useRevealMotion('.contest-detail .cn-learn-reveal')

const loading = ref(false)
const problemsLoading = ref(false)
const rankingLoading = ref(false)
const joining = ref(false)
const problemRows = ref([])
const rankingRows = ref([])
const contest = ref(adaptContestItem({}))

const contestId = computed(() => Number(route.params.id))
const currentUserId = computed(() => {
  const id = userStore.userInfo?.id ?? userStore.userInfo?.userId
  return Number.isFinite(Number(id)) ? Number(id) : null
})

const canJoinContest = computed(() => {
  return contest.value.status === 1 || contest.value.status === 2
})

const getDifficultyTag = (difficulty) => {
  return ({ easy: 'success', medium: 'warning', hard: 'danger' }[difficulty] || 'info')
}

const getDifficultyLabel = (difficulty) => {
  return ({ easy: '简单', medium: '中等', hard: '困难' }[difficulty] || '未知')
}

const rankingRowClassName = ({ row }) => {
  return row.isCurrentUser ? 'current-user-row' : ''
}

const loadProblemRows = async (problemIds = []) => {
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
    rankingRows.value = adaptContestRanking(data, { currentUserId: currentUserId.value })
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
    contest.value = adaptContestItem(detail)
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
    const message = String(error?.message || '')
    if (/duplicate|已报名/i.test(message)) {
      ElMessage.info('你已报名该赛事')
      return
    }
  } finally {
    joining.value = false
  }
}

const goProblem = (problem) => {
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

.back-btn {
  margin-bottom: 8px;
  color: #fff;
}

.hero-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.summary-row {
  margin-top: 18px;
  padding: 16px 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4c6388;
  font-size: 13px;
}

.summary-label {
  color: #7b8faf;
}

.content-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.panel-block {
  padding: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--cn-text-primary);
}

:deep(.current-user-row) {
  background: #edf4ff !important;
}

@media (max-width: 1100px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .summary-row {
    grid-template-columns: 1fr;
  }

  .hero-actions {
    display: flex;
    flex-wrap: wrap;
  }
}
</style>
