<template>
  <div class="ai-governance-page">
    <div class="page-header">
      <div>
        <h2>AI治理</h2>
        <p>{{ overview.summary || '查看AI工作流治理状态、兜底覆盖与风险分布' }}</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadOverview">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">工作流总数</div>
          <div class="stat-value">{{ overview.totalWorkflows || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">已配置工作流</div>
          <div class="stat-value">{{ overview.configuredWorkflows || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">兜底覆盖率</div>
          <div class="stat-value">{{ formatRate(overview.fallbackCoverageRate) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">健康等级</div>
          <div class="stat-value">
            <el-tag :type="overview.healthy ? 'success' : 'warning'" effect="light">
              {{ overview.healthLevel || '-' }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-title">工作流治理目录</div>
          </template>
          <el-table v-loading="loading" :data="overview.workflows || []" stripe>
            <el-table-column prop="workflowName" label="工作流" min-width="190" show-overflow-tooltip />
            <el-table-column prop="domain" label="域" width="120" />
            <el-table-column prop="workflowId" label="工作流ID" width="150" />
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
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { aiGovernanceApi } from '@/api/aiGovernance'

const loading = ref(false)
const overview = ref({
  workflows: [],
  qualityRules: [],
  improvementSuggestions: []
})

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

const loadOverview = async () => {
  loading.value = true
  try {
    overview.value = await aiGovernanceApi.getOverview()
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
}

.panel-title {
  color: #243b53;
  font-size: 16px;
  font-weight: 600;
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

@media (max-width: 768px) {
  .ai-governance-page {
    padding: 12px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
