<template>
  <div class="oj-index">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <!-- 搜索框 -->
      <div class="sidebar-section search-section">
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
      <div class="sidebar-section">
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
      <div class="sidebar-section">
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
      <div class="sidebar-section quick-actions">
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
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 内容头部 -->
      <div class="content-header">
        <div class="header-left">
          <h2 class="page-title">在线判题</h2>
          <span class="total-badge" v-if="ojStore.problemsTotal > 0">
            {{ ojStore.problemsTotal }} 道题目
          </span>
        </div>
      </div>

      <!-- 题目表格 -->
      <el-card shadow="never" class="table-card">
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
      <div class="pagination-wrapper" v-if="ojStore.problemsTotal > 0">
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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search, Filter, CollectionTag, Lightning, List, DataLine
} from '@element-plus/icons-vue'
import { useOjStore } from '@/stores/oj'

const router = useRouter()
const ojStore = useOjStore()

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
})
</script>

<style scoped>
.oj-index {
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 60px);
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
}

.sidebar-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
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
  color: #6b7280;
  transition: all 0.2s;
  display: flex;
  align-items: center;
}

.difficulty-item:hover,
.tag-item:hover {
  background: #f3f4f6;
  color: #374151;
}

.difficulty-item.active,
.tag-item.active {
  background: #dbeafe;
  color: #2563eb;
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
  color: #6b7280;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f3f4f6;
  color: #374151;
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
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
}

.total-badge {
  background: #dbeafe;
  color: #2563eb;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.table-card {
  border-radius: 12px;
}

.table-card :deep(.el-card__body) {
  padding: 0;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background: #f9fafb;
}

.problem-title {
  font-weight: 500;
  color: #1f2937;
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
  margin-top: 20px;
  padding-bottom: 20px;
}

/* 响应式 */
@media (max-width: 900px) {
  .oj-index {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
  }
}
</style>
