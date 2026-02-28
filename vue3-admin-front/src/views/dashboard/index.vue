<template>
  <div class="dashboard">
    <h1 class="page-title">仪表板</h1>

    <el-row :gutter="20" class="hero-row">
      <el-col :xs="24" :lg="15">
        <el-card class="hero-card">
          <div class="hero-content">
            <div class="welcome-content">
              <h2 class="welcome-title">欢迎回来，{{ userStore.realName || userStore.username }}！</h2>
              <p class="welcome-subtitle">今天是 {{ currentDate }}，系统运行状态良好，祝你工作顺利。</p>

              <div class="role-line" v-if="userStore.roles.length > 0">
                <el-tag type="success" class="role-tag">当前角色：{{ currentRoles }}</el-tag>
                <el-tag type="info" v-if="userStore.userInfo?.lastLoginTime">
                  上次登录：{{ userStore.userInfo.lastLoginTime }}
                </el-tag>
              </div>
            </div>

            <div class="hero-decoration" aria-hidden="true">
              <span class="pulse-ring ring-1"></span>
              <span class="pulse-ring ring-2"></span>
              <span class="pulse-core">
                <el-icon><DataAnalysis /></el-icon>
              </span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card class="action-card">
          <template #header>
            <span>快捷操作</span>
          </template>

          <div class="quick-actions">
            <el-button type="primary" class="quick-btn" @click="$router.push('/logs/login')">
              <el-icon><Document /></el-icon>
              登录日志
            </el-button>

            <el-button type="success" class="quick-btn" @click="$router.push('/user')">
              <el-icon><UserFilled /></el-icon>
              用户管理
            </el-button>

            <el-button class="quick-btn" @click="$router.push('/notification')">
              <el-icon><Bell /></el-icon>
              通知中心
            </el-button>

            <el-button class="quick-btn" @click="$router.push('/profile/index')">
              <el-icon><User /></el-icon>
              个人中心
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="kpi-row" v-loading="kpiLoading">
      <el-col v-for="card in kpiCards" :key="card.key" :xs="24" :sm="12" :lg="6">
        <el-card class="kpi-card cn-hover-lift">
          <div class="kpi-top">
            <span class="kpi-title">{{ card.title }}</span>
            <span class="kpi-icon" :style="{ background: card.iconBg }">
              <el-icon><component :is="card.icon" /></el-icon>
            </span>
          </div>

          <div class="kpi-value">{{ formatNumber(card.display) }}{{ card.unit }}</div>

          <div class="kpi-meta">
            <span>{{ card.meta }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="detail-row">
      <el-col :xs="24" :lg="12">
        <el-card class="panel-card">
          <template #header>
            <span>模块健康</span>
          </template>

          <div class="health-list" v-if="moduleHealth.length > 0">
            <div v-for="item in moduleHealth" :key="item.name" class="health-item">
              <div class="health-left">
                <span class="health-dot" :class="item.status"></span>
                <span class="health-name">{{ item.name }}</span>
              </div>

              <div class="health-meta">
                <span class="health-latency">{{ item.latency }}</span>
                <el-tag :type="item.statusType" effect="light" size="small">{{ item.statusText }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无健康数据" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="panel-card">
          <template #header>
            <span>最近操作提示</span>
          </template>

          <div class="timeline-list" v-if="recentOps.length > 0">
            <div v-for="item in recentOps" :key="item.id" class="timeline-item">
              <span class="timeline-time">{{ item.time }}</span>
              <div class="timeline-content">
                <div class="timeline-title">{{ item.title }}</div>
                <div class="timeline-desc">{{ item.desc }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无操作记录" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Document, UserFilled, Bell, User, ChatDotRound, Warning, Coin } from '@element-plus/icons-vue'
import { getDashboardOverview } from '@/api/dashboard'

const userStore = useUserStore()

// 当前日期
const currentDate = computed(() => {
  const now = new Date()
  return now.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

// 当前角色
const currentRoles = computed(() => {
  if (userStore.roles.length === 0) return '暂无角色'
  return userStore.roles.map((role) => role.roleName).join('、')
})

const kpiLoading = ref(false)

const kpiCards = ref([
  {
    key: 'login',
    title: '今日登录',
    value: 0,
    display: 0,
    unit: '',
    meta: '来源：仪表板聚合',
    icon: UserFilled,
    iconBg: 'linear-gradient(135deg, #2f7bff 0%, #1f6feb 100%)'
  },
  {
    key: 'chat',
    title: '在线会话',
    value: 0,
    display: 0,
    unit: '',
    meta: '来源：仪表板聚合',
    icon: ChatDotRound,
    iconBg: 'linear-gradient(135deg, #28b887 0%, #1f9d64 100%)'
  },
  {
    key: 'risk',
    title: '今日失败操作',
    value: 0,
    display: 0,
    unit: '',
    meta: '来源：仪表板聚合',
    icon: Warning,
    iconBg: 'linear-gradient(135deg, #f6a23f 0%, #d9911b 100%)'
  },
  {
    key: 'points',
    title: '累计积分发放',
    value: 0,
    display: 0,
    unit: ' 分',
    meta: '来源：仪表板聚合',
    icon: Coin,
    iconBg: 'linear-gradient(135deg, #5b8cff 0%, #3b6de0 100%)'
  }
])

const moduleHealth = ref([])
const recentOps = ref([])

const formatNumber = (value) => {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    return '0'
  }
  if (value >= 10000) {
    return `${(value / 10000).toFixed(1)}w`
  }
  return value.toLocaleString('zh-CN')
}

const animateKpi = (targets) => {
  const duration = 1200
  const start = performance.now()

  const tick = (now) => {
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

const setKpiValue = (key, value) => {
  const card = kpiCards.value.find((item) => item.key === key)
  if (!card) return
  card.value = Number(value) || 0
}

const fetchDashboardData = async () => {
  kpiLoading.value = true

  try {
    const overview = await getDashboardOverview()

    setKpiValue('login', overview?.todayLoginCount || 0)
    setKpiValue('chat', overview?.onlineUserCount || 0)
    setKpiValue('risk', overview?.todayFailedOperationCount || 0)
    setKpiValue('points', overview?.totalPointsIssued || 0)

    moduleHealth.value = Array.isArray(overview?.moduleHealthList) ? overview.moduleHealthList : []
    recentOps.value = Array.isArray(overview?.recentOperations) ? overview.recentOperations : []

    const userTotal = Number(overview?.totalUsers) || 0
    const loginCard = kpiCards.value.find((item) => item.key === 'login')
    if (loginCard) {
      loginCard.meta = `来源：仪表板聚合（用户总量 ${formatNumber(userTotal)}）`
    }

    const activePointUsers = Number(overview?.activePointUsers) || 0
    const pointsCard = kpiCards.value.find((item) => item.key === 'points')
    if (pointsCard) {
      pointsCard.meta = `来源：仪表板聚合（活跃积分用户 ${formatNumber(activePointUsers)}）`
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
.dashboard {
  display: grid;
  gap: 14px;
}

.page-title {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.hero-row,
.kpi-row,
.detail-row {
  width: 100%;
}

.hero-card {
  overflow: hidden;
  border: 1px solid #d7e5fa;
  background: linear-gradient(140deg, #fafdff 0%, #f2f7ff 58%, #edf4ff 100%);
}

.hero-content {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 2px;
}

.welcome-content {
  flex: 1;
  padding: 18px 0 6px;
}

.welcome-title {
  margin: 0;
  font-size: 25px;
  color: var(--cn-text-primary);
}

.welcome-subtitle {
  margin: 10px 0 0;
  color: var(--cn-text-secondary);
}

.role-line {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.role-tag {
  margin-right: 0;
}

.hero-decoration {
  position: relative;
  width: 140px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pulse-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(31, 111, 235, 0.28);
  animation: pulse 2.8s ease-out infinite;
}

.ring-1 {
  width: 76px;
  height: 76px;
}

.ring-2 {
  width: 108px;
  height: 108px;
  animation-delay: 0.9s;
}

.pulse-core {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #2b7cf6 0%, #1f6feb 100%);
  box-shadow: 0 10px 22px rgba(31, 111, 235, 0.24);
}

.pulse-core .el-icon {
  font-size: 24px;
}

.action-card {
  height: 100%;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.quick-btn {
  width: 100%;
  justify-content: flex-start;
  height: 40px;
}

.kpi-row {
  margin-top: 2px;
}

.kpi-card {
  border: 1px solid #dbe7f8;
  min-height: 150px;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-title {
  color: var(--cn-text-secondary);
  font-size: 14px;
}

.kpi-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.kpi-icon .el-icon {
  font-size: 17px;
}

.kpi-value {
  margin-top: 14px;
  font-size: 30px;
  line-height: 1.2;
  color: var(--cn-text-primary);
  font-weight: 700;
}

.kpi-meta {
  margin-top: 10px;
  font-size: 13px;
  color: var(--cn-text-tertiary);
}

.detail-row {
  margin-top: 2px;
}

.panel-card {
  height: 100%;
}

.health-list {
  display: grid;
  gap: 12px;
}

.health-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid #e5edf9;
  background: #fbfdff;
}

.health-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.health-dot.healthy {
  background: #21aa6c;
  box-shadow: 0 0 0 3px rgba(33, 170, 108, 0.16);
}

.health-dot.warning {
  background: #e49a2a;
  box-shadow: 0 0 0 3px rgba(228, 154, 42, 0.16);
}

.health-dot.danger {
  background: #d14343;
  box-shadow: 0 0 0 3px rgba(209, 67, 67, 0.16);
}

.health-name {
  font-weight: 500;
  color: var(--cn-text-primary);
}

.health-meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.health-latency {
  font-size: 13px;
  color: var(--cn-text-tertiary);
}

.timeline-list {
  display: grid;
  gap: 14px;
}

.timeline-item {
  display: flex;
  gap: 12px;
}

.timeline-time {
  min-width: 46px;
  font-size: 12px;
  color: #5873a2;
  background: #eaf2ff;
  border: 1px solid #d7e5fa;
  border-radius: 999px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 3px;
}

.timeline-content {
  flex: 1;
  padding-bottom: 12px;
  border-bottom: 1px dashed #dce8f8;
}

.timeline-item:last-child .timeline-content {
  border-bottom: none;
  padding-bottom: 0;
}

.timeline-title {
  font-weight: 600;
  color: var(--cn-text-primary);
}

.timeline-desc {
  margin-top: 6px;
  color: var(--cn-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

@keyframes pulse {
  0% {
    opacity: 0.62;
    transform: scale(0.94);
  }

  70% {
    opacity: 0;
    transform: scale(1.2);
  }

  100% {
    opacity: 0;
    transform: scale(1.2);
  }
}

@media (max-width: 992px) {
  .hero-content {
    flex-direction: column;
  }

  .hero-decoration {
    width: 100%;
    min-height: 80px;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .page-title {
    margin-bottom: 16px;
    font-size: 24px;
  }

  .welcome-title {
    font-size: 21px;
  }

  .quick-actions {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pulse-ring {
    animation: none !important;
  }
}
</style> 
