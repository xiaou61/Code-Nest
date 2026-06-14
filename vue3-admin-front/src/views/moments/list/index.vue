<template>
  <CnPage class="moments-list-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="动态管理"
      description="管理朋友圈动态内容、图片预览和批量删除操作，保障用户动态内容秩序。"
      eyebrow="Moments"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">朋友圈管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条动态</CnStatusTag>
        <CnStatusTag type="success">正常 {{ normalCountInPage }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedRows.length" type="warning">已选择 {{ selectedRows.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="moment-stat-grid">
      <CnStatCard title="动态总量" :value="pagination.total" description="当前筛选条件下的朋友圈动态数量" tone="brand" />
      <CnStatCard title="正常动态" :value="normalCountInPage" description="当前页正常展示的动态" tone="success" />
      <CnStatCard title="审核中" :value="pendingCountInPage" description="当前页处于审核状态的动态" tone="warning" />
      <CnStatCard title="互动总量" :value="engagementCountInPage" description="当前页点赞和评论累计数量" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按用户昵称、状态和发布时间筛选朋友圈动态。" divided>
      <CnFilterForm
        :model-value="filterForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleFilterUpdate"
        @search="handleSearch"
        @reset="handleRefresh"
      />
    </CnSection>

    <CnSection title="动态列表" :description="`共 ${pagination.total} 条动态`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="动态数据" description="批量删除会影响用户端朋友圈展示，请谨慎操作。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag v-if="selectedRows.length" type="warning" size="sm">已选择 {{ selectedRows.length }} 条</CnStatusTag>
            </template>

            <el-button type="danger" :icon="Delete" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
          </CnToolbar>
        </template>

        <template #userNickname="{ row }">
          <div class="user-cell">
            <strong>{{ row.userNickname || '-' }}</strong>
            <span>ID {{ row.id }}</span>
          </div>
        </template>

        <template #content="{ row }">
          <div class="moment-content">
            <p class="content-text">{{ row.content || '-' }}</p>
            <div v-if="row.images?.length" class="images-preview">
              <el-image
                v-for="(image, index) in row.images.slice(0, 3)"
                :key="`${row.id}-${index}`"
                :src="image"
                :preview-src-list="row.images"
                :initial-index="index"
                class="preview-image"
                fit="cover"
              />
              <span v-if="row.images.length > 3" class="more-images">+{{ row.images.length - 3 }}</span>
            </div>
          </div>
        </template>

        <template #likeCount="{ row }">
          <CnStatusTag :type="Number(row.likeCount) > 0 ? 'success' : 'neutral'" size="sm">
            {{ row.likeCount || 0 }}
          </CnStatusTag>
        </template>

        <template #commentCount="{ row }">
          <CnStatusTag :type="Number(row.commentCount) > 0 ? 'info' : 'neutral'" size="sm">
            {{ row.commentCount || 0 }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ getStatusText(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无动态"
            description="当前筛选条件下没有朋友圈动态，可以重置筛选后再查看。"
            icon="MO"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleRefresh">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Refresh } from '@element-plus/icons-vue'
import { batchDeleteMoments, getAdminMomentList } from '@/api/moment'
import {
  CnDataTable,
  CnEmptyState,
  CnFilterForm,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface MomentRecord {
  id: number
  userNickname: string
  content?: string
  images?: string[]
  likeCount?: number
  commentCount?: number
  status: number
  createTime?: string
  [key: string]: unknown
}

interface FilterForm {
  userNickname: string
  status: number | null
  createTime: string[] | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '朋友圈管理' }, { label: '动态管理' }]

const loading = ref(false)
const tableData = ref<MomentRecord[]>([])
const selectedRows = ref<MomentRecord[]>([])

const filterForm = reactive<FilterForm>({
  userNickname: '',
  status: null,
  createTime: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const filterFields: CnFilterField[] = [
  { prop: 'userNickname', label: '用户昵称', type: 'input', placeholder: '请输入用户昵称' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '选择状态',
    options: [
      { label: '正常', value: 1 },
      { label: '已删除', value: 0 },
      { label: '审核中', value: 2 }
    ]
  },
  { prop: 'createTime', label: '发布时间', type: 'daterange', placeholder: '开始日期' }
]

const tableColumns: CnTableColumn<MomentRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'userNickname', label: '用户', width: 140, slot: 'userNickname', showOverflowTooltip: true },
  { prop: 'content', label: '动态内容', minWidth: 260, slot: 'content', showOverflowTooltip: true },
  { prop: 'likeCount', label: '点赞数', width: 90, align: 'center', slot: 'likeCount' },
  { prop: 'commentCount', label: '评论数', width: 90, align: 'center', slot: 'commentCount' },
  { prop: 'status', label: '状态', width: 90, slot: 'status' },
  { prop: 'createTime', label: '发布时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 100, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.currentPage,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const normalCountInPage = computed(() => tableData.value.filter((item) => item.status === 1).length)
const pendingCountInPage = computed(() => tableData.value.filter((item) => item.status === 2).length)
const engagementCountInPage = computed(() =>
  tableData.value.reduce((sum, item) => sum + (Number(item.likeCount) || 0) + (Number(item.commentCount) || 0), 0)
)

onMounted(() => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    const result = await getAdminMomentList({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      userNickname: filterForm.userNickname || undefined,
      status: filterForm.status !== null ? filterForm.status : undefined,
      startDate: filterForm.createTime?.[0] || undefined,
      endDate: filterForm.createTime?.[1] || undefined
    })
    tableData.value = result?.records || []
    pagination.total = result?.total || 0
  } catch (error) {
    console.error('加载动态列表失败:', error)
    ElMessage.error('加载列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadList()
}

const handleRefresh = () => {
  Object.assign(filterForm, {
    userNickname: '',
    status: null,
    createTime: null
  })
  pagination.currentPage = 1
  selectedRows.value = []
  loadList()
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(filterForm, value)
}

const handleSelectionChange = (selection: MomentRecord[]) => {
  selectedRows.value = selection
}

const handleDelete = async (row: MomentRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.userNickname}" 的这条动态吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await batchDeleteMoments([row.id])
    ElMessage.success('删除成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除动态失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) return

  try {
    await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedRows.value.length} 条动态吗？`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await batchDeleteMoments(selectedRows.value.map((item) => item.id))
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除动态失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.currentPage = page
  loadList()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  loadList()
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '已删除',
    1: '正常',
    2: '审核中'
  }
  return map[status] || '未知'
}

const getStatusTone = (status: number): CnTone => {
  const map: Record<number, CnTone> = {
    0: 'neutral',
    1: 'success',
    2: 'warning'
  }
  return map[status] || 'neutral'
}
</script>

<style scoped>
.moments-list-page {
  min-height: 100%;
}

.moment-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.user-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.user-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.moment-content {
  display: grid;
  gap: var(--cn-space-2);
  min-width: 0;
}

.content-text {
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.images-preview {
  display: flex;
  align-items: center;
  gap: 5px;
}

.preview-image {
  width: 40px;
  height: 40px;
  border-radius: var(--cn-radius-control);
  cursor: pointer;
}

.more-images {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
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
  .moment-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .moment-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
