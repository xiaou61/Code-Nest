<template>
  <CnPage class="codepen-pens-page" surface="transparent" max-width="1400px">
    <CnPageHeader
      title="CodePen 作品"
      description="审核和运营用户作品，支持搜索、分类筛选、推荐、上下架和删除。"
      eyebrow="CodePen Pens"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">作品 {{ total }} 个</CnStatusTag>
        <CnStatusTag type="success">本页发布 {{ publishedCount }} 个</CnStatusTag>
        <CnStatusTag type="danger">推荐 {{ recommendedCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按关键词、分类、付费类型和发布状态定位作品。" divided>
      <el-form :inline="true" :model="queryParams" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索作品标题或描述" clearable class="filter-control" />
        </el-form-item>

        <el-form-item label="分类">
          <el-select v-model="queryParams.category" placeholder="请选择分类" clearable class="filter-select">
            <el-option label="全部" value="" />
            <el-option label="动画" value="动画" />
            <el-option label="组件" value="组件" />
            <el-option label="游戏" value="游戏" />
            <el-option label="工具" value="工具" />
          </el-select>
        </el-form-item>

        <el-form-item label="类型">
          <el-select v-model="queryParams.isFree" placeholder="请选择类型" clearable class="filter-select is-compact">
            <el-option label="全部" :value="null" />
            <el-option label="免费" :value="1" />
            <el-option label="付费" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="filter-select is-compact">
            <el-option label="全部" :value="null" />
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下架" :value="2" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="作品列表" :description="`共 ${total} 条作品记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        empty-title="暂无作品"
        empty-description="当前筛选条件下没有匹配作品，可以重置筛选后再查看。"
        empty-icon="CP"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #title="{ row }">
          <el-link type="primary" @click="viewDetail(row.id)">{{ row.title }}</el-link>
        </template>

        <template #isFree="{ row }">
          <CnStatusTag v-if="row.isFree" type="success" size="sm">免费</CnStatusTag>
          <CnStatusTag v-else type="warning" size="sm">付费 {{ row.forkPrice || 0 }} 积分</CnStatusTag>
        </template>

        <template #statistics="{ row }">
          <div class="stats-cell">
            <span>浏览 {{ row.viewCount || 0 }}</span>
            <span>点赞 {{ row.likeCount || 0 }}</span>
            <span>Fork {{ row.forkCount || 0 }}</span>
          </div>
        </template>

        <template #isRecommend="{ row }">
          <CnStatusTag v-if="row.isRecommend === 1" type="danger" size="sm">推荐</CnStatusTag>
          <CnStatusTag v-else type="neutral" size="sm" subtle>普通</CnStatusTag>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ getStatusText(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button link type="primary" size="small" :icon="View" @click="viewDetail(row.id)">查看</el-button>

            <el-button
              v-if="row.isRecommend !== 1"
              link
              type="success"
              size="small"
              :icon="Star"
              @click="handleRecommend(row)"
            >
              推荐
            </el-button>

            <el-button
              v-else
              link
              type="warning"
              size="small"
              :icon="StarFilled"
              @click="handleCancelRecommend(row)"
            >
              取消推荐
            </el-button>

            <el-dropdown @command="(command) => handleCommand(command, row)">
              <el-button link type="primary" size="small">
                更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.status === 1" command="offline" :icon="Remove">下架</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 2" command="online" :icon="Check">上架</el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="recommendDialog.visible" title="设置推荐" width="400px">
      <el-form :model="recommendDialog.form" label-width="100px">
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="recommendDialog.form.expireTime"
            type="datetime"
            placeholder="选择过期时间"
            class="full-width"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="recommendDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="recommendDialog.loading" @click="confirmRecommend">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Check, Delete, Refresh, Remove, Search, Star, StarFilled, View } from '@element-plus/icons-vue'
import { codepenApi } from '@/api/codepen'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface PenQuery {
  pageNum: number
  pageSize: number
  keyword: string
  category: string
  isFree: number | null
  status: number | null
}

interface CodePenRecord extends Record<string, unknown> {
  id: number
  title: string
  userNickname?: string
  category?: string
  isFree?: number | boolean
  forkPrice?: number
  viewCount?: number
  likeCount?: number
  forkCount?: number
  isRecommend?: number
  status?: number
  createTime?: string
}

interface RecommendDialogState {
  visible: boolean
  loading: boolean
  penId: number | null
  form: {
    expireTime: string | Date | null
  }
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'CodePen 管理' }, { label: '作品管理' }]

const queryParams = ref<PenQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  category: '',
  isFree: null,
  status: null
})

const tableData = ref<CodePenRecord[]>([])
const total = ref(0)
const loading = ref(false)

const recommendDialog = ref<RecommendDialogState>({
  visible: false,
  loading: false,
  penId: null,
  form: {
    expireTime: null
  }
})

const tableColumns: CnTableColumn<CodePenRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '作品标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'userNickname', label: '作者', width: 130, showOverflowTooltip: true },
  { prop: 'category', label: '分类', width: 110, showOverflowTooltip: true },
  { prop: 'isFree', label: '类型', width: 130, slot: 'isFree' },
  { label: '统计数据', width: 180, slot: 'statistics' },
  { prop: 'isRecommend', label: '推荐', width: 90, slot: 'isRecommend' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 170, showOverflowTooltip: true },
  { label: '操作', width: 280, fixed: 'right', slot: 'actions' }
]

const pagination = computed<CnPagination>(() => ({
  page: queryParams.value.pageNum,
  pageSize: queryParams.value.pageSize,
  total: total.value,
  pageSizes: [10, 20, 50, 100]
}))

const publishedCount = computed(() => tableData.value.filter((item) => item.status === 1).length)
const recommendedCount = computed(() => tableData.value.filter((item) => item.isRecommend === 1).length)

const loadData = async () => {
  try {
    loading.value = true
    const result = await codepenApi.getPenList(queryParams.value)
    tableData.value = Array.isArray(result?.records) ? result.records : []
    total.value = Number(result?.total) || 0
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  queryParams.value.pageNum = 1
  loadData()
}

const handleReset = () => {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    keyword: '',
    category: '',
    isFree: null,
    status: null
  }
  loadData()
}

const handlePageChange = (page: number) => {
  queryParams.value.pageNum = page
  loadData()
}

const handlePageSizeChange = (size: number) => {
  queryParams.value.pageSize = size
  queryParams.value.pageNum = 1
  loadData()
}

const viewDetail = (id: number) => {
  window.open(`/codepen/${id}`, '_blank')
}

const handleRecommend = (row: CodePenRecord) => {
  recommendDialog.value.visible = true
  recommendDialog.value.penId = row.id
  recommendDialog.value.form.expireTime = null
}

const confirmRecommend = async () => {
  try {
    recommendDialog.value.loading = true
    await codepenApi.setRecommend({
      id: recommendDialog.value.penId,
      expireTime: recommendDialog.value.form.expireTime
    })

    ElMessage.success('设置推荐成功')
    recommendDialog.value.visible = false
    loadData()
  } catch (error) {
    console.error('设置推荐失败:', error)
  } finally {
    recommendDialog.value.loading = false
  }
}

const handleCancelRecommend = async (row: CodePenRecord) => {
  try {
    await ElMessageBox.confirm('确定取消推荐吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await codepenApi.cancelRecommend(row.id)
    ElMessage.success('取消推荐成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消推荐失败:', error)
    }
  }
}

const handleCommand = async (command: string, row: CodePenRecord) => {
  if (command === 'offline') {
    await handleUpdateStatus(row.id, 2, '下架')
  } else if (command === 'online') {
    await handleUpdateStatus(row.id, 1, '上架')
  } else if (command === 'delete') {
    await handleDelete(row)
  }
}

const handleUpdateStatus = async (id: number, status: number, actionName: string) => {
  try {
    await ElMessageBox.confirm(`确定${actionName}该作品吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await codepenApi.updatePenStatus({ id, status })
    ElMessage.success(`${actionName}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${actionName}失败:`, error)
    }
  }
}

const handleDelete = async (row: CodePenRecord) => {
  try {
    await ElMessageBox.confirm(`确定删除作品"${row.title}"吗？此操作不可恢复。`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'error'
    })

    await codepenApi.deletePen(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const getStatusText = (status?: number) => {
  const statusMap: Record<number, string> = {
    0: '草稿',
    1: '已发布',
    2: '已下架',
    3: '已删除'
  }
  return statusMap[Number(status)] || '未知'
}

const getStatusTone = (status?: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    0: 'info',
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return toneMap[Number(status)] || 'neutral'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.codepen-pens-page {
  min-height: 100%;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-3);
}

.filter-control {
  width: 240px;
}

.filter-select {
  width: 150px;
}

.filter-select.is-compact {
  width: 120px;
}

.stats-cell {
  display: grid;
  gap: var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.dialog-footer {
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}

@media (max-width: 720px) {
  .filter-control,
  .filter-select,
  .filter-select.is-compact {
    width: 100%;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
