<template>
  <CnPage class="community-users-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="社区用户"
      description="管理社区用户互动状态，支持封禁、解封以及查看发帖和评论记录。"
      eyebrow="Community Users"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">社区管理</CnStatusTag>
        <CnStatusTag type="neutral">共 {{ pagination.total }} 个用户</CnStatusTag>
        <CnStatusTag type="success">正常 {{ normalCountInPage }} 个</CnStatusTag>
        <CnStatusTag type="danger">封禁 {{ bannedCountInPage }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="fetchUsers">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="community-stat-grid">
      <CnStatCard title="用户总量" :value="pagination.total" description="当前筛选条件下的社区用户数量" tone="brand" />
      <CnStatCard title="正常用户" :value="normalCountInPage" description="当前页未被封禁的用户" tone="success" />
      <CnStatCard title="封禁用户" :value="bannedCountInPage" description="当前页处于封禁状态的用户" tone="danger" />
      <CnStatCard title="互动总量" :value="activityCountInPage" description="当前页发帖、评论、点赞、收藏累计" tone="info" />
    </div>

    <CnSection title="筛选条件" description="按用户名、封禁状态和注册时间定位社区用户。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="4"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="用户列表" :description="`共 ${pagination.total} 个社区用户`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="userList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="userId"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="用户数据" description="封禁会限制用户社区互动行为，请结合发帖与评论记录判断。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag type="info" size="sm">互动 {{ activityCountInPage }} 次</CnStatusTag>
            </template>
          </CnToolbar>
        </template>

        <template #userName="{ row }">
          <div class="user-cell">
            <strong>{{ row.userName || '-' }}</strong>
            <span>ID {{ row.userId }}</span>
          </div>
        </template>

        <template #activity="{ row }">
          <div class="metric-stack">
            <span>发帖 {{ row.postCount || 0 }}</span>
            <span>评论 {{ row.commentCount || 0 }}</span>
            <span>点赞 {{ row.likeCount || 0 }}</span>
            <span>收藏 {{ row.collectCount || 0 }}</span>
          </div>
        </template>

        <template #isBanned="{ row }">
          <CnStatusTag :type="row.isBanned === 0 ? 'success' : 'danger'" size="sm">
            {{ row.isBanned === 0 ? '正常' : '已封禁' }}
          </CnStatusTag>
        </template>

        <template #banReason="{ row }">
          <span class="muted-text">{{ row.banReason || '-' }}</span>
        </template>

        <template #banExpireTime="{ row }">
          <span class="muted-text">{{ row.banExpireTime || '-' }}</span>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" :icon="Document" @click="handleViewPosts(row)">发帖记录</el-button>
            <el-button type="info" link size="small" :icon="ChatDotRound" @click="handleViewComments(row)">评论记录</el-button>
            <el-button v-if="row.isBanned === 0" type="danger" link size="small" :icon="Lock" @click="handleBan(row)">封禁</el-button>
            <el-button v-if="row.isBanned === 1" type="success" link size="small" :icon="Unlock" @click="handleUnban(row)">解封</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无社区用户"
            description="当前筛选条件下没有社区用户，可以重置筛选后再查看。"
            icon="CU"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog title="封禁用户" v-model="banDialogVisible" width="500px">
      <el-form ref="banFormRef" :model="banForm" :rules="banRules" label-width="100px">
        <el-form-item label="用户" prop="userName">
          <el-input v-model="banForm.userName" disabled />
        </el-form-item>
        <el-form-item label="封禁原因" prop="reason">
          <el-input
            v-model="banForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入封禁原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="封禁时长" prop="duration">
          <div class="duration-field">
            <el-input-number v-model="banForm.duration" :min="1" :max="8760" />
            <span>小时</span>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="banDialogVisible = false">取消</el-button>
          <el-button type="danger" :loading="submitLoading" @click="handleBanSubmit">确定封禁</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="recordDialogTitle" v-model="recordDialogVisible" width="1000px">
      <div v-if="recordType === 'posts'" class="record-content">
        <el-table :data="userPosts" class="full-width-table" max-height="400">
          <el-table-column prop="id" label="帖子ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column prop="likeCount" label="点赞" width="80" />
          <el-table-column prop="commentCount" label="评论" width="80" />
          <el-table-column prop="createTime" label="发布时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <CnStatusTag :type="getPostStatusTone(row.status)" size="sm">
                {{ getPostStatusText(row.status) }}
              </CnStatusTag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="recordType === 'comments'" class="record-content">
        <el-table :data="userComments" class="full-width-table" max-height="400">
          <el-table-column prop="id" label="评论ID" width="80" />
          <el-table-column prop="postId" label="帖子ID" width="80" />
          <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip />
          <el-table-column prop="likeCount" label="点赞" width="80" />
          <el-table-column prop="createTime" label="评论时间" width="180" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <CnStatusTag :type="row.status === 1 ? 'success' : 'danger'" size="sm">
                {{ row.status === 1 ? '正常' : '删除' }}
              </CnStatusTag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Document, Lock, Refresh, Unlock } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
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

interface CommunityUserRecord {
  id?: number
  userId: number
  userName: string
  postCount?: number
  commentCount?: number
  likeCount?: number
  collectCount?: number
  isBanned: number
  banReason?: string
  banExpireTime?: string
  createTime?: string
  [key: string]: unknown
}

interface UserPostRecord {
  id: number
  title: string
  category?: string
  viewCount?: number
  likeCount?: number
  commentCount?: number
  createTime?: string
  status: number
}

interface UserCommentRecord {
  id: number
  postId?: number
  content: string
  likeCount?: number
  createTime?: string
  status: number
}

interface SearchForm {
  userName: string
  isBanned: number | null
  timeRange: string[] | null
}

interface BanForm {
  userName: string
  reason: string
  duration: number
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '社区管理' }, { label: '用户管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const banDialogVisible = ref(false)
const recordDialogVisible = ref(false)
const userList = ref<CommunityUserRecord[]>([])
const userPosts = ref<UserPostRecord[]>([])
const userComments = ref<UserCommentRecord[]>([])
const currentUser = ref<CommunityUserRecord | null>(null)
const recordType = ref<'posts' | 'comments' | ''>('')
const banFormRef = ref<FormInstance>()

const searchForm = reactive<SearchForm>({
  userName: '',
  isBanned: null,
  timeRange: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const banForm = reactive<BanForm>({
  userName: '',
  reason: '',
  duration: 24
})

const banRules: FormRules<BanForm> = {
  reason: [
    { required: true, message: '请输入封禁原因', trigger: 'blur' },
    { max: 200, message: '封禁原因长度不能超过200字符', trigger: 'blur' }
  ],
  duration: [
    { required: true, message: '请输入封禁时长', trigger: 'blur' },
    { type: 'number', min: 1, max: 8760, message: '封禁时长必须在1-8760小时之间', trigger: 'blur' }
  ]
}

const filterFields: CnFilterField[] = [
  { prop: 'userName', label: '用户名', type: 'input', placeholder: '请输入用户名' },
  {
    prop: 'isBanned',
    label: '封禁状态',
    type: 'select',
    placeholder: '封禁状态',
    options: [
      { label: '正常', value: 0 },
      { label: '已封禁', value: 1 }
    ]
  },
  { prop: 'timeRange', label: '注册时间', type: 'daterange', placeholder: '开始日期' }
]

const tableColumns: CnTableColumn<CommunityUserRecord>[] = [
  { prop: 'userId', label: '用户ID', width: 100 },
  { prop: 'userName', label: '用户名', minWidth: 150, slot: 'userName', showOverflowTooltip: true },
  { label: '活动统计', width: 240, align: 'center', slot: 'activity' },
  { prop: 'isBanned', label: '封禁状态', width: 110, slot: 'isBanned' },
  { prop: 'banReason', label: '封禁原因', minWidth: 180, slot: 'banReason', showOverflowTooltip: true },
  { prop: 'banExpireTime', label: '封禁到期时间', width: 180, slot: 'banExpireTime', showOverflowTooltip: true },
  { prop: 'createTime', label: '注册时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 250, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const recordDialogTitle = computed(() => {
  if (!currentUser.value) return ''
  return recordType.value === 'posts' ? `${currentUser.value.userName} 的发帖记录` : `${currentUser.value.userName} 的评论记录`
})

const normalCountInPage = computed(() => userList.value.filter((item) => item.isBanned === 0).length)
const bannedCountInPage = computed(() => userList.value.filter((item) => item.isBanned === 1).length)
const activityCountInPage = computed(() =>
  userList.value.reduce(
    (sum, item) =>
      sum +
      (Number(item.postCount) || 0) +
      (Number(item.commentCount) || 0) +
      (Number(item.likeCount) || 0) +
      (Number(item.collectCount) || 0),
    0
  )
)

onMounted(() => {
  fetchUsers()
})

const fetchUsers = async () => {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }

    if (searchForm.timeRange && searchForm.timeRange.length === 2) {
      params.startTime = searchForm.timeRange[0]
      params.endTime = searchForm.timeRange[1]
    }
    delete params.timeRange

    const data = await communityApi.getUserStatusList(params)
    userList.value = data?.records || []
    pagination.total = data?.total || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  fetchUsers()
}

const handleReset = () => {
  Object.assign(searchForm, {
    userName: '',
    isBanned: null,
    timeRange: null
  })
  pagination.pageNum = 1
  fetchUsers()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleViewPosts = async (row: CommunityUserRecord) => {
  try {
    currentUser.value = row
    recordType.value = 'posts'
    userPosts.value = await communityApi.getUserPosts(row.userId)
    recordDialogVisible.value = true
  } catch (error) {
    console.error('获取用户发帖记录失败:', error)
    ElMessage.error('获取用户发帖记录失败')
  }
}

const handleViewComments = async (row: CommunityUserRecord) => {
  try {
    currentUser.value = row
    recordType.value = 'comments'
    userComments.value = await communityApi.getUserComments(row.userId)
    recordDialogVisible.value = true
  } catch (error) {
    console.error('获取用户评论记录失败:', error)
    ElMessage.error('获取用户评论记录失败')
  }
}

const handleBan = (row: CommunityUserRecord) => {
  currentUser.value = row
  Object.assign(banForm, {
    userName: row.userName,
    reason: '',
    duration: 24
  })
  banDialogVisible.value = true
}

const handleBanSubmit = async () => {
  if (!banFormRef.value || !currentUser.value) return

  const valid = await banFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await communityApi.banUser(currentUser.value.userId, {
      reason: banForm.reason,
      duration: banForm.duration
    })

    ElMessage.success('封禁成功')
    banDialogVisible.value = false
    await fetchUsers()
  } catch (error) {
    console.error('封禁用户失败:', error)
    ElMessage.error('封禁用户失败')
  } finally {
    submitLoading.value = false
  }
}

const handleUnban = async (row: CommunityUserRecord) => {
  try {
    await ElMessageBox.confirm(`确定要解封用户 "${row.userName}" 吗？`, '解封确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await communityApi.unbanUser(row.userId)
    ElMessage.success('解封成功')
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解封用户失败:', error)
      ElMessage.error('解封用户失败')
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.pageNum = page
  fetchUsers()
}

const handlePageSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchUsers()
}

const getPostStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    1: '正常',
    2: '下架',
    3: '删除'
  }
  return statusMap[status] || '未知'
}

const getPostStatusTone = (status: number): CnTone => {
  const toneMap: Record<number, CnTone> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  }
  return toneMap[status] || 'neutral'
}
</script>

<style scoped>
.community-users-page {
  min-height: 100%;
}

.community-stat-grid {
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

.user-cell span,
.muted-text,
.duration-field span {
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.metric-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.table-actions,
.dialog-footer,
.duration-field {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}

.duration-field {
  align-items: center;
}

.table-actions .el-button {
  margin-left: 0;
}

.dialog-footer {
  justify-content: flex-end;
}

.record-content {
  max-height: 500px;
  overflow-y: auto;
}

@media (max-width: 1180px) {
  .community-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .community-stat-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
