<template>
  <CnPage class="file-migration-page" surface="transparent" max-width="1400px">
    <CnPageHeader
      title="文件迁移管理"
      description="创建和执行跨存储迁移任务，跟踪全量、增量、时间范围和文件类型迁移进度。"
      eyebrow="File Migration"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">任务 {{ migrationList.length }} 个</CnStatusTag>
        <CnStatusTag type="warning">执行中 {{ runningCount }} 个</CnStatusTag>
        <CnStatusTag type="success">已完成 {{ completedCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="refreshList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog">创建迁移任务</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按任务状态和返回数量查看迁移任务。" divided>
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="任务状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="filter-control">
            <el-option label="全部" value="" />
            <el-option label="待执行" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已失败" value="FAILED" />
            <el-option label="已停止" value="STOPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务数量">
          <el-select v-model="queryParams.limit" placeholder="显示数量" class="filter-control">
            <el-option label="20条" :value="20" />
            <el-option label="50条" :value="50" />
            <el-option label="100条" :value="100" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection title="迁移任务列表" description="执行待处理任务，停止运行中任务，并查看任务执行详情。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="migrationList"
        :loading="loading"
        :pagination="null"
        row-key="id"
        empty-title="暂无迁移任务"
        empty-description="当前条件下没有迁移任务，可以创建新的存储迁移任务。"
        empty-icon="MG"
      >
        <template #migrationType="{ row }">
          <CnStatusTag :type="getMigrationTypeTone(row.migrationType)" size="sm">
            {{ getMigrationTypeName(row.migrationType) }}
          </CnStatusTag>
        </template>

        <template #sourceStorage="{ row }">
          <span>{{ storageConfigMap[row.sourceStorageId]?.configName || '-' }}</span>
        </template>

        <template #targetStorage="{ row }">
          <span>{{ storageConfigMap[row.targetStorageId]?.configName || '-' }}</span>
        </template>

        <template #progress="{ row }">
          <div class="progress-info">
            <el-progress :percentage="getProgressPercentage(row)" :status="getProgressStatus(row.status)" :stroke-width="8" />
            <div class="progress-text">
              {{ row.successCount || 0 }} / {{ row.totalFiles || 0 }}
              <span v-if="row.failCount > 0" class="fail-count">(失败: {{ row.failCount }})</span>
            </div>
          </div>
        </template>

        <template #status="{ row }">
          <CnStatusTag :type="getStatusTone(row.status)" size="sm">
            {{ getStatusName(row.status) }}
          </CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="info" link size="small" @click="viewTaskDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" type="success" link size="small" @click="executeTask(row)">
              执行
            </el-button>
            <el-button v-if="row.status === 'RUNNING'" type="warning" link size="small" @click="stopTask(row)">
              停止
            </el-button>
            <el-button
              v-if="['COMPLETED', 'FAILED', 'STOPPED'].includes(row.status)"
              type="danger"
              link
              size="small"
              @click="deleteTask(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="createDialogVisible" title="创建迁移任务" width="600px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="120px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="源存储" prop="sourceStorageId">
          <el-select v-model="createForm.sourceStorageId" placeholder="请选择源存储" class="full-width">
            <el-option
              v-for="config in storageConfigs"
              :key="config.id"
              :label="`${config.configName} (${getStorageTypeName(config.storageType)})`"
              :value="config.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标存储" prop="targetStorageId">
          <el-select v-model="createForm.targetStorageId" placeholder="请选择目标存储" class="full-width">
            <el-option
              v-for="config in storageConfigs"
              :key="config.id"
              :label="`${config.configName} (${getStorageTypeName(config.storageType)})`"
              :value="config.id"
              :disabled="config.id === createForm.sourceStorageId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="迁移类型" prop="migrationType">
          <el-select
            v-model="createForm.migrationType"
            placeholder="请选择迁移类型"
            class="full-width"
            @change="handleMigrationTypeChange"
          >
            <el-option label="全量迁移" value="FULL" />
            <el-option label="增量迁移" value="INCREMENTAL" />
            <el-option label="时间范围迁移" value="TIME_RANGE" />
            <el-option label="文件类型迁移" value="FILE_TYPE" />
          </el-select>
        </el-form-item>

        <div v-if="createForm.migrationType" class="filter-params">
          <template v-if="createForm.migrationType === 'TIME_RANGE'">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="filterParams.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="filterParams.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="full-width"
              />
            </el-form-item>
          </template>

          <template v-if="createForm.migrationType === 'FILE_TYPE'">
            <el-form-item label="文件类型">
              <el-checkbox-group v-model="filterParams.contentTypes">
                <el-checkbox label="image">图片</el-checkbox>
                <el-checkbox label="document">文档</el-checkbox>
                <el-checkbox label="video">视频</el-checkbox>
                <el-checkbox label="audio">音频</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>

          <template v-if="createForm.migrationType === 'INCREMENTAL'">
            <el-form-item label="增量起始时间">
              <el-date-picker
                v-model="filterParams.incrementalStartTime"
                type="datetime"
                placeholder="选择增量起始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="full-width"
              />
            </el-form-item>
          </template>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateTask">创建任务</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="迁移任务详情" width="800px">
      <div v-if="taskDetail" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ taskDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ taskDetail.taskName }}</el-descriptions-item>
          <el-descriptions-item label="迁移类型">
            <CnStatusTag :type="getMigrationTypeTone(taskDetail.migrationType)" size="sm">
              {{ getMigrationTypeName(taskDetail.migrationType) }}
            </CnStatusTag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <CnStatusTag :type="getStatusTone(taskDetail.status)" size="sm">
              {{ getStatusName(taskDetail.status) }}
            </CnStatusTag>
          </el-descriptions-item>
          <el-descriptions-item label="源存储">
            {{ storageConfigMap[taskDetail.sourceStorageId]?.configName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="目标存储">
            {{ storageConfigMap[taskDetail.targetStorageId]?.configName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="总文件数">{{ taskDetail.totalFiles || 0 }}</el-descriptions-item>
          <el-descriptions-item label="成功数量">{{ taskDetail.successCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="失败数量">{{ taskDetail.failCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ taskDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ taskDetail.startTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ taskDetail.endTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="taskDetail.filterParams" class="detail-block">
          <h4>筛选参数</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item v-for="(value, key) in parseFilterParams(taskDetail.filterParams)" :key="key" :label="key">
              {{ Array.isArray(value) ? value.join(', ') : value }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="taskDetail.errorMessage" class="detail-block">
          <h4>错误信息</h4>
          <el-alert :title="taskDetail.errorMessage" type="error" :closable="false" show-icon />
        </div>

        <div v-if="taskDetail.status === 'RUNNING'" class="detail-block">
          <h4>实时进度</h4>
          <el-progress :percentage="getProgressPercentage(taskDetail)" :stroke-width="12" text-inside />
          <div class="progress-details">
            <p>已处理: {{ (taskDetail.successCount || 0) + (taskDetail.failCount || 0) }} / {{ taskDetail.totalFiles || 0 }}</p>
            <p>成功: {{ taskDetail.successCount || 0 }}, 失败: {{ taskDetail.failCount || 0 }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { migrationAPI, storageAPI } from '@/api/filestorage'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface StorageConfig extends Record<string, unknown> {
  id: number
  configName: string
  storageType: string
}

interface MigrationTask extends Record<string, unknown> {
  id: number
  taskName: string
  migrationType: string
  sourceStorageId: number
  targetStorageId: number
  status: string
  totalFiles?: number
  successCount?: number
  failCount?: number
  createTime?: string
  startTime?: string
  endTime?: string
  filterParams?: string
  errorMessage?: string
}

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '文件存储' }, { label: '文件迁移' }]

const loading = ref(false)
const migrationList = ref<MigrationTask[]>([])
const storageConfigs = ref<StorageConfig[]>([])
const storageConfigMap = ref<Record<number, StorageConfig>>({})
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const taskDetail = ref<MigrationTask | null>(null)
const createFormRef = ref()

const queryParams = reactive({
  status: '',
  limit: 50
})

const createForm = reactive({
  taskName: '',
  sourceStorageId: null as number | null,
  targetStorageId: null as number | null,
  migrationType: ''
})

const filterParams = reactive<Record<string, unknown>>({})

const migrationTypeConfig: Record<string, { name: string; tone: CnTone }> = {
  FULL: { name: '全量迁移', tone: 'brand' },
  INCREMENTAL: { name: '增量迁移', tone: 'success' },
  TIME_RANGE: { name: '时间范围迁移', tone: 'warning' },
  FILE_TYPE: { name: '文件类型迁移', tone: 'info' }
}

const statusConfig: Record<string, { name: string; tone: CnTone }> = {
  PENDING: { name: '待执行', tone: 'info' },
  RUNNING: { name: '执行中', tone: 'warning' },
  COMPLETED: { name: '已完成', tone: 'success' },
  FAILED: { name: '失败', tone: 'danger' },
  STOPPED: { name: '已停止', tone: 'neutral' }
}

const storageTypeConfig: Record<string, { name: string }> = {
  LOCAL: { name: '本地存储' },
  OSS: { name: '阿里云OSS' },
  COS: { name: '腾讯云COS' },
  KODO: { name: '七牛云KODO' },
  OBS: { name: '华为云OBS' }
}

const createRules = reactive({
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceStorageId: [{ required: true, message: '请选择源存储', trigger: 'change' }],
  targetStorageId: [{ required: true, message: '请选择目标存储', trigger: 'change' }],
  migrationType: [{ required: true, message: '请选择迁移类型', trigger: 'change' }]
})

const tableColumns: CnTableColumn<MigrationTask>[] = [
  { prop: 'id', label: '任务ID', width: 90 },
  { prop: 'taskName', label: '任务名称', minWidth: 170, showOverflowTooltip: true },
  { prop: 'migrationType', label: '迁移类型', width: 130, slot: 'migrationType' },
  { label: '源存储', width: 150, slot: 'sourceStorage' },
  { label: '目标存储', width: 150, slot: 'targetStorage' },
  { label: '进度', width: 220, slot: 'progress' },
  { prop: 'status', label: '状态', width: 120, slot: 'status' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 240, fixed: 'right', slot: 'actions' }
]

const runningCount = computed(() => migrationList.value.filter((item) => item.status === 'RUNNING').length)
const completedCount = computed(() => migrationList.value.filter((item) => item.status === 'COMPLETED').length)

const loadMigrationList = async () => {
  loading.value = true
  try {
    const data = await migrationAPI.getMigrationTasks(queryParams)
    migrationList.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error('获取迁移任务列表失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const loadStorageConfigs = async () => {
  try {
    const data = await storageAPI.getStorageConfigs({ isEnabled: 1 })
    storageConfigs.value = Array.isArray(data) ? data : []

    const configMap: Record<number, StorageConfig> = {}
    storageConfigs.value.forEach((config) => {
      configMap[config.id] = config
    })
    storageConfigMap.value = configMap
  } catch (error) {
    console.error('获取存储配置失败:', error)
  }
}

const handleQuery = () => {
  loadMigrationList()
}

const resetQuery = () => {
  queryParams.status = ''
  queryParams.limit = 50
  loadMigrationList()
}

const refreshList = () => {
  loadMigrationList()
}

const showCreateDialog = () => {
  resetCreateForm()
  createDialogVisible.value = true
}

const resetCreateForm = () => {
  Object.assign(createForm, {
    taskName: '',
    sourceStorageId: null,
    targetStorageId: null,
    migrationType: ''
  })
  Object.keys(filterParams).forEach((key) => delete filterParams[key])
}

const handleMigrationTypeChange = () => {
  Object.keys(filterParams).forEach((key) => delete filterParams[key])

  if (createForm.migrationType === 'FILE_TYPE') {
    filterParams.contentTypes = []
  }
}

const handleCreateTask = async () => {
  try {
    await createFormRef.value.validate()

    const taskData = {
      ...createForm,
      filterParams
    }

    await migrationAPI.createMigrationTask(taskData)
    ElMessage.success('创建迁移任务成功')
    createDialogVisible.value = false
    loadMigrationList()
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

const viewTaskDetail = async (task: MigrationTask) => {
  try {
    const data = await migrationAPI.getMigrationTask(task.id)
    taskDetail.value = data
    detailDialogVisible.value = true

    if (data.status === 'RUNNING') {
      refreshTaskProgress(task.id)
    }
  } catch (error) {
    ElMessage.error('获取任务详情失败：' + (error.message || '未知错误'))
  }
}

const executeTask = async (task: MigrationTask) => {
  try {
    await ElMessageBox.confirm('确定要执行此迁移任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await migrationAPI.executeMigration(task.id)
    ElMessage.success('迁移任务已开始执行')
    loadMigrationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('执行迁移任务失败：' + (error.message || '未知错误'))
    }
  }
}

const stopTask = async (task: MigrationTask) => {
  try {
    await ElMessageBox.confirm('确定要停止此迁移任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await migrationAPI.stopMigration(task.id)
    ElMessage.success('迁移任务已停止')
    loadMigrationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停止迁移任务失败：' + (error.message || '未知错误'))
    }
  }
}

const deleteTask = async (task: MigrationTask) => {
  try {
    await ElMessageBox.confirm('确定要删除此迁移任务吗？删除后无法恢复！', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await migrationAPI.deleteMigrationTask(task.id)
    ElMessage.success('删除迁移任务成功')
    loadMigrationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除迁移任务失败：' + (error.message || '未知错误'))
    }
  }
}

const refreshTaskProgress = async (taskId: number) => {
  try {
    const progress = await migrationAPI.getMigrationProgress(taskId)
    if (taskDetail.value && taskDetail.value.id === taskId) {
      Object.assign(taskDetail.value, progress)

      if (progress.status === 'RUNNING') {
        window.setTimeout(() => refreshTaskProgress(taskId), 3000)
      }
    }
  } catch (error) {
    console.error('刷新任务进度失败:', error)
  }
}

const getMigrationTypeName = (type: string) => {
  return migrationTypeConfig[type]?.name || type
}

const getMigrationTypeTone = (type: string): CnTone => {
  return migrationTypeConfig[type]?.tone || 'info'
}

const getStatusName = (status: string) => {
  return statusConfig[status]?.name || status
}

const getStatusTone = (status: string): CnTone => {
  return statusConfig[status]?.tone || 'info'
}

const getStorageTypeName = (type: string) => {
  return storageTypeConfig[type]?.name || type
}

const getProgressPercentage = (task: MigrationTask) => {
  if (!task.totalFiles || task.totalFiles === 0) return 0
  const processed = (task.successCount || 0) + (task.failCount || 0)
  return Math.round((processed / task.totalFiles) * 100)
}

const getProgressStatus = (status: string) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return undefined
}

const parseFilterParams = (value: string) => {
  try {
    return JSON.parse(value)
  } catch {
    return { raw: value }
  }
}

onMounted(() => {
  loadMigrationList()
  loadStorageConfigs()
})
</script>

<style scoped>
.file-migration-page {
  min-height: 100%;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2) var(--cn-space-3);
}

.filter-control,
.full-width {
  width: 100%;
  min-width: 180px;
}

.progress-info {
  min-width: 0;
}

.progress-text {
  margin-top: var(--cn-space-1);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.fail-count {
  color: var(--cn-color-danger);
}

.filter-params {
  margin-top: var(--cn-space-5);
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.task-detail {
  max-height: 600px;
  overflow-y: auto;
}

.detail-block {
  margin-top: var(--cn-space-5);
}

.detail-block h4 {
  margin: 0 0 var(--cn-space-3);
  color: var(--cn-color-text-primary);
  font-size: 15px;
  font-weight: 650;
}

.progress-details {
  margin-top: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 13px;
}

.progress-details p {
  margin: var(--cn-space-1) 0;
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

@media (max-width: 680px) {
  .dialog-footer {
    justify-content: flex-start;
  }
}
</style>
