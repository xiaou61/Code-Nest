<template>
  <CnPage class="mock-interview-session" surface="transparent" max-width="1180px" full-height>
    <CnPageHeader
      title="AI 面试进行中"
      description="围绕当前题目作答，提交后查看 AI 评价、追问或进入下一题。"
      eyebrow="Live Interview"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ session.directionName || session.direction || '模拟面试' }}</CnStatusTag>
        <CnStatusTag :type="currentQuestion.questionType === 1 ? 'info' : 'warning'" size="sm">
          {{ currentQuestion.questionType === 1 ? '主题题' : '追问题' }}
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">进度 {{ progressPercent }}%</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回入口
        </el-button>
        <el-button type="danger" plain @click="confirmEndInterview">
          <el-icon><CircleCloseFilled /></el-icon>
          结束面试
        </el-button>
      </template>
    </CnPageHeader>

    <section class="session-summary-grid" aria-label="面试进度概览">
      <CnStatCard
        title="当前题号"
        :value="currentQuestion.questionOrder || 0"
        :description="`总题数 ${currentTotalQuestions || 0}`"
        tone="brand"
        trend="flat"
        trend-text="进度"
        :loading="loading"
      />
      <CnStatCard
        title="完成进度"
        :value="progressPercent"
        unit="%"
        description="按当前题号和总题数计算"
        tone="success"
        trend="flat"
        trend-text="状态"
        :loading="loading"
      />
      <CnStatCard
        title="题目类型"
        :value="currentQuestion.questionType === 1 ? '主题' : '追问'"
        description="AI 可能基于回答继续追问"
        tone="info"
        trend="flat"
        trend-text="类型"
      />
      <CnStatCard
        title="本题反馈"
        :value="feedback ? `${feedback.score} 分` : '待提交'"
        description="提交回答后展示即时评分"
        :tone="feedback ? scoreTone(feedback.score) : 'warning'"
        trend="flat"
        trend-text="评分"
      />
    </section>

    <CnSection class="progress-section" title="会话状态" description="面试过程中请保持页面打开，完成后进入报告页复盘。" surface="panel" divided>
      <div class="progress-row">
        <span>第 {{ currentQuestion.questionOrder || 0 }} / {{ currentTotalQuestions || 0 }} 题</span>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="10"
          :show-text="false"
          class="progress-bar"
        />
      </div>
    </CnSection>

    <div v-loading="loading" class="session-body">
      <CnSection v-if="isFinished" class="finished-section" surface="panel">
        <el-result icon="success" title="面试完成" sub-title="本次模拟面试已完成，可以进入报告页查看评分、总结和逐题复盘。">
          <template #extra>
            <el-button type="primary" size="large" @click="viewReport">
              查看面试报告
            </el-button>
          </template>
        </el-result>
      </CnSection>

      <template v-else>
        <CnSection title="当前题目" description="阅读题目后组织答案，建议按背景、原理、应用和边界展开。" surface="panel" divided>
          <template #actions>
            <CnStatusTag :type="currentQuestion.questionType === 1 ? 'info' : 'warning'" size="sm">
              {{ currentQuestion.questionType === 1 ? '主题' : '追问' }}
            </CnStatusTag>
            <CnStatusTag type="neutral" size="sm" :dot="false">
              问题 {{ currentQuestion.questionOrder || 0 }}
            </CnStatusTag>
          </template>

          <article class="question-card">
            <p class="question-text">{{ currentQuestion.questionContent || '正在加载题目...' }}</p>
            <div v-if="currentQuestion.knowledgePoints?.length" class="knowledge-tags">
              <CnStatusTag
                v-for="point in currentQuestion.knowledgePoints"
                :key="point"
                type="info"
                size="sm"
                :dot="false"
                subtle
              >
                {{ point }}
              </CnStatusTag>
            </div>
          </article>
        </CnSection>

        <CnSection title="你的回答" description="提交后会锁定当前回答并展示 AI 面试官评价。" surface="panel" divided>
          <el-input
            v-model="userAnswer"
            type="textarea"
            :rows="8"
            placeholder="请在此输入你的回答..."
            :disabled="submitting || Boolean(feedback)"
            maxlength="2000"
            show-word-limit
          />

          <div v-if="!feedback" class="answer-actions">
            <el-button :loading="skipping" @click="skipQuestion">
              跳过此题
            </el-button>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!userAnswer.trim()"
              @click="submitAnswer"
            >
              提交答案
            </el-button>
          </div>
        </CnSection>

        <CnSection v-if="feedback" class="feedback-section-card" title="AI 面试官评价" description="根据当前回答给出评分、优点和改进方向。" surface="panel" divided>
          <template #actions>
            <span class="score-badge" :class="getScoreClass(feedback.score)">
              {{ feedback.score }} 分
            </span>
          </template>

          <div class="feedback-content">
            <article v-if="feedback.strengths?.length" class="feedback-block success">
              <h3>
                <el-icon><CircleCheckFilled /></el-icon>
                回答优点
              </h3>
              <ul>
                <li v-for="(item, index) in feedback.strengths" :key="`strength-${index}`">
                  {{ item }}
                </li>
              </ul>
            </article>

            <article v-if="feedback.improvements?.length" class="feedback-block warning">
              <h3>
                <el-icon><WarningFilled /></el-icon>
                需要改进
              </h3>
              <ul>
                <li v-for="(item, index) in feedback.improvements" :key="`improvement-${index}`">
                  {{ item }}
                </li>
              </ul>
            </article>

            <article v-if="feedback.referencePoints?.length" class="feedback-block info">
              <h3>
                <el-icon><Reading /></el-icon>
                考察知识点
              </h3>
              <div class="knowledge-tags">
                <CnStatusTag
                  v-for="point in feedback.referencePoints"
                  :key="point"
                  type="info"
                  size="sm"
                  :dot="false"
                  subtle
                >
                  {{ point }}
                </CnStatusTag>
              </div>
            </article>
          </div>

          <div class="feedback-actions">
            <el-button
              v-if="feedback.nextAction === 'followUp' && feedback.followUpQuestion"
              type="warning"
              @click="handleFollowUp"
            >
              回答追问
            </el-button>
            <el-button
              v-if="feedback.nextAction !== 'followUp' && !requestingFollowUp"
              type="info"
              plain
              :loading="requestingFollowUp"
              @click="requestFollowUp"
            >
              请求追问
            </el-button>
            <el-button
              v-if="feedback.hasNext && feedback.nextAction !== 'followUp'"
              type="primary"
              @click="nextQuestion"
            >
              下一题
            </el-button>
            <el-button
              v-if="!feedback.hasNext && feedback.nextAction !== 'followUp'"
              type="success"
              @click="finishInterview"
            >
              完成面试
            </el-button>
          </div>
        </CnSection>
      </template>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  CircleCheckFilled,
  CircleCloseFilled,
  Reading,
  WarningFilled
} from '@element-plus/icons-vue'
import {
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { mockInterviewApi } from '@/api/mockInterview'
import type { CnTone } from '@/design-system'

type SessionId = number | string

interface SessionState {
  sessionId: SessionId | null
  id?: SessionId
  direction: string
  directionName?: string
  questionCount: number
  status: number
}

interface CurrentQuestion {
  qaId: SessionId | null
  questionOrder: number
  totalQuestions: number
  questionContent: string
  questionType: number
  knowledgePoints: string[]
  estimatedTime: number
  finished: boolean
}

interface FollowUpQuestion {
  qaId: SessionId
  questionContent: string
  questionType?: number
}

interface AnswerFeedback {
  score: number
  strengths: string[]
  improvements: string[]
  referencePoints: string[]
  nextAction?: string
  hasNext?: boolean
  followUpQuestion?: FollowUpQuestion | null
}

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const skipping = ref(false)
const isFinished = ref(false)
const requestingFollowUp = ref(false)
const userAnswer = ref('')
const feedback = ref<AnswerFeedback | null>(null)

const session = reactive<SessionState>({
  sessionId: null,
  direction: '',
  directionName: '',
  questionCount: 0,
  status: 0
})

const currentQuestion = reactive<CurrentQuestion>({
  qaId: null,
  questionOrder: 0,
  totalQuestions: 0,
  questionContent: '',
  questionType: 1,
  knowledgePoints: [],
  estimatedTime: 0,
  finished: false
})

const routeSessionId = computed(() => {
  const value = route.query.sessionId
  return Array.isArray(value) ? value[0] : value
})

const currentTotalQuestions = computed(() => {
  return currentQuestion.totalQuestions || session.questionCount || 0
})

const progressPercent = computed(() => {
  const total = currentTotalQuestions.value
  if (!total) return 0
  return Math.min(100, Math.round((currentQuestion.questionOrder / total) * 100))
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: 'AI 模拟面试', to: '/mock-interview' },
  { label: '面试进行中' }
]

const scoreTone = (score?: number): CnTone => {
  if ((score || 0) >= 8) return 'success'
  if ((score || 0) >= 6) return 'warning'
  return 'danger'
}

const getScoreClass = (score?: number) => {
  if ((score || 0) >= 8) return 'score-high'
  if ((score || 0) >= 6) return 'score-medium'
  return 'score-low'
}

const goBack = () => {
  router.push('/mock-interview')
}

const updateCurrentQuestion = (data: Partial<CurrentQuestion>) => {
  if (data.finished) {
    isFinished.value = true
    return
  }

  currentQuestion.qaId = data.qaId || null
  currentQuestion.questionOrder = data.questionOrder || 0
  currentQuestion.totalQuestions = data.totalQuestions || currentQuestion.totalQuestions
  currentQuestion.questionContent = data.questionContent || ''
  currentQuestion.questionType = data.questionType || 1
  currentQuestion.knowledgePoints = data.knowledgePoints || []
  currentQuestion.estimatedTime = data.estimatedTime || 0
  currentQuestion.finished = false

  userAnswer.value = ''
  feedback.value = null
}

const initSession = async () => {
  const sessionId = routeSessionId.value
  if (!sessionId) {
    ElMessage.error('会话不存在')
    router.push('/mock-interview')
    return
  }

  session.sessionId = sessionId
  loading.value = true

  try {
    const statusData = (await mockInterviewApi.getSessionStatus(sessionId)) as Partial<SessionState>
    if (statusData) {
      Object.assign(session, statusData)
      session.sessionId = statusData.sessionId || statusData.id || sessionId
    }

    const startData = (await mockInterviewApi.startInterview(sessionId)) as Partial<CurrentQuestion>
    if (startData) {
      updateCurrentQuestion(startData)
    }
  } catch (error) {
    console.error('初始化会话失败', error)
    ElMessage.error('初始化面试失败')
  } finally {
    loading.value = false
  }
}

const submitAnswer = async () => {
  if (!userAnswer.value.trim()) {
    ElMessage.warning('请输入你的回答')
    return
  }

  submitting.value = true

  try {
    const data = (await mockInterviewApi.submitAnswer({
      sessionId: session.sessionId,
      qaId: currentQuestion.qaId,
      answer: userAnswer.value
    })) as {
      score: number
      feedback?: {
        strengths?: string[]
        improvements?: string[]
      }
      nextAction?: string
      hasNext?: boolean
      followUpQuestion?: FollowUpQuestion | null
    }

    feedback.value = {
      score: data.score,
      strengths: data.feedback?.strengths || [],
      improvements: data.feedback?.improvements || [],
      referencePoints: [],
      nextAction: data.nextAction,
      hasNext: data.hasNext,
      followUpQuestion: data.followUpQuestion || null
    }
  } catch (error) {
    console.error('提交答案失败', error)
    ElMessage.error('提交答案失败')
  } finally {
    submitting.value = false
  }
}

const skipQuestion = async () => {
  try {
    await ElMessageBox.confirm('确定要跳过这道题吗？跳过后将得0分。', '提示', {
      type: 'warning'
    })

    skipping.value = true
    const data = (await mockInterviewApi.skipQuestion(session.sessionId, currentQuestion.qaId)) as Partial<CurrentQuestion>

    if (data) {
      updateCurrentQuestion(data)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('跳过题目失败', error)
      ElMessage.error('操作失败')
    }
  } finally {
    skipping.value = false
  }
}

const handleFollowUp = () => {
  if (!feedback.value?.followUpQuestion) return

  currentQuestion.questionContent = feedback.value.followUpQuestion.questionContent
  currentQuestion.qaId = feedback.value.followUpQuestion.qaId
  currentQuestion.questionType = 2

  userAnswer.value = ''
  feedback.value = null
}

const requestFollowUp = async () => {
  requestingFollowUp.value = true

  try {
    const data = (await mockInterviewApi.requestFollowUp(session.sessionId, currentQuestion.qaId)) as Partial<CurrentQuestion>

    if (data) {
      currentQuestion.questionContent = data.questionContent || ''
      currentQuestion.qaId = data.qaId || null
      currentQuestion.questionType = 2

      userAnswer.value = ''
      feedback.value = null

      ElMessage.success('已生成追问，请回答')
    }
  } catch (error) {
    console.error('请求追问失败', error)
    ElMessage.error(error instanceof Error ? error.message : '请求追问失败')
  } finally {
    requestingFollowUp.value = false
  }
}

const nextQuestion = async () => {
  loading.value = true

  try {
    const data = (await mockInterviewApi.getNextQuestion(session.sessionId)) as Partial<CurrentQuestion>

    if (data) {
      updateCurrentQuestion(data)
    }
  } catch (error) {
    console.error('获取下一题失败', error)
    ElMessage.error('获取下一题失败')
  } finally {
    loading.value = false
  }
}

const finishInterview = async () => {
  loading.value = true

  try {
    await mockInterviewApi.endInterview(session.sessionId)
    isFinished.value = true
  } catch (error) {
    console.error('完成面试失败', error)
    ElMessage.error('完成面试失败')
  } finally {
    loading.value = false
  }
}

const confirmEndInterview = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要提前结束面试吗？未完成的题目将不计入评分。',
      '结束面试',
      { type: 'warning' }
    )

    await finishInterview()
  } catch (error) {
    // 用户取消时不需要提示。
  }
}

const viewReport = () => {
  if (!session.sessionId) return
  router.push({
    path: '/mock-interview/report',
    query: { sessionId: session.sessionId }
  })
}

onMounted(() => {
  initSession()
})
</script>

<style scoped>
.mock-interview-session {
  min-height: calc(100vh - 74px);
}

.session-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.progress-section {
  min-width: 0;
}

.progress-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: var(--cn-space-4);
}

.progress-row span {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.progress-bar {
  min-width: 0;
}

.session-body {
  display: grid;
  min-width: 0;
  min-height: 360px;
  gap: var(--cn-space-5);
}

.finished-section {
  min-height: 360px;
}

.question-card {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-bg-surface-muted) 84%, var(--cn-color-brand-soft));
}

.question-text {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.85;
  white-space: pre-wrap;
}

.knowledge-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.answer-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
}

.feedback-section-card {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border-subtle));
}

.score-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 var(--cn-space-3);
  border-radius: var(--cn-radius-pill);
  color: white;
  font-size: 14px;
  font-weight: 800;
}

.score-high {
  background: var(--cn-color-success);
}

.score-medium {
  background: var(--cn-color-warning);
}

.score-low {
  background: var(--cn-color-danger);
}

.feedback-content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.feedback-block {
  display: grid;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.feedback-block.info {
  grid-column: 1 / -1;
}

.feedback-block.success {
  border: 1px solid color-mix(in srgb, var(--cn-color-success) 28%, var(--cn-color-border-subtle));
}

.feedback-block.warning {
  border: 1px solid color-mix(in srgb, var(--cn-color-warning) 32%, var(--cn-color-border-subtle));
}

.feedback-block.info {
  border: 1px solid color-mix(in srgb, var(--cn-color-info) 28%, var(--cn-color-border-subtle));
}

.feedback-block h3 {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

.feedback-block.success h3 {
  color: var(--cn-color-success);
}

.feedback-block.warning h3 {
  color: var(--cn-color-warning);
}

.feedback-block.info h3 {
  color: var(--cn-color-info);
}

.feedback-block ul {
  display: grid;
  gap: var(--cn-space-2);
  margin: 0;
  padding-left: 20px;
  color: var(--cn-color-text-secondary);
  line-height: 1.75;
}

.feedback-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-5);
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
}

@media (max-width: 1180px) {
  .session-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .session-summary-grid,
  .feedback-content {
    grid-template-columns: minmax(0, 1fr);
  }

  .progress-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .answer-actions,
  .feedback-actions {
    justify-content: stretch;
  }

  .answer-actions .el-button,
  .feedback-actions .el-button {
    flex: 1;
    min-width: 0;
  }
}
</style>
