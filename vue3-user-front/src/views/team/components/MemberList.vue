<template>
  <div class="member-list">
    <!-- 成员统计 -->
    <div class="member-stats">
      <span class="stat-item">
        <el-icon><User /></el-icon>
        共 {{ members.length }} 名成员
      </span>
      <el-button v-if="isAdmin" size="small" @click="showApplications = true">
        <el-icon><Bell /></el-icon>
        申请列表
        <span v-if="pendingCount > 0" class="pending-badge">{{ pendingCount }}</span>
      </el-button>
    </div>
    
    <!-- 成员列表 -->
    <div v-loading="loading" class="members">
      <div v-if="members.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无成员" :image-size="80" />
      </div>
      
      <div 
        v-for="member in members" 
        :key="member.userId" 
        class="member-item"
      >
        <div class="member-avatar">
          <img v-if="member.userAvatar" :src="member.userAvatar" />
          <span v-else>{{ member.userName?.charAt(0) || '用' }}</span>
        </div>
        
        <div class="member-info">
          <div class="member-name">
            {{ member.userName }}
            <span v-if="member.userId === currentUserId" class="me-tag">我</span>
          </div>
          <div class="member-role">
            <span v-if="member.memberRole === 1" class="role-badge leader">
              <el-icon><Trophy /></el-icon>组长
            </span>
            <span v-else-if="member.memberRole === 2" class="role-badge admin">
              <el-icon><Star /></el-icon>管理员
            </span>
            <span v-else class="role-badge member">成员</span>
            <span v-if="member.isMuted" class="role-badge muted">禁言中</span>
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
        
        <div class="member-actions" v-if="canManage(member)">
          <el-dropdown trigger="click">
            <el-button text size="small">
              <el-icon><MoreFilled /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 设为管理员 -->
                <el-dropdown-item 
                  v-if="isLeader && member.memberRole === 3"
                  @click="setRole(member, 2)"
                >
                  <el-icon><Star /></el-icon>设为管理员
                </el-dropdown-item>
                <!-- 取消管理员 -->
                <el-dropdown-item 
                  v-if="isLeader && member.memberRole === 2"
                  @click="setRole(member, 3)"
                >
                  <el-icon><Star /></el-icon>取消管理员
                </el-dropdown-item>
                <!-- 转让组长 -->
                <el-dropdown-item 
                  v-if="isLeader && member.memberRole !== 1"
                  @click="transferLeader(member)"
                >
                <el-icon><Trophy /></el-icon>转让组长
                </el-dropdown-item>
                <!-- 禁言/解禁 -->
                <el-dropdown-item 
                  v-if="canMute(member)"
                  @click="toggleMute(member)"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  {{ member.isMuted ? '解除禁言' : '禁言' }}
                </el-dropdown-item>
                <!-- 移出小组 -->
                <el-dropdown-item 
                  v-if="canRemove(member)"
                  @click="removeMember(member)"
                  divided
                >
                  <el-icon><Remove /></el-icon>移出小组
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
    
    <!-- 申请列表弹窗 -->
    <el-dialog v-model="showApplications" title="入组申请" width="500px">
      <div class="application-list" v-loading="loadingApps">
        <div v-if="applications.length === 0" class="empty-state">
          <el-empty description="暂无申请" :image-size="60" />
        </div>
        
        <div 
          v-for="app in applications" 
          :key="app.id" 
          class="application-item"
        >
          <div class="app-avatar">
            <img v-if="app.userAvatar" :src="app.userAvatar" />
            <span v-else>{{ app.userName?.charAt(0) || '用' }}</span>
          </div>
          
          <div class="app-info">
            <div class="app-name">{{ app.userName }}</div>
            <div class="app-reason" v-if="app.applyReason">{{ app.applyReason }}</div>
            <div class="app-time">{{ formatTime(app.createTime) }}</div>
          </div>
          
          <div class="app-actions" v-if="app.status === 0">
            <el-button type="primary" size="small" @click="handleApprove(app)">
              通过
            </el-button>
            <el-button size="small" @click="handleReject(app)">
              拒绝
            </el-button>
          </div>
          <div class="app-status" v-else>
            <span v-if="app.status === 1" class="status approved">已通过</span>
            <span v-else-if="app.status === 2" class="status rejected">已拒绝</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Bell, Trophy, Star, MoreFilled, ChatDotRound, Remove } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  teamId: { type: [String, Number], required: true },
  isAdmin: { type: Boolean, default: false },
  isLeader: { type: Boolean, default: false },
  currentUserId: { type: [String, Number], default: null }
})

const emit = defineEmits(['refresh'])

const members = ref([])
const loading = ref(false)

// 申请列表
const showApplications = ref(false)
const applications = ref([])
const loadingApps = ref(false)
const pendingCount = computed(() => 
  applications.value.filter(a => a.status === 0).length
)

onMounted(() => {
  loadMembers()
  if (props.isAdmin) {
    loadApplications()
  }
})

watch(() => props.teamId, () => {
  loadMembers()
  if (props.isAdmin) {
    loadApplications()
  }
})

const loadMembers = async () => {
  loading.value = true
  try {
    const response = await teamApi.getMemberList(props.teamId)
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
    const response = await teamApi.getApplicationList(props.teamId)
    applications.value = response || []
  } catch (error) {
    console.error('加载申请列表失败:', error)
  } finally {
    loadingApps.value = false
  }
}

// 判断是否可以管理该成员
const canManage = (member) => {
  if (!props.isAdmin) return false
  if (member.userId === props.currentUserId) return false
  if (member.memberRole === 1) return false // 不能管理组长
  if (!props.isLeader && member.memberRole === 2) return false // 管理员不能管理其他管理员
  return true
}

// 判断是否可以禁言
const canMute = (member) => {
  return canManage(member) && member.memberRole === 3
}

// 判断是否可以移出
const canRemove = (member) => {
  return canManage(member)
}

// 设置角色
const setRole = async (member, role) => {
  try {
    await teamApi.setMemberRole(props.teamId, member.userId, role)
    ElMessage.success('操作成功')
    loadMembers()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 转让组长
const transferLeader = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将组长转让给 ${member.userName} 吗？转让后你将成为普通成员。`, 
      '转让组长', 
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await teamApi.transferLeader(props.teamId, member.userId)
    ElMessage.success('转让成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('转让失败:', error)
    }
  }
}

// 禁言/解禁
const toggleMute = async (member) => {
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

const promptMuteMinutes = async (member) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入对 ${member.userName} 的禁言时长（分钟）`,
      '禁言成员',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '60',
        inputPattern: /^(?:[1-9]\d{0,4})$/,
        inputErrorMessage: '请输入 1-10080 之间的整数分钟'
      }
    )
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

// 移出成员
const removeMember = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.userName} 移出小组吗？`, 
      '移出成员', 
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await teamApi.removeMember(props.teamId, member.userId)
    ElMessage.success('已移出')
    loadMembers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移出失败:', error)
    }
  }
}

// 通过申请
const handleApprove = async (app) => {
  try {
    await teamApi.approveApplication(props.teamId, app.id)
    ElMessage.success('已通过')
    loadApplications()
    loadMembers()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 拒绝申请
const handleReject = async (app) => {
  try {
    await teamApi.rejectApplication(props.teamId, app.id)
    ElMessage.success('已拒绝')
    loadApplications()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

defineExpose({ loadMembers, loadApplications })
</script>

<style lang="scss" scoped>
.member-list {
  .empty-state {
    padding: 20px 0;
  }
}

.member-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
  
  .stat-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: #666;
  }
  
  .pending-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    background: #f56c6c;
    color: white;
    font-size: 12px;
    border-radius: 9px;
    margin-left: 4px;
  }
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: background 0.2s;
  
  &:hover {
    background: #f8f9fc;
  }
  
  & + .member-item {
    border-top: 1px solid #f5f5f5;
  }
}

.member-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  flex-shrink: 0;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  span {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    color: white;
    font-size: 18px;
    font-weight: 500;
  }
}

.member-info {
  flex: 1;
  min-width: 0;
  
  .member-name {
    font-size: 14px;
    font-weight: 500;
    color: #333;
    display: flex;
    align-items: center;
    gap: 6px;
    
    .me-tag {
      padding: 1px 6px;
      background: #e8f4fd;
      color: #409eff;
      font-size: 11px;
      border-radius: 4px;
      font-weight: normal;
    }
  }
  
  .member-role {
    margin-top: 4px;
    
    .role-badge {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      padding: 2px 8px;
      font-size: 12px;
      border-radius: 4px;
      
      &.leader {
        background: #fdf2e9;
        color: #e6a23c;
      }
      
      &.admin {
        background: #e8f4fd;
        color: #409eff;
      }
      
      &.member {
        background: #f5f7fa;
        color: #909399;
      }

      &.muted {
        background: #fef0f0;
        color: #f56c6c;
      }
    }
  }
}

.member-stats-info {
  display: flex;
  gap: 16px;
  
  .stat {
    text-align: center;
    
    .stat-num {
      display: block;
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .stat-label {
      font-size: 12px;
      color: #999;
    }
  }
}

.member-actions {
  flex-shrink: 0;
}

// 申请列表
.application-list {
  max-height: 400px;
  overflow-y: auto;
  
  .empty-state {
    padding: 30px 0;
  }
}

.application-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  
  & + .application-item {
    border-top: 1px solid #f0f0f0;
  }
  
  .app-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    flex-shrink: 0;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    span {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      color: white;
      font-size: 16px;
    }
  }
  
  .app-info {
    flex: 1;
    min-width: 0;
    
    .app-name {
      font-size: 14px;
      font-weight: 500;
      color: #333;
    }
    
    .app-reason {
      font-size: 13px;
      color: #666;
      margin-top: 4px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .app-time {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }
  
  .app-actions {
    display: flex;
    gap: 8px;
    flex-shrink: 0;
  }
  
  .app-status {
    .status {
      padding: 4px 12px;
      font-size: 12px;
      border-radius: 4px;
      
      &.approved {
        background: #f0f9eb;
        color: #67c23a;
      }
      
      &.rejected {
        background: #fef0f0;
        color: #f56c6c;
      }
    }
  }
}
</style>
