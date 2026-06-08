<template>
  <CnPage class="blog-container" surface="transparent" max-width="1220px">
    <CnPageHeader
      title="我的博客"
      description="管理已发布文章和草稿，维护自己的技术博客主页。"
      eyebrow="Blog Studio"
    >
      <template #meta>
        <CnStatusTag :type="blogConfig ? 'success' : 'warning'" size="sm">
          {{ blogConfig ? '已开通' : '未开通' }}
        </CnStatusTag>
        <CnStatusTag v-if="blogConfig" type="info" size="sm">
          文章 {{ blogConfig.totalArticles || 0 }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button v-if="blogConfig" plain @click="viewMyBlogHome">查看博客主页</el-button>
        <el-button v-if="blogConfig" type="primary" @click="handleCreateArticle">写文章</el-button>
      </template>
    </CnPageHeader>

    <CnSection v-if="blogConfig" surface="panel" class="blog-profile">
      <div class="header-content">
        <el-avatar :size="80" :src="blogConfig.blogAvatar" />
        <div class="header-info">
          <h2>{{ blogConfig.blogName }}</h2>
          <p class="description">{{ blogConfig.blogDescription }}</p>
          <div class="stats">
            <CnStatusTag type="brand" size="sm">文章：{{ blogConfig.totalArticles || 0 }}</CnStatusTag>
          </div>
        </div>
      </div>
    </CnSection>

    <CnSection v-else surface="panel" class="open-blog-card">
      <CnEmptyState
        title="您还未开通博客"
        description="开通后即可创建文章、管理草稿，并拥有自己的博客主页。"
        icon="BL"
      >
        <template #actions>
        <el-button type="primary" @click="handleOpenBlog">开通博客（消耗50积分）</el-button>
        </template>
      </CnEmptyState>
    </CnSection>

    <CnSection
      v-if="blogConfig"
      title="我的文章"
      :description="listDescription"
      surface="panel"
      divided
      class="article-list"
    >
      <template #actions>
        <CnStatusTag type="neutral" size="sm">第 {{ pageNum }} 页</CnStatusTag>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="已发布" name="published">
          <div v-if="articleList.length > 0" class="articles">
            <div v-for="article in articleList" :key="article.id" class="article-item">
              <div class="article-content">
                <h4 class="article-title" @click="viewArticle(article.id)">
                  <CnStatusTag v-if="article.isTop === 1" type="warning" size="sm">置顶</CnStatusTag>
                  {{ article.title }}
                </h4>
                <p class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <CnStatusTag v-if="article.categoryName" type="info" size="sm" subtle>
                    {{ article.categoryName }}
                  </CnStatusTag>
                  <span>{{ article.publishTime }}</span>
                </div>
              </div>
              <div class="article-actions">
                <el-button link type="primary" @click="editArticle(article.id)">编辑</el-button>
                <el-button link type="danger" @click="deleteArticle(article.id)">删除</el-button>
              </div>
            </div>
          </div>
          <CnEmptyState
            v-else
            title="暂无文章"
            description="发布第一篇文章后，会在这里显示。"
            icon="AR"
            size="sm"
          />
        </el-tab-pane>

        <el-tab-pane label="草稿箱" name="draft">
          <div v-if="draftList.length > 0" class="articles">
            <div v-for="draft in draftList" :key="draft.id" class="article-item">
              <div class="article-content">
                <h4 class="article-title">{{ draft.title }}</h4>
                <p class="article-summary">{{ draft.summary }}</p>
                <div class="article-meta">
                  <span>{{ draft.createTime }}</span>
                </div>
              </div>
              <div class="article-actions">
                <el-button link type="primary" @click="editArticle(draft.id)">继续编辑</el-button>
                <el-button link type="danger" @click="deleteArticle(draft.id)">删除</el-button>
              </div>
            </div>
          </div>
          <CnEmptyState
            v-else
            title="暂无草稿"
            description="编辑器保存的草稿会显示在这里。"
            icon="DR"
            size="sm"
          />
        </el-tab-pane>
      </el-tabs>

      <div v-if="total > 0" class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="getList"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import {
  checkBlogStatus,
  getBlogConfig,
  openBlog,
  getMyArticleList,
  getMyDraftList,
  deleteArticle as deleteArticleApi
} from '@/api/blog'

interface BlogConfig {
  blogName?: string
  blogDescription?: string
  blogAvatar?: string
  totalArticles?: number
}

interface BlogArticle {
  id: number | string
  title?: string
  summary?: string
  categoryName?: string
  publishTime?: string
  createTime?: string
  isTop?: number
}

const router = useRouter()
const userStore = useUserStore()

const blogConfig = ref<BlogConfig | null>(null)
const articleList = ref<BlogArticle[]>([])
const draftList = ref<BlogArticle[]>([])
const activeTab = ref<'published' | 'draft'>('published')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const listDescription = computed(() => {
  return activeTab.value === 'published'
    ? '管理公开发布的博客文章。'
    : '继续编辑未发布的草稿。'
})

const getErrorMessage = (error: unknown) => error instanceof Error ? error.message : String(error)

const loadBlogStatus = async () => {
  try {
    const res = await checkBlogStatus()
    if (res.isOpened) {
      const userId = userStore.userInfo?.id
      if (userId) {
        const configRes = await getBlogConfig(userId)
        blogConfig.value = configRes
        getList()
      }
    }
  } catch (error) {
    console.error('加载博客状态失败', error)
  }
}

const handleOpenBlog = async () => {
  try {
    await ElMessageBox.confirm('开通博客需要消耗50积分，确定要开通吗？', '提示', {
      type: 'warning'
    })
    await openBlog()
    ElMessage.success('博客开通成功')
    loadBlogStatus()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error) || '开通失败')
    }
  }
}

const getList = async () => {
  try {
    if (activeTab.value === 'published') {
      const res = await getMyArticleList({
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      articleList.value = res.records || []
      total.value = res.total || 0
    } else {
      const res = await getMyDraftList(pageNum.value, pageSize.value)
      draftList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '获取列表失败')
  }
}

const handleTabChange = () => {
  pageNum.value = 1
  getList()
}

const handleCreateArticle = () => {
  router.push('/blog/editor')
}

const viewMyBlogHome = () => {
  const userStore = useUserStore()
  const userId = userStore.userInfo?.id
  if (userId) {
    router.push(`/blog/${userId}`)
  }
}

const editArticle = (id: number | string) => {
  router.push(`/blog/editor/${id}`)
}

const viewArticle = (id: number | string) => {
  const userId = userStore.userInfo?.id
  router.push(`/blog/${userId}/article/${id}`)
}

const deleteArticle = async (id: number | string) => {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？', '提示', {
      type: 'warning'
    })
    await deleteArticleApi(id)
    ElMessage.success('删除成功')
    getList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error) || '删除失败')
    }
  }
}

onMounted(() => {
  loadBlogStatus()
})
</script>

<style scoped>
.blog-container {
  min-height: calc(100vh - 68px);
}

.blog-profile {
  overflow: hidden;
}

.header-content {
  display: flex;
  gap: 18px;
  align-items: center;
  padding: 4px 0;
}

.header-info {
  flex: 1;
}

.header-info h2 {
  margin: 0 0 8px;
  color: var(--cn-color-text-primary);
  font-size: 24px;
  font-weight: 600;
}

.description {
  color: var(--cn-color-text-secondary);
  margin: 8px 0;
  line-height: 1.6;
}

.open-blog-card {
  text-align: center;
}

.articles {
  min-height: 280px;
}

.article-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 18px;
  border-bottom: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  transition: background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.article-item:last-child {
  border-bottom: none;
}

.article-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.article-content {
  flex: 1;
  min-width: 0;
}

.article-title {
  margin: 0 0 10px;
  cursor: pointer;
  color: var(--cn-color-text-primary);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  line-height: 1.45;
}

.article-title:hover {
  color: var(--cn-color-brand-primary);
}

.article-summary {
  color: var(--cn-color-text-secondary);
  margin: 8px 0 10px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.article-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.article-list {
  min-width: 0;
}

.article-list :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.article-list :deep(.el-tabs__item) {
  height: 40px;
}

.article-list :deep(.el-button.is-link) {
  font-weight: 500;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .header-content {
    align-items: flex-start;
  }

  .list-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .article-item {
    flex-direction: column;
    padding: 14px;
  }

  .article-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>

