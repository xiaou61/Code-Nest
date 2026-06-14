<template>
  <CnPage class="codepen-statistics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="CodePen 数据统计"
      description="汇总作品、创作者、浏览与互动指标，用于观察 CodePen 社区运营表现。"
      eyebrow="CodePen Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">作品 {{ statistics.totalPens || 0 }} 个</CnStatusTag>
        <CnStatusTag type="success">创作者 {{ statistics.totalUsers || 0 }} 人</CnStatusTag>
        <CnStatusTag type="info">今日新增 {{ statistics.todayNewPens || 0 }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadStatistics">刷新数据</el-button>
      </template>
    </CnPageHeader>

    <div class="statistics-grid">
      <CnStatCard
        title="作品总数"
        :value="statistics.totalPens || 0"
        description="已创建的 CodePen 作品数量"
        tone="brand"
        :loading="loading"
      />
      <CnStatCard
        title="创作者总数"
        :value="statistics.totalUsers || 0"
        description="参与创作的用户数量"
        tone="success"
        :loading="loading"
      />
      <CnStatCard
        title="总浏览量"
        :value="formatNumber(statistics.totalViews)"
        description="作品详情和预览累计访问"
        tone="info"
        :loading="loading"
      />
      <CnStatCard
        title="总点赞量"
        :value="formatNumber(statistics.totalLikes)"
        description="用户对作品的点赞反馈"
        tone="danger"
        :loading="loading"
      />
      <CnStatCard
        title="总评论量"
        :value="formatNumber(statistics.totalComments)"
        description="作品下的讨论与反馈"
        tone="warning"
        :loading="loading"
      />
      <CnStatCard
        title="总收藏量"
        :value="formatNumber(statistics.totalCollects)"
        description="用户收藏作品的累计次数"
        tone="success"
        :loading="loading"
      />
      <CnStatCard
        title="总 Fork 量"
        :value="formatNumber(statistics.totalForks)"
        description="作品被复用和二创的次数"
        tone="brand"
        :loading="loading"
      />
      <CnStatCard
        title="今日新增"
        :value="statistics.todayNewPens || 0"
        description="当天新发布或新创建作品"
        tone="info"
        :loading="loading"
      />
    </div>

    <CnSection title="数据概览" description="核心指标明细与平均互动表现。" divided>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="作品总数">{{ statistics.totalPens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="今日新增">{{ statistics.todayNewPens || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创作者总数">{{ statistics.totalUsers || 0 }}</el-descriptions-item>
        <el-descriptions-item label="总浏览量">{{ formatNumber(statistics.totalViews) }}</el-descriptions-item>
        <el-descriptions-item label="总点赞量">{{ formatNumber(statistics.totalLikes) }}</el-descriptions-item>
        <el-descriptions-item label="总评论量">{{ formatNumber(statistics.totalComments) }}</el-descriptions-item>
        <el-descriptions-item label="总收藏量">{{ formatNumber(statistics.totalCollects) }}</el-descriptions-item>
        <el-descriptions-item label="总 Fork 量">{{ formatNumber(statistics.totalForks) }}</el-descriptions-item>
        <el-descriptions-item label="平均互动率">{{ calculateEngagementRate() }}%</el-descriptions-item>
      </el-descriptions>
    </CnSection>

    <CnSection title="互动数据分布" description="按作品数量归一后的平均浏览、点赞、评论和收藏表现。" divided>
      <div class="average-grid">
        <div class="average-item">
          <span class="average-label">平均浏览量</span>
          <strong class="average-value">{{ calculateAverage(statistics.totalViews, statistics.totalPens) }}</strong>
        </div>
        <div class="average-item">
          <span class="average-label">平均点赞量</span>
          <strong class="average-value">{{ calculateAverage(statistics.totalLikes, statistics.totalPens) }}</strong>
        </div>
        <div class="average-item">
          <span class="average-label">平均评论量</span>
          <strong class="average-value">{{ calculateAverage(statistics.totalComments, statistics.totalPens) }}</strong>
        </div>
        <div class="average-item">
          <span class="average-label">平均收藏量</span>
          <strong class="average-value">{{ calculateAverage(statistics.totalCollects, statistics.totalPens) }}</strong>
        </div>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { codepenApi } from '@/api/codepen'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface CodePenStatistics {
  totalPens?: number
  totalUsers?: number
  todayNewPens?: number
  totalViews?: number
  totalLikes?: number
  totalComments?: number
  totalCollects?: number
  totalForks?: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'CodePen 管理' }, { label: '数据统计' }]

const loading = ref(false)
const statistics = ref<CodePenStatistics>({
  totalPens: 0,
  totalUsers: 0,
  todayNewPens: 0,
  totalViews: 0,
  totalLikes: 0,
  totalComments: 0,
  totalCollects: 0,
  totalForks: 0
})

const loadStatistics = async () => {
  try {
    loading.value = true
    const result = await codepenApi.getStatistics()
    statistics.value = result || {}
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

const formatNumber = (num?: number) => {
  if (!num) return 0
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`
  }
  if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`
  }
  return num
}

const calculateAverage = (total?: number, count?: number) => {
  if (!count || count === 0) return 0
  return Math.round((total || 0) / count)
}

const calculateEngagementRate = () => {
  const total = statistics.value.totalPens || 0
  if (total === 0) return 0

  const engagements =
    (statistics.value.totalLikes || 0) +
    (statistics.value.totalComments || 0) +
    (statistics.value.totalCollects || 0)

  const views = statistics.value.totalViews || 1

  return ((engagements / views) * 100).toFixed(2)
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.codepen-statistics-page {
  min-height: 100%;
}

.statistics-grid,
.average-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.average-item {
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: var(--cn-card-bg);
  box-shadow: var(--cn-card-shadow);
}

.average-label {
  display: block;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.average-value {
  display: block;
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
}

@media (max-width: 1180px) {
  .statistics-grid,
  .average-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .statistics-grid,
  .average-grid {
    grid-template-columns: 1fr;
  }
}
</style>
