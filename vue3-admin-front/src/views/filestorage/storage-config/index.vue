<template>
  <CnPage class="storage-config-page" surface="transparent" max-width="1320px">
    <CnPageHeader
      title="存储配置管理"
      description="维护本地、OSS、COS、KODO、OBS 等文件存储配置，管理启用状态、默认存储与连通性测试。"
      eyebrow="Storage Config"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag type="brand">配置 {{ storageList.length }} 个</CnStatusTag>
        <CnStatusTag type="success">启用 {{ enabledCount }} 个</CnStatusTag>
        <CnStatusTag type="warning">默认 {{ defaultCount }} 个</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="Refresh" :loading="loading" @click="refreshList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="showAddDialog">新增存储配置</el-button>
      </template>
    </CnPageHeader>

    <CnSection title="筛选条件" description="按存储类型和启用状态过滤配置。" divided>
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="存储类型">
          <el-select v-model="queryParams.storageType" placeholder="请选择存储类型" clearable class="filter-control">
            <el-option label="全部" value="" />
            <el-option v-for="type in storageTypes" :key="type" :label="getStorageTypeName(type)" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.isEnabled" placeholder="请选择状态" clearable class="filter-control">
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
    </CnSection>

    <CnSection title="配置列表" description="测试存储连接、切换启用状态，并设置默认存储。" divided>
      <CnDataTable
        :columns="tableColumns"
        :data="storageList"
        :loading="loading"
        :pagination="null"
        row-key="id"
        empty-title="暂无存储配置"
        empty-description="还没有符合条件的存储配置，可以新增一个本地或云存储配置。"
        empty-icon="SC"
      >
        <template #storageType="{ row }">
          <CnStatusTag :type="getStorageTypeTone(row.storageType)" size="sm">
            {{ getStorageTypeName(row.storageType) }}
          </CnStatusTag>
        </template>

        <template #isDefault="{ row }">
          <CnStatusTag v-if="row.isDefault === 1" type="warning" size="sm">默认</CnStatusTag>
          <CnStatusTag v-else type="neutral" size="sm" subtle>普通</CnStatusTag>
        </template>

        <template #isEnabled="{ row }">
          <CnStatusTag :type="row.isEnabled === 1 ? 'success' : 'danger'" size="sm">
            {{ row.isEnabled === 1 ? '启用' : '禁用' }}
          </CnStatusTag>
        </template>

        <template #testStatus="{ row }">
          <CnStatusTag v-if="row.testStatus === 1" type="success" size="sm">成功</CnStatusTag>
          <CnStatusTag v-else-if="row.testStatus === 0" type="danger" size="sm">失败</CnStatusTag>
          <CnStatusTag v-else type="neutral" size="sm" subtle>未测试</CnStatusTag>
        </template>

        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" link size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="testConfig(row)">测试</el-button>
            <el-button :type="row.isEnabled === 1 ? 'warning' : 'success'" link size="small" @click="toggleStatus(row)">
              {{ row.isEnabled === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="row.isDefault !== 1" type="info" link size="small" @click="setDefault(row)">
              设为默认
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑存储配置' : '新增存储配置'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="存储类型" prop="storageType">
          <el-select v-model="form.storageType" placeholder="请选择存储类型" class="full-width" @change="handleStorageTypeChange">
            <el-option v-for="type in storageTypes" :key="type" :label="getStorageTypeName(type)" :value="type" />
          </el-select>
        </el-form-item>

        <template v-if="form.storageType === 'LOCAL'">
          <el-form-item label="存储路径" prop="configParams.localPath">
            <el-input v-model="configParams.localPath" placeholder="请输入本地存储路径" />
          </el-form-item>
          <el-form-item label="容量限制(GB)" prop="configParams.maxSize">
            <el-input-number v-model="configParams.maxSize" :min="1" :max="1000" />
          </el-form-item>
        </template>

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
  </CnPage>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { storageAPI } from '@/api/filestorage'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface StorageConfig extends Record<string, unknown> {
  id: number | null
  configName: string
  storageType: string
  isEnabled: number
  isDefault: number
  testStatus?: number | null
  createTime?: string
  configParams?: string
}

interface StorageForm {
  id: number | null
  configName: string
  storageType: string
  isEnabled: number
  isDefault: number
}

type ConfigParams = Record<string, string | number | null>

const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '文件存储' }, { label: '存储配置' }]

const loading = ref(false)
const storageList = ref<StorageConfig[]>([])
const storageTypes = ref<string[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const queryParams = reactive({
  storageType: '',
  isEnabled: '' as string | number
})

const form = reactive<StorageForm>({
  id: null,
  configName: '',
  storageType: '',
  isEnabled: 1,
  isDefault: 0
})

const configParams = reactive<ConfigParams>({})

const storageTypeConfig: Record<string, { name: string; tone: CnTone }> = {
  LOCAL: { name: '本地存储', tone: 'brand' },
  OSS: { name: '阿里云OSS', tone: 'success' },
  COS: { name: '腾讯云COS', tone: 'warning' },
  KODO: { name: '七牛云KODO', tone: 'info' },
  OBS: { name: '华为云OBS', tone: 'danger' }
}

const rules = reactive({
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  storageType: [{ required: true, message: '请选择存储类型', trigger: 'change' }]
})

const tableColumns: CnTableColumn<StorageConfig>[] = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'configName', label: '配置名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'storageType', label: '存储类型', width: 130, slot: 'storageType' },
  { prop: 'isDefault', label: '默认存储', width: 110, slot: 'isDefault' },
  { prop: 'isEnabled', label: '状态', width: 100, slot: 'isEnabled' },
  { prop: 'testStatus', label: '测试状态', width: 120, slot: 'testStatus' },
  { prop: 'createTime', label: '创建时间', width: 180, showOverflowTooltip: true },
  { label: '操作', width: 300, fixed: 'right', slot: 'actions' }
]

const enabledCount = computed(() => storageList.value.filter((item) => item.isEnabled === 1).length)
const defaultCount = computed(() => storageList.value.filter((item) => item.isDefault === 1).length)

const getStorageTypeName = (type: string) => {
  return storageTypeConfig[type]?.name || type
}

const getStorageTypeTone = (type: string): CnTone => {
  return storageTypeConfig[type]?.tone || 'info'
}

const loadStorageTypes = async () => {
  try {
    const data = await storageAPI.getSupportedStorageTypes()
    storageTypes.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error('获取存储类型失败：' + (error.message || '未知错误'))
  }
}

const loadStorageList = async () => {
  loading.value = true
  try {
    const data = await storageAPI.getStorageConfigs(queryParams)
    storageList.value = Array.isArray(data) ? data : []
  } catch (error) {
    ElMessage.error('获取存储配置列表失败：' + (error.message || '未知错误'))
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

const showEditDialog = async (row: StorageConfig) => {
  try {
    const data = await storageAPI.getStorageConfig(row.id)
    Object.assign(form, data)

    Object.keys(configParams).forEach((key) => delete configParams[key])
    if (data.configParams) {
      const params = JSON.parse(data.configParams)
      Object.assign(configParams, params)
    }

    isEdit.value = true
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取存储配置详情失败：' + (error.message || '未知错误'))
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
  Object.keys(configParams).forEach((key) => delete configParams[key])
  nextTick(() => {
    formRef.value?.resetFields()
  })
}

const handleStorageTypeChange = () => {
  Object.keys(configParams).forEach((key) => delete configParams[key])
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

const testConfig = async (row: StorageConfig) => {
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
    ElMessage.error('测试存储配置失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row: StorageConfig) => {
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
      ElMessage.error(`${action}存储配置失败：` + (error.message || '未知错误'))
    }
  }
}

const setDefault = async (row: StorageConfig) => {
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
      ElMessage.error('设置默认存储失败：' + (error.message || '未知错误'))
    }
  }
}

const handleDelete = async (row: StorageConfig) => {
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
      ElMessage.error('删除存储配置失败：' + (error.message || '未知错误'))
    }
  }
}

onMounted(() => {
  loadStorageTypes()
  loadStorageList()
})
</script>

<style scoped>
.storage-config-page {
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
