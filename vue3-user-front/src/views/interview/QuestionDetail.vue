<template>
  <CnPage class="question-detail-page" max-width="1200px" full-height>
    <CnPageHeader
      :title="question.title || '题目详情'"
      :description="headerDescription"
      eyebrow="INTERVIEW QUESTION"
      :breadcrumbs="[
        { label: '面试题库', to: '/interview' },
        { label: questionSet.title || '题单详情', to: `/interview/question-sets/${setId}` },
        { label: `第 ${currentIndex + 1} 题` }
      ]"
    >
      <template #meta>
        <CnStatusTag :type="isStudyMode ? 'success' : 'brand'" size="sm">
          {{ isStudyMode ? '背题模式' : '做题模式' }}
        </CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>
          {{ question.viewCount || 0 }} 浏览
        </CnStatusTag>
        <CnStatusTag type="warning" size="sm" subtle>
          {{ question.favoriteCount || 0 }} 收藏
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Back" @click="goBack">返回题单</el-button>
        <el-switch
          v-model="isStudyMode"
          class="mode-switch"
          active-color="var(--cn-color-brand-primary)"
          :active-icon="Reading"
          :inactive-icon="EditPen"
          active-text="背题"
          inactive-text="做题"
          inline-prompt
          @change="handleModeChange"
        />
        <el-button
          :type="isFavorited ? 'danger' : 'primary'"
          :icon="Star"
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          {{ isFavorited ? '取消收藏' : '收藏' }}
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection class="question-section" title="题目内容" :description="questionMetaDescription" divided>
      <template #actions>
        <CnStatusTag type="neutral" size="sm" subtle>
          {{ currentIndex + 1 }} / {{ totalQuestions || 0 }}
        </CnStatusTag>
      </template>

      <div v-loading="loading" class="question-workbench">
        <div class="question-title-row">
          <h2>{{ question.title || '题目加载中' }}</h2>
          <div class="question-meta">
            <span>
              <el-icon><View /></el-icon>
              {{ question.viewCount || 0 }} 浏览
            </span>
            <span>
              <el-icon><Star /></el-icon>
              {{ question.favoriteCount || 0 }} 收藏
            </span>
          </div>
        </div>

        <el-alert
          v-if="isStudyMode"
          title="背题模式"
          description="当前为背题模式，答案已自动显示，适合复习和记忆。"
          type="success"
          :closable="false"
          show-icon
        />

        <div v-if="shouldShowAnswer" class="answer-section">
          <h3>参考答案</h3>
          <div class="markdown-content" v-html="renderedAnswer" />
        </div>

        <CnEmptyState
          v-else
          title="答案暂未显示"
          description="做题模式下先独立思考，再查看参考答案并标记掌握度。"
          icon="QA"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" :icon="View" @click="showAnswer = true">查看答案</el-button>
          </template>
        </CnEmptyState>

        <div v-if="!isStudyMode && shouldShowAnswer" class="answer-actions">
          <el-button :icon="Hide" @click="showAnswer = false">隐藏答案</el-button>
        </div>

        <MasterySelector
          :question-id="questionId"
          :question-set-id="setId"
          :visible="!isStudyMode && showAnswer"
          @marked="handleMasteryMarked"
        />
      </div>
    </CnSection>

    <CnSection class="question-nav-section" surface="panel" compact>
      <div class="nav-content">
        <el-button class="desktop-nav-btn" :icon="ArrowLeft" :disabled="!hasPrev" @click="goToPrevQuestion">
          上一题
        </el-button>

        <div class="progress-info">
          <span class="progress-text">{{ currentIndex + 1 }} / {{ totalQuestions }}</span>
          <el-progress :percentage="progressPercentage" :stroke-width="8" :show-text="false" />
        </div>

        <el-button class="desktop-nav-btn" :disabled="!hasNext" @click="goToNextQuestion">
          下一题
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="mobile-nav-buttons">
        <el-button :icon="ArrowLeft" :disabled="!hasPrev" size="large" @click="goToPrevQuestion">
          上一题
        </el-button>
        <el-button type="primary" :disabled="!hasNext" size="large" @click="goToNextQuestion">
          下一题
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Back, EditPen, Hide, Reading, Star, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { renderMarkdown } from '@/utils/markdown'
import { useInterviewStore } from '@/stores/interview'
import { interviewApi } from '@/api/interview'
import MasterySelector from './components/MasterySelector.vue'

interface Question {
  id?: number
  title?: string
  answer?: string
  viewCount?: number
  favoriteCount?: number
}

interface QuestionSet {
  title?: string
}

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()

const favoriteLoading = ref(false)
const showAnswer = ref(false)
const isStudyMode = ref(false)

const loading = computed(() => interviewStore.currentQuestionLoading)
const question = computed<Question>(() => interviewStore.currentQuestion || {})
const questionSet = computed<QuestionSet>(() => interviewStore.currentQuestionSet || {})
const questionList = computed<Question[]>(() => interviewStore.questions || [])

const parseRouteNumber = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return Number.parseInt(String(rawValue), 10)
}

const setId = ref(parseRouteNumber(route.params.setId))
const questionId = ref(parseRouteNumber(route.params.questionId))

const isFavorited = computed(() => {
  const statusKey = `3-${questionId.value}`
  return interviewStore.favoriteStatus.get(statusKey) || false
})

const currentIndex = computed(() => {
  return questionList.value.findIndex((item) => item.id === questionId.value)
})

const totalQuestions = computed(() => questionList.value.length)

const progressPercentage = computed(() => {
  if (totalQuestions.value === 0) return 0
  return Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100)
})

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < totalQuestions.value - 1)

const shouldShowAnswer = computed(() => isStudyMode.value || showAnswer.value)

const renderedAnswer = computed(() => {
  if (!question.value.answer) return ''
  return renderMarkdown(question.value.answer)
})

const headerDescription = computed(() => {
  const setTitle = questionSet.value.title || '当前题单'
  return `${setTitle} / 第 ${currentIndex.value + 1} 题`
})

const questionMetaDescription = computed(() => {
  if (totalQuestions.value === 0) return '正在加载题目列表和导航信息。'
  return `当前题单共 ${totalQuestions.value} 题，当前进度 ${progressPercentage.value}%。`
})

const fetchQuestionSet = async () => {
  try {
    await interviewStore.fetchQuestionSetById(setId.value)
  } catch (error) {
    console.error('获取题单信息失败:', error)
  }
}

const fetchQuestionList = async () => {
  try {
    await interviewStore.fetchQuestionsBySetId(setId.value)
  } catch (error) {
    console.error('获取题目列表失败:', error)
  }
}

const fetchQuestion = async () => {
  try {
    await interviewStore.fetchQuestionById(setId.value, questionId.value)
    showAnswer.value = false
    markAsLearned()
  } catch (error) {
    console.error('获取题目详情失败:', error)
    ElMessage.error('获取题目详情失败')
  }
}

const checkFavoriteStatus = async () => {
  try {
    await interviewStore.checkFavoriteStatus(3, questionId.value)
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

const toggleFavorite = async () => {
  favoriteLoading.value = true
  try {
    if (isFavorited.value) {
      await interviewStore.removeFavorite(3, questionId.value)
      ElMessage.success('取消收藏成功')
    } else {
      await interviewStore.addFavorite(3, questionId.value)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('收藏操作失败')
  } finally {
    favoriteLoading.value = false
  }
}

const goToPrevQuestion = async () => {
  try {
    const data = await interviewStore.fetchPrevQuestion(setId.value, questionId.value)
    if (data) {
      router.push(`/interview/questions/${setId.value}/${data.id}`)
    } else {
      ElMessage.info('已经是第一题了')
    }
  } catch (error) {
    ElMessage.error('获取上一题失败')
  }
}

const goToNextQuestion = async () => {
  try {
    const data = await interviewStore.fetchNextQuestion(setId.value, questionId.value)
    if (data) {
      router.push(`/interview/questions/${setId.value}/${data.id}`)
    } else {
      ElMessage.info('已经是最后一题了')
    }
  } catch (error) {
    ElMessage.error('获取下一题失败')
  }
}

const goBack = () => {
  router.push(`/interview/question-sets/${setId.value}`)
}

const handleModeChange = (value: string | number | boolean) => {
  const nextValue = Boolean(value)
  isStudyMode.value = nextValue
  localStorage.setItem('question-mode', nextValue ? 'study' : 'practice')

  if (!nextValue) {
    showAnswer.value = false
  }
}

const initMode = () => {
  const savedMode = localStorage.getItem('question-mode')
  isStudyMode.value = savedMode === 'study'
}

const markAsLearned = async () => {
  try {
    await interviewApi.recordLearn(setId.value, questionId.value)
  } catch (error) {
    console.debug('记录学习进度失败:', error)
  }
}

const handleMasteryMarked = (masteryData: unknown) => {
  console.log('掌握度已标记:', masteryData)
}

watch(() => route.params.questionId, (newQuestionId) => {
  if (newQuestionId) {
    questionId.value = parseRouteNumber(newQuestionId)
    fetchQuestion()
    checkFavoriteStatus()
    if (!isStudyMode.value) {
      showAnswer.value = false
    }
  }
})

onMounted(() => {
  initMode()
  fetchQuestionSet()
  fetchQuestionList()
  fetchQuestion()
  checkFavoriteStatus()
})
</script>

<style scoped>
.question-detail-page {
  min-width: 0;
}

.question-section :deep(.cn-section__body) {
  padding: 0;
}

.question-workbench {
  display: grid;
  gap: var(--cn-space-5);
  min-height: 420px;
  padding: var(--cn-space-6);
}

.question-title-row {
  display: grid;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-5);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.question-title-row h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 25px;
  font-weight: 700;
  line-height: 1.5;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.question-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  padding: 0 var(--cn-space-3);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
}

.question-meta .el-icon {
  color: var(--cn-color-brand-primary);
}

.answer-section {
  display: grid;
  gap: var(--cn-space-4);
}

.answer-section h3 {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 650;
}

.answer-section h3::before {
  width: 4px;
  height: 20px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-primary);
  content: '';
}

.markdown-content {
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.8;
}

.markdown-content :deep(pre) {
  overflow-x: auto;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-elevated);
}

.markdown-content :deep(code) {
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
  font-size: 13px;
}

.markdown-content :deep(p) {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 0 0 var(--cn-space-3);
  padding-left: var(--cn-space-6);
}

.markdown-content :deep(li) {
  margin-bottom: var(--cn-space-2);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin: var(--cn-space-5) 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
}

.markdown-content :deep(blockquote) {
  margin: var(--cn-space-4) 0;
  padding: var(--cn-space-3) var(--cn-space-4);
  border-left: 4px solid var(--cn-color-brand-primary);
  border-radius: 0 var(--cn-radius-card) var(--cn-radius-card) 0;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-text-secondary);
}

.answer-actions {
  display: flex;
  justify-content: center;
}

.question-nav-section {
  position: sticky;
  bottom: var(--cn-space-4);
  z-index: 10;
}

.nav-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.progress-info {
  display: flex;
  align-items: center;
  gap: var(--cn-space-4);
  min-width: 260px;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.progress-info :deep(.el-progress) {
  flex: 1;
  min-width: 180px;
}

.progress-info :deep(.el-progress-bar__outer) {
  background: var(--cn-color-bg-surface-muted);
}

.progress-info :deep(.el-progress-bar__inner) {
  background: var(--cn-color-brand-primary);
}

.mobile-nav-buttons {
  display: none;
}

@media (max-width: 768px) {
  .question-workbench {
    padding: var(--cn-space-4);
  }

  .question-title-row h2 {
    font-size: 20px;
  }

  .markdown-content {
    padding: var(--cn-space-4);
  }

  .question-nav-section {
    bottom: var(--cn-space-2);
  }

  .nav-content {
    justify-content: center;
  }

  .desktop-nav-btn {
    display: none;
  }

  .progress-info {
    width: 100%;
    min-width: 0;
    justify-content: center;
  }

  .progress-info :deep(.el-progress) {
    min-width: 0;
    max-width: 220px;
  }

  .mobile-nav-buttons {
    display: flex;
    gap: var(--cn-space-3);
    margin-top: var(--cn-space-4);
  }

  .mobile-nav-buttons :deep(.el-button) {
    flex: 1;
  }
}

@media print {
  .question-nav-section,
  .answer-actions,
  .mode-switch {
    display: none;
  }
}
</style>
