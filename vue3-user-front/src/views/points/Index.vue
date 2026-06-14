<template>
  <CnPage class="points-page" max-width="1040px" full-height>
    <CnPageHeader
      title="积分中心"
      description="查看积分余额、每日打卡状态和最近积分变动。"
      eyebrow="POINTS"
    >
      <template #meta>
        <CnStatusTag :type="pointsBalance?.hasCheckedToday ? 'success' : 'warning'" size="sm">
          {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '今日待打卡' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>连续 {{ pointsBalance?.continuousDays || 0 }} 天</CnStatusTag>
      </template>
    </CnPageHeader>

    <CnSection class="balance-section" surface="panel">
      <div class="balance-layout">
        <div class="balance-copy">
          <p class="balance-label">当前可用积分</p>
          <div class="balance-value">{{ pointsBalance?.totalPoints || 0 }}</div>
          <p class="balance-description">积分可用于兑换礼品、服务权益和平台内消耗操作。</p>
        </div>

        <div class="checkin-panel">
          <div>
            <p class="checkin-title">每日打卡</p>
            <p class="checkin-copy">
              明日打卡预计可获得
              <strong>{{ pointsBalance?.nextDayPoints || 0 }}</strong>
              积分
            </p>
          </div>
          <el-button
            type="primary"
            size="large"
            :loading="checkinLoading"
            :disabled="pointsBalance?.hasCheckedToday || checkinLoading"
            @click="handleCheckin"
          >
            {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '立即打卡' }}
          </el-button>
        </div>
      </div>
    </CnSection>

    <div class="stats-grid">
      <CnStatCard
        title="积分余额"
        :value="pointsBalance?.totalPoints || 0"
        unit="分"
        description="当前账户可用积分"
        tone="brand"
      />
      <CnStatCard
        title="连续打卡"
        :value="pointsBalance?.continuousDays || 0"
        unit="天"
        description="保持打卡可提升奖励"
        tone="success"
      />
      <CnStatCard
        title="明日奖励"
        :value="pointsBalance?.nextDayPoints || 0"
        unit="分"
        description="按当前连续天数估算"
        tone="warning"
      />
    </div>

    <div class="action-grid" aria-label="积分功能入口">
      <button class="action-card" type="button" @click="showDetailDialog = true">
        <span class="action-mark">DETAIL</span>
        <span class="action-title">积分明细</span>
        <span class="action-desc">查看收入和支出记录</span>
      </button>

      <button class="action-card" type="button" @click="showCalendarDialog = true">
        <span class="action-mark">CAL</span>
        <span class="action-title">打卡日历</span>
        <span class="action-desc">查看每月打卡轨迹</span>
      </button>

      <button class="action-card" type="button" @click="showStatisticsDialog = true">
        <span class="action-mark">STAT</span>
        <span class="action-title">打卡统计</span>
        <span class="action-desc">追踪连续与累计表现</span>
      </button>
    </div>

    <CnSection title="最近积分明细" description="展示最近 5 条积分变动。" divided>
      <template #actions>
        <el-button type="primary" plain @click="showDetailDialog = true">查看全部</el-button>
      </template>

      <CnEmptyState
        v-if="recentDetails.length === 0"
        title="暂无积分明细"
        description="完成打卡或获得奖励后，这里会出现最新记录。"
        icon="PT"
      />

      <div v-else class="detail-list">
        <article v-for="detail in recentDetails" :key="detail.id" class="detail-item">
          <div class="detail-main">
            <div class="detail-heading">
              <span class="detail-type">{{ getPointsTypeText(detail.pointsType) }}</span>
              <CnStatusTag
                :type="(detail.pointsChange || 0) >= 0 ? 'success' : 'danger'"
                size="sm"
                :dot="false"
              >
                {{ formatPointsChange(detail.pointsChange) }}
              </CnStatusTag>
            </div>
            <p class="detail-desc">{{ detail.description || '积分变动' }}</p>
            <p class="detail-time">{{ formatTime(detail.createTime) }}</p>
          </div>
          <div class="detail-balance">余额 {{ detail.balanceAfter ?? '-' }}</div>
        </article>
      </div>
    </CnSection>

    <PointsDetailDialog v-model="showDetailDialog" />
    <CheckinCalendarDialog v-model="showCalendarDialog" />
    <CheckinStatisticsDialog v-model="showStatisticsDialog" />
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import pointsApi from '@/api/points'
import PointsDetailDialog from './components/PointsDetailDialog.vue'
import CheckinCalendarDialog from './components/CheckinCalendarDialog.vue'
import CheckinStatisticsDialog from './components/CheckinStatisticsDialog.vue'

interface PointsBalance {
  totalPoints?: number
  continuousDays?: number
  nextDayPoints?: number
  hasCheckedToday?: boolean
}

interface PointsDetail {
  id: number | string
  pointsType?: string
  description?: string
  createTime?: string
  pointsChange?: number
  balanceAfter?: number
}

interface CheckinResult {
  pointsEarned?: number
}

const pointsBalance = ref<PointsBalance | null>(null)
const recentDetails = ref<PointsDetail[]>([])
const checkinLoading = ref(false)

const showDetailDialog = ref(false)
const showCalendarDialog = ref(false)
const showStatisticsDialog = ref(false)

onMounted(() => {
  loadPointsBalance()
  loadRecentDetails()
})

const loadPointsBalance = async () => {
  try {
    const response = (await pointsApi.getPointsBalance()) as PointsBalance
    pointsBalance.value = response
  } catch (error) {
    console.error('加载积分余额失败:', error)
    ElMessage.error('加载积分信息失败')
  }
}

const loadRecentDetails = async () => {
  try {
    const response = (await pointsApi.getPointsDetailList({
      pageNum: 1,
      pageSize: 5
    })) as { records?: PointsDetail[] }
    recentDetails.value = response.records || []
  } catch (error) {
    console.error('加载积分明细失败:', error)
  }
}

const handleCheckin = async () => {
  if (pointsBalance.value?.hasCheckedToday) {
    ElMessage.warning('今日已打卡，请勿重复操作')
    return
  }

  checkinLoading.value = true

  try {
    const response = (await pointsApi.checkin()) as CheckinResult

    ElMessage.success({
      message: `打卡成功！获得 ${response.pointsEarned || 0} 积分`,
      duration: 3000
    })

    await loadPointsBalance()
    await loadRecentDetails()
  } catch (error) {
    console.error('打卡失败:', error)
    ElMessage.error(getErrorMessage(error) || '打卡失败，请重试')
  } finally {
    checkinLoading.value = false
  }
}

const getPointsTypeText = (type?: string) => {
  const typeMap: Record<string, string> = {
    CHECK_IN: '每日打卡',
    ADMIN_GRANT: '管理员发放'
  }
  return type ? typeMap[type] || type : '积分变动'
}

const formatPointsChange = (value?: number) => {
  const safeValue = value || 0
  return `${safeValue > 0 ? '+' : ''}${safeValue}`
}

const formatTime = (dateTime?: string) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const now = new Date()
  const diffTime = now.getTime() - date.getTime()
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 0) {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  if (diffDays === 1) {
    return `昨天 ${date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })}`
  }

  return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })}`
}

const getErrorMessage = (error: unknown) => {
  return error instanceof Error ? error.message : ''
}
</script>

<style lang="scss" scoped>
.points-page {
  .balance-section {
    overflow: hidden;
  }
}

.balance-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 0.8fr);
  gap: var(--cn-space-6);
  align-items: stretch;
}

.balance-copy {
  min-width: 0;
}

.balance-label {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.balance-value {
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 56px;
  font-weight: 760;
  line-height: 1;
  overflow-wrap: anywhere;
}

.balance-description {
  max-width: 520px;
  margin: var(--cn-space-4) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.checkin-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--cn-space-5);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface-muted);
}

.checkin-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 700;
}

.checkin-copy {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;

  strong {
    color: var(--cn-color-brand-primary);
  }
}

.stats-grid,
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.action-card {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  color: var(--cn-color-text-primary);
  text-align: left;
  cursor: pointer;
  box-shadow: var(--cn-card-shadow);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);

  &:hover {
    transform: translateY(-1px);
    border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-card-border));
    box-shadow: var(--cn-shadow-sm);
  }
}

.action-mark {
  width: fit-content;
  padding: 3px 8px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.3;
}

.action-title {
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 700;
}

.action-desc {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.detail-list {
  display: grid;
  gap: var(--cn-space-3);
}

.detail-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.detail-main {
  min-width: 0;
}

.detail-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.detail-type {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.detail-desc,
.detail-time,
.detail-balance {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.detail-time {
  color: var(--cn-color-text-tertiary);
}

.detail-balance {
  flex-shrink: 0;
  margin-top: 0;
  text-align: right;
  white-space: nowrap;
}

@media (max-width: 860px) {
  .balance-layout,
  .stats-grid,
  .action-grid {
    grid-template-columns: 1fr;
  }

  .balance-value {
    font-size: 44px;
  }
}

@media (max-width: 640px) {
  .detail-item {
    display: grid;
  }

  .detail-balance {
    text-align: left;
  }
}
</style>
