<template>
  <div class="discussion-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>

    <CnEmptyState
      v-else-if="discussions.length === 0"
      title="暂无讨论"
      description="发布讨论后，小组成员可以在这里交流经验和问题。"
      icon="DS"
      size="sm"
      surface="transparent"
    />

    <div v-else class="discussions">
      <article v-for="item in discussions" :key="item.id" class="discussion-item" @click="viewDetail(item)">
        <div class="discussion-header">
          <div class="author-info">
            <div class="author-avatar">
              <img v-if="item.userAvatar" :src="item.userAvatar" :alt="item.userName || '作者头像'" />
              <span v-else>{{ item.userName?.charAt(0) || '用' }}</span>
            </div>
            <span class="author-name">{{ item.userName || '匿名成员' }}</span>
            <span class="post-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="discussion-badges">
            <CnStatusTag v-if="item.isTop" type="brand" size="sm" :dot="false">置顶</CnStatusTag>
            <CnStatusTag v-if="item.isEssence" type="warning" size="sm" :dot="false">精华</CnStatusTag>
          </div>
        </div>

        <h3 class="discussion-title">{{ item.title || '未命名讨论' }}</h3>
        <p class="discussion-content">{{ truncateContent(item.content) }}</p>

        <div class="discussion-footer">
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ item.viewCount || 0 }}
          </span>
          <button class="meta-item like" :class="{ liked: item.isLiked }" type="button" @click.stop="handleLike(item)">
            <el-icon><Star /></el-icon>
            {{ item.likeCount || 0 }}
          </button>
          <span class="meta-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ item.replyCount || 0 }}
          </span>

          <el-dropdown v-if="isAdmin" trigger="click" @click.stop>
            <el-button text size="small" aria-label="讨论操作">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click.stop="toggleTop(item)">
                  {{ item.isTop ? '取消置顶' : '设为置顶' }}
                </el-dropdown-item>
                <el-dropdown-item @click.stop="toggleEssence(item)">
                  {{ item.isEssence ? '取消精华' : '设为精华' }}
                </el-dropdown-item>
                <el-dropdown-item @click.stop="handleDelete(item)" divided>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>
    </div>

    <div v-if="hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">加载更多</el-button>
    </div>

    <el-dialog v-model="showDetailDialog" :title="currentDiscussion?.title" width="600px">
      <div v-if="currentDiscussion" class="discussion-detail">
        <div class="detail-author">
          <div class="author-avatar">
            <img v-if="currentDiscussion.userAvatar" :src="currentDiscussion.userAvatar" :alt="currentDiscussion.userName || '作者头像'" />
            <span v-else>{{ currentDiscussion.userName?.charAt(0) || '用' }}</span>
          </div>
          <div class="author-copy">
            <span class="author-name">{{ currentDiscussion.userName || '匿名成员' }}</span>
            <span class="post-time">{{ formatTime(currentDiscussion.createTime) }}</span>
          </div>
        </div>
        <div class="detail-content" v-html="sanitizeDiscussionContent(currentDiscussion.content)" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Delete, MoreFilled, Star, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'
import { sanitizeHtml } from '@/utils/markdown'

interface DiscussionRecord {
  id: number | string
  title?: string
  content?: string
  userAvatar?: string
  userName?: string
  createTime?: string
  isTop?: boolean | number
  isEssence?: boolean | number
  viewCount?: number
  isLiked?: boolean
  likeCount?: number
  replyCount?: number
}

const props = withDefaults(
  defineProps<{
    teamId: string | number
    isAdmin?: boolean
  }>(),
  {
    isAdmin: false
  }
)

const discussions = ref<DiscussionRecord[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)

const showDetailDialog = ref(false)
const currentDiscussion = ref<DiscussionRecord | null>(null)

onMounted(() => {
  loadDiscussions()
})

watch(
  () => props.teamId,
  () => {
    pageNum.value = 1
    loadDiscussions()
  }
)

const sanitizeDiscussionContent = (content?: string) => {
  if (!content) return ''
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text: string) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const loadDiscussions = async () => {
  loading.value = true
  try {
    const response = (await teamApi.getDiscussionList(props.teamId, {
      pageNum: pageNum.value,
      pageSize: 10
    })) as DiscussionRecord[]
    discussions.value = response || []
    hasMore.value = discussions.value.length === 10
  } catch (error) {
    console.error('加载讨论列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  loadingMore.value = true
  pageNum.value++
  try {
    const response = (await teamApi.getDiscussionList(props.teamId, {
      pageNum: pageNum.value,
      pageSize: 10
    })) as DiscussionRecord[]
    const newItems = response || []
    discussions.value = [...discussions.value, ...newItems]
    hasMore.value = newItems.length === 10
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

const viewDetail = async (item: DiscussionRecord) => {
  try {
    currentDiscussion.value = (await teamApi.getDiscussionDetail(item.id)) as DiscussionRecord
    showDetailDialog.value = true
  } catch (error) {
    console.error('加载详情失败:', error)
  }
}

const handleLike = async (item: DiscussionRecord) => {
  try {
    if (item.isLiked) {
      await teamApi.unlikeDiscussion(item.id)
      item.isLiked = false
      item.likeCount = Math.max(0, (item.likeCount || 1) - 1)
    } else {
      await teamApi.likeDiscussion(item.id)
      item.isLiked = true
      item.likeCount = (item.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const toggleTop = async (item: DiscussionRecord) => {
  try {
    await teamApi.setDiscussionTop(item.id, item.isTop ? 0 : 1)
    item.isTop = !item.isTop
    ElMessage.success(item.isTop ? '已置顶' : '已取消置顶')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const toggleEssence = async (item: DiscussionRecord) => {
  try {
    await teamApi.setDiscussionEssence(item.id, item.isEssence ? 0 : 1)
    item.isEssence = !item.isEssence
    ElMessage.success(item.isEssence ? '已设为精华' : '已取消精华')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (item: DiscussionRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除这个讨论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.deleteDiscussion(item.id)
    ElMessage.success('删除成功')
    loadDiscussions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const truncateContent = (content?: string) => {
  if (!content) return ''
  const text = content.replace(/<[^>]+>/g, '')
  return text.length > 100 ? `${text.substring(0, 100)}...` : text
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 172800000) return '昨天'

  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

defineExpose({ loadDiscussions })
</script>

<style scoped>
.discussion-list {
  min-width: 0;
}

.loading-state {
  padding: var(--cn-space-5) 0;
}

.discussions {
  display: grid;
}

.discussion-item {
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  cursor: pointer;
  transition: background-color var(--cn-motion-base) var(--cn-ease-out);
}

.discussion-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.discussion-item + .discussion-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.discussion-header,
.author-info,
.discussion-badges,
.discussion-footer,
.detail-author {
  display: flex;
  align-items: center;
}

.discussion-header {
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
}

.author-info {
  gap: var(--cn-space-2);
  min-width: 0;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  overflow: hidden;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 800;
}

.detail-author .author-avatar {
  width: 42px;
  height: 42px;
  font-size: 16px;
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
}

.post-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.discussion-badges {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.discussion-title {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.discussion-content {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.discussion-footer {
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-3);
}

.discussion-footer .el-dropdown {
  margin-left: auto;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  font-weight: 650;
}

button.meta-item {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.meta-item.liked,
.meta-item.like:hover {
  color: var(--cn-color-danger);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-3) 0;
}

.discussion-detail {
  display: grid;
  gap: var(--cn-space-4);
}

.detail-author {
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.author-copy {
  display: grid;
  gap: var(--cn-space-1);
}

.detail-content {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  line-height: 1.8;
  overflow-wrap: anywhere;
}
</style>
