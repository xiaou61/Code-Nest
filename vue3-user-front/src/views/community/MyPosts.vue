<template>
  <CnPage class="community-my-posts" surface="transparent" max-width="1120px">
    <CnPageHeader
      title="我的帖子"
      description="查看自己发布过的社区内容，以及每篇帖子获得的浏览、点赞、评论和收藏反馈。"
      eyebrow="Community Posts"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ total }} 个帖子</CnStatusTag>
        <CnStatusTag type="info" size="sm">当前页 {{ queryParams.pageNum }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain :icon="Back" @click="goBack">返回社区</el-button>
        <el-button type="primary" @click="goToCreatePost">发表帖子</el-button>
      </template>
    </CnPageHeader>

    <CnSection
      title="发布记录"
      :description="sectionDescription"
      surface="panel"
      divided
    >
      <template #actions>
        <CnStatusTag v-if="total > 0" type="neutral" size="sm">
          每页 {{ queryParams.pageSize }} 条
        </CnStatusTag>
      </template>

      <div v-loading="loading" class="posts-list">
        <article
          v-for="post in postList"
          :key="post.id"
          class="post-card"
          @click="goToPostDetail(post)"
        >
          <header class="post-header">
            <div class="post-meta">
              <span class="post-date">发表于 {{ formatDate(post.createTime) }}</span>
              <CnStatusTag v-if="post.categoryName" type="info" size="sm" :dot="false" subtle>
                {{ post.categoryName }}
              </CnStatusTag>
            </div>
          </header>

          <h2 class="post-title">{{ post.title }}</h2>
          <p class="post-content">{{ post.content }}</p>

          <div class="post-stats" aria-label="帖子数据">
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ post.viewCount || 0 }} 浏览
            </span>
            <span class="stat-item">
              <el-icon><StarFilled /></el-icon>
              {{ post.likeCount || 0 }} 点赞
            </span>
            <span class="stat-item">
              <el-icon><ChatDotRound /></el-icon>
              {{ post.commentCount || 0 }} 评论
            </span>
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ post.collectCount || 0 }} 收藏
            </span>
          </div>

          <footer class="post-actions" @click.stop>
            <el-button size="small" @click="goToPostDetail(post)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
          </footer>
        </article>
      </div>

      <CnEmptyState
        v-if="!loading && postList.length === 0"
        title="暂无发表的帖子"
        description="发布第一篇帖子后，你可以在这里查看自己的社区内容表现。"
        icon="PO"
      >
        <template #actions>
          <el-button type="primary" @click="goToCreatePost">去发表帖子</el-button>
        </template>
      </CnEmptyState>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, ChatDotRound, Star, StarFilled, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { communityApi } from '@/api/community'

interface CommunityPost {
  id: number | string
  title?: string
  content?: string
  categoryName?: string
  createTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  collectCount?: number
}

const router = useRouter()

const loading = ref(false)
const postList = ref<CommunityPost[]>([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '技术社区', to: '/community' },
  { label: '我的帖子' }
]

const sectionDescription = computed(() => {
  if (total.value <= 0) return '暂时没有发布记录。'
  return `当前第 ${queryParams.pageNum} 页，按发布时间分页展示。`
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchMyPosts = async () => {
  loading.value = true
  try {
    const response = await communityApi.getUserPosts(queryParams)
    postList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取我的帖子失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size: number) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchMyPosts()
}

const handleCurrentChange = (page: number) => {
  queryParams.pageNum = page
  fetchMyPosts()
}

const goToPostDetail = (post: CommunityPost) => {
  router.push(`/community/posts/${post.id}`)
}

const goBack = () => {
  router.push('/community')
}

const goToCreatePost = () => {
  router.push('/community/create')
}

onMounted(() => {
  fetchMyPosts()
})
</script>

<style scoped>
.community-my-posts {
  min-height: calc(100vh - 68px);
}

.posts-list {
  display: grid;
  gap: var(--cn-space-4);
}

.post-card {
  display: grid;
  gap: var(--cn-space-4);
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

.post-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.post-date {
  color: var(--cn-color-text-tertiary);
}

.post-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.45;
}

.post-card:hover .post-title {
  color: var(--cn-color-brand-primary);
}

.post-content {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.post-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.post-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-5);
}

@media (max-width: 768px) {
  .post-header {
    display: grid;
  }

  .post-card {
    padding: var(--cn-space-4);
  }
}
</style>
