<template>
  <CnPage class="learning-assets-statistics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="学习资产统计"
      description="追踪转化成功率、资产发布率、编辑率、驳回率和高质量来源表现。"
      eyebrow="Learning Assets Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">转化 {{ statistics.overview.totalTransforms || 0 }} 次</CnStatusTag>
        <CnStatusTag type="success">成功率 {{ formatPercent(statistics.overview.transformSuccessRate) }}</CnStatusTag>
        <CnStatusTag type="danger">驳回率 {{ formatPercent(statistics.overview.rejectRate) }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadStatistics">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <div class="overview-grid">
      <CnStatCard
        title="总转化次数"
        :value="statistics.overview.totalTransforms || 0"
        description="用户触发内容转学习资产的次数"
        tone="brand"
        :loading="loading"
      />
      <CnStatCard
        title="转化成功率"
        :value="formatPercent(statistics.overview.transformSuccessRate)"
        description="成功生成候选资产的比例"
        tone="success"
        :loading="loading"
      />
      <CnStatCard
        title="用户编辑率"
        :value="formatPercent(statistics.overview.editRate)"
        description="候选进入审核前被编辑的比例"
        tone="warning"
        :loading="loading"
      />
      <CnStatCard
        title="审核驳回率"
        :value="formatPercent(statistics.overview.rejectRate)"
        description="审核阶段被驳回的候选比例"
        tone="danger"
        :loading="loading"
      />
    </div>

    <div class="statistics-grid">
      <CnSection title="来源类型转化成功率" description="按来源模块查看转化成功表现。" divided>
        <CnDataTable
          :columns="sourceColumns"
          :data="statistics.sourceStats"
          :loading="loading"
          :pagination="null"
          row-key="sourceTypeText"
          empty-title="暂无来源统计"
          empty-description="当前还没有可展示的来源转化数据。"
          empty-icon="LS"
        >
          <template #successRate="{ row }">
            <CnStatusTag type="success" size="sm">{{ formatPercent(row.successRate) }}</CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>

      <CnSection title="资产类型发布率" description="按资产类型查看候选、发布、审核中和驳回情况。" divided>
        <CnDataTable
          :columns="assetColumns"
          :data="statistics.assetStats"
          :loading="loading"
          :pagination="null"
          row-key="assetTypeText"
          empty-title="暂无资产统计"
          empty-description="当前还没有可展示的资产发布数据。"
          empty-icon="LA"
        >
          <template #publishRate="{ row }">
            <CnStatusTag type="brand" size="sm">{{ formatPercent(row.publishRate) }}</CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>

      <CnSection title="常见失败原因" description="转化或审核失败的原因聚合。" divided>
        <CnDataTable
          :columns="failReasonColumns"
          :data="statistics.failReasonStats"
          :loading="loading"
          :pagination="null"
          row-key="failReason"
          empty-title="暂无失败原因"
          empty-description="当前没有失败原因数据。"
          empty-icon="FR"
        />
      </CnSection>

      <CnSection title="高质量来源排行" description="发布资产数量较高的来源内容。" divided>
        <CnDataTable
          :columns="topSourceColumns"
          :data="statistics.topSourceStats"
          :loading="loading"
          :pagination="null"
          row-key="sourceTitle"
          empty-title="暂无高质量来源"
          empty-description="当前没有高质量来源排行数据。"
          empty-icon="TS"
        />
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import learningAssetAdminApi from '@/api/learningAssets'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface OverviewStats extends Record<string, unknown> {
  totalTransforms?: number
  transformSuccessRate?: number
  editRate?: number
  rejectRate?: number
}

interface SourceStats extends Record<string, unknown> {
  sourceTypeText?: string
  totalCount?: number
  successCount?: number
  successRate?: number
}

interface AssetStats extends Record<string, unknown> {
  assetTypeText?: string
  totalCount?: number
  publishedCount?: number
  reviewingCount?: number
  rejectedCount?: number
  publishRate?: number
}

interface FailReasonStats extends Record<string, unknown> {
  failReason?: string
  count?: number
}

interface TopSourceStats extends Record<string, unknown> {
  sourceTypeText?: string
  sourceTitle?: string
  publishedCount?: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '学习资产' }, { label: '统计分析' }]

const loading = ref(false)
const statistics = reactive({
  overview: {} as OverviewStats,
  sourceStats: [] as SourceStats[],
  assetStats: [] as AssetStats[],
  failReasonStats: [] as FailReasonStats[],
  topSourceStats: [] as TopSourceStats[]
})

const sourceColumns: CnTableColumn<SourceStats>[] = [
  { prop: 'sourceTypeText', label: '来源类型', minWidth: 140, showOverflowTooltip: true },
  { prop: 'totalCount', label: '总转化', width: 100 },
  { prop: 'successCount', label: '成功数', width: 100 },
  { prop: 'successRate', label: '成功率', width: 120, slot: 'successRate' }
]

const assetColumns: CnTableColumn<AssetStats>[] = [
  { prop: 'assetTypeText', label: '资产类型', minWidth: 140, showOverflowTooltip: true },
  { prop: 'totalCount', label: '总候选', width: 90 },
  { prop: 'publishedCount', label: '已发布', width: 90 },
  { prop: 'reviewingCount', label: '审核中', width: 90 },
  { prop: 'rejectedCount', label: '已驳回', width: 90 },
  { prop: 'publishRate', label: '发布率', width: 120, slot: 'publishRate' }
]

const failReasonColumns: CnTableColumn<FailReasonStats>[] = [
  { prop: 'failReason', label: '失败原因', minWidth: 260, showOverflowTooltip: true },
  { prop: 'count', label: '次数', width: 100 }
]

const topSourceColumns: CnTableColumn<TopSourceStats>[] = [
  { prop: 'sourceTypeText', label: '来源类型', width: 120, showOverflowTooltip: true },
  { prop: 'sourceTitle', label: '来源标题', minWidth: 240, showOverflowTooltip: true },
  { prop: 'publishedCount', label: '已发布资产数', width: 130 }
]

const formatPercent = (value?: number) => {
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
    statistics.overview = res?.overview || {}
    statistics.sourceStats = Array.isArray(res?.sourceStats) ? res.sourceStats : []
    statistics.assetStats = Array.isArray(res?.assetStats) ? res.assetStats : []
    statistics.failReasonStats = Array.isArray(res?.failReasonStats) ? res.failReasonStats : []
    statistics.topSourceStats = Array.isArray(res?.topSourceStats) ? res.topSourceStats : []
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
  min-height: 100%;
}

.overview-grid,
.statistics-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.overview-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.statistics-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

@media (max-width: 1180px) {
  .overview-grid,
  .statistics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .overview-grid,
  .statistics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
