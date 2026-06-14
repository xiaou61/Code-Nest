<template>
  <CnPage class="moment-comments-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="评论管理"
      description="管理朋友圈动态评论，支持按评论用户、动态 ID、内容关键词和时间范围筛选。"
      eyebrow="Moment Comments"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">朋友圈管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条评论</CnStatusTag>
        <CnStatusTag type="success">当前页 {{ tableData.length }} 条</CnStatusTag>
        <CnStatusTag type="info">关联动态 {{ relatedMomentCount }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="moment-stat-grid">
      <CnStatCard title="评论总量" :value="pagination.total" description="当前筛选条件下的朋友圈评论数量" tone="brand" />
      <CnStatCard title="当前页评论" :value="tableData.length" description="当前页可管理的评论记录" tone="success" />
      <CnStatCard title="关联动态" :value="relatedMomentCount" description="当前页评论关联的动态数量" tone="info" />
      <CnStatCard title="已删动态评论" :value="deletedMomentCommentCount" description="所属动态已删除的评论数量" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按评论用户、动态 ID、评论内容和发布时间筛选朋友圈评论。" divided>
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

    <CnSection title="评论列表" :description="`共 ${pagination.total} 条评论`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="评论数据" description="删除评论会影响动态互动内容，请确认上下文后操作。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">动态 {{ relatedMomentCount }} 条</CnStatusTag>
            </template>

            <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
          </CnToolbar>
        </template>

        <template #userNickname="{ row }">
          <div class="user-cell">
            <strong>{{ row.userNickname || '-' }}</strong>
            <span>评论 ID {{ row.id }}</span>
          </div>
        </template>

        <template #content="{ row }">
          <p class="comment-content">{{ row.content || '-' }}</p>
        </template>

        <template #moment="{ row }">
          <div v-if="row.momentContentSummary" class="moment-preview">
            <CnStatusTag type="neutral" size="sm">动态 {{ row.momentId || '-' }}</CnStatusTag>
            <p>{{ row.momentContentSummary }}</p>
          </div>
          <CnStatusTag v-else type="warning" size="sm">动态已删除</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无评论"
            description="当前筛选条件下没有朋友圈评论，可以重置筛选后再查看。"
            icon="MC"
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
import { deleteComment, getAdminCommentList } from '@/api/moment'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn } from '@/design-system'

interface MomentCommentRecord {
  id: number
  momentId?: number | string
  userNickname?: string
  content?: string
  momentContentSummary?: string
  createTime?: string
  [key: string]: unknown
}

interface FilterForm {
  userNickname: string
  momentId: string
  contentKeyword: string
  createTime: string[] | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '朋友圈管理' }, { label: '评论管理' }]

const loading = ref(false)
const tableData = ref<MomentCommentRecord[]>([])

const filterForm = reactive<FilterForm>({
  userNickname: '',
  momentId: '',
  contentKeyword: '',
  createTime: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const filterFields: CnFilterField[] = [
  { prop: 'userNickname', label: '评论用户', type: 'input', placeholder: '请输入评论用户昵称' },
  { prop: 'momentId', label: '动态ID', type: 'input', placeholder: '请输入动态ID' },
  { prop: 'contentKeyword', label: '内容关键词', type: 'input', placeholder: '请输入评论内容关键词' },
  { prop: 'createTime', label: '评论时间', type: 'daterange', placeholder: '开始日期' }
]

const tableColumns: CnTableColumn<MomentCommentRecord>[] = [
  { prop: 'id', label: '评论ID', width: 100 },
  { prop: 'momentId', label: '动态ID', width: 100 },
  { prop: 'userNickname', label: '评论用户', width: 140, slot: 'userNickname', showOverflowTooltip: true },
  { prop: 'content', label: '评论内容', minWidth: 260, slot: 'content', showOverflowTooltip: true },
  { prop: 'momentContentSummary', label: '所属动态', minWidth: 240, slot: 'moment', showOverflowTooltip: true },
  { prop: 'createTime', label: '评论时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 100, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.currentPage,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const relatedMomentCount = computed(() => new Set(tableData.value.map((item) => item.momentId).filter(Boolean)).size)
const deletedMomentCommentCount = computed(() => tableData.value.filter((item) => !item.momentContentSummary).length)

onMounted(() => {
  loadList()
})

const loadList = async () => {
  loading.value = true
  try {
    const result = await getAdminCommentList({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      userNickname: filterForm.userNickname || undefined,
      momentId: filterForm.momentId || undefined,
      contentKeyword: filterForm.contentKeyword || undefined,
      startDate: filterForm.createTime?.[0] || undefined,
      endDate: filterForm.createTime?.[1] || undefined
    })

    tableData.value = result?.records || []
    pagination.total = result?.total || 0
  } catch (error) {
    console.error('加载朋友圈评论失败:', error)
    ElMessage.error('加载评论列表失败')
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
    momentId: '',
    contentKeyword: '',
    createTime: null
  })
  pagination.currentPage = 1
  loadList()
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(filterForm, value)
}

const handleDelete = async (row: MomentCommentRecord) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.userNickname || '-'}" 的这条评论吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteComment(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除朋友圈评论失败:', error)
      ElMessage.error('删除失败')
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
</script>

<style scoped>
.moment-comments-page {
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

.comment-content {
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-primary);
  line-height: 1.55;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.moment-preview {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.moment-preview p {
  margin: 0;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.45;
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
