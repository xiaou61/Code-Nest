<template>
  <CnPage class="oj-contests" surface="transparent" max-width="1360px">
    <CnPageHeader
      class="cn-learn-reveal"
      title="赛事中心"
      description="集中查看周赛与挑战赛，报名后即可进入比赛题单并参与实时排名。"
      eyebrow="Contest Hub"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          总赛事 {{ total }}
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          进行中 {{ runningCount }}
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">
          当前页 {{ queryForm.pageNum }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj')">
          <el-icon><ArrowLeft /></el-icon>
          返回题目列表
        </el-button>
        <el-button type="primary" :loading="loading" @click="loadContests">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <section class="contest-stats cn-learn-reveal" aria-label="赛事数据概览">
      <CnStatCard
        title="赛事总数"
        :value="total"
        unit="场"
        description="当前筛选条件下的赛事"
        tone="brand"
        trend="flat"
        :loading="loading"
        trend-text="分页"
      />
      <CnStatCard
        title="进行中"
        :value="runningCount"
        unit="场"
        description="当前页可立即参与的赛事"
        tone="success"
        trend="flat"
        :loading="loading"
        trend-text="Live"
      />
      <CnStatCard
        title="可报名"
        :value="joinableCount"
        unit="场"
        description="即将开始或进行中的赛事"
        tone="warning"
        trend="flat"
        :loading="loading"
        trend-text="报名"
      />
      <CnStatCard
        title="当前页"
        :value="queryForm.pageNum"
        description="每页展示赛事卡片"
        tone="info"
        trend="flat"
        trend-text="浏览"
      />
    </section>

    <div class="contests-layout">
      <aside class="contest-sidebar">
        <CnSection
          class="cn-learn-reveal"
          title="筛选条件"
          description="按标题、类型和状态定位赛事"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><Search /></el-icon>
          </template>
          <CnFilterForm
            v-model="filterModel"
            :fields="filterFields"
            :columns="1"
            :loading="loading"
            @search="handleSearch"
            @reset="handleReset"
          />
        </CnSection>
      </aside>

      <main class="contest-main">
        <CnSection
          class="contest-list-section cn-learn-reveal"
          title="赛事列表"
          :description="listDescription"
          surface="panel"
          divided
        >
          <template #actions>
            <CnStatusTag v-if="contestList.length > 0" type="neutral" size="sm">
              每页 {{ queryForm.pageSize }} 场
            </CnStatusTag>
          </template>

          <div class="contest-list" v-loading="loading">
            <div v-if="contestList.length > 0" class="contest-grid">
              <article
                v-for="contest in contestList"
                :key="contest.id"
                class="contest-card cn-learn-reveal"
              >
                <div class="contest-card__header">
                  <CnStatusTag :type="contest.contestType === 'weekly' ? 'success' : 'warning'" size="sm" :dot="false">
                    {{ getContestTypeLabel(contest.contestType) }}
                  </CnStatusTag>
                  <CnStatusTag :type="getContestStatusTone(contest.status)" size="sm">
                    {{ getContestStatusLabel(contest.status) }}
                  </CnStatusTag>
                </div>

                <h3 class="contest-card__title">{{ contest.title }}</h3>
                <p class="contest-card__desc">{{ contest.description || '暂无赛事描述' }}</p>

                <div class="contest-card__meta">
                  <span class="meta-item">
                    <el-icon><Calendar /></el-icon>
                    {{ contest.startTime || '--' }} ~ {{ contest.endTime || '--' }}
                  </span>
                  <span class="meta-item">
                    <el-icon><List /></el-icon>
                    {{ contest.problemCount }} 题
                  </span>
                  <span class="meta-item">
                    <el-icon><UserFilled /></el-icon>
                    {{ contest.participantCount }} 人
                  </span>
                </div>

                <div class="contest-card__actions">
                  <el-button plain @click="openContestDetail(contest)">
                    <el-icon><ArrowRight /></el-icon>
                    查看详情
                  </el-button>
                  <el-button
                    type="success"
                    :loading="joiningId === contest.id"
                    :disabled="!canJoin(contest)"
                    @click="handleJoin(contest)"
                  >
                    {{ canJoin(contest) ? '立即报名' : '不可报名' }}
                  </el-button>
                </div>
              </article>
            </div>

            <CnEmptyState
              v-else-if="!loading"
              title="暂无赛事数据"
              description="调整筛选条件后重新搜索，或稍后刷新赛事列表。"
              icon="OJ"
              surface="transparent"
              size="lg"
            >
              <template #actions>
                <el-button plain @click="handleReset">重置筛选</el-button>
              </template>
            </CnEmptyState>
          </div>

          <div v-if="total > 0" class="pagination-wrapper">
            <el-pagination
              v-model:current-page="queryForm.pageNum"
              v-model:page-size="queryForm.pageSize"
              :page-sizes="[6, 12, 18]"
              :total="total"
              layout="total, sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </CnSection>
      </main>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Calendar, List, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnFilterField, CnTone } from '@/design-system'
import { ojApi } from '@/api/oj'
import {
  adaptContestList,
  getContestStatusLabel,
  getContestStatusTag,
  getContestTypeLabel
} from '@/utils/oj-contest-adapter'
import { useRevealMotion } from '@/utils/reveal-motion'

interface ContestRecord extends Record<string, unknown> {
  id: number | string
  title: string
  description?: string
  contestType: 'weekly' | 'challenge'
  status: number
  startTime?: string
  endTime?: string
  problemCount: number
  participantCount: number
}

const router = useRouter()
useRevealMotion('.oj-contests .cn-learn-reveal')

const loading = ref(false)
const total = ref(0)
const contestList = ref<ContestRecord[]>([])
const joiningId = ref<number | string | null>(null)

const queryForm = reactive({
  pageNum: 1,
  pageSize: 12,
  keyword: '',
  contestType: '',
  status: null as number | null
})

const filterModel = ref<Record<string, unknown>>({
  keyword: '',
  contestType: '',
  status: null
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题', to: '/oj' },
  { label: '赛事中心' }
]

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '赛事标题', type: 'input', placeholder: '搜索赛事标题' },
  {
    prop: 'contestType',
    label: '赛事类型',
    type: 'select',
    placeholder: '全部类型',
    options: [
      { label: '周赛', value: 'weekly' },
      { label: '挑战赛', value: 'challenge' }
    ]
  },
  {
    prop: 'status',
    label: '赛事状态',
    type: 'select',
    placeholder: '全部状态',
    options: [
      { label: '即将开始', value: 1 },
      { label: '进行中', value: 2 },
      { label: '已结束', value: 3 }
    ]
  }
]

const runningCount = computed(() => contestList.value.filter((item) => item.status === 2).length)

const joinableCount = computed(() => contestList.value.filter((item) => canJoin(item)).length)

const listDescription = computed(() => {
  const keyword = queryForm.keyword ? `关键词“${queryForm.keyword}”` : '无关键词'
  return `当前第 ${queryForm.pageNum} 页，每页 ${queryForm.pageSize} 场，筛选条件：${keyword}。`
})

const getContestStatusTone = (status: number): CnTone => {
  const tagType = getContestStatusTag(status)
  if (['success', 'warning', 'danger', 'info'].includes(tagType)) return tagType as CnTone
  return 'neutral'
}

const loadContests = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: queryForm.pageNum,
      pageSize: queryForm.pageSize
    }
    if (queryForm.keyword) params.keyword = queryForm.keyword
    if (queryForm.contestType) params.contestType = queryForm.contestType
    if (queryForm.status != null) params.status = queryForm.status

    const data = await ojApi.getContestList(params)
    const adapted = adaptContestList(data)
    contestList.value = adapted.records as ContestRecord[]
    total.value = adapted.total
  } catch {
    contestList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const syncQueryFromFilters = () => {
  queryForm.keyword = String(filterModel.value.keyword || '').trim()
  queryForm.contestType = String(filterModel.value.contestType || '')
  queryForm.status = filterModel.value.status == null || filterModel.value.status === ''
    ? null
    : Number(filterModel.value.status)
}

const handleSearch = () => {
  syncQueryFromFilters()
  queryForm.pageNum = 1
  loadContests()
}

const handleReset = () => {
  filterModel.value = {
    keyword: '',
    contestType: '',
    status: null
  }
  queryForm.keyword = ''
  queryForm.contestType = ''
  queryForm.status = null
  queryForm.pageNum = 1
  loadContests()
}

const handleSizeChange = () => {
  queryForm.pageNum = 1
  loadContests()
}

const handleCurrentChange = () => {
  loadContests()
}

const openContestDetail = (contest: ContestRecord) => {
  router.push(`/oj/contests/${contest.id}`)
}

const canJoin = (contest: ContestRecord) => {
  return contest.status === 1 || contest.status === 2
}

const handleJoin = async (contest: ContestRecord) => {
  if (!canJoin(contest)) {
    ElMessage.warning('当前赛事不可报名')
    return
  }
  joiningId.value = contest.id
  try {
    await ojApi.joinContest(contest.id)
    ElMessage.success('报名成功')
    loadContests()
  } catch (error) {
    const message = String((error as Error)?.message || '')
    if (/duplicate|已报名/i.test(message)) {
      ElMessage.info('你已报名该赛事')
      return
    }
  } finally {
    joiningId.value = null
  }
}

onMounted(() => {
  loadContests()
})
</script>

<style scoped>
.oj-contests {
  min-height: calc(100vh - 68px);
}

.contest-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.contests-layout {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-5);
  min-width: 0;
}

.contest-sidebar {
  width: 300px;
  flex-shrink: 0;
}

.contest-main {
  flex: 1;
  min-width: 0;
}

.contest-list-section :deep(.cn-section__body) {
  padding: 0;
}

.contest-list {
  min-height: 360px;
  padding: var(--cn-space-5);
}

.contest-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.contest-card {
  display: flex;
  min-width: 0;
  min-height: 280px;
  flex-direction: column;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.contest-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-card-border));
  box-shadow: var(--cn-shadow-lg);
}

.contest-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.contest-card__title {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.contest-card__desc {
  min-height: 44px;
  margin: 0 0 var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.contest-card__meta {
  display: grid;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-4);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.contest-card__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  margin-top: auto;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

@media (max-width: 1180px) {
  .contest-stats,
  .contest-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .contests-layout {
    flex-direction: column;
  }

  .contest-sidebar {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .contest-stats,
  .contest-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .contest-card__actions,
  .pagination-wrapper {
    justify-content: flex-start;
  }
}
</style>
