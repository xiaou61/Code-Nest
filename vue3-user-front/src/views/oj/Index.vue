<template>
  <CnPage class="oj-index" surface="transparent" max-width="1440px" full-height>
    <CnPageHeader
      class="oj-page-header cn-wave-reveal"
      title="在线判题训练台"
      description="按难度与标签精确筛题，结合每日一题与数据追踪，稳步提升算法实战能力。"
      eyebrow="Online Judge"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">
          总题量 {{ ojStore.problemsTotal }}
        </CnStatusTag>
        <CnStatusTag type="success" size="sm">
          标签 {{ ojStore.tags.length }}
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">
          {{ activeFilterText }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="router.push('/oj/my-submissions')">
          <el-icon><List /></el-icon>
          我的提交
        </el-button>
        <el-button type="primary" @click="router.push('/oj/playground')">
          <el-icon><Lightning /></el-icon>
          练习场
        </el-button>
      </template>
    </CnPageHeader>

    <section class="oj-summary-grid cn-learn-reveal" aria-label="OJ 数据概览">
      <CnStatCard
        title="题库总量"
        :value="ojStore.problemsTotal"
        unit="题"
        description="当前筛选条件下的公开题目"
        tone="brand"
        trend="flat"
        :loading="ojStore.problemsLoading"
        trend-text="实时"
      />
      <CnStatCard
        title="标签数量"
        :value="ojStore.tags.length"
        unit="个"
        description="用于拆解算法知识点"
        tone="success"
        trend="flat"
        :loading="ojStore.tagsLoading"
        trend-text="筛选"
      />
      <CnStatCard
        title="当前页码"
        :value="queryParams.pageNum"
        description="分页浏览刷题列表"
        tone="info"
        trend="flat"
        trend-text="分页"
      />
      <CnStatCard
        title="当前难度"
        :value="selectedDifficultyLabel"
        description="可按难度快速聚焦训练"
        tone="warning"
        trend="flat"
        trend-text="过滤"
      />
    </section>

    <div class="oj-layout">
      <aside class="sidebar" aria-label="题目筛选">
        <CnSection class="sidebar-section search-section cn-learn-reveal" surface="panel" compact>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索题目..."
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button :icon="Search" aria-label="搜索题目" @click="handleSearch" />
            </template>
          </el-input>
        </CnSection>

        <CnSection
          class="sidebar-section cn-learn-reveal"
          title="难度筛选"
          description="控制训练颗粒度"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><Filter /></el-icon>
          </template>
          <div class="filter-list">
            <button
              class="filter-item"
              :class="{ active: !queryParams.difficulty }"
              type="button"
              @click="selectDifficulty(null)"
            >
              <span>全部难度</span>
            </button>
            <button
              v-for="d in difficultyOptions"
              :key="d.value"
              class="filter-item"
              :class="{ active: queryParams.difficulty === d.value }"
              type="button"
              @click="selectDifficulty(d.value)"
            >
              <CnStatusTag :type="d.tagType" size="sm" :dot="false">
                {{ d.label }}
              </CnStatusTag>
            </button>
          </div>
        </CnSection>

        <CnSection
          class="sidebar-section cn-learn-reveal"
          title="标签筛选"
          description="按知识点聚焦题目"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><CollectionTag /></el-icon>
          </template>
          <div class="filter-list tag-list" v-loading="ojStore.tagsLoading">
            <button
              class="filter-item"
              :class="{ active: !queryParams.tagId }"
              type="button"
              @click="selectTag(null)"
            >
              <span>全部标签</span>
            </button>
            <button
              v-for="tag in ojStore.tags"
              :key="tag.id"
              class="filter-item"
              :class="{ active: queryParams.tagId === tag.id }"
              type="button"
              @click="selectTag(tag.id)"
            >
              <span>{{ tag.name }}</span>
            </button>
          </div>
        </CnSection>

        <CnSection
          class="sidebar-section quick-actions cn-learn-reveal"
          title="快捷入口"
          description="跳转到常用 OJ 工作区"
          surface="panel"
          compact
        >
          <template #actions>
            <el-icon><Lightning /></el-icon>
          </template>
          <div class="action-buttons">
            <button class="action-btn" type="button" @click="router.push('/oj/my-submissions')">
              <el-icon class="action-icon"><List /></el-icon>
              <span>我的提交</span>
            </button>
            <button class="action-btn" type="button" @click="router.push('/oj/statistics')">
              <el-icon class="action-icon"><DataLine /></el-icon>
              <span>做题统计</span>
            </button>
            <button class="action-btn" type="button" @click="router.push('/oj/ranking')">
              <el-icon class="action-icon"><Trophy /></el-icon>
              <span>排行榜</span>
            </button>
            <button class="action-btn" type="button" @click="router.push('/oj/contests')">
              <el-icon class="action-icon"><CollectionTag /></el-icon>
              <span>赛事中心</span>
            </button>
          </div>
        </CnSection>
      </aside>

      <main class="main-content">
        <button
          v-if="dailyProblem"
          class="daily-card cn-learn-reveal cn-learn-shine"
          type="button"
          @click="goToProblem(dailyProblem)"
        >
          <span class="daily-label">每日一题</span>
          <span class="daily-body">
            <span class="daily-title">
              <span>{{ dailyProblem.id }}. {{ dailyProblem.title }}</span>
              <CnStatusTag :type="getDifficultyTag(dailyProblem.difficulty)" size="sm" :dot="false">
                {{ getDifficultyLabel(dailyProblem.difficulty) }}
              </CnStatusTag>
            </span>
            <span class="daily-desc" v-if="dailyProblem.inputDescription">
              {{ dailyProblem.inputDescription.substring(0, 80) }}
              <span v-if="dailyProblem.inputDescription.length > 80">...</span>
            </span>
          </span>
          <el-icon class="daily-arrow"><ArrowRight /></el-icon>
        </button>

        <CnSection
          class="problem-section cn-learn-reveal"
          title="题目列表"
          :description="problemSectionDescription"
          surface="panel"
          divided
        >
          <template #actions>
            <CnStatusTag v-if="ojStore.problemsTotal > 0" type="brand" size="sm">
              {{ ojStore.problemsTotal }} 道题目
            </CnStatusTag>
          </template>
          <div class="problem-table-wrap">
            <el-table
              v-if="ojStore.problemsLoading || ojStore.problems.length > 0"
              v-loading="ojStore.problemsLoading"
              :data="ojStore.problems"
              class="problem-table"
              @row-click="goToProblem"
              row-class-name="clickable-row"
            >
              <el-table-column prop="id" label="编号" width="80" align="center" />
              <el-table-column prop="title" label="题目" min-width="250">
                <template #default="{ row }">
                  <span class="problem-title">{{ row.title }}</span>
                </template>
              </el-table-column>
              <el-table-column label="难度" width="100" align="center">
                <template #default="{ row }">
                  <CnStatusTag :type="getDifficultyTag(row.difficulty)" size="sm" :dot="false">
                    {{ getDifficultyLabel(row.difficulty) }}
                  </CnStatusTag>
                </template>
              </el-table-column>
              <el-table-column label="通过率" width="120" align="center">
                <template #default="{ row }">
                  <span v-if="row.submitCount > 0">
                    {{ ((row.acceptedCount / row.submitCount) * 100).toFixed(1) }}%
                  </span>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="标签" min-width="200">
                <template #default="{ row }">
                  <CnStatusTag
                    v-for="tag in (row.tags || []).slice(0, 3)"
                    :key="tag.id"
                    type="neutral"
                    size="sm"
                    :dot="false"
                    subtle
                    class="tag-chip"
                  >
                    {{ tag.name }}
                  </CnStatusTag>
                  <CnStatusTag v-if="(row.tags || []).length > 3" type="info" size="sm" :dot="false">
                    +{{ row.tags.length - 3 }}
                  </CnStatusTag>
                </template>
              </el-table-column>
              <el-table-column label="提交/通过" width="120" align="center">
                <template #default="{ row }">
                  <span class="text-muted">{{ row.submitCount || 0 }} / {{ row.acceptedCount || 0 }}</span>
                </template>
              </el-table-column>
            </el-table>
            <CnEmptyState
              v-else
              title="暂无题目数据"
              description="调整关键词、难度或标签后重新筛选。"
              icon="OJ"
              size="lg"
              surface="transparent"
            >
              <template #actions>
                <el-button plain @click="resetFilters">重置筛选</el-button>
              </template>
            </CnEmptyState>
          </div>

          <div class="pagination-wrapper cn-learn-reveal" v-if="ojStore.problemsTotal > 0">
            <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :page-sizes="[15, 30, 50]"
              :total="ojStore.problemsTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </CnSection>
      </main>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, Filter, CollectionTag, Lightning, List, DataLine, Trophy, ArrowRight
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { useOjStore } from '@/stores/oj'
import { ojApi } from '@/api/oj'
import { useRevealMotion } from '@/utils/reveal-motion'

const router = useRouter()
const ojStore = useOjStore()
useRevealMotion('.oj-index .cn-learn-reveal')

const dailyProblem = ref(null)
const searchKeyword = ref('')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 15,
  keyword: '',
  difficulty: null,
  tagId: null
})

const difficultyOptions = [
  { value: 'easy', label: '简单', tagType: 'success' },
  { value: 'medium', label: '中等', tagType: 'warning' },
  { value: 'hard', label: '困难', tagType: 'danger' }
]

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '在线判题' }
]

const selectedDifficultyLabel = computed(() => {
  const option = difficultyOptions.find(item => item.value === queryParams.difficulty)
  return option?.label || '全部'
})

const selectedTagName = computed(() => {
  const tag = ojStore.tags.find(item => item.id === queryParams.tagId)
  return tag?.name || '全部标签'
})

const activeFilterText = computed(() => {
  const keyword = queryParams.keyword ? `关键词 ${queryParams.keyword}` : '无关键词'
  return `${selectedDifficultyLabel.value} · ${selectedTagName.value} · ${keyword}`
})

const problemSectionDescription = computed(() => {
  return `当前页 ${queryParams.pageNum}，每页 ${queryParams.pageSize} 题，筛选条件：${activeFilterText.value}`
})

// ============ 方法 ============

const loadProblems = () => {
  const params = { ...queryParams }
  if (!params.difficulty) delete params.difficulty
  if (!params.tagId) delete params.tagId
  if (!params.keyword) delete params.keyword
  ojStore.fetchProblems(params, { forceRefresh: true })
}

const handleSearch = () => {
  queryParams.keyword = searchKeyword.value
  queryParams.pageNum = 1
  loadProblems()
}

const selectDifficulty = (value) => {
  queryParams.difficulty = value
  queryParams.pageNum = 1
  loadProblems()
}

const selectTag = (tagId) => {
  queryParams.tagId = tagId
  queryParams.pageNum = 1
  loadProblems()
}

const handleSizeChange = () => {
  queryParams.pageNum = 1
  loadProblems()
}

const handleCurrentChange = () => {
  loadProblems()
}

const resetFilters = () => {
  searchKeyword.value = ''
  queryParams.keyword = ''
  queryParams.difficulty = null
  queryParams.tagId = null
  queryParams.pageNum = 1
  loadProblems()
}

const goToProblem = (row) => {
  router.push(`/oj/problem/${row.id}`)
}

const getDifficultyTag = (difficulty) => {
  const map = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

const getDifficultyLabel = (difficulty) => {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

// ============ 初始化 ============

onMounted(() => {
  loadProblems()
  ojStore.fetchTags()
  ojApi.getDailyProblem().then(p => { dailyProblem.value = p }).catch(() => {})
})
</script>

<style scoped>
.oj-index {
  min-height: calc(100vh - 68px);
}

.oj-page-header {
  margin-bottom: var(--cn-space-1);
}

.oj-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.oj-layout {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-5);
  min-width: 0;
}

.sidebar {
  width: 276px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-section {
  min-width: 0;
}

.filter-list,
.tag-list {
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
}

.filter-item {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 34px;
  padding: 0 var(--cn-space-3);
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  background: transparent;
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.filter-item:hover {
  border-color: var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
}

.filter-item.active {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 28%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  font-weight: 650;
}

.tag-list {
  max-height: 300px;
  overflow-y: auto;
}

.quick-actions .action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 38px;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: var(--cn-radius-card);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--cn-color-text-secondary);
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.action-btn:hover {
  border-color: var(--cn-color-border);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-brand-primary);
}

.action-icon {
  font-size: 16px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.problem-section :deep(.cn-section__body) {
  padding: 0;
}

.problem-table-wrap {
  min-width: 0;
}

.problem-table {
  width: 100%;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: var(--cn-color-bg-surface-muted);
}

.problem-title {
  font-weight: 600;
  color: var(--cn-color-text-primary);
}

.tag-chip {
  margin-right: 4px;
}

.text-muted {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface);
}

.daily-card {
  width: 100%;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-info));
  border-radius: var(--cn-radius-panel);
  padding: 16px 20px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition:
    transform var(--cn-motion-base) var(--cn-ease-out),
    box-shadow var(--cn-motion-base) var(--cn-ease-out);
  color: white;
  text-align: left;
  box-shadow: var(--cn-shadow-card);
  border: 1px solid color-mix(in srgb, var(--cn-color-brand-primary) 26%, transparent);
}

.daily-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--cn-shadow-lg);
}

.daily-label {
  background: color-mix(in srgb, white 24%, transparent);
  padding: 4px 10px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.daily-body {
  flex: 1;
  min-width: 0;
}

.daily-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.daily-desc {
  font-size: 12px;
  color: color-mix(in srgb, white 82%, transparent);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.daily-arrow {
  font-size: 20px;
  opacity: 0.7;
  flex-shrink: 0;
}

@media (max-width: 1180px) {
  .oj-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .oj-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .oj-summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .daily-card {
    align-items: flex-start;
    padding: 14px 16px;
  }

  .daily-label,
  .daily-arrow {
    display: none;
  }
}
</style>
