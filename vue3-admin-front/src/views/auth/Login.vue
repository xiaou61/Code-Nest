<template>
  <CnPage class="admin-login-page" surface="transparent" max-width="1180px">
    <div class="login-shell">
      <section class="login-hero">
        <CnStatusTag type="brand" size="lg">Admin Workspace</CnStatusTag>
        <div class="login-hero__copy">
          <h1>Code-Nest 管理后台</h1>
          <p>统一管理用户、内容、系统配置和运营数据，保持后台操作链路清晰稳定。</p>
        </div>
        <div class="login-hero__meta">
          <CnStatusTag type="success" size="sm" subtle>Element Plus</CnStatusTag>
          <CnStatusTag type="info" size="sm" subtle>Design System</CnStatusTag>
          <CnStatusTag type="warning" size="sm" subtle>Theme Ready</CnStatusTag>
        </div>
      </section>

      <CnSection class="login-card" surface="plain">
        <CnPageHeader
          compact
          title="管理员登录"
          description="请输入管理端账号密码进入后台工作台。"
        />

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <div class="login-hints" aria-label="默认测试账号">
          <div class="hint-item">
            <span>默认账户</span>
            <strong>admin / 123456</strong>
          </div>
          <div class="hint-item">
            <span>系统账户</span>
            <strong>system / 123456</strong>
          </div>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import { useUserStore } from '@/stores/user'

interface LoginForm {
  username: string
  password: string
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref<FormInstance | null>(null)
const loading = ref(false)

const loginForm = reactive<LoginForm>({
  username: 'admin',
  password: '123456'
})

const loginRules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password
    })

    ElMessage.success('登录成功')
    router.push((route.query.redirect as RouteLocationRaw) || '/')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  display: grid;
  align-items: center;
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 460px);
  gap: var(--cn-space-6);
  align-items: stretch;
}

.login-hero {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--cn-space-6);
  min-width: 0;
  padding: clamp(var(--cn-space-6), 6vw, var(--cn-space-10));
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-brand-soft) 42%, var(--cn-color-bg-surface));
  box-shadow: var(--cn-shadow-card);
}

.login-hero__copy {
  display: grid;
  gap: var(--cn-space-4);
}

.login-hero__copy h1 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(34px, 5vw, 56px);
  font-weight: 760;
  line-height: 1.08;
  overflow-wrap: anywhere;
}

.login-hero__copy p {
  max-width: 620px;
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.login-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.login-card :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-5);
}

.login-form {
  display: grid;
}

.login-button {
  width: 100%;
  min-height: 44px;
  margin-top: var(--cn-space-2);
}

.login-hints {
  display: grid;
  gap: var(--cn-space-3);
  padding-top: var(--cn-space-4);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.hint-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  min-width: 0;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.hint-item strong {
  color: var(--cn-color-text-primary);
  font-weight: 750;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
}
</style>
