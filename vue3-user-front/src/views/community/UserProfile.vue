<template>
  <div class="user-profile-container">
    <!-- 顶部封面区域 -->
    <div class="profile-cover">
      <div class="cover-overlay"></div>
      <div class="cover-content">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-nav">
          <span class="back-link" @click="goBack">
            <el-icon><Back /></el-icon>
            社区首页
          </span>
          <span class="breadcrumb-sep">/</span>
          <span class="current-page">用户主页</span>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="profile-main">
      <!-- 用户信息卡片 -->
      <div class="user-card" v-if="userProfile">
        <div class="user-avatar-wrapper">
          <div class="user-avatar">
            {{ userProfile.username?.charAt(0)?.toUpperCase() }}
          </div>
          <div class="user-status online"></div>
        </div>
        
        <div class="user-info">
          <h1 class="user-name">{{ userProfile.username }}</h1>
          <p class="user-bio">{{ userProfile.bio || '这个人很懒，什么都没写~' }}</p>
          
          <!-- 统计数据 -->
          <div class="user-stats">
            <div class="stat-item">
              <span class="stat-value">{{ userProfile.postCount || 0 }}</span>
              <span class="stat-label">帖子</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ userProfile.commentCount || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ userProfile.likeCount || 0 }}</span>
              <span class="stat-label">获赞</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ userProfile.collectCount || 0 }}</span>
              <span class="stat-label">收藏</span>
            </div>
          </div>
        </div>

        <div class="user-actions">
          <button class="follow-btn primary">
            <el-icon><Plus /></el-icon>
            关注
          </button>
          <button class="follow-btn secondary">
            <el-icon><ChatDotRound /></el-icon>
            私信
          </button>
        </div>
      </div>

      <!-- 内容布局 -->
      <div class="content-layout">
        <!-- 左侧边栏 -->
        <aside class="left-sidebar">
          <!-- 活跃标签 -->
          <div class="sidebar-card" v-if="userProfile?.activeTags?.length > 0">
            <h3 class="card-title">
              <el-icon><PriceTag /></el-icon>
              活跃标签
            </h3>
            <div class="tags-cloud">
              <span 
                v-for="tag in userProfile.activeTags" 
                :key="tag.id"
                class="tag-item"
              >
                # {{ tag.name }}
                <span class="tag-count">{{ tag.count || 0 }}</span>
              </span>
            </div>
          </div>

          <!-- 个人成就 -->
          <div class="sidebar-card">
            <h3 class="card-title">
              <el-icon><Trophy /></el-icon>
              个人成就
            </h3>
            <div class="achievements">
              <div class="achievement-item">
                <div class="achievement-icon">🎯</div>
                <div class="achievement-info">
                  <span class="achievement-name">社区新星</span>
                  <span class="achievement-desc">发布首篇帖子</span>
                </div>
              </div>
              <div class="achievement-item">
                <div class="achievement-icon">💬</div>
                <div class="achievement-info">
                  <span class="achievement-name">热心评论</span>
                  <span class="achievement-desc">评论数达到10</span>
                </div>
              </div>
              <div class="achievement-item locked">
                <div class="achievement-icon">🔥</div>
                <div class="achievement-info">
                  <span class="achievement-name">人气作者</span>
                  <span class="achievement-desc">获得100个赞</span>
                </div>
              </div>
            </div>
          </div>
        </aside>

        <!-- 主内容区 -->
        <main class="main-content">
          <!-- 帖子列表头部 -->
          <div class="posts-header">
            <h2 class="posts-title">
              <el-icon><Document /></el-icon>
              TA的帖子
              <span class="posts-count" v-if="postsTotal > 0">{{ postsTotal }}</span>
            </h2>
            <div class="posts-sort">
              <span class="sort-item active">最新</span>
              <span class="sort-item">最热</span>
            </div>
          </div>

          <!-- 帖子列表 -->
          <div v-loading="postsLoading" class="posts-list">
            <div 
              v-for="post in postsList" 
              :key="post.id"
              class="post-card"
              @click="goToPostDetail(post)"
            >
              <div class="post-main">
                <h3 class="post-title">{{ post.title }}</h3>
                
                <!-- AI摘要 -->
                <div v-if="post.aiSummary" class="ai-summary">
                  <span class="ai-badge">🤖 AI</span>
                  <span class="summary-text">{{ post.aiSummary }}</span>
                </div>
                
                <p class="post-excerpt">{{ post.content }}</p>
                
                <!-- 帖子标签 -->
                <div v-if="post.tags && post.tags.length > 0" class="post-tags">
                  <span 
                    v-for="tag in post.tags" 
                    :key="tag.id"
                    class="post-tag"
                  >
                    # {{ tag.name }}
                  </span>
                </div>
                
                <div class="post-meta">
                  <span class="meta-time">{{ formatDate(post.createTime) }}</span>
                  <div class="meta-stats">
                    <span class="stat">
                      <el-icon><View /></el-icon>
                      {{ post.viewCount || 0 }}
                    </span>
                    <span class="stat">
                      <el-icon><Pointer /></el-icon>
                      {{ post.likeCount || 0 }}
                    </span>
                    <span class="stat">
                      <el-icon><ChatDotRound /></el-icon>
                      {{ post.commentCount || 0 }}
                    </span>
                    <span class="stat">
                      <el-icon><Star /></el-icon>
                      {{ post.collectCount || 0 }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="!postsLoading && postsList.length === 0" class="empty-posts">
              <div class="empty-icon">📝</div>
              <p class="empty-text">暂无帖子</p>
              <p class="empty-hint">TA还没有发布任何内容</p>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="postsTotal > 0">
            <el-pagination 
              v-model:current-page="postsQueryParams.pageNum" 
              v-model:page-size="postsQueryParams.pageSize"
              :page-sizes="[10, 20, 30]"
              :total="postsTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="handlePostsSizeChange"
              @current-change="handlePostsCurrentChange"
            />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Back, Document, View, Star, ChatDotRound, PriceTag, Trophy, Plus, Pointer
} from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'

const route = useRoute()
const router = useRouter()

// 响应式数据
const userProfile = ref(null)
const postsList = ref([])
const postsTotal = ref(0)
const postsLoading = ref(false)

// 帖子查询参数
const postsQueryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return Math.floor(diff / 60000) + '分钟前'
  }
  // 小于1天
  if (diff < 86400000) {
    return Math.floor(diff / 3600000) + '小时前'
  }
  // 小于7天
  if (diff < 604800000) {
    return Math.floor(diff / 86400000) + '天前'
  }
  
  return date.toLocaleDateString('zh-CN')
}

// 获取用户信息
const fetchUserProfile = async () => {
  try {
    const userId = route.params.userId
    userProfile.value = await communityApi.getUserProfile(userId)
  } catch (error) {
    ElMessage.error('获取用户信息失败')
    goBack()
  }
}

// 获取用户帖子列表
const fetchUserPosts = async () => {
  postsLoading.value = true
  try {
    const userId = route.params.userId
    const response = await communityApi.getUserPostList(userId, postsQueryParams)
    postsList.value = response.records || []
    postsTotal.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取帖子列表失败')
  } finally {
    postsLoading.value = false
  }
}

// 跳转到帖子详情
const goToPostDetail = (post) => {
  router.push(`/community/posts/${post.id}`)
}

// 分页大小改变
const handlePostsSizeChange = (size) => {
  postsQueryParams.pageSize = size
  postsQueryParams.pageNum = 1
  fetchUserPosts()
}

// 当前页改变
const handlePostsCurrentChange = (page) => {
  postsQueryParams.pageNum = page
  fetchUserPosts()
}

// 返回社区
const goBack = () => {
  router.push('/community')
}

// 初始化
onMounted(async () => {
  await fetchUserProfile()
  await fetchUserPosts()
})
</script>

<style scoped>
/* ========== 全局容器 ========== */
.user-profile-container {
  min-height: 100vh;
  background: #f4f5f5;
}

/* ========== 顶部封面 ========== */
.profile-cover {
  position: relative;
  height: 200px;
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  overflow: hidden;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
}

.cover-content {
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  z-index: 1;
}

/* 面包屑导航 */
.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: white;
  cursor: pointer;
  transition: opacity 0.3s;
}

.back-link:hover {
  opacity: 0.8;
}

.breadcrumb-sep {
  color: rgba(255, 255, 255, 0.5);
}

.current-page {
  color: rgba(255, 255, 255, 0.9);
}

/* ========== 主内容区 ========== */
.profile-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 40px;
  margin-top: -80px;
  position: relative;
  z-index: 2;
}

/* ========== 用户卡片 ========== */
.user-card {
  display: flex;
  align-items: center;
  gap: 24px;
  background: white;
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.user-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 40px;
  font-weight: 700;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 184, 148, 0.3);
}

.user-status {
  position: absolute;
  bottom: 6px;
  right: 6px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 3px solid white;
}

.user-status.online {
  background: #10b981;
}

.user-info {
  flex: 1;
}

.user-name {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
}

.user-bio {
  margin: 0 0 16px 0;
  color: #666;
  font-size: 15px;
  line-height: 1.6;
}

.user-stats {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-stats .stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 20px;
}

.user-stats .stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
}

.user-stats .stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e5e5e5;
}

.user-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.follow-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.follow-btn.primary {
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  color: white;
}

.follow-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 184, 148, 0.4);
}

.follow-btn.secondary {
  background: #f5f5f5;
  color: #333;
}

.follow-btn.secondary:hover {
  background: #ebebeb;
}

/* ========== 内容布局 ========== */
.content-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
}

/* ========== 左侧边栏 ========== */
.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

/* 标签云 */
.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #e8f8f5;
  border-radius: 6px;
  font-size: 13px;
  color: #00b894;
  cursor: pointer;
  transition: all 0.3s;
}

.tag-item:hover {
  background: #00b894;
  color: white;
}

.tag-item:hover .tag-count {
  background: rgba(255, 255, 255, 0.2);
  color: white;
}

.tag-count {
  padding: 2px 6px;
  background: #e8e8e8;
  border-radius: 4px;
  font-size: 11px;
  color: #999;
}

/* 成就 */
.achievements {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: all 0.3s;
}

.achievement-item:hover {
  background: #e8f8f5;
}

.achievement-item.locked {
  opacity: 0.5;
}

.achievement-item.locked .achievement-icon {
  filter: grayscale(1);
}

.achievement-icon {
  font-size: 24px;
}

.achievement-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.achievement-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.achievement-desc {
  font-size: 12px;
  color: #999;
}

/* ========== 主内容 ========== */
.main-content {
  min-width: 0;
}

.posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.posts-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.posts-count {
  padding: 2px 10px;
  background: #00b894;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
  color: white;
}

.posts-sort {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 8px;
}

.sort-item {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.sort-item.active {
  background: white;
  color: #00b894;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.sort-item:hover:not(.active) {
  color: #333;
}

/* 帖子列表 */
.posts-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.post-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
  transition: color 0.3s;
}

.post-card:hover .post-title {
  color: #00b894;
}

/* AI摘要 */
.ai-summary {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #e8f8f5 0%, #d1f2eb 100%);
  border-radius: 8px;
}

.ai-badge {
  padding: 2px 8px;
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  color: white;
  flex-shrink: 0;
}

.summary-text {
  flex: 1;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
}

.post-excerpt {
  margin: 0 0 12px 0;
  font-size: 14px;
  line-height: 1.7;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.post-tag {
  padding: 4px 10px;
  background: #e8f8f5;
  border-radius: 4px;
  font-size: 12px;
  color: #00b894;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.meta-time {
  font-size: 13px;
  color: #999;
}

.meta-stats {
  display: flex;
  gap: 16px;
}

.meta-stats .stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
}

/* 空状态 */
.empty-posts {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.empty-hint {
  margin: 0;
  font-size: 14px;
  color: #999;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* ========== 响应式 ========== */
@media (max-width: 992px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
  
  .left-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .profile-cover {
    height: 160px;
  }
  
  .profile-main {
    padding: 0 16px 24px;
    margin-top: -60px;
  }
  
  .user-card {
    flex-direction: column;
    text-align: center;
    padding: 24px;
  }
  
  .user-info {
    width: 100%;
  }
  
  .user-stats {
    justify-content: center;
  }
  
  .user-actions {
    width: 100%;
    justify-content: center;
  }
  
  .posts-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .post-card {
    padding: 16px;
  }
  
  .post-meta {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>

