<template>
  <div class="plan-container cn-learn-shell">
    <div class="cn-learn-shell__inner">
    <section class="cn-learn-hero cn-wave-reveal">
      <div class="cn-learn-hero__content">
        <span class="cn-learn-hero__eyebrow">Plan Check-in</span>
        <h1 class="cn-learn-hero__title">计划打卡执行面板</h1>
        <p class="cn-learn-hero__desc">把每日任务、打卡状态和长期趋势集中到同一个视图，持续稳定推进目标。</p>
      </div>
      <div class="cn-learn-hero__meta">
        <span class="cn-learn-chip">进行中 {{ stats.totalPlans || 0 }}</span>
        <span class="cn-learn-chip">今日打卡 {{ stats.todayCheckins || 0 }}</span>
        <span class="cn-learn-chip">最长连续 {{ stats.maxStreak || 0 }}</span>
      </div>
    </section>

    <!-- 统计概览卡片 -->
    <div class="stats-card cn-learn-panel cn-learn-reveal">
      <div class="stats-header">
        <h2>📋 我的计划打卡</h2>
        <p class="stats-subtitle">坚持每日打卡，养成好习惯</p>
      </div>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalPlans || 0 }}</div>
          <div class="stat-label">进行中</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.todayCheckins || 0 }}</div>
          <div class="stat-label">今日已打卡</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalCheckins || 0 }}</div>
          <div class="stat-label">累计打卡</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ stats.maxStreak || 0 }}</div>
          <div class="stat-label">最长连续</div>
        </div>
      </div>
    </div>

    <!-- 今日任务区域 -->
    <div class="section today-section cn-learn-panel cn-learn-reveal">
      <div class="section-header">
        <h3>🎯 今日任务</h3>
        <span class="task-count">{{ todayTasks.length }} 个任务</span>
      </div>
      
      <div v-if="todayLoading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      
      <div v-else-if="todayTasks.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p>今日暂无任务，快去创建计划吧~</p>
      </div>
      
      <div v-else class="task-list">
        <div 
          v-for="task in todayTasks" 
          :key="task.planId"
          class="task-card cn-learn-float"
          :class="{ 'completed': task.todayChecked }"
        >
          <div class="task-main">
            <div class="task-info">
              <div class="task-name">
                <span class="type-tag" :class="getPlanTypeClass(task.planType)">
                  {{ getPlanTypeText(task.planType) }}
                </span>
                {{ task.planName }}
              </div>
              <div class="task-target">
                目标: {{ task.targetValue }} {{ task.targetUnit }}
              </div>
              <div class="task-time" v-if="task.dailyStartTime || task.dailyEndTime">
                <el-icon><Clock /></el-icon>
                {{ task.dailyStartTime || '--:--' }} - {{ task.dailyEndTime || '--:--' }}
              </div>
            </div>
            <div class="task-progress">
              <div class="streak-info">
                <span class="streak-label">连续</span>
                <span class="streak-value">{{ task.currentStreak }}</span>
                <span class="streak-unit">天</span>
              </div>
            </div>
          </div>
          <div class="task-action">
            <button 
              v-if="!task.todayChecked"
              class="checkin-btn"
              @click="openCheckinDialog(task)"
            >
              <el-icon><Check /></el-icon>
              打卡
            </button>
            <div v-else class="checked-badge">
              <el-icon><SuccessFilled /></el-icon>
              已完成
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的计划列表 -->
    <div class="section plan-section cn-learn-panel cn-learn-reveal">
      <div class="section-header">
        <h3>📑 我的计划</h3>
        <button class="create-btn" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建计划
        </button>
      </div>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="loadPlanList">
          <el-option label="进行中" :value="1" />
          <el-option label="已暂停" :value="2" />
          <el-option label="已完成" :value="3" />
        </el-select>
        <el-select v-model="filterType" placeholder="全部类型" clearable @change="loadPlanList">
          <el-option label="学习计划" :value="1" />
          <el-option label="运动计划" :value="2" />
          <el-option label="阅读计划" :value="3" />
          <el-option label="编程计划" :value="4" />
          <el-option label="其他计划" :value="99" />
        </el-select>
        <el-input 
          v-model="filterKeyword" 
          placeholder="搜索计划名称" 
          clearable
          @clear="loadPlanList"
          @keyup.enter="loadPlanList"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <div v-if="planLoading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="planList.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p>暂无计划，点击上方按钮创建你的第一个计划吧</p>
      </div>

      <div v-else class="plan-list">
        <div 
          v-for="plan in planList" 
          :key="plan.id"
          class="plan-card cn-learn-float"
        >
          <div class="plan-header">
            <div class="plan-title">
              <span class="type-tag" :class="getPlanTypeClass(plan.planType)">
                {{ getPlanTypeText(plan.planType) }}
              </span>
              <span class="plan-name">{{ plan.planName }}</span>
              <span class="status-tag" :class="getStatusClass(plan.status)">
                {{ getStatusText(plan.status) }}
              </span>
            </div>
            <el-dropdown trigger="click" @command="handlePlanAction($event, plan)">
              <el-icon class="more-btn"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">
                    <el-icon><Edit /></el-icon>编辑
                  </el-dropdown-item>
                  <el-dropdown-item command="records">
                    <el-icon><Document /></el-icon>打卡记录
                  </el-dropdown-item>
                  <el-dropdown-item v-if="plan.status === 1" command="pause">
                    <el-icon><VideoPause /></el-icon>暂停
                  </el-dropdown-item>
                  <el-dropdown-item v-if="plan.status === 2" command="resume">
                    <el-icon><VideoPlay /></el-icon>恢复
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>
                    <el-icon><Delete /></el-icon>删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          
          <div class="plan-desc" v-if="plan.planDesc">{{ plan.planDesc }}</div>
          
          <div class="plan-meta">
            <div class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(plan.startDate) }} - {{ plan.endDate ? formatDate(plan.endDate) : '长期' }}
            </div>
            <div class="meta-item">
              <el-icon><Aim /></el-icon>
              {{ plan.targetValue }} {{ plan.targetUnit }}
            </div>
          </div>
          
          <div class="plan-stats">
            <div class="stat">
              <span class="stat-num">{{ plan.totalCheckinDays }}</span>
              <span class="stat-text">累计打卡</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ plan.currentStreak }}</span>
              <span class="stat-text">当前连续</span>
            </div>
            <div class="stat">
              <span class="stat-num">{{ plan.maxStreak }}</span>
              <span class="stat-text">最长连续</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadPlanList"
        />
      </div>
    </div>

    <!-- 创建/编辑计划弹窗 -->
    <PlanFormDialog 
      v-model="showFormDialog"
      :plan-data="editingPlan"
      @success="onPlanSaved"
    />

    <!-- 打卡弹窗 -->
    <CheckinDialog 
      v-model="showCheckinDialog"
      :task="checkinTask"
      @success="onCheckinSuccess"
    />

    <!-- 打卡记录弹窗 -->
    <CheckinRecordDialog 
      v-model="showRecordDialog"
      :plan-id="recordPlanId"
      :plan-name="recordPlanName"
    />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Loading, Clock, Check, SuccessFilled, Plus, Search, MoreFilled,
  Edit, Document, VideoPause, VideoPlay, Delete, Calendar, Aim
} from '@element-plus/icons-vue'
import planApi from '@/api/plan'
import { useRevealMotion } from '@/utils/reveal-motion'
import PlanFormDialog from './components/PlanFormDialog.vue'
import CheckinDialog from './components/CheckinDialog.vue'
import CheckinRecordDialog from './components/CheckinRecordDialog.vue'
useRevealMotion('.plan-container .cn-learn-reveal')

// 统计数据
const stats = ref({})

// 今日任务
const todayTasks = ref([])
const todayLoading = ref(false)

// 计划列表
const planList = ref([])
const planLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 筛选条件
const filterStatus = ref(null)
const filterType = ref(null)
const filterKeyword = ref('')

// 弹窗控制
const showFormDialog = ref(false)
const editingPlan = ref(null)
const showCheckinDialog = ref(false)
const checkinTask = ref(null)
const showRecordDialog = ref(false)
const recordPlanId = ref(null)
const recordPlanName = ref('')

// 页面初始化
onMounted(() => {
  loadStats()
  loadTodayTasks()
  loadPlanList()
})

// 加载统计数据
const loadStats = async () => {
  try {
    const response = await planApi.getStatsOverview()
    stats.value = response || {}
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载今日任务
const loadTodayTasks = async () => {
  todayLoading.value = true
  try {
    const response = await planApi.getTodayTasks()
    todayTasks.value = response || []
  } catch (error) {
    console.error('加载今日任务失败:', error)
  } finally {
    todayLoading.value = false
  }
}

// 加载计划列表
const loadPlanList = async () => {
  planLoading.value = true
  try {
    const response = await planApi.getPlanList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: filterStatus.value,
      planType: filterType.value,
      keyword: filterKeyword.value
    })
    planList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    console.error('加载计划列表失败:', error)
  } finally {
    planLoading.value = false
  }
}

// 打开创建弹窗
const openCreateDialog = () => {
  editingPlan.value = null
  showFormDialog.value = true
}

// 打开打卡弹窗
const openCheckinDialog = (task) => {
  checkinTask.value = task
  showCheckinDialog.value = true
}

// 处理计划操作
const handlePlanAction = async (command, plan) => {
  switch (command) {
    case 'edit':
      editingPlan.value = plan
      showFormDialog.value = true
      break
    case 'records':
      recordPlanId.value = plan.id
      recordPlanName.value = plan.planName
      showRecordDialog.value = true
      break
    case 'pause':
      await handlePausePlan(plan)
      break
    case 'resume':
      await handleResumePlan(plan)
      break
    case 'delete':
      await handleDeletePlan(plan)
      break
  }
}

// 暂停计划
const handlePausePlan = async (plan) => {
  try {
    await ElMessageBox.confirm('确定要暂停该计划吗？暂停后将不再生成提醒', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await planApi.pausePlan(plan.id)
    ElMessage.success('计划已暂停')
    loadPlanList()
    loadTodayTasks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('暂停计划失败:', error)
      ElMessage.error('暂停失败')
    }
  }
}

// 恢复计划
const handleResumePlan = async (plan) => {
  try {
    await planApi.resumePlan(plan.id)
    ElMessage.success('计划已恢复')
    loadPlanList()
    loadTodayTasks()
  } catch (error) {
    console.error('恢复计划失败:', error)
    ElMessage.error('恢复失败')
  }
}

// 删除计划
const handleDeletePlan = async (plan) => {
  try {
    await ElMessageBox.confirm('确定要删除该计划吗？删除后数据无法恢复', '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    await planApi.deletePlan(plan.id)
    ElMessage.success('计划已删除')
    loadPlanList()
    loadTodayTasks()
    loadStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除计划失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 计划保存成功
const onPlanSaved = () => {
  loadPlanList()
  loadTodayTasks()
  loadStats()
}

// 打卡成功
const onCheckinSuccess = () => {
  loadTodayTasks()
  loadPlanList()
  loadStats()
}

// 获取计划类型文本
const getPlanTypeText = (type) => {
  const typeMap = {
    1: '学习',
    2: '运动',
    3: '阅读',
    4: '编程',
    99: '其他'
  }
  return typeMap[type] || '其他'
}

// 获取计划类型样式
const getPlanTypeClass = (type) => {
  const classMap = {
    1: 'type-study',
    2: 'type-sport',
    3: 'type-read',
    4: 'type-code',
    99: 'type-other'
  }
  return classMap[type] || 'type-other'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    1: '进行中',
    2: '已暂停',
    3: '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取状态样式
const getStatusClass = (status) => {
  const classMap = {
    1: 'status-active',
    2: 'status-paused',
    3: 'status-completed'
  }
  return classMap[status] || ''
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style lang="scss" scoped>
.plan-container {
  min-height: calc(100vh - 68px);
  
  @media (max-width: 768px) {
    min-height: calc(100vh - 62px);
  }
}

// 统计卡片
.stats-card {
  border-radius: 16px;
  padding: 24px 32px;
  margin: 20px 0 24px;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.stats-header {
  text-align: left;
  margin-bottom: 20px;
  
  h2 {
    font-size: 20px;
    font-weight: 600;
    margin: 0 0 4px 0;
    color: #333;
  }
  
  .stats-subtitle {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  
  @media (max-width: 600px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-item {
  text-align: center;
  padding: 16px 12px;
  background: #f8f9fc;
  border-radius: 12px;
  
  .stat-value {
    font-size: 28px;
    font-weight: bold;
    color: #409eff;
  }
  
  .stat-label {
    font-size: 13px;
    color: #666;
    margin-top: 4px;
  }
}

// 区块通用样式
.section {
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: #333;
  }
  
  .task-count {
    font-size: 14px;
    color: #999;
  }
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    background: #337ecc;
    transform: translateY(-1px);
  }
}

// 加载和空状态
.loading-state, .empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  
  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }
  
  p {
    margin: 0;
  }
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

// 今日任务卡片
.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f8f9fc;
  border-radius: 12px;
  border: 2px solid transparent;
  transition: all 0.3s;
  
  &:hover {
    border-color: #409eff;
  }
  
  &.completed {
    background: #f0f9eb;
    border-color: #67c23a;
  }
}

.task-main {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}

.task-info {
  .task-name {
    font-size: 16px;
    font-weight: 500;
    color: #333;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }
  
  .task-target {
    font-size: 13px;
    color: #666;
    margin-bottom: 4px;
  }
  
  .task-time {
    font-size: 12px;
    color: #999;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.task-progress {
  .streak-info {
    text-align: center;
    
    .streak-label {
      font-size: 12px;
      color: #999;
    }
    
    .streak-value {
      font-size: 24px;
      font-weight: bold;
      color: #409eff;
      margin: 0 2px;
    }
    
    .streak-unit {
      font-size: 12px;
      color: #999;
    }
  }
}

.task-action {
  .checkin-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    background: #409eff;
    color: white;
    border: none;
    border-radius: 8px;
    padding: 10px 20px;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #337ecc;
      transform: scale(1.02);
    }
  }
  
  .checked-badge {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #67c23a;
    font-size: 14px;
    font-weight: 500;
  }
}

// 类型标签
.type-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  
  &.type-study { background: #e8f4fd; color: #409eff; }
  &.type-sport { background: #fdf2e9; color: #e6a23c; }
  &.type-read { background: #f0f9eb; color: #67c23a; }
  &.type-code { background: #f4ecfb; color: #9c27b0; }
  &.type-other { background: #f5f5f5; color: #909399; }
}

// 状态标签
.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
  
  &.status-active { background: #e8f5e9; color: #4caf50; }
  &.status-paused { background: #fff3e0; color: #ff9800; }
  &.status-completed { background: #e3f2fd; color: #2196f3; }
}

// 筛选条件
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  
  .el-select {
    width: 120px;
  }
  
  .el-input {
    width: 200px;
  }
  
  @media (max-width: 600px) {
    .el-select, .el-input {
      width: 100%;
    }
  }
}

// 计划列表
.plan-list {
  display: grid;
  gap: 16px;
}

.plan-card {
  background: #f8f9fc;
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  }
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.plan-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  
  .plan-name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }
}

.more-btn {
  cursor: pointer;
  color: #999;
  font-size: 20px;
  padding: 4px;
  
  &:hover {
    color: #409eff;
  }
}

.plan-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
}

.plan-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: #999;
  }
}

.plan-stats {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #eee;
  
  .stat {
    text-align: center;
    
    .stat-num {
      display: block;
      font-size: 20px;
      font-weight: bold;
      color: #409eff;
    }
    
    .stat-text {
      font-size: 12px;
      color: #999;
    }
  }
}

// 分页
.pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
