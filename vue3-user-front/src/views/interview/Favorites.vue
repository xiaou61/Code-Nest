<template>
  <div class="favorites-page">
    <!-- 头部导航 -->
    <div class="header">
      <div class="header-content">
        <div class="header-left">
          <h2>我的收藏</h2>
          <p>收藏的题单和题目</p>
        </div>
        <div class="header-right">
          <el-button @click="goBack" :icon="Back">
            返回题库
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分类选择 -->
    <div class="type-nav">
      <el-card shadow="never" class="type-card">
        <el-radio-group v-model="currentType" @change="handleTypeChange">
          <el-radio-button :value="2">题单收藏</el-radio-button>
          <el-radio-button :value="3">题目收藏</el-radio-button>
        </el-radio-group>
      </el-card>
    </div>

    <!-- 收藏列表 -->
    <div class="content-section">
      <el-card shadow="never" class="content-card">
        <template #header>
          <div class="content-header">
            <span>{{ currentType === 2 ? '收藏的题单' : '收藏的题目' }}</span>
            <span class="total-count" v-if="total > 0">共 {{ total }} 个</span>
          </div>
        </template>

        <div v-loading="loading" class="favorites-list">
          <!-- 题单收藏列表 -->
          <div v-if="currentType === 2" class="question-sets-grid">
            <div 
              v-for="favorite in favoriteList" 
              :key="favorite.id"
              class="question-set-card"
              @click="goToQuestionSet(favorite.targetId)"
            >
              <div class="card-header">
                <h3 class="card-title">{{ favorite.title }}</h3>
                <div class="card-badges">
                  <el-tag :type="favorite.type === 1 ? 'success' : 'info'" size="small">
                    {{ favorite.type === 1 ? '官方' : '用户' }}
                  </el-tag>
                  <el-tag type="warning" size="small" v-if="favorite.categoryName">
                    {{ favorite.categoryName }}
                  </el-tag>
                </div>
              </div>
              
              <p class="card-description">{{ favorite.description || '暂无描述' }}</p>
              
              <div class="card-stats">
                <div class="stat-item">
                  <el-icon><Edit /></el-icon>
                  <span>{{ favorite.questionCount || 0 }} 题</span>
                </div>
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ favorite.viewCount || 0 }} 浏览</span>
                </div>
                <div class="stat-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ favorite.favoriteCount || 0 }} 收藏</span>
                </div>
              </div>
              
              <div class="card-footer">
                <span class="creator">{{ favorite.creatorName }}</span>
                <span class="favorite-time">收藏于 {{ formatDate(favorite.favoriteTime) }}</span>
              </div>

              <div class="remove-favorite">
                <el-button 
                  type="danger" 
                  :icon="Delete" 
                  size="small" 
                  @click.stop="removeFavorite(favorite)"
                  circle
                />
              </div>
            </div>
          </div>

          <!-- 题目收藏列表 -->
          <div v-else class="questions-list">
            <div 
              v-for="favorite in favoriteList" 
              :key="favorite.id"
              class="question-item"
              @click="goToQuestion(favorite)"
            >
              <div class="question-content">
                <h4 class="question-title">{{ favorite.title }}</h4>
                <div class="question-meta">
                  <span class="set-name">所属题单：{{ favorite.questionSetTitle }}</span>
                  <span class="view-count">
                    <el-icon><View /></el-icon>
                    {{ favorite.viewCount || 0 }} 浏览
                  </span>
                  <span class="favorite-count">
                    <el-icon><Star /></el-icon>
                    {{ favorite.favoriteCount || 0 }} 收藏
                  </span>
                  <span class="favorite-time">
                    收藏于 {{ formatDate(favorite.favoriteTime) }}
                  </span>
                </div>
              </div>
              <div class="question-actions">
                <el-button 
                  type="danger" 
                  :icon="Delete" 
                  size="small" 
                  @click.stop="removeFavorite(favorite)"
                >
                  取消收藏
                </el-button>
                <el-button type="primary" :icon="ArrowRight" size="small">
                  查看题目
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty 
          v-if="!loading && favoriteList.length === 0" 
          :description="`暂无${currentType === 2 ? '题单' : '题目'}收藏`"
          :image-size="120"
        />

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
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
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Back, Edit, View, Star, Delete, ArrowRight
} from '@element-plus/icons-vue'
import { interviewApi } from '@/api/interview'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const favoriteList = ref([])
const currentType = ref(2) // 2=题单，3=题目
const total = ref(0)

// 查询参数
const queryParams = reactive({
  page: 1,
  size: 12
})

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 获取收藏列表
const fetchFavorites = async () => {
  loading.value = true
  try {
    const data = await interviewApi.getMyFavoritePage(
      currentType.value,
      queryParams.page,
      queryParams.size
    )
    favoriteList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取收藏列表失败:', error)
    ElMessage.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

// 类型切换
const handleTypeChange = (type) => {
  currentType.value = type
  queryParams.page = 1
  fetchFavorites()
}

// 分页大小改变
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  fetchFavorites()
}

// 当前页改变
const handleCurrentChange = (page) => {
  queryParams.page = page
  fetchFavorites()
}

// 取消收藏
const removeFavorite = async (favorite) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消收藏"${favorite.title}"吗？`,
      '确认取消收藏',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await interviewApi.removeFavorite(currentType.value, favorite.targetId)
    ElMessage.success('取消收藏成功')
    fetchFavorites() // 重新加载列表
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
      ElMessage.error('取消收藏失败')
    }
  }
}

// 跳转到题单详情
const goToQuestionSet = (setId) => {
  router.push(`/interview/question-sets/${setId}`)
}

// 跳转到题目详情
const goToQuestion = (favorite) => {
  // 需要从favorite中获取题单ID和题目ID
  router.push(`/interview/questions/${favorite.questionSetId}/${favorite.targetId}`)
}

// 返回题库
const goBack = () => {
  router.push('/interview')
}

// 页面挂载
onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8f7ff 0%, #f0eeff 50%, #ede9fe 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #e4e7ed;
  padding: 20px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left h2 {
  margin: 0 0 5px 0;
  font-size: 28px;
  font-weight: bold;
  background: linear-gradient(135deg, #6c63ff, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-left p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.type-nav {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.type-card {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  text-align: center;
}

.content-section {
  padding: 0 20px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.content-card {
  background: rgba(255, 255, 255, 0.95);
  border: none;
  min-height: 500px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.total-count {
  color: #909399;
  font-size: 14px;
  font-weight: normal;
}

.favorites-list {
  margin-top: 20px;
}

/* 题单卡片样式 */
.question-sets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.question-set-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
  position: relative;
  overflow: hidden;
}

.question-set-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 32px rgba(108, 99, 255, 0.15);
  border-color: #6c63ff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-title {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: bold;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
}

.card-badges {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
}

.card-description {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  min-height: 40px;
}

.card-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 0;
  border-top: 1px solid #f0f2f5;
  border-bottom: 1px solid #f0f2f5;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #C0C4CC;
}

.remove-favorite {
  position: absolute;
  top: 10px;
  right: 10px;
}

/* 题目列表样式 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  display: flex;
  align-items: center;
  padding: 20px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.question-item:hover {
  background: #f8f7ff;
  border-color: #6c63ff;
  transform: translateX(4px);
}

.question-content {
  flex: 1;
}

.question-title {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.question-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
  flex-wrap: wrap;
}

.question-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.set-name {
  color: #6c63ff;
  font-weight: 500;
}

.question-actions {
  display: flex;
  gap: 8px;
  margin-left: 16px;
}

.pagination-wrapper {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .header-right {
    align-self: flex-end;
  }
  
  .question-sets-grid {
    grid-template-columns: 1fr;
  }
  
  .question-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .question-actions {
    margin-left: 0;
    align-self: flex-end;
  }
  
  .question-meta {
    gap: 8px;
  }
}
</style> 