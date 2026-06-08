<template>
  <CnPage class="job-battle-page" max-width="1320px" full-height>
    <CnPageHeader
      title="求职作战台"
      description="串行推进 JD 解析、简历匹配、30 天计划和面试复盘，把求职动作沉淀成可执行流程。"
      eyebrow="JOB BATTLE"
      :breadcrumbs="[{ label: '首页', to: '/' }, { label: '求职作战台' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ currentStepName }}</CnStatusTag>
        <CnStatusTag v-if="jdResult" :type="jdResult?.fallback ? 'warning' : 'success'" size="sm" subtle>
          {{ jdResult?.fallback ? 'JD 降级结果' : 'JD AI 结果' }}
        </CnStatusTag>
        <CnStatusTag v-if="planResult" :type="planResult?.fallback ? 'warning' : 'success'" size="sm" subtle>
          {{ planResult?.fallback ? '计划降级结果' : 'AI 计划' }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="openPlanHistory">历史计划</el-button>
        <el-button type="primary" plain @click="goMatchEngine">岗位匹配引擎 2.0</el-button>
        <el-button type="success" plain @click="goCareerLoop">求职闭环中台</el-button>
      </template>
    </CnPageHeader>

    <section class="job-battle-stats" aria-label="求职作战台概览">
      <CnStatCard
        title="当前步骤"
        :value="activeStep + 1"
        unit="/ 4"
        :description="currentStepName"
        tone="brand"
        trend="flat"
        trend-text="流程"
      />
      <CnStatCard
        title="差距项"
        :value="matchResult?.gaps?.length || 0"
        unit="项"
        description="由简历匹配结果识别"
        tone="warning"
        trend="flat"
        trend-text="匹配"
      />
      <CnStatCard
        title="每日任务"
        :value="normalizedDailyTasks.length"
        unit="条"
        description="计划生成后的任务预览"
        tone="success"
        trend="flat"
        trend-text="计划"
      />
      <CnStatCard
        title="复盘状态"
        :value="reviewResult ? '已生成' : '待复盘'"
        description="面试复盘结论"
        :tone="reviewResult ? 'success' : 'neutral'"
        trend="flat"
        trend-text="复盘"
      />
    </section>

      <CnSection class="steps-card" title="作战流程" description="按顺序完成四个步骤，已生成的结果会作为下一步输入。" divided>
        <el-steps :active="activeStep" finish-status="success" align-center>
          <el-step title="JD解析" description="识别岗位要求" />
          <el-step title="简历匹配" description="评估差距项" />
          <el-step title="计划生成" description="自动生成任务" />
          <el-step title="面试复盘" description="沉淀改进动作" />
        </el-steps>
      </CnSection>

      <el-card v-if="activeStep === 0" class="step-card" shadow="never">
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
            <el-button plain @click="openPlanHistory">
              查看历史计划
            </el-button>
            <el-button type="primary" :loading="loading.jd" @click="handleParseJd">
              下一步：解析JD
            </el-button>
          </div>
          <div class="tip-text">可先载入历史计划，系统会自动跳转到步骤3继续执行。</div>
        </el-form>
      </el-card>

      <el-card v-if="activeStep === 1" class="step-card" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤2：简历匹配评估</span>
            <CnStatusTag size="sm" :type="jdResult?.fallback ? 'warning' : 'success'" subtle>
              {{ jdResult?.fallback ? 'JD降级结果' : 'JD AI结果' }}
            </CnStatusTag>
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

      <el-card v-if="activeStep === 2" class="step-card" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤3：生成30天行动计划</span>
            <CnStatusTag
              size="sm"
              :type="planResult ? (planResult.fallback ? 'warning' : 'success') : (matchResult?.fallback ? 'warning' : 'success')"
              subtle
            >
              {{ planResult ? (planResult.fallback ? '计划降级结果' : '计划 AI结果') : (matchResult?.fallback ? '匹配降级结果' : '匹配 AI结果') }}
            </CnStatusTag>
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
            <el-button plain type="primary" @click="goMatchEngine">
              岗位匹配引擎 2.0
            </el-button>
            <el-button
              type="warning"
              plain
              :disabled="!planResult"
              :loading="loading.loopSync"
              @click="syncToCareerLoop"
            >
              同步到闭环
            </el-button>
            <el-button type="info" plain @click="goCareerLoop">
              查看闭环进度
            </el-button>
            <el-button type="success" plain :disabled="!planResult" @click="goToReviewStep">
              下一步：进入复盘
            </el-button>
          </div>
        </el-form>

        <div v-if="planResult" class="plan-preview">
          <el-divider>计划生成结果</el-divider>

          <div class="plan-summary-grid">
            <el-card shadow="never" class="plan-summary-card">
              <div class="summary-label">计划名称</div>
              <div class="summary-value">{{ planResult.planName || '-' }}</div>
            </el-card>
            <el-card shadow="never" class="plan-summary-card">
              <div class="summary-label">总天数</div>
              <div class="summary-value">{{ planResult.totalDays || '-' }} 天</div>
            </el-card>
            <el-card shadow="never" class="plan-summary-card">
              <div class="summary-label">每周投入</div>
              <div class="summary-value">{{ planForm.weeklyHours || '-' }} h</div>
            </el-card>
            <el-card shadow="never" class="plan-summary-card">
              <div class="summary-label">计划类型</div>
              <div class="summary-value">
                <CnStatusTag :type="planResult.fallback ? 'warning' : 'success'">
                  {{ planResult.fallback ? '降级计划' : 'AI计划' }}
                </CnStatusTag>
              </div>
            </el-card>
          </div>

          <div class="plan-main-grid">
            <el-card shadow="never" class="plan-section-card">
              <template #header>
                <div class="section-title">周目标卡片</div>
              </template>
              <div v-if="normalizedWeeklyGoals.length" class="weekly-goals-grid">
                <div
                  v-for="(week, index) in normalizedWeeklyGoals"
                  :key="`week-${index}`"
                  class="weekly-goal-card"
                >
                  <div class="weekly-goal-head">
                    <CnStatusTag size="sm" type="brand">Week {{ week.weekNum || index + 1 }}</CnStatusTag>
                  </div>
                  <div class="weekly-goal-text">{{ week.goal || '未提供周目标内容' }}</div>
                  <div v-if="week.focusGaps.length" class="weekly-gap-list">
                    <CnStatusTag
                      v-for="(gap, gapIndex) in week.focusGaps"
                      :key="`gap-${index}-${gapIndex}`"
                      size="sm"
                      type="warning"
                      subtle
                    >
                      {{ gap }}
                    </CnStatusTag>
                  </div>
                </div>
              </div>
              <CnEmptyState v-else title="暂无周目标数据" description="生成计划后会展示每周聚焦目标。" size="sm" surface="plain" />
            </el-card>

            <el-card shadow="never" class="plan-section-card">
              <template #header>
                <div class="section-title">里程碑时间线</div>
              </template>
              <el-timeline v-if="normalizedMilestones.length">
                <el-timeline-item
                  v-for="(milestone, index) in normalizedMilestones"
                  :key="`milestone-${index}`"
                  :timestamp="`Day ${milestone.day ?? '-'}`"
                  :type="index === normalizedMilestones.length - 1 ? 'success' : 'primary'"
                >
                  <div class="milestone-goal">{{ milestone.goal || '未提供里程碑目标' }}</div>
                </el-timeline-item>
              </el-timeline>
              <CnEmptyState v-else title="暂无里程碑数据" description="计划结果中暂未包含里程碑。" size="sm" surface="plain" />
            </el-card>
          </div>

          <el-card shadow="never" class="plan-section-card">
            <template #header>
              <div class="section-title">每日任务（预览）</div>
            </template>
            <el-table :data="normalizedDailyTasks" stripe border class="daily-task-table" max-height="360">
              <el-table-column prop="day" label="天数" width="72">
                <template #default="{ row }">Day {{ row.day ?? '-' }}</template>
              </el-table-column>
              <el-table-column prop="taskType" label="类型" width="108">
                <template #default="{ row }">
                  <CnStatusTag size="sm" type="info" subtle>{{ taskTypeLabel(row.taskType) }}</CnStatusTag>
                </template>
              </el-table-column>
              <el-table-column prop="task" label="任务" min-width="280" show-overflow-tooltip />
              <el-table-column prop="durationMinutes" label="时长" width="96">
                <template #default="{ row }">{{ row.durationMinutes || '-' }} min</template>
              </el-table-column>
              <el-table-column prop="deliverable" label="交付物" min-width="240" show-overflow-tooltip />
            </el-table>
            <CnEmptyState
              v-if="!normalizedDailyTasks.length"
              title="暂无每日任务数据"
              description="生成计划后会展示每日任务预览。"
              size="sm"
              surface="plain"
            />
          </el-card>

          <el-card shadow="never" class="plan-section-card">
            <template #header>
              <div class="section-title">风险与兜底</div>
            </template>
            <div v-if="normalizedRiskItems.length" class="risk-list">
              <div
                v-for="(risk, index) in normalizedRiskItems"
                :key="`risk-${index}`"
                class="risk-item"
              >
                <div class="risk-item-title">
                  <CnStatusTag type="danger" size="sm">风险 {{ index + 1 }}</CnStatusTag>
                  <span>{{ risk.risk || '未提供风险描述' }}</span>
                </div>
                <div class="risk-item-fallback">兜底：{{ risk.fallback || '未提供兜底方案' }}</div>
              </div>
            </div>
            <CnEmptyState v-else title="暂无风险与兜底数据" description="当前计划没有明显风险项。" size="sm" surface="plain" />
          </el-card>
        </div>
      </el-card>

      <el-card v-if="activeStep === 3" class="step-card" shadow="never">
        <template #header>
          <div class="step-header">
            <span>步骤4：面试复盘总结</span>
            <CnStatusTag v-if="planResult" size="sm" :type="planResult.fallback ? 'warning' : 'success'" subtle>
              {{ planResult.fallback ? '计划降级结果' : '计划 AI结果' }}
            </CnStatusTag>
          </div>
        </template>
        <div class="context-box">
          <div><b>计划名：</b>{{ planResult?.planName || '-' }}</div>
          <div><b>总天数：</b>{{ planResult?.totalDays || '-' }}</div>
          <div><b>周目标：</b>{{ joinText(planWeeklyGoalTexts, ' | ') }}</div>
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

      <el-card v-if="reviewResult" class="result-card" shadow="never">
        <template #header>
          最终复盘结果
          <CnStatusTag size="sm" :type="reviewResult.fallback ? 'warning' : 'success'" class="ml-8" subtle>
            {{ reviewResult.fallback ? '复盘降级结果' : '复盘 AI结果' }}
          </CnStatusTag>
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
              <CnStatusTag size="sm" :type="row.fallback ? 'warning' : 'success'" subtle>
                {{ row.fallback ? '降级' : 'AI' }}
              </CnStatusTag>
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
  </CnPage>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { jobBattleApi } from '@/api/jobBattle'
import { careerLoopApi } from '@/api/careerLoop'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'

const route = useRoute()
const router = useRouter()

const activeStep = ref(0)
const loading = reactive({
  jd: false,
  match: false,
  plan: false,
  review: false,
  loopSync: false
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

const TASK_TYPE_LABEL_MAP = {
  learn: '学习',
  practice: '练习',
  project: '项目',
  mock: '模拟面试',
  review: '复盘'
}

const LOOP_STAGE_ORDER = {
  INIT: 0,
  JD_PARSED: 1,
  RESUME_MATCHED: 2,
  PLAN_READY: 3,
  PLAN_EXECUTING: 4,
  INTERVIEW_DONE: 5,
  REVIEWED: 6
}
const STEP_NAME_MAP = ['步骤1：JD解析', '步骤2：简历匹配', '步骤3：计划生成', '步骤4：面试复盘']
const routeStepHintKey = ref('')
const currentStepName = computed(() => STEP_NAME_MAP[activeStep.value] || '步骤1：JD解析')

const taskTypeLabel = (taskType) => {
  if (!taskType) {
    return '未分类'
  }
  return TASK_TYPE_LABEL_MAP[taskType] || taskType
}

const tryParseJson = (value) => {
  if (typeof value !== 'string') {
    return null
  }
  const text = value.trim()
  if (!text || (!text.startsWith('{') && !text.startsWith('['))) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch (e) {
    return null
  }
}

const normalizeToArray = (value) => {
  if (Array.isArray(value)) {
    return value
  }
  if (value == null) {
    return []
  }
  if (typeof value === 'string') {
    const parsed = tryParseJson(value)
    if (Array.isArray(parsed)) {
      return parsed
    }
    return value
      .split(/\n+/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  return [value]
}

const normalizeTextList = (value) => {
  if (Array.isArray(value)) {
    return value.map(item => (item == null ? '' : String(item).trim())).filter(Boolean)
  }
  if (typeof value === 'string') {
    return value
      .split(/[|；;、,，]/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  return []
}

const normalizeWeeklyGoalItem = (item, index) => {
  let source = item
  if (typeof source === 'string') {
    const parsed = tryParseJson(source)
    source = parsed || source
  }

  if (source && typeof source === 'object' && !Array.isArray(source)) {
    return {
      weekNum: Number(source.weekNum || source.week || source.weekNo || index + 1),
      goal: source.goal || source.target || source.title || source.content || '',
      focusGaps: normalizeTextList(source.focusGaps || source.gaps || source.focus || source.keywords)
    }
  }

  return {
    weekNum: index + 1,
    goal: source == null ? '' : String(source),
    focusGaps: []
  }
}

const normalizeMilestoneItem = (item) => {
  let source = item
  if (typeof source === 'string') {
    const parsed = tryParseJson(source)
    source = parsed || source
  }

  if (source && typeof source === 'object' && !Array.isArray(source)) {
    return {
      day: Number(source.day || source.milestoneDay || source.dueDay || 0) || null,
      goal: source.goal || source.target || source.content || ''
    }
  }

  return {
    day: null,
    goal: source == null ? '' : String(source)
  }
}

const normalizeDailyTaskItem = (item) => {
  let source = item
  if (typeof source === 'string') {
    const parsed = tryParseJson(source)
    source = parsed || source
  }

  if (source && typeof source === 'object' && !Array.isArray(source)) {
    return {
      day: Number(source.day || source.taskDay || 0) || null,
      task: source.task || source.title || source.content || '',
      durationMinutes: Number(source.durationMinutes || source.duration || 0) || null,
      taskType: source.taskType || source.type || '',
      deliverable: source.deliverable || source.output || source.result || ''
    }
  }

  return {
    day: null,
    task: source == null ? '' : String(source),
    durationMinutes: null,
    taskType: '',
    deliverable: ''
  }
}

const normalizeRiskItem = (item) => {
  let source = item
  if (typeof source === 'string') {
    const parsed = tryParseJson(source)
    source = parsed || source
  }

  if (source && typeof source === 'object' && !Array.isArray(source)) {
    return {
      risk: source.risk || source.issue || source.problem || '',
      fallback: source.fallback || source.solution || source.action || ''
    }
  }

  return {
    risk: source == null ? '' : String(source),
    fallback: ''
  }
}

const normalizedWeeklyGoals = computed(() => {
  const raw = normalizeToArray(planResult.value?.weeklyGoals)
  return raw
    .map((item, index) => normalizeWeeklyGoalItem(item, index))
    .filter(item => item.goal || item.focusGaps.length)
})

const planWeeklyGoalTexts = computed(() => normalizedWeeklyGoals.value.map(item => item.goal).filter(Boolean))

const normalizedMilestones = computed(() => {
  const raw = normalizeToArray(planResult.value?.milestones)
  return raw
    .map(item => normalizeMilestoneItem(item))
    .filter(item => item.day != null || item.goal)
    .sort((a, b) => (a.day || 0) - (b.day || 0))
})

const normalizedDailyTasks = computed(() => {
  const raw = normalizeToArray(planResult.value?.dailyTasks)
  return raw
    .map(item => normalizeDailyTaskItem(item))
    .filter(item => item.task || item.day != null)
    .sort((a, b) => (a.day || 0) - (b.day || 0))
})

const normalizedRiskItems = computed(() => {
  const raw = normalizeToArray(planResult.value?.riskAndFallback)
  return raw
    .map(item => normalizeRiskItem(item))
    .filter(item => item.risk || item.fallback)
})

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

const buildLoopSyncPayload = (currentStage) => {
  const riskFlags = normalizedRiskItems.value
    .map(item => item.risk)
    .filter(Boolean)
    .slice(0, 6)
  const nextSuggestions = normalizedDailyTasks.value
    .map(item => item.task)
    .filter(Boolean)
    .slice(0, 6)

  const payload = {
    source: 'job_battle',
    note: `同步行动计划：${planResult.value?.planName || '30天行动计划'}`,
    planProgress: 10,
    riskFlags,
    nextSuggestions
  }

  const stageOrder = LOOP_STAGE_ORDER[currentStage] ?? 0
  if (stageOrder <= LOOP_STAGE_ORDER.PLAN_READY) {
    payload.targetStage = 'PLAN_READY'
  }
  return payload
}

const syncToCareerLoop = async () => {
  if (!planResult.value) {
    ElMessage.warning('请先生成计划后再同步闭环')
    return
  }
  loading.loopSync = true
  try {
    let currentStage = 'INIT'
    try {
      const current = await careerLoopApi.getCurrent()
      currentStage = current?.session?.currentStage || 'INIT'
    } catch (e) {
      // 忽略阶段探测失败，按默认阶段尝试同步
    }
    await careerLoopApi.sync(buildLoopSyncPayload(currentStage))
    ElMessage.success('已同步到求职闭环中台')
  } catch (e) {
    console.error('同步闭环失败', e)
    ElMessage.error('同步闭环失败')
  } finally {
    loading.loopSync = false
  }
}

const goCareerLoop = () => {
  router.push('/career-loop')
}

const goMatchEngine = () => {
  router.push('/job-match-engine')
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

const applyRouteStepIntent = () => {
  const stepQuery = route.query.step
  if (stepQuery == null || stepQuery === '') {
    return
  }
  const step = Number(stepQuery)
  if (!Number.isInteger(step) || step < 0 || step > 3) {
    return
  }
  activeStep.value = step
  if (route.query.from === 'career-loop') {
    const currentHintKey = `${step}-${String(route.query.action || '')}`
    if (routeStepHintKey.value !== currentHintKey) {
      routeStepHintKey.value = currentHintKey
      ElMessage.info(`已从求职闭环中台跳转到${STEP_NAME_MAP[step] || '作战台'}`)
    }
  }
}

watch(
  () => [route.query.step, route.query.from, route.query.action],
  () => {
    applyRouteStepIntent()
  },
  { immediate: true }
)
</script>

<style scoped>
.job-battle-page {
  min-height: calc(100vh - 68px);
}

.job-battle-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-4);
}

.steps-card {
  margin-bottom: var(--cn-space-4);
}

.step-card,
.result-card {
  margin-bottom: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.context-box {
  margin-bottom: var(--cn-space-4);
  padding: var(--cn-space-3) var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  line-height: 1.8;
}

.tip-text {
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  line-height: 1.6;
}

.step-actions {
  margin-top: var(--cn-space-3);
  display: flex;
  gap: var(--cn-space-2);
  flex-wrap: wrap;
}

.plan-preview {
  margin-top: var(--cn-space-3);
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.plan-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.plan-summary-card {
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-card-bg);
}

.summary-label {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  margin-bottom: var(--cn-space-1);
}

.summary-value {
  font-size: 16px;
  color: var(--cn-color-text-primary);
  font-weight: 600;
  line-height: 1.4;
}

.plan-main-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: var(--cn-space-3);
}

.plan-section-card {
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-card-bg);
}

.section-title {
  font-weight: 600;
  color: var(--cn-color-text-primary);
}

.weekly-goals-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.weekly-goal-card {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  padding: var(--cn-space-3);
  background: var(--cn-color-bg-surface-muted);
}

.weekly-goal-head {
  margin-bottom: var(--cn-space-2);
}

.weekly-goal-text {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.weekly-gap-list {
  margin-top: var(--cn-space-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.milestone-goal {
  color: var(--cn-color-text-secondary);
  line-height: 1.6;
}

.daily-task-table {
  margin-bottom: var(--cn-space-2);
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.risk-item {
  border: 1px solid color-mix(in srgb, var(--cn-color-danger) 24%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-card);
  padding: var(--cn-space-3);
  background: var(--cn-color-danger-soft);
}

.risk-item-title {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-danger);
  margin-bottom: var(--cn-space-1);
}

.risk-item-fallback {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.history-toolbar {
  display: flex;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.history-table {
  margin-bottom: var(--cn-space-3);
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
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-text-secondary);
}

.json-view {
  margin: var(--cn-space-2) 0 0;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-control);
  max-height: 200px;
  overflow: auto;
  border: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.ml-8 {
  margin-left: 8px;
}

@media (max-width: 992px) {
  .job-battle-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .plan-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .plan-main-grid {
    grid-template-columns: 1fr;
  }

  .weekly-goals-grid {
    grid-template-columns: 1fr;
  }

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

@media (max-width: 640px) {
  .job-battle-stats,
  .plan-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
