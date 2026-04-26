<template>
  <div class="ai-governance-page">
    <div class="page-header">
      <div>
        <h2>AI Runtime 质量与治理中心</h2>
        <p>{{ overview.summary || '查看AI场景治理状态、质量评分、运行观测与风险分布' }}</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadOverview">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">质量分</div>
          <div class="stat-value score-value">
            {{ overview.qualityScore || 0 }}
            <el-tag :type="overview.healthy ? 'success' : 'warning'" effect="light">
              {{ overview.qualityGrade || '-' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">AI场景</div>
          <div class="stat-value">{{ overview.configuredWorkflows || 0 }} / {{ overview.totalWorkflows || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">运行成功率</div>
          <div class="stat-value">{{ formatRate(runtimeInsight.successRate) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">风险队列</div>
          <div class="stat-value">
            {{ riskItems.length }}
            <span class="stat-unit">项</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row quality-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">治理覆盖矩阵</div>
          </template>
          <div class="coverage-grid">
            <div v-for="item in coverageMatrix" :key="item.key" class="coverage-item">
              <div class="coverage-head">
                <strong>{{ item.name }}</strong>
                <el-tag size="small" :type="coverageTagType(item.status)" effect="light">
                  {{ statusText(item.status) }}
                </el-tag>
              </div>
              <div class="coverage-rate">{{ formatRate(item.rate) }}</div>
              <el-progress :percentage="safePercent(item.rate)" :show-text="false" :stroke-width="8" />
              <p>{{ item.covered || 0 }} / {{ item.total || 0 }} · {{ item.description }}</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">运行时洞察</div>
          </template>
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
              <span>Schema失败率</span>
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
            <el-tag :type="overview.healthy ? 'success' : 'warning'" effect="light">
              {{ runtimeInsight.statusText || overview.healthLevel || '-' }}
            </el-tag>
            <span>最近调用：{{ runtimeInsight.lastInvocationAt || '-' }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="panel-card risk-card">
      <template #header>
        <div class="panel-title">质量风险队列</div>
      </template>
      <el-table v-loading="loading" :data="riskItems" stripe>
        <el-table-column label="级别" width="92" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="riskTagMap[row.severity] || 'info'" effect="light">
              {{ riskLabelMap[row.severity] || row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scene" label="场景" min-width="160" show-overflow-tooltip />
        <el-table-column prop="metric" label="指标" width="120" />
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column prop="suggestion" label="建议" min-width="260" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
      </el-table>
    </el-card>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">AI场景治理目录</div>
          </template>
          <el-table v-loading="loading" :data="overview.workflows || []" stripe>
            <el-table-column prop="workflowName" label="场景" min-width="190" show-overflow-tooltip />
            <el-table-column prop="domain" label="域" width="120" />
            <el-table-column prop="workflowId" label="Prompt ID" width="170" />
            <el-table-column label="配置" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.configured ? 'success' : 'danger'">
                  {{ row.configured ? '已配置' : '缺失' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="兜底" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.fallbackCovered ? 'success' : 'warning'">
                  {{ row.fallbackCovered ? '已覆盖' : '待补齐' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Schema" width="95" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.schemaCovered ? 'success' : 'warning'">
                  {{ row.schemaCovered ? '已覆盖' : '待补齐' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="RAG" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.ragCovered ? 'success' : 'info'">
                  {{ row.ragCovered ? '已登记' : '未登记' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="观测" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.runtimeObserved ? 'success' : 'info'">
                  {{ row.runtimeObserved ? '有样本' : '无样本' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" effect="light" :type="riskTagMap[row.riskLevel] || 'info'">
                  {{ riskLabelMap[row.riskLevel] || row.riskLevel }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">质量规则</div>
          </template>
          <div class="rule-list">
            <div v-for="(rule, index) in overview.qualityRules || []" :key="`rule-${index}`" class="rule-item">
              {{ index + 1 }}. {{ rule }}
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="panel-card suggestion-card">
          <template #header>
            <div class="panel-title">改进建议</div>
          </template>
          <div class="rule-list">
            <div
              v-for="(item, index) in overview.improvementSuggestions || []"
              :key="`suggestion-${index}`"
              class="rule-item suggestion-item"
            >
              {{ item }}
            </div>
          </div>
          <div class="generated-time">生成时间：{{ formatTime(overview.generatedAt) }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { aiGovernanceApi } from '@/api/aiGovernance'

const loading = ref(false)
const overview = ref({
  workflows: [],
  qualityRules: [],
  improvementSuggestions: [],
  runtimeInsight: {},
  coverageMatrix: [],
  riskItems: []
})

const runtimeInsight = computed(() => overview.value?.runtimeInsight || {})
const coverageMatrix = computed(() => overview.value?.coverageMatrix || [])
const riskItems = computed(() => overview.value?.riskItems || [])

const riskTagMap = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger'
}

const riskLabelMap = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高'
}

const statusText = (status) => {
  const map = {
    STABLE: '稳定',
    WATCH: '观察',
    RISK: '风险'
  }
  return map[status] || status || '-'
}

const coverageTagType = (status) => {
  const map = {
    STABLE: 'success',
    WATCH: 'warning',
    RISK: 'danger'
  }
  return map[status] || 'info'
}

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = await aiGovernanceApi.getOverview() || {
      workflows: [],
      qualityRules: [],
      improvementSuggestions: [],
      runtimeInsight: {},
      coverageMatrix: [],
      riskItems: []
    }
  } catch (error) {
    console.error('加载AI治理总览失败:', error)
    ElMessage.error('加载AI治理总览失败')
  } finally {
    loading.value = false
  }
}

const formatRate = (value) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return '0%'
  }
  return `${numberValue.toFixed(1)}%`
}

const safePercent = (value) => {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return 0
  }
  return Math.max(0, Math.min(100, numberValue))
}

const formatTime = (value) => {
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
.ai-governance-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin: 0;
  color: #7a8aa6;
  font-size: 14px;
}

.stat-row,
.content-row {
  margin-bottom: 16px;
}

.stat-card,
.panel-card {
  border-radius: 8px;
}

.stat-label {
  color: #7a8aa6;
  font-size: 13px;
  margin-bottom: 10px;
}

.stat-value {
  min-height: 32px;
  color: #1f2d3d;
  font-size: 26px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-value {
  color: #1f63c5;
}

.stat-unit {
  color: #8a9ab3;
  font-size: 14px;
  font-weight: 500;
}

.panel-title {
  color: #243b53;
  font-size: 16px;
  font-weight: 600;
}

.quality-row {
  align-items: stretch;
}

.coverage-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.coverage-item {
  border: 1px solid #dce8fb;
  border-radius: 8px;
  padding: 12px;
  background: #f8fbff;
  min-width: 0;
}

.coverage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.coverage-head strong {
  color: #243b53;
  font-size: 13px;
}

.coverage-rate {
  color: #1f63c5;
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.coverage-item p {
  margin: 8px 0 0;
  color: #607089;
  font-size: 12px;
  line-height: 1.55;
}

.runtime-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.runtime-item {
  border: 1px solid #dce8fb;
  border-radius: 8px;
  padding: 10px;
  background: #f8fbff;
}

.runtime-item span {
  display: block;
  color: #7a8aa6;
  font-size: 12px;
  margin-bottom: 8px;
}

.runtime-item strong {
  color: #243b53;
  font-size: 18px;
}

.runtime-foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #7a8aa6;
  font-size: 12px;
}

.risk-card {
  margin-bottom: 16px;
}

.suggestion-card {
  margin-top: 16px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rule-item {
  border: 1px solid #dce8fb;
  border-radius: 8px;
  padding: 10px 12px;
  color: #425673;
  line-height: 1.6;
  background: #f8fbff;
}

.suggestion-item {
  border-color: #d9ecff;
  background: #f5faff;
}

.generated-time {
  margin-top: 14px;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .coverage-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ai-governance-page {
    padding: 12px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

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
