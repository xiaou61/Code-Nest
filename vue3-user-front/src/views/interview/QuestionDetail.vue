<template>
  <div class="question-detail">
    <!-- 顶部导航 -->
    <div class="nav-bar">
      <div class="nav-left">
        <el-button @click="goBack" :icon="Back" text>
          返回题单
        </el-button>
        <el-divider direction="vertical" />
        <span class="breadcrumb">
          {{ questionSet.title }} / 第 {{ currentIndex + 1 }} 题
        </span>
      </div>
      <div class="nav-right">
        <div class="mode-toggle">
          <el-switch
            v-model="isStudyMode"
            :active-icon="Reading"
            :inactive-icon="EditPen"
            active-text="背题模式"
            inactive-text="做题模式"
            inline-prompt
            @change="handleModeChange"
            style="margin-right: 12px;"
          />
        </div>
        <el-button 
          :type="isFavorited ? 'danger' : 'primary'" 
          :icon="Star"
          @click="toggleFavorite"
          :loading="favoriteLoading"
          size="small"
        >
          {{ isFavorited ? '取消收藏' : '收藏' }}
        </el-button>
      </div>
    </div>

    <!-- 题目内容 -->
    <div class="question-content-wrapper">
      <el-card v-loading="loading" shadow="never" class="question-card">
        <div class="question-header">
          <h1 class="question-title">{{ question.title }}</h1>
          <div class="question-meta">
            <span class="view-count">
              <el-icon><View /></el-icon>
              {{ question.viewCount || 0 }} 浏览
            </span>
            <span class="favorite-count">
              <el-icon><Star /></el-icon>
              {{ question.favoriteCount || 0 }} 收藏
            </span>
          </div>
        </div>

        <div class="question-content">
          <div class="answer-section" v-if="shouldShowAnswer">
            <h3>参考答案</h3>
            <div class="markdown-content" v-html="renderedAnswer"></div>
          </div>

          <div class="action-buttons" v-if="!isStudyMode">
            <el-button 
              v-if="!showAnswer" 
              type="primary" 
              @click="showAnswer = true"
              :icon="View"
            >
              查看答案
            </el-button>
            <el-button 
              v-else 
              type="info" 
              @click="showAnswer = false"
              :icon="Hide"
            >
              隐藏答案
            </el-button>
          </div>
          
          <div class="mode-tip" v-if="isStudyMode">
            <el-alert
              title="背题模式"
              description="当前为背题模式，答案已自动显示，适合复习和记忆"
              type="success"
              :closable="false"
              show-icon
            />
          </div>

          <!-- 掌握度选择器（仅做题模式且答案已显示时显示） -->
          <MasterySelector
            :question-id="questionId"
            :question-set-id="setId"
            :visible="!isStudyMode && showAnswer"
            @marked="handleMasteryMarked"
          />
        </div>
      </el-card>
    </div>

    <!-- 底部导航 -->
    <div class="bottom-nav">
      <el-card shadow="never" class="nav-card">
        <div class="nav-content">
          <!-- 桌面端按钮 -->
          <el-button 
            class="desktop-nav-btn"
            @click="goToPrevQuestion" 
            :disabled="!hasPrev"
            :icon="ArrowLeft"
          >
            上一题
          </el-button>
          
          <div class="progress-info">
            <span class="progress-text">{{ currentIndex + 1 }} / {{ totalQuestions }}</span>
            <el-progress 
              :percentage="progressPercentage" 
              :stroke-width="8"
              :show-text="false"
            />
          </div>
          
          <!-- 桌面端按钮 -->
          <el-button 
            class="desktop-nav-btn"
            @click="goToNextQuestion" 
            :disabled="!hasNext"
          >
            下一题
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
        
        <!-- 手机端专用按钮组 -->
        <div class="mobile-nav-buttons">
          <el-button 
            @click="goToPrevQuestion" 
            :disabled="!hasPrev"
            :icon="ArrowLeft"
            size="large"
          >
            上一题
          </el-button>
          <el-button 
            type="primary"
            @click="goToNextQuestion" 
            :disabled="!hasNext"
            size="large"
          >
            下一题
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Back, View, Hide, Star, ArrowLeft, ArrowRight, Reading, EditPen
} from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'
import { useInterviewStore } from '@/stores/interview'
import { interviewApi } from '@/api/interview'
import MasterySelector from './components/MasterySelector.vue'

const route = useRoute()
const router = useRouter()
const interviewStore = useInterviewStore()

// 响应式数据
const favoriteLoading = ref(false)
const showAnswer = ref(false)
const isStudyMode = ref(false) // false: 做题模式, true: 背题模式

// 从store获取数据
const loading = computed(() => interviewStore.currentQuestionLoading)
const question = computed(() => interviewStore.currentQuestion)
const questionSet = computed(() => interviewStore.currentQuestionSet)
const questionList = computed(() => interviewStore.questions)

// 收藏状态
const isFavorited = computed(() => {
  const statusKey = `3-${questionId.value}` // 3表示题目类型
  return interviewStore.favoriteStatus.get(statusKey) || false
})

// 路由参数
const setId = ref(parseInt(route.params.setId))
const questionId = ref(parseInt(route.params.questionId))

// 计算属性
const currentIndex = computed(() => {
  return questionList.value.findIndex(q => q.id === questionId.value)
})

const totalQuestions = computed(() => questionList.value.length)

const progressPercentage = computed(() => {
  if (totalQuestions.value === 0) return 0
  return Math.round(((currentIndex.value + 1) / totalQuestions.value) * 100)
})

const hasPrev = computed(() => currentIndex.value > 0)
const hasNext = computed(() => currentIndex.value < totalQuestions.value - 1)

// 是否显示答案（根据模式决定）
const shouldShowAnswer = computed(() => {
  return isStudyMode.value || showAnswer.value
})

// 渲染Markdown内容
const renderedAnswer = computed(() => {
  if (!question.value.answer) return ''
  return renderMarkdown(question.value.answer)
})

// 获取题单信息
const fetchQuestionSet = async () => {
  try {
    await interviewStore.fetchQuestionSetById(setId.value)
  } catch (error) {
    console.error('获取题单信息失败:', error)
  }
}

// 获取题目列表（用于导航）
const fetchQuestionList = async () => {
  try {
    await interviewStore.fetchQuestionsBySetId(setId.value)
  } catch (error) {
    console.error('获取题目列表失败:', error)
  }
}

// 获取题目详情
const fetchQuestion = async () => {
  try {
    await interviewStore.fetchQuestionById(setId.value, questionId.value)
    showAnswer.value = false // 重置答案显示状态
    markAsLearned() // 记录学习进度
  } catch (error) {
    console.error('获取题目详情失败:', error)
    ElMessage.error('获取题目详情失败')
  }
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  try {
    await interviewStore.checkFavoriteStatus(3, questionId.value) // 3表示题目类型
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 切换收藏状态
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

// 上一题
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

// 下一题
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

// 返回题单详情
const goBack = () => {
  router.push(`/interview/question-sets/${setId.value}`)
}

// 模式切换处理
const handleModeChange = (value) => {
  isStudyMode.value = value
  // 保存用户偏好到localStorage
  localStorage.setItem('question-mode', value ? 'study' : 'practice')
  
  // 如果切换到做题模式，重置答案显示状态
  if (!value) {
    showAnswer.value = false
  }
}

// 初始化模式状态
const initMode = () => {
  const savedMode = localStorage.getItem('question-mode')
  isStudyMode.value = savedMode === 'study'
}

// 记录学习进度（调用后端API）
const markAsLearned = async () => {
  try {
    await interviewApi.recordLearn(setId.value, questionId.value)
  } catch (error) {
    // 静默失败，不影响用户体验
    console.debug('记录学习进度失败:', error)
  }
}

// 掌握度标记回调
const handleMasteryMarked = (masteryData) => {
  console.log('掌握度已标记:', masteryData)
  // 可以在这里添加额外的处理逻辑
}

// 监听路由变化
watch(() => route.params.questionId, (newQuestionId) => {
  if (newQuestionId) {
    questionId.value = parseInt(newQuestionId)
    fetchQuestion()
    checkFavoriteStatus()
    // 如果是做题模式，重置答案显示状态
    if (!isStudyMode.value) {
      showAnswer.value = false
    }
  }
})

// 页面挂载
onMounted(() => {
  // 初始化模式状态
  initMode()
  
  // 加载页面数据
  fetchQuestionSet()
  fetchQuestionList()
  fetchQuestion()
  checkFavoriteStatus()
})
</script>

<style scoped>
.question-detail {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8f7ff 0%, #f5f3ff 100%);
  display: flex;
  flex-direction: column;
}

/* 顶部导航栏 */
.nav-bar {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.nav-left :deep(.el-button) {
  font-weight: 500;
  color: #6c63ff;
}

.nav-left :deep(.el-button:hover) {
  color: #4f46e5;
}

.breadcrumb {
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.mode-toggle {
  display: flex;
  align-items: center;
}

.mode-toggle :deep(.el-switch) {
  --el-switch-on-color: #6c63ff;
}

/* 题目内容区域 */
.question-content-wrapper {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.question-card {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  background: #ffffff;
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 16px rgba(108, 99, 255, 0.08);
  overflow: hidden;
}

.question-card :deep(.el-card__body) {
  padding: 32px;
}

.question-header {
  border-bottom: 2px solid #f0f2f5;
  padding-bottom: 24px;
  margin-bottom: 32px;
  position: relative;
}

.question-header::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #6c63ff, #ec4899);
  border-radius: 2px;
}

.question-title {
  margin: 0 0 16px 0;
  color: #1a1a2e;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.5;
  letter-spacing: -0.5px;
}

.question-meta {
  display: flex;
  gap: 24px;
  color: #8c8c8c;
  font-size: 14px;
}

.question-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.question-meta span:hover {
  background: #f5f3ff;
  color: #6c63ff;
}

.question-meta :deep(.el-icon) {
  font-size: 16px;
}

/* 题目内容 */
.question-content {
  line-height: 1.8;
}

.answer-section {
  margin-bottom: 32px;
}

.answer-section h3 {
  margin: 0 0 20px 0;
  color: #1a1a2e;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.answer-section h3::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 20px;
  background: linear-gradient(180deg, #6c63ff, #ec4899);
  border-radius: 2px;
}

.markdown-content {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e2e8f0;
}

.markdown-content :deep(pre) {
  background: #1e293b;
  border-radius: 12px;
  padding: 16px;
  overflow-x: auto;
}

.markdown-content :deep(code) {
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
  font-size: 14px;
}

.markdown-content :deep(p) {
  margin-bottom: 16px;
  color: #334155;
}

.markdown-content :deep(ul), 
.markdown-content :deep(ol) {
  padding-left: 24px;
  margin-bottom: 16px;
}

.markdown-content :deep(li) {
  margin-bottom: 8px;
  color: #475569;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  color: #1e293b;
  margin-top: 24px;
  margin-bottom: 12px;
}

.markdown-content :deep(blockquote) {
  border-left: 4px solid #6c63ff;
  padding-left: 16px;
  margin: 16px 0;
  color: #64748b;
  background: #f8f7ff;
  padding: 12px 16px;
  border-radius: 0 8px 8px 0;
}

.action-buttons {
  margin-top: 32px;
  text-align: center;
}

.action-buttons :deep(.el-button) {
  padding: 12px 32px;
  font-size: 15px;
  border-radius: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
}

.action-buttons :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #4f46e5 0%, #6c63ff 100%);
}

.mode-tip {
  margin-top: 24px;
}

.mode-tip :deep(.el-alert) {
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
}

/* 底部导航 */
.bottom-nav {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px 24px;
  position: sticky;
  bottom: 0;
  z-index: 100;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.06);
}

.nav-card {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  background: transparent;
  border: none;
  box-shadow: none;
}

.nav-card :deep(.el-card__body) {
  padding: 0;
}

.nav-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-content :deep(.el-button) {
  border-radius: 10px;
  font-weight: 500;
  padding: 10px 20px;
  transition: all 0.3s ease;
}

.nav-content :deep(.el-button:not(.is-disabled):hover) {
  transform: translateX(-3px);
}

.nav-content :deep(.el-button:last-child:not(.is-disabled):hover) {
  transform: translateX(3px);
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 16px;
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
}

.progress-info :deep(.el-progress) {
  width: 200px;
}

.progress-info :deep(.el-progress-bar__outer) {
  background: #e2e8f0;
}

.progress-info :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #6c63ff, #8b5cf6);
}

/* ===== 平板端适配 (768px - 1024px) ===== */
@media (max-width: 1024px) and (min-width: 769px) {
  .nav-bar {
    padding: 14px 20px;
  }
  
  .question-content-wrapper {
    padding: 20px;
  }
  
  .question-card :deep(.el-card__body) {
    padding: 28px;
  }
  
  .question-title {
    font-size: 24px;
  }
  
  .progress-info :deep(.el-progress) {
    width: 160px;
  }
}

/* ===== 手机端适配 (小于等于768px) ===== */
@media (max-width: 768px) {
  .question-detail {
    background: #f5f7fa;
  }
  
  /* 顶部导航 - 手机端 */
  .nav-bar {
    padding: 12px 16px;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .nav-left {
    width: 100%;
    justify-content: flex-start;
  }
  
  .nav-left :deep(.el-divider) {
    display: none;
  }
  
  .breadcrumb {
    font-size: 13px;
    max-width: 200px;
  }
  
  .nav-right {
    width: 100%;
    justify-content: space-between;
    padding-top: 8px;
    border-top: 1px solid #f0f2f5;
  }
  
  .mode-toggle {
    flex: 1;
  }
  
  .mode-toggle :deep(.el-switch__label) {
    font-size: 12px;
  }
  
  /* 内容区域 - 手机端 */
  .question-content-wrapper {
    padding: 16px 12px;
  }
  
  .question-card {
    width: 100%;
    min-width: unset;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }
  
  .question-card :deep(.el-card__body) {
    padding: 20px;
  }
  
  .question-header {
    padding-bottom: 16px;
    margin-bottom: 20px;
  }
  
  .question-title {
    font-size: 20px;
    line-height: 1.4;
  }
  
  .question-meta {
    flex-wrap: wrap;
    gap: 10px;
  }
  
  .question-meta span {
    padding: 4px 10px;
    font-size: 13px;
  }
  
  .answer-section h3 {
    font-size: 16px;
  }
  
  .markdown-content {
    padding: 16px;
    font-size: 14px;
    border-radius: 12px;
  }
  
  .markdown-content :deep(pre) {
    padding: 12px;
    font-size: 12px;
    border-radius: 8px;
  }
  
  .markdown-content :deep(code) {
    font-size: 12px;
  }
  
  .action-buttons :deep(.el-button) {
    width: 100%;
    padding: 14px 24px;
  }
  
  /* 底部导航 - 手机端 */
  .bottom-nav {
    padding: 12px 16px;
  }
  
  .nav-content {
    justify-content: center;
  }
  
  .progress-info {
    width: 100%;
    justify-content: center;
  }
  
  .progress-info :deep(.el-progress) {
    flex: 1;
    max-width: 200px;
  }
}

/* ===== 小屏手机适配 (小于等于480px) ===== */
@media (max-width: 480px) {
  .nav-bar {
    padding: 10px 12px;
  }
  
  .nav-left :deep(.el-button span) {
    display: none;
  }
  
  .breadcrumb {
    font-size: 12px;
    max-width: 150px;
  }
  
  .question-content-wrapper {
    padding: 12px 8px;
  }
  
  .question-card :deep(.el-card__body) {
    padding: 16px;
  }
  
  .question-title {
    font-size: 18px;
  }
  
  .question-meta {
    gap: 8px;
  }
  
  .question-meta span {
    padding: 3px 8px;
    font-size: 12px;
  }
  
  .markdown-content {
    padding: 12px;
  }
  
  .markdown-content :deep(pre) {
    padding: 10px;
    margin: 12px -12px;
    border-radius: 0;
  }
  
  .bottom-nav {
    padding: 10px 12px;
  }
  
  .progress-info {
    font-size: 14px;
    gap: 10px;
  }
  
  .progress-info :deep(.el-progress) {
    max-width: 150px;
  }
}

/* 桌面端导航按钮 */
.desktop-nav-btn {
  display: inline-flex;
}

/* 手机端专用的底部导航按钮 */
.mobile-nav-buttons {
  display: none;
}

@media (max-width: 768px) {
  .desktop-nav-btn {
    display: none !important;
  }
  
  .mobile-nav-buttons {
    display: flex;
    width: 100%;
    gap: 12px;
    margin-top: 16px;
  }
  
  .mobile-nav-buttons :deep(.el-button) {
    flex: 1;
    border-radius: 12px;
    font-weight: 500;
  }
  
  .mobile-nav-buttons :deep(.el-button--primary) {
    background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
    border: none;
  }
  
  .progress-info {
    margin-bottom: 0;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .question-card {
    background: rgba(30, 30, 46, 0.98);
  }
  
  .nav-bar,
  .bottom-nav {
    background: rgba(30, 30, 46, 0.98);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .question-title {
    color: #f1f5f9;
  }
  
  .breadcrumb {
    color: #94a3b8;
  }
  
  .markdown-content {
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
    border-color: #334155;
  }
  
  .markdown-content :deep(p),
  .markdown-content :deep(li) {
    color: #cbd5e1;
  }
  
  .question-meta span {
    background: #1e293b;
    color: #94a3b8;
  }
}

/* 安全区域适配 (iPhone X 等) */
@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .bottom-nav {
    padding-bottom: calc(16px + env(safe-area-inset-bottom));
  }
  
  @media (max-width: 768px) {
    .bottom-nav {
      padding-bottom: calc(12px + env(safe-area-inset-bottom));
    }
  }
}

/* 横屏模式优化 */
@media (max-height: 500px) and (orientation: landscape) {
  .nav-bar {
    padding: 8px 16px;
  }
  
  .question-content-wrapper {
    padding: 12px;
  }
  
  .bottom-nav {
    padding: 8px 16px;
  }
  
  .question-card :deep(.el-card__body) {
    padding: 16px;
  }
}

/* 打印样式 */
@media print {
  .nav-bar,
  .bottom-nav,
  .action-buttons,
  .mode-tip {
    display: none;
  }
  
  .question-detail {
    background: white;
  }
  
  .question-card {
    box-shadow: none;
    border: 1px solid #e2e8f0;
  }
}
</style>
