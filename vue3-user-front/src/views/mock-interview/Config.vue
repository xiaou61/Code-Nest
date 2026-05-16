<template>
  <div class="mock-interview-config">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <el-card shadow="never" class="config-card">
      <template #header>
        <div class="card-header">
          <h2 class="card-title">
            <el-icon><Setting /></el-icon>
            面试配置
          </h2>
          <p class="card-subtitle">自定义你的面试参数</p>
        </div>
      </template>

      <el-form 
        ref="formRef"
        :model="formData" 
        :rules="rules"
        label-width="120px"
        label-position="left"
        size="large"
        class="config-form"
      >
        <!-- 面试方向 -->
        <el-form-item label="面试方向" prop="direction">
          <div class="direction-display">
            <el-tag type="primary" size="large">
              {{ currentDirectionName }}
            </el-tag>
            <el-button text type="primary" @click="goBack">更换方向</el-button>
          </div>
        </el-form-item>

        <!-- 难度级别 -->
        <el-form-item label="难度级别" prop="level">
          <el-radio-group v-model="formData.level" class="level-group">
            <el-radio-button 
              v-for="level in config.levels" 
              :key="level.code" 
              :label="level.code"
            >
              <div class="level-option">
                <span class="level-name">{{ level.name }}</span>
                <span class="level-desc">{{ level.description }}</span>
              </div>
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 出题模式 -->
        <el-form-item label="出题模式" prop="questionMode">
          <el-radio-group v-model="formData.questionMode" @change="handleQuestionModeChange">
            <el-radio 
              v-for="mode in config.questionModes" 
              :key="mode.code" 
              :label="mode.code"
              border
              class="mode-radio"
            >
              <div class="mode-content">
                <span class="mode-name">{{ mode.name }}</span>
                <span class="mode-desc">{{ mode.description }}</span>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 选择题库（本地出题模式时显示） -->
        <el-form-item 
          v-if="formData.questionMode === 1" 
          label="选择题库"
          prop="questionSetIds"
        >
          <div class="question-sets-wrapper">
            <el-checkbox-group 
              v-model="formData.questionSetIds" 
              v-loading="loadingQuestionSets"
              class="question-sets-group"
            >
              <el-checkbox 
                v-for="qs in questionSets" 
                :key="qs.id" 
                :label="qs.id"
                border
                class="question-set-item"
              >
                <div class="question-set-content">
                  <span class="question-set-title">{{ qs.title }}</span>
                  <span class="question-set-count">{{ qs.questionCount || 0 }} 题</span>
                </div>
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="questionSets.length === 0 && !loadingQuestionSets" class="no-question-sets">
              <el-empty description="暂无可用题库，请选择AI出题模式" :image-size="80" />
            </div>
          </div>
        </el-form-item>

        <!-- 题目数量 -->
        <el-form-item label="题目数量" prop="questionCount">
          <el-radio-group v-model="formData.questionCount">
            <el-radio-button 
              v-for="count in config.questionCounts" 
              :key="count" 
              :label="count"
            >
              {{ count }} 题
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- AI风格 -->
        <el-form-item label="面试官风格" prop="style">
          <el-select v-model="formData.style" placeholder="选择面试官风格" style="width: 100%">
            <el-option 
              v-for="style in config.styles" 
              :key="style.code" 
              :label="style.name"
              :value="style.code"
            >
              <div class="style-option">
                <span class="style-name">{{ style.name }}</span>
                <span class="style-desc">{{ style.description }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 预计时长 -->
        <el-form-item label="预计时长">
          <div class="duration-info">
            <el-icon><Timer /></el-icon>
            <span>约 {{ estimatedDuration }} 分钟</span>
          </div>
        </el-form-item>

        <!-- 开始按钮 -->
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            class="start-btn"
            :loading="submitting"
            @click="startInterview"
          >
            <el-icon><VideoPlay /></el-icon>
            开始面试
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 温馨提示 -->
    <el-card shadow="never" class="tips-card">
      <template #header>
        <span class="tips-title">
          <el-icon><InfoFilled /></el-icon>
          面试须知
        </span>
      </template>
      <ul class="tips-list">
        <li>请确保网络环境稳定，避免面试中断</li>
        <li>建议在安静的环境下进行，集中注意力</li>
        <li>每道题建议思考后再作答，答案提交后不可修改</li>
        <li>如遇到不会的题目，可以选择跳过</li>
        <li>面试结束后会生成详细的评价报告</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, Setting, Timer, VideoPlay, InfoFilled 
} from '@element-plus/icons-vue'
import { mockInterviewApi } from '@/api/mockInterview'

const router = useRouter()
const route = useRoute()

// 表单ref
const formRef = ref(null)

// 提交状态
const submitting = ref(false)

// 配置数据
const config = reactive({
  levels: [],
  types: [],
  styles: [],
  questionCounts: [5, 8, 10],
  questionModes: []
})

// 方向列表
const directions = ref([])

// 题库列表
const questionSets = ref([])
const loadingQuestionSets = ref(false)

// 表单数据
const formData = reactive({
  direction: route.query.direction || '',
  level: 2, // 默认中级
  questionMode: 2, // 默认AI出题
  questionCount: 5,
  style: 2, // 默认标准型
  interviewType: 1, // 默认技术面试
  questionSetIds: [] // 选择的题库ID
})

// 表单校验规则
const rules = {
  direction: [{ required: true, message: '请选择面试方向', trigger: 'change' }],
  level: [{ required: true, message: '请选择难度级别', trigger: 'change' }],
  questionCount: [{ required: true, message: '请选择题目数量', trigger: 'change' }]
}

// 当前方向名称
const currentDirectionName = computed(() => {
  const dir = directions.value.find(d => d.directionCode === formData.direction)
  return dir ? dir.directionName : formData.direction
})

// 预计时长
const estimatedDuration = computed(() => {
  // 每道题约3分钟
  return formData.questionCount * 3
})

// 返回
const goBack = () => {
  router.push('/mock-interview')
}

// 获取配置
const fetchConfig = async () => {
  try {
    const data = await mockInterviewApi.getConfig()
    if (data) {
      Object.assign(config, data)
    }
  } catch (error) {
    console.error('获取配置失败', error)
  }
}

// 获取方向列表
const fetchDirections = async () => {
  try {
    const data = await mockInterviewApi.getDirections()
    directions.value = data || []
  } catch (error) {
    console.error('获取方向列表失败', error)
  }
}

// 获取题库列表
const fetchQuestionSets = async () => {
  if (!formData.direction) return
  
  loadingQuestionSets.value = true
  try {
    const data = await mockInterviewApi.getQuestionSets(formData.direction)
    questionSets.value = data || []
  } catch (error) {
    console.error('获取题库列表失败', error)
    questionSets.value = []
  } finally {
    loadingQuestionSets.value = false
  }
}

// 出题模式变化时
const handleQuestionModeChange = (mode) => {
  formData.questionSetIds = []
  if (mode === 1) {
    fetchQuestionSets()
  }
}

// 开始面试
const startInterview = async () => {
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    // 创建面试
    const data = await mockInterviewApi.createInterview(formData)
    
    ElMessage.success('面试创建成功，即将开始...')
    // 跳转到面试页面
    router.push({
      path: '/mock-interview/session',
      query: { sessionId: data.sessionId }
    })
  } catch (error) {
    if (error !== 'cancel') {
      console.error('创建面试失败', error)
      ElMessage.error('创建面试失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (!formData.direction) {
    ElMessage.warning('请先选择面试方向')
    router.push('/mock-interview')
    return
  }
  fetchConfig()
  fetchDirections()
})
</script>

<style scoped>
.mock-interview-config {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.back-nav {
  margin-bottom: 16px;
}

.config-card {
  border-radius: 12px;
  margin-bottom: 20px;
}

.card-header {
  text-align: center;
}

.card-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.card-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.config-form {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 0;
}

/* 方向显示 */
.direction-display {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 难度级别 */
.level-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.level-group :deep(.el-radio-button__inner) {
  padding: 12px 24px;
  height: auto;
}

.level-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.level-name {
  font-weight: 600;
}

.level-desc {
  font-size: 12px;
  color: #909399;
}

/* 出题模式 */
.mode-radio {
  margin-right: 16px;
  margin-bottom: 12px;
  height: auto !important;
  padding: 12px 16px !important;
}

/* 题库选择 */
.question-sets-wrapper {
  width: 100%;
}

.question-sets-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}

.question-set-item {
  margin: 0 !important;
  height: auto !important;
  padding: 12px 16px !important;
  flex: 0 0 calc(50% - 6px);
  box-sizing: border-box;
}

.question-set-item :deep(.el-checkbox__label) {
  padding-left: 8px;
  width: calc(100% - 24px);
}

.question-set-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.question-set-title {
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-set-count {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  flex-shrink: 0;
}

.no-question-sets {
  width: 100%;
  padding: 20px 0;
}

.mode-radio :deep(.el-radio__label) {
  padding-left: 8px;
}

.mode-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-name {
  font-weight: 600;
}

.mode-desc {
  font-size: 12px;
  color: #909399;
}

/* AI风格选项 */
.style-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.style-name {
  font-weight: 500;
}

.style-desc {
  font-size: 12px;
  color: #909399;
}

/* 预计时长 */
.duration-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  color: #409eff;
  font-weight: 500;
}

/* 开始按钮 */
.start-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
  margin-top: 20px;
}

/* 温馨提示 */
.tips-card {
  border-radius: 12px;
}

.tips-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  line-height: 2;
}

.tips-list li {
  font-size: 14px;
}
</style>
