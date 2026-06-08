<template>
  <CnPage class="profile-page" surface="transparent" max-width="1280px" full-height>
    <CnPageHeader
      title="个人中心"
      description="集中维护账号资料、安全密码和成长积分，让学习资产和账户设置保持同步。"
      eyebrow="Account Center"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="pointsBalance?.hasCheckedToday ? 'success' : 'warning'" size="sm">
          {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '今日待打卡' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm">
          积分 {{ pointsBalance?.totalPoints || 0 }}
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button plain @click="goToMoments">
          <el-icon><Picture /></el-icon>
          朋友圈
        </el-button>
        <el-button type="primary" @click="goToPointsPage">
          <el-icon><Trophy /></el-icon>
          积分中心
        </el-button>
      </template>
    </CnPageHeader>

    <section class="profile-summary-grid" aria-label="账户概览">
      <CnStatCard
        title="总积分"
        :value="pointsBalance?.totalPoints || 0"
        unit="分"
        description="当前账户累计可用积分"
        tone="brand"
        trend="flat"
        trend-text="资产"
        :loading="pointsLoading"
      />
      <CnStatCard
        title="连续打卡"
        :value="pointsBalance?.continuousDays || 0"
        unit="天"
        description="连续完成每日打卡记录"
        tone="success"
        trend="up"
        trend-text="习惯"
        :loading="pointsLoading"
      />
      <CnStatCard
        title="明日可得"
        :value="pointsBalance?.nextDayPoints || 0"
        unit="分"
        description="按当前连续打卡状态预估"
        tone="info"
        trend="flat"
        trend-text="预估"
        :loading="pointsLoading"
      />
      <article class="checkin-card">
        <div>
          <span>每日打卡</span>
          <strong>{{ pointsBalance?.hasCheckedToday ? '已完成' : '未完成' }}</strong>
          <p>{{ pointsBalance?.hasCheckedToday ? '今天的成长记录已经同步。' : '完成打卡后会刷新积分和连续天数。' }}</p>
        </div>
        <el-button
          type="primary"
          :disabled="pointsBalance?.hasCheckedToday || checkinLoading"
          :loading="checkinLoading"
          @click="handleQuickCheckin"
        >
          {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '立即打卡' }}
        </el-button>
      </article>
    </section>

    <div class="profile-layout">
      <CnSection class="profile-card" title="个人资料" description="头像、邮箱、真实姓名和手机号会同步到用户工作台。" surface="panel" divided>
        <template #actions>
          <el-button :type="editMode ? 'default' : 'primary'" @click="toggleEditMode">
            {{ editMode ? '取消编辑' : '编辑资料' }}
          </el-button>
        </template>

        <div class="profile-content" v-loading="loading">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-position="top"
            :disabled="!editMode"
          >
            <div class="avatar-section">
              <el-avatar
                :size="96"
                :src="profileForm.avatar"
                :alt="profileForm.nickname || profileForm.username"
              >
                <el-icon><User /></el-icon>
              </el-avatar>

              <div class="avatar-copy">
                <strong>{{ profileForm.username || 'Code Nest 用户' }}</strong>
                <span>{{ profileForm.email || '暂无邮箱' }}</span>
                <div v-if="editMode" class="avatar-actions">
                  <el-upload
                    ref="avatarUploadRef"
                    :auto-upload="false"
                    :show-file-list="false"
                    :before-upload="beforeAvatarUpload"
                    :on-change="handleAvatarChange"
                    accept="image/*"
                  >
                    <el-button plain :icon="Plus" :loading="avatarUploading">
                      {{ avatarUploading ? '上传中...' : '更换头像' }}
                    </el-button>
                  </el-upload>
                  <em>支持 JPG、PNG、GIF，文件不超过 5MB。</em>
                </div>
              </div>
            </div>

            <div class="form-grid">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="profileForm.username" disabled />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="profileForm.email" />
              </el-form-item>
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="profileForm.realName" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="profileForm.phone" />
              </el-form-item>
              <el-form-item label="创建时间">
                <el-input v-model="profileForm.createTime" disabled />
              </el-form-item>
              <el-form-item label="最后登录">
                <el-input v-model="profileForm.lastLoginTime" disabled />
              </el-form-item>
            </div>

            <div v-if="editMode" class="form-actions">
              <el-button type="primary" :loading="saveLoading" @click="handleSave">
                保存修改
              </el-button>
            </div>
          </el-form>
        </div>
      </CnSection>

      <CnSection class="password-card" title="修改密码" description="修改成功后会自动退出登录，请使用新密码重新进入。" surface="panel" divided>
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <div class="form-grid single-row">
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>
          </div>

          <el-form-item v-if="passwordCaptchaImage" label="验证码" prop="captcha">
            <div class="captcha-row">
              <el-input v-model="passwordForm.captcha" placeholder="请输入验证码" class="captcha-input" />
              <button class="captcha-image" type="button" title="刷新验证码" @click="loadPasswordCaptcha">
                <img :src="passwordCaptchaImage" alt="验证码" />
              </button>
            </div>
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
              修改密码
            </el-button>
            <el-button @click="resetPasswordForm">重置</el-button>
          </div>
        </el-form>
      </CnSection>
    </div>
  </CnPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadFile, type UploadRawFile } from 'element-plus'
import { Picture, Plus, Trophy, User } from '@element-plus/icons-vue'
import {
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag
} from '@/design-system'
import { captchaApi } from '@/api/captcha'
import pointsApi from '@/api/points'
import { userApi } from '@/api/user'
import { useUserStore } from '@/stores/user'

interface ProfileForm {
  username: string
  email: string
  realName: string
  phone: string
  avatar: string
  createTime: string
  lastLoginTime: string
  nickname?: string
}

interface PasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
  captcha: string
  captchaKey: string
}

interface UserInfo {
  username?: string
  email?: string
  realName?: string
  phone?: string
  avatar?: string
  createTime?: string
  lastLoginTime?: string
  nickname?: string
  [key: string]: unknown
}

interface PointsBalance {
  totalPoints?: number
  continuousDays?: number
  nextDayPoints?: number
  hasCheckedToday?: boolean
}

interface CheckinResult {
  pointsEarned?: number
}

interface CaptchaResult {
  captchaImage: string
  captchaKey: string
}

const router = useRouter()
const userStore = useUserStore()

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const avatarUploadRef = ref()

const editMode = ref(false)
const loading = ref(false)
const saveLoading = ref(false)
const passwordLoading = ref(false)
const passwordCaptchaImage = ref('')
const avatarUploading = ref(false)
const pointsBalance = ref<PointsBalance | null>(null)
const pointsLoading = ref(false)
const checkinLoading = ref(false)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '个人中心' }
]

const profileForm = reactive<ProfileForm>({
  username: '',
  email: '',
  realName: '',
  phone: '',
  avatar: '',
  createTime: '',
  lastLoginTime: ''
})

const passwordForm = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captcha: '',
  captchaKey: ''
})

const profileRules: FormRules<ProfileForm> = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

const passwordRules: FormRules<PasswordForm> = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
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

const loadUserInfo = async () => {
  loading.value = true
  try {
    const userInfo = (await userApi.getCurrentUserInfo()) as UserInfo
    Object.assign(profileForm, {
      username: userInfo.username || '',
      email: userInfo.email || '',
      realName: userInfo.realName || '',
      phone: userInfo.phone || '',
      avatar: userInfo.avatar || '',
      createTime: userInfo.createTime || '',
      lastLoginTime: userInfo.lastLoginTime || '从未登录',
      nickname: userInfo.nickname || ''
    })
    userStore.setUserInfo(userInfo)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

const toggleEditMode = () => {
  editMode.value = !editMode.value
  if (!editMode.value) {
    profileFormRef.value?.clearValidate()
  }
}

const handleSave = async () => {
  try {
    await profileFormRef.value?.validate()
    saveLoading.value = true

    const updateData = {
      email: profileForm.email,
      realName: profileForm.realName || undefined,
      phone: profileForm.phone || undefined
    }

    const result = (await userApi.updateUserInfo(updateData)) as UserInfo
    userStore.setUserInfo(result)
    ElMessage.success('个人资料更新成功')
    editMode.value = false
  } catch (error) {
    console.error('更新个人资料失败:', error)
  } finally {
    saveLoading.value = false
  }
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value?.validate()
    passwordLoading.value = true

    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
      captcha: passwordForm.captcha,
      captchaKey: passwordForm.captchaKey
    })

    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  } catch (error) {
    console.error('修改密码失败:', error)
    loadPasswordCaptcha()
  } finally {
    passwordLoading.value = false
  }
}

const loadPasswordCaptcha = async () => {
  try {
    const result = (await captchaApi.generateCaptcha()) as CaptchaResult
    passwordCaptchaImage.value = result.captchaImage
    passwordForm.captchaKey = result.captchaKey
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

const resetPasswordForm = () => {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    captcha: '',
    captchaKey: ''
  })
  passwordFormRef.value?.clearValidate()
  loadPasswordCaptcha()
}

const beforeAvatarUpload = (rawFile: UploadRawFile) => {
  const isValidType = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'].includes(rawFile.type)
  const isLt5M = rawFile.size / 1024 / 1024 < 5

  if (!isValidType) {
    ElMessage.error('头像只能是 JPG、PNG、GIF 格式!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('头像大小不能超过 5MB!')
    return false
  }
  return true
}

const handleAvatarChange = async (uploadFile: UploadFile) => {
  if (!uploadFile.raw) return

  try {
    avatarUploading.value = true
    const avatarUrl = (await userApi.uploadAvatar(uploadFile.raw)) as string
    profileForm.avatar = avatarUrl

    const currentUserInfo = userStore.userInfo as UserInfo | null
    if (currentUserInfo) {
      userStore.setUserInfo({
        ...currentUserInfo,
        avatar: avatarUrl
      })
    }

    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

const loadPointsBalance = async () => {
  pointsLoading.value = true
  try {
    pointsBalance.value = (await pointsApi.getPointsBalance()) as PointsBalance
  } catch (error) {
    console.error('加载积分信息失败:', error)
  } finally {
    pointsLoading.value = false
  }
}

const handleQuickCheckin = async () => {
  if (pointsBalance.value?.hasCheckedToday) {
    ElMessage.warning('今日已打卡，请勿重复操作')
    return
  }

  checkinLoading.value = true
  try {
    const response = (await pointsApi.checkin()) as CheckinResult
    ElMessage.success({
      message: `打卡成功！获得 ${response.pointsEarned || 0} 积分`,
      duration: 3000
    })
    await loadPointsBalance()
  } catch (error) {
    console.error('打卡失败:', error)
    ElMessage.error(error instanceof Error ? error.message : '打卡失败，请重试')
  } finally {
    checkinLoading.value = false
  }
}

const goToMoments = () => {
  router.push('/moments')
}

const goToPointsPage = () => {
  router.push('/points')
}

onMounted(() => {
  loadUserInfo()
  loadPasswordCaptcha()
  loadPointsBalance()
})
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 74px);
}

.profile-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.checkin-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
  min-width: 0;
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-card-border);
  border-radius: var(--cn-card-radius);
  background: color-mix(in srgb, var(--cn-card-bg) 86%, var(--cn-color-success-soft));
  box-shadow: var(--cn-card-shadow);
}

.checkin-card span {
  display: block;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.checkin-card strong {
  display: block;
  margin-top: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  line-height: 1.15;
}

.checkin-card p {
  margin: var(--cn-space-2) 0 0;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
  line-height: 1.55;
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(320px, 0.88fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.profile-card,
.password-card {
  min-width: 0;
}

.profile-content {
  min-width: 0;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: var(--cn-space-5);
  margin-bottom: var(--cn-space-6);
  padding: var(--cn-space-5);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.avatar-section :deep(.el-avatar) {
  flex-shrink: 0;
  border: 3px solid var(--cn-color-bg-surface);
  box-shadow: var(--cn-shadow-sm);
}

.avatar-copy {
  display: grid;
  min-width: 0;
  gap: var(--cn-space-2);
}

.avatar-copy strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-size: 18px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-copy span {
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-2);
}

.avatar-actions em {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-style: normal;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--cn-space-4);
}

.form-grid.single-row {
  gap: var(--cn-space-4);
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-2);
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
  width: 150px;
  height: 40px;
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

.profile-card :deep(.cn-section__body),
.password-card :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-4);
}

@media (max-width: 1180px) {
  .profile-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-layout {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .profile-summary-grid,
  .form-grid,
  .form-grid.single-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .checkin-card,
  .avatar-section,
  .captcha-row {
    align-items: stretch;
    flex-direction: column;
  }

  .captcha-image {
    width: 100%;
  }

  .avatar-copy strong,
  .avatar-copy span {
    white-space: normal;
  }
}
</style>
