<template>
  <div class="team-detail-container">
    <!-- 小组头部信息 -->
    <div class="team-header-card" v-loading="loading">
      <div class="team-header">
        <el-button class="back-btn" text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>

        <div class="team-avatar">
          <img v-if="team.teamAvatar" :src="team.teamAvatar" />
          <span v-else class="avatar-text">{{ team.teamName?.charAt(0) || '组' }}</span>
          <span class="type-badge" :class="getTypeClass(team.teamType)">
            {{ getTypeText(team.teamType) }}
          </span>
        </div>

        <div class="team-info">
          <h1 class="team-name">{{ team.teamName }}</h1>
          <p class="team-desc">{{ team.teamDesc || '暂无简介' }}</p>
          <div class="team-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ team.currentMembers }}/{{ team.maxMembers }}人
            </span>
            <span class="meta-item">
              <el-icon><Calendar /></el-icon>
              创建于 {{ formatDate(team.createTime) }}
            </span>
            <span v-if="team.tags" class="meta-tags">
              <span v-for="tag in parseTags(team.tags)" :key="tag" class="tag">{{ tag }}</span>
            </span>
          </div>
        </div>

        <div class="team-actions">
          <template v-if="!team.joined">
            <el-button type="primary" @click="handleJoin" :loading="joining">
              {{ team.joinType === 1 ? '加入小组' : '申请加入' }}
            </el-button>
          </template>
          <template v-else>
            <el-button v-if="isAdmin" type="primary" @click="goToEdit">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button v-if="isAdmin" @click="showInviteCodeDialog">
              <el-icon><Share /></el-icon>
              邀请
            </el-button>
            <el-button type="danger" plain @click="handleQuit" v-if="!isLeader">
              退出小组
            </el-button>
          </template>
        </div>
      </div>

      <!-- 小组目标 -->
      <div class="team-goal" v-if="team.goalTitle">
        <div class="goal-header">
          <el-icon><Aim /></el-icon>
          <span class="goal-title">{{ team.goalTitle }}</span>
        </div>
        <p class="goal-desc" v-if="team.goalDesc">{{ team.goalDesc }}</p>
        <div class="goal-meta">
          <span v-if="team.goalStartDate">
            {{ formatDate(team.goalStartDate) }} - {{ formatDate(team.goalEndDate) }}
          </span>
          <span v-if="team.dailyTarget">每日目标: {{ team.dailyTarget }}</span>
        </div>
      </div>

      <!-- 统计概览 -->
      <div class="stats-overview">
        <div class="stat-item">
          <div class="stat-value">{{ team.currentMembers || 0 }}</div>
          <div class="stat-label">成员</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ team.totalCheckins || 0 }}</div>
          <div class="stat-label">总打卡</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ team.totalDiscussions || 0 }}</div>
          <div class="stat-label">讨论</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ team.activeDays || 0 }}</div>
          <div class="stat-label">活跃天</div>
        </div>
      </div>
    </div>

    <!-- Tab内容区域 -->
    <div class="tab-container">
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
    </div>

    <!-- Tab内容 -->
    <div class="tab-content">
      <!-- 概览 -->
      <div v-if="activeTab === 'overview'" class="overview-content">
        <div class="content-grid">
          <!-- 今日任务 -->
          <div class="content-card">
            <div class="card-header">
              <h3><el-icon><Calendar /></el-icon>今日任务</h3>
              <el-button v-if="isAdmin" size="small" type="primary" @click="openTaskCreate">
                <el-icon><Plus /></el-icon>创建任务
              </el-button>
            </div>
            <TaskList
              ref="overviewTaskListRef"
              :team-id="teamId"
              :is-admin="isAdmin"
              @checkin="openCheckinDialog"
            />
          </div>

          <!-- 最近动态 -->
          <div class="content-card">
            <div class="card-header">
              <h3><el-icon><Timer /></el-icon>最近打卡</h3>
            </div>
            <CheckinList ref="overviewCheckinListRef" :team-id="teamId" :limit="5" />
          </div>

          <div class="content-card full-width">
            <div class="card-header">
              <h3><el-icon><Trophy /></el-icon>小组统计</h3>
            </div>
            <TeamStats :team-id="teamId" />
          </div>
        </div>
      </div>

      <!-- 成员 -->
      <div v-if="activeTab === 'members'" class="members-content">
        <MemberList
          :team-id="teamId"
          :is-admin="isAdmin"
          :is-leader="isLeader"
          :current-user-id="currentUserId"
          @refresh="loadTeamDetail"
        />
      </div>

      <!-- 打卡 -->
      <div v-if="activeTab === 'checkin'" class="checkin-content">
        <div class="checkin-header">
          <div class="checkin-stats">
            <div class="stat">
              <span class="stat-num">{{ myStreak }}</span>
              <span class="stat-text">连续打卡</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ myTotal }}</span>
              <span class="stat-text">累计打卡</span>
            </div>
          </div>
          <el-button type="primary" @click="openCheckinDialog(null)" v-if="team.joined">
            <el-icon><Check /></el-icon>
            去打卡
          </el-button>
        </div>

        <div class="tasks-section">
          <h3>打卡任务</h3>
          <TaskList
            ref="allTaskListRef"
            :team-id="teamId"
            :is-admin="isAdmin"
            show-all
            @checkin="openCheckinDialog"
            @edit="editTask"
          />
        </div>

        <div class="checkin-feed-section">
          <h3>打卡动态</h3>
          <CheckinList ref="allCheckinListRef" :team-id="teamId" />
        </div>
      </div>

      <!-- 排行 -->
      <div v-if="activeTab === 'rank'" class="rank-content">
        <RankBoard :team-id="teamId" />
      </div>

      <!-- 讨论 -->
      <div v-if="activeTab === 'discussion'" class="discussion-content">
        <div class="discussion-header">
          <h3>小组讨论区</h3>
          <el-button type="primary" @click="showDiscussionForm = true" v-if="team.joined">
            <el-icon><Edit /></el-icon>
            发布讨论
          </el-button>
        </div>
        <DiscussionList
          ref="discussionListRef"
          :team-id="teamId"
          :is-admin="isAdmin"
        />
      </div>
    </div>

    <!-- 打卡弹窗 -->
    <CheckinDialog
      v-model="showCheckinDialog"
      :team-id="teamId"
      :task="checkinTask"
      @success="onCheckinSuccess"
    />

    <!-- 任务创建弹窗 -->
    <TaskFormDialog
      v-model="showTaskForm"
      :team-id="teamId"
      :task-data="editingTask"
      @success="onTaskSaved"
    />

    <!-- 讨论发布弹窗 -->
    <DiscussionFormDialog
      v-model="showDiscussionForm"
      :team-id="teamId"
      @success="onDiscussionPosted"
    />

    <!-- 邀请码弹窗 -->
    <el-dialog v-model="showInviteDialog" title="邀请成员" width="400px">
      <div class="invite-dialog-content">
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

    <!-- 申请加入弹窗 -->
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
        <el-button type="primary" @click="submitApply" :loading="applying">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, User, Calendar, Edit, Share, Aim, HomeFilled,
  Check, Trophy, ChatDotRound, Plus, Timer, DocumentCopy, Refresh
} from '@element-plus/icons-vue'
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

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const teamId = computed(() => route.params.id)
const currentUserId = computed(() => userStore.userInfo?.id)

// 小组信息
const team = ref({})
const loading = ref(false)

// Tab状态
const activeTab = ref('overview')

// 用户角色
const isAdmin = computed(() => {
  return team.value.memberRole === 1 || team.value.memberRole === 2
})
const isLeader = computed(() => team.value.memberRole === 1)

// 打卡统计
const myStreak = ref(0)
const myTotal = ref(0)

// 打卡弹窗
const showCheckinDialog = ref(false)
const checkinTask = ref(null)
const overviewTaskListRef = ref()
const allTaskListRef = ref()
const overviewCheckinListRef = ref()
const allCheckinListRef = ref()
const discussionListRef = ref()

// 任务弹窗
const showTaskForm = ref(false)
const editingTask = ref(null)

// 讨论弹窗
const showDiscussionForm = ref(false)

// 邀请码
const showInviteDialog = ref(false)
const inviteCode = ref('')
const refreshingCode = ref(false)

// 申请
const showApplyDialog = ref(false)
const applyReason = ref('')
const applying = ref(false)
const joining = ref(false)

// 页面初始化
onMounted(() => {
  loadTeamDetail()
})

// 监听路由变化
watch(() => route.params.id, () => {
  loadTeamDetail()
})

// 加载小组详情
const loadTeamDetail = async () => {
  loading.value = true
  try {
    const response = await teamApi.getTeamDetail(teamId.value)
    team.value = response || {}

    // 如果是成员，加载打卡统计
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

// 加载我的打卡统计
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

// 加入/申请
const handleJoin = async () => {
  if (team.value.joinType === 1) {
    // 公开小组，直接加入
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
    // 需要申请
    showApplyDialog.value = true
  }
}

// 提交申请
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

// 退出小组
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

// 显示邀请码弹窗
const showInviteCodeDialog = async () => {
  showInviteDialog.value = true
  try {
    const code = await teamApi.getInviteCode(teamId.value)
    inviteCode.value = code
  } catch (error) {
    console.error('获取邀请码失败:', error)
  }
}

// 刷新邀请码
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

// 复制邀请码
const copyInviteCode = () => {
  navigator.clipboard.writeText(inviteCode.value)
  ElMessage.success('已复制到剪贴板')
}

// 打开打卡弹窗
const openCheckinDialog = (task) => {
  checkinTask.value = task
  showCheckinDialog.value = true
}

const openTaskCreate = () => {
  editingTask.value = null
  showTaskForm.value = true
}

// 打卡成功
const onCheckinSuccess = () => {
  loadMyStats()
  loadTeamDetail()
  overviewTaskListRef.value?.loadTasks?.()
  allTaskListRef.value?.loadTasks?.()
  overviewCheckinListRef.value?.loadCheckins?.()
  allCheckinListRef.value?.loadCheckins?.()
}

// 编辑任务
const editTask = (task) => {
  editingTask.value = task
  showTaskForm.value = true
}

// 任务保存成功
const onTaskSaved = () => {
  editingTask.value = null
  overviewTaskListRef.value?.loadTasks?.()
  allTaskListRef.value?.loadTasks?.()
  loadTeamDetail()
}

// 讨论发布成功
const onDiscussionPosted = () => {
  discussionListRef.value?.loadDiscussions?.()
  loadTeamDetail()
}

// 工具函数
const getTypeText = (type) => {
  const map = { 1: '目标型', 2: '学习型', 3: '打卡型' }
  return map[type] || '学习型'
}

const getTypeClass = (type) => {
  const map = { 1: 'type-goal', 2: 'type-study', 3: 'type-checkin' }
  return map[type] || 'type-study'
}

const parseTags = (tags) => {
  if (!tags) return []
  return tags.split(',').filter(t => t.trim())
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 路由
const goBack = () => router.back()
const goToEdit = () => router.push(`/team/${teamId.value}/edit`)
</script>

<style lang="scss" scoped>
.team-detail-container {
  padding: 24px 32px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  @media (max-width: 768px) {
    padding: 16px;
  }
}

// 头部卡片
.team-header-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.team-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
  position: relative;

  .back-btn {
    position: absolute;
    top: -8px;
    left: -8px;
  }
}

.team-avatar {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  flex-shrink: 0;
  margin-left: 40px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-text {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
    height: 100%;
    font-size: 32px;
    font-weight: bold;
    color: white;
  }

  .type-badge {
    position: absolute;
    bottom: -4px;
    left: 50%;
    transform: translateX(-50%);
    padding: 2px 10px;
    font-size: 11px;
    border-radius: 10px;
    white-space: nowrap;

    &.type-goal { background: #e8f4fd; color: #409eff; }
    &.type-study { background: #f0f9eb; color: #67c23a; }
    &.type-checkin { background: #fdf2e9; color: #e6a23c; }
  }
}

.team-info {
  flex: 1;
  min-width: 0;

  .team-name {
    font-size: 22px;
    font-weight: 600;
    color: #333;
    margin: 0 0 8px 0;
  }

  .team-desc {
    font-size: 14px;
    color: #666;
    margin: 0 0 12px 0;
    line-height: 1.5;
  }

  .team-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    align-items: center;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #999;
    }

    .meta-tags {
      display: flex;
      gap: 6px;

      .tag {
        padding: 2px 8px;
        background: #f5f7fa;
        color: #909399;
        border-radius: 4px;
        font-size: 12px;
      }
    }
  }
}

.team-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

// 小组目标
.team-goal {
  padding: 16px;
  background: #f8f9fc;
  border-radius: 12px;
  margin-bottom: 20px;

  .goal-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;

    .el-icon {
      color: #409eff;
    }

    .goal-title {
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }
  }

  .goal-desc {
    font-size: 14px;
    color: #666;
    margin: 0 0 8px 0;
  }

  .goal-meta {
    font-size: 12px;
    color: #999;
    display: flex;
    gap: 16px;
  }
}

// 统计概览
.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 600px) {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-item {
    text-align: center;
    padding: 16px;
    background: #f8f9fc;
    border-radius: 12px;

    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #409eff;
    }

    .stat-label {
      font-size: 13px;
      color: #999;
      margin-top: 4px;
    }
  }
}

// Tab容器
.tab-container {
  background: white;
  border-radius: 12px;
  padding: 0 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  :deep(.el-tabs__header) {
    margin: 0;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

// Tab内容
.tab-content {
  min-height: 400px;
}

// 概览内容
.overview-content {
  .content-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }
}

.content-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  &.full-width {
    grid-column: 1 / -1;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin: 0;

      .el-icon {
        color: #409eff;
      }
    }
  }
}

// 打卡内容
.checkin-content {
  .checkin-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    background: white;
    border-radius: 12px;
    margin-bottom: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .checkin-stats {
    display: flex;
    gap: 32px;

    .stat {
      text-align: center;

      .stat-num {
        font-size: 28px;
        font-weight: bold;
        color: #409eff;
      }

      .stat-text {
        font-size: 13px;
        color: #999;
      }
    }
  }

  .tasks-section, .checkin-feed-section {
    background: white;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin: 0 0 16px 0;
    }
  }
}

// 成员内容
.members-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

// 排行内容
.rank-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

// 讨论内容
.discussion-content {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .discussion-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin: 0;
    }
  }
}

// 邀请码弹窗
.invite-dialog-content {
  text-align: center;

  .invite-code-box {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    margin-bottom: 16px;

    .invite-code {
      font-size: 24px;
      font-weight: bold;
      color: #409eff;
      letter-spacing: 2px;
    }
  }

  .invite-actions {
    margin-bottom: 16px;
  }

  .invite-tip {
    font-size: 13px;
    color: #999;
  }
}
</style>
