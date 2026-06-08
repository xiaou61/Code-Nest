<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="发布讨论"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="discussion-form-dialog">
      <CnSection surface="plain" compact class="discussion-summary">
        <div class="discussion-summary__content">
          <CnStatusTag type="brand" size="sm" subtle>小组讨论</CnStatusTag>
          <span>{{ form.title || '准备发布新的讨论' }}</span>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入讨论标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入讨论内容..."
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">发布</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CnSection, CnStatusTag } from '@/design-system'
import teamApi from '@/api/team'

interface DiscussionForm {
  title: string
  content: string
}

const props = defineProps<{
  modelValue: boolean
  teamId: string | number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance | null>(null)
const submitting = ref(false)

const form = ref<DiscussionForm>({
  title: '',
  content: ''
})

const rules: FormRules<DiscussionForm> = {
  title: [{ required: true, message: '请输入讨论标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入讨论内容', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      resetForm()
    }
  }
)

const resetForm = () => {
  form.value = {
    title: '',
    content: ''
  }
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await teamApi.createDiscussion(props.teamId, form.value)
    ElMessage.success('发布成功')
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('发布失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.discussion-form-dialog {
  display: grid;
  gap: var(--cn-space-5);
}

.discussion-summary {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.discussion-summary__content {
  display: grid;
  gap: var(--cn-space-2);
}

.discussion-summary__content > span {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
</style>
