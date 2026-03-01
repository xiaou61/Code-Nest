<template>
  <div class="mock-interview-sessions-page">
    <el-card class="overview-card" shadow="never">
      <div class="overview-header">
        <div>
          <h2>模拟面试运营台</h2>
          <p>查看核心指标、筛选会话并快速质检问答质量</p>
        </div>
        <div class="actions">
          <el-button @click="handleReset">
            重置筛选
          </el-button>
          <el-button type="primary" :loading="loading" @click="loadAllData">
            刷新数据
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="metric-row" v-loading="statsLoading">
      <el-col :xs="12" :md="6">
        <div class="metric-item metric-primary">
          <div class="metric-title">总会话数</div>
          <div class="metric-value">{{ stats.totalSessions || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="metric-item">
          <div class="metric-title">完成率</div>
          <div class="metric-value">{{ toPercent(stats.completionRate) }}%</div>
          <div class="metric-sub">完成 {{ stats.completedSessions || 0 }} 场</div>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="metric-item">
          <div class="metric-title">平均分</div>
          <div class="metric-value">{{ toNumber(stats.avgScore, 2) }}</div>
          <div class="metric-sub">活跃用户 {{ stats.activeUsers || 0 }}</div>
        </div>
      </el-col>
      <el-col :xs="12" :md="6">
        <div class="metric-item">
          <div class="metric-title">平均时长</div>
          <div class="metric-value">{{ stats.avgDurationMinutes || 0 }} 分钟</div>
          <div class="metric-sub">
            进行中 {{ stats.ongoingSessions || 0 }} / 中断 {{ stats.interruptedSessions || 0 }}
          </div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="distribution-card">
      <template #header>
        <div class="card-header">
          <span>方向分布</span>
        </div>
      </template>
      <el-empty v-if="!stats.directionDistributions?.length" description="暂无方向统计数据" />
      <el-table v-else :data="stats.directionDistributions" size="small">
        <el-table-column prop="directionName" label="方向" min-width="140" />
        <el-table-column prop="direction" label="代码" min-width="120" />
        <el-table-column prop="sessionCount" label="会话数" min-width="100" />
        <el-table-column label="平均分" min-width="100">
          <template #default="{ row }">{{ toNumber(row.avgScore, 2) }}</template>
        </el-table-column>
        <el-table-column label="完成率" min-width="120">
          <template #default="{ row }">{{ toPercent(row.completionRate) }}%</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="filter-form">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :md="6">
            <el-input v-model="filters.userId" clearable placeholder="用户ID" />
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-select v-model="filters.direction" clearable placeholder="面试方向" style="width: 100%">
              <el-option
                v-for="item in directions"
                :key="item.id"
                :label="item.directionName"
                :value="item.directionCode"
              />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-select v-model="filters.status" clearable placeholder="会话状态" style="width: 100%">
              <el-option label="进行中" :value="0" />
              <el-option label="已完成" :value="1" />
              <el-option label="已中断" :value="2" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-date-picker
              v-model="filters.timeRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 100%"
            />
          </el-col>
        </el-row>

        <el-row :gutter="12" class="mt-12">
          <el-col :xs="24" :sm="12" :md="6">
            <el-input-number v-model="filters.minScore" :min="0" :max="100" :step="1" controls-position="right" placeholder="最低分" style="width: 100%" />
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-input-number v-model="filters.maxScore" :min="0" :max="100" :step="1" controls-position="right" placeholder="最高分" style="width: 100%" />
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" class="filter-actions">
            <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">清空</el-button>
          </el-col>
        </el-row>
      </div>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="会话ID" min-width="90" />
        <el-table-column prop="userId" label="用户ID" min-width="90" />
        <el-table-column prop="directionName" label="方向" min-width="120" />
        <el-table-column prop="levelName" label="难度" min-width="100" />
        <el-table-column prop="styleName" label="风格" min-width="100" />
        <el-table-column prop="questionCount" label="题数" min-width="80" />
        <el-table-column label="模式" min-width="90">
          <template #default="{ row }">{{ questionModeText(row.questionMode) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="answeredCount" label="已回答主题" min-width="110" />
        <el-table-column prop="totalScore" label="总分" min-width="80" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="endTime" label="结束时间" min-width="170" />
        <el-table-column fixed="right" label="操作" min-width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="会话详情" size="65%" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="sessionDetail">
          <el-descriptions :column="3" border class="detail-descriptions">
            <el-descriptions-item label="会话ID">{{ sessionDetail.session.id }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ sessionDetail.session.userId }}</el-descriptions-item>
            <el-descriptions-item label="方向">{{ sessionDetail.session.directionName || sessionDetail.session.direction }}</el-descriptions-item>
            <el-descriptions-item label="难度">{{ sessionDetail.session.levelName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="风格">{{ sessionDetail.session.styleName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTagType(sessionDetail.session.status)">
                {{ statusText(sessionDetail.session.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总分">{{ sessionDetail.session.totalScore ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ sessionDetail.session.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ sessionDetail.session.endTime || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-row :gutter="12" class="qa-metrics">
            <el-col :span="6"><el-tag>已回答: {{ sessionDetail.answeredCount || 0 }}</el-tag></el-col>
            <el-col :span="6"><el-tag type="warning">已跳过: {{ sessionDetail.skippedCount || 0 }}</el-tag></el-col>
            <el-col :span="6"><el-tag type="info">待回答: {{ sessionDetail.pendingCount || 0 }}</el-tag></el-col>
            <el-col :span="6"><el-tag type="success">追问: {{ sessionDetail.followUpCount || 0 }}</el-tag></el-col>
          </el-row>

          <el-empty v-if="!sessionDetail.qaList?.length" description="暂无问答数据" />
          <el-collapse v-else>
            <el-collapse-item
              v-for="item in sessionDetail.qaList"
              :key="item.id"
              :title="`第 ${item.questionOrder} 题（${qaStatusText(item.status)}）`"
              :name="item.id"
            >
              <div class="qa-block">
                <p><strong>问题：</strong>{{ item.questionContent }}</p>
                <p><strong>回答：</strong>{{ item.userAnswer || '（未作答）' }}</p>
                <p><strong>得分：</strong>{{ item.score ?? '-' }}</p>
                <p><strong>知识点：</strong>{{ item.knowledgePoints || '-' }}</p>
                <p><strong>参考答案：</strong>{{ item.referenceAnswer || '-' }}</p>
                <div class="feedback" v-if="parseFeedback(item.aiFeedback)">
                  <p><strong>AI反馈：</strong></p>
                  <p>优点：{{ parseFeedback(item.aiFeedback).strengths?.join('、') || '无' }}</p>
                  <p>改进：{{ parseFeedback(item.aiFeedback).improvements?.join('、') || '无' }}</p>
                </div>
              </div>

              <div v-if="item.followUps?.length" class="follow-up-block">
                <h4>追问链路（{{ item.followUps.length }}）</h4>
                <el-card v-for="follow in item.followUps" :key="follow.id" shadow="never" class="follow-up-item">
                  <p><strong>追问：</strong>{{ follow.questionContent }}</p>
                  <p><strong>回答：</strong>{{ follow.userAnswer || '（未作答）' }}</p>
                  <p><strong>得分：</strong>{{ follow.score ?? '-' }}</p>
                </el-card>
              </div>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import mockInterviewAdminApi from '@/api/mockInterview'

const loading = ref(false)
const statsLoading = ref(false)
const detailLoading = ref(false)

const directions = ref([])
const tableData = ref([])
const stats = ref({})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const filters = reactive({
  userId: null,
  direction: '',
  status: null,
  minScore: null,
  maxScore: null,
  timeRange: []
})

const detailVisible = ref(false)
const sessionDetail = ref(null)

const statusText = (status) => {
  const map = { 0: '进行中', 1: '已完成', 2: '已中断' }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || ''
}

const qaStatusText = (status) => {
  const map = { 0: '待回答', 1: '已回答', 2: '已跳过' }
  return map[status] || '未知'
}

const questionModeText = (mode) => (mode === 2 ? 'AI出题' : '本地题库')

const toPercent = (value) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

const toNumber = (value, digits = 0) => {
  const num = Number(value || 0)
  return Number.isFinite(num) ? num.toFixed(digits) : (0).toFixed(digits)
}

const parseFeedback = (raw) => {
  if (!raw) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (error) {
    return null
  }
}

const buildQuery = () => {
  const [startTime, endTime] = filters.timeRange || []
  const rawUserId = filters.userId !== null && filters.userId !== undefined
    ? String(filters.userId).trim()
    : ''
  const userId = /^\d+$/.test(rawUserId) ? Number(rawUserId) : undefined
  return {
    pageNum: pagination.pageNum,
    pageSize: pagination.pageSize,
    userId,
    direction: filters.direction || undefined,
    status: filters.status,
    minScore: filters.minScore,
    maxScore: filters.maxScore,
    startTime,
    endTime
  }
}

const loadDirections = async () => {
  try {
    directions.value = await mockInterviewAdminApi.getDirections()
  } catch (error) {
    ElMessage.error('加载方向配置失败')
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const [startTime, endTime] = filters.timeRange || []
    stats.value = await mockInterviewAdminApi.getStatsOverview({ startTime, endTime })
  } catch (error) {
    ElMessage.error('加载运营指标失败')
  } finally {
    statsLoading.value = false
  }
}

const loadSessions = async () => {
  loading.value = true
  try {
    const res = await mockInterviewAdminApi.getSessions(buildQuery())
    tableData.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    ElMessage.error('加载会话列表失败')
  } finally {
    loading.value = false
  }
}

const loadAllData = async () => {
  await Promise.all([loadStats(), loadSessions()])
}

const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  sessionDetail.value = null
  try {
    sessionDetail.value = await mockInterviewAdminApi.getSessionDetail(row.id)
  } catch (error) {
    ElMessage.error('加载会话详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadAllData()
}

const handleReset = async () => {
  filters.userId = null
  filters.direction = ''
  filters.status = null
  filters.minScore = null
  filters.maxScore = null
  filters.timeRange = []
  pagination.pageNum = 1
  await loadAllData()
}

const handlePageChange = () => {
  loadSessions()
}

const handleSizeChange = () => {
  pagination.pageNum = 1
  loadSessions()
}

onMounted(async () => {
  await loadDirections()
  await loadAllData()
})
</script>

<style scoped>
.mock-interview-sessions-page {
  padding: 20px;
  background: #f5f7fb;
  min-height: 100vh;
}

.overview-card {
  margin-bottom: 16px;
}

.overview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overview-header h2 {
  margin: 0 0 6px;
}

.overview-header p {
  margin: 0;
  color: #909399;
}

.actions {
  display: flex;
  gap: 10px;
}

.metric-row {
  margin-bottom: 16px;
}

.metric-item {
  background: #fff;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.metric-primary {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
}

.metric-title {
  font-size: 13px;
  color: inherit;
  opacity: 0.85;
}

.metric-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}

.metric-sub {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.distribution-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  margin-bottom: 16px;
}

.filter-form {
  margin-bottom: 14px;
}

.mt-12 {
  margin-top: 12px;
}

.filter-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-descriptions {
  margin-bottom: 14px;
}

.qa-metrics {
  margin-bottom: 12px;
}

.qa-block p {
  margin: 8px 0;
  line-height: 1.6;
  word-break: break-word;
}

.follow-up-block {
  margin-top: 12px;
}

.follow-up-block h4 {
  margin: 0 0 8px;
}

.follow-up-item {
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .overview-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-actions {
    justify-content: flex-start;
  }
}
</style>
