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
          <div class="upload-title">添加图片</div>
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

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { publishMoment } from '@/api/moment'
import { uploadSingle } from '@/api/upload'
import EmojiPicker from '@/components/EmojiPicker.vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'published'])

// 响应式数据
const formRef = ref(null)
const fileInputRef = ref(null)
const textareaRef = ref(null)
const publishing = ref(false)

// 表单数据
const form = reactive({
  content: '',
  images: []
})

// 草稿保存Key
const DRAFT_KEY = 'moment_publish_draft'
// 自动保存定时器
let autoSaveTimer = null

// 表单验证规则
const rules = {
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
const handleBeforeClose = (done) => {
  publishing.value = false // 重置发布状态
  done() // 调用done()才能真正关闭弹窗
}

// 触发文件选择
const triggerUpload = () => {
  fileInputRef.value.click()
}

// 处理文件选择
const handleFileSelect = async (event) => {
  const files = Array.from(event.target.files)
  if (!files.length) return

  // 检查文件数量限制
  const remainingSlots = 9 - form.images.length
  if (files.length > remainingSlots) {
    ElMessage.warning(`最多只能再添加 ${remainingSlots} 张图片`)
    return
  }

  // 检查文件类型和大小
  const validFiles = []
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
    const results = await Promise.all(uploadPromises)
    
    // 添加到图片列表
    const newImages = results.map(result => result.accessUrl)
    form.images.push(...newImages)
    
    ElMessage.success(`成功上传 ${validFiles.length} 张图片`)
  } catch (error) {
    ElMessage.error('图片上传失败：' + error.message)
  }

  // 清空文件选择器
  event.target.value = ''
}

// 删除图片
const removeImage = (index) => {
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
    ElMessage.error('发布失败：' + (error.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}

// 插入表情
const insertEmoji = (emoji) => {
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
  console.log('草稿已自动保存')
}

// 加载草稿
const loadDraft = () => {
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
/* 对话框装饰 */
:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #f0f4ff 0%, #f5f0ff 50%, #fff5f7 100%);
  border-bottom: 1px solid #e3edfa;
  padding: 16px 20px;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 700;
  color: var(--cn-text-primary, #1a2233);
}

:deep(.el-dialog__body) {
  padding: 16px 20px;
}

:deep(.el-dialog__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #e8eef8;
}

/* textarea 美化 */
:deep(.el-textarea__inner) {
  border-radius: 10px;
  border-color: #d7e4f8;
  transition: all 0.25s;
}

:deep(.el-textarea__inner:focus) {
  border-color: #6c63ff;
  box-shadow: 0 0 0 3px rgba(108, 99, 255, 0.1);
}

/* 发布按钮 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #6c63ff 0%, #4f46e5 100%);
  border: none;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.25);
  transition: all 0.3s;
}

:deep(.el-button--primary:hover) {
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.35);
  transform: translateY(-1px);
}

.upload-section {
  margin-top: 10px;
}

.upload-title {
  font-size: 14px;
  color: var(--cn-text-primary, #1a2233);
  font-weight: 600;
  margin-bottom: 10px;
}

.image-upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 100px);
  gap: 10px;
  margin-bottom: 10px;
}

.image-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(18, 38, 63, 0.08);
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
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s;
}

.image-item:hover .image-overlay {
  opacity: 1;
}

.upload-item {
  width: 100px;
  height: 100px;
  border: 2px dashed #c2d8f5;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s;
  background: #f8fbff;
}

.upload-item:hover {
  border-color: #6c63ff;
  background: #f5f3ff;
  box-shadow: 0 0 0 3px rgba(108, 99, 255, 0.08);
}

.upload-icon {
  font-size: 24px;
  color: #6a82ae;
  margin-bottom: 5px;
}

.upload-text {
  font-size: 12px;
  color: #6a82ae;
}

.upload-tips {
  font-size: 12px;
  color: #8ea0bd;
  line-height: 1.5;
}

.toolbar {
  margin-bottom: 10px;
}
</style>
