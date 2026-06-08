<template>
  <CnPage class="knowledge-viewer" surface="transparent" full-height max-width="100%">
    <CnPageHeader
      :title="mapInfo.title || '知识图谱'"
      :description="mapInfo.description || '可视化查看知识节点结构，点击节点打开详情内容。'"
      eyebrow="KNOWLEDGE MAP"
      :breadcrumbs="breadcrumbs"
      compact
    >
      <template #meta>
        <CnStatusTag type="brand" size="sm">{{ mapInfo.nodeCount || 0 }} 个节点</CnStatusTag>
        <CnStatusTag type="info" size="sm">{{ mapInfo.viewCount || 0 }} 次查看</CnStatusTag>
        <CnStatusTag v-if="nodeTree.length" type="success" size="sm" subtle>
          已加载 {{ flatNodeList.length }} 个可导航节点
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索节点..."
          :prefix-icon="Search"
          class="viewer-search"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button-group>
          <el-button :icon="ZoomOut" @click="handleZoomOut" title="缩小" />
          <el-button @click="handleResetZoom" title="适应画布">
            {{ Math.round(zoomLevel * 100) }}%
          </el-button>
          <el-button :icon="ZoomIn" @click="handleZoomIn" title="放大" />
        </el-button-group>
      </template>
    </CnPageHeader>

    <!-- 主要内容区域 -->
    <CnSection class="viewer-section" surface="panel" compact>
      <div class="viewer-content" v-loading="loading">
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

        <CnEmptyState
          v-if="!nodeTree.length && !loading"
          class="empty-canvas"
          title="暂无内容"
          description="当前知识图谱还没有可展示的节点。"
          icon="KM"
          surface="transparent"
        />
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
    </CnSection>

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
          <CnStatusTag :type="getNodeTypeTagType(selectedNode.nodeType)" size="sm">
            {{ getNodeTypeText(selectedNode.nodeType) }}
          </CnStatusTag>
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
        <CnEmptyState
          v-else
          class="empty-content"
          title="该节点暂无文档链接"
          description="可以切换到相邻节点继续查看。"
          icon="DOC"
        />
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
            <CnStatusTag :type="getNodeTypeTagType(result.nodeType)" size="sm">
              {{ getNodeTypeText(result.nodeType) }}
            </CnStatusTag>
          </div>
          <div class="result-content">
            {{ result.url || '暂无链接' }}
          </div>
        </div>
      </div>

      <CnEmptyState
        v-else-if="searchKeyword"
        class="no-search-results"
        title="未找到相关节点"
        description="尝试使用其他关键词搜索。"
        icon="SR"
        surface="transparent"
      />
    </el-drawer>
  </CnPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Search, ZoomIn, ZoomOut,
  Mouse, Switch
} from '@element-plus/icons-vue'
import {
  CnEmptyState,
  CnPage,
  CnPageHeader,
  CnSection,
  CnStatusTag
} from '@/design-system'
import {
  getKnowledgeMapById,
  getKnowledgeNodeTree,
  searchKnowledgeNodes
} from '@/api/knowledge'

const MindMap = defineAsyncComponent(() => import('@/components/MindMap.vue'))

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

const breadcrumbs = computed(() => [
  { label: '知识图谱', to: '/knowledge' },
  { label: mapInfo.value?.title || '图谱详情' }
])

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
  min-height: calc(100vh - 68px);
}

.knowledge-viewer :deep(.cn-page__body) {
  min-height: 0;
}

.knowledge-viewer :deep(.cn-page-header__actions) {
  align-items: center;
}

.viewer-search {
  width: 220px;
}

.viewer-section {
  min-height: 0;
}

.viewer-section :deep(.cn-section__body) {
  height: calc(100vh - 236px);
  min-height: 560px;
  padding: 0;
}

.viewer-content {
  position: relative;
  height: 100%;
  overflow: hidden;
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-surface);
}

.mind-map-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: var(--cn-color-bg-surface);
}

.empty-canvas {
  position: absolute;
  inset: var(--cn-space-6);
  height: auto;
}

.canvas-tips {
  position: absolute;
  bottom: var(--cn-space-5);
  left: var(--cn-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--cn-space-2);
  padding: var(--cn-space-3) var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: color-mix(in srgb, var(--cn-color-bg-surface) 92%, transparent);
  box-shadow: var(--cn-shadow-popover);
  backdrop-filter: blur(8px);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  color: var(--cn-color-text-secondary);
  font-size: 12px;
}

.tip-item .el-icon {
  font-size: 14px;
  color: var(--cn-color-text-tertiary);
}

.node-detail-dialog {
  border-radius: var(--cn-radius-panel);
}

.node-content {
  display: grid;
  gap: var(--cn-space-4);
  min-height: 200px;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
  padding-bottom: var(--cn-space-3);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.node-stats {
  color: var(--cn-color-text-tertiary);
  font-size: 12px;
}

.document-content {
  width: 100%;
  height: 800px;
  overflow: hidden;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  background: var(--cn-color-bg-surface-muted);
}

.document-content iframe {
  border-radius: var(--cn-radius-card);
}

.empty-content {
  min-height: 260px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--cn-space-3);
}

.navigation-buttons {
  display: flex;
  gap: var(--cn-space-3);
}

.search-results {
  display: grid;
  gap: var(--cn-space-3);
  padding: var(--cn-space-2) 0;
}

.search-result-item {
  padding: var(--cn-space-4);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: var(--cn-color-bg-surface);
  cursor: pointer;
  transition:
    transform var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out),
    background-color var(--cn-motion-fast) var(--cn-ease-out);
}

.search-result-item:hover {
  border-color: color-mix(in srgb, var(--cn-color-brand-primary) 36%, var(--cn-color-border-subtle));
  background: var(--cn-color-brand-soft);
  transform: translateY(-1px);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--cn-space-3);
  margin-bottom: var(--cn-space-2);
}

.result-title {
  margin: 0;
  color: var(--cn-color-text-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
}

.result-content {
  color: var(--cn-color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
}

.no-search-results {
  min-height: 280px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .knowledge-viewer {
    padding: var(--cn-space-4);
  }

  .knowledge-viewer :deep(.cn-page-header__actions) {
    width: 100%;
    justify-content: flex-start;
  }

  .viewer-search {
    width: 100%;
  }

  .viewer-section :deep(.cn-section__body) {
    height: calc(100vh - 300px);
    min-height: 460px;
  }

  .canvas-tips {
    display: none;
  }

  .dialog-footer {
    flex-direction: column;
    gap: var(--cn-space-4);
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
    padding: var(--cn-space-2) !important;
  }

  .node-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--cn-space-2);
  }
}

/* Element Plus 样式覆盖 */
:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: var(--cn-space-4);
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

/* 移动端弹框样式优化 */
@media (max-width: 768px) {
  :deep(.el-dialog) {
    margin: 0 !important;
    max-height: 100vh;
    border-radius: 0;
  }

  :deep(.el-dialog__body) {
    padding: var(--cn-space-3) !important;
    max-height: calc(100vh - 120px);
    overflow-y: auto;
  }

  :deep(.el-dialog__footer) {
    padding: var(--cn-space-3) !important;
    border-top: 1px solid var(--cn-color-border-subtle);
  }
}
</style>
