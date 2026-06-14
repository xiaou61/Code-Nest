<template>
  <CnPage class="chat-messages-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="消息管理"
      description="管理聊天室文本、图片和系统消息，支持按用户、关键词和发送时间筛选。"
      eyebrow="Chat Messages"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">聊天室管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 条消息</CnStatusTag>
        <CnStatusTag type="success">文本 {{ textCountInPage }} 条</CnStatusTag>
        <CnStatusTag v-if="selectedIds.length" type="warning">已选择 {{ selectedIds.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button type="warning" :icon="Bell" @click="showAnnouncementDialog">发送公告</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="chat-stat-grid">
      <CnStatCard title="消息总量" :value="pagination.total" description="当前筛选条件下的聊天室消息数量" tone="brand" />
      <CnStatCard title="文本消息" :value="textCountInPage" description="当前页文本消息数量" tone="success" />
      <CnStatCard title="图片消息" :value="imageCountInPage" description="当前页图片消息数量" tone="info" />
      <CnStatCard title="系统消息" :value="systemCountInPage" description="当前页系统公告消息数量" tone="warning" />
    </div>

    <CnSection title="筛选条件" description="按用户 ID、消息内容关键词和发送时间筛选聊天室消息。" divided>
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

    <CnSection title="消息列表" :description="`共 ${pagination.total} 条消息`" divided>
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
          <CnToolbar title="消息数据" description="批量删除会影响聊天室历史记录，请谨慎操作。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag v-if="selectedIds.length" type="warning" size="sm">已选择 {{ selectedIds.length }} 条</CnStatusTag>
            </template>

            <el-button type="danger" :icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              批量删除
            </el-button>
            <el-button type="warning" :icon="Bell" @click="showAnnouncementDialog">发送公告</el-button>
            <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
          </CnToolbar>
        </template>

        <template #sender="{ row }">
          <div class="sender-cell">
            <strong>{{ row.userNickname || '-' }}</strong>
            <span>用户 {{ row.userId || '-' }}</span>
          </div>
        </template>

        <template #messageType="{ row }">
          <CnStatusTag :type="getMessageTypeTone(row.messageType)" size="sm">
            {{ getMessageTypeText(row.messageType) }}
          </CnStatusTag>
        </template>

        <template #content="{ row }">
          <div class="message-content">
            <p v-if="row.messageType === 1" class="text-message">{{ row.content || '-' }}</p>
            <el-image
              v-else-if="row.messageType === 2 && row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              class="preview-image"
              fit="cover"
            />
            <p v-else-if="row.messageType === 3" class="system-message">{{ row.content || '-' }}</p>
            <p v-else class="text-message">{{ row.content || '-' }}</p>
          </div>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无消息"
            description="当前筛选条件下没有聊天室消息，可以重置筛选后再查看。"
            icon="CH"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleRefresh">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="announcementVisible" title="发送系统公告" width="520px">
      <el-form :model="announcementForm" label-width="88px">
        <el-form-item label="公告内容">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="5"
            maxlength="200"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementVisible = false">取消</el-button>
        <el-button type="primary" :loading="sendingAnnouncement" @click="handleSendAnnouncement">发送</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Delete, Refresh } from '@element-plus/icons-vue'
import { batchDeleteMessages, deleteMessage, getAdminMessageList, sendAnnouncement } from '@/api/chat'
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

interface ChatMessageRecord {
  id: number
  userId?: number | string
  userNickname?: string
  messageType: number
  content?: string
  imageUrl?: string
  ipAddress?: string
  deviceInfo?: string
  createTime?: string
  [key: string]: unknown
}

interface FilterForm {
  userId: string
  keyword: string
  timeRange: string[] | null
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '聊天室管理' }, { label: '消息管理' }]

const loading = ref(false)
const tableData = ref<ChatMessageRecord[]>([])
const selectedIds = ref<number[]>([])
const announcementVisible = ref(false)
const sendingAnnouncement = ref(false)

const filterForm = reactive<FilterForm>({
  userId: '',
  keyword: '',
  timeRange: null
})

const announcementForm = reactive({
  content: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const filterFields: CnFilterField[] = [
  { prop: 'userId', label: '用户ID', type: 'input', placeholder: '请输入用户ID' },
  { prop: 'keyword', label: '内容关键词', type: 'input', placeholder: '搜索消息内容' },
  { prop: 'timeRange', label: '发送时间', type: 'datetimerange', placeholder: '开始时间' }
]

const tableColumns: CnTableColumn<ChatMessageRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: '消息ID', width: 100 },
  { prop: 'userNickname', label: '发送者', width: 140, slot: 'sender', showOverflowTooltip: true },
  { prop: 'messageType', label: '消息类型', width: 100, align: 'center', slot: 'messageType' },
  { prop: 'content', label: '消息内容', minWidth: 300, slot: 'content', showOverflowTooltip: true },
  { prop: 'ipAddress', label: 'IP地址', width: 140, showOverflowTooltip: true },
  { prop: 'deviceInfo', label: '设备信息', width: 180, showOverflowTooltip: true },
  { prop: 'createTime', label: '发送时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 100, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const textCountInPage = computed(() => tableData.value.filter((item) => item.messageType === 1).length)
const imageCountInPage = computed(() => tableData.value.filter((item) => item.messageType === 2).length)
const systemCountInPage = computed(() => tableData.value.filter((item) => item.messageType === 3).length)

onMounted(() => {
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const result = await getAdminMessageList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      userId: filterForm.userId || null,
      keyword: filterForm.keyword || null,
      startTime: filterForm.timeRange?.[0] || null,
      endTime: filterForm.timeRange?.[1] || null
    })

    tableData.value = result?.records || []
    pagination.total = result?.total || 0
  } catch (error) {
    console.error('加载聊天室消息失败:', error)
    ElMessage.error('加载消息列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleRefresh = () => {
  Object.assign(filterForm, {
    userId: '',
    keyword: '',
    timeRange: null
  })
  pagination.pageNum = 1
  selectedIds.value = []
  loadData()
}

const handleFilterUpdate = (value: Record<string, unknown>) => {
  Object.assign(filterForm, value)
}

const handleSelectionChange = (selection: ChatMessageRecord[]) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleDelete = async (row: ChatMessageRecord) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMessage(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除聊天室消息失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return

  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条消息吗？`, '批量删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await batchDeleteMessages(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除聊天室消息失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

const showAnnouncementDialog = () => {
  announcementForm.content = ''
  announcementVisible.value = true
}

const handleSendAnnouncement = async () => {
  if (!announcementForm.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }

  sendingAnnouncement.value = true
  try {
    await sendAnnouncement({ content: announcementForm.content })
    ElMessage.success('公告发送成功')
    announcementVisible.value = false
  } catch (error) {
    console.error('发送聊天室公告失败:', error)
    ElMessage.error('发送公告失败')
  } finally {
    sendingAnnouncement.value = false
  }
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  loadData()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadData()
}

const getMessageTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '文本',
    2: '图片',
    3: '系统'
  }
  return map[type] || '未知'
}

const getMessageTypeTone = (type: number): CnTone => {
  const map: Record<number, CnTone> = {
    1: 'success',
    2: 'info',
    3: 'warning'
  }
  return map[type] || 'neutral'
}
</script>

<style scoped>
.chat-messages-page {
  min-height: 100%;
}

.chat-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.sender-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.sender-cell strong {
  overflow: hidden;
  color: var(--cn-color-text-primary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sender-cell span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.message-content {
  min-width: 0;
}

.text-message,
.system-message {
  margin: 0;
  overflow: hidden;
  line-height: 1.55;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.text-message {
  color: var(--cn-color-text-primary);
}

.system-message {
  color: var(--cn-color-warning);
  font-weight: 650;
}

.preview-image {
  width: 64px;
  height: 64px;
  border-radius: var(--cn-radius-control);
  cursor: pointer;
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
  .chat-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .chat-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
