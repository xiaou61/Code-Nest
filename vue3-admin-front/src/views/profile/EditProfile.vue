<template>
  <el-dialog v-model="visible" title="编辑个人信息" width="600px" :before-close="handleClose">
    <div class="profile-dialog">
      <CnSection surface="plain" compact class="dialog-summary">
        <div class="dialog-summary__content">
          <CnStatusTag type="brand" size="sm" subtle>资料维护</CnStatusTag>
          <span>{{ form.realName || form.email || '完善管理员资料' }}</span>
        </div>
      </CnSection>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" @submit.prevent>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" clearable />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" type="email" placeholder="请输入邮箱地址" clearable />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :label="0">未知</el-radio>
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="头像URL" prop="avatar">
          <div class="avatar-input">
            <el-input v-model="form.avatar" placeholder="请输入头像图片URL" clearable />
            <el-avatar v-if="form.avatar" :size="60" :src="form.avatar">预览</el-avatar>
          </div>
        </el-form-item>

        <el-form-item label="个人简介" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入个人简介"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CnSection, CnStatusTag } from '@/design-system'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

interface ProfileForm {
  realName: string
  email: string
  phone: string
  gender: number
  avatar: string
  remark: string
}

interface UserInfo extends Partial<ProfileForm> {
  [key: string]: unknown
}

const props = withDefaults(
  defineProps<{
    modelValue?: boolean
    userInfo?: UserInfo | null
  }>(),
  {
    modelValue: false,
    userInfo: () => ({})
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

const form = reactive<ProfileForm>({
  realName: '',
  email: '',
  phone: '',
  gender: 0,
  avatar: '',
  remark: ''
})

const rules = reactive<FormRules<ProfileForm>>({
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }],
  realName: [{ max: 50, message: '真实姓名长度不能超过50个字符', trigger: 'blur' }]
})

watch(
  () => props.modelValue,
  (val) => {
    visible.value = val
    if (val) {
      Object.assign(form, {
        realName: props.userInfo?.realName || '',
        email: props.userInfo?.email || '',
        phone: props.userInfo?.phone || '',
        gender: props.userInfo?.gender || 0,
        avatar: props.userInfo?.avatar || '',
        remark: props.userInfo?.remark || ''
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
    await authApi.updateProfile(form)
    ElMessage.success('个人信息更新成功')
    await userStore.getUserInfo()
    emit('success')
    handleClose()
  } catch (error) {
    const errorName = error instanceof Error ? error.name : ''
    if (errorName !== 'FormValidateError') {
      console.error('更新个人信息失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '更新失败，请重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-dialog {
  display: grid;
  gap: var(--cn-space-5);
}

.dialog-summary {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 22%, var(--cn-color-border));
}

.dialog-summary__content {
  display: grid;
  gap: var(--cn-space-2);
}

.dialog-summary__content > span {
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 750;
  overflow-wrap: anywhere;
}

.avatar-input {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
}

.avatar-input :deep(.el-input) {
  flex: 1;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

@media (max-width: 560px) {
  .avatar-input {
    display: grid;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
