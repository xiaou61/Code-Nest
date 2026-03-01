<template>
  <div class="sensitive-statistics-page">
    <!-- 筛选条件 -->
    <div class="filter-bar">
      <el-card shadow="never">
        <el-form :model="queryForm" inline>
          <el-form-item label="日期范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
              style="width: 280px"
            />
          </el-form-item>
          <el-form-item label="业务模块">
            <el-select
              v-model="queryForm.module"
              placeholder="请选择模块"
              clearable
              style="width: 150px"
            >
              <el-option label="全部" value="" />
              <el-option label="社区" value="community" />
              <el-option label="面试" value="interview" />
              <el-option label="朋友圈" value="moment" />
              <el-option label="博客" value="blog" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button
              type="success"
              :loading="exportingReport"
              @click="handleExportReport"
            >
              <el-icon><Download /></el-icon>
              导出报表
            </el-button>
            <el-button @click="resetQuery">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 数据总览 -->
    <div class="overview-section">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-item">
              <div class="stats-label">总检测次数</div>
              <div class="stats-value">{{ overview.totalCheck || 0 }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-item">
              <div class="stats-label">敏感词命中</div>
              <div class="stats-value danger">{{ overview.hitCount || 0 }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-item">
              <div class="stats-label">拦截率</div>
              <div class="stats-value warning">{{ formatPercent(overview.hitRate) }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stats-card">
            <div class="stats-item">
              <div class="stats-label">违规用户数</div>
              <div class="stats-value info">{{ overview.violationUserCount || 0 }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 趋势图 -->
    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="card-header">
          <span>命中趋势</span>
        </div>
      </template>
      <div ref="trendChartRef" class="chart-container"></div>
    </el-card>

    <!-- 热词和分布 -->
    <el-row :gutter="16" class="analysis-section">
      <!-- 热门敏感词 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>热门敏感词 TOP 10</span>
            </div>
          </template>
          <el-table :data="hotWords" :show-header="false" max-height="400">
            <el-table-column prop="word" label="敏感词" />
            <el-table-column prop="hitCount" label="命中次数" width="100" align="right">
              <template #default="{ row }">
                <el-tag type="danger" size="small">{{ row.hitCount }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 分类分布 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>分类分布</span>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-container-small"></div>
        </el-card>
      </el-col>

      <!-- 模块分布 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>模块分布</span>
            </div>
          </template>
          <div ref="moduleChartRef" class="chart-container-small"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import {
  getStatisticsOverview,
  getHitTrend,
  getHotWords,
  getCategoryDistribution,
  getModuleDistribution,
  exportStatisticsReport
} from '@/api/sensitive'

// 响应式数据
const dateRange = ref([])
const overview = ref({})
const hotWords = ref([])
const trendChartRef = ref()
const categoryChartRef = ref()
const moduleChartRef = ref()
let trendChart = null
let categoryChart = null
let moduleChart = null
const exportingReport = ref(false)

// 查询表单
const queryForm = reactive({
  startDate: '',
  endDate: '',
  module: ''
})

// 方法
const handleDateChange = (dates) => {
  if (dates && dates.length === 2) {
    queryForm.startDate = dates[0]
    queryForm.endDate = dates[1]
  } else {
    queryForm.startDate = ''
    queryForm.endDate = ''
  }
}

const handleQuery = async () => {
  await Promise.all([
    loadOverview(),
    loadHitTrend(),
    loadHotWords(),
    loadCategoryDistribution(),
    loadModuleDistribution()
  ])
}

const resetQuery = () => {
  dateRange.value = []
  queryForm.startDate = ''
  queryForm.endDate = ''
  queryForm.module = ''
  handleQuery()
}

const handleExportReport = async () => {
  exportingReport.value = true
  try {
    const result = await exportStatisticsReport(queryForm)
    const blob = new Blob([result.content || ''], { type: 'text/csv;charset=utf-8;' })
    const fileName = result.fileName || `sensitive_statistics_${Date.now()}.csv`
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
    ElMessage.error('统计报表导出失败')
  } finally {
    exportingReport.value = false
  }
}

const loadOverview = async () => {
  try {
    const data = await getStatisticsOverview(queryForm)
    overview.value = data
  } catch (error) {
    ElMessage.error('加载数据总览失败')
  }
}

const loadHitTrend = async () => {
  try {
    const data = await getHitTrend(queryForm)
    await nextTick()
    renderTrendChart(data)
  } catch (error) {
    ElMessage.error('加载趋势数据失败')
  }
}

const loadHotWords = async () => {
  try {
    const data = await getHotWords({
      ...queryForm,
      limit: 10
    })
    hotWords.value = data
  } catch (error) {
    ElMessage.error('加载热词数据失败')
  }
}

const loadCategoryDistribution = async () => {
  try {
    const data = await getCategoryDistribution(queryForm)
    await nextTick()
    renderCategoryChart(data)
  } catch (error) {
    ElMessage.error('加载分类分布失败')
  }
}

const loadModuleDistribution = async () => {
  try {
    const data = await getModuleDistribution(queryForm)
    await nextTick()
    renderModuleChart(data)
  } catch (error) {
    ElMessage.error('加载模块分布失败')
  }
}

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return
  
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const dates = data.map(item => item.date)
  const counts = data.map(item => item.hitCount || 0)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '命中次数',
        type: 'line',
        smooth: true,
        data: counts,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 99, 71, 0.3)' },
            { offset: 1, color: 'rgba(255, 99, 71, 0.05)' }
          ])
        },
        lineStyle: {
          color: '#ff6347'
        },
        itemStyle: {
          color: '#ff6347'
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }

  trendChart.setOption(option)
}

const renderCategoryChart = (data) => {
  if (!categoryChartRef.value) return
  
  if (!categoryChart) {
    categoryChart = echarts.init(categoryChartRef.value)
  }

  const chartData = data.map(item => ({
    name: item.category || item.name || item.categoryName || '未分类',
    value: item.count ?? item.value ?? 0
  }))

  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '分类分布',
        type: 'pie',
        radius: '70%',
        data: chartData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }

  categoryChart.setOption(option)
}

const renderModuleChart = (data) => {
  if (!moduleChartRef.value) return
  
  if (!moduleChart) {
    moduleChart = echarts.init(moduleChartRef.value)
  }

  const modules = data.map(item => item.module || item.name || item.moduleName || '未知模块')
  const counts = data.map(item => item.count ?? item.value ?? 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    xAxis: {
      type: 'category',
      data: modules
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '命中次数',
        type: 'bar',
        data: counts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#a0cfff' }
          ])
        }
      }
    ],
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    }
  }

  moduleChart.setOption(option)
}

// 窗口大小改变时重新渲染图表
const handleResize = () => {
  trendChart?.resize()
  categoryChart?.resize()
  moduleChart?.resize()
}

const formatPercent = (value) => {
  if (value === null || value === undefined) {
    return '0%'
  }
  const numberValue = Number(value)
  if (Number.isNaN(numberValue)) {
    return '0%'
  }
  return `${numberValue.toFixed(2)}%`
}

// 生命周期
onMounted(() => {
  // 设置默认日期为最近7天
  const endDate = new Date()
  const startDate = new Date()
  startDate.setDate(startDate.getDate() - 7)
  
  dateRange.value = [
    startDate.toISOString().split('T')[0],
    endDate.toISOString().split('T')[0]
  ]
  
  queryForm.startDate = dateRange.value[0]
  queryForm.endDate = dateRange.value[1]
  
  handleQuery()
  
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
  moduleChart?.dispose()
})
</script>

<style scoped>
.sensitive-statistics-page {
  padding: 20px;
}

.filter-bar {
  margin-bottom: 16px;
}

.overview-section {
  margin-bottom: 16px;
}

.stats-card {
  text-align: center;
}

.stats-item {
  padding: 20px 0;
}

.stats-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stats-value.danger {
  color: #F56C6C;
}

.stats-value.warning {
  color: #E6A23C;
}

.stats-value.info {
  color: #409EFF;
}

.chart-card {
  margin-bottom: 16px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.chart-container-small {
  height: 350px;
  width: 100%;
}

.analysis-section {
  margin-top: 16px;
}
</style>
