<template>
  <div class="mock-interview-directions-page">
    <el-card shadow="never">
      <div class="page-header">
        <div>
          <h2>面试方向配置</h2>
          <p>维护模拟面试可用方向、排序和题库分类映射</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">
          新增方向
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border>
        <el-table-column prop="id" label="ID" min-width="80" />
        <el-table-column prop="directionName" label="方向名称" min-width="140" />
        <el-table-column prop="directionCode" label="方向代码" min-width="140" />
        <el-table-column prop="icon" label="图标" min-width="90" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="categoryIds" label="分类ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" min-width="90" />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="statusLoadingIds.includes(row.id)"
              @change="(val) => handleToggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" fixed="right" min-width="130">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增方向' : '编辑方向'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="方向名称" prop="directionName">
          <el-input v-model="formData.directionName" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="方向代码" prop="directionCode">
          <el-input v-model="formData.directionCode" :disabled="dialogMode === 'edit'" placeholder="如：java / frontend" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="formData.icon" placeholder="如：☕ / 🌐" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="分类ID" prop="categoryIds">
          <el-input v-model="formData.categoryIds" placeholder="多个ID逗号分隔，如：1,2,3" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" controls-position="right" style="width: 180px" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import mockInterviewAdminApi from '@/api/mockInterview'

const loading = ref(false)
const submitLoading = ref(false)
const statusLoadingIds = ref([])
const tableData = ref([])

const dialogVisible = ref(false)
const dialogMode = ref('create')
const formRef = ref(null)

const defaultForm = () => ({
  id: null,
  directionName: '',
  directionCode: '',
  icon: '',
  description: '',
  categoryIds: '',
  sortOrder: 0
})

const formData = reactive(defaultForm())

const formRules = {
  directionName: [{ required: true, message: '请输入方向名称', trigger: 'blur' }],
  directionCode: [{ required: true, message: '请输入方向代码', trigger: 'blur' }]
}

const resetForm = () => {
  const next = defaultForm()
  Object.keys(next).forEach((key) => {
    formData[key] = next[key]
  })
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await mockInterviewAdminApi.getDirections()
  } catch (error) {
    ElMessage.error('加载方向列表失败')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogMode.value = 'edit'
  resetForm()
  formData.id = row.id
  formData.directionName = row.directionName
  formData.directionCode = row.directionCode
  formData.icon = row.icon || ''
  formData.description = row.description || ''
  formData.categoryIds = row.categoryIds || ''
  formData.sortOrder = row.sortOrder ?? 0
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const payload = {
      directionName: formData.directionName,
      directionCode: formData.directionCode,
      icon: formData.icon,
      description: formData.description,
      categoryIds: formData.categoryIds,
      sortOrder: formData.sortOrder
    }

    if (dialogMode.value === 'create') {
      await mockInterviewAdminApi.createDirection(payload)
      ElMessage.success('新增成功')
    } else {
      await mockInterviewAdminApi.updateDirection(formData.id, payload)
      ElMessage.success('更新成功')
    }

    dialogVisible.value = false
    await loadData()
  } catch (error) {
    if (error) {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除方向「${row.directionName}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await mockInterviewAdminApi.deleteDirection(row.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleToggleStatus = async (row, value) => {
  statusLoadingIds.value.push(row.id)
  try {
    await mockInterviewAdminApi.updateDirectionStatus(row.id, value ? 1 : 0)
    row.status = value ? 1 : 0
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    statusLoadingIds.value = statusLoadingIds.value.filter((id) => id !== row.id)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.mock-interview-directions-page {
  padding: 20px;
  background: #f5f7fb;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 14px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: #909399;
}
</style>

