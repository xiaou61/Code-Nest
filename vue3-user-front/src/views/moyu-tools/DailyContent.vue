<template>
  <CnPage class="daily-content-page" max-width="1280px" full-height>
    <CnPageHeader
      title="每日内容"
      description="每天一点小知识，按编程格言、技术小贴士、代码片段和历史事件持续积累。"
      eyebrow="MOYU TOOL"
      :breadcrumbs="[{ label: '摸鱼工具箱', to: '/moyu-tools' }, { label: '每日内容' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ activeTabLabel }}</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>今日 {{ todayContents.length }} 条</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>列表 {{ currentContents.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回工具箱</el-button>
        <el-button type="primary" :icon="Refresh" :loading="todayLoading || contentLoading" @click="refreshAll">
          刷新内容
        </el-button>
      </template>
    </CnPageHeader>

    <section class="content-stats" aria-label="每日内容概览">
      <CnStatCard
        title="今日推荐"
        :value="todayContents.length"
        unit="条"
        description="系统推荐的今日阅读内容"
        tone="brand"
        trend="flat"
        trend-text="今日"
      />
      <CnStatCard
        title="当前分类"
        :value="activeTabLabel"
        :description="contentSummary"
        tone="info"
        trend="flat"
        trend-text="分类"
      />
      <CnStatCard
        title="已收藏"
        :value="collectedCount"
        unit="条"
        description="当前列表中已收藏内容"
        tone="warning"
        trend="flat"
        trend-text="收藏"
      />
    </section>

    <CnSection title="今日推荐" description="快速浏览今日精选内容，可以进入详情、点赞或收藏。" divided>
      <template #actions>
        <el-button :icon="Refresh" :loading="todayLoading" @click="refreshTodayContent">刷新今日推荐</el-button>
      </template>

      <div v-loading="todayLoading" class="content-grid">
        <article
          v-for="content in todayContents"
          :key="content.id"
          class="content-card"
          @click="showContentDetail(content)"
        >
          <header class="content-card__header">
            <CnStatusTag :type="getContentTypeTone(content.contentType)" size="sm">
              <el-icon><component :is="getContentIcon(content.contentType)" /></el-icon>
              {{ getContentTypeName(content.contentType) }}
            </CnStatusTag>
            <CnStatusTag
              v-if="content.difficultyLevel"
              :type="getDifficultyTone(content.difficultyLevel)"
              size="sm"
              subtle
            >
              {{ getDifficultyName(content.difficultyLevel) }}
            </CnStatusTag>
          </header>

          <div class="content-card__body">
            <h3>{{ content.title }}</h3>
            <div class="content-preview" v-html="formatContent(content.content)" />
            <div v-if="content.author" class="content-author">
              <el-icon><User /></el-icon>
              {{ content.author }}
            </div>
          </div>

          <footer class="content-card__footer">
            <div class="content-counts">
              <span>
                <el-icon><View /></el-icon>
                {{ content.viewCount || 0 }}
              </span>
              <span :class="{ 'is-liked': content.userLiked }">
                <el-icon><Star /></el-icon>
                {{ content.likeCount || 0 }}
              </span>
            </div>
            <div class="content-actions">
              <el-button
                size="small"
                :type="content.userLiked ? 'primary' : 'default'"
                @click.stop="toggleLike(content)"
              >
                <el-icon><Star /></el-icon>
                {{ content.userLiked ? '已赞' : '点赞' }}
              </el-button>
              <el-button
                size="small"
                :type="content.userCollected ? 'warning' : 'default'"
                @click.stop="toggleCollect(content)"
              >
                <el-icon><Collection /></el-icon>
                {{ content.userCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </footer>
        </article>

        <CnEmptyState
          v-if="!todayLoading && todayContents.length === 0"
          title="暂无今日内容"
          description="可以刷新今日推荐后再试。"
          icon="DAY"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" :loading="todayLoading" @click="refreshTodayContent">刷新试试</el-button>
          </template>
        </CnEmptyState>
      </div>
    </CnSection>

    <CnSection title="内容分类" :description="contentSummary" divided>
      <el-tabs v-model="activeTab" class="category-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="编程格言" name="1">
          <template #label>
            <span class="tab-label">
              <el-icon><ChatLineRound /></el-icon>
              编程格言
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="技术小贴士" name="2">
          <template #label>
            <span class="tab-label">
              <el-icon><Notification /></el-icon>
              技术小贴士
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="代码片段" name="3">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              代码片段
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="历史上的今天" name="4">
          <template #label>
            <span class="tab-label">
              <el-icon><Clock /></el-icon>
              历史上的今天
            </span>
          </template>
        </el-tab-pane>

        <el-tab-pane label="我的收藏" name="collections">
          <template #label>
            <span class="tab-label">
              <el-icon><Star /></el-icon>
              我的收藏
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div v-loading="contentLoading" class="content-grid content-grid--list">
        <article
          v-for="content in currentContents"
          :key="content.id"
          class="content-card"
          @click="showContentDetail(content)"
        >
          <header class="content-card__header">
            <CnStatusTag :type="getContentTypeTone(content.contentType)" size="sm">
              <el-icon><component :is="getContentIcon(content.contentType)" /></el-icon>
              {{ getContentTypeName(content.contentType) }}
            </CnStatusTag>
            <div class="content-meta">
              <CnStatusTag
                v-if="content.difficultyLevel"
                :type="getDifficultyTone(content.difficultyLevel)"
                size="sm"
                subtle
              >
                {{ getDifficultyName(content.difficultyLevel) }}
              </CnStatusTag>
              <span class="content-date">{{ formatDate(content.createTime) }}</span>
            </div>
          </header>

          <div class="content-card__body">
            <h3>{{ content.title }}</h3>
            <div class="content-preview" v-html="formatContent(content.content)" />

            <div v-if="content.tags?.length" class="tag-list">
              <CnStatusTag v-for="tag in content.tags" :key="tag" type="neutral" size="sm" subtle>
                {{ tag }}
              </CnStatusTag>
            </div>

            <div v-if="content.author" class="content-author">
              <el-icon><User /></el-icon>
              {{ content.author }}
            </div>
          </div>

          <footer class="content-card__footer">
            <div class="content-counts">
              <span>
                <el-icon><View /></el-icon>
                {{ content.viewCount || 0 }}
              </span>
              <span :class="{ 'is-liked': content.userLiked }">
                <el-icon><Star /></el-icon>
                {{ content.likeCount || 0 }}
              </span>
            </div>
            <div class="content-actions">
              <el-button
                size="small"
                :type="content.userLiked ? 'primary' : 'default'"
                @click.stop="toggleLike(content)"
              >
                <el-icon><Star /></el-icon>
                {{ content.userLiked ? '已赞' : '点赞' }}
              </el-button>
              <el-button
                size="small"
                :type="content.userCollected ? 'warning' : 'default'"
                @click.stop="toggleCollect(content)"
              >
                <el-icon><Collection /></el-icon>
                {{ content.userCollected ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </footer>
        </article>

        <CnEmptyState
          v-if="!contentLoading && currentContents.length === 0"
          title="暂无内容"
          description="当前分类还没有内容，可以刷新试试。"
          icon="DAY"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" @click="refreshContent">刷新试试</el-button>
          </template>
        </CnEmptyState>
      </div>
    </CnSection>

    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedContent?.title"
      width="min(840px, 92vw)"
      destroy-on-close
    >
      <div v-if="selectedContent" class="content-detail">
        <div class="detail-header">
          <div class="detail-meta">
            <CnStatusTag :type="getContentTypeTone(selectedContent.contentType)" size="lg">
              <el-icon><component :is="getContentIcon(selectedContent.contentType)" /></el-icon>
              {{ getContentTypeName(selectedContent.contentType) }}
            </CnStatusTag>
            <div class="detail-tags">
              <CnStatusTag
                v-if="selectedContent.difficultyLevel"
                :type="getDifficultyTone(selectedContent.difficultyLevel)"
                size="sm"
              >
                {{ getDifficultyName(selectedContent.difficultyLevel) }}
              </CnStatusTag>
              <CnStatusTag v-if="selectedContent.programmingLanguage" type="success" size="sm">
                {{ selectedContent.programmingLanguage }}
              </CnStatusTag>
            </div>
          </div>
          <div class="detail-stats">
            <span>
              <el-icon><View /></el-icon>
              {{ selectedContent.viewCount || 0 }} 查看
            </span>
            <span>
              <el-icon><Star /></el-icon>
              {{ selectedContent.likeCount || 0 }} 点赞
            </span>
          </div>
        </div>

        <div class="detail-content" v-html="formatDetailContent(selectedContent.content)" />

        <div v-if="selectedContent.tags?.length" class="detail-block">
          <h4>标签</h4>
          <div class="tag-list">
            <CnStatusTag v-for="tag in selectedContent.tags" :key="tag" type="neutral" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
          </div>
        </div>

        <div v-if="selectedContent.author" class="detail-block">
          <h4>作者</h4>
          <div class="author-info">
            <el-icon><User /></el-icon>
            <span>{{ selectedContent.author }}</span>
          </div>
        </div>

        <div v-if="selectedContent.sourceUrl" class="detail-block">
          <h4>来源</h4>
          <el-link :href="selectedContent.sourceUrl" target="_blank" type="primary">
            <el-icon><Link /></el-icon>
            {{ selectedContent.sourceUrl }}
          </el-link>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button
            :type="selectedContent?.userLiked ? 'primary' : 'default'"
            @click="toggleLike(selectedContent)"
          >
            <el-icon><Star /></el-icon>
            {{ selectedContent?.userLiked ? '已赞' : '点赞' }}
          </el-button>
          <el-button
            :type="selectedContent?.userCollected ? 'warning' : 'default'"
            @click="toggleCollect(selectedContent)"
          >
            <el-icon><Collection /></el-icon>
            {{ selectedContent?.userCollected ? '已收藏' : '收藏' }}
          </el-button>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  ChatLineRound,
  Clock,
  Collection,
  Document,
  Link,
  Notification,
  Refresh,
  Star,
  User,
  View
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import {
  collectContent,
  getContentsByType,
  getTodayContent,
  getUserCollections,
  likeContent
} from '@/api/moyu'
import { sanitizeHtml } from '@/utils/markdown'

type ContentType = 1 | 2 | 3 | 4
type TabName = `${ContentType}` | 'collections'

interface DailyContentItem {
  id: number | string
  title: string
  content?: string
  contentType: ContentType
  difficultyLevel?: number
  createTime?: string | Date
  tags?: string[]
  author?: string
  sourceUrl?: string
  programmingLanguage?: string
  viewCount?: number
  likeCount?: number
  userLiked?: boolean
  userCollected?: boolean
}

const router = useRouter()

const todayLoading = ref(false)
const contentLoading = ref(false)
const todayContents = ref<DailyContentItem[]>([])
const currentContents = ref<DailyContentItem[]>([])
const activeTab = ref<TabName>('1')
const detailDialogVisible = ref(false)
const selectedContent = ref<DailyContentItem | null>(null)

const activeTabLabel = computed(() => {
  if (activeTab.value === 'collections') return '我的收藏'
  return getContentTypeName(Number(activeTab.value))
})

const collectedCount = computed(() => currentContents.value.filter((content) => content.userCollected).length)

const contentSummary = computed(() => {
  return `${activeTabLabel.value}当前展示 ${currentContents.value.length} 条内容。`
})

const goBack = () => {
  router.push('/moyu-tools')
}

const getContentTypeName = (type?: number) => {
  const typeMap: Record<number, string> = {
    1: '编程格言',
    2: '技术小贴士',
    3: '代码片段',
    4: '历史上的今天'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

const getContentTypeTone = (type?: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'brand',
    2: 'info',
    3: 'success',
    4: 'warning'
  }
  return type ? toneMap[type] || 'neutral' : 'neutral'
}

const getContentIcon = (type?: number): Component => {
  const iconMap: Record<number, Component> = {
    1: ChatLineRound,
    2: Notification,
    3: Document,
    4: Clock
  }
  return type ? iconMap[type] || Document : Document
}

const getDifficultyName = (level?: number) => {
  const levelMap: Record<number, string> = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return level ? levelMap[level] || '未知' : '未知'
}

const getDifficultyTone = (level?: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return level ? toneMap[level] || 'info' : 'info'
}

const formatContent = (content?: string) => {
  if (!content) return ''

  let formatted = escapeHtml(content).replace(/\n/g, '<br>')

  if (content.includes('```') || content.includes('`')) {
    formatted = formatted.replace(/```([^`]+)```/g, '<pre><code>$1</code></pre>')
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>')
  }

  if (formatted.length > 200) {
    formatted = `${formatted.substring(0, 200)}...`
  }

  return sanitizeHtml(formatted)
}

const formatDetailContent = (content?: string) => {
  if (!content) return ''

  let formatted = escapeHtml(content).replace(/\n/g, '<br>')
  formatted = formatted.replace(/```([^`]+)```/g, '<pre class="code-block"><code>$1</code></pre>')
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')

  return sanitizeHtml(formatted)
}

const escapeHtml = (text: string) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const formatDate = (dateTime?: string | Date) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

const loadTodayContent = async () => {
  try {
    todayLoading.value = true
    const data = (await getTodayContent()) as DailyContentItem[] | null
    todayContents.value = data || []
  } catch (error) {
    console.error('加载今日内容失败:', error)
    ElMessage.error('加载今日内容失败')
  } finally {
    todayLoading.value = false
  }
}

const loadContentsByType = async (contentType: number) => {
  try {
    contentLoading.value = true
    const data = (await getContentsByType(contentType)) as DailyContentItem[] | null
    currentContents.value = data || []
  } catch (error) {
    console.error('加载内容列表失败:', error)
    ElMessage.error('加载内容列表失败')
  } finally {
    contentLoading.value = false
  }
}

const loadUserCollections = async () => {
  try {
    contentLoading.value = true
    const data = (await getUserCollections()) as DailyContentItem[] | null
    currentContents.value = data || []
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载收藏列表失败')
  } finally {
    contentLoading.value = false
  }
}

const refreshTodayContent = () => {
  loadTodayContent()
}

const refreshContent = () => {
  handleTabChange(activeTab.value)
}

const refreshAll = () => {
  loadTodayContent()
  refreshContent()
}

const handleTabChange = (tabName: string | number) => {
  const nextTab = String(tabName) as TabName
  activeTab.value = nextTab

  if (nextTab === 'collections') {
    loadUserCollections()
  } else {
    loadContentsByType(Number.parseInt(nextTab, 10))
  }
}

const showContentDetail = (content: DailyContentItem) => {
  selectedContent.value = content
  detailDialogVisible.value = true
}

const toggleLike = async (content?: DailyContentItem | null) => {
  if (!content) return

  try {
    await likeContent(content.id)
    content.userLiked = !content.userLiked
    content.likeCount = (content.likeCount || 0) + (content.userLiked ? 1 : -1)
    ElMessage.success(content.userLiked ? '点赞成功' : '取消点赞')
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const toggleCollect = async (content?: DailyContentItem | null) => {
  if (!content) return

  try {
    await collectContent(content.id)
    content.userCollected = !content.userCollected
    ElMessage.success(content.userCollected ? '收藏成功' : '取消收藏')
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadTodayContent()
  loadContentsByType(1)
})
</script>

<style scoped>
.daily-content-page {
  min-width: 0;
}

.content-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--cn-space-4);
  min-height: 180px;
}

.content-grid--list {
  margin-top: var(--cn-space-4);
}

.category-tabs {
  min-width: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.content-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.content-card:hover {
  transform: translateY(-2px);
  border-color: var(--cn-color-brand-primary);
  box-shadow: var(--cn-shadow-sm);
}

.content-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
}

.content-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
  min-width: 0;
}

.content-date {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 22px;
  white-space: nowrap;
}

.content-card__body {
  display: grid;
  align-content: start;
  gap: var(--cn-space-3);
  min-width: 0;
}

.content-card__body h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 700;
  line-height: 1.45;
}

.content-preview {
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.content-preview :deep(code),
.detail-content :deep(.inline-code) {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-danger);
  font-size: 0.92em;
}

.content-preview :deep(pre),
.detail-content :deep(.code-block) {
  overflow-x: auto;
  margin: var(--cn-space-3) 0;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-text-primary);
  color: var(--cn-color-bg-surface);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.content-author,
.author-info {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.content-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.content-counts,
.detail-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.content-counts span,
.detail-stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.content-counts .is-liked {
  color: var(--cn-color-warning);
}

.content-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.content-detail {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.detail-meta {
  display: grid;
  gap: var(--cn-space-3);
  min-width: 0;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.detail-content {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  line-height: 1.85;
}

.detail-block {
  display: grid;
  gap: var(--cn-space-3);
}

.detail-block h4 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
}

@media (max-width: 980px) {
  .content-stats {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .content-card__header,
  .content-card__footer,
  .detail-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .content-meta,
  .content-actions,
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
