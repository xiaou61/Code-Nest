<template>
  <CnPage class="resume-report-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="健康巡检"
      description="聚焦异常简历，提前发现缺失、格式和发布数据问题。"
      eyebrow="Resume Health"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">简历中心</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ reports.length }} 条</CnStatusTag>
        <CnStatusTag type="danger">高危 {{ highCount }} 条</CnStatusTag>
        <CnStatusTag type="warning">中危 {{ mediumCount }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchReports">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="resume-stat-grid">
      <CnStatCard title="巡检问题" :value="reports.length" description="当前返回的简历健康问题总量" tone="brand" />
      <CnStatCard title="高危问题" :value="highCount" description="需要优先处理的严重问题" tone="danger" />
      <CnStatCard title="中危问题" :value="mediumCount" description="建议尽快修复的问题" tone="warning" />
      <CnStatCard title="低危问题" :value="lowCount" description="可排期优化的问题" tone="info" />
    </div>

    <CnSection title="巡检列表" :description="`共 ${reports.length} 条巡检结果`" divided>
      <CnDataTable :columns="tableColumns" :data="reports" :loading="loading" :pagination="null" row-key="resumeId">
        <template #severity="{ row }">
          <CnStatusTag :type="severityTone(row.severity)" size="sm">{{ severityLabel(row.severity) }}</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <el-button type="primary" link size="small" @click="gotoResume(row.resumeId)">查看简历</el-button>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { resumeApi } from '@/api/resume'
import {
  CnDataTable,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface HealthReport {
  resumeId: number | string
  resumeName?: string
  issue?: string
  severity?: number
  detectedAt?: string
  [key: string]: unknown
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '简历中心' }, { label: '健康巡检' }]

const loading = ref(false)
const reports = ref<HealthReport[]>([])

const tableColumns: CnTableColumn<HealthReport>[] = [
  { prop: 'resumeId', label: '简历ID', width: 100 },
  { prop: 'resumeName', label: '简历名称', minWidth: 200, showOverflowTooltip: true },
  { prop: 'issue', label: '问题描述', minWidth: 260, showOverflowTooltip: true },
  { prop: 'severity', label: '严重级别', width: 140, align: 'center', slot: 'severity' },
  { prop: 'detectedAt', label: '检测时间', width: 180, showOverflowTooltip: true },
  { label: '快捷操作', width: 120, fixed: 'right', slot: 'actions' }
]

const highCount = computed(() => reports.value.filter((item) => item.severity === 2).length)
const mediumCount = computed(() => reports.value.filter((item) => item.severity === 1).length)
const lowCount = computed(() => reports.value.filter((item) => !item.severity).length)

const fetchReports = async () => {
  loading.value = true
  try {
    reports.value = (await resumeApi.getHealthReports()) || []
  } catch {
    ElMessage.error('获取巡检数据失败')
  } finally {
    loading.value = false
  }
}

const severityLabel = (level?: number) => ({ 2: '高', 1: '中', 0: '低' })[Number(level)] || '低'
const severityTone = (level?: number): CnTone => ({ 2: 'danger', 1: 'warning', 0: 'info' })[Number(level)] as CnTone || 'info'

const gotoResume = (resumeId: number | string) => {
  router.push({
    path: '/resume',
    query: { focusId: resumeId }
  })
}

onMounted(() => {
  fetchReports()
})
</script>

<style scoped>
.resume-report-page {
  min-height: 100%;
}

.resume-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

@media (max-width: 1180px) {
  .resume-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .resume-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
