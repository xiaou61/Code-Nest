<template>
  <CnPage class="operation-logs-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="操作日志"
      description="审计后台操作模块、请求链路、执行状态和耗时，辅助追责与系统治理。"
      eyebrow="Audit Operation"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">操作审计</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedRows.length" type="warning">
          已选择 {{ selectedRows.length }} 条
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" @click="getOperationLogs">刷新</el-button>
        <el-button type="danger" :disabled="selectedRows.length === 0" :icon="Delete" @click="handleBatchDelete">
          批量删除
        </el-button>
        <el-button type="warning" :icon="DeleteFilled" @click="showCleanDialog = true">清理日志</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按操作模块、类型、操作人和执行状态定位日志。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleQueryFormUpdate"
        @search="handleQuery"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="操作记录" :description="`共 ${pagination.total} 条操作审计记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="审计列表" description="批量删除和清理操作会保留当前筛选视图。" align="center">
            <template #meta>
              <CnStatusTag type="success" size="sm">成功 {{ successCount }} 条</CnStatusTag>
              <CnStatusTag type="danger" size="sm">失败 {{ failedCount }} 条</CnStatusTag>
            </template>

            <el-button :icon="Refresh" @click="getOperationLogs">刷新</el-button>
            <el-button type="danger" :disabled="selectedRows.length === 0" :icon="Delete" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button type="warning" :icon="DeleteFilled" @click="showCleanDialog = true">清理日志</el-button>
          </CnToolbar>
        </template>

        <template #operationType="{ row }">
          <CnStatusTag :type="getOperationTypeTone(row.operationType)" size="sm">
            {{ getOperationTypeText(row) }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 0 ? 'success' : 'danger'" size="sm">
            {{ getStatusText(row) }}
          </CnStatusTag>
        </template>

        <template #costTime="{ row }">
          {{ row.costTime ?? '-' }}ms
        </template>

        <template #operationTime="{ row }">
          {{ formatDateTime(row.operationTime) }}
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" size="small" :icon="View" @click="handleView(row)">详情</el-button>
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无操作日志"
            description="当前筛选条件下没有匹配记录，可以重置筛选或稍后刷新。"
            icon="OP"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
              <el-button type="primary" @click="getOperationLogs">刷新</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="showDetail" title="操作日志详情" width="800px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="日志ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="操作ID">{{ currentRow.operationId }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ currentRow.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <CnStatusTag :type="getOperationTypeTone(currentRow.operationType)" size="sm">
            {{ getOperationTypeText(currentRow) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentRow.description }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentRow.method }}</el-descriptions-item>
        <el-descriptions-item label="请求URI">{{ currentRow.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="HTTP方法">{{ currentRow.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentRow.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentRow.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="操作地点">{{ currentRow.operationLocation }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ currentRow.browser }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ currentRow.os }}</el-descriptions-item>
        <el-descriptions-item label="操作状态">
          <CnStatusTag :type="currentRow.status === 0 ? 'success' : 'danger'" size="sm">
            {{ getStatusText(currentRow) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentRow.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ formatDateTime(currentRow.operationTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2" v-if="currentRow.requestParams">
          <el-input :model-value="currentRow.requestParams" type="textarea" :rows="3" readonly />
        </el-descriptions-item>
        <el-descriptions-item label="响应数据" :span="2" v-if="currentRow.responseData">
          <el-input :model-value="currentRow.responseData" type="textarea" :rows="3" readonly />
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentRow.errorMsg">
          <el-input :model-value="currentRow.errorMsg" type="textarea" :rows="2" readonly />
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="showCleanDialog" title="清理操作日志" width="400px">
      <el-form :model="cleanForm" label-width="120px">
        <el-form-item label="清理类型:">
          <el-radio-group v-model="cleanForm.type">
            <el-radio label="all">清空所有日志</el-radio>
            <el-radio label="days">按天数清理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="保留天数:" v-if="cleanForm.type === 'days'">
          <div class="clean-days-field">
            <el-input-number v-model="cleanForm.days" :min="1" :max="365" placeholder="请输入保留天数" />
            <span>将删除 {{ cleanForm.days }} 天前的日志</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCleanDialog = false">取消</el-button>
        <el-button type="danger" @click="handleClean">确定清理</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, DeleteFilled, Refresh, View } from '@element-plus/icons-vue'
import { logApi } from '@/api/log'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type {
  CnBreadcrumbItem,
  CnFilterField,
  CnPagination,
  CnTableColumn,
  CnTone
} from '@/design-system'

interface OperationLogRecord {
  id: number
  operationId?: string
  module?: string
  operationType?: string
  operationTypeText?: string
  description?: string
  method?: string
  requestUri?: string
  requestMethod?: string
  operatorName?: string
  operatorIp?: string
  operationLocation?: string
  browser?: string
  os?: string
  status?: number
  statusText?: string
  costTime?: number
  operationTime?: string
  requestParams?: string
  responseData?: string
  errorMsg?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '日志管理' }, { label: '操作日志' }]

const loading = ref(false)
const tableData = ref<OperationLogRecord[]>([])
const selectedRows = ref<OperationLogRecord[]>([])
const showDetail = ref(false)
const showCleanDialog = ref(false)
const currentRow = ref<OperationLogRecord | null>(null)

const queryForm = reactive<{
  module: string
  operationType: string
  operatorName: string
  status: number | null
}>({
  module: '',
  operationType: '',
  operatorName: '',
  status: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const cleanForm = reactive({
  type: 'days',
  days: 30
})

const operationTypeOptions = [
  { label: '查询', value: 'SELECT' },
  { label: '新增', value: 'INSERT' },
  { label: '修改', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '授权', value: 'GRANT' },
  { label: '导出', value: 'EXPORT' },
  { label: '导入', value: 'IMPORT' },
  { label: '强退', value: 'FORCE' },
  { label: '生成代码', value: 'GENCODE' },
  { label: '清空数据', value: 'CLEAN' },
  { label: '其它', value: 'OTHER' }
]

const filterFields: CnFilterField[] = [
  { prop: 'module', label: '操作模块', type: 'input', placeholder: '请输入操作模块' },
  { prop: 'operationType', label: '操作类型', type: 'select', placeholder: '请选择操作类型', options: operationTypeOptions },
  { prop: 'operatorName', label: '操作人', type: 'input', placeholder: '请输入操作人' },
  {
    prop: 'status',
    label: '操作状态',
    type: 'select',
    placeholder: '请选择操作状态',
    options: [
      { label: '成功', value: 0 },
      { label: '失败', value: 1 }
    ]
  }
]

const tableColumns: CnTableColumn<OperationLogRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'module', label: '操作模块', minWidth: 120, showOverflowTooltip: true },
  { prop: 'operationType', label: '操作类型', width: 110, slot: 'operationType' },
  { prop: 'description', label: '操作描述', minWidth: 160, showOverflowTooltip: true },
  { prop: 'operatorName', label: '操作人', minWidth: 120 },
  { prop: 'operatorIp', label: '操作 IP', minWidth: 140 },
  { prop: 'status', label: '操作状态', width: 110, slot: 'status' },
  { prop: 'costTime', label: '耗时', width: 100, slot: 'costTime' },
  { prop: 'operationTime', label: '操作时间', width: 180, slot: 'operationTime' },
  { label: '操作', width: 160, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const successCount = computed(() => tableData.value.filter((item) => item.status === 0).length)
const failedCount = computed(() => tableData.value.filter((item) => item.status === 1).length)

onMounted(() => {
  getOperationLogs()
})

const getOperationLogs = async () => {
  try {
    loading.value = true

    const params = {
      ...queryForm,
      pageNum: pagination.page,
      pageSize: pagination.size
    }

    const result = await logApi.getOperationLogs(params)

    tableData.value = result.records || []
    pagination.total = result.total || 0
  } catch (error) {
    console.error('获取操作日志失败:', error)
    ElMessage.error('获取操作日志失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pagination.page = 1
  getOperationLogs()
}

const handleReset = () => {
  Object.assign(queryForm, {
    module: '',
    operationType: '',
    operatorName: '',
    status: null
  })
  pagination.page = 1
  getOperationLogs()
}

const handleQueryFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handleView = async (row: OperationLogRecord) => {
  try {
    const result = await logApi.getOperationLogById(row.id)
    currentRow.value = result
    showDetail.value = true
  } catch (error) {
    console.error('获取操作日志详情失败:', error)
    ElMessage.error('获取操作日志详情失败')
  }
}

const handleDelete = async (row: OperationLogRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除这条操作日志吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await logApi.deleteOperationLogs([row.id])

    ElMessage.success('删除成功')
    getOperationLogs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除操作日志失败:', error)
      ElMessage.error('删除操作日志失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条操作日志吗？`, '确认批量删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedRows.value.map((row) => row.id)
    await logApi.deleteOperationLogs(ids)

    ElMessage.success('批量删除成功')
    getOperationLogs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除操作日志失败:', error)
      ElMessage.error('批量删除操作日志失败')
    }
  }
}

const handleClean = async () => {
  try {
    const confirmText =
      cleanForm.type === 'all'
        ? '确定要清空所有操作日志吗？此操作不可恢复！'
        : `确定要清理 ${cleanForm.days} 天前的操作日志吗？此操作不可恢复！`

    await ElMessageBox.confirm(confirmText, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    if (cleanForm.type === 'all') {
      await logApi.clearOperationLogs()
    } else {
      await logApi.cleanOperationLogs(cleanForm.days)
    }

    ElMessage.success('清理成功')
    showCleanDialog.value = false
    getOperationLogs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清理操作日志失败:', error)
      ElMessage.error('清理操作日志失败')
    }
  }
}

const handleSelectionChange = (selection: OperationLogRecord[]) => {
  selectedRows.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  getOperationLogs()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  getOperationLogs()
}

const getOperationTypeTone = (operationType?: string): CnTone => {
  const typeMap: Record<string, CnTone> = {
    SELECT: 'neutral',
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    GRANT: 'info',
    EXPORT: 'brand',
    IMPORT: 'brand',
    FORCE: 'danger',
    GENCODE: 'success',
    CLEAN: 'danger',
    OTHER: 'neutral'
  }
  return typeMap[operationType || ''] || 'neutral'
}

const getOperationTypeText = (row: OperationLogRecord) => {
  if (row.operationTypeText) return row.operationTypeText
  return operationTypeOptions.find((item) => item.value === row.operationType)?.label || row.operationType || '-'
}

const getStatusText = (row: OperationLogRecord) => {
  if (row.statusText) return row.statusText
  return row.status === 0 ? '成功' : '失败'
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString()
}
</script>

<style scoped>
.operation-logs-page {
  min-height: 100%;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.clean-days-field {
  display: grid;
  gap: var(--cn-space-2);
}

.clean-days-field span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
