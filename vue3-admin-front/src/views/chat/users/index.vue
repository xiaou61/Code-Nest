<template>
  <CnPage class="chat-users-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="在线用户"
      description="查看聊天室实时在线用户，支持禁言、踢出和手动刷新在线列表。"
      eyebrow="Chat Users"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">聊天室管理</CnStatusTag>
        <CnStatusTag type="success">在线 {{ onlineCount }} 人</CnStatusTag>
        <CnStatusTag type="neutral">设备 {{ deviceCount }} 类</CnStatusTag>
        <CnStatusTag type="info">自动刷新 30 秒</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="handleRefresh">刷新</el-button>
      </template>
    </CnPageHeader>

    <div class="chat-stat-grid">
      <CnStatCard title="当前在线人数" :value="onlineCount" description="实时连接聊天室的用户数量" tone="success" />
      <CnStatCard title="设备类型" :value="deviceCount" description="当前在线用户设备信息去重数量" tone="brand" />
      <CnStatCard title="IP 数量" :value="ipCount" description="当前在线用户来源 IP 去重数量" tone="info" />
      <CnStatCard title="可操作用户" :value="tableData.length" description="当前列表可禁言或踢出的用户" tone="warning" />
    </div>

    <CnSection title="在线用户列表" :description="`当前在线 ${onlineCount} 人`" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="tableData"
        :loading="loading"
        :pagination="null"
        row-key="userId"
        empty-title="暂无在线用户"
        empty-description="当前没有用户连接聊天室。"
      >
        <template #toolbar>
          <CnToolbar title="在线会话" description="在线用户列表每 30 秒自动刷新一次，也可以手动刷新。" align="center">
            <template #meta>
              <CnStatusTag type="success" size="sm">在线 {{ onlineCount }} 人</CnStatusTag>
              <CnStatusTag type="neutral" size="sm">IP {{ ipCount }} 个</CnStatusTag>
            </template>

            <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
          </CnToolbar>
        </template>

        <template #user="{ row }">
          <div class="user-cell">
            <el-avatar :size="40" :src="row.userAvatar">
              {{ row.userNickname?.charAt(0) || 'U' }}
            </el-avatar>
            <div>
              <strong>{{ row.userNickname || '-' }}</strong>
              <span>用户 {{ row.userId }}</span>
            </div>
          </div>
        </template>

        <template #ipAddress="{ row }">
          <CnStatusTag type="neutral" size="sm">{{ row.ipAddress || '-' }}</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="warning" link size="small" @click="handleBanUser(row)">禁言</el-button>
            <el-button type="danger" link size="small" @click="handleKickUser(row)">踢出</el-button>
          </div>
        </template>

        <template #empty>
          <CnEmptyState title="暂无在线用户" description="当前没有用户连接聊天室。" icon="OU" surface="transparent">
            <template #actions>
              <el-button @click="handleRefresh">刷新</el-button>
            </template>
          </CnEmptyState>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="banDialogVisible" title="禁言用户" width="520px">
      <el-form :model="banForm" label-width="96px">
        <el-form-item label="用户昵称">
          <el-input :value="currentUser?.userNickname" disabled />
        </el-form-item>
        <el-form-item label="禁言时长">
          <el-radio-group v-model="banForm.banDuration">
            <el-radio :label="3600">1小时</el-radio>
            <el-radio :label="86400">1天</el-radio>
            <el-radio :label="604800">7天</el-radio>
            <el-radio :label="0">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="禁言原因">
          <el-input
            v-model="banForm.banReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入禁言原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="banning" @click="confirmBan">确定</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { banUser, getAdminOnlineUsers, kickUser } from '@/api/chat'
import {
  CnDataTable,
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatCard,
  CnStatusTag,
  CnToolbar
} from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn } from '@/design-system'

interface OnlineUserRecord {
  userId: number | string
  userNickname?: string
  userAvatar?: string
  ipAddress?: string
  deviceInfo?: string
  connectTime?: string
  [key: string]: unknown
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '聊天室管理' }, { label: '在线用户' }]

const loading = ref(false)
const banning = ref(false)
const tableData = ref<OnlineUserRecord[]>([])
const banDialogVisible = ref(false)
const currentUser = ref<OnlineUserRecord | null>(null)
let refreshTimer: ReturnType<typeof window.setInterval> | null = null

const banForm = reactive({
  userId: null as number | string | null,
  banDuration: 3600,
  banReason: ''
})

const tableColumns: CnTableColumn<OnlineUserRecord>[] = [
  { prop: 'userId', label: '用户ID', width: 100 },
  { prop: 'userNickname', label: '用户', minWidth: 180, slot: 'user', showOverflowTooltip: true },
  { prop: 'ipAddress', label: 'IP地址', width: 150, slot: 'ipAddress', showOverflowTooltip: true },
  { prop: 'deviceInfo', label: '设备信息', minWidth: 220, showOverflowTooltip: true },
  { prop: 'connectTime', label: '连接时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 140, fixed: 'right', slot: 'actions' }
]

const onlineCount = computed(() => tableData.value.length)
const deviceCount = computed(() => new Set(tableData.value.map((item) => item.deviceInfo).filter(Boolean)).size)
const ipCount = computed(() => new Set(tableData.value.map((item) => item.ipAddress).filter(Boolean)).size)

onMounted(() => {
  loadData()
  refreshTimer = window.setInterval(loadData, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
    refreshTimer = null
  }
})

const loadData = async () => {
  loading.value = true
  try {
    const result = await getAdminOnlineUsers()
    tableData.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('加载在线用户失败:', error)
    ElMessage.error('加载在线用户失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  loadData()
}

const handleBanUser = (row: OnlineUserRecord) => {
  currentUser.value = row
  banForm.userId = row.userId
  banForm.banDuration = 3600
  banForm.banReason = ''
  banDialogVisible.value = true
}

const confirmBan = async () => {
  if (!banForm.banReason.trim()) {
    ElMessage.warning('请输入禁言原因')
    return
  }

  banning.value = true
  try {
    await banUser({
      userId: banForm.userId,
      banDuration: banForm.banDuration,
      banReason: banForm.banReason
    })
    ElMessage.success('禁言成功')
    banDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('禁言聊天室用户失败:', error)
    ElMessage.error('禁言失败')
  } finally {
    banning.value = false
  }
}

const handleKickUser = async (row: OnlineUserRecord) => {
  try {
    await ElMessageBox.confirm(`确定要踢出用户 "${row.userNickname || '-'}" 吗？`, '踢出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await kickUser(row.userId)
    ElMessage.success('已踢出用户')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('踢出聊天室用户失败:', error)
      ElMessage.error('踢出失败')
    }
  }
}
</script>

<style scoped>
.chat-users-page {
  min-height: 100%;
}

.chat-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--cn-space-4);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
}

.user-cell > div {
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
