<template>
  <div class="moyu-statistics">
    <!-- 页面头部 -->
    <el-card class="header-card" shadow="never">
      <div class="header-content">
        <div class="title-section">
          <h2>统计分析</h2>
          <p>摸鱼工具模块的数据统计分析，包括事件统计、内容统计、用户行为分析等</p>
        </div>
        <div class="action-section">
          <el-button type="primary" @click="refreshAllData">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 概览统计 -->
    <el-card class="overview-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>数据概览</span>
          <el-tag type="info">{{ updateTime }}</el-tag>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="overview-item">
            <div class="overview-icon events">
              <el-icon><Calendar /></el-icon>
            </div>
            <div class="overview-content">
              <div class="overview-number">{{ eventStats.totalEvents || 0 }}</div>
              <div class="overview-label">总事件数</div>
              <div class="overview-detail">
                重要事件 {{ eventStats.majorEvents || 0 }} 个
              </div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="overview-item">
            <div class="overview-icon contents">
              <el-icon><Document /></el-icon>
            </div>
            <div class="overview-content">
              <div class="overview-number">{{ contentStats.totalContents || 0 }}</div>
              <div class="overview-label">总内容数</div>
              <div class="overview-detail">
                总查看 {{ contentStats.totalViews || 0 }} 次
              </div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="overview-item">
            <div class="overview-icon views">
              <el-icon><View /></el-icon>
            </div>
            <div class="overview-content">
              <div class="overview-number">{{ contentStats.totalLikes || 0 }}</div>
              <div class="overview-label">总点赞数</div>
              <div class="overview-detail">
                平均点赞 {{ averageLikes }} 次/内容
              </div>
            </div>
          </div>
        </el-col>
        
        <el-col :span="6">
          <div class="overview-item">
            <div class="overview-icon collections">
              <el-icon><Star /></el-icon>
            </div>
            <div class="overview-content">
              <div class="overview-number">{{ collectionStats.totalCollections || 0 }}</div>
              <div class="overview-label">总收藏数</div>
              <div class="overview-detail">
                事件收藏 {{ collectionStats.eventCollections || 0 }} 个
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="20">
      <!-- 事件统计 -->
      <el-col :span="12">
        <el-card class="stats-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>事件统计</span>
              <el-button type="text" @click="loadEventStats">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div class="stats-content">
            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-value primary">{{ eventStats.programmerHolidays || 0 }}</div>
                <div class="stat-label">程序员节日</div>
              </div>
              <div class="stat-item">
                <div class="stat-value success">{{ eventStats.techMemorials || 0 }}</div>
                <div class="stat-label">技术纪念日</div>
              </div>
              <div class="stat-item">
                <div class="stat-value warning">{{ eventStats.openSourceHolidays || 0 }}</div>
                <div class="stat-label">开源节日</div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="progress-list">
              <div class="progress-item">
                <div class="progress-label">
                  <span>程序员节日</span>
                  <span>{{ getPercentage(eventStats.programmerHolidays, eventStats.totalEvents) }}%</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(eventStats.programmerHolidays, eventStats.totalEvents)"
                  :stroke-width="8"
                  :show-text="false"
                  color="#409eff"
                />
              </div>
              
              <div class="progress-item">
                <div class="progress-label">
                  <span>技术纪念日</span>
                  <span>{{ getPercentage(eventStats.techMemorials, eventStats.totalEvents) }}%</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(eventStats.techMemorials, eventStats.totalEvents)"
                  :stroke-width="8"
                  :show-text="false"
                  color="#67c23a"
                />
              </div>
              
              <div class="progress-item">
                <div class="progress-label">
                  <span>开源节日</span>
                  <span>{{ getPercentage(eventStats.openSourceHolidays, eventStats.totalEvents) }}%</span>
                </div>
                <el-progress 
                  :percentage="getPercentage(eventStats.openSourceHolidays, eventStats.totalEvents)"
                  :stroke-width="8"
                  :show-text="false"
                  color="#e6a23c"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 内容统计 -->
      <el-col :span="12">
        <el-card class="stats-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>内容统计</span>
              <el-button type="text" @click="loadContentStats">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </div>
          </template>
          
          <div class="stats-content">
            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-value primary">{{ contentStats.quotes || 0 }}</div>
                <div class="stat-label">编程格言</div>
              </div>
              <div class="stat-item">
                <div class="stat-value success">{{ contentStats.tips || 0 }}</div>
                <div class="stat-label">技术小贴士</div>
              </div>
            </div>
            
            <div class="stat-row">
              <div class="stat-item">
                <div class="stat-value warning">{{ contentStats.codeSnippets || 0 }}</div>
                <div class="stat-label">代码片段</div>
              </div>
              <div class="stat-item">
                <div class="stat-value info">{{ contentStats.histories || 0 }}</div>
                <div class="stat-label">历史上的今天</div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="engagement-stats">
              <div class="engagement-item">
                <el-icon><View /></el-icon>
                <span>总查看数：{{ contentStats.totalViews || 0 }}</span>
              </div>
              <div class="engagement-item">
                <el-icon><Star /></el-icon>
                <span>总点赞数：{{ contentStats.totalLikes || 0 }}</span>
              </div>
              <div class="engagement-item">
                <el-icon><DataAnalysis /></el-icon>
                <span>平均互动率：{{ engagementRate }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门内容排行 -->
    <el-card class="ranking-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>热门内容排行</span>
          <div>
            <el-button type="text" @click="loadPopularContent">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button type="primary" size="small" @click="viewAllPopular">
              查看全部
            </el-button>
          </div>
        </div>
      </template>
      
      <el-table :data="popularContent" v-loading="popularLoading" stripe>
        <el-table-column type="index" label="排名" width="70" align="center" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="content-title-ranking">
              <el-icon :color="getContentTypeColor(row.contentType)">
                <component :is="getContentTypeIcon(row.contentType)" />
              </el-icon>
              <span style="margin-left: 8px;">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="contentType" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getContentTypeTag(row.contentType)" size="small">
              {{ getContentTypeName(row.contentType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="查看数" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-number">{{ row.viewCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞数" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-number">{{ row.likeCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="热度指数" width="100" align="center">
          <template #default="{ row }">
            <span class="heat-index">{{ getHeatIndex(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 近期趋势 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="trend-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>数据趋势</span>
              <el-radio-group v-model="trendType" size="small">
                <el-radio-button label="events">事件</el-radio-button>
                <el-radio-button label="contents">内容</el-radio-button>
                <el-radio-button label="interactions">互动</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <div class="trend-placeholder">
            <el-empty description="图表功能开发中，敬请期待">
              <template #image>
                <el-icon size="100" color="#409eff"><DataAnalysis /></el-icon>
              </template>
            </el-empty>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Refresh, Calendar, Document, View, Star, DataAnalysis
} from '@element-plus/icons-vue'
import { statisticsApi } from '@/api/moyu'

// 响应式数据
const updateTime = ref('')
const popularLoading = ref(false)
const trendType = ref('events')

// 统计数据
const eventStats = reactive({
  totalEvents: 0,
  majorEvents: 0,
  programmerHolidays: 0,
  techMemorials: 0,
  openSourceHolidays: 0
})

const contentStats = reactive({
  totalContents: 0,
  quotes: 0,
  tips: 0,
  codeSnippets: 0,
  histories: 0,
  totalViews: 0,
  totalLikes: 0
})

const collectionStats = reactive({
  totalCollections: 0,
  eventCollections: 0,
  contentCollections: 0
})

const popularContent = ref([])

// 计算属性
const averageLikes = computed(() => {
  if (!contentStats.totalContents || !contentStats.totalLikes) return '0'
  return (contentStats.totalLikes / contentStats.totalContents).toFixed(1)
})

const engagementRate = computed(() => {
  if (!contentStats.totalContents || (!contentStats.totalViews && !contentStats.totalLikes)) return '0'
  const totalEngagement = (contentStats.totalViews || 0) + (contentStats.totalLikes || 0) * 2
  return ((totalEngagement / contentStats.totalContents) / 100).toFixed(1)
})

// 获取百分比
const getPercentage = (value, total) => {
  if (!total) return 0
  return Math.round((value / total) * 100)
}

// 获取内容类型相关方法
const getContentTypeName = (type) => {
  const typeMap = {
    1: '编程格言',
    2: '技术小贴士', 
    3: '代码片段',
    4: '历史上的今天'
  }
  return typeMap[type] || '未知'
}

const getContentTypeTag = (type) => {
  const tagMap = {
    1: 'success',
    2: 'warning',
    3: 'primary',
    4: 'info'
  }
  return tagMap[type] || 'info'
}

const getContentTypeColor = (type) => {
  const colorMap = {
    1: '#67c23a',
    2: '#e6a23c',
    3: '#409eff',
    4: '#909399'
  }
  return colorMap[type] || '#909399'
}

const getContentTypeIcon = (type) => {
  const iconMap = {
    1: 'Star',
    2: 'InfoFilled',
    3: 'EditPen',
    4: 'Calendar'
  }
  return iconMap[type] || 'Document'
}

// 获取热度指数
const getHeatIndex = (content) => {
  const views = content.viewCount || 0
  const likes = content.likeCount || 0
  // 简单的热度计算：查看数 + 点赞数 * 2
  return views + likes * 2
}

// 格式化日期
const formatDate = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

// 加载事件统计
const loadEventStats = async () => {
  try {
    const data = await statisticsApi.getEventStatistics()
    Object.assign(eventStats, data || {})
  } catch (error) {
    console.error('加载事件统计失败:', error)
    ElMessage.error('加载事件统计失败')
  }
}

// 加载内容统计
const loadContentStats = async () => {
  try {
    const data = await statisticsApi.getContentStatistics()
    Object.assign(contentStats, data || {})
  } catch (error) {
    console.error('加载内容统计失败:', error)
    ElMessage.error('加载内容统计失败')
  }
}

// 加载收藏统计
const loadCollectionStats = async () => {
  try {
    const data = await statisticsApi.getCollectionStatistics()
    Object.assign(collectionStats, data || {})
  } catch (error) {
    console.error('加载收藏统计失败:', error)
  }
}

// 加载热门内容
const loadPopularContent = async () => {
  try {
    popularLoading.value = true
    const data = await statisticsApi.getPopularContent({ limit: 10 })
    popularContent.value = data || []
  } catch (error) {
    console.error('加载热门内容失败:', error)
    ElMessage.error('加载热门内容失败')
  } finally {
    popularLoading.value = false
  }
}

// 刷新所有数据
const refreshAllData = async () => {
  updateTime.value = '更新中...'
  await Promise.all([
    loadEventStats(),
    loadContentStats(),
    loadCollectionStats(),
    loadPopularContent()
  ])
  updateTime.value = `最后更新: ${new Date().toLocaleString('zh-CN')}`
  ElMessage.success('数据刷新完成')
}

// 查看全部热门内容
const viewAllPopular = () => {
  // 跳转到内容管理页面并加载热门排行
  window.open('#/moyu/daily-content')
}

// 初始化
onMounted(() => {
  refreshAllData()
})
</script>

<style scoped>
.moyu-statistics {
  padding: 0;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title-section h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.title-section p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.action-section {
  display: flex;
  gap: 12px;
}

.overview-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-item {
  display: flex;
  align-items: center;
  padding: 20px;
}

.overview-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: white;
}

.overview-icon.events { background: #409eff; }
.overview-icon.contents { background: #67c23a; }
.overview-icon.views { background: #e6a23c; }
.overview-icon.collections { background: #f56c6c; }

.overview-content {
  flex: 1;
}

.overview-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 4px;
}

.overview-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.overview-detail {
  font-size: 12px;
  color: #909399;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-content {
  padding: 10px;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 8px;
}

.stat-value.primary { color: #409eff; }
.stat-value.success { color: #67c23a; }
.stat-value.warning { color: #e6a23c; }
.stat-value.info { color: #909399; }

.stat-label {
  font-size: 14px;
  color: #606266;
}

.progress-list {
  margin-top: 20px;
}

.progress-item {
  margin-bottom: 16px;
}

.progress-item:last-child {
  margin-bottom: 0;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.engagement-stats {
  margin-top: 20px;
}

.engagement-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
}

.engagement-item .el-icon {
  margin-right: 8px;
  color: #909399;
}

.ranking-card {
  margin-bottom: 20px;
}

.content-title-ranking {
  display: flex;
  align-items: center;
}

.stat-number {
  font-weight: bold;
  color: #409eff;
}

.heat-index {
  font-weight: bold;
  color: #f56c6c;
}

.trend-card {
  margin-bottom: 20px;
}

.trend-placeholder {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 16px;
  }
  
  .overview-item {
    padding: 16px;
  }
  
  .overview-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
  }
  
  .overview-number {
    font-size: 20px;
  }
  
  .stat-row {
    flex-direction: column;
    gap: 16px;
  }
}
</style>
