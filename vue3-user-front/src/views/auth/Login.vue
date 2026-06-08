<template>
  <CnPage class="legacy-auth" surface="transparent">
    <div class="auth-shell">
      <section class="auth-aside">
        <CnStatusTag type="brand" size="lg">Code Nest</CnStatusTag>
        <h1>欢迎回来</h1>
        <p>继续你的刷题、面试、知识沉淀和作品打磨节奏。</p>
        <div class="auth-aside__tags">
          <CnStatusTag type="success" size="sm" subtle>面试题库</CnStatusTag>
          <CnStatusTag type="info" size="sm" subtle>知识图谱</CnStatusTag>
          <CnStatusTag type="warning" size="sm" subtle>计划打卡</CnStatusTag>
        </div>
      </section>

      <CnSection surface="plain" class="auth-card">
        <CnPageHeader
          compact
          title="登录账户"
          description="这个旧登录页已接入 design-system；当前路由默认使用 Auth.vue。"
        />

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="auth-form"
          label-position="top"
          @submit.prevent="handleLogin"
        >
          <el-form-item label="用户名或邮箱" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名或邮箱" size="large" :prefix-icon="User" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item v-if="captchaImage" label="验证码" prop="captcha">
            <div class="captcha-row">
              <el-input v-model="loginForm.captcha" placeholder="请输入验证码" size="large" />
              <button class="captcha-image" type="button" aria-label="刷新验证码" @click="refreshCaptcha">
                <img :src="captchaImage" alt="验证码" />
              </button>
            </div>
          </el-form-item>

          <el-button type="primary" size="large" :loading="loading" class="submit-button" @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>还没有账户？</span>
          <router-link to="/register">立即注册</router-link>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/auth'
import { captchaApi } from '@/api/captcha'

interface LoginForm {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

const router = useRouter()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance | null>(null)
const captchaImage = ref('')
const loading = ref(false)

const loginForm = reactive<LoginForm>({
  username: '',
  password: '',
  captcha: '',
  captchaKey: ''
})

const loginRules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const loadCaptcha = async () => {
  try {
    const result = await captchaApi.generateCaptcha()
    captchaImage.value = result.captchaImage
    loginForm.captchaKey = result.captchaKey
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const refreshCaptcha = () => {
  loginForm.captcha = ''
  loadCaptcha()
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await authApi.login({
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha,
      captchaKey: loginForm.captchaKey
    })

    userStore.login(result.accessToken, result.userInfo)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
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
  grid-template-columns: minmax(0, 0.9fr) minmax(360px, 460px);
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
  background: color-mix(in srgb, var(--cn-color-bg-surface) 88%, var(--cn-color-brand-soft));
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

@media (max-width: 860px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .captcha-row {
    display: grid;
  }

  .captcha-image {
    width: 100%;
  }
}
</style>
