<template>
  <CnPage class="legacy-auth" surface="transparent">
    <div class="auth-shell">
      <section class="auth-aside">
        <CnStatusTag type="success" size="lg">Join Code Nest</CnStatusTag>
        <h1>创建账户</h1>
        <p>把刷题、复盘、作品和社区交流放进一个稳定的成长系统里。</p>
        <div class="auth-aside__tags">
          <CnStatusTag type="brand" size="sm" subtle>学习路径</CnStatusTag>
          <CnStatusTag type="info" size="sm" subtle>代码工坊</CnStatusTag>
          <CnStatusTag type="warning" size="sm" subtle>积分激励</CnStatusTag>
        </div>
      </section>

      <CnSection surface="plain" class="auth-card">
        <CnPageHeader
          compact
          title="注册 Code Nest"
          description="这个旧注册页已接入 design-system；当前路由默认使用 Auth.vue。"
        />

        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="auth-form"
          label-position="top"
          @submit.prevent="handleRegister"
        >
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
                @blur="checkUsername"
              />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="邮箱"
                size="large"
                :prefix-icon="Message"
                @blur="checkEmail"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item label="姓名" prop="realName">
              <el-input v-model="registerForm.realName" placeholder="姓名（可选）" size="large" :prefix-icon="UserFilled" />
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号（可选）" size="large" :prefix-icon="Phone" />
            </el-form-item>
          </div>

          <el-form-item v-if="captchaImage" label="验证码" prop="captcha">
            <div class="captcha-row">
              <el-input v-model="registerForm.captcha" placeholder="请输入验证码" size="large" />
              <button class="captcha-image" type="button" aria-label="刷新验证码" @click="refreshCaptcha">
                <img :src="captchaImage" alt="验证码" />
              </button>
            </div>
          </el-form-item>

          <el-button type="primary" size="large" :loading="loading" class="submit-button" @click="handleRegister">
            注册
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>已有账户？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, Message, Phone, User, UserFilled } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { authApi } from '@/api/auth'
import { captchaApi } from '@/api/captcha'

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
  captcha: string
  captchaKey: string
}

const router = useRouter()

const registerFormRef = ref<FormInstance | null>(null)
const captchaImage = ref('')
const loading = ref(false)

const registerForm = reactive<RegisterForm>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  captcha: '',
  captchaKey: ''
})

const registerRules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const loadCaptcha = async () => {
  try {
    const result = await captchaApi.generateCaptcha()
    captchaImage.value = result.captchaImage
    registerForm.captchaKey = result.captchaKey
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const refreshCaptcha = () => {
  registerForm.captcha = ''
  loadCaptcha()
}

const checkUsername = async () => {
  if (!registerForm.username || registerForm.username.length < 3) return
  try {
    await authApi.checkUsername(registerForm.username)
  } catch (error) {
    // API interceptor shows availability errors.
  }
}

const checkEmail = async () => {
  if (!registerForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) return
  try {
    await authApi.checkEmail(registerForm.email)
  } catch (error) {
    // API interceptor shows availability errors.
  }
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.realName || undefined,
      phone: registerForm.phone || undefined,
      captcha: registerForm.captcha,
      captchaKey: registerForm.captchaKey
    })

    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<style scoped>
.legacy-auth {
  min-height: 100vh;
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 0.78fr) minmax(460px, 620px);
  gap: var(--cn-space-6);
  align-items: stretch;
  min-height: calc(100vh - 2 * var(--cn-space-6));
}

.auth-aside,
.auth-card {
  min-width: 0;
}

.auth-aside {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--cn-space-5);
  padding: clamp(var(--cn-space-6), 6vw, var(--cn-space-10));
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 88%, var(--cn-color-success-soft));
  box-shadow: var(--cn-shadow-card);
}

.auth-aside h1 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(34px, 5vw, 54px);
  font-weight: 760;
  line-height: 1.08;
  overflow-wrap: anywhere;
}

.auth-aside p {
  max-width: 520px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.auth-aside__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.auth-card :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-5);
}

.auth-form {
  display: grid;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--cn-space-3);
}

.captcha-row {
  display: flex;
  gap: var(--cn-space-3);
  width: 100%;
}

.captcha-row :deep(.el-input) {
  flex: 1;
}

.captcha-image {
  width: 122px;
  height: 42px;
  margin: 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--cn-color-border);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.submit-button {
  width: 100%;
  min-height: 44px;
  margin-top: var(--cn-space-2);
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-2);
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
}

.auth-footer a {
  color: var(--cn-color-brand-primary);
  font-weight: 700;
  text-decoration: none;
}

@media (max-width: 980px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .form-grid,
  .captcha-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .captcha-image {
    width: 100%;
  }
}
</style>
