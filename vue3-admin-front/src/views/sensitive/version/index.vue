<template>
  <CnPage class="sensitive-version-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="版本历史"
      description="查看敏感词词库变更历史、当前版本和回滚入口，便于追踪导入、更新和删除操作影响。"
      eyebrow="Sensitive Versions"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="success">当前 {{ latestVersion }}</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 个版本</CnStatusTag>
        <CnStatusTag type="warning">导入 {{ importCountInPage }} 次</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadVersions">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="当前版本" :value="latestVersion" description="后端返回的最新词库版本号" tone="success" />
      <CnStatCard title="历史版本" :value="total" description="当前筛选条件下的版本记录数量" tone="brand" />
      <CnStatCard title="变更词数" :value="changeCountInPage" description="当前页累计变更词汇数量" tone="info" />
      <CnStatCard title="导入记录" :value="importCountInPage" description="当前页导入类型变更次数" tone="warning" />
    </div>

    <CnSection title="版本历史列表" :description="`共 ${total} 条版本记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="词库版本" description="回滚会恢复到指定版本词库状态，请确认变更详情后操作。" align="center">
            <template #meta>
              <CnStatusTag type="success" size="sm">{{ latestVersion }}</CnStatusTag>
              <CnStatusTag type="neutral" size="sm">每页 {{ queryForm.pageSize }} 条</CnStatusTag>
            </template>
          </CnToolbar>
        </template>

        <template #versionNo="{ row }">
          <div class="version-cell">
            <strong>{{ row.versionNo || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #changeType="{ row }">
          <CnStatusTag :type="getChangeTypeTone(row.changeType)" size="sm">
            {{ getChangeTypeText(row.changeType) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="View" @click="handleView(row)">详情</el-button>
            <el-button type="warning" link size="small" :icon="RefreshLeft" @click="handleRollback(row)">回滚</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无版本历史"
            description="当前没有可展示的词库版本变更记录。"
            icon="VH"
            surface="transparent"
          />
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="detailDialogVisible" title="版本详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="版本号">
          {{ versionDetail.versionNo }}
        </el-descriptions-item>
        <el-descriptions-item label="变更类型">
          <CnStatusTag :type="getChangeTypeTone(versionDetail.changeType)" size="sm">
            {{ getChangeTypeText(versionDetail.changeType) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="变更数量">
          {{ versionDetail.changeCount || 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ versionDetail.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="变更详情" :span="2">
          <div class="change-detail">{{ formatChangeDetail(versionDetail.changeDetail) }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ versionDetail.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, RefreshLeft, View } from '@element-plus/icons-vue'
import { getLatestVersion, getVersionById, listVersions, rollbackVersion } from '@/api/sensitive'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

type ChangeType = 'add' | 'update' | 'delete' | 'import' | string

interface VersionRecord {
  id: number
  versionNo: string
  changeType: ChangeType
  changeCount?: number
  changeDetail?: unknown
  remark?: string
  createTime?: string
  [key: string]: unknown
}

interface VersionQuery {
  pageNum: number
  pageSize: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '版本历史' }]

const loading = ref(false)
const detailDialogVisible = ref(false)
const tableData = ref<VersionRecord[]>([])
const total = ref(0)
const latestVersion = ref('v1.0.0')
const versionDetail = ref<Partial<VersionRecord>>({})

const queryForm = reactive<VersionQuery>({
  pageNum: 1,
  pageSize: 10
})

const tableColumns: CnTableColumn<VersionRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'versionNo', label: '版本号', minWidth: 150, slot: 'versionNo', showOverflowTooltip: true },
  { prop: 'changeType', label: '变更类型', width: 110, slot: 'changeType' },
  { prop: 'changeCount', label: '变更数量', width: 110, align: 'right' },
  { prop: 'changeDetail', label: '变更详情', minWidth: 220, formatter: (row) => formatChangeDetail(row.changeDetail), showOverflowTooltip: true },
  { prop: 'remark', label: '备注', minWidth: 140, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', width: 180 },
  { label: '操作', width: 130, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const changeCountInPage = computed(() => tableData.value.reduce((sum, item) => sum + (Number(item.changeCount) || 0), 0))
const importCountInPage = computed(() => tableData.value.filter((item) => item.changeType === 'import').length)

onMounted(() => {
  loadVersions()
  loadLatestVersion()
})

const loadVersions = async () => {
  loading.value = true
  try {
    const response = await listVersions({ ...queryForm })
    tableData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('查询版本历史失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const loadLatestVersion = async () => {
  try {
    const version = await getLatestVersion()
    latestVersion.value = version || 'v1.0.0'
  } catch (error) {
    console.error('获取最新版本失败:', error)
  }
}

const handlePageChange = (page: number) => {
  queryForm.pageNum = page
  loadVersions()
}

const handlePageSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  loadVersions()
}

const handleView = async (row: VersionRecord) => {
  try {
    versionDetail.value = await getVersionById(row.id)
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取版本详情失败:', error)
    ElMessage.error('获取版本详情失败')
  }
}

const handleRollback = async (row: VersionRecord) => {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到版本 ${row.versionNo} 吗？此操作将恢复到该版本的词库状态。`,
      '回滚确认',
      {
        type: 'warning',
        confirmButtonText: '确定回滚',
        cancelButtonText: '取消'
      }
    )

    await rollbackVersion(row.id, { operatorId: 1 })
    ElMessage.success('回滚成功')
    loadVersions()
    loadLatestVersion()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('回滚版本失败:', error)
      ElMessage.error('回滚失败')
    }
  }
}

const getChangeTypeText = (type: ChangeType | undefined) => {
  const typeMap: Record<string, string> = {
    add: '新增',
    update: '更新',
    delete: '删除',
    import: '导入'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

const getChangeTypeTone = (type: ChangeType | undefined): CnTone => {
  const toneMap: Record<string, CnTone> = {
    add: 'success',
    update: 'warning',
    delete: 'danger',
    import: 'info'
  }
  return type ? toneMap[type] || 'info' : 'info'
}

const formatChangeDetail = (detail: unknown) => {
  if (detail === null || detail === undefined || detail === '') {
    return '-'
  }

  if (typeof detail === 'string') {
    return detail
  }

  try {
    return JSON.stringify(detail, null, 2)
  } catch (error) {
    return String(detail)
  }
}
</script>

<style scoped>
.sensitive-version-page {
  min-height: 100%;
}

.sensitive-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.version-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.version-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.change-detail {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .sensitive-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .sensitive-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
