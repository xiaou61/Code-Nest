<template>
  <div class="random-questions">
    <!-- 头部导航 -->
    <div class="nav-bar">
      <el-button @click="goBack" :icon="Back" text>
        返回题库
      </el-button>
      <el-divider direction="vertical" />
      <h2>随机抽题</h2>
    </div>

    <!-- 配置区域 -->
    <el-card v-if="!hasStarted" shadow="never" class="config-card">
      <template #header>
        <div class="config-header">
          <span>配置抽题参数</span>
          <small>请选择题单和抽题数量</small>
        </div>
      </template>

      <div class="config-content">
        <!-- 题单选择 -->
        <div class="question-sets-section">
          <h3>选择题单 ({{ selectedSetIds.length }}/{{ questionSetList.length }})</h3>
          <div v-loading="setsLoading" class="question-sets-grid">
            <div 
              v-for="questionSet in questionSetList" 
              :key="questionSet.id"
              class="question-set-item"
              :class="{ 'selected': selectedSetIds.includes(questionSet.id) }"
              @click="toggleQuestionSet(questionSet.id)"
            >
              <div class="set-header">
                <h4 class="set-title">{{ questionSet.title }}</h4>
                <el-checkbox 
                  :modelValue="selectedSetIds.includes(questionSet.id)"
                  @change="toggleQuestionSet(questionSet.id)"
                  @click.stop
                />
              </div>
              <p class="set-description">{{ questionSet.description || '暂无描述' }}</p>
              <div class="set-meta">
                <el-tag :type="questionSet.type === 1 ? 'success' : 'info'" size="small">
                  {{ questionSet.type === 1 ? '官方' : '用户' }}
                </el-tag>
                <el-tag type="warning" size="small" v-if="questionSet.categoryName">
                  {{ questionSet.categoryName }}
                </el-tag>
                <span class="question-count">{{ questionSet.questionCount || 0 }} 题</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 抽题配置 -->
        <div class="config-options">
          <div class="option-item">
            <label>抽题数量：</label>
            <el-input-number 
              v-model="questionCount" 
              :min="1" 
              :max="totalQuestions > 0 ? totalQuestions : 50"
              :disabled="selectedSetIds.length === 0"
              controls-position="right"
              style="width: 200px;"
            />
            <span class="total-hint" v-if="totalQuestions > 0">
              / 共 {{ totalQuestions }} 题可选
            </span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="config-actions">
          <el-button 
            type="primary" 
            size="large"
            @click="startRandomQuestions"
            :disabled="selectedSetIds.length === 0 || questionCount <= 0"
            :loading="startLoading"
            :icon="Refresh"
          >
            开始随机抽题
          </el-button>
          <el-button size="large" @click="resetConfig">
            重置配置
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 题目展示区域 -->
    <div v-else class="questions-display">
      <!-- 结果头部 -->
      <el-card shadow="never" class="result-header-card">
        <div class="result-header">
          <div class="result-info">
            <h3>随机抽题结果</h3>
            <p>从 {{ selectedSetIds.length }} 个题单中随机抽取了 {{ randomQuestions.length }} 题</p>
          </div>
          <div class="result-actions">
            <el-button @click="regenerateQuestions" :icon="Refresh" :loading="startLoading">
              重新抽题
            </el-button>
            <el-button @click="resetConfig" :icon="Setting">
              重新配置
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 题目列表 -->
      <el-card shadow="never" class="questions-list-card">
        <div class="questions-wrapper">
          <div 
            v-for="(question, index) in randomQuestions" 
            :key="question.id"
            class="question-item"
          >
            <div class="question-header">
              <div class="question-number">第 {{ index + 1 }} 题</div>
              <div class="question-actions">
                <el-button 
                  :type="isFavorited(question.id) ? 'danger' : 'primary'" 
                  :icon="Star"
                  size="small"
                  @click="toggleQuestionFavorite(question)"
                >
                  {{ isFavorited(question.id) ? '取消收藏' : '收藏' }}
                </el-button>
              </div>
            </div>
            
            <div class="question-content">
              <h3 class="question-title">{{ question.title }}</h3>
              
              <div class="question-meta">
                <span class="set-title">来源：{{ question.questionSetTitle || '未知题单' }}</span>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ question.viewCount || 0 }} 浏览
                </span>
                <span class="favorite-count">
                  <el-icon><Star /></el-icon>
                  {{ question.favoriteCount || 0 }} 收藏
                </span>
              </div>

              <div class="answer-section">
                <div class="answer-toggle">
                  <el-button 
                    v-if="!question.showAnswer" 
                    type="primary" 
                    @click="toggleAnswer(question)"
                    :icon="View"
                  >
                    查看答案
                  </el-button>
                  <el-button 
                    v-else 
                    type="info" 
                    @click="toggleAnswer(question)"
                    :icon="Hide"
                  >
                    隐藏答案
                  </el-button>
                </div>
                
                <div v-if="question.showAnswer" class="answer-content">
                  <h4>参考答案</h4>
                  <div class="markdown-content" v-html="renderMarkdown(question.answer)"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { interviewApi } from '@/api/interview'
import { ElMessage } from 'element-plus'
import { Back, Refresh, Setting, Star, View, Hide } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'

const router = useRouter()

// 响应式数据
const questionSetList = ref([])
const selectedSetIds = ref([])
const questionCount = ref(5)
const randomQuestions = ref([])
const hasStarted = ref(false)
const favoriteMap = ref(new Map()) // 收藏状态映射

// 加载状态
const setsLoading = ref(false)
const startLoading = ref(false)

// 计算属性
const totalQuestions = computed(() => {
  return questionSetList.value
    .filter(set => selectedSetIds.value.includes(set.id))
    .reduce((total, set) => total + (set.questionCount || 0), 0)
})

// 获取题单列表
const loadQuestionSets = async () => {
  try {
    setsLoading.value = true
    const data = await interviewApi.getPublicQuestionSets({
      page: 1,
      size: 100 // 获取所有可用题单
    })
    questionSetList.value = data.records || []
  } catch (error) {
    console.error('获取题单列表失败:', error)
    ElMessage.error('获取题单列表失败')
  } finally {
    setsLoading.value = false
  }
}

// 切换题单选择
const toggleQuestionSet = (setId) => {
  const index = selectedSetIds.value.indexOf(setId)
  if (index > -1) {
    selectedSetIds.value.splice(index, 1)
  } else {
    selectedSetIds.value.push(setId)
  }
  
  // 调整抽题数量
  if (totalQuestions.value === 0) {
    questionCount.value = 5  // 没有选择题单时保持默认值
  } else if (questionCount.value > totalQuestions.value) {
    questionCount.value = Math.min(5, totalQuestions.value)
  }
}

// 开始随机抽题
const startRandomQuestions = async () => {
  if (selectedSetIds.value.length === 0) {
    ElMessage.warning('请选择至少一个题单')
    return
  }
  
  if (questionCount.value <= 0) {
    ElMessage.warning('请设置有效的抽题数量')
    return
  }

  try {
    startLoading.value = true
    const questions = await interviewApi.getRandomQuestions(selectedSetIds.value, questionCount.value)
    
    // 为每个题目添加显示状态
    randomQuestions.value = questions.map(q => ({
      ...q,
      showAnswer: false
    }))
    
    hasStarted.value = true
    ElMessage.success(`成功抽取 ${questions.length} 题`)
    
    // 批量检查收藏状态
    await checkFavoriteStatus()
  } catch (error) {
    console.error('随机抽题失败:', error)
    ElMessage.error('随机抽题失败')
  } finally {
    startLoading.value = false
  }
}

// 重新抽题
const regenerateQuestions = async () => {
  await startRandomQuestions()
}

// 重置配置
const resetConfig = () => {
  hasStarted.value = false
  selectedSetIds.value = []
  questionCount.value = 5
  randomQuestions.value = []
  favoriteMap.value.clear()
}

// 切换答案显示
const toggleAnswer = (question) => {
  question.showAnswer = !question.showAnswer
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  for (const question of randomQuestions.value) {
    try {
      const isFav = await interviewApi.isFavorited(2, question.id) // 2表示题目类型
      favoriteMap.value.set(question.id, isFav)
    } catch (error) {
      console.error('检查收藏状态失败:', error)
    }
  }
}

// 判断是否已收藏
const isFavorited = (questionId) => {
  return favoriteMap.value.get(questionId) || false
}

// 切换题目收藏状态
const toggleQuestionFavorite = async (question) => {
  try {
    const currentState = isFavorited(question.id)
    
    if (currentState) {
      await interviewApi.removeFavorite(2, question.id)
      favoriteMap.value.set(question.id, false)
      ElMessage.success('取消收藏成功')
    } else {
      await interviewApi.addFavorite(2, question.id)
      favoriteMap.value.set(question.id, true)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('操作收藏失败:', error)
    ElMessage.error('操作失败')
  }
}

// 返回上级
const goBack = () => {
  router.push('/interview')
}

// 初始化
onMounted(() => {
  loadQuestionSets()
})
</script>

<style scoped>
.random-questions {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f7ff 0%, #f0eeff 50%, #f5f0ff 100%);
  padding: 20px;
}

.nav-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  background: white;
  padding: 12px 20px;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(108, 99, 255, 0.06);
  border: 1px solid rgba(108, 99, 255, 0.06);
}

.nav-bar h2 {
  margin: 0;
  background: linear-gradient(135deg, #6c63ff, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.config-card {
  margin-bottom: 20px;
}

.config-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-header small {
  color: #999;
  font-size: 12px;
}

.config-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.question-sets-section h3 {
  margin: 0 0 16px 0;
  color: #6c63ff;
}

.question-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
}

.question-set-item {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
}

.question-set-item:hover {
  border-color: #6c63ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(108, 99, 255, 0.1);
}

.question-set-item.selected {
  border-color: #6c63ff;
  background: #f8f7ff;
}

.set-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.set-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  line-height: 1.4;
}

.set-description {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.set-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.question-count {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}

.config-options {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.option-item label {
  font-weight: 600;
  color: #303133;
}

.total-hint {
  color: #909399;
  font-size: 12px;
}

.config-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.result-header-card {
  margin-bottom: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-info h3 {
  margin: 0 0 4px 0;
  color: #6c63ff;
}

.result-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.questions-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.question-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.question-number {
  font-weight: 600;
  color: #6c63ff;
}

.question-content {
  padding: 20px;
}

.question-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.question-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 8px 0;
  border-bottom: 1px solid #e4e7ed;
}

.set-title {
  color: #6c63ff;
  font-weight: 500;
}

.view-count, .favorite-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
}

.answer-section {
  margin-top: 16px;
}

.answer-toggle {
  margin-bottom: 16px;
}

.answer-content {
  padding: 16px;
  background: #f8f7ff;
  border-radius: 10px;
  border-left: 4px solid #6c63ff;
}

.answer-content h4 {
  margin: 0 0 12px 0;
  color: #6c63ff;
  font-size: 16px;
}

.markdown-content {
  line-height: 1.6;
  color: #303133;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .random-questions {
    padding: 10px;
  }
  
  .nav-bar {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
  
  .question-sets-grid {
    grid-template-columns: 1fr;
  }
  
  .result-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
}
</style> 