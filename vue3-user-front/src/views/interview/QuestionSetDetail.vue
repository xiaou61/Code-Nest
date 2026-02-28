<template>
  <div class="question-set-detail">
    <!-- 左侧信息栏 -->
    <aside class="sidebar" v-loading="loading">
      <!-- 返回按钮 -->
      <div class="back-btn" @click="goBack">
        <el-icon><Back /></el-icon>
        <span>返回题库</span>
      </div>

      <!-- 题单信息卡片 -->
      <div class="info-card">
        <div class="info-header">
          <el-tag :type="questionSet.type === 1 ? 'success' : 'info'" size="small" effect="dark">
            {{ questionSet.type === 1 ? '官方题单' : '用户题单' }}
          </el-tag>
          <el-tag type="warning" size="small" v-if="questionSet.categoryName">
            {{ questionSet.categoryName }}
          </el-tag>
        </div>
        <h1 class="set-title">{{ questionSet.title }}</h1>
        <p class="set-description">{{ questionSet.description || '暂无描述' }}</p>
      </div>

      <!-- 统计数据 -->
      <div class="stats-card">
        <div class="stat-item">
          <div class="stat-icon questions-icon">
            <el-icon><Edit /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ questionSet.questionCount || 0 }}</span>
            <span class="stat-label">题目数量</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon views-icon">
            <el-icon><View /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ questionSet.viewCount || 0 }}</span>
            <span class="stat-label">浏览次数</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon favorites-icon">
            <el-icon><Star /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ questionSet.favoriteCount || 0 }}</span>
            <span class="stat-label">收藏人数</span>
          </div>
        </div>
      </div>

      <!-- 学习进度 -->
      <div class="progress-card">
        <div class="progress-header">
          <span>学习进度</span>
          <span class="progress-text">{{ learnedCount }}/{{ questionList.length }}</span>
        </div>
        <el-progress 
          :percentage="progressPercent" 
          :stroke-width="8"
          :color="progressColor"
        />
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button 
          type="primary" 
          size="large"
          :icon="VideoPlay"
          @click="startLearning"
          :disabled="questionList.length === 0"
          class="start-btn"
        >
          开始学习
        </el-button>
        <el-button 
          :type="isFavorited ? 'danger' : 'default'" 
          size="large"
          :icon="Star"
          @click="toggleFavorite"
          :loading="favoriteLoading"
          class="favorite-btn"
        >
          {{ isFavorited ? '取消收藏' : '收藏题单' }}
        </el-button>
      </div>
    </aside>

    <!-- 右侧题目列表 -->
    <main class="main-content">
      <!-- 列表头部 -->
      <div class="list-header">
        <div class="header-left">
          <h2 class="list-title">题目列表</h2>
          <span class="question-badge">{{ questionList.length }} 道题目</span>
        </div>
        <div class="header-right">
          <el-input 
            v-model="searchKeyword" 
            placeholder="搜索题目..." 
            clearable
            style="width: 200px"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <!-- 题目列表 -->
      <div v-loading="questionsLoading" class="questions-list">
        <div 
          v-for="(question, index) in filteredQuestions" 
          :key="question.id"
          class="question-item"
          @click="goToQuestion(question)"
        >
          <div class="question-index" :class="getIndexClass(index)">
            {{ index + 1 }}
          </div>
          <div class="question-content">
            <h4 class="question-title">{{ question.title }}</h4>
            <div class="question-meta">
              <span class="meta-item">
                <el-icon><View /></el-icon>
                {{ question.viewCount || 0 }}
              </span>
              <span class="meta-item">
                <el-icon><Star /></el-icon>
                {{ question.favoriteCount || 0 }}
              </span>
              <span class="meta-item time">
                <el-icon><Clock /></el-icon>
                {{ formatDate(question.createTime) }}
              </span>
            </div>
          </div>
          <div class="question-status">
            <el-tag v-if="isLearned(question.id)" type="success" size="small" effect="plain">
              <el-icon><Check /></el-icon>
              已学习
            </el-tag>
          </div>
          <div class="question-action">
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-if="!questionsLoading && filteredQuestions.length === 0" 
        :description="searchKeyword ? '未找到匹配的题目' : '该题单暂无题目'"
        :image-size="120"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Back, Edit, View, Star, VideoPlay, ArrowRight, Search, Clock, Check
} from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'

const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const questionsLoading = ref(false)
const favoriteLoading = ref(false)
const questionSet = ref({})
const questionList = ref([])
const isFavorited = ref(false)
const searchKeyword = ref('')
const learnedQuestionIds = ref([]) // 已学习的题目ID列表

// 获取题单ID
const questionSetId = ref(parseInt(route.params.id))

// 计算已学习数量
const learnedCount = computed(() => learnedQuestionIds.value.length)

// 计算属性 - 搜索过滤
const filteredQuestions = computed(() => {
  if (!searchKeyword.value) return questionList.value
  const keyword = searchKeyword.value.toLowerCase()
  return questionList.value.filter(q => 
    q.title.toLowerCase().includes(keyword)
  )
})

// 计算进度百分比
const progressPercent = computed(() => {
  if (questionList.value.length === 0) return 0
  return Math.round((learnedCount.value / questionList.value.length) * 100)
})

// 进度条颜色
const progressColor = computed(() => {
  if (progressPercent.value < 30) return '#909399'
  if (progressPercent.value < 70) return '#409eff'
  return '#67c23a'
})

// 获取序号样式
const getIndexClass = (index) => {
  if (index < 3) return `top-${index + 1}`
  return ''
}

// 判断题目是否已学习
const isLearned = (questionId) => {
  return learnedQuestionIds.value.includes(questionId)
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 获取题单详情
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

// 获取题目列表
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

// 检查收藏状态
const checkFavoriteStatus = async () => {
  try {
    const favorited = await interviewApi.isFavorited(2, questionSetId.value) // 2表示题单类型
    isFavorited.value = favorited || false
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 切换收藏状态
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

// 开始学习（跳转到第一题）
const startLearning = () => {
  if (questionList.value.length === 0) {
    ElMessage.warning('该题单暂无题目')
    return
  }
  const firstQuestion = questionList.value[0]
  router.push(`/interview/questions/${questionSetId.value}/${firstQuestion.id}`)
}

// 跳转到指定题目
const goToQuestion = (question) => {
  router.push(`/interview/questions/${questionSetId.value}/${question.id}`)
}

// 返回题库
const goBack = () => {
  router.push('/interview')
}

// 获取学习进度
const fetchLearnProgress = async () => {
  try {
    const data = await interviewApi.getLearnProgress(questionSetId.value)
    learnedQuestionIds.value = data?.learnedQuestionIds || []
  } catch (error) {
    console.debug('获取学习进度失败:', error)
  }
}

// 页面挂载
onMounted(() => {
  fetchQuestionSet()
  fetchQuestions()
  checkFavoriteStatus()
  fetchLearnProgress()
})
</script>

<style scoped>
/* 整体布局 */
.question-set-detail {
  display: flex;
  gap: 24px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f7ff 0%, #f0eeff 50%, #f5f0ff 100%);
  padding: 24px;
}

/* ========== 左侧边栏 ========== */
.sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 24px;
  height: fit-content;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #606266;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.back-btn:hover {
  color: #6c63ff;
  transform: translateX(-4px);
}

/* 信息卡片 */
.info-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(108, 99, 255, 0.06);
  border: 1px solid rgba(108, 99, 255, 0.06);
}

.info-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.set-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.4;
}

.set-description {
  margin: 0;
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
}

/* 统计卡片 */
.stats-card {
  background: white;
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 2px 12px rgba(108, 99, 255, 0.06);
  border: 1px solid rgba(108, 99, 255, 0.06);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.questions-icon {
  background: linear-gradient(135deg, #f0edff 0%, #e8e4ff 100%);
  color: #6c63ff;
}

.views-icon {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  color: #10b981;
}

.favorites-icon {
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
  color: #f59e0b;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

/* 进度卡片 */
.progress-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.progress-text {
  background: linear-gradient(135deg, #6c63ff, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.start-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
}

.start-btn:hover {
  background: linear-gradient(135deg, #4f46e5 0%, #6c63ff 100%);
}

.favorite-btn {
  width: 100%;
  height: 44px;
  border-radius: 12px;
}

/* ========== 右侧主内容 ========== */
.main-content {
  flex: 1;
  min-width: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 16px 20px;
  border-radius: 14px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(108, 99, 255, 0.06);
  border: 1px solid rgba(108, 99, 255, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.question-badge {
  background: linear-gradient(135deg, #6c63ff 0%, #8b5cf6 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

/* 题目列表 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: white;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
  box-shadow: 0 2px 12px rgba(108, 99, 255, 0.04);
}

.question-item:hover {
  border-color: rgba(108, 99, 255, 0.3);
  transform: translateX(8px);
  box-shadow: 0 8px 24px rgba(108, 99, 255, 0.12);
}

.question-item:hover .question-action {
  color: #6c63ff;
}

.question-item:hover .action-text {
  opacity: 1;
  transform: translateX(0);
}

.question-index {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #f0f2f5;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  margin-right: 16px;
  flex-shrink: 0;
}

.question-index.top-1 {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  color: white;
}

.question-index.top-2 {
  background: linear-gradient(135deg, #c0c0c0 0%, #a8a8a8 100%);
  color: white;
}

.question-index.top-3 {
  background: linear-gradient(135deg, #cd7f32 0%, #b87333 100%);
  color: white;
}

.question-content {
  flex: 1;
  min-width: 0;
}

.question-title {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-meta {
  display: flex;
  gap: 16px;
}

.question-meta .meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.question-meta .meta-item.time {
  color: #c0c4cc;
}

.question-status {
  margin-right: 12px;
  flex-shrink: 0;
}

.question-status .el-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.question-action {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #c0c4cc;
  font-size: 14px;
  transition: all 0.3s;
}

.action-text {
  font-size: 12px;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s;
}

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .question-set-detail {
    flex-direction: column;
    padding: 16px;
  }
  
  .sidebar {
    width: 100%;
    position: relative;
    top: 0;
    max-height: none;
    flex-direction: row;
    flex-wrap: wrap;
  }
  
  .back-btn {
    width: auto;
  }
  
  .info-card {
    flex: 1;
    min-width: 200px;
  }
  
  .stats-card {
    flex-direction: row;
    flex: 1;
    min-width: 200px;
  }
  
  .stat-item {
    flex: 1;
  }
  
  .progress-card {
    flex: 1;
    min-width: 200px;
  }
  
  .action-buttons {
    flex-direction: row;
    width: 100%;
  }
  
  .start-btn, .favorite-btn {
    flex: 1;
  }
}

@media (max-width: 640px) {
  .question-set-detail {
    padding: 12px;
    gap: 12px;
  }
  
  .sidebar {
    flex-direction: column;
  }
  
  .stats-card {
    flex-direction: column;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .list-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .header-right {
    width: 100%;
  }
  
  .header-right .el-input {
    width: 100% !important;
  }
  
  .question-item {
    padding: 12px 16px;
  }
  
  .question-index {
    width: 32px;
    height: 32px;
    font-size: 12px;
    margin-right: 12px;
  }
  
  .question-title {
    font-size: 14px;
  }
  
  .question-meta {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .action-text {
    display: none;
  }
}
</style>
