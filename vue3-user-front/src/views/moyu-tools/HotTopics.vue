<template>
  <div class="hot-topics">
    <!-- 头部标题区域 -->
    <div class="page-header">
      <div class="header-content">
        <div class="title-section">
          <h1 class="main-title">
            <el-icon class="title-icon"><TrendCharts /></el-icon>
            今日热榜
          </h1>
          <p class="subtitle">汇聚各大平台实时热点，一站式浏览全网热门话题</p>
        </div>
        <div class="header-actions">
          <el-button 
            type="primary" 
            @click="refreshAllData" 
            :loading="refreshing"
            :icon="Refresh"
          >
            {{ refreshing ? '刷新中...' : '刷新数据' }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 分类筛选区域 -->
    <div class="filter-section">
      <div class="filter-content">
        <div class="category-filters">
          <el-button 
            v-for="category in categories" 
            :key="category.name"
            :type="selectedCategory === category.name ? 'primary' : 'default'"
            @click="selectCategory(category.name)"
            class="category-btn"
          >
            {{ category.name }}
          </el-button>
        </div>
        <div class="update-info" v-if="lastUpdateTime">
          <el-icon><Clock /></el-icon>
          <span>最后更新：{{ formatTime(lastUpdateTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 热榜内容区域 -->
    <div class="content-section">
      <div class="content-container">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="10" animated />
        </div>

        <!-- 热榜列表 -->
        <div v-else class="hot-topics-grid">
          <div 
            v-for="(platformData, platform) in filteredPlatforms" 
            :key="platform"
            class="platform-card"
          >
            <div class="platform-header">
              <div class="platform-info">
                <h3 class="platform-title">{{ platformData.title }}</h3>
                <span class="platform-type">{{ platformData.type }}</span>
              </div>
              <div class="platform-meta">
                <span class="update-time">{{ formatTime(platformData.updatedAt) }}</span>
                <el-tag size="small" type="success" v-if="platformData.fromCache">缓存</el-tag>
              </div>
            </div>

            <div class="hot-list">
              <div 
                v-for="(item, index) in platformData.data?.slice(0, 10)" 
                :key="item.id"
                class="hot-item"
                @click="openLink(item.url)"
              >
                <div class="item-rank">{{ index + 1 }}</div>
                <div class="item-content">
                  <h4 class="item-title">{{ item.title }}</h4>
                  <div class="item-meta">
                    <span v-if="item.hot" class="hot-value">
                      <el-icon><TrendCharts /></el-icon>
                      {{ formatHotValue(item.hot) }}
                    </span>
                    <span v-if="item.author" class="author">{{ item.author }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 查看更多 -->
            <div class="platform-footer">
              <el-button 
                text 
                type="primary" 
                @click="openLink(platformData.link)"
                class="view-more-btn"
              >
                查看更多
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && Object.keys(filteredPlatforms).length === 0" class="empty-state">
          <el-empty description="暂无热榜数据">
            <el-button type="primary" @click="refreshAllData">重新加载</el-button>
          </el-empty>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  TrendCharts, Refresh, Clock, ArrowRight
} from '@element-plus/icons-vue'
import { getAllHotTopicData, refreshHotTopicData } from '@/api/moyu'

// 响应式数据
const loading = ref(true)
const refreshing = ref(false)
const selectedCategory = ref('全部')
const lastUpdateTime = ref(null)

// 分类数据
const categories = ref([
  { name: '全部', description: '所有平台' },
  { name: '社交媒体', description: '社交平台热榜和趋势数据' },
  { name: '知识社区', description: '知识问答和技术社区热门内容' },
  { name: '新闻资讯', description: '新闻媒体和资讯平台热点内容' },
  { name: '科技数码', description: '科技、数码、编程相关热门内容' },
  { name: '娱乐生活', description: '娱乐、文化、生活方式相关内容' },
  { name: '实用信息', description: '天气、灾害等实用信息服务' }
])

// 平台数据
const platformsData = reactive({})

// 平台分类映射
const platformCategoryMap = {
  'douyin': '社交媒体',
  'kuaishou': '社交媒体', 
  'weibo': '社交媒体',
  'hupu': '知识社区',
  'linuxdo': '知识社区',
  'newsmth': '知识社区',
  'tieba': '知识社区',
  'zhihu': '知识社区',
  'zhihu-daily': '知识社区',
  'ifanr': '新闻资讯',
  'netease-news': '新闻资讯',
  'toutiao': '新闻资讯',
  'csdn': '科技数码',
  'dgtle': '科技数码',
  'geekpark': '科技数码',
  'guokr': '科技数码',
  'hellogithub': '科技数码',
  'ithome': '科技数码',
  'juejin': '科技数码',
  'douban-movie': '娱乐生活',
  'jianshu': '娱乐生活',
  'weread': '娱乐生活',
  'earthquake': '实用信息',
  'history': '实用信息',
  'weather-alarm': '实用信息'
}

// 计算属性：过滤后的平台数据
const filteredPlatforms = computed(() => {
  if (selectedCategory.value === '全部') {
    return platformsData
  }
  
  const filtered = {}
  Object.keys(platformsData).forEach(platform => {
    if (platformCategoryMap[platform] === selectedCategory.value) {
      filtered[platform] = platformsData[platform]
    }
  })
  return filtered
})

// 选择分类
const selectCategory = (category) => {
  selectedCategory.value = category
}

// 加载热榜数据
const loadHotTopicsData = async () => {
  try {
    loading.value = true
    const response = await getAllHotTopicData()
    
    if (response) {
      Object.keys(response).forEach(platform => {
        platformsData[platform] = response[platform]
      })
      lastUpdateTime.value = new Date()
    }
  } catch (error) {
    console.error('加载热榜数据失败:', error)
    ElMessage.error('加载热榜数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新所有数据
const refreshAllData = async () => {
  try {
    refreshing.value = true
    await refreshHotTopicData()
    ElMessage.success('数据刷新成功')
    // 刷新后重新加载数据
    await loadHotTopicsData()
  } catch (error) {
    console.error('刷新数据失败:', error)
    ElMessage.error('刷新数据失败')
  } finally {
    refreshing.value = false
  }
}

// 打开链接
const openLink = (url) => {
  if (url) {
    window.open(url, '_blank')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) { // 1分钟内
    return '刚刚'
  } else if (diff < 3600000) { // 1小时内
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) { // 24小时内
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleString()
  }
}

// 格式化热度值
const formatHotValue = (hot) => {
  if (!hot) return ''
  if (hot >= 100000000) {
    return `${(hot / 100000000).toFixed(1)}亿`
  } else if (hot >= 10000) {
    return `${(hot / 10000).toFixed(1)}万`
  } else {
    return hot.toString()
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadHotTopicsData()
})
</script>

<style scoped>
.hot-topics {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

/* 头部区域 */
.page-header {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  padding: 40px 20px;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  color: white;
}

.main-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  font-size: 2.2rem;
  color: #fbbf24;
}

.subtitle {
  font-size: 1rem;
  opacity: 0.9;
  margin: 0;
  font-weight: 300;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 筛选区域 */
.filter-section {
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 20px;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.filter-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.category-btn {
  border-radius: 20px;
  font-weight: 500;
}

.update-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  font-size: 0.875rem;
}

/* 内容区域 */
.content-section {
  padding: 30px 20px 60px;
}

.content-container {
  max-width: 1400px;
  margin: 0 auto;
}

.loading-container {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.hot-topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

/* 平台卡片 */
.platform-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s ease;
}

.platform-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.platform-header {
  padding: 20px 24px 16px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.platform-info {
  flex: 1;
}

.platform-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.platform-type {
  font-size: 0.875rem;
  color: #6b7280;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 12px;
}

.platform-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.update-time {
  font-size: 0.75rem;
  color: #9ca3af;
}

/* 热榜列表 */
.hot-list {
  max-height: 480px;
  overflow-y: auto;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #f8fafc;
}

.hot-item:hover {
  background: #f8fafc;
}

.hot-item:last-child {
  border-bottom: none;
}

.item-rank {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #e5e7eb;
  color: #6b7280;
  font-size: 0.875rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hot-item:nth-child(-n+3) .item-rank {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: white;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 0.95rem;
  font-weight: 500;
  color: #1f2937;
  margin: 0 0 6px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.8rem;
  color: #6b7280;
}

.hot-value {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ef4444;
  font-weight: 500;
}

.author {
  color: #6b7280;
}

/* 平台底部 */
.platform-footer {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.view-more-btn {
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  background: white;
  border-radius: 16px;
  padding: 60px 30px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .main-title {
    font-size: 2rem;
    justify-content: center;
  }

  .filter-content {
    flex-direction: column;
    gap: 16px;
  }

  .hot-topics-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .platform-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .platform-meta {
    align-items: flex-start;
  }
}

@media (max-width: 480px) {
  .page-header {
    padding: 30px 15px;
  }

  .content-section {
    padding: 20px 15px 40px;
  }

  .platform-card {
    border-radius: 12px;
  }

  .hot-item {
    padding: 10px 20px;
  }

  .category-filters {
    justify-content: center;
  }
}
</style>
