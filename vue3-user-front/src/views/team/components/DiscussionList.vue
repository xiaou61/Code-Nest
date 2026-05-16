<template>
  <div class="discussion-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>
    
    <div v-else-if="discussions.length === 0" class="empty-state">
      <el-empty description="暂无讨论" :image-size="80" />
    </div>
    
    <div v-else class="discussions">
      <div 
        v-for="item in discussions" 
        :key="item.id" 
        class="discussion-item"
        @click="viewDetail(item)"
      >
        <div class="discussion-header">
          <div class="author-info">
            <div class="author-avatar">
              <img v-if="item.userAvatar" :src="item.userAvatar" />
              <span v-else>{{ item.userName?.charAt(0) || '用' }}</span>
            </div>
            <span class="author-name">{{ item.userName }}</span>
            <span class="post-time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="discussion-badges">
            <span v-if="item.isTop" class="badge top">置顶</span>
            <span v-if="item.isEssence" class="badge essence">精华</span>
          </div>
        </div>
        
        <h3 class="discussion-title">{{ item.title }}</h3>
        <p class="discussion-content">{{ truncateContent(item.content) }}</p>
        
        <div class="discussion-footer">
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ item.viewCount || 0 }}
          </span>
          <span class="meta-item" :class="{ liked: item.isLiked }" @click.stop="handleLike(item)">
            <el-icon><Star /></el-icon>
            {{ item.likeCount || 0 }}
          </span>
          <span class="meta-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ item.replyCount || 0 }}
          </span>
          
          <el-dropdown v-if="isAdmin" trigger="click" @click.stop>
            <el-button text size="small">
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
                  <el-icon><Delete /></el-icon>删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <!-- 加载更多 -->
    <div v-if="hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">
        加载更多
      </el-button>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog 
      v-model="showDetailDialog" 
      :title="currentDiscussion?.title"
      width="600px"
    >
      <div class="discussion-detail" v-if="currentDiscussion">
        <div class="detail-author">
          <div class="author-avatar">
            <img v-if="currentDiscussion.userAvatar" :src="currentDiscussion.userAvatar" />
            <span v-else>{{ currentDiscussion.userName?.charAt(0) || '用' }}</span>
          </div>
          <div class="author-info">
            <span class="author-name">{{ currentDiscussion.userName }}</span>
            <span class="post-time">{{ formatTime(currentDiscussion.createTime) }}</span>
          </div>
        </div>
        <div class="detail-content" v-html="sanitizeDiscussionContent(currentDiscussion.content)"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, Star, ChatDotRound, MoreFilled, Delete } from '@element-plus/icons-vue'
import teamApi from '@/api/team'
import { sanitizeHtml } from '@/utils/markdown'

const props = defineProps({
  teamId: { type: [String, Number], required: true },
  isAdmin: { type: Boolean, default: false }
})

const discussions = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)

// 详情弹窗
const showDetailDialog = ref(false)
const currentDiscussion = ref(null)

onMounted(() => {
  loadDiscussions()
})

watch(() => props.teamId, () => {
  pageNum.value = 1
  loadDiscussions()
})

const sanitizeDiscussionContent = (content) => {
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

const loadDiscussions = async () => {
  loading.value = true
  try {
    const response = await teamApi.getDiscussionList(props.teamId, { 
      pageNum: pageNum.value, 
      pageSize: 10 
    })
    discussions.value = response.records || response || []
    hasMore.value = (response.records?.length || 0) === 10
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
    const response = await teamApi.getDiscussionList(props.teamId, { 
      pageNum: pageNum.value, 
      pageSize: 10 
    })
    const newItems = response.records || response || []
    discussions.value = [...discussions.value, ...newItems]
    hasMore.value = newItems.length === 10
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

const viewDetail = async (item) => {
  try {
    const detail = await teamApi.getDiscussionDetail(props.teamId, item.id)
    currentDiscussion.value = detail
    showDetailDialog.value = true
  } catch (error) {
    console.error('加载详情失败:', error)
  }
}

const handleLike = async (item) => {
  try {
    if (item.isLiked) {
      await teamApi.unlikeDiscussion(props.teamId, item.id)
      item.isLiked = false
      item.likeCount = Math.max(0, (item.likeCount || 1) - 1)
    } else {
      await teamApi.likeDiscussion(props.teamId, item.id)
      item.isLiked = true
      item.likeCount = (item.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const toggleTop = async (item) => {
  try {
    await teamApi.setDiscussionTop(props.teamId, item.id, !item.isTop)
    item.isTop = !item.isTop
    ElMessage.success(item.isTop ? '已置顶' : '已取消置顶')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const toggleEssence = async (item) => {
  try {
    await teamApi.setDiscussionEssence(props.teamId, item.id, !item.isEssence)
    item.isEssence = !item.isEssence
    ElMessage.success(item.isEssence ? '已设为精华' : '已取消精华')
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除这个讨论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.deleteDiscussion(props.teamId, item.id)
    ElMessage.success('删除成功')
    loadDiscussions()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const truncateContent = (content) => {
  if (!content) return ''
  // 去除HTML标签
  const text = content.replace(/<[^>]+>/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 172800000) return '昨天'
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

defineExpose({ loadDiscussions })
</script>

<style lang="scss" scoped>
.discussion-list {
  .loading-state, .empty-state {
    padding: 20px 0;
  }
}

.discussion-item {
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover {
    background: #f8f9fc;
  }
  
  & + .discussion-item {
    border-top: 1px solid #f0f0f0;
  }
}

.discussion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  
  .author-info {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .author-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      overflow: hidden;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      span {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        color: white;
        font-size: 12px;
      }
    }
    
    .author-name {
      font-size: 13px;
      color: #666;
    }
    
    .post-time {
      font-size: 12px;
      color: #999;
    }
  }
  
  .discussion-badges {
    display: flex;
    gap: 6px;
    
    .badge {
      padding: 2px 8px;
      font-size: 11px;
      border-radius: 4px;
      
      &.top {
        background: #e8f4fd;
        color: #409eff;
      }
      
      &.essence {
        background: #fdf2e9;
        color: #e6a23c;
      }
    }
  }
}

.discussion-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
}

.discussion-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.discussion-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: #999;
    
    &.liked {
      color: #f56c6c;
    }
  }
  
  .el-dropdown {
    margin-left: auto;
  }
}

.load-more {
  text-align: center;
  padding: 12px 0;
}

// 详情弹窗
.discussion-detail {
  .detail-author {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 16px;
    
    .author-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
      
      span {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
        color: white;
        font-size: 16px;
      }
    }
    
    .author-info {
      .author-name {
        display: block;
        font-size: 14px;
        font-weight: 500;
        color: #333;
      }
      
      .post-time {
        font-size: 12px;
        color: #999;
      }
    }
  }
  
  .detail-content {
    font-size: 15px;
    color: #333;
    line-height: 1.8;
  }
}
</style>
