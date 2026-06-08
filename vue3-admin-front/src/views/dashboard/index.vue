<template>
  <CnPage class="dashboard-page" surface="transparent" max-width="1240px">
    <CnPageHeader
      title="仪表板"
      :description="`今天是 ${currentDate}，这里汇总后台关键运营指标、模块健康和最近操作提示。`"
      eyebrow="Admin Workspace"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="success">当前角色：{{ currentRoles }}</CnStatusTag>
        <CnStatusTag v-if="lastLoginTime" type="info">上次登录：{{ lastLoginTime }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :loading="kpiLoading" @click="fetchDashboardData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="primary" @click="navigateTo('/user')">
          <el-icon><UserFilled /></el-icon>
          用户管理
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection surface="panel" class="dashboard-hero">
      <div class="dashboard-hero__grid">
        <div class="dashboard-hero__copy">
          <p class="dashboard-hero__eyebrow">欢迎回来</p>
          <h2 class="dashboard-hero__title">{{ displayName }}</h2>
          <p class="dashboard-hero__description">
            系统运行状态良好。优先关注异常操作、在线会话和模块健康，保持后台运营链路稳定。
          </p>
        </div>

        <div class="dashboard-quick" aria-label="快捷操作">
          <el-button type="primary" @click="navigateTo('/logs/login')">
            <el-icon><Document /></el-icon>
            登录日志
          </el-button>
          <el-button type="success" @click="navigateTo('/user')">
            <el-icon><UserFilled /></el-icon>
            用户管理
          </el-button>
          <el-button @click="navigateTo('/notification')">
            <el-icon><Bell /></el-icon>
            通知中心
          </el-button>
          <el-button @click="navigateTo('/profile/index')">
            <el-icon><User /></el-icon>
            个人中心
          </el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="关键指标" description="来自仪表板聚合接口，保留原有数据源和计数动画。" divided>
      <div v-loading="kpiLoading" class="dashboard-stat-grid">
        <CnStatCard
          v-for="card in kpiCards"
          :key="card.key"
          :title="card.title"
          :value="formatNumber(card.display)"
          :unit="card.unit"
          :description="card.description"
          :trend="card.trend"
          :trend-text="card.trendText"
          :tone="card.tone"
          :loading="kpiLoading"
        />
      </div>
    </CnSection>

    <div class="dashboard-detail-grid">
      <CnSection title="模块健康" description="关注核心模块可用性、响应状态和接口延迟。" divided>
        <div v-if="moduleHealth.length > 0" class="health-list">
          <div v-for="(item, index) in moduleHealth" :key="`${item.name}-${index}`" class="health-item">
            <div class="health-item__main">
              <span class="health-item__dot" :class="`is-${getHealthTone(item)}`" aria-hidden="true" />
              <div class="health-item__copy">
                <span class="health-item__name">{{ item.name || '未命名模块' }}</span>
                <span class="health-item__latency">{{ item.latency || '暂无延迟数据' }}</span>
              </div>
            </div>

            <CnStatusTag :type="getHealthTone(item)" size="sm">
              {{ item.statusText || '未知' }}
            </CnStatusTag>
          </div>
        </div>

        <CnEmptyState
          v-else
          title="暂无健康数据"
          description="当前接口没有返回模块健康列表。"
          icon="HE"
          surface="transparent"
          size="sm"
        />
      </CnSection>

      <CnSection title="最近操作提示" description="辅助管理员快速判断近期后台活动。" divided>
        <div v-if="recentOps.length > 0" class="timeline-list">
          <div v-for="item in recentOps" :key="item.id" class="timeline-item">
            <span class="timeline-item__time">{{ item.time }}</span>
            <div class="timeline-item__content">
              <p class="timeline-item__title">{{ item.title }}</p>
              <p class="timeline-item__desc">{{ item.desc }}</p>
            </div>
          </div>
        </div>

        <CnEmptyState
          v-else
          title="暂无操作记录"
          description="近期还没有可展示的操作提示。"
          icon="OP"
          surface="transparent"
          size="sm"
        />
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Document, Refresh, User, UserFilled } from '@element-plus/icons-vue'
import { getDashboardOverview } from '@/api/dashboard'
import { useUserStore } from '@/stores/user'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTone, CnTrend } from '@/design-system'

interface UserRole {
  roleName?: string
}

interface KpiCard {
  key: 'login' | 'chat' | 'risk' | 'points'
  title: string
  value: number
  display: number
  unit: string
  description: string
  trend: CnTrend
  trendText: string
  tone: CnTone
}

interface ModuleHealthItem {
  name?: string
  status?: string
  statusType?: string
  statusText?: string
  latency?: string
}

interface RecentOperation {
  id: string | number
  time: string
  title: string
  desc: string
}

interface DashboardOverview {
  todayLoginCount?: number
  onlineUserCount?: number
  todayFailedOperationCount?: number
  totalPointsIssued?: number
  totalUsers?: number
  activePointUsers?: number
  moduleHealthList?: ModuleHealthItem[]
  recentOperations?: RecentOperation[]
}

const router = useRouter()
const userStore = useUserStore()

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '仪表板' }]

const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const displayName = computed(() => userStore.realName || userStore.username || '管理员')

const roles = computed<UserRole[]>(() => (Array.isArray(userStore.roles) ? userStore.roles : []))

const currentRoles = computed(() => {
  if (roles.value.length === 0) return '暂无角色'
  return roles.value.map((role) => role.roleName || '未命名角色').join('、')
})

const lastLoginTime = computed(() => userStore.userInfo?.lastLoginTime || '')

const kpiLoading = ref(false)

const kpiCards = ref<KpiCard[]>([
  {
    key: 'login',
    title: '今日登录',
    value: 0,
    display: 0,
    unit: '',
    description: '来源：仪表板聚合',
    trend: 'flat',
    trendText: '实时',
    tone: 'brand'
  },
  {
    key: 'chat',
    title: '在线会话',
    value: 0,
    display: 0,
    unit: '',
    description: '来源：仪表板聚合',
    trend: 'flat',
    trendText: '实时',
    tone: 'success'
  },
  {
    key: 'risk',
    title: '今日失败操作',
    value: 0,
    display: 0,
    unit: '',
    description: '来源：仪表板聚合',
    trend: 'flat',
    trendText: '待关注',
    tone: 'warning'
  },
  {
    key: 'points',
    title: '累计积分发放',
    value: 0,
    display: 0,
    unit: ' 分',
    description: '来源：仪表板聚合',
    trend: 'flat',
    trendText: '累计',
    tone: 'info'
  }
])

const moduleHealth = ref<ModuleHealthItem[]>([])
const recentOps = ref<RecentOperation[]>([])

const formatNumber = (value: number | string) => {
  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) {
    return '0'
  }
  if (numericValue >= 10000) {
    return `${(numericValue / 10000).toFixed(1)}w`
  }
  return numericValue.toLocaleString('zh-CN')
}

const animateKpi = (targets: number[]) => {
  const duration = 1200
  const start = performance.now()

  const tick = (now: number) => {
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)

    kpiCards.value.forEach((item, index) => {
      const target = targets[index] ?? 0
      item.display = Math.round(target * eased)
    })

    if (progress < 1) {
      requestAnimationFrame(tick)
    }
  }

  requestAnimationFrame(tick)
}

const setKpiValue = (key: KpiCard['key'], value: unknown) => {
  const card = kpiCards.value.find((item) => item.key === key)
  if (!card) return
  card.value = Number(value) || 0
}

const getHealthTone = (item: ModuleHealthItem): CnTone => {
  const status = `${item.status || item.statusType || item.statusText || ''}`.toLowerCase()
  if (['healthy', 'success', 'normal', 'up', 'ok', '正常'].some((key) => status.includes(key))) {
    return 'success'
  }
  if (['warning', 'warn', 'degraded', '慢', '警告'].some((key) => status.includes(key))) {
    return 'warning'
  }
  if (['danger', 'error', 'down', 'fail', '异常', '失败'].some((key) => status.includes(key))) {
    return 'danger'
  }
  return 'neutral'
}

const navigateTo = (path: string) => {
  router.push(path)
}

const fetchDashboardData = async () => {
  kpiLoading.value = true

  try {
    const overview = (await getDashboardOverview()) as DashboardOverview

    setKpiValue('login', overview?.todayLoginCount || 0)
    setKpiValue('chat', overview?.onlineUserCount || 0)
    setKpiValue('risk', overview?.todayFailedOperationCount || 0)
    setKpiValue('points', overview?.totalPointsIssued || 0)

    moduleHealth.value = Array.isArray(overview?.moduleHealthList) ? overview.moduleHealthList : []
    recentOps.value = Array.isArray(overview?.recentOperations) ? overview.recentOperations : []

    const userTotal = Number(overview?.totalUsers) || 0
    const loginCard = kpiCards.value.find((item) => item.key === 'login')
    if (loginCard) {
      loginCard.description = `用户总量 ${formatNumber(userTotal)}`
    }

    const activePointUsers = Number(overview?.activePointUsers) || 0
    const pointsCard = kpiCards.value.find((item) => item.key === 'points')
    if (pointsCard) {
      pointsCard.description = `活跃积分用户 ${formatNumber(activePointUsers)}`
    }

    animateKpi(kpiCards.value.map((item) => item.value))
  } catch (error) {
    ElMessage.error('仪表板数据加载失败，请稍后重试')
  } finally {
    kpiLoading.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
}

.dashboard-hero {
  overflow: hidden;
}

.dashboard-hero__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 360px);
  gap: var(--cn-space-6);
  align-items: center;
}

.dashboard-hero__copy {
  min-width: 0;
}

.dashboard-hero__eyebrow {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.dashboard-hero__title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.dashboard-hero__description {
  max-width: 720px;
  margin: var(--cn-space-3) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.dashboard-quick {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.dashboard-quick .el-button {
  width: 100%;
  min-width: 0;
  margin: 0;
  justify-content: flex-start;
}

.dashboard-stat-grid,
.dashboard-detail-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.dashboard-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.health-list,
.timeline-list {
  display: grid;
  gap: var(--cn-space-3);
}

.health-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.health-item__main {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
}

.health-item__dot {
  width: 8px;
  height: 8px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-text-tertiary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cn-color-text-tertiary) 16%, transparent);
  flex-shrink: 0;
}

.health-item__dot.is-success {
  background: var(--cn-color-success);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cn-color-success) 16%, transparent);
}

.health-item__dot.is-warning {
  background: var(--cn-color-warning);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cn-color-warning) 16%, transparent);
}

.health-item__dot.is-danger {
  background: var(--cn-color-danger);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--cn-color-danger) 16%, transparent);
}

.health-item__copy {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
}

.health-item__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.health-item__latency {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  gap: var(--cn-space-3);
}

.timeline-item__time {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  align-self: start;
  min-height: 24px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 700;
}

.timeline-item__content {
  min-width: 0;
  padding-bottom: var(--cn-space-3);
  border-bottom: 1px dashed var(--cn-color-border-subtle);
}

.timeline-item:last-child .timeline-item__content {
  padding-bottom: 0;
  border-bottom: 0;
}

.timeline-item__title,
.timeline-item__desc {
  margin: 0;
}

.timeline-item__title {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.45;
}

.timeline-item__desc {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 1180px) {
  .dashboard-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .dashboard-hero__grid,
  .dashboard-detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .dashboard-stat-grid,
  .dashboard-quick {
    grid-template-columns: 1fr;
  }

  .dashboard-hero__title {
    font-size: 23px;
  }

  .health-item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
