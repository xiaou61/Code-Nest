<template>
  <CnPage class="translation-page" max-width="1180px" full-height>
    <CnPageHeader
      title="聚合翻译工具"
      description="输入文本后直接展示百度翻译结果，也可以在新窗口中打开官方翻译页面。"
      eyebrow="DEV TOOL"
      :breadcrumbs="[{ label: '开发工具', to: '/dev-tools' }, { label: '聚合翻译' }]"
    >
      <template #meta>
        <CnStatusTag :type="inputText.trim() ? 'success' : 'info'" size="sm">
          {{ inputText.trim() ? '已输入文本' : '待输入' }}
        </CnStatusTag>
        <CnStatusTag type="brand" size="sm" subtle>百度翻译</CnStatusTag>
      </template>
    </CnPageHeader>

    <CnSection title="翻译文本" description="输入或粘贴要翻译的文本，结果区域会自动刷新为百度翻译页面。" divided>
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="4"
        placeholder="请输入要翻译的文本..."
        class="input-textarea"
        @input="onInputChange"
      />

      <div class="input-actions">
        <el-button size="small" text :icon="CopyDocument" @click="pasteFromClipboard">粘贴</el-button>
        <el-button size="small" text :icon="Delete" @click="clearInput">清空</el-button>
        <el-button size="small" text @click="loadExample">示例</el-button>
      </div>
    </CnSection>

    <CnSection v-if="inputText.trim()" title="翻译结果" description="结果来自百度翻译官方页面。" divided>
      <template #actions>
        <el-button size="small" text :icon="Link" @click="openBaiduTranslation">新窗口打开</el-button>
      </template>

      <div class="translation-frame">
        <div class="frame-header">
          <div class="platform-info">
            <span class="platform-icon">百</span>
            <span class="platform-name">百度翻译</span>
          </div>
          <CnStatusTag type="info" size="sm" subtle>iframe</CnStatusTag>
        </div>
        <div class="frame-content">
          <iframe
            :src="baiduUrl"
            frameborder="0"
            class="translation-iframe"
          />
        </div>
      </div>
    </CnSection>

    <CnEmptyState
      v-else
      title="等待输入"
      description="请在上方输入要翻译的文本，将显示百度翻译的结果。"
      icon="TR"
      surface="panel"
    >
      <template #actions>
        <el-button type="primary" @click="loadExample">加载示例</el-button>
      </template>
    </CnEmptyState>

    <CnSection title="使用提示" description="翻译页面由第三方站点提供，展示效果取决于目标站点 iframe 策略和网络状态。" divided>
      <div class="tips-content">
        <div class="tip-item">
          <CnStatusTag type="success" size="sm">1</CnStatusTag>
          <span>输入文本后将直接展示百度翻译的结果。</span>
        </div>
        <div class="tip-item">
          <CnStatusTag type="success" size="sm">2</CnStatusTag>
          <span>可以点击新窗口打开，在独立页面中使用百度翻译。</span>
        </div>
        <div class="tip-item">
          <CnStatusTag type="success" size="sm">3</CnStatusTag>
          <span>支持中英文互译，以及百度翻译页面支持的其他语言。</span>
        </div>
      </div>
    </CnSection>
  </CnPage>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument, Delete, Link } from '@element-plus/icons-vue'
import { CnEmptyState, CnPage, CnPageHeader, CnSection, CnStatusTag } from '@/design-system'

const inputText = ref('')

const baiduUrl = computed(() => {
  const text = encodeURIComponent(inputText.value)
  return `https://fanyi.baidu.com/mtpe-individual/transText?query=${text}&lang=auto2zh`
})

const onInputChange = () => {
  // Kept for parity with the old component and future debounce hooks.
}

const openBaiduTranslation = () => {
  window.open(baiduUrl.value, '_blank')
  ElMessage.success('已在新窗口打开百度翻译')
}

const pasteFromClipboard = async () => {
  try {
    inputText.value = await navigator.clipboard.readText()
    ElMessage.success('已从剪贴板粘贴')
  } catch {
    ElMessage.error('粘贴失败，请手动粘贴')
  }
}

const clearInput = () => {
  inputText.value = ''
}

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
.translation-page {
  min-height: calc(100vh - 68px);
}

.input-textarea {
  display: block;
}

.input-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  margin-top: var(--cn-space-3);
  padding-top: var(--cn-space-3);
  border-top: 1px solid var(--cn-color-border-subtle);
}

.translation-frame {
  display: flex;
  flex-direction: column;
  height: 620px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
}

.frame-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--cn-space-3);
  padding: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
}

.platform-info {
  display: inline-flex;
  align-items: center;
  gap: var(--cn-space-3);
  min-width: 0;
}

.platform-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-brand-primary);
  color: white;
  font-size: 14px;
  font-weight: 800;
}

.platform-name {
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 750;
}

.frame-content {
  flex: 1;
  min-height: 0;
}

.translation-iframe {
  width: 100%;
  height: 100%;
  border: 0;
  background: white;
}

.tips-content {
  display: grid;
  gap: var(--cn-space-3);
}

.tip-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: var(--cn-space-3);
  color: var(--cn-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .translation-frame {
    height: 480px;
  }

  .input-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
