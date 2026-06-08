<template>
  <el-dialog
    v-model="visible"
    title="发表评论"
    width="500px"
    :before-close="handleClose"
  >
    <div v-if="moment" class="moment-preview">
      <div class="user-info">
        <el-avatar :size="40" :src="moment.userAvatar">
          {{ moment.userNickname?.charAt(0) }}
        </el-avatar>
        <div class="user-detail">
          <div class="user-name">{{ moment.userNickname }}</div>
          <div class="publish-time">{{ moment.createTime }}</div>
        </div>
      </div>
      <div class="moment-content">
        {{ moment.content }}
      </div>
    </div>

    <el-divider />

    <div class="dialog-meta">
      <CnStatusTag type="info" size="sm">最多 200 字</CnStatusTag>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
      <el-form-item prop="content">
        <el-input
          ref="textareaRef"
          v-model="form.content"
          type="textarea"
          placeholder="写下你的评论..."
          :rows="4"
          maxlength="200"
          show-word-limit
          autofocus
        />
      </el-form-item>

      <!-- 表情选择器 -->
      <el-form-item>
        <EmojiPicker @select="insertEmoji" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleComment" :loading="commenting">
        发表评论
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { CnStatusTag } from '@/design-system'
import { publishComment } from '@/api/moment'
import EmojiPicker from '@/components/EmojiPicker.vue'

interface MomentPreview {
  id: number
  content?: string
  createTime?: string
  userAvatar?: string
  userNickname?: string
}

interface CommentResult {
  id: number
  content: string
  createTime: string
  canDelete: boolean
}

const props = withDefaults(defineProps<{
  modelValue?: boolean
  moment?: MomentPreview | null
}>(), {
  modelValue: false,
  moment: null
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'commented', comment: CommentResult): void
}>()

// 响应式数据
const formRef = ref<FormInstance | null>(null)
const textareaRef = ref()
const commenting = ref(false)

// 表单数据
const form = reactive({
  content: ''
})

// 表单验证规则
const rules: FormRules = {
  content: [
    { required: true, message: '请输入评论内容', trigger: 'blur' },
    { min: 1, max: 200, message: '评论长度应在1-200字符之间', trigger: 'blur' }
  ]
}

// 对话框可见性
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 监听对话框打开，重置表单
watch(visible, (newVal) => {
  if (newVal) {
    resetForm()
  }
})

// 重置表单
const resetForm = () => {
  form.content = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}

// 发表评论
const handleComment = async () => {
  if (!formRef.value || !props.moment) return

  try {
    await formRef.value.validate()
    
    commenting.value = true
    
    const data = {
      momentId: props.moment.id,
      content: form.content
    }
    
    const commentId = await publishComment(data) as number
    
    ElMessage.success('评论发表成功！')
    
    // 构造评论对象返回给父组件
    const comment = {
      id: commentId,
      content: form.content,
      createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      canDelete: true // 自己的评论可以删除
    }
    
    emit('commented', comment)
    visible.value = false
  } catch (error) {
    if (error instanceof Error && error.message) {
      ElMessage.error('评论发表失败：' + error.message)
    }
  } finally {
    commenting.value = false
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

.moment-preview {
  background: var(--cn-color-bg-surface-muted);
  border: 1px solid var(--cn-color-border-subtle);
  border-left: 3px solid var(--cn-color-brand-primary);
  border-radius: var(--cn-radius-card);
  padding: var(--cn-space-4);
  margin-bottom: var(--cn-space-3);
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: var(--cn-space-3);
}

.user-detail {
  margin-left: var(--cn-space-3);
}

.user-name {
  font-weight: 600;
  color: var(--cn-color-brand-primary);
  font-size: 14px;
}

.publish-time {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  margin-top: var(--cn-space-1);
}

.moment-content {
  color: var(--cn-color-text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.dialog-meta {
  display: flex;
  justify-content: flex-end;
  margin-bottom: var(--cn-space-3);
}

:deep(.el-divider) {
  margin: var(--cn-space-4) 0;
  border-color: var(--cn-color-border-subtle);
}
</style>
