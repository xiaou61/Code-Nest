<template>
  <div class="tags-page">
    <div class="header">
      <h2>OJ 标签管理</h2>
      <el-button type="primary" @click="showAddDialog" :icon="Plus">
        新建标签
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tagList" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="name" label="标签名称" min-width="200">
          <template #default="{ row }">
            <el-tag type="success" effect="plain"># {{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增标签对话框 -->
    <el-dialog v-model="dialogVisible" title="新建标签" width="450px" :close-on-click-modal="false">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="30" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const tagList = ref([])
const formRef = ref(null)

const form = reactive({ name: '' })
const rules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 30, message: '标签名称长度为1-30个字符', trigger: 'blur' }
  ]
}

const fetchTags = async () => {
  loading.value = true
  try {
    tagList.value = await ojApi.getTags() || []
  } catch {
    ElMessage.error('获取标签列表失败')
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  form.name = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true
    await ojApi.createTag(form)
    ElMessage.success('创建标签成功')
    dialogVisible.value = false
    fetchTags()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除标签「${row.name}」吗？`, '确认删除', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // Note: backend may not have a dedicated delete endpoint for OJ tags.
    // If needed, extend the admin API. For now show info.
    ElMessage.info('如需删除标签，请联系开发者扩展后端接口')
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchTags()
})
</script>

<style scoped>
.tags-page {
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
  font-size: 24px;
  color: #303133;
}

.table-card {
  margin-bottom: 20px;
}
</style>
