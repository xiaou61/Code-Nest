<template>
  <CnPage class="system-settings-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="文件系统设置"
      description="配置文件上传限制、安全策略、存储策略、性能策略和文件类型白名单。"
      eyebrow="File System Settings"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">最大 {{ systemSummary.maxFileSize || uploadSettings.maxFileSize }} MB</CnStatusTag>
        <CnStatusTag type="success">白名单 {{ allowedFileTypes.length }} 种</CnStatusTag>
        <CnStatusTag type="warning">临时链接 {{ securitySettings.tempLinkExpireHours }} 小时</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" @click="loadSettings">刷新</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="saveSettings">保存设置</el-button>
      </template>
    </CnPageHeader>

    <div v-if="systemSummary" class="summary-grid">
      <CnStatCard
        title="最大文件大小"
        :value="systemSummary.maxFileSize || 0"
        unit="MB"
        description="单文件上传大小上限"
        tone="brand"
      />
      <CnStatCard
        title="模块存储配额"
        :value="systemSummary.moduleStorageQuota || 0"
        unit="GB"
        description="单模块可用存储容量"
        tone="success"
      />
      <CnStatCard
        title="临时链接有效期"
        :value="systemSummary.tempLinkExpireHours || 0"
        unit="小时"
        description="文件临时访问链接时长"
        tone="warning"
      />
      <CnStatCard
        title="允许文件类型"
        :value="systemSummary.allowedFileTypesCount || allowedFileTypes.length"
        unit="种"
        description="当前白名单扩展名数量"
        tone="info"
      />
    </div>

    <div class="settings-grid">
      <CnSection title="上传限制设置" description="控制单文件大小、批量数量、配额和上传前处理。" divided>
        <el-form :model="uploadSettings" label-width="150px">
          <el-form-item label="最大文件大小">
            <el-input-number v-model="uploadSettings.maxFileSize" :min="1" :max="1024" :step="1" controls-position="right" />
            <span class="unit-text">MB</span>
          </el-form-item>
          <el-form-item label="单次上传文件数">
            <el-input-number v-model="uploadSettings.maxFilesPerUpload" :min="1" :max="100" :step="1" controls-position="right" />
            <span class="unit-text">个</span>
          </el-form-item>
          <el-form-item label="模块存储配额">
            <el-input-number
              v-model="uploadSettings.moduleStorageQuota"
              :min="1"
              :max="10000"
              :step="1"
              controls-position="right"
            />
            <span class="unit-text">GB</span>
          </el-form-item>
          <el-form-item label="启用重复检测">
            <el-switch v-model="uploadSettings.enableDuplicateCheck" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="自动压缩图片">
            <el-switch v-model="uploadSettings.autoCompressImage" active-text="启用" inactive-text="禁用" />
          </el-form-item>
        </el-form>
      </CnSection>

      <CnSection title="安全设置" description="控制默认权限、临时链接、扫描和访问日志策略。" divided>
        <el-form :model="securitySettings" label-width="150px">
          <el-form-item label="默认访问权限">
            <el-radio-group v-model="securitySettings.defaultAccessLevel">
              <el-radio value="private">私有</el-radio>
              <el-radio value="public">公开</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="临时链接有效期">
            <el-input-number
              v-model="securitySettings.tempLinkExpireHours"
              :min="1"
              :max="168"
              :step="1"
              controls-position="right"
            />
            <span class="unit-text">小时</span>
          </el-form-item>
          <el-form-item label="启用病毒扫描">
            <el-switch v-model="securitySettings.enableVirusScan" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="记录访问日志">
            <el-switch v-model="securitySettings.enableAccessLog" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="IP访问限制">
            <el-switch v-model="securitySettings.enableIpRestriction" active-text="启用" inactive-text="禁用" />
          </el-form-item>
        </el-form>
      </CnSection>

      <CnSection title="存储设置" description="配置备份、分片上传和备份保留策略。" divided>
        <el-form :model="storageSettings" label-width="150px">
          <el-form-item label="启用自动备份">
            <el-switch v-model="storageSettings.autoBackupEnabled" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="备份存储类型">
            <el-select v-model="storageSettings.backupStorageType" placeholder="请选择备份存储" class="full-width">
              <el-option label="本地存储" value="LOCAL" />
              <el-option label="阿里云OSS" value="OSS" />
              <el-option label="腾讯云COS" value="COS" />
              <el-option label="七牛云KODO" value="KODO" />
              <el-option label="华为云OBS" value="OBS" />
            </el-select>
          </el-form-item>
          <el-form-item label="本地备份保留天数">
            <el-input-number
              v-model="storageSettings.localBackupRetentionDays"
              :min="1"
              :max="365"
              :step="1"
              controls-position="right"
            />
            <span class="unit-text">天</span>
          </el-form-item>
          <el-form-item label="启用分片上传">
            <el-switch v-model="storageSettings.enableChunkUpload" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="分片大小">
            <el-input-number
              v-model="storageSettings.chunkSize"
              :min="1"
              :max="100"
              :step="1"
              controls-position="right"
              :disabled="!storageSettings.enableChunkUpload"
            />
            <span class="unit-text">MB</span>
          </el-form-item>
        </el-form>
      </CnSection>

      <CnSection title="性能设置" description="控制缓存、并发上传和缩略图生成。" divided>
        <el-form :model="performanceSettings" label-width="150px">
          <el-form-item label="启用缓存">
            <el-switch v-model="performanceSettings.enableCache" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="缓存过期时间">
            <el-input-number
              v-model="performanceSettings.cacheExpireMinutes"
              :min="5"
              :max="1440"
              :step="5"
              controls-position="right"
              :disabled="!performanceSettings.enableCache"
            />
            <span class="unit-text">分钟</span>
          </el-form-item>
          <el-form-item label="并发上传数">
            <el-input-number v-model="performanceSettings.maxConcurrentUploads" :min="1" :max="10" :step="1" controls-position="right" />
          </el-form-item>
          <el-form-item label="缩略图生成">
            <el-switch v-model="performanceSettings.enableThumbnail" active-text="启用" inactive-text="禁用" />
          </el-form-item>
          <el-form-item label="缩略图大小">
            <el-select v-model="performanceSettings.thumbnailSize" :disabled="!performanceSettings.enableThumbnail" class="full-width">
              <el-option label="128x128" value="128" />
              <el-option label="256x256" value="256" />
              <el-option label="512x512" value="512" />
            </el-select>
          </el-form-item>
        </el-form>
      </CnSection>
    </div>

    <CnSection title="文件类型白名单" description="按类型维护允许上传的文件扩展名。" divided>
      <template #actions>
        <el-button type="primary" size="small" @click="showAddFileTypeDialog">添加类型</el-button>
      </template>

      <div class="file-types-grid">
        <div v-for="(types, category) in fileTypesByCategory" :key="category" class="file-type-category">
          <h4>{{ getCategoryName(category) }}</h4>
          <div class="file-type-list">
            <CnStatusTag v-for="type in types" :key="type" type="info" size="sm">
              <span class="file-type-chip">
                {{ type }}
                <button type="button" class="chip-close" @click="removeFileType(type)">×</button>
              </span>
            </CnStatusTag>
            <CnStatusTag v-if="!types.length" type="neutral" size="sm" subtle>暂无</CnStatusTag>
          </div>
        </div>
      </div>
    </CnSection>

    <el-dialog v-model="fileTypeDialogVisible" title="添加文件类型" width="500px">
      <el-form :model="fileTypeForm" label-width="100px">
        <el-form-item label="文件类型">
          <el-input v-model="fileTypeForm.extension" placeholder="如: jpg, pdf, doc" />
          <div class="form-tip">请输入文件扩展名，不包含点号</div>
        </el-form-item>
        <el-form-item label="MIME类型">
          <el-input v-model="fileTypeForm.mimeType" placeholder="如: image/jpeg, application/pdf" />
          <div class="form-tip">可选，用于更精确的类型检查</div>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="fileTypeForm.category" placeholder="请选择类别" class="full-width">
            <el-option label="图片" value="image" />
            <el-option label="文档" value="document" />
            <el-option label="视频" value="video" />
            <el-option label="音频" value="audio" />
            <el-option label="压缩包" value="archive" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="fileTypeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="addFileType">添加</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Refresh } from '@element-plus/icons-vue'
import { systemAPI } from '@/api/filestorage'
import { CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'

interface SystemSummary {
  maxFileSize?: number
  moduleStorageQuota?: number
  tempLinkExpireHours?: number
  allowedFileTypesCount?: number
}

interface FileTypeCategory {
  name: string
  extensions: string[]
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '文件存储' }, { label: '系统设置' }]

const saving = ref(false)
const systemSummary = ref<SystemSummary>({})
const allowedFileTypes = ref<string[]>([])
const fileTypeDialogVisible = ref(false)

const uploadSettings = reactive({
  maxFileSize: 100,
  maxFilesPerUpload: 10,
  moduleStorageQuota: 1000,
  enableDuplicateCheck: true,
  autoCompressImage: true
})

const securitySettings = reactive({
  defaultAccessLevel: 'private',
  tempLinkExpireHours: 24,
  enableVirusScan: false,
  enableAccessLog: true,
  enableIpRestriction: false
})

const storageSettings = reactive({
  autoBackupEnabled: true,
  backupStorageType: 'LOCAL',
  localBackupRetentionDays: 30,
  enableChunkUpload: false,
  chunkSize: 5
})

const performanceSettings = reactive({
  enableCache: true,
  cacheExpireMinutes: 60,
  maxConcurrentUploads: 3,
  enableThumbnail: true,
  thumbnailSize: '256'
})

const fileTypeForm = reactive({
  extension: '',
  mimeType: '',
  category: ''
})

const fileTypeCategories: Record<string, FileTypeCategory> = {
  image: { name: '图片', extensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'] },
  document: { name: '文档', extensions: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'] },
  video: { name: '视频', extensions: ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'webm'] },
  audio: { name: '音频', extensions: ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'] },
  archive: { name: '压缩包', extensions: ['zip', 'rar', '7z', 'tar', 'gz'] },
  other: { name: '其他', extensions: [] }
}

const fileTypesByCategory = computed<Record<string, string[]>>(() => {
  const result: Record<string, string[]> = {}

  Object.keys(fileTypeCategories).forEach((category) => {
    result[category] = allowedFileTypes.value.filter((type) => {
      if (category === 'other') {
        return !Object.values(fileTypeCategories).some((cat) => cat.extensions.includes(type))
      }
      return fileTypeCategories[category].extensions.includes(type)
    })
  })

  return result
})

const loadSettings = async () => {
  try {
    const data = await systemAPI.getSystemSettings()

    Object.keys(data || {}).forEach((key) => {
      const value = data[key]

      if (key === 'maxFileSize') uploadSettings.maxFileSize = parseInt(value)
      if (key === 'maxFilesPerUpload') uploadSettings.maxFilesPerUpload = parseInt(value)
      if (key === 'moduleStorageQuota') uploadSettings.moduleStorageQuota = parseInt(value)
      if (key === 'enableDuplicateCheck') uploadSettings.enableDuplicateCheck = value === 'true'
      if (key === 'autoCompressImage') uploadSettings.autoCompressImage = value === 'true'

      if (key === 'defaultAccessLevel') securitySettings.defaultAccessLevel = value
      if (key === 'tempLinkExpireHours') securitySettings.tempLinkExpireHours = parseInt(value)
      if (key === 'enableVirusScan') securitySettings.enableVirusScan = value === 'true'
      if (key === 'enableAccessLog') securitySettings.enableAccessLog = value === 'true'
      if (key === 'enableIpRestriction') securitySettings.enableIpRestriction = value === 'true'

      if (key === 'autoBackupEnabled') storageSettings.autoBackupEnabled = value === 'true'
      if (key === 'backupStorageType') storageSettings.backupStorageType = value
      if (key === 'localBackupRetentionDays') storageSettings.localBackupRetentionDays = parseInt(value)
      if (key === 'enableChunkUpload') storageSettings.enableChunkUpload = value === 'true'
      if (key === 'chunkSize') storageSettings.chunkSize = parseInt(value)

      if (key === 'enableCache') performanceSettings.enableCache = value === 'true'
      if (key === 'cacheExpireMinutes') performanceSettings.cacheExpireMinutes = parseInt(value)
      if (key === 'maxConcurrentUploads') performanceSettings.maxConcurrentUploads = parseInt(value)
      if (key === 'enableThumbnail') performanceSettings.enableThumbnail = value === 'true'
      if (key === 'thumbnailSize') performanceSettings.thumbnailSize = value
    })
  } catch (error) {
    console.error('获取系统设置失败:', error)
  }
}

const loadSystemSummary = async () => {
  try {
    const data = await systemAPI.getSystemSummary()
    systemSummary.value = data || {}
  } catch (error) {
    console.error('获取系统概览失败:', error)
  }
}

const loadFileTypes = async () => {
  try {
    const data = await systemAPI.getFileTypes()
    allowedFileTypes.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('获取文件类型失败:', error)
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    const allSettings = {
      maxFileSize: uploadSettings.maxFileSize.toString(),
      maxFilesPerUpload: uploadSettings.maxFilesPerUpload.toString(),
      moduleStorageQuota: uploadSettings.moduleStorageQuota.toString(),
      enableDuplicateCheck: uploadSettings.enableDuplicateCheck.toString(),
      autoCompressImage: uploadSettings.autoCompressImage.toString(),
      defaultAccessLevel: securitySettings.defaultAccessLevel,
      tempLinkExpireHours: securitySettings.tempLinkExpireHours.toString(),
      enableVirusScan: securitySettings.enableVirusScan.toString(),
      enableAccessLog: securitySettings.enableAccessLog.toString(),
      enableIpRestriction: securitySettings.enableIpRestriction.toString(),
      autoBackupEnabled: storageSettings.autoBackupEnabled.toString(),
      backupStorageType: storageSettings.backupStorageType,
      localBackupRetentionDays: storageSettings.localBackupRetentionDays.toString(),
      enableChunkUpload: storageSettings.enableChunkUpload.toString(),
      chunkSize: storageSettings.chunkSize.toString(),
      enableCache: performanceSettings.enableCache.toString(),
      cacheExpireMinutes: performanceSettings.cacheExpireMinutes.toString(),
      maxConcurrentUploads: performanceSettings.maxConcurrentUploads.toString(),
      enableThumbnail: performanceSettings.enableThumbnail.toString(),
      thumbnailSize: performanceSettings.thumbnailSize
    }

    await systemAPI.updateSystemSettings(allSettings)
    ElMessage.success('系统设置保存成功')
    loadSystemSummary()
  } catch (error) {
    ElMessage.error('保存系统设置失败：' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

const showAddFileTypeDialog = () => {
  Object.assign(fileTypeForm, {
    extension: '',
    mimeType: '',
    category: ''
  })
  fileTypeDialogVisible.value = true
}

const addFileType = async () => {
  if (!fileTypeForm.extension.trim()) {
    ElMessage.warning('请输入文件扩展名')
    return
  }

  const extension = fileTypeForm.extension.toLowerCase().trim()

  if (allowedFileTypes.value.includes(extension)) {
    ElMessage.warning('该文件类型已存在')
    return
  }

  try {
    const newFileTypes = [...allowedFileTypes.value, extension]
    await systemAPI.updateFileTypes(newFileTypes)
    allowedFileTypes.value = newFileTypes
    fileTypeDialogVisible.value = false
    ElMessage.success('添加文件类型成功')
  } catch (error) {
    ElMessage.error('添加文件类型失败：' + (error.message || '未知错误'))
  }
}

const removeFileType = async (extension: string) => {
  try {
    await ElMessageBox.confirm('确定要删除此文件类型吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const newFileTypes = allowedFileTypes.value.filter((type) => type !== extension)
    await systemAPI.updateFileTypes(newFileTypes)
    allowedFileTypes.value = newFileTypes
    ElMessage.success('删除文件类型成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除文件类型失败：' + (error.message || '未知错误'))
    }
  }
}

const getCategoryName = (category: string) => {
  return fileTypeCategories[category]?.name || category
}

onMounted(() => {
  loadSettings()
  loadSystemSummary()
  loadFileTypes()
})
</script>

<style scoped>
.system-settings-page {
  min-height: 100%;
}

.summary-grid,
.settings-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.settings-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.unit-text {
  margin-left: var(--cn-space-2);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.file-types-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--cn-space-4);
}

.file-type-category {
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.file-type-category h4 {
  margin: 0 0 var(--cn-space-3);
  padding-bottom: var(--cn-space-2);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.file-type-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.file-type-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.chip-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  border: 0;
  border-radius: var(--cn-radius-pill);
  background: transparent;
  color: currentColor;
  cursor: pointer;
  font: inherit;
  line-height: 1;
}

.chip-close:hover {
  background: color-mix(in srgb, currentColor 12%, transparent);
}

.form-tip {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
