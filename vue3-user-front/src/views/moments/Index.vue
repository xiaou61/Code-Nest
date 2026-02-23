<template>
  <div class="moments-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">🌟 朋友圈</h1>
        <p class="page-subtitle">分享学习与生活的精彩瞬间</p>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="moments-main">
      <div class="moments-layout">
        <!-- 左侧边栏 -->
        <aside class="left-sidebar">
          <!-- 搜索框 -->
          <div class="sidebar-card search-card">
            <div class="search-wrapper">
              <el-icon class="search-icon"><Search /></el-icon>
              <input
                v-model="searchKeyword"
                type="text"
                class="search-input"
                placeholder="搜索动态..."
                @keyup.enter="handleSearch"
              />
              <button v-if="searchKeyword" class="clear-btn" @click="handleClearSearch">×</button>
            </div>
          </div>

          <!-- 热门动态 -->
          <div v-if="!isSearchMode && hotMoments.length > 0" class="sidebar-card hot-card">
            <h3 class="card-title">
              <span class="hot-icon">🔥</span>
              热门动态
            </h3>
            <div class="hot-list">
              <div v-for="(hot, index) in hotMoments" :key="hot.id" class="hot-item">
                <span class="hot-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
                <div class="hot-info">
                  <p class="hot-text">{{ hot.content }}</p>
                  <div class="hot-meta">
                    <span class="hot-user">{{ hot.userNickname }}</span>
                    <span class="hot-stats">
                      <el-icon><Pointer /></el-icon> {{ hot.likeCount }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 快捷发布 -->
          <div class="sidebar-card publish-quick">
            <h3 class="card-title">
              <span>✍️</span>
              快速发布
            </h3>
            <button class="publish-btn" @click="showPublishDialog">
              <el-icon><Edit /></el-icon>
              发布新动态
            </button>
          </div>
        </aside>

        <!-- 中间内容区 -->
        <main class="main-content">
          <!-- 发布卡片 -->
          <div class="publish-card" @click="showPublishDialog">
            <div class="publish-avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) || 'U' }}
            </div>
            <div class="publish-input-fake">
              分享你的想法...
            </div>
            <div class="publish-actions">
              <span class="action-item">
                <el-icon><Picture /></el-icon>
                图片
              </span>
              <span class="action-item">
                <el-icon><Location /></el-icon>
                位置
              </span>
            </div>
          </div>

          <!-- 动态列表 -->
          <div v-loading="loading" class="moments-feed">
            <div v-if="!loading && momentList.length === 0" class="empty-state">
              <div class="empty-icon">📝</div>
              <p class="empty-text">暂无动态</p>
              <p class="empty-hint">快来发布第一条动态吧！</p>
              <button class="publish-first-btn" @click="showPublishDialog">发布动态</button>
            </div>

            <!-- 动态卡片 -->
            <div v-for="moment in momentList" :key="moment.id" class="moment-card">
              <!-- 用户头部 -->
              <div class="moment-header">
                <div class="user-avatar" @click="goToUserProfile(moment.userId)">
                  {{ moment.userNickname?.charAt(0) }}
                </div>
                <div class="user-info">
                  <span class="user-name" @click="goToUserProfile(moment.userId)">{{ moment.userNickname }}</span>
                  <span class="post-time">{{ formatTime(moment.createTime) }}</span>
                </div>
                <div v-if="moment.canDelete" class="more-actions">
                  <el-dropdown trigger="click">
                    <button class="more-btn">
                      <el-icon><MoreFilled /></el-icon>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="deleteMoment(moment)">
                          <el-icon><Delete /></el-icon>
                          删除动态
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>

              <!-- 动态内容 -->
              <div class="moment-body">
                <p class="moment-text">{{ moment.content }}</p>
                
                <!-- 图片展示 -->
                <div v-if="moment.images && moment.images.length" 
                     class="moment-images"
                     :class="'images-' + Math.min(moment.images.length, 9)">
                  <div 
                    v-for="(image, index) in moment.images.slice(0, 9)" 
                    :key="index" 
                    class="image-item"
                    @click="previewImage(moment.images, index)"
                  >
                    <img :src="image" alt="" loading="lazy" />
                    <div v-if="index === 8 && moment.images.length > 9" class="more-images">
                      +{{ moment.images.length - 9 }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 互动区 -->
              <div class="moment-footer">
                <!-- 统计数据 -->
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

                <!-- 操作按钮 -->
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
                    class="action-btn"
                    :class="{ active: moment.isFavorited, favorited: moment.isFavorited }"
                    @click="toggleFavoriteMoment(moment)"
                    :disabled="moment.favoriting"
                  >
                    <el-icon><Star /></el-icon>
                    <span>{{ moment.isFavorited ? '已收藏' : '收藏' }}</span>
                  </button>
                </div>

                <!-- 评论区 -->
                <div v-if="moment.recentComments && moment.recentComments.length" class="comments-area">
                  <div v-for="comment in moment.recentComments" :key="comment.id" class="comment-item">
                    <span class="comment-author">{{ comment.userNickname }}</span>
                    <span class="comment-text">{{ comment.content }}</span>
                    <button 
                      v-if="comment.canDelete" 
                      class="delete-comment"
                      @click="deleteComment(comment, moment)"
                    >
                      删除
                    </button>
                  </div>
                  <button 
                    v-if="moment.commentCount > moment.recentComments.length"
                    class="view-all-btn"
                    @click="viewAllComments(moment)"
                  >
                    查看全部 {{ moment.commentCount }} 条评论
                  </button>
                </div>
              </div>
            </div>

            <!-- 加载更多 -->
            <div v-if="hasMore" class="load-more" ref="loadMoreRef">
              <div v-if="loadingMore" class="loading-spinner">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>加载中...</span>
              </div>
              <div v-else class="scroll-hint">下滑加载更多</div>
            </div>
            <div v-else-if="momentList.length > 0" class="no-more-data">
              —— 已经到底了 ——
            </div>
          </div>
        </main>

        <!-- 右侧边栏 -->
        <aside class="right-sidebar">
          <!-- 统计卡片 -->
          <div class="sidebar-card stats-card">
            <h3 class="card-title">
              <span>📊</span>
              动态统计
            </h3>
            <div class="stats-grid">
              <div class="stat-item">
                <span class="stat-value">{{ momentList.length }}</span>
                <span class="stat-label">动态</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ totalLikes }}</span>
                <span class="stat-label">获赞</span>
              </div>
            </div>
          </div>

          <!-- 功能入口 -->
          <div class="sidebar-card quick-links">
            <h3 class="card-title">
              <span>📢</span>
              快捷入口
            </h3>
            <div class="links-list">
              <div class="link-item" @click="$router.push('/community')">
                <el-icon><ChatLineSquare /></el-icon>
                <span>社区讨论</span>
              </div>
              <div class="link-item" @click="$router.push('/resources')">
                <el-icon><Folder /></el-icon>
                <span>资源中心</span>
              </div>
              <div class="link-item" @click="$router.push('/practice')">
                <el-icon><Edit /></el-icon>
                <span>在线练习</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>

    <!-- 发布动态对话框 -->
    <PublishDialog
      v-model="publishDialogVisible"
      @published="handlePublished"
    />

    <!-- 评论对话框 -->
    <CommentDialog
      v-model="commentDialogVisible"
      :moment="currentMoment"
      @commented="handleCommented"
    />

    <!-- 查看全部评论对话框 -->
    <AllCommentsDialog
      v-model="allCommentsDialogVisible"
      :moment="currentMoment"
      @comment-deleted="handleCommentDeleted"
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
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import { 
  Star, ChatDotRound, MoreFilled, StarFilled, View, Search, Loading, 
  Edit, Picture, Location, Pointer, Delete, ChatLineSquare, Folder
} from '@element-plus/icons-vue'
import { 
  getMomentList, 
  toggleLike, 
  deleteMoment as deleteMomentApi, 
  deleteComment as deleteCommentApi,
  toggleFavorite,
  getHotMoments,
  searchMoments 
} from '@/api/moment'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { formatRelativeTime } from '@/utils/timeUtil'
import PublishDialog from './components/PublishDialog.vue'
import CommentDialog from './components/CommentDialog.vue'
import AllCommentsDialog from './components/AllCommentsDialog.vue'

const userStore = useUserStore()
const router = useRouter()

// 计算属性
const totalLikes = computed(() => {
  return momentList.value.reduce((sum, m) => sum + (m.likeCount || 0), 0)
})

// 数据状态
const loading = ref(false)
const loadingMore = ref(false)
const momentList = ref([])
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(10)

// 热门动态
const hotMoments = ref([])
const loadingHot = ref(false)

// 搜索相关
const searchKeyword = ref('')
const isSearchMode = ref(false)
const searching = ref(false)

// 发布相关
const publishForm = ref({
  content: ''
})
const publishDialogVisible = ref(false)

// 评论相关
const commentDialogVisible = ref(false)
const allCommentsDialogVisible = ref(false)
const currentMoment = ref(null)

// 图片预览
const imageViewerVisible = ref(false)
const previewImages = ref([])
const previewIndex = ref(0)

// 无限滚动
const loadMoreRef = ref(null)
let observer = null

// 加载热门动态
const loadHotMoments = async () => {
  loadingHot.value = true
  try {
    const result = await getHotMoments({ limit: 3 })
    hotMoments.value = result || []
  } catch (error) {
    console.error('加载热门动态失败', error)
  } finally {
    loadingHot.value = false
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
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getMomentList(params)
    const newMoments = result.records.map(moment => ({
      ...moment,
      images: moment.images || [],
      liking: false,
      favoriting: false
    }))

    if (page === 1) {
      momentList.value = newMoments
    } else {
      momentList.value.push(...newMoments)
    }

    hasMore.value = result.records.length === pageSize.value
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
  if (isSearchMode.value) {
    // 搜索模式下加载更多
    loadMoreSearch()
  } else {
    loadMomentList(currentPage.value)
  }
}

// 搜索模式加载更多
const loadMoreSearch = async () => {
  loadingMore.value = true
  try {
    const params = {
      keyword: searchKeyword.value.trim(),
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }
    
    const result = await searchMoments(params)
    const newMoments = result.records.map(moment => ({
      ...moment,
      images: moment.images || [],
      liking: false,
      favoriting: false
    }))
    
    momentList.value.push(...newMoments)
    hasMore.value = newMoments.length === pageSize.value
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loadingMore.value = false
  }
}

// 显示发布对话框
const showPublishDialog = () => {
  publishDialogVisible.value = true
}

// 处理发布成功
const handlePublished = () => {
  publishDialogVisible.value = false
  currentPage.value = 1
  loadMomentList(1)
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

// 删除动态
const deleteMoment = async (moment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMomentApi(moment.id)
    ElMessage.success('删除成功')
    
    // 从列表中移除
    const index = momentList.value.findIndex(item => item.id === moment.id)
    if (index > -1) {
      momentList.value.splice(index, 1)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

// 显示评论对话框
const showCommentDialog = (moment) => {
  currentMoment.value = moment
  commentDialogVisible.value = true
}

// 处理评论成功
const handleCommented = (comment) => {
  const moment = currentMoment.value
  if (moment) {
    moment.commentCount++
    if (!moment.recentComments) {
      moment.recentComments = []
    }
    moment.recentComments.unshift(comment)
    // 保持最新评论只显示3条
    if (moment.recentComments.length > 3) {
      moment.recentComments = moment.recentComments.slice(0, 3)
    }
  }
}

// 删除评论
const deleteComment = async (comment, moment) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteCommentApi(comment.id)
    ElMessage.success('删除成功')
    
    // 从最新评论中移除
    const commentIndex = moment.recentComments.findIndex(item => item.id === comment.id)
    if (commentIndex > -1) {
      moment.recentComments.splice(commentIndex, 1)
      moment.commentCount--
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

// 查看全部评论
const viewAllComments = (moment) => {
  currentMoment.value = moment
  allCommentsDialogVisible.value = true
}

// 处理评论删除（从全部评论对话框触发）
const handleCommentDeleted = (commentId) => {
  const moment = currentMoment.value
  if (moment) {
    moment.commentCount--
    // 如果删除的评论在最新评论中，也要移除
    const index = moment.recentComments.findIndex(item => item.id === commentId)
    if (index > -1) {
      moment.recentComments.splice(index, 1)
    }
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

// 收藏切换
const toggleFavoriteMoment = async (moment) => {
  moment.favoriting = true
  try {
    const isFavorited = await toggleFavorite(moment.id)
    moment.isFavorited = isFavorited
    moment.favoriteCount += isFavorited ? 1 : -1
    ElMessage.success(isFavorited ? '收藏成功' : '取消收藏')
  } catch (error) {
    ElMessage.error('操作失败：' + error.message)
  } finally {
    moment.favoriting = false
  }
}

// 搜索动态
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  
  searching.value = true
  isSearchMode.value = true
  currentPage.value = 1
  
  try {
    const params = {
      keyword: searchKeyword.value.trim(),
      pageNum: 1,
      pageSize: pageSize.value
    }
    
    const result = await searchMoments(params)
    const newMoments = result.records.map(moment => ({
      ...moment,
      images: moment.images || [],
      liking: false,
      favoriting: false
    }))
    
    momentList.value = newMoments
    hasMore.value = newMoments.length === pageSize.value
  } catch (error) {
    ElMessage.error('搜索失败：' + error.message)
  } finally {
    searching.value = false
  }
}

// 清空搜索
const handleClearSearch = () => {
  isSearchMode.value = false
  searchKeyword.value = ''
  currentPage.value = 1
  loadMomentList(1)
}

// 跳转到用户主页
const goToUserProfile = (userId) => {
  router.push(`/moments/user/${userId}`)
}

// 相对时间格式化
const formatTime = (time) => {
  return formatRelativeTime(time)
}

// 初始化无限滚动
const initInfiniteScroll = () => {
  if (!loadMoreRef.value) return
  
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      // 当"加载更多"元素进入视口且有更多数据且不在加载中
      if (entry.isIntersecting && hasMore.value && !loadingMore.value) {
        loadMore()
      }
    })
  }, {
    rootMargin: '100px' // 提前100px触发加载
  })
  
  observer.observe(loadMoreRef.value)
}

onMounted(() => {
  loadHotMoments()
  loadMomentList()
  
  // 延迟初始化无限滚动，等待DOM渲染
  nextTick(() => {
    setTimeout(() => {
      initInfiniteScroll()
    }, 1000)
  })
})

onUnmounted(() => {
  // 清理observer
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>
/* ========== 全局容器 ========== */
.moments-container {
  min-height: 100vh;
  background: transparent;
}

/* ========== 页面头部 ========== */
.page-header {
  position: relative;
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 40%, #fff5f5 100%);
  border-bottom: 1px solid #e8e0f0;
  overflow: hidden;
}

.page-header::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -20px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(255, 107, 129, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.page-header::after {
  content: '';
  position: absolute;
  bottom: -50px;
  left: 8%;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(108, 99, 255, 0.06) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 20px;
  display: flex;
  align-items: baseline;
  gap: 16px;
  position: relative;
  z-index: 1;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
  background: linear-gradient(135deg, #2d2b55 0%, #6c63ff 50%, #3d7cf7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.5px;
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: #6a82ae;
  letter-spacing: 0.3px;
}

/* ========== 主内容区 ========== */
.moments-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 14px 24px;
}

.moments-layout {
  display: grid;
  grid-template-columns: 240px 1fr 240px;
  gap: 16px;
}

/* ========== 侧边栏通用 ========== */
.sidebar-card {
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
  margin-bottom: 14px;
  transition: box-shadow 0.3s ease;
}

.sidebar-card:hover {
  box-shadow: 0 12px 28px rgba(18, 38, 63, 0.08);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8eef8;
  font-size: 15px;
  font-weight: 600;
  color: var(--cn-text-primary, #1a2233);
}

/* ========== 左侧边栏 ========== */
.left-sidebar {
  position: sticky;
  top: 24px;
  height: fit-content;
}

/* 搜索框 */
.search-wrapper {
  display: flex;
  align-items: center;
  padding: 7px 7px 7px 14px;
  border-radius: 10px;
  border: 1px solid #d7e4f8;
  background: #f8fbff;
  transition: all 0.25s ease;
}

.search-wrapper:focus-within {
  border-color: #6c63ff;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(108, 99, 255, 0.1);
}

.search-icon {
  color: #6a82ae;
  margin-right: 8px;
  font-size: 16px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
  color: var(--cn-text-primary, #1a2233);
}

.search-input::placeholder {
  color: #8ea0bd;
}

.clear-btn {
  background: none;
  border: none;
  color: #8ea0bd;
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.clear-btn:hover {
  background: #f5f3ff;
  color: #6c63ff;
}

/* 热门动态 */
.hot-card .card-title .hot-icon {
  font-size: 16px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hot-item {
  display: flex;
  gap: 10px;
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 10px;
  transition: all 0.25s ease;
}

.hot-item:hover {
  background: #f1f7ff;
  transform: translateX(2px);
}

.hot-rank {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: white;
  background: #c4d4e9;
  flex-shrink: 0;
  margin-top: 2px;
}

.hot-rank.rank-1 { background: linear-gradient(135deg, #ff6b6b, #ee5a5a); box-shadow: 0 3px 8px rgba(238, 90, 90, 0.3); }
.hot-rank.rank-2 { background: linear-gradient(135deg, #ffa502, #f39c12); box-shadow: 0 3px 8px rgba(243, 156, 18, 0.3); }
.hot-rank.rank-3 { background: linear-gradient(135deg, #2f7dff, #1f6feb); box-shadow: 0 3px 8px rgba(31, 111, 235, 0.3); }

.hot-info {
  flex: 1;
  min-width: 0;
}

.hot-text {
  margin: 0 0 4px 0;
  font-size: 13px;
  color: var(--cn-text-primary, #1a2233);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #8ea0bd;
}

.hot-stats {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* 快捷发布 */
.publish-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.4);
}

.publish-btn:active {
  transform: translateY(0);
}

/* ========== 主内容区 ========== */
.main-content {
  min-width: 0;
}

/* 发布卡片 */
.publish-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 16px 18px;
  margin-bottom: 14px;
  cursor: pointer;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
  transition: all 0.3s ease;
}

.publish-card:hover {
  border-color: #c2d4f2;
  box-shadow: 0 14px 28px rgba(18, 38, 63, 0.08);
  transform: translateY(-1px);
}

.publish-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c63ff 0%, #f472b6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 18px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(108, 99, 255, 0.25);
}

.publish-input-fake {
  flex: 1;
  padding: 10px 16px;
  background: #f4f8ff;
  border: 1px solid #e3edfa;
  border-radius: 20px;
  color: #8ea0bd;
  font-size: 14px;
  transition: all 0.2s;
}

.publish-card:hover .publish-input-fake {
  border-color: #c8d9f5;
  background: #eef4ff;
}

.publish-actions {
  display: flex;
  gap: 16px;
}

.publish-actions .action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6a82ae;
  transition: color 0.2s;
}

.publish-actions .action-item:hover {
  color: #6c63ff;
}

/* 动态Feed */
.moments-feed {
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
  filter: grayscale(0.2);
}

.empty-text {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--cn-text-primary, #1a2233);
}

.empty-hint {
  margin: 0 0 24px 0;
  font-size: 14px;
  color: #8ea0bd;
}

.publish-first-btn {
  padding: 10px 28px;
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
  border-radius: 10px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.3);
  transition: all 0.3s;
}

.publish-first-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(79, 70, 229, 0.4);
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

/* 动态头部 */
.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6c63ff 0%, #f472b6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(108, 99, 255, 0.22);
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.08);
  box-shadow: 0 5px 16px rgba(108, 99, 255, 0.35);
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--cn-text-primary, #1a2233);
  cursor: pointer;
  transition: color 0.25s;
}

.user-name:hover {
  color: #6c63ff;
}

.post-time {
  display: block;
  font-size: 12px;
  color: #8ea0bd;
  margin-top: 2px;
}

.more-btn {
  background: none;
  border: none;
  color: #8ea0bd;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.more-btn:hover {
  background: #f5f3ff;
  color: #6c63ff;
}

/* 动态内容 */
.moment-body {
  margin-bottom: 14px;
}

.moment-text {
  margin: 0 0 12px 0;
  font-size: 15px;
  line-height: 1.75;
  color: var(--cn-text-primary, #1a2233);
  white-space: pre-wrap;
}

/* 图片展示 */
.moment-images {
  display: grid;
  gap: 6px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(18, 38, 63, 0.06);
}

.moment-images.images-1 {
  max-width: 320px;
}

.moment-images.images-2 {
  grid-template-columns: repeat(2, 1fr);
  max-width: 420px;
}

.moment-images.images-3 {
  grid-template-columns: repeat(3, 1fr);
  max-width: 420px;
}

.moment-images.images-4 {
  grid-template-columns: repeat(2, 1fr);
  max-width: 320px;
}

.moment-images.images-5,
.moment-images.images-6 {
  grid-template-columns: repeat(3, 1fr);
  max-width: 420px;
}

.moment-images.images-7,
.moment-images.images-8,
.moment-images.images-9 {
  grid-template-columns: repeat(3, 1fr);
  max-width: 420px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  cursor: pointer;
  overflow: hidden;
  border-radius: 4px;
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

.more-images {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
}

/* 动态底部 */
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
  border-bottom: 1px solid #e8eef8;
  margin-bottom: 12px;
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

.action-btn.favorited {
  color: #f59e0b;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 评论区 */
.comments-area {
  background: #faf9ff;
  border-radius: 10px;
  padding: 12px 12px 12px 16px;
  border-left: 3px solid #d4ccff;
  position: relative;
}

.comment-item {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.6;
  padding: 4px 0;
}

.comment-item:last-of-type {
  margin-bottom: 0;
}

.comment-author {
  color: #6c63ff;
  font-weight: 600;
  margin-right: 6px;
  cursor: pointer;
}

.comment-author:hover {
  text-decoration: underline;
}

.comment-text {
  color: var(--cn-text-primary, #1a2233);
}

.delete-comment {
  background: none;
  border: none;
  color: #8ea0bd;
  font-size: 12px;
  cursor: pointer;
  margin-left: 8px;
  transition: color 0.2s;
}

.delete-comment:hover {
  color: #e5534b;
}

.view-all-btn {
  background: none;
  border: none;
  color: #6a82ae;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 0;
  margin-top: 8px;
  transition: color 0.2s;
}

.view-all-btn:hover {
  color: #6c63ff;
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 30px;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #6c63ff;
  font-size: 14px;
}

.scroll-hint {
  color: #b8c9e0;
  font-size: 13px;
}

.no-more-data {
  text-align: center;
  padding: 30px;
  color: #b8c9e0;
  font-size: 13px;
}

/* ========== 右侧边栏 ========== */
.right-sidebar {
  position: sticky;
  top: 24px;
  height: fit-content;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stats-card .stat-item {
  text-align: center;
  padding: 14px 12px;
  background: linear-gradient(135deg, #faf8ff 0%, #fff5f7 100%);
  border-radius: 10px;
  border: 1px solid #ede8fa;
  transition: all 0.25s;
}

.stats-card .stat-item:hover {
  border-color: #d4ccff;
  transform: translateY(-1px);
}

.stats-card .stat-value {
  display: block;
  font-size: 26px;
  font-weight: 800;
  background: linear-gradient(135deg, #6c63ff 0%, #f43f5e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-card .stat-label {
  font-size: 12px;
  color: #6a82ae;
  margin-top: 2px;
}

/* 快捷入口 */
.links-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f6f9ff;
  border: 1px solid #e8f0fa;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--cn-text-secondary, #5a6d8a);
  transition: all 0.25s ease;
  font-weight: 500;
}

.link-item:hover {
  background: #f5f3ff;
  border-color: #d4ccff;
  color: #6c63ff;
  transform: translateX(3px);
}

/* ========== 响应式 ========== */
@media (max-width: 1100px) {
  .moments-layout {
    grid-template-columns: 1fr;
  }
  
  .left-sidebar,
  .right-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 4px;
    padding: 16px;
  }
  
  .page-title {
    font-size: 22px;
  }
  
  .moments-main {
    padding: 12px;
  }
  
  .publish-card {
    padding: 12px;
  }
  
  .publish-actions {
    display: none;
  }
  
  .moment-card {
    padding: 16px;
    border-radius: 12px;
  }
  
  .moment-images {
    max-width: 100% !important;
  }
  
  .action-bar {
    padding: 4px 0;
  }
  
  .action-btn {
    padding: 6px 10px;
    font-size: 13px;
  }
}
</style>
