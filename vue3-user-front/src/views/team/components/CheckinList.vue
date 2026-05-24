<template>
  <div class="checkin-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>
    
    <div v-else-if="checkins.length === 0" class="empty-state">
      <el-empty description="暂无打卡记录" :image-size="80" />
    </div>
    
    <div v-else class="checkins">
      <div 
        v-for="checkin in checkins" 
        :key="checkin.id" 
        class="checkin-item"
      >
        <div class="checkin-avatar">
          <img v-if="checkin.userAvatar" :src="checkin.userAvatar" />
          <span v-else>{{ checkin.userName?.charAt(0) || '用' }}</span>
        </div>
        
        <div class="checkin-body">
          <div class="checkin-header">
            <span class="user-name">{{ checkin.userName }}</span>
            <span class="checkin-time">{{ formatTime(checkin.createTime) }}</span>
            <span v-if="checkin.isSupplement" class="supplement-tag">补卡</span>
          </div>
          
          <div class="checkin-task" v-if="checkin.taskName">
            <el-icon><Calendar /></el-icon>
            {{ checkin.taskName }}
          </div>
          
          <div class="checkin-content">{{ checkin.checkinContent }}</div>
          
          <div class="checkin-images" v-if="checkin.images">
            <img 
              v-for="(img, index) in parseImages(checkin.images)" 
              :key="index" 
              :src="img"
              @click="previewImage(img)"
            />
          </div>
          
          <div class="checkin-meta">
            <span v-if="checkin.duration" class="meta-item">
              <el-icon><Timer /></el-icon>
              {{ checkin.duration }}分钟
            </span>
            <span class="meta-item like" :class="{ liked: checkin.isLiked }" @click="handleLike(checkin)">
              <el-icon><Star /></el-icon>
              {{ checkin.likeCount || 0 }}
            </span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">
        加载更多
      </el-button>
    </div>
    
    <!-- 图片预览 -->
    <el-image-viewer 
      v-if="previewVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Calendar, Timer, Star } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  teamId: { type: [String, Number], required: true },
  limit: { type: Number, default: 0 }
})

const checkins = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

// 图片预览
const previewVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

onMounted(() => {
  if (props.limit > 0) {
    pageSize.value = props.limit
  }
  loadCheckins()
})

watch(() => props.teamId, () => {
  pageNum.value = 1
  loadCheckins()
})

const loadCheckins = async () => {
  loading.value = true
  try {
    const response = await teamApi.getCheckinList(props.teamId, { 
      pageNum: pageNum.value, 
      pageSize: pageSize.value 
    })
    checkins.value = response || []
    hasMore.value = props.limit === 0 && checkins.value.length === pageSize.value
  } catch (error) {
    console.error('加载打卡列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  loadingMore.value = true
  pageNum.value++
  try {
    const response = await teamApi.getCheckinList(props.teamId, { 
      pageNum: pageNum.value, 
      pageSize: pageSize.value 
    })
    const newCheckins = response || []
    checkins.value = [...checkins.value, ...newCheckins]
    hasMore.value = newCheckins.length === pageSize.value
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

const handleLike = async (checkin) => {
  try {
    if (checkin.isLiked) {
      await teamApi.unlikeCheckin(checkin.id)
      checkin.isLiked = false
      checkin.likeCount = Math.max(0, (checkin.likeCount || 1) - 1)
    } else {
      await teamApi.likeCheckin(checkin.id)
      checkin.isLiked = true
      checkin.likeCount = (checkin.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const parseImages = (images) => {
  if (!images) return []
  return images.split(',').filter(img => img.trim())
}

const previewImage = (img) => {
  previewImages.value = [img]
  previewIndex.value = 0
  previewVisible.value = true
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

defineExpose({ loadCheckins })
</script>

<style lang="scss" scoped>
.checkin-list {
  .loading-state, .empty-state {
    padding: 20px 0;
  }
}

.checkin-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  
  & + .checkin-item {
    border-top: 1px solid #f0f0f0;
  }
}

.checkin-avatar {
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

.checkin-body {
  flex: 1;
  min-width: 0;
}

.checkin-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  
  .user-name {
    font-size: 14px;
    font-weight: 500;
    color: #333;
  }
  
  .checkin-time {
    font-size: 12px;
    color: #999;
  }
  
  .supplement-tag {
    padding: 1px 6px;
    background: #fdf2e9;
    color: #e6a23c;
    font-size: 11px;
    border-radius: 4px;
  }
}

.checkin-task {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #409eff;
  margin-bottom: 8px;
  
  .el-icon {
    font-size: 14px;
  }
}

.checkin-content {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  word-break: break-all;
}

.checkin-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  
  img {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    object-fit: cover;
    cursor: pointer;
    transition: transform 0.2s;
    
    &:hover {
      transform: scale(1.05);
    }
  }
}

.checkin-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #999;
    
    &.like {
      cursor: pointer;
      
      &:hover, &.liked {
        color: #f56c6c;
      }
    }
  }
}

.load-more {
  text-align: center;
  padding: 12px 0;
}
</style>
