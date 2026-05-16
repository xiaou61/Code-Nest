<template>
  <div class="my-favorites-page">
    <!-- 页面头部 -->
    <div class="page-banner">
      <div class="banner-content">
        <button class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <div class="banner-info">
          <h1 class="banner-title">⭐ 我的收藏</h1>
          <p class="banner-desc">收藏的精彩动态都在这里</p>
        </div>
      </div>
    </div>

    <div v-loading="loading" class="favorites-list">
      <!-- 空状态 -->
      <div v-if="!loading && favoriteList.length === 0" class="empty-state">
        <div class="empty-icon">📑</div>
        <p class="empty-text">暂无收藏</p>
        <p class="empty-hint">收藏感兴趣的动态，方便随时查看</p>
      </div>
      
      <div v-for="moment in favoriteList" :key="moment.id" class="moment-card">
        <!-- 用户信息 -->
        <div class="moment-header">
          <div class="user-avatar">
            {{ moment.userNickname?.charAt(0) }}
          </div>
          <div class="user-info">
            <span class="user-name">{{ moment.userNickname }}</span>
            <span class="post-time">{{ formatTime(moment.createTime) }}</span>
          </div>
        </div>

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

        <!-- 互动区域 -->
        <div class="moment-footer">
          <div v-if="moment.likeCount > 0 || moment.commentCount > 0 || moment.viewCount > 0" class="stats-bar">
            <span v-if="moment.likeCount > 0" class="stat">
              <el-icon><Pointer /></el-icon> {{ moment.likeCount }} 赞
            </span>
            <span v-if="moment.commentCount > 0" class="stat">
              {{ moment.commentCount }} 评论
            </span>
            <span v-if="moment.viewCount > 0" class="stat">
              {{ moment.viewCount }} 浏览
            </span>
          </div>
          
          <div class="action-bar">
            <button 
              class="action-btn"
              :class="{ active: moment.isLiked }"
              @click="toggleLikeMoment(moment)"
              :disabled="moment.liking"
            >
              <el-icon><Pointer /></el-icon>
              <span>{{ moment.isLiked ? '已赞' : '点赞' }}</span>
            </button>
            <button class="action-btn" @click="showCommentDialog(moment)">
              <el-icon><ChatDotRound /></el-icon>
              <span>评论</span>
            </button>
            <button 
              class="action-btn unfav-btn"
              @click="toggleFavoriteMoment(moment)"
              :disabled="moment.favoriting"
            >
              <el-icon><StarFilled /></el-icon>
              <span>取消收藏</span>
            </button>
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

    <!-- 评论对话框 -->
    <CommentDialog
      v-model="commentDialogVisible"
      :moment="currentMoment"
      @commented="handleCommented"
    />

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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import { ChatDotRound, StarFilled, ArrowLeft, Pointer } from '@element-plus/icons-vue'
import { getMyFavorites, toggleLike, toggleFavorite } from '@/api/moment'
import { formatRelativeTime } from '@/utils/timeUtil'
import CommentDialog from './components/CommentDialog.vue'

const router = useRouter()

// 数据状态
const loading = ref(false)
const loadingMore = ref(false)
const favoriteList = ref([])
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

// 评论相关
const commentDialogVisible = ref(false)
const currentMoment = ref(null)

// 图片预览
const imageViewerVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

// 加载收藏列表
const loadFavoriteList = async (page = 1) => {
  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getMyFavorites(params)
    const newMoments = result.records.map(moment => ({
      ...moment,
      images: moment.images || [],
      liking: false,
      favoriting: false
    }))

    if (page === 1) {
      favoriteList.value = newMoments
    } else {
      favoriteList.value.push(...newMoments)
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
  loadFavoriteList(currentPage.value)
}

// 点赞切换
const toggleLikeMoment = async (moment) => {
  moment.liking = true
  try {
    const isLiked = await toggleLike(moment.id)
    moment.isLiked = isLiked
    moment.likeCount += isLiked ? 1 : -1
  } catch (error) {
    ElMessage.error('操作失败：' + error.message)
  } finally {
    moment.liking = false
  }
}

// 取消收藏
const toggleFavoriteMoment = async (moment) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    moment.favoriting = true
    await toggleFavorite(moment.id)
    
    // 从列表中移除
    const index = favoriteList.value.findIndex(item => item.id === moment.id)
    if (index > -1) {
      favoriteList.value.splice(index, 1)
    }
    
    ElMessage.success('取消收藏成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败：' + error.message)
    }
  } finally {
    moment.favoriting = false
  }
}

// 显示评论对话框
const showCommentDialog = (moment) => {
  currentMoment.value = moment
  commentDialogVisible.value = true
}

// 处理评论成功
const handleCommented = (_comment) => {
  const moment = currentMoment.value
  if (moment) {
    moment.commentCount++
  }
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

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadFavoriteList()
})
</script>

<style scoped>
.my-favorites-page {
  min-height: 100vh;
  background: transparent;
  max-width: 800px;
  margin: 0 auto;
  padding: 0 14px 24px;
}

/* 页面头部 */
.page-banner {
  position: relative;
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 40%, #fff5f5 100%);
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 16px;
  margin-top: 16px;
  overflow: hidden;
}

.page-banner::before {
  content: '';
  position: absolute;
  top: -20px;
  right: -30px;
  width: 140px;
  height: 140px;
  background: radial-gradient(circle, rgba(31, 111, 235, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.banner-content {
  position: relative;
  z-index: 1;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid #d7e4f8;
  border-radius: 8px;
  color: #6a82ae;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 14px;
}

.back-btn:hover {
  border-color: #6c63ff;
  color: #6c63ff;
  background: #f5f3ff;
}

.banner-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #2d2b55 0%, #6c63ff 50%, #3d7cf7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.banner-desc {
  margin: 0;
  font-size: 14px;
  color: #6a82ae;
}

.favorites-list {
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
  font-size: 52px;
  margin-bottom: 16px;
}

.empty-text {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--cn-text-primary, #1a2233);
}

.empty-hint {
  margin: 0;
  font-size: 14px;
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
  background: linear-gradient(180deg, #f59e0b 0%, #f472b6 100%);
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

/* 用户头部 */
.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c63ff 0%, #f472b6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 15px;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(108, 99, 255, 0.22);
}

.user-info {
  flex: 1;
}

.user-name {
  display: block;
  font-weight: 600;
  color: var(--cn-text-primary, #1a2233);
  font-size: 15px;
}

.post-time {
  display: block;
  color: #8ea0bd;
  font-size: 12px;
  margin-top: 2px;
}

/* 内容 */
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

/* 底部互动 */
.moment-footer {
  border-top: 1px solid #e8eef8;
  padding-top: 12px;
}

.stats-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  font-size: 13px;
  color: #8ea0bd;
}

.stats-bar .stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-bar {
  display: flex;
  justify-content: space-around;
  padding: 6px 0;
  border-top: 1px solid #e8eef8;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: none;
  border: none;
  color: #6a82ae;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.25s ease;
  font-weight: 500;
}

.action-btn:hover {
  background: #f5f3ff;
  color: #6c63ff;
  transform: translateY(-1px);
}

.action-btn.active {
  color: #f43f5e;
  font-weight: 600;
}

.action-btn.unfav-btn {
  color: #e0923b;
}

.action-btn.unfav-btn:hover {
  background: #fff8f0;
  color: #c2792e;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 加载更多 */
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

