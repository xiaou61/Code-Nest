<template>
  <CnPage class="mock-interview-report" surface="transparent" max-width="1280px" full-height>
    <CnPageHeader
      title="面试报告"
      description="查看 AI 模拟面试的总分、能力维度、总结建议和逐题复盘记录。"
      eyebrow="Interview Report"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ report.directionName || report.direction || '模拟面试' }}</CnStatusTag>
        <CnStatusTag type="warning" size="sm">{{ report.levelName || levelText }}</CnStatusTag>
        <CnStatusTag :type="scoreTone(report.totalScore)" size="sm">总分 {{ report.totalScore || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回入口
        </el-button>
        <el-button @click="goToHistory">
          <el-icon><Clock /></el-icon>
          历史记录
        </el-button>
        <el-button type="primary" @click="startNew">
          <el-icon><Refresh /></el-icon>
          再来一次
        </el-button>
      </template>
    </CnPageHeader>

    <div v-loading="loading" class="report-content">
      <section class="report-summary-grid" aria-label="面试报告概览">
        <article class="score-card" :class="getScoreLevel(report.totalScore)">
          <span class="score-label">总分</span>
          <strong>{{ report.totalScore || 0 }}</strong>
          <em>{{ scoreLevelText(report.totalScore) }}</em>
        </article>
        <CnStatCard
          title="面试方向"
          :value="report.directionName || report.direction || '-'"
          description="本次模拟面试的技术方向"
          tone="brand"
          trend="flat"
          trend-text="方向"
          :loading="loading"
        />
        <CnStatCard
          title="面试时长"
          :value="formatDuration(report.duration)"
          description="从开始作答到面试结束"
          tone="info"
          trend="flat"
          trend-text="时长"
          :loading="loading"
        />
        <CnStatCard
          title="完成时间"
          :value="formatDate(report.endTime)"
          description="报告生成对应的结束时间"
          tone="success"
          trend="flat"
          trend-text="完成"
          :loading="loading"
        />
      </section>

      <div class="report-layout">
        <main class="report-main">
          <CnSection title="能力维度分析" description="从知识掌握、理解深度、表达能力和应变能力拆解本次表现。" surface="panel" divided>
            <template #actions>
              <el-icon><DataAnalysis /></el-icon>
            </template>

            <div class="dimension-grid">
              <article v-for="dimension in dimensions" :key="dimension.key" class="dimension-item">
                <el-progress
                  type="dashboard"
                  :percentage="dimension.value"
                  :width="104"
                  :stroke-width="8"
                  :color="dimension.color"
                />
                <strong>{{ dimension.name }}</strong>
                <span>{{ dimension.description }}</span>
              </article>
            </div>
          </CnSection>

          <CnSection title="AI 面试官总结" description="对整体表现的归纳，可以手动触发生成。" surface="panel" divided>
            <template #actions>
              <el-button
                v-if="!report.aiSummary"
                type="primary"
                size="small"
                :loading="generatingSummary"
                @click="handleGenerateSummary"
              >
                <el-icon v-if="!generatingSummary"><MagicStick /></el-icon>
                {{ generatingSummary ? '生成中...' : '生成 AI 总结' }}
              </el-button>
              <CnStatusTag v-else type="success" size="sm">已生成</CnStatusTag>
            </template>

            <div v-if="report.aiSummary" class="summary-content">
              {{ report.aiSummary }}
            </div>
            <CnEmptyState
              v-else
              title="暂无 AI 总结"
              description="点击右上角按钮生成 AI 面试总结和学习建议。"
              icon="AI"
              size="sm"
              surface="transparent"
            />
          </CnSection>

          <CnSection v-if="report.aiSuggestion?.length" title="学习建议" description="优先处理这些复盘建议，下一轮模拟面试更容易形成提升闭环。" surface="panel" divided>
            <template #actions>
              <el-icon><Aim /></el-icon>
            </template>

            <ul class="suggestion-list">
              <li v-for="(suggestion, index) in report.aiSuggestion" :key="`${index}-${suggestion}`">
                <el-icon><Right /></el-icon>
                <span>{{ suggestion }}</span>
              </li>
            </ul>
          </CnSection>

          <CnSection title="问答记录" description="逐题查看题目、回答、参考答案、AI 点评和追问记录。" surface="panel" divided>
            <template #actions>
              <CnStatusTag type="neutral" size="sm" :dot="false">{{ report.qaList.length }} 题</CnStatusTag>
            </template>

            <CnEmptyState
              v-if="!loading && report.qaList.length === 0"
              title="暂无问答记录"
              description="当前报告没有返回问答详情。"
              icon="QA"
              size="sm"
              surface="transparent"
            />

            <el-collapse v-else v-model="activeQa" class="qa-collapse">
              <el-collapse-item
                v-for="(qa, index) in report.qaList"
                :key="qa.qaId || index"
                :name="index"
              >
                <template #title>
                  <div class="qa-title">
                    <CnStatusTag :type="qa.userAnswer ? 'success' : 'info'" size="sm">
                      {{ qa.userAnswer ? '已答' : '跳过' }}
                    </CnStatusTag>
                    <span class="qa-order">问题 {{ index + 1 }}</span>
                    <span v-if="hasScore(qa.score)" class="qa-score">
                      得分 <strong>{{ qa.score }}</strong>
                    </span>
                  </div>
                </template>

                <div class="qa-detail">
                  <section class="qa-section">
                    <h4>题目</h4>
                    <p>{{ qa.questionContent || '暂无题目内容' }}</p>
                  </section>

                  <section v-if="qa.userAnswer" class="qa-section">
                    <h4>你的回答</h4>
                    <p>{{ qa.userAnswer }}</p>
                  </section>

                  <section v-if="qa.referenceAnswer" class="qa-section">
                    <h4>参考答案</h4>
                    <p class="reference">{{ qa.referenceAnswer }}</p>
                  </section>

                  <section v-if="qa.feedback" class="qa-section">
                    <h4>AI 点评</h4>
                    <div class="feedback-grid">
                      <div v-if="qa.feedback.strengths?.length" class="feedback-box success">
                        <strong>优点</strong>
                        <ul>
                          <li v-for="(item, itemIndex) in qa.feedback.strengths" :key="`s-${itemIndex}`">
                            {{ item }}
                          </li>
                        </ul>
                      </div>
                      <div v-if="qa.feedback.improvements?.length" class="feedback-box warning">
                        <strong>改进点</strong>
                        <ul>
                          <li v-for="(item, itemIndex) in qa.feedback.improvements" :key="`i-${itemIndex}`">
                            {{ item }}
                          </li>
                        </ul>
                      </div>
                    </div>
                  </section>

                  <section v-if="qa.followUps?.length" class="follow-ups">
                    <h4>追问记录</h4>
                    <article v-for="(followUp, followUpIndex) in qa.followUps" :key="followUp.qaId || followUpIndex" class="follow-up-item">
                      <div class="follow-up-question">
                        <CnStatusTag type="warning" size="sm">追问</CnStatusTag>
                        <span>{{ followUp.questionContent }}</span>
                      </div>
                      <p v-if="followUp.userAnswer"><strong>回答：</strong>{{ followUp.userAnswer }}</p>
                      <p v-if="hasScore(followUp.score)"><strong>得分：</strong>{{ followUp.score }}</p>
                    </article>
                  </section>
                </div>
              </el-collapse-item>
            </el-collapse>
          </CnSection>
        </main>

        <aside class="report-side">
          <CnSection title="报告动作" description="把复盘结果接到后续学习流程。" surface="panel" divided>
            <div class="action-stack">
              <el-button type="warning" plain @click="openTransformDialog">
                <el-icon><Document /></el-icon>
                转为学习资产
              </el-button>
              <el-button @click="goToHistory">
                查看历史记录
              </el-button>
              <el-button type="primary" @click="startNew">
                再来一次
              </el-button>
            </div>
          </CnSection>

          <CnSection title="薄弱点标签" description="转化学习资产时会作为默认标签。" surface="panel" divided>
            <div v-if="report.weakPoints.length" class="weak-point-list">
              <CnStatusTag
                v-for="point in report.weakPoints"
                :key="point"
                type="warning"
                size="sm"
                :dot="false"
                subtle
              >
                {{ point }}
              </CnStatusTag>
            </div>
            <CnEmptyState
              v-else
              title="暂无薄弱点"
              description="报告没有返回薄弱知识点标签。"
              icon="WP"
              size="sm"
              surface="transparent"
            />
          </CnSection>
        </aside>
      </div>
    </div>

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="mock_interview"
      :source-id="transformSourceId"
      :source-title="report.directionName || '模拟面试报告'"
      :default-tags="report.weakPoints || []"
      @success="handleTransformSuccess"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Aim,
  ArrowLeft,
  Clock,
  DataAnalysis,
  Document,
  MagicStick,
  Refresh,
  Right
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
import { useUserStore } from '@/stores/user'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'
import type { CnTone } from '@/design-system'

interface ReportDimensions {
  knowledge: number
  depth: number
  expression: number
  adaptability: number
}

interface QaFeedback {
  strengths?: string[]
  improvements?: string[]
}

interface QaDetail {
  qaId?: number | string
  questionOrder?: number
  questionContent?: string
  userAnswer?: string
  score?: number | null
  feedback?: QaFeedback
  referenceAnswer?: string
  followUps?: QaDetail[]
}

interface InterviewReport {
  sessionId: number | string | null
  direction: string
  directionName: string
  level: number
  levelName: string
  totalScore: number
  duration: number
  startTime: string | null
  endTime: string | null
  dimensions: ReportDimensions
  aiSummary: string
  aiSuggestion: string[]
  weakPoints: string[]
  qaList: QaDetail[]
}

interface LearningAssetRecord {
  recordId: number | string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const generatingSummary = ref(false)
const activeQa = ref<Array<number | string>>([0])
const transformDialogVisible = ref(false)

const report = reactive<InterviewReport>({
  sessionId: null,
  direction: '',
  directionName: '',
  level: 1,
  levelName: '',
  totalScore: 0,
  duration: 0,
  startTime: null,
  endTime: null,
  dimensions: {
    knowledge: 0,
    depth: 0,
    expression: 0,
    adaptability: 0
  },
  aiSummary: '',
  aiSuggestion: [],
  weakPoints: [],
  qaList: []
})

const sessionId = computed(() => {
  const value = route.query.sessionId
  return Array.isArray(value) ? value[0] : value
})

const transformSourceId = computed(() => {
  return report.sessionId || sessionId.value || 0
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: 'AI 模拟面试', to: '/mock-interview' },
  { label: '面试报告' }
]

const levelText = computed(() => {
  const map: Record<number, string> = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return map[report.level] || '未知难度'
})

const dimensions = computed(() => [
  {
    key: 'knowledge',
    name: '知识掌握',
    value: report.dimensions?.knowledge || 0,
    description: '基础概念和知识点覆盖',
    color: 'var(--cn-color-brand-primary)'
  },
  {
    key: 'depth',
    name: '理解深度',
    value: report.dimensions?.depth || 0,
    description: '原理解释和边界分析',
    color: 'var(--cn-color-success)'
  },
  {
    key: 'expression',
    name: '表达能力',
    value: report.dimensions?.expression || 0,
    description: '结构化表达和清晰度',
    color: 'var(--cn-color-warning)'
  },
  {
    key: 'adaptability',
    name: '应变能力',
    value: report.dimensions?.adaptability || 0,
    description: '追问和临场处理能力',
    color: 'var(--cn-color-text-tertiary)'
  }
])

const hasScore = (score: unknown): score is number => {
  return typeof score === 'number' && Number.isFinite(score)
}

const scoreTone = (score?: number): CnTone => {
  if ((score || 0) >= 80) return 'success'
  if ((score || 0) >= 60) return 'warning'
  return 'danger'
}

const getScoreLevel = (score?: number) => {
  if ((score || 0) >= 80) return 'level-high'
  if ((score || 0) >= 60) return 'level-medium'
  return 'level-low'
}

const scoreLevelText = (score?: number) => {
  if ((score || 0) >= 80) return '表现优秀'
  if ((score || 0) >= 60) return '仍有提升空间'
  return '建议重点复盘'
}

const formatDuration = (seconds?: number) => {
  if (!seconds) return '0分钟'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (minutes === 0) return `${secs}秒`
  if (secs === 0) return `${minutes}分钟`
  return `${minutes}分${secs}秒`
}

const formatDate = (dateStr?: string | null) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  return date.toLocaleString('zh-CN')
}

const goBack = () => {
  router.push('/mock-interview')
}

const goToHistory = () => {
  router.push('/mock-interview/history')
}

const startNew = () => {
  router.push('/mock-interview')
}

const fetchReport = async () => {
  if (!sessionId.value) {
    ElMessage.error('报告不存在')
    router.push('/mock-interview')
    return
  }

  loading.value = true
  try {
    const data = (await mockInterviewApi.getReport(sessionId.value)) as Partial<InterviewReport>
    if (data) {
      Object.assign(report, {
        ...data,
        dimensions: data.dimensions || report.dimensions,
        aiSuggestion: data.aiSuggestion || [],
        weakPoints: data.weakPoints || [],
        qaList: data.qaList || []
      })
    }
  } catch (error) {
    console.error('获取报告失败', error)
    ElMessage.error('获取报告失败')
  } finally {
    loading.value = false
  }
}

const handleGenerateSummary = async () => {
  if (!sessionId.value) return

  generatingSummary.value = true
  try {
    const data = (await mockInterviewApi.generateSummary(sessionId.value)) as Partial<InterviewReport>
    if (data) {
      Object.assign(report, {
        ...data,
        dimensions: data.dimensions || report.dimensions,
        aiSuggestion: data.aiSuggestion || [],
        weakPoints: data.weakPoints || report.weakPoints,
        qaList: data.qaList || report.qaList
      })
      ElMessage.success('总结生成成功')
    }
  } catch (error) {
    console.error('生成总结失败', error)
    ElMessage.error('生成总结失败，请重试')
  } finally {
    generatingSummary.value = false
  }
}

const openTransformDialog = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning('请先登录后再转化学习资产')
    router.push('/login')
    return
  }
  transformDialogVisible.value = true
}

const handleTransformSuccess = (record: LearningAssetRecord) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.mock-interview-report {
  min-height: calc(100vh - 74px);
}

.report-content {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

.report-summary-grid {
  display: grid;
  grid-template-columns: 220px repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.score-card {
  display: grid;
  align-content: center;
  justify-items: center;
  min-height: 168px;
  padding: var(--cn-space-5);
  border-radius: var(--cn-radius-panel);
  color: white;
  box-shadow: var(--cn-shadow-card);
}

.score-card.level-high {
  background: color-mix(in srgb, var(--cn-color-success) 82%, var(--cn-color-bg-surface));
}

.score-card.level-medium {
  background: color-mix(in srgb, var(--cn-color-warning) 82%, var(--cn-color-bg-surface));
}

.score-card.level-low {
  background: color-mix(in srgb, var(--cn-color-danger) 82%, var(--cn-color-bg-surface));
}

.score-card strong {
  font-family: var(--cn-font-heading);
  font-size: 54px;
  font-weight: 800;
  line-height: 1;
}

.score-label,
.score-card em {
  font-size: 13px;
  font-style: normal;
  opacity: 0.92;
}

.score-card em {
  margin-top: var(--cn-space-2);
}

.report-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 340px);
  gap: var(--cn-space-5);
  align-items: start;
}

.report-main,
.report-side {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-5);
}

.dimension-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.dimension-item {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  text-align: center;
}

.dimension-item strong {
  color: var(--cn-color-text-primary);
  font-weight: 700;
}

.dimension-item span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.summary-content {
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}

.suggestion-list {
  display: grid;
  gap: var(--cn-space-3);
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestion-list li {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  line-height: 1.7;
}

.suggestion-list .el-icon {
  flex-shrink: 0;
  margin-top: 4px;
  color: var(--cn-color-success);
}

.qa-collapse {
  border: 0;
}

.qa-collapse :deep(.el-collapse-item__header) {
  min-height: 56px;
  border-color: var(--cn-color-border-subtle);
}

.qa-title {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
}

.qa-order {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-score {
  margin-left: auto;
  color: var(--cn-color-brand-primary);
  font-size: 13px;
}

.qa-score strong {
  font-size: 16px;
}

.qa-detail {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.qa-section,
.follow-ups {
  display: grid;
  gap: var(--cn-space-2);
}

.qa-section h4,
.follow-ups h4 {
  margin: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  font-weight: 700;
}

.qa-section p,
.follow-up-item p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}

.qa-section .reference {
  padding: var(--cn-space-3);
  border-left: 3px solid var(--cn-color-success);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-success) 9%, var(--cn-color-bg-surface));
}

.feedback-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.feedback-box {
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.feedback-box.success {
  border: 1px solid color-mix(in srgb, var(--cn-color-success) 28%, var(--cn-color-border-subtle));
}

.feedback-box.warning {
  border: 1px solid color-mix(in srgb, var(--cn-color-warning) 32%, var(--cn-color-border-subtle));
}

.feedback-box strong {
  color: var(--cn-color-text-primary);
}

.feedback-box ul {
  display: grid;
  gap: var(--cn-space-2);
  margin: var(--cn-space-2) 0 0;
  padding-left: 18px;
  color: var(--cn-color-text-secondary);
  line-height: 1.7;
}

.follow-ups {
  padding-top: var(--cn-space-3);
  border-top: 1px dashed var(--cn-color-border);
}

.follow-up-item {
  display: grid;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-warning) 9%, var(--cn-color-bg-surface));
}

.follow-up-question {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-weight: 650;
  line-height: 1.7;
}

.action-stack {
  display: grid;
  gap: var(--cn-space-3);
}

.action-stack .el-button {
  width: 100%;
  margin-left: 0;
}

.weak-point-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .report-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .score-card {
    min-height: 150px;
  }

  .report-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .report-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .report-summary-grid,
  .dimension-grid,
  .feedback-grid,
  .report-side {
    grid-template-columns: minmax(0, 1fr);
  }

  .qa-title {
    gap: var(--cn-space-2);
  }

  .qa-score {
    margin-left: 0;
  }
}
</style>
