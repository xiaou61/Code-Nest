<template>
  <CnPage class="admin-login-page" surface="transparent" max-width="1120px">
    <div class="login-shell">
      <section class="login-hero">
        <div class="login-hero__badge">
          <el-icon><Lock /></el-icon>
          <span>Admin Workspace</span>
        </div>
        <div class="login-hero__copy">
          <h1>Code-Nest 管理后台</h1>
          <p>统一处理内容、用户、配置和运营数据，让后台工作台保持清晰、稳定和可控。</p>
        </div>
        <div class="login-hero__metrics" aria-label="后台能力">
          <div>
            <strong>RBAC</strong>
            <span>权限隔离</span>
          </div>
          <div>
            <strong>Audit</strong>
            <span>操作追踪</span>
          </div>
          <div>
            <strong>Config</strong>
            <span>系统配置</span>
          </div>
        </div>
      </section>

      <CnSection class="login-card" surface="plain">
        <div class="login-card__mark" aria-hidden="true">
          <el-icon><Lock /></el-icon>
        </div>

        <CnPageHeader
          compact
          title="管理员登录"
          description="请输入你的管理端凭据进入后台工作台。"
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
              autocomplete="username"
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
              autocomplete="current-password"
              show-password
              clearable
            />
          </el-form-item>

          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            :disabled="!canSubmit"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <div class="login-security-note">
          <el-icon><Lock /></el-icon>
          <span>凭据仅用于本次认证，不会在页面中展示或预填。</span>
        </div>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { CnPage, CnPageHeader, CnSection } from '@/design-system'
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
  username: '',
  password: ''
})

const canSubmit = computed(() => {
  return loginForm.username.trim().length >= 2 && loginForm.password.length >= 6 && !loading.value
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
  padding-block: clamp(var(--cn-space-5), 4vw, var(--cn-space-9));
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(380px, 430px);
  gap: clamp(var(--cn-space-5), 4vw, var(--cn-space-8));
  align-items: center;
}

.login-hero {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--cn-space-6);
  min-width: 0;
  min-height: 520px;
  padding: clamp(var(--cn-space-6), 6vw, var(--cn-space-11));
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 28px;
  background:
    radial-gradient(circle at 18% 18%, color-mix(in srgb, var(--cn-color-brand) 18%, transparent) 0 26%, transparent 27%),
    linear-gradient(135deg, color-mix(in srgb, var(--cn-color-brand-soft) 52%, var(--cn-color-bg-surface)), var(--cn-color-bg-surface));
  box-shadow: 0 24px 70px color-mix(in srgb, var(--cn-color-brand) 12%, transparent);
}

.login-hero__badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  gap: var(--cn-space-2);
  padding: 9px 13px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 999px;
  color: var(--cn-color-brand);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 72%, transparent);
  font-size: 13px;
  font-weight: 760;
}

.login-hero__copy {
  display: grid;
  gap: var(--cn-space-4);
}

.login-hero__copy h1 {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: clamp(36px, 5vw, 58px);
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

.login-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--cn-space-3);
  max-width: 560px;
}

.login-hero__metrics > div {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 18px;
  background: color-mix(in srgb, var(--cn-color-bg-surface) 70%, transparent);
}

.login-hero__metrics strong {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 18px;
  font-weight: 780;
}

.login-hero__metrics span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  font-weight: 650;
}

.login-card {
  position: relative;
}

.login-card :deep(.cn-section) {
  border-radius: 24px;
}

.login-card__mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  margin-bottom: var(--cn-space-3);
  border-radius: 16px;
  color: var(--cn-color-brand);
  background: var(--cn-color-brand-soft);
  font-size: 22px;
}

.login-card :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-5);
}

.login-form {
  display: grid;
  gap: var(--cn-space-1);
}

.login-form :deep(.el-form-item__label) {
  color: var(--cn-color-text-primary);
  font-weight: 720;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 14px;
  box-shadow: 0 0 0 1px var(--cn-color-border-subtle) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--cn-color-brand) inset, 0 0 0 4px var(--cn-color-brand-soft);
}

.login-button {
  width: 100%;
  min-height: 48px;
  margin-top: var(--cn-space-2);
  border-radius: 14px;
  font-weight: 760;
}

.login-security-note {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: 14px;
  background: color-mix(in srgb, var(--cn-color-bg-muted) 72%, transparent);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .admin-login-page {
    align-items: start;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-hero {
    min-height: auto;
  }
}

@media (max-width: 560px) {
  .login-shell {
    gap: var(--cn-space-4);
  }

  .login-hero {
    padding: var(--cn-space-5);
    border-radius: 22px;
  }

  .login-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>
