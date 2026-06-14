<template>
  <CnPage class="oj-problems-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="题目管理"
      description="管理 OJ 判题系统题目，支持难度、状态筛选和题目编辑删除。"
      eyebrow="OJ Problems"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">OJ 判题</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ total }} 道题</CnStatusTag>
        <CnStatusTag type="success">公开 {{ publicCountInPage }} 道</CnStatusTag>
        <CnStatusTag type="warning">隐藏 {{ hiddenCountInPage }} 道</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="primary" :icon="Plus" @click="router.push('/oj/problems/create')">新增题目</el-button>
      </template>
    </CnPageHeader>

    <div class="oj-stat-grid">
      <CnStatCard title="题目总量" :value="total" description="当前筛选条件下的题目数量" tone="brand" />
      <CnStatCard title="公开题目" :value="publicCountInPage" description="当前页公开展示的题目" tone="success" />
      <CnStatCard title="提交总数" :value="submitCountInPage" description="当前页题目累计提交数" tone="info" />
      <CnStatCard title="通过总数" :value="acceptedCountInPage" description="当前页题目累计通过数" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按题目标题、难度和发布状态筛选 OJ 题目。" divided>
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

    <CnSection title="题目列表" :description="`共 ${total} 道题目`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="problemList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="题目数据" description="状态切换会直接影响用户端题目可见性。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ searchForm.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">提交 {{ submitCountInPage }} 次</CnStatusTag>
            </template>
            <el-button type="primary" :icon="Plus" @click="router.push('/oj/problems/create')">新增题目</el-button>
          </CnToolbar>
        </template>

        <template #difficulty="{ row }">
          <CnStatusTag :type="getDifficultyTone(row.difficulty)" size="sm">
            {{ getDifficultyLabel(row.difficulty) }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>

        <template #submitCount="{ row }">
          <CnStatusTag type="info" size="sm">{{ row.submitCount || 0 }}</CnStatusTag>
        </template>

        <template #acceptedCount="{ row }">
          <CnStatusTag type="success" size="sm">{{ row.acceptedCount || 0 }}</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="router.push(`/oj/problems/${row.id}/edit`)">编辑</el-button>
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

interface OjProblem {
  id: number
  title: string
  difficulty?: string
  status: number
  submitCount?: number
  acceptedCount?: number
  createTime?: string
  [key: string]: unknown
}

const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'OJ 判题管理' }, { label: '题目管理' }]

const loading = ref(false)
const problemList = ref<OjProblem[]>([])
const total = ref(0)

const searchForm = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  difficulty: null as string | null,
  status: null as number | null
})

const filterFields: CnFilterField[] = [
  { prop: 'keyword', label: '题目标题', type: 'input', placeholder: '请输入题目标题' },
  {
    prop: 'difficulty',
    label: '难度',
    type: 'select',
    placeholder: '请选择难度',
    options: [
      { label: '简单', value: 'easy' },
      { label: '中等', value: 'medium' },
      { label: '困难', value: 'hard' }
    ]
  },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '公开', value: 1 },
      { label: '隐藏', value: 0 }
    ]
  }
]

const tableColumns: CnTableColumn<OjProblem>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '标题', minWidth: 220, showOverflowTooltip: true },
  { prop: 'difficulty', label: '难度', width: 100, align: 'center', slot: 'difficulty' },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { prop: 'submitCount', label: '提交数', width: 100, align: 'center', slot: 'submitCount' },
  { prop: 'acceptedCount', label: '通过数', width: 100, align: 'center', slot: 'acceptedCount' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 150, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: searchForm.pageNum,
  pageSize: searchForm.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50]
}))

const publicCountInPage = computed(() => problemList.value.filter((item) => item.status === 1).length)
const hiddenCountInPage = computed(() => problemList.value.filter((item) => item.status === 0).length)
const submitCountInPage = computed(() => problemList.value.reduce((sum, item) => sum + (Number(item.submitCount) || 0), 0))
const acceptedCountInPage = computed(() => problemList.value.reduce((sum, item) => sum + (Number(item.acceptedCount) || 0), 0))

const getDifficultyLabel = (difficulty?: string) => ({ easy: '简单', medium: '中等', hard: '困难' })[difficulty || ''] || difficulty || '-'
const getDifficultyTone = (difficulty?: string): CnTone => ({ easy: 'success', medium: 'warning', hard: 'danger' })[difficulty || ''] as CnTone || 'neutral'

const loadData = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = { ...searchForm }
    if (!params.keyword) delete params.keyword
    if (params.difficulty === null) delete params.difficulty
    if (params.status === null) delete params.status
    const data = await ojApi.getProblemList(params)
    problemList.value = data?.records || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('加载题目列表失败:', error)
    ElMessage.error('加载题目列表失败')
  } finally {
    loading.value = false
  }
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
    difficulty: null,
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

const handleStatusChange = async (row: OjProblem) => {
  try {
    await ojApi.updateProblem(row.id, { status: row.status })
    ElMessage.success('状态更新成功')
  } catch {
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleDelete = (row: OjProblem) => {
  ElMessageBox.confirm(`确定删除题目「${row.title}」吗？`, '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await ojApi.deleteProblem(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.oj-problems-page {
  min-height: 100%;
}

.oj-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
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
