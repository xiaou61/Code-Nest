<template>
  <div class="knowledge-page cn-learn-shell">
    <div class="cn-learn-shell__inner">
      <!-- 页面头部 -->
      <div class="page-header cn-learn-hero cn-wave-reveal">
        <div class="header-content">
          <span class="cn-learn-hero__eyebrow">Knowledge Map</span>
          <h1 class="cn-learn-hero__title">知识图谱</h1>
          <p class="cn-learn-hero__desc">可视化学习面试知识点，从整体到细节的渐进式学习体验</p>
        </div>

        <!-- 搜索栏 -->
        <div class="search-section cn-learn-hero__meta">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索知识图谱..."
            :prefix-icon="Search"
            size="large"
            style="max-width: 400px;"
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" size="large" :icon="Search" @click="handleSearch">
            搜索
          </el-button>
        </div>
      </div>

      <!-- 图谱列表 -->
      <div class="maps-container">
        <div v-loading="loading" class="maps-grid">
          <div
            v-for="map in mapList"
            :key="map.id"
            class="map-card cn-learn-panel cn-learn-float cn-learn-shine cn-learn-reveal"
            @click="handleViewMap(map)"
          >
            <div class="card-cover">
              <img
                v-if="map.coverImage"
                :src="map.coverImage"
                :alt="map.title"
                class="cover-image"
              />
              <div v-else class="cover-placeholder">
                <el-icon size="48"><DataAnalysis /></el-icon>
              </div>
            </div>

            <div class="card-content">
              <h3 class="map-title">{{ map.title }}</h3>
              <p class="map-description">{{ map.description || '暂无描述' }}</p>

              <div class="map-stats">
                <div class="stat-item">
                  <el-icon><Connection /></el-icon>
                  <span>{{ map.nodeCount || 0 }} 个节点</span>
                </div>
                <div class="stat-item">
                  <el-icon><View /></el-icon>
                  <span>{{ map.viewCount || 0 }} 次查看</span>
                </div>
              </div>

              <div class="card-footer">
                <div class="update-time">
                  更新于 {{ formatDate(map.updateTime) }}
                </div>
                <el-button type="primary" size="small">
                  开始学习
                  <el-icon class="ml-2"><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && !mapList.length" class="empty-state cn-learn-panel cn-learn-reveal">
          <el-icon size="64" color="#c0c4cc"><DataAnalysis /></el-icon>
          <p>暂无知识图谱</p>
          <p style="color: #909399; font-size: 14px;">
            {{ searchForm.keyword ? '未找到相关图谱，请尝试其他关键词' : '敬请期待更多内容' }}
          </p>
        </div>

        <!-- 分页 -->
        <div v-if="pagination.total > 0" class="pagination-container cn-learn-reveal">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, ArrowRight, DataAnalysis, Connection, View
} from '@element-plus/icons-vue'
import { getPublishedKnowledgeMaps } from '@/api/knowledge'
import { useRevealMotion } from '@/utils/reveal-motion'

const router = useRouter()
useRevealMotion('.knowledge-page .cn-learn-reveal')

// 响应式数据
const loading = ref(false)
const mapList = ref([])

// 搜索表单
const searchForm = reactive({
  keyword: ''
})

// 分页数据
const pagination = reactive({
  page: 1,
  size: 12,
  total: 0
})

// 方法
const fetchMapList = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      ...searchForm
    }
    
    const data = await getPublishedKnowledgeMaps(params)
    mapList.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.pageNum || 1
  } catch (error) {
    ElMessage.error('获取知识图谱失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchMapList()
}

const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchMapList()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchMapList()
}

const handleViewMap = (map) => {
  router.push(`/knowledge/maps/${map.id}`)
}

const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  const now = new Date()
  const diffTime = now - date
  const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24))
  
  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '昨天'
  if (diffDays < 7) return `${diffDays}天前`
  if (diffDays < 30) return `${Math.floor(diffDays / 7)}周前`
  if (diffDays < 365) return `${Math.floor(diffDays / 30)}个月前`
  
  return date.toLocaleDateString('zh-CN')
}

// 生命周期
onMounted(() => {
  fetchMapList()
})
</script>

<style scoped>
.knowledge-page {
  min-height: calc(100vh - 68px);
}

.page-header {
  padding: 24px 28px;
  margin-bottom: 18px;
  border-radius: 24px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-content h1 {
  margin: 10px 0 8px;
}

.header-content p {
  margin: 0;
  max-width: 600px;
  line-height: 1.6;
}

.search-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  max-width: 500px;
  margin: 0 auto;
}

.maps-container {
  padding: 0 2px 8px;
}

.maps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.map-card {
  border-radius: 16px;
  overflow: hidden;
  background: transparent;
  box-shadow: none;
  border: 0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.map-card:hover {
  transform: translateY(-4px);
}

.card-cover {
  height: 180px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(45deg, #f0f2f5, #e6e8eb);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.map-card:hover .cover-image {
  transform: scale(1.05);
}

.cover-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #c0c4cc;
  background: linear-gradient(45deg, #f8f9fa, #e9ecef);
}

.card-content {
  padding: 20px;
}

.map-title {
  margin: 0 0 12px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.map-description {
  margin: 0 0 16px 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  min-height: 63px;
}

.map-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.stat-item .el-icon {
  font-size: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.update-time {
  font-size: 12px;
  color: #c0c4cc;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
  border-radius: 16px;
}

.empty-state p {
  margin: 16px 0 8px 0;
  font-size: 16px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(115, 156, 225, 0.24);
  box-shadow: 0 18px 42px rgba(22, 63, 119, 0.12);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 18px 16px;
  }
  
  .search-section {
    flex-direction: column;
    gap: 12px;
  }
  
  .search-section .el-input {
    max-width: none !important;
    width: 100%;
  }
  
  .maps-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .maps-container {
    padding: 20px;
  }
}

/* Element Plus 样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 25px;
}

:deep(.el-button) {
  border-radius: 25px;
}

.ml-2 {
  margin-left: 8px;
}
</style> 
