<template>
  <CnPage class="job-match-engine-page" max-width="1320px" full-height>
    <CnPageHeader
      title="岗位匹配引擎 2.0"
      description="一次输入简历，批量评估多个岗位 JD，输出可解释评分、差距优先级与下一步动作。"
      eyebrow="MATCH ENGINE"
      :breadcrumbs="[{ label: '首页', to: '/' }, { label: '求职作战台', to: '/job-battle' }, { label: '岗位匹配引擎' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ form.targets.length }} 个岗位</CnStatusTag>
        <CnStatusTag v-if="analysisResult" type="success" size="sm" subtle>
          {{ analysisResult.analysisName || '已生成分析' }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button @click="fillExample">填充示例</el-button>
        <el-button type="primary" :loading="running" @click="runAnalysis">开始分析</el-button>
      </template>
    </CnPageHeader>

    <section class="engine-stats" aria-label="岗位匹配引擎概览">
      <CnStatCard
        title="目标岗位"
        :value="form.targets.length"
        unit="个"
        description="当前批量评估数量"
        tone="brand"
        trend="flat"
        trend-text="输入"
      />
      <CnStatCard
        title="最佳匹配分"
        :value="analysisResult?.bestScore ?? '-'"
        description="最近一次分析结果"
        tone="success"
        trend="flat"
        trend-text="结果"
      />
      <CnStatCard
        title="平均匹配分"
        :value="analysisResult?.averageScore ?? '-'"
        description="所有目标岗位平均值"
        tone="info"
        trend="flat"
        trend-text="排行"
      />
      <CnStatCard
        title="历史分析"
        :value="historyTotal"
        unit="条"
        description="可加载过往分析"
        tone="neutral"
        trend="flat"
        trend-text="历史"
      />
    </section>

    <div class="engine-layout">
      <CnSection class="form-section" title="输入区" description="先输入简历，再添加需要评估的目标岗位 JD。" divided>
        <el-form label-position="top">
          <el-form-item label="简历文本">
            <el-input
              v-model="form.resumeText"
              type="textarea"
              :rows="8"
              placeholder="粘贴简历正文，建议包含项目经历和量化结果。"
            />
          </el-form-item>

          <el-form-item label="项目亮点（可选）">
            <el-input
              v-model="form.projectHighlights"
              type="textarea"
              :rows="3"
              placeholder="例如：主导百万级并发链路优化，接口 P99 延迟下降 42%。"
            />
          </el-form-item>

          <el-form-item label="目标公司类型（可选）">
            <el-select v-model="form.targetCompanyType" clearable class="company-select">
              <el-option label="互联网" value="互联网" />
              <el-option label="外企" value="外企" />
              <el-option label="国企/央企" value="国企/央企" />
              <el-option label="创业公司" value="创业公司" />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="target-toolbar">
          <div>
            <h3>目标岗位列表</h3>
            <p>单次最多添加 10 个岗位。</p>
          </div>
          <el-button type="primary" plain @click="addTarget">新增岗位</el-button>
        </div>

        <div class="target-list">
          <el-card v-for="(target, idx) in form.targets" :key="idx" shadow="never" class="target-item">
            <template #header>
              <div class="target-head">
                <CnStatusTag type="brand" size="sm">岗位 {{ idx + 1 }}</CnStatusTag>
                <el-button v-if="form.targets.length > 1" type="danger" link @click="removeTarget(idx)">
                  删除
                </el-button>
              </div>
            </template>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="8">
                <el-form-item label="岗位名称（可选）">
                  <el-input v-model="target.targetRole" placeholder="如：Java后端开发" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="级别（可选）">
                  <el-input v-model="target.targetLevel" placeholder="如：P6 / 资深" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="城市（可选）">
                  <el-input v-model="target.city" placeholder="如：上海" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="岗位JD">
              <el-input
                v-model="target.jdText"
                type="textarea"
                :rows="6"
                placeholder="粘贴岗位JD全文。"
              />
            </el-form-item>
          </el-card>
        </div>
      </CnSection>

      <CnSection class="tips-section" title="使用建议" description="批量评估前先保证简历证据足够具体。" divided>
        <ol class="tips-list">
          <li>优先放 3-5 个目标岗位，能看到投递优先级。</li>
          <li>简历里尽量包含量化结果，评分稳定性更高。</li>
          <li>关注 P0/P1 差距项，先补核心短板再扩展。</li>
          <li>分析完成后可回到作战台继续生成冲刺计划。</li>
        </ol>
      </CnSection>
    </div>

    <CnSection v-if="analysisResult" class="result-section" title="分析结果" divided>
      <template #actions>
        <CnStatusTag type="success" size="sm">{{ analysisResult.analysisName }}</CnStatusTag>
      </template>

      <div v-if="analysisResult.nextActions?.length" class="next-actions">
        <h4>推荐下一步</h4>
        <div class="action-list">
          <CnStatusTag
            v-for="(item, idx) in analysisResult.nextActions || []"
            :key="idx"
            type="brand"
            size="sm"
            subtle
          >
            {{ item }}
          </CnStatusTag>
        </div>
      </div>

      <CnDataTable
        :columns="rankingColumns"
        :data="rankingRows"
        border
        empty-title="暂无分析结果"
        empty-description="完成一次岗位匹配分析后，这里会展示目标岗位排行。"
      >
        <template #expand="{ row }">
          <div class="expand-panel">
            <div class="expand-block">
              <h5>关键优势</h5>
              <div class="tag-list">
                <CnStatusTag v-for="(s, i) in row.strengths || []" :key="`s-${i}`" size="sm" type="success" subtle>
                  {{ s }}
                </CnStatusTag>
              </div>
            </div>
            <div class="expand-block">
              <h5>缺失关键词</h5>
              <div class="tag-list">
                <CnStatusTag
                  v-for="(k, i) in row.missingKeywords || []"
                  :key="`k-${i}`"
                  size="sm"
                  type="warning"
                  subtle
                >
                  {{ k }}
                </CnStatusTag>
              </div>
            </div>
            <div class="expand-block">
              <h5>差距项（Top3）</h5>
              <div v-for="(g, i) in row.topGaps || []" :key="`g-${i}`" class="gap-item">
                <CnStatusTag size="sm" :type="gapPriorityTone(g.priority)">
                  {{ g.priority || 'P2' }}
                </CnStatusTag>
                <span class="gap-skill">{{ g.skill }}</span>
                <span class="gap-action">{{ g.suggestedAction }}</span>
              </div>
            </div>
          </div>
        </template>

        <template #engineScore="{ row }">
          <div class="score-cell">
            <el-progress :percentage="row.engineScore || 0" :stroke-width="10" :show-text="false" />
            <span class="score-text">{{ row.engineScore }}</span>
          </div>
        </template>

        <template #gapCounts="{ row }">{{ row.p0GapCount || 0 }}/{{ row.p1GapCount || 0 }}</template>

        <template #fallback="{ row }">
          <CnStatusTag size="sm" :type="row.fallback ? 'warning' : 'success'" subtle>
            {{ row.fallback ? '是' : '否' }}
          </CnStatusTag>
        </template>
      </CnDataTable>
    </CnSection>

    <CnSection class="history-section" title="历史分析" description="加载历史结果后可直接查看排行与差距项。" divided>
      <template #actions>
        <el-input
          v-model="historyQuery.keyword"
          class="history-search"
          placeholder="搜索分析名称/岗位"
          clearable
          @keyup.enter="handleHistorySearch"
        />
        <el-button type="primary" :loading="loadingHistory" @click="handleHistorySearch">查询</el-button>
      </template>

      <CnDataTable
        :columns="historyColumns"
        :data="historyList"
        :loading="loadingHistory"
        :pagination="historyPagination"
        border
        empty-title="暂无历史分析"
        empty-description="开始一次分析后，历史记录会出现在这里。"
        @page-change="handleHistoryPageChange"
      >
        <template #historyActions="{ row }">
          <el-button type="primary" link @click="applyHistory(row.id)">加载</el-button>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { jobBattleApi } from '@/api/jobBattle'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'

const rankingColumns = [
  { type: 'expand', slot: 'expand', width: 54 },
  { prop: 'rank', label: '#', width: 54 },
  { prop: 'targetRole', label: '岗位', minWidth: 160, showOverflowTooltip: true },
  { prop: 'targetLevel', label: '级别', minWidth: 100, showOverflowTooltip: true },
  { prop: 'city', label: '城市', minWidth: 90, showOverflowTooltip: true },
  { prop: 'engineScore', label: '引擎分', minWidth: 150, slot: 'engineScore' },
  { prop: 'overallScore', label: '匹配总分', width: 98 },
  { prop: 'estimatedPassRate', label: '通过率', width: 88 },
  { label: 'P0/P1', width: 88, slot: 'gapCounts' },
  { label: '降级', width: 76, slot: 'fallback' }
]

const historyColumns = [
  { prop: 'id', label: 'ID', width: 76 },
  { prop: 'analysisName', label: '分析名称', minWidth: 220, showOverflowTooltip: true },
  { prop: 'bestTargetRole', label: '最佳岗位', minWidth: 140, showOverflowTooltip: true },
  { prop: 'bestScore', label: '最佳分', width: 90 },
  { prop: 'targetCount', label: '岗位数', width: 80 },
  { prop: 'createTime', label: '时间', minWidth: 170, showOverflowTooltip: true },
  { label: '操作', width: 110, slot: 'historyActions' }
]

const running = ref(false)
const loadingHistory = ref(false)
const analysisResult = ref(null)
const historyList = ref([])
const historyTotal = ref(0)

const historyQuery = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 8
})

const rankingRows = computed(() => analysisResult.value?.ranking || [])

const historyPagination = computed(() => ({
  page: historyQuery.pageNum,
  pageSize: historyQuery.pageSize,
  total: historyTotal.value,
  layout: 'total, prev, pager, next'
}))

const form = reactive({
  resumeText: '',
  projectHighlights: '',
  targetCompanyType: '互联网',
  targets: [createTarget()]
})

function createTarget() {
  return {
    targetRole: '',
    targetLevel: '',
    city: '',
    jdText: ''
  }
}

function addTarget() {
  if (form.targets.length >= 10) {
    ElMessage.warning('单次最多添加10个岗位')
    return
  }
  form.targets.push(createTarget())
}

function removeTarget(index) {
  form.targets.splice(index, 1)
}

function validateForm() {
  if (!form.resumeText || !form.resumeText.trim()) {
    ElMessage.warning('请先输入简历文本')
    return false
  }
  const invalid = form.targets.findIndex(item => !item.jdText || !item.jdText.trim())
  if (invalid !== -1) {
    ElMessage.warning(`请补全岗位${invalid + 1}的JD文本`)
    return false
  }
  return true
}

async function runAnalysis() {
  if (!validateForm()) return
  running.value = true
  try {
    const payload = {
      resumeText: form.resumeText.trim(),
      projectHighlights: form.projectHighlights?.trim() || '',
      targetCompanyType: form.targetCompanyType || '',
      targets: form.targets.map(item => ({
        targetRole: item.targetRole?.trim() || '',
        targetLevel: item.targetLevel?.trim() || '',
        city: item.city?.trim() || '',
        jdText: item.jdText?.trim() || ''
      }))
    }
    const data = await jobBattleApi.runMatchEngine(payload)
    analysisResult.value = data
    ElMessage.success('岗位匹配分析完成')
    loadHistory()
  } catch (error) {
    console.error('岗位匹配分析失败', error)
    ElMessage.error('岗位匹配分析失败')
  } finally {
    running.value = false
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const data = await jobBattleApi.getMatchEngineHistory({
      keyword: historyQuery.keyword,
      pageNum: historyQuery.pageNum,
      pageSize: historyQuery.pageSize
    })
    historyList.value = data?.records || []
    historyTotal.value = data?.total || 0
  } catch (error) {
    console.error('获取历史记录失败', error)
    ElMessage.error('获取历史记录失败')
  } finally {
    loadingHistory.value = false
  }
}

function handleHistorySearch() {
  historyQuery.pageNum = 1
  loadHistory()
}

function handleHistoryPageChange(page) {
  historyQuery.pageNum = page
  loadHistory()
}

function gapPriorityTone(priority) {
  if (priority === 'P0') {
    return 'danger'
  }
  if (priority === 'P1') {
    return 'warning'
  }
  return 'info'
}

async function applyHistory(id) {
  try {
    const data = await jobBattleApi.getMatchEngineHistoryDetail(id)
    analysisResult.value = data
    ElMessage.success('已加载历史分析结果')
  } catch (error) {
    console.error('加载历史详情失败', error)
    ElMessage.error('加载历史详情失败')
  }
}

async function loadLatest() {
  try {
    const data = await jobBattleApi.getLatestMatchEngineResult()
    if (data) {
      analysisResult.value = data
    }
  } catch (error) {
    console.warn('加载最近分析结果失败', error)
  }
}

function fillExample() {
  form.resumeText = `5年Java后端开发经验，负责支付与订单核心链路。熟悉 Spring Boot、MySQL、Redis、Kafka，主导过性能优化与容灾演练。最近一年负责网关限流与缓存策略改造，接口P99从380ms降到210ms。`
  form.projectHighlights = '负责高并发交易系统优化，参与服务拆分和链路压测，推动故障演练制度化。'
  form.targetCompanyType = '互联网'
  form.targets = [
    {
      targetRole: 'Java后端开发',
      targetLevel: '资深',
      city: '上海',
      jdText: '负责交易系统后端研发，要求熟悉 Java、Spring Boot、MySQL、Redis、Kafka，具备高并发和分布式架构经验，有性能优化实战经验。'
    },
    {
      targetRole: '后端工程师',
      targetLevel: 'P6',
      city: '杭州',
      jdText: '负责平台中台服务开发，要求 Java 基础扎实，熟悉微服务治理、缓存策略、数据库调优，具备跨团队沟通能力。'
    }
  ]
}

onMounted(async () => {
  await Promise.all([loadLatest(), loadHistory()])
})
</script>

<style scoped>
.job-match-engine-page {
  min-height: calc(100vh - 68px);
}

.engine-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-4);
}

.engine-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: var(--cn-space-4);
  align-items: start;
}

.form-section,
.tips-section,
.result-section,
.history-section {
  margin-bottom: var(--cn-space-4);
}

.company-select {
  width: 260px;
  max-width: 100%;
}

.target-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--cn-space-3);
  margin: var(--cn-space-2) 0 var(--cn-space-3);
}

.target-toolbar h3 {
  margin: 0;
  font-size: 16px;
  color: var(--cn-color-text-primary);
}

.target-toolbar p {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.target-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-3);
}

.target-item {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.target-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-2);
}

.tips-list {
  margin: 0;
  padding-left: var(--cn-space-5);
  color: var(--cn-color-text-secondary);
  line-height: 1.8;
}

.next-actions {
  margin-bottom: var(--cn-space-4);
}

.next-actions h4 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.action-list,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.score-cell {
  display: grid;
  grid-template-columns: minmax(88px, 1fr) auto;
  gap: var(--cn-space-2);
  align-items: center;
}

.score-text {
  font-weight: 600;
  color: var(--cn-color-brand-primary);
}

.expand-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--cn-space-3);
}

.expand-block {
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.expand-block h5 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.gap-item {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-2);
  padding-left: var(--cn-space-2);
  border-left: 3px solid var(--cn-color-border);
}

.gap-skill {
  font-weight: 600;
  color: var(--cn-color-text-primary);
}

.gap-action {
  flex-basis: 100%;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.history-search {
  width: 220px;
}

@media (max-width: 1180px) {
  .engine-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 992px) {
  .engine-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .engine-stats {
    grid-template-columns: 1fr;
  }

  .target-toolbar {
    display: grid;
  }

  .history-search {
    width: 100%;
  }
}
</style>

