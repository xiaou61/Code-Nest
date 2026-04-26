<template>
  <div class="rank-board">
    <!-- 排行类型切换 -->
    <div class="rank-tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.key" 
        class="rank-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        {{ tab.label }}
      </div>
    </div>
    
    <!-- 我的排名 -->
    <div class="my-rank" v-if="myRank">
      <div class="my-rank-label">我的排名</div>
      <div class="my-rank-info">
        <span class="rank-num">#{{ myRank.rank || '-' }}</span>
        <span class="rank-value">{{ getRankValue(myRank) }}</span>
      </div>
    </div>
    
    <!-- 排行榜列表 -->
    <div class="rank-list" v-loading="loading">
      <div v-if="rankList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无排行数据" :image-size="80" />
      </div>
      
      <div 
        v-for="(item, index) in rankList" 
        :key="item.userId || index" 
        class="rank-item"
        :class="{ 'top-three': index < 3 }"
      >
        <div class="rank-position">
          <span v-if="index === 0" class="medal gold">🥇</span>
          <span v-else-if="index === 1" class="medal silver">🥈</span>
          <span v-else-if="index === 2" class="medal bronze">🥉</span>
          <span v-else class="rank-number">{{ index + 1 }}</span>
        </div>
        
        <div class="rank-avatar">
          <img v-if="item.userAvatar" :src="item.userAvatar" />
          <span v-else>{{ item.userName?.charAt(0) || '用' }}</span>
        </div>
        
        <div class="rank-info">
          <div class="rank-name">{{ item.userName }}</div>
        <div class="rank-badge" v-if="item.memberRole === 1">
            <el-icon><Trophy /></el-icon>
          </div>
        </div>
        
        <div class="rank-value">
          <span class="value">{{ getRankValue(item) }}</span>
          <span class="label">{{ getRankLabel() }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Check, Timer, Odometer, TrendCharts, Trophy } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  teamId: { type: [String, Number], required: true }
})

const tabs = [
  { key: 'checkin', label: '打卡榜', icon: Check },
  { key: 'streak', label: '连续榜', icon: Odometer },
  { key: 'duration', label: '时长榜', icon: Timer },
  { key: 'contribution', label: '贡献榜', icon: TrendCharts }
]

const activeTab = ref('checkin')
const rankList = ref([])
const myRank = ref(null)
const loading = ref(false)

onMounted(() => {
  loadRankData()
})

watch(activeTab, () => {
  loadRankData()
})

watch(() => props.teamId, () => {
  loadRankData()
})

const loadRankData = async () => {
  loading.value = true
  try {
    let response
    switch (activeTab.value) {
      case 'checkin':
        response = await teamApi.getCheckinRank(props.teamId)
        break
      case 'streak':
        response = await teamApi.getStreakRank(props.teamId)
        break
      case 'duration':
        response = await teamApi.getDurationRank(props.teamId)
        break
      case 'contribution':
        response = await teamApi.getContributionRank(props.teamId)
        break
    }
    rankList.value = response || []
    
    // 获取我的排名
    try {
      const myRankData = await teamApi.getMyRank(props.teamId, activeTab.value)
      myRank.value = myRankData
    } catch {
      myRank.value = null
    }
  } catch (error) {
    console.error('加载排行榜失败:', error)
    rankList.value = []
  } finally {
    loading.value = false
  }
}

const getRankValue = (item) => {
  switch (activeTab.value) {
    case 'checkin':
      return item.checkinCount || item.count || 0
    case 'streak':
      return item.streakDays || item.count || 0
    case 'duration':
      return formatDuration(item.totalDuration || item.count || 0)
    case 'contribution':
      return item.contribution || item.count || 0
    default:
      return item.count || 0
  }
}

const getRankLabel = () => {
  switch (activeTab.value) {
    case 'checkin':
      return '次'
    case 'streak':
      return '天'
    case 'duration':
      return ''
    case 'contribution':
      return '分'
    default:
      return ''
  }
}

const formatDuration = (minutes) => {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h${mins}m` : `${hours}小时`
}
</script>

<style lang="scss" scoped>
.rank-board {
  min-height: 300px;
}

.rank-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  
  .rank-tab {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      background: #f5f7fa;
    }
    
    &.active {
      background: #e8f4fd;
      color: #409eff;
    }
  }
}

.my-rank {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 20px;
  color: white;
  
  .my-rank-label {
    font-size: 14px;
    opacity: 0.9;
  }
  
  .my-rank-info {
    display: flex;
    align-items: baseline;
    gap: 8px;
    
    .rank-num {
      font-size: 24px;
      font-weight: bold;
    }
    
    .rank-value {
      font-size: 14px;
      opacity: 0.9;
    }
  }
}

.rank-list {
  .empty-state {
    padding: 40px 0;
  }
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: background 0.2s;
  
  &:hover {
    background: #f8f9fc;
  }
  
  &.top-three {
    background: #fafafa;
    
    .rank-name {
      font-weight: 600;
    }
  }
  
  & + .rank-item {
    border-top: 1px solid #f5f5f5;
  }
}

.rank-position {
  width: 32px;
  text-align: center;
  
  .medal {
    font-size: 20px;
  }
  
  .rank-number {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    background: #f0f0f0;
    border-radius: 50%;
    font-size: 12px;
    color: #666;
    font-weight: 500;
  }
}

.rank-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  flex-shrink: 0;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  span {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: white;
    font-size: 16px;
    font-weight: 500;
  }
}

.rank-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  
  .rank-name {
    font-size: 14px;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .rank-badge {
    color: #e6a23c;
    font-size: 14px;
  }
}

.rank-value {
  text-align: right;
  flex-shrink: 0;
  
  .value {
    font-size: 18px;
    font-weight: bold;
    color: #409eff;
  }
  
  .label {
    font-size: 12px;
    color: #999;
    margin-left: 2px;
  }
}
</style>
