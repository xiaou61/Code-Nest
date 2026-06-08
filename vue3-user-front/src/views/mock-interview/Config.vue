<template>
  <CnPage class="mock-interview-config" surface="transparent" max-width="1180px" full-height>
    <CnPageHeader
      title="面试配置"
      description="确认方向、难度、出题模式和面试官风格后，创建一场新的 AI 模拟面试。"
      eyebrow="Interview Setup"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ currentDirectionName || '未选择方向' }}</CnStatusTag>
        <CnStatusTag type="warning" size="sm">{{ currentLevelName }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">约 {{ estimatedDuration }} 分钟</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          更换方向
        </el-button>
        <el-button type="primary" :loading="submitting" @click="startInterview">
          <el-icon><VideoPlay /></el-icon>
          开始面试
        </el-button>
      </template>
    </CnPageHeader>

    <section class="config-summary-grid" aria-label="面试配置概览">
      <CnStatCard
        title="面试方向"
        :value="currentDirectionName || formData.direction || '-'"
        description="来自入口页选择的技术方向"
        tone="brand"
        trend="flat"
        trend-text="方向"
      />
      <CnStatCard
        title="题目数量"
        :value="formData.questionCount"
        unit="题"
        description="本次会话预计生成的问题数"
        tone="success"
        trend="flat"
        trend-text="题量"
      />
      <CnStatCard
        title="预计时长"
        :value="estimatedDuration"
        unit="分钟"
        description="按每题约 3 分钟估算"
        tone="warning"
        trend="flat"
        trend-text="时间"
      />
      <CnStatCard
        title="出题模式"
        :value="currentQuestionModeName"
        description="AI 实时生成或从本地题库抽题"
        tone="info"
        trend="flat"
        trend-text="模式"
      />
    </section>

    <div class="config-layout">
      <CnSection class="form-section" title="配置参数" description="这些参数会直接用于创建面试会话。" surface="panel" divided>
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          size="large"
          class="config-form"
        >
          <el-form-item label="面试方向" prop="direction">
            <div class="direction-display">
              <CnStatusTag type="brand" size="md" :dot="false">
                {{ currentDirectionName || formData.direction }}
              </CnStatusTag>
              <el-button text type="primary" @click="goBack">重新选择</el-button>
            </div>
          </el-form-item>

          <el-form-item label="难度级别" prop="level">
            <el-radio-group v-model="formData.level" class="option-grid option-grid--three">
              <el-radio-button
                v-for="level in config.levels"
                :key="level.code"
                :label="level.code"
                class="option-radio"
              >
                <span class="option-content">
                  <strong>{{ level.name }}</strong>
                  <em>{{ level.description }}</em>
                </span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="出题模式" prop="questionMode">
            <el-radio-group v-model="formData.questionMode" class="option-grid option-grid--two" @change="handleQuestionModeChange">
              <el-radio
                v-for="mode in config.questionModes"
                :key="mode.code"
                :label="mode.code"
                border
                class="mode-radio"
              >
                <span class="option-content">
                  <strong>{{ mode.name }}</strong>
                  <em>{{ mode.description }}</em>
                </span>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="formData.questionMode === 1" label="选择题库" prop="questionSetIds">
            <div class="question-sets-wrapper" v-loading="loadingQuestionSets">
              <el-checkbox-group v-if="questionSets.length" v-model="formData.questionSetIds" class="question-set-grid">
                <el-checkbox
                  v-for="questionSet in questionSets"
                  :key="questionSet.id"
                  :label="questionSet.id"
                  border
                  class="question-set-item"
                >
                  <span class="question-set-content">
                    <strong>{{ questionSet.title }}</strong>
                    <em>{{ questionSet.questionCount || 0 }} 题</em>
                  </span>
                </el-checkbox>
              </el-checkbox-group>

              <CnEmptyState
                v-else-if="!loadingQuestionSets"
                title="暂无可用题库"
                description="当前方向没有可用题库，可以切换为 AI 出题模式继续创建面试。"
                icon="QS"
                size="sm"
                surface="transparent"
              >
                <template #actions>
                  <el-button type="primary" plain @click="formData.questionMode = 2">
                    使用 AI 出题
                  </el-button>
                </template>
              </CnEmptyState>
            </div>
          </el-form-item>

          <el-form-item label="题目数量" prop="questionCount">
            <el-radio-group v-model="formData.questionCount" class="count-group">
              <el-radio-button v-for="count in config.questionCounts" :key="count" :label="count">
                {{ count }} 题
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="面试官风格" prop="style">
            <el-select v-model="formData.style" placeholder="选择面试官风格">
              <el-option
                v-for="style in config.styles"
                :key="style.code"
                :label="style.name"
                :value="style.code"
              >
                <div class="select-option">
                  <span>{{ style.name }}</span>
                  <em>{{ style.description }}</em>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="预计时长">
            <div class="duration-info">
              <el-icon><Timer /></el-icon>
              <span>约 {{ estimatedDuration }} 分钟</span>
            </div>
          </el-form-item>

          <el-form-item class="submit-item">
            <el-button type="primary" size="large" class="start-btn" :loading="submitting" @click="startInterview">
              <el-icon><VideoPlay /></el-icon>
              开始面试
            </el-button>
          </el-form-item>
        </el-form>
      </CnSection>

      <aside class="side-panel">
        <CnSection title="面试须知" description="开始前确认这些基础条件。" surface="panel" divided>
          <template #actions>
            <el-icon><InfoFilled /></el-icon>
          </template>

          <ul class="tips-list">
            <li v-for="tip in tips" :key="tip">{{ tip }}</li>
          </ul>
        </CnSection>

        <CnSection title="当前选择" description="创建会话前的配置摘要。" surface="panel" divided>
          <div class="selection-list">
            <div class="selection-item">
              <span>方向</span>
              <strong>{{ currentDirectionName || formData.direction || '-' }}</strong>
            </div>
            <div class="selection-item">
              <span>难度</span>
              <strong>{{ currentLevelName }}</strong>
            </div>
            <div class="selection-item">
              <span>风格</span>
              <strong>{{ currentStyleName }}</strong>
            </div>
            <div class="selection-item">
              <span>模式</span>
              <strong>{{ currentQuestionModeName }}</strong>
            </div>
            <div class="selection-item">
              <span>题量</span>
              <strong>{{ formData.questionCount }} 题</strong>
            </div>
          </div>
        </CnSection>
      </aside>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  ArrowLeft,
  InfoFilled,
  Timer,
  VideoPlay
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { mockInterviewApi } from '@/api/mockInterview'

interface ConfigOption {
  code: number
  name: string
  description: string
}

interface InterviewConfig {
  levels: ConfigOption[]
  types: ConfigOption[]
  styles: ConfigOption[]
  questionCounts: number[]
  questionModes: ConfigOption[]
}

interface InterviewDirection {
  directionCode: string
  directionName: string
}

interface QuestionSet {
  id: number
  title: string
  questionCount?: number
}

interface CreateInterviewResponse {
  sessionId: number | string
}

const router = useRouter()
const route = useRoute()

const initialDirection = Array.isArray(route.query.direction)
  ? route.query.direction[0]
  : route.query.direction

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loadingQuestionSets = ref(false)
const directions = ref<InterviewDirection[]>([])
const questionSets = ref<QuestionSet[]>([])

const config = reactive<InterviewConfig>({
  levels: [],
  types: [],
  styles: [],
  questionCounts: [5, 8, 10],
  questionModes: []
})

const formData = reactive({
  direction: String(initialDirection || ''),
  level: 2,
  questionMode: 2,
  questionCount: 5,
  style: 2,
  interviewType: 1,
  questionSetIds: [] as number[]
})

const rules: FormRules = {
  direction: [{ required: true, message: '请选择面试方向', trigger: 'change' }],
  level: [{ required: true, message: '请选择难度级别', trigger: 'change' }],
  questionCount: [{ required: true, message: '请选择题目数量', trigger: 'change' }]
}

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: 'AI 模拟面试', to: '/mock-interview' },
  { label: '面试配置' }
]

const tips = [
  '请确保网络环境稳定，避免面试中断。',
  '建议在安静环境下进行，集中注意力作答。',
  '每道题建议思考后再提交，答案提交后不可修改。',
  '遇到不会的题目可以跳过，面试结束后会生成报告。',
  '本地题库模式会优先从所选题单中抽题。'
]

const currentDirectionName = computed(() => {
  const direction = directions.value.find(item => item.directionCode === formData.direction)
  return direction?.directionName || formData.direction
})

const currentLevelName = computed(() => {
  return config.levels.find(item => item.code === formData.level)?.name || '中级'
})

const currentStyleName = computed(() => {
  return config.styles.find(item => item.code === formData.style)?.name || '标准'
})

const currentQuestionModeName = computed(() => {
  return config.questionModes.find(item => item.code === formData.questionMode)?.name || 'AI 出题'
})

const estimatedDuration = computed(() => {
  return formData.questionCount * 3
})

const goBack = () => {
  router.push('/mock-interview')
}

const fetchConfig = async () => {
  try {
    const data = (await mockInterviewApi.getConfig()) as Partial<InterviewConfig>
    if (data) {
      Object.assign(config, data)
    }
  } catch (error) {
    console.error('获取配置失败', error)
  }
}

const fetchDirections = async () => {
  try {
    const data = (await mockInterviewApi.getDirections()) as InterviewDirection[]
    directions.value = data || []
  } catch (error) {
    console.error('获取方向列表失败', error)
  }
}

const fetchQuestionSets = async () => {
  if (!formData.direction) return

  loadingQuestionSets.value = true
  try {
    const data = (await mockInterviewApi.getQuestionSets(formData.direction)) as QuestionSet[]
    questionSets.value = data || []
  } catch (error) {
    console.error('获取题库列表失败', error)
    questionSets.value = []
  } finally {
    loadingQuestionSets.value = false
  }
}

const handleQuestionModeChange = (mode: string | number | boolean | undefined) => {
  formData.questionSetIds = []
  if (Number(mode) === 1) {
    fetchQuestionSets()
  }
}

const startInterview = async () => {
  try {
    await formRef.value?.validate()

    submitting.value = true
    const data = (await mockInterviewApi.createInterview({
      direction: formData.direction,
      level: formData.level,
      questionMode: formData.questionMode,
      questionCount: formData.questionCount,
      style: formData.style,
      interviewType: formData.interviewType,
      questionSetIds: formData.questionSetIds
    })) as CreateInterviewResponse

    ElMessage.success('面试创建成功，即将开始...')
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

onMounted(async () => {
  if (!formData.direction) {
    ElMessage.warning('请先选择面试方向')
    router.push('/mock-interview')
    return
  }

  await Promise.all([fetchConfig(), fetchDirections()])
  if (formData.questionMode === 1) {
    fetchQuestionSets()
  }
})
</script>

<style scoped>
.mock-interview-config {
  min-height: calc(100vh - 74px);
}

.config-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.config-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: var(--cn-space-5);
  align-items: start;
}

.form-section,
.side-panel {
  min-width: 0;
}

.side-panel {
  display: grid;
  gap: var(--cn-space-4);
}

.config-form {
  display: grid;
  gap: var(--cn-space-3);
}

.direction-display {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
}

.option-grid {
  display: grid;
  width: 100%;
  gap: var(--cn-space-3);
}

.option-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.option-grid--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.option-radio {
  min-width: 0;
}

.option-radio :deep(.el-radio-button__inner) {
  width: 100%;
  height: 100%;
  min-height: 82px;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  text-align: left;
}

.mode-radio {
  height: auto;
  margin: 0;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
}

.mode-radio :deep(.el-radio__label) {
  width: 100%;
  padding-left: var(--cn-space-2);
}

.option-content,
.question-set-content {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-1);
}

.option-content strong,
.question-set-content strong {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-content em,
.question-set-content em {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
  white-space: normal;
}

.question-sets-wrapper {
  width: 100%;
  min-height: 120px;
}

.question-set-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
  width: 100%;
}

.question-set-item {
  height: auto;
  min-height: 68px;
  margin: 0;
  padding: var(--cn-space-3);
}

.question-set-item :deep(.el-checkbox__label) {
  min-width: 0;
  width: 100%;
  padding-left: var(--cn-space-2);
}

.count-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.select-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.select-option em {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-style: normal;
}

.duration-info {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-weight: 700;
}

.submit-item :deep(.el-form-item__content) {
  display: block;
}

.start-btn {
  width: 100%;
  min-height: 48px;
  font-weight: 700;
}

.tips-list {
  display: grid;
  gap: var(--cn-space-3);
  margin: 0;
  padding-left: 18px;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.selection-list {
  display: grid;
  gap: var(--cn-space-3);
}

.selection-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.selection-item span {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.selection-item strong {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .config-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .config-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .side-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .config-summary-grid,
  .side-panel,
  .option-grid--three,
  .option-grid--two,
  .question-set-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .select-option {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }
}
</style>
