<template>
  <div class="mock-interview-report">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <div v-loading="loading" class="report-content">
      <!-- 报告头部 -->
      <div class="report-header">
        <div class="header-bg">
          <h1 class="report-title">面试报告</h1>
          <p class="report-subtitle">{{ report.directionName }} · {{ report.levelName }}</p>
        </div>
        
        <!-- 总分展示 -->
        <div class="score-section">
          <div class="total-score" :class="getScoreLevel(report.totalScore)">
            <div class="score-value">{{ report.totalScore || 0 }}</div>
            <div class="score-label">总分</div>
          </div>
          <div class="score-info">
            <div class="info-item">
              <span class="info-label">面试时长</span>
              <span class="info-value">{{ formatDuration(report.duration) }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">完成时间</span>
              <span class="info-value">{{ formatDate(report.endTime) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 维度得分 -->
      <el-card shadow="never" class="dimension-card">
        <template #header>
          <span class="card-title">
            <el-icon><DataAnalysis /></el-icon>
            能力维度分析
          </span>
        </template>

        <div class="dimension-grid">
          <div class="dimension-item">
            <div class="dimension-chart">
              <el-progress 
                type="dashboard" 
                :percentage="report.dimensions?.knowledge || 0"
                :width="100"
                :stroke-width="8"
              />
            </div>
            <div class="dimension-name">知识掌握</div>
          </div>
          <div class="dimension-item">
            <div class="dimension-chart">
              <el-progress 
                type="dashboard" 
                :percentage="report.dimensions?.depth || 0"
                :width="100"
                :stroke-width="8"
                color="#67c23a"
              />
            </div>
            <div class="dimension-name">理解深度</div>
          </div>
          <div class="dimension-item">
            <div class="dimension-chart">
              <el-progress 
                type="dashboard" 
                :percentage="report.dimensions?.expression || 0"
                :width="100"
                :stroke-width="8"
                color="#e6a23c"
              />
            </div>
            <div class="dimension-name">表达能力</div>
          </div>
          <div class="dimension-item">
            <div class="dimension-chart">
              <el-progress 
                type="dashboard" 
                :percentage="report.dimensions?.adaptability || 0"
                :width="100"
                :stroke-width="8"
                color="#909399"
              />
            </div>
            <div class="dimension-name">应变能力</div>
          </div>
        </div>
      </el-card>

      <!-- AI总结 -->
      <el-card shadow="never" class="summary-card">
        <template #header>
          <div class="card-header-with-action">
            <span class="card-title">
              <el-icon><ChatDotRound /></el-icon>
              AI 面试官总结
            </span>
            <el-button 
              v-if="!report.aiSummary" 
              type="primary" 
              size="small"
              :loading="generatingSummary"
              @click="handleGenerateSummary"
            >
              <el-icon v-if="!generatingSummary"><MagicStick /></el-icon>
              {{ generatingSummary ? '生成中...' : '生成AI总结' }}
            </el-button>
          </div>
        </template>

        <div class="summary-content">
          <template v-if="report.aiSummary">
            <p>{{ report.aiSummary }}</p>
          </template>
          <template v-else>
            <div class="empty-summary">
              <p>暂无总结，点击上方按钮生成 AI 面试总结</p>
            </div>
          </template>
        </div>
      </el-card>

      <!-- 改进建议 -->
      <el-card shadow="never" class="suggestion-card" v-if="report.aiSuggestion?.length">
        <template #header>
          <span class="card-title">
            <el-icon><Aim /></el-icon>
            学习建议
          </span>
        </template>

        <ul class="suggestion-list">
          <li v-for="(suggestion, index) in report.aiSuggestion" :key="index">
            <el-icon><Right /></el-icon>
            {{ suggestion }}
          </li>
        </ul>
      </el-card>

      <!-- 问答详情 -->
      <el-card shadow="never" class="qa-card">
        <template #header>
          <span class="card-title">
            <el-icon><Document /></el-icon>
            问答记录
          </span>
        </template>

        <el-collapse v-model="activeQa" class="qa-collapse">
          <el-collapse-item 
            v-for="(qa, index) in report.qaList" 
            :key="qa.qaId"
            :name="index"
          >
            <template #title>
              <div class="qa-title">
                <el-tag 
                  :type="qa.userAnswer ? 'success' : 'info'"
                  size="small"
                >
                  {{ qa.userAnswer ? '已答' : '跳过' }}
                </el-tag>
                <span class="qa-order">问题 {{ index + 1 }}</span>
                <span class="qa-score" v-if="qa.score !== null">
                  得分: <strong>{{ qa.score }}</strong>
                </span>
              </div>
            </template>

            <div class="qa-detail">
              <div class="qa-section">
                <h4 class="section-label">题目</h4>
                <p class="section-content">{{ qa.questionContent }}</p>
              </div>

              <div class="qa-section" v-if="qa.userAnswer">
                <h4 class="section-label">你的回答</h4>
                <p class="section-content">{{ qa.userAnswer }}</p>
              </div>

              <div class="qa-section" v-if="qa.referenceAnswer">
                <h4 class="section-label">参考答案</h4>
                <p class="section-content reference">{{ qa.referenceAnswer }}</p>
              </div>

              <div class="qa-section" v-if="qa.feedback">
                <h4 class="section-label">AI 点评</h4>
                <div class="section-content">
                  <div v-if="qa.feedback.strengths?.length">
                    <strong>优点：</strong>
                    <ul>
                      <li v-for="(item, i) in qa.feedback.strengths" :key="i">{{ item }}</li>
                    </ul>
                  </div>
                  <div v-if="qa.feedback.improvements?.length">
                    <strong>改进点：</strong>
                    <ul>
                      <li v-for="(item, i) in qa.feedback.improvements" :key="i">{{ item }}</li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 追问 -->
              <div class="follow-ups" v-if="qa.followUps?.length">
                <h4 class="follow-up-title">追问记录</h4>
                <div 
                  v-for="(followUp, fIndex) in qa.followUps" 
                  :key="fIndex"
                  class="follow-up-item"
                >
                  <div class="follow-up-question">
                    <el-tag type="warning" size="small">追问</el-tag>
                    {{ followUp.questionContent }}
                  </div>
                  <div class="follow-up-answer" v-if="followUp.userAnswer">
                    <strong>回答：</strong>{{ followUp.userAnswer }}
                  </div>
                  <div class="follow-up-score" v-if="followUp.score !== null">
                    得分：{{ followUp.score }}
                  </div>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-card>

      <!-- 操作按钮 -->
      <div class="report-actions">
        <el-button size="large" @click="goToHistory">
          查看历史记录
        </el-button>
        <el-button size="large" type="warning" plain @click="openTransformDialog">
          转为学习资产
        </el-button>
        <el-button type="primary" size="large" @click="startNew">
          再来一次
        </el-button>
      </div>
    </div>

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="mock_interview"
      :source-id="report.sessionId || Number(route.query.sessionId)"
      :source-title="report.directionName || '模拟面试报告'"
      :default-tags="report.weakPoints || []"
      @success="handleTransformSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, DataAnalysis, ChatDotRound, Aim, Document, Right, MagicStick 
} from '@element-plus/icons-vue'
import { mockInterviewApi } from '@/api/mockInterview'
import { useUserStore } from '@/stores/user'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const generatingSummary = ref(false)
const activeQa = ref([0])
const transformDialogVisible = ref(false)

const report = reactive({
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

// 获取分数等级
const getScoreLevel = (score) => {
  if (score >= 80) return 'level-high'
  if (score >= 60) return 'level-medium'
  return 'level-low'
}

// 格式化时长
const formatDuration = (seconds) => {
  if (!seconds) return '0分钟'
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  if (minutes === 0) return `${secs}秒`
  if (secs === 0) return `${minutes}分钟`
  return `${minutes}分${secs}秒`
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 返回
const goBack = () => {
  router.push('/mock-interview')
}

// 查看历史记录
const goToHistory = () => {
  router.push('/mock-interview/history')
}

// 再来一次
const startNew = () => {
  router.push('/mock-interview')
}

// 获取报告
const fetchReport = async () => {
  const sessionId = route.query.sessionId
  if (!sessionId) {
    ElMessage.error('报告不存在')
    router.push('/mock-interview')
    return
  }

  loading.value = true

  try {
    const data = await mockInterviewApi.getReport(sessionId)
    if (data) {
      Object.assign(report, data)
    }
  } catch (error) {
    console.error('获取报告失败', error)
    ElMessage.error('获取报告失败')
  } finally {
    loading.value = false
  }
}

// 生成AI总结
const handleGenerateSummary = async () => {
  const sessionId = route.query.sessionId
  if (!sessionId) return

  generatingSummary.value = true
  try {
    const data = await mockInterviewApi.generateSummary(sessionId)
    if (data) {
      report.aiSummary = data.aiSummary
      report.aiSuggestion = data.aiSuggestion || []
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

const handleTransformSuccess = (record) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.mock-interview-report {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.back-nav {
  margin-bottom: 16px;
}

/* 报告头部 */
.report-header {
  margin-bottom: 24px;
  border-radius: 12px;
  overflow: hidden;
  background: white;
}

.header-bg {
  padding: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
}

.report-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.report-subtitle {
  margin: 0;
  opacity: 0.9;
}

/* 总分区域 */
.score-section {
  display: flex;
  align-items: center;
  padding: 24px 32px;
}

.total-score {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-right: 32px;
}

.total-score.level-high {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  color: white;
}

.total-score.level-medium {
  background: linear-gradient(135deg, #e6a23c 0%, #f0c78a 100%);
  color: white;
}

.total-score.level-low {
  background: linear-gradient(135deg, #f56c6c 0%, #fab6b6 100%);
  color: white;
}

.score-value {
  font-size: 42px;
  font-weight: 700;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
}

.score-info {
  flex: 1;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  color: #909399;
}

.info-value {
  font-weight: 500;
  color: #303133;
}

/* 卡片通用样式 */
.dimension-card,
.summary-card,
.suggestion-card,
.qa-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.card-header-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.empty-summary {
  text-align: center;
  padding: 24px;
  color: #909399;
}

.empty-summary p {
  margin: 0;
}

/* 维度分析 */
.dimension-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.dimension-item {
  text-align: center;
}

.dimension-chart {
  margin-bottom: 12px;
}

.dimension-name {
  font-size: 14px;
  color: #606266;
}

/* AI总结 */
.summary-content p {
  margin: 0;
  line-height: 1.8;
  color: #606266;
}

/* 建议列表 */
.suggestion-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.suggestion-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
  color: #606266;
  line-height: 1.6;
}

.suggestion-list li:last-child {
  border-bottom: none;
}

.suggestion-list li .el-icon {
  color: #67c23a;
  margin-top: 4px;
}

/* 问答记录 */
.qa-collapse {
  border: none;
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.qa-order {
  font-weight: 500;
}

.qa-score {
  margin-left: auto;
  color: #409eff;
}

.qa-detail {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.qa-section {
  margin-bottom: 16px;
}

.qa-section:last-child {
  margin-bottom: 0;
}

.section-label {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
}

.section-content {
  margin: 0;
  line-height: 1.8;
  color: #606266;
}

.section-content.reference {
  padding: 12px;
  background: #f0f9eb;
  border-radius: 6px;
  border-left: 3px solid #67c23a;
}

/* 追问 */
.follow-ups {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #dcdfe6;
}

.follow-up-title {
  font-size: 14px;
  color: #e6a23c;
  margin: 0 0 12px 0;
}

.follow-up-item {
  padding: 12px;
  background: #fdf6ec;
  border-radius: 6px;
  margin-bottom: 12px;
}

.follow-up-item:last-child {
  margin-bottom: 0;
}

.follow-up-question {
  font-weight: 500;
  margin-bottom: 8px;
}

.follow-up-answer {
  color: #606266;
  margin-bottom: 4px;
}

.follow-up-score {
  font-size: 13px;
  color: #909399;
}

/* 操作按钮 */
.report-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

/* 响应式 */
@media (max-width: 768px) {
  .dimension-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .score-section {
    flex-direction: column;
    text-align: center;
  }
  
  .total-score {
    margin-right: 0;
    margin-bottom: 20px;
  }
}
</style>
