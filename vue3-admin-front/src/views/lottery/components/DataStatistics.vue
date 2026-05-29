<template>
  <div class="data-statistics">
    <el-row :gutter="20" class="filter-row">
      <el-col :span="8">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadStatistics" />
      </el-col>
      <el-col :span="4">
        <el-button type="primary" @click="loadStatistics">查询</el-button>
      </el-col>
    </el-row>

    <!-- 综合分析数据 -->
    <el-card shadow="hover" v-loading="loading">
      <template #header>
        <h3>综合分析</h3>
      </template>
      <el-row :gutter="20" v-if="analysisData">
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">总抽奖次数</div>
            <div class="value">{{ summaryData.totalDrawCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">总消耗积分</div>
            <div class="value">{{ summaryData.totalCostPoints }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">总发放积分</div>
            <div class="value">{{ summaryData.totalRewardPoints }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">平台利润</div>
            <div class="value profit">{{ summaryData.profitPoints }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">综合回报率</div>
            <div class="value" :class="getReturnRateClass(summaryData.returnRate)">
              {{ (summaryData.returnRate * 100).toFixed(2) }}%
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">参与用户数</div>
            <div class="value">{{ summaryData.totalUsers }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">平均抽奖次数</div>
            <div class="value">{{ summaryData.avgDrawTimes }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-box">
            <div class="label">ROI</div>
            <div class="value">{{ (summaryData.roi * 100).toFixed(2) }}%</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 历史统计表格 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <h3>历史统计</h3>
      </template>
      <el-table :data="historyList" stripe border v-loading="loading">
        <el-table-column prop="statDate" label="日期" width="120" />
        <el-table-column prop="drawCount" label="抽奖次数" width="100" align="center" />
        <el-table-column prop="totalCostPoints" label="消耗积分" width="120" align="center" />
        <el-table-column prop="totalRewardPoints" label="发放积分" width="120" align="center" />
        <el-table-column label="回报率" width="100" align="center">
          <template #default="{ row }">
            {{ ((row.returnRate || 0) * 100).toFixed(2) }}%
          </template>
        </el-table-column>
        <el-table-column prop="platformProfitPoints" label="平台利润" width="120" align="center" />
        <el-table-column prop="participantCount" label="参与人数" width="100" align="center" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { lotteryAdminApi } from '@/api/lotteryAdmin'

const loading = ref(false)
const dateRange = ref([])
const analysisData = ref(null)
const historyList = ref([])

const summaryData = computed(() => {
  const historySummary = historyList.value.reduce((acc, item) => {
    acc.totalDrawCount += Number(item?.drawCount || item?.totalDrawCount || 0)
    acc.totalCostPoints += Number(item?.totalCostPoints || 0)
    acc.totalRewardPoints += Number(item?.totalRewardPoints || 0)
    acc.profitPoints += Number(item?.platformProfitPoints || 0)
    return acc
  }, {
    totalDrawCount: 0,
    totalCostPoints: 0,
    totalRewardPoints: 0,
    profitPoints: 0
  })

  return {
    ...historySummary,
    returnRate: historySummary.totalCostPoints > 0
      ? historySummary.totalRewardPoints / historySummary.totalCostPoints
      : 0,
    totalUsers: Number(analysisData.value?.userBehaviorAnalysis?.activeUsers || 0),
    avgDrawTimes: Number(analysisData.value?.userBehaviorAnalysis?.avgDrawCount || 0),
    roi: Number(analysisData.value?.roiAnalysis?.roi || 0)
  }
})

const loadStatistics = async () => {
  const [startDate, endDate] = dateRange.value || []
  if (!startDate || !endDate) {
    ElMessage.warning('请选择开始和结束日期')
    return
  }

  loading.value = true
  try {
    const [analysis, history] = await Promise.all([
      lotteryAdminApi.getComprehensiveAnalysis(startDate, endDate),
      lotteryAdminApi.getHistoryStatistics({ startDate, endDate })
    ])
    
    analysisData.value = analysis
    historyList.value = history
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const getReturnRateClass = (rate) => {
  if (!rate) return ''
  if (rate > 0.9) return 'rate-danger'
  if (rate > 0.75) return 'rate-warning'
  return 'rate-normal'
}

onMounted(() => {
  // 默认查询最近7天
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 7)
  dateRange.value = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]
  loadStatistics()
})
</script>

<style scoped lang="scss">
.data-statistics {
  .filter-row {
    margin-bottom: 20px;
  }
  
  .stat-box {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 4px;
    margin-bottom: 20px;
    
    .label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 10px;
    }
    
    .value {
      font-size: 24px;
      font-weight: bold;
      color: #303133;
      
      &.profit {
        color: #67c23a;
      }
      
      &.rate-danger {
        color: #f56c6c;
      }
      
      &.rate-warning {
        color: #e6a23c;
      }
      
      &.rate-normal {
        color: #67c23a;
      }
    }
  }
}
</style>

