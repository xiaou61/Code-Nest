<template>
  <CnPage class="post-detail-container" surface="transparent" max-width="1240px">
    <div class="detail-layout">
      <!-- 左侧主内容 -->
      <div class="detail-main">
        <!-- 帖子详情卡片 -->
        <article v-if="postDetail" class="post-article">
          <!-- 返回导航 -->
          <div class="breadcrumb-nav">
            <span class="back-link" @click="goBack">
              <el-icon><Back /></el-icon>
              返回社区
            </span>
            <span class="breadcrumb-sep">/</span>
            <span class="current-page">{{ postDetail.categoryName || '帖子详情' }}</span>
          </div>

          <!-- 帖子标题 -->
          <h1 class="article-title">{{ postDetail.title }}</h1>

          <!-- 作者信息栏 -->
          <div class="author-bar">
            <div class="author-info">
              <div class="author-avatar" @click.stop="goToUserProfile(postDetail.authorId)">
                {{ postDetail.authorName?.charAt(0) || '匿' }}
              </div>
              <div class="author-detail">
                <span class="author-name" @click.stop="goToUserProfile(postDetail.authorId)">
                  {{ postDetail.authorName }}
                </span>
                <div class="post-meta-info">
                  <span>{{ formatRelativeTime(postDetail.createTime) }}发布</span>
                  <span>·</span>
                  <span>{{ postDetail.viewCount || 0 }} 阅读</span>
                </div>
              </div>
            </div>
            <div class="action-buttons">
              <button class="action-btn action-asset-btn" @click="openTransformDialog">
                <el-icon><MagicStick /></el-icon>
                <span>转学习资产</span>
              </button>
              <button 
                class="action-btn" 
                :class="{ active: postDetail.isLiked }"
                @click="toggleLike"
              >
                <el-icon><Pointer /></el-icon>
                <span>{{ postDetail.isLiked ? '已赞' : '点赞' }}</span>
                <span class="count">{{ postDetail.likeCount || 0 }}</span>
              </button>
              <button 
                class="action-btn" 
                :class="{ active: postDetail.isCollected, collected: postDetail.isCollected }"
                @click="toggleCollect"
              >
                <el-icon><Star /></el-icon>
                <span>{{ postDetail.isCollected ? '已收藏' : '收藏' }}</span>
                <span class="count">{{ postDetail.collectCount || 0 }}</span>
              </button>
            </div>
          </div>
        
          <!-- AI智能分析卡片 -->
          <div class="ai-analysis-card">
            <div class="ai-card-header">
              <div class="ai-title">
                <span class="ai-icon">🤖</span>
                <span>AI 智能分析</span>
              </div>
              <button 
                v-if="!postDetail.aiSummary && !aiSummaryLoading"
                class="ai-btn"
                @click="generateAISummary"
              >
                <el-icon><MagicStick /></el-icon>
                一键分析
              </button>
              <button 
                v-else-if="postDetail.aiSummary"
                class="ai-btn secondary"
                @click="generateAISummary"
                :disabled="aiSummaryLoading"
              >
                重新分析
              </button>
            </div>
            
            <!-- 未生成状态 -->
            <div v-if="!postDetail.aiSummary && !aiSummaryLoading" class="ai-empty">
              <p>点击「一键分析」，让 AI 为您提取核心内容和关键技术点</p>
            </div>
            
            <!-- 生成中状态 -->
            <div v-else-if="aiSummaryLoading" class="ai-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>AI 正在分析中...</span>
            </div>
            
            <!-- 已生成状态 -->
            <div v-else-if="postDetail.aiSummary" class="ai-result">
              <p class="ai-summary-text">{{ postDetail.aiSummary }}</p>
              <div v-if="aiKeywords && aiKeywords.length > 0" class="ai-keywords">
                <CnStatusTag
                  v-for="(keyword, index) in aiKeywords" 
                  :key="index"
                  class="keyword-tag"
                  type="brand"
                  size="sm"
                  :dot="false"
                  subtle
                >
                  {{ keyword }}
                </CnStatusTag>
              </div>
            </div>
          </div>

          <!-- 文章内容 -->
          <div class="article-content markdown-content" v-html="formatContent(postDetail.content)"></div>
          
          <!-- 帖子标签 -->
          <div v-if="postDetail.tags && postDetail.tags.length > 0" class="article-tags">
            <CnStatusTag
              v-for="tag in postDetail.tags" 
              :key="tag.id"
              class="tag-item"
              type="neutral"
              size="sm"
              :dot="false"
              subtle
            >
              # {{ tag.name }}
            </CnStatusTag>
          </div>

          <!-- 底部互动栏 -->
          <div class="article-footer">
            <div class="footer-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                {{ postDetail.viewCount || 0 }} 阅读
              </span>
              <span class="stat-item">
                <el-icon><ChatDotRound /></el-icon>
                {{ postDetail.commentCount || 0 }} 评论
              </span>
            </div>
            <div class="footer-actions">
              <button 
                class="footer-btn" 
                :class="{ active: postDetail.isLiked }"
                @click="toggleLike"
              >
                <el-icon><Pointer /></el-icon>
                {{ postDetail.likeCount || 0 }}
              </button>
              <button 
                class="footer-btn" 
                :class="{ active: postDetail.isCollected }"
                @click="toggleCollect"
              >
                <el-icon><Star /></el-icon>
                {{ postDetail.collectCount || 0 }}
              </button>
            </div>
          </div>
        </article>

        <!-- 评论区 -->
        <section class="comments-section">
          <div class="section-header">
            <h3>评论 <span class="count">{{ commentTotal }}</span></h3>
          </div>

          <!-- 发表评论 -->
          <div class="comment-form">
            <div class="form-avatar">
              {{ currentUserInitial }}
            </div>
            <div class="form-content">
              <el-form :model="commentForm" :rules="commentRules" ref="commentFormRef">
                <el-form-item prop="content">
                  <el-input 
                    v-model="commentForm.content" 
                    type="textarea" 
                    placeholder="说说你的看法..." 
                    :rows="3"
                    maxlength="500"
                    show-word-limit
                    resize="none"
                  />
                </el-form-item>
                <div class="form-actions">
                  <button class="submit-btn" @click="handleCreateComment" :disabled="commentLoading">
                    {{ commentLoading ? '发布中...' : '发布评论' }}
                  </button>
                </div>
              </el-form>
            </div>
          </div>

          <!-- 评论列表 -->
          <div v-loading="commentsLoading" class="comments-list">
            <div 
              v-for="comment in commentList" 
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-avatar" @click.stop="goToUserProfile(comment.authorId)">
                {{ comment.authorName?.charAt(0) || '匿' }}
              </div>
              <div class="comment-main">
                <div class="comment-header">
                  <span class="comment-author" @click.stop="goToUserProfile(comment.authorId)">
                    {{ comment.authorName }}
                  </span>
                  <span class="comment-time">{{ formatRelativeTime(comment.createTime) }}</span>
                </div>
                <div class="comment-body">{{ comment.content }}</div>
                <div class="comment-actions">
                  <span 
                    class="action-item" 
                    :class="{ active: comment.isLiked }"
                    @click="toggleCommentLike(comment)"
                  >
                    <el-icon><Pointer /></el-icon>
                    {{ comment.likeCount || 0 }}
                  </span>
                  <span class="action-item" @click="showReplyBox(comment)">
                    <el-icon><ChatDotRound /></el-icon>
                    回复
                  </span>
                </div>
                
                <!-- 回复框 -->
                <div v-if="replyingComment && replyingComment.id === comment.id" class="reply-form">
                  <el-input 
                    v-model="replyForm.content" 
                    type="textarea"
                    :placeholder="`回复 @${comment.authorName}...`"
                    :rows="2"
                    maxlength="500"
                    resize="none"
                  />
                  <div class="reply-form-actions">
                    <button class="cancel-btn" @click="cancelReply">取消</button>
                    <button 
                      class="reply-btn" 
                      @click="handleReply(comment)"
                      :disabled="replyLoading"
                    >
                      回复
                    </button>
                  </div>
                </div>
                
                <!-- 二级回复 -->
                <div v-if="comment.replyCount > 0" class="replies-wrapper">
                  <div class="show-replies" @click="toggleReplies(comment)">
                    <el-icon :class="{ expanded: expandedComments[comment.id] }"><ArrowDown /></el-icon>
                    {{ expandedComments[comment.id] ? '收起回复' : `展开 ${comment.replyCount} 条回复` }}
                  </div>
                  
                  <div v-if="expandedComments[comment.id]" class="replies-list">
                    <div 
                      v-for="reply in comment.replies || []" 
                      :key="reply.id"
                      class="reply-item"
                    >
                      <div class="reply-avatar" @click.stop="goToUserProfile(reply.authorId)">
                        {{ reply.authorName?.charAt(0) || '匿' }}
                      </div>
                      <div class="reply-main">
                        <div class="reply-header">
                          <span class="reply-author" @click.stop="goToUserProfile(reply.authorId)">
                            {{ reply.authorName }}
                          </span>
                          <span v-if="reply.replyToUserName" class="reply-to">
                            回复 <span @click.stop="goToUserProfile(reply.replyToUserId)">@{{ reply.replyToUserName }}</span>
                          </span>
                          <span class="reply-time">{{ formatRelativeTime(reply.createTime) }}</span>
                        </div>
                        <div class="reply-body">{{ reply.content }}</div>
                        <div class="reply-actions">
                          <span 
                            class="action-item" 
                            :class="{ active: reply.isLiked }"
                            @click="toggleCommentLike(reply)"
                          >
                            <el-icon><Pointer /></el-icon>
                            {{ reply.likeCount || 0 }}
                          </span>
                          <span class="action-item" @click="showReplyBox(reply, comment)">
                            <el-icon><ChatDotRound /></el-icon>
                            回复
                          </span>
                        </div>
                      </div>
                    </div>
                    
                    <div v-if="comment.replyCount > (comment.replies || []).length" class="load-more">
                      <span @click="loadMoreReplies(comment)" :class="{ loading: loadingReplies[comment.id] }">
                        {{ loadingReplies[comment.id] ? '加载中...' : '加载更多回复' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <CnEmptyState
              v-if="!commentsLoading && commentList.length === 0"
              class="empty-comments"
              title="暂无评论"
              description="来发表第一条评论吧。"
              icon="CM"
              size="sm"
            />
          </div>

          <!-- 分页 -->
          <div class="comments-pagination" v-if="commentTotal > commentQueryParams.size">
            <el-pagination 
              v-model:current-page="commentQueryParams.page" 
              v-model:page-size="commentQueryParams.size"
              :total="commentTotal"
              layout="prev, pager, next"
              @current-change="handleCommentCurrentChange"
            />
          </div>
        </section>
      </div>

      <!-- 右侧边栏 -->
      <aside class="detail-sidebar">
        <!-- 作者卡片 -->
        <div v-if="postDetail" class="sidebar-card author-card">
          <div class="author-card-avatar" @click="goToUserProfile(postDetail.authorId)">
            {{ postDetail.authorName?.charAt(0) || '匿' }}
          </div>
          <div class="author-card-name" @click="goToUserProfile(postDetail.authorId)">
            {{ postDetail.authorName }}
          </div>
          <button class="follow-btn" @click="goToUserProfile(postDetail.authorId)">
            查看主页
          </button>
        </div>

        <!-- 目录导航（可选） -->
        <div class="sidebar-card toc-card">
          <div class="card-title">文章信息</div>
          <div class="info-list">
            <div class="info-item">
              <span class="label">发布时间</span>
              <span class="value">{{ formatDate(postDetail?.createTime) }}</span>
            </div>
            <div class="info-item">
              <span class="label">阅读数</span>
              <span class="value">{{ postDetail?.viewCount || 0 }}</span>
            </div>
            <div class="info-item">
              <span class="label">评论数</span>
              <span class="value">{{ postDetail?.commentCount || 0 }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="community"
      :source-id="postDetail?.id || Number(route.params.id)"
      :source-title="postDetail?.title || ''"
      :default-tags="(postDetail?.tags || []).map(tag => tag.name)"
      @success="handleTransformSuccess"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Back, Star, View, ChatDotRound, MagicStick, Loading, ArrowDown, Pointer
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnStatusTag } from '@/design-system'
import { communityApi } from '@/api/community'
import { renderMarkdown } from '@/utils/markdown'
import { useUserStore } from '@/stores/user'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const transformDialogVisible = ref(false)

// 当前用户头像首字母
const currentUserInitial = computed(() => {
  return userStore.userInfo?.username?.charAt(0) || '我'
})

// 响应式数据
const postDetail = ref(null)
const commentList = ref([])
const commentTotal = ref(0)
const commentsLoading = ref(false)
const commentLoading = ref(false)
const aiSummaryLoading = ref(false)
const aiKeywords = ref([]) // AI提取的关键词

// 二级评论相关
const replyingComment = ref(null) // 当前正在回复的评论
const parentComment = ref(null) // 父评论（用于二级回复）
const replyLoading = ref(false)
const expandedComments = ref({}) // 展开的评论ID集合
const loadingReplies = ref({}) // 正在加载回复的评论ID集合

// 评论查询参数
const commentQueryParams = reactive({
  page: 1,
  size: 10
})

// 评论表单
const commentForm = reactive({
  content: ''
})

// 回复表单
const replyForm = reactive({
  content: '',
  replyToUserId: null,
  replyToCommentId: null
})

const commentFormRef = ref(null)

// 评论验证规则
const commentRules = reactive({
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 3, max: 500, message: '评论内容为3-500个字符', trigger: 'blur' }
  ]
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

// 格式化帖子内容（渲染Markdown）
const formatContent = (content) => {
  if (!content) return ''
  return renderMarkdown(content)
}

// 获取帖子详情
const fetchPostDetail = async () => {
  try {
    const postId = route.params.id
    postDetail.value = await communityApi.getPostDetail(postId)
  } catch (error) {
    ElMessage.error('获取帖子详情失败')
    goBack()
  }
}

// 获取评论列表
const fetchComments = async () => {
  commentsLoading.value = true
  try {
    const postId = route.params.id
    const response = await communityApi.getPostComments(postId, commentQueryParams)
    commentList.value = response.records || []
    commentTotal.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取评论列表失败')
  } finally {
    commentsLoading.value = false
  }
}

// 切换帖子点赞状态
const toggleLike = async () => {
  try {
    if (postDetail.value.isLiked) {
      await communityApi.unlikePost(postDetail.value.id)
      postDetail.value.likeCount = Math.max(0, postDetail.value.likeCount - 1)
      postDetail.value.isLiked = false
      ElMessage.success('取消点赞成功')
    } else {
      await communityApi.likePost(postDetail.value.id)
      postDetail.value.likeCount = postDetail.value.likeCount + 1
      postDetail.value.isLiked = true
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 切换帖子收藏状态
const toggleCollect = async () => {
  try {
    if (postDetail.value.isCollected) {
      await communityApi.uncollectPost(postDetail.value.id)
      postDetail.value.collectCount = Math.max(0, postDetail.value.collectCount - 1)
      postDetail.value.isCollected = false
      ElMessage.success('取消收藏成功')
    } else {
      await communityApi.collectPost(postDetail.value.id)
      postDetail.value.collectCount = postDetail.value.collectCount + 1
      postDetail.value.isCollected = true
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 发表评论
const handleCreateComment = async () => {
  try {
    await commentFormRef.value.validate()
    commentLoading.value = true
    
    const postId = route.params.id
    await communityApi.createComment(postId, commentForm)
    ElMessage.success('评论成功')
    
    // 清空表单
    commentForm.content = ''
    commentFormRef.value?.clearValidate()
    
    // 刷新评论列表
    commentQueryParams.page = 1
    await fetchComments()
    
    // 更新帖子评论数
    postDetail.value.commentCount = postDetail.value.commentCount + 1
  } catch (error) {
    if (error !== false) { // 不是表单验证失败
      ElMessage.error('评论失败')
    }
  } finally {
    commentLoading.value = false
  }
}

// 生成AI摘要
const generateAISummary = async () => {
  aiSummaryLoading.value = true
  try {
    // 如果已经有摘要，说明是重新分析，传true强制刷新
    const forceRefresh = !!postDetail.value.aiSummary
    const result = await communityApi.generateSummary(postDetail.value.id, forceRefresh)
    
    // 更新关键词
    if (result.keywords && Array.isArray(result.keywords)) {
      aiKeywords.value = result.keywords
    }
    
    // 重新获取帖子详情以获取AI摘要
    await fetchPostDetail()
    ElMessage.success('AI摘要生成成功！')
  } catch (error) {
    ElMessage.error('生成AI摘要失败')
  } finally {
    aiSummaryLoading.value = false
  }
}

// 切换评论点赞状态
const toggleCommentLike = async (comment) => {
  try {
    if (comment.isLiked) {
      await communityApi.unlikeComment(comment.id)
      comment.likeCount = Math.max(0, comment.likeCount - 1)
      comment.isLiked = false
    } else {
      await communityApi.likeComment(comment.id)
      comment.likeCount = comment.likeCount + 1
      comment.isLiked = true
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 显示回复框
const showReplyBox = (comment, parent = null) => {
  replyingComment.value = comment
  parentComment.value = parent
  replyForm.content = ''
  replyForm.replyToUserId = comment.authorId
  replyForm.replyToCommentId = comment.id
}

// 取消回复
const cancelReply = () => {
  replyingComment.value = null
  parentComment.value = null
  replyForm.content = ''
  replyForm.replyToUserId = null
  replyForm.replyToCommentId = null
}

// 发送回复
const handleReply = async (comment) => {
  if (!replyForm.content.trim()) {
    ElMessage.error('请输入回复内容')
    return
  }
  
  if (replyForm.content.length < 3 || replyForm.content.length > 500) {
    ElMessage.error('回复内容为3-500个字符')
    return
  }
  
  replyLoading.value = true
  try {
    await communityApi.replyComment(comment.id, replyForm)
    ElMessage.success('回复成功')
    
    // 清空回复表单
    cancelReply()
    
    // 如果该评论的回复已展开，则刷新回复列表
    const targetComment = parentComment.value || comment
    if (expandedComments.value[targetComment.id]) {
      await loadReplies(targetComment)
    } else {
      // 否则增加回复计数
      targetComment.replyCount = (targetComment.replyCount || 0) + 1
    }
    
    // 更新帖子评论数
    postDetail.value.commentCount = postDetail.value.commentCount + 1
  } catch (error) {
    ElMessage.error('回复失败')
  } finally {
    replyLoading.value = false
  }
}

// 切换回复列表展开/折叠
const toggleReplies = async (comment) => {
  if (expandedComments.value[comment.id]) {
    expandedComments.value[comment.id] = false
  } else {
    expandedComments.value[comment.id] = true
    if (!comment.replies || comment.replies.length === 0) {
      await loadReplies(comment)
    }
  }
}

// 加载回复列表
const loadReplies = async (comment) => {
  loadingReplies.value[comment.id] = true
  try {
    const response = await communityApi.getCommentReplies(comment.id, {
      pageNum: 1,
      pageSize: 10
    })
    comment.replies = response.records || []
    comment.replyCount = response.total || 0
  } catch (error) {
    ElMessage.error('加载回复列表失败')
  } finally {
    loadingReplies.value[comment.id] = false
  }
}

// 加载更多回复
const loadMoreReplies = async (comment) => {
  loadingReplies.value[comment.id] = true
  try {
    const currentPage = Math.floor((comment.replies || []).length / 10) + 1
    const response = await communityApi.getCommentReplies(comment.id, {
      pageNum: currentPage + 1,
      pageSize: 10
    })
    comment.replies = [...(comment.replies || []), ...(response.records || [])]
  } catch (error) {
    ElMessage.error('加载回复失败')
  } finally {
    loadingReplies.value[comment.id] = false
  }
}

// 评论当前页改变
const handleCommentCurrentChange = (page) => {
  commentQueryParams.page = page
  fetchComments()
}

// 返回社区
const goBack = () => {
  router.push('/community')
}

// 跳转到用户主页
const goToUserProfile = (userId) => {
  if (userId) {
    router.push(`/community/users/${userId}`)
  }
}

const openTransformDialog = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning('请先登录后再转化学习资产')
    router.push('/login')
    return
  }
  transformDialogVisible.value = true
}

const handleTransformSuccess = (record) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

// 初始化
onMounted(async () => {
  await fetchPostDetail()
  await fetchComments()
})
</script>

<style scoped>
.post-detail-container {
  --detail-surface: var(--cn-color-bg-surface);
  --detail-surface-muted: var(--cn-color-bg-surface-muted);
  --detail-surface-soft: color-mix(in srgb, var(--cn-color-brand-soft) 56%, var(--cn-color-bg-surface));
  --detail-border: var(--cn-color-border-subtle);
  --detail-border-strong: var(--cn-color-border);
  --detail-accent: var(--cn-color-brand-primary);
  --detail-accent-hover: var(--cn-color-brand-hover);
  --detail-accent-soft: var(--cn-color-brand-soft);
  --detail-text: var(--cn-color-text-primary);
  --detail-text-muted: var(--cn-color-text-secondary);
  --detail-text-subtle: var(--cn-color-text-tertiary);
  --detail-warning: var(--cn-color-warning);
  --detail-warning-soft: var(--cn-color-warning-soft);
  --detail-shadow: var(--cn-shadow-sm);
  --detail-shadow-hover: var(--cn-shadow-md);
  --detail-radius: var(--cn-radius-card);
  --detail-radius-sm: var(--cn-radius-control);

  min-height: 100vh;
  background: var(--cn-color-bg-page);
  padding: 24px;
}

.detail-layout {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 24px;
}

.detail-main {
  min-width: 0;
}

.post-article,
.comments-section,
.sidebar-card {
  background: var(--detail-surface);
  border: 1px solid var(--detail-border);
  border-radius: var(--detail-radius);
  box-shadow: var(--detail-shadow);
}

.post-article {
  padding: 32px;
  margin-bottom: 24px;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  color: var(--detail-text-subtle);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--detail-accent);
  cursor: pointer;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.back-link:hover {
  color: var(--detail-accent-hover);
}

.breadcrumb-sep {
  color: var(--detail-border-strong);
}

.current-page {
  color: var(--detail-text-muted);
}

.article-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--detail-text);
  line-height: 1.4;
  margin: 0 0 24px;
}

.author-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--detail-border);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar,
.form-avatar,
.comment-avatar,
.reply-avatar,
.author-card-avatar {
  border-radius: 50%;
  background: var(--detail-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.author-avatar {
  width: 48px;
  height: 48px;
  font-size: 18px;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
}

.author-avatar:hover,
.author-card-avatar:hover {
  transform: scale(1.05);
}

.author-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name,
.comment-author,
.reply-author,
.author-card-name {
  font-weight: 600;
  color: var(--detail-text);
  cursor: pointer;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.author-name {
  font-size: 16px;
}

.author-name:hover,
.comment-author:hover,
.reply-author:hover,
.author-card-name:hover,
.action-item:hover,
.show-replies,
.show-replies:hover,
.load-more span,
.reply-to span {
  color: var(--detail-accent);
}

.post-meta-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--detail-text-subtle);
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.action-btn,
.footer-btn,
.cancel-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--detail-border-strong);
  border-radius: var(--detail-radius-sm);
  background: var(--detail-surface);
  color: var(--detail-text-muted);
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.action-btn {
  padding: 8px 16px;
  font-size: 14px;
}

.action-btn:hover,
.footer-btn:hover,
.cancel-btn:hover {
  border-color: var(--detail-accent);
  color: var(--detail-accent);
}

.action-asset-btn {
  border-color: color-mix(in srgb, var(--detail-accent) 28%, var(--detail-border));
  color: var(--detail-accent);
}

.action-btn.active,
.footer-btn.active,
.submit-btn,
.reply-btn,
.follow-btn,
.ai-btn {
  background: var(--detail-accent);
  border-color: transparent;
  color: white;
}

.action-btn.collected.active {
  background: var(--detail-warning);
}

.action-btn .count {
  padding-left: 4px;
  border-left: 1px solid color-mix(in srgb, white 36%, transparent);
  margin-left: 4px;
}

.ai-analysis-card {
  background: var(--detail-surface-soft);
  border-radius: var(--detail-radius);
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid color-mix(in srgb, var(--detail-accent) 24%, var(--detail-border));
}

.ai-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.ai-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--detail-text);
}

.ai-icon {
  font-size: 20px;
}

.ai-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: var(--detail-radius-sm);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.ai-btn:hover,
.submit-btn:hover:not(:disabled),
.reply-btn:hover:not(:disabled),
.follow-btn:hover {
  box-shadow: 0 4px 12px color-mix(in srgb, var(--detail-accent) 35%, transparent);
}

.ai-btn:hover,
.submit-btn:hover:not(:disabled) {
  transform: scale(1.02);
}

.ai-btn.secondary {
  background: var(--detail-surface);
  color: var(--detail-accent);
  border: 1px solid var(--detail-accent);
}

.ai-btn:disabled,
.submit-btn:disabled,
.reply-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ai-empty p {
  margin: 0;
  text-align: center;
  color: var(--detail-text-subtle);
  font-size: 14px;
  padding: 20px 0;
}

.ai-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 30px 0;
  color: var(--detail-accent);
  font-size: 14px;
}

.ai-summary-text {
  margin: 0 0 16px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--detail-text);
}

.ai-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag,
.tag-item {
  padding: 4px 12px;
  background: var(--detail-surface);
  border-radius: var(--detail-radius-sm);
  font-size: 12px;
  color: var(--detail-accent);
  border: 1px solid color-mix(in srgb, var(--detail-accent) 24%, var(--detail-border));
}

.article-content {
  font-size: 16px;
  line-height: 1.8;
  color: var(--detail-text);
  margin-bottom: 24px;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--detail-border);
}

.tag-item {
  padding: 6px 14px;
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.tag-item:hover {
  background: var(--detail-accent);
  color: white;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid var(--detail-border);
}

.footer-stats,
.footer-actions,
.comment-actions,
.reply-actions {
  display: flex;
}

.footer-stats {
  gap: 20px;
}

.footer-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--detail-text-subtle);
}

.footer-actions {
  gap: 12px;
}

.footer-btn {
  padding: 10px 20px;
  font-size: 14px;
}

.comments-section {
  padding: 24px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--detail-text);
}

.section-header .count {
  color: var(--detail-text-subtle);
  font-weight: 400;
  margin-left: 8px;
}

.comment-form {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--detail-border);
}

.form-avatar {
  width: 40px;
  height: 40px;
  font-size: 16px;
  flex-shrink: 0;
}

.form-content {
  flex: 1;
}

.form-content :deep(.el-textarea__inner) {
  border-radius: var(--detail-radius-sm);
  border-color: var(--detail-border-strong);
}

.form-content :deep(.el-textarea__inner:focus) {
  border-color: var(--detail-accent);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.submit-btn,
.reply-btn,
.follow-btn {
  border: none;
  border-radius: var(--detail-radius-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--cn-motion-base) var(--cn-ease-out);
}

.submit-btn {
  padding: 10px 24px;
  font-size: 14px;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item,
.reply-item {
  display: flex;
}

.comment-item {
  gap: 12px;
}

.comment-avatar {
  width: 40px;
  height: 40px;
  font-size: 14px;
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.comment-author {
  font-size: 14px;
}

.comment-time,
.reply-to,
.reply-time,
.action-item,
.empty-comments,
.info-item .label {
  color: var(--detail-text-subtle);
}

.comment-time,
.reply-to,
.reply-time {
  font-size: 12px;
}

.comment-body,
.reply-body {
  line-height: 1.6;
  color: var(--detail-text);
  white-space: pre-wrap;
}

.comment-body {
  font-size: 15px;
  margin-bottom: 10px;
}

.comment-actions {
  gap: 16px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: color var(--cn-motion-fast) var(--cn-ease-out);
}

.action-item.active {
  color: var(--detail-accent);
}

.reply-form {
  margin-top: 12px;
  padding: 12px;
  background: var(--detail-surface-muted);
  border-radius: var(--detail-radius-sm);
}

.reply-form :deep(.el-textarea__inner) {
  border-radius: 6px;
  background: var(--detail-surface);
}

.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.cancel-btn,
.reply-btn {
  padding: 6px 16px;
  font-size: 13px;
}

.reply-btn:hover:not(:disabled) {
  box-shadow: 0 2px 8px color-mix(in srgb, var(--detail-accent) 35%, transparent);
}

.replies-wrapper {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 2px solid var(--detail-border);
}

.show-replies {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
  margin-bottom: 12px;
}

.show-replies .el-icon {
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.show-replies .el-icon.expanded {
  transform: rotate(180deg);
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  gap: 10px;
}

.reply-avatar {
  width: 32px;
  height: 32px;
  font-size: 12px;
  flex-shrink: 0;
  background: color-mix(in srgb, var(--detail-accent) 82%, var(--detail-surface));
}

.reply-main {
  flex: 1;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.reply-author {
  font-size: 13px;
}

.reply-body {
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.reply-actions {
  gap: 12px;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}

.load-more span {
  font-size: 13px;
  cursor: pointer;
}

.load-more span.loading {
  color: var(--detail-text-subtle);
  cursor: default;
}

.empty-comments {
  text-align: center;
  padding: 40px 20px;
}

.comments-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  padding: 20px;
}

.author-card {
  text-align: center;
}

.author-card-avatar {
  width: 64px;
  height: 64px;
  font-size: 24px;
  margin: 0 auto 12px;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
}

.author-card-name {
  font-size: 16px;
  margin-bottom: 16px;
}

.follow-btn {
  width: 100%;
  padding: 10px;
  font-size: 14px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--detail-text);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--detail-border);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.info-item .value {
  color: var(--detail-text);
}

@media (max-width: 992px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-sidebar {
    display: none;
  }
}

@media (max-width: 768px) {
  .post-detail-container {
    padding: 16px;
  }

  .post-article {
    padding: 20px;
  }

  .article-title {
    font-size: 22px;
  }

  .author-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .action-buttons {
    width: 100%;
  }

  .action-btn {
    flex: 1;
    justify-content: center;
  }

  .article-footer {
    flex-direction: column;
    gap: 16px;
  }

  .footer-actions {
    width: 100%;
  }

  .footer-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>
