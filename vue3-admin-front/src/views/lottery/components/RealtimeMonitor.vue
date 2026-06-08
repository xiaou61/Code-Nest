<template>
  <div class="realtime-monitor">
    <CnSection title="实时控制" description="手动刷新监控数据，或开启 10 秒自动刷新。" surface="plain" divided>
      <div class="action-bar">
        <el-button :icon="Refresh" @click="loadMonitorData">刷新数据</el-button>
        <el-switch v-model="autoRefresh" active-text="自动刷新" @change="handleAutoRefreshChange" />
      </div>
    </CnSection>

    <div v-loading="loading" class="overview-grid">
      <CnStatCard title="今日抽奖次数" :value="monitorData.todayOverview?.drawCount || 0" description="今日累计抽奖请求数" tone="brand" />
      <CnStatCard title="今日消耗积分" :value="monitorData.todayOverview?.totalCost || 0" description="抽奖消耗积分总量" tone="warning" />
      <CnStatCard title="今日发放积分" :value="monitorData.todayOverview?.totalReward || 0" description="中奖发放积分总量" tone="success" />
      <CnStatCard
        title="实际回报率"
        :value="`${((monitorData.todayOverview?.returnRate || 0) * 100).toFixed(2)}%`"
        description="今日奖励回报率"
        :tone="getReturnRateTone(monitorData.todayOverview?.returnRate)"
      />
    </div>

    <CnSection v-if="alerts.length > 0" title="系统预警" description="实时监控返回的异常和预警信息。" surface="plain" divided>
      <el-timeline>
        <el-timeline-item
          v-for="alert in alerts"
          :key="alert.alertTime"
          :timestamp="alert.alertTime"
          :type="getAlertType(alert.alertLevel)"
          placement="top"
        >
          <div class="alert-card">
            <h4>{{ alert.alertType }}</h4>
            <p>{{ alert.message }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
    </CnSection>

    <CnSection title="奖品状态监控" description="查看各奖品的抽取、中奖、库存、回报率和预警状态。" surface="plain" divided>
      <CnDataTable
        :columns="columns"
        :data="monitorData.prizeStatusList || []"
        :loading="loading"
        :pagination="null"
        row-key="prizeId"
        border
        empty-title="暂无奖品监控"
        empty-description="当前没有奖品状态监控数据。"
        empty-icon="RM"
      >
        <template #currentProbability="{ row }">
          {{ formatProbability(row.currentProbability) }}
        </template>

        <template #actualReturnRate="{ row }">
          <CnStatusTag :type="getReturnRateTone(row.actualReturnRate)" size="sm">
            {{ formatProbability(row.actualReturnRate) }}
          </CnStatusTag>
        </template>

        <template #targetReturnRate="{ row }">
          {{ formatProbability(row.targetReturnRate) }}
        </template>

        <template #stock="{ row }">
          <div v-if="row.totalStock && row.totalStock > 0" class="stock-cell">
            <el-progress :percentage="(row.currentStock / row.totalStock) * 100" :color="getStockColor(row.currentStock, row.totalStock)" />
            <span>{{ row.currentStock }} / {{ row.totalStock }}</span>
          </div>
          <CnStatusTag v-else type="info" size="sm" subtle>无限制</CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">{{ row.status }}</CnStatusTag>
        </template>

        <template #alert="{ row }">
          <CnStatusTag v-if="row.alertMessage" :type="row.alertLevel === '危险' ? 'danger' : 'warning'" size="sm">
            {{ row.alertMessage }}
          </CnStatusTag>
          <CnStatusTag v-else type="success" size="sm" subtle>正常</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="viewPrizeDetail(row)">查看详情</el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="detailDialog" title="奖品监控详情" width="800px">
      <el-descriptions v-if="currentPrizeDetail" :column="2" border>
        <el-descriptions-item label="奖品名称">{{ currentPrizeDetail.prizeName }}</el-descriptions-item>
        <el-descriptions-item label="奖品等级">{{ getLevelName(currentPrizeDetail.prizeLevel) }}</el-descriptions-item>
        <el-descriptions-item label="今日抽取">{{ currentPrizeDetail.todayDrawCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="今日中奖">{{ currentPrizeDetail.todayWinCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="总抽取">{{ currentPrizeDetail.totalDrawCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="总中奖">{{ currentPrizeDetail.totalWinCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="当前概率">{{ formatProbability(currentPrizeDetail.currentProbability) }}</el-descriptions-item>
        <el-descriptions-item label="基础概率">{{ formatProbability(currentPrizeDetail.baseProbability) }}</el-descriptions-item>
        <el-descriptions-item label="实际回报率">
          <CnStatusTag :type="getReturnRateTone(currentPrizeDetail.actualReturnRate)" size="sm">
            {{ formatProbability(currentPrizeDetail.actualReturnRate) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="目标回报率">{{ formatProbability(currentPrizeDetail.targetReturnRate) }}</el-descriptions-item>
        <el-descriptions-item label="最大回报率">{{ formatProbability(currentPrizeDetail.maxReturnRate) }}</el-descriptions-item>
        <el-descriptions-item label="最小回报率">{{ formatProbability(currentPrizeDetail.minReturnRate) }}</el-descriptions-item>
        <el-descriptions-item label="库存状态" :span="2">
          <span v-if="currentPrizeDetail.totalStock && currentPrizeDetail.totalStock > 0">
            {{ currentPrizeDetail.currentStock }} / {{ currentPrizeDetail.totalStock }}
            （剩余 {{ ((currentPrizeDetail.currentStock / currentPrizeDetail.totalStock) * 100).toFixed(2) }}%）
          </span>
          <span v-else>无限制</span>
        </el-descriptions-item>
        <el-descriptions-item label="调整策略">{{ currentPrizeDetail.adjustStrategy }}</el-descriptions-item>
        <el-descriptions-item label="奖品状态">
          <CnStatusTag :type="getStatusTone(currentPrizeDetail.status)" size="sm">{{ currentPrizeDetail.status }}</CnStatusTag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnDataTable, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTableColumn, CnTone } from '@/design-system'

interface TodayOverview {
  drawCount?: number
  totalCost?: number
  totalReward?: number
  returnRate?: number
}

interface PrizeStatus extends Record<string, unknown> {
  prizeId?: number
  prizeName?: string
  prizeLevel?: number
  todayDrawCount?: number
  todayWinCount?: number
  totalDrawCount?: number
  totalWinCount?: number
  currentProbability?: number
  baseProbability?: number
  actualReturnRate?: number
  targetReturnRate?: number
  maxReturnRate?: number
  minReturnRate?: number
  totalStock?: number
  currentStock?: number
  status?: string
  alertMessage?: string
  alertLevel?: string
  adjustStrategy?: string
}

interface MonitorData {
  systemStatus: unknown | null
  todayOverview: TodayOverview | null
  prizeStatusList: PrizeStatus[]
  strategyInfo: unknown | null
}

interface AlertItem {
  alertTime: string
  alertLevel?: string
  alertType?: string
  message?: string
}

const loading = ref(false)
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const monitorData = ref<MonitorData>({
  systemStatus: null,
  todayOverview: null,
  prizeStatusList: [],
  strategyInfo: null
})

const alerts = ref<AlertItem[]>([])
const detailDialog = ref(false)
const currentPrizeDetail = ref<PrizeStatus | null>(null)

const columns: CnTableColumn<PrizeStatus>[] = [
  { prop: 'prizeName', label: '奖品名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'todayDrawCount', label: '今日抽取', width: 100, align: 'center' },
  { prop: 'todayWinCount', label: '今日中奖', width: 100, align: 'center' },
  { prop: 'currentProbability', label: '当前概率', width: 120, align: 'center', slot: 'currentProbability' },
  { prop: 'actualReturnRate', label: '实际回报率', width: 130, align: 'center', slot: 'actualReturnRate' },
  { prop: 'targetReturnRate', label: '目标回报率', width: 130, align: 'center', slot: 'targetReturnRate' },
  { label: '库存状态', width: 150, align: 'center', slot: 'stock' },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { label: '预警', width: 200, slot: 'alert' },
  { label: '操作', width: 120, fixed: 'right', slot: 'actions' }
]

const loadMonitorData = async () => {
  loading.value = true
  try {
    const [monitor, alertList] = await Promise.all([lotteryAdminApi.getRealtimeMonitor(), lotteryAdminApi.getAlerts()])
    monitorData.value = monitor
    alerts.value = alertList || []
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

const viewPrizeDetail = async (row: PrizeStatus) => {
  try {
    currentPrizeDetail.value = await lotteryAdminApi.getPrizeMonitor(row.prizeId)
    detailDialog.value = true
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  }
}

const handleAutoRefreshChange = (value: boolean) => {
  if (value) {
    refreshTimer = setInterval(loadMonitorData, 10000)
    ElMessage.success('已开启自动刷新（10秒/次）')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已关闭自动刷新')
  }
}

const getReturnRateTone = (rate?: number): CnTone => {
  if (!rate) return 'neutral'
  if (rate > 0.9) return 'danger'
  if (rate > 0.75) return 'warning'
  return 'success'
}

const getAlertType = (level?: string) => {
  const typeMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    危险: 'danger',
    警告: 'warning',
    信息: 'info'
  }
  return level ? typeMap[level] || 'info' : 'info'
}

const getStockColor = (current?: number, total?: number) => {
  const ratio = Number(current || 0) / Number(total || 1)
  if (ratio < 0.2) return 'var(--cn-color-danger)'
  if (ratio < 0.5) return 'var(--cn-color-warning)'
  return 'var(--cn-color-success)'
}

const getStatusTone = (status?: string): CnTone => {
  if (status === '暂停') return 'danger'
  if (status === '正常') return 'success'
  return 'warning'
}

const getLevelName = (level?: number) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return level ? names[level] || '未知' : '未知'
}

const formatProbability = (value?: number) => `${(Number(value || 0) * 100).toFixed(4)}%`

onMounted(() => {
  loadMonitorData()
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.realtime-monitor {
  display: grid;
  gap: var(--cn-space-4);
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-4);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.alert-card {
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.alert-card h4 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
}

.alert-card p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  line-height: 1.6;
}

.stock-cell {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.stock-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

@media (max-width: 1080px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
