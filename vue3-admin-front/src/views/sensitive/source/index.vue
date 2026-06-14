<template>
  <CnPage class="sensitive-source-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="词库来源"
      description="管理本地词库、第三方 API 和 GitHub 词库来源，统一跟踪同步状态和词汇数量。"
      eyebrow="Sensitive Sources"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 个来源</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="warning">远程 {{ remoteCountInPage }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadSources">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增词库来源</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="来源总量" :value="total" description="当前筛选条件下的词库来源数量" tone="brand" />
      <CnStatCard title="启用来源" :value="enabledCountInPage" description="当前页处于启用状态的来源" tone="success" />
      <CnStatCard title="远程来源" :value="remoteCountInPage" description="当前页 API 和 GitHub 来源数量" tone="warning" />
      <CnStatCard title="词汇总量" :value="wordCountInPage" description="当前页来源累计词汇数量" tone="info" />
    </div>

    <CnSection title="词库来源列表" :description="`共 ${total} 个词库来源`" divided>
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
          <CnToolbar title="来源数据" description="同步和测试操作会调用外部来源，请避免在高峰期频繁触发。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ queryForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">词汇 {{ wordCountInPage }} 条</CnStatusTag>
            </template>

            <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
          </CnToolbar>
        </template>

        <template #sourceName="{ row }">
          <div class="source-cell">
            <strong>{{ row.sourceName || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #sourceType="{ row }">
          <CnStatusTag :type="getSourceTypeTone(row.sourceType)" size="sm">
            {{ getSourceTypeText(row.sourceType) }}
          </CnStatusTag>
        </template>

        <template #syncInterval="{ row }">
          <span class="muted-text">{{ row.syncInterval || 0 }} 小时</span>
        </template>

        <template #syncStatus="{ row }">
          <CnStatusTag :type="row.syncStatus === 1 ? 'success' : 'danger'" size="sm">
            {{ row.syncStatus === 1 ? '成功' : '失败' }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" :icon="Refresh" @click="handleSync(row)">同步</el-button>
            <el-button type="info" link size="small" :icon="Connection" @click="handleTestConnection(row)">测试</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无词库来源"
            description="当前没有可展示的词库来源，可以新增本地、API 或 GitHub 来源。"
            icon="SC"
            surface="transparent"
          >
            <template #actions>
              <el-button type="primary" @click="handleAdd">新增词库来源</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="来源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="请输入来源名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择类型" class="full-width-control">
            <el-option label="本地词库" value="local" />
            <el-option label="API接口" value="api" />
            <el-option label="GitHub" value="github" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.sourceType !== 'local'"
          :label="form.sourceType === 'github' ? 'GitHub地址' : 'API地址'"
          prop="apiUrl"
        >
          <el-input
            v-model="form.apiUrl"
            :placeholder="form.sourceType === 'github' ? '请输入GitHub文件地址（支持blob/raw/api）' : '请输入API地址'"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item
          v-if="form.sourceType !== 'local'"
          :label="form.sourceType === 'github' ? '访问令牌' : 'API密钥'"
          prop="apiKey"
        >
          <el-input
            v-model="form.apiKey"
            type="password"
            :placeholder="form.sourceType === 'github' ? '可选，私有仓库请填写Token' : '可选，按需填写API密钥'"
            maxlength="255"
            show-password
          />
        </el-form-item>
        <el-form-item label="同步间隔(小时)" prop="syncInterval">
          <el-input-number v-model="form.syncInterval" :min="1" :max="720" class="full-width-control" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { addSource, deleteSource, listSources, syncSource, testSourceConnection, updateSource } from '@/api/sensitive'
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

type SourceType = 'local' | 'api' | 'github' | string

interface SourceRecord {
  id: number
  sourceName: string
  sourceType: SourceType
  apiUrl?: string
  apiKey?: string
  wordCount?: number
  syncInterval?: number
  syncStatus?: number
  lastSyncTime?: string
  status: number
  [key: string]: unknown
}

interface SourceQuery {
  pageNum: number
  pageSize: number
}

interface SourceForm {
  id: number | null
  sourceName: string
  sourceType: SourceType
  apiUrl: string
  apiKey: string
  syncInterval: number
  status: number
}

interface SyncResult {
  message?: string
  addedCount?: number
  updatedCount?: number
  failedCount?: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '词库来源' }]

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const tableData = ref<SourceRecord[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

const queryForm = reactive<SourceQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = reactive<SourceForm>({
  id: null,
  sourceName: '',
  sourceType: 'local',
  apiUrl: '',
  apiKey: '',
  syncInterval: 24,
  status: 1
})

const formRules: FormRules<SourceForm> = {
  sourceName: [{ required: true, message: '请输入来源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  apiUrl: [
    {
      validator: (_rule, value, callback) => {
        if (form.sourceType !== 'local' && !value) {
          callback(new Error(form.sourceType === 'github' ? '请输入GitHub地址' : '请输入API地址'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  syncInterval: [{ required: true, message: '请输入同步间隔', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const tableColumns: CnTableColumn<SourceRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'sourceName', label: '来源名称', minWidth: 150, slot: 'sourceName', showOverflowTooltip: true },
  { prop: 'sourceType', label: '来源类型', width: 110, slot: 'sourceType' },
  { prop: 'wordCount', label: '词汇数量', width: 100, align: 'right' },
  { prop: 'syncInterval', label: '同步间隔', width: 110, slot: 'syncInterval' },
  { prop: 'syncStatus', label: '同步状态', width: 100, slot: 'syncStatus' },
  { prop: 'lastSyncTime', label: '最后同步时间', width: 180, showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { label: '操作', width: 210, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const enabledCountInPage = computed(() => tableData.value.filter((item) => item.status === 1).length)
const remoteCountInPage = computed(() => tableData.value.filter((item) => item.sourceType !== 'local').length)
const wordCountInPage = computed(() => tableData.value.reduce((sum, item) => sum + (Number(item.wordCount) || 0), 0))

onMounted(() => {
  loadSources()
})

const loadSources = async () => {
  loading.value = true
  try {
    const response = await listSources({ ...queryForm })
    tableData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('查询词库来源失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  queryForm.pageNum = page
  loadSources()
}

const handlePageSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  loadSources()
}

const handleAdd = () => {
  dialogTitle.value = '新增词库来源'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: SourceRecord) => {
  dialogTitle.value = '编辑词库来源'
  Object.assign(form, {
    id: row.id,
    sourceName: row.sourceName || '',
    sourceType: row.sourceType || 'local',
    apiUrl: row.apiUrl || '',
    apiKey: row.apiKey || '',
    syncInterval: Number(row.syncInterval) || 24,
    status: Number(row.status) === 0 ? 0 : 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    const payload = {
      ...form,
      apiUrl: form.sourceType === 'local' ? '' : form.apiUrl,
      apiKey: form.sourceType === 'local' ? '' : form.apiKey
    }

    if (form.id) {
      await updateSource(payload)
      ElMessage.success('更新成功')
    } else {
      await addSource(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadSources()
  } catch (error) {
    console.error(form.id ? '更新词库来源失败:' : '新增词库来源失败:', error)
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (row: SourceRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除词库来源 "${row.sourceName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSource(row.id)
    ElMessage.success('删除成功')
    loadSources()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除词库来源失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleTestConnection = async (row: SourceRecord) => {
  try {
    const message = await testSourceConnection(row.id)
    ElMessage.success(String(message || '连接测试成功'))
  } catch (error) {
    console.error('连接测试失败:', error)
    ElMessage.error('连接测试失败')
  }
}

const handleSync = async (row: SourceRecord) => {
  const syncMessage = ElMessage({
    message: '正在同步词库，请稍候...',
    type: 'info',
    duration: 0
  })

  try {
    const result: SyncResult = await syncSource(row.id)
    syncMessage.close()
    ElMessage.success(
      result?.message || `同步完成：新增 ${result?.addedCount || 0}，更新 ${result?.updatedCount || 0}，失败 ${result?.failedCount || 0}`
    )
    loadSources()
  } catch (error) {
    syncMessage.close()
    console.error('同步词库来源失败:', error)
    ElMessage.error('同步失败')
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    sourceName: '',
    sourceType: 'local',
    apiUrl: '',
    apiKey: '',
    syncInterval: 24,
    status: 1
  })
  formRef.value?.resetFields()
}

const getSourceTypeText = (type: SourceType) => {
  const typeMap: Record<string, string> = {
    local: '本地词库',
    api: 'API接口',
    github: 'GitHub'
  }
  return typeMap[type] || '未知'
}

const getSourceTypeTone = (type: SourceType): CnTone => {
  const toneMap: Record<string, CnTone> = {
    local: 'info',
    api: 'success',
    github: 'warning'
  }
  return toneMap[type] || 'info'
}
</script>

<style scoped>
.sensitive-source-page {
  min-height: 100%;
}

.sensitive-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.source-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.source-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-cell span,
.muted-text {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.dialog-footer {
  justify-content: flex-end;
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

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
