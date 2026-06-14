<template>
  <CnPage class="auth-page" surface="transparent" max-width="1440px" full-height>
    <section class="auth-shell" :class="{ 'is-register': isRegister }" aria-label="Code Nest 账户认证">
      <div class="auth-brand-panel">
        <div class="auth-brand-top">
          <button class="brand-mark" type="button" @click="goHome">
            <span>CN</span>
          </button>
          <CnThemeSwitch class="auth-theme-switch" />
        </div>

        <div class="auth-brand-copy">
          <CnStatusTag type="brand" size="sm">Developer Growth OS</CnStatusTag>
          <h1>Code Nest</h1>
          <p>
            把刷题、AI 模拟面试、知识图谱、博客输出和计划打卡放进同一个成长工作台。
          </p>
        </div>

        <div class="auth-feature-grid" aria-label="核心能力">
          <article v-for="item in featureCards" :key="item.title" class="feature-card">
            <span class="feature-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <strong>{{ item.title }}</strong>
            <em>{{ item.description }}</em>
          </article>
        </div>

        <div class="auth-brand-footer">
          <span>学习</span>
          <span>练习</span>
          <span>输出</span>
          <span>复盘</span>
        </div>
      </div>

      <CnSection class="auth-form-panel" surface="panel" compact>
        <div class="auth-mode-tabs" role="tablist" aria-label="认证模式">
          <button
            class="auth-mode-tab"
            :class="{ active: !isRegister }"
            type="button"
            role="tab"
            :aria-selected="!isRegister"
            @click="switchMode(false)"
          >
            登录
          </button>
          <button
            class="auth-mode-tab"
            :class="{ active: isRegister }"
            type="button"
            role="tab"
            :aria-selected="isRegister"
            @click="switchMode(true)"
          >
            注册
          </button>
        </div>

        <transition name="auth-fade" mode="out-in">
          <div v-if="!isRegister" key="login" class="auth-card">
            <div class="auth-header">
              <CnStatusTag type="success" size="sm">欢迎回来</CnStatusTag>
              <h2>登录账户</h2>
              <p>继续你的题单、计划、社区和创作进度。</p>
            </div>

            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="auth-form"
              @submit.prevent="handleLogin"
            >
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="用户名或邮箱" size="large" :prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-form-item v-if="captchaImage" prop="captcha">
                <div class="captcha-row">
                  <el-input v-model="loginForm.captcha" placeholder="验证码" size="large" class="captcha-input" />
                  <button class="captcha-image" type="button" title="刷新验证码" @click="refreshCaptcha">
                    <img :src="captchaImage" alt="验证码" />
                  </button>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" native-type="submit" :loading="loading" class="submit-btn">
                  登录
                </el-button>
              </el-form-item>
            </el-form>

            <p class="auth-footer">
              还没有账户？
              <button type="button" @click="switchMode(true)">立即注册</button>
            </p>
          </div>

          <div v-else key="register" class="auth-card">
            <div class="auth-header">
              <CnStatusTag type="brand" size="sm">开始成长</CnStatusTag>
              <h2>创建账户</h2>
              <p>注册后即可保存你的学习轨迹与社区资产。</p>
            </div>

            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              class="auth-form"
              @submit.prevent="handleRegister"
            >
              <div class="form-row">
                <el-form-item prop="username">
                  <el-input v-model="registerForm.username" placeholder="用户名" size="large" :prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input v-model="registerForm.email" placeholder="邮箱" size="large" :prefix-icon="Message" />
                </el-form-item>
              </div>
              <div class="form-row">
                <el-form-item prop="password">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
                <el-form-item prop="confirmPassword">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="确认密码"
                    size="large"
                    :prefix-icon="Lock"
                    show-password
                  />
                </el-form-item>
              </div>
              <el-form-item v-if="captchaImage" prop="captcha">
                <div class="captcha-row">
                  <el-input v-model="registerForm.captcha" placeholder="验证码" size="large" class="captcha-input" />
                  <button class="captcha-image" type="button" title="刷新验证码" @click="refreshCaptcha">
                    <img :src="captchaImage" alt="验证码" />
                  </button>
                </div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" native-type="submit" :loading="loading" class="submit-btn">
                  注册
                </el-button>
              </el-form-item>
            </el-form>

            <p class="auth-footer">
              已有账户？
              <button type="button" @click="switchMode(false)">立即登录</button>
            </p>
          </div>
        </transition>
      </CnSection>
    </section>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  Collection,
  Connection,
  Cpu,
  DataAnalysis,
  Lock,
  Message,
  User
} from '@element-plus/icons-vue'
import { CnPage, CnSection, CnStatusTag, CnThemeSwitch } from '@/design-system'
import { authApi } from '@/api/auth'
import { captchaApi } from '@/api/captcha'
import { useUserStore } from '@/stores/user'

interface LoginForm {
  username: string
  password: string
  captcha: string
  captchaKey: string
}

interface RegisterForm {
  username: string
  email: string
  password: string
  confirmPassword: string
  captcha: string
  captchaKey: string
}

interface AuthResult {
  accessToken: string
  userInfo: unknown
}

interface CaptchaResult {
  captchaImage: string
  captchaKey: string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isRegister = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const captchaImage = ref('')
const loading = ref(false)

const loginForm = reactive<LoginForm>({
  username: '',
  password: '',
  captcha: '',
  captchaKey: ''
})

const registerForm = reactive<RegisterForm>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captcha: '',
  captchaKey: ''
})

const featureCards = [
  {
    title: '面试题库',
    description: '题单、收藏、复习串成完整准备路径',
    icon: Collection
  },
  {
    title: 'AI 模拟面试',
    description: '用报告和评分补齐表达短板',
    icon: DataAnalysis
  },
  {
    title: '在线判题',
    description: '多语言练习与提交记录统一沉淀',
    icon: Cpu
  },
  {
    title: '社区协作',
    description: '帖子、动态和小组承接学习输出',
    icon: Connection
  }
]

const loginRules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const registerRules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次输入密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

watch(
  () => route.path,
  (path) => {
    isRegister.value = path === '/register'
  },
  { immediate: true }
)

const goHome = () => {
  router.push('/')
}

const switchMode = (registerMode: boolean) => {
  if (isRegister.value === registerMode) return
  isRegister.value = registerMode
  router.replace(registerMode ? '/register' : '/login')
  refreshCaptcha()
}

const loadCaptcha = async () => {
  try {
    const result = (await captchaApi.generateCaptcha()) as CaptchaResult
    captchaImage.value = result.captchaImage
    if (isRegister.value) {
      registerForm.captchaKey = result.captchaKey
    } else {
      loginForm.captchaKey = result.captchaKey
    }
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const refreshCaptcha = () => {
  loginForm.captcha = ''
  registerForm.captcha = ''
  loadCaptcha()
}

const handleLogin = async () => {
  try {
    await loginFormRef.value?.validate()
    loading.value = true
    const result = (await authApi.login({
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha,
      captchaKey: loginForm.captchaKey
    })) as AuthResult
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

const handleRegister = async () => {
  try {
    await registerFormRef.value?.validate()
    loading.value = true
    await authApi.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      captcha: registerForm.captcha,
      captchaKey: registerForm.captchaKey
    })
    ElMessage.success('注册成功，请登录')
    switchMode(false)
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
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(420px, 0.9fr);
  gap: var(--cn-space-6);
  width: 100%;
  min-height: calc(100vh - var(--cn-space-6) * 2);
}

.auth-brand-panel,
.auth-form-panel {
  min-width: 0;
}

.auth-brand-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: var(--cn-space-8);
  border: 1px solid var(--cn-panel-border);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-bg-elevated) 84%, var(--cn-color-brand-soft));
  box-shadow: var(--cn-shadow-card);
}

.auth-brand-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 6%, transparent);
  opacity: 0.64;
  pointer-events: none;
}

.auth-brand-top,
.auth-brand-copy,
.auth-feature-grid,
.auth-brand-footer {
  position: relative;
  z-index: 1;
}

.auth-brand-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 0;
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-brand-primary) 84%, var(--cn-color-info));
  color: white;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
  box-shadow: 0 14px 28px color-mix(in srgb, var(--cn-color-brand-primary) 24%, transparent);
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.brand-mark:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 32px color-mix(in srgb, var(--cn-color-brand-primary) 30%, transparent);
}

.auth-theme-switch {
  background: color-mix(in srgb, var(--cn-color-bg-surface) 80%, transparent);
}

.auth-brand-copy {
  max-width: 720px;
  margin: auto 0;
}

.auth-brand-copy h1 {
  margin: var(--cn-space-4) 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: clamp(42px, 7vw, 84px);
  font-weight: 750;
  line-height: 0.96;
}

.auth-brand-copy p {
  max-width: 600px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

.auth-feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-4);
  margin-top: var(--cn-space-8);
}

.feature-card {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-2);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 82%, transparent);
  backdrop-filter: blur(10px);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.feature-card strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
}

.feature-card em {
  min-width: 0;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-style: normal;
  line-height: 1.6;
}

.auth-brand-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-8);
}

.auth-brand-footer span {
  padding: 6px 10px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 650;
}

.auth-form-panel {
  align-self: center;
}

.auth-form-panel :deep(.cn-section__body) {
  padding: var(--cn-space-6);
}

.auth-mode-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
}

.auth-mode-tab {
  min-height: 38px;
  border: 0;
  border-radius: var(--cn-radius-pill);
  background: transparent;
  color: var(--cn-color-text-secondary);
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  transition:
    color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    box-shadow var(--cn-motion-fast) var(--cn-ease-out);
}

.auth-mode-tab:hover {
  color: var(--cn-color-brand-primary);
}

.auth-mode-tab.active {
  background: var(--cn-color-bg-surface);
  color: var(--cn-color-brand-primary);
  box-shadow: var(--cn-shadow-xs);
}

.auth-card {
  display: grid;
  gap: var(--cn-space-5);
  margin-top: var(--cn-space-6);
}

.auth-header {
  display: grid;
  justify-items: start;
  gap: var(--cn-space-2);
}

.auth-header h2 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 28px;
  line-height: 1.2;
}

.auth-header p {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.auth-form {
  min-width: 0;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: var(--cn-space-4);
}

.auth-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 46px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-3);
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  width: 100%;
  min-width: 0;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-image {
  flex-shrink: 0;
  width: 138px;
  height: 46px;
  overflow: hidden;
  padding: 0;
  border: 1px solid var(--cn-input-border);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface-muted);
  cursor: pointer;
  transition:
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    transform var(--cn-motion-fast) var(--cn-ease-out);
}

.captcha-image:hover {
  border-color: var(--cn-color-brand-primary);
  transform: translateY(-1px);
}

.captcha-image img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.submit-btn {
  width: 100%;
  min-height: 46px;
  font-size: 15px;
  font-weight: 750;
}

.auth-footer {
  margin: 0;
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  text-align: center;
}

.auth-footer button {
  border: 0;
  background: transparent;
  color: var(--cn-color-brand-primary);
  cursor: pointer;
  font: inherit;
  font-weight: 750;
}

.auth-footer button:hover {
  color: var(--cn-color-brand-hover);
}

.auth-fade-enter-active,
.auth-fade-leave-active {
  transition:
    opacity var(--cn-motion-base) var(--cn-ease-out),
    transform var(--cn-motion-base) var(--cn-ease-out);
}

.auth-fade-enter-from,
.auth-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 1100px) {
  .auth-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .auth-brand-panel {
    min-height: auto;
  }

  .auth-form-panel {
    align-self: stretch;
  }
}

@media (max-width: 768px) {
  .auth-page {
    padding: var(--cn-space-4);
  }

  .auth-shell {
    min-height: auto;
    gap: var(--cn-space-4);
  }

  .auth-brand-panel {
    padding: var(--cn-space-5);
  }

  .auth-brand-copy {
    margin: var(--cn-space-8) 0 0;
  }

  .auth-feature-grid {
    grid-template-columns: minmax(0, 1fr);
    margin-top: var(--cn-space-6);
  }

  .form-row {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
  }
}

@media (max-width: 520px) {
  .auth-brand-top,
  .captcha-row {
    align-items: stretch;
    flex-direction: column;
  }

  .captcha-image {
    width: 100%;
  }

  .auth-form-panel :deep(.cn-section__body) {
    padding: var(--cn-space-5);
  }

  .auth-header h2 {
    font-size: 24px;
  }
}
</style>
