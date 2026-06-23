<template>
  <CnPage class="knowledge-map-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="知识图谱管理"
      description="管理和创建知识图谱，用于组织面试相关知识点。"
      eyebrow="Knowledge Maps"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">知识图谱</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 个图谱</CnStatusTag>
        <CnStatusTag type="success">已发布 {{ publishedCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="warning">已选 {{ multipleSelection.length }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="danger" :icon="Delete" :disabled="multipleSelection.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新建图谱</el-button>
      </template>
    </CnPageHeader>

    <div class="knowledge-stat-grid">
      <CnStatCard title="图谱总量" :value="pagination.total" description="当前筛选条件下的图谱数量" tone="brand" />
      <CnStatCard title="当前页节点" :value="nodeCountInPage" description="当前页图谱累计节点数" tone="info" />
      <CnStatCard title="当前页浏览" :value="viewCountInPage" description="当前页图谱累计查看次数" tone="success" />
      <CnStatCard title="已选图谱" :value="multipleSelection.length" description="可用于批量删除操作" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按图谱标题、描述和发布状态筛选知识图谱。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="2"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="图谱列表" :description="`共 ${pagination.total} 个图谱`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="mapList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="图谱数据" description="编辑结构会进入可视化图谱编辑器，状态操作会影响用户端可见性。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.size }} 条</CnStatusTag>
              <CnStatusTag type="warning" size="sm">已选 {{ multipleSelection.length }} 个</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="handleCreate">新建图谱</el-button>
          </CnToolbar>
        </template>

        <template #cover="{ row }">
          <div class="cover-container">
            <img v-if="row.coverImage" :src="row.coverImage" class="cover-image" alt="封面" />
            <div v-else class="cover-placeholder">
              <el-icon><Picture /></el-icon>
            </div>
          </div>
        </template>

        <template #title="{ row }">
          <div class="title-container">
            <h4 class="map-title">{{ row.title }}</h4>
            <p class="map-description">{{ row.description || '暂无描述' }}</p>
          </div>
        </template>

        <template #stats="{ row }">
          <div class="stats-container">
            <CnStatusTag type="info" size="sm">{{ row.nodeCount || 0 }} 节点</CnStatusTag>
            <CnStatusTag type="success" size="sm">{{ row.viewCount || 0 }} 查看</CnStatusTag>
          </div>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">{{ row.statusDesc || getStatusText(row.status) }}</CnStatusTag>
        </template>

        <template #createTime="{ row }">{{ formatDate(row.createTime) }}</template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEditStructure(row)">结构</el-button>
            <el-button type="primary" link size="small" :icon="Setting" @click="handleEditInfo(row)">信息</el-button>
            <el-dropdown @command="(command: string) => handleDropdownCommand(command, row)">
              <el-button link size="small" :icon="More">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status !== 1" command="publish" :icon="Upload">发布</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 1" command="hide" :icon="Hide">隐藏</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" class="danger-dropdown">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleDialogClose">
      <el-form ref="mapFormRef" :model="mapForm" :rules="mapFormRules" label-width="100px">
        <el-form-item label="图谱标题" prop="title">
          <el-input v-model="mapForm.title" placeholder="请输入图谱标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="图谱描述" prop="description">
          <el-input v-model="mapForm.description" type="textarea" :rows="4" placeholder="请输入图谱描述" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="封面图片" prop="coverImage">
          <el-upload
            ref="uploadRef"
            class="cover-upload"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
            accept="image/*"
            drag
          >
            <div v-if="mapForm.coverImage" class="cover-preview">
              <img :src="mapForm.coverImage" alt="封面预览" />
              <div class="cover-overlay">
                <el-button type="primary" :icon="Upload" size="small">重新上传</el-button>
                <el-button type="danger" :icon="Delete" size="small" @click.stop="removeCoverImage">删除</el-button>
              </div>
            </div>
            <div v-else class="upload-placeholder">
              <el-icon class="el-icon--upload" size="48"><UploadFilled /></el-icon>
              <div class="el-upload__text">将图片拖到此处，或<em>点击上传</em></div>
              <div class="el-upload__tip">支持 JPG、PNG、GIF 格式，文件大小不超过 2MB</div>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="mapForm.sortOrder" :min="0" :max="9999" controls-position="right" />
          <span class="form-tip">数值越大排序越靠前</span>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始状态" prop="status">
          <el-radio-group v-model="mapForm.status">
            <el-radio :label="0">草稿</el-radio>
            <el-radio :label="1">直接发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Edit,
  Hide,
  More,
  Picture,
  Plus,
  Setting,
  Upload,
  UploadFilled
} from '@element-plus/icons-vue'
import {
  createKnowledgeMap,
  deleteBatchKnowledgeMaps,
  deleteKnowledgeMap,
  getKnowledgeMapList,
  hideKnowledgeMap,
  publishKnowledgeMap,
  updateKnowledgeMap
} from '@/api/knowledge'
import { fileAPI } from '@/api/filestorage'
import router from '@/router'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface KnowledgeMap {
  id: number
  title: string
  description?: string
  coverImage?: string
  sortOrder?: number
  status?: number
  statusDesc?: string
  nodeCount?: number
  viewCount?: number
  createTime?: string
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '知识图谱' }, { label: '图谱管理' }]

const loading = ref(false)
const mapList = ref<KnowledgeMap[]>([])
const multipleSelection = ref<KnowledgeMap[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentMapId = ref<number | null>(null)
const uploadRef = ref()
const uploading = ref(false)
const mapFormRef = ref()

const searchForm = reactive({
  keyword: '',
  status: null as number | null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const mapForm = reactive({
  title: '',
  description: '',
  coverImage: '',
  sortOrder: 0,
  status: 0
})

const mapFormRules = {
  title: [
    { required: true, message: '请输入图谱标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200个字符', trigger: 'blur' }
  ],
  description: [{ max: 2000, message: '描述长度不能超过2000个字符', trigger: 'blur' }],
  coverImage: [{ max: 500, message: 'URL长度不能超过500个字符', trigger: 'blur' }]
}

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '关键词', type: 'input', placeholder: '搜索图谱标题或描述' },
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
  }
]

const tableColumns: CnTableColumn<KnowledgeMap>[] = [
  { type: 'selection', width: 55 },
  { prop: 'coverImage', label: '封面', width: 100, slot: 'cover' },
  { prop: 'title', label: '标题', minWidth: 220, slot: 'title' },
  { label: '统计信息', width: 150, slot: 'stats' },
  { prop: 'status', label: '状态', width: 110, align: 'center', slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, slot: 'createTime' },
  { label: '操作', width: 190, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.page,
  pageSize: pagination.size,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const dialogTitle = computed(() => (isEdit.value ? '编辑图谱信息' : '创建知识图谱'))
const publishedCountInPage = computed(() => mapList.value.filter((item) => item.status === 1).length)
const nodeCountInPage = computed(() => mapList.value.reduce((sum, item) => sum + (Number(item.nodeCount) || 0), 0))
const viewCountInPage = computed(() => mapList.value.reduce((sum, item) => sum + (Number(item.viewCount) || 0), 0))

const fetchMapList = async () => {
  try {
    loading.value = true
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      ...searchForm
    }
    const data = await getKnowledgeMapList(params)
    mapList.value = data.records || []
    pagination.total = data.total || 0
    pagination.page = data.pageNum || 1
  } catch {
    ElMessage.error('获取图谱列表失败')
  } finally {
    loading.value = false
  }
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSearch = () => {
  pagination.page = 1
  fetchMapList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    status: null
  })
  pagination.page = 1
  fetchMapList()
}

const handleSelectionChange = (selection: unknown[]) => {
  multipleSelection.value = selection as KnowledgeMap[]
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchMapList()
}

const handleCurrentChange = (page: number) => {
  pagination.page = page
  fetchMapList()
}

const getStatusText = (status?: number) => ({ 0: '草稿', 1: '已发布', 2: '已隐藏' })[Number(status)] || '未知'
const getStatusTone = (status?: number): CnTone => ({ 0: 'neutral', 1: 'success', 2: 'warning' })[Number(status)] as CnTone || 'neutral'

const formatDate = (dateString?: string) => {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

const handleCreate = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

const handleEditStructure = (row: KnowledgeMap) => {
  router.push(`/knowledge/maps/${row.id}/edit`)
}

const handleEditInfo = (row: KnowledgeMap) => {
  resetForm()
  isEdit.value = true
  currentMapId.value = row.id
  Object.assign(mapForm, {
    title: row.title,
    description: row.description,
    coverImage: row.coverImage,
    sortOrder: row.sortOrder
  })
  dialogVisible.value = true
}

const handleDropdownCommand = async (command: string, row: KnowledgeMap) => {
  switch (command) {
    case 'publish':
      await handlePublish(row)
      break
    case 'hide':
      await handleHide(row)
      break
    case 'delete':
      await handleDelete(row)
      break
  }
}

const handlePublish = async (row: KnowledgeMap) => {
  try {
    await ElMessageBox.confirm('确认发布此知识图谱？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await publishKnowledgeMap(row.id)
    ElMessage.success('发布成功')
    fetchMapList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('发布失败')
    }
  }
}

const handleHide = async (row: KnowledgeMap) => {
  try {
    await ElMessageBox.confirm('确认隐藏此知识图谱？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await hideKnowledgeMap(row.id)
    ElMessage.success('隐藏成功')
    fetchMapList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('隐藏失败')
    }
  }
}

const handleDelete = async (row: KnowledgeMap) => {
  try {
    await ElMessageBox.confirm('确认删除此知识图谱？删除后无法恢复！', '危险操作', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await deleteKnowledgeMap(row.id)
    ElMessage.success('删除成功')
    fetchMapList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${multipleSelection.value.length} 个知识图谱？删除后无法恢复！`, '批量删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    const ids = multipleSelection.value.map((item) => item.id)
    await deleteBatchKnowledgeMaps(ids)
    ElMessage.success('批量删除成功')
    fetchMapList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await mapFormRef.value.validate()
    if (isEdit.value) {
      await updateKnowledgeMap(currentMapId.value, mapForm)
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeMap(mapForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchMapList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  }
}

const handleDialogClose = () => {
  resetForm()
}

const handleFileChange = async (file: { raw: File }) => {
  if (!file) return
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif']
  if (!allowedTypes.includes(file.raw.type)) {
    ElMessage.error('只支持 JPG、PNG、GIF 格式的图片')
    return
  }
  const maxSize = 2 * 1024 * 1024
  if (file.raw.size > maxSize) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  try {
    uploading.value = true
    const result = await fileAPI.uploadSingle(file.raw, 'knowledge', 'cover')
    mapForm.coverImage = result.url
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(`上传失败：${error?.message || '未知错误'}`)
  } finally {
    uploading.value = false
  }
}

const removeCoverImage = () => {
  mapForm.coverImage = ''
}

const resetForm = () => {
  Object.assign(mapForm, {
    title: '',
    description: '',
    coverImage: '',
    sortOrder: 0,
    status: 0
  })
  currentMapId.value = null
  mapFormRef.value?.resetFields()
  uploadRef.value?.clearFiles()
}

onMounted(() => {
  fetchMapList()
})
</script>

<style scoped>
.knowledge-map-page {
  min-height: 100%;
}

.knowledge-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.cover-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 60px;
  height: 40px;
  overflow: hidden;
  border-radius: var(--cn-radius-sm);
  background: var(--cn-color-bg-surface-muted);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  color: var(--cn-color-text-tertiary);
}

.title-container {
  padding: 4px 0;
}

.map-title {
  margin: 0 0 4px;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 650;
  line-height: 1.4;
}

.map-description {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.3;
}

.stats-container,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.danger-dropdown {
  color: var(--cn-color-danger);
}

.upload-placeholder {
  padding: 40px 20px;
  border: 2px dashed var(--cn-color-border);
  border-radius: var(--cn-radius-card);
  color: var(--cn-color-text-secondary);
  text-align: center;
  cursor: pointer;
  transition: border-color var(--cn-motion-base) var(--cn-ease-out);
}

.upload-placeholder:hover {
  border-color: var(--cn-color-primary);
}

.cover-upload,
.cover-upload :deep(.el-upload),
.cover-upload :deep(.el-upload-dragger) {
  width: 100%;
}

:deep(.el-upload__text) {
  margin-top: 16px;
  font-size: 14px;
}

:deep(.el-upload__text em) {
  color: var(--cn-color-primary);
  font-style: normal;
}

:deep(.el-upload__tip) {
  margin-top: 8px;
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.cover-preview {
  position: relative;
  margin-top: 8px;
  cursor: pointer;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-preview img {
  display: block;
  width: 120px;
  height: 80px;
  border: 1px solid var(--cn-color-border);
  border-radius: var(--cn-radius-sm);
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--cn-space-2);
  border-radius: var(--cn-radius-sm);
  background: color-mix(in srgb, black 52%, transparent);
  opacity: 0;
  transition: opacity var(--cn-motion-base) var(--cn-ease-out);
}

.form-tip {
  margin-left: var(--cn-space-2);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .knowledge-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .knowledge-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
