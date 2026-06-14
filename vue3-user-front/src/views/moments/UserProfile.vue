<template>
  <CnPage class="moment-user-profile" surface="transparent" max-width="860px">
    <CnPageHeader
      :title="profileTitle"
      description="查看这位用户在朋友圈发布过的动态，以及获得的互动反馈。"
      eyebrow="Moments Profile"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">用户 ID {{ userId || '-' }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">动态 {{ userInfo.totalMoments || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goBackToMoments">返回朋友圈</el-button>
      </template>
    </CnPageHeader>

    <div v-loading="loadingUserInfo" class="profile-hero">
      <div class="profile-cover" />
      <div class="profile-body">
        <div class="user-avatar-large">
          {{ userInitial }}
        </div>
        <div class="user-details">
          <h2 class="user-name">{{ userInfo.nickname }}</h2>
          <p class="user-subtitle">朋友圈个人主页</p>
        </div>
      </div>
    </div>

    <div class="profile-stats" aria-label="用户动态数据">
      <CnStatCard
        title="动态"
        :value="userInfo.totalMoments || 0"
        description="已发布内容"
        tone="brand"
        :loading="loadingUserInfo"
      />
      <CnStatCard
        title="获赞"
        :value="userInfo.totalLikes || 0"
        description="累计点赞"
        tone="danger"
        :loading="loadingUserInfo"
      />
      <CnStatCard
        title="评论"
        :value="userInfo.totalComments || 0"
        description="累计评论"
        tone="info"
        :loading="loadingUserInfo"
      />
    </div>

    <CnSection
      title="Ta 的动态"
      :description="sectionDescription"
      surface="panel"
      divided
    >
      <div v-loading="loading" class="moments-list">
        <CnEmptyState
          v-if="!loading && momentList.length === 0"
          title="暂无动态"
          description="这位用户暂时还没有发布朋友圈动态。"
          icon="MO"
        />
        
        <div v-for="moment in momentList" :key="moment.id" class="moment-card">
          <div class="moment-body">
            <p class="moment-text">{{ moment.content }}</p>
            
            <div v-if="moment.images && moment.images.length" class="images-grid">
              <div 
                v-for="(image, index) in moment.images" 
                :key="index" 
                class="image-item"
                @click="previewImage(moment.images, index)"
              >
                <img :src="image" alt="" loading="lazy" />
              </div>
            </div>
          </div>

          <div class="interaction-footer">
            <span class="time-text">{{ formatTime(moment.createTime) }}</span>
            <div class="interaction-stats">
              <CnStatusTag v-if="moment.likeCount > 0" type="danger" size="sm" subtle>
                <el-icon><Star /></el-icon>
                {{ moment.likeCount }}
              </CnStatusTag>
              <CnStatusTag v-if="moment.commentCount > 0" type="info" size="sm" subtle>
                <el-icon><ChatDotRound /></el-icon>
                {{ moment.commentCount }}
              </CnStatusTag>
              <CnStatusTag v-if="moment.viewCount > 0" type="neutral" size="sm" subtle>
                <el-icon><View /></el-icon>
                {{ moment.viewCount }}
              </CnStatusTag>
            </div>
          </div>
        </div>

        <div v-if="hasMore" class="load-more">
          <el-button :loading="loadingMore" @click="loadMore">
            {{ loadingMore ? '加载中...' : '加载更多' }}
          </el-button>
        </div>
      </div>
    </CnSection>

    <el-image-viewer
      v-if="imageViewerVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="closeImageViewer"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElImageViewer } from 'element-plus'
import { Star, ChatDotRound, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { getUserMomentList, getUserMomentInfo } from '@/api/moment'
import { formatRelativeTime } from '@/utils/timeUtil'

interface MomentUserInfo {
  nickname?: string
  totalMoments?: number
  totalLikes?: number
  totalComments?: number
}

interface UserMoment {
  id: number | string
  content?: string
  images?: string[]
  createTime?: string
  likeCount?: number
  commentCount?: number
  viewCount?: number
}

const route = useRoute()
const router = useRouter()

const userId = ref<number | null>(null)

const userInfo = ref<MomentUserInfo>({})
const loadingUserInfo = ref(false)

const loading = ref(false)
const loadingMore = ref(false)
const momentList = ref<UserMoment[]>([])
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

const imageViewerVisible = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '朋友圈', to: '/moments' },
  { label: '用户主页' }
]

const profileTitle = computed(() => {
  return userInfo.value.nickname ? `${userInfo.value.nickname} 的主页` : '用户主页'
})

const userInitial = computed(() => userInfo.value.nickname?.charAt(0) || 'U')

const sectionDescription = computed(() => {
  if (momentList.value.length <= 0) return '按发布时间展示用户动态。'
  return `已加载 ${momentList.value.length} 条动态${hasMore.value ? '，可继续加载更多。' : '。'}`
})

const getErrorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)

const loadUserInfo = async () => {
  loadingUserInfo.value = true
  try {
    const result = await getUserMomentInfo({ userId: userId.value })
    userInfo.value = result || {}
  } catch (error) {
    ElMessage.error('加载用户信息失败：' + getErrorMessage(error))
  } finally {
    loadingUserInfo.value = false
  }
}

const loadMomentList = async (page = 1) => {
  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      userId: userId.value,
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getUserMomentList(params)
    const newMoments = result.records || []

    if (page === 1) {
      momentList.value = newMoments
    } else {
      momentList.value.push(...newMoments)
    }

    hasMore.value = newMoments.length === pageSize.value
  } catch (error) {
    ElMessage.error('加载失败：' + getErrorMessage(error))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  loadMomentList(currentPage.value)
}

const previewImage = (images: string[], index: number) => {
  previewImages.value = images
  previewIndex.value = index
  imageViewerVisible.value = true
}

const closeImageViewer = () => {
  imageViewerVisible.value = false
}

const formatTime = (time?: string) => {
  return formatRelativeTime(time)
}

const goBackToMoments = () => {
  router.push('/moments')
}

onMounted(() => {
  userId.value = Number(route.params.userId || route.query.userId)
  
  if (!userId.value) {
    ElMessage.error('用户ID不存在')
    return
  }
  
  loadUserInfo()
  loadMomentList()
})
</script>

<style scoped>
.moment-user-profile {
  min-height: calc(100vh - 68px);
}

.profile-hero {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-card);
  margin-bottom: var(--cn-space-5);
}

.profile-cover {
  height: 100px;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 72%, var(--cn-color-info));
  position: relative;
}

.profile-cover::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 40px;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 68%, transparent);
}

.profile-body {
  display: flex;
  align-items: center;
  gap: var(--cn-space-5);
  padding: 0 var(--cn-space-6) var(--cn-space-6);
  margin-top: -36px;
  position: relative;
  z-index: 1;
}

.user-avatar-large {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 78%, var(--cn-color-warning));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 28px;
  border: 4px solid var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-card);
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  padding-top: 20px;
  min-width: 0;
}

.user-name {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--cn-color-text-primary);
}

.user-subtitle {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-bottom: var(--cn-space-5);
}

.moments-list {
  display: grid;
  gap: var(--cn-space-4);
  min-height: 400px;
}

.moment-card {
  position: relative;
  overflow: hidden;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-xs);
  transition: all 0.3s ease;
}

.moment-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: var(--cn-color-brand-primary);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.moment-card:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 32%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-card);
  transform: translateY(-2px);
}

.moment-card:hover::before {
  opacity: 1;
}

.moment-body {
  margin-bottom: var(--cn-space-4);
}

.moment-text {
  margin: 0 0 var(--cn-space-3) 0;
  line-height: 1.75;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  white-space: pre-wrap;
}

.images-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-radius: var(--cn-radius-control);
  overflow: hidden;
}

.image-item {
  cursor: pointer;
  border-radius: var(--cn-radius-control);
  overflow: hidden;
  width: 120px;
  height: 120px;
}

.image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.image-item:hover img {
  transform: scale(1.08);
}

.interaction-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.time-text {
  font-size: 12px;
  color: var(--cn-color-text-tertiary);
}

.interaction-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.load-more {
  text-align: center;
  padding: var(--cn-space-5);
}

@media (max-width: 768px) {
  .profile-stats {
    grid-template-columns: 1fr;
  }

  .profile-body {
    align-items: flex-start;
    padding: 0 var(--cn-space-4) var(--cn-space-5);
  }

  .interaction-footer {
    display: grid;
  }
}
</style>

