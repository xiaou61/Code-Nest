<template>
  <CnPage class="sensitive-words-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="词库管理"
      description="维护敏感词词库、风险等级、处理动作和导入导出流程，保障内容安全策略稳定生效。"
      eyebrow="Sensitive Words"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">内容安全</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 条</CnStatusTag>
        <CnStatusTag type="danger">高风险 {{ highRiskCountInPage }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedRows.length" type="warning">已选择 {{ selectedRows.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadWords">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增敏感词</el-button>
      </template>
    </CnPageHeader>

    <div class="sensitive-stat-grid">
      <CnStatCard title="当前总量" :value="total" description="当前筛选条件下的词库规模" tone="brand" />
      <CnStatCard title="启用词条" :value="enabledCountInPage" description="当前页可参与检测的词条" tone="success" />
      <CnStatCard title="高风险词条" :value="highRiskCountInPage" description="当前页高风险词条数量" tone="danger" />
      <CnStatCard title="已选择" :value="selectedRows.length" description="批量删除将作用于所选词条" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按敏感词、分类、风险等级和启用状态定位词条。" divided>
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

    <CnSection title="敏感词列表" :description="`共 ${total} 条敏感词记录`" divided>
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
          <CnToolbar title="词库数据" description="导入和刷新词库会影响内容检测实时策略。" align="center">
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
            <el-button type="info" :icon="Upload" @click="handleImport">导入</el-button>
            <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">导出</el-button>
            <el-button type="warning" :icon="Refresh" @click="handleRefresh">刷新词库</el-button>
          </CnToolbar>
        </template>

        <template #word="{ row }">
          <div class="word-cell">
            <strong>{{ row.word || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #level="{ row }">
          <CnStatusTag :type="getLevelTone(row.level)" size="sm">
            {{ getLevelText(row.level) }}
          </CnStatusTag>
        </template>

        <template #action="{ row }">
          <CnStatusTag :type="getActionTone(row.action)" size="sm">
            {{ getActionText(row.action) }}
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
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无敏感词"
            description="当前筛选条件下没有匹配词条，可以重置筛选或新增词条。"
            icon="SW"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="resetQuery">重置筛选</el-button>
              <el-button type="primary" @click="handleAdd">新增敏感词</el-button>
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
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" placeholder="请输入敏感词" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" class="full-width-control">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择风险等级" class="full-width-control">
            <el-option label="低风险" :value="1" />
            <el-option label="中风险" :value="2" />
            <el-option label="高风险" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理动作" prop="action">
          <el-select v-model="form.action" placeholder="请选择处理动作" class="full-width-control">
            <el-option label="替换" :value="1" />
            <el-option label="拒绝" :value="2" />
            <el-option label="审核" :value="3" />
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

    <el-dialog
      v-model="importDialogVisible"
      title="批量导入敏感词（预览确认）"
      width="620px"
      @close="resetImportState"
    >
      <div class="import-stack">
        <el-alert title="导入说明" type="info" :closable="false">
          <div>1. 支持 TXT 文件格式</div>
          <div>2. 每行一个敏感词，或用逗号分隔</div>
          <div>3. 重复的敏感词将自动跳过</div>
        </el-alert>

        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :on-change="handleFileChange"
          :file-list="fileList"
          accept=".txt"
          drag
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        </el-upload>

        <div v-if="previewResult" class="preview-result">
          <el-descriptions :column="3" border size="small" title="导入预览">
            <el-descriptions-item label="总词条">{{ previewResult.total || 0 }}</el-descriptions-item>
            <el-descriptions-item label="可导入">{{ previewResult.importableCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="无效词条">{{ previewResult.invalidCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="文件内重复">{{ previewResult.duplicateInFileCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="库内已存在">{{ previewResult.duplicateInDbCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="有效去重后">{{ previewResult.validCount || 0 }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="previewResult.importableSamples?.length" class="preview-samples">
            <div class="preview-samples-title">可导入示例</div>
            <CnStatusTag
              v-for="item in previewResult.importableSamples"
              :key="`importable-${item}`"
              type="success"
              size="sm"
            >
              {{ item }}
            </CnStatusTag>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="info" :loading="previewingImport" @click="handlePreviewImport">预览</el-button>
          <el-button
            type="primary"
            :loading="importing"
            :disabled="!previewResult || (previewResult.importableCount || 0) <= 0"
            @click="handleImportSubmit"
          >
            确认导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules, UploadInstance, UploadUserFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, Upload } from '@element-plus/icons-vue'
import {
  addSensitiveWord,
  confirmSensitiveWordsImport,
  deleteSensitiveWord,
  deleteSensitiveWords,
  exportSensitiveWords,
  listSensitiveCategories,
  listSensitiveWords,
  previewSensitiveWordsImport,
  refreshWordLibrary,
  updateSensitiveWord
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

interface SensitiveCategory {
  id: number | string
  name: string
}

interface SensitiveWordRecord {
  id: number
  word: string
  categoryId?: number | string | null
  categoryName?: string
  level: number
  action: number
  status: number
  creatorName?: string
  createTime?: string
  [key: string]: unknown
}

interface SensitiveWordForm {
  id: number | null
  word: string
  categoryId: number | string | null
  level: number
  action: number
  status: number
}

interface SensitiveWordQuery {
  pageNum: number
  pageSize: number
  word: string
  categoryId: number | string | null
  level: number | null
  status: number | null
}

interface ImportPreviewResult {
  total?: number
  importableCount?: number
  invalidCount?: number
  duplicateInFileCount?: number
  duplicateInDbCount?: number
  validCount?: number
  importableSamples?: string[]
}

interface ImportConfirmResult {
  success?: number
  duplicate?: number
  errors?: unknown[]
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '敏感词管理' }, { label: '词库管理' }]

const loading = ref(false)
const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const importing = ref(false)
const previewingImport = ref(false)
const exporting = ref(false)
const dialogTitle = ref('')
const tableData = ref<SensitiveWordRecord[]>([])
const categories = ref<SensitiveCategory[]>([])
const selectedRows = ref<SensitiveWordRecord[]>([])
const total = ref(0)
const fileList = ref<UploadUserFile[]>([])
const previewResult = ref<ImportPreviewResult | null>(null)
const formRef = ref<FormInstance>()
const uploadRef = ref<UploadInstance>()

const queryForm = reactive<SensitiveWordQuery>({
  pageNum: 1,
  pageSize: 10,
  word: '',
  categoryId: null,
  level: null,
  status: null
})

const form = reactive<SensitiveWordForm>({
  id: null,
  word: '',
  categoryId: null,
  level: 1,
  action: 1,
  status: 1
})

const formRules: FormRules<SensitiveWordForm> = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  level: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  action: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const filterFields = computed<CnFilterField[]>(() => [
  { prop: 'word', label: '敏感词', type: 'input', placeholder: '请输入敏感词' },
  {
    prop: 'categoryId',
    label: '分类',
    type: 'select',
    placeholder: '请选择分类',
    options: categories.value.map((category) => ({
      label: category.name,
      value: category.id
    }))
  },
  {
    prop: 'level',
    label: '风险等级',
    type: 'select',
    placeholder: '请选择风险等级',
    options: [
      { label: '低风险', value: 1 },
      { label: '中风险', value: 2 },
      { label: '高风险', value: 3 }
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
])

const tableColumns: CnTableColumn<SensitiveWordRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'word', label: '敏感词', minWidth: 150, slot: 'word', showOverflowTooltip: true },
  { prop: 'categoryName', label: '分类', minWidth: 120, showOverflowTooltip: true },
  { prop: 'level', label: '风险等级', width: 110, slot: 'level' },
  { prop: 'action', label: '处理动作', width: 110, slot: 'action' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'creatorName', label: '创建人', minWidth: 120, showOverflowTooltip: true },
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
const highRiskCountInPage = computed(() => tableData.value.filter((item) => item.level === 3).length)

onMounted(() => {
  loadCategories()
  loadWords()
})

const loadWords = async () => {
  loading.value = true
  try {
    const response = await listSensitiveWords({ ...queryForm })
    tableData.value = response?.records || []
    total.value = response?.total || 0
  } catch (error) {
    console.error('查询敏感词失败:', error)
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryForm.pageNum = 1
  loadWords()
}

const resetQuery = () => {
  Object.assign(queryForm, {
    pageNum: 1,
    pageSize: 10,
    word: '',
    categoryId: null,
    level: null,
    status: null
  })
  loadWords()
}

const handleQueryFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(queryForm, value)
}

const handlePageChange = (page: number) => {
  queryForm.pageNum = page
  loadWords()
}

const handlePageSizeChange = (size: number) => {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  loadWords()
}

const handleAdd = () => {
  dialogTitle.value = '新增敏感词'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: SensitiveWordRecord) => {
  dialogTitle.value = '编辑敏感词'
  Object.assign(form, {
    id: row.id,
    word: row.word || '',
    categoryId: row.categoryId ?? null,
    level: Number(row.level) || 1,
    action: Number(row.action) || 1,
    status: Number(row.status) === 0 ? 0 : 1
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (form.id) {
      await updateSensitiveWord({ ...form })
      ElMessage.success('更新成功')
    } else {
      await addSensitiveWord({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadWords()
  } catch (error) {
    console.error(form.id ? '更新敏感词失败:' : '新增敏感词失败:', error)
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (row: SensitiveWordRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除敏感词 "${row.word}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    loadWords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除敏感词失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个敏感词吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = selectedRows.value.map((row) => row.id)
    await deleteSensitiveWords(ids)
    ElMessage.success('删除成功')
    loadWords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除敏感词失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSelectionChange = (selection: SensitiveWordRecord[]) => {
  selectedRows.value = selection
}

const handleImport = () => {
  resetImportState()
  importDialogVisible.value = true
}

const handleFileChange = (_file: unknown, files: UploadUserFile[]) => {
  fileList.value = files
  previewResult.value = null
}

const handlePreviewImport = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  previewingImport.value = true
  try {
    const file = fileList.value[0].raw
    if (!file) {
      ElMessage.warning('请选择要导入的文件')
      return
    }

    previewResult.value = await previewSensitiveWordsImport(file)
    ElMessage.success('预览完成，请确认后导入')
  } catch (error) {
    console.error('预览敏感词导入失败:', error)
    ElMessage.error('预览失败')
  } finally {
    previewingImport.value = false
  }
}

const handleImportSubmit = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  importing.value = true
  try {
    const file = fileList.value[0].raw
    if (!file) {
      ElMessage.warning('请选择要导入的文件')
      return
    }

    const result: ImportConfirmResult = await confirmSensitiveWordsImport(file)
    ElMessage.success(
      `导入完成：成功 ${result.success || 0} 个，重复 ${result.duplicate || 0} 个，失败 ${result.errors?.length || 0} 个`
    )
    importDialogVisible.value = false
    resetImportState()
    loadWords()
  } catch (error) {
    console.error('导入敏感词失败:', error)
    ElMessage.error('导入失败')
  } finally {
    importing.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const result = await exportSensitiveWords({
      word: queryForm.word,
      categoryId: queryForm.categoryId,
      level: queryForm.level,
      status: queryForm.status
    })

    const blob = new Blob([result?.content || ''], { type: 'text/csv;charset=utf-8;' })
    const fileName = result?.fileName || `sensitive_words_${Date.now()}.csv`

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success(`导出成功，共 ${result?.total || 0} 条`)
  } catch (error) {
    console.error('导出敏感词失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleRefresh = async () => {
  try {
    await refreshWordLibrary()
    ElMessage.success('敏感词库刷新成功')
  } catch (error) {
    console.error('刷新敏感词库失败:', error)
    ElMessage.error('敏感词库刷新失败')
  }
}

const resetImportState = () => {
  fileList.value = []
  previewResult.value = null
  importing.value = false
  previewingImport.value = false
  uploadRef.value?.clearFiles()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    word: '',
    categoryId: null,
    level: 1,
    action: 1,
    status: 1
  })
  formRef.value?.resetFields()
}

const loadCategories = async () => {
  try {
    const result = await listSensitiveCategories()
    categories.value = Array.isArray(result) ? result : result?.records || []
  } catch (error) {
    console.error('加载敏感词分类失败:', error)
    ElMessage.error('加载分类失败')
  }
}

const getLevelText = (level: number) => {
  const levelMap: Record<number, string> = { 1: '低风险', 2: '中风险', 3: '高风险' }
  return levelMap[level] || '未知'
}

const getLevelTone = (level: number): CnTone => {
  const toneMap: Record<number, CnTone> = { 1: 'success', 2: 'warning', 3: 'danger' }
  return toneMap[level] || 'info'
}

const getActionText = (action: number) => {
  const actionMap: Record<number, string> = { 1: '替换', 2: '拒绝', 3: '审核' }
  return actionMap[action] || '未知'
}

const getActionTone = (action: number): CnTone => {
  const toneMap: Record<number, CnTone> = { 1: 'info', 2: 'danger', 3: 'warning' }
  return toneMap[action] || 'info'
}
</script>

<style scoped>
.sensitive-words-page {
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

.word-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer,
.preview-samples {
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

.import-stack {
  display: grid;
  gap: var(--cn-space-4);
}

.preview-samples {
  margin-top: var(--cn-space-3);
}

.preview-samples-title {
  width: 100%;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 650;
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
