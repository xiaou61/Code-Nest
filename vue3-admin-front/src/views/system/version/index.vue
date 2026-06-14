<template>
  <CnPage class="version-management" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="版本更新历史管理"
      description="管理网站版本更新历史，记录产品迭代轨迹。"
      eyebrow="System Version"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">产品迭代</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条</CnStatusTag>
        <CnStatusTag v-if="multipleSelection.length" type="warning">
          已选择 {{ multipleSelection.length }} 条
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadVersionList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增版本</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按版本关键词、更新类型、状态、推荐位和发布时间定位版本记录。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="handleReset"
      >
        <template #releaseTimeRange="{ value, setValue }">
          <el-date-picker
            :model-value="value"
            type="datetimerange"
            range-separator="至"
            start-placeholder="发布开始时间"
            end-placeholder="发布结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @update:model-value="setValue"
          />
        </template>
      </CnFilterForm>
    </CnSection>

    <CnSection title="版本列表" :description="`共 ${pagination.total} 条版本历史记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="versionList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="版本数据" description="批量操作只作用于当前勾选的版本记录。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag v-if="multipleSelection.length" type="warning" size="sm">
                已选择 {{ multipleSelection.length }} 条
              </CnStatusTag>
            </template>

            <el-button type="success" :disabled="multipleSelection.length === 0" :icon="VideoPlay" @click="handleBatchPublish">
              批量发布
            </el-button>
            <el-button type="warning" :disabled="multipleSelection.length === 0" :icon="Hide" @click="handleBatchHide">
              批量隐藏
            </el-button>
            <el-button type="danger" :disabled="multipleSelection.length === 0" :icon="Delete" @click="handleBatchDelete">
              批量删除
            </el-button>
          </CnToolbar>
        </template>

        <template #versionNumber="{ row }">
          <CnStatusTag type="brand" size="sm" :dot="false">{{ row.versionNumber }}</CnStatusTag>
        </template>

        <template #title="{ row }">
          <div class="version-title-cell">
            <CnStatusTag v-if="row.isFeatured === 1" type="warning" size="sm">推荐</CnStatusTag>
            <span>{{ row.title }}</span>
          </div>
        </template>

        <template #updateType="{ row }">
          <CnStatusTag :type="getUpdateTypeTone(row.updateType)" size="sm">
            {{ row.updateTypeName || getUpdateTypeName(row.updateType) }}
          </CnStatusTag>
        </template>

        <template #prdUrl="{ row }">
          <el-button v-if="row.prdUrl" type="primary" link size="small" :icon="Link" @click="openPrdLink(row.prdUrl)">
            查看 PRD
          </el-button>
          <span v-else class="muted-text">未关联</span>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ row.statusName || getStatusName(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" link size="small" :icon="VideoPlay" @click="handlePublish(row.id)">
              发布
            </el-button>
            <el-button v-if="row.status === 1" type="warning" link size="small" :icon="Hide" @click="handleHide(row.id)">
              隐藏
            </el-button>
            <el-button v-if="row.status === 2" type="success" link size="small" :icon="VideoPlay" @click="handlePublish(row.id)">
              发布
            </el-button>
            <el-button v-if="row.status !== 0" type="info" link size="small" :icon="Document" @click="handleUnpublish(row.id)">
              草稿
            </el-button>
            <el-popconfirm title="确定要删除这个版本记录吗？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无版本记录"
            description="当前筛选条件下没有匹配版本，可以重置筛选或新增版本。"
            icon="VR"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
              <el-button type="primary" @click="handleCreate">新增版本</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      :before-close="handleDialogClose"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="版本号" prop="versionNumber">
              <el-input v-model="form.versionNumber" placeholder="如：v1.3.0" @blur="checkVersionNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="更新类型" prop="updateType">
              <el-select v-model="form.updateType" placeholder="请选择更新类型" class="full-width-control">
                <el-option label="重大更新" :value="1" />
                <el-option label="功能更新" :value="2" />
                <el-option label="修复更新" :value="3" />
                <el-option label="其他" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="更新标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入更新标题" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="简要描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入版本更新的简要描述（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="PRD文档链接">
          <el-input v-model="form.prdUrl" placeholder="请输入PRD文档链接（选填）" maxlength="500">
            <template #append>
              <el-button v-if="form.prdUrl" @click="openPrdLink(form.prdUrl)">预览</el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发布时间" prop="releaseTime">
              <el-date-picker
                v-model="form.releaseTime"
                type="datetime"
                placeholder="请选择发布时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="full-width-control"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="请选择状态" class="full-width-control">
                <el-option label="草稿" :value="0" />
                <el-option label="已发布" :value="1" />
                <el-option label="已隐藏" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序权重">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" class="full-width-control" />
              <div class="form-tip">数字越大越靠前显示</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重点推荐">
              <el-switch
                v-model="form.isFeatured"
                :active-value="1"
                :inactive-value="0"
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            {{ editMode ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Document, Edit, Hide, Link, Plus, Refresh, VideoPlay } from '@element-plus/icons-vue'
import {
  batchDeleteVersions,
  batchHideVersions,
  batchPublishVersions,
  checkVersionNumberExists,
  createVersion,
  deleteVersion,
  getVersionList,
  hideVersion,
  publishVersion,
  unpublishVersion,
  updateVersion
} from '@/api/version'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface VersionRecord {
  id: number
  versionNumber: string
  title: string
  updateType: number
  updateTypeName?: string
  description?: string
  prdUrl?: string
  releaseTime?: string
  status: number
  statusName?: string
  viewCount?: number
  sortOrder?: number
  isFeatured?: number
}

interface VersionForm {
  id: number | null
  versionNumber: string
  title: string
  updateType: number | null
  description: string
  prdUrl: string
  releaseTime: string
  status: number
  sortOrder: number
  isFeatured: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '系统管理' }, { label: '版本历史' }]

const loading = ref(false)
const submitLoading = ref(false)
const versionList = ref<VersionRecord[]>([])
const multipleSelection = ref<VersionRecord[]>([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const searchForm = reactive<{
  keyword: string
  updateType: number | ''
  status: number | ''
  isFeatured: number | ''
  releaseTimeRange: string[] | null
}>({
  keyword: '',
  updateType: '',
  status: '',
  isFeatured: '',
  releaseTimeRange: null
})

const dialogVisible = ref(false)
const editMode = ref(false)
const dialogTitle = ref('新增版本')
const formRef = ref()

const form = reactive<VersionForm>({
  id: null,
  versionNumber: '',
  title: '',
  updateType: null,
  description: '',
  prdUrl: '',
  releaseTime: '',
  status: 0,
  sortOrder: 0,
  isFeatured: 0
})

const rules = {
  versionNumber: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { max: 50, message: '版本号长度不能超过50个字符', trigger: 'blur' }
  ],
  title: [
    { required: true, message: '请输入更新标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200个字符', trigger: 'blur' }
  ],
  updateType: [{ required: true, message: '请选择更新类型', trigger: 'change', type: 'number' }],
  releaseTime: [{ required: true, message: '请选择发布时间', trigger: 'change' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '请输入版本号或标题关键词' },
  {
    prop: 'updateType',
    label: '更新类型',
    type: 'select',
    placeholder: '更新类型',
    options: [
      { label: '重大更新', value: 1 },
      { label: '功能更新', value: 2 },
      { label: '修复更新', value: 3 },
      { label: '其他', value: 4 }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '状态筛选',
    options: [
      { label: '草稿', value: 0 },
      { label: '已发布', value: 1 },
      { label: '已隐藏', value: 2 }
    ]
  },
  {
    prop: 'isFeatured',
    label: '推荐',
    type: 'select',
    placeholder: '推荐筛选',
    options: [
      { label: '普通版本', value: 0 },
      { label: '重点推荐', value: 1 }
    ]
  },
  { prop: 'releaseTimeRange', label: '发布时间', type: 'custom', slot: 'releaseTimeRange' }
]

const tableColumns: CnTableColumn<VersionRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'versionNumber', label: '版本号', width: 120, slot: 'versionNumber' },
  { prop: 'title', label: '更新标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'updateType', label: '更新类型', width: 120, slot: 'updateType' },
  { prop: 'description', label: '简要描述', minWidth: 260, showOverflowTooltip: true },
  { prop: 'prdUrl', label: 'PRD 文档', width: 120, slot: 'prdUrl' },
  { prop: 'releaseTime', label: '发布时间', width: 170 },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'viewCount', label: '查看次数', width: 100, align: 'right' },
  { label: '操作', width: 220, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

onMounted(() => {
  loadVersionList()
})

const loadVersionList = async () => {
  try {
    loading.value = true
    const params: Record<string, unknown> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    }

    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    if (searchForm.updateType !== '' && searchForm.updateType !== null && searchForm.updateType !== undefined) {
      params.updateType = searchForm.updateType
    }
    if (searchForm.status !== '' && searchForm.status !== null && searchForm.status !== undefined) {
      params.status = searchForm.status
    }
    if (searchForm.isFeatured !== '' && searchForm.isFeatured !== null && searchForm.isFeatured !== undefined) {
      params.isFeatured = searchForm.isFeatured
    }
    if (searchForm.releaseTimeRange && searchForm.releaseTimeRange.length === 2) {
      params.releaseTimeStart = searchForm.releaseTimeRange[0]
      params.releaseTimeEnd = searchForm.releaseTimeRange[1]
    }

    const data = await getVersionList(params)
    versionList.value = data.records || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('加载版本列表失败:', error)
    ElMessage.error('加载版本列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadVersionList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    updateType: '',
    status: '',
    isFeatured: '',
    releaseTimeRange: null
  })
  handleSearch()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSelectionChange = (selection: VersionRecord[]) => {
  multipleSelection.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadVersionList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadVersionList()
}

const handleCreate = () => {
  editMode.value = false
  dialogTitle.value = '新增版本'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: VersionRecord) => {
  editMode.value = true
  dialogTitle.value = '编辑版本'
  Object.assign(form, {
    ...row,
    updateType: row.updateType ? Number(row.updateType) : null,
    status: Number(row.status) || 0,
    isFeatured: Number(row.isFeatured) || 0,
    sortOrder: Number(row.sortOrder) || 0
  })
  dialogVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    versionNumber: '',
    title: '',
    updateType: null,
    description: '',
    prdUrl: '',
    releaseTime: '',
    status: 0,
    sortOrder: 0,
    isFeatured: 0
  })
  formRef.value?.resetFields()
}

const checkVersionNumber = async () => {
  if (!form.versionNumber) return false

  try {
    const exists = await checkVersionNumberExists(form.versionNumber, form.id)
    if (exists) {
      ElMessage.warning('该版本号已存在，请使用其他版本号')
      return false
    }
    return true
  } catch (error) {
    console.error('检查版本号失败:', error)
    return false
  }
}

const handleSubmit = () => {
  if (!formRef.value) return

  formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    const versionValid = await checkVersionNumber()
    if (!versionValid) return

    try {
      submitLoading.value = true

      if (editMode.value) {
        await updateVersion(form)
        ElMessage.success('更新版本成功')
      } else {
        await createVersion(form)
        ElMessage.success('创建版本成功')
      }

      dialogVisible.value = false
      loadVersionList()
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error(editMode.value ? '更新版本失败' : '创建版本失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  dialogVisible.value = false
  resetForm()
}

const handlePublish = async (id: number) => {
  try {
    await publishVersion(id)
    ElMessage.success('发布成功')
    loadVersionList()
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  }
}

const handleHide = async (id: number) => {
  try {
    await hideVersion(id)
    ElMessage.success('隐藏成功')
    loadVersionList()
  } catch (error) {
    console.error('隐藏失败:', error)
    ElMessage.error('隐藏失败')
  }
}

const handleUnpublish = async (id: number) => {
  try {
    await unpublishVersion(id)
    ElMessage.success('已设置为草稿')
    loadVersionList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteVersion(id)
    ElMessage.success('删除成功')
    loadVersionList()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleBatchPublish = async () => {
  if (multipleSelection.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要发布选中的 ${multipleSelection.value.length} 个版本吗？`, '批量发布', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = multipleSelection.value.map((item) => item.id)
    await batchPublishVersions(ids)
    ElMessage.success('批量发布成功')
    loadVersionList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量发布失败:', error)
      ElMessage.error('批量发布失败')
    }
  }
}

const handleBatchHide = async () => {
  if (multipleSelection.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要隐藏选中的 ${multipleSelection.value.length} 个版本吗？`, '批量隐藏', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const ids = multipleSelection.value.map((item) => item.id)
    await batchHideVersions(ids)
    ElMessage.success('批量隐藏成功')
    loadVersionList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量隐藏失败:', error)
      ElMessage.error('批量隐藏失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (multipleSelection.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${multipleSelection.value.length} 个版本吗？此操作不可恢复！`, '批量删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })
    const ids = multipleSelection.value.map((item) => item.id)
    await batchDeleteVersions(ids)
    ElMessage.success('批量删除成功')
    loadVersionList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const openPrdLink = (url?: string) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const getUpdateTypeTone = (type: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'danger',
    2: 'brand',
    3: 'warning',
    4: 'info'
  }
  return toneMap[type] || 'info'
}

const getStatusTone = (status: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    0: 'info',
    1: 'success',
    2: 'warning'
  }
  return toneMap[status] || 'info'
}

const getUpdateTypeName = (type: number) => {
  const nameMap: Record<number, string> = {
    1: '重大更新',
    2: '功能更新',
    3: '修复更新',
    4: '其他'
  }
  return nameMap[type] || '其他'
}

const getStatusName = (status: number) => {
  const nameMap: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已隐藏'
  }
  return nameMap[status] || '未知'
}
</script>

<style scoped>
.version-management {
  min-height: 100%;
}

.version-title-cell {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
  max-width: 100%;
}

.version-title-cell span:last-child {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.table-actions {
  justify-content: flex-start;
}

.table-actions .el-button {
  margin-left: 0;
}

.muted-text {
  color: var(--cn-color-text-muted);
  font-size: 13px;
}

.form-tip {
  margin-top: 4px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

@media (max-width: 680px) {
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
