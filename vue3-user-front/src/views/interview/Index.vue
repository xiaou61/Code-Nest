<template>
  <div class="interview-index cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <section class="cn-learn-hero cn-wave-reveal">
        <div class="cn-learn-hero__content">
          <span class="cn-learn-hero__eyebrow">Interview Studio</span>
          <h1 class="cn-learn-hero__title">面试题库学习中枢</h1>
          <p class="cn-learn-hero__desc">把刷题、收藏、复习和学习足迹放在同一工作台，形成高效闭环。</p>
        </div>
        <div class="cn-learn-hero__meta">
          <span class="cn-learn-chip">题单 {{ totalQuestionSets }}</span>
          <span class="cn-learn-chip">分类 {{ categoryList.length }}</span>
          <span class="cn-learn-chip">当前结果 {{ total }}</span>
        </div>
      </section>

      <div class="cn-learn-layout">
        <!-- 左侧边栏 -->
        <aside class="sidebar cn-learn-sidebar">
          <!-- 搜索框 -->
          <div class="sidebar-section search-section cn-learn-panel cn-learn-float cn-learn-reveal">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索题单..."
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

          <!-- 分类筛选 -->
          <div class="sidebar-section category-section cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><Folder /></el-icon>
              <span>分类筛选</span>
            </div>
            <div class="category-list">
              <div
                class="category-item"
                :class="{ active: currentCategoryId === null }"
                @click="selectCategory(null)"
              >
                <span class="category-name">全部分类</span>
                <span class="category-count">{{ totalQuestionSets }}</span>
              </div>
              <div
                v-for="category in categoryList"
                :key="category.id"
                class="category-item"
                :class="{ active: currentCategoryId === category.id }"
                @click="selectCategory(category.id)"
              >
                <span class="category-name">{{ category.name }}</span>
                <span class="category-count">{{ category.questionSetCount || 0 }}</span>
              </div>
            </div>
          </div>

          <!-- 快捷功能 -->
          <div class="sidebar-section quick-actions cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><Lightning /></el-icon>
              <span>快捷功能</span>
            </div>
            <div class="action-buttons">
              <div class="action-btn" @click="goToRandomQuestions">
                <el-icon class="action-icon refresh-icon"><Refresh /></el-icon>
                <span>随机抽题</span>
              </div>
              <div class="action-btn" @click="goToFavorites">
                <el-icon class="action-icon star-icon"><Star /></el-icon>
                <span>我的收藏</span>
              </div>
              <div class="action-btn review-btn" @click="goToReview">
                <el-icon class="action-icon review-icon"><Clock /></el-icon>
                <span>复习中心</span>
              </div>
            </div>
          </div>

          <!-- 学习统计 -->
          <div class="sidebar-section stats-section cn-learn-panel cn-learn-float cn-learn-reveal">
            <div class="section-title">
              <el-icon><DataLine /></el-icon>
              <span>数据统计</span>
            </div>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-value">{{ totalQuestionSets }}</div>
                <div class="stat-label">题单总数</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ categoryList.length }}</div>
                <div class="stat-label">分类数量</div>
              </div>
            </div>
          </div>

          <!-- 复习提醒卡片 -->
          <ReviewReminderCard class="cn-learn-reveal" />
        </aside>

        <!-- 主内容区 -->
        <main class="main-content cn-learn-main">
          <!-- 学习热力图 -->
          <LearningHeatmap class="heatmap-section cn-learn-reveal" />

          <!-- 内容头部 -->
          <div class="content-header cn-learn-panel cn-learn-reveal">
            <div class="header-left">
              <h2 class="page-title">题单列表</h2>
              <span class="total-badge" v-if="total > 0">{{ total }} 个题单</span>
            </div>
            <div class="header-right">
              <el-select v-model="sortType" placeholder="排序方式" style="width: 140px" @change="handleSortChange">
                <el-option label="最新发布" value="newest" />
                <el-option label="最多浏览" value="views" />
                <el-option label="最多收藏" value="favorites" />
              </el-select>
            </div>
          </div>

          <!-- 题单网格 -->
          <div v-loading="loading" class="question-sets-grid">
            <div
              v-for="questionSet in questionSetList"
              :key="questionSet.id"
              class="question-set-card cn-learn-panel cn-learn-float cn-learn-shine cn-learn-reveal"
              @click="goToQuestionSet(questionSet)"
            >
              <div class="card-header">
                <h3 class="card-title">{{ questionSet.title }}</h3>
                <el-tag :type="questionSet.type === 1 ? 'success' : 'info'" size="small" effect="dark">
                  {{ questionSet.type === 1 ? '官方' : '用户' }}
                </el-tag>
              </div>

              <p class="card-description">{{ questionSet.description || '暂无描述' }}</p>

              <div class="card-tags" v-if="questionSet.categoryName">
                <el-tag type="warning" size="small" round>
                  {{ questionSet.categoryName }}
                </el-tag>
              </div>

              <div class="card-stats">
                <div class="stat-item">
                  <el-icon><Edit /></el-icon>
                  <span>{{ questionSet.questionCount || 0 }} 题</span>
                </div>
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ questionSet.viewCount || 0 }}</span>
                </div>
                <div class="stat-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ questionSet.favoriteCount || 0 }}</span>
                </div>
              </div>

              <div class="card-footer">
                <div class="card-author" v-if="questionSet.creatorName">
                  <el-avatar :size="20" :src="questionSet.creatorAvatar">
                    {{ questionSet.creatorName?.charAt(0) }}
                  </el-avatar>
                  <span>{{ questionSet.creatorName }}</span>
                </div>
                <div class="card-date">
                  {{ formatDate(questionSet.createTime) }}
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty
            v-if="!loading && questionSetList.length === 0"
            description="暂无题单数据"
            :image-size="120"
          />

          <!-- 分页 -->
          <div class="pagination-wrapper" v-if="total > 0">
            <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :page-sizes="[12, 24, 36, 48]"
              :total="total"
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, Star, Edit, View, Refresh, Folder, DataLine, Lightning, Clock
} from '@element-plus/icons-vue'
import { useInterviewStore } from '@/stores/interview'
import { useRevealMotion } from '@/utils/reveal-motion'
import ReviewReminderCard from './components/ReviewReminderCard.vue'
import LearningHeatmap from './components/LearningHeatmap.vue'

const router = useRouter()
const interviewStore = useInterviewStore()
useRevealMotion('.interview-index .cn-learn-reveal')

// 响应式数据
const searchKeyword = ref('')
const currentCategoryId = ref(null)
const sortType = ref('newest')

// 从store获取数据
const loading = computed(() => interviewStore.questionSetsLoading)
const categoryList = computed(() => interviewStore.categories)
const questionSetList = computed(() => interviewStore.questionSets)
const total = computed(() => interviewStore.questionSetsTotal)

// 计算题单总数
const totalQuestionSets = computed(() => {
  return categoryList.value.reduce((sum, cat) => sum + (cat.questionSetCount || 0), 0)
})

// 查询参数
const queryParams = reactive({
  keyword: '',
  page: 1,
  size: 12
})

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    await interviewStore.fetchCategories()
  } catch (error) {
    ElMessage.error('获取分类列表失败')
  }
}

// 获取题单列表
const fetchQuestionSets = async (forceRefresh = false) => {
  try {
    // 如果有搜索关键词，使用搜索接口，否则使用公开题单列表接口
    if (queryParams.keyword) {
      await interviewStore.searchQuestions(queryParams, { forceRefresh })
    } else {
      await interviewStore.fetchPublicQuestionSets({
        categoryId: currentCategoryId.value,
        page: queryParams.page,
        size: queryParams.size
      }, { forceRefresh })
    }
  } catch (error) {
    ElMessage.error('获取题单列表失败')
  }
}

// 选择分类
const selectCategory = (categoryId) => {
  currentCategoryId.value = categoryId
  queryParams.page = 1
  searchKeyword.value = '' // 清空搜索关键词
  queryParams.keyword = ''
  fetchQuestionSets(true) // 强制刷新
}

// 搜索
const handleSearch = () => {
  queryParams.keyword = searchKeyword.value
  queryParams.page = 1
  fetchQuestionSets()
}

// 分页大小改变
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchQuestionSets()
}

// 当前页改变
const handleCurrentChange = (page) => {
  queryParams.page = page
  fetchQuestionSets()
}

// 排序改变
const handleSortChange = (sort) => {
  sortType.value = sort
  queryParams.page = 1
  fetchQuestionSets()
}

// 跳转到题单详情
const goToQuestionSet = (questionSet) => {
  // 如果是搜索结果（题目），直接跳转到题目详情
  if (typeof questionSet.id === 'string' && questionSet.id.startsWith('q-') && questionSet.originalQuestion) {
    router.push(`/interview/questions/${questionSet.originalQuestion.id}`)
  } else {
    // 正常题单，跳转到题单详情
    router.push(`/interview/question-sets/${questionSet.id}`)
  }
}

// 跳转到收藏页面
const goToFavorites = () => {
  router.push('/interview/favorites')
}

// 跳转到复习中心
const goToReview = () => {
  router.push('/interview/review')
}

// 跳转到随机抽题页面
const goToRandomQuestions = () => {
  router.push('/interview/random')
}

// 初始化
onMounted(async () => {
  await fetchCategories()
  await fetchQuestionSets()
})
</script>

<style scoped>
/* 整体布局 */
.interview-index {
  min-height: calc(100vh - 68px);
}

.interview-index .cn-learn-layout {
  align-items: flex-start;
}

/* ========== 左侧边栏 ========== */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 24px;
  height: fit-content;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}

.sidebar-section {
  border-radius: 16px;
  padding: 16px;
  background: transparent;
  box-shadow: none;
  border: none;
}

.sidebar-section:hover {
  box-shadow: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.section-title .el-icon {
  color: #1f6feb;
}

/* 搜索框 */
.search-section {
  padding: 12px;
}

.search-section :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #2f7cf5 0%, #1f6feb 100%);
  border-color: #2f7cf5;
}

.search-section :deep(.el-input-group__append .el-button) {
  color: white;
}

/* 分类列表 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 280px;
  overflow-y: auto;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.category-item:hover {
  background: #f5f7fa;
}

.category-item.active {
  background: linear-gradient(135deg, #2f7cf5 0%, #1f6feb 100%);
  color: white;
}

.category-item.active .category-count {
  background: rgba(255, 255, 255, 0.3);
  color: white;
}

.category-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-count {
  background: #f0f2f5;
  color: #606266;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  min-width: 24px;
  text-align: center;
}

/* 快捷功能 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  background: #f5f7fa;
}

.action-btn:hover {
  transform: translateX(4px);
}

.action-btn:first-child:hover {
  background: linear-gradient(135deg, #edf6ff 0%, #e4f1ff 100%);
  color: #1f6feb;
}

.action-btn:nth-child(2):hover {
  background: linear-gradient(135deg, #fff4ea 0%, #ffecdb 100%);
  color: #d87c16;
}

.action-btn.review-btn:hover {
  background: linear-gradient(135deg, #edf8ff 0%, #e3f3ff 100%);
  color: #1779d4;
}

.review-icon {
  color: #1779d4;
}

.action-icon {
  font-size: 18px;
}

.refresh-icon {
  color: #1f6feb;
}

.star-icon {
  color: #d87c16;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-card {
  background: linear-gradient(135deg, #edf5ff 0%, #e6f0ff 100%);
  border-radius: 10px;
  padding: 12px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #1f6feb, #1799eb);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* ========== 主内容区 ========== */
.main-content {
  flex: 1;
  min-width: 0;
}

.heatmap-section {
  margin-bottom: 20px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  border-radius: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.total-badge {
  background: linear-gradient(135deg, #2f7cf5 0%, #1f6feb 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

/* 题单网格 */
.question-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  min-height: 200px;
}

.question-set-card {
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.question-set-card::after {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #2f7cf5, #15a5ef);
  opacity: 0;
  transition: opacity 0.3s;
}

.question-set-card:hover {
  transform: translateY(-4px);
  border-color: rgba(47, 124, 245, 0.36);
}

.question-set-card:hover::after {
  opacity: 1;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.card-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-description {
  margin: 0 0 12px 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-tags {
  margin-bottom: 12px;
}

.card-stats {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.card-stats .stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.card-stats .stat-item .el-icon {
  font-size: 14px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #c0c4cc;
}

.card-author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-author .el-avatar {
  background: linear-gradient(135deg, #1f6feb 0%, #15a5ef 100%);
  font-size: 12px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(115, 156, 225, 0.24);
  box-shadow: 0 18px 42px rgba(22, 63, 119, 0.12);
}

/* ========== 响应式设计 ========== */
@media (max-width: 1024px) {
  .interview-index {
    flex-direction: column;
    padding: 16px;
  }
  
  .sidebar {
    width: 100%;
    position: relative;
    top: 0;
    max-height: none;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .sidebar-section {
    flex: 1;
    min-width: 200px;
  }
  
  .category-list {
    max-height: 150px;
  }
  
  .question-sets-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 640px) {
  .interview-index {
    padding: 12px;
    gap: 12px;
  }
  
  .sidebar {
    flex-direction: column;
  }
  
  .sidebar-section {
    min-width: 100%;
  }
  
  .question-sets-grid {
    grid-template-columns: 1fr;
  }
  
  .content-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .header-right {
    width: 100%;
  }
  
  .header-right .el-select {
    width: 100% !important;
  }
  
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
