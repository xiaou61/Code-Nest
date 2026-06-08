<template>
  <CnPage v-loading="loading" class="study-page" max-width="980px" full-height>
    <CnPageHeader
      :title="deckName || '今日闪卡学习'"
      :description="deckId ? '按当前卡组进行集中复习，翻开答案后选择记忆质量。' : '复习今天到期或需要强化的新卡片。'"
      eyebrow="STUDY SESSION"
      :breadcrumbs="[
        { label: '闪卡记忆', to: '/flashcard' },
        ...(deckId ? [{ label: deckName || '卡组', to: `/flashcard/deck/${deckId}` }] : []),
        { label: '学习' }
      ]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm" subtle>{{ studiedCount }} / {{ totalCards }}</CnStatusTag>
        <CnStatusTag v-if="remainingNew > 0" type="success" size="sm" subtle>新卡 {{ remainingNew }}</CnStatusTag>
        <CnStatusTag v-if="remainingDue > 0" type="warning" size="sm" subtle>复习 {{ remainingDue }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">退出学习</el-button>
      </template>
    </CnPageHeader>

    <CnSection
      v-if="totalCards > 0 && !studyComplete"
      title="学习进度"
      description="先回忆正面内容，再翻卡核对答案并选择记忆质量。"
      divided
    >
      <StudyProgress
        :current="studiedCount"
        :total="totalCards"
        :new-count="remainingNew"
        :due-count="remainingDue"
      />
    </CnSection>

    <CnSection v-if="currentCard && !studyComplete" class="study-stage" surface="transparent">
      <div class="study-area">
        <FlashCard
          ref="flashcardRef"
          :front-content="currentCard.frontContent"
          :back-content="currentCard.backContent"
          :content-type="currentCard.contentType"
          @flip="handleFlip"
        />

        <transition name="fade">
          <div v-if="showRating" class="rating-panel">
            <div class="rating-copy">
              <CnStatusTag type="info" size="sm" subtle>记忆反馈</CnStatusTag>
              <h2>你记得这张卡片吗？</h2>
              <p>选择越接近真实记忆状态，后续复习节奏越准确。</p>
            </div>

            <div class="rating-buttons">
              <el-button
                v-for="option in ratingOptions"
                :key="option.quality"
                class="rating-btn"
                :class="option.className"
                :loading="submitting"
                @click="submitRating(option.quality)"
              >
                <span class="rating-label">{{ option.label }}</span>
                <span class="rating-desc">{{ option.description }}</span>
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </CnSection>

    <CnSection v-else-if="studyComplete" class="complete-section" divided>
      <CnEmptyState
        title="学习完成"
        description="今天这组卡片已经处理完，可以返回卡组或继续拉取下一轮待学内容。"
        icon="OK"
        size="lg"
        surface="transparent"
      >
        <template #actions>
          <el-button type="primary" size="large" @click="goBack">返回卡组</el-button>
          <el-button size="large" @click="continueStudy">继续学习</el-button>
        </template>
      </CnEmptyState>

      <div class="complete-stats">
        <CnStatCard title="学习卡片" :value="studiedCount" description="本轮完成数量" tone="success" />
        <CnStatCard title="学习时长" :value="studyDuration" unit="分钟" description="按本轮会话估算" tone="brand" />
      </div>
    </CnSection>

    <CnEmptyState
      v-if="!loading && !currentCard && !studyComplete"
      title="暂无待学习卡片"
      description="当前没有可学习的闪卡，可以返回卡组或稍后再来。"
      icon="CARD"
      surface="panel"
    >
      <template #actions>
        <el-button type="primary" @click="goBack">返回</el-button>
      </template>
    </CnEmptyState>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { flashcardApi } from '@/api/flashcard'
import FlashCard from './components/FlashCard.vue'
import StudyProgress from './components/StudyProgress.vue'

interface StudyStatus {
  masteryLevel?: number
}

interface StudyCard {
  id: number | string
  frontContent?: string
  backContent?: string
  contentType?: number
  studyStatus?: StudyStatus
}

interface DeckDetail {
  name?: string
}

interface StudyResult {
  remainingNew?: number
  remainingDue?: number
}

interface FlashCardExpose {
  reset: () => void
  flip: () => void
}

const router = useRouter()
const route = useRoute()

const flashcardRef = ref<FlashCardExpose | null>(null)
const loading = ref(false)
const submitting = ref(false)
const showRating = ref(false)
const studyComplete = ref(false)

const deckId = computed(() => route.params.deckId as string | undefined)
const deckName = ref('')
const cardQueue = ref<StudyCard[]>([])
const currentCard = ref<StudyCard | null>(null)
const studiedCount = ref(0)
const totalCards = ref(0)
const remainingNew = ref(0)
const remainingDue = ref(0)
const startTime = ref<number | null>(null)
const cardStartTime = ref<number | null>(null)

const ratingOptions = [
  {
    quality: 1,
    label: '完全忘记',
    description: '需要重新学习',
    className: 'forget'
  },
  {
    quality: 2,
    label: '模糊记忆',
    description: '有印象但不确定',
    className: 'hard'
  },
  {
    quality: 3,
    label: '想起来了',
    description: '经过思考想起来',
    className: 'good'
  },
  {
    quality: 4,
    label: '完全记住',
    description: '轻松回忆无压力',
    className: 'easy'
  }
]

const studyDuration = computed(() => {
  if (!startTime.value) return 0
  return Math.round((Date.now() - startTime.value) / 60000)
})

const loadStudyCards = async () => {
  loading.value = true
  try {
    let cards: StudyCard[]
    if (deckId.value) {
      const deck = (await flashcardApi.getDeckById(deckId.value)) as DeckDetail
      deckName.value = deck.name || ''
      const deckCards = (await flashcardApi.getCardsByDeckId(deckId.value)) as StudyCard[]
      cards = (deckCards || []).filter((card) => !card.studyStatus || (card.studyStatus.masteryLevel ?? 0) < 3)
    } else {
      cards = (await flashcardApi.getTodayStudyCards(50)) as StudyCard[]
    }

    cardQueue.value = cards || []
    totalCards.value = cardQueue.value.length
    remainingNew.value = cardQueue.value.filter((card) => !card.studyStatus || card.studyStatus.masteryLevel === 1).length
    remainingDue.value = totalCards.value - remainingNew.value

    if (cardQueue.value.length > 0) {
      loadNextCard()
      startTime.value = Date.now()
      return
    }

    currentCard.value = null
  } catch (error) {
    console.error('加载卡片失败:', error)
    ElMessage.error('加载卡片失败')
  } finally {
    loading.value = false
  }
}

const loadNextCard = () => {
  if (cardQueue.value.length === 0) {
    currentCard.value = null
    studyComplete.value = studiedCount.value > 0 || totalCards.value > 0
    return
  }

  currentCard.value = cardQueue.value.shift() || null
  showRating.value = false
  cardStartTime.value = Date.now()
  flashcardRef.value?.reset()
}

const handleFlip = (isFlipped: boolean) => {
  if (isFlipped) {
    showRating.value = true
  }
}

const submitRating = async (quality: number) => {
  if (!currentCard.value) return

  submitting.value = true
  try {
    const duration = cardStartTime.value ? Math.round((Date.now() - cardStartTime.value) / 1000) : 0

    const result = (await flashcardApi.submitStudyResult({
      cardId: currentCard.value.id,
      quality,
      duration
    })) as StudyResult

    studiedCount.value += 1
    remainingNew.value = result.remainingNew ?? remainingNew.value
    remainingDue.value = result.remainingDue ?? remainingDue.value

    if (quality <= 2) {
      cardQueue.value.push(currentCard.value)
    }

    loadNextCard()
  } catch (error) {
    const message = error instanceof Error ? error.message : '提交失败'
    ElMessage.error(message)
  } finally {
    submitting.value = false
  }
}

const continueStudy = () => {
  studyComplete.value = false
  studiedCount.value = 0
  loadStudyCards()
}

const goBack = () => {
  if (deckId.value) {
    router.push(`/flashcard/deck/${deckId.value}`)
    return
  }
  router.push('/flashcard')
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!showRating.value || submitting.value) return

  switch (event.key) {
    case '1':
      submitRating(1)
      break
    case '2':
      submitRating(2)
      break
    case '3':
      submitRating(3)
      break
    case '4':
      submitRating(4)
      break
    case ' ':
      event.preventDefault()
      flashcardRef.value?.flip()
      break
  }
}

onMounted(() => {
  loadStudyCards()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.study-page {
  min-height: calc(100vh - 68px);
}

.study-stage {
  display: grid;
}

.study-area {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-6);
  min-height: 460px;
}

.rating-panel {
  display: grid;
  gap: var(--cn-space-4);
  width: 100%;
  max-width: 760px;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-card);
}

.rating-copy {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
  text-align: center;
}

.rating-copy h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 780;
  line-height: 1.35;
}

.rating-copy p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.rating-buttons {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.rating-btn {
  display: grid;
  gap: var(--cn-space-1);
  min-height: 78px;
  height: auto;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  white-space: normal;
}

.rating-label {
  font-size: 14px;
  font-weight: 800;
  line-height: 1.3;
}

.rating-desc {
  font-size: 11px;
  line-height: 1.45;
  opacity: 0.85;
}

.rating-btn.forget {
  border-color: color-mix(in srgb, var(--cn-color-danger) 55%, var(--cn-color-border));
  color: var(--cn-color-danger);
  background: color-mix(in srgb, var(--cn-color-danger) 10%, var(--cn-color-bg-surface));
}

.rating-btn.hard {
  border-color: color-mix(in srgb, var(--cn-color-warning) 55%, var(--cn-color-border));
  color: var(--cn-color-warning);
  background: color-mix(in srgb, var(--cn-color-warning) 12%, var(--cn-color-bg-surface));
}

.rating-btn.good {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 55%, var(--cn-color-border));
  color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.rating-btn.easy {
  border-color: color-mix(in srgb, var(--cn-color-success) 55%, var(--cn-color-border));
  color: var(--cn-color-success);
  background: color-mix(in srgb, var(--cn-color-success) 10%, var(--cn-color-bg-surface));
}

.complete-section {
  display: grid;
  gap: var(--cn-space-4);
}

.complete-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--cn-motion-base) var(--cn-ease-out);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 760px) {
  .rating-buttons,
  .complete-stats {
    grid-template-columns: 1fr;
  }

  .study-area {
    min-height: 360px;
  }
}
</style>
