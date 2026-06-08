<template>
  <CnPage class="my-decks-page" max-width="1180px" full-height>
    <CnPageHeader
      title="我的卡组"
      description="管理自己创建或收藏的闪卡卡组，继续编辑、学习或进入详情。"
      eyebrow="MY FLASHCARD DECKS"
      :breadcrumbs="[{ label: '闪卡记忆', to: '/flashcard' }, { label: '我的卡组' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ deckList.length }} 个卡组</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>显示学习进度</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="router.push('/flashcard')">返回</el-button>
        <el-button type="primary" :icon="Plus" @click="goToCreate">创建卡组</el-button>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="我的卡组" :value="deckList.length" description="当前账户下的卡组数量" tone="brand" :loading="loading" />
      <CnStatCard title="总卡片" :value="totalCards" description="卡组内卡片总量" tone="info" :loading="loading" />
      <CnStatCard title="待复习" :value="totalDue" description="当前待复习卡片" tone="warning" :loading="loading" />
    </div>

    <CnSection title="卡组列表" description="点击卡组进入详情，继续管理卡片或启动学习。" divided>
      <template #actions>
        <el-button type="primary" :icon="Plus" @click="goToCreate">新建卡组</el-button>
      </template>

      <div v-loading="loading" class="deck-shell">
        <div v-if="deckList.length" class="deck-list">
          <DeckCard
            v-for="deck in deckList"
            :key="deck.id"
            :deck="deck"
            :show-progress="true"
            @click="goToDeckDetail(deck)"
          />
        </div>

        <CnEmptyState
          v-else-if="!loading"
          title="还没有卡组"
          description="创建一个卡组，把面试题、知识点或错题沉淀成长期记忆材料。"
          icon="MD"
          surface="transparent"
          size="sm"
        >
          <template #actions>
            <el-button type="primary" :icon="Plus" @click="goToCreate">创建第一个卡组</el-button>
          </template>
        </CnEmptyState>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { flashcardApi } from '@/api/flashcard'
import DeckCard from './components/DeckCard.vue'

interface DeckItem extends Record<string, unknown> {
  id: number | string
  cardCount?: number
  dueCount?: number
}

const router = useRouter()
const loading = ref(false)
const deckList = ref<DeckItem[]>([])

const totalCards = computed(() => deckList.value.reduce((total, deck) => total + Number(deck.cardCount || 0), 0))
const totalDue = computed(() => deckList.value.reduce((total, deck) => total + Number(deck.dueCount || 0), 0))

const loadMyDecks = async () => {
  loading.value = true
  try {
    const data = (await flashcardApi.getMyDecks()) as DeckItem[]
    deckList.value = data || []
  } catch (error) {
    console.error('加载卡组失败:', error)
    ElMessage.error('加载卡组失败')
  } finally {
    loading.value = false
  }
}

const goToCreate = () => {
  router.push('/flashcard/deck/create')
}

const goToDeckDetail = (deck: DeckItem) => {
  router.push(`/flashcard/deck/${deck.id}`)
}

onMounted(() => {
  loadMyDecks()
})
</script>

<style scoped>
.my-decks-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.deck-shell {
  min-height: 220px;
}

.deck-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: var(--cn-space-4);
}

:deep(.deck-card) {
  border-color: var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

@media (max-width: 820px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .deck-list {
    grid-template-columns: 1fr;
  }
}
</style>
