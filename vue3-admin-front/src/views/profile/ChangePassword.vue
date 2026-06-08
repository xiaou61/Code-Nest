<template>
  <el-dialog v-model="visible" title="修改密码" width="500px" :before-close="handleClose">
    <div class="password-dialog">
      <CnSection surface="plain" compact class="dialog-summary">
        <div class="dialog-summary__content">
          <CnStatusTag type="warning" size="sm" subtle>安全操作</CnStatusTag>
          <span>修改成功后需要重新登录，以保护当前管理员账户。</span>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" @submit.prevent>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" show-password clearable />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password clearable />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password clearable />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">修改密码</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { CnSection, CnStatusTag } from '@/design-system'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import router from '@/router'

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
  }>(),
  {
    modelValue: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const userStore = useUserStore()
const formRef = ref<FormInstance | null>(null)
const loading = ref(false)
const visible = ref(false)

const form = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度必须在6-20个字符之间'))
  } else {
    if (form.confirmPassword !== '') {
      formRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive<FormRules<PasswordForm>>({
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validateConfirmPassword, trigger: 'blur' }]
})

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      Object.assign(form, {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      })
    }
  }
)

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const handleClose = () => {
  visible.value = false
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.changePassword(form)
    ElMessage.success('密码修改成功')
    handleClose()
    emit('success')

    await ElMessageBox.alert('密码修改成功，为了您的账户安全，请重新登录。', '提示', {
      confirmButtonText: '确定',
      type: 'success'
    })

    userStore.logout()
    router.push('/login')
  } catch (error) {
    const errorName = error instanceof Error ? error.name : ''
    if (errorName !== 'FormValidateError') {
      console.error('修改密码失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '修改密码失败，请重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.password-dialog {
  display: grid;
  gap: var(--cn-space-5);
}

.dialog-summary {
  border-color: color-mix(in srgb, var(--cn-color-warning) 24%, var(--cn-color-border));
}

.dialog-summary__content {
  display: grid;
  gap: var(--cn-space-2);
}

.dialog-summary__content > span {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

@media (max-width: 560px) {
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
