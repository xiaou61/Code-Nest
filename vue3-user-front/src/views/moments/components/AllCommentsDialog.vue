<template>
  <el-dialog
    v-model="visible"
    title="全部评论"
    width="600px"
    :before-close="handleClose"
  >
    <div v-if="moment" class="moment-preview">
      <div class="user-info">
        <el-avatar :size="40" :src="moment.userAvatar">
          {{ moment.userNickname?.charAt(0) }}
        </el-avatar>
        <div class="user-detail">
          <div class="user-name">{{ moment.userNickname }}</div>
          <div class="publish-time">{{ moment.createTime }}</div>
        </div>
      </div>
      <div class="moment-content">
        {{ moment.content }}
      </div>
    </div>

    <el-divider />

    <div class="comments-section">
      <div class="comments-header">
        <span>全部评论</span>
        <CnStatusTag type="info" size="sm">共 {{ totalComments }} 条</CnStatusTag>
        <el-button type="primary" text @click="showCommentForm = !showCommentForm">
          <el-icon><ChatDotRound /></el-icon>
          写评论
        </el-button>
      </div>

      <!-- 写评论 -->
      <div v-if="showCommentForm" class="comment-form">
        <el-input
          v-model="newComment"
          type="textarea"
          placeholder="写下你的评论..."
          :rows="3"
          maxlength="200"
          show-word-limit
        />
        <div class="comment-actions">
          <el-button size="small" @click="showCommentForm = false">取消</el-button>
          <el-button 
            type="primary" 
            size="small" 
            @click="submitComment" 
            :loading="commenting"
          >
            发表
          </el-button>
        </div>
      </div>

      <!-- 评论列表 -->
      <div v-loading="loading" class="comments-list">
        <CnEmptyState v-if="!loading && commentList.length === 0" title="暂无评论" icon="CM" size="sm" surface="transparent" />
        
        <div 
          v-for="comment in commentList" 
          :key="comment.id" 
          class="comment-item"
        >
          <el-avatar :size="36" :src="comment.userAvatar">
            {{ comment.userNickname?.charAt(0) }}
          </el-avatar>
          <div class="comment-content">
            <div class="comment-header">
              <span class="comment-user">{{ comment.userNickname }}</span>
              <span class="comment-time">{{ comment.createTime }}</span>
              <el-button 
                v-if="comment.canDelete" 
                type="danger" 
                text 
                size="small"
                @click="handleDeleteComment(comment)"
              >
                删除
              </el-button>
            </div>
            <div class="comment-text">{{ comment.content }}</div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <el-button @click="loadMore" :loading="loadingMore" text>
            加载更多评论
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatusTag } from '@/design-system'
import { getMomentComments, publishComment, deleteComment as deleteCommentApi } from '@/api/moment'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface MomentPreview {
  id: number
  content?: string
  createTime?: string
  userAvatar?: string
  userNickname?: string
}

interface MomentComment {
  id: number
  content: string
  createTime?: string
  userAvatar?: string
  userNickname?: string
  canDelete?: boolean
}

interface CommentPageResult {
  records?: MomentComment[]
  total?: number
}

const props = withDefaults(defineProps<{
  modelValue?: boolean
  moment?: MomentPreview | null
}>(), {
  modelValue: false,
  moment: null
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'comment-deleted', commentId: number): void
}>()

// 响应式数据
const loading = ref(false)
const loadingMore = ref(false)
const commenting = ref(false)
const commentList = ref<MomentComment[]>([])
const totalComments = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const hasMore = ref(true)

// 评论表单
const showCommentForm = ref(false)
const newComment = ref('')

// 对话框可见性
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 监听对话框打开，加载评论
watch(visible, (newVal) => {
  if (newVal && props.moment) {
    resetData()
    loadComments()
  }
})

// 重置数据
const resetData = () => {
  commentList.value = []
  totalComments.value = 0
  currentPage.value = 1
  hasMore.value = true
  showCommentForm.value = false
  newComment.value = ''
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}

// 加载评论列表
const getErrorMessage = (error: unknown, fallback: string) => {
  return error instanceof Error ? error.message : fallback
}

const loadComments = async (page = 1) => {
  if (!props.moment) return

  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      momentId: props.moment.id,
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getMomentComments(params) as CommentPageResult
    const newComments = result.records || []

    if (page === 1) {
      commentList.value = newComments
    } else {
      commentList.value.push(...newComments)
    }

    totalComments.value = result.total || 0
    hasMore.value = newComments.length === pageSize.value

  } catch (error) {
    ElMessage.error('加载评论失败：' + getErrorMessage(error, '请稍后重试'))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多评论
const loadMore = () => {
  currentPage.value++
  loadComments(currentPage.value)
}

// 提交评论
const submitComment = async () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  if (newComment.value.length > 200) {
    ElMessage.warning('评论内容不能超过200字符')
    return
  }

  commenting.value = true

  try {
    const data = {
      momentId: props.moment.id,
      content: newComment.value.trim()
    }

    const commentId = await publishComment(data) as number

    ElMessage.success('评论发表成功')

    // 构造新评论对象
    const comment = {
      id: commentId,
      content: newComment.value.trim(),
      userNickname: userStore.realName || userStore.username,
      userAvatar: userStore.avatar,
      createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      canDelete: true
    }

    // 添加到评论列表顶部
    commentList.value.unshift(comment)
    totalComments.value++
    
    // 重置表单
    newComment.value = ''
    showCommentForm.value = false

  } catch (error) {
    ElMessage.error('评论发表失败：' + getErrorMessage(error, '请稍后重试'))
  } finally {
    commenting.value = false
  }
}

// 删除评论
const handleDeleteComment = async (comment: MomentComment) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条评论吗？',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteCommentApi(comment.id)
    ElMessage.success('删除成功')

    // 从列表中移除
    const index = commentList.value.findIndex(item => item.id === comment.id)
    if (index > -1) {
      commentList.value.splice(index, 1)
      totalComments.value--
      
      // 通知父组件
      emit('comment-deleted', comment.id)
    }

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + getErrorMessage(error, '请稍后重试'))
    }
  }
}
</script>

<style scoped>
:deep(.el-dialog) {
  border-radius: var(--cn-radius-panel);
  overflow: hidden;
  background: var(--cn-color-bg-surface);
}

:deep(.el-dialog__header) {
  background: var(--cn-color-bg-surface-muted);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  padding: var(--cn-space-4) var(--cn-space-5);
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--cn-color-text-primary);
}

:deep(.el-dialog__body) {
  padding: var(--cn-space-4) var(--cn-space-5);
  max-height: 70vh;
  overflow: hidden;
}

:deep(.el-textarea__inner) {
  border-radius: var(--cn-radius-control);
  border-color: var(--cn-color-border);
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--cn-color-brand-primary);
  box-shadow: 0 0 0 3px var(--cn-color-focus-ring);
}

.moment-preview {
  background: var(--cn-color-bg-surface-muted);
  border: 1px solid var(--cn-color-border-subtle);
  border-left: 3px solid var(--cn-color-brand-primary);
  border-radius: var(--cn-radius-card);
  padding: var(--cn-space-4);
  margin-bottom: var(--cn-space-3);
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: var(--cn-space-3);
}

.user-detail {
  margin-left: var(--cn-space-3);
}

.user-name {
  font-weight: 600;
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.publish-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  margin-top: var(--cn-space-1);
}

.moment-content {
  color: var(--cn-color-text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.comments-section {
  max-height: 500px;
  overflow-y: auto;
  position: relative;
}

.comments-section::before {
  content: '';
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  display: block;
  height: 1px;
  box-shadow: 0 8px 16px 8px color-mix(in srgb, var(--cn-color-bg-surface) 90%, transparent);
  z-index: 2;
  pointer-events: none;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
  font-weight: 600;
  color: var(--cn-color-text-primary);
}

.comment-form {
  background: var(--cn-color-bg-surface-muted);
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  margin-bottom: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.comments-list {
  min-height: 200px;
}

.comment-item {
  display: flex;
  margin-bottom: var(--cn-space-1);
  align-items: flex-start;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  transition: background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.comment-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.comment-content {
  flex: 1;
  margin-left: var(--cn-space-3);
}

.comment-header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-2);
}

.comment-user {
  font-weight: 600;
  color: var(--cn-color-brand-primary);
  font-size: 14px;
}

.comment-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.comment-text {
  color: var(--cn-color-text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.load-more {
  text-align: center;
  padding: var(--cn-space-4);
}

.load-more :deep(.el-button) {
  color: var(--cn-color-text-secondary);
  font-weight: 500;
}

.load-more :deep(.el-button:hover) {
  color: var(--cn-color-brand-primary);
}

:deep(.el-divider) {
  margin: var(--cn-space-4) 0;
  border-color: var(--cn-color-border-subtle);
}
</style>
