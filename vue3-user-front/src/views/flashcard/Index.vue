<template>
  <CnPage class="flashcard-index" max-width="1240px" full-height>
    <CnPageHeader
      title="闪卡记忆工作区"
      description="把卡组检索、间隔复习和学习热力图整合在同一视图，持续巩固知识点。"
      eyebrow="FLASHCARD LAB"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">公开卡组 {{ deckList.length }}</CnStatusTag>
        <CnStatusTag type="warning" size="sm" subtle>今日待复习 {{ stats?.todayDueCount || 0 }}</CnStatusTag>
        <CnStatusTag :type="isLoggedIn ? 'success' : 'neutral'" size="sm" subtle>
          {{ isLoggedIn ? '已登录' : '未登录' }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Collection" @click="goToMyDecks">我的卡组</el-button>
        <el-button type="primary" :icon="Reading" @click="goToStudy">今日学习</el-button>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard title="公开卡组" :value="deckList.length" description="当前筛选结果" tone="brand" :loading="loading" />
      <CnStatCard title="今日已学" :value="stats?.todayLearnedCount || 0" description="登录后统计" tone="success" />
      <CnStatCard title="待复习" :value="stats?.todayDueCount || 0" description="今日到期卡片" tone="warning" />
      <CnStatCard title="连续天数" :value="stats?.streakDays || 0" unit="天" description="最近学习连续记录" tone="info" />
    </div>

    <div class="workspace-grid">
      <aside class="side-stack">
        <CnSection title="检索卡组" description="按关键词和标签筛选公开卡组。" divided>
          <div class="search-stack">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索卡组..."
              clearable
              :prefix-icon="Search"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>

            <el-input
              v-model="filterTags"
              placeholder="按标签筛选"
              clearable
              :prefix-icon="PriceTag"
              @change="handleSearch"
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            />
          </div>
        </CnSection>

        <CnSection title="快捷功能" description="进入学习、管理和创建流程。" divided>
          <div class="quick-actions">
            <button type="button" class="quick-action" @click="goToStudy">
              <span class="quick-icon is-study">
                <el-icon><Reading /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>今日学习</strong>
                <small>{{ stats?.todayDueCount || 0 }} 张待复习</small>
              </span>
              <CnStatusTag v-if="stats?.todayDueCount" type="warning" size="sm">{{ stats.todayDueCount }}</CnStatusTag>
            </button>

            <button type="button" class="quick-action" @click="goToMyDecks">
              <span class="quick-icon is-deck">
                <el-icon><Collection /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>我的卡组</strong>
                <small>管理自建和 Fork 卡组</small>
              </span>
            </button>

            <button type="button" class="quick-action" @click="goToCreate">
              <span class="quick-icon is-create">
                <el-icon><Plus /></el-icon>
              </span>
              <span class="quick-copy">
                <strong>创建卡组</strong>
                <small>沉淀新的记忆材料</small>
              </span>
            </button>
          </div>
        </CnSection>

        <StudyStats v-if="isLoggedIn" :stats="stats" />
      </aside>

      <main class="main-stack">
        <Heatmap v-if="isLoggedIn" :data="heatmapData" />

        <CnSection title="公开卡组" description="浏览社区公开卡组，进入详情后可查看和 Fork。" divided>
          <template #actions>
            <CnStatusTag v-if="deckList.length" type="brand" size="sm">{{ deckList.length }} 个卡组</CnStatusTag>
            <el-button type="primary" :icon="Plus" @click="goToCreate">创建卡组</el-button>
          </template>

          <div v-loading="loading" class="deck-shell">
            <div v-if="deckList.length" class="deck-grid">
              <DeckCard
                v-for="deck in deckList"
                :key="deck.id"
                :deck="deck"
                :show-progress="isLoggedIn"
                @click="goToDeckDetail(deck)"
              />
            </div>

            <CnEmptyState
              v-else-if="!loading"
              title="暂无公开卡组"
              description="当前条件下没有卡组，可以调整搜索条件或创建第一个卡组。"
              icon="FC"
              surface="transparent"
              size="sm"
            >
              <template #actions>
                <el-button type="primary" :icon="Plus" @click="goToCreate">创建第一个卡组</el-button>
              </template>
            </CnEmptyState>
          </div>
        </CnSection>
      </main>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection, Plus, PriceTag, Reading, Search } from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { useUserStore } from '@/stores/user'
import { flashcardApi } from '@/api/flashcard'
import DeckCard from './components/DeckCard.vue'
import Heatmap from './components/Heatmap.vue'
import StudyStats from './components/StudyStats.vue'

interface DeckItem extends Record<string, unknown> {
  id: number | string
  name?: string
}

interface StudyStatsData {
  todayLearnedCount?: number
  todayDueCount?: number
  todayNewCount?: number
  streakDays?: number
  masteredCount?: number
  learningCount?: number
  newCount?: number
}

interface HeatmapResponse {
  data?: unknown[]
}

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const searchKeyword = ref('')
const filterTags = ref('')
const deckList = ref<DeckItem[]>([])
const stats = ref<StudyStatsData | null>(null)
const heatmapData = ref<unknown[]>([])

const isLoggedIn = computed(() => Boolean(userStore.isLoggedIn))

const loadPublicDecks = async () => {
  loading.value = true
  try {
    const data = (await flashcardApi.getPublicDecks(searchKeyword.value, filterTags.value)) as DeckItem[]
    deckList.value = data || []
  } catch (error) {
    console.error('加载卡组失败:', error)
    ElMessage.error('加载卡组失败')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  if (!isLoggedIn.value) return
  try {
    stats.value = (await flashcardApi.getStudyStats()) as StudyStatsData
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const loadHeatmap = async () => {
  if (!isLoggedIn.value) return
  try {
    const data = (await flashcardApi.getHeatmap(365)) as HeatmapResponse
    heatmapData.value = data?.data || []
  } catch (error) {
    console.error('加载热力图失败:', error)
  }
}

const handleSearch = () => {
  loadPublicDecks()
}

const goToStudy = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/flashcard/study')
}

const goToMyDecks = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/flashcard/my')
}

const goToCreate = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push('/flashcard/deck/create')
}

const goToDeckDetail = (deck: DeckItem) => {
  router.push(`/flashcard/deck/${deck.id}`)
}

onMounted(() => {
  loadPublicDecks()
  loadStats()
  loadHeatmap()
})
</script>

<style scoped>
.flashcard-index {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.side-stack,
.main-stack {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

.search-stack {
  display: grid;
  gap: var(--cn-space-3);
}

.quick-actions {
  display: grid;
  gap: var(--cn-space-3);
}

.quick-action {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-card-bg);
  color: var(--cn-color-text-primary);
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    background-color var(--cn-motion-base) var(--cn-ease-out);
}

.quick-action:hover,
.quick-action:focus-visible {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 30%, var(--cn-color-border));
  background: var(--cn-color-bg-surface-muted);
  outline: none;
}

.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--cn-radius-card);
  flex-shrink: 0;
  font-size: 18px;
}

.quick-icon.is-study {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.quick-icon.is-deck {
  background: var(--cn-color-success-soft);
  color: var(--cn-color-success);
}

.quick-icon.is-create {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.quick-copy {
  display: grid;
  gap: var(--cn-space-1);
  min-width: 0;
  flex: 1;
}

.quick-copy strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.25;
}

.quick-copy small {
  overflow-wrap: anywhere;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  line-height: 1.35;
}

.deck-shell {
  min-height: 220px;
}

.deck-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--cn-space-4);
}

:deep(.study-stats),
:deep(.heatmap-container),
:deep(.deck-card) {
  border-color: var(--cn-card-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

@media (max-width: 1080px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .side-stack {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .side-stack :deep(.study-stats) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 700px) {
  .summary-grid,
  .side-stack,
  .deck-grid {
    grid-template-columns: 1fr;
  }
}
</style>
