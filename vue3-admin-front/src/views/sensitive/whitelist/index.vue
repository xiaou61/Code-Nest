<template>
  <CnPage class="sensitive-whitelist-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="白名单管理"
      description="维护不会触发敏感词命中的安全词汇，按全局和模块级范围控制内容检测例外策略。"
      eyebrow="Sensitive Whitelist"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 条</CnStatusTag>
        <CnStatusTag type="success">全局 {{ globalCountInPage }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedRows.length" type="warning">已选择 {{ selectedRows.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadWhitelist">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增白名单</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="当前总量" :value="total" description="当前筛选条件下的白名单规模" tone="brand" />
      <CnStatCard title="启用词汇" :value="enabledCountInPage" description="当前页可参与豁免的词汇" tone="success" />
      <CnStatCard title="全局词汇" :value="globalCountInPage" description="当前页全站生效的白名单" tone="info" />
      <CnStatCard title="已选择" :value="selectedRows.length" description="批量删除将作用于所选词汇" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按白名单词汇、分类、作用范围和启用状态定位记录。" divided>
      <CnFilterForm
        :model-value="queryForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleQueryFormUpdate"
        @search="handleSearch"
        @reset="resetQuery"
      />
    </CnSection>

    <CnSection title="白名单列表" :description="`共 ${total} 条白名单记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="白名单数据" description="刷新缓存后，白名单例外策略会按最新配置参与内容检测。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ queryForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag v-if="selectedRows.length" type="warning" size="sm">
                已选择 {{ selectedRows.length }} 条
              </CnStatusTag>
            </template>

            <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
            <el-button type="danger" :icon="Delete" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button type="warning" :icon="Refresh" @click="handleRefreshCache">刷新缓存</el-button>
          </CnToolbar>
        </template>

        <template #word="{ row }">
          <div class="word-cell">
            <strong>{{ row.word || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #scope="{ row }">
          <CnStatusTag :type="getScopeTone(row.scope)" size="sm">
            {{ getScopeText(row.scope) }}
          </CnStatusTag>
        </template>

        <template #moduleName="{ row }">
          <span class="muted-text">{{ row.scope === 'module' ? row.moduleName || '-' : '全局生效' }}</span>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无白名单"
            description="当前筛选条件下没有匹配记录，可以重置筛选或新增白名单词汇。"
            icon="WL"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="resetQuery">重置筛选</el-button>
              <el-button type="primary" @click="handleAdd">新增白名单</el-button>
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
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="词汇" prop="word">
          <el-input v-model="form.word" placeholder="请输入白名单词汇" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="请输入分类，如：专业术语、成语、人名等" maxlength="50" />
        </el-form-item>
        <el-form-item label="作用范围" prop="scope">
          <el-radio-group v-model="form.scope">
            <el-radio value="global">全局</el-radio>
            <el-radio value="module">模块级</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scope === 'module'" label="模块名称" prop="moduleName">
          <el-select v-model="form.moduleName" placeholder="请选择模块" class="full-width-control">
            <el-option label="社区模块" value="community" />
            <el-option label="面试模块" value="interview" />
            <el-option label="朋友圈模块" value="moment" />
            <el-option label="博客模块" value="blog" />
          </el-select>
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
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import {
  addWhitelist,
  deleteWhitelist,
  deleteWhitelistBatch,
  listWhitelist,
  refreshWhitelistCache,
  updateWhitelist
} from '@/api/sensitive'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

type WhitelistScope = 'global' | 'module'

interface WhitelistRecord {
  id: number
  word: string
  category?: string
  scope: WhitelistScope
  moduleName?: string
  status: number
  createTime?: string
  [key: string]: unknown
}

interface WhitelistQuery {
  pageNum: number
  pageSize: number
  word: string
  category: string
  scope: WhitelistScope | null
  moduleName: string
  status: number | null
}

interface WhitelistForm {
  id: number | null
  word: string
  category: string
  scope: WhitelistScope
  moduleName: string
  status: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '白名单管理' }]

const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const tableData = ref<WhitelistRecord[]>([])
const selectedRows = ref<WhitelistRecord[]>([])
const total = ref(0)
const formRef = ref<FormInstance>()

const queryForm = reactive<WhitelistQuery>({
  pageNum: 1,
  pageSize: 10,
  word: '',
  category: '',
  scope: null,
  moduleName: '',
  status: null
})

const form = reactive<WhitelistForm>({
  id: null,
  word: '',
  category: '',
  scope: 'global',
  moduleName: '',
  status: 1
})

const formRules: FormRules<WhitelistForm> = {
  word: [
    { required: true, message: '请输入白名单词汇', trigger: 'blur' },
    { min: 1, max: 255, message: '长度在 1 到 255 个字符', trigger: 'blur' }
  ],
  scope: [{ required: true, message: '请选择作用范围', trigger: 'change' }],
  moduleName: [
    {
      validator: (_rule, value, callback) => {
        if (form.scope === 'module' && !value) {
          callback(new Error('请选择模块名称'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'word', label: '白名单词汇', type: 'input', placeholder: '请输入词汇' },
  { prop: 'category', label: '分类', type: 'input', placeholder: '请输入分类' },
  {
    prop: 'scope',
    label: '作用范围',
    type: 'select',
    placeholder: '请选择范围',
    options: [
      { label: '全局', value: 'global' },
      { label: '模块级', value: 'module' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<WhitelistRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'word', label: '白名单词汇', minWidth: 150, slot: 'word', showOverflowTooltip: true },
  { prop: 'category', label: '分类', minWidth: 120, showOverflowTooltip: true },
  { prop: 'scope', label: '作用范围', width: 110, slot: 'scope' },
  { prop: 'moduleName', label: '模块名称', minWidth: 120, slot: 'moduleName', showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180 },
  { label: '操作', width: 130, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: queryForm.pageNum,
  pageSize: queryForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const enabledCountInPage = computed(() => tableData.value.filter((item) => item.status === 1).length)
const globalCountInPage = computed(() => tableData.value.filter((item) => item.scope === 'global').length)

onMounted(() => {
  loadWhitelist()
})

const loadWhitelist = async () => {
  loading.value = true
  try {
    const response = await listWhitelist({ ...queryForm })
    tableData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('查询白名单失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.pageNum = 1
  loadWhitelist()
}

const resetQuery = () => {
  Object.assign(queryForm, {
    pageNum: 1,
    pageSize: 10,
    word: '',
    category: '',
    scope: null,
    moduleName: '',
    status: null
  })
  loadWhitelist()
}

const handleQueryFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handlePageChange = (page: number) => {
  queryForm.pageNum = page
  loadWhitelist()
}

const handlePageSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  loadWhitelist()
}

const handleAdd = () => {
  dialogTitle.value = '新增白名单'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: WhitelistRecord) => {
  dialogTitle.value = '编辑白名单'
  Object.assign(form, {
    id: row.id,
    word: row.word || '',
    category: row.category || '',
    scope: row.scope === 'module' ? 'module' : 'global',
    moduleName: row.moduleName || '',
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
      moduleName: form.scope === 'module' ? form.moduleName : ''
    }

    if (form.id) {
      await updateWhitelist(payload)
      ElMessage.success('更新成功')
    } else {
      await addWhitelist(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadWhitelist()
  } catch (error) {
    console.error(form.id ? '更新白名单失败:' : '新增白名单失败:', error)
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (row: WhitelistRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除白名单词汇 "${row.word}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWhitelist(row.id)
    ElMessage.success('删除成功')
    loadWhitelist()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除白名单失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个白名单词汇吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = selectedRows.value.map((row) => row.id)
    await deleteWhitelistBatch(ids)
    ElMessage.success('删除成功')
    loadWhitelist()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除白名单失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection: WhitelistRecord[]) => {
  selectedRows.value = selection
}

const handleRefreshCache = async () => {
  try {
    await refreshWhitelistCache()
    ElMessage.success('缓存刷新成功')
  } catch (error) {
    console.error('刷新白名单缓存失败:', error)
    ElMessage.error('缓存刷新失败')
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    word: '',
    category: '',
    scope: 'global',
    moduleName: '',
    status: 1
  })
  formRef.value?.resetFields()
}

const getScopeText = (scope: WhitelistScope) => {
  const scopeMap: Record<WhitelistScope, string> = { global: '全局', module: '模块级' }
  return scopeMap[scope] || '未知'
}

const getScopeTone = (scope: WhitelistScope): CnTone => {
  const toneMap: Record<WhitelistScope, CnTone> = { global: 'success', module: 'info' }
  return toneMap[scope] || 'neutral'
}
</script>

<style scoped>
.sensitive-whitelist-page {
  min-height: 100%;
}

.sensitive-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.word-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.word-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.word-cell span,
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
