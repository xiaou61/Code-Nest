<template>
  <div class="job-battle-page cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <div class="page-header cn-learn-hero cn-wave-reveal">
        <div class="header-content">
          <span class="cn-learn-hero__eyebrow">Job Battle</span>
          <h1 class="page-title cn-learn-hero__title">
            <el-icon><Trophy /></el-icon>
            求职作战台（增强版）
          </h1>
          <p class="page-subtitle cn-learn-hero__desc">
            串行流程：JD解析 → 简历匹配 → 30天计划 → 面试复盘
          </p>
        </div>
      </div>

      <el-card class="steps-card cn-learn-panel" shadow="never">
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step title="JD解析" description="识别岗位要求" />
          <el-step title="简历匹配" description="评估差距项" />
          <el-step title="计划生成" description="自动生成任务" />
          <el-step title="面试复盘" description="沉淀改进动作" />
        </el-steps>
      </el-card>

      <el-card v-if="activeStep === 0" class="step-card cn-learn-panel" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤1：输入岗位JD</span>
          </div>
        </template>
        <el-form label-position="top">
          <el-form-item label="JD内容">
            <el-input
              v-model="jdForm.jdText"
              type="textarea"
              :rows="12"
              placeholder="粘贴岗位JD全文"
            />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="目标岗位（可选）">
                <el-input v-model="jdForm.targetRole" placeholder="如 Java后端" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="目标级别（可选）">
                <el-input v-model="jdForm.targetLevel" placeholder="如 中级" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="城市（可选）">
                <el-input v-model="jdForm.city" placeholder="如 深圳" />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="step-actions">
            <el-button type="primary" :loading="loading.jd" @click="handleParseJd">
              下一步：解析JD
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card v-if="activeStep === 1" class="step-card cn-learn-panel" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤2：简历匹配评估</span>
            <el-tag size="small" :type="jdResult?.fallback ? 'warning' : 'success'">
              {{ jdResult?.fallback ? 'JD降级结果' : 'JD AI结果' }}
            </el-tag>
          </div>
        </template>
        <div class="context-box">
          <div><b>岗位：</b>{{ jdResult?.jobTitle || '-' }}</div>
          <div><b>必备技能：</b>{{ joinText(jdResult?.mustSkills) }}</div>
          <div><b>关键词：</b>{{ joinText(jdResult?.keywords) }}</div>
          <div><b>摘要：</b>{{ jdResult?.summary || '-' }}</div>
        </div>
        <el-form label-position="top">
          <el-form-item label="简历内容">
            <el-input
              v-model="matchForm.resumeText"
              type="textarea"
              :rows="10"
              placeholder="粘贴简历全文"
            />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="项目亮点（可选）">
                <el-input v-model="matchForm.projectHighlights" placeholder="补充你最强项目证据" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标公司类型（可选）">
                <el-input v-model="matchForm.targetCompanyType" placeholder="如 大厂/中厂/创业公司" />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="step-actions">
            <el-button @click="activeStep = 0">上一步</el-button>
            <el-button type="primary" :loading="loading.match" @click="handleMatchResume">
              下一步：生成差距项
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card v-if="activeStep === 2" class="step-card cn-learn-panel" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤3：生成30天行动计划</span>
            <el-tag size="small" :type="planResult ? (planResult.fallback ? 'warning' : 'success') : (matchResult?.fallback ? 'warning' : 'success')">
              {{ planResult ? (planResult.fallback ? '计划降级结果' : '计划 AI结果') : (matchResult?.fallback ? '匹配降级结果' : '匹配 AI结果') }}
            </el-tag>
          </div>
        </template>
        <div class="context-box">
          <div><b>匹配分：</b>{{ matchResult?.overallScore ?? '-' }}</div>
          <div><b>预计通过率：</b>{{ matchResult?.estimatedPassRate ?? '-' }}%</div>
          <div><b>缺失关键词：</b>{{ joinText(matchResult?.missingKeywords) }}</div>
          <div><b>差距项数量：</b>{{ (matchResult?.gaps || []).length }}</div>
          <div class="tip-text">系统会自动使用上一步差距项，不需要你手输结果。</div>
        </div>
        <el-form label-position="top">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="计划天数">
                <el-input-number v-model="planForm.targetDays" :min="7" :max="90" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="每周可投入小时">
                <el-input-number v-model="planForm.weeklyHours" :min="1" :max="60" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="学习偏好（可选）">
                <el-select v-model="planForm.preferredLearningMode" placeholder="可选">
                  <el-option label="混合" value="混合" />
                  <el-option label="实战" value="实战" />
                  <el-option label="文档" value="文档" />
                  <el-option label="视频" value="视频" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="下一场面试日期（可选）">
            <el-date-picker
              v-model="planForm.nextInterviewDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
          <div class="step-actions">
            <el-button @click="activeStep = 1">上一步</el-button>
            <el-button type="primary" :loading="loading.plan" @click="handleGeneratePlan">
              生成计划
            </el-button>
            <el-button plain @click="openPlanHistory">
              查看历史计划
            </el-button>
            <el-button type="success" plain :disabled="!planResult" @click="goToReviewStep">
              下一步：进入复盘
            </el-button>
          </div>
        </el-form>

        <div v-if="planResult" class="plan-preview">
          <el-divider>计划生成结果</el-divider>
          <div class="result-item"><b>计划名：</b>{{ planResult.planName || '-' }}</div>
          <div class="result-item"><b>总天数：</b>{{ planResult.totalDays || '-' }}</div>
          <div class="result-item"><b>周目标：</b>{{ joinText(planResult.weeklyGoals, ' | ') }}</div>
          <div class="result-item">
            <b>里程碑：</b>
            <pre class="json-view">{{ formatJson(planResult.milestones) }}</pre>
          </div>
          <div class="result-item">
            <b>每日任务（预览）：</b>
            <pre class="json-view">{{ formatJson(planResult.dailyTasks) }}</pre>
          </div>
          <div class="result-item"><b>风险与兜底：</b>{{ joinText(planResult.riskAndFallback) }}</div>
        </div>
      </el-card>

      <el-card v-if="activeStep === 3" class="step-card cn-learn-panel" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤4：面试复盘总结</span>
            <el-tag v-if="planResult" size="small" :type="planResult.fallback ? 'warning' : 'success'">
              {{ planResult.fallback ? '计划降级结果' : '计划 AI结果' }}
            </el-tag>
          </div>
        </template>
        <div class="context-box">
          <div><b>计划名：</b>{{ planResult?.planName || '-' }}</div>
          <div><b>总天数：</b>{{ planResult?.totalDays || '-' }}</div>
          <div><b>周目标：</b>{{ joinText(planResult?.weeklyGoals, ' | ') }}</div>
        </div>
        <el-form label-position="top">
          <el-form-item label="面试记录">
            <el-input
              v-model="reviewForm.interviewNotes"
              type="textarea"
              :rows="7"
              placeholder="记录本次面试问题、回答情况、面试官反馈"
            />
          </el-form-item>
          <el-form-item label="问答转写（可选）">
            <el-input
              v-model="reviewForm.qaTranscriptJson"
              type="textarea"
              :rows="5"
              placeholder="可粘贴问答JSON或文本"
            />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="面试结果">
                <el-select v-model="reviewForm.interviewResult">
                  <el-option label="通过" value="pass" />
                  <el-option label="未通过" value="reject" />
                  <el-option label="待定" value="pending" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="目标岗位">
                <el-input v-model="reviewForm.targetRole" placeholder="如 Java后端" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="下一场面试日期（可选）">
                <el-date-picker
                  v-model="reviewForm.nextInterviewDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="step-actions">
            <el-button @click="activeStep = 2">上一步</el-button>
            <el-button type="primary" :loading="loading.review" @click="handleReviewInterview">
              生成复盘结论
            </el-button>
          </div>
        </el-form>
      </el-card>

      <el-card v-if="reviewResult" class="result-card cn-learn-panel" shadow="never">
        <template #header>
          最终复盘结果
          <el-tag size="small" :type="reviewResult.fallback ? 'warning' : 'success'" class="ml-8">
            {{ reviewResult.fallback ? '复盘降级结果' : '复盘 AI结果' }}
          </el-tag>
        </template>
        <div class="result-block">
          <div class="result-item"><b>结论：</b>{{ reviewResult.overallConclusion || '-' }}</div>
          <div class="result-item"><b>根因：</b>{{ joinText(reviewResult.rootCauses) }}</div>
          <div class="result-item"><b>信心分：</b>{{ reviewResult.confidenceScore ?? '-' }}</div>
          <div class="result-item">
            <b>高影响修复动作：</b>
            <pre class="json-view">{{ formatJson(reviewResult.highImpactFixes) }}</pre>
          </div>
          <div class="result-item">
            <b>题型短板：</b>
            <pre class="json-view">{{ formatJson(reviewResult.questionTypeWeakness) }}</pre>
          </div>
          <div class="result-item"><b>7天计划：</b>{{ joinText(reviewResult.next7DayPlan, ' | ') }}</div>
          <div class="step-actions">
            <el-button type="primary" plain @click="restartFlow">重新开始一轮</el-button>
          </div>
        </div>
      </el-card>

      <el-drawer
        v-model="historyState.visible"
        title="历史计划"
        size="56%"
        :destroy-on-close="false"
      >
        <div class="history-toolbar">
          <el-input
            v-model="historyState.query.keyword"
            placeholder="按计划名搜索"
            clearable
            @keyup.enter="handleHistorySearch"
          />
          <el-button type="primary" :loading="historyState.loading" @click="handleHistorySearch">
            查询
          </el-button>
          <el-button @click="resetHistorySearch">重置</el-button>
        </div>

        <el-table
          v-loading="historyState.loading"
          :data="historyState.records"
          stripe
          class="history-table"
        >
          <el-table-column prop="planName" label="计划名" min-width="200" show-overflow-tooltip />
          <el-table-column prop="targetDays" label="目标天数" width="92" />
          <el-table-column prop="weeklyHours" label="周投入" width="86">
            <template #default="{ row }">{{ row.weeklyHours ?? '-' }}h</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column prop="fallback" label="类型" width="96">
            <template #default="{ row }">
              <el-tag size="small" :type="row.fallback ? 'warning' : 'success'">
                {{ row.fallback ? '降级' : 'AI' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="144" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="useHistoryPlan(row)">
                载入
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="history-pagination" v-if="historyState.total > 0">
          <el-pagination
            v-model:current-page="historyState.query.pageNum"
            v-model:page-size="historyState.query.pageSize"
            :total="historyState.total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleHistorySizeChange"
            @current-change="fetchPlanHistory"
          />
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Trophy } from '@element-plus/icons-vue'
import { jobBattleApi } from '@/api/jobBattle'

const activeStep = ref(0)
const loading = reactive({
  jd: false,
  match: false,
  plan: false,
  review: false
})

const jdForm = reactive({
  jdText: '',
  targetRole: '',
  targetLevel: '',
  city: ''
})

const matchForm = reactive({
  resumeText: '',
  projectHighlights: '',
  targetCompanyType: ''
})

const planForm = reactive({
  targetDays: 30,
  weeklyHours: 6,
  preferredLearningMode: '',
  nextInterviewDate: ''
})

const reviewForm = reactive({
  interviewNotes: '',
  qaTranscriptJson: '',
  interviewResult: 'pending',
  targetRole: '',
  nextInterviewDate: ''
})

const jdResult = ref(null)
const matchResult = ref(null)
const planResult = ref(null)
const reviewResult = ref(null)
const historyState = reactive({
  visible: false,
  loading: false,
  records: [],
  total: 0,
  query: {
    pageNum: 1,
    pageSize: 10,
    keyword: ''
  }
})

const buildParsedJdJson = () => JSON.stringify(jdResult.value || {})
const buildGapsJson = () => JSON.stringify(matchResult.value?.gaps || [])

const handleParseJd = async () => {
  if (!jdForm.jdText.trim()) {
    ElMessage.warning('请先输入JD内容')
    return
  }
  loading.jd = true
  try {
    const data = await jobBattleApi.parseJd(jdForm)
    jdResult.value = data
    // 重置后续步骤结果，保证串行流程一致
    matchResult.value = null
    planResult.value = null
    reviewResult.value = null
    if (!reviewForm.targetRole) {
      reviewForm.targetRole = jdForm.targetRole || data?.jobTitle || ''
    }
    activeStep.value = 1
    ElMessage.success('JD解析完成，进入简历匹配')
  } catch (e) {
    console.error('JD解析失败', e)
  } finally {
    loading.jd = false
  }
}

const handleMatchResume = async () => {
  if (!jdResult.value) {
    ElMessage.warning('请先完成JD解析')
    activeStep.value = 0
    return
  }
  if (!matchForm.resumeText.trim()) {
    ElMessage.warning('请先填写简历内容')
    return
  }
  loading.match = true
  try {
    const data = await jobBattleApi.matchResume({
      parsedJdJson: buildParsedJdJson(),
      resumeText: matchForm.resumeText,
      projectHighlights: matchForm.projectHighlights,
      targetCompanyType: matchForm.targetCompanyType
    })
    matchResult.value = data
    // 重新匹配后，计划与复盘结果失效
    planResult.value = null
    reviewResult.value = null
    activeStep.value = 2
    ElMessage.success('简历匹配完成，进入计划生成')
  } catch (e) {
    console.error('简历匹配失败', e)
  } finally {
    loading.match = false
  }
}

const handleGeneratePlan = async () => {
  if (!matchResult.value) {
    ElMessage.warning('请先完成简历匹配')
    activeStep.value = 1
    return
  }
  loading.plan = true
  try {
    // 避免上一次复盘结果残留导致用户误判已进入第4步
    reviewResult.value = null
    const data = await jobBattleApi.generatePlan({
      gapsJson: buildGapsJson(),
      targetDays: planForm.targetDays,
      weeklyHours: planForm.weeklyHours,
      preferredLearningMode: planForm.preferredLearningMode,
      nextInterviewDate: planForm.nextInterviewDate
    })
    planResult.value = data
    // 无论任何情况下都停留在第3步，等用户手动进入复盘
    activeStep.value = 2
    ElMessage.success('计划已生成，请确认结果后进入复盘')
  } catch (e) {
    console.error('计划生成失败', e)
  } finally {
    loading.plan = false
  }
}

const goToReviewStep = () => {
  if (!planResult.value) {
    ElMessage.warning('请先生成计划')
    return
  }
  activeStep.value = 3
}

const openPlanHistory = async () => {
  historyState.visible = true
  await fetchPlanHistory()
}

const fetchPlanHistory = async () => {
  historyState.loading = true
  try {
    const data = await jobBattleApi.getPlanHistory(historyState.query)
    historyState.records = data?.records || []
    historyState.total = data?.total || 0
  } catch (e) {
    console.error('获取历史计划失败', e)
  } finally {
    historyState.loading = false
  }
}

const handleHistorySearch = async () => {
  historyState.query.pageNum = 1
  await fetchPlanHistory()
}

const resetHistorySearch = async () => {
  historyState.query.keyword = ''
  historyState.query.pageNum = 1
  await fetchPlanHistory()
}

const handleHistorySizeChange = async () => {
  historyState.query.pageNum = 1
  await fetchPlanHistory()
}

const useHistoryPlan = async (row) => {
  if (!row?.id) {
    ElMessage.warning('历史记录ID不存在')
    return
  }
  historyState.loading = true
  try {
    const detail = await jobBattleApi.getPlanHistoryDetail(row.id)
    const parsedPlan = parsePlanJson(detail?.planResultJson)
    if (!parsedPlan) {
      ElMessage.error('历史计划数据格式异常，无法载入')
      return
    }
    planResult.value = parsedPlan
    planForm.targetDays = detail?.targetDays || planForm.targetDays
    planForm.weeklyHours = detail?.weeklyHours || planForm.weeklyHours
    planForm.preferredLearningMode = detail?.preferredLearningMode || ''
    planForm.nextInterviewDate = detail?.nextInterviewDate || ''
    reviewResult.value = null
    activeStep.value = 2
    historyState.visible = false
    ElMessage.success('历史计划已载入')
  } catch (e) {
    console.error('载入历史计划失败', e)
    ElMessage.error('载入历史计划失败')
  } finally {
    historyState.loading = false
  }
}

const handleReviewInterview = async () => {
  if (!reviewForm.interviewNotes.trim()) {
    ElMessage.warning('请先填写面试记录')
    return
  }
  if (!reviewForm.targetRole.trim()) {
    ElMessage.warning('请先填写目标岗位')
    return
  }
  loading.review = true
  try {
    const data = await jobBattleApi.reviewInterview(reviewForm)
    reviewResult.value = data
    ElMessage.success('复盘结论生成完成')
  } catch (e) {
    console.error('复盘生成失败', e)
  } finally {
    loading.review = false
  }
}

const restartFlow = () => {
  activeStep.value = 0
  jdResult.value = null
  matchResult.value = null
  planResult.value = null
  reviewResult.value = null
  jdForm.jdText = ''
  jdForm.targetRole = ''
  jdForm.targetLevel = ''
  jdForm.city = ''
  matchForm.resumeText = ''
  matchForm.projectHighlights = ''
  matchForm.targetCompanyType = ''
  planForm.targetDays = 30
  planForm.weeklyHours = 6
  planForm.preferredLearningMode = ''
  planForm.nextInterviewDate = ''
  reviewForm.interviewNotes = ''
  reviewForm.qaTranscriptJson = ''
  reviewForm.interviewResult = 'pending'
  reviewForm.targetRole = ''
  reviewForm.nextInterviewDate = ''
  historyState.query.pageNum = 1
  historyState.query.keyword = ''
}

const joinText = (arr, sep = '、') => {
  if (!Array.isArray(arr) || arr.length === 0) {
    return '-'
  }
  return arr.join(sep)
}

const formatJson = (value) => {
  if (value == null) {
    return '-'
  }
  try {
    return JSON.stringify(value, null, 2)
  } catch (e) {
    return String(value)
  }
}

const parsePlanJson = (jsonText) => {
  if (!jsonText) {
    return null
  }
  try {
    return JSON.parse(jsonText)
  } catch (e) {
    return null
  }
}
</script>

<style scoped>
.job-battle-page {
  min-height: calc(100vh - 68px);
}

.page-header {
  margin-bottom: 20px;
  border-radius: 20px;
}

.page-title {
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-subtitle {
  margin: 0;
  opacity: 0.88;
}

.steps-card {
  border-radius: 16px;
  margin-bottom: 16px;
}

.step-card,
.result-card {
  border-radius: 14px;
  margin-bottom: 14px;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.context-box {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #dce8fb;
  background: #f7fbff;
  color: #425673;
  line-height: 1.8;
}

.tip-text {
  color: #1f6feb;
  font-size: 12px;
}

.step-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.plan-preview {
  margin-top: 12px;
}

.history-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.history-table {
  margin-bottom: 12px;
}

.history-pagination {
  display: flex;
  justify-content: flex-end;
}

.result-block {
  font-size: 13px;
  line-height: 1.7;
}

.result-item {
  margin-bottom: 10px;
  color: #46556e;
}

.json-view {
  margin: 8px 0 0;
  padding: 10px;
  border-radius: 8px;
  max-height: 200px;
  overflow: auto;
  border: 1px solid #dce8fb;
  background: #f8fbff;
  color: #33475f;
  white-space: pre-wrap;
  word-break: break-word;
}

.ml-8 {
  margin-left: 8px;
}

@media (max-width: 992px) {
  .history-toolbar {
    flex-wrap: wrap;
  }

  .history-toolbar :deep(.el-input) {
    width: 100%;
  }

  :deep(.el-col) {
    width: 100%;
    max-width: 100%;
    flex: 0 0 100%;
    margin-bottom: 10px;
  }
}
</style>
