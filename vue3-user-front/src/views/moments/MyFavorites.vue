<template>
  <CnPage class="my-favorites-page" surface="transparent" max-width="800px">
    <CnPageHeader
      title="我的收藏"
      description="收藏的精彩动态都在这里，方便随时回看和继续互动。"
      eyebrow="Moments Favorites"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="warning" size="sm">已收藏 {{ favoriteList.length }} 条</CnStatusTag>
        <CnStatusTag v-if="hasMore" type="info" size="sm">可继续加载</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain :icon="ArrowLeft" @click="goBack">返回</el-button>
      </template>
    </CnPageHeader>

    <CnSection
      title="收藏动态"
      description="按收藏时间分页展示，取消收藏后会从当前列表移除。"
      surface="panel"
      divided
    >
      <div v-loading="loading" class="favorites-list">
        <CnEmptyState
          v-if="!loading && favoriteList.length === 0"
          title="暂无收藏"
          description="收藏感兴趣的动态后，可以在这里快速查看。"
          icon="FA"
        />
      
        <div v-for="moment in favoriteList" :key="moment.id" class="moment-card">
          <div class="moment-header">
            <div class="user-avatar">
              {{ moment.userNickname?.charAt(0) }}
            </div>
            <div class="user-info">
              <span class="user-name">{{ moment.userNickname }}</span>
              <span class="post-time">{{ formatTime(moment.createTime) }}</span>
            </div>
          </div>

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

          <div class="moment-footer">
            <div v-if="moment.likeCount > 0 || moment.commentCount > 0 || moment.viewCount > 0" class="stats-bar">
              <CnStatusTag v-if="moment.likeCount > 0" type="danger" size="sm" subtle>
                <el-icon><Pointer /></el-icon>
                {{ moment.likeCount }} 赞
              </CnStatusTag>
              <CnStatusTag v-if="moment.commentCount > 0" type="info" size="sm" subtle>
                {{ moment.commentCount }} 评论
              </CnStatusTag>
              <CnStatusTag v-if="moment.viewCount > 0" type="neutral" size="sm" subtle>
                {{ moment.viewCount }} 浏览
              </CnStatusTag>
            </div>

            <div class="action-bar">
              <button
                class="action-btn"
                :class="{ active: moment.isLiked }"
                :disabled="moment.liking"
                @click="toggleLikeMoment(moment)"
              >
                <el-icon><Pointer /></el-icon>
                <span>{{ moment.isLiked ? '已赞' : '点赞' }}</span>
              </button>
              <button class="action-btn" @click="showCommentDialog(moment)">
                <el-icon><ChatDotRound /></el-icon>
                <span>评论</span>
              </button>
              <button
                class="action-btn unfav-btn"
                :disabled="moment.favoriting"
                @click="toggleFavoriteMoment(moment)"
              >
                <el-icon><StarFilled /></el-icon>
                <span>取消收藏</span>
              </button>
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

    <CommentDialog
      v-model="commentDialogVisible"
      :moment="currentMoment"
      @commented="handleCommented"
    />

    <el-image-viewer
      v-if="imageViewerVisible"
      :url-list="previewImages"
      :initial-index="previewIndex"
      @close="closeImageViewer"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElImageViewer } from 'element-plus'
import { ChatDotRound, StarFilled, ArrowLeft, Pointer } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { getMyFavorites, toggleLike, toggleFavorite } from '@/api/moment'
import { formatRelativeTime } from '@/utils/timeUtil'
import CommentDialog from './components/CommentDialog.vue'

interface FavoriteMoment {
  id: number | string
  userNickname?: string
  content?: string
  images: string[]
  createTime?: string
  likeCount: number
  commentCount: number
  viewCount: number
  isLiked?: boolean
  liking?: boolean
  favoriting?: boolean
}

const router = useRouter()

const loading = ref(false)
const loadingMore = ref(false)
const favoriteList = ref<FavoriteMoment[]>([])
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

const commentDialogVisible = ref(false)
const currentMoment = ref<FavoriteMoment | null>(null)

const imageViewerVisible = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '朋友圈', to: '/moments' },
  { label: '我的收藏' }
]

const getErrorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)

const loadFavoriteList = async (page = 1) => {
  if (page === 1) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      pageNum: page,
      pageSize: pageSize.value
    }

    const result = await getMyFavorites(params)
    const newMoments = (result.records || []).map((moment: Partial<FavoriteMoment>) => ({
      ...moment,
      images: moment.images || [],
      likeCount: moment.likeCount || 0,
      commentCount: moment.commentCount || 0,
      viewCount: moment.viewCount || 0,
      liking: false,
      favoriting: false
    })) as FavoriteMoment[]

    if (page === 1) {
      favoriteList.value = newMoments
    } else {
      favoriteList.value.push(...newMoments)
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
  loadFavoriteList(currentPage.value)
}

const toggleLikeMoment = async (moment: FavoriteMoment) => {
  moment.liking = true
  try {
    const isLiked = await toggleLike(moment.id)
    moment.isLiked = isLiked
    moment.likeCount += isLiked ? 1 : -1
  } catch (error) {
    ElMessage.error('操作失败：' + getErrorMessage(error))
  } finally {
    moment.liking = false
  }
}

const toggleFavoriteMoment = async (moment: FavoriteMoment) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    moment.favoriting = true
    await toggleFavorite(moment.id)
    
    // 从列表中移除
    const index = favoriteList.value.findIndex(item => item.id === moment.id)
    if (index > -1) {
      favoriteList.value.splice(index, 1)
    }
    
    ElMessage.success('取消收藏成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败：' + getErrorMessage(error))
    }
  } finally {
    moment.favoriting = false
  }
}

const showCommentDialog = (moment: FavoriteMoment) => {
  currentMoment.value = moment
  commentDialogVisible.value = true
}

const handleCommented = (_comment) => {
  const moment = currentMoment.value
  if (moment) {
    moment.commentCount++
  }
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

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadFavoriteList()
})
</script>

<style scoped>
.my-favorites-page {
  min-height: calc(100vh - 68px);
}

.favorites-list {
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
  background: var(--cn-color-warning);
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

.moment-header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-4);
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 72%, var(--cn-color-warning));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 15px;
  flex-shrink: 0;
  box-shadow: var(--cn-shadow-sm);
}

.user-info {
  flex: 1;
}

.user-name {
  display: block;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.post-time {
  display: block;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  margin-top: 2px;
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

.moment-footer {
  border-top: 1px solid var(--cn-color-border-subtle);
  padding-top: var(--cn-space-3);
}

.stats-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: var(--cn-space-3);
}

.action-bar {
  display: flex;
  justify-content: space-around;
  padding: 6px 0;
  border-top: 1px solid var(--cn-color-border-subtle);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: none;
  border: none;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  border-radius: var(--cn-radius-control);
  transition: all 0.25s ease;
  font-weight: 500;
}

.action-btn:hover {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  transform: translateY(-1px);
}

.action-btn.active {
  color: var(--cn-color-danger);
  font-weight: 600;
}

.action-btn.unfav-btn {
  color: var(--cn-color-warning);
}

.action-btn.unfav-btn:hover {
  background: color-mix(in srgb, var(--cn-color-warning) 10%, transparent);
  color: var(--cn-color-warning);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.load-more {
  text-align: center;
  padding: var(--cn-space-5);
}
</style>

