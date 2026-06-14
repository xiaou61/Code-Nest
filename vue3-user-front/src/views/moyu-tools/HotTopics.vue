<template>
  <CnPage class="hot-topics-page" max-width="1440px" full-height>
    <CnPageHeader
      title="今日热榜"
      description="汇聚各大平台实时热点，一站式浏览全网热门话题。"
      eyebrow="MOYU TOOL"
      :breadcrumbs="[{ label: '摸鱼工具箱', to: '/moyu-tools' }, { label: '今日热榜' }]"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ selectedCategory }}</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>{{ platformCount }} 个平台</CnStatusTag>
        <CnStatusTag v-if="lastUpdateTime" type="success" size="sm" subtle>
          更新于 {{ formatTime(lastUpdateTime) }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="refreshing" @click="refreshAllData">
          {{ refreshing ? '刷新中...' : '刷新数据' }}
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection title="分类筛选" description="按平台内容方向筛选热榜。" divided>
      <div class="filter-bar">
        <div class="category-filters">
          <el-button
            v-for="category in categories"
            :key="category.name"
            :type="selectedCategory === category.name ? 'primary' : 'default'"
            @click="selectCategory(category.name)"
          >
            {{ category.name }}
          </el-button>
        </div>
        <span v-if="lastUpdateTime" class="update-info">
          <el-icon><Clock /></el-icon>
          最后更新：{{ formatTime(lastUpdateTime) }}
        </span>
      </div>
    </CnSection>

    <CnSection title="热榜内容" :description="contentDescription" divided>
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-else class="hot-topics-grid">
        <article
          v-for="(platformData, platform) in filteredPlatforms"
          :key="platform"
          class="platform-card"
        >
          <header class="platform-header">
            <div class="platform-info">
              <h3>{{ platformData.title }}</h3>
              <CnStatusTag type="neutral" size="sm" subtle>{{ platformData.type || '热榜' }}</CnStatusTag>
            </div>
            <div class="platform-meta">
              <span>{{ formatTime(platformData.updatedAt) }}</span>
              <CnStatusTag v-if="platformData.fromCache" type="success" size="sm">缓存</CnStatusTag>
            </div>
          </header>

          <div class="hot-list">
            <button
              v-for="(item, index) in platformData.data?.slice(0, 10)"
              :key="item.id || `${platform}-${index}`"
              class="hot-item"
              type="button"
              @click="openLink(item.url)"
            >
              <span class="item-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <span class="item-content">
                <span class="item-title">{{ item.title }}</span>
                <span class="item-meta">
                  <span v-if="item.hot" class="hot-value">
                    <el-icon><TrendCharts /></el-icon>
                    {{ formatHotValue(item.hot) }}
                  </span>
                  <span v-if="item.author">{{ item.author }}</span>
                </span>
              </span>
            </button>
          </div>

          <footer class="platform-footer">
            <el-button text type="primary" @click="openLink(platformData.link)">
              查看更多
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </footer>
        </article>
      </div>

      <CnEmptyState
        v-if="!loading && platformCount === 0"
        title="暂无热榜数据"
        description="可以重新加载，或切换其他分类。"
        icon="HOT"
        surface="transparent"
      >
        <template #actions>
          <el-button type="primary" :loading="refreshing" @click="refreshAllData">重新加载</el-button>
        </template>
      </CnEmptyState>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, Clock, Refresh, TrendCharts } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { getAllHotTopicData, refreshHotTopicData } from '@/api/moyu'

interface CategoryItem {
  name: string
  description: string
}

interface HotItem {
  id?: string | number
  title: string
  url?: string
  hot?: number
  author?: string
}

interface PlatformData {
  title: string
  type?: string
  updatedAt?: string | Date
  fromCache?: boolean
  link?: string
  data?: HotItem[]
}

type PlatformMap = Record<string, PlatformData>

const loading = ref(true)
const refreshing = ref(false)
const selectedCategory = ref('全部')
const lastUpdateTime = ref<Date | null>(null)

const categories = ref<CategoryItem[]>([
  { name: '全部', description: '所有平台' },
  { name: '社交媒体', description: '社交平台热榜和趋势数据' },
  { name: '知识社区', description: '知识问答和技术社区热门内容' },
  { name: '新闻资讯', description: '新闻媒体和资讯平台热点内容' },
  { name: '科技数码', description: '科技、数码、编程相关热门内容' },
  { name: '娱乐生活', description: '娱乐、文化、生活方式相关内容' },
  { name: '实用信息', description: '天气、灾害等实用信息服务' }
])

const platformsData = reactive<PlatformMap>({})

const platformCategoryMap: Record<string, string> = {
  douyin: '社交媒体',
  kuaishou: '社交媒体',
  weibo: '社交媒体',
  hupu: '知识社区',
  linuxdo: '知识社区',
  newsmth: '知识社区',
  tieba: '知识社区',
  zhihu: '知识社区',
  'zhihu-daily': '知识社区',
  ifanr: '新闻资讯',
  'netease-news': '新闻资讯',
  toutiao: '新闻资讯',
  csdn: '科技数码',
  dgtle: '科技数码',
  geekpark: '科技数码',
  guokr: '科技数码',
  hellogithub: '科技数码',
  ithome: '科技数码',
  juejin: '科技数码',
  'douban-movie': '娱乐生活',
  jianshu: '娱乐生活',
  weread: '娱乐生活',
  earthquake: '实用信息',
  history: '实用信息',
  'weather-alarm': '实用信息'
}

const filteredPlatforms = computed<PlatformMap>(() => {
  if (selectedCategory.value === '全部') {
    return platformsData
  }

  const filtered: PlatformMap = {}
  Object.keys(platformsData).forEach((platform) => {
    if (platformCategoryMap[platform] === selectedCategory.value) {
      filtered[platform] = platformsData[platform]
    }
  })
  return filtered
})

const platformCount = computed(() => Object.keys(filteredPlatforms.value).length)

const contentDescription = computed(() => {
  if (loading.value) return '正在加载各平台热榜数据。'
  return `当前分类展示 ${platformCount.value} 个平台的前 10 条热门内容。`
})

const selectCategory = (category: string) => {
  selectedCategory.value = category
}

const loadHotTopicsData = async () => {
  try {
    loading.value = true
    const response = (await getAllHotTopicData()) as PlatformMap | null

    if (response) {
      Object.keys(platformsData).forEach((platform) => {
        delete platformsData[platform]
      })
      Object.keys(response).forEach((platform) => {
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

const refreshAllData = async () => {
  try {
    refreshing.value = true
    await refreshHotTopicData()
    ElMessage.success('数据刷新成功')
    await loadHotTopicsData()
  } catch (error) {
    console.error('刷新数据失败:', error)
    ElMessage.error('刷新数据失败')
  } finally {
    refreshing.value = false
  }
}

const openLink = (url?: string) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const formatTime = (time?: string | Date | null) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) {
    return '刚刚'
  }
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  return date.toLocaleString()
}

const formatHotValue = (hot?: number) => {
  if (!hot) return ''
  if (hot >= 100000000) {
    return `${(hot / 100000000).toFixed(1)}亿`
  }
  if (hot >= 10000) {
    return `${(hot / 10000).toFixed(1)}万`
  }
  return hot.toString()
}

onMounted(() => {
  loadHotTopicsData()
})
</script>

<style scoped>
.hot-topics-page {
  min-width: 0;
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.category-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.update-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  white-space: nowrap;
}

.loading-container {
  padding: var(--cn-space-4);
}

.hot-topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--cn-space-4);
}

.platform-card {
  display: grid;
  overflow: hidden;
  min-width: 0;
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.platform-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.platform-info {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.platform-info h3 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 650;
  line-height: 1.4;
}

.platform-meta {
  display: grid;
  justify-items: end;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  white-space: nowrap;
}

.hot-list {
  display: grid;
  max-height: 480px;
  overflow-y: auto;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
  padding: var(--cn-space-3) var(--cn-space-4);
  border: 0;
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.hot-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.item-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
}

.item-rank.top {
  background: var(--cn-color-danger-soft);
  color: var(--cn-color-danger);
}

.item-content {
  display: grid;
  flex: 1;
  gap: var(--cn-space-1);
  min-width: 0;
}

.item-title {
  display: -webkit-box;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.hot-value {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--cn-color-danger);
  font-weight: 700;
}

.platform-footer {
  padding: var(--cn-space-3) var(--cn-space-4);
  text-align: center;
}

@media (max-width: 768px) {
  .filter-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .hot-topics-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .platform-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .platform-meta {
    justify-items: start;
  }
}
</style>
