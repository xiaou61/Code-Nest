<template>
  <CnPage class="question-set-detail-page" max-width="1440px" full-height>
    <CnPageHeader
      :title="questionSet.title || '题单详情'"
      :description="questionSet.description || '暂无描述'"
      eyebrow="INTERVIEW SET"
      :breadcrumbs="[{ label: '面试题库', to: '/interview' }, { label: questionSet.title || '题单详情' }]"
    >
      <template #meta>
        <CnStatusTag :type="questionSet.type === 1 ? 'success' : 'info'" size="sm">
          {{ questionSet.type === 1 ? '官方题单' : '用户题单' }}
        </CnStatusTag>
        <CnStatusTag v-if="questionSet.categoryName" type="warning" size="sm" subtle>
          {{ questionSet.categoryName }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>{{ questionList.length }} 道题目</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Back" @click="goBack">返回题库</el-button>
        <el-button
          :type="isFavorited ? 'danger' : 'default'"
          :icon="Star"
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          {{ isFavorited ? '取消收藏' : '收藏题单' }}
        </el-button>
        <el-button type="primary" :icon="VideoPlay" :disabled="questionList.length === 0" @click="startLearning">
          开始学习
        </el-button>
      </template>
    </CnPageHeader>

    <section class="set-summary-grid" aria-label="题单概览">
      <CnStatCard
        title="题目数量"
        :value="questionSet.questionCount || questionList.length || 0"
        unit="题"
        description="当前题单收录的面试题"
        tone="brand"
        trend="flat"
        :loading="loading"
        trend-text="题单"
      />
      <CnStatCard
        title="浏览次数"
        :value="questionSet.viewCount || 0"
        description="题单累计访问量"
        tone="info"
        trend="flat"
        :loading="loading"
        trend-text="热度"
      />
      <CnStatCard
        title="收藏人数"
        :value="questionSet.favoriteCount || 0"
        description="收藏该题单的用户数量"
        tone="warning"
        trend="flat"
        :loading="loading"
        trend-text="收藏"
      />
      <CnStatCard
        title="学习进度"
        :value="`${learnedCount}/${questionList.length}`"
        description="根据已学习题目统计"
        tone="success"
        trend="flat"
        :loading="questionsLoading"
        trend-text="进度"
      />
    </section>

    <div class="detail-layout">
      <aside class="set-sidebar">
        <CnSection title="学习进度" description="从第一题开始学习，或直接进入列表中的任意题目。" divided compact>
          <div class="progress-panel">
            <div class="progress-header">
              <span>完成度</span>
              <strong>{{ progressPercent }}%</strong>
            </div>
            <el-progress :percentage="progressPercent" :stroke-width="10" :color="progressColor" />
            <p class="progress-copy">已学习 {{ learnedCount }} 道，剩余 {{ remainingCount }} 道。</p>
          </div>

          <div class="sidebar-actions">
            <el-button type="primary" :icon="VideoPlay" :disabled="questionList.length === 0" @click="startLearning">
              开始学习
            </el-button>
            <el-button :icon="Star" :loading="favoriteLoading" @click="toggleFavorite">
              {{ isFavorited ? '取消收藏' : '收藏题单' }}
            </el-button>
          </div>
        </CnSection>

        <CnSection title="题单信息" surface="plain" compact>
          <div class="set-info-list">
            <div class="info-row">
              <span>题单类型</span>
              <strong>{{ questionSet.type === 1 ? '官方题单' : '用户题单' }}</strong>
            </div>
            <div class="info-row">
              <span>分类</span>
              <strong>{{ questionSet.categoryName || '未分类' }}</strong>
            </div>
            <div class="info-row">
              <span>题目数量</span>
              <strong>{{ questionSet.questionCount || questionList.length || 0 }}</strong>
            </div>
          </div>
        </CnSection>
      </aside>

      <main class="questions-main">
        <CnSection title="题目列表" :description="questionListDescription" divided>
          <template #actions>
            <el-input v-model="searchKeyword" class="search-input" placeholder="搜索题目" clearable>
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </template>

          <div v-loading="questionsLoading" class="questions-list">
            <button
              v-for="(question, index) in filteredQuestions"
              :key="question.id"
              class="question-item"
              type="button"
              @click="goToQuestion(question)"
            >
              <span class="question-index" :class="getIndexClass(index)">{{ index + 1 }}</span>

              <span class="question-content">
                <span class="question-title">{{ question.title }}</span>
                <span class="question-meta">
                  <span>
                    <el-icon><View /></el-icon>
                    {{ question.viewCount || 0 }} 浏览
                  </span>
                  <span>
                    <el-icon><Star /></el-icon>
                    {{ question.favoriteCount || 0 }} 收藏
                  </span>
                  <span>
                    <el-icon><Clock /></el-icon>
                    {{ formatDate(question.createTime) }}
                  </span>
                </span>
              </span>

              <CnStatusTag v-if="isLearned(question.id)" type="success" size="sm" subtle>
                已学习
              </CnStatusTag>
              <el-icon class="question-arrow"><ArrowRight /></el-icon>
            </button>

            <CnEmptyState
              v-if="!questionsLoading && filteredQuestions.length === 0"
              :title="searchKeyword ? '未找到匹配的题目' : '该题单暂无题目'"
              description="可以返回题库选择其他题单，或调整搜索关键词。"
              icon="QS"
              surface="transparent"
            >
              <template #actions>
                <el-button v-if="searchKeyword" @click="searchKeyword = ''">清空搜索</el-button>
                <el-button type="primary" @click="goBack">返回题库</el-button>
              </template>
            </CnEmptyState>
          </div>
        </CnSection>
      </main>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Back, Clock, Search, Star, VideoPlay, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { interviewApi } from '@/api/interview'

interface QuestionSet {
  id?: number
  title?: string
  description?: string
  type?: number
  categoryName?: string
  questionCount?: number
  viewCount?: number
  favoriteCount?: number
}

interface QuestionItem {
  id: number
  title: string
  viewCount?: number
  favoriteCount?: number
  createTime?: string
}

interface LearnProgress {
  learnedQuestionIds?: number[]
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const questionsLoading = ref(false)
const favoriteLoading = ref(false)
const questionSet = ref<QuestionSet>({})
const questionList = ref<QuestionItem[]>([])
const isFavorited = ref(false)
const searchKeyword = ref('')
const learnedQuestionIds = ref<number[]>([])

const questionSetId = ref(Number.parseInt(String(route.params.id), 10))

const learnedCount = computed(() => learnedQuestionIds.value.length)

const remainingCount = computed(() => Math.max(questionList.value.length - learnedCount.value, 0))

const filteredQuestions = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return questionList.value
  return questionList.value.filter((question) => question.title.toLowerCase().includes(keyword))
})

const progressPercent = computed(() => {
  if (questionList.value.length === 0) return 0
  return Math.round((learnedCount.value / questionList.value.length) * 100)
})

const progressColor = computed(() => {
  if (progressPercent.value < 30) return 'var(--cn-color-text-tertiary)'
  if (progressPercent.value < 70) return 'var(--cn-color-brand-primary)'
  return 'var(--cn-color-success)'
})

const questionListDescription = computed(() => {
  if (searchKeyword.value) return `当前搜索命中 ${filteredQuestions.value.length} 道题目。`
  return `共 ${questionList.value.length} 道题目，点击任意题目进入学习。`
})

const getIndexClass = (index: number) => {
  if (index < 3) return `top-${index + 1}`
  return ''
}

const isLearned = (questionId: number) => learnedQuestionIds.value.includes(questionId)

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const fetchQuestionSet = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getQuestionSetById(questionSetId.value)
    questionSet.value = data || {}
  } catch (error) {
    console.error('获取题单详情失败:', error)
    ElMessage.error('获取题单详情失败')
  } finally {
    loading.value = false
  }
}

const fetchQuestions = async () => {
  questionsLoading.value = true
  try {
    const data = await interviewApi.getQuestionsBySetId(questionSetId.value)
    questionList.value = data || []
  } catch (error) {
    console.error('获取题目列表失败:', error)
    ElMessage.error('获取题目列表失败')
  } finally {
    questionsLoading.value = false
  }
}

const checkFavoriteStatus = async () => {
  try {
    const favorited = await interviewApi.isFavorited(2, questionSetId.value)
    isFavorited.value = Boolean(favorited)
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

const toggleFavorite = async () => {
  favoriteLoading.value = true
  try {
    if (isFavorited.value) {
      await interviewApi.removeFavorite(2, questionSetId.value)
      ElMessage.success('取消收藏成功')
      isFavorited.value = false
    } else {
      await interviewApi.addFavorite(2, questionSetId.value)
      ElMessage.success('收藏成功')
      isFavorited.value = true
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('收藏操作失败')
  } finally {
    favoriteLoading.value = false
  }
}

const startLearning = () => {
  if (questionList.value.length === 0) {
    ElMessage.warning('该题单暂无题目')
    return
  }
  const firstQuestion = questionList.value[0]
  router.push(`/interview/questions/${questionSetId.value}/${firstQuestion.id}`)
}

const goToQuestion = (question: QuestionItem) => {
  router.push(`/interview/questions/${questionSetId.value}/${question.id}`)
}

const goBack = () => {
  router.push('/interview')
}

const fetchLearnProgress = async () => {
  try {
    const data = (await interviewApi.getLearnProgress(questionSetId.value)) as LearnProgress
    learnedQuestionIds.value = data?.learnedQuestionIds || []
  } catch (error) {
    console.debug('获取学习进度失败:', error)
  }
}

onMounted(() => {
  fetchQuestionSet()
  fetchQuestions()
  checkFavoriteStatus()
  fetchLearnProgress()
})
</script>

<style scoped>
.question-set-detail-page {
  min-width: 0;
}

.set-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.detail-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.set-sidebar {
  position: sticky;
  top: var(--cn-space-5);
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.progress-panel {
  display: grid;
  gap: var(--cn-space-3);
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
}

.progress-header strong {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 20px;
}

.progress-copy {
  margin: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.6;
}

.sidebar-actions {
  display: grid;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-4);
}

.set-info-list {
  display: grid;
  gap: var(--cn-space-3);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.info-row strong {
  color: var(--cn-color-text-primary);
  font-weight: 650;
  text-align: right;
}

.questions-main {
  min-width: 0;
}

.search-input {
  width: 240px;
}

.questions-list {
  display: grid;
  gap: var(--cn-space-3);
  min-height: 220px;
}

.question-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-4);
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

.question-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateX(4px);
}

.question-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  font-weight: 800;
  flex-shrink: 0;
}

.question-index.top-1 {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.question-index.top-2 {
  background: var(--cn-color-info-soft);
  color: var(--cn-color-info);
}

.question-index.top-3 {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.question-content {
  display: grid;
  flex: 1;
  gap: var(--cn-space-2);
  min-width: 0;
}

.question-title {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.question-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.question-meta .el-icon {
  color: var(--cn-color-brand-primary);
}

.question-arrow {
  color: var(--cn-color-text-tertiary);
  flex-shrink: 0;
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.question-item:hover .question-arrow {
  color: var(--cn-color-brand-primary);
  transform: translateX(2px);
}

@media (max-width: 1180px) {
  .set-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .set-sidebar {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .set-summary-grid,
  .set-sidebar {
    grid-template-columns: minmax(0, 1fr);
  }

  .search-input {
    width: 100%;
  }

  .question-item {
    align-items: flex-start;
  }

  .question-title {
    white-space: normal;
  }
}
</style>
