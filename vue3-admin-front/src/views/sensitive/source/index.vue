<template>
  <div class="sensitive-source-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-card shadow="never">
        <el-row :gutter="10">
          <el-col :span="1.5">
            <el-button type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="sourceName" label="来源名称" min-width="150" />
        <el-table-column label="来源类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getSourceTypeTagType(row.sourceType)" size="small">
              {{ getSourceTypeText(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="wordCount" label="词汇数量" width="100" align="right" />
        <el-table-column prop="syncInterval" label="同步间隔(小时)" width="140" />
        <el-table-column label="同步状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.syncStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.syncStatus === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步时间" width="180" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="success"
              size="small"
              @click="handleSync(row)"
            >
              同步
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="handleTestConnection(row)"
            >
              测试
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="来源名称" prop="sourceName">
          <el-input
            v-model="form.sourceName"
            placeholder="请输入来源名称"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择类型" style="width: 100%">
            <el-option label="本地词库" value="local" />
            <el-option label="API接口" value="api" />
            <el-option label="GitHub" value="github" />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.sourceType !== 'local'"
          :label="form.sourceType === 'github' ? 'GitHub地址' : 'API地址'"
          prop="apiUrl"
        >
          <el-input
            v-model="form.apiUrl"
            :placeholder="form.sourceType === 'github' ? '请输入GitHub文件地址（支持blob/raw/api）' : '请输入API地址'"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item
          v-if="form.sourceType !== 'local'"
          :label="form.sourceType === 'github' ? '访问令牌' : 'API密钥'"
          prop="apiKey"
        >
          <el-input
            v-model="form.apiKey"
            type="password"
            :placeholder="form.sourceType === 'github' ? '可选，私有仓库请填写Token' : '可选，按需填写API密钥'"
            maxlength="255"
            show-password
          />
        </el-form-item>
        <el-form-item label="同步间隔(小时)" prop="syncInterval">
          <el-input-number
            v-model="form.syncInterval"
            :min="1"
            :max="720"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listSources,
  addSource,
  updateSource,
  deleteSource,
  testSourceConnection,
  syncSource
} from '@/api/sensitive'

// 响应式数据
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const tableData = ref([])
const total = ref(0)
const formRef = ref()

// 查询表单
const queryForm = reactive({
  pageNum: 1,
  pageSize: 10
})

// 编辑表单
const form = reactive({
  id: null,
  sourceName: '',
  sourceType: 'local',
  apiUrl: '',
  apiKey: '',
  syncInterval: 24,
  status: 1
})

// 表单验证规则
const formRules = {
  sourceName: [
    { required: true, message: '请输入来源名称', trigger: 'blur' }
  ],
  sourceType: [
    { required: true, message: '请选择来源类型', trigger: 'change' }
  ],
  apiUrl: [
    { required: true, message: '请输入API地址', trigger: 'blur' }
  ],
  syncInterval: [
    { required: true, message: '请输入同步间隔', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 方法
const handleQuery = async () => {
  loading.value = true
  try {
    const response = await listSources(queryForm)
    tableData.value = response.records
    total.value = response.total
  } catch (error) {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增词库来源'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑词库来源'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return

  try {
    if (form.id) {
      await updateSource(form)
      ElMessage.success('更新成功')
    } else {
      await addSource(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    handleQuery()
  } catch (error) {
    ElMessage.error(form.id ? '更新失败' : '新增失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这个词库来源吗？', '提示', {
      type: 'warning'
    })
    await deleteSource(row.id)
    ElMessage.success('删除成功')
    handleQuery()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleTestConnection = async (row) => {
  try {
    const message = await testSourceConnection(row.id)
    ElMessage.success(message || '连接测试成功')
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
}

const handleSync = async (row) => {
  try {
    const syncLoading = ElMessage.loading('正在同步词库，请稍候...')
    const result = await syncSource(row.id)
    syncLoading.close()

    ElMessage.success(
      result.message || `同步完成：新增 ${result.addedCount || 0}，更新 ${result.updatedCount || 0}，失败 ${result.failedCount || 0}`
    )
    handleQuery()
  } catch (error) {
    ElMessage.error('同步失败')
  }
}

const resetForm = () => {
  form.id = null
  form.sourceName = ''
  form.sourceType = 'local'
  form.apiUrl = ''
  form.apiKey = ''
  form.syncInterval = 24
  form.status = 1
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 工具方法
const getSourceTypeText = (type) => {
  const typeMap = {
    local: '本地词库',
    api: 'API接口',
    github: 'GitHub'
  }
  return typeMap[type] || '未知'
}

const getSourceTypeTagType = (type) => {
  const typeMap = {
    local: 'info',
    api: 'success',
    github: 'warning'
  }
  return typeMap[type] || 'info'
}

// 生命周期
onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.sensitive-source-page {
  padding: 20px;
}

.toolbar {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  text-align: center;
}
</style>
