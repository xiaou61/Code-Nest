<template>
  <div class="oj-index cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <section class="cn-learn-hero cn-wave-reveal">
        <div class="cn-learn-hero__content">
          <span class="cn-learn-hero__eyebrow">Online Judge</span>
          <h1 class="cn-learn-hero__title">在线判题训练台</h1>
          <p class="cn-learn-hero__desc">按难度与标签精确筛题，结合每日一题与数据追踪，稳步提升算法实战能力。</p>
        </div>
        <div class="cn-learn-hero__meta">
          <span class="cn-learn-chip">总题量 {{ ojStore.problemsTotal }}</span>
          <span class="cn-learn-chip">标签 {{ ojStore.tags.length }}</span>
          <span class="cn-learn-chip">筛选页 {{ queryParams.pageNum }}</span>
        </div>
      </section>

      <div class="cn-learn-layout">
        <!-- 左侧边栏 -->
        <aside class="sidebar cn-learn-sidebar">
          <!-- 搜索框 -->
          <div class="sidebar-section search-section cn-learn-panel cn-learn-float cn-learn-reveal">
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
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>

          <!-- 难度筛选 -->
          <div class="sidebar-section cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><Filter /></el-icon>
              <span>难度筛选</span>
            </div>
            <div class="difficulty-list">
              <div
                class="difficulty-item"
                :class="{ active: !queryParams.difficulty }"
                @click="selectDifficulty(null)"
              >
                <span>全部难度</span>
              </div>
              <div
                v-for="d in difficultyOptions"
                :key="d.value"
                class="difficulty-item"
                :class="{ active: queryParams.difficulty === d.value }"
                @click="selectDifficulty(d.value)"
              >
                <el-tag :type="d.tagType" size="small" effect="dark">{{ d.label }}</el-tag>
              </div>
            </div>
          </div>

          <!-- 标签筛选 -->
          <div class="sidebar-section cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><CollectionTag /></el-icon>
              <span>标签筛选</span>
            </div>
            <div class="tag-list" v-loading="ojStore.tagsLoading">
              <div
                class="tag-item"
                :class="{ active: !queryParams.tagId }"
                @click="selectTag(null)"
              >
                <span>全部标签</span>
              </div>
              <div
                v-for="tag in ojStore.tags"
                :key="tag.id"
                class="tag-item"
                :class="{ active: queryParams.tagId === tag.id }"
                @click="selectTag(tag.id)"
              >
                <span>{{ tag.name }}</span>
              </div>
            </div>
          </div>

          <!-- 快捷入口 -->
          <div class="sidebar-section quick-actions cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><Lightning /></el-icon>
              <span>快捷入口</span>
            </div>
            <div class="action-buttons">
              <div class="action-btn" @click="$router.push('/oj/my-submissions')">
                <el-icon class="action-icon"><List /></el-icon>
                <span>我的提交</span>
              </div>
              <div class="action-btn" @click="$router.push('/oj/statistics')">
                <el-icon class="action-icon"><DataLine /></el-icon>
                <span>做题统计</span>
              </div>
              <div class="action-btn" @click="$router.push('/oj/ranking')">
                <el-icon class="action-icon"><Trophy /></el-icon>
                <span>排行榜</span>
              </div>
              <div class="action-btn" @click="$router.push('/oj/contests')">
                <el-icon class="action-icon"><CollectionTag /></el-icon>
                <span>赛事中心</span>
              </div>
            </div>
          </div>
        </aside>

        <!-- 主内容区 -->
        <main class="main-content cn-learn-main">
          <!-- 内容头部 -->
          <div class="content-header cn-learn-panel cn-learn-reveal">
            <div class="header-left">
              <h2 class="page-title">在线判题</h2>
              <span class="total-badge" v-if="ojStore.problemsTotal > 0">
                {{ ojStore.problemsTotal }} 道题目
              </span>
            </div>
          </div>

          <!-- 每日一题 -->
          <div class="daily-card cn-learn-reveal cn-learn-shine" v-if="dailyProblem" @click="goToProblem(dailyProblem)">
            <div class="daily-label">每日一题</div>
            <div class="daily-body">
              <div class="daily-title">
                <span>{{ dailyProblem.id }}. {{ dailyProblem.title }}</span>
                <el-tag :type="getDifficultyTag(dailyProblem.difficulty)" size="small" effect="dark">
                  {{ getDifficultyLabel(dailyProblem.difficulty) }}
                </el-tag>
              </div>
              <div class="daily-desc" v-if="dailyProblem.inputDescription">
                {{ dailyProblem.inputDescription.substring(0, 80) }}
                <span v-if="dailyProblem.inputDescription.length > 80">...</span>
              </div>
            </div>
            <el-icon class="daily-arrow"><ArrowRight /></el-icon>
          </div>

          <!-- 题目表格 -->
          <el-card shadow="never" class="table-card cn-learn-panel cn-learn-reveal">
            <el-table
              v-loading="ojStore.problemsLoading"
              :data="ojStore.problems"
              style="width: 100%"
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
                  <el-tag
                    :type="getDifficultyTag(row.difficulty)"
                    size="small"
                    effect="dark"
                  >
                    {{ getDifficultyLabel(row.difficulty) }}
                  </el-tag>
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
                  <el-tag
                    v-for="tag in (row.tags || []).slice(0, 3)"
                    :key="tag.id"
                    size="small"
                    class="tag-chip"
                  >
                    {{ tag.name }}
                  </el-tag>
                  <el-tag v-if="(row.tags || []).length > 3" size="small" type="info">
                    +{{ row.tags.length - 3 }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="提交/通过" width="120" align="center">
                <template #default="{ row }">
                  <span class="text-muted">{{ row.submitCount || 0 }} / {{ row.acceptedCount || 0 }}</span>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 空状态 -->
          <el-empty
            v-if="!ojStore.problemsLoading && ojStore.problems.length === 0"
            description="暂无题目数据"
            :image-size="120"
          />

          <!-- 分页 -->
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
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, Filter, CollectionTag, Lightning, List, DataLine, Trophy, ArrowRight
} from '@element-plus/icons-vue'
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

.oj-index .cn-learn-layout {
  align-items: flex-start;
}

.sidebar {
  width: 276px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-section {
  border-radius: 16px;
  padding: 16px;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--cn-text-primary);
  margin-bottom: 12px;
}

.difficulty-list,
.tag-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.difficulty-item,
.tag-item {
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--cn-text-secondary);
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.difficulty-item:hover,
.tag-item:hover {
  background: #eef4ff;
  color: #2458b1;
}

.difficulty-item.active,
.tag-item.active {
  background: #e6f0ff;
  color: #1f6feb;
  font-weight: 500;
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
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--cn-text-secondary);
  transition: all 0.2s;
}

.action-btn:hover {
  background: #edf4ff;
  color: var(--cn-primary);
}

.action-icon {
  font-size: 16px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--cn-text-primary);
  margin: 0;
}

.total-badge {
  background: #edf4ff;
  color: #1f6feb;
  padding: 3px 11px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.table-card {
  border-radius: 16px;
  border: 1px solid rgba(115, 156, 225, 0.24);
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #f4f9ff;
}

.problem-title {
  font-weight: 500;
  color: var(--cn-text-primary);
}

.tag-chip {
  margin-right: 4px;
}

.text-muted {
  color: #9ca3af;
  font-size: 13px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(115, 156, 225, 0.24);
  box-shadow: 0 18px 42px rgba(22, 63, 119, 0.12);
}

/* 每日一题 */
.daily-card {
  background: linear-gradient(135deg, #4f8eff 0%, #1f6feb 100%);
  border-radius: 16px;
  padding: 16px 20px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: transform 0.22s, box-shadow 0.22s;
  color: #fff;
  box-shadow: 0 16px 36px rgba(31, 111, 235, 0.28);
  border: 1px solid rgba(163, 212, 255, 0.36);
}

.daily-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 34px rgba(31, 111, 235, 0.34);
}

.daily-label {
  background: rgba(255, 255, 255, 0.24);
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
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.daily-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.daily-arrow {
  font-size: 20px;
  opacity: 0.7;
}

/* 响应式 */
@media (max-width: 900px) {
  .oj-index .cn-learn-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }
}
</style>
