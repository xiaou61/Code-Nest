<template>
  <div class="learning-assets-statistics-page">
    <el-card shadow="never" class="hero-card">
      <div class="hero-header">
        <div>
          <h2>学习资产统计</h2>
          <p>追踪转化成功率、资产发布率、编辑率、驳回率和高质量来源表现。</p>
        </div>
        <el-button type="primary" :loading="loading" @click="loadStatistics">刷新数据</el-button>
      </div>
    </el-card>

    <el-row :gutter="16" class="overview-grid">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">总转化次数</div>
          <div class="metric-value">{{ statistics.overview.totalTransforms || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">转化成功率</div>
          <div class="metric-value success">{{ formatPercent(statistics.overview.transformSuccessRate) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">用户编辑率</div>
          <div class="metric-value warning">{{ formatPercent(statistics.overview.editRate) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">审核驳回率</div>
          <div class="metric-value danger">{{ formatPercent(statistics.overview.rejectRate) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>来源类型转化成功率</span>
          </template>
          <el-table :data="statistics.sourceStats" v-loading="loading" border>
            <el-table-column prop="sourceTypeText" label="来源类型" min-width="140" />
            <el-table-column prop="totalCount" label="总转化" min-width="100" />
            <el-table-column prop="successCount" label="成功数" min-width="100" />
            <el-table-column label="成功率" min-width="120">
              <template #default="{ row }">
                <el-tag type="success">{{ formatPercent(row.successRate) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>资产类型发布率</span>
          </template>
          <el-table :data="statistics.assetStats" v-loading="loading" border>
            <el-table-column prop="assetTypeText" label="资产类型" min-width="140" />
            <el-table-column prop="totalCount" label="总候选" min-width="90" />
            <el-table-column prop="publishedCount" label="已发布" min-width="90" />
            <el-table-column prop="reviewingCount" label="审核中" min-width="90" />
            <el-table-column prop="rejectedCount" label="已驳回" min-width="90" />
            <el-table-column label="发布率" min-width="120">
              <template #default="{ row }">
                <el-tag type="primary">{{ formatPercent(row.publishRate) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>常见失败原因</span>
          </template>
          <el-table :data="statistics.failReasonStats" v-loading="loading" border>
            <el-table-column prop="failReason" label="失败原因" min-width="260" show-overflow-tooltip />
            <el-table-column prop="count" label="次数" min-width="100" />
          </el-table>
          <el-empty
            v-if="!loading && !statistics.failReasonStats.length"
            description="当前没有失败原因数据"
          />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="table-card">
          <template #header>
            <span>高质量来源排行</span>
          </template>
          <el-table :data="statistics.topSourceStats" v-loading="loading" border>
            <el-table-column prop="sourceTypeText" label="来源类型" min-width="120" />
            <el-table-column prop="sourceTitle" label="来源标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="publishedCount" label="已发布资产数" min-width="120" />
          </el-table>
          <el-empty
            v-if="!loading && !statistics.topSourceStats.length"
            description="当前没有高质量来源排行"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import learningAssetAdminApi from '@/api/learningAssets'

const loading = ref(false)
const statistics = reactive({
  overview: {},
  sourceStats: [],
  assetStats: [],
  failReasonStats: [],
  topSourceStats: []
})

const formatPercent = (value) => {
  const numberValue = Number(value || 0)
  if (Number.isNaN(numberValue)) {
    return '0%'
  }
  return `${numberValue.toFixed(2)}%`
}

const loadStatistics = async () => {
  loading.value = true
  try {
    const res = await learningAssetAdminApi.getStatistics()
    statistics.overview = res.overview || {}
    statistics.sourceStats = res.sourceStats || []
    statistics.assetStats = res.assetStats || []
    statistics.failReasonStats = res.failReasonStats || []
    statistics.topSourceStats = res.topSourceStats || []
  } catch (error) {
    console.error('加载学习资产统计失败', error)
    ElMessage.error('加载学习资产统计失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.learning-assets-statistics-page {
  padding: 20px;
}

.hero-card,
.table-card {
  margin-bottom: 16px;
}

.hero-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.hero-header h2 {
  margin: 0 0 8px;
}

.hero-header p {
  margin: 0;
  color: #909399;
}

.overview-grid {
  margin-bottom: 16px;
}

.metric-card {
  border-radius: 16px;
}

.metric-label {
  color: #909399;
  margin-bottom: 10px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.metric-value.success {
  color: #67c23a;
}

.metric-value.warning {
  color: #e6a23c;
}

.metric-value.danger {
  color: #f56c6c;
}

@media (max-width: 768px) {
  .learning-assets-statistics-page {
    padding: 12px;
  }

  .hero-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
