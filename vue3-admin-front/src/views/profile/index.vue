<template>
  <CnPage class="profile-page" surface="transparent" max-width="1180px">
    <CnPageHeader
      title="个人中心"
      description="查看当前管理员资料、角色权限和最近登录信息。"
      eyebrow="Admin Profile"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">{{ userStore.username || '管理员' }}</CnStatusTag>
        <CnStatusTag v-if="currentRoles" type="success">{{ currentRoles }}</CnStatusTag>
      </template>
      <template #actions>
        <el-button type="primary" :icon="Edit" @click="showEditProfile = true">编辑资料</el-button>
        <el-button type="warning" :icon="Key" @click="showChangePassword = true">修改密码</el-button>
      </template>
    </CnPageHeader>

    <div class="profile-layout">
      <CnSection surface="panel" class="identity-card">
        <div class="avatar-section">
          <el-avatar :size="84" :src="userStore.avatar">
            {{ avatarText }}
          </el-avatar>
          <div class="user-info">
            <h2>{{ userStore.realName || userStore.username || '管理员' }}</h2>
            <p>@{{ userStore.username || 'unknown' }}</p>
            <CnStatusTag v-if="genderText" type="neutral" size="sm" subtle>{{ genderText }}</CnStatusTag>
          </div>
        </div>

        <div class="profile-stat-list">
          <div v-for="item in profileStats" :key="item.label" class="profile-stat-item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </CnSection>

      <CnSection title="详细信息" description="来自当前登录用户信息与权限数据。" divided>
        <el-descriptions :column="2" border v-if="userStore.userInfo">
          <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ userStore.userInfo.realName || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ userStore.userInfo.email || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userStore.userInfo.phone || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="上次登录">
            {{ formatDateTime(userStore.userInfo.lastLoginTime) || '无记录' }}
          </el-descriptions-item>
          <el-descriptions-item label="个人简介" :span="2">
            {{ userStore.userInfo.remark || '暂无简介' }}
          </el-descriptions-item>
          <el-descriptions-item label="角色权限" :span="2">
            <div v-if="roleItems.length > 0" class="tag-list">
              <CnStatusTag v-for="role in roleItems" :key="role" type="success" size="sm">
                {{ role }}
              </CnStatusTag>
            </div>
            <span v-else>暂无角色</span>
          </el-descriptions-item>
          <el-descriptions-item label="权限列表" :span="2">
            <div v-if="userStore.permissions.length > 0" class="tag-list">
              <CnStatusTag v-for="permission in userStore.permissions" :key="permission" type="info" size="sm" subtle>
                {{ permission }}
              </CnStatusTag>
            </div>
            <span v-else>暂无权限</span>
          </el-descriptions-item>
        </el-descriptions>

        <CnEmptyState
          v-else
          title="暂无用户信息"
          description="当前登录态没有返回完整用户资料。"
          icon="PF"
          surface="transparent"
        />
      </CnSection>
    </div>

    <EditProfile v-model="showEditProfile" :user-info="userStore.userInfo" @success="onProfileUpdated" />
    <ChangePassword v-model="showChangePassword" @success="onPasswordChanged" />
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Key } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem } from '@/design-system'
import EditProfile from './EditProfile.vue'
import ChangePassword from './ChangePassword.vue'

interface RoleObject {
  roleName?: string
  name?: string
}

const userStore = useUserStore()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '个人中心' }]

const showEditProfile = ref(false)
const showChangePassword = ref(false)

const roleItems = computed(() => {
  if (!Array.isArray(userStore.roles)) return []
  return userStore.roles
    .map((role: string | RoleObject) => {
      if (typeof role === 'object') {
        return role.roleName || role.name || ''
      }
      return role
    })
    .filter(Boolean)
})

const currentRoles = computed(() => roleItems.value.join('、'))

const genderText = computed(() => {
  const gender = userStore.userInfo?.gender
  if (gender === 1) return '男'
  if (gender === 2) return '女'
  if (gender === 0) return '未知'
  return ''
})

const avatarText = computed(() => {
  const name = userStore.realName || userStore.username || '管'
  return name.charAt(0)
})

const formatDate = (date?: string) => {
  if (!date) return '暂无'
  return new Date(date).toLocaleDateString('zh-CN')
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '暂无'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const profileStats = computed(() => [
  { label: '上次登录', value: formatDate(userStore.userInfo?.lastLoginTime) },
  { label: '邮箱地址', value: userStore.userInfo?.email || '未设置' },
  { label: '手机号码', value: userStore.userInfo?.phone || '未设置' },
  { label: '角色数量', value: `${roleItems.value.length}` }
])

const onProfileUpdated = () => {
  ElMessage.success('个人信息已更新')
}

const onPasswordChanged = () => {
  ElMessage.success('密码修改成功，即将重新登录')
}
</script>

<style scoped>
.profile-page {
  min-height: 100%;
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: var(--cn-space-5);
  align-items: start;
}

.identity-card :deep(.cn-section__body) {
  display: grid;
  gap: var(--cn-space-5);
}

.avatar-section {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-4);
  text-align: center;
}

.user-info {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-2);
}

.user-info h2,
.user-info p {
  margin: 0;
}

.user-info h2 {
  color: var(--cn-color-text-primary);
  font-size: 22px;
  font-weight: 760;
  overflow-wrap: anywhere;
}

.user-info p {
  color: var(--cn-color-text-secondary);
  font-size: 14px;
}

.profile-stat-list {
  display: grid;
  gap: var(--cn-space-3);
}

.profile-stat-item {
  display: grid;
  gap: var(--cn-space-1);
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.profile-stat-item span {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.profile-stat-item strong {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  overflow-wrap: anywhere;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

@media (max-width: 960px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
