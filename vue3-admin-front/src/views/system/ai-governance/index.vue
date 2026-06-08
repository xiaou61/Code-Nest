<template>
  <CnPage class="ai-governance-page" surface="transparent" max-width="1380px">
    <CnPageHeader
      title="AI Runtime 质量与治理中心"
      :description="overview.summary || '查看 AI 场景治理状态、质量评分、运行观测与风险分布。'"
      eyebrow="AI Governance"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="overview.healthy ? 'success' : 'warning'">
          {{ overview.healthLevel || (overview.healthy ? '健康' : '需关注') }}
        </CnStatusTag>
        <CnStatusTag type="brand">质量分 {{ overview.qualityScore || 0 }}</CnStatusTag>
        <CnStatusTag type="neutral">风险 {{ riskItems.length }} 项</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadOverview">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="stat-grid">
      <CnStatCard
        title="质量分"
        :value="overview.qualityScore || 0"
        :description="`质量等级：${overview.qualityGrade || '-'}`"
        :tone="overview.healthy ? 'success' : 'warning'"
      />
      <CnStatCard
        title="AI 场景"
        :value="`${overview.configuredWorkflows || 0} / ${overview.totalWorkflows || 0}`"
        description="已配置场景 / 全部治理场景"
        tone="brand"
      />
      <CnStatCard
        title="运行成功率"
        :value="formatRate(runtimeInsight.successRate)"
        description="最近运行观测中的成功占比"
        tone="success"
      />
      <CnStatCard title="风险队列" :value="riskItems.length" description="当前质量风险待处理项" tone="warning" />
    </div>

    <div class="quality-grid">
      <CnSection title="治理覆盖矩阵" description="按配置、兜底、Schema、RAG 和运行观测查看治理覆盖情况。" divided>
        <div class="coverage-grid">
          <article v-for="item in coverageMatrix" :key="item.key" class="coverage-item">
            <div class="coverage-head">
              <strong>{{ item.name }}</strong>
              <CnStatusTag :type="coverageTone(item.status)" size="sm">{{ statusText(item.status) }}</CnStatusTag>
            </div>
            <div class="coverage-rate">{{ formatRate(item.rate) }}</div>
            <el-progress :percentage="safePercent(item.rate)" :show-text="false" :stroke-width="8" />
            <p>{{ item.covered || 0 }} / {{ item.total || 0 }} - {{ item.description }}</p>
          </article>

          <CnEmptyState
            v-if="!coverageMatrix.length"
            title="暂无覆盖数据"
            description="治理总览接口暂未返回覆盖矩阵。"
            icon="GM"
            surface="transparent"
          />
        </div>
      </CnSection>

      <CnSection title="运行时洞察" description="聚合调用量、失败率、兜底率和结构化解析质量。" divided>
        <div class="runtime-grid">
          <div class="runtime-item">
            <span>总调用</span>
            <strong>{{ runtimeInsight.totalInvocations || 0 }}</strong>
          </div>
          <div class="runtime-item">
            <span>失败率</span>
            <strong>{{ formatRate(runtimeInsight.errorRate) }}</strong>
          </div>
          <div class="runtime-item">
            <span>兜底率</span>
            <strong>{{ formatRate(runtimeInsight.fallbackRate) }}</strong>
          </div>
          <div class="runtime-item">
            <span>Schema 失败率</span>
            <strong>{{ formatRate(runtimeInsight.structuredParseFailureRate) }}</strong>
          </div>
          <div class="runtime-item">
            <span>平均耗时</span>
            <strong>{{ runtimeInsight.averageLatencyMs || 0 }} ms</strong>
          </div>
          <div class="runtime-item">
            <span>观测场景</span>
            <strong>{{ runtimeInsight.observedScenes || 0 }}</strong>
          </div>
        </div>

        <div class="runtime-foot">
          <CnStatusTag :type="overview.healthy ? 'success' : 'warning'" size="sm">
            {{ runtimeInsight.statusText || overview.healthLevel || '-' }}
          </CnStatusTag>
          <span>最近调用：{{ runtimeInsight.lastInvocationAt || '-' }}</span>
        </div>
      </CnSection>
    </div>

    <CnSection title="质量风险队列" :description="`共 ${riskItems.length} 项质量风险`" divided>
      <CnDataTable
        :columns="riskColumns"
        :data="riskItems"
        :loading="loading"
        row-key="scene"
        empty-title="暂无质量风险"
        empty-description="当前没有需要处理的 AI 治理风险。"
      >
        <template #severity="{ row }">
          <CnStatusTag :type="riskTone(row.severity)" size="sm">
            {{ riskLabel(row.severity) }}
          </CnStatusTag>
        </template>
      </CnDataTable>
    </CnSection>

    <div class="catalog-grid">
      <CnSection title="AI 场景治理目录" :description="`共 ${workflows.length} 个 AI 场景`" divided>
        <CnDataTable
          :columns="workflowColumns"
          :data="workflows"
          :loading="loading"
          row-key="workflowId"
          empty-title="暂无 AI 场景"
          empty-description="治理总览接口暂未返回场景目录。"
        >
          <template #configured="{ row }">
            <CnStatusTag :type="row.configured ? 'success' : 'danger'" size="sm">
              {{ row.configured ? '已配置' : '缺失' }}
            </CnStatusTag>
          </template>
          <template #fallbackCovered="{ row }">
            <CnStatusTag :type="row.fallbackCovered ? 'success' : 'warning'" size="sm">
              {{ row.fallbackCovered ? '已覆盖' : '待补齐' }}
            </CnStatusTag>
          </template>
          <template #schemaCovered="{ row }">
            <CnStatusTag :type="row.schemaCovered ? 'success' : 'warning'" size="sm">
              {{ row.schemaCovered ? '已覆盖' : '待补齐' }}
            </CnStatusTag>
          </template>
          <template #ragCovered="{ row }">
            <CnStatusTag :type="row.ragCovered ? 'success' : 'info'" size="sm">
              {{ row.ragCovered ? '已登记' : '未登记' }}
            </CnStatusTag>
          </template>
          <template #runtimeObserved="{ row }">
            <CnStatusTag :type="row.runtimeObserved ? 'success' : 'info'" size="sm">
              {{ row.runtimeObserved ? '有样本' : '无样本' }}
            </CnStatusTag>
          </template>
          <template #riskLevel="{ row }">
            <CnStatusTag :type="riskTone(row.riskLevel)" size="sm">
              {{ riskLabel(row.riskLevel) }}
            </CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>

      <div class="side-stack">
        <CnSection title="质量规则" description="当前质量评分与风险识别使用的治理规则。" divided>
          <div v-if="overview.qualityRules?.length" class="rule-list">
            <div v-for="(rule, index) in overview.qualityRules" :key="`rule-${index}`" class="rule-item">
              <CnStatusTag type="brand" size="sm" :dot="false">{{ index + 1 }}</CnStatusTag>
              <span>{{ rule }}</span>
            </div>
          </div>
          <CnEmptyState v-else title="暂无质量规则" description="治理总览接口暂未返回规则列表。" icon="QR" size="sm" surface="transparent" />
        </CnSection>

        <CnSection title="改进建议" description="根据覆盖矩阵和风险队列生成的后续优化方向。" divided>
          <div v-if="overview.improvementSuggestions?.length" class="rule-list">
            <div
              v-for="(item, index) in overview.improvementSuggestions"
              :key="`suggestion-${index}`"
              class="rule-item suggestion-item"
            >
              <CnStatusTag type="warning" size="sm" :dot="false">{{ index + 1 }}</CnStatusTag>
              <span>{{ item }}</span>
            </div>
          </div>
          <CnEmptyState v-else title="暂无改进建议" description="当前治理状态暂未生成建议。" icon="IS" size="sm" surface="transparent" />
          <div class="generated-time">生成时间：{{ formatTime(overview.generatedAt) }}</div>
        </CnSection>
      </div>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { aiGovernanceApi } from '@/api/aiGovernance'
import { CnDataTable, CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface RuntimeInsight {
  successRate?: number
  errorRate?: number
  fallbackRate?: number
  structuredParseFailureRate?: number
  totalInvocations?: number
  averageLatencyMs?: number
  observedScenes?: number
  statusText?: string
  lastInvocationAt?: string
  [key: string]: unknown
}

interface CoverageItem {
  key: string
  name: string
  status?: string
  rate?: number
  covered?: number
  total?: number
  description?: string
}

interface RiskItem {
  severity?: string
  scene?: string
  metric?: string
  reason?: string
  suggestion?: string
  score?: number
  [key: string]: unknown
}

interface WorkflowItem {
  workflowName?: string
  domain?: string
  workflowId?: string
  configured?: boolean
  fallbackCovered?: boolean
  schemaCovered?: boolean
  ragCovered?: boolean
  runtimeObserved?: boolean
  riskLevel?: string
  [key: string]: unknown
}

interface GovernanceOverview {
  summary?: string
  healthy?: boolean
  healthLevel?: string
  qualityScore?: number
  qualityGrade?: string
  configuredWorkflows?: number
  totalWorkflows?: number
  workflows: WorkflowItem[]
  qualityRules: string[]
  improvementSuggestions: string[]
  runtimeInsight: RuntimeInsight
  coverageMatrix: CoverageItem[]
  riskItems: RiskItem[]
  generatedAt?: string
  [key: string]: unknown
}

const emptyOverview = (): GovernanceOverview => ({
  workflows: [],
  qualityRules: [],
  improvementSuggestions: [],
  runtimeInsight: {},
  coverageMatrix: [],
  riskItems: []
})

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '系统管理' }, { label: 'AI 治理中心' }]

const loading = ref(false)
const overview = ref<GovernanceOverview>(emptyOverview())

const runtimeInsight = computed(() => overview.value.runtimeInsight || {})
const coverageMatrix = computed(() => overview.value.coverageMatrix || [])
const riskItems = computed(() => overview.value.riskItems || [])
const workflows = computed(() => overview.value.workflows || [])

const riskColumns: CnTableColumn<RiskItem>[] = [
  { label: '级别', prop: 'severity', slot: 'severity', width: 92, align: 'center' },
  { label: '场景', prop: 'scene', minWidth: 160, showOverflowTooltip: true },
  { label: '指标', prop: 'metric', width: 120 },
  { label: '原因', prop: 'reason', minWidth: 220, showOverflowTooltip: true },
  { label: '建议', prop: 'suggestion', minWidth: 260, showOverflowTooltip: true },
  { label: '分值', prop: 'score', width: 80, align: 'center' }
]

const workflowColumns: CnTableColumn<WorkflowItem>[] = [
  { label: '场景', prop: 'workflowName', minWidth: 190, showOverflowTooltip: true },
  { label: '域', prop: 'domain', width: 120 },
  { label: 'Prompt ID', prop: 'workflowId', width: 170 },
  { label: '配置', prop: 'configured', slot: 'configured', width: 90, align: 'center' },
  { label: '兜底', prop: 'fallbackCovered', slot: 'fallbackCovered', width: 90, align: 'center' },
  { label: 'Schema', prop: 'schemaCovered', slot: 'schemaCovered', width: 95, align: 'center' },
  { label: 'RAG', prop: 'ragCovered', slot: 'ragCovered', width: 90, align: 'center' },
  { label: '观测', prop: 'runtimeObserved', slot: 'runtimeObserved', width: 90, align: 'center' },
  { label: '风险', prop: 'riskLevel', slot: 'riskLevel', width: 90, align: 'center' }
]

const riskToneMap: Record<string, CnTone> = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger'
}

const riskLabelMap: Record<string, string> = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
}

const coverageToneMap: Record<string, CnTone> = {
  STABLE: 'success',
  WATCH: 'warning',
  RISK: 'danger'
}

const coverageStatusMap: Record<string, string> = {
  STABLE: '稳定',
  WATCH: '观察',
  RISK: '风险'
}

const riskTone = (value?: string): CnTone => riskToneMap[value || ''] || 'info'
const riskLabel = (value?: string) => riskLabelMap[value || ''] || value || '-'
const coverageTone = (value?: string): CnTone => coverageToneMap[value || ''] || 'info'
const statusText = (value?: string) => coverageStatusMap[value || ''] || value || '-'

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = (await aiGovernanceApi.getOverview()) || emptyOverview()
  } catch (error) {
    console.error('加载AI治理总览失败:', error)
    ElMessage.error('加载AI治理总览失败')
  } finally {
    loading.value = false
  }
}

const formatRate = (value: unknown) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return '0%'
  }
  return `${numberValue.toFixed(1)}%`
}

const safePercent = (value: unknown) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return 0
  }
  return Math.max(0, Math.min(100, numberValue))
}

const formatTime = (value: unknown) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.quality-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 0.65fr);
  gap: var(--cn-space-5);
  align-items: stretch;
}

.coverage-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.coverage-item,
.runtime-item,
.rule-item {
  min-width: 0;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.coverage-item {
  padding: var(--cn-space-4);
}

.coverage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
}

.coverage-head strong {
  color: var(--cn-color-text-primary);
  font-size: 13px;
  line-height: 1.4;
}

.coverage-rate {
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-size: 24px;
  font-weight: 750;
  line-height: 1.2;
}

.coverage-item p {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.runtime-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.runtime-item {
  padding: var(--cn-space-3);
}

.runtime-item span {
  display: block;
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.runtime-item strong {
  color: var(--cn-color-text-primary);
  font-size: 18px;
  line-height: 1.3;
}

.runtime-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.catalog-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.45fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.side-stack {
  display: grid;
  gap: var(--cn-space-5);
}

.rule-list {
  display: grid;
  gap: var(--cn-space-3);
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3) var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.suggestion-item {
  background: var(--cn-color-warning-soft);
}

.generated-time {
  margin-top: var(--cn-space-4);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

@media (max-width: 1200px) {
  .stat-grid,
  .coverage-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quality-grid,
  .catalog-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-grid,
  .coverage-grid,
  .runtime-grid {
    grid-template-columns: 1fr;
  }

  .runtime-foot {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
