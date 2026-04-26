<template>
  <div class="mock-interview-history">
    <!-- 返回按钮 -->
    <div class="back-nav">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><Clock /></el-icon>
        面试历史
      </h1>
    </div>

    <!-- 筛选区域 -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-select 
            v-model="queryParams.direction" 
            placeholder="面试方向"
            clearable
            style="width: 100%"
            @change="handleFilter"
          >
            <el-option 
              v-for="dir in directions" 
              :key="dir.directionCode" 
              :label="dir.directionName" 
              :value="dir.directionCode" 
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select 
            v-model="queryParams.status" 
            placeholder="面试状态"
            clearable
            style="width: 100%"
            @change="handleFilter"
          >
              <el-option label="进行中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已中断" :value="2" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 历史记录列表 -->
    <el-card shadow="never" class="history-card">
      <div v-loading="loading">
        <el-empty v-if="!loading && historyList.length === 0" description="暂无面试记录" />

        <div v-else class="history-list">
          <div 
            v-for="item in historyList" 
            :key="item.id" 
            class="history-item"
            @click="viewReport(item)"
          >
            <div class="item-left">
              <div class="item-direction">
                <el-tag type="primary">{{ item.direction }}</el-tag>
                <el-tag 
                  :type="getStatusType(item.status)"
                  size="small"
                >
                  {{ getStatusText(item.status) }}
                </el-tag>
              </div>
            <div class="item-info">
                <span class="info-label">方向：</span>
                <span>{{ item.directionName || item.direction }}</span>
                <span class="divider">|</span>
                <span class="info-label">难度：</span>
                <span>{{ item.levelName || getLevelText(item.level) }}</span>
                <span class="divider">|</span>
                <span class="info-label">题数：</span>
                <span>{{ item.questionCount }} 题</span>
              </div>
              <div class="item-time">
                {{ formatDate(item.createTime) }}
              </div>
            </div>

            <div class="item-right">
              <div class="item-score" v-if="item.totalScore !== null">
                <span class="score-value" :class="getScoreClass(item.totalScore)">
                  {{ item.totalScore }}
                </span>
                <span class="score-label">分</span>
              </div>
              <div class="item-score" v-else>
                <span class="score-pending">未完成</span>
              </div>
              
              <div class="item-actions">
                <el-button 
                  v-if="item.status === 0"
                  type="primary"
                  size="small"
                  @click.stop="continueInterview(item)"
                >
                  继续面试
                </el-button>
                <el-button 
                  v-else
                  type="primary"
                  size="small"
                  text
                  @click.stop="viewReport(item)"
                >
                  查看报告
                </el-button>
                <el-button 
                  type="danger"
                  size="small"
                  text
                  @click.stop="deleteInterview(item)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <el-pagination
            v-model:current-page="queryParams.pageNum"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 30]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Clock, Search } from '@element-plus/icons-vue'
import { mockInterviewApi } from '@/api/mockInterview'

const router = useRouter()

// 状态
const loading = ref(false)
const directions = ref([])
const historyList = ref([])
const total = ref(0)

// 查询参数
const queryParams = reactive({
  direction: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

// 获取状态类型 - 后端状态: 0-进行中 1-已完成 2-已中断
const getStatusType = (status) => {
  const types = {
    0: 'warning',
    1: 'success',
    2: 'info'
  }
  return types[status] || 'info'
}

// 获取状态文本 - 后端状态: 0-进行中 1-已完成 2-已中断
const getStatusText = (status) => {
  const texts = {
    0: '进行中',
    1: '已完成',
    2: '已中断'
  }
  return texts[status] || '未知'
}

// 获取难度文本
const getLevelText = (level) => {
  const texts = {
    1: '初级',
    2: '中级',
    3: '高级'
  }
  return texts[level] || '未知'
}

// 获取分数样式
const getScoreClass = (score) => {
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-medium'
  return 'score-low'
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 返回
const goBack = () => {
  router.push('/mock-interview')
}

// 获取方向列表
const fetchDirections = async () => {
try {
    const data = await mockInterviewApi.getDirections()
    directions.value = data || []
  } catch (error) {
    console.error('获取方向列表失败', error)
  }
}

// 获取历史记录
const fetchHistory = async () => {
  loading.value = true

try {
    const data = await mockInterviewApi.getHistory(queryParams)
    historyList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('获取历史记录失败', error)
    ElMessage.error('获取历史记录失败')
  } finally {
    loading.value = false
  }
}

// 筛选
const handleFilter = () => {
  queryParams.pageNum = 1
  fetchHistory()
}

// 重置筛选
const resetFilter = () => {
  queryParams.direction = ''
  queryParams.status = null
  queryParams.pageNum = 1
  fetchHistory()
}

// 分页大小改变
const handleSizeChange = () => {
  queryParams.pageNum = 1
  fetchHistory()
}

// 页码改变
const handlePageChange = () => {
  fetchHistory()
}

// 继续面试
const continueInterview = (item) => {
  router.push({
    path: '/mock-interview/session',
    query: { sessionId: item.id }
  })
}

// 查看报告 - 后端状态: 0-进行中 1-已完成 2-已中断
const viewReport = (item) => {
  if (item.status === 0) {
    ElMessage.warning('面试尚未完成，请先完成面试')
    return
  }
  router.push({
    path: '/mock-interview/report',
    query: { sessionId: item.id }
  })
}

// 删除面试记录
const deleteInterview = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除这条面试记录吗？删除后无法恢复。', '提示', {
      type: 'warning'
    })

    await mockInterviewApi.deleteInterview(item.id)
    ElMessage.success('删除成功')
    fetchHistory()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchDirections()
  fetchHistory()
})
</script>

<style scoped>
.mock-interview-history {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.back-nav {
  margin-bottom: 16px;
}

/* 页面头部 */
.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 筛选区域 */
.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

/* 历史记录卡片 */
.history-card {
  border-radius: 12px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.history-item:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.1);
}

.item-left {
  flex: 1;
}

.item-direction {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-info {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.info-label {
  color: #909399;
}

.divider {
  margin: 0 8px;
  color: #dcdfe6;
}

.item-time {
  font-size: 13px;
  color: #909399;
}

.item-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.item-score {
  text-align: center;
  min-width: 80px;
}

.score-value {
  font-size: 32px;
  font-weight: 700;
}

.score-value.score-high {
  color: #67c23a;
}

.score-value.score-medium {
  color: #e6a23c;
}

.score-value.score-low {
  color: #f56c6c;
}

.score-label {
  font-size: 14px;
  color: #909399;
}

.score-pending {
  font-size: 14px;
  color: #909399;
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
