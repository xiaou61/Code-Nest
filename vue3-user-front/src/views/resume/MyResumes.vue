<template>
  <CnPage class="resume-manage-page" max-width="1180px" full-height>
    <CnPageHeader
      title="在线简历工作台"
      description="集中管理你的简历版本，完成预览、导出、分享和访问统计。"
      eyebrow="RESUME DESK"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">共 {{ pagination.total }} 份简历</CnStatusTag>
        <CnStatusTag type="info" size="sm" subtle>最近更新 {{ lastUpdateTime || '--' }}</CnStatusTag>
        <CnStatusTag type="success" size="sm" subtle>最近导出 {{ lastExportTime || '--' }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Files" @click="goTemplate">浏览模板</el-button>
        <el-button type="primary" :icon="Plus" @click="goEditor">新建简历</el-button>
      </template>
    </CnPageHeader>

    <div class="summary-grid">
      <CnStatCard
        title="总简历数"
        :value="pagination.total"
        description="当前筛选条件下的总量"
        tone="brand"
        :loading="loading"
      />
      <CnStatCard
        title="本页展示"
        :value="resumeList.length"
        description="当前分页已加载条目"
        tone="info"
        :loading="loading"
      />
      <CnStatCard
        title="已发布"
        :value="publishedCount"
        description="本页已发布简历"
        tone="success"
        :loading="loading"
      />
      <CnStatCard
        title="公开可见"
        :value="publicCount"
        description="本页公开简历"
        tone="warning"
        :loading="loading"
      />
    </div>

    <CnSection title="筛选简历" description="按简历名称、发布状态和可见性快速定位目标版本。" divided>
      <CnFilterForm
        v-model="filters"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #keyword="{ value }">
          <el-input
            :model-value="String(value || '')"
            placeholder="输入关键词搜索简历"
            clearable
            :prefix-icon="Search"
            @update:model-value="(next) => updateFilter('keyword', next)"
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          />
        </template>

        <template #status="{ value }">
          <el-select
            :model-value="value"
            placeholder="状态"
            clearable
            @update:model-value="(next) => handleSelectFilterChange('status', next)"
          >
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
          </el-select>
        </template>

        <template #visibility="{ value }">
          <el-select
            :model-value="value"
            placeholder="可见性"
            clearable
            @update:model-value="(next) => handleSelectFilterChange('visibility', next)"
          >
            <el-option label="私密" :value="0" />
            <el-option label="公开" :value="1" />
          </el-select>
        </template>
      </CnFilterForm>
    </CnSection>

    <CnSection title="我的简历" description="预览内容、继续编辑，或将简历导出为可投递文件。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="resumeList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无简历"
        empty-description="点击右上角新建简历，或从模板中心快速开始。"
        empty-icon="CV"
        @page-change="handlePageChange"
      >
        <template #resumeName="{ row }">
          <div class="name-cell">
            <div class="name-line">
              <span class="name">{{ row.resumeName || '未命名简历' }}</span>
              <CnStatusTag type="neutral" size="sm" subtle>v{{ row.version || '-' }}</CnStatusTag>
            </div>
            <p class="summary">{{ row.summary || '暂无简介' }}</p>
          </div>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="statusTagType(row.status)" size="sm">
            {{ formatStatus(row.status) }}
          </CnStatusTag>
        </template>

        <template #visibility="{ row }">
          <CnStatusTag :type="visibilityTagType(row.visibility)" size="sm">
            {{ formatVisibility(row.visibility) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" :icon="View" @click="handlePreview(row)">预览</el-button>
            <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" :icon="Download" @click="openExportDialog(row)">导出</el-button>
            <el-button link type="primary" :icon="Share" @click="handleShare(row)">分享</el-button>
            <el-button link type="primary" :icon="DataAnalysis" @click="handleAnalytics(row)">统计</el-button>
            <el-popconfirm title="确认删除该简历？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无简历"
            description="你可以从空白简历开始，也可以先挑选模板。"
            icon="CV"
            surface="transparent"
            size="sm"
          />
        </template>
      </CnDataTable>
    </CnSection>

    <el-drawer v-model="previewVisible" size="42%" :title="previewData?.resume?.resumeName || '简历预览'">
      <div v-if="previewData" class="preview-panel">
        <p class="preview-summary">{{ previewData.resume?.summary || '暂无简介' }}</p>
        <el-timeline v-if="previewSections.length">
          <el-timeline-item
            v-for="section in previewSections"
            :key="section.id || section.title"
            :timestamp="section.sectionType || '简历模块'"
          >
            <h4>{{ section.title || '未命名模块' }}</h4>
            <pre>{{ section.content || '暂无内容' }}</pre>
          </el-timeline-item>
        </el-timeline>
        <CnEmptyState
          v-else
          title="暂无预览内容"
          description="这份简历还没有可展示的模块。"
          icon="PV"
          surface="transparent"
          size="sm"
        />
      </div>
    </el-drawer>

    <el-dialog v-model="exportDialogVisible" title="导出简历" width="420px">
      <el-form :model="exportForm" label-width="80px">
        <el-form-item label="格式">
          <el-select v-model="exportForm.format">
            <el-option label="PDF" value="PDF" />
            <el-option label="Word" value="WORD" />
            <el-option label="HTML" value="HTML" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="exportForm.theme" placeholder="用于标记导出主题，可选" />
        </el-form-item>
        <el-form-item label="水印">
          <el-switch v-model="exportForm.watermark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="exportLoading" @click="submitExport">立即导出</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shareDialogVisible" title="分享链接" width="420px">
      <el-descriptions v-if="shareData" :column="1" border>
        <el-descriptions-item label="访问链接">
          <el-input v-model="shareData.shareUrl" readonly />
        </el-descriptions-item>
        <el-descriptions-item label="分享口令">
          {{ shareData.shareCode || '无需口令' }}
        </el-descriptions-item>
        <el-descriptions-item label="到期时间">
          {{ shareData.expireTime || '--' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="shareDialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="analyticsDialogVisible" title="访问统计" width="480px">
      <div class="metric-grid">
        <div class="metric-card">
          <p class="metric-label">浏览次数</p>
          <p class="metric-value">{{ analyticsData?.viewCount ?? 0 }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">导出次数</p>
          <p class="metric-value">{{ analyticsData?.exportCount ?? 0 }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">分享次数</p>
          <p class="metric-value">{{ analyticsData?.shareCount ?? 0 }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">访客数</p>
          <p class="metric-value">{{ analyticsData?.uniqueVisitors ?? 0 }}</p>
        </div>
      </div>
      <p class="metric-footer">
        最近访问：{{ analyticsData?.lastAccessTime || '暂无记录' }}
      </p>
      <template #footer>
        <el-button type="primary" @click="analyticsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Delete, Download, EditPen, Files, Plus, Search, Share, View } from '@element-plus/icons-vue'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  type CnFilterField,
  type CnPagination,
  type CnTableColumn,
  type CnTone
} from '@/design-system'
import { resumeApi } from '@/api/resume'

interface ResumeItem extends Record<string, unknown> {
  id: number | string
  resumeName?: string
  version?: number | string
  summary?: string
  status?: number | string
  visibility?: number | string
  updateTime?: string
}

interface ResumeResponse {
  records?: ResumeItem[]
  total?: number
}

interface PreviewSection {
  id?: number | string
  title?: string
  sectionType?: string
  content?: string
}

interface PreviewData {
  resume?: ResumeItem
  sections?: PreviewSection[]
}

interface ExportForm {
  format: 'PDF' | 'WORD' | 'HTML'
  theme: string
  watermark: boolean
}

interface ExportResponse {
  downloadUrl?: string
}

interface ShareData {
  shareUrl?: string
  shareCode?: string
  expireTime?: string
}

interface AnalyticsData {
  viewCount?: number
  exportCount?: number
  shareCount?: number
  uniqueVisitors?: number
  lastAccessTime?: string
}

const router = useRouter()
const loading = ref(false)
const resumeList = ref<ResumeItem[]>([])
const lastExportTime = ref('')
const lastUpdateTime = ref('')

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const createFilters = () => ({
  keyword: '',
  status: '',
  visibility: ''
})

const filters = ref<Record<string, unknown>>(createFilters())

const filterFields: CnFilterField[] = [
  {
    prop: 'keyword',
    label: '关键字',
    type: 'custom',
    slot: 'keyword'
  },
  {
    prop: 'status',
    label: '状态',
    type: 'custom',
    slot: 'status'
  },
  {
    prop: 'visibility',
    label: '可见性',
    type: 'custom',
    slot: 'visibility'
  }
]

const tableColumns: CnTableColumn<ResumeItem>[] = [
  { prop: 'resumeName', label: '简历名称', minWidth: 220, slot: 'resumeName' },
  { prop: 'status', label: '状态', width: 120, align: 'center', slot: 'status' },
  { prop: 'visibility', label: '可见性', width: 120, align: 'center', slot: 'visibility' },
  { prop: 'updateTime', label: '更新时间', width: 190, showOverflowTooltip: true },
  { label: '操作', width: 350, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  layout: 'total, prev, pager, next',
  background: true
}))

const publishedCount = computed(() => resumeList.value.filter((item) => Number(item.status) === 1).length)
const publicCount = computed(() => resumeList.value.filter((item) => Number(item.visibility) === 1).length)

const previewVisible = ref(false)
const previewData = ref<PreviewData | null>(null)
const previewSections = computed(() => previewData.value?.sections || [])

const exportDialogVisible = ref(false)
const exportResumeId = ref<number | string | null>(null)
const exportLoading = ref(false)
const exportForm = reactive<ExportForm>({
  format: 'PDF',
  theme: '',
  watermark: true
})

const shareDialogVisible = ref(false)
const shareData = ref<ShareData | null>(null)

const analyticsDialogVisible = ref(false)
const analyticsData = ref<AnalyticsData | null>(null)

const normalizeOptionalFilter = (value: unknown) => {
  return value === '' || value === null || value === undefined ? undefined : value
}

const fetchResumes = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      keyword: (filters.value.keyword as string) || undefined,
      status: normalizeOptionalFilter(filters.value.status),
      visibility: normalizeOptionalFilter(filters.value.visibility)
    }
    const result = (await resumeApi.getMyResumes(params)) as ResumeResponse
    resumeList.value = result?.records || []
    pagination.total = result?.total || resumeList.value.length
    lastUpdateTime.value = resumeList.value[0]?.updateTime || ''
  } catch (error) {
    ElMessage.error('加载简历列表失败')
  } finally {
    loading.value = false
  }
}

const updateFilter = (prop: string, value: unknown) => {
  filters.value = {
    ...filters.value,
    [prop]: value ?? ''
  }
}

const handleSelectFilterChange = (prop: string, value: unknown) => {
  updateFilter(prop, value)
  handleSearch()
}

const handleSearch = () => {
  pagination.page = 1
  fetchResumes()
}

const handleReset = () => {
  filters.value = createFilters()
  pagination.page = 1
  fetchResumes()
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchResumes()
}

const goEditor = () => {
  router.push('/resume/editor')
}

const goTemplate = () => {
  router.push('/resume/templates')
}

const handleEdit = (row: ResumeItem) => {
  router.push(`/resume/editor/${row.id}`)
}

const handlePreview = async (row: ResumeItem) => {
  try {
    previewData.value = (await resumeApi.previewResume(row.id)) as PreviewData
    previewVisible.value = true
  } catch (error) {
    ElMessage.error('获取预览失败')
  }
}

const openExportDialog = (row: ResumeItem) => {
  exportResumeId.value = row.id
  exportForm.theme = row.resumeName || ''
  exportDialogVisible.value = true
}

const submitExport = async () => {
  if (!exportResumeId.value) return
  exportLoading.value = true
  try {
    const result = (await resumeApi.exportResume(exportResumeId.value, { ...exportForm })) as ExportResponse
    lastExportTime.value = new Date().toLocaleString()
    exportDialogVisible.value = false
    ElMessage.success('导出成功，正在为你打开文件')
    if (result?.downloadUrl) {
      window.open(result.downloadUrl, '_blank')
    }
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

const handleShare = async (row: ResumeItem) => {
  try {
    shareData.value = (await resumeApi.createShareLink(row.id)) as ShareData
    shareDialogVisible.value = true
  } catch (error) {
    ElMessage.error('创建分享链接失败')
  }
}

const handleAnalytics = async (row: ResumeItem) => {
  try {
    analyticsData.value = (await resumeApi.getAnalytics(row.id)) as AnalyticsData
    analyticsDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取统计信息失败')
  }
}

const handleDelete = async (row: ResumeItem) => {
  try {
    await resumeApi.deleteResume(row.id)
    ElMessage.success('删除成功')
    fetchResumes()
  } catch (error) {
    ElMessage.error('删除失败，请稍后重试')
  }
}

const statusTagType = (status: unknown): CnTone => {
  return Number(status) === 1 ? 'success' : 'info'
}

const visibilityTagType = (visibility: unknown): CnTone => {
  return Number(visibility) === 1 ? 'success' : 'warning'
}

const formatStatus = (status: unknown) => {
  return Number(status) === 1 ? '已发布' : '草稿'
}

const formatVisibility = (visibility: unknown) => {
  return Number(visibility) === 1 ? '公开' : '私密'
}

onMounted(() => {
  fetchResumes()
})
</script>

<style scoped>
.resume-manage-page {
  min-height: calc(100vh - 68px);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.name-cell {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.name {
  min-width: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-1);
}

.preview-panel {
  display: grid;
  gap: var(--cn-space-4);
}

.preview-summary {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.preview-panel h4 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 15px;
}

.preview-panel pre {
  min-width: 0;
  margin: 0;
  padding: var(--cn-space-3);
  overflow: auto;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-family: var(--cn-font-mono);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.metric-card {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  text-align: center;
}

.metric-label {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.metric-value {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 750;
  line-height: 1.1;
}

.metric-footer {
  margin: var(--cn-space-4) 0 0;
  color: var(--cn-color-text-secondary);
  text-align: center;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .summary-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .table-actions :deep(.el-button) {
    margin-left: 0;
  }
}
</style>
