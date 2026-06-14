<template>
  <div class="data-statistics">
    <CnSection title="统计范围" description="选择日期范围后加载综合分析和历史统计。" surface="plain" divided>
      <div class="filter-row">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="loadStatistics"
        />
        <el-button type="primary" @click="loadStatistics">查询</el-button>
      </div>
    </CnSection>

    <CnSection title="综合分析" description="按所选时间范围汇总抽奖、积分、利润和 ROI。" surface="plain" divided>
      <div v-loading="loading" class="summary-grid">
        <CnStatCard title="总抽奖次数" :value="summaryData.totalDrawCount" description="范围内累计抽奖次数" tone="brand" />
        <CnStatCard title="总消耗积分" :value="summaryData.totalCostPoints" description="用户抽奖累计消耗" tone="warning" />
        <CnStatCard title="总发放积分" :value="summaryData.totalRewardPoints" description="中奖奖励累计发放" tone="info" />
        <CnStatCard title="平台利润" :value="summaryData.profitPoints" description="消耗与发放之间的差值" tone="success" />
        <CnStatCard
          title="综合回报率"
          :value="`${(summaryData.returnRate * 100).toFixed(2)}%`"
          description="发放积分 / 消耗积分"
          :tone="getReturnRateTone(summaryData.returnRate)"
        />
        <CnStatCard title="参与用户数" :value="summaryData.totalUsers" description="范围内活跃抽奖用户" tone="neutral" />
        <CnStatCard title="平均抽奖次数" :value="summaryData.avgDrawTimes" description="用户行为分析均值" tone="brand" />
        <CnStatCard title="ROI" :value="`${(summaryData.roi * 100).toFixed(2)}%`" description="综合 ROI 分析结果" tone="success" />
      </div>
    </CnSection>

    <CnSection title="历史统计" description="按日期查看抽奖、积分和利润变化。" surface="plain" divided>
      <CnDataTable
        :columns="historyColumns"
        :data="historyList"
        :loading="loading"
        :pagination="null"
        row-key="statDate"
        border
        empty-title="暂无历史统计"
        empty-description="当前日期范围内没有历史统计数据。"
        empty-icon="LS"
      >
        <template #returnRate="{ row }">
          <CnStatusTag :type="getReturnRateTone(row.returnRate)" size="sm">
            {{ ((row.returnRate || 0) * 100).toFixed(2) }}%
          </CnStatusTag>
        </template>
      </CnDataTable>
    </CnSection>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { lotteryAdminApi } from '@/api/lotteryAdmin'
import { CnDataTable, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTableColumn, CnTone } from '@/design-system'

interface HistoryRow extends Record<string, unknown> {
  statDate?: string
  drawCount?: number
  totalDrawCount?: number
  totalCostPoints?: number
  totalRewardPoints?: number
  returnRate?: number
  platformProfitPoints?: number
  participantCount?: number
}

interface AnalysisData {
  userBehaviorAnalysis?: {
    activeUsers?: number
    avgDrawCount?: number
  }
  roiAnalysis?: {
    roi?: number
  }
}

const loading = ref(false)
const dateRange = ref<string[]>([])
const analysisData = ref<AnalysisData | null>(null)
const historyList = ref<HistoryRow[]>([])

const historyColumns: CnTableColumn<HistoryRow>[] = [
  { prop: 'statDate', label: '日期', width: 120 },
  { prop: 'drawCount', label: '抽奖次数', width: 110, align: 'center' },
  { prop: 'totalCostPoints', label: '消耗积分', width: 120, align: 'center' },
  { prop: 'totalRewardPoints', label: '发放积分', width: 120, align: 'center' },
  { prop: 'returnRate', label: '回报率', width: 110, align: 'center', slot: 'returnRate' },
  { prop: 'platformProfitPoints', label: '平台利润', width: 120, align: 'center' },
  { prop: 'participantCount', label: '参与人数', width: 110, align: 'center' }
]

const summaryData = computed(() => {
  const historySummary = historyList.value.reduce(
    (acc, item) => {
      acc.totalDrawCount += Number(item?.drawCount || item?.totalDrawCount || 0)
      acc.totalCostPoints += Number(item?.totalCostPoints || 0)
      acc.totalRewardPoints += Number(item?.totalRewardPoints || 0)
      acc.profitPoints += Number(item?.platformProfitPoints || 0)
      return acc
    },
    {
      totalDrawCount: 0,
      totalCostPoints: 0,
      totalRewardPoints: 0,
      profitPoints: 0
    }
  )

  return {
    ...historySummary,
    returnRate: historySummary.totalCostPoints > 0 ? historySummary.totalRewardPoints / historySummary.totalCostPoints : 0,
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
    historyList.value = history || []
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

const getReturnRateTone = (rate?: number): CnTone => {
  if (!rate) return 'neutral'
  if (rate > 0.9) return 'danger'
  if (rate > 0.75) return 'warning'
  return 'success'
}

onMounted(() => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 7)
  dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  loadStatistics()
})
</script>

<style scoped>
.data-statistics {
  display: grid;
  gap: var(--cn-space-4);
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  align-items: center;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
