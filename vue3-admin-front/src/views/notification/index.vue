<template>
  <CnPage class="notification-management-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="通知管理"
      description="统一管理系统公告、消息统计、批量发送和模板配置，支撑运营通知与站内消息审计。"
      eyebrow="Notification Center"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">消息中台</CnStatusTag>
        <CnStatusTag type="neutral">消息 {{ messagePagination.total }} 条</CnStatusTag>
        <CnStatusTag type="warning">未读 {{ statisticsData?.unreadTotal || unreadMessagesInPage }} 条</CnStatusTag>
        <CnStatusTag type="info">模板 {{ templateList.length }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="pageRefreshing" @click="refreshAll">刷新全部</el-button>
        <el-button type="primary" :icon="Plus" @click="showCreateTemplateDialog">创建模板</el-button>
      </template>
    </CnPageHeader>

    <CnSection class="notification-tabs-section" surface="panel" compact>
      <el-tabs v-model="activeTab" class="notification-tabs">
        <el-tab-pane label="消息统计" name="statistics">
          <div class="tab-stack">
            <CnSection title="统计筛选" description="按消息类型和时间范围查看发送规模、未读量和分类分布。" surface="plain" divided>
              <CnFilterForm
                :model-value="statisticsForm"
                :fields="statisticsFilterFields"
                :columns="2"
                :loading="statisticsLoading"
                search-text="查询统计"
                @update:model-value="handleStatisticsFormUpdate"
                @search="handleStatisticsSearch"
                @reset="resetStatisticsForm"
              />
            </CnSection>

            <div v-if="statisticsData || statisticsLoading" class="stats-grid">
              <CnStatCard
                v-for="item in statisticsCards"
                :key="item.title"
                :title="item.title"
                :value="item.value"
                :description="item.description"
                :tone="item.tone"
                :loading="statisticsLoading"
              />
            </div>

            <CnEmptyState
              v-else
              title="暂无统计数据"
              description="调整筛选条件后查询通知统计。"
              icon="NS"
              surface="plain"
            >
              <template #actions>
                <el-button type="primary" @click="handleStatisticsSearch">查询统计</el-button>
              </template>
            </CnEmptyState>
          </div>
        </el-tab-pane>

        <el-tab-pane label="系统公告" name="announcement">
          <CnSection title="公告发布" description="发布面向全站用户的系统公告，内容会进入站内通知流。" surface="plain" divided>
            <el-form
              ref="announcementFormRef"
              class="notification-form"
              :model="announcementForm"
              :rules="announcementRules"
              label-width="120px"
            >
              <el-form-item label="公告标题" prop="title">
                <el-input
                  v-model="announcementForm.title"
                  placeholder="请输入公告标题"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="公告内容" prop="content">
                <el-input
                  v-model="announcementForm.content"
                  type="textarea"
                  :rows="8"
                  placeholder="请输入公告内容"
                />
              </el-form-item>
              <el-form-item label="优先级" prop="priority">
                <el-select v-model="announcementForm.priority" placeholder="请选择优先级">
                  <el-option label="低" value="LOW" />
                  <el-option label="中" value="MEDIUM" />
                  <el-option label="高" value="HIGH" />
                </el-select>
              </el-form-item>
              <el-form-item class="form-actions">
                <el-button type="primary" :loading="announcementLoading" @click="handlePublishAnnouncement">
                  发布公告
                </el-button>
                <el-button @click="resetAnnouncementForm">重置</el-button>
              </el-form-item>
            </el-form>
          </CnSection>
        </el-tab-pane>

        <el-tab-pane label="消息列表" name="messages">
          <div class="tab-stack">
            <CnSection title="消息筛选" description="按标题、状态、类型和时间范围定位站内消息。" surface="plain" divided>
              <CnFilterForm
                :model-value="messageSearchForm"
                :fields="messageFilterFields"
                :columns="4"
                :loading="messageLoading"
                @update:model-value="handleMessageSearchFormUpdate"
                @search="handleMessageSearch"
                @reset="resetMessageSearch"
              />
            </CnSection>

            <CnSection title="消息列表" :description="`共 ${messagePagination.total} 条消息记录`" surface="plain" divided>
              <CnDataTable
                :columns="messageTableColumns"
                :data="messageList"
                :loading="messageLoading"
                :pagination="messageTablePagination"
                row-key="id"
                @page-change="handleMessagePageChange"
                @page-size-change="handleMessagePageSizeChange"
              >
                <template #toolbar>
                  <CnToolbar title="通知记录" description="消息删除会从管理列表移除，请谨慎操作。" align="center">
                    <template #meta>
                      <CnStatusTag type="warning" size="sm">未读 {{ unreadMessagesInPage }} 条</CnStatusTag>
                      <CnStatusTag type="danger" size="sm">高优先级 {{ highPriorityMessagesInPage }} 条</CnStatusTag>
                    </template>

                    <el-button :icon="Refresh" :loading="messageLoading" @click="searchMessages">刷新</el-button>
                  </CnToolbar>
                </template>

                <template #title="{ row }">
                  <div class="table-title-cell">
                    <span>{{ row.title || '-' }}</span>
                    <small v-if="row.content">{{ row.content }}</small>
                  </div>
                </template>

                <template #type="{ row }">
                  <CnStatusTag :type="getTypeTone(row.type)" size="sm">
                    {{ getTypeText(row.type) }}
                  </CnStatusTag>
                </template>

                <template #status="{ row }">
                  <CnStatusTag :type="getStatusTone(row.status)" size="sm">
                    {{ getStatusText(row.status) }}
                  </CnStatusTag>
                </template>

                <template #priority="{ row }">
                  <CnStatusTag :type="getPriorityTone(row.priority)" size="sm">
                    {{ getPriorityText(row.priority) }}
                  </CnStatusTag>
                </template>

                <template #actions="{ row }">
                  <div class="table-actions">
                    <el-button type="danger" link size="small" :icon="Delete" @click="deleteMessageById(row.id)">
                      删除
                    </el-button>
                  </div>
                </template>

                <template #empty>
                  <CnEmptyState
                    title="暂无消息记录"
                    description="当前筛选条件下没有匹配消息，可以重置筛选或刷新列表。"
                    icon="NM"
                    surface="transparent"
                  >
                    <template #actions>
                      <el-button @click="resetMessageSearch">重置筛选</el-button>
                      <el-button type="primary" @click="searchMessages">刷新</el-button>
                    </template>
                  </CnEmptyState>
                </template>
              </CnDataTable>
            </CnSection>
          </div>
        </el-tab-pane>

        <el-tab-pane label="批量发送" name="batchSend">
          <CnSection title="批量发送" description="向指定用户批量发送个人消息或系统通知。" surface="plain" divided>
            <el-form
              ref="batchSendFormRef"
              class="notification-form"
              :model="batchSendForm"
              :rules="batchSendRules"
              label-width="120px"
            >
              <el-form-item label="接收者" prop="receiverIds">
                <el-input
                  v-model="receiverIdsText"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入接收者用户ID，多个ID用逗号分隔，如：1,2,3"
                />
              </el-form-item>
              <el-form-item label="消息标题" prop="title">
                <el-input
                  v-model="batchSendForm.title"
                  placeholder="请输入消息标题"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item label="消息内容" prop="content">
                <el-input
                  v-model="batchSendForm.content"
                  type="textarea"
                  :rows="6"
                  placeholder="请输入消息内容"
                />
              </el-form-item>
              <el-form-item label="消息类型" prop="type">
                <el-select v-model="batchSendForm.type" placeholder="请选择消息类型">
                  <el-option label="个人消息" value="PERSONAL" />
                  <el-option label="系统通知" value="SYSTEM" />
                </el-select>
              </el-form-item>
              <el-form-item class="form-actions">
                <el-button type="primary" :loading="batchSendLoading" @click="sendBatchMessage">
                  批量发送
                </el-button>
                <el-button @click="resetBatchSendForm">重置</el-button>
              </el-form-item>
            </el-form>
          </CnSection>
        </el-tab-pane>

        <el-tab-pane label="模板管理" name="templates">
          <CnSection title="模板列表" :description="`共 ${templateList.length} 个消息模板`" surface="plain" divided>
            <template #actions>
              <el-button type="primary" :icon="Plus" @click="showCreateTemplateDialog">创建模板</el-button>
            </template>

            <CnDataTable
              :columns="templateTableColumns"
              :data="templateList"
              :loading="templateLoading"
              row-key="id"
            >
              <template #toolbar>
                <CnToolbar title="模板数据" description="用于复用消息标题和内容结构。" align="center">
                  <template #meta>
                    <CnStatusTag type="neutral" size="sm">模板 {{ templateList.length }} 个</CnStatusTag>
                  </template>

                  <el-button :icon="Refresh" :loading="templateLoading" @click="getTemplateList">刷新</el-button>
                </CnToolbar>
              </template>

              <template #type="{ row }">
                <CnStatusTag :type="getTypeTone(row.type)" size="sm">
                  {{ getTypeText(row.type) }}
                </CnStatusTag>
              </template>

              <template #content="{ row }">
                <span class="template-content-cell">{{ row.content || '-' }}</span>
              </template>

              <template #actions="{ row }">
                <div class="table-actions">
                  <el-button type="primary" link size="small" :icon="Edit" @click="editTemplate(row)">编辑</el-button>
                  <el-button type="danger" link size="small" :icon="Delete" @click="deleteTemplateById(row.id)">删除</el-button>
                </div>
              </template>

              <template #empty>
                <CnEmptyState
                  title="暂无消息模板"
                  description="创建模板后可复用标题和内容结构，减少重复录入。"
                  icon="TP"
                  surface="transparent"
                >
                  <template #actions>
                    <el-button type="primary" @click="showCreateTemplateDialog">创建模板</el-button>
                  </template>
                </CnEmptyState>
              </template>
            </CnDataTable>
          </CnSection>
        </el-tab-pane>
      </el-tabs>
    </CnSection>

    <el-dialog
      v-model="templateDialogVisible"
      :title="templateDialogTitle"
      width="600px"
    >
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="120px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="消息类型" prop="type">
          <el-select v-model="templateForm.type" placeholder="请选择消息类型" class="full-width-control">
            <el-option label="个人消息" value="PERSONAL" />
            <el-option label="系统公告" value="ANNOUNCEMENT" />
            <el-option label="社区互动" value="COMMUNITY_INTERACTION" />
            <el-option label="系统通知" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题模板" prop="title">
          <el-input v-model="templateForm.title" placeholder="请输入标题模板" />
        </el-form-item>
        <el-form-item label="内容模板" prop="content">
          <el-input
            v-model="templateForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入内容模板"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="templateDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="templateSaveLoading" @click="saveTemplate">
            {{ isEditMode ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import {
  batchSendMessage,
  createTemplate,
  deleteMessage,
  deleteTemplate,
  getAllMessages,
  getStatistics,
  getTemplates,
  publishAnnouncement as apiPublishAnnouncement,
  updateTemplate
} from '@/api/notification'
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

type NotificationType = 'PERSONAL' | 'ANNOUNCEMENT' | 'COMMUNITY_INTERACTION' | 'SYSTEM'
type MessageStatus = 'UNREAD' | 'READ' | 'DELETED'
type MessagePriority = 'LOW' | 'MEDIUM' | 'HIGH'

interface StatisticsData {
  todayTotal?: number
  monthTotal?: number
  unreadTotal?: number
  announcementCount?: number
  personalCount?: number
  communityCount?: number
  systemCount?: number
}

interface MessageRecord {
  id: number
  title?: string
  content?: string
  type?: NotificationType
  status?: MessageStatus
  priority?: MessagePriority
  receiverName?: string
  createTime?: string
  [key: string]: unknown
}

interface TemplateRecord {
  id: number
  name: string
  type: NotificationType
  title: string
  content: string
  createTime?: string
  [key: string]: unknown
}

interface AnnouncementForm {
  title: string
  content: string
  priority: MessagePriority
}

interface BatchSendForm {
  receiverIds: number[]
  title: string
  content: string
  type: Extract<NotificationType, 'PERSONAL' | 'SYSTEM'>
}

interface TemplateForm {
  id: number | null
  name: string
  type: NotificationType
  title: string
  content: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '系统管理' }, { label: '通知管理' }]

const activeTab = ref('statistics')
const pageRefreshing = ref(false)

const typeOptions = [
  { label: '个人消息', value: 'PERSONAL' },
  { label: '系统公告', value: 'ANNOUNCEMENT' },
  { label: '社区互动', value: 'COMMUNITY_INTERACTION' },
  { label: '系统通知', value: 'SYSTEM' }
]

const statusOptions = [
  { label: '未读', value: 'UNREAD' },
  { label: '已读', value: 'READ' },
  { label: '已删除', value: 'DELETED' }
]

const statisticsLoading = ref(false)
const statisticsForm = reactive<{
  type: NotificationType | ''
  dateRange: string[]
}>({
  type: '',
  dateRange: []
})
const statisticsData = ref<StatisticsData | null>(null)

const announcementForm = reactive<AnnouncementForm>({
  title: '',
  content: '',
  priority: 'LOW'
})
const announcementFormRef = ref<FormInstance>()
const announcementLoading = ref(false)
const announcementRules: FormRules<AnnouncementForm> = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200字符', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const messageSearchForm = reactive<{
  title: string
  status: MessageStatus | ''
  type: NotificationType | ''
  dateRange: string[]
}>({
  title: '',
  status: '',
  type: '',
  dateRange: []
})
const messageList = ref<MessageRecord[]>([])
const messageLoading = ref(false)
const messagePagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const batchSendForm = reactive<BatchSendForm>({
  receiverIds: [],
  title: '',
  content: '',
  type: 'PERSONAL'
})
const receiverIdsText = ref('')
const batchSendFormRef = ref<FormInstance>()
const batchSendLoading = ref(false)
const batchSendRules: FormRules<BatchSendForm> = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200字符', trigger: 'blur' }
  ],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }]
}

const templateList = ref<TemplateRecord[]>([])
const templateLoading = ref(false)
const templateDialogVisible = ref(false)
const templateDialogTitle = ref('')
const isEditMode = ref(false)
const templateSaveLoading = ref(false)
const templateFormRef = ref<FormInstance>()
const templateForm = reactive<TemplateForm>({
  id: null,
  name: '',
  type: 'PERSONAL',
  title: '',
  content: ''
})
const templateRules: FormRules<TemplateForm> = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容模板', trigger: 'blur' }]
}

const statisticsFilterFields: CnFilterField[] = [
  {
    prop: 'type',
    label: '消息类型',
    type: 'select',
    placeholder: '全部类型',
    options: typeOptions
  },
  { prop: 'dateRange', label: '时间范围', type: 'daterange' }
]

const messageFilterFields: CnFilterField[] = [
  { prop: 'title', label: '标题', type: 'input', placeholder: '输入标题关键词' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '全部状态',
    options: statusOptions
  },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    placeholder: '全部类型',
    options: typeOptions
  },
  { prop: 'dateRange', label: '时间范围', type: 'daterange' }
]

const messageTableColumns: CnTableColumn<MessageRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'title', label: '标题', minWidth: 220, slot: 'title', showOverflowTooltip: true },
  { prop: 'type', label: '类型', width: 130, slot: 'type' },
  { prop: 'status', label: '状态', width: 100, slot: 'status' },
  { prop: 'priority', label: '优先级', width: 100, slot: 'priority' },
  { prop: 'receiverName', label: '接收者', minWidth: 120, showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', width: 180 },
  { label: '操作', width: 90, fixed: 'right', slot: 'actions' }
]

const templateTableColumns: CnTableColumn<TemplateRecord>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '模板名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'type', label: '类型', width: 130, slot: 'type' },
  { prop: 'title', label: '标题模板', minWidth: 180, showOverflowTooltip: true },
  { prop: 'content', label: '内容模板', minWidth: 240, slot: 'content', showOverflowTooltip: true },
  { prop: 'createTime', label: '创建时间', width: 180 },
  { label: '操作', width: 140, fixed: 'right', slot: 'actions' }
]

const messageTablePagination = computed<CnPagination>(() => ({
  page: messagePagination.pageNum,
  pageSize: messagePagination.pageSize,
  total: messagePagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const totalMessages = computed(() => {
  if (!statisticsData.value) return 0
  const { announcementCount, personalCount, communityCount, systemCount } = statisticsData.value
  return (announcementCount || 0) + (personalCount || 0) + (communityCount || 0) + (systemCount || 0)
})

const statisticsCards = computed<Array<{ title: string; value: number; description: string; tone: CnTone }>>(() => {
  const data = statisticsData.value || {}
  return [
    { title: '今日发送', value: data.todayTotal || 0, description: '今日新增通知消息', tone: 'brand' },
    { title: '本月发送', value: data.monthTotal || 0, description: '当前自然月发送量', tone: 'success' },
    { title: '未读消息', value: data.unreadTotal || 0, description: '仍未被用户读取', tone: 'warning' },
    { title: '总消息数', value: totalMessages.value, description: '按分类统计汇总', tone: 'neutral' },
    { title: '系统公告', value: data.announcementCount || 0, description: '全站公告类消息', tone: 'success' },
    { title: '个人消息', value: data.personalCount || 0, description: '指定用户个人消息', tone: 'brand' },
    { title: '社区互动', value: data.communityCount || 0, description: '评论、点赞等互动通知', tone: 'warning' },
    { title: '系统通知', value: data.systemCount || 0, description: '系统事件类通知', tone: 'info' }
  ]
})

const unreadMessagesInPage = computed(() => messageList.value.filter((item) => item.status === 'UNREAD').length)
const highPriorityMessagesInPage = computed(() => messageList.value.filter((item) => item.priority === 'HIGH').length)

watch(receiverIdsText, (value) => {
  batchSendForm.receiverIds = value
    ? value
        .split(',')
        .map((id) => Number.parseInt(id.trim(), 10))
        .filter((id) => !Number.isNaN(id))
    : []
})

onMounted(() => {
  getStatisticsData()
  searchMessages()
  getTemplateList()
})

const unwrapApiData = (response: unknown): any => {
  if (response && typeof response === 'object' && 'data' in response && 'code' in response) {
    return (response as { data?: unknown }).data
  }
  return response
}

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error) return error.message
  return String(error || '未知错误')
}

const getStatisticsData = async (showSuccess = false) => {
  try {
    statisticsLoading.value = true
    const params: Record<string, unknown> = {}

    if (statisticsForm.type) {
      params.type = statisticsForm.type
    }
    if (statisticsForm.dateRange?.length === 2) {
      params.startTime = statisticsForm.dateRange[0]
      params.endTime = statisticsForm.dateRange[1]
    }

    const response = await getStatistics(params)
    statisticsData.value = {
      todayTotal: 0,
      monthTotal: 0,
      unreadTotal: 0,
      announcementCount: 0,
      personalCount: 0,
      communityCount: 0,
      systemCount: 0,
      ...(unwrapApiData(response) || {})
    }

    if (showSuccess) {
      ElMessage.success('统计数据获取成功')
    }
  } catch (error) {
    ElMessage.error(`获取统计数据失败：${getErrorMessage(error)}`)
  } finally {
    statisticsLoading.value = false
  }
}

const handleStatisticsSearch = () => {
  getStatisticsData(true)
}

const resetStatisticsForm = () => {
  Object.assign(statisticsForm, {
    type: '',
    dateRange: []
  })
  getStatisticsData()
}

const handleStatisticsFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(statisticsForm, value)
}

const handlePublishAnnouncement = async () => {
  if (!announcementFormRef.value) return

  const valid = await announcementFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    announcementLoading.value = true
    await apiPublishAnnouncement({ ...announcementForm })
    ElMessage.success('公告发布成功')
    resetAnnouncementForm()
    getStatisticsData()
    searchMessages()
  } catch (error) {
    ElMessage.error(`发布公告失败：${getErrorMessage(error)}`)
  } finally {
    announcementLoading.value = false
  }
}

const resetAnnouncementForm = () => {
  announcementFormRef.value?.resetFields()
  Object.assign(announcementForm, {
    title: '',
    content: '',
    priority: 'LOW'
  })
}

const searchMessages = async () => {
  try {
    messageLoading.value = true
    const params: Record<string, unknown> = {
      pageNum: messagePagination.pageNum,
      pageSize: messagePagination.pageSize
    }

    if (messageSearchForm.title) params.title = messageSearchForm.title
    if (messageSearchForm.status) params.status = messageSearchForm.status
    if (messageSearchForm.type) params.type = messageSearchForm.type
    if (messageSearchForm.dateRange?.length === 2) {
      params.startTime = messageSearchForm.dateRange[0]
      params.endTime = messageSearchForm.dateRange[1]
    }

    const response = await getAllMessages(params)
    const data = unwrapApiData(response) || {}
    const records = data.records || data.list || []

    messageList.value = Array.isArray(records) ? records : []
    messagePagination.total = Number(data.total || messageList.value.length || 0)
  } catch (error) {
    ElMessage.error(`获取消息列表失败：${getErrorMessage(error)}`)
  } finally {
    messageLoading.value = false
  }
}

const handleMessageSearch = () => {
  messagePagination.pageNum = 1
  searchMessages()
}

const resetMessageSearch = () => {
  Object.assign(messageSearchForm, {
    title: '',
    status: '',
    type: '',
    dateRange: []
  })
  handleMessageSearch()
}

const handleMessageSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(messageSearchForm, value)
}

const handleMessagePageChange = (page: number) => {
  messagePagination.pageNum = page
  searchMessages()
}

const handleMessagePageSizeChange = (size: number) => {
  messagePagination.pageSize = size
  messagePagination.pageNum = 1
  searchMessages()
}

const deleteMessageById = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMessage(id)
    ElMessage.success('删除成功')
    searchMessages()
    getStatisticsData()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(`删除失败：${getErrorMessage(error)}`)
  }
}

const sendBatchMessage = async () => {
  if (!batchSendFormRef.value) return

  const valid = await batchSendFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (batchSendForm.receiverIds.length === 0) {
    ElMessage.error('请输入有效的接收者ID')
    return
  }

  try {
    batchSendLoading.value = true
    await batchSendMessage({ ...batchSendForm })
    ElMessage.success('批量发送成功')
    resetBatchSendForm()
    searchMessages()
    getStatisticsData()
  } catch (error) {
    ElMessage.error(`批量发送失败：${getErrorMessage(error)}`)
  } finally {
    batchSendLoading.value = false
  }
}

const resetBatchSendForm = () => {
  batchSendFormRef.value?.resetFields()
  Object.assign(batchSendForm, {
    receiverIds: [],
    title: '',
    content: '',
    type: 'PERSONAL'
  })
  receiverIdsText.value = ''
}

const getTemplateList = async () => {
  try {
    templateLoading.value = true
    const response = await getTemplates()
    const data = unwrapApiData(response)
    const records = Array.isArray(data) ? data : data?.records || data?.list || []
    templateList.value = Array.isArray(records) ? records : []
  } catch (error) {
    ElMessage.error(`获取模板列表失败：${getErrorMessage(error)}`)
  } finally {
    templateLoading.value = false
  }
}

const showCreateTemplateDialog = () => {
  isEditMode.value = false
  templateDialogTitle.value = '创建模板'
  resetTemplateForm()
  templateDialogVisible.value = true
}

const editTemplate = (template: TemplateRecord) => {
  isEditMode.value = true
  templateDialogTitle.value = '编辑模板'
  Object.assign(templateForm, template)
  templateDialogVisible.value = true
}

const saveTemplate = async () => {
  if (!templateFormRef.value) return

  const valid = await templateFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    templateSaveLoading.value = true

    if (isEditMode.value && templateForm.id) {
      await updateTemplate(templateForm.id, { ...templateForm })
      ElMessage.success('模板更新成功')
    } else {
      await createTemplate({ ...templateForm })
      ElMessage.success('模板创建成功')
    }

    templateDialogVisible.value = false
    getTemplateList()
  } catch (error) {
    ElMessage.error(`保存模板失败：${getErrorMessage(error)}`)
  } finally {
    templateSaveLoading.value = false
  }
}

const deleteTemplateById = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个模板吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteTemplate(id)
    ElMessage.success('删除成功')
    getTemplateList()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(`删除失败：${getErrorMessage(error)}`)
  }
}

const resetTemplateForm = () => {
  templateFormRef.value?.resetFields()
  Object.assign(templateForm, {
    id: null,
    name: '',
    type: 'PERSONAL',
    title: '',
    content: ''
  })
}

const refreshAll = async () => {
  pageRefreshing.value = true
  try {
    await Promise.all([getStatisticsData(), searchMessages(), getTemplateList()])
  } finally {
    pageRefreshing.value = false
  }
}

const getTypeText = (type?: NotificationType) => {
  const typeMap: Record<NotificationType, string> = {
    PERSONAL: '个人消息',
    ANNOUNCEMENT: '系统公告',
    COMMUNITY_INTERACTION: '社区互动',
    SYSTEM: '系统通知'
  }
  return type ? typeMap[type] || type : '-'
}

const getTypeTone = (type?: NotificationType): CnTone => {
  const toneMap: Record<NotificationType, CnTone> = {
    PERSONAL: 'brand',
    ANNOUNCEMENT: 'success',
    COMMUNITY_INTERACTION: 'warning',
    SYSTEM: 'info'
  }
  return type ? toneMap[type] || 'neutral' : 'neutral'
}

const getStatusText = (status?: MessageStatus) => {
  const statusMap: Record<MessageStatus, string> = {
    UNREAD: '未读',
    READ: '已读',
    DELETED: '已删除'
  }
  return status ? statusMap[status] || status : '-'
}

const getStatusTone = (status?: MessageStatus): CnTone => {
  const toneMap: Record<MessageStatus, CnTone> = {
    UNREAD: 'warning',
    READ: 'success',
    DELETED: 'danger'
  }
  return status ? toneMap[status] || 'neutral' : 'neutral'
}

const getPriorityText = (priority?: MessagePriority) => {
  const priorityMap: Record<MessagePriority, string> = {
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  }
  return priority ? priorityMap[priority] || priority : '-'
}

const getPriorityTone = (priority?: MessagePriority): CnTone => {
  const toneMap: Record<MessagePriority, CnTone> = {
    LOW: 'info',
    MEDIUM: 'warning',
    HIGH: 'danger'
  }
  return priority ? toneMap[priority] || 'neutral' : 'neutral'
}
</script>

<style scoped>
.notification-management-page {
  min-height: 100%;
}

.notification-tabs-section {
  overflow: hidden;
}

.notification-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.notification-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 var(--cn-space-5);
  overflow-x: auto;
}

.notification-tabs :deep(.el-tabs__content) {
  padding: var(--cn-space-5);
}

.tab-stack {
  display: grid;
  gap: var(--cn-space-5);
  min-width: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.notification-form {
  max-width: 860px;
}

.notification-form :deep(.el-select),
.notification-form :deep(.el-input),
.notification-form :deep(.el-textarea) {
  width: 100%;
}

.form-actions :deep(.el-form-item__content),
.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.table-actions .el-button {
  margin-left: 0;
}

.dialog-footer {
  justify-content: flex-end;
}

.table-title-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.table-title-cell span {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-title-cell small {
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-content-cell {
  display: -webkit-box;
  overflow: hidden;
  color: var(--cn-color-text-secondary);
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

@media (max-width: 1080px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .notification-tabs :deep(.el-tabs__nav-wrap) {
    padding: 0 var(--cn-space-3);
  }

  .notification-tabs :deep(.el-tabs__content) {
    padding: var(--cn-space-4);
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .notification-form :deep(.el-form-item) {
    display: grid;
  }

  .notification-form :deep(.el-form-item__label) {
    width: auto !important;
    justify-content: flex-start;
  }

  .notification-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
