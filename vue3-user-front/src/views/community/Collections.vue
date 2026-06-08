<template>
  <CnPage class="community-collections" surface="transparent" max-width="1120px">
    <CnPageHeader
      title="我的收藏"
      description="集中管理收藏过的社区帖子，快速回到内容详情或移除不再关注的帖子。"
      eyebrow="Community Collections"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ total }} 个收藏</CnStatusTag>
        <CnStatusTag type="info" size="sm">当前页 {{ queryParams.pageNum }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button plain :icon="Back" @click="goBack">返回社区</el-button>
        <el-button type="primary" @click="goToCommunity">去社区逛逛</el-button>
      </template>
    </CnPageHeader>

    <CnSection
      title="收藏列表"
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
              <span class="post-author">{{ post.authorName || '匿名用户' }}</span>
              <span class="post-date">{{ formatDate(post.createTime) }}</span>
              <CnStatusTag v-if="post.categoryName" type="info" size="sm" :dot="false" subtle>
                {{ post.categoryName }}
              </CnStatusTag>
            </div>
            <CnStatusTag v-if="post.collectTime" type="warning" size="sm" :dot="false" subtle>
              收藏于 {{ formatDate(post.collectTime) }}
            </CnStatusTag>
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
          </div>

          <footer class="post-actions" @click.stop>
            <el-button size="small" @click="goToPostDetail(post)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button type="danger" plain size="small" @click="uncollectPost(post)">
              <el-icon><Star /></el-icon>
              取消收藏
            </el-button>
          </footer>
        </article>
      </div>

      <CnEmptyState
        v-if="!loading && postList.length === 0"
        title="暂无收藏的帖子"
        description="在社区浏览时收藏有价值的帖子，它们会集中显示在这里。"
        icon="CO"
      >
        <template #actions>
          <el-button type="primary" @click="goToCommunity">去社区逛逛</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, ChatDotRound, Star, StarFilled, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { communityApi } from '@/api/community'

interface CommunityPost {
  id: number | string
  title?: string
  content?: string
  authorName?: string
  categoryName?: string
  createTime?: string
  collectTime?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
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
  { label: '我的收藏' }
]

const sectionDescription = computed(() => {
  if (total.value <= 0) return '暂时没有收藏内容。'
  return `当前第 ${queryParams.pageNum} 页，按收藏记录分页展示。`
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchCollections = async () => {
  loading.value = true
  try {
    const response = await communityApi.getUserCollections(queryParams)
    postList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    ElMessage.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size: number) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  fetchCollections()
}

const handleCurrentChange = (page: number) => {
  queryParams.pageNum = page
  fetchCollections()
}

const uncollectPost = async (post: CommunityPost) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏这个帖子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.uncollectPost(post.id)
    ElMessage.success('取消收藏成功')

    const index = postList.value.findIndex((item) => item.id === post.id)
    if (index > -1) {
      postList.value.splice(index, 1)
      total.value = Math.max(0, total.value - 1)
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const goToPostDetail = (post: CommunityPost) => {
  router.push(`/community/posts/${post.id}`)
}

const goBack = () => {
  router.push('/community')
}

const goToCommunity = () => {
  router.push('/community')
}

onMounted(() => {
  fetchCollections()
})
</script>

<style scoped>
.community-collections {
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

.post-author {
  color: var(--cn-color-brand-primary);
  font-weight: 650;
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
