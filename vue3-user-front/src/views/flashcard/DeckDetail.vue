<template>
  <div class="deck-detail" v-loading="loading">
    <div class="page-header">
      <el-button :icon="ArrowLeft" text @click="$router.push('/flashcard')">返回</el-button>
    </div>

    <div class="deck-info-card" v-if="deck">
      <div class="deck-cover">
        <img v-if="deck.coverImage" :src="deck.coverImage" alt="封面" />
        <div v-else class="default-cover">
          <el-icon :size="48"><Collection /></el-icon>
        </div>
      </div>
      
      <div class="deck-content">
        <div class="deck-header">
          <h1 class="deck-name">{{ deck.name }}</h1>
          <el-tag v-if="deck.isPublic" type="success" size="small">公开</el-tag>
          <el-tag v-else type="info" size="small">私有</el-tag>
        </div>
        
        <p class="deck-description">{{ deck.description || '暂无描述' }}</p>
        
        <div class="deck-tags" v-if="deck.tags">
          <el-tag 
            v-for="tag in tagList" 
            :key="tag"
            size="small"
            effect="plain"
          >
            {{ tag }}
          </el-tag>
        </div>
        
        <div class="deck-meta">
          <div class="meta-item">
            <el-avatar :size="24" :src="deck.userAvatar">
              {{ deck.userName?.charAt(0) }}
            </el-avatar>
            <span>{{ deck.userName }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Files /></el-icon>
            <span>{{ deck.cardCount || 0 }} 张卡片</span>
          </div>
          <div class="meta-item">
            <el-icon><DocumentCopy /></el-icon>
            <span>{{ deck.forkCount || 0 }} 次Fork</span>
          </div>
          <div class="meta-item" v-if="deck.learnedCount !== undefined">
            <el-icon><Check /></el-icon>
            <span>已学 {{ deck.learnedCount }}</span>
          </div>
        </div>
        
        <div class="deck-actions">
          <el-button type="primary" size="large" :icon="Reading" @click="startStudy" :disabled="!deck.cardCount">
            开始学习
          </el-button>
          <el-button v-if="!isOwner && isLoggedIn" :icon="DocumentCopy" @click="handleFork">
            Fork到我的卡组
          </el-button>
          <template v-if="isOwner">
            <el-button :icon="Edit" @click="goToEdit">编辑卡组</el-button>
            <el-button :icon="Plus" @click="goToAddCards">添加闪卡</el-button>
            <el-button type="danger" :icon="Delete" @click="handleDelete">删除</el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 闪卡列表 -->
    <div class="cards-section" v-if="deck">
      <div class="section-header">
        <h2>闪卡列表</h2>
        <span class="card-count">{{ cardList.length }} 张</span>
      </div>
      
      <div class="card-list" v-if="cardList.length > 0">
        <div 
          v-for="(card, index) in cardList" 
          :key="card.id"
          class="card-item"
        >
          <div class="card-index">{{ index + 1 }}</div>
          <div class="card-content">
            <div class="card-front">
              <span class="label">正面</span>
              <div class="content" v-html="renderContent(card.frontContent, card.contentType)"></div>
            </div>
            <div class="card-back">
              <span class="label">背面</span>
              <div class="content" v-html="renderContent(card.backContent, card.contentType)"></div>
            </div>
          </div>
          <div class="card-status" v-if="card.studyStatus">
            <el-tag 
              :type="getMasteryType(card.studyStatus.masteryLevel)" 
              size="small"
            >
              {{ getMasteryText(card.studyStatus.masteryLevel) }}
            </el-tag>
          </div>
          <div class="card-actions" v-if="isOwner">
            <el-button text :icon="Delete" @click="handleDeleteCard(card)" />
          </div>
        </div>
      </div>
      
      <el-empty v-else description="暂无闪卡，点击添加闪卡开始" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Collection, Files, DocumentCopy, Check, Reading, Edit, Plus, Delete } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { flashcardApi } from '@/api/flashcard'
import { renderMarkdown, sanitizeHtml } from '@/utils/markdown'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const deck = ref(null)
const cardList = ref([])

const deckId = computed(() => route.params.id)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const isOwner = computed(() => deck.value && userStore.userInfo?.id === deck.value.userId)

const tagList = computed(() => {
  if (!deck.value?.tags) return []
  return deck.value.tags.split(',').filter(t => t.trim())
})

// 加载卡组详情
const loadDeckDetail = async () => {
  loading.value = true
  try {
    if (isLoggedIn.value) {
      deck.value = await flashcardApi.getDeckById(deckId.value)
      cardList.value = await flashcardApi.getCardsByDeckId(deckId.value)
    } else {
      deck.value = await flashcardApi.getPublicDeckById(deckId.value)
      cardList.value = await flashcardApi.getPublicDeckCards(deckId.value)
    }
  } catch (error) {
    console.error('加载卡组失败:', error)
    ElMessage.error('加载卡组失败')
    router.push('/flashcard')
  } finally {
    loading.value = false
  }
}

// 渲染内容
const renderContent = (content, contentType) => {
  if (!content) return ''
  if (contentType === 2) { // Markdown
    return renderMarkdown(content)
  }
  if (contentType === 3) { // 代码
    return sanitizeHtml(`<pre><code>${escapeHtml(content)}</code></pre>`)
  }
  return sanitizeHtml(escapeHtml(content).replace(/\n/g, '<br>'))
}

const escapeHtml = (text) => {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

// 掌握程度
const getMasteryType = (level) => {
  const types = { 1: 'info', 2: 'warning', 3: 'success' }
  return types[level] || 'info'
}

const getMasteryText = (level) => {
  const texts = { 1: '新学', 2: '学习中', 3: '已掌握' }
  return texts[level] || '未学习'
}

// 操作
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
    ElMessage.error(error.message || 'Fork失败')
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
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleDeleteCard = async (card) => {
  try {
    await ElMessageBox.confirm('确定要删除这张闪卡吗？', '删除确认', {
      type: 'warning'
    })
    
    await flashcardApi.deleteCard(card.id)
    ElMessage.success('闪卡已删除')
    cardList.value = cardList.value.filter(c => c.id !== card.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadDeckDetail()
})
</script>

<style lang="scss" scoped>
.deck-detail {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 16px;
}

.deck-info-card {
  display: flex;
  gap: 24px;
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
  margin-bottom: 24px;
}

.deck-cover {
  width: 200px;
  height: 150px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary-light-5));
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  .default-cover {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--el-color-primary);
    opacity: 0.5;
  }
}

.deck-content {
  flex: 1;
  min-width: 0;
}

.deck-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  
  .deck-name {
    font-size: 24px;
    font-weight: 600;
    margin: 0;
    color: var(--el-text-color-primary);
  }
}

.deck-description {
  color: var(--el-text-color-secondary);
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.deck-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.deck-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

.deck-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.cards-section {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--el-border-color-light);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  
  h2 {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
  }
  
  .card-count {
    padding: 2px 10px;
    background: var(--el-fill-color-light);
    border-radius: 10px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  
  .card-index {
    width: 28px;
    height: 28px;
    background: var(--el-color-primary-light-8);
    color: var(--el-color-primary);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }
  
  .card-content {
    flex: 1;
    min-width: 0;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
    
    .card-front, .card-back {
      .label {
        display: block;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        margin-bottom: 4px;
      }
      
      .content {
        font-size: 14px;
        line-height: 1.6;
        color: var(--el-text-color-primary);
        
        :deep(pre) {
          background: var(--el-fill-color);
          padding: 8px;
          border-radius: 4px;
          overflow-x: auto;
        }
      }
    }
  }
  
  .card-status {
    flex-shrink: 0;
  }
  
  .card-actions {
    flex-shrink: 0;
  }
}

@media (max-width: 768px) {
  .deck-detail {
    padding: 16px;
  }
  
  .deck-info-card {
    flex-direction: column;
    
    .deck-cover {
      width: 100%;
      height: 180px;
    }
  }
  
  .card-item .card-content {
    grid-template-columns: 1fr;
  }
}
</style>
