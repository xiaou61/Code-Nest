<template>
  <CnPage class="bug-store-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="Bug 商店管理"
      description="维护经典 Bug 案例，支持筛选、编辑、删除和批量导入。"
      eyebrow="Moyu Bug Store"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">总数 {{ statistics?.totalBugs || 0 }}</CnStatusTag>
        <CnStatusTag type="success">初级 {{ statistics?.easyBugs || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">中级 {{ statistics?.mediumBugs || 0 }}</CnStatusTag>
        <CnStatusTag type="danger">高级 {{ statistics?.hardBugs || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="danger" :icon="Delete" :disabled="selectedBugs.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedBugs.length }})
        </el-button>
        <el-button type="success" :icon="Upload" @click="handleBatchImport">批量导入</el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增 Bug</el-button>
      </template>
    </CnPageHeader>

    <div v-if="statistics" class="stats-grid">
      <CnStatCard title="总 Bug 数" :value="statistics.totalBugs || 0" description="当前 Bug 商店收录总量" tone="brand" />
      <CnStatCard title="初级 Bug" :value="statistics.easyBugs || 0" description="适合入门排查的案例" tone="success" />
      <CnStatCard title="中级 Bug" :value="statistics.mediumBugs || 0" description="需要一定工程经验的案例" tone="warning" />
      <CnStatCard title="高级 Bug" :value="statistics.hardBugs || 0" description="复杂链路与疑难问题案例" tone="danger" />
    </div>

    <CnSection title="筛选条件" description="按标题、难度、状态和技术标签定位 Bug 案例。" divided>
      <div class="filter-grid">
        <el-input
          v-model="searchForm.title"
          placeholder="请输入 Bug 标题"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="searchForm.difficultyLevel" placeholder="难度等级" clearable @change="handleSearch">
          <el-option label="初级" :value="1" />
          <el-option label="中级" :value="2" />
          <el-option label="高级" :value="3" />
          <el-option label="专家级" :value="4" />
        </el-select>

        <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>

        <el-input
          v-model="searchForm.techTag"
          placeholder="技术标签"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="Bug 列表" :description="`共 ${pagination.total} 条 Bug 记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="bugList"
        :loading="tableLoading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无 Bug"
        empty-description="当前筛选条件下没有 Bug 记录。"
        empty-icon="BS"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #title="{ row }">
          <div class="bug-title-cell">
            <span class="title-text">{{ row.title }}</span>
            <CnStatusTag :type="getDifficultyTone(row.difficultyLevel)" size="sm">
              {{ getDifficultyName(row.difficultyLevel) }}
            </CnStatusTag>
          </div>
        </template>

        <template #phenomenon="{ row }">
          <span class="phenomenon-text">{{ truncateText(row.phenomenon, 60) }}</span>
        </template>

        <template #techTags="{ row }">
          <div class="tag-list">
            <CnStatusTag v-for="tag in getTechTagsArray(row.techTags)" :key="`${row.id}-${tag}`" type="info" size="sm" subtle>
              {{ tag }}
            </CnStatusTag>
            <span v-if="!getTechTagsArray(row.techTags).length" class="muted-text">-</span>
          </div>
        </template>

        <template #status="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>

        <template #createTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button type="warning" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="viewDialogVisible" title="Bug 详情" width="800px" :close-on-click-modal="false">
      <div v-if="currentBug" class="bug-detail">
        <CnSection title="基本信息" surface="plain" divided>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="Bug 标题">{{ currentBug.title }}</el-descriptions-item>
            <el-descriptions-item label="难度等级">
              <CnStatusTag :type="getDifficultyTone(currentBug.difficultyLevel)" size="sm">
                {{ getDifficultyName(currentBug.difficultyLevel) }}
              </CnStatusTag>
            </el-descriptions-item>
            <el-descriptions-item label="技术标签">
              <div class="tag-list">
                <CnStatusTag v-for="tag in getTechTagsArray(currentBug.techTags)" :key="`detail-${tag}`" type="info" size="sm" subtle>
                  {{ tag }}
                </CnStatusTag>
                <span v-if="!getTechTagsArray(currentBug.techTags).length" class="muted-text">-</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <CnStatusTag :type="currentBug.status === 1 ? 'success' : 'danger'" size="sm">
                {{ currentBug.status === 1 ? '启用' : '禁用' }}
              </CnStatusTag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(currentBug.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(currentBug.updateTime) }}</el-descriptions-item>
          </el-descriptions>
        </CnSection>

        <CnSection title="现象描述" surface="plain" divided>
          <div class="content-box phenomenon">{{ currentBug.phenomenon }}</div>
        </CnSection>

        <CnSection title="原因分析" surface="plain" divided>
          <div class="content-box analysis">
            <pre>{{ currentBug.causeAnalysis }}</pre>
          </div>
        </CnSection>

        <CnSection title="解决方案" surface="plain" divided>
          <div class="content-box solution">
            <pre>{{ currentBug.solution }}</pre>
          </div>
        </CnSection>
      </div>
    </el-dialog>

    <el-dialog v-model="formDialogVisible" :title="isEditing ? '编辑 Bug' : '新增 Bug'" width="900px" :close-on-click-modal="false">
      <el-form ref="bugFormRef" :model="bugForm" :rules="bugRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="Bug 标题" prop="title">
              <el-input v-model="bugForm.title" placeholder="请输入 Bug 标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度等级" prop="difficultyLevel">
              <el-select v-model="bugForm.difficultyLevel" placeholder="选择难度等级">
                <el-option label="初级" :value="1" />
                <el-option label="中级" :value="2" />
                <el-option label="高级" :value="3" />
                <el-option label="专家级" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="bugForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术标签" prop="techTags">
              <el-input v-model="bugForm.techTags" placeholder="请输入技术标签，多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="现象描述" prop="phenomenon">
              <el-input
                v-model="bugForm.phenomenon"
                type="textarea"
                :rows="4"
                placeholder="请描述 Bug 的现象表现"
                maxlength="1000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="原因分析" prop="causeAnalysis">
              <el-input
                v-model="bugForm.causeAnalysis"
                type="textarea"
                :rows="5"
                placeholder="请分析 Bug 产生的原因"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="解决方案" prop="solution">
              <el-input
                v-model="bugForm.solution"
                type="textarea"
                :rows="6"
                placeholder="请提供 Bug 的解决方案"
                maxlength="2000"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序值" prop="sortOrder">
              <el-input-number v-model="bugForm.sortOrder" :min="0" :max="9999" placeholder="数值越大越靠前" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEditing ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" title="批量导入 Bug" width="600px">
      <div class="import-content">
        <el-alert title="导入说明" type="info" show-icon :closable="false">
          <p>1. 请使用标准的 Excel 格式文件</p>
          <p>2. 表格列顺序：Bug 标题、现象描述、原因分析、解决方案、技术标签、难度等级</p>
          <p>3. 难度等级：1-初级，2-中级，3-高级，4-专家级</p>
        </el-alert>

        <div class="upload-section">
          <el-upload ref="uploadRef" class="upload-demo" :auto-upload="false" :limit="1" accept=".xlsx,.xls" :on-change="handleFileChange">
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="upload-tip">只能上传 xlsx/xls 文件</div>
            </template>
          </el-upload>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">导入</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { Delete, Plus, Search, Upload } from '@element-plus/icons-vue'
import { moyuApi } from '@/api/moyu'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface BugRecord extends Record<string, unknown> {
  id: number | null
  title: string
  phenomenon?: string
  causeAnalysis?: string
  solution?: string
  techTags?: string
  difficultyLevel?: number
  status?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

interface BugStatistics {
  totalBugs?: number
  easyBugs?: number
  mediumBugs?: number
  hardBugs?: number
}

interface BugSearchForm {
  title: string
  difficultyLevel: number | null
  status: number | null
  techTag: string
}

interface BugForm {
  id: number | null
  title: string
  phenomenon: string
  causeAnalysis: string
  solution: string
  techTags: string
  difficultyLevel: number
  status: number
  sortOrder: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '摸鱼工具' }, { label: 'Bug 商店' }]

const tableLoading = ref(false)
const submitLoading = ref(false)
const importLoading = ref(false)
const bugList = ref<BugRecord[]>([])
const selectedBugs = ref<BugRecord[]>([])
const statistics = ref<BugStatistics>({})
const viewDialogVisible = ref(false)
const formDialogVisible = ref(false)
const importDialogVisible = ref(false)
const isEditing = ref(false)
const currentBug = ref<BugRecord | null>(null)
const bugFormRef = ref<FormInstance>()
const uploadRef = ref()

const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const searchForm = reactive<BugSearchForm>({
  title: '',
  difficultyLevel: null,
  status: null,
  techTag: ''
})

const bugForm = reactive<BugForm>({
  id: null,
  title: '',
  phenomenon: '',
  causeAnalysis: '',
  solution: '',
  techTags: '',
  difficultyLevel: 1,
  status: 1,
  sortOrder: 0
})

const bugRules: FormRules<BugForm> = {
  title: [{ required: true, message: '请输入 Bug 标题', trigger: 'blur' }],
  phenomenon: [{ required: true, message: '请输入现象描述', trigger: 'blur' }],
  causeAnalysis: [{ required: true, message: '请输入原因分析', trigger: 'blur' }],
  solution: [{ required: true, message: '请输入解决方案', trigger: 'blur' }],
  difficultyLevel: [{ required: true, message: '请选择难度等级', trigger: 'change' }]
}

const tableColumns: CnTableColumn<BugRecord>[] = [
  { type: 'selection', width: 55 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: 'Bug 标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'phenomenon', label: '现象描述', minWidth: 180, slot: 'phenomenon', showOverflowTooltip: true },
  { prop: 'techTags', label: '技术标签', minWidth: 160, slot: 'techTags' },
  { prop: 'status', label: '状态', width: 90, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170, slot: 'createTime' },
  { label: '操作', width: 180, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.current,
  pageSize: pagination.size,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const loadBugList = async () => {
  try {
    tableLoading.value = true
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...searchForm
    }

    const response = await moyuApi.bugStore.getBugList(params)

    if (response && response.records) {
      bugList.value = response.records
      pagination.total = response.total
    }
  } catch (error) {
    console.error('获取 Bug 列表失败:', error)
    ElMessage.error('获取 Bug 列表失败')
  } finally {
    tableLoading.value = false
  }
}

const loadStatistics = async () => {
  try {
    // 这里可以根据需要调用统计接口
    statistics.value = {
      totalBugs: 0,
      easyBugs: 0,
      mediumBugs: 0,
      hardBugs: 0
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadBugList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    title: '',
    difficultyLevel: null,
    status: null,
    techTag: ''
  })
  handleSearch()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  loadBugList()
}

const handleCurrentChange = (val: number) => {
  pagination.current = val
  loadBugList()
}

const handleSelectionChange = (val: unknown[]) => {
  selectedBugs.value = val as BugRecord[]
}

const handleAdd = () => {
  resetBugForm()
  isEditing.value = false
  formDialogVisible.value = true
}

const handleEdit = (row: BugRecord) => {
  Object.assign(bugForm, { ...row })
  isEditing.value = true
  formDialogVisible.value = true
}

const handleView = (row: BugRecord) => {
  currentBug.value = row
  viewDialogVisible.value = true
}

const handleDelete = async (row: BugRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除 Bug "${row.title}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await moyuApi.bugStore.deleteBug(row.id)
    ElMessage.success('删除成功')
    loadBugList()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除 Bug 失败:', error)
    ElMessage.error('删除 Bug 失败')
  }
}

const handleBatchDelete = async () => {
  if (selectedBugs.value.length === 0) {
    ElMessage.warning('请选择要删除的 Bug')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedBugs.value.length} 个 Bug 吗？`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    for (const bug of selectedBugs.value) {
      await moyuApi.bugStore.deleteBug(bug.id)
    }

    ElMessage.success('批量删除成功')
    loadBugList()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

const handleStatusChange = async (row: BugRecord) => {
  try {
    await moyuApi.bugStore.updateBug(row.id, {
      ...row,
      status: row.status
    })
    ElMessage.success('状态更新成功')
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新状态失败')
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleSubmit = async () => {
  if (!bugFormRef.value) return

  try {
    await bugFormRef.value.validate()
    submitLoading.value = true

    if (isEditing.value) {
      await moyuApi.bugStore.updateBug(bugForm.id, bugForm)
      ElMessage.success('更新成功')
    } else {
      await moyuApi.bugStore.addBug(bugForm)
      ElMessage.success('创建成功')
    }

    formDialogVisible.value = false
    loadBugList()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error(isEditing.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleBatchImport = () => {
  importDialogVisible.value = true
}

const handleFileChange = (file: UploadFile) => {
  console.log('Selected file:', file)
}

const handleImportSubmit = async () => {
  try {
    importLoading.value = true
    // 这里处理文件上传和导入逻辑
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    loadBugList()
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

const resetBugForm = () => {
  Object.assign(bugForm, {
    id: null,
    title: '',
    phenomenon: '',
    causeAnalysis: '',
    solution: '',
    techTags: '',
    difficultyLevel: 1,
    status: 1,
    sortOrder: 0
  })
}

const getDifficultyName = (level?: number) => {
  const names: Record<number, string> = { 1: '初级', 2: '中级', 3: '高级', 4: '专家级' }
  return level ? names[level] || '未知' : '未知'
}

const getDifficultyTone = (level?: number): CnTone => {
  const tones: Record<number, CnTone> = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'brand' }
  return level ? tones[level] || 'neutral' : 'neutral'
}

const getTechTagsArray = (techTags?: string) => {
  if (!techTags) return []
  return techTags
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

const truncateText = (text?: string, maxLength = 60) => {
  if (!text) return ''
  return text.length > maxLength ? `${text.substring(0, maxLength)}...` : text
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadBugList()
  loadStatistics()
})
</script>

<style scoped>
.bug-store-management-page {
  min-height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(3, minmax(150px, 1fr)) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions,
.table-actions,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.bug-title-cell {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.title-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.phenomenon-text,
.muted-text,
.upload-tip {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.bug-detail {
  display: grid;
  gap: var(--cn-space-4);
  max-height: 600px;
  overflow-y: auto;
}

.content-box {
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.content-box.phenomenon {
  border-color: color-mix(in srgb, var(--cn-color-danger) 24%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-danger) 8%, var(--cn-color-bg-surface));
  color: var(--cn-color-danger);
}

.content-box.analysis {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 24%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-brand-primary) 8%, var(--cn-color-bg-surface));
  color: var(--cn-color-brand-primary);
}

.content-box.solution {
  border-color: color-mix(in srgb, var(--cn-color-success) 24%, var(--cn-color-border-subtle));
  background: color-mix(in srgb, var(--cn-color-success) 8%, var(--cn-color-bg-surface));
  color: var(--cn-color-success);
}

.content-box pre {
  margin: 0;
  font-family: inherit;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.dialog-footer {
  text-align: right;
}

.import-content {
  margin-bottom: var(--cn-space-4);
}

.upload-section {
  margin-top: var(--cn-space-5);
}

.upload-demo {
  width: 100%;
}

.upload-tip {
  margin-top: var(--cn-space-2);
}

@media (max-width: 1080px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }
}
</style>
