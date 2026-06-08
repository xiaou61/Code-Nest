<template>
  <CnPage max-width="1180px" full-height>
    <CnPageHeader
      title="通知中心"
      description="集中查看系统通知、社区互动和个人消息。"
      eyebrow="NOTIFICATIONS"
    >
      <template #meta>
        <CnStatusTag :type="unreadCount > 0 ? 'warning' : 'success'" size="sm">
          {{ unreadCount > 0 ? `${unreadCount} 条未读` : '全部已读' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>共 {{ totalCount }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Picture" @click="goToMoments">朋友圈</el-button>
        <el-button :icon="HomeFilled" @click="goToHome">返回首页</el-button>
        <el-button type="primary" :disabled="unreadCount === 0" @click="markAllRead">全部已读</el-button>
      </template>
    </CnPageHeader>

    <div class="stats-grid">
      <CnStatCard title="总消息" :value="totalCount" description="当前筛选范围内消息数" tone="brand" />
      <CnStatCard title="未读消息" :value="unreadCount" description="需要处理的通知" tone="warning" />
      <CnStatCard title="已读消息" :value="readCount" description="已经查看过的通知" tone="success" />
    </div>

    <CnSection title="筛选消息" description="按标题、状态、类型和日期范围定位通知。" divided>
      <CnFilterForm
        v-model="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @search="handleSearch"
        @reset="resetSearch"
      />
    </CnSection>

    <CnSection title="消息列表" description="点击消息可查看详情，未读消息会自动标记为已读。" divided>
      <div v-loading="loading" class="messages-container">
        <CnEmptyState
          v-if="messageList.length === 0 && !loading"
          title="暂无消息"
          description="当前条件下没有可展示的通知。"
          icon="MSG"
        />

        <article
          v-for="message in messageList"
          :key="message.id"
          class="message-item"
          :class="{ 'is-unread': message.status === 'UNREAD' }"
          @click="viewMessageDetail(message)"
        >
          <div class="message-avatar" :class="`message-avatar--${getTypeClass(message.type)}`">
            <el-icon>
              <component :is="getMessageIcon(message.type)" />
            </el-icon>
          </div>

          <div class="message-content">
            <div class="message-header">
              <div class="message-title-row">
                <h3 class="message-title">{{ message.title }}</h3>
                <CnStatusTag v-if="message.status === 'UNREAD'" type="warning" size="sm">未读</CnStatusTag>
              </div>

              <div class="message-meta">
                <CnStatusTag :type="getTypeTone(message.type)" size="sm">
                  {{ getTypeText(message.type) }}
                </CnStatusTag>
                <CnStatusTag
                  v-if="message.priority && message.priority !== 'LOW'"
                  :type="getPriorityTone(message.priority)"
                  size="sm"
                  subtle
                >
                  {{ getPriorityText(message.priority) }}
                </CnStatusTag>
                <span class="message-time">{{ formatTime(message.createdTime || message.createTime) }}</span>
              </div>
            </div>

            <p class="message-preview">{{ message.content }}</p>
          </div>

          <div class="message-actions" @click.stop>
            <el-dropdown @command="handleMessageAction">
              <el-button text class="action-btn" :icon="MoreFilled" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-if="message.status === 'UNREAD'"
                    :command="{ action: 'markRead', id: message.id }"
                  >
                    标记已读
                  </el-dropdown-item>
                  <el-dropdown-item :command="{ action: 'delete', id: message.id }">
                    删除消息
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </article>
      </div>

      <div v-if="totalCount > 0" class="pagination-container">
        <el-pagination
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="totalCount"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @update:current-page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </CnSection>

    <el-dialog v-model="detailDialogVisible" title="消息详情" width="640px" @closed="currentMessage = null">
      <div v-if="currentMessage" class="message-detail">
        <div class="detail-header">
          <h3>{{ currentMessage.title }}</h3>
          <div class="detail-meta">
            <CnStatusTag :type="getTypeTone(currentMessage.type)" size="sm">
              {{ getTypeText(currentMessage.type) }}
            </CnStatusTag>
            <CnStatusTag
              v-if="currentMessage.priority && currentMessage.priority !== 'LOW'"
              :type="getPriorityTone(currentMessage.priority)"
              size="sm"
              subtle
            >
              {{ getPriorityText(currentMessage.priority) }}
            </CnStatusTag>
            <span class="detail-time">{{ formatTime(currentMessage.createTime || currentMessage.createdTime) }}</span>
          </div>
        </div>

        <div class="detail-content" v-html="formatContent(currentMessage.content)" />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="currentMessage && currentMessage.status === 'UNREAD'"
            type="primary"
            @click="markMessageAsRead(currentMessage.id)"
          >
            标记已读
          </el-button>
        </span>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  Bell,
  ChatDotSquare,
  HomeFilled,
  Message,
  MoreFilled,
  Notification,
  Picture,
  User
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnFilterField,
  type CnTone
} from '@/design-system'
import { sanitizeHtml } from '@/utils/markdown'
import {
  deleteMessage,
  getMessageDetail,
  getMessages,
  getUnreadCount,
  markAllAsRead,
  markAsRead
} from '@/api/notification'

interface NotificationMessage {
  id: number | string
  title?: string
  content?: string
  status?: 'UNREAD' | 'READ' | string
  type?: string
  priority?: string
  createdTime?: string
  createTime?: string
}

interface MessageQueryResponse {
  records?: NotificationMessage[]
  total?: number
}

interface MessageActionCommand {
  action: 'markRead' | 'delete'
  id: number | string
}

const router = useRouter()

const loading = ref(false)
const messageList = ref<NotificationMessage[]>([])
const totalCount = ref(0)
const unreadCount = ref(0)

const searchForm = ref<Record<string, unknown>>({
  title: '',
  status: '',
  type: '',
  dateRange: []
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

const detailDialogVisible = ref(false)
const currentMessage = ref<NotificationMessage | null>(null)

const readCount = computed(() => Math.max(totalCount.value - unreadCount.value, 0))

const filterFields: CnFilterField[] = [
  {
    prop: 'title',
    label: '消息标题',
    type: 'input',
    placeholder: '搜索消息标题'
  },
  {
    prop: 'status',
    label: '消息状态',
    type: 'select',
    placeholder: '全部状态',
    options: [
      { label: '未读', value: 'UNREAD' },
      { label: '已读', value: 'READ' }
    ]
  },
  {
    prop: 'type',
    label: '消息类型',
    type: 'select',
    placeholder: '全部类型',
    options: [
      { label: '个人消息', value: 'PERSONAL' },
      { label: '系统公告', value: 'ANNOUNCEMENT' },
      { label: '社区互动', value: 'COMMUNITY_INTERACTION' },
      { label: '系统通知', value: 'SYSTEM' }
    ]
  },
  {
    prop: 'dateRange',
    label: '日期范围',
    type: 'daterange',
    placeholder: '开始日期'
  }
]

onMounted(() => {
  searchMessages()
  getUnreadCountData()
})

const searchMessages = async () => {
  try {
    loading.value = true

    const params: Record<string, unknown> = {
      ...searchForm.value,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }

    const dateRange = searchForm.value.dateRange
    if (Array.isArray(dateRange) && dateRange.length === 2) {
      params.startTime = dateRange[0]
      params.endTime = dateRange[1]
    }

    const response = (await getMessages(params)) as MessageQueryResponse
    messageList.value = response.records || []
    totalCount.value = response.total || 0
  } catch (error) {
    ElMessage.error(`获取消息列表失败：${getErrorMessage(error)}`)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  searchMessages()
}

const resetSearch = () => {
  pagination.pageNum = 1
  searchForm.value = {
    title: '',
    status: '',
    type: '',
    dateRange: []
  }
  searchMessages()
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  searchMessages()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageNum = 1
  pagination.pageSize = size
  searchMessages()
}

const getUnreadCountData = async () => {
  try {
    const response = (await getUnreadCount()) as number
    unreadCount.value = response || 0
  } catch (error) {
    console.error('获取未读消息数量失败：', getErrorMessage(error))
  }
}

const viewMessageDetail = async (message: NotificationMessage) => {
  try {
    const response = (await getMessageDetail(message.id)) as NotificationMessage
    currentMessage.value = response
    detailDialogVisible.value = true

    if (message.status === 'UNREAD') {
      await markMessageAsRead(message.id, false)
    }
  } catch (error) {
    ElMessage.error(`获取消息详情失败：${getErrorMessage(error)}`)
  }
}

const markMessageAsRead = async (messageId: number | string, showMessage = true) => {
  try {
    await markAsRead({ messageId })
    if (showMessage) {
      ElMessage.success('已标记为已读')
    }

    const messageIndex = messageList.value.findIndex((message) => message.id === messageId)
    if (messageIndex !== -1) {
      messageList.value[messageIndex].status = 'READ'
    }

    if (currentMessage.value && currentMessage.value.id === messageId) {
      currentMessage.value.status = 'READ'
    }

    getUnreadCountData()
  } catch (error) {
    ElMessage.error(`标记已读失败：${getErrorMessage(error)}`)
  }
}

const deleteMessageById = async (messageId: number | string) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMessage({ messageIds: [messageId] })
    ElMessage.success('删除成功')

    const messageIndex = messageList.value.findIndex((message) => message.id === messageId)
    if (messageIndex !== -1) {
      messageList.value.splice(messageIndex, 1)
      totalCount.value = Math.max(totalCount.value - 1, 0)
    }

    getUnreadCountData()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(`删除失败：${getErrorMessage(error)}`)
  }
}

const markAllRead = async () => {
  try {
    await markAllAsRead()
    ElMessage.success('全部消息已标记为已读')

    messageList.value.forEach((message) => {
      message.status = 'READ'
    })

    getUnreadCountData()
  } catch (error) {
    ElMessage.error(`标记全部已读失败：${getErrorMessage(error)}`)
  }
}

const handleMessageAction = ({ action, id }: MessageActionCommand) => {
  if (action === 'markRead') {
    markMessageAsRead(id)
  } else if (action === 'delete') {
    deleteMessageById(id)
  }
}

const goToHome = () => {
  router.push('/')
}

const goToMoments = () => {
  router.push('/moments')
}

const getMessageIcon = (type?: string): Component => {
  const iconMap: Record<string, Component> = {
    PERSONAL: User,
    ANNOUNCEMENT: Notification,
    COMMUNITY_INTERACTION: ChatDotSquare,
    SYSTEM: Bell
  }
  return type ? iconMap[type] || Message : Message
}

const getTypeText = (type?: string) => {
  const typeMap: Record<string, string> = {
    PERSONAL: '个人消息',
    ANNOUNCEMENT: '系统公告',
    COMMUNITY_INTERACTION: '社区互动',
    SYSTEM: '系统通知'
  }
  return type ? typeMap[type] || type : '消息'
}

const getTypeClass = (type?: string) => {
  const classMap: Record<string, string> = {
    PERSONAL: 'personal',
    ANNOUNCEMENT: 'announcement',
    COMMUNITY_INTERACTION: 'community',
    SYSTEM: 'system'
  }
  return type ? classMap[type] || 'system' : 'system'
}

const getTypeTone = (type?: string): CnTone => {
  const typeMap: Record<string, CnTone> = {
    PERSONAL: 'brand',
    ANNOUNCEMENT: 'success',
    COMMUNITY_INTERACTION: 'warning',
    SYSTEM: 'info'
  }
  return type ? typeMap[type] || 'neutral' : 'neutral'
}

const getPriorityText = (priority?: string) => {
  const priorityMap: Record<string, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return priority ? priorityMap[priority] || priority : ''
}

const getPriorityTone = (priority?: string): CnTone => {
  const priorityMap: Record<string, CnTone> = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return priority ? priorityMap[priority] || 'neutral' : 'neutral'
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60 * 1000) {
    return '刚刚'
  }

  if (diff < 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 1000))}分钟前`
  }

  if (diff < 24 * 60 * 60 * 1000) {
    return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  }

  return date.toLocaleDateString()
}

const formatContent = (content?: string) => {
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

const getErrorMessage = (error: unknown) => {
  return error instanceof Error ? error.message : '未知错误'
}
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.messages-container {
  display: grid;
  gap: var(--cn-space-3);
  min-height: 320px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.message-item:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
}

.message-item.is-unread {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 32%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-bg-surface) 86%, var(--cn-color-brand-soft));
}

.message-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: var(--cn-radius-panel);
  color: var(--cn-color-bg-surface);
  flex-shrink: 0;
}

.message-avatar--personal {
  background: var(--cn-color-brand-primary);
}

.message-avatar--announcement {
  background: var(--cn-color-success);
}

.message-avatar--community {
  background: var(--cn-color-warning);
}

.message-avatar--system {
  background: var(--cn-color-info);
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: grid;
  gap: var(--cn-space-2);
}

.message-title-row,
.message-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.message-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}

.message-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 600;
}

.message-preview {
  display: -webkit-box;
  margin: var(--cn-space-3) 0 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.message-actions {
  flex-shrink: 0;
}

.action-btn {
  color: var(--cn-color-text-secondary);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--cn-space-5);
  overflow-x: auto;
}

.message-detail {
  display: grid;
  gap: var(--cn-space-4);
}

.detail-header {
  display: grid;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.detail-header h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 750;
  line-height: 1.4;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.detail-time {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.detail-content {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.dialog-footer {
  display: inline-flex;
  gap: var(--cn-space-2);
}

@media (max-width: 820px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .message-item {
    display: grid;
  }

  .message-actions {
    justify-self: start;
  }

  .pagination-container {
    justify-content: flex-start;
  }
}
</style>
