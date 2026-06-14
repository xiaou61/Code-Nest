<template>
  <CnPage class="random-questions-page" max-width="1280px" full-height>
    <CnPageHeader
      title="随机抽题"
      description="从一个或多个面试题单中按数量随机抽取题目，用于快速自测和查漏补缺。"
      eyebrow="INTERVIEW RANDOM"
      :breadcrumbs="[{ label: '面试题库', to: '/interview' }, { label: '随机抽题' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ selectedSetIds.length }} 个题单</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>{{ questionCount }} 题计划</CnStatusTag>
        <CnStatusTag v-if="hasStarted" type="success" size="sm">{{ randomQuestions.length }} 题已生成</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Back" @click="goBack">返回题库</el-button>
        <el-button v-if="hasStarted" :icon="Setting" @click="resetConfig">重新配置</el-button>
        <el-button
          type="primary"
          :icon="hasStarted ? Refresh : VideoPlay"
          :loading="startLoading"
          :disabled="selectedSetIds.length === 0 || questionCount <= 0"
          @click="hasStarted ? regenerateQuestions() : startRandomQuestions()"
        >
          {{ hasStarted ? '重新抽题' : '开始随机抽题' }}
        </el-button>
      </template>
    </CnPageHeader>

    <section class="random-summary-grid" aria-label="随机抽题配置概览">
      <CnStatCard
        title="可选题单"
        :value="questionSetList.length"
        unit="套"
        description="当前加载的公开题单"
        tone="brand"
        trend="flat"
        :loading="setsLoading"
        trend-text="题单"
      />
      <CnStatCard
        title="已选题单"
        :value="selectedSetIds.length"
        unit="套"
        description="参与本次随机抽题"
        tone="success"
        trend="flat"
        trend-text="已选"
      />
      <CnStatCard
        title="可选题量"
        :value="totalQuestions"
        unit="题"
        description="已选题单中的题目总量"
        tone="info"
        trend="flat"
        trend-text="范围"
      />
      <CnStatCard
        title="抽题数量"
        :value="questionCount"
        unit="题"
        description="本次计划抽取数量"
        tone="warning"
        trend="flat"
        trend-text="计划"
      />
    </section>

    <CnSection v-if="!hasStarted" title="配置抽题参数" description="选择题单后设置抽题数量，数量上限会随题单总题量调整。" divided>
      <div class="config-grid">
        <div class="question-sets-panel">
          <div class="section-kicker">
            <span>选择题单</span>
            <CnStatusTag type="neutral" size="sm" subtle>
              {{ selectedSetIds.length }}/{{ questionSetList.length }}
            </CnStatusTag>
          </div>

          <div v-loading="setsLoading" class="question-sets-grid">
            <button
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              class="question-set-item"
              :class="{ selected: selectedSetIds.includes(questionSet.id) }"
              type="button"
              @click="toggleQuestionSet(questionSet.id)"
            >
              <span class="set-card-top">
                <span class="set-copy">
                  <span class="set-title">{{ questionSet.title }}</span>
                  <span class="set-description">{{ questionSet.description || '暂无描述' }}</span>
                </span>
                <el-checkbox
                  :model-value="selectedSetIds.includes(questionSet.id)"
                  @change="toggleQuestionSet(questionSet.id)"
                  @click.stop
                />
              </span>

              <span class="set-meta">
                <CnStatusTag :type="questionSet.type === 1 ? 'success' : 'info'" size="sm">
                  {{ questionSet.type === 1 ? '官方' : '用户' }}
                </CnStatusTag>
                <CnStatusTag v-if="questionSet.categoryName" type="warning" size="sm" subtle>
                  {{ questionSet.categoryName }}
                </CnStatusTag>
                <span class="question-count">{{ questionSet.questionCount || 0 }} 题</span>
              </span>
            </button>

            <CnEmptyState
              v-if="!setsLoading && questionSetList.length === 0"
              title="暂无可选题单"
              description="当前没有可用于随机抽题的公开题单。"
              icon="RQ"
              surface="transparent"
            />
          </div>
        </div>

        <aside class="config-panel">
          <div class="section-kicker">
            <span>抽题配置</span>
            <CnStatusTag type="brand" size="sm" subtle>{{ totalQuestions }} 题可选</CnStatusTag>
          </div>

          <label class="option-row">
            <span class="option-label">抽题数量</span>
            <el-input-number
              v-model="questionCount"
              :min="1"
              :max="totalQuestions > 0 ? totalQuestions : 50"
              :disabled="selectedSetIds.length === 0"
              controls-position="right"
            />
          </label>

          <p class="config-hint">
            选择多个题单后会从题单题目池中随机抽取。当前可选题量为 {{ totalQuestions }} 题。
          </p>

          <div class="config-actions">
            <el-button
              type="primary"
              :icon="Refresh"
              :disabled="selectedSetIds.length === 0 || questionCount <= 0"
              :loading="startLoading"
              @click="startRandomQuestions"
            >
              开始随机抽题
            </el-button>
            <el-button @click="resetConfig">重置配置</el-button>
          </div>
        </aside>
      </div>
    </CnSection>

    <template v-else>
      <CnSection title="随机抽题结果" :description="resultDescription" divided>
        <template #actions>
          <el-button :icon="Refresh" :loading="startLoading" @click="regenerateQuestions">重新抽题</el-button>
          <el-button :icon="Setting" @click="resetConfig">重新配置</el-button>
        </template>

        <div class="questions-wrapper">
          <article v-for="(question, index) in randomQuestions" :key="question.id" class="question-item">
            <header class="question-header">
              <div class="question-number">第 {{ index + 1 }} 题</div>
              <el-button
                :type="isFavorited(question.id) ? 'danger' : 'primary'"
                :icon="Star"
                size="small"
                @click="toggleQuestionFavorite(question)"
              >
                {{ isFavorited(question.id) ? '取消收藏' : '收藏' }}
              </el-button>
            </header>

            <div class="question-content">
              <h3 class="question-title">{{ question.title }}</h3>

              <div class="question-meta">
                <CnStatusTag type="info" size="sm" subtle>
                  来源：{{ question.questionSetTitle || '未知题单' }}
                </CnStatusTag>
                <span>
                  <el-icon><View /></el-icon>
                  {{ question.viewCount || 0 }} 浏览
                </span>
                <span>
                  <el-icon><Star /></el-icon>
                  {{ question.favoriteCount || 0 }} 收藏
                </span>
              </div>

              <div class="answer-section">
                <el-button
                  v-if="!question.showAnswer"
                  type="primary"
                  :icon="View"
                  @click="toggleAnswer(question)"
                >
                  查看答案
                </el-button>
                <el-button v-else :icon="Hide" @click="toggleAnswer(question)">隐藏答案</el-button>

                <div v-if="question.showAnswer" class="answer-content">
                  <h4>参考答案</h4>
                  <div class="markdown-content" v-html="renderMarkdown(question.answer)" />
                </div>
              </div>
            </div>
          </article>
        </div>
      </CnSection>
    </template>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back, Hide, Refresh, Setting, Star, VideoPlay, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import { interviewApi } from '@/api/interview'
import { renderMarkdown } from '@/utils/markdown'

type EntityId = number | string

interface QuestionSet {
  id: EntityId
  title: string
  description?: string
  type?: number
  categoryName?: string
  questionCount?: number
}

interface QuestionSetPage {
  records?: QuestionSet[]
}

interface RandomQuestion {
  id: EntityId
  title: string
  answer?: string
  questionSetTitle?: string
  viewCount?: number
  favoriteCount?: number
  showAnswer: boolean
}

const router = useRouter()

const questionSetList = ref<QuestionSet[]>([])
const selectedSetIds = ref<EntityId[]>([])
const questionCount = ref(5)
const randomQuestions = ref<RandomQuestion[]>([])
const hasStarted = ref(false)
const favoriteMap = ref(new Map<EntityId, boolean>())

const setsLoading = ref(false)
const startLoading = ref(false)

const totalQuestions = computed(() => {
  return questionSetList.value
    .filter((set) => selectedSetIds.value.includes(set.id))
    .reduce((total, set) => total + (set.questionCount || 0), 0)
})

const resultDescription = computed(() => {
  return `从 ${selectedSetIds.value.length} 个题单中随机抽取了 ${randomQuestions.value.length} 题。`
})

const loadQuestionSets = async () => {
  try {
    setsLoading.value = true
    const data = (await interviewApi.getPublicQuestionSets({
      page: 1,
      size: 100
    })) as QuestionSetPage
    questionSetList.value = data.records || []
  } catch (error) {
    console.error('获取题单列表失败:', error)
    ElMessage.error('获取题单列表失败')
  } finally {
    setsLoading.value = false
  }
}

const toggleQuestionSet = (setId: EntityId) => {
  const index = selectedSetIds.value.indexOf(setId)
  if (index > -1) {
    selectedSetIds.value.splice(index, 1)
  } else {
    selectedSetIds.value.push(setId)
  }

  if (totalQuestions.value === 0) {
    questionCount.value = 5
  } else if (questionCount.value > totalQuestions.value) {
    questionCount.value = Math.min(5, totalQuestions.value)
  }
}

const startRandomQuestions = async () => {
  if (selectedSetIds.value.length === 0) {
    ElMessage.warning('请选择至少一个题单')
    return
  }

  if (questionCount.value <= 0) {
    ElMessage.warning('请设置有效的抽题数量')
    return
  }

  try {
    startLoading.value = true
    const questions = (await interviewApi.getRandomQuestions(selectedSetIds.value, questionCount.value)) as Array<
      Omit<RandomQuestion, 'showAnswer'>
    >

    randomQuestions.value = (questions || []).map((question) => ({
      ...question,
      showAnswer: false
    }))

    hasStarted.value = true
    ElMessage.success(`成功抽取 ${randomQuestions.value.length} 题`)

    await checkFavoriteStatus()
  } catch (error) {
    console.error('随机抽题失败:', error)
    ElMessage.error('随机抽题失败')
  } finally {
    startLoading.value = false
  }
}

const regenerateQuestions = async () => {
  await startRandomQuestions()
}

const resetConfig = () => {
  hasStarted.value = false
  selectedSetIds.value = []
  questionCount.value = 5
  randomQuestions.value = []
  favoriteMap.value.clear()
}

const toggleAnswer = (question: RandomQuestion) => {
  question.showAnswer = !question.showAnswer
}

const checkFavoriteStatus = async () => {
  for (const question of randomQuestions.value) {
    try {
      const isFav = await interviewApi.isFavorited(2, question.id)
      favoriteMap.value.set(question.id, Boolean(isFav))
    } catch (error) {
      console.error('检查收藏状态失败:', error)
    }
  }
}

const isFavorited = (questionId: EntityId) => favoriteMap.value.get(questionId) || false

const toggleQuestionFavorite = async (question: RandomQuestion) => {
  try {
    const currentState = isFavorited(question.id)

    if (currentState) {
      await interviewApi.removeFavorite(2, question.id)
      favoriteMap.value.set(question.id, false)
      ElMessage.success('取消收藏成功')
    } else {
      await interviewApi.addFavorite(2, question.id)
      favoriteMap.value.set(question.id, true)
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    console.error('操作收藏失败:', error)
    ElMessage.error('操作失败')
  }
}

const goBack = () => {
  router.push('/interview')
}

onMounted(() => {
  loadQuestionSets()
})
</script>

<style scoped>
.random-questions-page {
  min-width: 0;
}

.random-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.config-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--cn-space-5);
  align-items: start;
}

.section-kicker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
}

.question-sets-grid {
  display: grid;
  max-height: 520px;
  overflow-y: auto;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--cn-space-3);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface-muted);
}

.question-set-item {
  display: grid;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    border-color var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
}

.question-set-item:hover,
.question-set-item.selected {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 42%, var(--cn-color-border-subtle));
  box-shadow: var(--cn-shadow-sm);
  transform: translateY(-2px);
}

.question-set-item.selected {
  background: var(--cn-color-brand-soft);
}

.set-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
}

.set-copy {
  display: grid;
  flex: 1;
  gap: var(--cn-space-2);
  min-width: 0;
}

.set-title {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.set-description {
  display: -webkit-box;
  min-height: 40px;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.set-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.question-count {
  margin-left: auto;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.config-panel {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
}

.option-row {
  display: grid;
  gap: var(--cn-space-2);
}

.option-label {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
}

.config-hint {
  margin: 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.7;
}

.config-actions {
  display: grid;
  gap: var(--cn-space-2);
}

.questions-wrapper {
  display: grid;
  gap: var(--cn-space-4);
}

.question-item {
  overflow: hidden;
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.question-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
}

.question-number {
  color: var(--cn-color-brand-primary);
  font-size: 13px;
  font-weight: 800;
}

.question-content {
  display: grid;
  gap: var(--cn-space-4);
  padding: var(--cn-space-5);
}

.question-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.5;
}

.question-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-3);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.question-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.question-meta .el-icon {
  color: var(--cn-color-brand-primary);
}

.answer-section {
  display: grid;
  justify-items: start;
  gap: var(--cn-space-4);
}

.answer-content {
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-4);
  border-left: 3px solid var(--cn-color-brand-primary);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.answer-content h4 {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.markdown-content {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  line-height: 1.75;
}

.markdown-content :deep(pre) {
  overflow-x: auto;
  padding: var(--cn-space-4);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-elevated);
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

@media (max-width: 1024px) {
  .random-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .config-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .random-summary-grid,
  .question-sets-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .question-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .question-count {
    margin-left: 0;
  }
}
</style>
