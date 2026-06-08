<template>
  <CnPage class="sensitive-statistics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="统计分析"
      description="查看敏感词检测总览、命中趋势、热门词汇和分类模块分布，支持按时间范围和业务模块导出报表。"
      eyebrow="Sensitive Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">检测 {{ overview.totalCheck || 0 }} 次</CnStatusTag>
        <CnStatusTag type="danger">命中 {{ overview.hitCount || 0 }} 次</CnStatusTag>
        <CnStatusTag type="warning">拦截率 {{ formatPercent(overview.hitRate) }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="handleQuery">刷新</el-button>
        <el-button type="success" :icon="Download" :loading="exportingReport" @click="handleExportReport">
          导出报表
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按日期范围和业务模块查看检测命中情况。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        search-text="查询"
        @update:model-value="handleQueryFormUpdate"
        @search="handleQuery"
        @reset="resetQuery"
      />
    </CnSection>

    <div class="sensitive-stat-grid">
      <CnStatCard title="总检测次数" :value="overview.totalCheck || 0" description="当前筛选条件下的检测总量" tone="brand" />
      <CnStatCard title="敏感词命中" :value="overview.hitCount || 0" description="当前筛选条件下的命中次数" tone="danger" />
      <CnStatCard title="拦截率" :value="formatPercent(overview.hitRate)" description="命中次数占检测总量比例" tone="warning" />
      <CnStatCard title="违规用户数" :value="overview.violationUserCount || 0" description="发生命中行为的用户数量" tone="info" />
    </div>

    <CnSection title="命中趋势" description="按日期展示敏感词命中次数变化。" divided>
      <div ref="trendChartRef" class="chart-container"></div>
    </CnSection>

    <div class="analysis-grid">
      <CnSection title="热门敏感词 TOP 10" description="按命中次数排序的高频词汇。" divided>
        <CnDataTable
          :columns="hotWordColumns"
          :data="hotWords"
          :loading="loading"
          :pagination="null"
          row-key="word"
          empty-title="暂无热词"
          empty-description="当前筛选条件下没有热门敏感词数据。"
        >
          <template #word="{ row, $index }">
            <div class="hot-word-cell">
              <CnStatusTag :type="$index < 3 ? 'danger' : 'neutral'" size="sm">#{{ $index + 1 }}</CnStatusTag>
              <span>{{ row.word || '-' }}</span>
            </div>
          </template>

          <template #hitCount="{ row }">
            <CnStatusTag type="danger" size="sm">{{ row.hitCount || 0 }}</CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>

      <CnSection title="分类分布" description="按敏感词分类统计命中占比。" divided>
        <div ref="categoryChartRef" class="chart-container-small"></div>
      </CnSection>

      <CnSection title="模块分布" description="按业务模块统计命中次数。" divided>
        <div ref="moduleChartRef" class="chart-container-small"></div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { graphic, init, use } from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import {
  exportStatisticsReport,
  getCategoryDistribution,
  getHitTrend,
  getHotWords,
  getModuleDistribution,
  getStatisticsOverview
} from '@/api/sensitive'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnTableColumn } from '@/design-system'

use([LineChart, PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

interface StatisticsOverview {
  totalCheck?: number
  hitCount?: number
  hitRate?: number
  violationUserCount?: number
}

interface StatisticsQuery {
  startDate: string
  endDate: string
  module: string
  dateRange: string[]
}

interface TrendPoint {
  date: string
  hitCount?: number
}

interface HotWord {
  word: string
  hitCount: number
}

interface DistributionItem {
  category?: string
  name?: string
  categoryName?: string
  module?: string
  moduleName?: string
  count?: number
  value?: number
}

interface ExportResult {
  content?: string
  fileName?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '统计分析' }]

const overview = ref<StatisticsOverview>({})
const hotWords = ref<HotWord[]>([])
const trendChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
const moduleChartRef = ref<HTMLElement>()
const loading = ref(false)
const exportingReport = ref(false)
let trendChart: ECharts | null = null
let categoryChart: ECharts | null = null
let moduleChart: ECharts | null = null

const queryForm = reactive<StatisticsQuery>({
  startDate: '',
  endDate: '',
  module: '',
  dateRange: []
})

const filterFields: CnFilterField[] = [
  { prop: 'dateRange', label: '日期范围', type: 'daterange', placeholder: '开始日期' },
  {
    prop: 'module',
    label: '业务模块',
    type: 'select',
    placeholder: '请选择模块',
    options: [
      { label: '全部', value: '' },
      { label: '社区', value: 'community' },
      { label: '面试', value: 'interview' },
      { label: '朋友圈', value: 'moment' },
      { label: '博客', value: 'blog' }
    ]
  }
]

const hotWordColumns: CnTableColumn<HotWord>[] = [
  { prop: 'word', label: '敏感词', minWidth: 140, slot: 'word', showOverflowTooltip: true },
  { prop: 'hitCount', label: '命中次数', width: 100, align: 'right', slot: 'hitCount' }
]

const resolveCssColor = (variableName: string, fallback: string) => {
  if (typeof window === 'undefined') return fallback

  const probe = document.createElement('span')
  probe.style.color = `var(${variableName})`
  probe.style.display = 'none'
  document.body.appendChild(probe)
  const color = window.getComputedStyle(probe).color
  document.body.removeChild(probe)

  return color || fallback
}

onMounted(() => {
  setDefaultDateRange()
  handleQuery()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
  moduleChart?.dispose()
})

const setDefaultDateRange = () => {
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(startDate.getDate() - 7)

  queryForm.dateRange = [formatDate(startDate), formatDate(endDate)]
  syncDateFields()
}

const syncDateFields = () => {
  if (Array.isArray(queryForm.dateRange) && queryForm.dateRange.length === 2) {
    queryForm.startDate = queryForm.dateRange[0]
    queryForm.endDate = queryForm.dateRange[1]
    return
  }

  queryForm.startDate = ''
  queryForm.endDate = ''
}

const handleQueryFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
  syncDateFields()
}

const handleQuery = async () => {
  syncDateFields()
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadHitTrend(),
      loadHotWords(),
      loadCategoryDistribution(),
      loadModuleDistribution()
    ])
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryForm.module = ''
  setDefaultDateRange()
  handleQuery()
}

const handleExportReport = async () => {
  exportingReport.value = true
  try {
    const result: ExportResult = await exportStatisticsReport(getQueryPayload())
    const blob = new Blob([result?.content || ''], { type: 'text/csv;charset=utf-8;' })
    const fileName = result?.fileName || `sensitive_statistics_${Date.now()}.csv`
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('统计报表导出成功')
  } catch (error) {
    console.error('统计报表导出失败:', error)
    ElMessage.error('统计报表导出失败')
  } finally {
    exportingReport.value = false
  }
}

const loadOverview = async () => {
  try {
    overview.value = await getStatisticsOverview(getQueryPayload())
  } catch (error) {
    console.error('加载数据总览失败:', error)
    ElMessage.error('加载数据总览失败')
  }
}

const loadHitTrend = async () => {
  try {
    const data: TrendPoint[] = await getHitTrend(getQueryPayload())
    await nextTick()
    renderTrendChart(Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('加载趋势数据失败:', error)
    ElMessage.error('加载趋势数据失败')
  }
}

const loadHotWords = async () => {
  try {
    const data = await getHotWords({
      ...getQueryPayload(),
      limit: 10
    })
    hotWords.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载热词数据失败:', error)
    ElMessage.error('加载热词数据失败')
  }
}

const loadCategoryDistribution = async () => {
  try {
    const data: DistributionItem[] = await getCategoryDistribution(getQueryPayload())
    await nextTick()
    renderCategoryChart(Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('加载分类分布失败:', error)
    ElMessage.error('加载分类分布失败')
  }
}

const loadModuleDistribution = async () => {
  try {
    const data: DistributionItem[] = await getModuleDistribution(getQueryPayload())
    await nextTick()
    renderModuleChart(Array.isArray(data) ? data : [])
  } catch (error) {
    console.error('加载模块分布失败:', error)
    ElMessage.error('加载模块分布失败')
  }
}

const renderTrendChart = (data: TrendPoint[]) => {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = init(trendChartRef.value)
  }

  const dates = data.map((item) => item.date)
  const counts = data.map((item) => item.hitCount || 0)
  const dangerColor = resolveCssColor('--cn-color-danger', 'firebrick')
  const dangerSoftColor = resolveCssColor('--cn-color-danger-soft', 'mistyrose')

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '命中次数',
        type: 'line',
        smooth: true,
        data: counts,
        areaStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: dangerSoftColor },
            { offset: 1, color: 'transparent' }
          ])
        },
        lineStyle: { color: dangerColor },
        itemStyle: { color: dangerColor }
      }
    ],
    grid: {
      left: 12,
      right: 18,
      bottom: 8,
      top: 24,
      containLabel: true
    }
  })
}

const renderCategoryChart = (data: DistributionItem[]) => {
  if (!categoryChartRef.value) return

  if (!categoryChart) {
    categoryChart = init(categoryChartRef.value)
  }

  const chartData = data.map((item) => ({
    name: item.category || item.name || item.categoryName || '未分类',
    value: item.count ?? item.value ?? 0
  }))
  const chartTextColor = resolveCssColor('--cn-color-text-secondary', 'slategray')
  const chartShadowColor = resolveCssColor('--cn-color-border', 'lightgray')

  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      left: 0,
      top: 0,
      textStyle: { color: chartTextColor }
    },
    series: [
      {
        name: '分类分布',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['58%', '55%'],
        data: chartData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: chartShadowColor
          }
        }
      }
    ]
  })
}

const renderModuleChart = (data: DistributionItem[]) => {
  if (!moduleChartRef.value) return

  if (!moduleChart) {
    moduleChart = init(moduleChartRef.value)
  }

  const modules = data.map((item) => item.module || item.name || item.moduleName || '未知模块')
  const counts = data.map((item) => item.count ?? item.value ?? 0)
  const chartTextColor = resolveCssColor('--cn-color-text-secondary', 'slategray')
  const brandColor = resolveCssColor('--cn-color-brand-primary', 'steelblue')
  const brandSoftColor = resolveCssColor('--cn-color-brand-soft', 'aliceblue')

  moduleChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    xAxis: {
      type: 'category',
      data: modules,
      axisLabel: { color: chartTextColor }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor }
    },
    series: [
      {
        name: '命中次数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: brandColor },
            { offset: 1, color: brandSoftColor }
          ])
        }
      }
    ],
    grid: {
      left: 10,
      right: 12,
      bottom: 8,
      top: 24,
      containLabel: true
    }
  })
}

const handleResize = () => {
  trendChart?.resize()
  categoryChart?.resize()
  moduleChart?.resize()
}

const formatPercent = (value: number | string | null | undefined) => {
  if (value === null || value === undefined) {
    return '0%'
  }
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) {
    return '0%'
  }
  return `${numberValue.toFixed(2)}%`
}

const formatDate = (date: Date) => date.toISOString().split('T')[0]

const getQueryPayload = () => ({
  startDate: queryForm.startDate,
  endDate: queryForm.endDate,
  module: queryForm.module
})
</script>

<style scoped>
.sensitive-statistics-page {
  min-height: 100%;
}

.sensitive-stat-grid,
.analysis-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.sensitive-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.analysis-grid {
  grid-template-columns: minmax(280px, 0.9fr) repeat(2, minmax(280px, 1fr));
  align-items: stretch;
}

.chart-container,
.chart-container-small {
  width: 100%;
  min-width: 0;
}

.chart-container {
  height: 350px;
}

.chart-container-small {
  height: 320px;
}

.hot-word-cell {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.hot-word-cell span {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .sensitive-stat-grid,
  .analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .sensitive-stat-grid,
  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .chart-container {
    height: 300px;
  }

  .chart-container-small {
    height: 280px;
  }
}
</style>
