<template>
  <div class="translation-tool">
    <!-- 头部导航 -->
    <div class="tool-header">
      <div class="header-content">
        <h1 class="tool-title">
          <el-icon><ChatLineSquare /></el-icon>
          聚合翻译工具
        </h1>
        <p class="tool-description">同时展示多个翻译平台的结果，快速对比选择最佳翻译</p>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-section">
      <div class="input-container">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="3"
          placeholder="请输入要翻译的文本..."
          class="input-textarea"
          @input="onInputChange"
        />
        <div class="input-actions">
          <el-button size="small" text @click="pasteFromClipboard">
            <el-icon><CopyDocument /></el-icon>
            粘贴
          </el-button>
          <el-button size="small" text @click="clearInput">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          <el-button size="small" text @click="loadExample">
            示例
          </el-button>
        </div>
      </div>
    </div>

    <!-- 翻译结果区域 -->
    <div class="translation-results" v-if="inputText.trim()">
      <div class="single-translation">
        <!-- 百度翻译 -->
        <div class="translation-frame baidu-frame">
          <div class="frame-header">
            <div class="platform-info">
              <div class="platform-icon baidu">百</div>
              <span class="platform-name">百度翻译</span>
            </div>
            <el-button size="small" text @click="openBaiduTranslation">
              <el-icon><Link /></el-icon>
              新窗口打开
            </el-button>
          </div>
          <div class="frame-content">
            <iframe
              :src="getBaiduUrl()"
              frameborder="0"
              class="translation-iframe"
              @load="onFrameLoad('baidu')"
            ></iframe>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-icon class="empty-icon"><ChatLineSquare /></el-icon>
      <h3>百度翻译工具</h3>
      <p class="empty-text">请在上方输入要翻译的文本，将显示百度翻译的结果</p>
      <div class="platform-preview">
        <div class="preview-item">
          <div class="platform-icon baidu">百</div>
          <span>百度翻译</span>
        </div>
      </div>
    </div>

    <!-- 使用提示 -->
    <div class="usage-tips">
      <el-card>
        <template #header>
          <div class="card-header">
            <el-icon><InfoFilled /></el-icon>
            使用提示
          </div>
        </template>
        <div class="tips-content">
          <div class="tip-item">
            <el-icon><Check /></el-icon>
            <span>输入文本后将直接展示百度翻译的结果</span>
          </div>
          <div class="tip-item">
            <el-icon><Check /></el-icon>
            <span>可以点击"新窗口打开"在独立窗口中使用百度翻译</span>
          </div>
          <div class="tip-item">
            <el-icon><Check /></el-icon>
            <span>支持中英文互译，以及其他多种语言翻译</span>
          </div>
          <div class="tip-item">
            <el-icon><Check /></el-icon>
            <span>翻译服务来自百度翻译官方网站，保证准确性</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ChatLineSquare, CopyDocument, Delete, Link, InfoFilled, Check
} from '@element-plus/icons-vue'

// 响应式数据
const inputText = ref('')

// 输入变化处理
const onInputChange = () => {
  // 输入改变时的处理逻辑
}

// 获取百度翻译URL
const getBaiduUrl = () => {
  const text = encodeURIComponent(inputText.value)
  return `https://fanyi.baidu.com/mtpe-individual/transText?query=${text}&lang=auto2zh`
}


// 打开百度翻译
const openBaiduTranslation = () => {
  const url = getBaiduUrl()
  window.open(url, '_blank')
  ElMessage.success('已在新窗口打开百度翻译')
}


// iframe加载完成处理
const onFrameLoad = (platform) => {
  console.log(`${platform} iframe loaded`)
}

// 从剪贴板粘贴
const pasteFromClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    inputText.value = text
    ElMessage.success('已从剪贴板粘贴')
  } catch (error) {
    ElMessage.error('粘贴失败，请手动粘贴')
  }
}

// 清空输入
const clearInput = () => {
  inputText.value = ''
}

// 加载示例文本
const loadExample = () => {
  const examples = [
    'Hello World',
    '你好世界',
    'function',
    '编程',
    'artificial intelligence',
    '人工智能'
  ]
  inputText.value = examples[Math.floor(Math.random() * examples.length)]
  ElMessage.success('示例文本已加载')
}
</script>

<style scoped>
.translation-tool {
  min-height: 100vh;
  background: #f8fafc;
}

/* 头部区域 */
.tool-header {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
  padding: 30px 20px;
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
}


.tool-title {
  font-size: 2rem;
  font-weight: 700;
  margin: 16px 0 8px 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-description {
  opacity: 0.9;
  font-size: 1.1rem;
  margin: 0;
}

/* 输入区域 */
.input-section {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.input-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.input-textarea {
  border: none;
}

:deep(.input-textarea .el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 20px;
  font-size: 16px;
  line-height: 1.6;
  resize: none;
}

.input-actions {
  padding: 12px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
}

/* 翻译结果区域 */
.translation-results {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px 20px;
}

.single-translation {
  max-width: 1000px;
  margin: 0 auto;
}

.translation-frame {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  height: 600px;
  display: flex;
  flex-direction: column;
}

.frame-header {
  padding: 16px 20px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.platform-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.platform-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 0.875rem;
}

.baidu .platform-icon {
  background: #3385ff;
}



.platform-name {
  font-weight: 600;
  color: #374151;
}

.frame-content {
  flex: 1;
  position: relative;
}

.translation-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #6b7280;
  max-width: 1400px;
  margin: 0 auto;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 1.5rem;
  color: #374151;
  margin: 0 0 12px 0;
}

.empty-text {
  font-size: 1.1rem;
  margin: 0 0 32px 0;
}

.platform-preview {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.preview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border-radius: 12px;
  background: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  min-width: 100px;
}

.preview-item span {
  font-size: 0.875rem;
  color: #6b7280;
}

/* 使用提示 */
.usage-tips {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.tips-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.tip-item .el-icon {
  color: #10b981;
  flex-shrink: 0;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .translation-frame {
    height: 500px;
  }
  
  .platform-preview {
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .tool-header {
    padding: 20px 15px;
  }
  
  .tool-title {
    font-size: 1.5rem;
  }
  
  .input-section,
  .translation-results,
  .usage-tips {
    padding-left: 15px;
    padding-right: 15px;
  }
  
  .results-grid {
    gap: 15px;
  }
  
  .translation-frame {
    height: 500px;
  }
  
  .platform-preview {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }
  
  .preview-item {
    flex-direction: row;
    min-width: auto;
    width: 200px;
  }
}

@media (max-width: 480px) {
  .input-actions {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .translation-frame {
    height: 400px;
  }
  
  :deep(.input-textarea .el-textarea__inner) {
    padding: 15px;
    font-size: 14px;
  }
}
</style>
