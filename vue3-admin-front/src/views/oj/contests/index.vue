<template>
  <CnPage class="oj-contests-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="赛事管理"
      description="管理 OJ 赛事创建、发布状态、时间窗口和参赛数据。"
      eyebrow="OJ Contests"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">OJ 判题</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 场赛事</CnStatusTag>
        <CnStatusTag type="success">进行中 {{ runningCountInPage }} 场</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="router.push('/oj/contests/create')">新增赛事</el-button>
      </template>
    </CnPageHeader>

    <div class="oj-stat-grid">
      <CnStatCard title="赛事总量" :value="total" description="当前筛选条件下的赛事数量" tone="brand" />
      <CnStatCard title="进行中" :value="runningCountInPage" description="当前页正在进行的赛事" tone="success" />
      <CnStatCard title="题目总数" :value="problemCountInPage" description="当前页赛事题目累计数" tone="info" />
      <CnStatCard title="参赛人数" :value="participantCountInPage" description="当前页赛事参赛人数累计" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按赛事标题、赛事类型和状态筛选 OJ 赛事。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="赛事列表" :description="`共 ${total} 场赛事`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="contestList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="赛事数据" description="发布或下线赛事会影响用户端可参与状态。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ searchForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">参赛 {{ participantCountInPage }} 人</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="router.push('/oj/contests/create')">新增赛事</el-button>
          </CnToolbar>
        </template>

        <template #contestType="{ row }">
          <CnStatusTag :type="getTypeTone(row.contestType)" size="sm">{{ getTypeLabel(row.contestType) }}</CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">{{ getStatusLabel(row.status) }}</CnStatusTag>
        </template>

        <template #timeRange="{ row }">
          <div class="time-range">
            <span>{{ row.startTime || '-' }}</span>
            <span>~</span>
            <span>{{ row.endTime || '-' }}</span>
          </div>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="router.push(`/oj/contests/${row.id}/edit`)">编辑</el-button>
            <el-button v-if="getStatusAction(row)" :type="getStatusAction(row)?.type" link size="small" @click="handleStatusChange(row)">
              {{ getStatusAction(row)?.label }}
            </el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'
import {
  CnDataTable,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface ContestRecord {
  id: number | string
  title: string
  contestType?: string
  status: number
  startTime?: string
  endTime?: string
  problemCount?: number
  participantCount?: number
  createTime?: string
  [key: string]: unknown
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'OJ 判题管理' }, { label: '赛事管理' }]

const loading = ref(false)
const contestList = ref<ContestRecord[]>([])
const total = ref(0)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  contestType: '',
  status: null as number | null
})

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '赛事标题', type: 'input', placeholder: '请输入赛事标题关键词' },
  {
    prop: 'contestType',
    label: '赛事类型',
    type: 'select',
    placeholder: '请选择类型',
    options: [
      { label: '周赛', value: 'weekly' },
      { label: '挑战赛', value: 'challenge' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '草稿', value: 0 },
      { label: '即将开始', value: 1 },
      { label: '进行中', value: 2 },
      { label: '已结束', value: 3 }
    ]
  }
]

const tableColumns: CnTableColumn<ContestRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '赛事标题', minWidth: 220, showOverflowTooltip: true },
  { prop: 'contestType', label: '类型', width: 110, align: 'center', slot: 'contestType' },
  { prop: 'status', label: '状态', width: 120, align: 'center', slot: 'status' },
  { prop: 'startTime', label: '时间窗口', minWidth: 280, slot: 'timeRange', showOverflowTooltip: true },
  { prop: 'problemCount', label: '题目数', width: 100, align: 'center' },
  { prop: 'participantCount', label: '参赛人数', width: 110, align: 'center' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 180, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: searchForm.pageNum,
  pageSize: searchForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50]
}))

const runningCountInPage = computed(() => contestList.value.filter((item) => item.status === 2).length)
const problemCountInPage = computed(() => contestList.value.reduce((sum, item) => sum + (Number(item.problemCount) || 0), 0))
const participantCountInPage = computed(() => contestList.value.reduce((sum, item) => sum + (Number(item.participantCount) || 0), 0))

const getTypeLabel = (type?: string) => ({ weekly: '周赛', challenge: '挑战赛' })[type || ''] || type || '未知'
const getTypeTone = (type?: string): CnTone => ({ weekly: 'success', challenge: 'warning' })[type || ''] as CnTone || 'neutral'
const getStatusLabel = (status: number) => ({ 0: '草稿', 1: '即将开始', 2: '进行中', 3: '已结束' })[status] || `状态${status}`
const getStatusTone = (status: number): CnTone => ({ 0: 'neutral', 1: 'warning', 2: 'success', 3: 'info' })[status] as CnTone || 'neutral'

const normalizeContest = (item: Record<string, unknown>): ContestRecord => {
  const problemList = item.problemIdList || item.problemIds || item.problems || []
  return {
    id: (item.id ?? item.contestId ?? '') as number | string,
    title: String(item.title || item.name || ''),
    contestType: String(item.contestType || item.type || ''),
    status: Number(item.status ?? 0),
    startTime: String(item.startTime || item.beginTime || ''),
    endTime: String(item.endTime || item.finishTime || ''),
    problemCount: Number(item.problemCount ?? (Array.isArray(problemList) ? problemList.length : 0)),
    participantCount: Number(item.participantCount ?? item.joinCount ?? 0),
    createTime: String(item.createTime || item.createdAt || '')
  }
}

const resolveListData = (data: unknown) => {
  if (Array.isArray(data)) {
    return { records: data, total: data.length }
  }
  if (!data || typeof data !== 'object') {
    return { records: [], total: 0 }
  }
  const value = data as { records?: Record<string, unknown>[]; list?: Record<string, unknown>[]; total?: number }
  return {
    records: value.records || value.list || [],
    total: Number(value.total || 0)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = { ...searchForm }
    if (!params.keyword) delete params.keyword
    if (!params.contestType) delete params.contestType
    if (params.status === null || params.status === undefined) delete params.status
    const data = await ojApi.getContestList(params)
    const pageData = resolveListData(data)
    contestList.value = pageData.records.map(normalizeContest)
    total.value = pageData.total || 0
  } catch (error) {
    console.error('加载赛事列表失败:', error)
    ElMessage.error('加载赛事列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusAction = (row: ContestRecord) => {
  if (row.status === 0) {
    return { label: '发布', targetStatus: 1, type: 'success' as const }
  }
  if (row.status === 1) {
    return { label: '下线', targetStatus: 0, type: 'warning' as const }
  }
  return null
}

const handleStatusChange = (row: ContestRecord) => {
  const action = getStatusAction(row)
  if (!action) {
    ElMessage.warning('当前状态不支持直接切换')
    return
  }
  ElMessageBox.confirm(`确定${action.label}赛事「${row.title}」吗？`, '状态变更', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await ojApi.updateContestStatus(row.id, action.targetStatus)
      ElMessage.success(`${action.label}成功`)
      loadData()
    })
    .catch(() => {})
}

const handleDelete = (row: ContestRecord) => {
  ElMessageBox.confirm(`确定删除赛事「${row.title}」吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await ojApi.deleteContest(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleSearch = () => {
  searchForm.pageNum = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    contestType: '',
    status: null,
    pageNum: 1
  })
  loadData()
}

const handleSizeChange = (size: number) => {
  searchForm.pageSize = size
  searchForm.pageNum = 1
  loadData()
}

const handleCurrentChange = (page: number) => {
  searchForm.pageNum = page
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.oj-contests-page {
  min-height: 100%;
}

.oj-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.time-range {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  color: var(--cn-color-text-secondary);
}

.time-range span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .oj-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .oj-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
