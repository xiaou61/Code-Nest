<template>
  <div class="community-container">
    <!-- 主体内容区域 - 三栏布局 -->
    <div class="community-main">
      <!-- 左侧边栏 -->
      <aside class="sidebar sidebar-left">
        <!-- 快捷操作 -->
        <div class="sidebar-card quick-actions">
          <button class="action-btn primary" @click="showCreateDialog">
            <el-icon><Edit /></el-icon>
            <span>发表帖子</span>
          </button>
          <button class="action-btn" @click="goToMyPosts">
            <el-icon><Document /></el-icon>
            <span>我的帖子</span>
          </button>
          <button class="action-btn" @click="goToMyCollection">
            <el-icon><Star /></el-icon>
            <span>我的收藏</span>
          </button>
        </div>

        <!-- 分类导航 -->
        <div class="sidebar-card categories-nav">
          <div class="card-header">
            <el-icon><Menu /></el-icon>
            <span>分类导航</span>
          </div>
          <ul class="nav-list">
            <li 
              class="nav-item" 
              :class="{ active: selectedCategoryId === null }"
              @click="selectCategory(null)"
            >
              <span class="nav-icon">全</span>
              <span class="nav-text">全部帖子</span>
              <span class="nav-count">{{ total }}</span>
            </li>
            <li 
              v-for="category in categoryList" 
              :key="category.id"
              class="nav-item"
              :class="{ active: selectedCategoryId === category.id }"
              @click="selectCategory(category.id)"
            >
              <span class="nav-icon">{{ getCategoryIcon(category.name) }}</span>
              <span class="nav-text">{{ category.name }}</span>
              <span class="nav-count">{{ category.postCount || 0 }}</span>
            </li>
          </ul>
        </div>

        <!-- 热门标签 -->
        <div class="sidebar-card hot-tags-card" v-if="tagList.length > 0">
          <div class="card-header">
            <el-icon><PriceTag /></el-icon>
            <span>热门标签</span>
          </div>
          <div class="tags-cloud">
            <span 
              v-for="tag in tagList.slice(0, 15)" 
              :key="tag.id"
              class="cloud-tag"
              :class="{ active: selectedTagId === tag.id }"
              @click="selectTag(tag.id)"
            >
              # {{ tag.name }}
            </span>
          </div>
        </div>
      </aside>

      <!-- 中间内容区 -->
      <main class="main-content">
        <!-- 搜索栏 + 排序选项 -->
        <div class="content-header-card">
          <!-- 搜索框 -->
          <div class="search-bar">
            <div class="search-wrapper">
              <el-icon class="search-icon"><Search /></el-icon>
              <input 
                v-model="searchKeyword" 
                type="text" 
                class="search-input" 
                placeholder="搜索帖子、标签、作者..."
                @keyup.enter="handleSearch"
                @focus="showHotKeywords = true"
              />
              <button class="search-btn" @click="handleSearch">搜索</button>
            </div>
            <!-- 热门搜索词 -->
            <div v-if="showHotKeywords && hotKeywords.length > 0" class="hot-keywords-dropdown">
              <span class="hot-label">热门搜索</span>
              <div class="hot-tags">
                <span 
                  v-for="(keyword, index) in hotKeywords" 
                  :key="index"
                  class="hot-tag"
                  @click="selectHotKeyword(keyword)"
                >
                  {{ keyword }}
                </span>
              </div>
            </div>
          </div>
          
          <!-- 排序选项 -->
          <div class="content-tabs">
            <div class="tabs-left">
              <button 
                class="tab-btn" 
                :class="{ active: queryParams.sortBy === 'time' }"
                @click="queryParams.sortBy = 'time'; handleSortChange()"
              >
                <el-icon><Clock /></el-icon>
                最新
              </button>
              <button 
                class="tab-btn" 
                :class="{ active: queryParams.sortBy === 'hot' }"
                @click="queryParams.sortBy = 'hot'; handleSortChange()"
              >
                <el-icon><TrendCharts /></el-icon>
                最热
              </button>
            </div>
            <div class="tabs-right">
              <span class="posts-count">共 {{ total }} 篇帖子</span>
            </div>
          </div>
        </div>

        <!-- 帖子列表 -->
        <div v-loading="loading" class="posts-feed">
          <article 
            v-for="post in postList" 
            :key="post.id"
            class="post-item"
            @click="goToPostDetail(post)"
          >
            <!-- 作者信息 -->
            <div class="post-author-info">
              <div class="author-avatar" @click.stop="goToUserProfile(post.authorId)">
                {{ post.authorName?.charAt(0) || '匿' }}
              </div>
              <div class="author-details">
                <span class="author-name" @click.stop="goToUserProfile(post.authorId)">
                  {{ post.authorName }}
                </span>
                <span class="post-time">{{ formatRelativeTime(post.createTime) }}</span>
              </div>
              <span v-if="post.categoryName" class="post-category">
                {{ post.categoryName }}
              </span>
            </div>

            <!-- 帖子主体 -->
            <div class="post-body">
              <h2 class="post-title">{{ post.title }}</h2>
              
              <!-- AI摘要 -->
              <div v-if="post.aiSummary" class="ai-summary">
                <span class="ai-badge">AI 摘要</span>
                <p>{{ post.aiSummary }}</p>
              </div>
              
              <p class="post-excerpt">{{ post.content }}</p>
              
              <!-- 帖子标签 -->
              <div v-if="post.tags && post.tags.length > 0" class="post-tags-inline">
                <span 
                  v-for="tag in post.tags" 
                  :key="tag.id"
                  class="inline-tag"
                  @click.stop="selectTag(tag.id)"
                >
                  # {{ tag.name }}
                </span>
              </div>
            </div>

            <!-- 帖子底部互动区 -->
            <div class="post-footer">
              <div class="post-stats-row">
                <span class="stat">
                  <el-icon><View /></el-icon>
                  {{ post.viewCount || 0 }}
                </span>
                <span class="stat like-stat" :class="{ active: post.isLiked }" @click.stop="toggleLike(post)">
                  <el-icon><Pointer /></el-icon>
                  {{ post.likeCount || 0 }}
                </span>
                <span class="stat">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ post.commentCount || 0 }}
                </span>
                <span class="stat" :class="{ active: post.isCollected }" @click.stop="toggleCollect(post)">
                  <el-icon><Star /></el-icon>
                  {{ post.collectCount || 0 }}
                </span>
              </div>
            </div>
          </article>

          <!-- 空状态 -->
          <div v-if="!loading && postList.length === 0" class="empty-state">
            <div class="empty-icon">暂无内容</div>
            <p class="empty-text">暂无帖子</p>
            <button class="empty-btn" @click="showCreateDialog">发表第一篇帖子</button>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <el-pagination 
            v-model:current-page="queryParams.pageNum" 
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            :total="total"
            layout="prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </main>

      <!-- 右侧边栏 -->
      <aside class="sidebar sidebar-right">
        <!-- 热门榜单 -->
        <div class="sidebar-card hot-ranking" v-if="hotPosts.length > 0">
          <div class="card-header">
            <el-icon><TrendCharts /></el-icon>
            <span>热门榜单</span>
          </div>
          <ul class="ranking-list">
            <li 
              v-for="(post, index) in hotPosts" 
              :key="post.id"
              class="ranking-item"
              @click="goToPostDetail(post)"
            >
              <span class="rank-number" :class="'rank-' + (index + 1)">{{ index + 1 }}</span>
              <div class="rank-content">
                <p class="rank-title">{{ post.title }}</p>
                <span class="rank-heat">热度 {{ post.hotScore || 0 }}</span>
              </div>
            </li>
          </ul>
        </div>

        <!-- 社区公告 -->
        <div class="sidebar-card community-notice">
          <div class="card-header">
            <el-icon><Bell /></el-icon>
            <span>社区公告</span>
          </div>
          <div class="notice-content">
            <p>欢迎来到 Code Nest 社区</p>
            <p>发帖前请遵守社区规范</p>
            <p>优质内容将获得推荐展示</p>
          </div>
        </div>

        <!-- 社区数据 -->
        <div class="sidebar-card community-stats">
          <div class="card-header">
            <el-icon><DataLine /></el-icon>
            <span>社区数据</span>
          </div>
          <div class="stats-grid">
            <div class="stat-box">
              <span class="stat-number">{{ total }}</span>
              <span class="stat-label">帖子总数</span>
            </div>
            <div class="stat-box">
              <span class="stat-number">{{ categoryList.length }}</span>
              <span class="stat-label">分类数</span>
            </div>
            <div class="stat-box">
              <span class="stat-number">{{ tagList.length }}</span>
              <span class="stat-label">标签数</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, Star, Edit, View, ChatDotRound, Document, TrendCharts,
  Menu, PriceTag, Clock, Bell, DataLine, Pointer
} from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'

const router = useRouter()

// 响应式数据
const searchKeyword = ref('')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const loading = ref(false)
const postList = ref([])
const categoryList = ref([])
const tagList = ref([])
const hotPosts = ref([])
const hotKeywords = ref([])
const showHotKeywords = ref(false)
const total = ref(0)

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  categoryId: null,
  tagId: null,
  keyword: null,
  sortBy: 'time'
})



// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 格式化相对时间
const formatRelativeTime = (dateStr) => {
  if (!dateStr) return ''
  const now = new Date()
  const date = new Date(dateStr)
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return formatDate(dateStr)
}

// 获取分类图标
const getCategoryIcon = (name) => {
  const icons = {
    '技术分享': '技',
    '求助问答': '问',
    '项目展示': '项',
    '学习笔记': '学',
    '职场交流': '职',
    '闲聊灌水': '聊',
    '资源分享': '资',
    '面试经验': '面'
  }
  return icons[name] || '帖'
}

// 点击外部关闭热门搜索
const handleClickOutside = (e) => {
  if (!e.target.closest('.search-bar')) {
    showHotKeywords.value = false
  }
}

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 初始化社区
const initCommunity = async () => {
  try {
    await communityApi.init()
  } catch (error) {
    console.error('社区初始化失败:', error)
  }
}

// 获取帖子列表
const fetchPostList = async () => {
  loading.value = true
  try {
    const response = await communityApi.getPostList(queryParams)
    postList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取帖子列表失败')
  } finally {
    loading.value = false
  }
}

// 选择分类
const selectCategory = (categoryId) => {
  selectedCategoryId.value = categoryId
  queryParams.categoryId = categoryId
  queryParams.pageNum = 1
  fetchPostList()
}

// 选择标签
const selectTag = (tagId) => {
  selectedTagId.value = tagId
  queryParams.tagId = tagId
  queryParams.pageNum = 1
  showHotKeywords.value = false
  fetchPostList()
}

// 搜索
const handleSearch = () => {
  queryParams.keyword = searchKeyword.value || null
  queryParams.pageNum = 1
  showHotKeywords.value = false
  fetchPostList()
}

// 排序改变
const handleSortChange = () => {
  queryParams.pageNum = 1
  fetchPostList()
}

// 选择热门搜索词
const selectHotKeyword = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

// 分页大小改变
const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchPostList()
}

// 当前页改变
const handleCurrentChange = (page) => {
  queryParams.pageNum = page
  fetchPostList()
}

// 跳转到创建帖子页面
const showCreateDialog = () => {
  router.push('/community/create')
}



// 切换点赞状态
const toggleLike = async (post) => {
  try {
    if (post.isLiked) {
      await communityApi.unlikePost(post.id)
      post.likeCount = Math.max(0, post.likeCount - 1)
      post.isLiked = false
      ElMessage.success('取消点赞成功')
    } else {
      await communityApi.likePost(post.id)
      post.likeCount = post.likeCount + 1
      post.isLiked = true
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换收藏状态
const toggleCollect = async (post) => {
  try {
    if (post.isCollected) {
      await communityApi.uncollectPost(post.id)
      post.collectCount = Math.max(0, post.collectCount - 1)
      post.isCollected = false
      ElMessage.success('取消收藏成功')
    } else {
      await communityApi.collectPost(post.id)
      post.collectCount = post.collectCount + 1
      post.isCollected = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 跳转到帖子详情
const goToPostDetail = (post) => {
  router.push(`/community/posts/${post.id}`)
}

// 跳转到我的收藏
const goToMyCollection = () => {
  router.push('/community/collections')
}

// 跳转到我的帖子
const goToMyPosts = () => {
  router.push('/community/my-posts')
}

// 跳转到用户主页
const goToUserProfile = (userId) => {
  if (userId) {
    router.push(`/community/users/${userId}`)
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const response = await communityApi.getEnabledCategories()
    categoryList.value = response || []
  } catch (error) {
    console.error('加载分类列表失败:', error)
  }
}

// 加载标签列表
const loadTags = async () => {
  try {
    const response = await communityApi.getTags()
    tagList.value = response || []
  } catch (error) {
    console.error('加载标签列表失败:', error)
  }
}

// 加载热门帖子
const loadHotPosts = async () => {
  try {
    const response = await communityApi.getHotPosts(5)
    hotPosts.value = response || []
  } catch (error) {
    console.error('加载热门帖子失败:', error)
  }
}

// 加载热门搜索词
const loadHotKeywords = async () => {
  try {
    const response = await communityApi.getHotKeywords(10)
    hotKeywords.value = response || []
  } catch (error) {
    console.error('加载热门搜索词失败:', error)
  }
}

// 初始化
onMounted(async () => {
  await loadCategories()
  await loadTags()
  await loadHotPosts()
  await loadHotKeywords()
  await initCommunity()
  await fetchPostList()
  document.addEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* ========== 容器与布局 ========== */
.community-container {
  min-height: calc(100vh - 68px);
  background: transparent;
}

.community-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 10px 14px 24px;
  display: grid;
  grid-template-columns: 248px 1fr 286px;
  gap: 16px;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-card {
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8eef8;
  color: var(--cn-text-primary);
  font-size: 15px;
  font-weight: 600;
}

/* ========== 左侧侧边栏 ========== */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #d5e2f6;
  border-radius: 10px;
  background: #f9fbff;
  color: var(--cn-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.22s ease;
}

.action-btn:hover {
  border-color: #c2d4f2;
  background: #f1f7ff;
  color: var(--cn-primary);
  transform: translateY(-1px);
}

.action-btn.primary {
  border-color: #1f6feb;
  background: linear-gradient(135deg, #2f7dff 0%, #1f6feb 100%);
  color: #fff;
  box-shadow: 0 8px 18px rgba(31, 111, 235, 0.22);
}

.action-btn.primary:hover {
  box-shadow: 0 10px 20px rgba(31, 111, 235, 0.3);
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 11px;
  margin-bottom: 4px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: #eff5ff;
}

.nav-item.active {
  background: linear-gradient(90deg, #e6f0ff 0%, #f2f7ff 100%);
  box-shadow: inset 3px 0 0 #1f6feb;
  color: #1f6feb;
}

.nav-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ecf3ff;
  color: #55709b;
  font-size: 11px;
  font-weight: 600;
  opacity: 0.92;
}

.nav-text {
  flex: 1;
  font-size: 13px;
  color: var(--cn-text-secondary);
}

.nav-item.active .nav-text {
  color: #1f6feb;
  font-weight: 500;
}

.nav-item.active .nav-icon {
  background: #1f6feb;
  color: #fff;
}

.nav-count {
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #edf3ff;
  color: #5a75a6;
  font-size: 12px;
  text-align: center;
}

.nav-item.active .nav-count {
  background: #1f6feb;
  color: #fff;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cloud-tag {
  padding: 5px 10px;
  border-radius: 999px;
  border: 1px solid #d7e4f7;
  background: #f7faff;
  color: #4b678f;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cloud-tag:hover,
.cloud-tag.active {
  border-color: #bfd4ff;
  background: #e8f1ff;
  color: #1f6feb;
}

/* ========== 主内容区 ========== */
.main-content {
  min-width: 0;
}

.content-header-card {
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 16px 18px;
  margin-bottom: 14px;
  box-shadow: 0 10px 24px rgba(18, 38, 63, 0.05);
}

.search-bar {
  position: relative;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e8eef8;
}

.search-wrapper {
  display: flex;
  align-items: center;
  padding: 7px 7px 7px 14px;
  border-radius: 10px;
  border: 1px solid #d7e4f8;
  background: #f8fbff;
  transition: all 0.2s ease;
}

.search-wrapper:focus-within {
  border-color: #1f6feb;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(31, 111, 235, 0.12);
}

.search-icon {
  margin-right: 8px;
  color: #6a82ae;
  font-size: 17px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--cn-text-primary);
  font-size: 14px;
}

.search-input::placeholder {
  color: #8ea0bd;
}

.search-btn {
  height: 34px;
  padding: 0 18px;
  border: 1px solid #1f6feb;
  border-radius: 8px;
  background: linear-gradient(135deg, #2f7dff 0%, #1f6feb 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.search-btn:hover {
  box-shadow: 0 8px 16px rgba(31, 111, 235, 0.28);
}

.hot-keywords-dropdown {
  position: absolute;
  top: 50px;
  left: 0;
  right: 0;
  z-index: 20;
  border: 1px solid #d8e4f7;
  border-radius: 10px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 16px 30px rgba(18, 38, 63, 0.14);
}

.hot-label {
  font-size: 12px;
  font-weight: 600;
  color: #cd5f3f;
}

.hot-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #d7e4f7;
  background: #f7faff;
  color: #54719a;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.hot-tag:hover {
  border-color: #bfd4ff;
  background: #e8f1ff;
  color: #1f6feb;
}

.content-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tabs-left {
  display: flex;
  gap: 8px;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 84px;
  height: 36px;
  border: 1px solid #d4e2f7;
  border-radius: 9px;
  background: #f7fbff;
  color: var(--cn-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  border-color: #c4d7f7;
  color: var(--cn-primary);
}

.tab-btn.active {
  border-color: #c0d5ff;
  background: linear-gradient(180deg, #eef4ff 0%, #e3edff 100%);
  color: #1f6feb;
  font-weight: 600;
}

.posts-count {
  color: var(--cn-text-tertiary);
  font-size: 12px;
}

.posts-feed {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-item {
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
  padding: 16px 18px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(18, 38, 63, 0.05);
  transition: all 0.22s ease;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(18, 38, 63, 0.1);
}

.post-author-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3a84ff 0%, #1f6feb 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.author-avatar:hover {
  transform: scale(1.04);
}

.author-details {
  flex: 1;
}

.author-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--cn-text-primary);
}

.author-name:hover {
  color: var(--cn-primary);
}

.post-time {
  font-size: 12px;
  color: var(--cn-text-tertiary);
}

.post-category {
  padding: 3px 10px;
  border-radius: 999px;
  background: #edf3ff;
  color: #5373a8;
  font-size: 12px;
}

.post-body {
  margin-bottom: 12px;
}

.post-title {
  margin: 0 0 9px;
  color: var(--cn-text-primary);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
}

.post-item:hover .post-title {
  color: var(--cn-primary);
}

.ai-summary {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #d8e6ff;
  background: linear-gradient(180deg, #f5f9ff 0%, #ecf3ff 100%);
}

.ai-badge {
  display: inline-block;
  margin-bottom: 6px;
  color: #1f6feb;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.ai-summary p {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.post-excerpt {
  margin: 0;
  color: var(--cn-text-secondary);
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags-inline {
  margin-top: 11px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.inline-tag {
  padding: 3px 9px;
  border-radius: 999px;
  border: 1px solid #d4e3f8;
  background: #f4f8ff;
  color: #5676a8;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.inline-tag:hover {
  border-color: #c0d5ff;
  background: #e8f1ff;
  color: #1f6feb;
}

.post-footer {
  padding-top: 12px;
  border-top: 1px solid #e8eef8;
}

.post-stats-row {
  display: flex;
  gap: 20px;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #7d8ca7;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s ease;
}

.stat:hover {
  color: #1f6feb;
}

.stat.active,
.like-stat.active {
  color: #d35353;
}

.empty-state {
  text-align: center;
  background: #fff;
  border: 1px dashed #cfddf3;
  border-radius: 14px;
  padding: 50px 20px;
}

.empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  height: 34px;
  margin-bottom: 12px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #d8e4f7;
  background: #f6faff;
  color: #5e79a6;
  font-size: 12px;
  font-weight: 500;
}

.empty-text {
  margin: 0 0 16px;
  color: var(--cn-text-secondary);
  font-size: 15px;
}

.empty-btn {
  height: 38px;
  padding: 0 22px;
  border: 1px solid #1f6feb;
  border-radius: 10px;
  background: linear-gradient(135deg, #2f7dff 0%, #1f6feb 100%);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.empty-btn:hover {
  box-shadow: 0 8px 18px rgba(31, 111, 235, 0.28);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 14px;
  background: #fff;
  border: 1px solid #dbe7f8;
  border-radius: 14px;
}

/* ========== 右侧区域 ========== */
.ranking-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.ranking-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #eef3fb;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-item:hover {
  transform: translateX(4px);
}

.rank-number {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef3fc;
  color: #6e84ac;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.rank-number.rank-1 {
  background: linear-gradient(135deg, #ff7d64 0%, #f05e4d 100%);
  color: #fff;
}

.rank-number.rank-2 {
  background: linear-gradient(135deg, #f7b74d 0%, #ea9a32 100%);
  color: #fff;
}

.rank-number.rank-3 {
  background: linear-gradient(135deg, #f5cf63 0%, #e8b84d 100%);
  color: #fff;
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-title {
  margin: 0 0 4px;
  color: var(--cn-text-primary);
  font-size: 13px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ranking-item:hover .rank-title {
  color: var(--cn-primary);
}

.rank-heat {
  color: #d35353;
  font-size: 11px;
}

.notice-content {
  color: var(--cn-text-secondary);
  font-size: 13px;
  line-height: 1.9;
}

.notice-content p {
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat-box {
  border-radius: 10px;
  border: 1px solid #dbe7f8;
  background: #f8fbff;
  padding: 10px 6px;
  text-align: center;
}

.stat-number {
  display: block;
  margin-bottom: 4px;
  color: #1f6feb;
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  color: #7c8ea8;
  font-size: 11px;
}

/* ========== 响应式 ========== */
@media (max-width: 1240px) {
  .community-main {
    grid-template-columns: 220px 1fr 250px;
  }
}

@media (max-width: 1020px) {
  .community-main {
    grid-template-columns: 1fr 250px;
    padding: 8px 10px 20px;
  }

  .sidebar-left {
    display: none;
  }
}

@media (max-width: 768px) {
  .community-main {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 6px 2px 14px;
  }

  .sidebar-right {
    display: none;
  }

  .content-header-card {
    padding: 12px;
  }

  .search-wrapper {
    padding-left: 10px;
  }

  .search-btn {
    padding: 0 12px;
  }

  .content-tabs {
    flex-wrap: wrap;
    gap: 10px;
  }

  .post-item {
    padding: 14px;
  }

  .post-stats-row {
    gap: 14px;
    flex-wrap: wrap;
  }
}
</style>
