<template>
  <CnPage class="moyu-statistics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="摸鱼工具统计"
      description="汇总日历事件、每日内容、收藏和热门内容排行。"
      eyebrow="Moyu Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="info" subtle>{{ updateTime }}</CnStatusTag>
        <CnStatusTag type="brand">事件 {{ eventStats.totalEvents || 0 }}</CnStatusTag>
        <CnStatusTag type="success">内容 {{ contentStats.totalContents || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">收藏 {{ collectionStats.totalCollections || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" @click="refreshAllData">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <div class="overview-grid">
      <CnStatCard
        title="总事件数"
        :value="eventStats.totalEvents || 0"
        :description="`重要事件 ${eventStats.majorEvents || 0} 个`"
        tone="brand"
      />
      <CnStatCard
        title="总内容数"
        :value="contentStats.totalContents || 0"
        :description="`总查看 ${contentStats.totalViews || 0} 次`"
        tone="success"
      />
      <CnStatCard
        title="总点赞数"
        :value="contentStats.totalLikes || 0"
        :description="`平均点赞 ${averageLikes} 次/内容`"
        tone="warning"
      />
      <CnStatCard
        title="总收藏数"
        :value="collectionStats.totalCollections || 0"
        :description="`事件收藏 ${collectionStats.eventCollections || 0} 个`"
        tone="danger"
      />
    </div>

    <div class="stats-panels">
      <CnSection title="事件统计" description="按事件类型拆分当前日历事件库。" divided>
        <template #actions>
          <el-button type="primary" link :icon="Refresh" @click="loadEventStats">刷新</el-button>
        </template>

        <div class="mini-stat-grid">
          <div class="mini-stat">
            <span class="mini-stat__value tone-brand">{{ eventStats.programmerHolidays || 0 }}</span>
            <span class="mini-stat__label">程序员节日</span>
          </div>
          <div class="mini-stat">
            <span class="mini-stat__value tone-success">{{ eventStats.techMemorials || 0 }}</span>
            <span class="mini-stat__label">技术纪念日</span>
          </div>
          <div class="mini-stat">
            <span class="mini-stat__value tone-warning">{{ eventStats.openSourceHolidays || 0 }}</span>
            <span class="mini-stat__label">开源节日</span>
          </div>
        </div>

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
              color="var(--cn-color-brand-primary)"
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
              color="var(--cn-color-success)"
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
              color="var(--cn-color-warning)"
            />
          </div>
        </div>
      </CnSection>

      <CnSection title="内容统计" description="按内容类型和互动行为观察每日内容质量。" divided>
        <template #actions>
          <el-button type="primary" link :icon="Refresh" @click="loadContentStats">刷新</el-button>
        </template>

        <div class="mini-stat-grid">
          <div class="mini-stat">
            <span class="mini-stat__value tone-brand">{{ contentStats.quotes || 0 }}</span>
            <span class="mini-stat__label">编程格言</span>
          </div>
          <div class="mini-stat">
            <span class="mini-stat__value tone-success">{{ contentStats.tips || 0 }}</span>
            <span class="mini-stat__label">技术小贴士</span>
          </div>
          <div class="mini-stat">
            <span class="mini-stat__value tone-warning">{{ contentStats.codeSnippets || 0 }}</span>
            <span class="mini-stat__label">代码片段</span>
          </div>
          <div class="mini-stat">
            <span class="mini-stat__value tone-info">{{ contentStats.histories || 0 }}</span>
            <span class="mini-stat__label">历史上的今天</span>
          </div>
        </div>

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
      </CnSection>
    </div>

    <CnSection title="热门内容排行" description="按查看数和点赞数计算热度指数。" divided>
      <template #actions>
        <el-button type="primary" link :icon="Refresh" @click="loadPopularContent">刷新</el-button>
        <el-button type="primary" size="small" @click="viewAllPopular">查看全部</el-button>
      </template>

      <CnDataTable
        :columns="popularColumns"
        :data="popularContent"
        :loading="popularLoading"
        :pagination="null"
        row-key="id"
        empty-title="暂无热门内容"
        empty-description="还没有可展示的热门内容排行。"
        empty-icon="MR"
      >
        <template #title="{ row }">
          <div class="content-title-ranking">
            <el-icon class="type-icon" :class="`type-icon--${row.contentType || 'default'}`">
              <component :is="getContentTypeIcon(row.contentType)" />
            </el-icon>
            <span>{{ row.title }}</span>
          </div>
        </template>

        <template #contentType="{ row }">
          <CnStatusTag :type="getContentTypeTone(row.contentType)" size="sm">
            {{ getContentTypeName(row.contentType) }}
          </CnStatusTag>
        </template>

        <template #viewCount="{ row }">
          <span class="metric-number">{{ row.viewCount || 0 }}</span>
        </template>

        <template #likeCount="{ row }">
          <span class="metric-number">{{ row.likeCount || 0 }}</span>
        </template>

        <template #heatIndex="{ row }">
          <CnStatusTag type="danger" size="sm">{{ getHeatIndex(row) }}</CnStatusTag>
        </template>

        <template #createTime="{ row }">
          {{ formatDate(row.createTime) }}
        </template>
      </CnDataTable>
    </CnSection>

    <CnSection title="数据趋势" description="趋势图表入口已预留，后续可接入趋势接口。" divided>
      <template #actions>
        <el-radio-group v-model="trendType" size="small">
          <el-radio-button label="events">事件</el-radio-button>
          <el-radio-button label="contents">内容</el-radio-button>
          <el-radio-button label="interactions">互动</el-radio-button>
        </el-radio-group>
      </template>

      <CnEmptyState title="图表功能开发中" description="趋势数据接口已保留，图表组件接入后可展示事件、内容和互动趋势。" icon="DA" />
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Refresh, Star, View } from '@element-plus/icons-vue'
import { statisticsApi } from '@/api/moyu'
import { CnDataTable, CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface EventStats {
  totalEvents: number
  majorEvents: number
  programmerHolidays: number
  techMemorials: number
  openSourceHolidays: number
}

interface ContentStats {
  totalContents: number
  quotes: number
  tips: number
  codeSnippets: number
  histories: number
  totalViews: number
  totalLikes: number
}

interface CollectionStats {
  totalCollections: number
  eventCollections: number
  contentCollections: number
}

interface PopularContent extends Record<string, unknown> {
  id?: number
  title?: string
  contentType?: number
  viewCount?: number
  likeCount?: number
  createTime?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '摸鱼工具' }, { label: '统计分析' }]

const updateTime = ref('')
const popularLoading = ref(false)
const trendType = ref('events')

const eventStats = reactive<EventStats>({
  totalEvents: 0,
  majorEvents: 0,
  programmerHolidays: 0,
  techMemorials: 0,
  openSourceHolidays: 0
})

const contentStats = reactive<ContentStats>({
  totalContents: 0,
  quotes: 0,
  tips: 0,
  codeSnippets: 0,
  histories: 0,
  totalViews: 0,
  totalLikes: 0
})

const collectionStats = reactive<CollectionStats>({
  totalCollections: 0,
  eventCollections: 0,
  contentCollections: 0
})

const popularContent = ref<PopularContent[]>([])

const popularColumns: CnTableColumn<PopularContent>[] = [
  { type: 'index', label: '排名', width: 70, align: 'center' },
  { prop: 'title', label: '标题', minWidth: 240, slot: 'title', showOverflowTooltip: true },
  { prop: 'contentType', label: '类型', width: 120, align: 'center', slot: 'contentType' },
  { prop: 'viewCount', label: '查看数', width: 100, align: 'center', slot: 'viewCount' },
  { prop: 'likeCount', label: '点赞数', width: 100, align: 'center', slot: 'likeCount' },
  { label: '热度指数', width: 110, align: 'center', slot: 'heatIndex' },
  { prop: 'createTime', label: '创建时间', width: 160, align: 'center', slot: 'createTime' }
]

const averageLikes = computed(() => {
  if (!contentStats.totalContents || !contentStats.totalLikes) return '0'
  return (contentStats.totalLikes / contentStats.totalContents).toFixed(1)
})

const engagementRate = computed(() => {
  if (!contentStats.totalContents || (!contentStats.totalViews && !contentStats.totalLikes)) return '0'
  const totalEngagement = (contentStats.totalViews || 0) + (contentStats.totalLikes || 0) * 2
  return ((totalEngagement / contentStats.totalContents) / 100).toFixed(1)
})

const getPercentage = (value: number, total: number) => {
  if (!total) return 0
  return Math.round((value / total) * 100)
}

const getContentTypeName = (type?: number) => {
  const typeMap: Record<number, string> = {
    1: '编程格言',
    2: '技术小贴士',
    3: '代码片段',
    4: '历史上的今天'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

const getContentTypeTone = (type?: number): CnTone => {
  const tagMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'brand',
    4: 'info'
  }
  return type ? tagMap[type] || 'neutral' : 'neutral'
}

const getContentTypeIcon = (type?: number) => {
  const iconMap: Record<number, string> = {
    1: 'Star',
    2: 'InfoFilled',
    3: 'EditPen',
    4: 'Calendar'
  }
  return type ? iconMap[type] || 'Document' : 'Document'
}

const getHeatIndex = (content: PopularContent) => {
  const views = content.viewCount || 0
  const likes = content.likeCount || 0
  return views + likes * 2
}

const formatDate = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

const loadEventStats = async () => {
  try {
    const data = await statisticsApi.getEventStatistics()
    Object.assign(eventStats, data || {})
  } catch (error) {
    console.error('加载事件统计失败:', error)
    ElMessage.error('加载事件统计失败')
  }
}

const loadContentStats = async () => {
  try {
    const data = await statisticsApi.getContentStatistics()
    Object.assign(contentStats, data || {})
  } catch (error) {
    console.error('加载内容统计失败:', error)
    ElMessage.error('加载内容统计失败')
  }
}

const loadCollectionStats = async () => {
  try {
    const data = await statisticsApi.getCollectionStatistics()
    Object.assign(collectionStats, data || {})
  } catch (error) {
    console.error('加载收藏统计失败:', error)
  }
}

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

const refreshAllData = async () => {
  updateTime.value = '更新中...'
  await Promise.all([loadEventStats(), loadContentStats(), loadCollectionStats(), loadPopularContent()])
  updateTime.value = `最后更新: ${new Date().toLocaleString('zh-CN')}`
  ElMessage.success('数据刷新完成')
}

const viewAllPopular = () => {
  window.open('#/moyu/daily-content')
}

onMounted(() => {
  refreshAllData()
})
</script>

<style scoped>
.moyu-statistics-page {
  min-height: 100%;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.stats-panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.mini-stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.mini-stat {
  display: grid;
  gap: var(--cn-space-2);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  text-align: center;
}

.mini-stat__value {
  font-family: var(--cn-font-heading);
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
}

.mini-stat__label,
.progress-label,
.engagement-item {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
}

.tone-brand {
  color: var(--cn-color-brand-primary);
}

.tone-success {
  color: var(--cn-color-success);
}

.tone-warning {
  color: var(--cn-color-warning);
}

.tone-info {
  color: var(--cn-color-info);
}

.progress-list,
.engagement-stats {
  display: grid;
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-5);
}

.progress-label,
.engagement-item,
.content-title-ranking {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.progress-label {
  justify-content: space-between;
  margin-bottom: var(--cn-space-2);
}

.content-title-ranking {
  min-width: 0;
}

.content-title-ranking span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-icon--1 {
  color: var(--cn-color-success);
}

.type-icon--2 {
  color: var(--cn-color-warning);
}

.type-icon--3 {
  color: var(--cn-color-brand-primary);
}

.type-icon--4 {
  color: var(--cn-color-info);
}

.type-icon--default {
  color: var(--cn-color-text-tertiary);
}

.metric-number {
  color: var(--cn-color-brand-primary);
  font-weight: 700;
}

@media (max-width: 1080px) {
  .overview-grid,
  .stats-panels {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .overview-grid,
  .stats-panels,
  .mini-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
