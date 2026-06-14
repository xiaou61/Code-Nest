<template>
  <div class="member-list">
    <div class="member-stats">
      <CnStatusTag type="brand" size="sm">
        <el-icon><User /></el-icon>
        共 {{ members.length }} 名成员
      </CnStatusTag>
      <el-button v-if="isAdmin" size="small" @click="showApplications = true">
        <el-icon><Bell /></el-icon>
        申请列表
        <CnStatusTag v-if="pendingCount > 0" type="danger" size="sm">{{ pendingCount }}</CnStatusTag>
      </el-button>
    </div>

    <div v-loading="loading" class="members">
      <CnEmptyState
        v-if="members.length === 0 && !loading"
        title="暂无成员"
        description="小组成员加入后会显示在这里。"
        icon="MB"
        size="sm"
        surface="transparent"
      />

      <article v-for="member in members" :key="member.userId" class="member-item">
        <div class="member-avatar">
          <img v-if="member.userAvatar" :src="member.userAvatar" :alt="member.userName || '成员头像'" />
          <span v-else>{{ member.userName?.charAt(0) || '用' }}</span>
        </div>

        <div class="member-info">
          <div class="member-name">
            {{ member.userName || '匿名成员' }}
            <CnStatusTag v-if="member.userId === currentUserId" type="info" size="sm" :dot="false" subtle>我</CnStatusTag>
          </div>
          <div class="member-role">
            <CnStatusTag :type="getRoleTone(member.memberRole)" size="sm" :dot="false" subtle>
              <el-icon v-if="Number(member.memberRole) === 1"><Trophy /></el-icon>
              <el-icon v-else-if="Number(member.memberRole) === 2"><Star /></el-icon>
              {{ getRoleText(member.memberRole) }}
            </CnStatusTag>
            <CnStatusTag v-if="member.isMuted" type="danger" size="sm" :dot="false">禁言中</CnStatusTag>
          </div>
        </div>

        <div class="member-stats-info">
          <span class="stat">
            <span class="stat-num">{{ member.checkinCount || 0 }}</span>
            <span class="stat-label">打卡</span>
          </span>
          <span class="stat">
            <span class="stat-num">{{ member.streakDays || 0 }}</span>
            <span class="stat-label">连续</span>
          </span>
        </div>

        <div v-if="canManage(member)" class="member-actions">
          <el-dropdown trigger="click">
            <el-button text size="small" aria-label="成员操作">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="isLeader && Number(member.memberRole) === 3" @click="setRole(member, 2)">
                  <el-icon><Star /></el-icon>
                  设为管理员
                </el-dropdown-item>
                <el-dropdown-item v-if="isLeader && Number(member.memberRole) === 2" @click="setRole(member, 3)">
                  <el-icon><Star /></el-icon>
                  取消管理员
                </el-dropdown-item>
                <el-dropdown-item v-if="isLeader && Number(member.memberRole) !== 1" @click="transferLeader(member)">
                  <el-icon><Trophy /></el-icon>
                  转让组长
                </el-dropdown-item>
                <el-dropdown-item v-if="canMute(member)" @click="toggleMute(member)">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ member.isMuted ? '解除禁言' : '禁言' }}
                </el-dropdown-item>
                <el-dropdown-item v-if="canRemove(member)" @click="removeMember(member)" divided>
                  <el-icon><Remove /></el-icon>
                  移出小组
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </article>
    </div>

    <el-dialog v-model="showApplications" title="入组申请" width="500px">
      <div v-loading="loadingApps" class="application-list">
        <CnEmptyState
          v-if="applications.length === 0"
          title="暂无申请"
          description="新的入组申请会显示在这里。"
          icon="AP"
          size="sm"
          surface="transparent"
        />

        <article v-for="app in applications" :key="app.id" class="application-item">
          <div class="app-avatar">
            <img v-if="app.userAvatar" :src="app.userAvatar" :alt="app.userName || '申请人头像'" />
            <span v-else>{{ app.userName?.charAt(0) || '用' }}</span>
          </div>

          <div class="app-info">
            <div class="app-name">{{ app.userName || '匿名用户' }}</div>
            <div v-if="app.applyReason" class="app-reason">{{ app.applyReason }}</div>
            <div class="app-time">{{ formatTime(app.createTime) }}</div>
          </div>

          <div v-if="Number(app.status) === 0" class="app-actions">
            <el-button type="primary" size="small" @click="handleApprove(app)">通过</el-button>
            <el-button size="small" @click="handleReject(app)">拒绝</el-button>
          </div>
          <div v-else class="app-status">
            <CnStatusTag :type="Number(app.status) === 1 ? 'success' : 'danger'" size="sm">
              {{ Number(app.status) === 1 ? '已通过' : '已拒绝' }}
            </CnStatusTag>
          </div>
        </article>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, ChatDotRound, MoreFilled, Remove, Star, Trophy, User } from '@element-plus/icons-vue'
import { CnEmptyState, CnStatusTag, type CnTone } from '@/design-system'
import teamApi from '@/api/team'

interface MemberRecord {
  userId: number | string
  userAvatar?: string
  userName?: string
  memberRole?: number | string
  isMuted?: boolean
  checkinCount?: number
  streakDays?: number
}

interface ApplicationRecord {
  id: number | string
  userAvatar?: string
  userName?: string
  applyReason?: string
  createTime?: string
  status?: number | string
}

const props = withDefaults(
  defineProps<{
    teamId: string | number
    isAdmin?: boolean
    isLeader?: boolean
    currentUserId?: string | number | null
  }>(),
  {
    isAdmin: false,
    isLeader: false,
    currentUserId: null
  }
)

const emit = defineEmits<{
  refresh: []
}>()

const members = ref<MemberRecord[]>([])
const loading = ref(false)

const showApplications = ref(false)
const applications = ref<ApplicationRecord[]>([])
const loadingApps = ref(false)
const pendingCount = computed(() => applications.value.filter((app) => Number(app.status) === 0).length)

onMounted(() => {
  loadMembers()
  if (props.isAdmin) {
    loadApplications()
  }
})

watch(
  () => props.teamId,
  () => {
    loadMembers()
    if (props.isAdmin) {
      loadApplications()
    }
  }
)

const loadMembers = async () => {
  loading.value = true
  try {
    const response = (await teamApi.getMemberList(props.teamId)) as MemberRecord[]
    members.value = response || []
  } catch (error) {
    console.error('加载成员列表失败:', error)
  } finally {
    loading.value = false
  }
}

const loadApplications = async () => {
  loadingApps.value = true
  try {
    const response = (await teamApi.getApplicationList(props.teamId)) as ApplicationRecord[]
    applications.value = response || []
  } catch (error) {
    console.error('加载申请列表失败:', error)
  } finally {
    loadingApps.value = false
  }
}

const canManage = (member: MemberRecord) => {
  if (!props.isAdmin) return false
  if (member.userId === props.currentUserId) return false
  if (Number(member.memberRole) === 1) return false
  if (!props.isLeader && Number(member.memberRole) === 2) return false
  return true
}

const canMute = (member: MemberRecord) => canManage(member) && Number(member.memberRole) === 3

const canRemove = (member: MemberRecord) => canManage(member)

const setRole = async (member: MemberRecord, role: number) => {
  try {
    await teamApi.setMemberRole(props.teamId, member.userId, role)
    ElMessage.success('操作成功')
    loadMembers()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const transferLeader = async (member: MemberRecord) => {
  try {
    await ElMessageBox.confirm(`确定要将组长转让给 ${member.userName} 吗？转让后你将成为普通成员。`, '转让组长', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.transferLeader(props.teamId, member.userId)
    ElMessage.success('转让成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('转让失败:', error)
    }
  }
}

const toggleMute = async (member: MemberRecord) => {
  try {
    if (member.isMuted) {
      await teamApi.unmuteMember(props.teamId, member.userId)
      ElMessage.success('已解除禁言')
    } else {
      const minutes = await promptMuteMinutes(member)
      if (minutes === null) {
        return
      }
      await teamApi.muteMember(props.teamId, member.userId, minutes)
      ElMessage.success(`已禁言 ${minutes} 分钟`)
    }
    loadMembers()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const promptMuteMinutes = async (member: MemberRecord) => {
  try {
    const { value } = await ElMessageBox.prompt(`请输入对 ${member.userName} 的禁言时长（分钟）`, '禁言成员', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: '60',
      inputPattern: /^(?:[1-9]\d{0,4})$/,
      inputErrorMessage: '请输入 1-10080 之间的整数分钟'
    })
    const minutes = Number(value)
    if (!Number.isInteger(minutes) || minutes < 1 || minutes > 10080) {
      ElMessage.warning('禁言时长需为 1-10080 分钟')
      return null
    }
    return minutes
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return null
    }
    throw error
  }
}

const removeMember = async (member: MemberRecord) => {
  try {
    await ElMessageBox.confirm(`确定要将 ${member.userName} 移出小组吗？`, '移出成员', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.removeMember(props.teamId, member.userId)
    ElMessage.success('已移出')
    loadMembers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移出失败:', error)
    }
  }
}

const handleApprove = async (app: ApplicationRecord) => {
  try {
    await teamApi.approveApplication(props.teamId, app.id)
    ElMessage.success('已通过')
    loadApplications()
    loadMembers()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const handleReject = async (app: ApplicationRecord) => {
  try {
    await teamApi.rejectApplication(props.teamId, app.id)
    ElMessage.success('已拒绝')
    loadApplications()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const getRoleText = (role: unknown) => {
  const map: Record<string, string> = {
    1: '组长',
    2: '管理员',
    3: '成员'
  }
  return map[String(role)] || '成员'
}

const getRoleTone = (role: unknown): CnTone => {
  const map: Record<string, CnTone> = {
    1: 'warning',
    2: 'brand',
    3: 'neutral'
  }
  return map[String(role)] || 'neutral'
}

const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

defineExpose({ loadMembers, loadApplications })
</script>

<style scoped>
.member-list {
  min-width: 0;
}

.member-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  margin-bottom: var(--cn-space-4);
}

.member-stats :deep(.cn-status-tag__label),
.member-role :deep(.cn-status-tag__label) {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-1);
}

.members {
  display: grid;
  min-height: 120px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
  transition: background-color var(--cn-motion-base) var(--cn-ease-out);
}

.member-item:hover {
  background: var(--cn-color-bg-surface-muted);
}

.member-item + .member-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.member-avatar,
.app-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
  flex-shrink: 0;
  font-weight: 800;
}

.member-avatar {
  width: 44px;
  height: 44px;
  font-size: 18px;
}

.app-avatar {
  width: 40px;
  height: 40px;
  font-size: 16px;
}

.member-avatar img,
.app-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-info,
.app-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.member-role {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-2);
}

.member-stats-info {
  display: flex;
  gap: var(--cn-space-4);
  flex-shrink: 0;
}

.stat {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-1);
}

.stat-num {
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 17px;
  font-weight: 750;
  line-height: 1;
}

.stat-label {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
  font-weight: 650;
}

.member-actions {
  flex-shrink: 0;
}

.application-list {
  display: grid;
  max-height: 400px;
  overflow-y: auto;
}

.application-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
  padding: var(--cn-space-3);
  border-radius: var(--cn-radius-card);
}

.application-item + .application-item {
  border-top: 1px solid var(--cn-color-border-subtle);
}

.app-name {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 700;
}

.app-reason {
  margin-top: var(--cn-space-1);
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-time {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.app-actions {
  display: flex;
  gap: var(--cn-space-2);
  flex-shrink: 0;
}

.app-status {
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .member-stats,
  .member-item,
  .application-item {
    align-items: flex-start;
  }

  .member-stats,
  .member-item {
    display: grid;
  }

  .member-stats-info {
    justify-content: flex-start;
  }
}
</style>
