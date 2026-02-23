<template>
  <div class="contest-edit">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="router.push('/oj/contests')">
            <el-icon><ArrowLeft /></el-icon>
            返回列表
          </el-button>
          <h2>{{ isEdit ? '编辑赛事' : '新增赛事' }}</h2>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="赛事标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入赛事标题" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="赛事类型" prop="contestType">
              <el-select v-model="form.contestType" placeholder="请选择赛事类型" style="width: 100%">
                <el-option label="周赛" value="weekly" />
                <el-option label="挑战赛" value="challenge" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="初始状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="0">草稿</el-radio>
                <el-radio :label="1">发布</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题目数量">
              <el-input :model-value="`${form.problemIds.length} 题`" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="赛事描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="请输入赛事介绍、规则说明等"
          />
        </el-form-item>

        <el-form-item label="题目编排" prop="problemIds">
          <el-select
            v-model="form.problemIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择赛事题目"
            style="width: 100%"
          >
            <el-option
              v-for="problem in problemOptions"
              :key="problem.id"
              :label="`${problem.id}. ${problem.title}`"
              :value="problem.id"
            >
              <div class="problem-option">
                <span class="problem-title">{{ problem.id }}. {{ problem.title }}</span>
                <el-tag size="small" :type="getDifficultyTag(problem.difficulty)">
                  {{ getDifficultyLabel(problem.difficulty) }}
                </el-tag>
              </div>
            </el-option>
          </el-select>
          <div class="tips">仅展示当前公开题目，如需更多题目请先在题目管理中设置状态。</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建赛事' }}
          </el-button>
          <el-button @click="router.push('/oj/contests')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ojApi } from '@/api/oj'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const problemOptions = ref([])

const contestId = computed(() => route.params.id)
const isEdit = computed(() => !!contestId.value)

const form = reactive({
  title: '',
  contestType: 'weekly',
  startTime: '',
  endTime: '',
  status: 0,
  description: '',
  problemIds: []
})

const validateEndTime = (_rule, value, callback) => {
  if (!value || !form.startTime) {
    callback()
    return
  }
  const start = new Date(form.startTime).getTime()
  const end = new Date(value).getTime()
  if (Number.isNaN(start) || Number.isNaN(end) || end <= start) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  callback()
}

const rules = {
  title: [{ required: true, message: '请输入赛事标题', trigger: 'blur' }],
  contestType: [{ required: true, message: '请选择赛事类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: validateEndTime, trigger: 'change' }
  ],
  problemIds: [{ type: 'array', required: true, min: 1, message: '请至少选择 1 道题目', trigger: 'change' }]
}

const getDifficultyTag = (d) => ({ easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info')
const getDifficultyLabel = (d) => ({ easy: '简单', medium: '中等', hard: '困难' }[d] || (d || '未知'))

const normalizeDateTime = (value) => {
  if (!value) return ''
  if (typeof value !== 'string') return value
  return value.replace('T', ' ').slice(0, 19)
}

const normalizeContestType = (value) => {
  if (value === 1 || value === '1' || value === 'weekly') return 'weekly'
  if (value === 2 || value === '2' || value === 'challenge') return 'challenge'
  return value || 'weekly'
}

const resolveProblemListData = (data) => {
  if (Array.isArray(data)) return data
  if (!data || typeof data !== 'object') return []
  return data.records || data.list || []
}

const loadProblemOptions = async () => {
  try {
    const data = await ojApi.getProblemList({
      pageNum: 1,
      pageSize: 500,
      status: 1
    })
    const list = resolveProblemListData(data)
    problemOptions.value = list.map((item) => ({
      id: item.id,
      title: item.title || '',
      difficulty: item.difficulty || ''
    }))
  } catch (error) {
    console.error('加载题目选项失败:', error)
  }
}

const resolveContestProblemIds = (data) => {
  if (Array.isArray(data.problemIds)) return data.problemIds
  if (Array.isArray(data.problemIdList)) return data.problemIdList
  if (Array.isArray(data.problems)) return data.problems.map((item) => item.id).filter(Boolean)
  return []
}

const loadContestDetail = async () => {
  try {
    const data = await ojApi.getContestDetail(contestId.value)
    Object.assign(form, {
      title: data.title || data.name || '',
      contestType: normalizeContestType(data.contestType || data.type),
      startTime: normalizeDateTime(data.startTime || data.beginTime || ''),
      endTime: normalizeDateTime(data.endTime || data.finishTime || ''),
      status: Number(data.status ?? 0),
      description: data.description || '',
      problemIds: resolveContestProblemIds(data)
    })
  } catch (error) {
    ElMessage.error('加载赛事详情失败')
    router.push('/oj/contests')
  }
}

const buildPayload = () => {
  return {
    title: form.title,
    contestType: form.contestType,
    startTime: form.startTime,
    endTime: form.endTime,
    status: form.status,
    description: form.description,
    problemIds: [...form.problemIds]
  }
}

const extractCreatedContestId = (data) => {
  if (typeof data === 'number') return data
  if (typeof data === 'string' && /^\d+$/.test(data)) return Number(data)
  if (data && typeof data === 'object') {
    if (typeof data.id === 'number') return data.id
    if (typeof data.contestId === 'number') return data.contestId
  }
  return null
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value) {
      await ojApi.updateContest(contestId.value, payload)
      ElMessage.success('修改成功')
      router.push('/oj/contests')
      return
    }
    const created = await ojApi.createContest(payload)
    ElMessage.success('创建成功')
    const id = extractCreatedContestId(created)
    if (id) {
      router.push(`/oj/contests/${id}/edit`)
      return
    }
    router.push('/oj/contests')
  } catch (error) {
    console.error('保存赛事失败:', error)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadProblemOptions()
  if (isEdit.value) {
    await loadContestDetail()
  }
})
</script>

<style scoped>
.contest-edit {
  display: grid;
  gap: 16px;
}

.main-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
}

.problem-option {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.problem-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tips {
  margin-top: 8px;
  font-size: 12px;
  color: var(--cn-text-tertiary);
}
</style>
