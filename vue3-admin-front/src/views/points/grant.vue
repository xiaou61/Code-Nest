<template>
  <div class="points-grant">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <h2>积分发放管理</h2>
        <p>为用户发放积分奖励，支持单个发放和批量发放</p>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：单个发放 -->
      <el-col :span="12">
        <el-card class="grant-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><Plus /></el-icon>
              <span>单个发放积分</span>
            </div>
          </template>
          
          <el-form
            :model="singleGrantForm"
            :rules="singleGrantRules"
            ref="singleGrantFormRef"
            label-width="100px"
          >
            <el-form-item label="用户ID" prop="userId">
              <el-input
                v-model="singleGrantForm.userId"
                placeholder="请输入用户ID"
                type="number"
              >
                <template #append>
                  <el-button @click="handleSearchUser" :loading="searchingUser">
                    <el-icon><Search /></el-icon>
                    查找
                  </el-button>
                </template>
              </el-input>
            </el-form-item>

            <!-- 用户信息显示 -->
            <div v-if="selectedUser" class="user-info-display">
              <div class="user-card">
                <el-avatar :size="50">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="user-details">
                  <div class="user-name">{{ selectedUser.userName || `用户${selectedUser.userId}` }}</div>
                  <div class="user-stats">
                    <span class="stat-item">当前积分：{{ formatNumber(selectedUser.totalPoints) }}</span>
                    <span class="stat-item">连续打卡：{{ selectedUser.continuousDays || 0 }}天</span>
                  </div>
                </div>
              </div>
            </div>

            <el-form-item label="积分数量" prop="points">
              <el-input-number
                v-model="singleGrantForm.points"
                :min="1"
                :max="10000"
                controls-position="right"
                placeholder="请输入积分数量"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="发放原因" prop="reason">
              <el-input
                v-model="singleGrantForm.reason"
                type="textarea"
                :rows="4"
                placeholder="请输入发放原因（例如：活动奖励、bug反馈奖励等）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                @click="handleSingleGrant"
                :loading="singleGrantLoading"
                style="width: 100%"
                size="large"
              >
                <el-icon><Check /></el-icon>
                确认发放
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：批量发放 -->
      <el-col :span="12">
        <el-card class="grant-card">
          <template #header>
            <div class="card-header">
              <el-icon class="header-icon"><UserFilled /></el-icon>
              <span>批量发放积分</span>
            </div>
          </template>

          <el-form
            :model="batchGrantForm"
            :rules="batchGrantRules"
            ref="batchGrantFormRef"
            label-width="100px"
          >
            <el-form-item label="用户ID列表" prop="userIdsText">
              <el-input
                v-model="batchGrantForm.userIdsText"
                type="textarea"
                :rows="6"
                placeholder="请输入用户ID，多个用户用英文逗号分隔&#10;例如：123,456,789,101,202"
              />
              <div class="form-tip">
                <el-icon><InfoFilled /></el-icon>
                支持批量输入多个用户ID，用英文逗号分隔
              </div>
            </el-form-item>

            <el-form-item label="积分数量" prop="points">
              <el-input-number
                v-model="batchGrantForm.points"
                :min="1"
                :max="10000"
                controls-position="right"
                placeholder="请输入积分数量"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="发放原因" prop="reason">
              <el-input
                v-model="batchGrantForm.reason"
                type="textarea"
                :rows="4"
                placeholder="请输入发放原因（例如：春节活动奖励、社区贡献奖励等）"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="warning"
                @click="handleBatchGrant"
                :loading="batchGrantLoading"
                style="width: 100%"
                size="large"
              >
                <el-icon><Finished /></el-icon>
                批量发放
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 发放历史记录 -->
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Document /></el-icon>
          <span>最近发放记录</span>
          <el-button text @click="$router.push('/points/details')">查看全部</el-button>
        </div>
      </template>

      <el-table
        :data="historyData"
        v-loading="historyLoading"
        stripe
        style="width: 100%"
      >
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-info">
                <div class="user-name">{{ row.userName || `用户${row.userId}` }}</div>
                <div class="user-id">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="积分变动" width="100" align="center">
          <template #default="{ row }">
            <div class="points-change points-positive">
              +{{ formatNumber(row.pointsChange) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.pointsType === 1 ? 'warning' : 'info'"
              effect="light"
            >
              {{ row.pointsTypeDesc }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="发放原因" min-width="200">
          <template #default="{ row }">
            <div class="description-text">{{ row.description }}</div>
          </template>
        </el-table-column>

        <el-table-column label="操作管理员" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.adminId" class="admin-cell">
              <div class="admin-name">{{ row.adminName || `管理员${row.adminId}` }}</div>
            </div>
            <el-text v-else type="info">系统</el-text>
          </template>
        </el-table-column>

        <el-table-column label="发放时间" width="160" align="center">
          <template #default="{ row }">
            <div class="time-cell">
              <div class="time-date">{{ formatDate(row.createTime) }}</div>
              <div class="time-time">{{ formatTime(row.createTime) }}</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 批量发放结果对话框 -->
    <el-dialog
      v-model="showBatchResultDialog"
      title="批量发放结果"
      width="800px"
    >
      <div v-if="batchResult" class="batch-result">
        <!-- 总体统计 -->
        <div class="result-summary">
          <el-row :gutter="16">
            <el-col :span="8">
              <div class="summary-item success">
                <div class="summary-number">{{ batchResult.successCount }}</div>
                <div class="summary-label">成功</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item error">
                <div class="summary-number">{{ batchResult.failCount }}</div>
                <div class="summary-label">失败</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="summary-item info">
                <div class="summary-number">{{ formatNumber(batchResult.totalPointsGranted) }}</div>
                <div class="summary-label">总积分</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 详细结果列表 -->
        <div class="result-details">
          <h4>详细结果</h4>
          <el-table :data="batchResult.grantResults" stripe max-height="300">
            <el-table-column label="用户ID" prop="userId" width="100" align="center" />
            <el-table-column label="用户名" prop="userName" width="150" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'" effect="light">
                  {{ row.success ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="结果信息" prop="message" min-width="150" />
            <el-table-column label="当前余额" width="120" align="right">
              <template #default="{ row }">
                <span v-if="row.success">{{ formatNumber(row.currentBalance) }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showBatchResultDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  UserFilled,
  Search,
  User,
  Check,
  Finished,
  Document,
  InfoFilled
} from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'

// 响应式数据
const selectedUser = ref(null)
const searchingUser = ref(false)
const singleGrantLoading = ref(false)
const batchGrantLoading = ref(false)
const historyLoading = ref(false)
const historyData = ref([])
const showBatchResultDialog = ref(false)
const batchResult = ref(null)

// 表单引用
const singleGrantFormRef = ref(null)
const batchGrantFormRef = ref(null)

// 单个发放表单
const singleGrantForm = reactive({
  userId: '',
  points: null,
  reason: ''
})

// 批量发放表单
const batchGrantForm = reactive({
  userIdsText: '',
  points: null,
  reason: ''
})

// 表单验证规则
const singleGrantRules = {
  userId: [
    { required: true, message: '请输入用户ID', trigger: 'blur' }
  ],
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '积分数量必须在1-10000之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入发放原因', trigger: 'blur' },
    { max: 200, message: '发放原因不能超过200个字符', trigger: 'blur' }
  ]
}

const batchGrantRules = {
  userIdsText: [
    { required: true, message: '请输入用户ID', trigger: 'blur' }
  ],
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '积分数量必须在1-10000之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入发放原因', trigger: 'blur' },
    { max: 200, message: '发放原因不能超过200个字符', trigger: 'blur' }
  ]
}

// 格式化数字显示
const formatNumber = (num) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 格式化日期
const formatDate = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

// 格式化时间
const formatTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleTimeString('zh-CN')
}

// 搜索用户信息
const handleSearchUser = async () => {
  if (!singleGrantForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }
  
  searchingUser.value = true
  
  try {
    // 使用专门的接口获取用户积分信息（包含完整的用户信息和积分数据）
    const userPointsInfo = await pointsApi.getUserPointsInfo(singleGrantForm.userId)
    
    selectedUser.value = {
      userId: userPointsInfo.userId,
      userName: userPointsInfo.userName,
      totalPoints: userPointsInfo.totalPoints,
      continuousDays: userPointsInfo.continuousDays,
      avatar: userPointsInfo.avatar,
      realName: userPointsInfo.realName,
      nickname: userPointsInfo.nickName,
      monthCheckinDays: userPointsInfo.monthCheckinDays,
      hasCheckedToday: userPointsInfo.hasCheckedToday,
      lastCheckinDate: userPointsInfo.lastCheckinDate
    }
    
    ElMessage.success('用户信息加载成功')
  } catch (error) {
    console.error('查找用户失败:', error)
    ElMessage.error(error.message || '用户不存在或查找失败')
    selectedUser.value = null
  } finally {
    searchingUser.value = false
  }
}

// 单个发放积分
const handleSingleGrant = async () => {
  if (!singleGrantFormRef.value) return
  
  try {
    await singleGrantFormRef.value.validate()
    
    await ElMessageBox.confirm(
      `确认为用户 ${singleGrantForm.userId} 发放 ${singleGrantForm.points} 积分吗？`,
      '确认发放',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    singleGrantLoading.value = true
    
    const requestData = {
      userId: parseInt(singleGrantForm.userId),
      points: singleGrantForm.points,
      reason: singleGrantForm.reason
    }
    
    await pointsApi.grantPoints(requestData)
    
    ElMessage.success('积分发放成功')
    
    // 重置表单
    singleGrantFormRef.value.resetFields()
    Object.assign(singleGrantForm, {
      userId: '',
      points: null,
      reason: ''
    })
    selectedUser.value = null
    
    // 刷新历史记录
    await loadHistory()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发放积分失败:', error)
      ElMessage.error(error.message || '发放积分失败')
    }
  } finally {
    singleGrantLoading.value = false
  }
}

// 批量发放积分
const handleBatchGrant = async () => {
  if (!batchGrantFormRef.value) return
  
  try {
    await batchGrantFormRef.value.validate()
    
    // 解析用户ID
    const userIds = batchGrantForm.userIdsText
      .split(',')
      .map(id => parseInt(id.trim()))
      .filter(id => !isNaN(id))
    
    if (userIds.length === 0) {
      ElMessage.warning('请输入有效的用户ID')
      return
    }
    
    await ElMessageBox.confirm(
      `确认为${userIds.length}个用户发放${batchGrantForm.points}积分吗？`,
      '确认批量发放',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    batchGrantLoading.value = true
    
    const requestData = {
      userIds: userIds,
      points: batchGrantForm.points,
      reason: batchGrantForm.reason
    }
    
    const result = await pointsApi.batchGrantPoints(requestData)
    
    // 显示批量发放结果
    batchResult.value = result
    showBatchResultDialog.value = true
    
    ElMessage.success(
      `批量发放完成！成功：${result.successCount}人，失败：${result.failCount}人`
    )
    
    // 重置表单
    batchGrantFormRef.value.resetFields()
    Object.assign(batchGrantForm, {
      userIdsText: '',
      points: null,
      reason: ''
    })
    
    // 刷新历史记录
    await loadHistory()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量发放积分失败:', error)
      ElMessage.error(error.message || '批量发放积分失败')
    }
  } finally {
    batchGrantLoading.value = false
  }
}

// 加载发放历史记录
const loadHistory = async () => {
  historyLoading.value = true
  
  try {
    const params = {
      pageNum: 1,
      pageSize: 10,
      pointsType: 1 // 只查询后台发放的记录
    }
    
    const result = await pointsApi.getAllPointsDetailList(params)
    historyData.value = result.records || []
    
  } catch (error) {
    console.error('加载历史记录失败:', error)
  } finally {
    historyLoading.value = false
  }
}

// 组件挂载时加载历史记录
onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.points-grant {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-title h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-title p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.grant-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-icon {
  margin-right: 8px;
  color: #409EFF;
}

.user-info-display {
  margin: 15px 0;
}

.user-card {
  display: flex;
  align-items: center;
  padding: 15px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.user-details {
  margin-left: 12px;
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.user-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  font-size: 13px;
  color: #67C23A;
  background: rgba(103, 194, 58, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

.history-card {
  margin-top: 20px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-info {
  margin-left: 8px;
  flex: 1;
}

.user-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
  font-size: 13px;
}

.user-id {
  font-size: 11px;
  color: #909399;
}

.points-change {
  font-weight: 600;
  color: #909399;
}

.points-change.points-positive {
  color: #67C23A;
}

.description-text {
  color: #303133;
  line-height: 1.4;
}

.admin-cell {
  text-align: center;
}

.admin-name {
  color: #303133;
  font-size: 12px;
}

.time-cell {
  text-align: center;
}

.time-date {
  color: #303133;
  margin-bottom: 2px;
  font-size: 12px;
}

.time-time {
  font-size: 11px;
  color: #909399;
}

.batch-result {
  padding: 10px 0;
}

.result-summary {
  margin-bottom: 20px;
}

.summary-item {
  text-align: center;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
}

.summary-item.success {
  background: #f0f9ff;
  border-color: #67C23A;
}

.summary-item.error {
  background: #fef0f0;
  border-color: #F56C6C;
}

.summary-item.info {
  background: #f8f9fa;
  border-color: #409EFF;
}

.summary-number {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.summary-item.success .summary-number {
  color: #67C23A;
}

.summary-item.error .summary-number {
  color: #F56C6C;
}

.summary-item.info .summary-number {
  color: #409EFF;
}

.summary-label {
  font-size: 14px;
  color: #909399;
}

.result-details h4 {
  margin: 0 0 15px 0;
  color: #303133;
}

:deep(.el-card__body) {
  padding: 20px;
}
</style>
