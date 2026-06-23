<template>
  <el-dialog
    v-model="visible"
    title="发布动态"
    width="600px"
    :before-close="handleBeforeClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
      <!-- 文字内容 -->
      <el-form-item prop="content">
        <el-input
          ref="textareaRef"
          v-model="form.content"
          type="textarea"
          placeholder="分享此刻的想法..."
          :rows="4"
          maxlength="100"
          show-word-limit
          @input="handleContentInput"
        />
      </el-form-item>

      <!-- 工具栏 -->
      <el-form-item>
        <div class="toolbar">
          <EmojiPicker @select="insertEmoji" />
        </div>
      </el-form-item>

      <!-- 图片上传 -->
      <el-form-item>
        <div class="upload-section">
          <div class="upload-title-row">
            <div class="upload-title">添加图片</div>
            <CnStatusTag type="info" size="sm">{{ form.images.length }} / 9</CnStatusTag>
          </div>
          <div class="image-upload-grid">
            <!-- 已上传的图片 -->
            <div 
              v-for="(image, index) in form.images" 
              :key="index" 
              class="image-item"
            >
              <el-image
                :src="image"
                fit="cover"
                class="uploaded-image"
              />
              <div class="image-overlay">
                <el-button 
                  type="danger" 
                  circle 
                  size="small" 
                  @click="removeImage(index)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            
            <!-- 上传按钮 -->
            <div 
              v-if="form.images.length < 9" 
              class="upload-item"
              @click="triggerUpload"
            >
              <el-icon class="upload-icon"><Plus /></el-icon>
              <div class="upload-text">添加图片</div>
            </div>
          </div>
          
          <div class="upload-tips">
            最多可添加9张图片，支持JPG、PNG格式，单张图片不超过5MB
          </div>
        </div>

        <!-- 隐藏的文件选择器 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          multiple
          style="display: none"
          @change="handleFileSelect"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button type="primary" @click="handlePublish" :loading="publishing">
        发布
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'
import { publishMoment } from '@/api/moment'
import { uploadSingle } from '@/api/upload'
import EmojiPicker from '@/components/EmojiPicker.vue'

interface DraftData {
  content?: string
  images?: string[]
  timestamp: number
}

interface UploadResult {
  accessUrl: string
}

const props = withDefaults(defineProps<{
  modelValue?: boolean
}>(), {
  modelValue: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'published'): void
}>()

// 响应式数据
const formRef = ref<FormInstance | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const textareaRef = ref()
const publishing = ref(false)

// 表单数据
const form = reactive({
  content: '',
  images: [] as string[]
})

// 草稿保存Key
const DRAFT_KEY = 'moment_publish_draft'
// 自动保存定时器
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

// 表单验证规则
const rules: FormRules = {
  content: [
    { required: true, message: '请输入动态内容', trigger: 'blur' },
    { min: 1, max: 100, message: '内容长度应在1-100字符之间', trigger: 'blur' }
  ]
}

// 对话框可见性
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 监听对话框打开，尝试恢复草稿
watch(visible, async (newVal) => {
  if (newVal) {
    // 检查是否有草稿
    const draft = loadDraft()
    if (draft) {
      try {
        await ElMessageBox.confirm(
          '检测到未发布的草稿，是否恢复？',
          '恢复草稿',
          {
            confirmButtonText: '恢复',
            cancelButtonText: '放弃',
            type: 'info'
          }
        )
        // 恢复草稿
        form.content = draft.content || ''
        form.images = draft.images || []
      } catch (error) {
        // 用户选择放弃草稿
        clearDraft()
        resetForm()
      }
    } else {
      resetForm()
    }
  } else {
    // 关闭时清除自动保存定时器
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
      autoSaveTimer = null
    }
  }
})

// 重置表单
const resetForm = () => {
  form.content = ''
  form.images = []
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// Element Plus的before-close处理函数
const handleBeforeClose = (done: () => void) => {
  publishing.value = false // 重置发布状态
  done() // 调用done()才能真正关闭弹窗
}

// 触发文件选择
const triggerUpload = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files || [])
  if (!files.length) return

  // 检查文件数量限制
  const remainingSlots = 9 - form.images.length
  if (files.length > remainingSlots) {
    ElMessage.warning(`最多只能再添加 ${remainingSlots} 张图片`)
    return
  }

  // 检查文件类型和大小
  const validFiles: File[] = []
  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning(`文件 ${file.name} 不是图片格式`)
      continue
    }
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning(`图片 ${file.name} 超过5MB限制`)
      continue
    }
    validFiles.push(file)
  }

  if (validFiles.length === 0) return

  // 上传图片
  try {
    const uploadPromises = validFiles.map(file => 
      uploadSingle(file, 'moment', 'image')
    )
    
    ElMessage.info('正在上传图片...')
    const results = await Promise.all(uploadPromises) as UploadResult[]
    
    // 添加到图片列表
    const newImages = results.map(result => result.accessUrl)
    form.images.push(...newImages)
    
    ElMessage.success(`成功上传 ${validFiles.length} 张图片`)
  } catch (error) {
    ElMessage.error('图片上传失败：' + (error instanceof Error ? error.message : '请稍后重试'))
  }

  // 清空文件选择器
  target.value = ''
}

// 删除图片
const removeImage = (index: number) => {
  form.images.splice(index, 1)
}

// 发布动态
const handlePublish = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    publishing.value = true
    
    const data = {
      content: form.content,
      images: form.images
    }
    
    await publishMoment(data)
    
    ElMessage.success('发布成功！')
    // 清除草稿
    clearDraft()
    emit('published')
    visible.value = false
  } catch (error) {
    ElMessage.error('发布失败：' + (error instanceof Error ? error.message : '未知错误'))
  } finally {
    publishing.value = false
  }
}

// 插入表情
const insertEmoji = (emoji: string) => {
  const textarea = textareaRef.value?.$refs?.textarea
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const text = form.content
    
    form.content = text.substring(0, start) + emoji + text.substring(end)
    
    // 恢复光标位置
    setTimeout(() => {
      textarea.selectionStart = textarea.selectionEnd = start + emoji.length
      textarea.focus()
    }, 10)
  } else {
    form.content += emoji
  }
}

// 内容输入时自动保存草稿
const handleContentInput = () => {
  // 清除之前的定时器
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  
  // 5秒后自动保存
  autoSaveTimer = setTimeout(() => {
    saveDraft()
  }, 5000)
}

// 保存草稿
const saveDraft = () => {
  if (!form.content && form.images.length === 0) {
    return
  }
  
  const draft = {
    content: form.content,
    images: form.images,
    timestamp: Date.now()
  }

  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

// 加载草稿
const loadDraft = (): DraftData | null => {
  try {
    const draftStr = localStorage.getItem(DRAFT_KEY)
    if (!draftStr) return null
    
    const draft = JSON.parse(draftStr)
    // 草稿超过24小时则丢弃
    if (Date.now() - draft.timestamp > 24 * 60 * 60 * 1000) {
      clearDraft()
      return null
    }
    
    return draft
  } catch (error) {
    console.error('加载草稿失败', error)
    return null
  }
}

// 清除草稿
const clearDraft = () => {
  localStorage.removeItem(DRAFT_KEY)
}
</script>

<style scoped>
:deep(.el-dialog) {
  border-radius: var(--cn-radius-panel);
  overflow: hidden;
  background: var(--cn-color-bg-surface);
}

:deep(.el-dialog__header) {
  background: var(--cn-color-bg-surface-muted);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  padding: var(--cn-space-4) var(--cn-space-5);
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--cn-color-text-primary);
}

:deep(.el-dialog__body) {
  padding: var(--cn-space-4) var(--cn-space-5);
}

:deep(.el-dialog__footer) {
  padding: var(--cn-space-3) var(--cn-space-5) var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
}

:deep(.el-textarea__inner) {
  border-radius: var(--cn-radius-control);
  border-color: var(--cn-color-border);
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

:deep(.el-textarea__inner:focus) {
  border-color: var(--cn-color-brand-primary);
  box-shadow: 0 0 0 3px var(--cn-color-focus-ring);
}

.upload-section {
  margin-top: var(--cn-space-3);
  width: 100%;
}

.upload-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
}

.upload-title {
  font-size: 14px;
  color: var(--cn-color-text-primary);
  font-weight: 600;
}

.image-upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 100px);
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-3);
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: var(--cn-radius-card);
  overflow: hidden;
  box-shadow: var(--cn-shadow-xs);
}

.uploaded-image {
  width: 100%;
  height: 100%;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: color-mix(in srgb, var(--cn-color-text-primary) 58%, transparent);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--cn-motion-fast) var(--cn-ease-out);
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.upload-item {
  width: 100px;
  height: 100px;
  border: 2px dashed var(--cn-color-border);
  border-radius: var(--cn-radius-card);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
  background: var(--cn-color-bg-surface-muted);
}

.upload-item:hover {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
  box-shadow: 0 0 0 3px var(--cn-color-focus-ring);
}

.upload-icon {
  font-size: 24px;
  color: var(--cn-color-text-secondary);
  margin-bottom: var(--cn-space-1);
}

.upload-text {
  font-size: 12px;
  color: var(--cn-color-text-secondary);
}

.upload-tips {
  font-size: 12px;
  color: var(--cn-color-text-tertiary);
  line-height: 1.5;
}

.toolbar {
  margin-bottom: var(--cn-space-3);
}

@media (prefers-reduced-motion: reduce) {
  :deep(.el-textarea__inner),
  .image-overlay,
  .upload-item {
    transition: none;
  }
}
</style>
