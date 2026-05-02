<template>
  <div class="moment-statistics">
    <!-- 统计概览 -->
    <div class="stats-overview">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon total-moments">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stats-content">
                <h3>{{ statisticsData.totalMoments || 0 }}</h3>
                <p>总动态数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon total-likes">
                <el-icon><Star /></el-icon>
              </div>
              <div class="stats-content">
                <h3>{{ statisticsData.totalLikes || 0 }}</h3>
                <p>总点赞数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon total-comments">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <div class="stats-content">
                <h3>{{ statisticsData.totalComments || 0 }}</h3>
                <p>总评论数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stats-card">
            <div class="stats-item">
              <div class="stats-icon active-users">
                <el-icon><User /></el-icon>
              </div>
              <div class="stats-content">
                <h3>{{ statisticsData.activeUsers || 0 }}</h3>
                <p>活跃用户数</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 时间范围选择 -->
    <el-card style="margin-top: 20px;">
      <div class="filter-section">
        <el-row :gutter="20" justify="space-between" align="middle">
          <el-col :span="12">
            <div class="date-filter">
              <span>时间范围：</span>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="handleDateChange"
              />
              <el-button type="primary" @click="loadStatistics" :loading="loading" style="margin-left: 10px;">
                <el-icon><Search /></el-icon>
                查询
              </el-button>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="quick-filters">
              <el-button @click="setQuickDate(7)" size="small">近7天</el-button>
              <el-button @click="setQuickDate(30)" size="small">近30天</el-button>
              <el-button @click="setQuickDate(90)" size="small">近3个月</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 趋势图表 -->
      <div class="chart-section" v-loading="loading">
        <h4>数据趋势</h4>
        <div id="trendsChart" style="height: 400px;"></div>
      </div>

      <!-- 详细数据表格 -->
      <div class="table-section" v-if="dailyStats.length">
        <h4>每日数据详情</h4>
        <el-table
          :data="dailyStats"
          style="width: 100%"
        >
          <el-table-column prop="date" label="日期" width="120" />
          <el-table-column prop="momentCount" label="新增动态" width="100" align="center" />
          <el-table-column prop="likeCount" label="新增点赞" width="100" align="center" />
          <el-table-column prop="commentCount" label="新增评论" width="100" align="center" />
          <el-table-column prop="activeUserCount" label="活跃用户" width="100" align="center" />
        </el-table>
        
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :total="dailyStats.length"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Star, ChatDotRound, User, Search } from '@element-plus/icons-vue'
import { getMomentStatistics } from '@/api/moment'
import { use, init } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

// 数据状态
const loading = ref(false)
const statisticsData = ref({})
const dailyStats = ref([])
const dateRange = ref([])
const chart = ref(null)

// 分页配置
const pagination = ref({
  currentPage: 1,
  pageSize: 20
})

// 初始化日期范围（默认近30天）
const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  
  dateRange.value = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]
}

// 设置快速日期
const setQuickDate = (days) => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - (days - 1))
  
  dateRange.value = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]
  
  loadStatistics()
}

// 日期变化处理
const handleDateChange = (value) => {
  if (value && value.length === 2) {
    loadStatistics()
  }
}

// 加载统计数据
const loadStatistics = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择时间范围')
    return
  }
  
  loading.value = true
  try {
    const params = {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      type: 'daily'
    }
    
    const result = await getMomentStatistics(params)
    statisticsData.value = result
    dailyStats.value = result.dailyStats || []
    
    // 更新图表
    await nextTick()
    renderChart()
    
  } catch (error) {
    ElMessage.error('加载统计数据失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 渲染图表
const renderChart = () => {
  const chartDom = document.getElementById('trendsChart')
  if (!chartDom) return
  
  if (chart.value) {
    chart.value.dispose()
  }
  
  chart.value = init(chartDom)
  
  const dates = dailyStats.value.map(item => item.date)
  const moments = dailyStats.value.map(item => item.momentCount || 0)
  const likes = dailyStats.value.map(item => item.likeCount || 0)
  const comments = dailyStats.value.map(item => item.commentCount || 0)
  const activeUsers = dailyStats.value.map(item => item.activeUserCount || 0)
  
  const option = {
    title: {
      text: '朋友圈数据趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['动态数', '点赞数', '评论数', '活跃用户'],
      bottom: 10
    },
    xAxis: {
      type: 'category',
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '动态数',
        type: 'line',
        data: moments,
        smooth: true,
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '点赞数',
        type: 'line',
        data: likes,
        smooth: true,
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '评论数',
        type: 'line',
        data: comments,
        smooth: true,
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '活跃用户',
        type: 'line',
        data: activeUsers,
        smooth: true,
        itemStyle: { color: '#F56C6C' }
      }
    ]
  }
  
  chart.value.setOption(option)
}

// 分页处理
const handlePageSizeChange = () => {
  // 由于是前端分页，这里可以不做处理或者实现前端分页逻辑
}

const handlePageChange = () => {
  // 由于是前端分页，这里可以不做处理或者实现前端分页逻辑
}

// 窗口大小变化时重新调整图表
const handleResize = () => {
  if (chart.value) {
    chart.value.resize()
  }
}

onMounted(() => {
  initDateRange()
  loadStatistics()
  
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.moment-statistics {
  padding: 20px;
}

.stats-overview {
  margin-bottom: 20px;
}

.stats-card {
  height: 120px;
}

.stats-item {
  display: flex;
  align-items: center;
  height: 100%;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.total-moments {
  background: linear-gradient(45deg, #409EFF, #66B1FF);
}

.total-likes {
  background: linear-gradient(45deg, #67C23A, #85CE61);
}

.total-comments {
  background: linear-gradient(45deg, #E6A23C, #EEBC6E);
}

.active-users {
  background: linear-gradient(45deg, #F56C6C, #F78989);
}

.stats-content h3 {
  margin: 0;
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stats-content p {
  margin: 5px 0 0 0;
  color: #666;
  font-size: 14px;
}

.filter-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.date-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quick-filters {
  text-align: right;
}

.chart-section,
.table-section {
  margin-top: 30px;
}

.chart-section h4,
.table-section h4 {
  margin: 0 0 20px 0;
  color: #333;
  font-size: 16px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style> 
