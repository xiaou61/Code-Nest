<template>
  <CnPage class="problem-edit-page" surface="transparent" max-width="1180px">
    <CnPageHeader
      :title="isEdit ? '编辑题目' : '新增题目'"
      description="维护 OJ 题目描述、限制、标签、测试用例和标准答案。"
      eyebrow="OJ Problem"
      :breadcrumbs="breadcrumbs"
    >
      <template #meta>
        <CnStatusTag :type="form.status === 1 ? 'success' : 'neutral'">
          {{ form.status === 1 ? '公开' : '隐藏' }}
        </CnStatusTag>
        <CnStatusTag :type="getDifficultyTone(form.difficulty)">
          {{ getDifficultyLabel(form.difficulty) }}
        </CnStatusTag>
        <CnStatusTag v-if="isEdit" type="brand">用例 {{ testCases.length }} 条</CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="router.push('/oj/problems')">返回列表</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '创建题目' }}
        </el-button>
      </template>
    </CnPageHeader>

    <CnSection title="题目信息" description="填写题目描述、输入输出、样例、限制和标签。" divided>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="题目标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入题目标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" placeholder="请选择难度" class="full-width">
                <el-option label="简单" value="easy" />
                <el-option label="中等" value="medium" />
                <el-option label="困难" value="hard" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="标签">
          <el-select v-model="selectedTagIds" multiple filterable placeholder="选择标签" class="full-width">
            <el-option v-for="tag in allTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="时间限制(ms)" prop="timeLimit">
              <el-input-number v-model="form.timeLimit" :min="100" :max="30000" :step="100" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="内存限制(MB)" prop="memoryLimit">
              <el-input-number v-model="form.memoryLimit" :min="16" :max="1024" :step="16" class="full-width" />
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
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '创建题目' }}
          </el-button>
          <el-button @click="router.push('/oj/problems')">取消</el-button>
        </el-form-item>
      </el-form>
    </CnSection>

    <CnSection v-if="isEdit" title="测试用例管理" description="维护该题目的输入输出用例。" divided>
      <template #actions>
        <el-button type="primary" size="small" :icon="Plus" @click="openTestCaseDialog(null)">添加用例</el-button>
      </template>

      <CnDataTable
        :columns="testCaseColumns"
        :data="testCases"
        :loading="testCasesLoading"
        :pagination="null"
        row-key="id"
        empty-title="暂无测试用例"
        empty-description="还没有为该题目配置测试用例。"
        empty-icon="TC"
      >
        <template #input="{ row }">
          <el-text truncated>{{ row.input || '(空)' }}</el-text>
        </template>
        <template #expectedOutput="{ row }">
          <el-text truncated>{{ row.expectedOutput || '(空)' }}</el-text>
        </template>
        <template #isSample="{ row }">
          <CnStatusTag :type="row.isSample ? 'success' : 'info'" size="sm">{{ row.isSample ? '是' : '否' }}</CnStatusTag>
        </template>
        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" size="small" link @click="openTestCaseDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDeleteTestCase(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <CnSection v-if="isEdit" title="标准答案管理" description="维护题目的参考答案和题解。" divided>
      <template #actions>
        <el-button type="primary" size="small" :icon="Plus" @click="openSolutionDialog(null)">添加答案</el-button>
      </template>

      <CnDataTable
        :columns="solutionColumns"
        :data="solutions"
        :loading="solutionsLoading"
        :pagination="null"
        row-key="id"
        empty-title="暂无标准答案"
        empty-description="还没有为该题目配置标准答案。"
        empty-icon="SA"
      >
        <template #codePreview="{ row }">
          <el-text truncated>{{ row.code?.substring(0, 80) || '' }}...</el-text>
        </template>
        <template #actions="{ row }">
          <div class="table-actions">
            <el-button type="primary" size="small" link @click="openSolutionDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDeleteSolution(row)">删除</el-button>
          </div>
        </template>
      </CnDataTable>
    </CnSection>

    <el-dialog
      v-model="testCaseDialogVisible"
      :title="editingTestCase?.id ? '编辑测试用例' : '添加测试用例'"
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
        <el-button type="primary" :loading="testCaseSaving" @click="handleSaveTestCase">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="solutionDialogVisible"
      :title="editingSolution?.id ? '编辑标准答案' : '添加标准答案'"
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
              <el-select v-model="solutionForm.language" class="full-width">
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
        <el-button type="primary" :loading="solutionSaving" @click="handleSaveSolution">确定</el-button>
      </template>
    </el-dialog>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'
import { CnDataTable, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTableColumn, CnTone } from '@/design-system'

interface OjTag {
  id: number
  name: string
}

interface ProblemForm {
  title: string
  description: string
  difficulty: string
  timeLimit: number
  memoryLimit: number
  inputDescription: string
  outputDescription: string
  sampleInput: string
  sampleOutput: string
  status: number
}

interface TestCase extends Record<string, unknown> {
  id?: number
  input?: string
  expectedOutput?: string
  sortOrder?: number
  isSample?: boolean
}

interface Solution extends Record<string, unknown> {
  id?: number
  title?: string
  language?: string
  code?: string
  description?: string
  sortOrder?: number
}

const route = useRoute()
const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: 'OJ 管理' }, { label: '题目编辑' }]

const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const problemId = computed(() => route.params.id as string | undefined)

const form = reactive<ProblemForm>({
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

const selectedTagIds = ref<number[]>([])
const allTags = ref<OjTag[]>([])

const rules: FormRules<ProblemForm> = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
}

const testCases = ref<TestCase[]>([])
const testCasesLoading = ref(false)
const testCaseDialogVisible = ref(false)
const testCaseSaving = ref(false)
const editingTestCase = ref<TestCase | null>(null)
const testCaseForm = reactive({
  input: '',
  expectedOutput: '',
  sortOrder: 0,
  isSample: false
})

const solutions = ref<Solution[]>([])
const solutionsLoading = ref(false)
const solutionDialogVisible = ref(false)
const solutionSaving = ref(false)
const editingSolution = ref<Solution | null>(null)
const solutionForm = reactive({
  title: '',
  language: 'java',
  code: '',
  description: '',
  sortOrder: 0
})

const testCaseColumns: CnTableColumn<TestCase>[] = [
  { type: 'index', label: '#', width: 60 },
  { prop: 'input', label: '输入数据', minWidth: 220, slot: 'input' },
  { prop: 'expectedOutput', label: '期望输出', minWidth: 220, slot: 'expectedOutput' },
  { prop: 'isSample', label: '示例', width: 90, align: 'center', slot: 'isSample' },
  { label: '操作', width: 160, fixed: 'right', slot: 'actions' }
]

const solutionColumns: CnTableColumn<Solution>[] = [
  { type: 'index', label: '#', width: 60 },
  { prop: 'title', label: '标题', minWidth: 160 },
  { prop: 'language', label: '语言', width: 120 },
  { label: '代码预览', minWidth: 240, slot: 'codePreview' },
  { label: '操作', width: 160, fixed: 'right', slot: 'actions' }
]

const getDifficultyTone = (difficulty?: string): CnTone => {
  return ({ easy: 'success', medium: 'warning', hard: 'danger' } as Record<string, CnTone>)[difficulty || ''] || 'info'
}

const getDifficultyLabel = (difficulty?: string) => {
  return ({ easy: '简单', medium: '中等', hard: '困难' } as Record<string, string>)[difficulty || ''] || difficulty || '未知'
}

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
    selectedTagIds.value = (data.tags || []).map((tag: OjTag) => tag.id)
  } catch {
    ElMessage.error('加载题目失败')
    router.push('/oj/problems')
  }
}

const loadTags = async () => {
  try {
    allTags.value = (await ojApi.getTags()) || []
  } catch {
    console.error('加载标签失败')
  }
}

const loadTestCases = async () => {
  if (!isEdit.value) return
  testCasesLoading.value = true
  try {
    testCases.value = (await ojApi.getTestCasesByProblem(problemId.value)) || []
  } catch {
    console.error('加载测试用例失败')
  } finally {
    testCasesLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
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

const openTestCaseDialog = (testCase: TestCase | null) => {
  editingTestCase.value = testCase
  if (testCase) {
    Object.assign(testCaseForm, {
      input: testCase.input || '',
      expectedOutput: testCase.expectedOutput || '',
      sortOrder: testCase.sortOrder || 0,
      isSample: Boolean(testCase.isSample)
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

const handleDeleteTestCase = (testCase: TestCase) => {
  ElMessageBox.confirm('确定删除该测试用例吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await ojApi.deleteTestCase(testCase.id)
      ElMessage.success('删除成功')
      loadTestCases()
    })
    .catch(() => {})
}

const loadSolutions = async () => {
  if (!isEdit.value) return
  solutionsLoading.value = true
  try {
    solutions.value = (await ojApi.getSolutionsByProblem(problemId.value)) || []
  } catch {
    console.error('加载标准答案失败')
  } finally {
    solutionsLoading.value = false
  }
}

const openSolutionDialog = (solution: Solution | null) => {
  editingSolution.value = solution
  if (solution) {
    Object.assign(solutionForm, {
      title: solution.title || '',
      language: solution.language || 'java',
      code: solution.code || '',
      description: solution.description || '',
      sortOrder: solution.sortOrder || 0
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

const handleDeleteSolution = (solution: Solution) => {
  ElMessageBox.confirm('确定删除该标准答案吗？', '确认删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      await ojApi.deleteSolution(solution.id)
      ElMessage.success('删除成功')
      loadSolutions()
    })
    .catch(() => {})
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
.problem-edit-page {
  min-height: 100%;
}

.full-width {
  width: 100%;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
}
</style>
