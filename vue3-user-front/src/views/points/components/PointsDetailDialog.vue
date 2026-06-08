<template>
  <el-dialog
    class="points-detail-dialog"
    :model-value="modelValue"
    title="积分明细"
    width="760px"
    destroy-on-close
    @update:model-value="handleVisibleChange"
  >
    <div class="points-detail-dialog__body">
      <CnFilterForm
        v-model="queryForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        search-text="筛选"
        reset-text="重置"
        @search="handleSearch"
        @reset="resetQuery"
      />

      <CnDataTable
        :columns="columns"
        :data="detailList"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        empty-title="暂无积分明细"
        empty-description="当前筛选条件下没有积分变动记录。"
        empty-icon="PT"
        @page-change="changePage"
        @page-size-change="changePageSize"
      >
        <template #pointsType="{ row }">
          <CnStatusTag type="brand" size="sm">{{ getPointsTypeText(row.pointsType) }}</CnStatusTag>
        </template>

        <template #time="{ row }">
          <span class="muted-text">{{ formatDateTime(row.createTime) }}</span>
        </template>

        <template #continuousDays="{ row }">
          <CnStatusTag v-if="row.continuousDays > 0" type="success" size="sm" subtle>
            连续 {{ row.continuousDays }} 天
          </CnStatusTag>
          <span v-else class="muted-text">-</span>
        </template>

        <template #pointsChange="{ row }">
          <CnStatusTag
            :type="(row.pointsChange || 0) >= 0 ? 'success' : 'danger'"
            size="sm"
            :dot="false"
          >
            {{ formatPointsChange(row.pointsChange) }}
          </CnStatusTag>
        </template>
      </CnDataTable>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  CnDataTable,
  CnFilterForm,
  CnStatusTag,
  type CnFilterField,
  type CnPagination,
  type CnTableColumn
} from '@/design-system'
import pointsApi from '@/api/points'

interface PointsDetail {
  id: number | string
  pointsType?: string
  description?: string
  createTime?: string
  continuousDays?: number
  pointsChange?: number
  balanceAfter?: number
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const detailList = ref<PointsDetail[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const queryForm = ref<Record<string, unknown>>({
  pointsType: '',
  startTime: '',
  endTime: ''
})

const filterFields: CnFilterField[] = [
  {
    prop: 'pointsType',
    label: '积分类型',
    type: 'select',
    placeholder: '全部类型',
    options: [
      { label: '每日打卡', value: 'CHECK_IN' },
      { label: '管理员发放', value: 'ADMIN_GRANT' }
    ]
  },
  {
    prop: 'startTime',
    label: '开始日期',
    type: 'date',
    placeholder: '选择开始日期'
  },
  {
    prop: 'endTime',
    label: '结束日期',
    type: 'date',
    placeholder: '选择结束日期'
  }
]

const columns: CnTableColumn<PointsDetail>[] = [
  { prop: 'pointsType', label: '类型', minWidth: 120, slot: 'pointsType' },
  { prop: 'description', label: '说明', minWidth: 180, showOverflowTooltip: true },
  { prop: 'createTime', label: '时间', minWidth: 160, slot: 'time' },
  { prop: 'continuousDays', label: '连续天数', minWidth: 120, slot: 'continuousDays' },
  { prop: 'pointsChange', label: '变动', width: 96, align: 'right', slot: 'pointsChange' },
  { prop: 'balanceAfter', label: '余额', width: 96, align: 'right' }
]

const pagination = computed<CnPagination>(() => ({
  page: currentPage.value,
  pageSize: pageSize.value,
  total: total.value,
  pageSizes: [10, 20, 50],
  layout: 'total, sizes, prev, pager, next'
}))

watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue) {
      loadDetailList()
    }
  }
)

const loadDetailList = async () => {
  loading.value = true

  try {
    const response = (await pointsApi.getPointsDetailList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...queryForm.value
    })) as { records?: PointsDetail[]; total?: number }

    detailList.value = response.records || []
    total.value = response.total || 0
  } catch (error) {
    console.error('加载积分明细失败:', error)
    detailList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadDetailList()
}

const resetQuery = () => {
  currentPage.value = 1
  queryForm.value = {
    pointsType: '',
    startTime: '',
    endTime: ''
  }
  loadDetailList()
}

const changePage = (page: number) => {
  currentPage.value = page
  loadDetailList()
}

const changePageSize = (size: number) => {
  currentPage.value = 1
  pageSize.value = size
  loadDetailList()
}

const handleVisibleChange = (visible: boolean) => {
  if (!visible) {
    closeDialog()
    return
  }

  emit('update:modelValue', true)
}

const closeDialog = () => {
  emit('update:modelValue', false)
  currentPage.value = 1
  queryForm.value = {
    pointsType: '',
    startTime: '',
    endTime: ''
  }
}

const getPointsTypeText = (type?: string) => {
  const typeMap: Record<string, string> = {
    CHECK_IN: '每日打卡',
    ADMIN_GRANT: '管理员发放'
  }
  return type ? typeMap[type] || type : '积分变动'
}

const formatPointsChange = (value?: number) => {
  const safeValue = value || 0
  return `${safeValue > 0 ? '+' : ''}${safeValue}`
}

const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style lang="scss" scoped>
.points-detail-dialog__body {
  display: grid;
  gap: var(--cn-space-4);
  min-width: 0;
}

.muted-text {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

:deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}
</style>
