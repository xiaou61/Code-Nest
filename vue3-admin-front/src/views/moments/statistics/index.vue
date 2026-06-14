<template>
  <CnPage class="moment-statistics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="数据统计"
      description="查看朋友圈动态、点赞、评论和活跃用户趋势，支持按时间范围快速查询。"
      eyebrow="Moment Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">朋友圈管理</CnStatusTag>
        <CnStatusTag type="neutral">动态 {{ statisticsData.totalMoments || 0 }}</CnStatusTag>
        <CnStatusTag type="success">点赞 {{ statisticsData.totalLikes || 0 }}</CnStatusTag>
        <CnStatusTag type="info">评论 {{ statisticsData.totalComments || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadStatistics">刷新</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="时间筛选" description="选择日期范围，或使用快捷入口查看近 7 天、近 30 天、近 3 个月趋势。" divided>
      <div class="date-filter-bar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :clearable="false"
          @change="handleDateChange"
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="loadStatistics">查询</el-button>
        <div class="quick-filters">
          <el-button size="small" @click="setQuickDate(7)">近7天</el-button>
          <el-button size="small" @click="setQuickDate(30)">近30天</el-button>
          <el-button size="small" @click="setQuickDate(90)">近3个月</el-button>
        </div>
      </div>
    </CnSection>

    <div class="moment-stat-grid">
      <CnStatCard title="总动态数" :value="statisticsData.totalMoments || 0" description="当前范围内累计动态数量" tone="brand" />
      <CnStatCard title="总点赞数" :value="statisticsData.totalLikes || 0" description="当前范围内累计点赞数量" tone="success" />
      <CnStatCard title="总评论数" :value="statisticsData.totalComments || 0" description="当前范围内累计评论数量" tone="warning" />
      <CnStatCard title="活跃用户数" :value="statisticsData.activeUsers || 0" description="当前范围内参与互动用户数" tone="info" />
    </div>

    <CnSection title="数据趋势" description="按日展示动态、点赞、评论和活跃用户变化。" divided>
      <div v-loading="loading" ref="trendChartRef" class="trend-chart"></div>
    </CnSection>

    <CnSection v-if="dailyStats.length" title="每日数据详情" description="统计趋势图下钻明细，便于运营复核每日表现。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="pagedDailyStats"
        :loading="loading"
        :pagination="tablePagination"
        row-key="date"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #momentCount="{ row }">
          <CnStatusTag type="brand" size="sm">{{ row.momentCount || 0 }}</CnStatusTag>
        </template>

        <template #likeCount="{ row }">
          <CnStatusTag type="success" size="sm">{{ row.likeCount || 0 }}</CnStatusTag>
        </template>

        <template #commentCount="{ row }">
          <CnStatusTag type="warning" size="sm">{{ row.commentCount || 0 }}</CnStatusTag>
        </template>

        <template #activeUserCount="{ row }">
          <CnStatusTag type="info" size="sm">{{ row.activeUserCount || 0 }}</CnStatusTag>
        </template>
      </CnDataTable>
    </CnSection>

    <CnSection v-else title="每日数据详情" description="当前时间范围暂无每日明细。" divided>
      <CnEmptyState title="暂无统计数据" description="当前时间范围没有朋友圈统计明细，可以切换日期范围后再查询。" icon="MS" surface="transparent">
        <template #actions>
          <el-button @click="setQuickDate(30)">查看近30天</el-button>
        </template>
      </CnEmptyState>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { graphic, init, use } from 'echarts/core'
import type { ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { getMomentStatistics } from '@/api/moment'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn } from '@/design-system'

use([LineChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

interface MomentStatistics {
  totalMoments?: number
  totalLikes?: number
  totalComments?: number
  activeUsers?: number
  dailyStats?: DailyStat[]
}

interface DailyStat {
  date: string
  momentCount?: number
  likeCount?: number
  commentCount?: number
  activeUserCount?: number
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '朋友圈管理' }, { label: '数据统计' }]

const loading = ref(false)
const statisticsData = ref<MomentStatistics>({})
const dailyStats = ref<DailyStat[]>([])
const dateRange = ref<string[]>([])
const trendChartRef = ref<HTMLElement>()
let trendChart: ECharts | null = null

const pagination = ref({
  currentPage: 1,
  pageSize: 20
})

const tableColumns: CnTableColumn<DailyStat>[] = [
  { prop: 'date', label: '日期', minWidth: 140, showOverflowTooltip: true },
  { prop: 'momentCount', label: '新增动态', width: 110, align: 'center', slot: 'momentCount' },
  { prop: 'likeCount', label: '新增点赞', width: 110, align: 'center', slot: 'likeCount' },
  { prop: 'commentCount', label: '新增评论', width: 110, align: 'center', slot: 'commentCount' },
  { prop: 'activeUserCount', label: '活跃用户', width: 110, align: 'center', slot: 'activeUserCount' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.value.currentPage,
  pageSize: pagination.value.pageSize,
  total: dailyStats.value.length,
  pageSizes: [10, 20, 50, 100]
}))

const pagedDailyStats = computed(() => {
  const start = (pagination.value.currentPage - 1) * pagination.value.pageSize
  return dailyStats.value.slice(start, start + pagination.value.pageSize)
})

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
  initDateRange()
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})

const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  dateRange.value = [formatDate(start), formatDate(end)]
}

const setQuickDate = (days: number) => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - (days - 1))
  dateRange.value = [formatDate(start), formatDate(end)]
  pagination.value.currentPage = 1
  loadStatistics()
}

const handleDateChange = (value: string[] | null) => {
  if (value && value.length === 2) {
    pagination.value.currentPage = 1
    loadStatistics()
  }
}

const loadStatistics = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }

  loading.value = true
  try {
    const result: MomentStatistics = await getMomentStatistics({
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      type: 'daily'
    })

    statisticsData.value = result || {}
    dailyStats.value = result?.dailyStats || []
    await nextTick()
    renderChart()
  } catch (error) {
    console.error('加载朋友圈统计失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const renderChart = () => {
  if (!trendChartRef.value) return

  if (!trendChart) {
    trendChart = init(trendChartRef.value)
  }

  const dates = dailyStats.value.map((item) => item.date)
  const chartTextColor = resolveCssColor('--cn-color-text-secondary', 'slategray')
  const chartPalette = {
    brand: resolveCssColor('--cn-color-brand-primary', 'steelblue'),
    success: resolveCssColor('--cn-color-success', 'seagreen'),
    warning: resolveCssColor('--cn-color-warning', 'darkorange'),
    info: resolveCssColor('--cn-color-info', 'dodgerblue')
  }
  const chartSoftPalette = {
    brand: resolveCssColor('--cn-color-brand-soft', 'aliceblue'),
    success: resolveCssColor('--cn-color-success-soft', 'honeydew'),
    warning: resolveCssColor('--cn-color-warning-soft', 'cornsilk'),
    info: resolveCssColor('--cn-color-info-soft', 'lavender')
  }

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['动态数', '点赞数', '评论数', '活跃用户'],
      top: 0,
      textStyle: { color: chartTextColor }
    },
    grid: {
      left: 12,
      right: 18,
      bottom: 8,
      top: 48,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLabel: { color: chartTextColor }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: chartTextColor }
    },
    series: [
      buildLineSeries('动态数', chartPalette.brand, chartSoftPalette.brand, dailyStats.value.map((item) => item.momentCount || 0)),
      buildLineSeries('点赞数', chartPalette.success, chartSoftPalette.success, dailyStats.value.map((item) => item.likeCount || 0)),
      buildLineSeries('评论数', chartPalette.warning, chartSoftPalette.warning, dailyStats.value.map((item) => item.commentCount || 0)),
      buildLineSeries('活跃用户', chartPalette.info, chartSoftPalette.info, dailyStats.value.map((item) => item.activeUserCount || 0))
    ]
  })
}

const buildLineSeries = (name: string, color: string, areaColor: string, data: number[]) => ({
  name,
  type: 'line',
  smooth: true,
  data,
  lineStyle: { color },
  itemStyle: { color },
  areaStyle: {
    color: new graphic.LinearGradient(0, 0, 0, 1, [
      { offset: 0, color: areaColor },
      { offset: 1, color: 'transparent' }
    ])
  }
})

const handleResize = () => {
  trendChart?.resize()
}

const handlePageChange = (page: number) => {
  pagination.value.currentPage = page
}

const handlePageSizeChange = (size: number) => {
  pagination.value.pageSize = size
  pagination.value.currentPage = 1
}

const formatDate = (date: Date) => date.toISOString().split('T')[0]
</script>

<style scoped>
.moment-statistics-page {
  min-height: 100%;
}

.date-filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
}

.date-filter-bar :deep(.el-date-editor) {
  width: min(100%, 360px);
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.quick-filters .el-button {
  margin-left: 0;
}

.moment-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.trend-chart {
  width: 100%;
  min-width: 0;
  height: 380px;
}

@media (max-width: 1180px) {
  .moment-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .date-filter-bar {
    align-items: stretch;
  }

  .date-filter-bar :deep(.el-date-editor),
  .date-filter-bar > .el-button {
    width: 100%;
  }

  .moment-stat-grid {
    grid-template-columns: 1fr;
  }

  .trend-chart {
    height: 320px;
  }
}
</style>
