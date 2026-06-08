<template>
  <CnPage class="resume-analytics-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="简历数据总览"
      description="实时掌握模板、简历、发布、浏览、导出和分享表现。"
      eyebrow="Resume Analytics"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">简历中心</CnStatusTag>
        <CnStatusTag type="neutral">简历 {{ summary?.resumeCount ?? 0 }}</CnStatusTag>
        <CnStatusTag type="success">发布率 {{ publishedRate }}%</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchAnalytics">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="resume-stat-grid" v-loading="loading">
      <CnStatCard title="模板总数" :value="summary?.templateCount ?? 0" description="当前可用于创建简历的模板数量" tone="brand" />
      <CnStatCard title="简历总数" :value="summary?.resumeCount ?? 0" description="平台累计创建简历数量" tone="info" />
      <CnStatCard title="发布简历" :value="summary?.publishedResumeCount ?? 0" :description="`发布率 ${publishedRate}%`" tone="success" />
      <CnStatCard title="草稿" :value="summary?.draftResumeCount ?? 0" description="尚未公开发布的简历数量" tone="warning" />
    </div>

    <div class="analytics-grid">
      <CnSection title="访问行为" description="简历浏览、导出和分享次数概览。" divided>
        <div class="stat-list">
          <div class="stat-item">
            <div>
              <p class="stat-label">累计浏览</p>
              <p class="stat-value">{{ summary?.totalViews ?? 0 }}</p>
            </div>
            <CnStatusTag type="success">+{{ summary?.totalShares ?? 0 }} 分享带来</CnStatusTag>
          </div>
          <div class="stat-item">
            <div>
              <p class="stat-label">导出次数</p>
              <p class="stat-value">{{ summary?.totalExports ?? 0 }}</p>
            </div>
            <el-progress class="stat-progress" :percentage="exportRate" :stroke-width="6" status="success" />
          </div>
          <div class="stat-item">
            <div>
              <p class="stat-label">分享次数</p>
              <p class="stat-value">{{ summary?.totalShares ?? 0 }}</p>
            </div>
            <el-progress class="stat-progress" :percentage="shareRate" :stroke-width="6" status="warning" />
          </div>
        </div>
      </CnSection>

      <CnSection title="优化建议" description="来自健康巡检的前 3 条异常提醒。" divided>
        <CnEmptyState v-if="!healthTips.length" title="暂无巡检数据" description="查看健康巡检页获取详情。" size="sm" surface="transparent" />
        <ul v-else class="tips-list">
          <li v-for="tip in healthTips" :key="tip">
            <el-icon><Warning /></el-icon>
            <span>{{ tip }}</span>
          </li>
        </ul>
        <el-button type="primary" link @click="goReports">前往健康巡检</el-button>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Warning } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface ResumeSummary {
  templateCount?: number
  resumeCount?: number
  publishedResumeCount?: number
  draftResumeCount?: number
  totalViews?: number
  totalExports?: number
  totalShares?: number
  [key: string]: unknown
}

interface HealthReport {
  resumeName?: string
  issue?: string
  [key: string]: unknown
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '简历中心' }, { label: '数据总览' }]

const loading = ref(false)
const summary = ref<ResumeSummary | null>(null)
const healthTips = ref<string[]>([])

const fetchAnalytics = async () => {
  loading.value = true
  try {
    summary.value = await resumeApi.getAnalytics()
  } catch {
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const loadHealthTips = async () => {
  try {
    const reports: HealthReport[] = await resumeApi.getHealthReports()
    healthTips.value = (reports || []).slice(0, 3).map((item) => `${item.resumeName}: ${item.issue}`)
  } catch {
    healthTips.value = []
  }
}

const publishedRate = computed(() => {
  if (!summary.value?.resumeCount) return 0
  return Math.round(((summary.value.publishedResumeCount || 0) / summary.value.resumeCount) * 100)
})

const exportRate = computed(() => {
  if (!summary.value?.totalViews) return 0
  return Math.min(100, Math.round(((summary.value.totalExports || 0) / summary.value.totalViews) * 100))
})

const shareRate = computed(() => {
  if (!summary.value?.resumeCount) return 0
  return Math.min(100, Math.round(((summary.value.totalShares || 0) / summary.value.resumeCount) * 100))
})

const goReports = () => {
  router.push('/resume/reports')
}

onMounted(() => {
  fetchAnalytics()
  loadHealthTips()
})
</script>

<style scoped>
.resume-analytics-page {
  min-height: 100%;
}

.resume-stat-grid,
.analytics-grid {
  display: grid;
  gap: var(--cn-space-4);
}

.resume-stat-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.analytics-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-list {
  display: grid;
  gap: var(--cn-space-5);
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-4);
}

.stat-label {
  margin: 0;
  color: var(--cn-color-text-secondary);
}

.stat-value {
  margin: var(--cn-space-1) 0 0;
  color: var(--cn-color-text-primary);
  font-size: 26px;
  font-weight: 700;
}

.stat-progress {
  width: 180px;
}

.tips-list {
  display: grid;
  gap: var(--cn-space-3);
  padding: 0;
  margin: 0 0 var(--cn-space-4);
  list-style: none;
}

.tips-list li {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  padding-bottom: var(--cn-space-3);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-warning);
}

@media (max-width: 1180px) {
  .resume-stat-grid,
  .analytics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .resume-stat-grid,
  .analytics-grid {
    grid-template-columns: 1fr;
  }

  .stat-item {
    display: grid;
  }

  .stat-progress {
    width: 100%;
  }
}
</style>
