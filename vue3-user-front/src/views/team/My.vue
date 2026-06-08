<template>
  <CnPage class="my-team-container" surface="transparent" max-width="1180px">
    <CnPageHeader
      title="我的小组"
      description="管理已加入的小组、自己创建的小组，以及小组申请记录。"
      eyebrow="Learning Teams"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">已加入 {{ joinedCount }}</CnStatusTag>
        <CnStatusTag type="success" size="sm">我创建的 {{ createdCount }}</CnStatusTag>
        <CnStatusTag v-if="pendingCount" type="warning" size="sm">待处理 {{ pendingCount }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="goToCreate">创建小组</el-button>
        <el-button plain :icon="Search" @click="goToSquare">发现小组</el-button>
      </template>
    </CnPageHeader>

    <CnSection surface="panel" class="tab-container">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="已加入" name="joined">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              已加入
              <span v-if="joinedCount" class="tab-count">{{ joinedCount }}</span>
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="我创建的" name="created">
          <template #label>
            <span class="tab-label">
              <el-icon><Star /></el-icon>
              我创建的
              <span v-if="createdCount" class="tab-count">{{ createdCount }}</span>
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="申请记录" name="applications">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              申请记录
              <span v-if="pendingCount" class="tab-count pending">{{ pendingCount }}</span>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </CnSection>

    <!-- 内容区域 -->
    <CnSection
      :title="sectionTitle"
      :description="sectionDescription"
      surface="panel"
      divided
      class="content-area"
    >
      <!-- 已加入小组列表 -->
      <div v-if="activeTab === 'joined'" v-loading="loadingJoined" class="team-list">
        <TeamCard 
          v-for="team in joinedList" 
          :key="team.id" 
          :team="team"
          @refresh="loadJoinedTeams"
        />
        <CnEmptyState
          v-if="!loadingJoined && joinedList.length === 0"
          title="还没有加入任何小组"
          description="去发现广场看看适合自己的学习小组。"
          icon="TM"
          size="sm"
        >
          <template #actions>
            <el-button type="primary" @click="goToSquare">去发现小组</el-button>
          </template>
        </CnEmptyState>
      </div>

      <!-- 我创建的小组列表 -->
      <div v-if="activeTab === 'created'" v-loading="loadingCreated" class="team-list">
        <div 
          v-for="team in createdList" 
          :key="team.id" 
          class="created-team-card"
        >
          <TeamCard :team="team" @refresh="loadCreatedTeams" />
          <div class="team-manage-bar">
            <el-button size="small" @click="goToEdit(team.id)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" @click="viewApplications(team)">
              <el-icon><Document /></el-icon>
              审批申请
              <CnStatusTag v-if="team.pendingApplications" type="danger" size="sm">
                {{ team.pendingApplications }}
              </CnStatusTag>
            </el-button>
            <el-button size="small" @click="viewInviteCode(team)">
              <el-icon><Share /></el-icon>
              邀请码
            </el-button>
            <el-button size="small" type="danger" plain @click="dissolveTeam(team)">
              <el-icon><Delete /></el-icon>
              解散
            </el-button>
          </div>
        </div>
        <CnEmptyState
          v-if="!loadingCreated && createdList.length === 0"
          title="还没有创建过小组"
          description="创建一个学习小组，邀请同伴一起推进目标。"
          icon="CT"
          size="sm"
        >
          <template #actions>
            <el-button type="primary" @click="goToCreate">创建第一个小组</el-button>
          </template>
        </CnEmptyState>
      </div>

      <!-- 申请记录列表 -->
      <div v-if="activeTab === 'applications'" v-loading="loadingApplications" class="application-list">
        <div 
          v-for="app in applicationList" 
          :key="app.id" 
          class="application-card"
          :class="getStatusClass(app.status)"
        >
          <div class="app-team-info" @click="goToDetail(app.teamId)">
            <div class="team-avatar">
              <img v-if="app.teamAvatar" :src="app.teamAvatar" />
              <span v-else>{{ app.teamName?.charAt(0) || '组' }}</span>
            </div>
            <div class="team-details">
              <div class="team-name">{{ app.teamName }}</div>
              <div class="apply-time">申请时间：{{ formatTime(app.createTime) }}</div>
            </div>
          </div>
          <div class="app-reason" v-if="app.applyReason">
            <span class="label">申请理由：</span>
            {{ app.applyReason }}
          </div>
          <div class="app-status">
            <CnStatusTag :type="getStatusType(app.status)" size="sm">
              {{ getStatusText(app.status) }}
            </CnStatusTag>
            <span v-if="app.status === 2" class="reject-reason">
              拒绝原因：{{ app.rejectReason || '无' }}
            </span>
          </div>
          <div class="app-actions" v-if="app.status === 0">
            <el-button size="small" @click="cancelApplication(app)">取消申请</el-button>
          </div>
        </div>
        <CnEmptyState
          v-if="!loadingApplications && applicationList.length === 0"
          title="暂无申请记录"
          description="申请加入小组后，审核状态会在这里显示。"
          icon="AP"
          size="sm"
        />
      </div>
    </CnSection>

    <!-- 邀请码弹窗 -->
    <el-dialog v-model="showInviteDialog" title="邀请码" width="400px">
      <div class="invite-dialog-content" v-if="currentTeam">
        <div class="invite-team-name">{{ currentTeam.teamName }}</div>
        <div class="invite-code-box">
          <span class="invite-code">{{ inviteCode }}</span>
          <el-button type="primary" size="small" @click="copyInviteCode">
            <el-icon><DocumentCopy /></el-icon>
            复制
          </el-button>
        </div>
        <div class="invite-actions">
          <el-button @click="refreshInviteCode" :loading="refreshingCode">
            <el-icon><Refresh /></el-icon>
            刷新邀请码
          </el-button>
        </div>
        <div class="invite-tip">
          分享邀请码给好友，好友输入邀请码即可加入小组
        </div>
      </div>
    </el-dialog>

    <!-- 审批申请弹窗 -->
    <el-dialog v-model="showAppDialog" title="待审批申请" width="500px">
      <div v-loading="loadingTeamApps" class="app-dialog-content">
        <div v-if="teamApplications.length === 0" class="empty-tip">
          暂无待审批的申请
        </div>
        <div 
          v-for="app in teamApplications" 
          :key="app.id" 
          class="app-item"
        >
          <div class="applicant-info">
            <div class="applicant-avatar">{{ app.userName?.charAt(0) || '用' }}</div>
            <div class="applicant-details">
              <div class="applicant-name">{{ app.userName }}</div>
              <div class="apply-time">{{ formatTime(app.createTime) }}</div>
            </div>
          </div>
          <div class="apply-reason" v-if="app.applyReason">
            {{ app.applyReason }}
          </div>
          <div class="app-actions">
            <el-button type="primary" size="small" @click="approveApp(app)">
              通过
            </el-button>
            <el-button type="danger" plain size="small" @click="rejectApp(app)">
              拒绝
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Search, User, Star, Document, Edit, Share, Delete,
  DocumentCopy, Refresh 
} from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnTone } from '@/design-system'
import teamApi from '@/api/team'
import TeamCard from './components/TeamCard.vue'

interface TeamRecord {
  id: number | string
  teamName?: string
  teamAvatar?: string
  pendingApplications?: number
}

interface ApplicationRecord {
  id: number | string
  teamId: number | string
  teamName?: string
  teamAvatar?: string
  userName?: string
  applyReason?: string
  rejectReason?: string
  createTime?: string
  status: number
}

const router = useRouter()

// Tab状态
const activeTab = ref<'joined' | 'created' | 'applications'>('joined')

// 已加入小组
const joinedList = ref<TeamRecord[]>([])
const loadingJoined = ref(false)

// 我创建的小组
const createdList = ref<TeamRecord[]>([])
const loadingCreated = ref(false)

// 申请记录
const applicationList = ref<ApplicationRecord[]>([])
const loadingApplications = ref(false)

// 统计数量
const joinedCount = computed(() => joinedList.value.length)
const createdCount = computed(() => createdList.value.length)
const pendingCount = computed(() => applicationList.value.filter(a => a.status === 0).length)

// 邀请码弹窗
const showInviteDialog = ref(false)
const currentTeam = ref<TeamRecord | null>(null)
const inviteCode = ref('')
const refreshingCode = ref(false)

// 审批弹窗
const showAppDialog = ref(false)
const teamApplications = ref<ApplicationRecord[]>([])
const loadingTeamApps = ref(false)

const breadcrumbs = [
  { label: '首页', to: '/' },
  { label: '学习小组', to: '/team' },
  { label: '我的小组' }
]

const sectionTitle = computed(() => {
  const titleMap = {
    joined: '已加入小组',
    created: '我创建的小组',
    applications: '申请记录'
  }
  return titleMap[activeTab.value]
})

const sectionDescription = computed(() => {
  const descMap = {
    joined: '查看自己当前参与的学习小组。',
    created: '管理自己创建的小组、邀请码和待审批申请。',
    applications: '查看加入申请的审核状态。'
  }
  return descMap[activeTab.value]
})

// 页面初始化
onMounted(() => {
  loadJoinedTeams()
  loadCreatedTeams()
  loadMyApplications()
})

// Tab切换
const handleTabChange = (tab: string | number) => {
  if (tab === 'joined' && joinedList.value.length === 0) {
    loadJoinedTeams()
  } else if (tab === 'created' && createdList.value.length === 0) {
    loadCreatedTeams()
  } else if (tab === 'applications' && applicationList.value.length === 0) {
    loadMyApplications()
  }
}

// 加载已加入小组
const loadJoinedTeams = async () => {
  loadingJoined.value = true
  try {
    const response = await teamApi.getMyTeams()
    joinedList.value = response || []
  } catch (error) {
    console.error('加载已加入小组失败:', error)
  } finally {
    loadingJoined.value = false
  }
}

// 加载我创建的小组
const loadCreatedTeams = async () => {
  loadingCreated.value = true
  try {
    const response = await teamApi.getCreatedTeams()
    createdList.value = response || []
  } catch (error) {
    console.error('加载我创建的小组失败:', error)
  } finally {
    loadingCreated.value = false
  }
}

// 加载我的申请记录
const loadMyApplications = async () => {
  loadingApplications.value = true
  try {
    const response = await teamApi.getMyApplications()
    applicationList.value = response || []
  } catch (error) {
    console.error('加载申请记录失败:', error)
  } finally {
    loadingApplications.value = false
  }
}

// 查看邀请码
const viewInviteCode = async (team: TeamRecord) => {
  currentTeam.value = team
  showInviteDialog.value = true
  try {
    const code = await teamApi.getInviteCode(team.id)
    inviteCode.value = code
  } catch (error) {
    console.error('获取邀请码失败:', error)
  }
}

// 刷新邀请码
const refreshInviteCode = async () => {
  if (!currentTeam.value) return
  refreshingCode.value = true
  try {
    const code = await teamApi.refreshInviteCode(currentTeam.value.id)
    inviteCode.value = code
    ElMessage.success('邀请码已刷新')
  } catch (error) {
    console.error('刷新邀请码失败:', error)
  } finally {
    refreshingCode.value = false
  }
}

// 复制邀请码
const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value)
  ElMessage.success('已复制到剪贴板')
}

// 查看待审批申请
const viewApplications = async (team: TeamRecord) => {
  currentTeam.value = team
  showAppDialog.value = true
  loadingTeamApps.value = true
  try {
    const response = await teamApi.getApplicationList(team.id)
    teamApplications.value = (response || []).filter((a: ApplicationRecord) => a.status === 0)
  } catch (error) {
    console.error('获取申请列表失败:', error)
  } finally {
    loadingTeamApps.value = false
  }
}

// 通过申请
const approveApp = async (app: ApplicationRecord) => {
  if (!currentTeam.value) return
  try {
    await teamApi.approveApplication(currentTeam.value.id, app.id)
    ElMessage.success('已通过')
    teamApplications.value = teamApplications.value.filter((a) => a.id !== app.id)
    loadCreatedTeams()
  } catch (error) {
    console.error('审批失败:', error)
  }
}

// 拒绝申请
const rejectApp = async (app: ApplicationRecord) => {
  if (!currentTeam.value) return
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝申请', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '拒绝原因'
    })
    await teamApi.rejectApplication(currentTeam.value.id, app.id, reason)
    ElMessage.success('已拒绝')
    teamApplications.value = teamApplications.value.filter((a) => a.id !== app.id)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝失败:', error)
    }
  }
}

// 取消申请
const cancelApplication = async (app: ApplicationRecord) => {
  try {
    await ElMessageBox.confirm('确定要取消这个申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.cancelApplication(app.id)
    ElMessage.success('已取消')
    loadMyApplications()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消申请失败:', error)
    }
  }
}

// 解散小组
const dissolveTeam = async (team: TeamRecord) => {
  try {
    await ElMessageBox.confirm(
      `确定要解散小组"${team.teamName}"吗？此操作不可恢复！`,
      '警告',
      {
        confirmButtonText: '解散',
        cancelButtonText: '取消',
        type: 'error'
      }
    )
    await teamApi.dissolveTeam(team.id)
    ElMessage.success('小组已解散')
    loadCreatedTeams()
    loadJoinedTeams()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解散小组失败:', error)
    }
  }
}

// 获取申请状态文本
const getStatusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已取消' }
  return map[status] || '未知'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const map = { 0: 'pending', 1: 'approved', 2: 'rejected', 3: 'cancelled' }
  return map[status] || ''
}

const getStatusType = (status: number): CnTone => {
  const map: Record<number, CnTone> = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'neutral' }
  return map[status] || 'neutral'
}

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN', { 
    month: 'short', 
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 路由跳转
const goToCreate = () => router.push('/team/create')
const goToSquare = () => router.push('/team')
const goToDetail = (id: number | string) => router.push(`/team/${id}`)
const goToEdit = (id: number | string) => router.push(`/team/${id}/edit`)
</script>

<style lang="scss" scoped>
.my-team-container {
  min-height: calc(100vh - 68px);
}

// Tab容器
.tab-container {
  :deep(.el-tabs__header) {
    margin: 0;
  }
  
  :deep(.el-tabs__nav-wrap::after) {
    height: 1px;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  
  .tab-count {
    background: var(--cn-color-bg-surface-muted);
    color: var(--cn-color-text-secondary);
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
    
    &.pending {
      background: var(--cn-color-danger-soft);
      color: var(--cn-color-danger);
    }
  }
}

// 内容区域
.content-area {
  min-height: 300px;
}

// 小组列表
.team-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

// 我创建的小组卡片
.created-team-card {
  .team-manage-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px 20px;
    background: var(--cn-color-bg-surface-muted);
    border-radius: 0 0 12px 12px;
    margin-top: -12px;
  }
}

// 申请列表
.application-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.application-card {
  background: var(--cn-color-bg-surface);
  border-radius: var(--cn-radius-card);
  padding: 16px 20px;
  border: 1px solid var(--cn-color-border-subtle);
  
  &.pending {
    border-left: 3px solid var(--cn-color-info);
  }
  &.approved {
    border-left: 3px solid var(--cn-color-success);
  }
  &.rejected {
    border-left: 3px solid var(--cn-color-danger);
  }
  &.cancelled {
    border-left: 3px solid var(--cn-color-text-tertiary);
  }
}

.app-team-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  
  .team-avatar {
    width: 48px;
    height: 48px;
    border-radius: 10px;
    overflow: hidden;
    background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-info));
    
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
      font-weight: bold;
    }
  }
  
  .team-details {
    flex: 1;
  }
  
  .team-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--cn-color-text-primary);
    margin-bottom: 4px;
  }
  
  .apply-time {
    font-size: 12px;
    color: var(--cn-color-text-tertiary);
  }
}

.app-reason {
  margin: 12px 0;
  padding: 10px 12px;
  background: var(--cn-color-bg-surface-muted);
  border-radius: 8px;
  font-size: 14px;
  color: var(--cn-color-text-secondary);
  
  .label {
    color: var(--cn-color-text-tertiary);
  }
}

.app-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  
  .reject-reason {
    font-size: 13px;
    color: var(--cn-color-danger);
  }
}

.app-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

// 邀请码弹窗
.invite-dialog-content {
  text-align: center;
  
  .invite-team-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--cn-color-text-primary);
    margin-bottom: 20px;
  }
  
  .invite-code-box {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 16px;
    background: var(--cn-color-bg-surface-muted);
    border-radius: 8px;
    margin-bottom: 16px;
    
    .invite-code {
      font-size: 24px;
      font-weight: bold;
      color: var(--cn-color-brand-primary);
      letter-spacing: 2px;
    }
  }
  
  .invite-actions {
    margin-bottom: 16px;
  }
  
  .invite-tip {
    font-size: 13px;
    color: var(--cn-color-text-tertiary);
  }
}

// 审批弹窗
.app-dialog-content {
  max-height: 400px;
  overflow-y: auto;
  
  .empty-tip {
    text-align: center;
    padding: 40px;
    color: var(--cn-color-text-tertiary);
  }
}

.app-item {
  padding: 16px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  margin-bottom: 12px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.applicant-info {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .applicant-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: color-mix(in srgb, var(--cn-color-brand-primary) 82%, var(--cn-color-info));
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 16px;
    font-weight: bold;
  }
  
  .applicant-name {
    font-size: 15px;
    font-weight: 500;
    color: var(--cn-color-text-primary);
  }
  
  .apply-time {
    font-size: 12px;
    color: var(--cn-color-text-tertiary);
  }
}

.apply-reason {
  margin: 12px 0;
  padding: 10px;
  background: var(--cn-color-bg-surface-muted);
  border-radius: 6px;
  font-size: 13px;
  color: var(--cn-color-text-secondary);
}

.app-item .app-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
