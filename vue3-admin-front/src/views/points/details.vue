<template>
  <div class="points-details">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <h2>积分明细管理</h2>
        <p>查看和管理所有用户的积分变动记录</p>
      </div>
    </div>

    <!-- 搜索和操作区域 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户ID">
          <el-input
            v-model="searchForm.userId"
            placeholder="输入用户ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.userName"
            placeholder="输入用户名搜索"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="积分类型">
          <el-select v-model="searchForm.pointsType" clearable style="width: 150px">
            <el-option label="全部" :value="null" />
            <el-option label="后台发放" :value="1" />
            <el-option label="打卡积分" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员ID">
          <el-input
            v-model="searchForm.adminId"
            placeholder="输入管理员ID"
            clearable
            style="width: 150px"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="searchForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="searchForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
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
          <el-button type="success" :loading="exporting" @click="handleExport">
            <el-icon><Download /></el-icon>
            {{ exporting ? '导出中...' : '导出' }}
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
        :default-sort="{ prop: 'createTime', order: 'descending' }"
      >
        <el-table-column label="明细ID" prop="id" width="100" align="center">
          <template #default="{ row }">
            <el-text type="info">#{{ row.id }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="用户信息" min-width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">
                <el-avatar :size="32">
                  <el-icon><User /></el-icon>
                </el-avatar>
              </div>
              <div class="user-info">
                <div class="user-name">{{ row.userName || `用户${row.userId}` }}</div>
                <div class="user-id">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="积分变动" prop="pointsChange" width="120" align="center">
          <template #default="{ row }">
            <div class="points-change" :class="{ 'points-positive': row.pointsChange > 0 }">
              <span class="points-symbol">+</span>
              <span class="points-value">{{ formatNumber(row.pointsChange) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="积分类型" prop="pointsType" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.pointsType === 1 ? 'warning' : row.pointsType === 2 ? 'success' : 'info'"
              effect="light"
            >
              {{ row.pointsTypeDesc }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="变动描述" prop="description" min-width="200">
          <template #default="{ row }">
            <div class="description-cell">
              <div class="description-text">{{ row.description }}</div>
              <div v-if="row.continuousDays" class="description-extra">
                连续{{ row.continuousDays }}天
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="变动后余额" prop="balanceAfter" width="120" align="right">
          <template #default="{ row }">
            <div class="balance-cell">
              <div class="balance-value">{{ formatNumber(row.balanceAfter) }}</div>
              <div class="balance-yuan">≈{{ ((row.balanceAfter || 0) / 1000).toFixed(2) }}元</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作管理员" width="120" align="center">
          <template #default="{ row }">
            <div v-if="row.adminId" class="admin-cell">
              <div class="admin-name">{{ row.adminName || `管理员${row.adminId}` }}</div>
              <div class="admin-id">ID: {{ row.adminId }}</div>
            </div>
            <el-text v-else type="info">系统</el-text>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" prop="createTime" width="160" align="center" sortable>
          <template #default="{ row }">
            <div class="time-cell">
              <div class="time-date">{{ formatDate(row.createTime) }}</div>
              <div class="time-time">{{ formatTime(row.createTime) }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="info"
              size="small"
              text
              @click="handleViewDetail(row)"
            >
              <el-icon><View /></el-icon>
              详情
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

    <!-- 积分明细详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="积分明细详情"
      width="600px"
    >
      <div v-if="selectedDetail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="明细ID">
            <el-text type="info">#{{ selectedDetail.id }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ selectedDetail.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="用户名">
            {{ selectedDetail.userName || `用户${selectedDetail.userId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="积分变动">
            <div class="points-change points-positive">
              +{{ formatNumber(selectedDetail.pointsChange) }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="积分类型">
            <el-tag
              :type="selectedDetail.pointsType === 1 ? 'warning' : selectedDetail.pointsType === 2 ? 'success' : 'info'"
              effect="light"
            >
              {{ selectedDetail.pointsTypeDesc }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="变动后余额">
            <div class="balance-cell">
              <div class="balance-value">{{ formatNumber(selectedDetail.balanceAfter) }}</div>
              <div class="balance-yuan">≈{{ ((selectedDetail.balanceAfter || 0) / 1000).toFixed(2) }}元</div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="变动描述" span="2">
            {{ selectedDetail.description }}
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.continuousDays" label="连续打卡天数">
            {{ selectedDetail.continuousDays }}天
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedDetail.adminId" label="操作管理员">
            {{ selectedDetail.adminName || `管理员${selectedDetail.adminId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" span="2">
            {{ formatDateTime(selectedDetail.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, User, View } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'

const route = useRoute()

// 响应式数据
const loading = ref(false)
const tableData = ref([])
const showDetailDialog = ref(false)
const selectedDetail = ref(null)
const exporting = ref(false)
const MAX_EXPORT_ROWS = 5000

// 搜索表单
const searchForm = reactive({
  userId: route.query.userId || '',
  userName: route.query.userName || '',
  pointsType: null,
  adminId: '',
  startTime: '',
  endTime: ''
})

// 分页信息
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

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
    
    // 过滤空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === null || params[key] === undefined) {
        delete params[key]
      }
    })
    
    const result = await pointsApi.getAllPointsDetailList(params)
    
    tableData.value = result.records || []
    pagination.total = result.total || 0
    
  } catch (error) {
    console.error('加载积分明细数据失败:', error)
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
    userId: '',
    userName: '',
    pointsType: null,
    adminId: '',
    startTime: '',
    endTime: ''
  })
  pagination.pageNum = 1
  loadData()
}

// 导出数据
const handleExport = async () => {
  exporting.value = true

  try {
    const params = buildQueryParams({
      pageNum: 1,
      pageSize: MAX_EXPORT_ROWS,
      ...searchForm
    })

    const result = await pointsApi.getAllPointsDetailList(params)
    const rows = result.records || []

    if (!rows.length) {
      ElMessage.warning('当前筛选条件下暂无可导出的积分明细')
      return
    }

    downloadCsv(rows)
    const extraTip = (result.total || 0) > MAX_EXPORT_ROWS ? `，已导出前 ${MAX_EXPORT_ROWS} 条` : ''
    ElMessage.success(`积分明细导出成功，共 ${rows.length} 条${extraTip}`)
  } catch (error) {
    console.error('导出积分明细失败:', error)
    ElMessage.error('导出积分明细失败')
  } finally {
    exporting.value = false
  }
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

// 查看详情
const handleViewDetail = (row) => {
  selectedDetail.value = row
  showDetailDialog.value = true
}

// 组件挂载时加载数据
onMounted(() => {
  loadData()
})

const buildQueryParams = (source) => {
  const params = { ...source }
  Object.keys(params).forEach(key => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) {
      delete params[key]
    }
  })
  return params
}

const downloadCsv = (rows) => {
  const headers = [
    ['id', '明细ID'],
    ['userId', '用户ID'],
    ['userName', '用户名'],
    ['pointsChange', '积分变动'],
    ['pointsTypeDesc', '积分类型'],
    ['description', '变动描述'],
    ['balanceAfter', '变动后余额'],
    ['continuousDays', '连续打卡天数'],
    ['adminId', '管理员ID'],
    ['adminName', '管理员名称'],
    ['createTime', '创建时间']
  ]

  const csvRows = [
    headers.map(([, title]) => escapeCsv(title)).join(','),
    ...rows.map(row => headers
      .map(([key]) => escapeCsv(row[key] ?? ''))
      .join(','))
  ]

  const blob = new Blob([`\uFEFF${csvRows.join('\n')}`], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `积分明细_${formatExportTime(new Date())}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const escapeCsv = (value) => {
  const text = String(value).replace(/\r?\n/g, ' ')
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const formatExportTime = (date) => {
  const pad = (num) => String(num).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}
</script>

<style scoped>
.points-details {
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

.user-cell {
  display: flex;
  align-items: center;
}

.user-avatar {
  margin-right: 8px;
}

.user-info {
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

.points-symbol {
  font-size: 14px;
}

.points-value {
  font-size: 16px;
}

.description-cell {
  line-height: 1.4;
}

.description-text {
  color: #303133;
  margin-bottom: 2px;
}

.description-extra {
  font-size: 12px;
  color: #67C23A;
}

.balance-cell {
  text-align: right;
}

.balance-value {
  font-weight: 600;
  color: #303133;
  margin-bottom: 2px;
}

.balance-yuan {
  font-size: 11px;
  color: #67C23A;
}

.admin-cell {
  text-align: center;
}

.admin-name {
  color: #303133;
  margin-bottom: 2px;
  font-size: 12px;
}

.admin-id {
  font-size: 10px;
  color: #909399;
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

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  padding: 10px 0;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-table .cell) {
  padding-left: 6px;
  padding-right: 6px;
}

:deep(.el-descriptions__label) {
  font-weight: 600;
}
</style>
