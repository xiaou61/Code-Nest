<template>
  <CnPage class="interview-index" surface="transparent" max-width="1440px" full-height>
    <CnPageHeader
      class="interview-page-header cn-learn-reveal"
      title="面试题库学习中枢"
      description="把刷题、收藏、复习和学习足迹放在同一工作台，围绕分类、题单和复习动作形成面试准备闭环。"
      eyebrow="Interview Studio"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">题单 {{ totalQuestionSets }}</CnStatusTag>
        <CnStatusTag type="success" size="sm">分类 {{ categoryList.length }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">{{ activeFilterText }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goToFavorites">
          <el-icon><Star /></el-icon>
          我的收藏
        </el-button>
        <el-button type="primary" @click="goToRandomQuestions">
          <el-icon><Refresh /></el-icon>
          随机抽题
        </el-button>
      </template>
    </CnPageHeader>

    <section class="interview-summary-grid cn-learn-reveal" aria-label="面试题库数据概览">
      <CnStatCard
        title="题单总数"
        :value="totalQuestionSets"
        unit="套"
        description="当前分类体系下的公开题单"
        tone="brand"
        trend="flat"
        :loading="interviewStore.categoriesLoading"
        trend-text="题库"
      />
      <CnStatCard
        title="分类数量"
        :value="categoryList.length"
        unit="个"
        description="按岗位与能力模块拆分题单"
        tone="success"
        trend="flat"
        :loading="interviewStore.categoriesLoading"
        trend-text="筛选"
      />
      <CnStatCard
        title="当前结果"
        :value="total"
        unit="项"
        description="当前筛选与搜索条件下的结果数"
        tone="info"
        trend="flat"
        :loading="loading"
        trend-text="结果"
      />
      <CnStatCard
        title="当前页码"
        :value="queryParams.page"
        description="分页浏览面试题单"
        tone="warning"
        trend="flat"
        trend-text="分页"
      />
    </section>

    <div class="interview-layout">
      <aside class="interview-sidebar" aria-label="面试题单筛选">
        <CnSection class="sidebar-section cn-learn-reveal" surface="panel" compact>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索题单或题目"
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button :icon="Search" aria-label="搜索题单" @click="handleSearch" />
            </template>
          </el-input>
        </CnSection>

        <CnSection
          class="sidebar-section cn-learn-reveal"
          title="分类筛选"
          description="按知识方向定位题单"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><Folder /></el-icon>
          </template>

          <div class="category-list" v-loading="interviewStore.categoriesLoading">
            <button
              class="category-item"
              :class="{ active: currentCategoryId === null }"
              type="button"
              @click="selectCategory(null)"
            >
              <span class="category-name">全部分类</span>
              <CnStatusTag type="neutral" size="sm" :dot="false">{{ totalQuestionSets }}</CnStatusTag>
            </button>
            <button
              v-for="category in categoryList"
              :key="category.id"
              class="category-item"
              :class="{ active: currentCategoryId === category.id }"
              type="button"
              @click="selectCategory(category.id)"
            >
              <span class="category-name">{{ category.name }}</span>
              <CnStatusTag type="neutral" size="sm" :dot="false">
                {{ category.questionSetCount || 0 }}
              </CnStatusTag>
            </button>
          </div>
        </CnSection>

        <CnSection
          class="sidebar-section cn-learn-reveal"
          title="快捷入口"
          description="进入高频面试学习动作"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><Lightning /></el-icon>
          </template>

          <div class="action-buttons">
            <button class="action-btn" type="button" @click="goToRandomQuestions">
              <el-icon class="action-icon"><Refresh /></el-icon>
              <span>随机抽题</span>
            </button>
            <button class="action-btn" type="button" @click="goToFavorites">
              <el-icon class="action-icon"><Star /></el-icon>
              <span>我的收藏</span>
            </button>
            <button class="action-btn" type="button" @click="goToReview">
              <el-icon class="action-icon"><Clock /></el-icon>
              <span>复习中心</span>
            </button>
          </div>
        </CnSection>

        <ReviewReminderCard class="review-reminder-card cn-learn-reveal" />
      </aside>

      <main class="interview-main">
        <LearningHeatmap class="heatmap-section cn-learn-reveal" />

        <CnSection
          class="question-set-section cn-learn-reveal"
          title="题单列表"
          :description="questionSetSectionDescription"
          surface="panel"
          divided
        >
          <template #actions>
            <CnStatusTag v-if="total > 0" type="brand" size="sm">{{ total }} 个题单</CnStatusTag>
            <el-select v-model="sortType" placeholder="排序方式" class="sort-select" @change="handleSortChange">
              <el-option label="最新发布" value="newest" />
              <el-option label="最多浏览" value="views" />
              <el-option label="最多收藏" value="favorites" />
            </el-select>
          </template>

          <div v-loading="loading" class="question-sets-grid">
            <button
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              class="question-set-card cn-learn-reveal"
              type="button"
              @click="goToQuestionSet(questionSet)"
            >
              <span class="card-accent" aria-hidden="true" />
              <div class="card-header">
                <h3 class="card-title">{{ questionSet.title }}</h3>
                <CnStatusTag :type="questionSet.type === 1 ? 'success' : 'info'" size="sm" :dot="false">
                  {{ questionSet.type === 1 ? '官方' : '用户' }}
                </CnStatusTag>
              </div>

              <p class="card-description">{{ questionSet.description || '暂无描述' }}</p>

              <div class="card-tags">
                <CnStatusTag v-if="questionSet.categoryName" type="warning" size="sm" :dot="false" subtle>
                  {{ questionSet.categoryName }}
                </CnStatusTag>
                <CnStatusTag v-else type="neutral" size="sm" :dot="false" subtle>
                  未分类
                </CnStatusTag>
              </div>

              <div class="card-stats">
                <span class="stat-item">
                  <el-icon><Edit /></el-icon>
                  {{ questionSet.questionCount || 0 }} 题
                </span>
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ questionSet.viewCount || 0 }}
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ questionSet.favoriteCount || 0 }}
                </span>
              </div>

              <div class="card-footer">
                <span class="card-author" v-if="questionSet.creatorName">
                  <el-avatar :size="22" :src="questionSet.creatorAvatar">
                    {{ questionSet.creatorName?.charAt(0) }}
                  </el-avatar>
                  <span>{{ questionSet.creatorName }}</span>
                </span>
                <span v-else class="card-author muted-author">Code Nest</span>

                <span class="card-date">{{ formatDate(questionSet.createTime) }}</span>
              </div>
            </button>
          </div>

          <CnEmptyState
            v-if="!loading && questionSetList.length === 0"
            title="暂无题单数据"
            description="调整关键词、分类或排序后重新筛选。"
            icon="IV"
            surface="transparent"
          >
            <template #actions>
              <el-button plain @click="resetFilters">重置筛选</el-button>
            </template>
          </CnEmptyState>

          <div v-if="total > 0" class="pagination-wrapper cn-learn-reveal">
            <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :page-sizes="[12, 24, 36, 48]"
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
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Clock,
  Edit,
  Folder,
  Lightning,
  Refresh,
  Search,
  Star,
  View
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { useInterviewStore } from '@/stores/interview'
import { useRevealMotion } from '@/utils/reveal-motion'
import ReviewReminderCard from './components/ReviewReminderCard.vue'
import LearningHeatmap from './components/LearningHeatmap.vue'

interface InterviewCategory {
  id: number
  name: string
  questionSetCount?: number
}

interface InterviewQuestionSet {
  id: number | string
  title: string
  description?: string
  questionCount?: number
  viewCount?: number
  favoriteCount?: number
  type?: number
  categoryName?: string
  creatorName?: string
  creatorAvatar?: string
  createTime?: string
  originalQuestion?: {
    id: number | string
  }
}

const router = useRouter()
const interviewStore = useInterviewStore()
const { refreshReveal } = useRevealMotion('.interview-index .cn-learn-reveal')

const searchKeyword = ref('')
const currentCategoryId = ref<number | null>(null)
const sortType = ref('newest')

const loading = computed(() => interviewStore.questionSetsLoading)
const categoryList = computed<InterviewCategory[]>(() => interviewStore.categories)
const questionSetList = computed<InterviewQuestionSet[]>(() => interviewStore.questionSets)
const total = computed(() => interviewStore.questionSetsTotal)

const totalQuestionSets = computed(() => {
  return categoryList.value.reduce((sum, category) => sum + (category.questionSetCount || 0), 0)
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '面试题库' }
]

const queryParams = reactive({
  keyword: '',
  page: 1,
  size: 12
})

const selectedCategoryName = computed(() => {
  if (currentCategoryId.value === null) return '全部分类'
  return categoryList.value.find(category => category.id === currentCategoryId.value)?.name || '当前分类'
})

const activeFilterText = computed(() => {
  const keyword = queryParams.keyword ? `关键词 ${queryParams.keyword}` : '无关键词'
  return `${selectedCategoryName.value} · ${keyword}`
})

const questionSetSectionDescription = computed(() => {
  return `当前页 ${queryParams.page}，每页 ${queryParams.size} 个，筛选条件：${activeFilterText.value}`
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '--'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const fetchCategories = async () => {
  try {
    await interviewStore.fetchCategories()
  } catch (error) {
    ElMessage.error('获取分类列表失败')
  }
}

const fetchQuestionSets = async (forceRefresh = false) => {
  try {
    if (queryParams.keyword) {
      await interviewStore.searchQuestions(queryParams, { forceRefresh })
    } else {
      await interviewStore.fetchPublicQuestionSets({
        categoryId: currentCategoryId.value,
        page: queryParams.page,
        size: queryParams.size
      }, { forceRefresh })
    }
  } catch (error) {
    ElMessage.error('获取题单列表失败')
    return
  }

  await nextTick()
  refreshReveal()
}

watch(
  () => questionSetList.value.length,
  async () => {
    await nextTick()
    refreshReveal()
  }
)

const selectCategory = (categoryId: number | null) => {
  currentCategoryId.value = categoryId
  queryParams.page = 1
  searchKeyword.value = ''
  queryParams.keyword = ''
  fetchQuestionSets(true)
}

const handleSearch = () => {
  queryParams.keyword = searchKeyword.value.trim()
  queryParams.page = 1
  fetchQuestionSets(true)
}

const handleSizeChange = (size: number) => {
  queryParams.size = size
  queryParams.page = 1
  fetchQuestionSets(true)
}

const handleCurrentChange = (page: number) => {
  queryParams.page = page
  fetchQuestionSets(true)
}

const handleSortChange = (sort: string) => {
  sortType.value = sort
  queryParams.page = 1
  fetchQuestionSets(true)
}

const resetFilters = () => {
  currentCategoryId.value = null
  searchKeyword.value = ''
  queryParams.keyword = ''
  queryParams.page = 1
  fetchQuestionSets(true)
}

const goToQuestionSet = (questionSet: InterviewQuestionSet) => {
  if (
    typeof questionSet.id === 'string' &&
    questionSet.id.startsWith('q-') &&
    questionSet.originalQuestion
  ) {
    router.push(`/interview/questions/${questionSet.originalQuestion.id}`)
    return
  }

  router.push(`/interview/question-sets/${questionSet.id}`)
}

const goToFavorites = () => {
  router.push('/interview/favorites')
}

const goToReview = () => {
  router.push('/interview/review')
}

const goToRandomQuestions = () => {
  router.push('/interview/random')
}

onMounted(async () => {
  await fetchCategories()
  await fetchQuestionSets()
})
</script>

<style scoped>
.interview-index {
  min-height: calc(100vh - 68px);
}

.interview-page-header {
  margin-bottom: var(--cn-space-1);
}

.interview-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.interview-layout {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-5);
  min-width: 0;
}

.interview-sidebar {
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  gap: var(--cn-space-4);
  width: 286px;
  max-height: calc(100vh - 96px);
  min-width: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.sidebar-section {
  min-width: 0;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  max-height: 310px;
  overflow-y: auto;
}

.category-item,
.action-btn,
.question-set-card {
  font: inherit;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  width: 100%;
  min-height: 36px;
  padding: 0 var(--cn-space-3);
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  background: transparent;
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.category-item:hover {
  border-color: var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
}

.category-item.active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-weight: 650;
}

.category-name {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  width: 100%;
  min-height: 38px;
  padding: 0 var(--cn-space-3);
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  background: transparent;
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.action-btn:hover {
  border-color: var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
  transform: translateX(2px);
}

.action-icon {
  color: var(--cn-color-brand-primary);
  font-size: 16px;
}

.review-reminder-card {
  min-width: 0;
}

.interview-main {
  flex: 1;
  min-width: 0;
}

.heatmap-section {
  margin-bottom: var(--cn-space-4);
}

.sort-select {
  width: 148px;
}

.question-set-section :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-4);
}

.question-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cn-space-4);
  min-height: 200px;
}

.question-set-card {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 218px;
  flex-direction: column;
  overflow: hidden;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.question-set-card:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateY(-3px);
}

.card-accent {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
  background: var(--cn-color-brand-primary);
  opacity: 0;
  transition: opacity var(--cn-motion-base) var(--cn-ease-out);
}

.question-set-card:hover .card-accent {
  opacity: 1;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
}

.card-title {
  display: -webkit-box;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-description {
  display: -webkit-box;
  min-height: 42px;
  margin: var(--cn-space-3) 0 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.card-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  margin-top: auto;
  padding: var(--cn-space-4) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  white-space: nowrap;
}

.stat-item .el-icon {
  color: var(--cn-color-brand-primary);
  font-size: 14px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.card-author {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
}

.card-author span,
.card-date {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.muted-author {
  color: var(--cn-color-text-tertiary);
  font-weight: 650;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: var(--cn-space-1);
}

@media (max-width: 1180px) {
  .interview-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .interview-layout {
    flex-direction: column;
  }

  .interview-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .review-reminder-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .interview-summary-grid,
  .interview-sidebar,
  .question-sets-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .sort-select {
    width: 100%;
  }

  .card-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
