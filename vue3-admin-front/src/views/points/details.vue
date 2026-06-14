<template>
  <CnPage class="points-details-page" surface="transparent" max-width="1480px">
    <CnPageHeader
      title="积分明细管理"
      description="查看和管理所有用户的积分变动记录，支持筛选、分页、详情查看与 CSV 导出。"
      eyebrow="Points Details"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">明细 {{ pagination.total }} 条</CnStatusTag>
        <CnStatusTag type="success">每页 {{ pagination.pageSize }} 条</CnStatusTag>
      </template>
      <template #actions>
        <el-button type="success" :icon="Download" :loading="exporting" @click="handleExport">
          {{ exporting ? '导出中...' : '导出' }}
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按用户、积分类型、管理员和时间范围筛选积分明细。" divided>
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="输入用户ID" clearable class="filter-sm" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.userName" placeholder="输入用户名搜索" clearable class="filter-md" />
        </el-form-item>
        <el-form-item label="积分类型">
          <el-select v-model="searchForm.pointsType" clearable class="filter-sm">
            <el-option label="全部" :value="null" />
            <el-option label="后台发放" :value="1" />
            <el-option label="打卡积分" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员ID">
          <el-input v-model="searchForm.adminId" placeholder="输入管理员ID" clearable class="filter-sm" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="searchForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="filter-date"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="searchForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            class="filter-date"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="积分明细列表" :description="`共 ${pagination.total} 条积分变动记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        empty-title="暂无积分明细"
        empty-description="当前筛选条件下没有匹配的积分变动记录。"
        empty-icon="PD"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #id="{ row }">
          <span class="muted-id">#{{ row.id }}</span>
        </template>

        <template #user="{ row }">
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

        <template #pointsChange="{ row }">
          <div class="points-change" :class="{ 'points-positive': row.pointsChange > 0 }">
            <span class="points-symbol">+</span>
            <span class="points-value">{{ formatNumber(row.pointsChange) }}</span>
          </div>
        </template>

        <template #pointsType="{ row }">
          <CnStatusTag :type="getPointsTypeTone(row.pointsType)" size="sm">{{ row.pointsTypeDesc }}</CnStatusTag>
        </template>

        <template #description="{ row }">
          <div class="description-cell">
            <div class="description-text">{{ row.description }}</div>
            <div v-if="row.continuousDays" class="description-extra">连续{{ row.continuousDays }}天</div>
          </div>
        </template>

        <template #balanceAfter="{ row }">
          <div class="balance-cell">
            <div class="balance-value">{{ formatNumber(row.balanceAfter) }}</div>
            <div class="balance-yuan">≈{{ ((row.balanceAfter || 0) / 1000).toFixed(2) }}元</div>
          </div>
        </template>

        <template #admin="{ row }">
          <div v-if="row.adminId" class="admin-cell">
            <div class="admin-name">{{ row.adminName || `管理员${row.adminId}` }}</div>
            <div class="admin-id">ID: {{ row.adminId }}</div>
          </div>
          <CnStatusTag v-else type="neutral" size="sm" subtle>系统</CnStatusTag>
        </template>

        <template #createTime="{ row }">
          <div class="time-cell">
            <div class="time-date">{{ formatDate(row.createTime) }}</div>
            <div class="time-time">{{ formatTime(row.createTime) }}</div>
          </div>
        </template>

        <template #actions="{ row }">
          <el-button type="info" link size="small" :icon="View" @click="handleViewDetail(row)">详情</el-button>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="showDetailDialog" title="积分明细详情" width="600px">
      <div v-if="selectedDetail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="明细ID">
            <span class="muted-id">#{{ selectedDetail.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">{{ selectedDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="用户名">
            {{ selectedDetail.userName || `用户${selectedDetail.userId}` }}
          </el-descriptions-item>
          <el-descriptions-item label="积分变动">
            <div class="points-change points-positive">+{{ formatNumber(selectedDetail.pointsChange) }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="积分类型">
            <CnStatusTag :type="getPointsTypeTone(selectedDetail.pointsType)" size="sm">
              {{ selectedDetail.pointsTypeDesc }}
            </CnStatusTag>
          </el-descriptions-item>
          <el-descriptions-item label="变动后余额">
            <div class="balance-cell">
              <div class="balance-value">{{ formatNumber(selectedDetail.balanceAfter) }}</div>
              <div class="balance-yuan">≈{{ ((selectedDetail.balanceAfter || 0) / 1000).toFixed(2) }}元</div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="变动描述" span="2">{{ selectedDetail.description }}</el-descriptions-item>
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
        <div class="dialog-footer">
          <el-button @click="showDetailDialog = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search, User, View } from '@element-plus/icons-vue'
import { pointsApi } from '@/api/points'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface PointsDetail extends Record<string, unknown> {
  id: number
  userId: number
  userName?: string
  pointsChange?: number
  pointsType?: number
  pointsTypeDesc?: string
  description?: string
  balanceAfter?: number
  continuousDays?: number
  adminId?: number
  adminName?: string
  createTime?: string
}

const route = useRoute()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '积分管理' }, { label: '积分明细' }]

const loading = ref(false)
const tableData = ref<PointsDetail[]>([])
const showDetailDialog = ref(false)
const selectedDetail = ref<PointsDetail | null>(null)
const exporting = ref(false)
const MAX_EXPORT_ROWS = 5000

const searchForm = reactive({
  userId: (route.query.userId as string) || '',
  userName: (route.query.userName as string) || '',
  pointsType: null as number | null,
  adminId: '',
  startTime: '',
  endTime: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const tableColumns: CnTableColumn<PointsDetail>[] = [
  { prop: 'id', label: '明细ID', width: 100, align: 'center', slot: 'id' },
  { label: '用户信息', minWidth: 160, slot: 'user' },
  { prop: 'pointsChange', label: '积分变动', width: 120, align: 'center', slot: 'pointsChange' },
  { prop: 'pointsType', label: '积分类型', width: 110, align: 'center', slot: 'pointsType' },
  { prop: 'description', label: '变动描述', minWidth: 200, slot: 'description' },
  { prop: 'balanceAfter', label: '变动后余额', width: 130, align: 'right', slot: 'balanceAfter' },
  { label: '操作管理员', width: 130, align: 'center', slot: 'admin' },
  { prop: 'createTime', label: '创建时间', width: 170, align: 'center', slot: 'createTime', sortable: true },
  { label: '操作', width: 100, align: 'center', fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const formatNumber = (num?: number | string) => {
  if (!num) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const formatDate = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleDateString('zh-CN')
}

const formatTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleTimeString('zh-CN')
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

const getPointsTypeTone = (type?: number): CnTone => {
  if (type === 1) return 'warning'
  if (type === 2) return 'success'
  return 'info'
}

const buildQueryParams = (source: Record<string, unknown>) => {
  const params = { ...source }
  Object.keys(params).forEach((key) => {
    if (params[key] === '' || params[key] === null || params[key] === undefined) {
      delete params[key]
    }
  })
  return params
}

const loadData = async () => {
  loading.value = true
  try {
    const params = buildQueryParams({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    const result = await pointsApi.getAllPointsDetailList(params)
    tableData.value = Array.isArray(result?.records) ? result.records : []
    pagination.total = Number(result?.total) || 0
  } catch (error) {
    console.error('加载积分明细数据失败:', error)
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

const handleExport = async () => {
  exporting.value = true

  try {
    const params = buildQueryParams({
      pageNum: 1,
      pageSize: MAX_EXPORT_ROWS,
      ...searchForm
    })

    const result = await pointsApi.getAllPointsDetailList(params)
    const rows: PointsDetail[] = Array.isArray(result?.records) ? result.records : []

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

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

const handleViewDetail = (row: PointsDetail) => {
  selectedDetail.value = row
  showDetailDialog.value = true
}

const downloadCsv = (rows: PointsDetail[]) => {
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
    ...rows.map((row) =>
      headers
        .map(([key]) => escapeCsv(row[key] ?? ''))
        .join(',')
    )
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

const escapeCsv = (value: unknown) => {
  const text = String(value).replace(/\r?\n/g, ' ')
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const formatExportTime = (date: Date) => {
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.points-details-page {
  min-height: 100%;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-3);
}

.filter-sm {
  width: 150px;
}

.filter-md {
  width: 200px;
}

.filter-date {
  width: 180px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
}

.user-info {
  min-width: 0;
}

.user-name,
.description-text,
.balance-value,
.admin-name,
.time-date {
  color: var(--cn-color-text-primary);
  font-weight: 600;
}

.user-id,
.description-extra,
.balance-yuan,
.admin-id,
.time-time,
.muted-id {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.points-change {
  color: var(--cn-color-text-tertiary);
  font-weight: 650;
}

.points-change.points-positive {
  color: var(--cn-color-success);
}

.points-value {
  font-size: 16px;
}

.description-cell {
  line-height: 1.5;
}

.balance-cell {
  text-align: right;
}

.admin-cell,
.time-cell {
  text-align: center;
}

.detail-content {
  padding: var(--cn-space-2) 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .filter-sm,
  .filter-md,
  .filter-date {
    width: 100%;
  }
}
</style>
