<template>
  <CnPage max-width="1180px" full-height>
    <CnPageHeader
      title="幸运抽奖"
      description="每次消耗 100 积分，九宫格跑灯结束后奖励会自动结算到账。"
      eyebrow="REWARDS"
    >
      <template #meta>
        <CnStatusTag :type="canDraw ? 'success' : 'warning'" size="sm">
          {{ canDraw ? '可抽奖' : getDrawButtonText }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>单次 100 积分</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="pageLoading" @click="loadInitialData">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <div class="stat-grid">
      <CnStatCard title="我的积分" :value="userPoints" unit="分" description="当前账户可用积分" tone="brand" />
      <CnStatCard title="今日剩余次数" :value="remainingCount" unit="次" description="每日抽奖次数限制" tone="warning" />
      <CnStatCard
        title="累计抽奖"
        :value="statistics.totalDrawCount || 0"
        unit="次"
        description="历史抽奖总次数"
        tone="info"
      />
      <CnStatCard
        title="累计中奖"
        :value="statistics.totalWinCount || 0"
        unit="次"
        description="历史中奖总次数"
        tone="success"
      />
    </div>

    <CnSection title="九宫格抽奖" description="跑灯动画停止的位置即为本次抽奖结果。" class="lottery-section">
      <div v-if="gridItems.length > 0" class="lottery-grid-wrapper">
        <div class="lottery-grid">
          <div
            v-for="(item, index) in gridItems"
            :key="index"
            class="grid-item"
            :class="{
              'is-active': currentIndex === index,
              'is-center': index === 4,
              'is-winner': winnerIndex === index && showWinner
            }"
          >
            <div v-if="index === 4" class="draw-button-wrapper">
              <el-button
                type="danger"
                :loading="drawing"
                :disabled="!canDraw"
                size="large"
                class="draw-btn"
                :title="!canDraw ? getDrawButtonTip : ''"
                @click.stop="handleDraw"
              >
                <span v-if="!drawing">{{ canDraw ? '立即抽奖' : getDrawButtonText }}</span>
                <span v-else>抽奖中...</span>
              </el-button>
              <div class="cost-tips">消耗 100 积分</div>
            </div>

            <div v-else-if="item" class="prize-box">
              <div class="prize-level">{{ getPrizeLevelShort(item.prizeLevel) }}</div>
              <div class="prize-name">{{ item.prizeName }}</div>
              <div class="prize-points">{{ item.prizePoints || 0 }} 积分</div>
              <CnStatusTag :type="getPrizeTone(item.prizeLevel)" size="sm" :dot="false">
                {{ ((item.probability || 0) * 100).toFixed(1) }}%
              </CnStatusTag>
            </div>
          </div>
        </div>
      </div>

      <CnEmptyState
        v-else
        title="暂无奖品配置"
        description="奖品列表加载完成后即可开始抽奖。"
        icon="LOT"
      />
    </CnSection>

    <div class="content-grid">
      <CnSection title="抽奖规则" description="平台当前生效的抽奖规则。" divided>
        <div class="rules-content" v-html="sanitizedRules"></div>
      </CnSection>

      <CnSection title="我的抽奖记录" description="按时间倒序展示抽奖结果。" divided>
        <template #actions>
          <el-button type="primary" plain :icon="Refresh" :loading="recordsLoading" @click="loadRecords">
            刷新
          </el-button>
        </template>

        <CnDataTable
          :columns="recordColumns"
          :data="recordList"
          :loading="recordsLoading"
          :pagination="recordPagination"
          row-key="id"
          empty-title="暂无抽奖记录"
          empty-description="完成一次抽奖后，记录会显示在这里。"
          empty-icon="LOT"
          @page-change="handleRecordPageChange"
          @page-size-change="handleRecordPageSizeChange"
        >
          <template #prizeLevel="{ row }">
            <CnStatusTag :type="getPrizeTone(row.prizeLevel)" size="sm">
              {{ getPrizeLevelName(row.prizeLevel) }}
            </CnStatusTag>
          </template>

          <template #prizePoints="{ row }">
            <CnStatusTag type="success" size="sm" :dot="false">+{{ row.prizePoints || 0 }}</CnStatusTag>
          </template>
        </CnDataTable>
      </CnSection>
    </div>

    <el-dialog
      v-model="resultDialog"
      :title="drawResult?.prizeLevel === 8 ? '很遗憾' : '恭喜中奖'"
      width="500px"
      :show-close="false"
      center
    >
      <div class="result-content">
        <div v-if="drawResult?.prizeLevel === 8" class="result-state result-state--fail">
          <div class="result-mark">MISS</div>
          <div class="result-text">{{ drawResult.prizeName }}</div>
          <div class="result-tips">再接再厉，下次继续。</div>
        </div>

        <div v-else class="result-state result-state--success">
          <div class="result-mark">WIN</div>
          <div class="result-prize-name">{{ drawResult?.prizeName }}</div>
          <div class="result-prize-points">+{{ drawResult?.prizePoints || 0 }} 积分</div>
          <div class="result-tips">奖励已发放到您的账户。</div>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" size="large" @click="handleContinue">
          {{ canDraw ? '再抽一次' : '确定' }}
        </el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnPagination,
  type CnTableColumn,
  type CnTone
} from '@/design-system'
import { lotteryApi } from '@/api/lottery'
import pointsApi from '@/api/points'
import { sanitizeHtml } from '@/utils/markdown'

interface Prize {
  prizeId?: number | string
  prizeName?: string
  prizeLevel?: number
  prizePoints?: number
  probability?: number
}

interface LotteryStatistics {
  totalDrawCount?: number
  totalWinCount?: number
}

interface PointsBalance {
  totalPoints?: number
}

interface DrawRecord {
  id?: number | string
  prizeName?: string
  prizeLevel?: number
  prizePoints?: number
  strategyType?: string
  createTime?: string
}

interface RecordResponse {
  records?: DrawRecord[]
  total?: number
}

type GridItem = Prize | null

const pageLoading = ref(false)
const recordsLoading = ref(false)
const drawing = ref(false)
const resultDialog = ref(false)
const drawResult = ref<Prize | null>(null)

const prizeList = ref<Prize[]>([])
const gridItems = ref<GridItem[]>([])
const remainingCount = ref(0)
const statistics = ref<LotteryStatistics>({})
const rules = ref('')
const recordList = ref<DrawRecord[]>([])
const recordTotal = ref(0)
const pointsBalance = ref<PointsBalance | null>(null)

const currentIndex = ref(-1)
const winnerIndex = ref(-1)
const showWinner = ref(false)

const recordQuery = reactive({
  page: 1,
  size: 10
})

const sanitizedRules = computed(() => sanitizeHtml(rules.value || ''))
const userPoints = computed(() => pointsBalance.value?.totalPoints || 0)
const canDraw = computed(() => remainingCount.value > 0 && userPoints.value >= 100 && !drawing.value)

const getDrawButtonText = computed(() => {
  if (remainingCount.value <= 0) return '次数不足'
  if (userPoints.value < 100) return '积分不足'
  return '立即抽奖'
})

const getDrawButtonTip = computed(() => {
  if (remainingCount.value <= 0) return '今日抽奖次数已用完'
  if (userPoints.value < 100) return `当前积分：${userPoints.value}，需要 100 积分`
  return ''
})

const recordColumns: CnTableColumn<DrawRecord>[] = [
  { prop: 'prizeName', label: '奖品', minWidth: 160, showOverflowTooltip: true },
  { prop: 'prizeLevel', label: '奖品等级', width: 120, slot: 'prizeLevel' },
  { prop: 'prizePoints', label: '获得积分', width: 120, align: 'center', slot: 'prizePoints' },
  { prop: 'strategyType', label: '抽奖策略', minWidth: 140, showOverflowTooltip: true },
  { prop: 'createTime', label: '抽奖时间', minWidth: 180 }
]

const recordPagination = computed<CnPagination>(() => ({
  page: recordQuery.page,
  pageSize: recordQuery.size,
  total: recordTotal.value,
  pageSizes: [5, 10, 20],
  layout: 'total, sizes, prev, pager, next'
}))

onMounted(() => {
  loadInitialData()
})

const loadInitialData = async () => {
  pageLoading.value = true

  try {
    await Promise.all([
      loadPrizeList(),
      loadRemainingCount(),
      loadStatistics(),
      loadRules(),
      loadRecords(),
      loadPointsBalance()
    ])
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '抽奖页初始化失败')
  } finally {
    pageLoading.value = false
  }
}

const loadPointsBalance = async () => {
  try {
    pointsBalance.value = (await pointsApi.getPointsBalance()) as PointsBalance
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载积分余额失败')
  }
}

const loadPrizeList = async () => {
  try {
    const prizes = ((await lotteryApi.getPrizeList()) || []) as Prize[]
    prizeList.value = prizes

    if (prizes.length === 0) {
      gridItems.value = []
      return
    }

    const items: Prize[] = []
    for (let i = 0; i < Math.min(8, prizes.length); i += 1) {
      items.push(prizes[i])
    }

    while (items.length < 8) {
      items.push(...prizes.slice(0, Math.min(8 - items.length, prizes.length)))
    }

    const nextGrid: GridItem[] = [...items]
    nextGrid.splice(4, 0, null)
    gridItems.value = nextGrid
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载奖品列表失败')
  }
}

const loadRemainingCount = async () => {
  try {
    remainingCount.value = ((await lotteryApi.getRemainingCount()) || 0) as number
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载剩余次数失败')
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = ((await lotteryApi.getStatistics()) || {}) as LotteryStatistics
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载统计信息失败')
  }
}

const loadRules = async () => {
  try {
    rules.value = ((await lotteryApi.getRules()) || '') as string
  } catch (error) {
    rules.value = `
      <ul>
        <li>每次抽奖消耗 100 积分</li>
        <li>每日最多抽奖 10 次</li>
        <li>中奖后积分立即到账</li>
        <li>连续 20 次未中奖，必中三等奖及以上</li>
        <li>奖品库存有限，先到先得</li>
      </ul>
    `
  }
}

const loadRecords = async () => {
  recordsLoading.value = true

  try {
    const res = (await lotteryApi.getDrawRecords(recordQuery)) as RecordResponse
    recordList.value = res.records || []
    recordTotal.value = res.total || 0
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '加载记录失败')
  } finally {
    recordsLoading.value = false
  }
}

const handleRecordPageChange = (page: number) => {
  recordQuery.page = page
  loadRecords()
}

const handleRecordPageSizeChange = (size: number) => {
  recordQuery.page = 1
  recordQuery.size = size
  loadRecords()
}

const handleDraw = async () => {
  if (!canDraw.value) {
    ElMessage.warning('今日抽奖次数已用完或积分不足')
    return
  }

  drawing.value = true
  currentIndex.value = -1
  showWinner.value = false

  try {
    drawResult.value = (await lotteryApi.draw({ strategyType: 'ALIAS_METHOD' })) as Prize

    const targetPrizeId = drawResult.value.prizeId
    let targetIndex = gridItems.value.findIndex(
      (item, index) => index !== 4 && Boolean(item) && item?.prizeId === targetPrizeId
    )

    if (targetIndex === -1) {
      const validIndexes = [0, 1, 2, 3, 5, 6, 7, 8]
      targetIndex = validIndexes[Math.floor(Math.random() * validIndexes.length)]
    }

    await runLotteryAnimation(targetIndex)

    winnerIndex.value = targetIndex
    showWinner.value = true

    window.setTimeout(() => {
      drawing.value = false
      resultDialog.value = true
      currentIndex.value = -1

      loadRemainingCount()
      loadStatistics()
      loadRecords()
      loadPointsBalance()
    }, 1000)
  } catch (error) {
    drawing.value = false
    currentIndex.value = -1
    ElMessage.error(getErrorMessage(error) || '抽奖失败')
  }
}

const runLotteryAnimation = (targetIndex: number) => {
  return new Promise<void>((resolve) => {
    const path = [0, 1, 2, 5, 8, 7, 6, 3]
    let currentStep = 0
    const totalSteps = 3 * 8 + path.indexOf(targetIndex) + 1
    const initialSpeed = 100
    const finalSpeed = 300

    const animate = () => {
      if (currentStep >= totalSteps) {
        resolve()
        return
      }

      currentIndex.value = path[currentStep % 8]
      currentStep += 1

      const progress = currentStep / totalSteps
      const speed = initialSpeed + (finalSpeed - initialSpeed) * Math.pow(progress, 2)

      window.setTimeout(animate, speed)
    }

    animate()
  })
}

const handleContinue = () => {
  resultDialog.value = false
  showWinner.value = false
  winnerIndex.value = -1
}

const getPrizeLevelShort = (level?: number) => {
  const names = ['', '特', '一', '二', '三', '四', '五', '六', '空']
  return names[level || 0] || '奖'
}

const getPrizeLevelName = (level?: number) => {
  const names = ['', '特等奖', '一等奖', '二等奖', '三等奖', '四等奖', '五等奖', '六等奖', '未中奖']
  return names[level || 0] || '未知'
}

const getPrizeTone = (level?: number): CnTone => {
  if (level === 1) return 'danger'
  if (level && level <= 3) return 'warning'
  if (level && level <= 7) return 'success'
  return 'info'
}

const getErrorMessage = (error: unknown) => {
  return error instanceof Error ? error.message : ''
}
</script>

<style scoped lang="scss">
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.lottery-section {
  overflow: hidden;
}

.lottery-grid-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-5);
}

.lottery-grid {
  display: grid;
  grid-template-columns: repeat(3, 148px);
  grid-template-rows: repeat(3, 148px);
  gap: var(--cn-space-3);
}

.grid-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
  overflow: hidden;
  border: 2px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);

  &.is-active {
    transform: scale(1.04);
    border-color: var(--cn-color-brand-primary);
    box-shadow: 0 0 0 4px var(--cn-color-brand-soft);
  }

  &.is-winner {
    transform: scale(1.06);
    border-color: var(--cn-color-danger);
    box-shadow: 0 0 0 5px var(--cn-color-danger-soft);
    animation: winner-pulse 0.6s var(--cn-ease-in-out) infinite;
  }

  &.is-center {
    border-color: color-mix(in srgb, var(--cn-color-brand-primary) 44%, var(--cn-color-border-subtle));
    background: color-mix(in srgb, var(--cn-color-bg-surface) 82%, var(--cn-color-brand-soft));
  }
}

.draw-button-wrapper,
.prize-box {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-2);
  width: 100%;
  height: 100%;
  min-width: 0;
  padding: var(--cn-space-3);
  text-align: center;
}

.draw-btn {
  font-weight: 700;
}

.cost-tips {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.prize-level {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 18px;
  font-weight: 800;
}

.prize-name {
  max-width: 100%;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prize-points {
  color: var(--cn-color-success);
  font-size: 13px;
  font-weight: 800;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.4fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.rules-content {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.8;

  :deep(ul) {
    margin: 0;
    padding-left: 20px;
  }

  :deep(li + li) {
    margin-top: var(--cn-space-2);
  }
}

.result-content {
  padding: var(--cn-space-4) 0;
  text-align: center;
}

.result-state {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-3);
}

.result-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: var(--cn-radius-panel);
  font-family: var(--cn-font-heading);
  font-size: 18px;
  font-weight: 800;
}

.result-state--success .result-mark {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.result-state--fail .result-mark {
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
}

.result-text,
.result-prize-name {
  color: var(--cn-color-text-primary);
  font-size: 24px;
  font-weight: 750;
}

.result-prize-points {
  color: var(--cn-color-success);
  font-size: 32px;
  font-weight: 800;
}

.result-tips {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
}

@keyframes winner-pulse {
  0%,
  100% {
    transform: scale(1.06);
  }

  50% {
    transform: scale(1.1);
  }
}

@media (max-width: 980px) {
  .stat-grid,
  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .stat-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .lottery-grid-wrapper {
    padding: var(--cn-space-3) 0;
    overflow-x: auto;
    justify-content: flex-start;
  }

  .lottery-grid {
    grid-template-columns: repeat(3, 104px);
    grid-template-rows: repeat(3, 104px);
    gap: var(--cn-space-2);
  }

  .prize-level {
    width: 34px;
    height: 34px;
    font-size: 15px;
  }

  .prize-name,
  .prize-points {
    font-size: 12px;
  }
}
</style>
