<template>
  <CnPage class="review-page" max-width="1280px" full-height>
    <CnPageHeader
      title="智能复习"
      description="按逾期、今日和本周维度查看待复习题目，继续巩固已经学习过的面试题。"
      eyebrow="INTERVIEW REVIEW"
      :breadcrumbs="[{ label: '面试题库', to: '/interview' }, { label: '智能复习' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ currentFilterLabel }}</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>{{ reviewList.length }} 道待复习</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Back" @click="goBack">返回题库</el-button>
        <el-select v-model="filterType" class="filter-select" placeholder="筛选范围">
          <el-option label="全部" value="all" />
          <el-option label="已逾期" value="overdue" />
          <el-option label="今日" value="today" />
          <el-option label="本周" value="week" />
        </el-select>
      </template>
    </CnPageHeader>

    <section class="stats-grid" aria-label="复习统计">
      <CnStatCard
        title="已逾期"
        :value="reviewStats.overdueCount || 0"
        unit="题"
        description="需要优先回顾的题目"
        tone="danger"
        trend="flat"
        trend-text="优先"
      />
      <CnStatCard
        title="今日待复习"
        :value="reviewStats.todayCount || 0"
        unit="题"
        description="今天计划复习的题目"
        tone="warning"
        trend="flat"
        trend-text="今日"
      />
      <CnStatCard
        title="本周待复习"
        :value="reviewStats.weekCount || 0"
        unit="题"
        description="本周复习窗口内的题目"
        tone="success"
        trend="flat"
        trend-text="本周"
      />
      <CnStatCard
        title="已学习"
        :value="reviewStats.totalLearned || 0"
        unit="题"
        description="已纳入复习体系的题目"
        tone="brand"
        trend="flat"
        trend-text="累计"
      />
    </section>

    <div class="review-layout">
      <CnSection class="review-list-section" title="复习题目" :description="reviewListDescription" divided>
        <template #actions>
          <CnStatusTag type="neutral" size="sm" subtle>{{ currentFilterLabel }}</CnStatusTag>
        </template>

        <div v-loading="loading" class="review-list">
          <button
            v-for="item in reviewList"
            :key="item.questionId"
            class="review-item"
            type="button"
            @click="goToQuestion(item)"
          >
            <span class="review-item-main">
              <span class="question-title">{{ item.questionTitle }}</span>
              <span class="question-meta">
                <CnStatusTag type="info" size="sm" subtle>{{ item.questionSetTitle || '未知题单' }}</CnStatusTag>
                <CnStatusTag :type="getMasteryTone(item.masteryLevel)" size="sm">
                  {{ getMasteryText(item.masteryLevel) }}
                </CnStatusTag>
                <span class="review-count">已复习 {{ item.reviewCount || 0 }} 次</span>
              </span>
            </span>

            <span class="review-item-extra">
              <CnStatusTag v-if="item.overdueDays > 0" type="danger" size="sm">
                逾期 {{ item.overdueDays }} 天
              </CnStatusTag>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </span>
          </button>

          <CnEmptyState
            v-if="!loading && reviewList.length === 0"
            title="暂无待复习题目"
            description="继续学习新题目后，这里会根据掌握度生成复习列表。"
            icon="RV"
            surface="transparent"
          >
            <template #actions>
              <el-button type="primary" @click="goBack">返回题库</el-button>
            </template>
          </CnEmptyState>
        </div>
      </CnSection>

      <CnSection class="mastery-section" title="掌握度分布" description="按不会、模糊、熟悉和已掌握拆分当前学习题目。" divided>
        <div class="distribution-bars">
          <div
            v-for="item in masteryDistribution"
            :key="item.level"
            class="bar-item"
          >
            <div class="bar-label">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }} 题</strong>
            </div>
            <el-progress
              :percentage="getPercentage(item.count)"
              :stroke-width="12"
              :color="item.color"
              :show-text="false"
            />
          </div>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Back } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import { interviewApi } from '@/api/interview'

type ReviewFilter = 'all' | 'overdue' | 'today' | 'week'

interface ReviewStats {
  overdueCount: number
  todayCount: number
  weekCount: number
  totalLearned: number
  level1Count: number
  level2Count: number
  level3Count: number
  level4Count: number
}

interface ReviewItem {
  questionId: number
  questionSetId: number
  questionTitle: string
  questionSetTitle?: string
  masteryLevel?: number
  reviewCount?: number
  overdueDays?: number
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const filterType = ref<ReviewFilter>('all')
const reviewList = ref<ReviewItem[]>([])
const reviewStats = ref<ReviewStats>({
  overdueCount: 0,
  todayCount: 0,
  weekCount: 0,
  totalLearned: 0,
  level1Count: 0,
  level2Count: 0,
  level3Count: 0,
  level4Count: 0
})

const filterLabels: Record<ReviewFilter, string> = {
  all: '全部',
  overdue: '已逾期',
  today: '今日',
  week: '本周'
}

const currentFilterLabel = computed(() => filterLabels[filterType.value])

const reviewListDescription = computed(() => {
  if (reviewList.value.length === 0) return '当前筛选范围下没有待复习题目。'
  return `当前筛选范围下共有 ${reviewList.value.length} 道题目。`
})

const masteryDistribution = computed(() => [
  {
    level: 1,
    label: '不会',
    count: reviewStats.value.level1Count || 0,
    color: 'var(--cn-color-danger)'
  },
  {
    level: 2,
    label: '模糊',
    count: reviewStats.value.level2Count || 0,
    color: 'var(--cn-color-warning)'
  },
  {
    level: 3,
    label: '熟悉',
    count: reviewStats.value.level3Count || 0,
    color: 'var(--cn-color-info)'
  },
  {
    level: 4,
    label: '已掌握',
    count: reviewStats.value.level4Count || 0,
    color: 'var(--cn-color-success)'
  }
])

watch(filterType, () => {
  fetchReviewList()
})

const isReviewFilter = (value: unknown): value is ReviewFilter => {
  return typeof value === 'string' && ['all', 'overdue', 'today', 'week'].includes(value)
}

const fetchData = async () => {
  await Promise.all([
    fetchReviewStats(),
    fetchReviewList()
  ])
}

const fetchReviewStats = async () => {
  try {
    const res = await interviewApi.getReviewStats()
    if (res) {
      reviewStats.value = res
    }
  } catch (error) {
    console.error('获取复习统计失败', error)
  }
}

const fetchReviewList = async () => {
  loading.value = true
  try {
    const res = await interviewApi.getReviewList(filterType.value)
    reviewList.value = res || []
  } catch (error) {
    console.error('获取复习列表失败', error)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/interview')
}

const goToQuestion = (item: ReviewItem) => {
  router.push(`/interview/questions/${item.questionSetId}/${item.questionId}`)
}

const getMasteryText = (level?: number) => {
  const texts: Record<number, string> = {
    1: '不会',
    2: '模糊',
    3: '熟悉',
    4: '已掌握'
  }
  return level ? texts[level] || '未知' : '未知'
}

const getMasteryTone = (level?: number): CnTone => {
  const tones: Record<number, CnTone> = {
    1: 'danger',
    2: 'warning',
    3: 'info',
    4: 'success'
  }
  return level ? tones[level] || 'neutral' : 'neutral'
}

const getPercentage = (count: number) => {
  const total = reviewStats.value.totalLearned || 1
  return Math.round((count || 0) / total * 100)
}

onMounted(() => {
  const typeParam = route.query.type
  filterType.value = isReviewFilter(typeParam) ? typeParam : 'all'
  fetchData()
})
</script>

<style scoped>
.review-page {
  min-width: 0;
}

.filter-select {
  width: 132px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.review-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: var(--cn-space-5);
  align-items: start;
}

.review-list-section,
.mastery-section {
  min-width: 0;
}

.review-list {
  display: grid;
  gap: var(--cn-space-3);
  min-height: 300px;
}

.review-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.review-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateX(4px);
}

.review-item-main {
  display: grid;
  flex: 1;
  gap: var(--cn-space-2);
  min-width: 0;
}

.question-title {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-meta,
.review-item-extra {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.review-count {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.arrow-icon {
  color: var(--cn-color-text-tertiary);
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.review-item:hover .arrow-icon {
  color: var(--cn-color-brand-primary);
  transform: translateX(2px);
}

.distribution-bars {
  display: grid;
  gap: var(--cn-space-4);
}

.bar-item {
  display: grid;
  gap: var(--cn-space-2);
}

.bar-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.bar-label strong {
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .filter-select {
    width: 100%;
  }

  .review-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .question-title {
    white-space: normal;
  }
}
</style>
