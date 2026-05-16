<template>
  <div class="codepen-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-header">
      <div class="header-left">
        <el-button 
          type="primary" 
          icon="Back" 
          @click="goBack"
        >
          返回
        </el-button>
        <el-input
          v-model="penData.title"
          placeholder="请输入作品标题"
          class="title-input"
          maxlength="100"
          show-word-limit
        />
      </div>
      <div class="header-right">
        <el-button 
          icon="Document" 
          @click="saveDraft"
          :loading="saving"
        >
          保存草稿
        </el-button>
        <el-button 
          type="success" 
          icon="Upload" 
          @click="publish"
          :loading="publishing"
        >
          发布作品
        </el-button>
        <el-button 
          icon="Setting" 
          @click="showSettings = true"
        >
          设置
        </el-button>
      </div>
    </div>

    <!-- 编辑器主体 -->
    <div class="editor-body">
      <!-- 代码编辑区 -->
      <div class="code-panels">
        <!-- HTML编辑器 -->
        <div class="code-panel">
          <div class="panel-header">
            <span class="panel-title">
              <el-icon><Document /></el-icon>
              HTML
            </span>
            <el-button 
              text 
              size="small" 
              icon="Refresh"
              @click="runCode"
            >
              运行
            </el-button>
          </div>
          <textarea
            v-model="penData.htmlCode"
            class="code-textarea"
            placeholder="在这里编写HTML代码..."
            spellcheck="false"
            @input="autoRunCode"
          ></textarea>
        </div>

        <!-- CSS编辑器 -->
        <div class="code-panel">
          <div class="panel-header">
            <span class="panel-title">
              <el-icon><Brush /></el-icon>
              CSS
            </span>
          </div>
          <textarea
            v-model="penData.cssCode"
            class="code-textarea"
            placeholder="在这里编写CSS代码..."
            spellcheck="false"
            @input="autoRunCode"
          ></textarea>
        </div>

        <!-- JavaScript编辑器 -->
        <div class="code-panel">
          <div class="panel-header">
            <span class="panel-title">
              <el-icon><Lightning /></el-icon>
              JavaScript
            </span>
          </div>
          <textarea
            v-model="penData.jsCode"
            class="code-textarea"
            placeholder="在这里编写JavaScript代码..."
            spellcheck="false"
            @input="autoRunCode"
          ></textarea>
        </div>
      </div>

      <!-- 预览区 -->
      <div class="preview-panel">
        <div class="panel-header">
          <span class="panel-title">
            <el-icon><View /></el-icon>
            预览
          </span>
          <el-button 
            text 
            size="small" 
            icon="FullScreen"
            @click="fullscreenPreview = true"
          >
            全屏
          </el-button>
        </div>
        <iframe
          ref="previewFrame"
          class="preview-iframe"
          sandbox="allow-scripts allow-same-origin"
        ></iframe>
      </div>
    </div>

    <!-- 设置对话框 -->
    <el-dialog
      v-model="showSettings"
      title="作品设置"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="penData" label-width="100px">
        <el-form-item label="作品标题">
          <el-input
            v-model="penData.title"
            placeholder="请输入作品标题"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="作品描述">
          <el-input
            v-model="penData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入作品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-select
            v-model="penData.tags"
            multiple
            filterable
            allow-create
            placeholder="选择或输入标签（最多5个）"
            style="width: 100%"
            :max-collapse-tags="3"
          >
            <el-option
              v-for="tag in allTags"
              :key="tag.tagName"
              :label="tag.tagName"
              :value="tag.tagName"
            />
          </el-select>
          <div class="form-tip">最多选择5个标签</div>
        </el-form-item>

        <el-form-item label="分类">
          <el-select
            v-model="penData.category"
            placeholder="选择分类"
            style="width: 100%"
          >
            <el-option label="动画特效" value="动画" />
            <el-option label="组件库" value="组件" />
            <el-option label="游戏" value="游戏" />
            <el-option label="工具" value="工具" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="可见性">
          <el-radio-group v-model="penData.isPublic">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="作品类型">
          <el-radio-group v-model="penData.isFree">
            <el-radio :label="1">免费（源码公开，可免费Fork）</el-radio>
            <el-radio :label="0">付费（源码隐藏，Fork需付费）</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Fork价格" v-if="penData.isFree === 0">
          <el-input-number
            v-model="penData.forkPrice"
            :min="1"
            :max="1000"
            placeholder="设置Fork价格"
            style="width: 200px"
          />
          <span class="form-tip" style="margin-left: 10px">积分（1-1000）</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="showSettings = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 全屏预览 -->
    <el-dialog
      v-model="fullscreenPreview"
      title="全屏预览"
      width="90%"
      :close-on-click-modal="false"
      fullscreen
    >
      <iframe
        ref="fullscreenFrame"
        class="fullscreen-iframe"
        sandbox="allow-scripts allow-same-origin"
      ></iframe>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { codepenApi } from '@/api/codepen'
import { Document, Brush, Lightning, View } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 作品数据
const penData = reactive({
  id: null,
  title: '',
  description: '',
  htmlCode: '',
  cssCode: '',
  jsCode: '',
  tags: [],
  category: '',
  isPublic: 1,
  isFree: 1,
  forkPrice: 0,
  status: 0 // 0-草稿 1-已发布
})

// 页面状态
const saving = ref(false)
const publishing = ref(false)
const showSettings = ref(false)
const fullscreenPreview = ref(false)
const allTags = ref([])
const previewFrame = ref(null)
const fullscreenFrame = ref(null)
const autoRunTimer = ref(null)

// 返回
const goBack = () => {
  ElMessageBox.confirm(
    '确定要离开吗？未保存的更改将丢失。',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(() => {
      router.back()
    })
    .catch(() => {})
}

// 保存草稿
const saveDraft = async () => {
  if (!validatePen()) return

  try {
    saving.value = true
    penData.status = 0 // 草稿状态
    
    const response = penData.id 
      ? await codepenApi.updatePen(penData)
      : await codepenApi.savePen(penData)
    
    if (!penData.id && response.penId) {
      penData.id = response.penId
    }
    
    ElMessage.success('草稿保存成功')
  } catch (error) {
    console.error('保存草稿失败:', error)
  } finally {
    saving.value = false
  }
}

// 发布作品
const publish = async () => {
  if (!validatePen()) return

  try {
    publishing.value = true
    penData.status = 1 // 发布状态
    
    const response = penData.id 
      ? await codepenApi.updatePen(penData)
      : await codepenApi.createPen(penData)
    
    if (response.pointsAdded) {
      ElMessage.success(`作品发布成功！奖励 ${response.pointsAdded} 积分`)
    } else {
      ElMessage.success('作品发布成功')
    }
    
    // 跳转到代码广场
    router.push('/codepen')
  } catch (error) {
    console.error('发布作品失败:', error)
    penData.status = 0 // 恢复草稿状态
  } finally {
    publishing.value = false
  }
}

// 验证作品
const validatePen = () => {
  if (!penData.title || penData.title.trim() === '') {
    ElMessage.warning('请输入作品标题')
    return false
  }

  if (!penData.htmlCode && !penData.cssCode && !penData.jsCode) {
    ElMessage.warning('至少需要编写一种代码（HTML/CSS/JS）')
    return false
  }

  if (penData.tags.length > 5) {
    ElMessage.warning('最多只能选择5个标签')
    return false
  }

  if (penData.isFree === 0 && (!penData.forkPrice || penData.forkPrice < 1 || penData.forkPrice > 1000)) {
    ElMessage.warning('Fork价格范围为1-1000积分')
    return false
  }

  return true
}

// 运行代码
const runCode = () => {
  const html = penData.htmlCode || ''
  const css = penData.cssCode || ''
  const js = penData.jsCode || ''
  const scriptCloseTag = '</scr' + 'ipt>'

  const content = `
    <!DOCTYPE html>
    <html>
      <head>
        <style>${css}</style>
      </head>
      <body>
        ${html}
        <script>${js}${scriptCloseTag}
      </body>
    </html>
  `

  if (previewFrame.value) {
    const iframe = previewFrame.value
    iframe.srcdoc = content
  }

  if (fullscreenPreview.value && fullscreenFrame.value) {
    const iframe = fullscreenFrame.value
    iframe.srcdoc = content
  }
}

// 自动运行代码（防抖）
const autoRunCode = () => {
  if (autoRunTimer.value) {
    clearTimeout(autoRunTimer.value)
  }
  autoRunTimer.value = setTimeout(() => {
    runCode()
  }, 500)
}

// 加载标签
const loadTags = async () => {
  try {
    allTags.value = await codepenApi.getHotTags()
  } catch (error) {
    console.error('加载标签失败:', error)
  }
}

// 加载作品数据（编辑模式）
const loadPenData = async () => {
  const penId = route.params.id
  if (!penId) return

  try {
    const data = await codepenApi.getPenDetail(penId)
    Object.assign(penData, {
      id: data.id,
      title: data.title,
      description: data.description,
      htmlCode: data.htmlCode || '',
      cssCode: data.cssCode || '',
      jsCode: data.jsCode || '',
      tags: data.tags || [],
      category: data.category,
      isPublic: data.isPublic,
      isFree: data.isFree ? 1 : 0,
      forkPrice: data.forkPrice || 0,
      status: data.status
    })

    runCode()
  } catch (error) {
    console.error('加载作品失败:', error)
    ElMessage.error('加载作品失败')
  }
}

// 从模板创建
const loadTemplate = async () => {
  const templateId = route.query.template
  if (!templateId) return

  try {
    const data = await codepenApi.getTemplateDetail(templateId)
    penData.htmlCode = data.htmlCode || ''
    penData.cssCode = data.cssCode || ''
    penData.jsCode = data.jsCode || ''
    penData.title = `基于模板：${data.title}`
    
    runCode()
  } catch (error) {
    console.error('加载模板失败:', error)
  }
}

// 监听全屏预览打开
watch(fullscreenPreview, (val) => {
  if (val) {
    setTimeout(() => {
      runCode()
    }, 100)
  }
})

// 初始化
onMounted(() => {
  loadTags()
  
  // 如果是编辑模式
  if (route.params.id) {
    loadPenData()
  }
  // 如果是从模板创建
  else if (route.query.template) {
    loadTemplate()
  }
  // 新建空白作品
  else {
    runCode()
  }
})

// 清理定时器
onBeforeUnmount(() => {
  if (autoRunTimer.value) {
    clearTimeout(autoRunTimer.value)
  }
})
</script>

<style scoped lang="scss">
.codepen-editor {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;

  .editor-header {
    height: 60px;
    background: #2d2d2d;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    border-bottom: 1px solid #3d3d3d;

    .header-left {
      display: flex;
      align-items: center;
      gap: 15px;
      flex: 1;

      .title-input {
        max-width: 400px;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
  }

  .editor-body {
    flex: 1;
    display: flex;
    overflow: hidden;

    .code-panels {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      .code-panel {
        flex: 1;
        display: flex;
        flex-direction: column;
        border-right: 1px solid #3d3d3d;
        border-bottom: 1px solid #3d3d3d;

        .panel-header {
          height: 40px;
          background: #252526;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 15px;
          border-bottom: 1px solid #3d3d3d;

          .panel-title {
            display: flex;
            align-items: center;
            gap: 8px;
            color: #d4d4d4;
            font-size: 14px;
            font-weight: 500;
          }
        }

        .code-textarea {
          flex: 1;
          width: 100%;
          padding: 15px;
          background: #1e1e1e;
          color: #d4d4d4;
          border: none;
          resize: none;
          font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
          font-size: 14px;
          line-height: 1.6;

          &:focus {
            outline: none;
          }

          &::placeholder {
            color: #666;
          }
        }
      }
    }

    .preview-panel {
      flex: 1;
      display: flex;
      flex-direction: column;
      background: #fff;

      .panel-header {
        height: 40px;
        background: #f5f5f5;
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0 15px;
        border-bottom: 1px solid #ddd;

        .panel-title {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #333;
          font-size: 14px;
          font-weight: 500;
        }
      }

      .preview-iframe {
        flex: 1;
        width: 100%;
        border: none;
        background: #fff;
      }
    }
  }

  .form-tip {
    font-size: 12px;
    color: #999;
    margin-top: 5px;
  }

  .fullscreen-iframe {
    width: 100%;
    height: calc(100vh - 120px);
    border: none;
  }
}

:deep(.el-dialog) {
  .el-dialog__body {
    padding: 20px;
  }
}
</style>

