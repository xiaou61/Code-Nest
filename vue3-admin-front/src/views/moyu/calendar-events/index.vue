<template>
  <CnPage class="calendar-events-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="日历事件管理"
      description="维护程序员日历中的节日事件、技术纪念日和开源节日。"
      eyebrow="Developer Calendar"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">总事件 {{ statistics?.totalEvents || 0 }}</CnStatusTag>
        <CnStatusTag type="danger">重要 {{ statistics?.majorEvents || 0 }}</CnStatusTag>
        <CnStatusTag type="warning">程序员节日 {{ statistics?.programmerHolidays || 0 }}</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="danger" :icon="Delete" :disabled="selectedEvents.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedEvents.length }})
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增事件</el-button>
      </template>
    </CnPageHeader>

    <div v-if="statistics" class="stats-grid">
      <CnStatCard title="总事件数" :value="statistics.totalEvents || 0" description="日历事件总量" tone="brand" />
      <CnStatCard title="重要事件" :value="statistics.majorEvents || 0" description="被标记为重要的事件" tone="danger" />
      <CnStatCard title="程序员节日" :value="statistics.programmerHolidays || 0" description="程序员专属节日" tone="warning" />
      <CnStatCard title="技术纪念日" :value="statistics.techMemorials || 0" description="技术发展纪念节点" tone="info" />
      <CnStatCard title="开源节日" :value="statistics.openSourceHolidays || 0" description="开源社区相关节日" tone="success" />
    </div>

    <CnSection title="筛选条件" description="按名称、事件类型、重要程度和状态筛选日历事件。" divided>
      <div class="filter-grid">
        <el-input
          v-model="searchForm.eventName"
          placeholder="请输入事件名称"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select v-model="searchForm.eventType" placeholder="事件类型" clearable @change="handleSearch">
          <el-option label="程序员节日" :value="1" />
          <el-option label="技术纪念日" :value="2" />
          <el-option label="开源节日" :value="3" />
        </el-select>

        <el-select v-model="searchForm.isMajor" placeholder="重要程度" clearable @change="handleSearch">
          <el-option label="重要事件" :value="1" />
          <el-option label="普通事件" :value="0" />
        </el-select>

        <el-select v-model="searchForm.status" placeholder="状态" clearable @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>

        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>
      </div>
    </CnSection>

    <CnSection title="事件列表" :description="`共 ${pagination.total} 个事件`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="eventList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        border
        empty-title="暂无事件"
        empty-description="当前筛选条件下没有程序员日历事件。"
        empty-icon="DC"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #eventName="{ row }">
          <div class="event-name">
            <el-icon class="event-icon" :style="getEventToneStyle(row)">
              <component :is="getIconComponent(row.icon)" />
            </el-icon>
            <span class="event-title">{{ row.eventName }}</span>
            <CnStatusTag v-if="row.isMajor" type="danger" size="sm">重要</CnStatusTag>
          </div>
        </template>

        <template #eventType="{ row }">
          <CnStatusTag :type="getEventTypeTone(row.eventType)" size="sm">
            {{ getEventTypeName(row.eventType) }}
          </CnStatusTag>
        </template>

        <template #status="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" :before-close="handleCloseDialog" destroy-on-close>
      <el-form ref="eventFormRef" :model="eventForm" :rules="eventRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="事件名称" prop="eventName">
              <el-input v-model="eventForm.eventName" placeholder="请输入事件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="事件日期" prop="eventDate">
              <el-input v-model="eventForm.eventDate" placeholder="MM-dd格式，如：10-24" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="事件类型" prop="eventType">
              <el-select v-model="eventForm.eventType" placeholder="请选择事件类型">
                <el-option label="程序员节日" :value="1" />
                <el-option label="技术纪念日" :value="2" />
                <el-option label="开源节日" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="重要程度">
              <el-switch v-model="eventForm.isMajor" :active-value="1" :inactive-value="0" active-text="重要事件" inactive-text="普通事件" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="eventForm.icon" placeholder="请输入图标名称，如：code" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="颜色" prop="color">
              <el-color-picker v-model="eventForm.color" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="事件描述" prop="description">
          <el-input v-model="eventForm.description" type="textarea" :rows="3" placeholder="请输入事件描述" />
        </el-form-item>

        <el-form-item label="祝福语">
          <el-input v-model="eventForm.blessingText" type="textarea" :rows="2" placeholder="请输入节日祝福语（可选）" />
        </el-form-item>

        <el-form-item label="相关链接">
          <el-input v-model="eventForm.relatedLinksText" type="textarea" :rows="2" placeholder="每行一个链接（可选）" />
        </el-form-item>

        <el-form-item label="排序值">
          <el-input-number v-model="eventForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { developerCalendarApi } from '@/api/moyu'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatCard, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnPagination, CnTableColumn, CnTone } from '@/design-system'

interface CalendarEvent extends Record<string, unknown> {
  id: number | null
  eventName: string
  eventDate?: string
  eventType?: number | null
  description?: string
  icon?: string
  color?: string
  isMajor?: number
  blessingText?: string
  relatedLinks?: string[]
  relatedLinksText?: string
  sortOrder?: number
  status?: number
}

interface EventStatistics {
  totalEvents?: number
  majorEvents?: number
  programmerHolidays?: number
  techMemorials?: number
  openSourceHolidays?: number
}

interface EventSearchForm {
  eventName: string
  eventType: number | null
  isMajor: number | null
  status: number | null
}

interface EventForm {
  id: number | null
  eventName: string
  eventDate: string
  eventType: number | null
  description: string
  icon: string
  color: string
  isMajor: number
  blessingText: string
  relatedLinksText: string
  sortOrder: number
  status: number
}

type SubmitEventData = Omit<EventForm, 'relatedLinksText'> & { relatedLinks?: string[] }

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '摸鱼工具' }, { label: '日历事件' }]
const defaultEventColor = ''

const loading = ref(false)
const eventList = ref<CalendarEvent[]>([])
const selectedEvents = ref<CalendarEvent[]>([])
const statistics = ref<EventStatistics | null>(null)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const eventFormRef = ref<FormInstance>()

const searchForm = reactive<EventSearchForm>({
  eventName: '',
  eventType: null,
  isMajor: null,
  status: null
})

const pagination = reactive({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const eventForm = reactive<EventForm>({
  id: null,
  eventName: '',
  eventDate: '',
  eventType: null,
  description: '',
  icon: '',
  color: defaultEventColor,
  isMajor: 0,
  blessingText: '',
  relatedLinksText: '',
  sortOrder: 0,
  status: 1
})

const eventRules: FormRules<EventForm> = {
  eventName: [{ required: true, message: '请输入事件名称', trigger: 'blur' }],
  eventDate: [
    { required: true, message: '请输入事件日期', trigger: 'blur' },
    { pattern: /^\d{2}-\d{2}$/, message: '日期格式应为MM-dd', trigger: 'blur' }
  ],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入事件描述', trigger: 'blur' }]
}

const tableColumns: CnTableColumn<CalendarEvent>[] = [
  { type: 'selection', width: 55 },
  { prop: 'eventName', label: '事件名称', minWidth: 220, slot: 'eventName', showOverflowTooltip: true },
  { prop: 'eventDate', label: '日期', width: 110, align: 'center' },
  { prop: 'eventType', label: '事件类型', width: 130, align: 'center', slot: 'eventType' },
  { prop: 'description', label: '描述', minWidth: 220, showOverflowTooltip: true },
  { prop: 'blessingText', label: '祝福语', minWidth: 160, showOverflowTooltip: true },
  { prop: 'status', label: '状态', width: 100, align: 'center', slot: 'status' },
  { label: '操作', width: 180, fixed: 'right', align: 'center', slot: 'actions' }
]

const dialogTitle = computed(() => (isEdit.value ? '编辑事件' : '新增事件'))
const tablePagination = computed<CnPagination>(() => ({
  page: pagination.currentPage,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const getEventTypeName = (type?: number | null) => {
  const typeMap: Record<number, string> = {
    1: '程序员节日',
    2: '技术纪念日',
    3: '开源节日'
  }
  return type ? typeMap[type] || '未知' : '未知'
}

const getEventTypeTone = (type?: number | null): CnTone => {
  const tagMap: Record<number, CnTone> = {
    1: 'warning',
    2: 'info',
    3: 'success'
  }
  return type ? tagMap[type] || 'neutral' : 'neutral'
}

const getIconComponent = (iconName?: string) => {
  const iconMap: Record<string, string> = {
    code: 'EditPen',
    calendar: 'Calendar',
    coffee: 'Mug',
    github: 'Platform'
  }
  return iconName ? iconMap[iconName] || 'Calendar' : 'Calendar'
}

const loadEventList = async () => {
  try {
    loading.value = true
    const data = await developerCalendarApi.getEventList(searchForm)
    eventList.value = data || []
    pagination.total = eventList.value.length
  } catch (error) {
    console.error('加载事件列表失败:', error)
    ElMessage.error('加载事件列表失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    const data = await developerCalendarApi.getEventStatistics()
    statistics.value = data
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const handleSearch = () => {
  pagination.currentPage = 1
  loadEventList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    eventName: '',
    eventType: null,
    isMajor: null,
    status: null
  })
  handleSearch()
}

const handleSelectionChange = (selection: unknown[]) => {
  selectedEvents.value = selection as CalendarEvent[]
}

const handleStatusChange = async (row: CalendarEvent) => {
  try {
    await developerCalendarApi.updateEventStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
    loadStatistics()
  } catch (error) {
    console.error('状态更新失败:', error)
    ElMessage.error('状态更新失败')
    row.status = row.status === 1 ? 0 : 1
  }
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = async (row: CalendarEvent) => {
  try {
    isEdit.value = true
    const eventData = await developerCalendarApi.getEventById(row.id)

    Object.keys(eventForm).forEach((key) => {
      const formKey = key as keyof EventForm
      if (formKey === 'relatedLinksText') {
        eventForm[formKey] = eventData.relatedLinks ? eventData.relatedLinks.join('\n') : ''
      } else {
        eventForm[formKey] = eventData[formKey] ?? getDefaultFormValue(formKey)
      }
    })

    dialogVisible.value = true
  } catch (error) {
    console.error('加载事件详情失败:', error)
    ElMessage.error('加载事件详情失败')
  }
}

const handleDelete = async (row: CalendarEvent) => {
  try {
    await ElMessageBox.confirm(`确定要删除事件 "${row.eventName}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await developerCalendarApi.deleteEvent(row.id)
    ElMessage.success('删除成功')
    loadEventList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleBatchDelete = async () => {
  if (!selectedEvents.value.length) {
    ElMessage.warning('请选择要删除的事件')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedEvents.value.length} 个事件吗？`, '确认批量删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const ids = selectedEvents.value.map((item) => item.id)
    await developerCalendarApi.batchDeleteEvents(ids)
    ElMessage.success('批量删除成功')
    selectedEvents.value = []
    loadEventList()
    loadStatistics()
  } catch (error) {
    if (error === 'cancel') return
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败')
  }
}

const handleSubmit = async () => {
  if (!eventFormRef.value) return

  try {
    await eventFormRef.value.validate()
    submitLoading.value = true

    const formData: SubmitEventData = { ...eventForm }
    if (eventForm.relatedLinksText) {
      formData.relatedLinks = eventForm.relatedLinksText.split('\n').filter((link) => link.trim())
    }
    delete (formData as Partial<EventForm>).relatedLinksText

    if (isEdit.value) {
      await developerCalendarApi.updateEvent(formData.id, formData)
      ElMessage.success('更新成功')
    } else {
      await developerCalendarApi.createEvent(formData)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadEventList()
    loadStatistics()
  } catch (error: unknown) {
    if (!(error instanceof Error) || error.name !== 'ValidationError') {
      console.error('提交失败:', error)
      ElMessage.error('提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const getDefaultFormValue = (key: keyof EventForm): EventForm[keyof EventForm] => {
  if (key === 'color') return defaultEventColor
  if (key === 'isMajor') return 0
  if (key === 'status') return 1
  if (key === 'sortOrder') return 0
  if (key === 'eventType' || key === 'id') return null
  return ''
}

const getEventToneStyle = (row: CalendarEvent) => (row.color ? { '--event-tone': row.color } : {})

const resetForm = () => {
  Object.keys(eventForm).forEach((key) => {
    const formKey = key as keyof EventForm
    eventForm[formKey] = getDefaultFormValue(formKey)
  })
}

const handleCloseDialog = (done: () => void) => {
  resetForm()
  done()
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  loadEventList()
}

const handleCurrentChange = (page: number) => {
  pagination.currentPage = page
  loadEventList()
}

onMounted(() => {
  loadEventList()
  loadStatistics()
})
</script>

<style scoped>
.calendar-events-management-page {
  min-height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(3, minmax(150px, 1fr)) auto;
  gap: var(--cn-space-3);
  align-items: center;
}

.filter-actions,
.table-actions,
.event-name {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--cn-space-2);
}

.event-name {
  flex-wrap: nowrap;
  min-width: 0;
}

.event-icon {
  flex-shrink: 0;
  color: var(--event-tone, var(--cn-color-brand-primary));
}

.event-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-footer {
  text-align: right;
}

@media (max-width: 1180px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
