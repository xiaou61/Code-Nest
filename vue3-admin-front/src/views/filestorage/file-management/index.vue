<template>
  <div class="file-management">
    <div class="header">
      <h2>文件管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
        <el-button type="success" @click="checkSelectedFilesExists" :disabled="!selectedFiles.length">
          <el-icon><View /></el-icon>
          检查文件
        </el-button>
        <el-button @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="info" @click="showStatistics">
          <el-icon><DataAnalysis /></el-icon>
          统计信息
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards" v-if="showStats">
      <el-row :gutter="16">
        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.totalFiles || 0 }}</div>
              <div class="stat-label">总文件数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ formatFileSize(statistics.totalStorageSize || 0) }}</div>
              <div class="stat-label">总存储量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.todayFiles || 0 }}</div>
              <div class="stat-label">今日上传</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="5">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.storageConfigs || 0 }}</div>
              <div class="stat-label">存储配置数</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card class="stat-card">
            <div class="stat-content">
              <div class="stat-value">{{ statistics.deletedFiles || 0 }}</div>
              <div class="stat-label">已删除文件</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 筛选条件 -->
    <div class="filters">
      <el-form :model="queryParams" inline>
        <el-form-item label="模块名称：">
          <el-input v-model="queryParams.moduleName" placeholder="请输入模块名称" clearable />
        </el-form-item>
        <el-form-item label="业务类型：">
          <el-input v-model="queryParams.businessType" placeholder="请输入业务类型" clearable />
        </el-form-item>
        <el-form-item label="文件类型：">
          <el-select v-model="queryParams.contentType" placeholder="请选择文件类型" clearable>
            <el-option label="全部" value="" />
            <el-option label="图片" value="image" />
            <el-option label="文档" value="document" />
            <el-option label="视频" value="video" />
            <el-option label="音频" value="audio" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态：">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
    </div>

    <!-- 数据表格 -->
    <el-table 
      v-loading="loading" 
      :data="fileList" 
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="id" label="文件ID" width="80" />
      <el-table-column prop="originalName" label="文件名称" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="file-info">
            <el-icon class="file-icon" :style="{ color: getFileTypeColor(row.contentType) }">
              <component :is="getFileIcon(row.contentType)" />
            </el-icon>
            <span>{{ row.originalName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="文件大小" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="contentType" label="文件类型" width="150" show-overflow-tooltip />
      <el-table-column prop="moduleName" label="所属模块" width="120" />
      <el-table-column prop="businessType" label="业务类型" width="120" />
      <el-table-column prop="isPublic" label="访问权限" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPublic === 1 ? 'success' : 'warning'">
            {{ row.isPublic === 1 ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '已删除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="previewFile(row)">
            预览
          </el-button>
          <el-button type="success" size="small" @click="getFileUrl(row)">
            获取链接
          </el-button>
          <el-button type="warning" size="small" @click="logicalDeleteFile(row)">
            逻辑删除
          </el-button>
          <el-button type="info" size="small" @click="showMoveDialog(row)">
            移动
          </el-button>
          <el-button type="danger" size="small" @click="forceDeleteFile(row)">
            物理删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 文件移动对话框 -->
    <el-dialog v-model="moveDialogVisible" title="移动文件" width="500px">
      <el-form :model="moveForm" label-width="120px">
        <el-form-item label="当前文件：">
          <div class="current-file">
            <el-icon><Document /></el-icon>
            {{ currentMoveFile?.originalName }}
          </div>
        </el-form-item>
        <el-form-item label="目标存储：" required>
          <el-select v-model="moveForm.targetStorageId" placeholder="请选择目标存储配置">
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

    <!-- 文件上传对话框 -->
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
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
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
          <el-button type="primary" @click="handleUploadFiles" :loading="uploading">
            {{ uploading ? '上传中...' : '开始上传' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文件URL对话框 -->
    <el-dialog v-model="urlDialogVisible" title="文件访问链接" width="600px">
      <div v-if="currentFileUrl" class="url-content">
        <div class="url-info">
          <h3>{{ currentUrlFile?.originalName }}</h3>
          <p>文件ID: {{ currentUrlFile?.id }}</p>
          <p>生成时间: {{ currentUrlTime }}</p>
        </div>
        
        <el-form label-width="120px">
          <el-form-item label="有效期设置">
            <el-input-number
              v-model="urlExpireHours"
              :min="1"
              :max="168"
              controls-position="right"
            />
            <span class="unit-text">小时</span>
            <el-button type="primary" size="small" @click="regenerateUrl" style="margin-left: 10px;">
              重新生成
            </el-button>
          </el-form-item>
          <el-form-item label="访问链接">
            <el-input
              v-model="currentFileUrl"
              readonly
              type="textarea"
              :rows="3"
            />
          </el-form-item>
        </el-form>
        
        <div class="url-actions">
          <el-button type="success" @click="copyUrl">
            <el-icon><DocumentCopy /></el-icon>
            复制链接
          </el-button>
          <el-button type="primary" @click="openUrl">
            <el-icon><Link /></el-icon>
            打开链接
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog v-model="previewDialogVisible" title="文件预览" width="800px">
      <div class="file-preview">
        <div class="preview-info">
          <h3>{{ currentPreviewFile?.originalName }}</h3>
          <p>文件大小: {{ formatFileSize(currentPreviewFile?.fileSize) }}</p>
          <p>上传时间: {{ currentPreviewFile?.uploadTime }}</p>
          <p>MD5校验: {{ currentPreviewFile?.md5Hash }}</p>
        </div>
        
        <!-- 图片预览 -->
        <div v-if="isImageFile(currentPreviewFile?.contentType)" class="image-preview">
          <img :src="currentPreviewFile?.accessUrl" alt="预览图片" style="max-width: 100%; max-height: 400px;" />
        </div>
        
        <!-- 文本预览 -->
        <div v-else-if="isTextFile(currentPreviewFile?.contentType)" v-loading="previewLoading" class="text-preview">
          <el-alert
            v-if="previewError"
            :title="previewError"
            type="warning"
            show-icon
            :closable="false"
            class="preview-alert"
          />
          <el-input
            v-model="previewContent"
            type="textarea"
            :rows="10"
            readonly
            placeholder="文本内容预览..."
          />
        </div>
        
        <!-- 其他文件类型 -->
        <div v-else class="other-preview">
          <el-alert
            title="此文件类型不支持预览"
            type="info"
            :closable="false"
          />
          <div class="download-section">
            <el-button type="primary" @click="downloadFile(currentPreviewFile)">
              <el-icon><Download /></el-icon>
              下载文件
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  DataAnalysis, 
  Document, 
  Picture, 
  VideoPlay, 
  Microphone, 
  Download,
  Upload,
  View,
  UploadFilled,
  DocumentCopy,
  Link
} from '@element-plus/icons-vue'
import { fileAPI, storageAPI } from '@/api/filestorage'

// 响应式数据
const loading = ref(false)
const fileList = ref([])
const total = ref(0)
const showStats = ref(true)
const statistics = ref({})
const storageConfigs = ref([])

// 对话框状态
const moveDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const urlDialogVisible = ref(false)
const currentMoveFile = ref(null)
const currentPreviewFile = ref(null)
const currentUrlFile = ref(null)
const previewContent = ref('')
const previewError = ref('')
const previewLoading = ref(false)
const currentFileUrl = ref('')
const currentUrlTime = ref('')
const uploading = ref(false)

// 文件选择
const selectedFiles = ref([])

// 上传相关
const uploadRef = ref()
const uploadFileList = ref([])
const uploadForm = reactive({
  moduleName: '',
  businessType: '',
  uploadType: 'single'
})

// URL相关
const urlExpireHours = ref(24)
const TEXT_PREVIEW_MAX_SIZE = 1024 * 1024

// 查询参数
const queryParams = reactive({
  moduleName: '',
  businessType: '',
  contentType: '',
  status: '',
  pageNum: 1,
  pageSize: 20
})

// 移动文件表单
const moveForm = reactive({
  targetStorageId: null
})

// 存储类型配置
const storageTypeConfig = {
  'LOCAL': { name: '本地存储', color: 'primary' },
  'OSS': { name: '阿里云OSS', color: 'success' },
  'COS': { name: '腾讯云COS', color: 'warning' },
  'KODO': { name: '七牛云KODO', color: 'info' },
  'OBS': { name: '华为云OBS', color: 'danger' }
}

// 生命周期
onMounted(() => {
  loadFileList()
  loadStatistics()
  loadStorageConfigs()
})

// 方法
const loadFileList = async () => {
  loading.value = true
  try {
    const data = await fileAPI.getFileList(queryParams)
    fileList.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error('获取文件列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const data = await fileAPI.getFileStatistics()
    statistics.value = data
  } catch (error) {
    console.error('获取统计信息失败:', error)
  }
}

const loadStorageConfigs = async () => {
  try {
    const data = await storageAPI.getStorageConfigs({ isEnabled: 1 })
    storageConfigs.value = data
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

const handleSizeChange = (size) => {
  queryParams.pageSize = size
  queryParams.pageNum = 1
  loadFileList()
}

const handleCurrentChange = (page) => {
  queryParams.pageNum = page
  loadFileList()
}

// 文件操作
const previewFile = async (file) => {
  currentPreviewFile.value = file
  previewContent.value = ''
  previewError.value = ''
  previewLoading.value = false
  previewDialogVisible.value = true
  
  // 如果是文本文件，尝试加载内容
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

const showMoveDialog = (file) => {
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
    await fileAPI.moveFile(currentMoveFile.value.id, moveForm.targetStorageId)
    ElMessage.success('文件移动成功')
    moveDialogVisible.value = false
    loadFileList()
  } catch (error) {
    ElMessage.error('文件移动失败：' + error.message)
  }
}

const forceDeleteFile = async (file) => {
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
      ElMessage.error('文件删除失败：' + error.message)
    }
  }
}

const downloadFile = async (file) => {
  try {
    // 使用新的下载API
    const response = await fileAPI.downloadFile(file.id)
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', file.originalName)
    document.body.appendChild(link)
    link.click()
    
    // 清理
    link.remove()
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('文件下载成功')
  } catch (error) {
    ElMessage.error('文件下载失败：' + error.message)
  }
}

// === 新增功能方法 ===

const readBlobAsText = async (blob) => {
  if (blob && typeof blob.text === 'function') {
    return blob.text()
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result || '')
    reader.onerror = () => reject(new Error('文本读取失败'))
    reader.readAsText(blob, 'UTF-8')
  })
}

// 显示上传对话框
const showUploadDialog = () => {
  Object.assign(uploadForm, {
    moduleName: '',
    businessType: '',
    uploadType: 'single'
  })
  uploadFileList.value = []
  uploadDialogVisible.value = true
}

// 处理文件选择变化
const handleFileChange = (file, fileList) => {
  if (uploadForm.uploadType === 'single' && fileList.length > 1) {
    uploadFileList.value = [fileList[fileList.length - 1]]
  } else {
    uploadFileList.value = fileList
  }
}

// 处理文件移除
const handleFileRemove = (file, fileList) => {
  uploadFileList.value = fileList
}

// 处理文件上传
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
    const files = uploadFileList.value.map(item => item.raw)
    
    if (uploadForm.uploadType === 'single') {
      // 单文件上传
      await fileAPI.uploadSingle(
        files[0], 
        uploadForm.moduleName, 
        uploadForm.businessType || 'default'
      )
      ElMessage.success('文件上传成功')
    } else {
      // 批量上传
      const results = await fileAPI.uploadBatch(
        files, 
        uploadForm.moduleName, 
        uploadForm.businessType || 'default'
      )
      ElMessage.success(`批量上传成功，共上传 ${results.length} 个文件`)
    }
    
    uploadDialogVisible.value = false
    loadFileList()
    loadStatistics()
  } catch (error) {
    ElMessage.error('文件上传失败：' + error.message)
  } finally {
    uploading.value = false
  }
}

// 获取文件URL
const getFileUrl = async (file) => {
  try {
    const url = await fileAPI.getFileUrl(file.id, urlExpireHours.value)
    currentFileUrl.value = url
    currentUrlFile.value = file
    currentUrlTime.value = new Date().toLocaleString()
    urlDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取文件URL失败：' + error.message)
  }
}

// 重新生成URL
const regenerateUrl = async () => {
  if (!currentUrlFile.value) return
  
  try {
    const url = await fileAPI.getFileUrl(currentUrlFile.value.id, urlExpireHours.value)
    currentFileUrl.value = url
    currentUrlTime.value = new Date().toLocaleString()
    ElMessage.success('URL重新生成成功')
  } catch (error) {
    ElMessage.error('重新生成URL失败：' + error.message)
  }
}

// 复制URL
const copyUrl = async () => {
  try {
    await navigator.clipboard.writeText(currentFileUrl.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    // 降级处理
    const textarea = document.createElement('textarea')
    textarea.value = currentFileUrl.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制到剪贴板')
  }
}

// 打开URL
const openUrl = () => {
  window.open(currentFileUrl.value, '_blank')
}

// 逻辑删除文件
const logicalDeleteFile = async (file) => {
  try {
    await ElMessageBox.confirm('确定要逻辑删除此文件吗？文件将被标记为删除状态。', '逻辑删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 需要提供模块名称，这里使用文件的模块名称
    await fileAPI.deleteFile(file.id, file.moduleName)
    ElMessage.success('文件逻辑删除成功')
    loadFileList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('文件逻辑删除失败：' + error.message)
    }
  }
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedFiles.value = selection
}

// 检查选中文件是否存在
const checkSelectedFilesExists = async () => {
  if (!selectedFiles.value.length) {
    ElMessage.warning('请先选择要检查的文件')
    return
  }
  
  try {
    const fileIds = selectedFiles.value.map(file => file.id)
    const existsMap = await fileAPI.checkFilesExists(fileIds)
    
    let existsCount = 0
    let notExistsCount = 0
    
    Object.values(existsMap).forEach(exists => {
      if (exists) {
        existsCount++
      } else {
        notExistsCount++
      }
    })
    
    ElMessage.success(`检查完成：${existsCount} 个文件存在，${notExistsCount} 个文件不存在`)
  } catch (error) {
    ElMessage.error('检查文件存在性失败：' + error.message)
  }
}

// 工具方法
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileIcon = (contentType) => {
  if (!contentType) return Document
  if (contentType.startsWith('image/')) return Picture
  if (contentType.startsWith('video/')) return VideoPlay
  if (contentType.startsWith('audio/')) return Microphone
  return Document
}

const getFileTypeColor = (contentType) => {
  if (!contentType) return '#909399'
  if (contentType.startsWith('image/')) return '#67C23A'
  if (contentType.startsWith('video/')) return '#E6A23C'
  if (contentType.startsWith('audio/')) return '#F56C6C'
  return '#409EFF'
}

const getStorageTypeName = (type) => {
  return storageTypeConfig[type]?.name || type
}

const isImageFile = (contentType) => {
  return contentType && contentType.startsWith('image/')
}

const isTextFile = (contentType) => {
  if (!contentType) return false
  return contentType.startsWith('text/') || 
         contentType.includes('json') || 
         contentType.includes('xml') ||
         contentType.includes('javascript') ||
         contentType.includes('css')
}
</script>

<style scoped>
.file-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-content {
  padding: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.filters {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  font-size: 16px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.current-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.file-preview {
  max-height: 600px;
  overflow-y: auto;
}

.preview-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.preview-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.preview-info p {
  margin: 5px 0;
  color: #606266;
}

.image-preview {
  text-align: center;
  padding: 20px;
}

.text-preview {
  margin-top: 15px;
}

.preview-alert {
  margin-bottom: 12px;
}

.other-preview {
  text-align: center;
  padding: 40px 20px;
}

.download-section {
  margin-top: 20px;
}

.dialog-footer {
  text-align: right;
}

.url-content {
  max-height: 500px;
  overflow-y: auto;
}

.url-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.url-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
}

.url-info p {
  margin: 5px 0;
  color: #606266;
}

.url-actions {
  margin-top: 20px;
  text-align: center;
}

.url-actions .el-button {
  margin: 0 10px;
}

.unit-text {
  margin-left: 10px;
  color: #909399;
}
</style>
