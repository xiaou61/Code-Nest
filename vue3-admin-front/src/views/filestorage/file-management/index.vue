<template>
  <CnPage class="file-management-page" surface="transparent" max-width="1480px">
    <CnPageHeader
      title="文件管理"
      description="管理平台文件上传、预览、链接生成、移动、删除和物理存在性检查。"
      eyebrow="File Management"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">文件 {{ total }} 个</CnStatusTag>
        <CnStatusTag type="success">已选 {{ selectedFiles.length }} 个</CnStatusTag>
        <CnStatusTag type="warning">统计 {{ showStats ? '展开' : '收起' }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Upload" @click="showUploadDialog">上传文件</el-button>
        <el-button type="success" :icon="View" :disabled="!selectedFiles.length" @click="checkSelectedFilesExists">
          检查文件
        </el-button>
        <el-button :icon="Refresh" :loading="loading" @click="refreshList">刷新</el-button>
        <el-button type="info" :icon="DataAnalysis" @click="showStatistics">统计信息</el-button>
      </template>
    </CnPageHeader>

    <div v-if="showStats" class="stats-grid">
      <CnStatCard title="总文件数" :value="statistics.totalFiles || 0" description="平台累计上传文件数量" tone="brand" />
      <CnStatCard title="总存储量" :value="formatFileSize(statistics.totalStorageSize || 0)" description="文件占用的总存储空间" tone="success" />
      <CnStatCard title="今日上传" :value="statistics.todayFiles || 0" description="当天新增上传文件数量" tone="info" />
      <CnStatCard title="存储配置数" :value="statistics.storageConfigs || 0" description="可用存储配置数量" tone="warning" />
      <CnStatCard title="已删除文件" :value="statistics.deletedFiles || 0" description="逻辑删除或删除状态文件" tone="danger" />
    </div>

    <CnSection title="筛选条件" description="按模块、业务类型、文件类型和状态定位文件。" divided>
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="模块名称">
          <el-input v-model="queryParams.moduleName" placeholder="请输入模块名称" clearable class="filter-control" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="queryParams.businessType" placeholder="请输入业务类型" clearable class="filter-control" />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="queryParams.contentType" placeholder="请选择文件类型" clearable class="filter-control">
            <el-option label="全部" value="" />
            <el-option label="图片" value="image" />
            <el-option label="文档" value="document" />
            <el-option label="视频" value="video" />
            <el-option label="音频" value="audio" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="filter-control">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="已删除" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="文件列表" :description="`共 ${total} 条文件记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="fileList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        empty-title="暂无文件"
        empty-description="当前筛选条件下没有匹配文件，可以重置筛选或上传新文件。"
        empty-icon="FM"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #originalName="{ row }">
          <div class="file-info">
            <el-icon class="file-icon" :style="{ '--file-tone': getFileTypeColor(row.contentType) }">
              <component :is="getFileIcon(row.contentType)" />
            </el-icon>
            <span>{{ row.originalName }}</span>
          </div>
        </template>

        <template #fileSize="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>

        <template #isPublic="{ row }">
          <CnStatusTag :type="row.isPublic === 1 ? 'success' : 'warning'" size="sm">
            {{ row.isPublic === 1 ? '公开' : '私有' }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
            {{ row.status === 1 ? '正常' : '已删除' }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" @click="previewFile(row)">预览</el-button>
            <el-button type="success" link size="small" @click="getFileUrl(row)">获取链接</el-button>
            <el-button type="warning" link size="small" @click="logicalDeleteFile(row)">逻辑删除</el-button>
            <el-button type="info" link size="small" @click="showMoveDialog(row)">移动</el-button>
            <el-button type="danger" link size="small" @click="forceDeleteFile(row)">物理删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="moveDialogVisible" title="移动文件" width="500px">
      <el-form :model="moveForm" label-width="120px">
        <el-form-item label="当前文件：">
          <div class="current-file">
            <el-icon><Document /></el-icon>
            {{ currentMoveFile?.originalName }}
          </div>
        </el-form-item>
        <el-form-item label="目标存储：" required>
          <el-select v-model="moveForm.targetStorageId" placeholder="请选择目标存储配置" class="full-width">
            <el-option
              v-for="config in storageConfigs"
              :key="config.id"
              :label="`${config.configName} (${getStorageTypeName(config.storageType)})`"
              :value="config.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="moveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleMoveFile">确定移动</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" title="文件上传" width="600px">
      <el-form :model="uploadForm" label-width="120px">
        <el-form-item label="模块名称" required>
          <el-input v-model="uploadForm.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-input v-model="uploadForm.businessType" placeholder="请输入业务类型(可选)" />
        </el-form-item>
        <el-form-item label="上传类型">
          <el-radio-group v-model="uploadForm.uploadType">
            <el-radio value="single">单文件上传</el-radio>
            <el-radio value="batch">批量上传</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :multiple="uploadForm.uploadType === 'batch'"
            :file-list="uploadFileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                {{ uploadForm.uploadType === 'batch' ? '支持多文件上传' : '仅支持单文件上传' }}
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="uploading" @click="handleUploadFiles">
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="urlDialogVisible" title="文件访问链接" width="600px">
      <div v-if="currentFileUrl" class="url-content">
        <div class="info-panel">
          <h3>{{ currentUrlFile?.originalName }}</h3>
          <p>文件ID: {{ currentUrlFile?.id }}</p>
          <p>生成时间: {{ currentUrlTime }}</p>
        </div>

        <el-form label-width="120px">
          <el-form-item label="有效期设置">
            <el-input-number v-model="urlExpireHours" :min="1" :max="168" controls-position="right" />
            <span class="unit-text">小时</span>
            <el-button type="primary" size="small" class="inline-action" @click="regenerateUrl">重新生成</el-button>
          </el-form-item>
          <el-form-item label="访问链接">
            <el-input v-model="currentFileUrl" readonly type="textarea" :rows="3" />
          </el-form-item>
        </el-form>

        <div class="url-actions">
          <el-button type="success" :icon="DocumentCopy" @click="copyUrl">复制链接</el-button>
          <el-button type="primary" :icon="Link" @click="openUrl">打开链接</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="previewDialogVisible" title="文件预览" width="800px">
      <div class="file-preview">
        <div class="info-panel">
          <h3>{{ currentPreviewFile?.originalName }}</h3>
          <p>文件大小: {{ formatFileSize(currentPreviewFile?.fileSize) }}</p>
          <p>上传时间: {{ currentPreviewFile?.uploadTime }}</p>
          <p>MD5校验: {{ currentPreviewFile?.md5Hash }}</p>
        </div>

        <div v-if="isImageFile(currentPreviewFile?.contentType)" class="image-preview">
          <img :src="currentPreviewFile?.accessUrl" alt="预览图片" class="preview-image" />
        </div>

        <div v-else-if="isTextFile(currentPreviewFile?.contentType)" v-loading="previewLoading" class="text-preview">
          <el-alert
            v-if="previewError"
            :title="previewError"
            type="warning"
            show-icon
            :closable="false"
            class="preview-alert"
          />
          <el-input v-model="previewContent" type="textarea" :rows="10" readonly placeholder="文本内容预览..." />
        </div>

        <div v-else class="other-preview">
          <el-alert title="此文件类型不支持预览" type="info" :closable="false" />
          <div class="download-section">
            <el-button type="primary" :icon="Download" @click="downloadFile(currentPreviewFile)">下载文件</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Document,
  DocumentCopy,
  Download,
  Link,
  Microphone,
  Picture,
  Refresh,
  Upload,
  UploadFilled,
  VideoPlay,
  View
} from '@element-plus/icons-vue'
import { fileAPI, storageAPI } from '@/api/filestorage'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn } from '@/design-system'

interface FileRecord extends Record<string, unknown> {
  id: number
  originalName: string
  fileSize?: number
  contentType?: string
  moduleName?: string
  businessType?: string
  isPublic?: number
  status?: number
  uploadTime?: string
  accessUrl?: string
  md5Hash?: string
}

interface StorageConfig extends Record<string, unknown> {
  id: number
  configName: string
  storageType: string
}

interface FileStatistics {
  totalFiles?: number
  totalStorageSize?: number
  todayFiles?: number
  storageConfigs?: number
  deletedFiles?: number
}

interface UploadListItem {
  raw?: File
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '文件存储' }, { label: '文件管理' }]

const loading = ref(false)
const fileList = ref<FileRecord[]>([])
const total = ref(0)
const showStats = ref(true)
const statistics = ref<FileStatistics>({})
const storageConfigs = ref<StorageConfig[]>([])
const moveDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const urlDialogVisible = ref(false)
const currentMoveFile = ref<FileRecord | null>(null)
const currentPreviewFile = ref<FileRecord | null>(null)
const currentUrlFile = ref<FileRecord | null>(null)
const previewContent = ref('')
const previewError = ref('')
const previewLoading = ref(false)
const currentFileUrl = ref('')
const currentUrlTime = ref('')
const uploading = ref(false)
const selectedFiles = ref<FileRecord[]>([])
const uploadRef = ref()
const uploadFileList = ref<UploadListItem[]>([])
const urlExpireHours = ref(24)
const TEXT_PREVIEW_MAX_SIZE = 1024 * 1024

const uploadForm = reactive({
  moduleName: '',
  businessType: '',
  uploadType: 'single'
})

const queryParams = reactive({
  moduleName: '',
  businessType: '',
  contentType: '',
  status: '' as string | number,
  pageNum: 1,
  pageSize: 20
})

const moveForm = reactive({
  targetStorageId: null as number | null
})

const storageTypeConfig: Record<string, { name: string }> = {
  LOCAL: { name: '本地存储' },
  OSS: { name: '阿里云OSS' },
  COS: { name: '腾讯云COS' },
  KODO: { name: '七牛云KODO' },
  OBS: { name: '华为云OBS' }
}

const tableColumns: CnTableColumn<FileRecord>[] = [
  { type: 'selection', width: 55 },
  { prop: 'id', label: '文件ID', width: 90 },
  { prop: 'originalName', label: '文件名称', minWidth: 220, slot: 'originalName', showOverflowTooltip: true },
  { prop: 'fileSize', label: '文件大小', width: 120, slot: 'fileSize' },
  { prop: 'contentType', label: '文件类型', width: 160, showOverflowTooltip: true },
  { prop: 'moduleName', label: '所属模块', width: 120, showOverflowTooltip: true },
  { prop: 'businessType', label: '业务类型', width: 120, showOverflowTooltip: true },
  { prop: 'isPublic', label: '访问权限', width: 100, slot: 'isPublic' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'uploadTime', label: '上传时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 320, fixed: 'right', slot: 'actions' }
]

const pagination = computed<CnPagination>(() => ({
  page: queryParams.pageNum,
  pageSize: queryParams.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const loadFileList = async () => {
  loading.value = true
  try {
    const data = await fileAPI.getFileList(queryParams)
    fileList.value = Array.isArray(data?.records) ? data.records : []
    total.value = Number(data?.total) || 0
  } catch (error) {
    ElMessage.error('获取文件列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const data = await fileAPI.getFileStatistics()
    statistics.value = data || {}
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

const loadStorageConfigs = async () => {
  try {
    const data = await storageAPI.getStorageConfigs({ isEnabled: 1 })
    storageConfigs.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取存储配置失败:', error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  loadFileList()
}

const resetQuery = () => {
  Object.assign(queryParams, {
    moduleName: '',
    businessType: '',
    contentType: '',
    status: '',
    pageNum: 1,
    pageSize: 20
  })
  loadFileList()
}

const refreshList = () => {
  loadFileList()
  loadStatistics()
}

const showStatistics = () => {
  showStats.value = !showStats.value
}

const handleSizeChange = (size: number) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadFileList()
}

const handleCurrentChange = (page: number) => {
  queryParams.pageNum = page
  loadFileList()
}

const previewFile = async (file: FileRecord) => {
  currentPreviewFile.value = file
  previewContent.value = ''
  previewError.value = ''
  previewLoading.value = false
  previewDialogVisible.value = true

  if (isTextFile(file.contentType)) {
    if ((file.fileSize || 0) > TEXT_PREVIEW_MAX_SIZE) {
      previewError.value = '文件超过 1MB，已停止在线预览，请下载后查看。'
      previewDialogVisible.value = true
      return
    }

    previewLoading.value = true
    try {
      const blob = await fileAPI.downloadFile(file.id)
      previewContent.value = await readBlobAsText(blob)
    } catch (error) {
      previewError.value = `无法加载文件内容：${error.message || '未知错误'}`
    } finally {
      previewLoading.value = false
    }
  }
}

const showMoveDialog = (file: FileRecord) => {
  currentMoveFile.value = file
  moveForm.targetStorageId = null
  moveDialogVisible.value = true
}

const handleMoveFile = async () => {
  if (!moveForm.targetStorageId) {
    ElMessage.warning('请选择目标存储配置')
    return
  }

  try {
    await fileAPI.moveFile(currentMoveFile.value?.id, moveForm.targetStorageId)
    ElMessage.success('文件移动成功')
    moveDialogVisible.value = false
    loadFileList()
  } catch (error) {
    ElMessage.error('文件移动失败：' + (error.message || '未知错误'))
  }
}

const forceDeleteFile = async (file: FileRecord) => {
  try {
    await ElMessageBox.confirm('确定要物理删除此文件吗？删除后无法恢复！', '危险操作', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      buttonSize: 'small'
    })

    await fileAPI.forceDeleteFile(file.id)
    ElMessage.success('文件删除成功')
    loadFileList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('文件删除失败：' + (error.message || '未知错误'))
    }
  }
}

const downloadFile = async (file?: FileRecord | null) => {
  if (!file) return
  try {
    const response = await fileAPI.downloadFile(file.id)
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.originalName)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('文件下载成功')
  } catch (error) {
    ElMessage.error('文件下载失败：' + (error.message || '未知错误'))
  }
}

const readBlobAsText = async (blob: Blob) => {
  if (blob && typeof blob.text === 'function') {
    return blob.text()
  }

  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('文本读取失败'))
    reader.readAsText(blob, 'UTF-8')
  })
}

const showUploadDialog = () => {
  Object.assign(uploadForm, {
    moduleName: '',
    businessType: '',
    uploadType: 'single'
  })
  uploadFileList.value = []
  uploadDialogVisible.value = true
}

const handleFileChange = (file: UploadListItem, fileList: UploadListItem[]) => {
  if (uploadForm.uploadType === 'single' && fileList.length > 1) {
    uploadFileList.value = [fileList[fileList.length - 1]]
  } else {
    uploadFileList.value = fileList
  }
}

const handleFileRemove = (_file: UploadListItem, fileList: UploadListItem[]) => {
  uploadFileList.value = fileList
}

const handleUploadFiles = async () => {
  if (!uploadForm.moduleName.trim()) {
    ElMessage.warning('请输入模块名称')
    return
  }

  if (!uploadFileList.value.length) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  try {
    const files = uploadFileList.value.map((item) => item.raw).filter(Boolean) as File[]

    if (uploadForm.uploadType === 'single') {
      await fileAPI.uploadSingle(files[0], uploadForm.moduleName, uploadForm.businessType || 'default')
      ElMessage.success('文件上传成功')
    } else {
      const results = await fileAPI.uploadBatch(files, uploadForm.moduleName, uploadForm.businessType || 'default')
      ElMessage.success(`批量上传成功，共上传 ${results.length} 个文件`)
    }

    uploadDialogVisible.value = false
    loadFileList()
    loadStatistics()
  } catch (error) {
    ElMessage.error('文件上传失败：' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

const getFileUrl = async (file: FileRecord) => {
  try {
    const url = await fileAPI.getFileUrl(file.id, urlExpireHours.value)
    currentFileUrl.value = String(url)
    currentUrlFile.value = file
    currentUrlTime.value = new Date().toLocaleString()
    urlDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取文件URL失败：' + (error.message || '未知错误'))
  }
}

const regenerateUrl = async () => {
  if (!currentUrlFile.value) return

  try {
    const url = await fileAPI.getFileUrl(currentUrlFile.value.id, urlExpireHours.value)
    currentFileUrl.value = String(url)
    currentUrlTime.value = new Date().toLocaleString()
    ElMessage.success('URL重新生成成功')
  } catch (error) {
    ElMessage.error('重新生成URL失败：' + (error.message || '未知错误'))
  }
}

const copyUrl = async () => {
  try {
    await navigator.clipboard.writeText(currentFileUrl.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = currentFileUrl.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制到剪贴板')
  }
}

const openUrl = () => {
  window.open(currentFileUrl.value, '_blank')
}

const logicalDeleteFile = async (file: FileRecord) => {
  try {
    await ElMessageBox.confirm('确定要逻辑删除此文件吗？文件将被标记为删除状态。', '逻辑删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await fileAPI.deleteFile(file.id, file.moduleName)
    ElMessage.success('文件逻辑删除成功')
    loadFileList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('文件逻辑删除失败：' + (error.message || '未知错误'))
    }
  }
}

const handleSelectionChange = (selection: unknown[]) => {
  selectedFiles.value = selection as FileRecord[]
}

const checkSelectedFilesExists = async () => {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请先选择要检查的文件')
    return
  }

  try {
    const fileIds = selectedFiles.value.map((file) => file.id)
    const existsMap = await fileAPI.checkFilesExists(fileIds)
    let existsCount = 0
    let notExistsCount = 0

    Object.values(existsMap || {}).forEach((exists) => {
      if (exists) {
        existsCount++
      } else {
        notExistsCount++
      }
    })

    ElMessage.success(`检查完成：${existsCount} 个文件存在，${notExistsCount} 个文件不存在`)
  } catch (error) {
    ElMessage.error('检查文件存在性失败：' + (error.message || '未知错误'))
  }
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`
}

const getFileIcon = (contentType?: string) => {
  if (!contentType) return Document
  if (contentType.startsWith('image/')) return Picture
  if (contentType.startsWith('video/')) return VideoPlay
  if (contentType.startsWith('audio/')) return Microphone
  return Document
}

const getFileTypeColor = (contentType?: string) => {
  if (!contentType) return 'var(--cn-color-text-tertiary)'
  if (contentType.startsWith('image/')) return 'var(--cn-color-success)'
  if (contentType.startsWith('video/')) return 'var(--cn-color-warning)'
  if (contentType.startsWith('audio/')) return 'var(--cn-color-danger)'
  return 'var(--cn-color-brand-primary)'
}

const getStorageTypeName = (type: string) => {
  return storageTypeConfig[type]?.name || type
}

const isImageFile = (contentType?: string) => {
  return Boolean(contentType && contentType.startsWith('image/'))
}

const isTextFile = (contentType?: string) => {
  if (!contentType) return false
  return (
    contentType.startsWith('text/') ||
    contentType.includes('json') ||
    contentType.includes('xml') ||
    contentType.includes('javascript') ||
    contentType.includes('css')
  )
}

onMounted(() => {
  loadFileList()
  loadStatistics()
  loadStorageConfigs()
})
</script>

<style scoped>
.file-management-page {
  min-height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-3);
}

.filter-control {
  width: 180px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  min-width: 0;
}

.file-icon {
  flex-shrink: 0;
  color: var(--file-tone, var(--cn-color-text-tertiary));
  font-size: 16px;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.dialog-footer {
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}

.current-file,
.info-panel {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.current-file {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.file-preview,
.url-content {
  max-height: 600px;
  overflow-y: auto;
}

.info-panel {
  margin-bottom: var(--cn-space-5);
}

.info-panel h3 {
  margin: 0 0 var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 17px;
  font-weight: 650;
}

.info-panel p {
  margin: var(--cn-space-1) 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.image-preview,
.other-preview {
  padding: var(--cn-space-5);
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: var(--cn-radius-card);
}

.text-preview {
  margin-top: var(--cn-space-4);
}

.preview-alert {
  margin-bottom: var(--cn-space-3);
}

.download-section,
.url-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-5);
}

.unit-text {
  margin-left: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.inline-action {
  margin-left: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .filter-control {
    width: 100%;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
