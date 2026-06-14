<template>
  <CnPage class="community-user-profile" surface="transparent" max-width="1180px">
    <CnPageHeader
      :title="profileTitle"
      :description="profileDescription"
      eyebrow="Community Profile"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">帖子 {{ userProfile?.postCount || 0 }}</CnStatusTag>
        <CnStatusTag type="success" size="sm">获赞 {{ userProfile?.likeCount || 0 }}</CnStatusTag>
        <CnStatusTag type="info" size="sm">评论 {{ userProfile?.commentCount || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain :icon="Back" @click="goBack">社区首页</el-button>
        <el-button type="primary">
          <el-icon><Plus /></el-icon>
          关注
        </el-button>
        <el-button>
          <el-icon><ChatDotRound /></el-icon>
          私信
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection v-if="userProfile" class="profile-card" surface="panel">
      <div class="profile-summary">
        <div class="user-avatar-wrapper">
          <div class="user-avatar">
            {{ userProfile.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <span class="user-status" aria-hidden="true" />
        </div>

        <div class="user-copy">
          <h2 class="user-name">{{ userProfile.username }}</h2>
          <p class="user-bio">{{ userProfile.bio || '这个人很懒，什么都没写~' }}</p>
        </div>
      </div>
    </CnSection>

    <section class="profile-stats" aria-label="用户社区数据">
      <CnStatCard
        title="帖子"
        :value="userProfile?.postCount || 0"
        unit="篇"
        description="TA 发布的社区内容"
        tone="brand"
        trend="flat"
      />
      <CnStatCard
        title="评论"
        :value="userProfile?.commentCount || 0"
        unit="条"
        description="TA 参与过的讨论"
        tone="info"
        trend="flat"
      />
      <CnStatCard
        title="获赞"
        :value="userProfile?.likeCount || 0"
        unit="次"
        description="内容获得的点赞"
        tone="success"
        trend="flat"
      />
      <CnStatCard
        title="收藏"
        :value="userProfile?.collectCount || 0"
        unit="次"
        description="被收藏的内容反馈"
        tone="warning"
        trend="flat"
      />
    </section>

    <div class="content-layout">
      <aside class="profile-sidebar">
        <CnSection
          v-if="userProfile?.activeTags?.length > 0"
          title="活跃标签"
          description="TA 近期常参与的社区话题"
          surface="panel"
          compact
        >
          <div class="tags-cloud">
            <CnStatusTag
              v-for="tag in userProfile.activeTags"
              :key="tag.id"
              type="brand"
              size="sm"
              :dot="false"
              subtle
            >
              # {{ tag.name }} · {{ tag.count || 0 }}
            </CnStatusTag>
          </div>
        </CnSection>

        <CnSection title="个人成就" description="基于社区活跃度展示" surface="panel" compact>
          <div class="achievements">
            <div class="achievement-item">
              <span class="achievement-icon">新</span>
              <div>
                <span class="achievement-name">社区新星</span>
                <span class="achievement-desc">发布首篇帖子</span>
              </div>
            </div>
            <div class="achievement-item">
              <span class="achievement-icon">评</span>
              <div>
                <span class="achievement-name">热心评论</span>
                <span class="achievement-desc">评论数达到 10</span>
              </div>
            </div>
            <div class="achievement-item is-locked">
              <span class="achievement-icon">赞</span>
              <div>
                <span class="achievement-name">人气作者</span>
                <span class="achievement-desc">获得 100 个赞</span>
              </div>
            </div>
          </div>
        </CnSection>
      </aside>

      <CnSection
        class="posts-section"
        title="TA 的帖子"
        :description="postsDescription"
        surface="panel"
        divided
      >
        <template #actions>
          <CnStatusTag v-if="postsTotal > 0" type="neutral" size="sm">
            共 {{ postsTotal }} 篇
          </CnStatusTag>
          <CnStatusTag type="brand" size="sm" :dot="false" subtle>
            最新
          </CnStatusTag>
        </template>

        <div v-loading="postsLoading" class="posts-list">
          <article
            v-for="post in postsList"
            :key="post.id"
            class="post-card"
            @click="goToPostDetail(post)"
          >
            <div class="post-main">
              <h3 class="post-title">{{ post.title }}</h3>

              <div v-if="post.aiSummary" class="ai-summary">
                <CnStatusTag type="brand" size="sm" :dot="false">AI</CnStatusTag>
                <span class="summary-text">{{ post.aiSummary }}</span>
              </div>

              <p class="post-excerpt">{{ post.content }}</p>

              <div v-if="post.tags && post.tags.length > 0" class="post-tags">
                <CnStatusTag
                  v-for="tag in post.tags"
                  :key="tag.id"
                  type="neutral"
                  size="sm"
                  :dot="false"
                  subtle
                >
                  # {{ tag.name }}
                </CnStatusTag>
              </div>

              <div class="post-meta">
                <span class="meta-time">{{ formatDate(post.createTime) }}</span>
                <div class="meta-stats">
                  <span class="stat">
                    <el-icon><View /></el-icon>
                    {{ post.viewCount || 0 }}
                  </span>
                  <span class="stat">
                    <el-icon><Pointer /></el-icon>
                    {{ post.likeCount || 0 }}
                  </span>
                  <span class="stat">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ post.commentCount || 0 }}
                  </span>
                  <span class="stat">
                    <el-icon><Star /></el-icon>
                    {{ post.collectCount || 0 }}
                  </span>
                </div>
              </div>
            </div>
          </article>
        </div>

        <CnEmptyState
          v-if="!postsLoading && postsList.length === 0"
          title="暂无帖子"
          description="TA 还没有发布任何社区内容。"
          icon="UP"
        />

        <div v-if="postsTotal > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="postsQueryParams.pageNum"
            v-model:page-size="postsQueryParams.pageSize"
            :page-sizes="[10, 20, 30]"
            :total="postsTotal"
            layout="total, sizes, prev, pager, next"
            @size-change="handlePostsSizeChange"
            @current-change="handlePostsCurrentChange"
          />
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, ChatDotRound, Plus, Pointer, Star, View } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { communityApi } from '@/api/community'

interface CommunityTag {
  id: number | string
  name?: string
  count?: number
}

interface UserProfile {
  username?: string
  bio?: string
  postCount?: number
  commentCount?: number
  likeCount?: number
  collectCount?: number
  activeTags?: CommunityTag[]
}

interface CommunityPost {
  id: number | string
  title?: string
  content?: string
  aiSummary?: string
  createTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
  tags?: CommunityTag[]
}

const route = useRoute()
const router = useRouter()

const userProfile = ref<UserProfile | null>(null)
const postsList = ref<CommunityPost[]>([])
const postsTotal = ref(0)
const postsLoading = ref(false)

const postsQueryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '技术社区', to: '/community' },
  { label: '用户主页' }
]

const profileTitle = computed(() => {
  return userProfile.value?.username ? `${userProfile.value.username} 的主页` : '用户主页'
})

const profileDescription = computed(() => {
  return userProfile.value?.bio || '查看 TA 在 Code Nest 社区发布的内容、互动数据和活跃标签。'
})

const postsDescription = computed(() => {
  if (postsTotal.value <= 0) return '暂时没有发布记录。'
  return `当前第 ${postsQueryParams.pageNum} 页，展示 TA 最近发布的社区帖子。`
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  return date.toLocaleDateString('zh-CN')
}

const fetchUserProfile = async () => {
  try {
    const userId = route.params.userId
    userProfile.value = await communityApi.getUserProfile(userId)
  } catch (error) {
    ElMessage.error('获取用户信息失败')
    goBack()
  }
}

const fetchUserPosts = async () => {
  postsLoading.value = true
  try {
    const userId = route.params.userId
    const response = await communityApi.getUserPostList(userId, postsQueryParams)
    postsList.value = response.records || []
    postsTotal.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取帖子列表失败')
  } finally {
    postsLoading.value = false
  }
}

const goToPostDetail = (post: CommunityPost) => {
  router.push(`/community/posts/${post.id}`)
}

const handlePostsSizeChange = (size: number) => {
  postsQueryParams.pageSize = size
  postsQueryParams.pageNum = 1
  fetchUserPosts()
}

const handlePostsCurrentChange = (page: number) => {
  postsQueryParams.pageNum = page
  fetchUserPosts()
}

const goBack = () => {
  router.push('/community')
}

onMounted(async () => {
  await fetchUserProfile()
  await fetchUserPosts()
})
</script>

<style scoped>
.community-user-profile {
  min-height: calc(100vh - 68px);
}

.profile-card :deep(.cn-section__body) {
  padding: var(--cn-space-6);
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: var(--cn-space-5);
  min-width: 0;
}

.user-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 92px;
  height: 92px;
  border: 4px solid var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-pill);
  background: color-mix(in srgb, var(--cn-color-brand-primary) 70%, var(--cn-color-success));
  box-shadow: var(--cn-shadow-card);
  color: white;
  font-size: 34px;
  font-weight: 800;
}

.user-status {
  position: absolute;
  right: 7px;
  bottom: 7px;
  width: 16px;
  height: 16px;
  border: 3px solid var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-success);
}

.user-copy {
  min-width: 0;
}

.user-name {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
}

.user-bio {
  max-width: 760px;
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.content-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.profile-sidebar {
  display: grid;
  gap: var(--cn-space-4);
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.achievements {
  display: grid;
  gap: var(--cn-space-3);
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.achievement-item.is-locked {
  opacity: 0.62;
}

.achievement-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}

.achievement-name,
.achievement-desc {
  display: block;
}

.achievement-name {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.achievement-desc {
  margin-top: 2px;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.posts-list {
  display: grid;
  gap: var(--cn-space-4);
}

.post-card {
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.post-card:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border));
  box-shadow: var(--cn-shadow-card);
  transform: translateY(-2px);
}

.post-title {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.45;
}

.post-card:hover .post-title {
  color: var(--cn-color-brand-primary);
}

.ai-summary {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-2);
  margin-bottom: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 18%, var(--cn-color-border-subtle));
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-soft);
}

.summary-text {
  min-width: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.post-excerpt {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
}

.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-4);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.meta-time {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.meta-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-5);
}

@media (max-width: 1024px) {
  .profile-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-layout {
    grid-template-columns: 1fr;
  }

  .profile-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .profile-summary,
  .post-meta {
    display: grid;
  }

  .profile-sidebar,
  .profile-stats {
    grid-template-columns: 1fr;
  }

  .profile-card :deep(.cn-section__body),
  .post-card {
    padding: var(--cn-space-4);
  }
}
</style>
