<template>
  <CnPage class="points-users-page" surface="transparent" max-width="1480px">
    <CnPageHeader
      title="用户积分排行"
      description="查看和管理用户积分排行数据，支持积分范围筛选、排序和快速发放积分。"
      eyebrow="Points Ranking"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">用户 {{ pagination.total }} 人</CnStatusTag>
        <CnStatusTag type="success">排序 {{ searchForm.orderDirection === 'desc' ? '降序' : '升序' }}</CnStatusTag>
      </template>
      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按用户名、积分范围和排序方式筛选用户排行。" divided>
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.userName" placeholder="输入用户名搜索" clearable class="filter-md" />
        </el-form-item>
        <el-form-item label="最小积分">
          <el-input-number v-model="searchForm.minPoints" :min="0" controls-position="right" placeholder="最小积分" class="filter-number" />
        </el-form-item>
        <el-form-item label="最大积分">
          <el-input-number v-model="searchForm.maxPoints" :min="0" controls-position="right" placeholder="最大积分" class="filter-number" />
        </el-form-item>
        <el-form-item label="排序方式">
          <el-select v-model="searchForm.orderBy" class="filter-sm">
            <el-option label="按积分" value="points" />
            <el-option label="按时间" value="create_time" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="searchForm.orderDirection" class="filter-xs">
            <el-option label="降序" value="desc" />
            <el-option label="升序" value="asc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="用户积分列表" :description="`共 ${pagination.total} 位用户`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="userId"
        empty-title="暂无积分用户"
        empty-description="当前筛选条件下没有匹配的用户积分数据。"
        empty-icon="PU"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #ranking="{ $index }">
          <div class="rank-cell">
            <span class="rank-badge" :class="rankClass(getRealRanking($index))">{{ getRealRanking($index) }}</span>
          </div>
        </template>

        <template #user="{ row }">
          <div class="user-cell">
            <el-avatar :size="40" :src="row.avatar">
              <el-icon><User /></el-icon>
            </el-avatar>
            <div class="user-info">
              <div class="user-name">{{ row.userName }}</div>
              <div class="user-nick">{{ row.nickName }}</div>
            </div>
          </div>
        </template>

        <template #totalPoints="{ row }">
          <div class="points-cell">
            <div class="points-value">{{ formatNumber(row.totalPoints) }}</div>
            <div class="points-yuan">≈{{ row.balanceYuan }}元</div>
          </div>
        </template>

        <template #continuousDays="{ row }">
          <CnStatusTag :type="row.continuousDays > 7 ? 'success' : row.continuousDays > 0 ? 'warning' : 'info'" size="sm">
            {{ row.continuousDays }}天
          </CnStatusTag>
        </template>

        <template #dateTime="{ row }">
          {{ formatDateTime(row.createTime) }}
        </template>

        <template #updateTime="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Plus" @click="handleGrantPoints(row)">发放积分</el-button>
            <el-button type="info" link size="small" :icon="View" @click="handleViewDetails(row)">查看明细</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="showGrantDialog" title="发放积分" width="500px">
      <el-form ref="grantFormRef" :model="grantForm" :rules="grantRules" label-width="100px">
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
          <el-input-number v-model="grantForm.points" :min="1" :max="10000" controls-position="right" placeholder="请输入积分数量" />
        </el-form-item>
        <el-form-item label="发放原因" prop="reason">
          <el-input v-model="grantForm.reason" type="textarea" :rows="3" placeholder="请输入发放原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showGrantDialog = false">取消</el-button>
          <el-button type="primary" :loading="grantLoading" @click="handleConfirmGrant">确认发放</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, User, View } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn } from '@/design-system'

interface UserPoints extends Record<string, unknown> {
  userId: number
  userName?: string
  nickName?: string
  avatar?: string
  totalPoints?: number
  balanceYuan?: number | string
  continuousDays?: number
  createTime?: string
  updateTime?: string
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '积分管理' }, { label: '积分排行' }]

const loading = ref(false)
const tableData = ref<UserPoints[]>([])
const showGrantDialog = ref(false)
const grantLoading = ref(false)
const grantFormRef = ref()

const searchForm = reactive({
  userName: '',
  minPoints: null as number | null,
  maxPoints: null as number | null,
  orderBy: 'points',
  orderDirection: 'desc'
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const grantForm = reactive({
  user: null as UserPoints | null,
  points: null as number | null,
  reason: ''
})

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

const tableColumns: CnTableColumn<UserPoints>[] = [
  { label: '排名', width: 80, align: 'center', slot: 'ranking' },
  { label: '用户信息', minWidth: 170, slot: 'user' },
  { prop: 'totalPoints', label: '总积分', width: 130, align: 'right', slot: 'totalPoints', sortable: true },
  { prop: 'continuousDays', label: '连续打卡', width: 110, align: 'center', slot: 'continuousDays' },
  { prop: 'createTime', label: '注册时间', width: 170, align: 'center', slot: 'dateTime' },
  { prop: 'updateTime', label: '最后更新', width: 170, align: 'center', slot: 'updateTime' },
  { label: '操作', width: 180, align: 'center', fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const getRealRanking = (index: number) => {
  return (pagination.pageNum - 1) * pagination.pageSize + index + 1
}

const rankClass = (rank: number) => ({
  'rank-first': rank === 1,
  'rank-second': rank === 2,
  'rank-third': rank === 3
})

const formatNumber = (num?: number | string | null) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true

  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }

    const result = await pointsApi.getUserPointsList(params)
    tableData.value = Array.isArray(result?.records) ? result.records : []
    pagination.total = Number(result?.total) || 0
  } catch (error) {
    console.error('加载用户积分数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

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

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

const handleGrantPoints = (user: UserPoints) => {
  grantForm.user = user
  grantForm.points = null
  grantForm.reason = ''
  showGrantDialog.value = true
}

const handleConfirmGrant = async () => {
  if (!grantFormRef.value || !grantForm.user) return

  try {
    await grantFormRef.value.validate()

    await ElMessageBox.confirm(`确认为用户 ${grantForm.user.userName} 发放 ${grantForm.points} 积分吗？`, '确认发放', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    grantLoading.value = true
    await pointsApi.grantPoints({
      userId: grantForm.user.userId,
      points: grantForm.points,
      reason: grantForm.reason
    })

    ElMessage.success('积分发放成功')
    grantFormRef.value.resetFields()
    Object.assign(grantForm, {
      user: null,
      points: null,
      reason: ''
    })
    showGrantDialog.value = false
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

const handleViewDetails = (user: UserPoints) => {
  router.push({
    path: '/points/details',
    query: { userId: user.userId, userName: user.userName }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.points-users-page {
  min-height: 100%;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-3);
}

.filter-xs {
  width: 100px;
}

.filter-sm {
  width: 120px;
}

.filter-md {
  width: 200px;
}

.filter-number {
  width: 150px;
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
  border-radius: var(--cn-radius-pill);
  background: var(--cn-color-bg-surface-muted);
  color: var(--cn-color-text-secondary);
  font-weight: 700;
}

.rank-badge.rank-first,
.rank-badge.rank-second,
.rank-badge.rank-third {
  background: var(--cn-color-warning-soft);
  color: var(--cn-color-warning);
}

.user-cell,
.grant-user-info,
.table-actions,
.dialog-footer {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.user-info,
.grant-user-detail {
  min-width: 0;
}

.user-name,
.points-value,
.grant-user-name {
  color: var(--cn-color-text-primary);
  font-weight: 650;
}

.user-nick,
.points-yuan,
.grant-user-points {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.points-cell {
  text-align: right;
}

.grant-user-info {
  width: 100%;
  padding: var(--cn-space-3);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.table-actions,
.dialog-footer {
  flex-wrap: wrap;
}

.dialog-footer {
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .filter-xs,
  .filter-sm,
  .filter-md,
  .filter-number {
    width: 100%;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
