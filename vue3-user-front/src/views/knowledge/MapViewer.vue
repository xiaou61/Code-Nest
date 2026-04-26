<template>
  <div class="knowledge-viewer">
    <!-- 顶部导航栏 -->
    <div class="viewer-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <div class="map-info">
          <h2 class="map-title">{{ mapInfo.title || '知识图谱' }}</h2>
          <div class="map-meta">
            <span>{{ mapInfo.nodeCount || 0 }} 个节点</span>
            <span style="margin-left: 16px;">{{ mapInfo.viewCount || 0 }} 次查看</span>
          </div>
        </div>
      </div>

      <div class="header-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索节点..."
          :prefix-icon="Search"
          style="width: 200px; margin-right: 12px;"
          @keyup.enter="handleSearch"
        />
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button-group style="margin-left: 12px;">
          <el-button :icon="ZoomOut" @click="handleZoomOut" title="缩小" />
          <el-button @click="handleResetZoom" title="适应画布">
            {{ Math.round(zoomLevel * 100) }}%
          </el-button>
          <el-button :icon="ZoomIn" @click="handleZoomIn" title="放大" />
        </el-button-group>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="viewer-content">
      <!-- 思维导图画布 -->
      <div class="mind-map-container">
        <!-- G6思维导图组件 -->
        <MindMap
          ref="mindMapRef"
          :data="mindMapData"
          :editable="false"
          :show-toolbar="false"
          :show-node-panel="false"
          height="100%"
          @node-click="handleNodeClick"
          @node-dblclick="handleNodeDblClick"
        />

        <div v-if="!nodeTree.length" class="empty-canvas">
          <el-icon size="64" color="#c0c4cc"><DataAnalysis /></el-icon>
          <p>暂无内容</p>
        </div>
      </div>

      <!-- 画布操作提示 -->
      <div class="canvas-tips-container">
        <div class="canvas-tips">
          <div class="tip-item">
            <el-icon><Mouse /></el-icon>
            <span>点击节点查看详情</span>
          </div>
          <div class="tip-item">
            <el-icon><Switch /></el-icon>
            <span>拖拽画布移动视图</span>
          </div>
          <div class="tip-item">
            <el-icon><ZoomIn /></el-icon>
            <span>滚轮缩放</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 节点详情弹窗 -->
    <el-dialog
      v-model="showNodeDetail"
      :title="selectedNode?.title || '节点详情'"
      :width="isMobile ? '95%' : '1200px'"
      :fullscreen="isMobile"
      class="node-detail-dialog"
      @close="handleCloseDetail"
    >
      <div v-if="selectedNode" class="node-content">
        <!-- 节点类型标识 -->
        <div class="node-header">
          <el-tag
            :type="getNodeTypeTagType(selectedNode.nodeType)"
            size="small"
          >
            {{ getNodeTypeText(selectedNode.nodeType) }}
          </el-tag>
          <div class="node-stats">
            <span>查看次数: {{ selectedNode.viewCount || 0 }}</span>
          </div>
        </div>

        <!-- 飞书文档内容 -->
        <div v-if="selectedNode.url" class="document-content">
          <iframe
            :src="selectedNode.url"
            frameborder="0"
            width="100%"
            :height="isMobile ? '60vh' : '800px'"
            sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
          ></iframe>
        </div>

        <!-- 空内容提示 -->
        <div v-else class="empty-content">
          <el-icon size="48" color="#c0c4cc"><Document /></el-icon>
          <p>该节点暂无文档链接</p>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <div class="navigation-buttons">
            <el-button
              :disabled="!hasPrevNode"
              @click="navigateToNode('prev')"
            >
              <el-icon><ArrowLeft /></el-icon>
              上一个节点
            </el-button>
            <el-button
              :disabled="!hasNextNode"
              @click="navigateToNode('next')"
            >
              下一个节点
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-button type="primary" @click="showNodeDetail = false">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 搜索结果面板 -->
    <el-drawer
      v-model="showSearchResults"
      title="搜索结果"
      size="400px"
      direction="rtl"
    >
      <div v-if="searchResults.length" class="search-results">
        <div
          v-for="result in searchResults"
          :key="result.id"
          class="search-result-item"
          @click="handleSelectSearchResult(result)"
        >
          <div class="result-header">
            <h4 class="result-title">{{ result.title }}</h4>
            <el-tag
              :type="getNodeTypeTagType(result.nodeType)"
              size="small"
            >
              {{ getNodeTypeText(result.nodeType) }}
            </el-tag>
          </div>
          <div class="result-content">
            {{ result.url || '暂无链接' }}
          </div>
        </div>
      </div>

      <div v-else-if="searchKeyword" class="no-search-results">
        <el-icon size="48" color="#c0c4cc"><Search /></el-icon>
        <p>未找到相关节点</p>
        <p style="color: #909399; font-size: 14px;">
          尝试使用其他关键词搜索
        </p>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Search, ZoomIn, ZoomOut, DataAnalysis,
  Mouse, Switch, Document
} from '@element-plus/icons-vue'
import {
  getKnowledgeMapById,
  getKnowledgeNodeTree,
  searchKnowledgeNodes
} from '@/api/knowledge'
import MindMap from '@/components/MindMap.vue'

const route = useRoute()
const router = useRouter()

// 基础数据
const mapId = ref(route.params.id)
const mapInfo = ref({})
const nodeTree = ref([])
const flatNodeList = ref([]) // 扁平化的节点列表，用于导航

// UI状态
const loading = ref(false)
const zoomLevel = ref(1)
const selectedNode = ref(null)
const selectedNodeIndex = ref(-1)
const showNodeDetail = ref(false)
const showSearchResults = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])

// 移动端检测
const isMobile = ref(false)

// 检测设备类型
const checkDevice = () => {
  isMobile.value = window.innerWidth <= 768 || /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

// 组件ref
const mindMapRef = ref()

const hasPrevNode = computed(() => {
  return selectedNodeIndex.value > 0
})

const hasNextNode = computed(() => {
  return selectedNodeIndex.value < flatNodeList.value.length - 1
})

// 转换为G6格式的思维导图数据
const mindMapData = computed(() => {
  const nodes = []
  const edges = []

  const processNode = (node, parent = null) => {
    nodes.push({
      id: node.id.toString(),
      title: node.title,
      description: node.url,
      nodeType: getNodeTypeString(node.nodeType)
    })

    if (parent) {
      edges.push({
        id: `edge-${parent.id}-${node.id}`,
        source: parent.id.toString(),
        target: node.id.toString()
      })
    }

    if (node.children && node.children.length) {
      node.children.forEach(child => processNode(child, node))
    }
  }

  nodeTree.value.forEach(rootNode => processNode(rootNode))

  return { nodes, edges }
})

const getNodeTypeString = (nodeType) => {
  const typeMap = { 1: 'normal', 2: 'important', 3: 'category' }
  return typeMap[nodeType] || 'normal'
}

// 方法
const fetchMapInfo = async () => {
  try {
    const data = await getKnowledgeMapById(mapId.value)
    mapInfo.value = data
    document.title = `${data.title} - 知识图谱`
  } catch (error) {
    ElMessage.error('获取图谱信息失败')
    goBack()
  }
}

const fetchNodeTree = async () => {
  try {
    loading.value = true
    const data = await getKnowledgeNodeTree(mapId.value)
    nodeTree.value = data

    // 扁平化节点列表
    flattenNodeTree(data)

    // 初始化思维导图
    await nextTick()
    initMindMap()
  } catch (error) {
    ElMessage.error('获取知识图谱失败')
  } finally {
    loading.value = false
  }
}

const flattenNodeTree = (nodes, result = []) => {
  for (const node of nodes) {
    result.push(node)
    if (node.children && node.children.length) {
      flattenNodeTree(node.children, result)
    }
  }
  flatNodeList.value = result
}

const initMindMap = () => {
  // 初始化思维导图
}

const handleNodeClick = (nodeData) => {
  console.log('节点点击事件触发:', nodeData) // 添加调试日志

  // 如果直接传入了完整节点数据，就直接使用
  let fullNode = nodeData

  // 如果传入的是G6格式的数据，需要从扁平化列表中查找
  if (nodeData.id && !nodeData.url) {
    fullNode = flatNodeList.value.find(n => n.id.toString() === nodeData.id.toString())
  }

  if (fullNode) {
    selectedNode.value = fullNode
    selectedNodeIndex.value = flatNodeList.value.findIndex(n => n.id === fullNode.id)

    // 延迟一帧确保在移动端正确显示
    nextTick(() => {
      showNodeDetail.value = true
      console.log('弹框应该显示了:', showNodeDetail.value) // 添加调试日志
    })

    // 处理图片加载
    handleImageLoading()
  }
}

const handleNodeDblClick = (nodeData) => {
  // 双击时可以做额外的操作，比如展开详情
  handleNodeClick(nodeData)
}

const handleCloseDetail = () => {
  showNodeDetail.value = false
  selectedNode.value = null
  selectedNodeIndex.value = -1
}

const navigateToNode = (direction) => {
  let newIndex
  if (direction === 'prev') {
    newIndex = selectedNodeIndex.value - 1
  } else {
    newIndex = selectedNodeIndex.value + 1
  }

  if (newIndex >= 0 && newIndex < flatNodeList.value.length) {
    const node = flatNodeList.value[newIndex]
    // 直接传递节点数据
    selectedNode.value = node
    selectedNodeIndex.value = newIndex

    // 处理图片加载
    handleImageLoading()


  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  try {
    const data = await searchKnowledgeNodes(mapId.value, searchKeyword.value)
    searchResults.value = data
    showSearchResults.value = true

    if (!data.length) {
      ElMessage.info('未找到相关节点')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  }
}

const handleSelectSearchResult = (result) => {
  showSearchResults.value = false
  searchKeyword.value = ''

  // 定位并选中节点
  handleNodeClick(result)

  // 节点定位由MindMap组件自动处理
}

const handleZoomIn = () => {
  if (mindMapRef.value) {
    mindMapRef.value.zoomIn()
  }
}

const handleZoomOut = () => {
  if (mindMapRef.value) {
    mindMapRef.value.zoomOut()
  }
}

const handleResetZoom = () => {
  if (mindMapRef.value) {
    mindMapRef.value.resetZoom()
  }
}

const goBack = () => {
  router.push('/knowledge')
}

// 辅助方法
const getNodeTypeTagType = (nodeType) => {
  const typeMap = {
    1: 'info',     // 普通
    2: 'danger',   // 重点
    3: 'warning',  // 难点
    'normal': 'info',
    'important': 'danger',
    'category': 'warning'
  }
  return typeMap[nodeType] || 'info'
}

const getNodeTypeText = (nodeType) => {
  const textMap = {
    1: '普通',
    2: '重点',
    3: '难点'
  }
  return textMap[nodeType] || '普通'
}

// 图片加载处理 - 简化版本
const handleImageLoading = () => {
  // 暂时移除复杂的加载处理，使用简单的图片显示
}

// 生命周期
onMounted(async () => {
  // 初始化设备检测
  checkDevice()

  // 监听窗口大小变化
  window.addEventListener('resize', checkDevice)

  await fetchMapInfo()
  await fetchNodeTree()
})

onUnmounted(() => {
  // 清理事件监听
  window.removeEventListener('resize', checkDevice)
})

// 监听路由变化
watch(() => route.params.id, (newId) => {
  if (newId && newId !== mapId.value) {
    mapId.value = newId
    fetchMapInfo()
    fetchNodeTree()
  }
})

// 键盘导航
onMounted(() => {
  const handleKeydown = (e) => {
    if (showNodeDetail.value) {
      if (e.key === 'ArrowLeft' && hasPrevNode.value) {
        navigateToNode('prev')
      } else if (e.key === 'ArrowRight' && hasNextNode.value) {
        navigateToNode('next')
      } else if (e.key === 'Escape') {
        handleCloseDetail()
      }
    }
  }

  document.addEventListener('keydown', handleKeydown)

  // 清理事件监听器
  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeydown)
  })
})
</script>

<style scoped>
.knowledge-viewer {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.map-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.map-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.map-meta {
  font-size: 12px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
}

.viewer-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.mind-map-container {
  height: 100%;
  position: relative;
}

.mind-map-canvas {
  width: 100%;
  height: 100%;
  background: white;
}

.empty-canvas {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
}

.empty-canvas p {
  margin: 16px 0 0 0;
  font-size: 16px;
}

.canvas-tips {
  position: absolute;
  bottom: 20px;
  left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  backdrop-filter: blur(8px);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #606266;
}

.tip-item .el-icon {
  font-size: 14px;
  color: #909399;
}

.node-detail-dialog {
  border-radius: 12px;
}

.node-content {
  min-height: 200px;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.node-stats {
  font-size: 12px;
  color: #909399;
}

.document-content {
  width: 100%;
  height: 800px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.document-content iframe {
  border-radius: 4px;
}

.markdown-content :deep(h1) {
  margin: 24px 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #eaecef;
  color: #2c3e50;
  font-weight: 700;
  font-size: 28px;
  line-height: 1.25;
}

.markdown-content :deep(h2) {
  margin: 20px 0 14px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #eaecef;
  color: #2c3e50;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.25;
}

.markdown-content :deep(h3) {
  margin: 18px 0 12px 0;
  color: #2c3e50;
  font-weight: 600;
  font-size: 20px;
  line-height: 1.25;
}

.markdown-content :deep(h4) {
  margin: 16px 0 10px 0;
  color: #2c3e50;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.25;
}

.markdown-content :deep(h5) {
  margin: 14px 0 8px 0;
  color: #2c3e50;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.25;
}

.markdown-content :deep(h6) {
  margin: 12px 0 8px 0;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.25;
}

.markdown-content :deep(p) {
  margin: 16px 0;
  line-height: 1.8;
  color: #34495e;
  font-size: 16px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 16px 0;
  padding-left: 28px;
}

.markdown-content :deep(li) {
  margin: 8px 0;
  line-height: 1.6;
  color: #34495e;
}

.markdown-content :deep(li::marker) {
  color: #7f8c8d;
}

.markdown-content :deep(blockquote) {
  margin: 20px 0;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border-left: 4px solid #4A90E2;
  border-radius: 8px;
  color: #5a6c7d;
  font-style: italic;
  position: relative;
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.1);
}

.markdown-content :deep(blockquote::before) {
  content: '"';
  position: absolute;
  top: 8px;
  left: 12px;
  color: #4A90E2;
  font-size: 24px;
  font-weight: bold;
  opacity: 0.3;
}

.markdown-content :deep(code.inline-code) {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  color: #e83e8c;
  padding: 3px 6px;
  border-radius: 4px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 0.9em;
  font-weight: 500;
  border: 1px solid #e1e8ed;
}

.markdown-content :deep(code) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
}

.markdown-content :deep(pre) {
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%);
  color: #e2e8f0;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 20px 0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border: 1px solid #4a5568;
  position: relative;
}

.markdown-content :deep(pre code) {
  background: transparent;
  color: inherit;
  padding: 0;
  border: none;
  border-radius: 0;
  font-size: 14px;
  line-height: 1.6;
  display: block;
  width: 100%;
}

/* 基础代码高亮 */
.markdown-content :deep(pre code .keyword) { color: #ff7b72; }
.markdown-content :deep(pre code .string) { color: #a5d6ff; }
.markdown-content :deep(pre code .comment) { color: #8b949e; font-style: italic; }
.markdown-content :deep(pre code .number) { color: #79c0ff; }
.markdown-content :deep(pre code .function) { color: #d2a8ff; }
.markdown-content :deep(pre code .variable) { color: #ffa657; }

/* 语言特定高亮 */
.markdown-content :deep(pre code.language-javascript),
.markdown-content :deep(pre code.language-js) {
  color: #e2e8f0;
}

.markdown-content :deep(pre code.language-python) {
  color: #e2e8f0;
}

.markdown-content :deep(pre code.language-java) {
  color: #e2e8f0;
}

.markdown-content :deep(pre code.language-css) {
  color: #e2e8f0;
}

.markdown-content :deep(pre code.language-html) {
  color: #e2e8f0;
}

.markdown-content :deep(pre code.language-sql) {
  color: #e2e8f0;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 20px 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e1e8ed;
}

.markdown-content :deep(th) {
  background: linear-gradient(135deg, #4A90E2 0%, #357abd 100%);
  color: white;
  font-weight: 600;
  font-size: 14px;
}

.markdown-content :deep(td) {
  background: white;
  font-size: 14px;
  color: #34495e;
}

.markdown-content :deep(tr:nth-child(even) td) {
  background: #f8f9fa;
}

.markdown-content :deep(a) {
  color: #4A90E2;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
}

.markdown-content :deep(a:hover) {
  color: #357abd;
  border-bottom-color: #4A90E2;
}

.markdown-content :deep(strong) {
  color: #2c3e50;
  font-weight: 700;
}

.markdown-content :deep(em) {
  color: #5a6c7d;
  font-style: italic;
}

.markdown-content :deep(hr) {
  margin: 32px 0;
  border: none;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #e1e8ed 50%, transparent 100%);
}

.markdown-content :deep(del) {
  color: #95a5a6;
  text-decoration: line-through;
}

.markdown-content :deep(ol) {
  margin: 16px 0;
  padding-left: 28px;
  counter-reset: item;
}

.markdown-content :deep(ol li) {
  margin: 8px 0;
  line-height: 1.6;
  color: #34495e;
}

/* 图片容器和样式 */
.markdown-content :deep(.image-container) {
  margin: 20px 0;
  text-align: center;
}

.markdown-content :deep(.markdown-image) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 0 auto;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  cursor: zoom-in;
  transition: all 0.3s ease;
}

.markdown-content :deep(.markdown-image:hover) {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

/* 代码高亮样式优化 */
.markdown-content :deep(.hljs) {
  background: linear-gradient(135deg, #2d3748 0%, #1a202c 100%) !important;
  color: #e2e8f0 !important;
  padding: 20px !important;
  border-radius: 12px !important;
  font-size: 14px !important;
  line-height: 1.6 !important;
  border: 1px solid #4a5568 !important;
}

.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  color: #909399;
}

.empty-content p {
  margin: 16px 0 0 0;
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navigation-buttons {
  display: flex;
  gap: 12px;
}

.search-results {
  padding: 16px 0;
}

.search-result-item {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.search-result-item:hover {
  border-color: #409eff;
  background-color: #f0f9ff;
  transform: translateY(-1px);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.result-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.result-content {
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.no-search-results {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.no-search-results p {
  margin: 16px 0 8px 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .viewer-header {
    padding: 12px 16px;
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .header-left,
  .header-right {
    justify-content: space-between;
    width: 100%;
  }

  .header-right {
    flex-wrap: wrap;
    gap: 8px;
  }

  .canvas-tips {
    display: none;
  }

  .dialog-footer {
    flex-direction: column;
    gap: 16px;
  }

  .navigation-buttons {
    width: 100%;
    justify-content: space-between;
  }

  /* 移动端弹框优化 */
  .node-detail-dialog {
    margin: 0 !important;
  }

  .document-content {
    height: auto !important;
    min-height: 60vh;
  }

  .node-content {
    padding: 10px !important;
  }

  .node-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

/* Element Plus 样式覆盖 */
:deep(.el-dialog__header) {
  border-bottom: 1px solid #f0f0f0;
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

/* 移动端弹框样式优化 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    margin: 0 !important;
    max-height: 100vh;
    border-radius: 0;
  }

  :deep(.el-dialog__body) {
    padding: 10px !important;
    max-height: calc(100vh - 120px);
    overflow-y: auto;
  }

  :deep(.el-dialog__footer) {
    padding: 10px !important;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
