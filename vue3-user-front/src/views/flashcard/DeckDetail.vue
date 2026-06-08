<template>
  <CnPage v-loading="loading" class="deck-detail-page" max-width="1080px" full-height>
    <CnPageHeader
      :title="deck?.name || '卡组详情'"
      :description="deck?.description || '查看卡组信息、闪卡内容和学习入口。'"
      eyebrow="DECK DETAIL"
      :breadcrumbs="[{ label: '闪卡记忆', to: '/flashcard' }, { label: deck?.name || '卡组详情' }]"
    >
      <template #meta>
        <CnStatusTag v-if="deck" :type="deck.isPublic ? 'success' : 'info'" size="sm">
          {{ deck.isPublic ? '公开' : '私有' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>{{ deck?.cardCount || 0 }} 张卡片</CnStatusTag>
        <CnStatusTag v-if="isOwner" type="warning" size="sm" subtle>拥有者</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="router.push('/flashcard')">返回</el-button>
        <el-button type="primary" :icon="Reading" :disabled="!deck?.cardCount" @click="startStudy">开始学习</el-button>
      </template>
    </CnPageHeader>

    <CnSection v-if="deck" title="卡组概览" description="卡组封面、作者、标签和基础学习数据。" divided>
      <div class="deck-overview">
        <div class="deck-cover">
          <img v-if="deck.coverImage" :src="deck.coverImage" alt="卡组封面" />
          <div v-else class="default-cover">
            <el-icon :size="48"><Collection /></el-icon>
          </div>
        </div>

        <div class="deck-content">
          <div class="tag-list" v-if="tagList.length">
            <CnStatusTag v-for="tag in tagList" :key="tag" type="neutral" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
          </div>

          <div class="deck-meta">
            <span>
              <el-avatar :size="24" :src="deck.userAvatar">
                {{ deck.userName?.charAt(0) }}
              </el-avatar>
              {{ deck.userName || '匿名用户' }}
            </span>
            <span>
              <el-icon><Files /></el-icon>
              {{ deck.cardCount || 0 }} 张卡片
            </span>
            <span>
              <el-icon><DocumentCopy /></el-icon>
              {{ deck.forkCount || 0 }} 次 Fork
            </span>
            <span v-if="deck.learnedCount !== undefined">
              <el-icon><Check /></el-icon>
              已学 {{ deck.learnedCount }}
            </span>
          </div>

          <div class="deck-actions">
            <el-button type="primary" :icon="Reading" :disabled="!deck.cardCount" @click="startStudy">开始学习</el-button>
            <el-button v-if="!isOwner && isLoggedIn" :icon="DocumentCopy" @click="handleFork">
              Fork 到我的卡组
            </el-button>
            <template v-if="isOwner">
              <el-button :icon="Edit" @click="goToEdit">编辑卡组</el-button>
              <el-button :icon="Plus" @click="goToAddCards">添加闪卡</el-button>
              <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
            </template>
          </div>
        </div>
      </div>
    </CnSection>

    <div v-if="deck" class="summary-grid">
      <CnStatCard title="卡片总数" :value="deck.cardCount || 0" description="卡组包含的闪卡" tone="brand" />
      <CnStatCard title="Fork 次数" :value="deck.forkCount || 0" description="被复用次数" tone="info" />
      <CnStatCard title="已学卡片" :value="deck.learnedCount ?? 0" description="当前账号学习进度" tone="success" />
    </div>

    <CnSection v-if="deck" title="闪卡列表" description="快速浏览卡片正反面内容和当前掌握程度。" divided>
      <template #actions>
        <CnStatusTag type="brand" size="sm">{{ cardList.length }} 张</CnStatusTag>
        <el-button v-if="isOwner" type="primary" :icon="Plus" @click="goToAddCards">添加闪卡</el-button>
      </template>

      <div v-if="cardList.length" class="card-list">
        <article v-for="(card, index) in cardList" :key="card.id" class="card-item">
          <div class="card-index">{{ index + 1 }}</div>
          <div class="card-content">
            <div class="card-side">
              <span class="label">正面</span>
              <div class="content" v-html="renderContent(card.frontContent, card.contentType)" />
            </div>
            <div class="card-side">
              <span class="label">背面</span>
              <div class="content" v-html="renderContent(card.backContent, card.contentType)" />
            </div>
          </div>
          <div class="card-status" v-if="card.studyStatus">
            <CnStatusTag :type="getMasteryTone(card.studyStatus.masteryLevel)" size="sm">
              {{ getMasteryText(card.studyStatus.masteryLevel) }}
            </CnStatusTag>
          </div>
          <div class="card-actions" v-if="isOwner">
            <el-button link type="danger" :icon="Delete" @click="handleDeleteCard(card)" />
          </div>
        </article>
      </div>

      <CnEmptyState
        v-else
        title="暂无闪卡"
        description="这个卡组还没有闪卡，拥有者可以继续添加内容。"
        icon="CD"
        surface="transparent"
        size="sm"
      >
        <template v-if="isOwner" #actions>
          <el-button type="primary" :icon="Plus" @click="goToAddCards">添加闪卡</el-button>
        </template>
      </CnEmptyState>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Check, Collection, Delete, DocumentCopy, Edit, Files, Plus, Reading } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnTone
} from '@/design-system'
import { useUserStore } from '@/stores/user'
import { flashcardApi } from '@/api/flashcard'
import { renderMarkdown, sanitizeHtml } from '@/utils/markdown'

interface DeckDetail extends Record<string, unknown> {
  id?: number | string
  name?: string
  description?: string
  coverImage?: string
  isPublic?: boolean | number
  tags?: string
  userId?: number | string
  userName?: string
  userAvatar?: string
  cardCount?: number
  forkCount?: number
  learnedCount?: number
}

interface StudyStatus {
  masteryLevel?: number
}

interface FlashcardItem extends Record<string, unknown> {
  id: number | string
  frontContent?: string
  backContent?: string
  contentType?: number
  studyStatus?: StudyStatus
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const deck = ref<DeckDetail | null>(null)
const cardList = ref<FlashcardItem[]>([])

const deckId = computed(() => route.params.id as string)
const isLoggedIn = computed(() => Boolean(userStore.isLoggedIn))
const isOwner = computed(() => {
  return Boolean(deck.value && String(userStore.userInfo?.id) === String(deck.value.userId))
})

const tagList = computed(() => {
  if (!deck.value?.tags) return []
  return deck.value.tags.split(',').map((tag) => tag.trim()).filter(Boolean)
})

const loadDeckDetail = async () => {
  loading.value = true
  try {
    if (isLoggedIn.value) {
      deck.value = (await flashcardApi.getDeckById(deckId.value)) as DeckDetail
      cardList.value = (await flashcardApi.getCardsByDeckId(deckId.value)) as FlashcardItem[]
    } else {
      deck.value = (await flashcardApi.getPublicDeckById(deckId.value)) as DeckDetail
      cardList.value = (await flashcardApi.getPublicDeckCards(deckId.value)) as FlashcardItem[]
    }
    cardList.value = cardList.value || []
  } catch (error) {
    console.error('加载卡组失败:', error)
    ElMessage.error('加载卡组失败')
    router.push('/flashcard')
  } finally {
    loading.value = false
  }
}

const renderContent = (content?: string, contentType?: number) => {
  if (!content) return ''
  if (contentType === 2) {
    return renderMarkdown(content)
  }
  if (contentType === 3) {
    return sanitizeHtml(`<pre><code>${escapeHtml(content)}</code></pre>`)
  }
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text: string) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const getMasteryTone = (level?: number): CnTone => {
  const tones: Record<string, CnTone> = {
    1: 'info',
    2: 'warning',
    3: 'success'
  }
  return tones[String(level)] || 'info'
}

const getMasteryText = (level?: number) => {
  const texts: Record<string, string> = {
    1: '新学',
    2: '学习中',
    3: '已掌握'
  }
  return texts[String(level)] || '未学习'
}

const startStudy = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push(`/flashcard/study/${deckId.value}`)
}

const handleFork = async () => {
  try {
    const newDeckId = await flashcardApi.forkDeck(deckId.value)
    ElMessage.success('Fork成功')
    router.push(`/flashcard/deck/${newDeckId}`)
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Fork失败'
    ElMessage.error(message)
  }
}

const goToEdit = () => {
  router.push(`/flashcard/deck/${deckId.value}/edit`)
}

const goToAddCards = () => {
  router.push(`/flashcard/deck/${deckId.value}/cards`)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这个卡组吗？删除后无法恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })

    await flashcardApi.deleteDeck(deckId.value)
    ElMessage.success('卡组已删除')
    router.push('/flashcard/my')
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '删除失败'
      ElMessage.error(message)
    }
  }
}

const handleDeleteCard = async (card: FlashcardItem) => {
  try {
    await ElMessageBox.confirm('确定要删除这张闪卡吗？', '删除确认', {
      type: 'warning'
    })

    await flashcardApi.deleteCard(card.id)
    ElMessage.success('闪卡已删除')
    cardList.value = cardList.value.filter((item) => item.id !== card.id)
  } catch (error) {
    if (error !== 'cancel') {
      const message = error instanceof Error ? error.message : '删除失败'
      ElMessage.error(message)
    }
  }
}

onMounted(() => {
  loadDeckDetail()
})
</script>

<style scoped>
.deck-detail-page {
  min-height: calc(100vh - 68px);
}

.deck-overview {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.deck-cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 74%, var(--cn-color-brand-soft));
}

.deck-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--cn-color-brand-primary);
}

.deck-content {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.tag-list,
.deck-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.deck-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-4);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.deck-meta span {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.card-list {
  display: grid;
  gap: var(--cn-space-3);
}

.card-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto auto;
  gap: var(--cn-space-3);
  align-items: start;
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.card-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-size: 12px;
  font-weight: 800;
}

.card-content {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
  min-width: 0;
}

.card-side {
  min-width: 0;
}

.label {
  display: block;
  margin-bottom: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 750;
}

.content {
  overflow-wrap: anywhere;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.7;
}

.content :deep(pre) {
  max-width: 100%;
  margin: 0;
  padding: var(--cn-space-3);
  overflow-x: auto;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface);
}

.card-status,
.card-actions {
  flex-shrink: 0;
}

@media (max-width: 860px) {
  .deck-overview,
  .summary-grid,
  .card-content {
    grid-template-columns: 1fr;
  }

  .card-item {
    grid-template-columns: 36px minmax(0, 1fr);
  }

  .card-status,
  .card-actions {
    grid-column: 2;
  }
}

@media (max-width: 560px) {
  .deck-actions :deep(.el-button) {
    width: 100%;
  }

  .card-item {
    grid-template-columns: 1fr;
  }

  .card-status,
  .card-actions {
    grid-column: auto;
  }
}
</style>
