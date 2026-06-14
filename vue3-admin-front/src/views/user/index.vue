<template>
  <CnPage class="user-management" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="用户管理"
      description="管理系统中的账号资料、启用状态、密码重置和批量删除操作。"
      eyebrow="Admin Users"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">账号运营</CnStatusTag>
        <CnStatusTag type="success">状态可控</CnStatusTag>
        <CnStatusTag v-if="multipleSelection.length" type="warning">
          已选择 {{ multipleSelection.length }} 人
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button @click="loadStatistics" :icon="Refresh">刷新统计</el-button>
        <el-button type="primary" @click="handleAdd" :icon="Plus">添加用户</el-button>
      </template>
    </CnPageHeader>

    <div v-if="statistics" class="user-stat-grid">
      <CnStatCard
        v-for="item in statisticCards"
        :key="item.title"
        :title="item.title"
        :value="item.value"
        :description="item.description"
        :tone="item.tone"
        :trend="item.trend"
        :trend-text="item.trendText"
      />
    </div>

    <CnSection title="筛选与操作" description="按用户名、邮箱和用户状态快速定位账号。" divided>
      <CnFilterForm
        :model-value="searchForm"
        :fields="filterFields"
        :columns="3"
        :loading="loading"
        @update:model-value="handleSearchFormUpdate"
        @search="handleSearch"
        @reset="handleReset"
      />
    </CnSection>

    <CnSection title="用户列表" :description="`共 ${pagination.total} 条用户记录`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="userList"
        :loading="loading"
        :pagination="tablePagination"
        row-key="id"
        @selection-change="handleSelectionChange"
        @page-change="handleCurrentChange"
        @page-size-change="handleSizeChange"
      >
        <template #toolbar>
          <CnToolbar title="账号数据" description="列表操作不会改变当前筛选条件。" align="center">
            <template #meta>
              <CnStatusTag type="neutral" size="sm">每页 {{ pagination.pageSize }} 条</CnStatusTag>
              <CnStatusTag v-if="multipleSelection.length" type="warning" size="sm">
                已选择 {{ multipleSelection.length }} 人
              </CnStatusTag>
            </template>

            <el-button type="primary" @click="handleAdd" :icon="Plus">添加用户</el-button>
            <el-button
              type="danger"
              :disabled="!multipleSelection.length"
              @click="handleBatchDelete"
              :icon="Delete"
            >
              批量删除
            </el-button>
          </CnToolbar>
        </template>

        <template #avatar="{ row }">
          <el-avatar :size="40" :src="row.avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ getStatusText(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button size="small" @click="handleView(row)" :icon="View">查看</el-button>
            <el-button size="small" type="primary" @click="handleEdit(row)" :icon="Edit">编辑</el-button>
            <el-dropdown @command="(command: string) => handleDropdownCommand(command, row)">
              <el-button size="small" type="info">
                更多 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="`status_${row.id}`">
                    {{ row.status === 0 ? '禁用' : '启用' }}用户
                  </el-dropdown-item>
                  <el-dropdown-item :command="`reset_${row.id}`">重置密码</el-dropdown-item>
                  <el-dropdown-item :command="`delete_${row.id}`" divided>删除用户</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>

        <template #empty>
          <CnEmptyState
            title="暂无用户数据"
            description="当前筛选条件下没有匹配用户，可以重置筛选或新增用户。"
            icon="US"
            surface="transparent"
          >
            <template #actions>
              <el-button @click="handleReset">重置筛选</el-button>
              <el-button type="primary" @click="handleAdd">添加用户</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      class="user-dialog"
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userFormRules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="userForm.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" v-if="!isEdit">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="userForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="用户详情" width="500px">
      <el-descriptions :column="2" border v-if="currentUser">
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ currentUser.realName || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <CnStatusTag :type="getStatusTone(currentUser.status)" size="sm">
            {{ getStatusText(currentUser.status) }}
          </CnStatusTag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentUser.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentUser.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ currentUser.lastLoginTime || '从未登录' }}</el-descriptions-item>
        <el-descriptions-item label="创建者">{{ currentUser.createBy }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Delete, Edit, Plus, Refresh, Search, User, View } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
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
import type { CnBreadcrumbItem, CnFilterField, CnPagination, CnTableColumn, CnTone, CnTrend } from '@/design-system'

interface UserRecord {
  id: number
  username: string
  email: string
  phone?: string
  realName?: string
  avatar?: string
  status: number
  createTime?: string
  updateTime?: string
  lastLoginTime?: string
  createBy?: string
}

interface UserStatistics {
  totalUsers?: number
  activeUsers?: number
  disabledUsers?: number
  deletedUsers?: number
}

interface StatisticCard {
  title: string
  value: number
  description: string
  tone: CnTone
  trend: CnTrend
  trendText: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '用户管理' }]

const loading = ref(false)
const submitLoading = ref(false)
const userList = ref<UserRecord[]>([])
const multipleSelection = ref<UserRecord[]>([])
const statistics = ref<UserStatistics | null>(null)

const searchForm = reactive<{
  username: string
  email: string
  status?: number
}>({
  username: '',
  email: '',
  status: undefined
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const currentUser = ref<UserRecord | null>(null)

const userFormRef = ref()
const userForm = reactive({
  username: '',
  email: '',
  phone: '',
  realName: '',
  password: '',
  confirmPassword: '',
  status: 0
})

const userFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value !== userForm.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑用户' : '添加用户'))

const filterFields: CnFilterField[] = [
  { prop: 'username', label: '用户名', type: 'input', placeholder: '请输入用户名' },
  { prop: 'email', label: '邮箱', type: 'input', placeholder: '请输入邮箱' },
  {
    prop: 'status',
    label: '状态',
    type: 'select',
    placeholder: '请选择状态',
    options: [
      { label: '启用', value: 0 },
      { label: '禁用', value: 1 },
      { label: '已删除', value: 2 }
    ]
  }
]

const tableColumns: CnTableColumn<UserRecord>[] = [
  { type: 'selection', width: 52 },
  { prop: 'id', label: 'ID', width: 80 },
  { label: '头像', width: 80, align: 'center', slot: 'avatar' },
  { prop: 'username', label: '用户名', minWidth: 130 },
  { prop: 'email', label: '邮箱', minWidth: 210, showOverflowTooltip: true },
  { prop: 'phone', label: '手机号', minWidth: 130 },
  { prop: 'realName', label: '真实姓名', minWidth: 110 },
  { prop: 'status', label: '状态', width: 110, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180 },
  { prop: 'lastLoginTime', label: '最后登录', width: 180 },
  { label: '操作', width: 280, fixed: 'right', slot: 'actions' }
]

const tablePagination = computed<CnPagination>(() => ({
  page: pagination.pageNum,
  pageSize: pagination.pageSize,
  total: pagination.total,
  pageSizes: [10, 20, 50, 100]
}))

const statisticCards = computed<StatisticCard[]>(() => [
  {
    title: '总用户数',
    value: statistics.value?.totalUsers || 0,
    description: '系统累计账号规模',
    tone: 'brand',
    trend: 'flat',
    trendText: '总量'
  },
  {
    title: '活跃用户',
    value: statistics.value?.activeUsers || 0,
    description: '当前可正常使用账号',
    tone: 'success',
    trend: 'up',
    trendText: '启用'
  },
  {
    title: '禁用用户',
    value: statistics.value?.disabledUsers || 0,
    description: '已限制登录和操作',
    tone: 'warning',
    trend: 'flat',
    trendText: '禁用'
  },
  {
    title: '已删除',
    value: statistics.value?.deletedUsers || 0,
    description: '逻辑删除用户记录',
    tone: 'danger',
    trend: 'down',
    trendText: '删除'
  }
])

const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }

    const result = await userApi.getUserList(params)
    userList.value = result.records || []
    pagination.total = result.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const loadStatistics = async () => {
  try {
    statistics.value = await userApi.getUserStatistics()
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadUserList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    username: '',
    email: '',
    status: undefined
  })
  pagination.pageNum = 1
  loadUserList()
}

const handleSearchFormUpdate = (value: Record<string, unknown>) => {
  Object.assign(searchForm, value)
}

const handleAdd = () => {
  isEdit.value = false
  resetUserForm()
  dialogVisible.value = true
}

const handleEdit = (row: UserRecord) => {
  isEdit.value = true
  Object.assign(userForm, {
    username: row.username,
    email: row.email,
    phone: row.phone || '',
    realName: row.realName || '',
    status: row.status,
    password: '',
    confirmPassword: ''
  })
  currentUser.value = row
  dialogVisible.value = true
}

const handleView = (row: UserRecord) => {
  currentUser.value = row
  detailDialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await userFormRef.value.validate()
    submitLoading.value = true

    if (isEdit.value) {
      if (!currentUser.value) return
      const { password: _password, confirmPassword: _confirmPassword, ...updateData } = userForm
      await userApi.updateUser(currentUser.value.id, updateData)
      ElMessage.success('用户更新成功')
    } else {
      await userApi.createUser(userForm)
      ElMessage.success('用户创建成功')
    }

    dialogVisible.value = false
    loadUserList()
    loadStatistics()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleSelectionChange = (selection: UserRecord[]) => {
  multipleSelection.value = selection
}

const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${multipleSelection.value.length} 个用户吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const userIds = multipleSelection.value.map((user) => user.id)
    await userApi.deleteUsers(userIds)
    ElMessage.success('批量删除成功')
    loadUserList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  }
}

const handleDropdownCommand = async (command: string, row: UserRecord) => {
  const [action] = command.split('_')

  switch (action) {
    case 'status':
      await handleToggleStatus(row)
      break
    case 'reset':
      await handleResetPassword(row)
      break
    case 'delete':
      await handleDelete(row)
      break
  }
}

const handleToggleStatus = async (row: UserRecord) => {
  const newStatus = row.status === 0 ? 1 : 0
  const action = newStatus === 0 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定${action}用户 "${row.username}" 吗？`, `确认${action}`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await userApi.updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadUserList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(`${action}失败:`, error)
    }
  }
}

const handleResetPassword = async (row: UserRecord) => {
  try {
    const { value: newPassword } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入 8-20 位新密码',
      inputPattern: /^.{8,20}$/,
      inputErrorMessage: '密码长度在 8 到 20 个字符'
    })

    await userApi.resetPassword(row.id, newPassword)
    ElMessage.success('密码重置成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
    }
  }
}

const handleDelete = async (row: UserRecord) => {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await userApi.deleteUser(row.id)
    ElMessage.success('删除成功')
    loadUserList()
    loadStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  pagination.pageNum = 1
  loadUserList()
}

const handleCurrentChange = (page: number) => {
  pagination.pageNum = page
  loadUserList()
}

const resetUserForm = () => {
  Object.assign(userForm, {
    username: '',
    email: '',
    phone: '',
    realName: '',
    password: '',
    confirmPassword: '',
    status: 0
  })
  userFormRef.value?.clearValidate()
}

const getStatusTone = (status: number): CnTone => {
  const statusMap: Record<number, CnTone> = {
    0: 'success',
    1: 'warning',
    2: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '启用',
    1: '禁用',
    2: '已删除'
  }
  return statusMap[status] || '未知'
}

onMounted(() => {
  loadUserList()
  loadStatistics()
})
</script>

<style scoped>
.user-management {
  min-height: 100%;
}

.user-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.table-actions,
.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--cn-space-2);
}

.table-actions {
  justify-content: flex-start;
}

.table-actions .el-button {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .user-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .user-stat-grid {
    grid-template-columns: 1fr;
  }

  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
