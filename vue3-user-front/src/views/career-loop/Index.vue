<template>
  <CnPage class="career-loop-page" max-width="1280px" full-height>
    <CnPageHeader
      title="求职闭环中台"
      description="把 JD 解析、计划执行、模拟面试和复盘统一到一条可追踪主线。"
      eyebrow="CAREER LOOP"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ current.session.currentStageLabel }}</CnStatusTag>
        <CnStatusTag :type="healthTone" size="sm" subtle>健康分 {{ current.session.healthScore }}</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>{{ syncStatusText }}</CnStatusTag>
      </template>

      <template #actions>
        <el-tooltip content="异常恢复" placement="bottom">
          <el-dropdown trigger="click" @command="handleRecoveryCommand">
            <el-button circle :icon="MoreFilled" :loading="loading.recovery" aria-label="异常恢复" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="reload">重新加载数据</el-dropdown-item>
                <el-dropdown-item command="sync" divided>恢复同步</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-tooltip>
        <el-button type="primary" :icon="Connection" :loading="loading.start" @click="handleStart">
          开始求职准备
        </el-button>
        <el-button plain :icon="Plus" @click="openApplicationDialog()">
          记录投递
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection class="career-actions-section" title="下一步行动" description="从当前阶段开始执行，完成后会同步刷新求职进度。" divided>
      <div v-loading="loading.main" class="career-action-list" aria-live="polite">
        <article v-for="action in actions" :key="action.id" class="career-action-item">
          <div class="career-action-copy">
            <div class="career-action-meta">
              <CnStatusTag type="brand" size="sm" subtle>{{ mapStageLabel(action.stage) }}</CnStatusTag>
              <CnStatusTag :type="mapActionStatusTone(action.status)" size="sm">
                {{ mapActionStatusLabel(action.status) }}
              </CnStatusTag>
            </div>
            <h2>{{ action.title || '待推进动作' }}</h2>
            <p>{{ action.description || '完成该动作后，系统会更新下一步建议。' }}</p>
          </div>

          <div class="career-action-controls">
            <el-button size="small" type="primary" plain @click="goByActionType(action.actionType)">
              {{ action.actionType === 'offer' ? '记录跟踪' : '去执行' }}
            </el-button>
            <el-button size="small" type="success" :disabled="action.status === 'done'" @click="handleDoneAction(action)">
              标记完成
            </el-button>
          </div>
        </article>

        <CnEmptyState
          v-if="!loading.main && actions.length === 0"
          title="暂无下一步行动"
          description="开始求职准备后会生成行动清单。"
          icon="AC"
          size="sm"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" :icon="Connection" :loading="loading.start" @click="handleStart">
              开始求职准备
            </el-button>
          </template>
        </CnEmptyState>
      </div>
    </CnSection>

    <CnSection class="application-tracking-section" title="投递跟踪" description="记录真实投递、面试和结果状态；仅对你本人可见。" divided>
      <template #actions>
        <el-button type="primary" size="small" :icon="Plus" @click="openApplicationDialog()">
          新增记录
        </el-button>
      </template>

      <div v-loading="loading.applications" class="application-tracking-content">
        <div v-if="applicationSummary.totalCount" class="application-summary">
          <CnStatusTag type="brand" size="sm">进行中 {{ applicationSummary.activeCount || 0 }}</CnStatusTag>
          <CnStatusTag v-if="applicationSummary.interviewingCount" type="warning" size="sm" subtle>
            面试中 {{ applicationSummary.interviewingCount }}
          </CnStatusTag>
          <CnStatusTag v-if="applicationSummary.offerCount" type="success" size="sm" subtle>
            Offer {{ applicationSummary.offerCount }}
          </CnStatusTag>
          <CnStatusTag v-if="applicationSummary.dueFollowUpCount" type="danger" size="sm" subtle>
            待跟进 {{ applicationSummary.dueFollowUpCount }}
          </CnStatusTag>
          <span v-if="applicationSummary.nextFollowUpDate">最近跟进 {{ applicationSummary.nextFollowUpDate }}</span>
        </div>

        <div v-if="applications.length" class="application-list">
          <article v-for="item in applications" :key="item.id" class="application-item">
            <div class="application-item-copy">
              <div class="application-item-meta">
                <CnStatusTag :type="applicationStatusTone(item.status)" size="sm">{{ item.statusLabel || applicationStatusLabel(item.status) }}</CnStatusTag>
                <span v-if="item.appliedDate">投递于 {{ item.appliedDate }}</span>
                <span v-if="item.nextFollowUpDate">下次跟进 {{ item.nextFollowUpDate }}</span>
              </div>
              <div v-if="item.matchRecordId || item.planRecordId || item.mockInterviewSessionId" class="application-source-meta">
                <CnStatusTag v-if="item.matchRecordId" type="brand" size="sm" subtle>岗位匹配 #{{ item.matchRecordId }}</CnStatusTag>
                <CnStatusTag v-if="item.planRecordId" type="info" size="sm" subtle>补短板计划 #{{ item.planRecordId }}</CnStatusTag>
                <CnStatusTag v-if="item.mockInterviewSessionId" type="success" size="sm" subtle>模拟面试 #{{ item.mockInterviewSessionId }}</CnStatusTag>
              </div>
              <h3>{{ item.positionName || '未命名岗位' }}</h3>
              <p>{{ item.companyName || '未填写公司' }}</p>
              <small v-if="item.note">{{ item.note }}</small>
            </div>
            <div class="application-item-actions">
              <el-tooltip content="编辑投递记录" placement="top">
                <el-button circle text :icon="EditPen" aria-label="编辑投递记录" @click="openApplicationDialog(item)" />
              </el-tooltip>
              <el-tooltip content="删除投递记录" placement="top">
                <el-button
                  circle
                  text
                  type="danger"
                  :icon="Delete"
                  :loading="loading.applicationDeleteId === item.id"
                  aria-label="删除投递记录"
                  @click="handleDeleteApplication(item)"
                />
              </el-tooltip>
            </div>
          </article>
        </div>
        <CnEmptyState
          v-else-if="!loading.applications"
          title="还没有投递记录"
          description="从第一条真实投递开始，持续维护面试与结果进展。"
          icon="JB"
          size="sm"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" size="small" :icon="Plus" @click="openApplicationDialog()">记录投递</el-button>
          </template>
        </CnEmptyState>
      </div>
    </CnSection>

    <div class="summary-grid">
      <CnStatCard
        title="当前阶段"
        :value="current.session.currentStageLabel"
        description="当前闭环推进位置"
        tone="brand"
        :loading="loading.main"
      />
      <CnStatCard
        title="阶段进度"
        :value="current.session.stagePercent"
        unit="%"
        description="按闭环阶段映射的完成度"
        tone="success"
        :loading="loading.main"
      />
      <CnStatCard
        title="健康分"
        :value="current.session.healthScore"
        description="节奏、风险和状态稳定性"
        :tone="healthTone"
        :loading="loading.main"
      />
      <CnStatCard
        title="计划进度"
        :value="current.snapshot.planProgress"
        unit="%"
        description="学习计划执行推进"
        tone="info"
        :loading="loading.main"
      />
    </div>

    <div class="insight-grid">
      <CnSection title="阶段能力雷达" description="用当前阶段、动作完成度、面试和复盘数据估算能力结构。" divided>
        <div class="radar-wrap">
          <svg class="radar-svg" viewBox="0 0 320 320" role="img" aria-label="求职闭环阶段能力雷达图">
            <polygon
              v-for="(polygon, index) in radarGridPolygons"
              :key="`grid-${index}`"
              :points="pointsToString(polygon)"
              class="radar-grid"
            />
            <line
              v-for="(axis, index) in radarAxes"
              :key="`axis-${index}`"
              :x1="radarCenter.x"
              :y1="radarCenter.y"
              :x2="axis.point.x"
              :y2="axis.point.y"
              class="radar-axis"
            />
            <polygon :points="pointsToString(radarValuePolygon)" class="radar-value" />
            <circle
              v-for="(point, index) in radarValuePolygon"
              :key="`point-${index}`"
              :cx="point.x"
              :cy="point.y"
              r="3.5"
              class="radar-point"
            />
            <text
              v-for="(axis, index) in radarAxes"
              :key="`label-${index}`"
              :x="axis.labelPoint.x"
              :y="axis.labelPoint.y"
              class="radar-label"
            >
              {{ axis.label }}
            </text>
          </svg>

          <div class="radar-legend">
            <div v-for="item in radarIndicators" :key="item.key" class="radar-legend-item">
              <span>{{ item.label }}</span>
              <b>{{ item.value }}</b>
            </div>
          </div>
        </div>
      </CnSection>

      <CnSection title="近 4 周进度热力图" description="根据阶段推进、已完成动作、计划和复盘数据估算近期活跃度。" divided>
        <div class="heatmap-head">
          <span v-for="day in heatmapDayLabels" :key="day">{{ day }}</span>
        </div>
        <div class="heatmap-grid">
          <div
            v-for="cell in heatmapCells"
            :key="cell.dateKey"
            class="heatmap-cell"
            :class="[`level-${cell.level}`, { today: cell.isToday }]"
            :title="`${cell.dateLabel} 活跃度 ${cell.level}/4`"
          />
        </div>
        <div class="heatmap-legend">
          <span>低</span>
          <span class="legend-dot level-1" />
          <span class="legend-dot level-2" />
          <span class="legend-dot level-3" />
          <span class="legend-dot level-4" />
          <span>高</span>
        </div>
      </CnSection>
    </div>

    <div class="content-grid">
      <CnSection title="阶段时间线" description="记录闭环阶段推进来源、时间和备注。" divided>
        <el-timeline v-if="timeline.length">
          <el-timeline-item
            v-for="item in timeline"
            :key="item.id"
            :timestamp="item.createTime || ''"
            type="primary"
          >
            <div class="timeline-title">{{ mapStageLabel(item.fromStage) }} -> {{ mapStageLabel(item.toStage) }}</div>
            <div class="timeline-meta">
              来源：{{ item.triggerSource || '-' }}{{ item.triggerRefId ? ` / ${item.triggerRefId}` : '' }}
            </div>
            <div class="timeline-note">{{ item.note || '无备注' }}</div>
          </el-timeline-item>
        </el-timeline>
        <CnEmptyState v-else title="暂无阶段推进记录" description="完成动作后会在这里沉淀时间线。" icon="TL" />
      </CnSection>

      <CnSection title="风险与建议" description="根据闭环快照展示当前风险项和下一步行动建议。" divided>
        <div class="section-block">
          <div class="section-subtitle">风险项</div>
          <div v-if="current.riskFlags.length" class="tag-list">
            <CnStatusTag
              v-for="(risk, index) in current.riskFlags"
              :key="`risk-${index}`"
              type="danger"
              size="sm"
              subtle
            >
              {{ risk }}
            </CnStatusTag>
          </div>
          <CnEmptyState v-else title="暂无风险项" description="当前闭环状态较稳定。" icon="OK" size="sm" />
        </div>

        <div class="section-block">
          <div class="section-subtitle">下一步建议</div>
          <div v-if="current.nextSuggestions.length" class="suggestion-list">
            <div v-for="(item, index) in current.nextSuggestions" :key="`suggestion-${index}`" class="suggestion-item">
              <span>{{ index + 1 }}</span>
              <p>{{ item }}</p>
            </div>
          </div>
          <CnEmptyState v-else title="暂无建议" description="完成更多动作后会生成新的建议。" icon="NX" size="sm" />
        </div>
      </CnSection>
    </div>

    <el-dialog
      v-model="applicationDialogVisible"
      :title="editingApplicationId ? '更新投递进展' : '记录投递进展'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="applicationFormRef" :model="applicationForm" :rules="applicationRules" label-position="top">
        <div class="application-form-grid">
          <el-form-item label="公司名称" prop="companyName">
            <el-input v-model.trim="applicationForm.companyName" maxlength="120" show-word-limit />
          </el-form-item>
          <el-form-item label="岗位名称" prop="positionName">
            <el-input v-model.trim="applicationForm.positionName" maxlength="120" show-word-limit />
          </el-form-item>
        </div>
        <div class="application-form-grid">
          <el-form-item label="当前状态" prop="status">
            <el-select v-model="applicationForm.status" class="full-width-control">
              <el-option v-for="item in applicationStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="投递日期" prop="appliedDate" :required="applicationForm.status !== 'PREPARING'">
            <el-date-picker
              v-model="applicationForm.appliedDate"
              class="full-width-control"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
        </div>
        <el-form-item v-if="!isTerminalApplicationStatus(applicationForm.status)" label="下一次跟进日期">
          <el-date-picker
            v-model="applicationForm.nextFollowUpDate"
            class="full-width-control"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="可选"
          />
        </el-form-item>
        <div class="application-source-grid">
          <el-form-item label="岗位匹配来源">
            <el-select
              v-model="applicationForm.matchRecordId"
              class="full-width-control"
              clearable
              filterable
              :loading="loading.applicationSources"
              placeholder="可选"
            >
              <el-option v-for="item in matchRecordOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="补短板计划来源">
            <el-select
              v-model="applicationForm.planRecordId"
              class="full-width-control"
              clearable
              filterable
              :loading="loading.applicationSources"
              placeholder="可选"
            >
              <el-option v-for="item in planRecordOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="模拟面试来源">
            <el-select
              v-model="applicationForm.mockInterviewSessionId"
              class="full-width-control"
              clearable
              filterable
              :loading="loading.applicationSources"
              placeholder="可选"
            >
              <el-option v-for="item in mockInterviewOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="note">
          <el-input v-model.trim="applicationForm.note" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applicationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading.applicationSave" @click="handleSaveApplication">保存</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Connection, Delete, EditPen, MoreFilled, Plus } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnTone
} from '@/design-system'
import { careerLoopApi } from '@/api/careerLoop'
import { jobBattleApi } from '@/api/jobBattle'
import { mockInterviewApi } from '@/api/mockInterview'
import {
  adaptCareerLoopCurrent,
  mapActionStatusLabel,
  mapStageLabel
} from '@/utils/career-loop-adapter'

interface CareerLoopSession extends Record<string, unknown> {
  currentStage?: string
  currentStageLabel: string
  stagePercent: number
  healthScore: number
  targetRole?: string
  targetCompanyType?: string
}

interface CareerLoopSnapshot extends Record<string, unknown> {
  planProgress: number
  mockCount?: number
  latestMockScore?: number | null
  reviewCount?: number
}

interface CareerLoopAction extends Record<string, unknown> {
  id: number | string
  title?: string
  description?: string
  stage?: string
  status?: string
  actionType?: string
  updateTime?: string
}

interface CareerApplicationRecord {
  id: number | string
  matchRecordId?: number | string | null
  planRecordId?: number | string | null
  mockInterviewSessionId?: number | string | null
  companyName?: string
  positionName?: string
  status?: string
  statusLabel?: string
  appliedDate?: string
  nextFollowUpDate?: string
  note?: string
}

interface CareerApplicationSummary {
  totalCount?: number
  activeCount?: number
  interviewingCount?: number
  offerCount?: number
  dueFollowUpCount?: number
  nextFollowUpDate?: string
}

interface CareerApplicationForm {
  matchRecordId: number | string | null
  planRecordId: number | string | null
  mockInterviewSessionId: number | string | null
  companyName: string
  positionName: string
  status: string
  appliedDate: string
  nextFollowUpDate: string
  note: string
}

interface CareerLoopTimelineItem {
  id: number | string
  fromStage?: string
  toStage?: string
  triggerSource?: string
  triggerRefId?: number | string
  note?: string
  createTime?: string
}

interface SourceOption {
  value: number | string
  label: string
}

interface CareerLoopCurrent {
  session: CareerLoopSession
  snapshot: CareerLoopSnapshot
  actions: CareerLoopAction[]
  riskFlags: string[]
  nextSuggestions: string[]
}

interface RadarPoint {
  x: number
  y: number
}

interface RadarIndicator {
  key: string
  label: string
  value: number
}

interface RadarAxis extends RadarIndicator {
  point: RadarPoint
  labelPoint: RadarPoint
  angle: number
}

interface HeatmapCell {
  dateKey: string
  dateLabel: string
  level: number
  isToday: boolean
}

const router = useRouter()
const route = useRoute()

const loading = reactive({
  main: false,
  start: false,
  recovery: false,
  applications: false,
  applicationSources: false,
  applicationSave: false,
  applicationDeleteId: null as number | string | null
})

const current = ref<CareerLoopCurrent>(adaptCareerLoopCurrent() as CareerLoopCurrent)
const timeline = ref<CareerLoopTimelineItem[]>([])
const actions = ref<CareerLoopAction[]>([])
const applications = ref<CareerApplicationRecord[]>([])
const applicationSummary = ref<CareerApplicationSummary>({})
const matchRecordOptions = ref<SourceOption[]>([])
const planRecordOptions = ref<SourceOption[]>([])
const mockInterviewOptions = ref<SourceOption[]>([])
const applicationDialogVisible = ref(false)
const editingApplicationId = ref<number | string | null>(null)
const applicationFormRef = ref<FormInstance>()
const applicationForm = reactive<CareerApplicationForm>({
  matchRecordId: null,
  planRecordId: null,
  mockInterviewSessionId: null,
  companyName: '',
  positionName: '',
  status: 'APPLIED',
  appliedDate: '',
  nextFollowUpDate: '',
  note: ''
})
const applicationStatusOptions = [
  { value: 'PREPARING', label: '准备投递' },
  { value: 'APPLIED', label: '已投递' },
  { value: 'INTERVIEWING', label: '面试中' },
  { value: 'OFFER', label: '收到 Offer' },
  { value: 'REJECTED', label: '未通过' },
  { value: 'WITHDRAWN', label: '已撤回' }
]
const applicationRules: FormRules<CareerApplicationForm> = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  positionName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择当前状态', trigger: 'change' }],
  appliedDate: [{
    validator: (_rule, value, callback) => {
      if (applicationForm.status !== 'PREPARING' && !value) {
        callback(new Error('请填写投递日期'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}
const lastSyncedAt = ref<Date | null>(null)
const heatmapDayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const radarCenter = { x: 160, y: 160 }
const radarRadius = 104

const healthTone = computed<CnTone>(() => {
  const score = Number(current.value.session.healthScore || 0)
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
})

const syncStatusText = computed(() => {
  if (!lastSyncedAt.value) {
    return '自动同步已开启'
  }
  return `自动同步 · ${lastSyncedAt.value.toLocaleTimeString('zh-CN', { hour12: false })}`
})

const fetchAll = async () => {
  loading.main = true
  loading.applications = true
  try {
    const [currentData, timelineData, actionData, applicationData, applicationSummaryData] = await Promise.all([
      careerLoopApi.getCurrent(),
      careerLoopApi.getTimeline(),
      careerLoopApi.getActions(),
      careerLoopApi.getApplications(),
      careerLoopApi.getApplicationSummary()
    ])
    current.value = adaptCareerLoopCurrent(currentData || {}) as CareerLoopCurrent
    timeline.value = Array.isArray(timelineData) ? timelineData : []
    actions.value = Array.isArray(actionData) ? actionData : []
    applications.value = Array.isArray(applicationData) ? applicationData : []
    applicationSummary.value = applicationSummaryData || {}
    lastSyncedAt.value = new Date()
  } catch (e) {
    console.error('加载求职闭环数据失败', e)
    ElMessage.error('加载求职闭环数据失败')
  } finally {
    loading.main = false
    loading.applications = false
  }
}

const resetApplicationForm = () => {
  editingApplicationId.value = null
  Object.assign(applicationForm, {
    matchRecordId: null,
    planRecordId: null,
    mockInterviewSessionId: null,
    companyName: '',
    positionName: '',
    status: 'APPLIED',
    appliedDate: '',
    nextFollowUpDate: '',
    note: ''
  })
  applicationFormRef.value?.clearValidate()
}

const loadApplicationSources = async () => {
  loading.applicationSources = true
  try {
    const results = await Promise.allSettled([
      jobBattleApi.getMatchEngineHistory({ pageNum: 1, pageSize: 50 }),
      jobBattleApi.getPlanHistory({ pageNum: 1, pageSize: 50 }),
      mockInterviewApi.getHistory({ pageNum: 1, pageSize: 50 })
    ])
    const records = (result: PromiseSettledResult<any>): any[] => result.status === 'fulfilled' && Array.isArray(result.value?.records)
      ? result.value.records
      : []
    matchRecordOptions.value = records(results[0]).map((item) => ({
      value: item.id,
      label: `${item.analysisName || item.bestTargetRole || '岗位匹配'}${item.bestScore == null ? '' : ` · ${item.bestScore} 分`}`
    }))
    planRecordOptions.value = records(results[1]).map((item) => ({
      value: item.id,
      label: `${item.planName || '补短板计划'}${item.createTime ? ` · ${String(item.createTime).slice(0, 10)}` : ''}`
    }))
    mockInterviewOptions.value = records(results[2]).map((item) => ({
      value: item.id,
      label: `${item.directionName || item.direction || '模拟面试'}${item.totalScore == null ? '' : ` · ${item.totalScore} 分`}`
    }))
  } finally {
    loading.applicationSources = false
  }
}

const openApplicationDialog = (record?: CareerApplicationRecord) => {
  if (record) {
    editingApplicationId.value = record.id
    Object.assign(applicationForm, {
      matchRecordId: record.matchRecordId || null,
      planRecordId: record.planRecordId || null,
      mockInterviewSessionId: record.mockInterviewSessionId || null,
      companyName: record.companyName || '',
      positionName: record.positionName || '',
      status: record.status || 'APPLIED',
      appliedDate: record.appliedDate || '',
      nextFollowUpDate: record.nextFollowUpDate || '',
      note: record.note || ''
    })
    applicationFormRef.value?.clearValidate()
  } else {
    resetApplicationForm()
  }
  applicationDialogVisible.value = true
  loadApplicationSources()
}

const isTerminalApplicationStatus = (status?: string) => ['OFFER', 'REJECTED', 'WITHDRAWN'].includes(status || '')

const handleSaveApplication = async () => {
  try {
    await applicationFormRef.value?.validate()
    loading.applicationSave = true
    const payload = {
      matchRecordId: applicationForm.matchRecordId || null,
      planRecordId: applicationForm.planRecordId || null,
      mockInterviewSessionId: applicationForm.mockInterviewSessionId || null,
      companyName: applicationForm.companyName,
      positionName: applicationForm.positionName,
      status: applicationForm.status,
      appliedDate: applicationForm.appliedDate || null,
      nextFollowUpDate: isTerminalApplicationStatus(applicationForm.status) ? null : (applicationForm.nextFollowUpDate || null),
      note: applicationForm.note || null
    }
    if (editingApplicationId.value !== null) {
      await careerLoopApi.updateApplication(editingApplicationId.value, payload)
      ElMessage.success('投递进展已更新')
    } else {
      await careerLoopApi.createApplication(payload)
      ElMessage.success('投递进展已记录')
    }
    applicationDialogVisible.value = false
    await fetchAll()
  } catch (e) {
    console.error('保存投递进展失败', e)
  } finally {
    loading.applicationSave = false
  }
}

const handleDeleteApplication = async (record: CareerApplicationRecord) => {
  try {
    await ElMessageBox.confirm('删除后，该投递状态不会继续出现在成长档案中。', '删除投递记录', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    loading.applicationDeleteId = record.id
    await careerLoopApi.deleteApplication(record.id)
    ElMessage.success('投递记录已删除')
    await fetchAll()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      console.error('删除投递记录失败', e)
    }
  } finally {
    loading.applicationDeleteId = null
  }
}

const handleStart = async () => {
  loading.start = true
  try {
    await careerLoopApi.start({
      targetRole: current.value.session.targetRole || '',
      targetCompanyType: current.value.session.targetCompanyType || ''
    })
    ElMessage.success('求职准备已就绪')
    await fetchAll()
  } catch (e) {
    console.error('启动求职准备失败', e)
    ElMessage.error('启动求职准备失败')
  } finally {
    loading.start = false
  }
}

const handleRecoverySync = async () => {
  loading.recovery = true
  try {
    await careerLoopApi.sync({})
    ElMessage.success('闭环状态已恢复同步')
    await fetchAll()
  } catch (e) {
    console.error('恢复同步失败', e)
    ElMessage.error('恢复同步失败')
  } finally {
    loading.recovery = false
  }
}

const handleRecoveryCommand = async (command: string) => {
  if (command === 'sync') {
    await handleRecoverySync()
    return
  }
  await fetchAll()
}

const handleDoneAction = async (row: CareerLoopAction) => {
  try {
    await careerLoopApi.completeAction(row.id)
    ElMessage.success('动作已标记完成')
    await fetchAll()
  } catch (e) {
    console.error('标记动作完成失败', e)
    ElMessage.error('标记动作完成失败')
  }
}

const goByActionType = async (actionType?: string) => {
  if (actionType === 'offer') {
    try {
      await careerLoopApi.event({
        targetStage: 'OFFER_TRACKING',
        source: 'manual',
        note: '记录投递与Offer跟踪',
        nextSuggestions: [
          '维护投递公司、岗位、轮次和下一次跟进时间',
          '将面试反馈同步回复盘清单，持续更新项目表达'
        ]
      })
      ElMessage.success('已进入投递与Offer跟踪阶段')
      await fetchAll()
      openApplicationDialog()
    } catch (e) {
      console.error('记录投递与Offer跟踪失败', e)
      ElMessage.error('记录投递与Offer跟踪失败')
    }
    return
  }

  const routeMap: Record<string, RouteLocationRaw> = {
    setup: { path: '/job-battle', query: { step: '0', from: 'career-loop', action: 'setup' } },
    resume: { path: '/job-battle', query: { step: '1', from: 'career-loop', action: 'resume' } },
    plan: { path: '/job-battle', query: { step: '2', from: 'career-loop', action: 'plan' } },
    study: { path: '/plan' },
    mock: { path: '/mock-interview/config' },
    review: { path: '/job-battle', query: { step: '3', from: 'career-loop', action: 'review' } }
  }
  router.push((actionType && routeMap[actionType]) || { path: '/job-battle' })
}

const mapActionStatusTone = (status?: string): CnTone => {
  const toneMap: Record<string, CnTone> = {
    todo: 'warning',
    doing: 'brand',
    done: 'success'
  }
  return status ? toneMap[status] || 'info' : 'info'
}

const applicationStatusTone = (status?: string): CnTone => {
  const toneMap: Record<string, CnTone> = {
    PREPARING: 'info',
    APPLIED: 'brand',
    INTERVIEWING: 'warning',
    OFFER: 'success',
    REJECTED: 'danger',
    WITHDRAWN: 'neutral'
  }
  return status ? toneMap[status] || 'info' : 'info'
}

const applicationStatusLabel = (status?: string) => {
  const item = applicationStatusOptions.find((option) => option.value === status)
  return item?.label || status || '未知状态'
}

const toPercent = (value: unknown, fallback = 0) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return fallback
  }
  return Math.max(0, Math.min(100, Math.round(numberValue)))
}

const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(max, value))

const radarIndicators = computed<RadarIndicator[]>(() => {
  const doneCount = actions.value.filter((item) => item.status === 'done').length
  const totalCount = actions.value.length || 1
  const doneRate = Math.round((doneCount / totalCount) * 100)
  const mockCount = Number(current.value.snapshot.mockCount || 0)
  const reviewCount = Number(current.value.snapshot.reviewCount || 0)
  const latestMockScore = Number(current.value.snapshot.latestMockScore || 0)
  const riskCount = current.value.riskFlags.length

  return [
    { key: 'match', label: '岗位匹配', value: toPercent(current.value.session.stagePercent) },
    { key: 'execute', label: '执行节奏', value: toPercent(Math.max(doneRate, current.value.snapshot.planProgress || 0)) },
    { key: 'mock', label: '面试实战', value: toPercent(mockCount * 18 + latestMockScore * 0.45) },
    { key: 'review', label: '复盘沉淀', value: toPercent(reviewCount * 34) },
    { key: 'health', label: '健康稳定', value: toPercent(current.value.session.healthScore, 60) },
    { key: 'risk', label: '风险控制', value: toPercent(100 - riskCount * 18, 100) }
  ]
})

const radarAxes = computed<RadarAxis[]>(() => {
  const count = radarIndicators.value.length
  return radarIndicators.value.map((item, index) => {
    const angle = -Math.PI / 2 + (Math.PI * 2 * index) / count
    const point = {
      x: radarCenter.x + Math.cos(angle) * radarRadius,
      y: radarCenter.y + Math.sin(angle) * radarRadius
    }
    const labelPoint = {
      x: radarCenter.x + Math.cos(angle) * (radarRadius + 24),
      y: radarCenter.y + Math.sin(angle) * (radarRadius + 24)
    }
    return { ...item, point, labelPoint, angle }
  })
})

const radarGridPolygons = computed<RadarPoint[][]>(() => {
  const levels = [0.25, 0.5, 0.75, 1]
  return levels.map((level) => radarAxes.value.map((axis) => ({
    x: radarCenter.x + Math.cos(axis.angle) * radarRadius * level,
    y: radarCenter.y + Math.sin(axis.angle) * radarRadius * level
  })))
})

const radarValuePolygon = computed<RadarPoint[]>(() => {
  return radarAxes.value.map((axis) => {
    const ratio = clamp(axis.value / 100, 0, 1)
    return {
      x: radarCenter.x + Math.cos(axis.angle) * radarRadius * ratio,
      y: radarCenter.y + Math.sin(axis.angle) * radarRadius * ratio
    }
  })
})

const pointsToString = (points: RadarPoint[]) => points.map((point) => `${point.x},${point.y}`).join(' ')

const toDateKey = (dateValue: unknown) => {
  const parsedValue = typeof dateValue === 'string' ? dateValue.replace(' ', 'T') : dateValue
  const date = new Date(parsedValue as string | number | Date)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const addHeat = (map: Map<string, number>, key: string, delta: number) => {
  if (!key) {
    return
  }
  const currentValue = map.get(key) || 0
  map.set(key, currentValue + delta)
}

const heatmapCells = computed<HeatmapCell[]>(() => {
  const today = new Date()
  const dates: Date[] = []
  for (let offset = 27; offset >= 0; offset -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - offset)
    dates.push(date)
  }

  const intensityMap = new Map<string, number>()
  timeline.value.forEach((item) => {
    addHeat(intensityMap, toDateKey(item.createTime), 2)
  })
  actions.value.forEach((item) => {
    if (item.status === 'done') {
      addHeat(intensityMap, toDateKey(item.updateTime), 1)
    }
  })

  const planProgress = toPercent(current.value.snapshot.planProgress || 0)
  const mockCount = Number(current.value.snapshot.mockCount || 0)
  const reviewCount = Number(current.value.snapshot.reviewCount || 0)
  const recentActiveDays = Math.max(0, Math.round((planProgress / 100) * 28))
  for (let i = 0; i < recentActiveDays; i += 1) {
    const key = toDateKey(dates[dates.length - 1 - i])
    addHeat(intensityMap, key, i < 7 ? 2 : 1)
  }
  for (let i = 0; i < mockCount; i += 1) {
    const key = toDateKey(dates[Math.max(0, dates.length - 1 - i * 3)])
    addHeat(intensityMap, key, 2)
  }
  for (let i = 0; i < reviewCount; i += 1) {
    const key = toDateKey(dates[Math.max(0, dates.length - 1 - i * 5)])
    addHeat(intensityMap, key, 1)
  }

  return dates.map((date) => {
    const dateKey = toDateKey(date)
    const score = intensityMap.get(dateKey) || 0
    const level = clamp(score, 0, 4)
    return {
      dateKey,
      dateLabel: `${date.getMonth() + 1}/${date.getDate()}`,
      level,
      isToday: dateKey === toDateKey(today)
    }
  })
})

onMounted(async () => {
  await fetchAll()
  if (route.query.focus === 'applications') {
    openApplicationDialog()
  }
})

onActivated(() => {
  if (lastSyncedAt.value) {
    fetchAll()
  }
})
</script>

<style scoped>
.career-loop-page {
  min-height: calc(100vh - 68px);
}

.summary-grid,
.insight-grid,
.content-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.insight-grid,
.content-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.radar-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-5);
  min-width: 0;
}

.radar-svg {
  width: 320px;
  max-width: 100%;
  height: auto;
  flex-shrink: 0;
}

.radar-grid {
  fill: none;
  stroke: var(--cn-color-border-subtle);
  stroke-width: 1;
}

.radar-axis {
  stroke: var(--cn-color-border);
  stroke-width: 1;
}

.radar-value {
  fill: color-mix(in srgb, var(--cn-color-brand-primary) 18%, transparent);
  stroke: var(--cn-color-brand-primary);
  stroke-width: 2;
}

.radar-point {
  fill: var(--cn-color-brand-primary);
}

.radar-label {
  fill: var(--cn-color-text-secondary);
  font-size: 11px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.radar-legend {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
  min-width: 220px;
}

.radar-legend-item,
.suggestion-item {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.radar-legend-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.radar-legend-item b {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 18px;
}

.heatmap-head {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  text-align: center;
}

.heatmap-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: var(--cn-space-2);
}

.heatmap-cell {
  height: 22px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-sm);
  background: var(--cn-color-bg-surface-muted);
}

.heatmap-cell.level-1 {
  background: color-mix(in srgb, var(--cn-color-success) 22%, var(--cn-color-bg-surface));
  border-color: color-mix(in srgb, var(--cn-color-success) 26%, var(--cn-color-border-subtle));
}

.heatmap-cell.level-2 {
  background: color-mix(in srgb, var(--cn-color-success) 38%, var(--cn-color-bg-surface));
  border-color: color-mix(in srgb, var(--cn-color-success) 42%, var(--cn-color-border-subtle));
}

.heatmap-cell.level-3 {
  background: color-mix(in srgb, var(--cn-color-success) 62%, var(--cn-color-bg-surface));
  border-color: color-mix(in srgb, var(--cn-color-success) 66%, var(--cn-color-border-subtle));
}

.heatmap-cell.level-4 {
  background: var(--cn-color-success);
  border-color: var(--cn-color-success);
}

.heatmap-cell.today {
  box-shadow: inset 0 0 0 2px var(--cn-color-brand-primary);
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-4);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.legend-dot {
  display: inline-block;
  width: 16px;
  height: 10px;
  border-radius: var(--cn-radius-pill);
}

.legend-dot.level-1 {
  background: color-mix(in srgb, var(--cn-color-success) 22%, var(--cn-color-bg-surface));
}

.legend-dot.level-2 {
  background: color-mix(in srgb, var(--cn-color-success) 38%, var(--cn-color-bg-surface));
}

.legend-dot.level-3 {
  background: color-mix(in srgb, var(--cn-color-success) 62%, var(--cn-color-bg-surface));
}

.legend-dot.level-4 {
  background: var(--cn-color-success);
}

.timeline-title {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.timeline-meta {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.timeline-note {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.section-block + .section-block {
  margin-top: var(--cn-space-5);
}

.section-subtitle {
  margin-bottom: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.suggestion-list {
  display: grid;
  gap: var(--cn-space-3);
}

.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  line-height: 1.65;
}

.suggestion-item span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

.suggestion-item p {
  margin: 0;
}

.career-actions-section :deep(.cn-section__body) {
  padding-top: var(--cn-space-4);
}

.career-action-list {
  display: grid;
  gap: var(--cn-space-3);
  min-height: 112px;
}

.career-action-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 8px;
  background: var(--cn-color-bg-surface-muted);
}

.career-action-copy {
  min-width: 0;
}

.career-action-meta,
.career-action-controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.career-action-copy h2 {
  margin: var(--cn-space-3) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 16px;
  line-height: 1.35;
}

.career-action-copy p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.career-action-controls {
  justify-content: flex-end;
}

.application-tracking-content {
  min-height: 108px;
}

.application-summary,
.application-item-meta,
.application-source-meta,
.application-item-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.application-summary {
  margin-bottom: var(--cn-space-3);
}

.application-summary span,
.application-item-meta span,
.application-item-copy small {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.application-list {
  display: grid;
  gap: var(--cn-space-3);
}

.application-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 8px;
  background: var(--cn-color-bg-surface-muted);
}

.application-item-copy {
  min-width: 0;
}

.application-item-copy h3 {
  margin: var(--cn-space-2) 0 var(--cn-space-1);
  color: var(--cn-color-text-primary);
  font-size: 15px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.application-item-copy p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.application-item-copy small {
  display: block;
  margin-top: var(--cn-space-2);
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.application-source-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.application-form-grid,
.application-source-grid {
  display: grid;
  gap: var(--cn-space-3);
}

.application-form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.application-source-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .radar-wrap {
    flex-direction: column;
    align-items: stretch;
  }

  .radar-svg {
    width: 100%;
  }

  .radar-legend {
    min-width: 0;
    grid-template-columns: 1fr;
  }

  .career-action-item {
    grid-template-columns: 1fr;
  }

  .career-action-controls {
    justify-content: flex-start;
  }

  .application-item,
  .application-form-grid,
  .application-source-grid {
    grid-template-columns: 1fr;
  }

  .application-item-actions {
    justify-content: flex-start;
  }
}
</style>
