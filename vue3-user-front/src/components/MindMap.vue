<template>
  <div class="mindmap-container" ref="containerRef">
    <div
      :id="containerId"
      class="mindmap-canvas"
      :style="{
        width: width,
        height: height
      }"
    ></div>

    <!-- 工具栏 -->
    <div v-if="showToolbar" class="mindmap-toolbar">
      <el-button-group>
        <el-button size="small" @click="zoomIn" title="放大">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <el-button size="small" @click="zoomOut" title="缩小">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <el-button size="small" @click="resetZoom" title="重置缩放">
          {{ Math.round(currentZoom * 100) }}%
        </el-button>
        <el-button size="small" @click="fitView" title="适应画布">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </el-button-group>

      <el-button-group v-if="editable" class="toolbar-group-secondary">
        <el-button size="small" @click="addNode" title="添加节点">
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button size="small" @click="deleteNode" title="删除节点" :disabled="!selectedNode">
          <el-icon><Delete /></el-icon>
        </el-button>
      </el-button-group>
    </div>

    <!-- 节点详情面板 -->
    <div v-if="showNodePanel && selectedNodeInfo" class="node-panel-overlay" @click="closeNodePanel">
      <div class="node-panel" @click.stop>
        <div class="panel-header">
          <h4>{{ selectedNodeInfo.title }}</h4>
          <el-button size="small" text @click="closeNodePanel">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="panel-content">
          <div v-if="selectedNodeInfo.description" class="node-description">
            <p>{{ selectedNodeInfo.description }}</p>
          </div>
          <div class="node-meta">
            <CnStatusTag size="sm" :type="getNodeTagType(selectedNodeInfo.nodeType)">
              {{ getNodeTypeText(selectedNodeInfo.nodeType) }}
            </CnStatusTag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Graph } from '@antv/g6'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut, FullScreen, Plus, Delete, Close } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'
import { debounce } from 'lodash'

// Props
const props = defineProps({
  data: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  width: {
    type: String,
    default: '100%'
  },
  height: {
    type: String,
    default: '600px'
  },
  editable: {
    type: Boolean,
    default: false
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  showNodePanel: {
    type: Boolean,
    default: true
  },
  layout: {
    type: String,
    default: 'mindmap'
  }
})

// Emits
const emit = defineEmits(['node-click', 'node-dblclick', 'data-change'])

// Refs
const containerRef = ref()
const containerId = ref(`mindmap-${Date.now()}`)

// Data
const graph = ref(null)
const currentZoom = ref(1)
const selectedNode = ref(null)
const selectedNodeInfo = ref(null)

const getThemeColor = (token, fallback) => {
  if (typeof window === 'undefined') {
    return fallback
  }

  const value = window.getComputedStyle(document.documentElement).getPropertyValue(token).trim()
  return value || fallback
}

const getMindMapTheme = () => ({
  canvas: getThemeColor('--cn-color-bg-surface', 'white'),
  canvasMuted: getThemeColor('--cn-color-bg-surface-muted', 'aliceblue'),
  border: getThemeColor('--cn-color-border-subtle', 'lightsteelblue'),
  brand: getThemeColor('--cn-color-brand-primary', 'royalblue'),
  brandSoft: getThemeColor('--cn-color-brand-soft', 'aliceblue'),
  edge: getThemeColor('--cn-color-text-tertiary', 'slategray'),
  text: getThemeColor('--cn-color-text-primary', 'midnightblue')
})

// Methods
// 检测是否为移动设备
const isMobileDevice = () => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
         ('ontouchstart' in window) ||
         (navigator.maxTouchPoints > 0)
}

const initGraph = () => {
  if (!containerRef.value) return

  const container = document.getElementById(containerId.value)
  if (!container) return

  // 创建G6图实例 - G6 v4 API
  try {
    const theme = getMindMapTheme()

    graph.value = new Graph({
      container: containerId.value,
      width: container.offsetWidth,
      height: container.offsetHeight,
      layout: {
        type: props.layout === 'mindmap' ? 'dagre' : props.layout,
        direction: 'LR',
        rankdir: 'LR',
        nodesep: 20,
        ranksep: 50
      },
      modes: {
        default: [
          'zoom-canvas',
          'drag-canvas',
          'drag-node',
          {
            type: 'tooltip',
            formatText: (model) => {
              return model.label || model.title || '节点'
            }
          }
        ]
      },
      // 移动端触摸支持
      fitView: true,
      animate: true,
      enabledStack: true,
      maxZoom: 5,
      minZoom: 0.1,
      defaultNode: {
        type: 'rect',
        size: [100, 40],
        style: {
          fill: `l(0) 0:${theme.canvas} 1:${theme.canvasMuted}`,
          stroke: theme.brand,
          lineWidth: 2,
          radius: 12,
          shadowColor: theme.brandSoft,
          shadowBlur: 8,
          shadowOffsetX: 2,
          shadowOffsetY: 2
        },
        labelCfg: {
          style: {
            fontSize: 14,
            fill: theme.text,
            fontWeight: 500
          }
        }
      },
      defaultEdge: {
        type: 'cubic-horizontal',
        style: {
          stroke: theme.edge,
          lineWidth: 3,
          opacity: 0.7,
          endArrow: false,
          lineDash: [0],
          shadowColor: theme.border,
          shadowBlur: 2,
          shadowOffsetX: 1,
          shadowOffsetY: 1
        }
      },
      nodeStateStyles: {
        selected: {
          stroke: theme.brand,
          lineWidth: 3
        },
        hover: {
          fill: theme.brandSoft,
          stroke: theme.brand
        }
      },
      edgeStateStyles: {
        hover: {
          stroke: theme.brand
        }
      }
    })
  } catch (error) {
    console.error('G6图实例创建失败:', error)
    ElMessage.error('思维导图初始化失败，请刷新页面重试')
    return
  }

  // 事件监听 - 同时监听点击和触摸事件
  const handleNodeInteraction = (event) => {
    try {
      const { item } = event
      const nodeData = item.getModel()
      selectedNode.value = item
      selectedNodeInfo.value = nodeData

      // 清除所有状态并设置选中状态
      if (selectedNode.value && selectedNode.value !== item) {
        graph.value.setItemState(selectedNode.value, 'selected', false)
      }
      graph.value.setItemState(item, 'selected', true)

      emit('node-click', nodeData)
    } catch (error) {
      console.warn('节点交互事件处理错误:', error)
    }
  }

  // 绑定多种事件以确保移动端兼容性
  graph.value.on('node:click', handleNodeInteraction)
  graph.value.on('node:tap', handleNodeInteraction) // 移动端tap事件
  graph.value.on('node:touchstart', handleNodeInteraction) // 触摸开始事件

  // 双击/双触摸事件
  const handleNodeDoubleInteraction = (event) => {
    try {
      const { item } = event
      const nodeData = item.getModel()
      emit('node-dblclick', nodeData)
    } catch (error) {
      console.warn('节点双击事件处理错误:', error)
    }
  }

  graph.value.on('node:dblclick', handleNodeDoubleInteraction)
  graph.value.on('node:dbltap', handleNodeDoubleInteraction) // 移动端双击事件

  graph.value.on('canvas:click', () => {
    if (selectedNode.value) {
      graph.value.setItemState(selectedNode.value, 'selected', false)
      selectedNode.value = null
      selectedNodeInfo.value = null
    }
  })

  // 缩放事件监听
  graph.value.on('viewportchange', () => {
    currentZoom.value = graph.value.getZoom()
  })

  // 移动端备选事件监听 - 直接在DOM上监听
  if (isMobileDevice()) {
    const canvas = container.querySelector('canvas')
    if (canvas) {
      // 触摸事件处理
      canvas.addEventListener('touchstart', (e) => {
        handleCanvasTouchStart(e)
      }, { passive: false })

      canvas.addEventListener('click', (e) => {
        handleCanvasClick(e)
      })
    }
  }

  // 加载数据
  loadData()
}

// 处理canvas触摸开始事件
const handleCanvasTouchStart = (e) => {
  const touch = e.touches[0]
  const point = graph.value.getPointByClient(touch.clientX, touch.clientY)
  const item = graph.value.getItemAt(point.x, point.y)

  if (item && item.getType() === 'node') {
    const nodeData = item.getModel()
    emit('node-click', nodeData)
  }
}

// 处理canvas点击事件
const handleCanvasClick = (e) => {
  const point = graph.value.getPointByClient(e.clientX, e.clientY)
  const item = graph.value.getItemAt(point.x, point.y)

  if (item && item.getType() === 'node') {
    const nodeData = item.getModel()
    emit('node-click', nodeData)
  }
}

const loadData = () => {
  if (!graph.value || !props.data) return

  const data = formatData(props.data)

  try {
    // G6 v4 API
    graph.value.data(data)
    graph.value.render()

    // 自动布局 - 多次尝试确保正确定位
    nextTick(() => {
      if (graph.value) {
        graph.value.fitView(30)
        // 延迟再次调用确保正确定位
        setTimeout(() => {
          if (graph.value) {
            graph.value.fitView(30)
          }
        }, 200)
        setTimeout(() => {
          if (graph.value) {
            graph.value.fitView(30)
          }
        }, 500)
      }
    })
  } catch (error) {
    console.error('数据加载失败:', error)
  }
}

const formatData = (rawData) => {
  // 转换数据格式为G6 v4格式
  const nodes = rawData.nodes?.map(node => {
    const title = node.title || node.name || node.label || node.id
    const nodeType = node.nodeType || node.type || 'normal'

    const nodeId = String(node.id || node.nodeId)
    const colorInfo = getNodeColor(nodeType, nodeId)

    return {
      id: nodeId,
      label: title,
      type: 'rect',
      size: [Math.max(title.length * 10 + 40, 80), 32],
      style: {
        fill: colorInfo.fill,
        stroke: colorInfo.stroke,
        lineWidth: 2,
        radius: 12,
        opacity: colorInfo.opacity,
        shadowColor: colorInfo.shadow,
        shadowBlur: 6,
        shadowOffsetX: 2,
        shadowOffsetY: 2,
        cursor: 'pointer' // 确保显示可点击状态
      },
      // 保存原始数据
      nodeType: nodeType,
      title: title,
      description: node.description || node.content,
      ...node
    }
  }) || []

  const edges = rawData.edges?.map(edge => ({
    id: String(edge.id || `${edge.source}-${edge.target}`),
    source: String(edge.source),
    target: String(edge.target),
    type: 'line',
    ...edge
  })) || []

  return { nodes, edges }
}

// Canvas 色值属于图谱节点可视化数据，使用命名色避免把页面主题 token 写进 G6 数据模型。
const colorPalettes = [
  { start: 'mistyrose', end: 'lavenderblush', border: 'indianred', shadow: 'lightpink' },
  { start: 'seashell', end: 'peachpuff', border: 'darkorange', shadow: 'moccasin' },
  { start: 'lightyellow', end: 'khaki', border: 'goldenrod', shadow: 'palegoldenrod' },
  { start: 'honeydew', end: 'palegreen', border: 'seagreen', shadow: 'lightgreen' },
  { start: 'azure', end: 'paleturquoise', border: 'teal', shadow: 'powderblue' },
  { start: 'aliceblue', end: 'lightskyblue', border: 'steelblue', shadow: 'lightblue' },
  { start: 'ghostwhite', end: 'lightsteelblue', border: 'slategray', shadow: 'gainsboro' },
  { start: 'lavender', end: 'plum', border: 'mediumpurple', shadow: 'thistle' },
  { start: 'lavenderblush', end: 'pink', border: 'mediumvioletred', shadow: 'lightpink' },
  { start: 'whitesmoke', end: 'powderblue', border: 'royalblue', shadow: 'lightblue' }
]

const getNodeColor = (nodeType, nodeId) => {
  // 根据节点ID生成一个稳定的随机索引
  const hash = nodeId.split('').reduce((a, b) => {
    a = ((a << 5) - a) + b.charCodeAt(0)
    return a & a
  }, 0)

  const paletteIndex = Math.abs(hash) % colorPalettes.length
  const palette = colorPalettes[paletteIndex]

  // 根据节点类型调整透明度
  const typeMultiplier = {
    normal: 1,
    important: 0.9,
    category: 0.8
  }

  return {
    fill: `l(0) 0:${palette.start} 1:${palette.end}`,
    stroke: palette.border,
    shadow: palette.shadow,
    opacity: typeMultiplier[nodeType] || 1
  }
}

const getNodeTagType = (nodeType) => {
  const types = {
    normal: 'info',
    important: 'warning',
    category: 'success'
  }
  return types[nodeType] || 'info'
}

const getNodeTypeText = (nodeType) => {
  const texts = {
    normal: '普通节点',
    important: '重要节点',
    category: '分类节点'
  }
  return texts[nodeType] || '普通节点'
}

// 工具栏方法
const zoomIn = () => {
  if (!graph.value) return
  const zoom = graph.value.getZoom()
  graph.value.zoomTo(Math.min(zoom * 1.2, 3))
}

const zoomOut = () => {
  if (!graph.value) return
  const zoom = graph.value.getZoom()
  graph.value.zoomTo(Math.max(zoom * 0.8, 0.1))
}

const resetZoom = () => {
  if (!graph.value) return
  graph.value.zoomTo(1)
}

const fitView = () => {
  if (!graph.value) return
  graph.value.fitView(20)
}

const addNode = () => {
  if (!selectedNodeInfo.value) {
    ElMessage.info('请先选择一个父节点')
    return
  }

  emit('data-change', {
    action: 'node-add-request',
    parentNode: selectedNodeInfo.value
  })
}

const deleteNode = () => {
  if (!selectedNodeInfo.value) {
    ElMessage.warning('请先选择要删除的节点')
    return
  }

  emit('data-change', {
    action: 'node-delete-request',
    node: selectedNodeInfo.value
  })
  closeNodePanel()
}

const closeNodePanel = () => {
  if (selectedNode.value) {
    try {
      graph.value.setItemState(selectedNode.value, 'selected', false)
    } catch (error) {
      console.warn('清除选中状态失败:', error)
    }
    selectedNode.value = null
  }
  selectedNodeInfo.value = null
}

// 响应式处理
const handleResize = debounce(() => {
  if (graph.value && containerRef.value) {
    const container = document.getElementById(containerId.value)
    if (container) {
      graph.value.changeSize(container.offsetWidth, container.offsetHeight)
      graph.value.fitView(20)
    }
  }
}, 200)

// Watch
watch(() => props.data, () => {
  loadData()
}, { deep: true })

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initGraph()
    window.addEventListener('resize', handleResize)
  })
})

onUnmounted(() => {
  if (graph.value) {
    graph.value.destroy()
  }
  window.removeEventListener('resize', handleResize)
})

// 暴露方法供外部调用
defineExpose({
  getGraph: () => graph.value,
  fitView,
  zoomIn,
  zoomOut,
  resetZoom,
  refresh: loadData,
  isMobileDevice // 暴露移动设备检测方法
})
</script>

<style scoped>
.mindmap-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.mindmap-canvas {
  position: relative;
  background: var(--cn-color-bg-surface);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
}

.mindmap-toolbar {
  position: absolute;
  top: var(--cn-space-3);
  right: var(--cn-space-3);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: var(--cn-space-2);
  padding: var(--cn-space-2);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-control);
  background: color-mix(in srgb, var(--cn-color-bg-elevated) 92%, transparent);
  box-shadow: var(--cn-shadow-popover);
  backdrop-filter: blur(12px);
}

.toolbar-group-secondary {
  margin-left: var(--cn-space-2);
}

.node-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: color-mix(in srgb, var(--cn-color-text-primary) 42%, transparent);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.node-panel {
  position: relative;
  width: 800px;
  max-width: 90vw;
  max-height: 80vh;
  background: var(--cn-color-bg-elevated);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-panel);
  box-shadow: var(--cn-shadow-popover);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: fadeInScale var(--cn-motion-base) var(--cn-ease-out);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--cn-space-4) var(--cn-space-6);
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
  flex-shrink: 0;
}

.panel-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-content {
  padding: var(--cn-space-5) var(--cn-space-6);
  flex: 1;
  overflow-y: auto;
  max-height: calc(80vh - 80px);
}

.node-description {
  margin-bottom: var(--cn-space-5);
  min-height: 200px;
}

.node-description p {
  margin: 0 0 var(--cn-space-3) 0;
  color: var(--cn-color-text-primary);
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.node-meta {
  display: flex;
  gap: var(--cn-space-2);
}

.el-button-group {
  vertical-align: top;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 768px) {
  .mindmap-canvas {
    touch-action: pan-x pan-y;
    -webkit-touch-callout: none;
    -webkit-user-select: none;
    user-select: none;
  }

  .node-panel {
    width: 95vw;
    max-height: 85vh;
  }

  .panel-header {
    padding: var(--cn-space-3) var(--cn-space-4);
  }

  .panel-header h4 {
    font-size: 16px;
  }

  .panel-content {
    padding: var(--cn-space-4) var(--cn-space-5);
  }

  .node-description p {
    font-size: 14px;
  }
}

.mindmap-canvas * {
  touch-action: manipulation;
}

:deep(.el-button--small) {
  padding: var(--cn-space-1) var(--cn-space-2);
  font-size: 12px;
}
</style>
