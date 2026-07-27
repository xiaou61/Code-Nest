<template>
  <CnPage class="sre-workbench" surface="transparent" max-width="1500px">
    <div class="sr-only" aria-live="polite">{{ liveStatus }}</div>

    <CnPageHeader
      title="SRE 事故工作台"
      :description="headerDescription"
      eyebrow="OPERATIONS / V2.5.0"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="overallTone">{{ overallLabel }}</CnStatusTag>
        <CnStatusTag type="neutral">自动执行关闭</CnStatusTag>
      </template>

      <template #actions>
        <el-button
          type="primary"
          :icon="Refresh"
          :loading="refreshing"
          aria-label="刷新 SRE 事故数据"
          @click="refreshWorkbench(true)"
        >
          刷新
        </el-button>
      </template>
    </CnPageHeader>

    <div class="stat-grid" aria-label="事故状态汇总">
      <CnStatCard
        title="活动事故"
        :value="summary.activeCount"
        :description="`${summary.openCount} 条待确认，${summary.acknowledgedCount} 条处理中`"
        :tone="summary.activeCount > 0 ? 'warning' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="严重事故"
        :value="summary.criticalActiveCount"
        description="当前尚未恢复的 critical 事故"
        :tone="summary.criticalActiveCount > 0 ? 'danger' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="警告事故"
        :value="summary.warningActiveCount"
        description="当前尚未恢复的 warning 事故"
        :tone="summary.warningActiveCount > 0 ? 'warning' : 'success'"
        :loading="summaryLoading"
      />
      <CnStatCard
        title="已恢复"
        :value="summary.resolvedCount"
        :description="`累计事故 ${summary.totalCount} 条`"
        tone="info"
        :loading="summaryLoading"
      />
    </div>

    <CnSection
      title="事故队列"
      :description="`当前条件共 ${pagination.total} 条记录`"
      divided
    >
      <CnFilterForm
        :model-value="filters"
        :fields="filterFields"
        :loading="listLoading"
        :columns="3"
        @update:model-value="updateFilters"
        @search="applyFilters"
        @reset="resetFilters"
      />

      <CnDataTable
        class="incident-table"
        :columns="incidentColumns"
        :data="incidents"
        :loading="listLoading"
        :pagination="tablePagination"
        row-key="id"
        empty-title="暂无事故"
        empty-description="当前筛选条件下没有事故记录。"
        empty-icon="OK"
        @row-click="openIncident"
        @page-change="changePage"
        @page-size-change="changePageSize"
      >
        <template #severity="{ row }">
          <CnStatusTag :type="severityTone(row.severity)" size="sm">
            {{ severityLabel(row.severity) }}
          </CnStatusTag>
        </template>

        <template #state="{ row }">
          <CnStatusTag :type="stateTone(row.state)" size="sm">
            {{ stateLabel(row.state) }}
          </CnStatusTag>
        </template>

        <template #summary="{ row }">
          <div class="incident-summary-cell">
            <strong>{{ row.alertName || '-' }}</strong>
            <span>{{ row.summary || '-' }}</span>
          </div>
        </template>

        <template #lastSeen="{ row }">
          <span class="time-cell">{{ formatTime(row.lastSeen) }}</span>
        </template>

        <template #actions="{ row }">
          <el-button
            link
            type="primary"
            :icon="View"
            :aria-label="`查看事故 ${row.incidentNo || row.id}`"
            @click.stop="openIncident(row)"
          >
            查看
          </el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-drawer
      v-model="drawerVisible"
      class="sre-incident-drawer"
      :title="drawerTitle"
      :size="drawerSize"
      append-to-body
      destroy-on-close
      @closed="resetDrawer"
    >
      <div v-loading="detailLoading" class="drawer-content">
        <template v-if="selectedIncident">
          <div class="drawer-status-row">
            <div class="drawer-status-tags">
              <CnStatusTag :type="severityTone(selectedIncident.severity)" size="sm">
                {{ severityLabel(selectedIncident.severity) }}
              </CnStatusTag>
              <CnStatusTag :type="stateTone(selectedIncident.state)" size="sm">
                {{ stateLabel(selectedIncident.state) }}
              </CnStatusTag>
              <CnStatusTag type="neutral" size="sm">
                {{ selectedIncident.service || 'unknown-service' }}
              </CnStatusTag>
            </div>

            <div class="drawer-actions" role="toolbar" aria-label="事故操作">
              <el-button
                :icon="Check"
                :loading="actionLoading === 'ack'"
                :disabled="!canAcknowledge || actionLoading !== ''"
                @click="acknowledgeSelected"
              >
                确认
              </el-button>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="rcaLoading"
                :disabled="detailLoading || rcaLoading"
                @click="generateRca"
              >
                AI 分析
              </el-button>
              <el-button
                type="danger"
                plain
                :icon="CircleCheck"
                :loading="actionLoading === 'resolve'"
                :disabled="!canResolve || actionLoading !== ''"
                @click="resolveSelected"
              >
                关闭事故
              </el-button>
            </div>
          </div>

          <el-tabs v-model="activeDetailTab" class="detail-tabs">
            <el-tab-pane label="概览" name="overview">
              <section class="detail-section" aria-labelledby="incident-facts-title">
                <h3 id="incident-facts-title">事故信息</h3>
                <dl class="fact-grid">
                  <div>
                    <dt>事故编号</dt>
                    <dd>{{ selectedIncident.incidentNo || '-' }}</dd>
                  </div>
                  <div>
                    <dt>告警名称</dt>
                    <dd>{{ selectedIncident.alertName || '-' }}</dd>
                  </div>
                  <div>
                    <dt>首次发现</dt>
                    <dd>{{ formatTime(selectedIncident.firstSeen) }}</dd>
                  </div>
                  <div>
                    <dt>最近观测</dt>
                    <dd>{{ formatTime(selectedIncident.lastSeen) }}</dd>
                  </div>
                  <div>
                    <dt>确认时间</dt>
                    <dd>{{ formatTime(selectedIncident.acknowledgedAt) }}</dd>
                  </div>
                  <div>
                    <dt>恢复时间</dt>
                    <dd>{{ formatTime(selectedIncident.resolvedAt) }}</dd>
                  </div>
                </dl>
                <p class="incident-description">{{ selectedIncident.summary || '暂无事故摘要。' }}</p>
              </section>

              <section class="detail-section" aria-labelledby="alert-timeline-title">
                <div class="section-heading">
                  <h3 id="alert-timeline-title">告警时间线</h3>
                  <CnStatusTag type="neutral" size="sm">{{ contextAlerts.length }} 条</CnStatusTag>
                </div>

                <ol v-if="contextAlerts.length" class="timeline-list">
                  <li v-for="alert in contextAlerts" :key="alert.id" class="timeline-item">
                    <span class="timeline-marker" :class="`is-${String(alert.status || '').toLowerCase()}`" aria-hidden="true" />
                    <article>
                      <div class="timeline-head">
                        <strong>{{ alert.alertName || '-' }}</strong>
                        <CnStatusTag :type="alert.status === 'FIRING' ? 'danger' : 'success'" size="sm">
                          {{ alert.status === 'FIRING' ? '触发' : '恢复' }}
                        </CnStatusTag>
                      </div>
                      <p>{{ alert.source || '-' }} · {{ formatTime(alert.observedAt || alert.startsAt) }}</p>
                    </article>
                  </li>
                </ol>
                <CnEmptyState
                  v-else
                  title="暂无告警快照"
                  description="该事故当前没有可展示的告警摘要。"
                  icon="AL"
                  size="sm"
                  surface="transparent"
                />
              </section>
            </el-tab-pane>

            <el-tab-pane :label="`证据 (${contextEvidence.length})`" name="evidence">
              <section class="detail-section evidence-section" aria-labelledby="evidence-title">
                <div class="section-heading">
                  <h3 id="evidence-title">证据快照</h3>
                  <CnStatusTag v-if="investigationContext?.evidenceTruncated" type="warning" size="sm">
                    已裁剪
                  </CnStatusTag>
                </div>

                <el-collapse v-if="contextEvidence.length" v-model="expandedEvidenceIds">
                  <el-collapse-item
                    v-for="evidence in contextEvidence"
                    :key="evidence.id"
                    :name="evidence.id"
                  >
                    <template #title>
                      <div class="evidence-title-row">
                        <strong>{{ evidence.sourceType || 'UNKNOWN' }}</strong>
                        <span>{{ evidence.sourceRef || '-' }}</span>
                        <CnStatusTag :type="evidenceTone(evidence.status)" size="sm">
                          {{ evidence.status || 'UNKNOWN' }}
                        </CnStatusTag>
                      </div>
                    </template>
                    <dl class="evidence-meta">
                      <div>
                        <dt>采集时间</dt>
                        <dd>{{ formatTime(evidence.capturedAt) }}</dd>
                      </div>
                      <div>
                        <dt>查询</dt>
                        <dd>{{ evidence.queryDescription || '-' }}</dd>
                      </div>
                    </dl>
                    <pre class="evidence-json">{{ prettySnapshot(evidence.snapshot) }}</pre>
                  </el-collapse-item>
                </el-collapse>
                <CnEmptyState
                  v-else
                  title="暂无调查证据"
                  description="证据采集任务尚未写入可用快照。"
                  icon="EV"
                  size="sm"
                  surface="transparent"
                />
              </section>
            </el-tab-pane>

            <el-tab-pane label="AI RCA" name="rca">
              <section v-loading="rcaLoading || rcaHistoryLoading" class="detail-section rca-section" aria-labelledby="rca-title">
                <div v-if="rcaRuns.length" class="rca-history-toolbar">
                  <span>历史版本</span>
                  <el-select
                    v-model="selectedRcaRunId"
                    aria-label="选择 RCA 历史版本"
                    @change="selectRcaRun"
                  >
                    <el-option
                      v-for="run in rcaRuns"
                      :key="run.id"
                      :label="runHistoryLabel(run)"
                      :value="run.id"
                    />
                  </el-select>
                </div>

                <template v-if="rcaReport">
                  <div class="section-heading rca-heading">
                    <div>
                      <h3 id="rca-title">根因分析</h3>
                      <p>{{ formatTime(rcaReport.generatedAt) }}</p>
                    </div>
                    <div class="rca-tags">
                      <CnStatusTag :type="rcaReport.generationMode === 'AI' ? 'brand' : 'warning'" size="sm">
                        {{ rcaReport.generationMode || 'UNKNOWN' }}
                      </CnStatusTag>
                      <CnStatusTag :type="conclusionTone(rcaReport.conclusionStatus)" size="sm">
                        {{ conclusionLabel(rcaReport.conclusionStatus) }}
                      </CnStatusTag>
                      <CnStatusTag v-if="selectedRcaRun" :type="runStatusTone(selectedRcaRun.status)" size="sm">
                        {{ runStatusLabel(selectedRcaRun.status) }}
                      </CnStatusTag>
                      <CnStatusTag :type="rcaReport.executionAllowed ? 'danger' : 'neutral'" size="sm">
                        {{ rcaReport.executionAllowed ? '允许执行' : '禁止自动执行' }}
                      </CnStatusTag>
                    </div>
                  </div>

                  <p class="rca-summary">{{ rcaReport.executiveSummary || '暂无执行摘要。' }}</p>

                  <div class="rca-block rca-feedback-block">
                    <div class="feedback-heading">
                      <div>
                        <h4>分析反馈</h4>
                        <div v-if="rcaFeedback" class="feedback-meta">
                          <CnStatusTag :type="feedbackAccuracyTone(rcaFeedback.accuracy)" size="sm">
                            {{ feedbackAccuracyLabel(rcaFeedback.accuracy) }}
                          </CnStatusTag>
                          <span>{{ feedbackGapLabel(rcaFeedback.gapType) }}</span>
                          <span>管理员 #{{ rcaFeedback.reviewedBy || '-' }}</span>
                          <time>{{ formatTime(rcaFeedback.reviewedAt) }}</time>
                        </div>
                      </div>
                      <el-button
                        :icon="Download"
                        :loading="evaluationExporting"
                        :disabled="!rcaFeedback"
                        plain
                        @click="exportRcaEvaluationSample"
                      >
                        导出样本
                      </el-button>
                    </div>

                    <el-form class="feedback-form" :model="feedbackForm" label-position="top">
                      <el-form-item label="准确度">
                        <el-radio-group
                          v-model="feedbackForm.accuracy"
                          size="small"
                          aria-label="RCA 准确度"
                          @change="handleAccuracyChange"
                        >
                          <el-radio-button label="ACCURATE">准确</el-radio-button>
                          <el-radio-button label="PARTIAL">部分准确</el-radio-button>
                          <el-radio-button label="INACCURATE">不准确</el-radio-button>
                        </el-radio-group>
                      </el-form-item>

                      <el-form-item
                        v-if="['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)"
                        label="主要缺口"
                      >
                        <el-select v-model="feedbackForm.gapType" aria-label="RCA 主要缺口">
                          <el-option label="证据检索不足" value="RETRIEVAL_GAP" />
                          <el-option label="推理判断偏差" value="REASONING_GAP" />
                          <el-option label="取证工具失败" value="TOOL_FAILURE" />
                          <el-option label="调查路由错误" value="ROUTING_GAP" />
                          <el-option label="尚未分类" value="UNKNOWN" />
                        </el-select>
                      </el-form-item>

                      <el-form-item
                        v-if="['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)"
                        class="feedback-wide"
                        label="期望结论"
                      >
                        <el-input
                          v-model="feedbackForm.expectedConclusion"
                          type="textarea"
                          :rows="3"
                          maxlength="2000"
                          show-word-limit
                        />
                      </el-form-item>

                      <el-form-item class="feedback-wide" label="管理员备注">
                        <el-input
                          v-model="feedbackForm.note"
                          type="textarea"
                          :rows="3"
                          maxlength="1000"
                          show-word-limit
                        />
                      </el-form-item>

                      <div class="feedback-actions">
                        <el-button
                          type="primary"
                          :icon="Check"
                          :loading="feedbackSaving"
                          @click="saveRcaFeedback"
                        >
                          {{ rcaFeedback ? '更新反馈' : '保存反馈' }}
                        </el-button>
                      </div>
                    </el-form>
                  </div>

                  <div v-if="investigationSteps.length" class="rca-block investigation-trace">
                    <h4>调查轨迹</h4>
                    <ol>
                      <li v-for="step in investigationSteps" :key="step.id || step.order">
                        <span class="trace-index" aria-hidden="true">{{ step.order }}</span>
                        <div>
                          <div class="trace-heading">
                            <strong>{{ investigationStepLabel(step.code) }}</strong>
                            <CnStatusTag :type="runStatusTone(step.status)" size="sm">
                              {{ runStatusLabel(step.status) }}
                            </CnStatusTag>
                          </div>
                          <p>{{ step.detail || '-' }}</p>
                          <time>{{ formatTime(step.recordedAt) }}</time>
                        </div>
                      </li>
                    </ol>
                  </div>

                  <div class="rca-block">
                    <h4>观测事实</h4>
                    <ol v-if="rcaReport.observations?.length" class="rca-list">
                      <li v-for="(observation, index) in rcaReport.observations" :key="`observation-${index}`">
                        <span>{{ observation.statement }}</span>
                        <small>{{ evidenceIdsText(observation.evidenceIds) }}</small>
                      </li>
                    </ol>
                    <p v-else class="empty-copy">暂无观测事实。</p>
                  </div>

                  <div class="rca-block">
                    <h4>原因假设</h4>
                    <div v-if="rcaReport.hypotheses?.length" class="hypothesis-list">
                      <article v-for="(hypothesis, index) in rcaReport.hypotheses" :key="`hypothesis-${index}`" class="hypothesis-item">
                        <div class="hypothesis-head">
                          <strong>{{ hypothesis.title }}</strong>
                          <CnStatusTag type="info" size="sm">置信度 {{ formatConfidence(hypothesis.confidence) }}</CnStatusTag>
                        </div>
                        <p>{{ hypothesis.reasoning }}</p>
                        <small>{{ evidenceIdsText(hypothesis.evidenceIds) }}</small>
                        <ul v-if="hypothesis.nextChecks?.length">
                          <li v-for="(check, checkIndex) in hypothesis.nextChecks" :key="`check-${index}-${checkIndex}`">
                            {{ check }}
                          </li>
                        </ul>
                      </article>
                    </div>
                    <p v-else class="empty-copy">暂无原因假设。</p>
                  </div>

                  <div class="rca-block">
                    <h4>建议动作</h4>
                    <ol v-if="rcaReport.recommendedNextSteps?.length" class="recommendation-list">
                      <li v-for="(step, index) in rcaReport.recommendedNextSteps" :key="`step-${index}`">
                        <div>
                          <span>{{ step.description }}</span>
                          <small>{{ evidenceIdsText(step.evidenceIds) }}</small>
                        </div>
                        <CnStatusTag :type="riskTone(step.risk)" size="sm">{{ riskLabel(step.risk) }}</CnStatusTag>
                      </li>
                    </ol>
                    <p v-else class="empty-copy">暂无建议动作。</p>
                  </div>

                  <div class="rca-block">
                    <h4>证据引用</h4>
                    <div v-if="rcaReport.evidenceReferences?.length" class="reference-list">
                      <span v-for="reference in rcaReport.evidenceReferences" :key="reference.id">
                        #{{ reference.id }} · {{ reference.sourceType }} · {{ reference.status }}
                      </span>
                    </div>
                    <p v-else class="empty-copy">暂无证据引用。</p>
                  </div>

                  <div v-if="rcaReport.limitations?.length" class="rca-block limitation-block">
                    <h4>限制</h4>
                    <ul>
                      <li v-for="(limitation, index) in rcaReport.limitations" :key="`limitation-${index}`">
                        {{ limitation }}
                      </li>
                    </ul>
                  </div>
                </template>

                <div v-else-if="selectedRcaRun" class="rca-empty">
                  <CnEmptyState
                    :title="selectedRcaRun.status === 'RUNNING' ? '调查正在进行' : '调查报告不可用'"
                    :description="selectedRcaRun.failureCode || '该次运行尚未生成可恢复的结构化报告。'"
                    icon="AI"
                    surface="transparent"
                  />
                </div>

                <div v-else class="rca-empty">
                  <CnEmptyState
                    title="尚未生成 RCA"
                    description="当前事故没有已生成的根因分析报告。"
                    icon="AI"
                    surface="transparent"
                  />
                  <el-button type="primary" :icon="MagicStick" :loading="rcaLoading" @click="generateRca">
                    生成 AI 分析
                  </el-button>
                </div>
              </section>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </el-drawer>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, CircleCheck, Download, MagicStick, Refresh, View } from '@element-plus/icons-vue'
import { sreApi } from '@/api/sre'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface IncidentSummary {
  totalCount: number
  activeCount: number
  openCount: number
  acknowledgedCount: number
  criticalActiveCount: number
  warningActiveCount: number
  resolvedCount: number
  lastObservedAt?: string
}

interface Incident {
  id: number
  incidentNo?: string
  service?: string
  alertName?: string
  severity?: string
  state?: string
  summary?: string
  firstSeen?: string
  lastSeen?: string
  acknowledgedAt?: string
  resolvedAt?: string
}

interface IncidentPage {
  pageNum?: number
  pageSize?: number
  total?: number
  records?: Incident[]
}

interface InvestigationAlert {
  id: number
  source?: string
  alertName?: string
  status?: string
  startsAt?: string
  observedAt?: string
}

interface InvestigationEvidence {
  id: number
  sourceType?: string
  sourceRef?: string
  queryDescription?: string
  capturedAt?: string
  status?: string
  snapshot?: Record<string, unknown>
  snapshotTruncated?: boolean
}

interface InvestigationContext {
  incident?: Incident
  alerts?: InvestigationAlert[]
  evidence?: InvestigationEvidence[]
  alertsTruncated?: boolean
  evidenceTruncated?: boolean
}

interface RcaReport {
  generationMode?: string
  conclusionStatus?: string
  executiveSummary?: string
  observations?: Array<{ statement?: string; evidenceIds?: number[] }>
  hypotheses?: Array<{
    title?: string
    reasoning?: string
    confidence?: number
    evidenceIds?: number[]
    nextChecks?: string[]
  }>
  recommendedNextSteps?: Array<{ description?: string; risk?: string; evidenceIds?: number[] }>
  evidenceReferences?: Array<{ id: number; sourceType?: string; status?: string }>
  limitations?: string[]
  executionAllowed?: boolean
  generatedAt?: string
}

interface RcaRunSummary {
  id: number
  incidentId?: number
  status?: string
  triggerSource?: string
  requestedBy?: number
  generationMode?: string
  conclusionStatus?: string
  alertCount?: number
  evidenceCount?: number
  contextTruncated?: boolean
  failureCode?: string
  startedAt?: string
  completedAt?: string
}

interface RcaFeedback {
  id?: number
  runId?: number
  accuracy?: string
  gapType?: string
  note?: string
  expectedConclusion?: string
  reviewedBy?: number
  reviewedAt?: string
}

interface RcaFeedbackForm {
  accuracy: string
  gapType: string
  note: string
  expectedConclusion: string
}

interface InvestigationStep {
  id?: number
  order: number
  code?: string
  status?: string
  detail?: string
  recordedAt?: string
}

interface RcaRunDetail {
  run?: RcaRunSummary
  steps?: InvestigationStep[]
  report?: RcaReport
  feedback?: RcaFeedback
}

interface FilterState extends Record<string, unknown> {
  state: string
  severity: string
  service: string
}

const emptySummary = (): IncidentSummary => ({
  totalCount: 0,
  activeCount: 0,
  openCount: 0,
  acknowledgedCount: 0,
  criticalActiveCount: 0,
  warningActiveCount: 0,
  resolvedCount: 0
})

const breadcrumbs: CnBreadcrumbItem[] = [
  { label: '管理后台' },
  { label: 'SRE 运维中心' },
  { label: '事故工作台' }
]

const filterFields: CnFilterField[] = [
  {
    prop: 'state',
    label: '事故状态',
    type: 'select',
    options: [
      { label: '待确认', value: 'OPEN' },
      { label: '处理中', value: 'ACKNOWLEDGED' },
      { label: '已恢复', value: 'RESOLVED' },
      { label: '已关闭', value: 'CLOSED' }
    ]
  },
  {
    prop: 'severity',
    label: '严重程度',
    type: 'select',
    options: [
      { label: '严重', value: 'critical' },
      { label: '警告', value: 'warning' },
      { label: '信息', value: 'info' }
    ]
  },
  {
    prop: 'service',
    label: '服务',
    type: 'input',
    placeholder: '例如 code-nest'
  }
]

const incidentColumns: CnTableColumn<Incident>[] = [
  { label: '级别', prop: 'severity', slot: 'severity', width: 92, align: 'center' },
  { label: '状态', prop: 'state', slot: 'state', width: 104, align: 'center' },
  { label: '事故编号', prop: 'incidentNo', width: 190, showOverflowTooltip: true },
  { label: '告警与摘要', prop: 'summary', slot: 'summary', minWidth: 300 },
  { label: '服务', prop: 'service', width: 130, showOverflowTooltip: true },
  { label: '最近观测', prop: 'lastSeen', slot: 'lastSeen', width: 168 },
  { label: '操作', prop: 'actions', slot: 'actions', width: 92, align: 'center', fixed: 'right' }
]

const filters = reactive<FilterState>({ state: '', severity: '', service: '' })
const summary = ref<IncidentSummary>(emptySummary())
const incidents = ref<Incident[]>([])
const summaryLoading = ref(false)
const listLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const liveStatus = ref('')
const viewportWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)

const drawerVisible = ref(false)
const detailLoading = ref(false)
const selectedIncident = ref<Incident | null>(null)
const investigationContext = ref<InvestigationContext | null>(null)
const activeDetailTab = ref('overview')
const expandedEvidenceIds = ref<Array<number | string>>([])
const rcaLoading = ref(false)
const rcaHistoryLoading = ref(false)
const rcaReport = ref<RcaReport | null>(null)
const rcaRuns = ref<RcaRunSummary[]>([])
const selectedRcaRunId = ref<number | null>(null)
const investigationSteps = ref<InvestigationStep[]>([])
const rcaFeedback = ref<RcaFeedback | null>(null)
const feedbackForm = reactive<RcaFeedbackForm>({
  accuracy: '',
  gapType: '',
  note: '',
  expectedConclusion: ''
})
const feedbackSaving = ref(false)
const evaluationExporting = ref(false)
const actionLoading = ref<'ack' | 'resolve' | ''>('')
let investigationRequestVersion = 0
let rcaRequestVersion = 0

const refreshing = computed(() => summaryLoading.value || listLoading.value)
const headerDescription = computed(() => `最近观测：${formatTime(summary.value.lastObservedAt)}`)
const overallTone = computed<CnTone>(() => {
  if (summary.value.criticalActiveCount > 0) return 'danger'
  if (summary.value.activeCount > 0) return 'warning'
  return 'success'
})
const overallLabel = computed(() => {
  if (summary.value.criticalActiveCount > 0) return '存在严重事故'
  if (summary.value.activeCount > 0) return '存在待处理事故'
  return '无活动事故'
})
const pagination = computed(() => ({ total: total.value }))
const tablePagination = computed<CnPagination>(() => ({
  page: pageNum.value,
  pageSize: pageSize.value,
  total: total.value,
  pageSizes: [20, 50, 100],
  background: true
}))
const drawerSize = computed(() => Math.min(780, Math.max(280, Math.floor(viewportWidth.value * 0.94))))
const drawerTitle = computed(() => selectedIncident.value?.incidentNo || '事故详情')
const contextAlerts = computed(() => investigationContext.value?.alerts || [])
const contextEvidence = computed(() => investigationContext.value?.evidence || [])
const selectedRcaRun = computed(() => (
  rcaRuns.value.find((run) => run.id === selectedRcaRunId.value) || null
))
const isCurrentIncident = (incidentId: number) => (
  drawerVisible.value && selectedIncident.value?.id === incidentId
)
const canAcknowledge = computed(() => selectedIncident.value?.state === 'OPEN')
const canResolve = computed(() => !['RESOLVED', 'CLOSED'].includes(selectedIncident.value?.state || ''))

const resetFeedbackForm = (feedback?: RcaFeedback | null) => {
  Object.assign(feedbackForm, {
    accuracy: feedback?.accuracy || '',
    gapType: feedback?.gapType || '',
    note: feedback?.note || '',
    expectedConclusion: feedback?.expectedConclusion || ''
  })
}

const buildQuery = () => ({
  state: filters.state || undefined,
  severity: filters.severity || undefined,
  service: String(filters.service || '').trim() || undefined,
  pageNum: pageNum.value,
  pageSize: pageSize.value
})

const updateFilters = (value: Record<string, unknown>) => {
  Object.assign(filters, value)
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const response = await sreApi.getIncidentSummary() as Partial<IncidentSummary> | null
    summary.value = { ...emptySummary(), ...(response || {}) }
  } finally {
    summaryLoading.value = false
  }
}

const loadIncidents = async () => {
  listLoading.value = true
  try {
    const response = await sreApi.getIncidents(buildQuery()) as IncidentPage | null
    incidents.value = response?.records || []
    total.value = Number(response?.total || 0)
  } finally {
    listLoading.value = false
  }
}

const refreshWorkbench = async (notify = false) => {
  liveStatus.value = '正在刷新事故数据'
  try {
    await Promise.all([loadSummary(), loadIncidents()])
    liveStatus.value = `刷新完成，共 ${total.value} 条事故记录`
    if (notify) ElMessage.success('事故数据已刷新')
  } catch (error) {
    console.error('刷新 SRE 事故数据失败:', error)
    liveStatus.value = '事故数据刷新失败'
  }
}

const applyFilters = () => {
  pageNum.value = 1
  refreshWorkbench()
}

const resetFilters = () => {
  Object.assign(filters, { state: '', severity: '', service: '' })
  pageNum.value = 1
  refreshWorkbench()
}

const changePage = (page: number) => {
  pageNum.value = page
  loadIncidents().catch((error) => console.error('加载 SRE 事故列表失败:', error))
}

const changePageSize = (size: number) => {
  pageSize.value = size
  pageNum.value = 1
  loadIncidents().catch((error) => console.error('加载 SRE 事故列表失败:', error))
}

const openIncident = async (incident: Incident) => {
  investigationRequestVersion += 1
  rcaRequestVersion += 1
  selectedIncident.value = { ...incident }
  investigationContext.value = null
  rcaReport.value = null
  rcaRuns.value = []
  selectedRcaRunId.value = null
  investigationSteps.value = []
  rcaFeedback.value = null
  resetFeedbackForm()
  activeDetailTab.value = 'overview'
  expandedEvidenceIds.value = []
  drawerVisible.value = true
  await Promise.all([
    loadInvestigationContext(incident.id),
    loadRcaRuns(incident.id)
  ])
}

const loadInvestigationContext = async (incidentId: number) => {
  const requestVersion = ++investigationRequestVersion
  detailLoading.value = true
  try {
    const response = await sreApi.getInvestigationContext(incidentId) as InvestigationContext | null
    if (requestVersion !== investigationRequestVersion || !isCurrentIncident(incidentId)) return
    investigationContext.value = response
    if (response?.incident) selectedIncident.value = { ...selectedIncident.value, ...response.incident }
    expandedEvidenceIds.value = (response?.evidence || []).slice(0, 1).map((item) => item.id)
  } catch (error) {
    console.error('加载 SRE 事故详情失败:', error)
  } finally {
    if (requestVersion === investigationRequestVersion) detailLoading.value = false
  }
}

const loadRcaRuns = async (incidentId: number) => {
  const requestVersion = ++rcaRequestVersion
  rcaHistoryLoading.value = true
  try {
    const response = await sreApi.getRcaRuns(incidentId, 20) as RcaRunSummary[] | null
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)) return
    rcaRuns.value = Array.isArray(response) ? response : []
    if (!rcaRuns.value.length) {
      selectedRcaRunId.value = null
      rcaReport.value = null
      investigationSteps.value = []
      rcaFeedback.value = null
      resetFeedbackForm()
      return
    }
    const selectedStillExists = rcaRuns.value.some((run) => run.id === selectedRcaRunId.value)
    const runId = selectedStillExists ? selectedRcaRunId.value : rcaRuns.value[0].id
    if (runId != null) await loadRcaRun(incidentId, runId, false, requestVersion)
  } catch (error) {
    console.error('加载 SRE RCA 历史失败:', error)
  } finally {
    if (requestVersion === rcaRequestVersion) rcaHistoryLoading.value = false
  }
}

const loadRcaRun = async (
  incidentId: number,
  runId: number,
  manageLoading = true,
  inheritedRequestVersion?: number
) => {
  const requestVersion = inheritedRequestVersion ?? ++rcaRequestVersion
  if (manageLoading) rcaHistoryLoading.value = true
  rcaFeedback.value = null
  resetFeedbackForm()
  try {
    const response = await sreApi.getRcaRun(incidentId, runId) as RcaRunDetail | null
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)) return
    selectedRcaRunId.value = runId
    rcaReport.value = response?.report || null
    investigationSteps.value = response?.steps || []
    rcaFeedback.value = response?.feedback || null
    resetFeedbackForm(rcaFeedback.value)
    if (response?.run) {
      const index = rcaRuns.value.findIndex((run) => run.id === response.run?.id)
      if (index >= 0) rcaRuns.value[index] = response.run
    }
  } catch (error) {
    console.error('加载 SRE RCA 详情失败:', error)
    if (requestVersion === rcaRequestVersion && isCurrentIncident(incidentId)) {
      rcaReport.value = null
      investigationSteps.value = []
      rcaFeedback.value = null
      resetFeedbackForm()
    }
  } finally {
    if (manageLoading && requestVersion === rcaRequestVersion) rcaHistoryLoading.value = false
  }
}

const selectRcaRun = (value: number | string) => {
  const incidentId = selectedIncident.value?.id
  const runId = Number(value)
  if (!incidentId || !Number.isInteger(runId) || runId <= 0) return
  loadRcaRun(incidentId, runId).catch((error) => console.error('切换 SRE RCA 历史失败:', error))
}

const handleAccuracyChange = (value: string | number | boolean | undefined) => {
  if (value === 'ACCURATE') {
    feedbackForm.gapType = ''
    feedbackForm.expectedConclusion = ''
  }
}

const saveRcaFeedback = async () => {
  const incidentId = selectedIncident.value?.id
  const runId = selectedRcaRunId.value
  if (!incidentId || !runId || feedbackSaving.value) return
  if (!feedbackForm.accuracy) {
    ElMessage.warning('请选择 RCA 准确度')
    return
  }
  const requiresCorrection = ['PARTIAL', 'INACCURATE'].includes(feedbackForm.accuracy)
  if (requiresCorrection && !feedbackForm.gapType) {
    ElMessage.warning('请选择主要缺口')
    return
  }
  if (requiresCorrection && !feedbackForm.expectedConclusion.trim()) {
    ElMessage.warning('请填写期望结论')
    return
  }

  const requestVersion = rcaRequestVersion
  feedbackSaving.value = true
  try {
    const response = await sreApi.saveRcaFeedback(incidentId, runId, {
      accuracy: feedbackForm.accuracy,
      gapType: requiresCorrection ? feedbackForm.gapType : null,
      note: feedbackForm.note.trim() || null,
      expectedConclusion: requiresCorrection ? feedbackForm.expectedConclusion.trim() : null
    }) as RcaFeedback
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)
      || selectedRcaRunId.value !== runId) return
    rcaFeedback.value = response
    resetFeedbackForm(response)
    liveStatus.value = 'RCA 分析反馈已保存'
    ElMessage.success('RCA 分析反馈已保存')
  } catch (error) {
    console.error('保存 SRE RCA 反馈失败:', error)
    liveStatus.value = 'RCA 分析反馈保存失败'
  } finally {
    feedbackSaving.value = false
  }
}

const exportRcaEvaluationSample = async () => {
  const incidentId = selectedIncident.value?.id
  const runId = selectedRcaRunId.value
  if (!incidentId || !runId || !rcaFeedback.value || evaluationExporting.value) return
  const requestVersion = rcaRequestVersion
  evaluationExporting.value = true
  try {
    const sample = await sreApi.getRcaEvaluationSample(incidentId, runId)
    if (requestVersion !== rcaRequestVersion || !isCurrentIncident(incidentId)
      || selectedRcaRunId.value !== runId) return
    const blobUrl = URL.createObjectURL(new Blob(
      [JSON.stringify(sample, null, 2)],
      { type: 'application/json;charset=utf-8' }
    ))
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = `sre-rca-eval-${incidentId}-${runId}.json`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(blobUrl)
    liveStatus.value = 'RCA 评测样本已导出'
    ElMessage.success('RCA 评测样本已导出')
  } catch (error) {
    console.error('导出 SRE RCA 评测样本失败:', error)
    liveStatus.value = 'RCA 评测样本导出失败'
  } finally {
    evaluationExporting.value = false
  }
}

const acknowledgeSelected = async () => {
  if (!selectedIncident.value || !canAcknowledge.value) return
  actionLoading.value = 'ack'
  try {
    await sreApi.acknowledgeIncident(selectedIncident.value.id)
    ElMessage.success('事故已确认')
    liveStatus.value = `${selectedIncident.value.incidentNo || '事故'} 已确认`
    await Promise.all([
      loadSummary(),
      loadIncidents(),
      loadInvestigationContext(selectedIncident.value.id)
    ])
  } catch (error) {
    console.error('确认 SRE 事故失败:', error)
  } finally {
    actionLoading.value = ''
  }
}

const resolveSelected = async () => {
  if (!selectedIncident.value || !canResolve.value) return
  try {
    await ElMessageBox.confirm(
      `确认关闭事故 ${selectedIncident.value.incidentNo || selectedIncident.value.id}？`,
      '关闭事故',
      { confirmButtonText: '确认关闭', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') console.error('关闭事故确认框异常:', error)
    return
  }

  actionLoading.value = 'resolve'
  try {
    await sreApi.resolveIncident(selectedIncident.value.id)
    ElMessage.success('事故已关闭')
    liveStatus.value = `${selectedIncident.value.incidentNo || '事故'} 已关闭`
    await Promise.all([
      loadSummary(),
      loadIncidents(),
      loadInvestigationContext(selectedIncident.value.id)
    ])
  } catch (error) {
    console.error('关闭 SRE 事故失败:', error)
  } finally {
    actionLoading.value = ''
  }
}

const generateRca = async () => {
  if (!selectedIncident.value || rcaLoading.value) return
  const incidentId = selectedIncident.value.id
  activeDetailTab.value = 'rca'
  rcaLoading.value = true
  liveStatus.value = '正在生成只读 AI 根因分析'
  try {
    const report = await sreApi.generateRca(incidentId) as RcaReport
    if (!isCurrentIncident(incidentId)) return
    rcaReport.value = report
    await loadRcaRuns(incidentId)
    if (!isCurrentIncident(incidentId)) return
    liveStatus.value = 'AI 根因分析已生成'
    ElMessage.success('AI 根因分析已生成')
  } catch (error) {
    console.error('生成 SRE RCA 失败:', error)
    liveStatus.value = 'AI 根因分析生成失败'
  } finally {
    rcaLoading.value = false
  }
}

const resetDrawer = () => {
  investigationRequestVersion += 1
  rcaRequestVersion += 1
  selectedIncident.value = null
  investigationContext.value = null
  rcaReport.value = null
  rcaRuns.value = []
  selectedRcaRunId.value = null
  investigationSteps.value = []
  rcaFeedback.value = null
  resetFeedbackForm()
  detailLoading.value = false
  rcaHistoryLoading.value = false
  feedbackSaving.value = false
  evaluationExporting.value = false
  activeDetailTab.value = 'overview'
  expandedEvidenceIds.value = []
  actionLoading.value = ''
}

const severityTone = (value?: string): CnTone => ({
  critical: 'danger',
  warning: 'warning',
  info: 'info'
}[String(value || '').toLowerCase()] as CnTone || 'neutral')

const severityLabel = (value?: string) => ({
  critical: '严重',
  warning: '警告',
  info: '信息'
}[String(value || '').toLowerCase()] || value || '-')

const stateTone = (value?: string): CnTone => ({
  OPEN: 'danger',
  ACKNOWLEDGED: 'warning',
  INVESTIGATING: 'info',
  MITIGATED: 'brand',
  RESOLVED: 'success',
  CLOSED: 'neutral'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const stateLabel = (value?: string) => ({
  OPEN: '待确认',
  ACKNOWLEDGED: '处理中',
  INVESTIGATING: '调查中',
  MITIGATED: '已缓解',
  RESOLVED: '已恢复',
  CLOSED: '已关闭'
}[String(value || '').toUpperCase()] || value || '-')

const evidenceTone = (value?: string): CnTone => ({
  AVAILABLE: 'success',
  INVALID: 'danger',
  UNAVAILABLE: 'warning'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const conclusionTone = (value?: string): CnTone => ({
  SUPPORTED: 'success',
  PARTIAL: 'warning',
  PARTIALLY_SUPPORTED: 'warning',
  INSUFFICIENT_EVIDENCE: 'warning'
}[String(value || '').toUpperCase()] as CnTone || 'info')

const conclusionLabel = (value?: string) => ({
  SUPPORTED: '证据支持',
  PARTIAL: '部分支持',
  PARTIALLY_SUPPORTED: '部分支持',
  INSUFFICIENT_EVIDENCE: '证据不足'
}[String(value || '').toUpperCase()] || value || '-')

const runStatusTone = (value?: string): CnTone => ({
  RUNNING: 'info',
  SUCCEEDED: 'success',
  DEGRADED: 'warning',
  FAILED: 'danger',
  SKIPPED: 'neutral'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const runStatusLabel = (value?: string) => ({
  RUNNING: '进行中',
  SUCCEEDED: '已完成',
  DEGRADED: '已降级',
  FAILED: '失败',
  SKIPPED: '已跳过'
}[String(value || '').toUpperCase()] || value || '-')

const investigationStepLabel = (value?: string) => ({
  CONTEXT_LOADED: '加载事故上下文',
  MODEL_CONTEXT_BUILT: '构建模型上下文',
  MODEL_ANALYSIS: '执行只读分析',
  REPORT_VALIDATED: '校验结构化报告',
  RUN_FAILED: '调查异常中止'
}[String(value || '').toUpperCase()] || value || '未知步骤')

const feedbackAccuracyLabel = (value?: string) => ({
  ACCURATE: '准确',
  PARTIAL: '部分准确',
  INACCURATE: '不准确'
}[String(value || '').toUpperCase()] || value || '未评价')

const feedbackAccuracyTone = (value?: string): CnTone => ({
  ACCURATE: 'success',
  PARTIAL: 'warning',
  INACCURATE: 'danger'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const feedbackGapLabel = (value?: string) => ({
  RETRIEVAL_GAP: '证据检索不足',
  REASONING_GAP: '推理判断偏差',
  TOOL_FAILURE: '取证工具失败',
  ROUTING_GAP: '调查路由错误',
  UNKNOWN: '尚未分类'
}[String(value || '').toUpperCase()] || '无主要缺口')

const runHistoryLabel = (run: RcaRunSummary) => {
  const mode = run.generationMode || runStatusLabel(run.status)
  return `${formatTime(run.startedAt)} · ${mode} · ${conclusionLabel(run.conclusionStatus)}`
}

const riskTone = (value?: string): CnTone => ({
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger'
}[String(value || '').toUpperCase()] as CnTone || 'neutral')

const riskLabel = (value?: string) => ({
  LOW: '低风险',
  MEDIUM: '中风险',
  HIGH: '高风险'
}[String(value || '').toUpperCase()] || value || '未分级')

const formatTime = (value: unknown) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('Z', '').slice(0, 19)
}

const prettySnapshot = (snapshot?: Record<string, unknown>) => {
  if (!snapshot || Object.keys(snapshot).length === 0) return '{}'
  return JSON.stringify(snapshot, null, 2)
}

const formatConfidence = (value?: number) => {
  const confidence = Number(value)
  if (!Number.isFinite(confidence)) return '-'
  return `${Math.round(Math.max(0, Math.min(1, confidence)) * 100)}%`
}

const evidenceIdsText = (ids?: number[]) => {
  if (!ids?.length) return '无证据引用'
  return `证据 ${ids.map((id) => `#${id}`).join('、')}`
}

const syncViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  syncViewportWidth()
  window.addEventListener('resize', syncViewportWidth)
  refreshWorkbench()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewportWidth)
})
</script>

<style scoped>
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.incident-table {
  margin-top: var(--cn-space-5);
}

.incident-table :deep(.el-table__row) {
  cursor: pointer;
}

.incident-summary-cell {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
  padding: 2px 0;
}

.incident-summary-cell strong,
.incident-summary-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.incident-summary-cell strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.incident-summary-cell span,
.time-cell {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.drawer-content {
  min-height: 420px;
}

.drawer-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.drawer-status-tags,
.drawer-actions,
.rca-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.detail-tabs {
  margin-top: var(--cn-space-4);
}

.detail-section {
  min-width: 0;
  padding: var(--cn-space-2) 0 var(--cn-space-5);
}

.detail-section + .detail-section {
  padding-top: var(--cn-space-5);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.detail-section h3,
.detail-section h4,
.section-heading p {
  margin: 0;
}

.detail-section h3 {
  color: var(--cn-color-text-primary);
  font-size: 16px;
  line-height: 1.4;
}

.detail-section h4 {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.4;
}

.section-heading,
.timeline-head,
.hypothesis-head,
.rca-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.fact-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin: var(--cn-space-4) 0 0;
}

.fact-grid > div,
.evidence-meta > div {
  min-width: 0;
}

.fact-grid dt,
.evidence-meta dt {
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.fact-grid dd,
.evidence-meta dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  line-height: 1.55;
}

.incident-description,
.rca-summary {
  margin: var(--cn-space-4) 0 0;
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-brand-primary);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.timeline-list {
  display: grid;
  gap: 0;
  margin: var(--cn-space-4) 0 0;
  padding: 0;
  list-style: none;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: var(--cn-space-3);
  min-width: 0;
  padding-bottom: var(--cn-space-4);
}

.timeline-item:not(:last-child)::before {
  position: absolute;
  top: 14px;
  bottom: 0;
  left: 6px;
  width: 1px;
  background: var(--cn-color-border);
  content: '';
}

.timeline-marker {
  z-index: 1;
  width: 13px;
  height: 13px;
  margin-top: 3px;
  border: 3px solid var(--cn-color-bg-surface);
  border-radius: 50%;
  background: var(--cn-color-text-tertiary);
  box-shadow: 0 0 0 1px var(--cn-color-border);
}

.timeline-marker.is-firing {
  background: var(--cn-color-danger);
}

.timeline-marker.is-resolved {
  background: var(--cn-color-success);
}

.timeline-item article,
.timeline-item p {
  min-width: 0;
  margin: 0;
}

.timeline-item p,
.section-heading p {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.evidence-section :deep(.el-collapse) {
  margin-top: var(--cn-space-4);
  border-top: 0;
}

.evidence-title-row {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  width: calc(100% - 24px);
}

.evidence-title-row strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.evidence-title-row > span:not(.cn-status-tag) {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-title-row .cn-status-tag {
  margin-left: auto;
}

.evidence-meta {
  display: grid;
  grid-template-columns: minmax(140px, 0.4fr) minmax(0, 1fr);
  gap: var(--cn-space-4);
  margin: 0 0 var(--cn-space-3);
}

.evidence-json {
  max-height: 360px;
  margin: 0;
  padding: var(--cn-space-4);
  overflow: auto;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-canvas);
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 12px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.rca-section {
  min-height: 360px;
}

.rca-history-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--cn-space-3);
  min-height: 40px;
  margin-bottom: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.rca-history-toolbar > span {
  flex: 0 0 auto;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.rca-history-toolbar :deep(.el-select) {
  width: min(360px, 100%);
}

.rca-block {
  margin-top: var(--cn-space-5);
}

.rca-feedback-block {
  padding: var(--cn-space-5) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.feedback-heading,
.feedback-meta,
.feedback-actions {
  display: flex;
  align-items: center;
}

.feedback-heading {
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.feedback-meta {
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.feedback-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--cn-space-4);
  margin-top: var(--cn-space-4);
}

.feedback-form .feedback-wide,
.feedback-actions {
  grid-column: 1 / -1;
}

.feedback-form :deep(.el-select) {
  width: 100%;
}

.feedback-form :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
}

.feedback-actions {
  justify-content: flex-end;
}

.investigation-trace {
  padding-bottom: var(--cn-space-5);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.investigation-trace ol {
  display: grid;
  gap: var(--cn-space-3);
  margin: var(--cn-space-3) 0 0;
  padding: 0;
  list-style: none;
}

.investigation-trace li {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: var(--cn-space-3);
  min-width: 0;
}

.trace-index {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: 1px solid var(--cn-color-border);
  border-radius: 50%;
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 11px;
}

.trace-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.trace-heading strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
}

.investigation-trace p,
.investigation-trace time {
  display: block;
  margin: var(--cn-space-1) 0 0;
  overflow-wrap: anywhere;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
}

.rca-list,
.recommendation-list,
.limitation-block ul {
  display: grid;
  gap: var(--cn-space-3);
  margin: var(--cn-space-3) 0 0;
  padding-left: 20px;
}

.rca-list li,
.recommendation-list li,
.limitation-block li {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.rca-list li span,
.rca-list li small,
.recommendation-list li span,
.recommendation-list li small {
  display: block;
}

.rca-list small,
.recommendation-list small,
.hypothesis-item small {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.hypothesis-list {
  display: grid;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-3);
}

.hypothesis-item {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.hypothesis-item p {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.hypothesis-item ul {
  margin: var(--cn-space-3) 0 0;
  padding-left: 18px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.recommendation-list {
  padding: 0;
  list-style: none;
}

.recommendation-list li {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.reference-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.reference-list span {
  padding: 6px 9px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.limitation-block {
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-warning);
  background: var(--cn-color-warning-soft);
}

.limitation-block ul {
  margin-bottom: 0;
}

.empty-copy {
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.rca-empty {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-3);
  min-height: 300px;
  align-content: center;
}

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stat-grid,
  .fact-grid,
  .evidence-meta {
    grid-template-columns: 1fr;
  }

  .drawer-status-row,
  .section-heading,
  .rca-heading,
  .feedback-heading,
  .rca-history-toolbar,
  .recommendation-list li {
    align-items: flex-start;
    flex-direction: column;
  }

  .drawer-actions {
    width: 100%;
  }

  .drawer-actions :deep(.el-button) {
    flex: 1 1 auto;
    margin-left: 0;
  }

  .rca-history-toolbar {
    align-items: stretch;
  }

  .rca-history-toolbar :deep(.el-select) {
    width: 100%;
  }

  .feedback-form {
    grid-template-columns: 1fr;
  }

  .feedback-form :deep(.el-form-item),
  .feedback-actions {
    grid-column: 1;
  }
}
</style>
