<template>
  <div class="mindmap-container" ref="containerRef">
    <div
      :id="containerId"
      class="mindmap-canvas"
      :style="{
        width: width,
        height: height,
        background: '#fff',
        border: '1px solid #e8e8e8',
        borderRadius: '4px'
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

      <el-button-group v-if="editable" style="margin-left: 8px;">
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
            <el-tag size="small" :type="getNodeTagType(selectedNodeInfo.nodeType)">
              {{ getNodeTypeText(selectedNodeInfo.nodeType) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { Graph } from '@antv/g6'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut, FullScreen, Plus, Delete, Close } from '@element-plus/icons-vue'
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

  console.log('初始化G6图实例，是否为移动设备:', isMobileDevice()) // 调试日志

  // 创建G6图实例 - G6 v4 API
  try {
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
          fill: 'l(0) 0:#ffffff 1:#f8fbff',
          stroke: '#4A90E2',
          lineWidth: 2,
          radius: 12,
          shadowColor: 'rgba(74, 144, 226, 0.15)',
          shadowBlur: 8,
          shadowOffsetX: 2,
          shadowOffsetY: 2
        },
        labelCfg: {
          style: {
            fontSize: 14,
            fill: '#2c3e50',
            fontWeight: 500
          }
        }
      },
      defaultEdge: {
        type: 'cubic-horizontal',
        style: {
          stroke: '#95a5a6',
          lineWidth: 3,
          opacity: 0.7,
          endArrow: false,
          lineDash: [0],
          shadowColor: 'rgba(149, 165, 166, 0.3)',
          shadowBlur: 2,
          shadowOffsetX: 1,
          shadowOffsetY: 1
        }
      },
      nodeStateStyles: {
        selected: {
          stroke: '#1890ff',
          lineWidth: 3
        },
        hover: {
          fill: '#f0f8ff',
          stroke: '#1890ff'
        }
      },
      edgeStateStyles: {
        hover: {
          stroke: '#1890ff'
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

      console.log('G6节点交互事件:', nodeData) // 调试日志
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
      console.log('G6节点双击/双触摸事件:', nodeData) // 调试日志
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
    console.log('添加移动端DOM事件监听') // 调试日志
    const canvas = container.querySelector('canvas')
    if (canvas) {
      // 触摸事件处理
      canvas.addEventListener('touchstart', (e) => {
        console.log('Canvas touchstart事件触发') // 调试日志
        handleCanvasTouchStart(e)
      }, { passive: false })

      canvas.addEventListener('click', (e) => {
        console.log('Canvas click事件触发') // 调试日志
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
    console.log('通过Canvas触摸找到节点:', item.getModel()) // 调试日志
    const nodeData = item.getModel()
    emit('node-click', nodeData)
  }
}

// 处理canvas点击事件
const handleCanvasClick = (e) => {
  const point = graph.value.getPointByClient(e.clientX, e.clientY)
  const item = graph.value.getItemAt(point.x, point.y)

  if (item && item.getType() === 'node') {
    console.log('通过Canvas点击找到节点:', item.getModel()) // 调试日志
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
        shadowColor: `${colorInfo.stroke}30`,
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

// 定义美观的颜色组合
const colorPalettes = [
  { bg: 'l(0) 0:#fff5f5 1:#fed7e2', border: '#f56565' }, // 红色
  { bg: 'l(0) 0:#fffaf0 1:#fbd38d', border: '#ed8936' }, // 橙色
  { bg: 'l(0) 0:#fffff0 1:#faf089', border: '#d69e2e' }, // 黄色
  { bg: 'l(0) 0:#f0fff4 1:#9ae6b4', border: '#38a169' }, // 绿色
  { bg: 'l(0) 0:#e6fffa 1:#81e6d9', border: '#319795' }, // 青色
  { bg: 'l(0) 0:#ebf8ff 1:#90cdf4', border: '#3182ce' }, // 蓝色
  { bg: 'l(0) 0:#f7fafc 1:#cbd5e0', border: '#4a5568' }, // 灰色
  { bg: 'l(0) 0:#faf5ff 1:#d6bcfa', border: '#805ad5' }, // 紫色
  { bg: 'l(0) 0:#fff5f7 1:#feb2d2', border: '#d53f8c' }, // 粉色
  { bg: 'l(0) 0:#f0f4f8 1:#bee3f8', border: '#2b6cb0' }  // 淡蓝色
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
    fill: palette.bg,
    stroke: palette.border,
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
}

.mindmap-toolbar {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.node-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
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
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: fadeInScale 0.3s ease-out;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
}

.panel-header h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-content {
  padding: 20px 24px;
  flex: 1;
  overflow-y: auto;
  max-height: calc(80vh - 80px);
}

.node-description {
  margin-bottom: 20px;
  min-height: 200px;
}

.node-description p {
  margin: 0 0 12px 0;
  color: #333;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.node-meta {
  display: flex;
  gap: 8px;
}

.el-button-group {
  vertical-align: top;
}

/* 动画效果 */
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

/* 响应式设计 */
@media (max-width: 768px) {
  .mindmap-canvas {
    /* 确保在移动端可以正常触摸 */
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
    padding: 12px 16px;
  }

  .panel-header h4 {
    font-size: 16px;
  }

  .panel-content {
    padding: 16px 20px;
  }

  .node-description p {
    font-size: 14px;
  }
}

/* 移动端触摸优化 */
.mindmap-canvas * {
  touch-action: manipulation;
}

/* 覆盖Element Plus样式 */
:deep(.el-button--small) {
  padding: 4px 8px;
  font-size: 12px;
}
</style>
