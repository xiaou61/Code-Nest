<template>
  <div class="user-profile-page">
    <!-- 用户信息卡片 -->
    <div class="user-info-card" v-loading="loadingUserInfo">
      <div class="card-cover"></div>
      <div class="user-header">
        <div class="avatar-wrapper">
          <div class="user-avatar-large">
            {{ userInfo.nickname?.charAt(0) }}
          </div>
        </div>
        <div class="user-details">
          <h2 class="user-name">{{ userInfo.nickname }}</h2>
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.totalMoments || 0 }}</span>
              <span class="stat-label">动态</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.totalLikes || 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ userInfo.totalComments || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 动态列表 -->
    <div class="moments-section">
      <div class="section-title">
        <span>Ta的动态</span>
      </div>

      <div v-loading="loading" class="moments-list">
        <div v-if="!loading && momentList.length === 0" class="empty-state">
          <div class="empty-icon">📝</div>
          <p class="empty-text">暂无动态</p>
        </div>
        
        <div v-for="moment in momentList" :key="moment.id" class="moment-card">
          <!-- 动态内容 -->
          <div class="moment-body">
            <p class="moment-text">{{ moment.content }}</p>
            
            <!-- 图片展示 -->
            <div v-if="moment.images && moment.images.length" class="images-grid">
              <div 
                v-for="(image, index) in moment.images" 
                :key="index" 
                class="image-item"
                @click="previewImage(moment.images, index)"
              >
                <img :src="image" alt="" loading="lazy" />
              </div>
            </div>
          </div>

          <!-- 互动信息 -->
          <div class="interaction-footer">
            <span class="time-text">{{ formatTime(moment.createTime) }}</span>
            <div class="interaction-stats">
              <span v-if="moment.likeCount > 0" class="stat-badge">
                <el-icon><Star /></el-icon>
                {{ moment.likeCount }}
              </span>
              <span v-if="moment.commentCount > 0" class="stat-badge">
                <el-icon><ChatDotRound /></el-icon>
                {{ moment.commentCount }}
              </span>
              <span v-if="moment.viewCount > 0" class="stat-badge">
                <el-icon><View /></el-icon>
                {{ moment.viewCount }}
              </span>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-image-viewer
      v-if="imageViewerVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="closeImageViewer"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElImageViewer } from 'element-plus'
import { Star, ChatDotRound, View } from '@element-plus/icons-vue'
import { getUserMomentList, getUserMomentInfo } from '@/api/moment'
import { formatRelativeTime } from '@/utils/timeUtil'

const route = useRoute()

// 用户ID
const userId = ref(null)

// 用户信息
const userInfo = ref({})
const loadingUserInfo = ref(false)

// 动态列表
const loading = ref(false)
const loadingMore = ref(false)
const momentList = ref([])
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

// 图片预览
const imageViewerVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

// 加载用户信息
const loadUserInfo = async () => {
  loadingUserInfo.value = true
  try {
    const result = await getUserMomentInfo({ userId: userId.value })
    userInfo.value = result
  } catch (error) {
    ElMessage.error('加载用户信息失败：' + error.message)
  } finally {
    loadingUserInfo.value = false
  }
}

// 加载动态列表
const loadMomentList = async (page = 1) => {
  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      userId: userId.value,
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getUserMomentList(params)
    const newMoments = result.records || []

    if (page === 1) {
      momentList.value = newMoments
    } else {
      momentList.value.push(...newMoments)
    }

    hasMore.value = newMoments.length === pageSize.value
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多
const loadMore = () => {
  currentPage.value++
  loadMomentList(currentPage.value)
}

// 图片预览
const previewImage = (images, index) => {
  previewImages.value = images
  previewIndex.value = index
  imageViewerVisible.value = true
}

const closeImageViewer = () => {
  imageViewerVisible.value = false
}

// 相对时间格式化
const formatTime = (time) => {
  return formatRelativeTime(time)
}

onMounted(() => {
  // 从路由获取用户ID
  userId.value = Number(route.params.userId || route.query.userId)
  
  if (!userId.value) {
    ElMessage.error('用户ID不存在')
    return
  }
  
  loadUserInfo()
  loadMomentList()
})
</script>

<style scoped>
.user-profile-page {
  min-height: 100vh;
  background: transparent;
  max-width: 800px;
  margin: 0 auto;
  padding: 16px 14px 24px;
}

/* 用户信息卡片 */
.user-info-card {
  position: relative;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
  margin-bottom: 20px;
}

.card-cover {
  height: 100px;
  background: linear-gradient(135deg, #6c63ff 0%, #a78bfa 50%, #f472b6 100%);
  position: relative;
}

.card-cover::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: linear-gradient(to top, rgba(255,255,255,0.6), transparent);
}

.user-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 0 24px 24px;
  margin-top: -36px;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.user-avatar-large {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c63ff 0%, #f472b6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 28px;
  border: 4px solid #fff;
  box-shadow: 0 4px 16px rgba(108, 99, 255, 0.3);
}

.user-details {
  flex: 1;
  padding-top: 20px;
}

.user-name {
  margin: 0 0 12px 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--cn-text-primary, #1a2233);
}

.user-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 800;
  background: linear-gradient(135deg, #6c63ff 0%, #f43f5e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 12px;
  color: #6a82ae;
  margin-top: 4px;
  font-weight: 500;
}

/* 动态列表 */
.moments-section {
  margin-top: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--cn-text-primary, #1a2233);
  padding: 14px 0;
  border-bottom: 2px solid #6c63ff;
  margin-bottom: 16px;
}

.moments-list {
  min-height: 400px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #8ea0bd;
}

/* 动态卡片 */
.moment-card {
  position: relative;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 14px;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
  transition: all 0.3s ease;
  overflow: hidden;
}

.moment-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, #6c63ff 0%, #f472b6 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.moment-card:hover {
  border-color: #c2d4f2;
  box-shadow: 0 14px 32px rgba(18, 38, 63, 0.09);
  transform: translateY(-2px);
}

.moment-card:hover::before {
  opacity: 1;
}

.moment-body {
  margin-bottom: 14px;
}

.moment-text {
  margin: 0 0 12px 0;
  line-height: 1.75;
  color: var(--cn-text-primary, #1a2233);
  font-size: 15px;
  white-space: pre-wrap;
}

.images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-radius: 12px;
  overflow: hidden;
}

.image-item {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  width: 120px;
  height: 120px;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.image-item:hover img {
  transform: scale(1.08);
}

.interaction-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #e8eef8;
}

.time-text {
  font-size: 12px;
  color: #8ea0bd;
}

.interaction-stats {
  display: flex;
  gap: 14px;
}

.stat-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6a82ae;
  padding: 4px 10px;
  background: #f6f9ff;
  border-radius: 999px;
  border: 1px solid #e8f0fa;
  transition: all 0.2s;
}

.stat-badge:hover {
  border-color: #c8d9f5;
  background: #edf3ff;
}

.load-more {
  text-align: center;
  padding: 24px;
}

.load-more-btn {
  padding: 10px 32px;
  background: #fff;
  border: 1px solid #d7e4f8;
  border-radius: 10px;
  color: #6a82ae;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
}

.load-more-btn:hover {
  border-color: #6c63ff;
  color: #6c63ff;
  background: #f5f3ff;
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

