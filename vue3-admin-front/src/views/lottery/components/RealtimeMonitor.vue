<template>
  <div class="realtime-monitor">
    <el-row :gutter="20" class="action-bar">
      <el-col :span="12">
        <el-button :icon="Refresh" @click="loadMonitorData">
          刷新数据
        </el-button>
        <el-switch
          v-model="autoRefresh"
          active-text="自动刷新"
          @change="handleAutoRefreshChange"
          style="margin-left: 20px" />
      </el-col>
    </el-row>

    <!-- 系统概览 -->
    <el-card class="overview-card" shadow="hover">
      <template #header>
        <h3>系统概览</h3>
      </template>
      <el-row :gutter="20" v-loading="loading">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">今日抽奖次数</div>
            <div class="stat-value">{{ monitorData.todayOverview?.drawCount || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">今日消耗积分</div>
            <div class="stat-value">{{ monitorData.todayOverview?.totalCost || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">今日发放积分</div>
            <div class="stat-value">{{ monitorData.todayOverview?.totalReward || 0 }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">实际回报率</div>
            <div class="stat-value" :class="getReturnRateClass(monitorData.todayOverview?.returnRate)">
              {{ ((monitorData.todayOverview?.returnRate || 0) * 100).toFixed(2) }}%
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 预警信息 -->
    <el-card class="alerts-card" shadow="hover" v-if="alerts.length > 0">
      <template #header>
        <h3>⚠️ 系统预警</h3>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="alert in alerts"
          :key="alert.alertTime"
          :timestamp="alert.alertTime"
          :type="getAlertType(alert.alertLevel)"
          placement="top">
          <el-card>
            <h4>{{ alert.alertType }}</h4>
            <p>{{ alert.message }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 奖品状态列表 -->
    <el-card class="prizes-card" shadow="hover">
      <template #header>
        <h3>奖品状态监控</h3>
      </template>
      <el-table 
        :data="monitorData.prizeStatusList || []" 
        stripe 
        border
        v-loading="loading">
        <el-table-column prop="prizeName" label="奖品名称" width="120" />
        <el-table-column label="今日抽取" width="100" align="center">
          <template #default="{ row }">
            {{ row.todayDrawCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="今日中奖" width="100" align="center">
          <template #default="{ row }">
            {{ row.todayWinCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="当前概率" width="120" align="center">
          <template #default="{ row }">
            {{ (row.currentProbability * 100).toFixed(4) }}%
          </template>
        </el-table-column>
        <el-table-column label="实际回报率" width="130" align="center">
          <template #default="{ row }">
            <span :class="getReturnRateClass(row.actualReturnRate)">
              {{ (row.actualReturnRate * 100).toFixed(4) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="目标回报率" width="130" align="center">
          <template #default="{ row }">
            {{ (row.targetReturnRate * 100).toFixed(4) }}%
          </template>
        </el-table-column>
        <el-table-column label="库存状态" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.totalStock && row.totalStock > 0">
              <el-progress 
                :percentage="row.currentStock / row.totalStock * 100"
                :color="getStockColor(row.currentStock, row.totalStock)" />
              <div style="margin-top: 5px">{{ row.currentStock }} / {{ row.totalStock }}</div>
            </span>
            <el-tag v-else type="info" size="small">无限制</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === '暂停'" type="danger" size="small">暂停</el-tag>
            <el-tag v-else-if="row.status === '正常'" type="success" size="small">正常</el-tag>
            <el-tag v-else type="warning" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预警" width="200">
          <template #default="{ row }">
            <el-tag 
              v-if="row.alertMessage" 
              :type="row.alertLevel === '危险' ? 'danger' : 'warning'" 
              size="small">
              {{ row.alertMessage }}
            </el-tag>
            <span v-else style="color: #67c23a">✓ 正常</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              link 
              type="primary" 
              size="small" 
              @click="viewPrizeDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 奖品详情对话框 -->
    <el-dialog 
      v-model="detailDialog" 
      title="奖品监控详情"
      width="800px">
      <el-descriptions :column="2" border v-if="currentPrizeDetail">
        <el-descriptions-item label="奖品名称">
          {{ currentPrizeDetail.prizeName }}
        </el-descriptions-item>
        <el-descriptions-item label="奖品等级">
          {{ getLevelName(currentPrizeDetail.prizeLevel) }}
        </el-descriptions-item>
        <el-descriptions-item label="今日抽取">
          {{ currentPrizeDetail.todayDrawCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="今日中奖">
          {{ currentPrizeDetail.todayWinCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="总抽取">
          {{ currentPrizeDetail.totalDrawCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="总中奖">
          {{ currentPrizeDetail.totalWinCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="当前概率">
          {{ (currentPrizeDetail.currentProbability * 100).toFixed(4) }}%
        </el-descriptions-item>
        <el-descriptions-item label="基础概率">
          {{ (currentPrizeDetail.baseProbability * 100).toFixed(4) }}%
        </el-descriptions-item>
        <el-descriptions-item label="实际回报率">
          <span :class="getReturnRateClass(currentPrizeDetail.actualReturnRate)">
            {{ (currentPrizeDetail.actualReturnRate * 100).toFixed(4) }}%
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="目标回报率">
          {{ (currentPrizeDetail.targetReturnRate * 100).toFixed(4) }}%
        </el-descriptions-item>
        <el-descriptions-item label="最大回报率">
          {{ (currentPrizeDetail.maxReturnRate * 100).toFixed(4) }}%
        </el-descriptions-item>
        <el-descriptions-item label="最小回报率">
          {{ (currentPrizeDetail.minReturnRate * 100).toFixed(4) }}%
        </el-descriptions-item>
        <el-descriptions-item label="库存状态" :span="2">
          <span v-if="currentPrizeDetail.totalStock && currentPrizeDetail.totalStock > 0">
            {{ currentPrizeDetail.currentStock }} / {{ currentPrizeDetail.totalStock }}
            （剩余 {{ (currentPrizeDetail.currentStock / currentPrizeDetail.totalStock * 100).toFixed(2) }}%）
          </span>
          <span v-else>无限制</span>
        </el-descriptions-item>
        <el-descriptions-item label="调整策略">
          {{ currentPrizeDetail.adjustStrategy }}
        </el-descriptions-item>
        <el-descriptions-item label="奖品状态">
          <el-tag v-if="currentPrizeDetail.status === '暂停'" type="danger">暂停</el-tag>
          <el-tag v-else-if="currentPrizeDetail.status === '正常'" type="success">正常</el-tag>
          <el-tag v-else type="warning">{{ currentPrizeDetail.status }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { lotteryAdminApi } from '@/api/lotteryAdmin'

const loading = ref(false)
const autoRefresh = ref(false)
let refreshTimer = null

const monitorData = ref({
  systemStatus: null,
  todayOverview: null,
  prizeStatusList: [],
  strategyInfo: null
})

const alerts = ref([])
const detailDialog = ref(false)
const currentPrizeDetail = ref(null)

// 加载监控数据
const loadMonitorData = async () => {
  loading.value = true
  try {
    const [monitor, alertList] = await Promise.all([
      lotteryAdminApi.getRealtimeMonitor(),
      lotteryAdminApi.getAlerts()
    ])
    monitorData.value = monitor
    alerts.value = alertList
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 查看奖品详情
const viewPrizeDetail = async (row) => {
  try {
    currentPrizeDetail.value = await lotteryAdminApi.getPrizeMonitor(row.prizeId)
    detailDialog.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  }
}

// 自动刷新
const handleAutoRefreshChange = (value) => {
  if (value) {
    refreshTimer = setInterval(loadMonitorData, 10000) // 每10秒刷新
    ElMessage.success('已开启自动刷新（10秒/次）')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已关闭自动刷新')
  }
}

// 辅助方法
const getReturnRateClass = (rate) => {
  if (!rate) return ''
  if (rate > 0.9) return 'rate-danger'
  if (rate > 0.75) return 'rate-warning'
  return 'rate-normal'
}

const getAlertType = (level) => {
  const typeMap = {
    '危险': 'danger',
    '警告': 'warning',
    '信息': 'info'
  }
  return typeMap[level] || 'info'
}

const getStockColor = (current, total) => {
  const ratio = current / total
  if (ratio < 0.2) return '#f56c6c'
  if (ratio < 0.5) return '#e6a23c'
  return '#67c23a'
}

const getLevelName = (level) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return names[level] || '未知'
}

onMounted(() => {
  loadMonitorData()
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped lang="scss">
.realtime-monitor {
  .action-bar {
    margin-bottom: 20px;
  }
  
  .overview-card, .alerts-card, .prizes-card {
    margin-bottom: 20px;
    
    h3 {
      margin: 0;
      font-size: 18px;
      color: #303133;
    }
  }
  
  .stat-item {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 4px;
    
    .stat-label {
      font-size: 14px;
      color: #909399;
      margin-bottom: 10px;
    }
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      
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
  
  .rate-danger {
    color: #f56c6c;
    font-weight: bold;
  }
  
  .rate-warning {
    color: #e6a23c;
    font-weight: bold;
  }
  
  .rate-normal {
    color: #67c23a;
  }
}
</style>

