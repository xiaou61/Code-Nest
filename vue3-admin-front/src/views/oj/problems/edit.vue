<template>
  <div class="problem-edit">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="$router.push('/oj/problems')">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <h2>{{ isEdit ? '编辑题目' : '新增题目' }}</h2>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="题目标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入题目标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="请选择难度" style="width: 100%">
                <el-option label="简单" value="easy" />
                <el-option label="中等" value="medium" />
                <el-option label="困难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="标签">
          <el-select
            v-model="selectedTagIds"
            multiple
            filterable
            placeholder="选择标签"
            style="width: 100%"
          >
            <el-option v-for="tag in allTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="时间限制(ms)" prop="timeLimit">
              <el-input-number v-model="form.timeLimit" :min="100" :max="30000" :step="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存限制(MB)" prop="memoryLimit">
              <el-input-number v-model="form.memoryLimit" :min="16" :max="1024" :step="16" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">公开</el-radio>
                <el-radio :label="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="8" placeholder="支持 Markdown 格式" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="输入说明">
              <el-input v-model="form.inputDescription" type="textarea" :rows="3" placeholder="描述输入格式" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出说明">
              <el-input v-model="form.outputDescription" type="textarea" :rows="3" placeholder="描述输出格式" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="示例输入">
              <el-input v-model="form.sampleInput" type="textarea" :rows="3" placeholder="示例输入数据" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="示例输出">
              <el-input v-model="form.sampleOutput" type="textarea" :rows="3" placeholder="示例输出数据" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建题目' }}
          </el-button>
          <el-button @click="$router.push('/oj/problems')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 测试用例管理 (仅编辑模式) -->
    <el-card shadow="never" class="testcase-card" v-if="isEdit">
      <template #header>
        <div class="card-header">
          <h3>测试用例管理</h3>
          <el-button type="primary" size="small" @click="openTestCaseDialog(null)">
            <el-icon><Plus /></el-icon>
            添加用例
          </el-button>
        </div>
      </template>

      <el-table v-loading="testCasesLoading" :data="testCases" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="输入数据" min-width="200">
          <template #default="{ row }">
            <el-text truncated>{{ row.input || '(空)' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="期望输出" min-width="200">
          <template #default="{ row }">
            <el-text truncated>{{ row.expectedOutput || '(空)' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="示例" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isSample" type="success" size="small">是</el-tag>
            <el-tag v-else type="info" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openTestCaseDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDeleteTestCase(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 标准答案管理 (仅编辑模式) -->
    <el-card shadow="never" class="testcase-card" v-if="isEdit">
      <template #header>
        <div class="card-header">
          <h3>标准答案管理</h3>
          <el-button type="primary" size="small" @click="openSolutionDialog(null)">
            <el-icon><Plus /></el-icon>
            添加答案
          </el-button>
        </div>
      </template>

      <el-table v-loading="solutionsLoading" :data="solutions" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column label="标题" min-width="160" prop="title" />
        <el-table-column label="语言" width="120" prop="language" />
        <el-table-column label="代码预览" min-width="200">
          <template #default="{ row }">
            <el-text truncated>{{ row.code?.substring(0, 80) || '' }}...</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="openSolutionDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" text @click="handleDeleteSolution(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 测试用例对话框 -->
    <el-dialog
      :title="editingTestCase?.id ? '编辑测试用例' : '添加测试用例'"
      v-model="testCaseDialogVisible"
      width="600px"
      @close="resetTestCaseForm"
    >
      <el-form :model="testCaseForm" label-width="100px">
        <el-form-item label="输入数据">
          <el-input v-model="testCaseForm.input" type="textarea" :rows="5" placeholder="每行一个输入" />
        </el-form-item>
        <el-form-item label="期望输出">
          <el-input v-model="testCaseForm.expectedOutput" type="textarea" :rows="5" placeholder="每行一个输出" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="testCaseForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="示例用例">
          <el-checkbox v-model="testCaseForm.isSample">在题目详情中展示为示例</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTestCase" :loading="testCaseSaving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 标准答案对话框 -->
    <el-dialog
      :title="editingSolution?.id ? '编辑标准答案' : '添加标准答案'"
      v-model="solutionDialogVisible"
      width="700px"
      @close="resetSolutionForm"
    >
      <el-form :model="solutionForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题">
              <el-input v-model="solutionForm.title" placeholder="如“哈希表解法”" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="语言">
              <el-select v-model="solutionForm.language" style="width: 100%">
                <el-option label="Java" value="java" />
                <el-option label="C++" value="cpp" />
                <el-option label="C" value="c" />
                <el-option label="Python3" value="python" />
                <el-option label="Go" value="go" />
                <el-option label="JavaScript" value="javascript" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="代码">
          <el-input v-model="solutionForm.code" type="textarea" :rows="12" placeholder="标准答案代码" />
        </el-form-item>
        <el-form-item label="思路讲解">
          <el-input v-model="solutionForm.description" type="textarea" :rows="5" placeholder="支持 Markdown，可填写解题思路和复杂度分析" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="solutionForm.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="solutionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSolution" :loading="solutionSaving">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)
const problemId = computed(() => route.params.id)

// ============ 题目表单 ============
const form = reactive({
  title: '',
  description: '',
  difficulty: 'easy',
  timeLimit: 1000,
  memoryLimit: 256,
  inputDescription: '',
  outputDescription: '',
  sampleInput: '',
  sampleOutput: '',
  status: 0
})

const selectedTagIds = ref([])
const allTags = ref([])

const rules = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
}

// ============ 测试用例 ============
const testCases = ref([])
const testCasesLoading = ref(false)
const testCaseDialogVisible = ref(false)
const testCaseSaving = ref(false)
const editingTestCase = ref(null)
const testCaseForm = reactive({
  input: '',
  expectedOutput: '',
  sortOrder: 0,
  isSample: false
})

// ============ 标准答案 ============
const solutions = ref([])
const solutionsLoading = ref(false)
const solutionDialogVisible = ref(false)
const solutionSaving = ref(false)
const editingSolution = ref(null)
const solutionForm = reactive({
  title: '',
  language: 'java',
  code: '',
  description: '',
  sortOrder: 0
})

// ============ 方法 ============

const loadProblem = async () => {
  if (!isEdit.value) return
  try {
    const data = await ojApi.getProblemDetail(problemId.value)
    Object.assign(form, {
      title: data.title,
      description: data.description || '',
      difficulty: data.difficulty,
      timeLimit: data.timeLimit || 1000,
      memoryLimit: data.memoryLimit || 256,
      inputDescription: data.inputDescription || '',
      outputDescription: data.outputDescription || '',
      sampleInput: data.sampleInput || '',
      sampleOutput: data.sampleOutput || '',
      status: data.status
    })
    selectedTagIds.value = (data.tags || []).map(t => t.id)
  } catch {
    ElMessage.error('加载题目失败')
    router.push('/oj/problems')
  }
}

const loadTags = async () => {
  try {
    allTags.value = await ojApi.getTags() || []
  } catch {
    console.error('加载标签失败')
  }
}

const loadTestCases = async () => {
  if (!isEdit.value) return
  testCasesLoading.value = true
  try {
    testCases.value = await ojApi.getTestCasesByProblem(problemId.value) || []
  } catch {
    console.error('加载测试用例失败')
  } finally {
    testCasesLoading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await ojApi.updateProblem(problemId.value, form, selectedTagIds.value)
      ElMessage.success('修改成功')
    } else {
      const id = await ojApi.createProblem(form, selectedTagIds.value)
      ElMessage.success('创建成功')
      router.push(`/oj/problems/${id}/edit`)
    }
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

const openTestCaseDialog = (tc) => {
  editingTestCase.value = tc
  if (tc) {
    Object.assign(testCaseForm, {
      input: tc.input || '',
      expectedOutput: tc.expectedOutput || '',
      sortOrder: tc.sortOrder || 0,
      isSample: !!tc.isSample
    })
  } else {
    resetTestCaseForm()
  }
  testCaseDialogVisible.value = true
}

const resetTestCaseForm = () => {
  testCaseForm.input = ''
  testCaseForm.expectedOutput = ''
  testCaseForm.sortOrder = 0
  testCaseForm.isSample = false
  editingTestCase.value = null
}

const handleSaveTestCase = async () => {
  testCaseSaving.value = true
  try {
    if (editingTestCase.value?.id) {
      await ojApi.updateTestCase(editingTestCase.value.id, { ...testCaseForm })
      ElMessage.success('更新成功')
    } else {
      await ojApi.addTestCase({ ...testCaseForm, problemId: problemId.value })
      ElMessage.success('添加成功')
    }
    testCaseDialogVisible.value = false
    loadTestCases()
  } catch {
    // handled by interceptor
  } finally {
    testCaseSaving.value = false
  }
}

const handleDeleteTestCase = (tc) => {
  ElMessageBox.confirm('确定删除该测试用例吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await ojApi.deleteTestCase(tc.id)
    ElMessage.success('删除成功')
    loadTestCases()
  }).catch(() => {})
}

// ============ 标准答案方法 ============

const loadSolutions = async () => {
  if (!isEdit.value) return
  solutionsLoading.value = true
  try {
    solutions.value = await ojApi.getSolutionsByProblem(problemId.value) || []
  } catch {
    console.error('加载标准答案失败')
  } finally {
    solutionsLoading.value = false
  }
}

const openSolutionDialog = (sol) => {
  editingSolution.value = sol
  if (sol) {
    Object.assign(solutionForm, {
      title: sol.title || '',
      language: sol.language || 'java',
      code: sol.code || '',
      description: sol.description || '',
      sortOrder: sol.sortOrder || 0
    })
  } else {
    resetSolutionForm()
  }
  solutionDialogVisible.value = true
}

const resetSolutionForm = () => {
  solutionForm.title = ''
  solutionForm.language = 'java'
  solutionForm.code = ''
  solutionForm.description = ''
  solutionForm.sortOrder = 0
  editingSolution.value = null
}

const handleSaveSolution = async () => {
  solutionSaving.value = true
  try {
    if (editingSolution.value?.id) {
      await ojApi.updateSolution(editingSolution.value.id, { ...solutionForm })
      ElMessage.success('更新成功')
    } else {
      await ojApi.addSolution({ ...solutionForm, problemId: problemId.value })
      ElMessage.success('添加成功')
    }
    solutionDialogVisible.value = false
    loadSolutions()
  } catch {
    // handled by interceptor
  } finally {
    solutionSaving.value = false
  }
}

const handleDeleteSolution = (sol) => {
  ElMessageBox.confirm('确定删除该标准答案吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await ojApi.deleteSolution(sol.id)
    ElMessage.success('删除成功')
    loadSolutions()
  }).catch(() => {})
}

onMounted(() => {
  loadTags()
  if (isEdit.value) {
    loadProblem()
    loadTestCases()
    loadSolutions()
  }
})
</script>

<style scoped>
.main-card,
.testcase-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h2,
.card-header h3 {
  margin: 0;
  font-size: 18px;
}

.testcase-card .card-header {
  justify-content: space-between;
}
</style>
