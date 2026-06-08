<template>
  <CnPage class="blog-home-container" surface="transparent" max-width="1200px">
    <CnSection v-if="blogConfig" surface="transparent" class="blog-hero-section">
      <div class="blog-hero" :class="{ 'blog-hero--cover': Boolean(blogConfig.blogCover) }" :style="heroStyle">
        <div class="blog-hero__overlay" />
        <div class="blog-hero__content">
          <el-avatar :size="96" :src="blogConfig.blogAvatar" class="blog-hero__avatar" />
          <div class="blog-hero__copy">
            <p class="blog-hero__eyebrow">Personal Blog</p>
            <h1 class="blog-hero__title">{{ blogConfig.blogName }}</h1>
            <p v-if="blogConfig.blogDescription" class="blog-hero__description">
              {{ blogConfig.blogDescription }}
            </p>
          </div>
          <div class="blog-hero__meta">
            <CnStatusTag type="brand" size="lg">文章 {{ blogConfig.totalArticles || 0 }}</CnStatusTag>
          </div>
        </div>
      </div>
    </CnSection>

    <CnSection v-else surface="panel">
      <CnEmptyState
        title="该用户还未开通博客"
        description="当前博客主页暂不可访问。"
        icon="BL"
        size="lg"
      />
    </CnSection>

    <div v-if="blogConfig" class="blog-layout">
      <main class="blog-main">
        <CnSection title="文章列表" :description="articleListDescription" surface="panel" divided>
          <div v-if="articleList.length > 0" class="article-list">
            <article
              v-for="article in articleList"
              :key="article.id"
              class="article-card"
              role="button"
              tabindex="0"
              @click="viewArticle(article.id)"
              @keydown.enter="viewArticle(article.id)"
            >
              <div v-if="article.coverImage" class="article-cover">
                <img :src="article.coverImage" :alt="article.title || '文章封面'" />
                <CnStatusTag v-if="article.isTop === 1" type="warning" size="sm" class="top-tag">
                  置顶
                </CnStatusTag>
              </div>

              <div class="article-info">
                <h3 class="article-title">
                  <CnStatusTag v-if="article.isTop === 1 && !article.coverImage" type="warning" size="sm">
                    置顶
                  </CnStatusTag>
                  <span>{{ article.title }}</span>
                </h3>
                <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
                <div class="article-meta">
                  <CnStatusTag v-if="article.categoryName" type="brand" size="sm" subtle>
                    {{ article.categoryName }}
                  </CnStatusTag>
                  <CnStatusTag
                    v-for="tag in article.tags || []"
                    :key="tag"
                    type="info"
                    size="sm"
                    subtle
                  >
                    {{ tag }}
                  </CnStatusTag>
                  <span v-if="article.publishTime" class="article-time">{{ article.publishTime }}</span>
                </div>
              </div>
            </article>
          </div>

          <CnEmptyState
            v-else
            title="暂无文章"
            description="当前分类下还没有公开文章。"
            icon="AR"
            size="sm"
          />

          <div v-if="total > 0" class="pagination">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="handlePageChange"
            />
          </div>
        </CnSection>
      </main>

      <aside class="blog-sidebar">
        <CnSection title="分类" surface="panel" compact divided>
          <div class="category-list">
            <button
              type="button"
              class="category-item"
              :class="{ active: !queryParams.categoryId }"
              @click="filterByCategory(null)"
            >
              <span>全部</span>
              <CnStatusTag type="neutral" size="sm" subtle>全部</CnStatusTag>
            </button>

            <button
              v-for="category in categories"
              :key="category.id"
              type="button"
              class="category-item"
              :class="{ active: queryParams.categoryId === category.id }"
              @click="filterByCategory(category.id)"
            >
              <span>{{ category.categoryName }}</span>
              <CnStatusTag type="neutral" size="sm" subtle>
                {{ category.articleCount || 0 }}
              </CnStatusTag>
            </button>
          </div>
        </CnSection>

        <CnSection
          v-if="blogConfig.personalTags?.length"
          title="个人标签"
          surface="panel"
          compact
          divided
        >
          <div class="tag-list">
            <CnStatusTag v-for="tag in blogConfig.personalTags" :key="tag" type="info" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
          </div>
        </CnSection>

        <CnSection v-if="socialEntries.length > 0" title="社交链接" surface="panel" compact divided>
          <div class="social-links">
            <a
              v-for="[name, link] in socialEntries"
              :key="name"
              :href="link"
              target="_blank"
              rel="noopener noreferrer"
              class="social-link"
            >
              {{ name }}
            </a>
          </div>
        </CnSection>
      </aside>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { CSSProperties } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CnEmptyState, CnPage, CnSection, CnStatusTag } from '@/design-system'
import { getAllCategories, getBlogConfig, getUserArticleList } from '@/api/blog'

interface BlogConfig {
  blogName?: string
  blogDescription?: string
  blogAvatar?: string
  blogCover?: string
  totalArticles?: number
  personalTags?: string[]
  socialLinks?: Record<string, string>
}

interface BlogArticle {
  id: number | string
  title?: string
  summary?: string
  coverImage?: string
  categoryName?: string
  tags?: string[]
  publishTime?: string
  isTop?: number
}

interface BlogCategory {
  id: number | string
  categoryName?: string
  articleCount?: number
}

interface BlogQueryParams {
  userId: string | null
  categoryId: number | string | null
  pageNum: number
  pageSize: number
}

const router = useRouter()
const route = useRoute()

const blogConfig = ref<BlogConfig | null>(null)
const articleList = ref<BlogArticle[]>([])
const categories = ref<BlogCategory[]>([])
const total = ref(0)

const queryParams = reactive<BlogQueryParams>({
  userId: null,
  categoryId: null,
  pageNum: 1,
  pageSize: 10
})

const heroStyle = computed<CSSProperties>(() => {
  if (!blogConfig.value?.blogCover) {
    return {}
  }

  return {
    backgroundImage: `url(${blogConfig.value.blogCover})`
  }
})

const articleListDescription = computed(() => {
  const activeCategory = categories.value.find(category => category.id === queryParams.categoryId)
  return activeCategory?.categoryName
    ? `正在查看「${activeCategory.categoryName}」分类下的公开文章。`
    : '按发布时间浏览该用户公开发布的博客文章。'
})

const socialEntries = computed<[string, string][]>(() => {
  return Object.entries(blogConfig.value?.socialLinks || {}).filter(([, link]) => Boolean(link))
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

const loadBlogConfig = async (userId: string) => {
  try {
    blogConfig.value = await getBlogConfig(userId)
  } catch (error) {
    blogConfig.value = null
    console.error('加载博客配置失败', error)
  }
}

const loadCategories = async () => {
  try {
    const res = await getAllCategories()
    categories.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

const getList = async () => {
  if (!queryParams.userId) {
    return
  }

  try {
    const res = await getUserArticleList({ ...queryParams })
    articleList.value = Array.isArray(res.records) ? res.records : []
    total.value = Number(res.total || 0)
  } catch (error) {
    ElMessage.error(getErrorMessage(error) || '获取文章列表失败')
  }
}

const filterByCategory = (categoryId: number | string | null) => {
  queryParams.categoryId = categoryId
  queryParams.pageNum = 1
  getList()
}

const handlePageChange = () => {
  getList()
}

const viewArticle = (articleId: number | string) => {
  if (!queryParams.userId) {
    return
  }

  router.push(`/blog/${queryParams.userId}/article/${articleId}`)
}

const initPage = async () => {
  const userId = getFirstRouteParam(route.params.userId)
  if (!userId) {
    return
  }

  queryParams.userId = userId
  await Promise.all([loadBlogConfig(userId), loadCategories()])
  await getList()
}

onMounted(() => {
  initPage()
})
</script>

<style scoped>
.blog-home-container {
  min-height: calc(100vh - 68px);
}

.blog-hero-section :deep(.cn-section__body) {
  padding: 0;
}

.blog-hero {
  position: relative;
  overflow: hidden;
  min-height: 300px;
  border: 1px solid var(--cn-panel-border);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-brand-primary) 78%, var(--cn-color-info));
  background-position: center;
  background-size: cover;
  box-shadow: var(--cn-shadow-card);
}

.blog-hero--cover {
  background-position: center;
  background-size: cover;
}

.blog-hero__overlay {
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--cn-color-text-primary) 48%, transparent);
}

.blog-hero__content {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: var(--cn-space-4);
  min-height: 300px;
  padding: var(--cn-space-8) var(--cn-space-6);
  color: white;
  text-align: center;
}

.blog-hero__avatar {
  border: 4px solid color-mix(in srgb, white 86%, transparent);
  box-shadow: 0 14px 32px color-mix(in srgb, var(--cn-color-text-primary) 28%, transparent);
}

.blog-hero__copy {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  max-width: 720px;
}

.blog-hero__eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
  text-transform: uppercase;
  opacity: 0.82;
}

.blog-hero__title {
  margin: 0;
  font-family: var(--cn-font-heading);
  font-size: clamp(28px, 5vw, 42px);
  font-weight: 750;
  line-height: 1.16;
}

.blog-hero__description {
  margin: 0;
  max-width: 640px;
  font-size: 15px;
  line-height: 1.75;
  opacity: 0.9;
}

.blog-hero__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-2);
}

.blog-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
  gap: var(--cn-space-5);
}

.blog-main,
.blog-sidebar {
  min-width: 0;
}

.blog-sidebar {
  display: grid;
  gap: var(--cn-space-4);
}

.article-list {
  display: grid;
  gap: var(--cn-space-4);
}

.article-card {
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.article-card:hover,
.article-card:focus-visible {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 34%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-card);
  transform: translateY(-2px);
  outline: none;
}

.article-cover {
  position: relative;
  width: 100%;
  height: 210px;
  overflow: hidden;
  background: var(--cn-color-bg-surface-muted);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--cn-motion-base) var(--cn-ease-out);
}

.article-card:hover .article-cover img {
  transform: scale(1.025);
}

.top-tag {
  position: absolute;
  top: var(--cn-space-3);
  right: var(--cn-space-3);
}

.article-info {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-5);
}

.article-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 20px;
  font-weight: 650;
  line-height: 1.45;
}

.article-summary {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.article-time {
  margin-left: auto;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-5);
}

.category-list,
.social-links {
  display: grid;
  gap: var(--cn-space-2);
}

.category-item {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  background: transparent;
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font: inherit;
  padding: 10px 12px;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.category-item:hover {
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-primary);
}

.category-item.active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-weight: 700;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  border-radius: var(--cn-radius-card);
  color: var(--cn-color-brand-primary);
  font-size: 14px;
  font-weight: 650;
  padding: 10px 12px;
  text-decoration: none;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.social-link:hover {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-hover);
}

@media (max-width: 960px) {
  .blog-layout {
    grid-template-columns: 1fr;
  }

  .blog-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .blog-hero,
  .blog-hero__content {
    min-height: 260px;
  }

  .blog-hero__content {
    padding: var(--cn-space-6) var(--cn-space-4);
  }

  .article-time {
    width: 100%;
    margin-left: 0;
  }

  .blog-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>
