<template>
  <div class="profile-container">
    <!-- 导航栏 -->
    <div class="header">
      <div class="header-content">
        <div class="logo" @click="goHome">
          <h2>Code Nest</h2>
        </div>
        <div class="nav-menu">
          <el-button @click="goToMoments" :icon="Picture">
            朋友圈
          </el-button>
          <el-button @click="goHome" :icon="House">
            返回首页
          </el-button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="main-content">
      <div class="profile-section">
        <!-- 积分卡片 -->
        <el-card class="points-card">
          <template #header>
            <div class="card-header">
              <h3>我的积分</h3>
              <el-button type="primary" @click="goToPointsPage" :icon="TrophyIcon">
                查看详情
              </el-button>
            </div>
          </template>

          <div class="points-content" v-loading="pointsLoading">
            <div class="points-overview">
              <div class="points-main">
                <div class="points-balance">
                  <span class="balance-number">{{ pointsBalance?.totalPoints || 0 }}</span>
                  <span class="balance-label">总积分</span>
                </div>

                <div class="checkin-info">
                  <div class="checkin-stats">
                    <div class="stat-item">
                      <span class="stat-number">{{ pointsBalance?.continuousDays || 0 }}</span>
                      <span class="stat-label">连续打卡</span>
                    </div>
                    <div class="stat-item">
                      <span class="stat-number">{{ pointsBalance?.nextDayPoints || 0 }}</span>
                      <span class="stat-label">明日可得</span>
                    </div>
                  </div>

                  <button
                    class="checkin-btn"
                    :class="{ 'checked': pointsBalance?.hasCheckedToday }"
                    :disabled="pointsBalance?.hasCheckedToday || checkinLoading"
                    @click="handleQuickCheckin"
                  >
                    <span v-if="checkinLoading" class="loading-icon">⏳</span>
                    <span v-else-if="pointsBalance?.hasCheckedToday" class="check-icon">✅</span>
                    <span v-else class="checkin-icon">📅</span>
                    {{ pointsBalance?.hasCheckedToday ? '今日已打卡' : '立即打卡' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="profile-card">
          <template #header>
            <div class="card-header">
              <h3>个人资料</h3>
              <el-button type="primary" @click="editMode = !editMode">
                {{ editMode ? '取消编辑' : '编辑资料' }}
              </el-button>
            </div>
          </template>

          <div class="profile-content" v-loading="loading">
            <el-form
              ref="profileFormRef"
              :model="profileForm"
              :rules="profileRules"
              label-width="100px"
              :disabled="!editMode"
            >
              <!-- 头像上传区域 -->
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="头像">
                    <div class="avatar-section">
                      <div class="avatar-display">
                        <el-avatar
                          :size="100"
                          :src="profileForm.avatar"
                          :alt="profileForm.nickname || profileForm.username"
                        >
                          <el-icon><User /></el-icon>
                        </el-avatar>
                      </div>
                      <div class="avatar-actions" v-if="editMode">
                        <el-upload
                          ref="avatarUploadRef"
                          :auto-upload="false"
                          :show-file-list="false"
                          :before-upload="beforeAvatarUpload"
                          :on-change="handleAvatarChange"
                          accept="image/*"
                        >
                          <el-button type="primary" :icon="Plus" :loading="avatarUploading">
                            {{ avatarUploading ? '上传中...' : '更换头像' }}
                          </el-button>
                        </el-upload>
                        <div class="avatar-tips">
                          <p>支持jpg、png、gif格式，文件大小不超过5MB</p>
                        </div>
                      </div>
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="用户名" prop="username">
                    <el-input v-model="profileForm.username" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="邮箱" prop="email">
                    <el-input v-model="profileForm.email" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="真实姓名" prop="realName">
                    <el-input v-model="profileForm.realName" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号" prop="phone">
                    <el-input v-model="profileForm.phone" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="创建时间">
                    <el-input v-model="profileForm.createTime" disabled />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最后登录">
                    <el-input v-model="profileForm.lastLoginTime" disabled />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item v-if="editMode">
                <el-button type="primary" @click="handleSave" :loading="saveLoading">
                  保存修改
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>

        <!-- 修改密码卡片 -->
        <el-card class="password-card">
          <template #header>
            <h3>修改密码</h3>
          </template>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入当前密码"
              />
            </el-form-item>

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

            <!-- 验证码 -->
            <el-form-item label="验证码" prop="captcha" v-if="passwordCaptchaImage">
              <div class="captcha-row">
                <el-input
                  v-model="passwordForm.captcha"
                  placeholder="请输入验证码"
                  style="flex: 1;"
                />
                <div class="captcha-image" @click="loadPasswordCaptcha">
                  <img :src="passwordCaptchaImage" alt="验证码" />
                </div>
              </div>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { captchaApi } from '@/api/captcha'
import pointsApi from '@/api/points'
import { ElMessage } from 'element-plus'
import { House, User, Plus, Picture, Trophy } from '@element-plus/icons-vue'

// 图标别名（避免和变量冲突）
const TrophyIcon = Trophy

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const profileFormRef = ref()
const passwordFormRef = ref()

// 编辑模式
const editMode = ref(false)
const loading = ref(false)
const saveLoading = ref(false)
const passwordLoading = ref(false)
const passwordCaptchaImage = ref('')

// 积分相关数据
const pointsBalance = ref(null)
const pointsLoading = ref(false)
const checkinLoading = ref(false)

// 个人资料表单
const profileForm = reactive({
  username: '',
  email: '',
  realName: '',
  phone: '',
  avatar: '',
  createTime: '',
  lastLoginTime: ''
})

// 头像上传相关
const avatarUploadRef = ref()
const avatarUploading = ref(false)

// 密码修改表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  captcha: '',
  captchaKey: ''
})

// 个人资料验证规则
const profileRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ]
}

// 密码修改验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  try {
    const userInfo = await userApi.getCurrentUserInfo()
    Object.assign(profileForm, {
      username: userInfo.username,
      email: userInfo.email,
      realName: userInfo.realName || '',
      phone: userInfo.phone || '',
      avatar: userInfo.avatar || '',
      createTime: userInfo.createTime || '',
      lastLoginTime: userInfo.lastLoginTime || '从未登录'
    })
    userStore.setUserInfo(userInfo)
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

// 保存个人资料
const handleSave = async () => {
  try {
    await profileFormRef.value.validate()
    saveLoading.value = true

    const updateData = {
      email: profileForm.email,
      realName: profileForm.realName || undefined,
      phone: profileForm.phone || undefined
    }

    const result = await userApi.updateUserInfo(updateData)
    userStore.setUserInfo(result)

    ElMessage.success('个人资料更新成功')
    editMode.value = false
  } catch (error) {
    console.error('更新个人资料失败:', error)
  } finally {
    saveLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true

    await userApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
      captcha: passwordForm.captcha,
      captchaKey: passwordForm.captchaKey
    })

    ElMessage.success('密码修改成功，请重新登录')

    // 修改密码成功后自动登出
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)

  } catch (error) {
    console.error('修改密码失败:', error)
    loadPasswordCaptcha() // 修改密码失败时刷新验证码
  } finally {
    passwordLoading.value = false
  }
}

// 加载密码修改验证码
const loadPasswordCaptcha = async () => {
  try {
    const result = await captchaApi.generateCaptcha()
    passwordCaptchaImage.value = result.captchaImage
    passwordForm.captchaKey = result.captchaKey
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    captcha: '',
    captchaKey: ''
  })
  passwordFormRef.value?.clearValidate()
  loadPasswordCaptcha() // 重置时重新加载验证码
}

// 头像上传前的校验
const beforeAvatarUpload = (rawFile) => {
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

// 头像文件选择变化
const handleAvatarChange = async (uploadFile) => {
  if (!uploadFile.raw) return

  try {
    avatarUploading.value = true
    const avatarUrl = await userApi.uploadAvatar(uploadFile.raw)

    // 更新头像显示
    profileForm.avatar = avatarUrl

    // 更新用户存储信息
    const currentUserInfo = userStore.userInfo
    if (currentUserInfo) {
      currentUserInfo.avatar = avatarUrl
      userStore.setUserInfo(currentUserInfo)
    }

    ElMessage.success('头像上传成功')
  } catch (error) {
    console.error('头像上传失败:', error)
    ElMessage.error('头像上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 跳转到朋友圈
const goToMoments = () => {
  router.push('/moments')
}

// 加载积分余额
const loadPointsBalance = async () => {
  pointsLoading.value = true
  try {
    const response = await pointsApi.getPointsBalance()
    pointsBalance.value = response
  } catch (error) {
    console.error('加载积分信息失败:', error)
    // 不显示错误提示，避免干扰用户体验
  } finally {
    pointsLoading.value = false
  }
}

// 快速打卡
const handleQuickCheckin = async () => {
  if (pointsBalance.value?.hasCheckedToday) {
    ElMessage.warning('今日已打卡，请勿重复操作')
    return
  }

  checkinLoading.value = true

  try {
    const response = await pointsApi.checkin()

    ElMessage.success({
      message: `打卡成功！获得 ${response.pointsEarned} 积分`,
      duration: 3000
    })

    // 刷新积分余额
    await loadPointsBalance()

  } catch (error) {
    console.error('打卡失败:', error)
    ElMessage.error(error.message || '打卡失败，请重试')
  } finally {
    checkinLoading.value = false
  }
}

// 跳转到积分页面
const goToPointsPage = () => {
  router.push('/points')
}

onMounted(() => {
  loadUserInfo()
  loadPasswordCaptcha() // 页面加载时获取密码修改验证码
  loadPointsBalance() // 加载积分信息
})
</script>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  position: relative;
  overflow-x: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background:
      radial-gradient(circle at 15% 15%, rgba(255, 255, 255, 0.1) 0%, transparent 50%),
      radial-gradient(circle at 85% 85%, rgba(255, 255, 255, 0.08) 0%, transparent 50%);
    animation: float 15s ease-in-out infinite;
  }
}

.header {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(20px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 10;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
}

.logo {
  cursor: pointer;
  transition: transform 0.3s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.logo h2 {
  color: white;
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  background: linear-gradient(45deg, #fff, #e3f2fd);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  display: flex;
  gap: 16px;
}

.nav-menu :deep(.el-button) {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white !important;
  font-weight: 600;
  padding: 12px 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    border-color: rgba(255, 255, 255, 0.5);
    color: white !important;
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  }

  /* 确保图标和文字都是白色 */
  .el-icon,
  span {
    color: white !important;
  }
}

.main-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
  position: relative;
  z-index: 1;
}

.profile-section {
  display: flex;
  flex-direction: column;
  gap: 32px;
  animation: slide-up 0.8s ease-out;

  @keyframes slide-up {
    from {
      opacity: 0;
      transform: translateY(30px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

.profile-card,
.password-card,
.points-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.15),
    0 8px 16px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #74b9ff, #0984e3);
  }

  &:hover {
    transform: translateY(-5px);
    box-shadow:
      0 25px 50px rgba(0, 0, 0, 0.2),
      0 12px 24px rgba(0, 0, 0, 0.15);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px 0;
}

.card-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.profile-content {
  padding: 32px;
}

.profile-content :deep(.el-form-item) {
  margin-bottom: 24px;

  .el-form-item__label {
    font-weight: 600;
    color: #2c3e50;
  }

  .el-input__wrapper {
    border-radius: 12px;
    border: 2px solid rgba(116, 185, 255, 0.1);
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(116, 185, 255, 0.3);
      background: rgba(255, 255, 255, 0.9);
    }

    &.is-focus {
      border-color: #74b9ff;
      background: white;
      box-shadow: 0 0 0 3px rgba(116, 185, 255, 0.1);
    }

    &.is-disabled {
      background: rgba(245, 247, 250, 0.8);
      border-color: rgba(200, 200, 200, 0.3);
    }
  }
}

.captcha-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.captcha-image {
  cursor: pointer;
  border: 2px solid rgba(116, 185, 255, 0.2);
  border-radius: 12px;
  overflow: hidden;
  width: 160px;
  height: 40px;
  transition: all 0.3s ease;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, rgba(116, 185, 255, 0.1), rgba(9, 132, 227, 0.1));
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    border-color: #74b9ff;
    transform: scale(1.05);
    box-shadow: 0 4px 12px rgba(116, 185, 255, 0.2);

    &::before {
      opacity: 1;
    }
  }
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

/* 按钮样式优化 */
:deep(.el-button) {
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;

  &.el-button--primary {
    background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
    border: none;

    &:hover {
      background: linear-gradient(135deg, #0984e3 0%, #2d3436 100%);
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(116, 185, 255, 0.4);
    }
  }

  &:not(.el-button--primary) {
    background: rgba(116, 185, 255, 0.1);
    border: 2px solid rgba(116, 185, 255, 0.2);
    color: #74b9ff;

    &:hover {
      background: rgba(116, 185, 255, 0.2);
      border-color: #74b9ff;
      transform: translateY(-1px);
    }
  }
}

/* 头像部分样式 */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }
}

.avatar-display {
  position: relative;

  :deep(.el-avatar) {
    border: 3px solid rgba(116, 185, 255, 0.2);
    box-shadow: 0 8px 24px rgba(116, 185, 255, 0.15);
    transition: all 0.3s ease;

    &:hover {
      border-color: rgba(116, 185, 255, 0.4);
      transform: scale(1.05);
      box-shadow: 0 12px 32px rgba(116, 185, 255, 0.25);
    }
  }
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;

  @media (max-width: 768px) {
    align-items: center;
    text-align: center;
  }
}

.avatar-tips {
  color: #666;
  font-size: 12px;
  line-height: 1.4;

  p {
    margin: 0;
  }
}

:deep(.el-upload) {
  .el-button {
    border-radius: 12px;
    padding: 10px 20px;
    font-weight: 600;
    background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
    border: none;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(135deg, #0984e3 0%, #2d3436 100%);
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(116, 185, 255, 0.4);
    }
  }
}

/* 表单行样式 */
:deep(.el-row) {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .header-content {
    height: 60px;
    padding: 0 16px;
  }

  .logo h2 {
    font-size: 24px;
  }

  .nav-menu {
    gap: 8px;
  }

  .nav-menu :deep(.el-button) {
    padding: 8px 12px;
    font-size: 14px;
    color: white !important;

    /* 移动端也确保图标和文字都是白色 */
    .el-icon,
    span {
      color: white !important;
    }
  }

  .main-content {
    padding: 20px 16px;
  }

  .profile-card,
  .password-card {
    margin: 0 8px;
  }

  .card-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
    padding: 20px 24px 0;
  }

  .profile-content {
    padding: 24px;
  }

  .captcha-row {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .captcha-image {
    width: 100%;
    max-width: 250px;
    align-self: center;
  }
}

/* 积分卡片样式 */
.points-card {
  background: linear-gradient(135deg, rgba(103, 194, 58, 0.95) 0%, rgba(116, 185, 255, 0.95) 100%);
  color: white;

  &::before {
    background: linear-gradient(90deg, #67c23a, #74b9ff);
  }

  .card-header h3 {
    color: white;
    background: none;
    -webkit-background-clip: unset;
    -webkit-text-fill-color: white;
    background-clip: unset;
    text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  }
}

.points-content {
  padding: 24px 32px;
}

.points-overview {
  .points-main {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 24px;

    @media (max-width: 768px) {
      flex-direction: column;
      gap: 20px;
      text-align: center;
    }
  }
}

.points-balance {
  display: flex;
  flex-direction: column;
  align-items: center;

  .balance-number {
    font-size: 42px;
    font-weight: 800;
    margin-bottom: 4px;
    text-shadow: 0 2px 8px rgba(0,0,0,0.2);
    background: linear-gradient(45deg, #fff, rgba(255,255,255,0.8));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .balance-label {
    font-size: 14px;
    opacity: 0.9;
    font-weight: 500;
  }
}

.checkin-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-end;

  @media (max-width: 768px) {
    align-items: center;
  }
}

.checkin-stats {
  display: flex;
  gap: 20px;

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;

    .stat-number {
      font-size: 24px;
      font-weight: 700;
      margin-bottom: 2px;
      text-shadow: 0 1px 4px rgba(0,0,0,0.2);
    }

    .stat-label {
      font-size: 12px;
      opacity: 0.9;
      font-weight: 500;
    }
  }
}

.checkin-btn {
  background: rgba(255,255,255,0.2);
  border: 2px solid rgba(255,255,255,0.3);
  border-radius: 12px;
  color: white;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(10px);

  &:hover:not(:disabled) {
    background: rgba(255,255,255,0.3);
    border-color: rgba(255,255,255,0.5);
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.2);
  }

  &:disabled {
    opacity: 0.8;
    cursor: not-allowed;
  }

  &.checked {
    background: rgba(76, 175, 80, 0.8);
    border-color: rgba(76, 175, 80, 0.9);
  }

  .loading-icon {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
