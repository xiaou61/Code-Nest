<template>
  <CnPage class="team-detail-container" surface="transparent" max-width="1220px">
    <CnSection class="team-hero" surface="panel" v-loading="loading">
      <div class="team-header">
        <el-button class="back-btn" text :icon="ArrowLeft" @click="goBack">返回</el-button>

        <div class="team-avatar">
          <img v-if="team.teamAvatar" :src="team.teamAvatar" alt="小组头像" />
          <span v-else class="avatar-text">{{ team.teamName?.charAt(0) || '组' }}</span>
        </div>

        <div class="team-info">
          <div class="team-title-row">
            <h1 class="team-name">{{ team.teamName || '小组详情' }}</h1>
            <CnStatusTag :type="getTypeTone(team.teamType)" size="sm">
              {{ getTypeText(team.teamType) }}
            </CnStatusTag>
          </div>
          <p class="team-desc">{{ team.teamDesc || '暂无简介' }}</p>
          <div class="team-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ team.currentMembers || 0 }}/{{ team.maxMembers || 0 }} 人
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              创建于 {{ formatDate(team.createTime) }}
            </span>
            <CnStatusTag
              v-for="tag in parseTags(team.tags)"
              :key="tag"
              type="neutral"
              size="sm"
              subtle
            >
              {{ tag }}
            </CnStatusTag>
          </div>
        </div>

        <div class="team-actions">
          <template v-if="!team.joined">
            <el-button type="primary" :loading="joining" @click="handleJoin">
              {{ team.joinType === 1 ? '加入小组' : '申请加入' }}
            </el-button>
          </template>
          <template v-else>
            <el-button v-if="isAdmin" type="primary" :icon="Edit" @click="goToEdit">编辑</el-button>
            <el-button v-if="isAdmin" :icon="Share" @click="showInviteCodeDialog">邀请</el-button>
            <el-button v-if="!isLeader" type="danger" plain @click="handleQuit">退出小组</el-button>
          </template>
        </div>
      </div>

      <div v-if="team.goalTitle" class="team-goal">
        <div class="goal-header">
          <el-icon><Aim /></el-icon>
          <span class="goal-title">{{ team.goalTitle }}</span>
        </div>
        <p v-if="team.goalDesc" class="goal-desc">{{ team.goalDesc }}</p>
        <div class="goal-meta">
          <span v-if="team.goalStartDate">
            {{ formatDate(team.goalStartDate) }} - {{ formatDate(team.goalEndDate) }}
          </span>
          <span v-if="team.dailyTarget">每日目标：{{ team.dailyTarget }}</span>
        </div>
      </div>

      <div class="stats-overview">
        <div class="stat-tile">
          <div class="stat-value">{{ team.currentMembers || 0 }}</div>
          <div class="stat-label">成员</div>
        </div>
        <div class="stat-tile">
          <div class="stat-value">{{ team.totalCheckins || 0 }}</div>
          <div class="stat-label">总打卡</div>
        </div>
        <div class="stat-tile">
          <div class="stat-value">{{ team.totalDiscussions || 0 }}</div>
          <div class="stat-label">讨论</div>
        </div>
        <div class="stat-tile">
          <div class="stat-value">{{ team.activeDays || 0 }}</div>
          <div class="stat-label">活跃天</div>
        </div>
      </div>
    </CnSection>

    <CnSection class="tab-container" surface="panel" compact>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="概览" name="overview">
          <template #label>
            <span class="tab-label"><el-icon><HomeFilled /></el-icon>概览</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="成员" name="members">
          <template #label>
            <span class="tab-label"><el-icon><User /></el-icon>成员</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="打卡" name="checkin">
          <template #label>
            <span class="tab-label"><el-icon><Check /></el-icon>打卡</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="排行" name="rank">
          <template #label>
            <span class="tab-label"><el-icon><Trophy /></el-icon>排行</span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="讨论" name="discussion">
          <template #label>
            <span class="tab-label"><el-icon><ChatDotRound /></el-icon>讨论</span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </CnSection>

    <div class="tab-content">
      <div v-if="activeTab === 'overview'" class="overview-content">
        <div class="content-grid">
          <CnSection title="今日任务" surface="panel" compact divided>
            <template #actions>
              <el-button v-if="isAdmin" size="small" type="primary" :icon="Plus" @click="openTaskCreate">
                创建任务
              </el-button>
            </template>
            <TaskList
              ref="overviewTaskListRef"
              :team-id="teamId"
              :is-admin="isAdmin"
              @checkin="openCheckinDialog"
            />
          </CnSection>

          <CnSection title="最近打卡" surface="panel" compact divided>
            <CheckinList ref="overviewCheckinListRef" :team-id="teamId" :limit="5" />
          </CnSection>

          <CnSection class="full-width" title="小组统计" surface="panel" compact divided>
            <TeamStats :team-id="teamId" />
          </CnSection>
        </div>
      </div>

      <CnSection v-if="activeTab === 'members'" surface="panel" compact>
        <MemberList
          :team-id="teamId"
          :is-admin="isAdmin"
          :is-leader="isLeader"
          :current-user-id="currentUserId"
          @refresh="loadTeamDetail"
        />
      </CnSection>

      <div v-if="activeTab === 'checkin'" class="checkin-content">
        <CnSection surface="panel" compact>
          <div class="checkin-header">
            <div class="checkin-stats">
              <div class="checkin-stat">
                <span class="stat-num">{{ myStreak }}</span>
                <span class="stat-text">连续打卡</span>
              </div>
              <div class="checkin-stat">
                <span class="stat-num">{{ myTotal }}</span>
                <span class="stat-text">累计打卡</span>
              </div>
            </div>
            <el-button v-if="team.joined" type="primary" :icon="Check" @click="openCheckinDialog(null)">
              去打卡
            </el-button>
          </div>
        </CnSection>

        <CnSection title="打卡任务" surface="panel" compact divided>
          <TaskList
            ref="allTaskListRef"
            :team-id="teamId"
            :is-admin="isAdmin"
            show-all
            @checkin="openCheckinDialog"
            @edit="editTask"
          />
        </CnSection>

        <CnSection title="打卡动态" surface="panel" compact divided>
          <CheckinList ref="allCheckinListRef" :team-id="teamId" />
        </CnSection>
      </div>

      <CnSection v-if="activeTab === 'rank'" surface="panel" compact>
        <RankBoard :team-id="teamId" />
      </CnSection>

      <CnSection v-if="activeTab === 'discussion'" title="小组讨论区" surface="panel" compact divided>
        <template #actions>
          <el-button v-if="team.joined" type="primary" :icon="Edit" @click="showDiscussionForm = true">
            发布讨论
          </el-button>
        </template>
        <DiscussionList ref="discussionListRef" :team-id="teamId" :is-admin="isAdmin" />
      </CnSection>
    </div>

    <CheckinDialog
      v-model="showCheckinDialog"
      :team-id="teamId"
      :task="checkinTask"
      @success="onCheckinSuccess"
    />

    <TaskFormDialog
      v-model="showTaskForm"
      :team-id="teamId"
      :task-data="editingTask"
      @success="onTaskSaved"
    />

    <DiscussionFormDialog
      v-model="showDiscussionForm"
      :team-id="teamId"
      @success="onDiscussionPosted"
    />

    <el-dialog v-model="showInviteDialog" title="邀请成员" width="400px">
      <div class="invite-dialog-content">
        <div class="invite-code-box">
          <span class="invite-code">{{ inviteCode }}</span>
          <el-button type="primary" size="small" :icon="DocumentCopy" @click="copyInviteCode">
            复制
          </el-button>
        </div>
        <div class="invite-actions">
          <el-button :icon="Refresh" :loading="refreshingCode" @click="refreshInviteCode">
            刷新邀请码
          </el-button>
        </div>
        <div class="invite-tip">分享邀请码给好友，好友输入邀请码即可加入小组</div>
      </div>
    </el-dialog>

    <el-dialog v-model="showApplyDialog" title="申请加入" width="400px">
      <el-form>
        <el-form-item label="申请理由">
          <el-input
            v-model="applyReason"
            type="textarea"
            :rows="3"
            placeholder="简单介绍一下自己"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Aim,
  ArrowLeft,
  Calendar,
  ChatDotRound,
  Check,
  DocumentCopy,
  Edit,
  HomeFilled,
  Plus,
  Refresh,
  Share,
  Trophy,
  User
} from '@element-plus/icons-vue'
import { CnPage, CnSection, CnStatusTag } from '@/design-system'
import { useUserStore } from '@/stores/user'
import teamApi from '@/api/team'
import TaskList from './components/TaskList.vue'
import CheckinList from './components/CheckinList.vue'
import CheckinDialog from './components/CheckinDialog.vue'
import TaskFormDialog from './components/TaskFormDialog.vue'
import RankBoard from './components/RankBoard.vue'
import DiscussionList from './components/DiscussionList.vue'
import DiscussionFormDialog from './components/DiscussionFormDialog.vue'
import MemberList from './components/MemberList.vue'
import TeamStats from './components/TeamStats.vue'

interface TeamDetail {
  id?: number | string
  teamName?: string
  teamDesc?: string
  teamAvatar?: string
  teamType?: number
  currentMembers?: number
  maxMembers?: number
  createTime?: string
  tags?: string
  joined?: boolean
  isMember?: boolean
  joinType?: number
  memberRole?: number
  goalTitle?: string
  goalDesc?: string
  goalStartDate?: string
  goalEndDate?: string
  dailyTarget?: number | string
  totalCheckins?: number
  totalDiscussions?: number
  activeDays?: number
}

interface TaskRecord {
  id?: number | string
  [key: string]: unknown
}

interface TaskListExpose {
  loadTasks?: () => void
}

interface CheckinListExpose {
  loadCheckins?: () => void
}

interface DiscussionListExpose {
  loadDiscussions?: () => void
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const teamId = computed(() => getFirstRouteParam(route.params.id))
const currentUserId = computed(() => userStore.userInfo?.id)

const team = ref<TeamDetail>({})
const loading = ref(false)
const activeTab = ref('overview')

const isAdmin = computed(() => team.value.memberRole === 1 || team.value.memberRole === 2)
const isLeader = computed(() => team.value.memberRole === 1)

const myStreak = ref(0)
const myTotal = ref(0)

const showCheckinDialog = ref(false)
const checkinTask = ref<TaskRecord | null>(null)
const overviewTaskListRef = ref<TaskListExpose | null>(null)
const allTaskListRef = ref<TaskListExpose | null>(null)
const overviewCheckinListRef = ref<CheckinListExpose | null>(null)
const allCheckinListRef = ref<CheckinListExpose | null>(null)
const discussionListRef = ref<DiscussionListExpose | null>(null)

const showTaskForm = ref(false)
const editingTask = ref<TaskRecord | null>(null)
const showDiscussionForm = ref(false)

const showInviteDialog = ref(false)
const inviteCode = ref('')
const refreshingCode = ref(false)

const showApplyDialog = ref(false)
const applyReason = ref('')
const applying = ref(false)
const joining = ref(false)

const getFirstRouteParam = (param: unknown) => {
  return Array.isArray(param) ? String(param[0] || '') : String(param || '')
}

const loadTeamDetail = async () => {
  loading.value = true
  try {
    const response = await teamApi.getTeamDetail(teamId.value)
    team.value = response || {}

    if (team.value.isMember) {
      loadMyStats()
    }
  } catch (error) {
    console.error('加载小组详情失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadMyStats = async () => {
  try {
    const [streak, total] = await Promise.all([
      teamApi.getStreakDays(teamId.value),
      teamApi.getTotalCheckinDays(teamId.value)
    ])
    myStreak.value = streak || 0
    myTotal.value = total || 0
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const handleJoin = async () => {
  if (team.value.joinType === 1) {
    joining.value = true
    try {
      await teamApi.applyJoin(teamId.value)
      ElMessage.success('加入成功')
      loadTeamDetail()
    } catch (error) {
      console.error('加入失败:', error)
    } finally {
      joining.value = false
    }
  } else {
    showApplyDialog.value = true
  }
}

const submitApply = async () => {
  applying.value = true
  try {
    await teamApi.applyJoin(teamId.value, { applyReason: applyReason.value })
    ElMessage.success('申请已提交，等待审核')
    showApplyDialog.value = false
  } catch (error) {
    console.error('申请失败:', error)
  } finally {
    applying.value = false
  }
}

const handleQuit = async () => {
  try {
    await ElMessageBox.confirm('确定要退出这个小组吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await teamApi.quitTeam(teamId.value)
    ElMessage.success('已退出小组')
    router.push('/team')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出失败:', error)
    }
  }
}

const showInviteCodeDialog = async () => {
  showInviteDialog.value = true
  try {
    const code = await teamApi.getInviteCode(teamId.value)
    inviteCode.value = code
  } catch (error) {
    console.error('获取邀请码失败:', error)
  }
}

const refreshInviteCode = async () => {
  refreshingCode.value = true
  try {
    const code = await teamApi.refreshInviteCode(teamId.value)
    inviteCode.value = code
    ElMessage.success('邀请码已刷新')
  } catch (error) {
    console.error('刷新失败:', error)
  } finally {
    refreshingCode.value = false
  }
}

const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value)
  ElMessage.success('已复制到剪贴板')
}

const openCheckinDialog = (task: TaskRecord | null) => {
  checkinTask.value = task
  showCheckinDialog.value = true
}

const openTaskCreate = () => {
  editingTask.value = null
  showTaskForm.value = true
}

const onCheckinSuccess = () => {
  loadMyStats()
  loadTeamDetail()
  overviewTaskListRef.value?.loadTasks?.()
  allTaskListRef.value?.loadTasks?.()
  overviewCheckinListRef.value?.loadCheckins?.()
  allCheckinListRef.value?.loadCheckins?.()
}

const editTask = (task: TaskRecord) => {
  editingTask.value = task
  showTaskForm.value = true
}

const onTaskSaved = () => {
  editingTask.value = null
  overviewTaskListRef.value?.loadTasks?.()
  allTaskListRef.value?.loadTasks?.()
  loadTeamDetail()
}

const onDiscussionPosted = () => {
  discussionListRef.value?.loadDiscussions?.()
  loadTeamDetail()
}

const getTypeText = (type?: number) => {
  const map: Record<number, string> = { 1: '目标型', 2: '学习型', 3: '打卡型' }
  return type ? map[type] || '学习型' : '学习型'
}

const getTypeTone = (type?: number) => {
  const map: Record<number, 'brand' | 'success' | 'warning'> = {
    1: 'brand',
    2: 'success',
    3: 'warning'
  }
  return type ? map[type] || 'success' : 'success'
}

const parseTags = (tags?: string) => {
  if (!tags) return []
  return tags.split(',').map(tag => tag.trim()).filter(Boolean)
}

const formatDate = (date?: string) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const goBack = () => router.back()
const goToEdit = () => router.push(`/team/${teamId.value}/edit`)

onMounted(() => {
  loadTeamDetail()
})

watch(() => route.params.id, () => {
  loadTeamDetail()
})
</script>

<style scoped>
.team-detail-container {
  min-height: calc(100vh - 68px);
}

.team-hero :deep(.cn-section__body),
.tab-container :deep(.cn-section__body) {
  padding: var(--cn-space-5);
}

.team-header {
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  align-items: start;
  gap: var(--cn-space-4);
}

.back-btn {
  align-self: start;
}

.team-avatar {
  width: 84px;
  height: 84px;
  overflow: hidden;
  border-radius: var(--cn-radius-panel);
  background: color-mix(in srgb, var(--cn-color-brand-primary) 74%, var(--cn-color-info));
  color: white;
  flex-shrink: 0;
}

.team-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-text {
  display: flex;
  width: 100%;
  height: 100%;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
}

.team-info {
  display: grid;
  gap: var(--cn-space-3);
  min-width: 0;
}

.team-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.team-name {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-family: var(--cn-font-heading);
  font-size: 26px;
  font-weight: 750;
  line-height: 1.25;
}

.team-desc {
  margin: 0;
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.team-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.team-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.team-goal {
  display: grid;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-5);
  border-top: 1px solid var(--cn-color-border-subtle);
  padding-top: var(--cn-space-5);
}

.goal-header {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-brand-primary);
  font-weight: 700;
}

.goal-title {
  color: var(--cn-color-text-primary);
}

.goal-desc {
  margin: 0;
  color: var(--cn-color-text-secondary);
  line-height: 1.7;
}

.goal-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-3);
  margin-top: var(--cn-space-5);
}

.stat-tile {
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  padding: var(--cn-space-4);
  text-align: center;
}

.stat-value {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.15;
}

.stat-label {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.tab-container :deep(.el-tabs__header) {
  margin: 0;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-content {
  min-height: 400px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--cn-space-5);
}

.full-width {
  grid-column: 1 / -1;
}

.checkin-content {
  display: grid;
  gap: var(--cn-space-5);
}

.checkin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-4);
}

.checkin-stats {
  display: flex;
  gap: var(--cn-space-6);
}

.checkin-stat {
  display: grid;
  gap: 4px;
  text-align: center;
}

.stat-num {
  color: var(--cn-color-brand-primary);
  font-size: 28px;
  font-weight: 800;
}

.stat-text {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

.invite-dialog-content {
  display: grid;
  justify-items: center;
  gap: var(--cn-space-4);
  text-align: center;
}

.invite-code-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--cn-space-3);
  width: 100%;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
  padding: var(--cn-space-4);
}

.invite-code {
  color: var(--cn-color-brand-primary);
  font-family: var(--cn-font-heading);
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 2px;
}

.invite-tip {
  color: var(--cn-color-text-tertiary);
  font-size: 13px;
}

@media (max-width: 900px) {
  .team-header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .back-btn,
  .team-actions {
    grid-column: 1 / -1;
  }

  .team-actions {
    justify-content: flex-start;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .checkin-header {
    align-items: stretch;
    flex-direction: column;
  }

  .checkin-stats {
    justify-content: space-around;
  }
}
</style>
