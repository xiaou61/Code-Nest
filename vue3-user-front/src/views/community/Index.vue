<template>
  <CnPage class="community-container" surface="transparent" max-width="100%" dense>
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
              <CnStatusTag
                v-for="tag in tagList.slice(0, 15)" 
                :key="tag.id"
                class="cloud-tag"
                :class="{ active: selectedTagId === tag.id }"
                type="neutral"
                size="sm"
                :dot="false"
                subtle
                @click="selectTag(tag.id)"
              >
                # {{ tag.name }}
              </CnStatusTag>
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
                <CnStatusTag
                  v-for="(keyword, index) in hotKeywords" 
                  :key="index"
                  class="hot-tag"
                  type="warning"
                  size="sm"
                  :dot="false"
                  subtle
                  @click="selectHotKeyword(keyword)"
                >
                  {{ keyword }}
                </CnStatusTag>
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
              <CnStatusTag v-if="post.categoryName" type="info" size="sm" :dot="false" subtle>
                {{ post.categoryName }}
              </CnStatusTag>
            </div>

            <!-- 帖子主体 -->
            <div class="post-body">
              <h2 class="post-title">{{ post.title }}</h2>
              
              <!-- AI摘要 -->
              <div v-if="post.aiSummary" class="ai-summary">
                <CnStatusTag type="brand" size="sm" :dot="false">AI 摘要</CnStatusTag>
                <p>{{ post.aiSummary }}</p>
              </div>
              
              <p class="post-excerpt">{{ post.content }}</p>
              
              <!-- 帖子标签 -->
              <div v-if="post.tags && post.tags.length > 0" class="post-tags-inline">
                <CnStatusTag
                  v-for="tag in post.tags" 
                  :key="tag.id"
                  class="inline-tag"
                  type="neutral"
                  size="sm"
                  :dot="false"
                  subtle
                  @click.stop="selectTag(tag.id)"
                >
                  # {{ tag.name }}
                </CnStatusTag>
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
          <CnEmptyState
            v-if="!loading && postList.length === 0"
            title="暂无帖子"
            description="换个分类或关键词试试，也可以发布第一篇社区帖子。"
            icon="CM"
          >
            <template #actions>
              <el-button type="primary" @click="showCreateDialog">发表第一篇帖子</el-button>
            </template>
          </CnEmptyState>
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
  </CnPage>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, Star, Edit, View, ChatDotRound, Document, TrendCharts,
  Menu, PriceTag, Clock, Bell, DataLine, Pointer
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnStatusTag } from '@/design-system'
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
.community-container {
  --community-surface: var(--cn-color-bg-surface);
  --community-surface-muted: var(--cn-color-bg-surface-muted);
  --community-surface-hover: color-mix(in srgb, var(--cn-color-brand-soft) 58%, var(--cn-color-bg-surface));
  --community-border: var(--cn-color-border-subtle);
  --community-border-strong: var(--cn-color-border);
  --community-accent: var(--cn-color-brand-primary);
  --community-accent-hover: var(--cn-color-brand-hover);
  --community-accent-soft: var(--cn-color-brand-soft);
  --community-text: var(--cn-color-text-primary);
  --community-text-muted: var(--cn-color-text-secondary);
  --community-text-subtle: var(--cn-color-text-tertiary);
  --community-danger: var(--cn-color-danger);
  --community-danger-soft: var(--cn-color-danger-soft);
  --community-warning: var(--cn-color-warning);
  --community-warning-soft: var(--cn-color-warning-soft);
  --community-shadow: var(--cn-shadow-sm);
  --community-shadow-hover: var(--cn-shadow-md);
  --community-radius: var(--cn-radius-card);
  --community-radius-sm: var(--cn-radius-control);
  --community-ring: var(--cn-color-focus-ring);

  min-height: calc(100vh - 68px);
  background: transparent;
}

.community-main {
  width: 100%;
  max-width: none;
  margin: 0 auto;
  padding: 10px clamp(10px, 1.2vw, 24px) 24px;
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr) minmax(280px, 340px);
  gap: 18px;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-card,
.content-header-card,
.post-item,
.pagination-container {
  background: var(--community-surface);
  border: 1px solid var(--community-border);
  border-radius: var(--community-radius);
  box-shadow: var(--community-shadow);
}

.sidebar-card {
  padding: 14px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--community-border);
  color: var(--community-text);
  font-size: 15px;
  font-weight: 600;
}

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
  border: 1px solid var(--community-border-strong);
  border-radius: var(--community-radius-sm);
  background: var(--community-surface-muted);
  color: var(--community-text-muted);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.action-btn:hover {
  border-color: color-mix(in srgb, var(--community-accent) 36%, var(--community-border));
  background: var(--community-surface-hover);
  color: var(--community-accent);
  transform: translateY(-1px);
}

.action-btn.primary {
  border-color: var(--community-accent);
  background: var(--community-accent);
  color: white;
  box-shadow: 0 8px 18px color-mix(in srgb, var(--community-accent) 24%, transparent);
}

.action-btn.primary:hover {
  background: var(--community-accent-hover);
  box-shadow: 0 10px 20px color-mix(in srgb, var(--community-accent) 32%, transparent);
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
  border-radius: var(--community-radius-sm);
  cursor: pointer;
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
}

.nav-item:hover {
  background: var(--community-surface-hover);
}

.nav-item.active {
  background: var(--community-accent-soft);
  box-shadow: inset 3px 0 0 var(--community-accent);
  color: var(--community-accent);
}

.nav-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--community-accent-soft);
  color: var(--community-text-muted);
  font-size: 11px;
  font-weight: 600;
  opacity: 0.92;
}

.nav-text {
  flex: 1;
  font-size: 13px;
  color: var(--community-text-muted);
}

.nav-item.active .nav-text {
  color: var(--community-accent);
  font-weight: 500;
}

.nav-item.active .nav-icon,
.nav-item.active .nav-count {
  background: var(--community-accent);
  color: white;
}

.nav-count {
  min-width: 28px;
  padding: 2px 8px;
  border-radius: var(--cn-radius-pill);
  background: var(--community-accent-soft);
  color: var(--community-text-muted);
  font-size: 12px;
  text-align: center;
}

.tags-cloud,
.hot-tags,
.post-tags-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cloud-tag,
.hot-tag,
.inline-tag {
  padding: 5px 10px;
  border-radius: var(--cn-radius-pill);
  border: 1px solid var(--community-border);
  background: var(--community-surface-muted);
  color: var(--community-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
}

.cloud-tag:hover,
.cloud-tag.active,
.hot-tag:hover,
.inline-tag:hover {
  border-color: color-mix(in srgb, var(--community-accent) 32%, var(--community-border));
  background: var(--community-accent-soft);
  color: var(--community-accent);
}

.main-content {
  min-width: 0;
}

.content-header-card {
  padding: 16px 18px;
  margin-bottom: 14px;
}

.search-bar {
  position: relative;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--community-border);
}

.search-wrapper {
  display: flex;
  align-items: center;
  padding: 7px 7px 7px 14px;
  border-radius: var(--community-radius-sm);
  border: 1px solid var(--community-border-strong);
  background: var(--community-surface-muted);
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
}

.search-wrapper:focus-within {
  border-color: var(--community-accent);
  background: var(--community-surface);
  box-shadow: 0 0 0 3px var(--community-ring);
}

.search-icon {
  margin-right: 8px;
  color: var(--community-text-subtle);
  font-size: 17px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  color: var(--community-text);
  font-size: 14px;
}

.search-input::placeholder {
  color: var(--community-text-subtle);
}

.search-btn,
.empty-btn {
  height: 34px;
  padding: 0 18px;
  border: 1px solid var(--community-accent);
  border-radius: var(--community-radius-sm);
  background: var(--community-accent);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
}

.search-btn:hover,
.empty-btn:hover {
  background: var(--community-accent-hover);
  box-shadow: 0 8px 18px color-mix(in srgb, var(--community-accent) 28%, transparent);
}

.hot-keywords-dropdown {
  position: absolute;
  top: 50px;
  left: 0;
  right: 0;
  z-index: 20;
  border: 1px solid var(--community-border-strong);
  border-radius: var(--community-radius-sm);
  background: var(--community-surface);
  padding: 12px;
  box-shadow: var(--cn-shadow-popover);
}

.hot-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--community-warning);
}

.hot-tags {
  margin-top: 10px;
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
  border: 1px solid var(--community-border-strong);
  border-radius: 9px;
  background: var(--community-surface-muted);
  color: var(--community-text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
}

.tab-btn:hover {
  border-color: color-mix(in srgb, var(--community-accent) 32%, var(--community-border));
  color: var(--community-accent);
}

.tab-btn.active {
  border-color: color-mix(in srgb, var(--community-accent) 36%, var(--community-border));
  background: var(--community-accent-soft);
  color: var(--community-accent);
  font-weight: 600;
}

.posts-count {
  color: var(--community-text-subtle);
  font-size: 12px;
}

.posts-feed {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-item {
  padding: 16px 18px;
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--community-shadow-hover);
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
  background: var(--community-accent);
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
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
  color: var(--community-text);
}

.author-name:hover,
.post-item:hover .post-title,
.ranking-item:hover .rank-title {
  color: var(--community-accent);
}

.post-time {
  font-size: 12px;
  color: var(--community-text-subtle);
}

.post-category {
  padding: 3px 10px;
  border-radius: var(--cn-radius-pill);
  background: var(--community-accent-soft);
  color: var(--community-text-muted);
  font-size: 12px;
}

.post-body {
  margin-bottom: 12px;
}

.post-title {
  margin: 0 0 9px;
  color: var(--community-text);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.45;
}

.ai-summary {
  margin-bottom: 10px;
  padding: 10px 12px;
  border-radius: var(--community-radius-sm);
  border: 1px solid color-mix(in srgb, var(--community-accent) 20%, var(--community-border));
  background: color-mix(in srgb, var(--community-accent-soft) 72%, var(--community-surface));
}

.ai-badge {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--community-accent);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.ai-summary p {
  margin: 0;
  color: var(--community-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.post-excerpt {
  margin: 0;
  color: var(--community-text-muted);
  font-size: 14px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags-inline {
  margin-top: 11px;
  gap: 7px;
}

.inline-tag {
  padding: 3px 9px;
}

.post-footer {
  padding-top: 12px;
  border-top: 1px solid var(--community-border);
}

.post-stats-row {
  display: flex;
  gap: 20px;
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--community-text-subtle);
  font-size: 13px;
  cursor: pointer;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.stat:hover {
  color: var(--community-accent);
}

.stat.active,
.like-stat.active {
  color: var(--community-danger);
}

.empty-state {
  text-align: center;
  background: var(--community-surface);
  border: 1px dashed var(--community-border-strong);
  border-radius: var(--community-radius);
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
  border-radius: var(--cn-radius-pill);
  border: 1px solid var(--community-border);
  background: var(--community-surface-muted);
  color: var(--community-text-muted);
  font-size: 12px;
  font-weight: 500;
}

.empty-text {
  margin: 0 0 16px;
  color: var(--community-text-muted);
  font-size: 15px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  padding: 14px;
}

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
  border-bottom: 1px solid var(--community-border);
  cursor: pointer;
  transition: all var(--cn-motion-fast) var(--cn-ease-out);
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
  background: var(--community-surface-muted);
  color: var(--community-text-muted);
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.rank-number.rank-1 {
  background: var(--community-danger);
  color: white;
}

.rank-number.rank-2 {
  background: var(--community-warning);
  color: white;
}

.rank-number.rank-3 {
  background: color-mix(in srgb, var(--community-warning) 76%, var(--community-accent));
  color: white;
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-title {
  margin: 0 0 4px;
  color: var(--community-text);
  font-size: 13px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rank-heat {
  color: var(--community-danger);
  font-size: 11px;
}

.notice-content {
  color: var(--community-text-muted);
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
  border-radius: var(--community-radius-sm);
  border: 1px solid var(--community-border);
  background: var(--community-surface-muted);
  padding: 10px 6px;
  text-align: center;
}

.stat-number {
  display: block;
  margin-bottom: 4px;
  color: var(--community-accent);
  font-size: 20px;
  font-weight: 700;
}

.stat-label {
  color: var(--community-text-subtle);
  font-size: 11px;
}

@media (max-width: 1240px) {
  .community-main {
    grid-template-columns: minmax(220px, 260px) minmax(0, 1fr) minmax(240px, 280px);
    gap: 14px;
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
