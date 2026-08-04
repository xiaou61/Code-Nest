<template>
  <CnPage class="growth-capabilities-page" surface="transparent" max-width="1360px" full-height>
    <CnPageHeader
      title="成长能力图谱"
      description="把刷题、面试、作品、计划和求职记录连成可解释的能力结构。"
      eyebrow="Capability Graph 1.0"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          {{ graph.evidenceCount || 0 }} 条证据
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          {{ graph.verifiedEvidenceCount || 0 }} 条已验证
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">
          {{ generatedAtText }}
        </CnStatusTag>
      </template>
      <template #actions>
        <el-button plain :loading="loading" @click="loadGraph">
          <el-icon><Refresh /></el-icon>
          刷新图谱
        </el-button>
      </template>
    </CnPageHeader>

    <section class="summary-grid" aria-label="能力图谱摘要">
      <CnStatCard
        title="综合能力分"
        :value="graph.overallScore || 0"
        unit="分"
        description="只基于已落库成长证据计算"
        tone="brand"
        :trend="graph.overallScore >= 70 ? 'up' : 'flat'"
        :trend-text="graph.overallScore >= 70 ? '证据稳定' : '持续积累'"
      />
      <CnStatCard
        title="能力节点"
        :value="graph.nodes?.length || 0"
        unit="个"
        description="学习、执行、交付与求职准备"
        tone="success"
        trend="flat"
        trend-text="结构化"
      />
      <CnStatCard
        title="优先补强"
        :value="graph.gaps?.length || 0"
        unit="项"
        description="来自现有能力弱项服务"
        tone="warning"
        :trend="graph.gaps?.length ? 'down' : 'up'"
        :trend-text="graph.gaps?.length ? '建议行动' : '暂无明显短板'"
      />
    </section>

    <CnSection title="能力节点" description="每个节点都能回溯到最近的证据，不把模型建议当作能力事实。" surface="panel" divided>
      <div v-if="graph.nodes?.length" class="node-grid">
        <article v-for="node in graph.nodes" :key="node.key" class="node-card">
          <header class="node-card-header">
            <div>
              <span class="node-category">{{ categoryLabel(node.category) }}</span>
              <h2>{{ node.title }}</h2>
            </div>
            <div class="node-score" :class="scoreTone(node.score)">
              <strong>{{ node.score || 0 }}</strong>
              <span>分</span>
            </div>
          </header>
          <el-progress :percentage="node.score || 0" :show-text="false" :stroke-width="8" :color="progressColor(node.score)" />
          <div class="node-meta">
            <CnStatusTag size="sm" :type="trendTone(node.trend)" subtle>
              {{ trendLabel(node.trend) }}
            </CnStatusTag>
            <span>{{ node.evidenceCount || 0 }} 条证据</span>
            <span>可信度 {{ node.confidence || 0 }}%</span>
          </div>
          <p class="node-summary">{{ node.summary }}</p>
          <div v-if="node.evidenceRefs?.length" class="evidence-ref-list" aria-label="节点证据">
            <span v-for="reference in node.evidenceRefs" :key="reference.evidenceId" class="evidence-ref">
              {{ evidenceLabel(reference.evidenceType) }}
            </span>
          </div>
          <small v-if="node.latestObservedAt" class="node-updated">最近更新 {{ formatDate(node.latestObservedAt) }}</small>
        </article>
      </div>
      <CnEmptyState v-else title="还没有足够证据" description="先完成一次刷题、面试、计划或作品动作，系统会自动生成能力节点。" icon="RAD" size="sm" surface="transparent" />
    </CnSection>

    <section class="lower-grid">
      <CnSection title="能力关系" description="关系只表示当前图谱的确定性支撑方向。" surface="panel" divided>
        <div v-if="graph.edges?.length" class="edge-list">
          <div v-for="edge in graph.edges" :key="`${edge.source}-${edge.target}`" class="edge-item">
            <span>{{ nodeTitle(edge.source) }}</span>
            <el-icon><ArrowRight /></el-icon>
            <span>{{ nodeTitle(edge.target) }}</span>
            <CnStatusTag type="info" size="sm" subtle>{{ edge.relation }}</CnStatusTag>
          </div>
        </div>
        <CnEmptyState v-else title="暂无关系数据" description="积累更多证据后会形成能力之间的支撑关系。" icon="LINK" size="sm" surface="transparent" />
      </CnSection>

      <CnSection title="优先补强" description="推荐只引用已有资源，不自动修改你的计划。" surface="panel" divided>
        <div v-if="graph.gaps?.length" class="gap-list">
          <article v-for="gap in graph.gaps" :key="gap.skillKey" class="gap-item">
            <div class="gap-copy">
              <div class="gap-title-row">
                <CnStatusTag size="sm" :type="gapTone(gap.level)">{{ gapLevelLabel(gap.level) }}</CnStatusTag>
                <strong>{{ gap.title }}</strong>
              </div>
              <p>{{ gap.explanation }}</p>
              <small>{{ gap.evidenceCount || 0 }} 条证据 · 可信度 {{ gap.confidence || 0 }}%</small>
            </div>
            <el-button v-if="gap.routePath" type="primary" plain size="small" @click="goRoute(gap.routePath)">
              去练习
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </article>
        </div>
        <CnEmptyState v-else title="暂无明确补强项" description="继续保持稳定练习，系统会在出现可验证短板时提醒你。" icon="OK" size="sm" surface="transparent" />
      </CnSection>
    </section>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import growthCoachApi from '@/api/growthCoach'

interface EvidenceReference {
  evidenceId?: string
  evidenceType?: string
  observedAt?: string
}

interface CapabilityNode {
  key: string
  title: string
  category?: string
  score?: number
  confidence?: number
  evidenceCount?: number
  trend?: string
  summary?: string
  latestObservedAt?: string
  evidenceRefs?: EvidenceReference[]
}

interface CapabilityGap {
  skillKey?: string
  title?: string
  level?: string
  confidence?: number
  evidenceCount?: number
  explanation?: string
  routePath?: string
}

interface CapabilityGraph {
  generatedAt?: string
  overallScore?: number
  evidenceCount?: number
  verifiedEvidenceCount?: number
  nodes: CapabilityNode[]
  edges: Array<{ source?: string; target?: string; relation?: string }>
  gaps: CapabilityGap[]
}

const router = useRouter()
const loading = ref(false)
const graph = ref<CapabilityGraph>({ nodes: [], edges: [], gaps: [] })
const breadcrumbs = [{ label: '学习成长' }, { label: '能力图谱' }]

const generatedAtText = computed(() => graph.value.generatedAt ? `更新于 ${formatDate(graph.value.generatedAt)}` : '等待首次计算')

const loadGraph = async () => {
  loading.value = true
  try {
    const response = await growthCoachApi.getCapabilityGraph()
    graph.value = {
      nodes: Array.isArray(response?.nodes) ? response.nodes : [],
      edges: Array.isArray(response?.edges) ? response.edges : [],
      gaps: Array.isArray(response?.gaps) ? response.gaps : [],
      generatedAt: response?.generatedAt,
      overallScore: Number(response?.overallScore || 0),
      evidenceCount: Number(response?.evidenceCount || 0),
      verifiedEvidenceCount: Number(response?.verifiedEvidenceCount || 0)
    }
  } catch (error) {
    ElMessage.error('能力图谱加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goRoute = (path: string) => {
  if (!path) return
  router.push(path)
}

const categoryLabel = (category?: string) => ({
  learning: '学习基础',
  career: '求职准备',
  practice: '项目实战',
  habit: '执行节奏'
}[category || ''] || '成长结构')

const nodeTitle = (key?: string) => graph.value.nodes.find(node => node.key === key)?.title || key || '能力节点'

const trendLabel = (trend?: string) => ({
  rising: '近期上升',
  new: '近期形成',
  cooling: '近期放缓',
  stable: '保持稳定'
}[trend || ''] || '等待数据')

const trendTone = (trend?: string) => trend === 'rising' || trend === 'new' ? 'success' : trend === 'cooling' ? 'warning' : 'info'

const scoreTone = (score?: number) => score && score >= 80 ? 'score-high' : score && score >= 60 ? 'score-mid' : 'score-low'

const progressColor = (score?: number) => score && score >= 80 ? '#35c98f' : score && score >= 60 ? '#f0a340' : '#7b8cff'

const gapTone = (level?: string) => level === 'urgent' ? 'danger' : level === 'needs_practice' ? 'warning' : 'info'

const gapLevelLabel = (level?: string) => level === 'urgent' ? '优先处理' : level === 'needs_practice' ? '需要练习' : '继续观察'

const evidenceLabel = (type?: string) => ({
  OJ_SUBMISSION_RESULT: 'OJ 判题',
  QUESTION_MASTERY: '题目掌握',
  INTERVIEW_SCORE: '模拟面试',
  SQL_REVIEW_RESULT: 'SQL 优化',
  PUBLIC_CODE_ARTIFACT: '公开作品',
  CODE_REVIEW_RESULT: '代码审查',
  TASK_COMPLETED: '计划任务',
  CAREER_STAGE_PROGRESS: '求职阶段',
  CAREER_APPLICATION_STATUS: '投递状态'
}[type || ''] || '成长证据')

const formatDate = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '--'

onMounted(loadGraph)
</script>

<style scoped>
.growth-capabilities-page {
  padding-bottom: var(--cn-space-10);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-5);
}

.node-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.node-card,
.gap-item,
.edge-item {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-lg);
  background: var(--cn-color-surface-raised);
}

.node-card {
  padding: var(--cn-space-5);
}

.node-card-header,
.node-meta,
.gap-title-row,
.edge-item {
  display: flex;
  align-items: center;
}

.node-card-header {
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.node-category {
  color: var(--cn-color-text-tertiary);
  font-size: var(--cn-font-size-xs);
}

.node-card h2 {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-primary);
  font-size: var(--cn-font-size-lg);
}

.node-score {
  display: flex;
  align-items: baseline;
  gap: 3px;
}

.node-score strong {
  font-size: 2rem;
  line-height: 1;
}

.node-score span {
  color: var(--cn-color-text-tertiary);
  font-size: var(--cn-font-size-xs);
}

.score-high { color: var(--cn-color-success); }
.score-mid { color: var(--cn-color-warning); }
.score-low { color: var(--cn-color-brand); }

.node-meta {
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: var(--cn-font-size-xs);
}

.node-summary,
.gap-copy p {
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-secondary);
  line-height: 1.6;
}

.evidence-ref-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-1);
  margin-top: var(--cn-space-4);
}

.evidence-ref {
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--cn-color-fill-subtle);
  color: var(--cn-color-text-secondary);
  font-size: var(--cn-font-size-xs);
}

.node-updated,
.gap-copy small {
  display: block;
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
}

.lower-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.25fr);
  gap: var(--cn-space-5);
  margin-top: var(--cn-space-5);
}

.edge-list,
.gap-list {
  display: grid;
  gap: var(--cn-space-3);
}

.edge-item {
  gap: var(--cn-space-2);
  padding: var(--cn-space-3) var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: var(--cn-font-size-sm);
}

.edge-item span:first-child,
.edge-item span:nth-child(3) {
  color: var(--cn-color-text-primary);
  font-weight: 600;
}

.edge-item :deep(.cn-status-tag) {
  margin-left: auto;
}

.gap-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
}

.gap-copy {
  min-width: 0;
}

.gap-title-row {
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.gap-title-row strong {
  color: var(--cn-color-text-primary);
}

@media (max-width: 1100px) {
  .node-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 780px) {
  .summary-grid,
  .node-grid,
  .lower-grid { grid-template-columns: 1fr; }

  .gap-item { align-items: flex-start; flex-direction: column; }
}
</style>
