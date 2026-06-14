<template>
  <CnPage class="favorites-page" max-width="1180px" full-height>
    <CnPageHeader
      title="我的收藏"
      description="集中管理收藏的面试题单和题目，快速回到学习上下文。"
      eyebrow="INTERVIEW FAVORITES"
      :breadcrumbs="[{ label: '面试题库', to: '/interview' }, { label: '我的收藏' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm" subtle>{{ currentTypeLabel }}</CnStatusTag>
        <CnStatusTag v-if="total > 0" type="info" size="sm">{{ total }} 个收藏</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Back" @click="goBack">返回题库</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="收藏类型" description="在题单收藏和题目收藏之间切换，列表会自动刷新。" divided>
      <el-radio-group v-model="currentType" @change="handleTypeChange">
        <el-radio-button :value="2">题单收藏</el-radio-button>
        <el-radio-button :value="3">题目收藏</el-radio-button>
      </el-radio-group>
    </CnSection>

    <CnSection :title="currentType === 2 ? '收藏的题单' : '收藏的题目'" divided>
      <template #actions>
        <CnStatusTag v-if="total > 0" type="neutral" size="sm" subtle>共 {{ total }} 个</CnStatusTag>
      </template>

      <div v-loading="loading" class="favorites-list">
        <div v-if="currentType === 2 && favoriteList.length" class="question-sets-grid">
          <article
            v-for="favorite in favoriteList"
            :key="favorite.id"
            class="question-set-card"
            @click="goToQuestionSet(favorite.targetId)"
          >
            <div class="card-header">
              <h3 class="card-title">{{ favorite.title }}</h3>
              <div class="card-badges">
                <CnStatusTag :type="favorite.type === 1 ? 'success' : 'info'" size="sm">
                  {{ favorite.type === 1 ? '官方' : '用户' }}
                </CnStatusTag>
                <CnStatusTag v-if="favorite.categoryName" type="warning" size="sm" subtle>
                  {{ favorite.categoryName }}
                </CnStatusTag>
              </div>
            </div>

            <p class="card-description">{{ favorite.description || '暂无描述' }}</p>

            <div class="card-stats">
              <span>
                <el-icon><Edit /></el-icon>
                {{ favorite.questionCount || 0 }} 题
              </span>
              <span>
                <el-icon><View /></el-icon>
                {{ favorite.viewCount || 0 }} 浏览
              </span>
              <span>
                <el-icon><Star /></el-icon>
                {{ favorite.favoriteCount || 0 }} 收藏
              </span>
            </div>

            <div class="card-footer">
              <span>{{ favorite.creatorName || '匿名用户' }}</span>
              <span>收藏于 {{ formatDate(favorite.favoriteTime) }}</span>
            </div>

            <el-button
              class="remove-favorite"
              type="danger"
              :icon="Delete"
              size="small"
              circle
              @click.stop="removeFavorite(favorite)"
            />
          </article>
        </div>

        <div v-else-if="currentType === 3 && favoriteList.length" class="questions-list">
          <article
            v-for="favorite in favoriteList"
            :key="favorite.id"
            class="question-item"
            @click="goToQuestion(favorite)"
          >
            <div class="question-content">
              <h4 class="question-title">{{ favorite.title }}</h4>
              <div class="question-meta">
                <span class="set-name">所属题单：{{ favorite.questionSetTitle || '未知题单' }}</span>
                <span>
                  <el-icon><View /></el-icon>
                  {{ favorite.viewCount || 0 }} 浏览
                </span>
                <span>
                  <el-icon><Star /></el-icon>
                  {{ favorite.favoriteCount || 0 }} 收藏
                </span>
                <span>收藏于 {{ formatDate(favorite.favoriteTime) }}</span>
              </div>
            </div>

            <div class="question-actions">
              <el-button type="danger" :icon="Delete" size="small" @click.stop="removeFavorite(favorite)">
                取消收藏
              </el-button>
              <el-button type="primary" :icon="ArrowRight" size="small">查看题目</el-button>
            </div>
          </article>
        </div>

        <CnEmptyState
          v-if="!loading && favoriteList.length === 0"
          :title="`暂无${currentType === 2 ? '题单' : '题目'}收藏`"
          description="可以回到面试题库，收藏需要重点复习的内容。"
          icon="FAV"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" @click="goBack">返回题库</el-button>
          </template>
        </CnEmptyState>
      </div>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Back, Delete, Edit, Star, View } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { interviewApi } from '@/api/interview'

type FavoriteType = 2 | 3

interface FavoriteItem {
  id: number | string
  targetId: number | string
  title?: string
  description?: string
  type?: number
  categoryName?: string
  questionCount?: number
  viewCount?: number
  favoriteCount?: number
  creatorName?: string
  favoriteTime?: string
  questionSetTitle?: string
  questionSetId?: number | string
}

interface FavoritePageResponse {
  records?: FavoriteItem[]
  total?: number
}

const router = useRouter()

const loading = ref(false)
const favoriteList = ref<FavoriteItem[]>([])
const currentType = ref<FavoriteType>(2)
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 12
})

const currentTypeLabel = computed(() => currentType.value === 2 ? '题单收藏' : '题目收藏')

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const fetchFavorites = async () => {
  loading.value = true
  try {
    const data = (await interviewApi.getMyFavoritePage(
      currentType.value,
      queryParams.page,
      queryParams.size
    )) as FavoritePageResponse
    favoriteList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    ElMessage.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

const handleTypeChange = (type: FavoriteType) => {
  currentType.value = type
  queryParams.page = 1
  fetchFavorites()
}

const handleSizeChange = (size: number) => {
  queryParams.size = size
  queryParams.page = 1
  fetchFavorites()
}

const handleCurrentChange = (page: number) => {
  queryParams.page = page
  fetchFavorites()
}

const removeFavorite = async (favorite: FavoriteItem) => {
  try {
    await ElMessageBox.confirm(`确定要取消收藏"${favorite.title}"吗？`, '确认取消收藏', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await interviewApi.removeFavorite(currentType.value, favorite.targetId)
    ElMessage.success('取消收藏成功')
    fetchFavorites()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

const goToQuestionSet = (setId: FavoriteItem['targetId']) => {
  router.push(`/interview/question-sets/${setId}`)
}

const goToQuestion = (favorite: FavoriteItem) => {
  router.push(`/interview/questions/${favorite.questionSetId}/${favorite.targetId}`)
}

const goBack = () => {
  router.push('/interview')
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page {
  min-height: calc(100vh - 68px);
}

.favorites-list {
  min-height: 260px;
}

.question-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--cn-space-4);
}

.question-set-card,
.question-item {
  position: relative;
  min-width: 0;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.question-set-card {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  overflow: hidden;
}

.question-set-card:hover,
.question-item:hover {
  transform: translateY(-2px);
  border-color: var(--cn-color-brand-primary);
  box-shadow: var(--cn-shadow-card);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
}

.card-title,
.question-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-weight: 750;
  line-height: 1.45;
}

.card-title {
  padding-right: var(--cn-space-6);
  font-size: 17px;
}

.question-title {
  font-size: 16px;
}

.card-badges,
.card-stats,
.question-meta,
.question-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.card-description {
  display: -webkit-box;
  min-height: 42px;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-stats {
  padding: var(--cn-space-3) 0;
  border-top: 1px solid var(--cn-color-border-subtle);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.card-stats span,
.question-meta span {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.remove-favorite {
  position: absolute;
  top: var(--cn-space-3);
  right: var(--cn-space-3);
}

.questions-list {
  display: grid;
  gap: var(--cn-space-3);
}

.question-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  padding: var(--cn-space-4);
}

.question-content {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.question-meta {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.set-name {
  color: var(--cn-color-brand-primary);
  font-weight: 650;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--cn-space-5);
}

@media (max-width: 760px) {
  .question-sets-grid {
    grid-template-columns: 1fr;
  }

  .question-item {
    display: grid;
  }

  .question-actions {
    justify-content: flex-start;
  }

  .pagination-wrapper {
    overflow-x: auto;
    justify-content: flex-start;
  }
}
</style>
