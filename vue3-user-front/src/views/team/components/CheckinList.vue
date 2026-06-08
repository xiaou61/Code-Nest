<template>
  <div class="checkin-list">
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="4" animated />
    </div>

    <CnEmptyState
      v-else-if="checkins.length === 0"
      title="暂无打卡记录"
      description="成员完成任务打卡后，动态会显示在这里。"
      icon="CI"
      size="sm"
      surface="transparent"
    />

    <div v-else class="checkins">
      <article v-for="checkin in checkins" :key="checkin.id" class="checkin-item">
        <div class="checkin-avatar">
          <img v-if="checkin.userAvatar" :src="checkin.userAvatar" :alt="checkin.userName || '成员头像'" />
          <span v-else>{{ checkin.userName?.charAt(0) || '用' }}</span>
        </div>

        <div class="checkin-body">
          <div class="checkin-header">
            <span class="user-name">{{ checkin.userName || '匿名成员' }}</span>
            <span class="checkin-time">{{ formatTime(checkin.createTime) }}</span>
            <CnStatusTag v-if="checkin.isSupplement" type="warning" size="sm" :dot="false" subtle>补卡</CnStatusTag>
          </div>

          <div v-if="checkin.taskName" class="checkin-task">
            <el-icon><Calendar /></el-icon>
            {{ checkin.taskName }}
          </div>

          <div class="checkin-content">{{ checkin.checkinContent }}</div>

          <div v-if="checkin.images" class="checkin-images">
            <img
              v-for="(img, index) in parseImages(checkin.images)"
              :key="`${img}-${index}`"
              :src="img"
              alt="打卡图片"
              @click="previewImage(img)"
            />
          </div>

          <div class="checkin-meta">
            <span v-if="checkin.duration" class="meta-item">
              <el-icon><Timer /></el-icon>
              {{ checkin.duration }} 分钟
            </span>
            <button class="meta-item like" :class="{ liked: checkin.isLiked }" type="button" @click="handleLike(checkin)">
              <el-icon><Star /></el-icon>
              {{ checkin.likeCount || 0 }}
            </button>
          </div>
        </div>
      </article>
    </div>

    <div v-if="hasMore" class="load-more">
      <el-button text @click="loadMore" :loading="loadingMore">加载更多</el-button>
    </div>

    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="previewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { Calendar, Star, Timer } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'

interface CheckinRecord {
  id: number | string
  userAvatar?: string
  userName?: string
  createTime?: string
  isSupplement?: boolean | number
  taskName?: string
  checkinContent?: string
  images?: string
  duration?: number
  isLiked?: boolean
  likeCount?: number
}

const props = withDefaults(
  defineProps<{
    teamId: string | number
    limit?: number
  }>(),
  {
    limit: 0
  }
)

const checkins = ref<CheckinRecord[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)

const previewVisible = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

onMounted(() => {
  if (props.limit > 0) {
    pageSize.value = props.limit
  }
  loadCheckins()
})

watch(
  () => props.teamId,
  () => {
    pageNum.value = 1
    loadCheckins()
  }
)

const loadCheckins = async () => {
  loading.value = true
  try {
    const response = (await teamApi.getCheckinList(props.teamId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })) as CheckinRecord[]
    checkins.value = response || []
    hasMore.value = props.limit === 0 && checkins.value.length === pageSize.value
  } catch (error) {
    console.error('加载打卡列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = async () => {
  loadingMore.value = true
  pageNum.value++
  try {
    const response = (await teamApi.getCheckinList(props.teamId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })) as CheckinRecord[]
    const newCheckins = response || []
    checkins.value = [...checkins.value, ...newCheckins]
    hasMore.value = newCheckins.length === pageSize.value
  } catch (error) {
    console.error('加载更多失败:', error)
    pageNum.value--
  } finally {
    loadingMore.value = false
  }
}

const handleLike = async (checkin: CheckinRecord) => {
  try {
    if (checkin.isLiked) {
      await teamApi.unlikeCheckin(checkin.id)
      checkin.isLiked = false
      checkin.likeCount = Math.max(0, (checkin.likeCount || 1) - 1)
    } else {
      await teamApi.likeCheckin(checkin.id)
      checkin.isLiked = true
      checkin.likeCount = (checkin.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const parseImages = (images?: string) => {
  if (!images) return []
  return images
    .split(',')
    .map((img) => img.trim())
    .filter(Boolean)
}

const previewImage = (img: string) => {
  previewImages.value = [img]
  previewIndex.value = 0
  previewVisible.value = true
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

defineExpose({ loadCheckins })
</script>

<style scoped>
.checkin-list {
  min-width: 0;
}

.loading-state {
  padding: var(--cn-space-5) 0;
}

.checkins {
  display: grid;
}

.checkin-item {
  display: flex;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-4) 0;
}

.checkin-item + .checkin-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.checkin-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  overflow: hidden;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
  font-size: 16px;
  font-weight: 800;
}

.checkin-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.checkin-body {
  flex: 1;
  min-width: 0;
}

.checkin-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-2);
}

.user-name {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.checkin-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.checkin-task {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  margin-bottom: var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 650;
}

.checkin-content {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.checkin-images {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.checkin-images img {
  width: 80px;
  height: 80px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  object-fit: cover;
  cursor: pointer;
  transition: transform var(--cn-motion-fast) var(--cn-ease-out);
}

.checkin-images img:hover {
  transform: scale(1.03);
}

.checkin-meta {
  display: flex;
  align-items: center;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-3);
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

.meta-item.like:hover,
.meta-item.like.liked {
  color: var(--cn-color-danger);
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-3) 0;
}
</style>
