<template>
  <div class="daily-content">
    <!-- 头部区域 -->
    <div class="content-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="page-title">
            <el-icon class="title-icon"><Reading /></el-icon>
            每日内容
          </h1>
          <p class="page-subtitle">每天一点小知识，积累成就大智慧</p>
        </div>
      </div>
    </div>

    <!-- 今日推荐区域 -->
    <div class="today-section" v-loading="todayLoading">
      <div class="section-header">
        <h2>
          <el-icon><Sunny /></el-icon>
          今日推荐
        </h2>
        <el-button @click="refreshTodayContent" :loading="todayLoading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
      
      <div class="today-content-grid">
        <div 
          class="content-card today-card" 
          v-for="content in todayContents" 
          :key="content.id"
          @click="showContentDetail(content)">
          
          <div class="card-header">
            <div class="content-type-badge">
              <el-icon><component :is="getContentIcon(content.contentType)" /></el-icon>
              <span>{{ getContentTypeName(content.contentType) }}</span>
            </div>
            <div class="content-meta">
              <el-tag v-if="content.difficultyLevel" :type="getDifficultyTag(content.difficultyLevel)" size="small">
                {{ getDifficultyName(content.difficultyLevel) }}
              </el-tag>
            </div>
          </div>
          
          <div class="card-content">
            <h3 class="content-title">{{ content.title }}</h3>
            <div class="content-preview" v-html="formatContent(content.content)"></div>
            <div class="content-author" v-if="content.author">
              <el-icon><User /></el-icon>
              {{ content.author }}
            </div>
          </div>
          
          <div class="card-footer">
            <div class="content-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ content.viewCount || 0 }}
              </span>
              <span class="stat-item" :class="{ 'liked': content.userLiked }">
                <el-icon><Star /></el-icon>
                {{ content.likeCount || 0 }}
              </span>
            </div>
            <div class="content-actions">
              <el-button 
                size="small" 
                :type="content.userLiked ? 'primary' : ''"
                @click.stop="toggleLike(content)">
                <el-icon><Star /></el-icon>
                {{ content.userLiked ? '已赞' : '点赞' }}
              </el-button>
              <el-button 
                size="small"
                :type="content.userCollected ? 'warning' : ''"
                @click.stop="toggleCollect(content)">
                <el-icon><Collection /></el-icon>
                {{ content.userCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容分类区域 -->
    <div class="content-categories">
      <div class="category-tabs">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="编程格言" name="1">
            <template #label>
              <span class="tab-label">
                <el-icon><ChatLineRound /></el-icon>
                编程格言
              </span>
            </template>
          </el-tab-pane>
          
          <el-tab-pane label="技术小贴士" name="2">
            <template #label>
              <span class="tab-label">
                <el-icon><Notification /></el-icon>
                技术小贴士
              </span>
            </template>
          </el-tab-pane>
          
          <el-tab-pane label="代码片段" name="3">
            <template #label>
              <span class="tab-label">
                <el-icon><Document /></el-icon>
                代码片段
              </span>
            </template>
          </el-tab-pane>
          
          <el-tab-pane label="历史上的今天" name="4">
            <template #label>
              <span class="tab-label">
                <el-icon><Clock /></el-icon>
                历史上的今天
              </span>
            </template>
          </el-tab-pane>
          
          <el-tab-pane label="我的收藏" name="collections">
            <template #label>
              <span class="tab-label">
                <el-icon><Star /></el-icon>
                我的收藏
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <!-- 内容列表 -->
      <div class="content-list" v-loading="contentLoading">
        <div 
          class="content-card list-card" 
          v-for="content in currentContents" 
          :key="content.id"
          @click="showContentDetail(content)">
          
          <div class="card-header">
            <div class="content-type-badge">
              <el-icon><component :is="getContentIcon(content.contentType)" /></el-icon>
              <span>{{ getContentTypeName(content.contentType) }}</span>
            </div>
            <div class="content-meta">
              <el-tag v-if="content.difficultyLevel" :type="getDifficultyTag(content.difficultyLevel)" size="small">
                {{ getDifficultyName(content.difficultyLevel) }}
              </el-tag>
              <span class="content-date">{{ formatDate(content.createTime) }}</span>
            </div>
          </div>
          
          <div class="card-content">
            <h3 class="content-title">{{ content.title }}</h3>
            <div class="content-preview" v-html="formatContent(content.content)"></div>
            
            <div class="content-tags" v-if="content.tags?.length">
              <el-tag v-for="tag in content.tags" :key="tag" size="small" type="info">
                {{ tag }}
              </el-tag>
            </div>
            
            <div class="content-author" v-if="content.author">
              <el-icon><User /></el-icon>
              {{ content.author }}
            </div>
          </div>
          
          <div class="card-footer">
            <div class="content-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ content.viewCount || 0 }}
              </span>
              <span class="stat-item" :class="{ 'liked': content.userLiked }">
                <el-icon><Star /></el-icon>
                {{ content.likeCount || 0 }}
              </span>
            </div>
            <div class="content-actions">
              <el-button 
                size="small" 
                :type="content.userLiked ? 'primary' : ''"
                @click.stop="toggleLike(content)">
                <el-icon><Star /></el-icon>
                {{ content.userLiked ? '已赞' : '点赞' }}
              </el-button>
              <el-button 
                size="small"
                :type="content.userCollected ? 'warning' : ''"
                @click.stop="toggleCollect(content)">
                <el-icon><Collection /></el-icon>
                {{ content.userCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <el-empty v-if="!contentLoading && currentContents.length === 0" description="暂无内容">
          <el-button type="primary" @click="refreshContent">刷新试试</el-button>
        </el-empty>
      </div>
    </div>

    <!-- 内容详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedContent?.title"
      width="800px"
      destroy-on-close
      class="content-detail-dialog">
      
      <div class="content-detail" v-if="selectedContent">
        <div class="detail-header">
          <div class="detail-meta">
            <div class="content-type-badge large">
              <el-icon><component :is="getContentIcon(selectedContent.contentType)" /></el-icon>
              <span>{{ getContentTypeName(selectedContent.contentType) }}</span>
            </div>
            <div class="detail-tags">
              <el-tag v-if="selectedContent.difficultyLevel" :type="getDifficultyTag(selectedContent.difficultyLevel)">
                {{ getDifficultyName(selectedContent.difficultyLevel) }}
              </el-tag>
              <el-tag v-if="selectedContent.programmingLanguage" type="success">
                {{ selectedContent.programmingLanguage }}
              </el-tag>
            </div>
          </div>
          <div class="detail-stats">
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ selectedContent.viewCount || 0 }} 查看
            </span>
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ selectedContent.likeCount || 0 }} 点赞
            </span>
          </div>
        </div>
        
        <div class="detail-content" v-html="formatDetailContent(selectedContent.content)"></div>
        
        <div class="detail-tags" v-if="selectedContent.tags?.length">
          <h4>标签</h4>
          <div class="tags-list">
            <el-tag v-for="tag in selectedContent.tags" :key="tag" type="info">
              {{ tag }}
            </el-tag>
          </div>
        </div>
        
        <div class="detail-author" v-if="selectedContent.author">
          <h4>作者</h4>
          <div class="author-info">
            <el-icon><User /></el-icon>
            <span>{{ selectedContent.author }}</span>
          </div>
        </div>
        
        <div class="detail-source" v-if="selectedContent.sourceUrl">
          <h4>来源</h4>
          <el-link :href="selectedContent.sourceUrl" target="_blank" type="primary">
            <el-icon><Link /></el-icon>
            {{ selectedContent.sourceUrl }}
          </el-link>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button 
            :type="selectedContent?.userLiked ? 'primary' : ''"
            @click="toggleLike(selectedContent)">
            <el-icon><Star /></el-icon>
            {{ selectedContent?.userLiked ? '已赞' : '点赞' }}
          </el-button>
          <el-button 
            :type="selectedContent?.userCollected ? 'warning' : ''"
            @click="toggleCollect(selectedContent)">
            <el-icon><Collection /></el-icon>
            {{ selectedContent?.userCollected ? '已收藏' : '收藏' }}
          </el-button>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Reading, Sunny, Refresh, User, View, Star, Collection, Link,
  ChatLineRound, Notification, Document, Clock
} from '@element-plus/icons-vue'
import {
  getTodayContent,
  getContentsByType,
  getUserCollections,
  likeContent,
  collectContent
} from '@/api/moyu'

// 响应式数据
const todayLoading = ref(false)
const contentLoading = ref(false)
const todayContents = ref([])
const currentContents = ref([])
const activeTab = ref('1')
const detailDialogVisible = ref(false)
const selectedContent = ref(null)

// 工具函数
const getContentTypeName = (type) => {
  const typeMap = {
    1: '编程格言',
    2: '技术小贴士',
    3: '代码片段',
    4: '历史上的今天'
  }
  return typeMap[type] || '未知'
}

const getContentIcon = (type) => {
  const iconMap = {
    1: 'ChatLineRound',
    2: 'Notification',
    3: 'Document',
    4: 'Clock'
  }
  return iconMap[type] || 'Document'
}

const getDifficultyName = (level) => {
  const levelMap = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return levelMap[level] || '未知'
}

const getDifficultyTag = (level) => {
  const tagMap = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return tagMap[level] || 'info'
}

const formatContent = (content) => {
  if (!content) return ''
  
  // 简单的内容格式化，限制长度并添加语法高亮
  let formatted = content.replace(/\n/g, '<br>')
  
  // 如果是代码片段，添加代码样式
  if (content.includes('```') || content.includes('`')) {
    formatted = formatted.replace(/```([^`]+)```/g, '<pre><code>$1</code></pre>')
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>')
  }
  
  // 限制预览长度
  if (formatted.length > 200) {
    formatted = formatted.substring(0, 200) + '...'
  }
  
  return formatted
}

const formatDetailContent = (content) => {
  if (!content) return ''
  
  let formatted = content.replace(/\n/g, '<br>')
  
  // 代码格式化
  formatted = formatted.replace(/```([^`]+)```/g, '<pre class="code-block"><code>$1</code></pre>')
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
  
  return formatted
}

const formatDate = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

// 数据加载
const loadTodayContent = async () => {
  try {
    todayLoading.value = true
    const data = await getTodayContent()
    todayContents.value = data || []
  } catch (error) {
    console.error('加载今日内容失败:', error)
    ElMessage.error('加载今日内容失败')
  } finally {
    todayLoading.value = false
  }
}

const loadContentsByType = async (contentType) => {
  try {
    contentLoading.value = true
    const data = await getContentsByType(contentType)
    currentContents.value = data || []
  } catch (error) {
    console.error('加载内容列表失败:', error)
    ElMessage.error('加载内容列表失败')
  } finally {
    contentLoading.value = false
  }
}

const loadUserCollections = async () => {
  try {
    contentLoading.value = true
    const data = await getUserCollections()
    currentContents.value = data || []
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载收藏列表失败')
  } finally {
    contentLoading.value = false
  }
}

// 事件处理
const refreshTodayContent = () => {
  loadTodayContent()
}

const refreshContent = () => {
  handleTabChange(activeTab.value)
}

const handleTabChange = (tabName) => {
  if (tabName === 'collections') {
    loadUserCollections()
  } else {
    loadContentsByType(parseInt(tabName))
  }
}

const showContentDetail = (content) => {
  selectedContent.value = content
  detailDialogVisible.value = true
}

const toggleLike = async (content) => {
  try {
    await likeContent(content.id)
    content.userLiked = !content.userLiked
    content.likeCount = (content.likeCount || 0) + (content.userLiked ? 1 : -1)
    ElMessage.success(content.userLiked ? '点赞成功' : '取消点赞')
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const toggleCollect = async (content) => {
  try {
    await collectContent(content.id)
    content.userCollected = !content.userCollected
    ElMessage.success(content.userCollected ? '收藏成功' : '取消收藏')
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 初始化
onMounted(() => {
  loadTodayContent()
  loadContentsByType(1) // 默认加载编程格言
})
</script>

<style scoped>
.daily-content {
  min-height: 100vh;
  background: linear-gradient(135deg, #74b9ff 0%, #6c5ce7 100%);
}

/* 头部区域 */
.content-header {
  padding: 60px 20px 40px;
  color: white;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.page-title {
  font-size: 3rem;
  font-weight: 700;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.title-icon {
  font-size: 3rem;
  color: #fdcb6e;
}

.page-subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  margin: 0;
}

/* 今日推荐区域 */
.today-section {
  padding: 0 20px 40px;
}

.section-header {
  max-width: 1200px;
  margin: 0 auto 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
}

.section-header h2 {
  margin: 0;
  font-size: 1.8rem;
  display: flex;
  align-items: center;
  gap: 12px;
}

.today-content-grid {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

/* 内容分类区域 */
.content-categories {
  background: white;
  border-radius: 24px 24px 0 0;
  padding: 30px 20px;
  min-height: 60vh;
}

.category-tabs {
  max-width: 1200px;
  margin: 0 auto;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-list {
  max-width: 1200px;
  margin: 20px auto 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

/* 内容卡片 */
.content-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.content-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.today-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.content-type-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.85rem;
  font-weight: 500;
  color: white;
}

.content-card .content-type-badge {
  background: linear-gradient(135deg, #74b9ff, #6c5ce7);
}

.content-type-badge.large {
  padding: 8px 16px;
  font-size: 1rem;
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-date {
  font-size: 0.8rem;
  color: #999;
}

.card-content {
  margin-bottom: 16px;
}

.content-title {
  margin: 0 0 12px 0;
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
}

.content-preview {
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
}

.content-preview :deep(code) {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #e74c3c;
}

.content-preview :deep(pre) {
  background: #2d3748;
  color: #e2e8f0;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.content-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.content-author {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 0.9rem;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.content-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
  font-size: 0.9rem;
}

.stat-item.liked {
  color: #f39c12;
}

.content-actions {
  display: flex;
  gap: 8px;
}

/* 内容详情对话框 */
:deep(.content-detail-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #74b9ff, #6c5ce7);
    color: white;
    border-radius: 8px 8px 0 0;
  }
  
  .el-dialog__title {
    color: white;
  }
  
  .el-dialog__headerbtn .el-dialog__close {
    color: white;
  }
}

.content-detail {
  color: #333;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-stats {
  display: flex;
  gap: 16px;
  color: #666;
}

.detail-content {
  line-height: 1.8;
  margin-bottom: 24px;
}

.detail-content :deep(.code-block) {
  background: #2d3748;
  color: #e2e8f0;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 16px 0;
}

.detail-content :deep(.inline-code) {
  background: #f8f9fa;
  padding: 2px 6px;
  border-radius: 4px;
  color: #e74c3c;
  font-size: 0.9em;
}

.detail-tags h4,
.detail-author h4,
.detail-source h4 {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 1rem;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
    flex-direction: column;
    gap: 8px;
  }
  
  .section-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  
  .today-content-grid,
  .content-list {
    grid-template-columns: 1fr;
  }
  
  .card-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  
  .content-actions {
    justify-content: center;
  }
  
  .detail-header {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
