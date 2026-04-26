<template>
  <div class="points-users">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <h2>用户积分排行</h2>
        <p>查看和管理用户积分排行数据</p>
      </div>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.userName"
            placeholder="输入用户名搜索"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="最小积分">
          <el-input-number
            v-model="searchForm.minPoints"
            :min="0"
            controls-position="right"
            placeholder="最小积分"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="最大积分">
          <el-input-number
            v-model="searchForm.maxPoints"
            :min="0"
            controls-position="right"
            placeholder="最大积分"
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="排序方式">
          <el-select v-model="searchForm.orderBy" style="width: 120px">
            <el-option label="按积分" value="points" />
            <el-option label="按时间" value="create_time" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="searchForm.orderDirection" style="width: 100px">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        style="width: 100%"
        :default-sort="{ prop: 'totalPoints', order: 'descending' }"
      >
        <el-table-column label="排名" width="80" align="center">
          <template #default="{ $index }">
            <div class="rank-cell">
              <span 
                class="rank-badge"
                :class="{
                  'rank-first': getRealRanking($index) === 1,
                  'rank-second': getRealRanking($index) === 2,
                  'rank-third': getRealRanking($index) === 3
                }"
              >
                {{ getRealRanking($index) }}
              </span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">
                <el-avatar :size="40" :src="row.avatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
              <div class="user-info">
                <div class="user-name">{{ row.userName }}</div>
                <div class="user-nick">{{ row.nickName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="总积分" prop="totalPoints" sortable width="120" align="right">
          <template #default="{ row }">
            <div class="points-cell">
              <div class="points-value">{{ formatNumber(row.totalPoints) }}</div>
              <div class="points-yuan">≈{{ row.balanceYuan }}元</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="连续打卡" prop="continuousDays" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="row.continuousDays > 7 ? 'success' : row.continuousDays > 0 ? 'warning' : 'info'"
              effect="light"
            >
              {{ row.continuousDays }}天
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="注册时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="最后更新" prop="updateTime" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              text
              @click="handleGrantPoints(row)"
            >
              <el-icon><Plus /></el-icon>
              发放积分
            </el-button>
            <el-button
              type="info"
              size="small"
              text
              @click="handleViewDetails(row)"
            >
              <el-icon><View /></el-icon>
              查看明细
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 发放积分对话框 -->
    <el-dialog
      v-model="showGrantDialog"
      title="发放积分"
      width="500px"
    >
      <el-form :model="grantForm" :rules="grantRules" ref="grantFormRef" label-width="100px">
        <el-form-item label="用户">
          <div class="grant-user-info">
            <el-avatar :size="40" :src="grantForm.user?.avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="grant-user-detail">
              <div class="grant-user-name">{{ grantForm.user?.userName }}</div>
              <div class="grant-user-points">当前积分：{{ formatNumber(grantForm.user?.totalPoints) }}</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="积分数量" prop="points">
          <el-input-number
            v-model="grantForm.points"
            :min="1"
            :max="10000"
            controls-position="right"
            placeholder="请输入积分数量"
          />
        </el-form-item>
        <el-form-item label="发放原因" prop="reason">
          <el-input
            v-model="grantForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入发放原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showGrantDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmGrant" :loading="grantLoading">
          确认发放
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, User, Plus, View } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'

const router = useRouter()

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const showGrantDialog = ref(false)
const grantLoading = ref(false)

// 搜索表单
const searchForm = reactive({
  userName: '',
  minPoints: null,
  maxPoints: null,
  orderBy: 'points',
  orderDirection: 'desc'
})

// 分页信息
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 发放积分表单
const grantForm = reactive({
  user: null,
  points: null,
  reason: ''
})

const grantFormRef = ref(null)

// 表单验证规则
const grantRules = {
  points: [
    { required: true, message: '请输入积分数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '积分数量必须在1-10000之间', trigger: 'blur' }
  ],
  reason: [
    { required: true, message: '请输入发放原因', trigger: 'blur' },
    { max: 200, message: '发放原因不能超过200个字符', trigger: 'blur' }
  ]
}

// 计算真实排名（考虑分页）
const getRealRanking = (index) => {
  return (pagination.pageNum - 1) * pagination.pageSize + index + 1
}

// 格式化数字显示
const formatNumber = (num) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 加载数据
const loadData = async () => {
  loading.value = true
  
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    
    const result = await pointsApi.getUserPointsList(params)
    
    tableData.value = result.records || []
    pagination.total = result.total || 0
    
  } catch (error) {
    console.error('加载用户积分数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索处理
const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    userName: '',
    minPoints: null,
    maxPoints: null,
    orderBy: 'points',
    orderDirection: 'desc'
  })
  pagination.pageNum = 1
  loadData()
}

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

// 当前页改变
const handleCurrentChange = (page) => {
  pagination.pageNum = page
  loadData()
}

// 发放积分
const handleGrantPoints = (user) => {
  grantForm.user = user
  grantForm.points = null
  grantForm.reason = ''
  showGrantDialog.value = true
}

// 确认发放积分
const handleConfirmGrant = async () => {
  if (!grantFormRef.value) return
  
  try {
    await grantFormRef.value.validate()
    
    await ElMessageBox.confirm(
      `确认为用户 ${grantForm.user.userName} 发放 ${grantForm.points} 积分吗？`,
      '确认发放',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    grantLoading.value = true
    
    const requestData = {
      userId: grantForm.user.userId,
      points: grantForm.points,
      reason: grantForm.reason
    }
    
    await pointsApi.grantPoints(requestData)
    
    ElMessage.success('积分发放成功')
    
    // 重置表单并关闭对话框
    grantFormRef.value.resetFields()
    Object.assign(grantForm, {
      user: null,
      points: null,
      reason: ''
    })
    showGrantDialog.value = false
    
    // 重新加载数据
    await loadData()
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('发放积分失败:', error)
      ElMessage.error(error.message || '发放积分失败')
    }
  } finally {
    grantLoading.value = false
  }
}

// 查看用户积分明细
const handleViewDetails = (user) => {
  router.push({
    path: '/points/details',
    query: { userId: user.userId, userName: user.userName }
  })
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.points-users {
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

.search-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 0;
}

.table-card {
  margin-bottom: 20px;
}

.rank-cell {
  display: flex;
  justify-content: center;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: bold;
  font-size: 14px;
  background: #f0f0f0;
  color: #606266;
}

.rank-badge.rank-first {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  color: white;
}

.rank-badge.rank-second {
  background: linear-gradient(135deg, #C0C0C0, #A8A8A8);
  color: white;
}

.rank-badge.rank-third {
  background: linear-gradient(135deg, #CD7F32, #B8860B);
  color: white;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-avatar {
  margin-right: 12px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.user-nick {
  font-size: 12px;
  color: #909399;
}

.points-cell {
  text-align: right;
}

.points-value {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.points-yuan {
  font-size: 12px;
  color: #67C23A;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.grant-user-info {
  display: flex;
  align-items: center;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.grant-user-detail {
  margin-left: 12px;
  flex: 1;
}

.grant-user-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.grant-user-points {
  font-size: 12px;
  color: #67C23A;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-table .cell) {
  padding-left: 8px;
  padding-right: 8px;
}
</style>
