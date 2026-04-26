<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    title="打卡"
    width="500px"
    :close-on-click-modal="false"
  >
    <div class="checkin-dialog">
      <!-- 任务信息 -->
      <div class="task-info" v-if="task">
        <div class="task-name">
          <el-icon><Calendar /></el-icon>
          {{ task.taskName }}
        </div>
        <div class="task-desc" v-if="task.taskDesc">{{ task.taskDesc }}</div>
      </div>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="打卡内容" prop="checkinContent">
          <el-input 
            v-model="form.checkinContent" 
            type="textarea"
            :rows="4"
            placeholder="记录你今天的学习/完成情况..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="学习时长（分钟）" prop="duration">
          <el-input-number 
            v-model="form.duration" 
            :min="1" 
            :max="1440"
            placeholder="可选"
          />
          <span class="form-tip">选填，记录你本次学习的时长</span>
        </el-form-item>
        
        <el-form-item label="图片（可选）">
          <div class="image-upload">
            <div 
              v-for="(img, index) in imageList" 
              :key="index" 
              class="image-item"
            >
              <img :src="img" />
              <div class="image-delete" @click="removeImage(index)">
                <el-icon><Close /></el-icon>
              </div>
            </div>
            <div 
              v-if="imageList.length < 3" 
              class="image-add"
              @click="openImageInput"
            >
              <el-icon><Plus /></el-icon>
            </div>
          </div>
          <input 
            ref="imageInput"
            type="file" 
            accept="image/*" 
            style="display: none"
            @change="handleImageSelect"
          />
        </el-form-item>
        
        <!-- 补卡选项 -->
        <el-form-item v-if="allowSupplement">
          <el-checkbox v-model="isSupplement">补卡（选择补卡日期）</el-checkbox>
          <el-date-picker
            v-if="isSupplement"
            v-model="form.checkinDate"
            type="date"
            placeholder="选择补卡日期"
            :disabled-date="disabledDate"
            value-format="YYYY-MM-DD"
            style="margin-left: 12px;"
          />
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        完成打卡
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar, Close, Plus } from '@element-plus/icons-vue'
import teamApi from '@/api/team'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  teamId: { type: [String, Number], required: true },
  task: { type: Object, default: null },
  allowSupplement: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const imageInput = ref()
const submitting = ref(false)
const imageList = ref([])
const isSupplement = ref(false)

const form = ref({
  checkinContent: '',
  duration: null,
  images: '',
  checkinDate: null
})

const rules = {
  checkinContent: [
    { required: true, message: '请输入打卡内容', trigger: 'blur' }
  ]
}

// 监听弹窗打开
watch(() => props.modelValue, (val) => {
  if (val) {
    resetForm()
  }
})

const resetForm = () => {
  form.value = {
    checkinContent: '',
    duration: null,
    images: '',
    checkinDate: null
  }
  imageList.value = []
  isSupplement.value = false
  formRef.value?.clearValidate()
}

// 禁用未来日期和7天前的日期
const disabledDate = (time) => {
  const now = new Date()
  now.setHours(23, 59, 59, 999)
  const weekAgo = new Date()
  weekAgo.setDate(weekAgo.getDate() - 7)
  weekAgo.setHours(0, 0, 0, 0)
  return time.getTime() > now.getTime() || time.getTime() < weekAgo.getTime()
}

const openImageInput = () => {
  imageInput.value?.click()
}

const handleImageSelect = (e) => {
  const file = e.target.files[0]
  if (!file) return
  
  // 简单预览，实际项目需要上传到服务器
  const reader = new FileReader()
  reader.onload = (event) => {
    imageList.value.push(event.target.result)
  }
  reader.readAsDataURL(file)
  
  // 清空input以便重复选择同一文件
  e.target.value = ''
}

const removeImage = (index) => {
  imageList.value.splice(index, 1)
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    const data = {
      ...form.value,
      taskId: props.task?.id,
      images: imageList.value.join(',')
    }
    
    if (isSupplement.value && form.value.checkinDate) {
      // 补卡
      await teamApi.supplementCheckin(props.teamId, data)
      ElMessage.success('补卡成功')
    } else {
      // 正常打卡
      await teamApi.checkin(props.teamId, data)
      ElMessage.success('打卡成功！')
    }
    
    emit('update:modelValue', false)
    emit('success')
  } catch (error) {
    console.error('打卡失败:', error)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.checkin-dialog {
  .task-info {
    padding: 12px 16px;
    background: #f8f9fc;
    border-radius: 8px;
    margin-bottom: 20px;
    
    .task-name {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 500;
      color: #333;
      
      .el-icon {
        color: #409eff;
      }
    }
    
    .task-desc {
      margin-top: 6px;
      font-size: 13px;
      color: #999;
      padding-left: 22px;
    }
  }
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}

.image-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  
  .image-item, .image-add {
    width: 80px;
    height: 80px;
    border-radius: 8px;
    overflow: hidden;
  }
  
  .image-item {
    position: relative;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .image-delete {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 20px;
      height: 20px;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      
      .el-icon {
        color: white;
        font-size: 12px;
      }
      
      &:hover {
        background: rgba(0, 0, 0, 0.7);
      }
    }
  }
  
  .image-add {
    border: 1px dashed #dcdfe6;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s;
    
    .el-icon {
      font-size: 24px;
      color: #c0c4cc;
    }
    
    &:hover {
      border-color: #409eff;
      
      .el-icon {
        color: #409eff;
      }
    }
  }
}
</style>
