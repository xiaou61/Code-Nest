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
        <span>全部评论 ({{ totalComments }})</span>
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
        <el-empty v-if="!loading && commentList.length === 0" description="暂无评论" />
        
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

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getMomentComments, publishComment, deleteComment as deleteCommentApi } from '@/api/moment'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  moment: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'comment-deleted'])

// 响应式数据
const loading = ref(false)
const loadingMore = ref(false)
const commenting = ref(false)
const commentList = ref([])
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

    const result = await getMomentComments(params)
    const newComments = result.records || []

    if (page === 1) {
      commentList.value = newComments
    } else {
      commentList.value.push(...newComments)
    }

    totalComments.value = result.total || 0
    hasMore.value = newComments.length === pageSize.value

  } catch (error) {
    ElMessage.error('加载评论失败：' + error.message)
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

    const commentId = await publishComment(data)

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
    ElMessage.error('评论发表失败：' + error.message)
  } finally {
    commenting.value = false
  }
}

// 删除评论
const handleDeleteComment = async (comment) => {
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
      ElMessage.error('删除失败：' + error.message)
    }
  }
}
</script>

<style scoped>
/* 对话框装饰 */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 50%, #fff5f7 100%);
  border-bottom: 1px solid #e3edfa;
  padding: 16px 20px;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--cn-text-primary, #1a2233);
}

:deep(.el-dialog__body) {
  padding: 16px 20px;
  max-height: 70vh;
  overflow: hidden;
}

/* textarea 美化 */
:deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: #d7e4f8;
  transition: all 0.25s;
}

:deep(.el-textarea__inner:focus) {
  border-color: #6c63ff;
  box-shadow: 0 0 0 3px rgba(108, 99, 255, 0.1);
}

/* 按钮 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
}

:deep(.el-button--primary:hover) {
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.35);
}

.moment-preview {
  background: #faf9ff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 10px;
  border-left: 3px solid #d4ccff;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.user-detail {
  margin-left: 10px;
}

.user-name {
  font-weight: 600;
  color: var(--cn-text-primary, #1a2233);
  font-size: 14px;
}

.publish-time {
  color: #8ea0bd;
  font-size: 12px;
  margin-top: 2px;
}

.moment-content {
  color: var(--cn-text-primary, #1a2233);
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
  box-shadow: 0 8px 16px 8px rgba(255, 255, 255, 0.9);
  z-index: 2;
  pointer-events: none;
}

.comments-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-weight: 600;
  color: var(--cn-text-primary, #1a2233);
}

.comments-header :deep(.el-button) {
  color: #6c63ff;
  font-weight: 600;
}

.comment-form {
  background: #f6f9ff;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 15px;
  border: 1px solid #e3edfa;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.comments-list {
  min-height: 200px;
}

.comment-item {
  display: flex;
  margin-bottom: 4px;
  align-items: flex-start;
  padding: 10px 12px;
  border-radius: 10px;
  transition: background 0.2s;
}

.comment-item:hover {
  background: #f6f9ff;
}

.comment-content {
  flex: 1;
  margin-left: 12px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.comment-user {
  font-weight: 600;
  color: #6c63ff;
  font-size: 14px;
}

.comment-time {
  color: #8ea0bd;
  font-size: 12px;
}

.comment-text {
  color: var(--cn-text-primary, #1a2233);
  line-height: 1.6;
  font-size: 14px;
}

.load-more {
  text-align: center;
  padding: 15px;
}

.load-more :deep(.el-button) {
  color: #6a82ae;
  font-weight: 500;
}

.load-more :deep(.el-button:hover) {
  color: #6c63ff;
}

:deep(.el-divider) {
  margin: 15px 0;
  border-color: #e8eef8;
}
</style>
