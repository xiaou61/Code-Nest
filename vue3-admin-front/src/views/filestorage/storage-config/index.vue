<template>
  <div class="storage-config">
    <div class="header">
      <h2>存储配置管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          新增存储配置
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
        <el-form-item label="存储类型：">
          <el-select v-model="queryParams.storageType" placeholder="请选择存储类型" clearable>
            <el-option label="全部" value="" />
            <el-option
              v-for="type in storageTypes"
              :key="type"
              :label="getStorageTypeName(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态：">
          <el-select v-model="queryParams.isEnabled" placeholder="请选择状态" clearable>
            <el-option label="全部" value="" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="storageList" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="configName" label="配置名称" min-width="150" />
      <el-table-column prop="storageType" label="存储类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getStorageTypeTagType(row.storageType)">
            {{ getStorageTypeName(row.storageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isDefault" label="默认存储" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="warning">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="isEnabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
            {{ row.isEnabled === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="testStatus" label="测试状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.testStatus === 1" type="success">成功</el-tag>
          <el-tag v-else-if="row.testStatus === 0" type="danger">失败</el-tag>
          <span v-else>未测试</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="showEditDialog(row)">
            编辑
          </el-button>
          <el-button type="warning" size="small" @click="testConfig(row)">
            测试
          </el-button>
          <el-button
            :type="row.isEnabled === 1 ? 'warning' : 'success'"
            size="small"
            @click="toggleStatus(row)"
          >
            {{ row.isEnabled === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            v-if="row.isDefault !== 1"
            type="info"
            size="small"
            @click="setDefault(row)"
          >
            设为默认
          </el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑存储配置' : '新增存储配置'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="存储类型" prop="storageType">
          <el-select v-model="form.storageType" placeholder="请选择存储类型" @change="handleStorageTypeChange">
            <el-option
              v-for="type in storageTypes"
              :key="type"
              :label="getStorageTypeName(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>

        <!-- 本地存储配置 -->
        <template v-if="form.storageType === 'LOCAL'">
          <el-form-item label="存储路径" prop="configParams.localPath">
            <el-input v-model="configParams.localPath" placeholder="请输入本地存储路径" />
          </el-form-item>
          <el-form-item label="容量限制(GB)" prop="configParams.maxSize">
            <el-input-number v-model="configParams.maxSize" :min="1" :max="1000" />
          </el-form-item>
        </template>

        <!-- 阿里云OSS配置 -->
        <template v-if="form.storageType === 'OSS'">
          <el-form-item label="Access Key ID" prop="configParams.accessKeyId">
            <el-input v-model="configParams.accessKeyId" placeholder="请输入Access Key ID" />
          </el-form-item>
          <el-form-item label="Access Key Secret" prop="configParams.accessKeySecret">
            <el-input v-model="configParams.accessKeySecret" type="password" placeholder="请输入Access Key Secret" />
          </el-form-item>
          <el-form-item label="Endpoint" prop="configParams.endpoint">
            <el-input v-model="configParams.endpoint" placeholder="请输入Endpoint" />
          </el-form-item>
          <el-form-item label="Bucket名称" prop="configParams.bucketName">
            <el-input v-model="configParams.bucketName" placeholder="请输入Bucket名称" />
          </el-form-item>
          <el-form-item label="域名" prop="configParams.domain">
            <el-input v-model="configParams.domain" placeholder="请输入访问域名(可选)" />
          </el-form-item>
        </template>

        <!-- 腾讯云COS配置 -->
        <template v-if="form.storageType === 'COS'">
          <el-form-item label="Secret ID" prop="configParams.secretId">
            <el-input v-model="configParams.secretId" placeholder="请输入Secret ID" />
          </el-form-item>
          <el-form-item label="Secret Key" prop="configParams.secretKey">
            <el-input v-model="configParams.secretKey" type="password" placeholder="请输入Secret Key" />
          </el-form-item>
          <el-form-item label="Region" prop="configParams.region">
            <el-input v-model="configParams.region" placeholder="请输入Region" />
          </el-form-item>
          <el-form-item label="Bucket名称" prop="configParams.bucketName">
            <el-input v-model="configParams.bucketName" placeholder="请输入Bucket名称" />
          </el-form-item>
          <el-form-item label="域名" prop="configParams.domain">
            <el-input v-model="configParams.domain" placeholder="请输入访问域名(可选)" />
          </el-form-item>
        </template>

        <!-- 七牛云KODO配置 -->
        <template v-if="form.storageType === 'KODO'">
          <el-form-item label="Access Key" prop="configParams.accessKey">
            <el-input v-model="configParams.accessKey" placeholder="请输入Access Key" />
          </el-form-item>
          <el-form-item label="Secret Key" prop="configParams.secretKey">
            <el-input v-model="configParams.secretKey" type="password" placeholder="请输入Secret Key" />
          </el-form-item>
          <el-form-item label="Bucket名称" prop="configParams.bucketName">
            <el-input v-model="configParams.bucketName" placeholder="请输入Bucket名称" />
          </el-form-item>
          <el-form-item label="域名" prop="configParams.domain">
            <el-input v-model="configParams.domain" placeholder="请输入访问域名" />
          </el-form-item>
        </template>

        <!-- 华为云OBS配置 -->
        <template v-if="form.storageType === 'OBS'">
          <el-form-item label="Access Key ID" prop="configParams.accessKeyId">
            <el-input v-model="configParams.accessKeyId" placeholder="请输入Access Key ID" />
          </el-form-item>
          <el-form-item label="Secret Access Key" prop="configParams.secretAccessKey">
            <el-input v-model="configParams.secretAccessKey" type="password" placeholder="请输入Secret Access Key" />
          </el-form-item>
          <el-form-item label="Endpoint" prop="configParams.endpoint">
            <el-input v-model="configParams.endpoint" placeholder="请输入Endpoint" />
          </el-form-item>
          <el-form-item label="Bucket名称" prop="configParams.bucketName">
            <el-input v-model="configParams.bucketName" placeholder="请输入Bucket名称" />
          </el-form-item>
          <el-form-item label="域名" prop="configParams.domain">
            <el-input v-model="configParams.domain" placeholder="请输入访问域名(可选)" />
          </el-form-item>
        </template>

        <el-form-item label="是否启用">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { storageAPI } from '@/api/filestorage'

// 响应式数据
const loading = ref(false)
const storageList = ref([])
const storageTypes = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

// 查询参数
const queryParams = reactive({
  storageType: '',
  isEnabled: ''
})

// 表单数据
const form = reactive({
  id: null,
  configName: '',
  storageType: '',
  isEnabled: 1,
  isDefault: 0
})

// 配置参数
const configParams = reactive({})

// 存储类型配置
const storageTypeConfig = {
  'LOCAL': { name: '本地存储', color: 'primary' },
  'OSS': { name: '阿里云OSS', color: 'success' },
  'COS': { name: '腾讯云COS', color: 'warning' },
  'KODO': { name: '七牛云KODO', color: 'info' },
  'OBS': { name: '华为云OBS', color: 'danger' }
}

// 表单验证规则
const rules = reactive({
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ],
  storageType: [
    { required: true, message: '请选择存储类型', trigger: 'change' }
  ]
})

// 计算属性
const getStorageTypeName = (type) => {
  return storageTypeConfig[type]?.name || type
}

const getStorageTypeTagType = (type) => {
  return storageTypeConfig[type]?.color || 'info'
}

// 生命周期
onMounted(() => {
  loadStorageTypes()
  loadStorageList()
})

// 方法
const loadStorageTypes = async () => {
  try {
    const data = await storageAPI.getSupportedStorageTypes()
    storageTypes.value = data
  } catch (error) {
    ElMessage.error('获取存储类型失败：' + error.message)
  }
}

const loadStorageList = async () => {
  loading.value = true
  try {
    const data = await storageAPI.getStorageConfigs(queryParams)
    storageList.value = data
  } catch (error) {
    ElMessage.error('获取存储配置列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  loadStorageList()
}

const resetQuery = () => {
  queryParams.storageType = ''
  queryParams.isEnabled = ''
  loadStorageList()
}

const refreshList = () => {
  loadStorageList()
}

const showAddDialog = () => {
  resetForm()
  isEdit.value = false
  dialogVisible.value = true
}

const showEditDialog = async (row) => {
  try {
    const data = await storageAPI.getStorageConfig(row.id)
    Object.assign(form, data)

    // 解析配置参数
    if (data.configParams) {
      const params = JSON.parse(data.configParams)
      Object.assign(configParams, params)
    }

    isEdit.value = true
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取存储配置详情失败：' + error.message)
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    configName: '',
    storageType: '',
    isEnabled: 1,
    isDefault: 0
  })
  Object.keys(configParams).forEach(key => delete configParams[key])
  nextTick(() => {
    formRef.value?.resetFields()
  })
}

const handleStorageTypeChange = () => {
  // 清空配置参数
  Object.keys(configParams).forEach(key => delete configParams[key])
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    const submitData = {
      ...form,
      configParams: JSON.stringify(configParams)
    }

    if (isEdit.value) {
      await storageAPI.updateStorageConfig(form.id, submitData)
      ElMessage.success('更新存储配置成功')
    } else {
      await storageAPI.createStorageConfig(submitData)
      ElMessage.success('创建存储配置成功')
    }

    dialogVisible.value = false
    loadStorageList()
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

const testConfig = async (row) => {
  try {
    loading.value = true
    const result = await storageAPI.testStorageConfig(row.id)
    if (result.success) {
      ElMessage.success('存储配置测试成功')
    } else {
      ElMessage.error('存储配置测试失败：' + result.message)
    }
    loadStorageList()
  } catch (error) {
    ElMessage.error('测试存储配置失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row) => {
  const newStatus = row.isEnabled === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(`确定要${action}此存储配置吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await storageAPI.toggleStorageConfig(row.id, newStatus)
    ElMessage.success(`${action}存储配置成功`)
    loadStorageList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}存储配置失败：` + error.message)
    }
  }
}

const setDefault = async (row) => {
  try {
    await ElMessageBox.confirm('确定要将此配置设为默认存储吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await storageAPI.setDefaultStorageConfig(row.id)
    ElMessage.success('设置默认存储成功')
    loadStorageList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('设置默认存储失败：' + error.message)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此存储配置吗？删除后无法恢复！', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await storageAPI.deleteStorageConfig(row.id)
    ElMessage.success('删除存储配置成功')
    loadStorageList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除存储配置失败：' + error.message)
    }
  }
}
</script>

<style scoped>
.storage-config {
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

.dialog-footer {
  text-align: right;
}
</style>
