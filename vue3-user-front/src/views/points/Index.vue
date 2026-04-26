<template>
  <div class="points-container">
    <!-- 积分余额卡片 -->
    <div class="balance-card">
      <div class="balance-header">
        <h2>我的积分</h2>
        <div class="balance-amount">
          {{ pointsBalance?.totalPoints || 0 }}
        </div>
        <p class="balance-tips">积分可用于兑换各种礼品和服务</p>
      </div>
      
      <!-- 打卡区域 -->
      <div class="checkin-section">
        <div class="checkin-info">
          <div class="checkin-status">
            <span class="continuous-days">连续打卡 {{ pointsBalance?.continuousDays || 0 }} 天</span>
            <span class="next-points">明日打卡可得 {{ pointsBalance?.nextDayPoints || 0 }} 积分</span>
          </div>
          
          <button 
            class="checkin-btn" 
            :class="{ 'checked': pointsBalance?.hasCheckedToday }"
            :disabled="pointsBalance?.hasCheckedToday || checkinLoading"
            @click="handleCheckin"
          >
            <span v-if="checkinLoading" class="loading-icon">⏳</span>
            <span v-else-if="pointsBalance?.hasCheckedToday" class="check-icon">✅</span>
            <span v-else class="checkin-icon">📅</span>
            {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '今日打卡' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 功能导航 -->
    <div class="function-nav">
      <div class="nav-item" @click="showDetailDialog = true">
        <div class="nav-icon">📋</div>
        <div class="nav-text">积分明细</div>
      </div>
      
      <div class="nav-item" @click="showCalendarDialog = true">
        <div class="nav-icon">📅</div>
        <div class="nav-text">打卡日历</div>
      </div>
      
      <div class="nav-item" @click="showStatisticsDialog = true">
        <div class="nav-icon">📊</div>
        <div class="nav-text">打卡统计</div>
      </div>
    </div>

    <!-- 最近积分明细 -->
    <div class="recent-details">
      <div class="section-header">
        <h3>最近积分明细</h3>
        <span class="view-all" @click="showDetailDialog = true">查看全部</span>
      </div>
      
      <div class="detail-list">
        <div v-if="recentDetails.length === 0" class="empty-state">
          暂无积分明细记录
        </div>
        
        <div 
          v-for="detail in recentDetails" 
          :key="detail.id"
          class="detail-item"
        >
          <div class="detail-left">
            <div class="detail-type">{{ getPointsTypeText(detail.pointsType) }}</div>
            <div class="detail-desc">{{ detail.description }}</div>
            <div class="detail-time">{{ formatTime(detail.createTime) }}</div>
          </div>
          <div class="detail-right">
            <div class="points-change" :class="{ 'positive': detail.pointsChange > 0 }">
              {{ detail.pointsChange > 0 ? '+' : '' }}{{ detail.pointsChange }}
            </div>
            <div class="balance-after">余额: {{ detail.balanceAfter }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 积分明细弹窗 -->
    <PointsDetailDialog 
      v-model="showDetailDialog"
    />

    <!-- 打卡日历弹窗 -->
    <CheckinCalendarDialog 
      v-model="showCalendarDialog"
    />

    <!-- 打卡统计弹窗 -->
    <CheckinStatisticsDialog 
      v-model="showStatisticsDialog"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import pointsApi from '@/api/points'
import PointsDetailDialog from './components/PointsDetailDialog.vue'
import CheckinCalendarDialog from './components/CheckinCalendarDialog.vue'
import CheckinStatisticsDialog from './components/CheckinStatisticsDialog.vue'

// 响应式数据
const pointsBalance = ref(null)
const recentDetails = ref([])
const checkinLoading = ref(false)

// 弹窗控制
const showDetailDialog = ref(false)
const showCalendarDialog = ref(false)
const showStatisticsDialog = ref(false)

// 页面初始化
onMounted(() => {
  loadPointsBalance()
  loadRecentDetails()
})

// 加载积分余额
const loadPointsBalance = async () => {
  try {
    const response = await pointsApi.getPointsBalance()
    pointsBalance.value = response
  } catch (error) {
    console.error('加载积分余额失败:', error)
    ElMessage.error('加载积分信息失败')
  }
}

// 加载最近积分明细
const loadRecentDetails = async () => {
  try {
    const response = await pointsApi.getPointsDetailList({
      pageNum: 1,
      pageSize: 5
    })
    recentDetails.value = response.records || []
  } catch (error) {
    console.error('加载积分明细失败:', error)
  }
}

// 处理打卡
const handleCheckin = async () => {
  if (pointsBalance.value?.hasCheckedToday) {
    ElMessage.warning('今日已打卡，请勿重复操作')
    return
  }

  checkinLoading.value = true
  
  try {
    const response = await pointsApi.checkin()
    
    ElMessage.success({
      message: `打卡成功！获得 ${response.pointsEarned} 积分`,
      duration: 3000
    })
    
    // 刷新积分余额和明细
    await loadPointsBalance()
    await loadRecentDetails()
    
  } catch (error) {
    console.error('打卡失败:', error)
    ElMessage.error(error.message || '打卡失败，请重试')
  } finally {
    checkinLoading.value = false
  }
}

// 获取积分类型文本
const getPointsTypeText = (type) => {
  const typeMap = {
    'CHECK_IN': '每日打卡',
    'ADMIN_GRANT': '管理员发放'
  }
  return typeMap[type] || type
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  const now = new Date()
  const diffTime = now - date
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) {
    return date.toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  } else if (diffDays === 1) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    })
  } else {
    return date.toLocaleDateString('zh-CN') + ' ' + 
           date.toLocaleTimeString('zh-CN', { 
             hour: '2-digit', 
             minute: '2-digit' 
           })
  }
}
</script>

<style lang="scss" scoped>
.points-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
  background: #f5f7fa;
  min-height: 100vh;
  
  @media (max-width: 768px) {
    max-width: 100%;
    padding: 20px;
  }
}

.balance-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 40px;
  color: white;
  margin-bottom: 32px;
  box-shadow: 0 12px 32px rgba(102, 126, 234, 0.3);
  
  @media (max-width: 768px) {
    padding: 24px;
    margin-bottom: 20px;
  }
}

.balance-header {
  text-align: center;
  margin-bottom: 20px;
  
  h2 {
    font-size: 18px;
    font-weight: 500;
    margin: 0 0 8px 0;
    opacity: 0.9;
  }
  
  .balance-amount {
    font-size: 36px;
    font-weight: bold;
    margin: 8px 0;
    text-shadow: 0 2px 4px rgba(0,0,0,0.1);
  }
  
  .balance-tips {
    font-size: 14px;
    opacity: 0.8;
    margin: 0;
  }
}

.checkin-section {
  border-top: 1px solid rgba(255,255,255,0.2);
  padding-top: 20px;
}

.checkin-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.checkin-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  span {
    font-size: 14px;
    opacity: 0.9;
  }
}

.checkin-btn {
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 12px;
  color: white;
  padding: 12px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  
  &:hover:not(:disabled) {
    background: rgba(255,255,255,0.3);
    transform: translateY(-1px);
  }
  
  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
  
  &.checked {
    background: rgba(76, 175, 80, 0.8);
    border-color: rgba(76, 175, 80, 0.9);
  }
}

.function-nav {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.nav-item {
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  }
}

.nav-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.nav-text {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.recent-details {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  h3 {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
    color: #333;
  }
  
  .view-all {
    color: #667eea;
    font-size: 14px;
    cursor: pointer;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.detail-list {
  .empty-state {
    text-align: center;
    color: #999;
    padding: 40px 0;
    font-size: 14px;
  }
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  
  &:last-child {
    border-bottom: none;
  }
}

.detail-left {
  flex: 1;
  
  .detail-type {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }
  
  .detail-desc {
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
  }
  
  .detail-time {
    font-size: 12px;
    color: #999;
  }
}

.detail-right {
  text-align: right;
  
  .points-change {
    font-size: 16px;
    font-weight: 600;
    color: #f56c6c;
    margin-bottom: 4px;
    
    &.positive {
      color: #67c23a;
    }
  }
  
  .balance-after {
    font-size: 12px;
    color: #999;
  }
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
