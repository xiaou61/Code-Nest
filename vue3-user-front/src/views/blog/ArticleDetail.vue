<template>
  <CnPage class="article-detail-container" surface="transparent" max-width="1000px">
    <article v-if="article" class="article-layout">
      <CnSection surface="panel" class="article-main">
        <header class="article-header">
          <div class="article-breadcrumb">
            <el-button link type="primary" @click="goBlogHome">返回博客主页</el-button>
            <span>/</span>
            <span>{{ article.categoryName || '文章详情' }}</span>
          </div>

          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <el-avatar :size="44" :src="article.userAvatar" />
            <div class="meta-info">
              <div class="author">{{ article.userNickname || '匿名作者' }}</div>
              <div class="time">
                发布于 {{ article.publishTime || '-' }}
                <span v-if="article.updateTime && article.updateTime !== article.publishTime">
                  · 更新于 {{ article.updateTime }}
                </span>
              </div>
            </div>
          </div>

          <div class="article-tags">
            <CnStatusTag v-if="article.categoryName" type="brand" size="sm" subtle>
              {{ article.categoryName }}
            </CnStatusTag>
            <CnStatusTag v-for="tag in article.tags || []" :key="tag" type="info" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
            <CnStatusTag v-if="article.isOriginal === 1" type="success" size="sm">原创</CnStatusTag>
          </div>

          <div class="article-tools">
            <el-button type="success" plain @click="openTransformDialog">
              转为学习资产
            </el-button>
            <template v-if="article.canEdit">
              <el-button type="primary" plain @click="editArticle">编辑文章</el-button>
              <el-button type="danger" plain @click="deleteArticle">删除文章</el-button>
            </template>
          </div>
        </header>

        <div class="article-body">
          <div class="markdown-content article-markdown" v-html="renderMarkdown(article.content || '')" />
        </div>
      </CnSection>

      <CnSection
        v-if="article.relatedArticles?.length"
        title="相关文章"
        description="继续阅读相近主题的博客文章。"
        surface="panel"
        divided
        compact
      >
        <div class="related-articles">
          <button
            v-for="related in article.relatedArticles"
            :key="related.id"
            type="button"
            class="related-item"
            @click="viewRelated(related.id)"
          >
            <span>{{ related.title }}</span>
            <CnStatusTag v-if="related.categoryName" type="neutral" size="sm" subtle>
              {{ related.categoryName }}
            </CnStatusTag>
          </button>
        </div>
      </CnSection>
    </article>

    <CnSection v-else surface="panel">
      <CnEmptyState
        title="文章不存在或已删除"
        description="该文章可能已被作者删除，或当前链接不可访问。"
        icon="AR"
        size="lg"
      >
        <template #actions>
          <el-button type="primary" @click="goBlogHome">返回博客主页</el-button>
        </template>
      </CnEmptyState>
    </CnSection>

    <TransformDialog
      v-model="transformDialogVisible"
      source-type="blog"
      :source-id="article?.id || articleIdNumber"
      :source-title="article?.title || ''"
      :default-tags="article?.tags || []"
      @success="handleTransformSuccess"
    />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CnEmptyState, CnPage, CnSection, CnStatusTag } from '@/design-system'
import { deleteArticle as deleteArticleApi, getArticleDetail } from '@/api/blog'
import TransformDialog from '@/components/learning-assets/TransformDialog.vue'
import { useUserStore } from '@/stores/user'
import { renderMarkdown } from '@/utils/markdown'

interface RelatedArticle {
  id: number | string
  title?: string
  categoryName?: string
}

interface BlogArticleDetail {
  id: number | string
  title?: string
  content?: string
  userAvatar?: string
  userNickname?: string
  publishTime?: string
  updateTime?: string
  categoryName?: string
  tags?: string[]
  isOriginal?: number
  canEdit?: boolean
  relatedArticles?: RelatedArticle[]
}

interface TransformRecord {
  recordId: number | string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const article = ref<BlogArticleDetail | null>(null)
const transformDialogVisible = ref(false)

const articleIdNumber = computed(() => {
  const rawId = getFirstRouteParam(route.params.articleId)
  const numericId = Number(rawId)
  return Number.isFinite(numericId) ? numericId : 0
})

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    return error.message
  }

  if (typeof error === 'object' && error && 'message' in error) {
    return String((error as { message?: unknown }).message || '')
  }

  return String(error || '')
}

const getFirstRouteParam = (param: unknown) => {
  return Array.isArray(param) ? String(param[0] || '') : String(param || '')
}

const loadArticle = async () => {
  try {
    const articleId = getFirstRouteParam(route.params.articleId)
    article.value = await getArticleDetail(articleId)
  } catch (error) {
    article.value = null
    ElMessage.error(getErrorMessage(error) || '加载文章失败')
  }
}

const goBlogHome = () => {
  const userId = getFirstRouteParam(route.params.userId)
  if (userId) {
    router.push(`/blog/${userId}`)
    return
  }

  router.push('/blog')
}

const editArticle = () => {
  if (!article.value?.id) {
    return
  }

  router.push(`/blog/editor/${article.value.id}`)
}

const deleteArticle = async () => {
  if (!article.value?.id) {
    return
  }

  try {
    await ElMessageBox.confirm('确定要删除该文章吗？', '提示', {
      type: 'warning'
    })
    await deleteArticleApi(article.value.id)
    ElMessage.success('删除成功')
    router.push('/blog')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(getErrorMessage(error) || '删除失败')
    }
  }
}

const viewRelated = async (id: number | string) => {
  const userId = getFirstRouteParam(route.params.userId)
  await router.push(`/blog/${userId}/article/${id}`)
  await loadArticle()
}

const openTransformDialog = () => {
  if (!userStore.isLogin()) {
    ElMessage.warning('请先登录后再转化学习资产')
    router.push('/login')
    return
  }
  transformDialogVisible.value = true
}

const handleTransformSuccess = (record: TransformRecord) => {
  router.push(`/learning-assets?recordId=${record.recordId}`)
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.article-detail-container {
  min-height: calc(100vh - 68px);
}

.article-layout {
  display: grid;
  gap: var(--cn-space-5);
}

.article-main {
  overflow: hidden;
}

.article-header {
  display: grid;
  gap: var(--cn-space-4);
  padding-bottom: var(--cn-space-5);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.article-breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.article-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(28px, 5vw, 38px);
  font-weight: 750;
  line-height: 1.28;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
}

.meta-info {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.author {
  color: var(--cn-color-text-primary);
  font-weight: 700;
}

.time {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.5;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.article-tools {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.article-body {
  padding-top: var(--cn-space-6);
}

.article-markdown {
  min-width: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
  padding: 0;
  background: transparent;
  color: var(--cn-color-text-primary);
}

.related-articles {
  display: grid;
  gap: var(--cn-space-2);
}

.related-item {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-primary);
  cursor: pointer;
  font: inherit;
  padding: 12px 14px;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.related-item:hover,
.related-item:focus-visible {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  outline: none;
}

@media (max-width: 768px) {
  .article-header {
    gap: var(--cn-space-3);
  }

  .article-meta {
    align-items: flex-start;
  }
}
</style>
