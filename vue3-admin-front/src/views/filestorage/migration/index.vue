<template>
  <div class="file-migration">
    <div class="header">
      <h2>文件迁移管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          创建迁移任务
        </el-button>
        <el-button @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 筛选条件 -->
    <div class="filters">
      <el-form :model="queryParams" inline>
        <el-form-item label="任务状态：">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="全部" value="" />
            <el-option label="待执行" value="PENDING" />
            <el-option label="执行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已失败" value="FAILED" />
            <el-option label="已停止" value="STOPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务数量：">
          <el-select v-model="queryParams.limit" placeholder="显示数量">
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
    </div>

    <!-- 迁移任务列表 -->
    <el-table v-loading="loading" :data="migrationList" style="width: 100%">
      <el-table-column prop="id" label="任务ID" width="80" />
      <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="migrationType" label="迁移类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getMigrationTypeTagType(row.migrationType)">
            {{ getMigrationTypeName(row.migrationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="源存储" width="150">
        <template #default="{ row }">
          <span v-if="storageConfigMap[row.sourceStorageId]">
            {{ storageConfigMap[row.sourceStorageId].configName }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="目标存储" width="150">
        <template #default="{ row }">
          <span v-if="storageConfigMap[row.targetStorageId]">
            {{ storageConfigMap[row.targetStorageId].configName }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="200">
        <template #default="{ row }">
          <div class="progress-info">
            <el-progress
              :percentage="getProgressPercentage(row)"
              :status="getProgressStatus(row.status)"
              :stroke-width="8"
            />
            <div class="progress-text">
              {{ row.successCount || 0 }} / {{ row.totalFiles || 0 }}
              <span v-if="row.failCount > 0" class="fail-count">(失败: {{ row.failCount }})</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="getStatusTagType(row.status)">
            {{ getStatusName(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="info" size="small" @click="viewTaskDetail(row)">
            详情
          </el-button>
          <el-button
            v-if="row.status === 'PENDING'"
            type="success"
            size="small"
            @click="executeTask(row)"
          >
            执行
          </el-button>
          <el-button
            v-if="row.status === 'RUNNING'"
            type="warning"
            size="small"
            @click="stopTask(row)"
          >
            停止
          </el-button>
          <el-button
            v-if="['COMPLETED', 'FAILED', 'STOPPED'].includes(row.status)"
            type="danger"
            size="small"
            @click="deleteTask(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建迁移任务对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建迁移任务" width="600px">
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="120px"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="源存储" prop="sourceStorageId">
          <el-select v-model="createForm.sourceStorageId" placeholder="请选择源存储">
            <el-option
              v-for="config in storageConfigs"
              :key="config.id"
              :label="`${config.configName} (${getStorageTypeName(config.storageType)})`"
              :value="config.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标存储" prop="targetStorageId">
          <el-select v-model="createForm.targetStorageId" placeholder="请选择目标存储">
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
          <el-select v-model="createForm.migrationType" placeholder="请选择迁移类型" @change="handleMigrationTypeChange">
            <el-option label="全量迁移" value="FULL" />
            <el-option label="增量迁移" value="INCREMENTAL" />
            <el-option label="时间范围迁移" value="TIME_RANGE" />
            <el-option label="文件类型迁移" value="FILE_TYPE" />
          </el-select>
        </el-form-item>

        <!-- 筛选参数 -->
        <div class="filter-params" v-if="createForm.migrationType">
          <!-- 时间范围迁移 -->
          <template v-if="createForm.migrationType === 'TIME_RANGE'">
            <el-form-item label="开始时间">
              <el-date-picker
                v-model="filterParams.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker
                v-model="filterParams.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </template>

          <!-- 文件类型迁移 -->
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

          <!-- 增量迁移 -->
          <template v-if="createForm.migrationType === 'INCREMENTAL'">
            <el-form-item label="增量起始时间">
              <el-date-picker
                v-model="filterParams.incrementalStartTime"
                type="datetime"
                placeholder="选择增量起始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
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

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="迁移任务详情" width="800px">
      <div v-if="taskDetail" class="task-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务ID">{{ taskDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ taskDetail.taskName }}</el-descriptions-item>
          <el-descriptions-item label="迁移类型">
            <el-tag :type="getMigrationTypeTagType(taskDetail.migrationType)">
              {{ getMigrationTypeName(taskDetail.migrationType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(taskDetail.status)">
              {{ getStatusName(taskDetail.status) }}
            </el-tag>
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

        <!-- 筛选参数 -->
        <div v-if="taskDetail.filterParams" class="filter-params-display">
          <h4>筛选参数</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item
              v-for="(value, key) in JSON.parse(taskDetail.filterParams)"
              :key="key"
              :label="key"
            >
              {{ Array.isArray(value) ? value.join(', ') : value }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 错误信息 -->
        <div v-if="taskDetail.errorMessage" class="error-message">
          <h4>错误信息</h4>
          <el-alert
            :title="taskDetail.errorMessage"
            type="error"
            :closable="false"
            show-icon
          />
        </div>

        <!-- 实时进度 -->
        <div v-if="taskDetail.status === 'RUNNING'" class="real-time-progress">
          <h4>实时进度</h4>
          <el-progress
            :percentage="getProgressPercentage(taskDetail)"
            :stroke-width="12"
            text-inside
          />
          <div class="progress-details">
            <p>已处理: {{ taskDetail.successCount + taskDetail.failCount }} / {{ taskDetail.totalFiles }}</p>
            <p>成功: {{ taskDetail.successCount }}, 失败: {{ taskDetail.failCount }}</p>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { migrationAPI, storageAPI } from '@/api/filestorage'

// 响应式数据
const loading = ref(false)
const migrationList = ref([])
const storageConfigs = ref([])
const storageConfigMap = ref({})

// 对话框状态
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const taskDetail = ref(null)
const createFormRef = ref()

// 查询参数
const queryParams = reactive({
  status: '',
  limit: 50
})

// 创建任务表单
const createForm = reactive({
  taskName: '',
  sourceStorageId: null,
  targetStorageId: null,
  migrationType: ''
})

// 筛选参数
const filterParams = reactive({})

// 迁移类型配置
const migrationTypeConfig = {
  'FULL': { name: '全量迁移', color: 'primary' },
  'INCREMENTAL': { name: '增量迁移', color: 'success' },
  'TIME_RANGE': { name: '时间范围迁移', color: 'warning' },
  'FILE_TYPE': { name: '文件类型迁移', color: 'info' }
}

// 状态配置
const statusConfig = {
  'PENDING': { name: '待执行', color: 'info' },
  'RUNNING': { name: '执行中', color: 'warning' },
  'COMPLETED': { name: '已完成', color: 'success' },
  'FAILED': { name: '失败', color: 'danger' },
  'STOPPED': { name: '已停止', color: 'info' }
}

// 存储类型配置
const storageTypeConfig = {
  'LOCAL': { name: '本地存储' },
  'OSS': { name: '阿里云OSS' },
  'COS': { name: '腾讯云COS' },
  'KODO': { name: '七牛云KODO' },
  'OBS': { name: '华为云OBS' }
}

// 表单验证规则
const createRules = reactive({
  taskName: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  sourceStorageId: [
    { required: true, message: '请选择源存储', trigger: 'change' }
  ],
  targetStorageId: [
    { required: true, message: '请选择目标存储', trigger: 'change' }
  ],
  migrationType: [
    { required: true, message: '请选择迁移类型', trigger: 'change' }
  ]
})

// 生命周期
onMounted(() => {
  loadMigrationList()
  loadStorageConfigs()
})

// 方法
const loadMigrationList = async () => {
  loading.value = true
  try {
    const data = await migrationAPI.getMigrationTasks(queryParams)
    migrationList.value = data
  } catch (error) {
    ElMessage.error('获取迁移任务列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const loadStorageConfigs = async () => {
  try {
    const data = await storageAPI.getStorageConfigs({ isEnabled: 1 })
    storageConfigs.value = data

    // 构建存储配置映射
    const configMap = {}
    data.forEach(config => {
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
  Object.keys(filterParams).forEach(key => delete filterParams[key])
}

const handleMigrationTypeChange = () => {
  // 清空筛选参数
  Object.keys(filterParams).forEach(key => delete filterParams[key])

  // 根据迁移类型初始化参数
  if (createForm.migrationType === 'FILE_TYPE') {
    filterParams.contentTypes = []
  }
}

const handleCreateTask = async () => {
  try {
    await createFormRef.value.validate()

    const taskData = {
      ...createForm,
      filterParams: filterParams
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

const viewTaskDetail = async (task) => {
  try {
    const data = await migrationAPI.getMigrationTask(task.id)
    taskDetail.value = data
    detailDialogVisible.value = true

    // 如果任务正在运行，定时刷新进度
    if (data.status === 'RUNNING') {
      refreshTaskProgress(task.id)
    }
  } catch (error) {
    ElMessage.error('获取任务详情失败：' + error.message)
  }
}

const executeTask = async (task) => {
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
      ElMessage.error('执行迁移任务失败：' + error.message)
    }
  }
}

const stopTask = async (task) => {
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
      ElMessage.error('停止迁移任务失败：' + error.message)
    }
  }
}

const deleteTask = async (task) => {
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
      ElMessage.error('删除迁移任务失败：' + error.message)
    }
  }
}

const refreshTaskProgress = async (taskId) => {
  try {
    const progress = await migrationAPI.getMigrationProgress(taskId)
    if (taskDetail.value && taskDetail.value.id === taskId) {
      Object.assign(taskDetail.value, progress)

      // 如果任务还在运行，3秒后再次刷新
      if (progress.status === 'RUNNING') {
        setTimeout(() => refreshTaskProgress(taskId), 3000)
      }
    }
  } catch (error) {
    console.error('刷新任务进度失败:', error)
  }
}

// 工具方法
const getMigrationTypeName = (type) => {
  return migrationTypeConfig[type]?.name || type
}

const getMigrationTypeTagType = (type) => {
  return migrationTypeConfig[type]?.color || 'info'
}

const getStatusName = (status) => {
  return statusConfig[status]?.name || status
}

const getStatusTagType = (status) => {
  return statusConfig[status]?.color || 'info'
}

const getStorageTypeName = (type) => {
  return storageTypeConfig[type]?.name || type
}

const getProgressPercentage = (task) => {
  if (!task.totalFiles || task.totalFiles === 0) return 0
  const processed = (task.successCount || 0) + (task.failCount || 0)
  return Math.round((processed / task.totalFiles) * 100)
}

const getProgressStatus = (status) => {
  if (status === 'COMPLETED') return 'success'
  if (status === 'FAILED') return 'exception'
  return null
}
</script>

<style scoped>
.file-migration {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.filters {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.progress-info {
  min-width: 150px;
}

.progress-text {
  margin-top: 5px;
  font-size: 12px;
  color: #606266;
}

.fail-count {
  color: #F56C6C;
}

.filter-params {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.task-detail {
  max-height: 600px;
  overflow-y: auto;
}

.filter-params-display,
.error-message,
.real-time-progress {
  margin-top: 20px;
}

.filter-params-display h4,
.error-message h4,
.real-time-progress h4 {
  margin: 0 0 10px 0;
  color: #303133;
}

.progress-details {
  margin-top: 10px;
  color: #606266;
}

.progress-details p {
  margin: 5px 0;
}

.dialog-footer {
  text-align: right;
}
</style>
