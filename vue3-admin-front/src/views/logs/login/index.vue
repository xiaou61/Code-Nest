<template>
  <CnPage class="login-logs-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="登录日志"
      description="追踪管理员登录来源、终端环境和登录结果，支撑安全审计与异常排查。"
      eyebrow="Audit Login"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">安全审计</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedRows.length" type="warning">
          已选择 {{ selectedRows.length }} 条
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" @click="getLoginLogs">刷新</el-button>
        <el-button type="danger" :icon="Delete" @click="handleClear">清空日志</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按用户名、登录 IP 和登录状态定位日志记录。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleQueryFormUpdate"
        @search="handleQuery"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="登录记录" :description="`共 ${pagination.total} 条登录审计记录`" divided>
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
          <CnToolbar title="审计列表" description="保留查询条件后执行查看、刷新或清空操作。" align="center">
            <template #meta>
              <CnStatusTag type="success" size="sm">成功 {{ successCount }} 条</CnStatusTag>
              <CnStatusTag type="danger" size="sm">失败 {{ failedCount }} 条</CnStatusTag>
            </template>

            <el-button :icon="Refresh" @click="getLoginLogs">刷新</el-button>
            <el-button type="danger" :icon="Delete" @click="handleClear">清空日志</el-button>
          </CnToolbar>
        </template>

        <template #loginStatus="{ row }">
          <CnStatusTag :type="row.loginStatus === 0 ? 'success' : 'danger'" size="sm">
            {{ getLoginStatusText(row) }}
          </CnStatusTag>
        </template>

        <template #loginTime="{ row }">
          {{ formatDateTime(row.loginTime) }}
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" size="small" :icon="View" @click="handleView(row)">详情</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无登录日志"
            description="当前筛选条件下没有匹配记录，可以重置筛选或稍后刷新。"
            icon="LG"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
              <el-button type="primary" @click="getLoginLogs">刷新</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="showDetail" title="登录日志详情" width="600px">
      <el-descriptions :column="2" border v-if="currentRow">
        <el-descriptions-item label="日志ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="管理员ID">{{ currentRow.adminId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentRow.username }}</el-descriptions-item>
        <el-descriptions-item label="登录IP">{{ currentRow.loginIp }}</el-descriptions-item>
        <el-descriptions-item label="登录地点">{{ currentRow.loginLocation }}</el-descriptions-item>
        <el-descriptions-item label="浏览器">{{ currentRow.browser }}</el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ currentRow.os }}</el-descriptions-item>
        <el-descriptions-item label="登录状态">
          <CnStatusTag :type="currentRow.loginStatus === 0 ? 'success' : 'danger'" size="sm">
            {{ getLoginStatusText(currentRow) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="登录消息" :span="2">{{ currentRow.loginMessage }}</el-descriptions-item>
        <el-descriptions-item label="登录时间" :span="2">{{ formatDateTime(currentRow.loginTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh, View } from '@element-plus/icons-vue'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn } from '@/design-system'

interface LoginLogRecord {
  id: number
  adminId?: number
  username?: string
  loginIp?: string
  loginLocation?: string
  browser?: string
  os?: string
  loginStatus?: number
  loginStatusText?: string
  loginMessage?: string
  loginTime?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '日志管理' }, { label: '登录日志' }]

const loading = ref(false)
const tableData = ref<LoginLogRecord[]>([])
const selectedRows = ref<LoginLogRecord[]>([])
const showDetail = ref(false)
const currentRow = ref<LoginLogRecord | null>(null)

const queryForm = reactive<{
  username: string
  loginIp: string
  loginStatus: number | null
}>({
  username: '',
  loginIp: '',
  loginStatus: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const filterFields: CnFilterField[] = [
  { prop: 'username', label: '用户名', type: 'input', placeholder: '请输入用户名' },
  { prop: 'loginIp', label: 'IP 地址', type: 'input', placeholder: '请输入 IP 地址' },
  {
    prop: 'loginStatus',
    label: '登录状态',
    type: 'select',
    placeholder: '请选择登录状态',
    options: [
      { label: '成功', value: 0 },
      { label: '失败', value: 1 }
    ]
  }
]

const tableColumns: CnTableColumn<LoginLogRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'username', label: '用户名', minWidth: 120 },
  { prop: 'loginIp', label: '登录 IP', minWidth: 140 },
  { prop: 'loginLocation', label: '登录地点', minWidth: 120, showOverflowTooltip: true },
  { prop: 'browser', label: '浏览器', minWidth: 120, showOverflowTooltip: true },
  { prop: 'os', label: '操作系统', minWidth: 120, showOverflowTooltip: true },
  { prop: 'loginStatus', label: '登录状态', width: 110, slot: 'loginStatus' },
  { prop: 'loginMessage', label: '登录消息', minWidth: 150, showOverflowTooltip: true },
  { prop: 'loginTime', label: '登录时间', width: 180, slot: 'loginTime' },
  { label: '操作', width: 100, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const successCount = computed(() => tableData.value.filter((item) => item.loginStatus === 0).length)
const failedCount = computed(() => tableData.value.filter((item) => item.loginStatus === 1).length)

onMounted(() => {
  getLoginLogs()
})

const getLoginLogs = async () => {
  try {
    loading.value = true

    const params = {
      ...queryForm,
      pageNum: pagination.page,
      pageSize: pagination.size
    }

    const result = await logApi.getLoginLogs(params)

    tableData.value = result.records || []
    pagination.total = result.total || 0
  } catch (error) {
    console.error('获取登录日志失败:', error)
    ElMessage.error('获取登录日志失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  pagination.page = 1
  getLoginLogs()
}

const handleReset = () => {
  Object.assign(queryForm, {
    username: '',
    loginIp: '',
    loginStatus: null
  })
  pagination.page = 1
  getLoginLogs()
}

const handleQueryFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('确定要清空所有登录日志吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await logApi.clearLoginLogs()

    ElMessage.success('登录日志已清空')
    getLoginLogs()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空登录日志失败:', error)
      ElMessage.error('清空登录日志失败')
    }
  }
}

const handleView = async (row: LoginLogRecord) => {
  try {
    const result = await logApi.getLoginLogById(row.id)
    currentRow.value = result
    showDetail.value = true
  } catch (error) {
    console.error('获取登录日志详情失败:', error)
    ElMessage.error('获取登录日志详情失败')
  }
}

const handleSelectionChange = (selection: LoginLogRecord[]) => {
  selectedRows.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  getLoginLogs()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  getLoginLogs()
}

const getLoginStatusText = (row: LoginLogRecord) => {
  if (row.loginStatusText) return row.loginStatusText
  return row.loginStatus === 0 ? '成功' : '失败'
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString()
}
</script>

<style scoped>
.login-logs-page {
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
</style>
