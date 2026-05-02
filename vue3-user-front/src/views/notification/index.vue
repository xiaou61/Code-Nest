<template>
  <div class="notification-center">
    <div class="container">
      <!-- 页面头部 -->
      <div class="header-section">
        <h1>通知中心</h1>
        <div class="header-actions">
          <el-button @click="goToMoments" icon="Picture">
            朋友圈
          </el-button>
          <el-button @click="goToHome" type="default" icon="HomeFilled">
            返回首页
          </el-button>
          <el-button @click="markAllRead" type="primary" :disabled="unreadCount === 0">
            全部已读
          </el-button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-section">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-icon">
                <el-icon><Message /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ totalCount }}</div>
                <div class="stat-label">总消息</div>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-icon unread">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ unreadCount }}</div>
                <div class="stat-label">未读消息</div>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card">
              <div class="stat-icon read">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ totalCount - unreadCount }}</div>
                <div class="stat-label">已读消息</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 筛选和搜索 -->
      <div class="filter-section">
        <el-card shadow="never">
          <el-row :gutter="20" class="filter-row">
            <el-col :span="6">
              <el-input 
                v-model="searchForm.title" 
                placeholder="搜索消息标题"
                clearable
                @clear="searchMessages"
                @keyup.enter="searchMessages"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col :span="4">
              <el-select 
                v-model="searchForm.status" 
                placeholder="消息状态" 
                clearable
                @change="searchMessages"
              >
                <el-option label="全部" value="" />
                <el-option label="未读" value="UNREAD" />
                <el-option label="已读" value="READ" />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-select 
                v-model="searchForm.type" 
                placeholder="消息类型" 
                clearable
                @change="searchMessages"
              >
                <el-option label="全部" value="" />
                <el-option label="个人消息" value="PERSONAL" />
                <el-option label="系统公告" value="ANNOUNCEMENT" />
                <el-option label="社区互动" value="COMMUNITY_INTERACTION" />
                <el-option label="系统通知" value="SYSTEM" />
              </el-select>
            </el-col>
            <el-col :span="6">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="searchMessages"
                size="default"
              />
            </el-col>
            <el-col :span="4">
              <el-button type="primary" @click="searchMessages">搜索</el-button>
            </el-col>
          </el-row>
        </el-card>
      </div>

      <!-- 消息列表 -->
      <div class="messages-section">
        <el-card shadow="never">
          <div v-loading="loading" class="messages-container">
            <div v-if="messageList.length === 0 && !loading" class="empty-state">
              <el-empty description="暂无消息" />
            </div>
            
            <div 
              v-for="message in messageList" 
              :key="message.id" 
              class="message-item"
              :class="{ 'unread': message.status === 'UNREAD' }"
              @click="viewMessageDetail(message)"
            >
              <div class="message-avatar">
                <el-icon>
                  <component :is="getMessageIcon(message.type)" />
                </el-icon>
              </div>
              
              <div class="message-content">
                <div class="message-header">
                  <div class="message-title">{{ message.title }}</div>
                  <div class="message-meta">
                    <el-tag 
                      :type="getTypeTagType(message.type)" 
                      size="small"
                    >
                      {{ getTypeText(message.type) }}
                    </el-tag>
                    <el-tag 
                      v-if="message.priority && message.priority !== 'LOW'" 
                      :type="getPriorityTagType(message.priority)" 
                      size="small"
                    >
                      {{ getPriorityText(message.priority) }}
                    </el-tag>
                    <span class="message-time">{{ formatTime(message.createdTime) }}</span>
                  </div>
                </div>
                
                <div class="message-preview">
                  {{ message.content }}
                </div>
              </div>
              
              <div class="message-actions">
                <el-dropdown @command="handleMessageAction">
                  <el-button type="text" class="action-btn">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item 
                        v-if="message.status === 'UNREAD'" 
                        :command="{action: 'markRead', id: message.id}"
                      >
                        标记已读
                      </el-dropdown-item>
                      <el-dropdown-item :command="{action: 'delete', id: message.id}">
                        删除消息
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
          
          <!-- 分页 -->
          <div class="pagination-container" v-if="totalCount > 0">
            <el-pagination
              v-model:current-page="pagination.pageNum"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="totalCount"
              background
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="searchMessages"
              @current-change="searchMessages"
            />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 消息详情对话框 -->
    <el-dialog 
      v-model="detailDialogVisible" 
      title="消息详情" 
      width="600px"
      :before-close="handleDetailClose"
    >
      <div v-if="currentMessage" class="message-detail">
        <div class="detail-header">
          <h3>{{ currentMessage.title }}</h3>
          <div class="detail-meta">
            <el-tag 
              :type="getTypeTagType(currentMessage.type)" 
              size="small"
            >
              {{ getTypeText(currentMessage.type) }}
            </el-tag>
            <el-tag 
              v-if="currentMessage.priority && currentMessage.priority !== 'LOW'" 
              :type="getPriorityTagType(currentMessage.priority)" 
              size="small"
            >
              {{ getPriorityText(currentMessage.priority) }}
            </el-tag>
            <span class="detail-time">{{ formatTime(currentMessage.createTime) }}</span>
          </div>
        </div>
        
        <div class="detail-content">
          <div v-html="formatContent(currentMessage.content)"></div>
        </div>
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


  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { 
  Message, 
  Bell, 
  CircleCheck, 
  Search, 
  MoreFilled,
  User,
  Notification,
  ChatDotSquare,
  HomeFilled,
  Picture
} from '@element-plus/icons-vue'
import { sanitizeHtml } from '@/utils/markdown'
import { 
  getMessages, 
  getUnreadCount, 
  getMessageDetail, 
  markAsRead, 
  deleteMessage, 
  markAllAsRead
} from '@/api/notification'

// 路由实例
const router = useRouter()

// 数据状态
const loading = ref(false)
const messageList = ref([])
const totalCount = ref(0)
const unreadCount = ref(0)

// 搜索表单
const searchForm = reactive({
  title: '',
  status: '',
  type: '',
  dateRange: [],
  pageNum: 1,
  pageSize: 10
})

// 分页信息
const pagination = reactive({
  pageNum: 1,
  pageSize: 10
})

// 消息详情
const detailDialogVisible = ref(false)
const currentMessage = ref(null)

// 获取消息列表
const searchMessages = async () => {
  try {
    loading.value = true
    
    const params = {
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }
    
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startTime = searchForm.dateRange[0]
      params.endTime = searchForm.dateRange[1]
    }
    
    const response = await getMessages(params)
    messageList.value = response.records
    totalCount.value = response.total
  } catch (error) {
    ElMessage.error('获取消息列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 获取未读消息数量
const getUnreadCountData = async () => {
  try {
    const response = await getUnreadCount()
    unreadCount.value = response
  } catch (error) {
    console.error('获取未读消息数量失败：', error.message)
  }
}

// 查看消息详情
const viewMessageDetail = async (message) => {
  try {
    const response = await getMessageDetail(message.id)
    currentMessage.value = response
    detailDialogVisible.value = true
    
    // 如果是未读消息，自动标记为已读
    if (message.status === 'UNREAD') {
      await markMessageAsRead(message.id, false)
    }
  } catch (error) {
    ElMessage.error('获取消息详情失败：' + error.message)
  }
}

// 标记消息已读
const markMessageAsRead = async (messageId, showMessage = true) => {
  try {
    await markAsRead({ messageId })
    if (showMessage) {
      ElMessage.success('已标记为已读')
    }
    
    // 更新列表中的消息状态
    const messageIndex = messageList.value.findIndex(m => m.id === messageId)
    if (messageIndex !== -1) {
      messageList.value[messageIndex].status = 'READ'
    }
    
    // 更新当前消息状态
    if (currentMessage.value && currentMessage.value.id === messageId) {
      currentMessage.value.status = 'READ'
    }
    
    // 更新未读数量
    getUnreadCountData()
  } catch (error) {
    ElMessage.error('标记已读失败：' + error.message)
  }
}

// 删除消息
const deleteMessageById = async (messageId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await deleteMessage({ messageIds: [messageId] })
    ElMessage.success('删除成功')
    
    // 从列表中移除
    const messageIndex = messageList.value.findIndex(m => m.id === messageId)
    if (messageIndex !== -1) {
      messageList.value.splice(messageIndex, 1)
      totalCount.value--
    }
    
    getUnreadCountData()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error('删除失败：' + error.message)
  }
}

// 全部标记已读
const markAllRead = async () => {
  try {
    await markAllAsRead()
    ElMessage.success('全部消息已标记为已读')
    
    // 更新列表状态
    messageList.value.forEach(message => {
      message.status = 'READ'
    })
    
    getUnreadCountData()
  } catch (error) {
    ElMessage.error('标记全部已读失败：' + error.message)
  }
}

// 处理消息操作
const handleMessageAction = ({ action, id }) => {
  if (action === 'markRead') {
    markMessageAsRead(id)
  } else if (action === 'delete') {
    deleteMessageById(id)
  }
}

// 详情对话框关闭处理
const handleDetailClose = () => {
  detailDialogVisible.value = false
  currentMessage.value = null
}

// 返回首页
const goToHome = () => {
  router.push('/')
}

// 跳转到朋友圈
const goToMoments = () => {
  router.push('/moments')
}

// 工具方法
const getMessageIcon = (type) => {
  const iconMap = {
    'PERSONAL': User,
    'ANNOUNCEMENT': Notification,
    'COMMUNITY_INTERACTION': ChatDotSquare,
    'SYSTEM': Bell
  }
  return iconMap[type] || Message
}

const getTypeText = (type) => {
  const typeMap = {
    'PERSONAL': '个人消息',
    'ANNOUNCEMENT': '系统公告',
    'COMMUNITY_INTERACTION': '社区互动',
    'SYSTEM': '系统通知'
  }
  return typeMap[type] || type
}

const getTypeTagType = (type) => {
  const typeMap = {
    'PERSONAL': '',
    'ANNOUNCEMENT': 'success',
    'COMMUNITY_INTERACTION': 'warning',
    'SYSTEM': 'info'
  }
  return typeMap[type] || ''
}

const getPriorityText = (priority) => {
  const priorityMap = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高'
  }
  return priorityMap[priority] || priority
}

const getPriorityTagType = (priority) => {
  const priorityMap = {
    'LOW': 'info',
    'MEDIUM': 'warning',
    'HIGH': 'danger'
  }
  return priorityMap[priority] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60 * 1000) {
    return '刚刚'
  }
  
  // 小于1小时
  if (diff < 60 * 60 * 1000) {
    return Math.floor(diff / (60 * 1000)) + '分钟前'
  }
  
  // 小于1天
  if (diff < 24 * 60 * 60 * 1000) {
    return Math.floor(diff / (60 * 60 * 1000)) + '小时前'
  }
  
  // 大于1天
  return date.toLocaleDateString()
}

const formatContent = (content) => {
  if (!content) return ''
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// 初始化
onMounted(() => {
  searchMessages()
  getUnreadCountData()
})
</script>

<style scoped>
.notification-center {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 头部样式 */
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-section h1 {
  margin: 0;
  color: #303133;
  font-size: 28px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 统计卡片样式 */
.stats-section {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  background-color: #409eff;
  color: white;
}

.stat-icon.unread {
  background-color: #e6a23c;
}

.stat-icon.read {
  background-color: #67c23a;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

/* 筛选样式 */
.filter-section {
  margin-bottom: 24px;
}

.filter-row {
  align-items: center;
}

/* 消息列表样式 */
.messages-container {
  min-height: 400px;
}

.message-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.message-item:hover {
  background-color: #f5f7fa;
}

.message-item.unread {
  background-color: #f0f8ff;
  border-left: 4px solid #409eff;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.message-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-preview {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-actions {
  margin-left: 16px;
  flex-shrink: 0;
}

.action-btn {
  color: #909399;
}

.action-btn:hover {
  color: #409eff;
}

/* 分页样式 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 详情对话框样式 */
.message-detail {
  padding: 16px 0;
}

.detail-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.detail-header h3 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-time {
  font-size: 14px;
  color: #909399;
  margin-left: auto;
}

.detail-content {
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

/* 设置对话框样式 */
.settings-container {
  padding: 16px 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label span {
  display: block;
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.setting-label small {
  font-size: 12px;
  color: #909399;
}

.empty-state,
.empty-settings {
  text-align: center;
  padding: 40px 20px;
}

.dialog-footer {
  text-align: right;
}
</style>
