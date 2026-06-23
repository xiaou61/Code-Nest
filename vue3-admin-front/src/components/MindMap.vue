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
        <CnStatusTag type="brand" size="sm" subtle>编辑</CnStatusTag>
        <el-button size="small" @click="addNode" title="添加节点">
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button size="small" @click="deleteNode" title="删除节点" :disabled="!selectedNode">
          <el-icon><Delete /></el-icon>
        </el-button>
      </el-button-group>
    </div>
    
    <!-- 节点编辑对话框 -->
    <el-dialog 
      v-model="showEditDialog" 
      title="编辑节点" 
      width="400px"
      @close="cancelEdit"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="节点名称">
          <el-input 
            v-model="editForm.title" 
            placeholder="请输入节点名称"
            @keyup.enter="confirmEdit"
          />
        </el-form-item>
        <el-form-item label="节点描述">
          <el-input 
            v-model="editForm.description" 
            type="textarea"
            :rows="3"
            placeholder="请输入节点描述（可选）"
          />
        </el-form-item>
        <el-form-item label="节点类型">
          <el-select v-model="editForm.nodeType" class="node-type-select">
            <el-option label="普通节点" value="normal" />
            <el-option label="重要节点" value="important" />
            <el-option label="分类节点" value="category" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Graph } from '@antv/g6'
import { ElMessage } from 'element-plus'
import { ZoomIn, ZoomOut, FullScreen, Plus, Delete } from '@element-plus/icons-vue'
import { CnStatusTag } from '@/design-system'

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
  layout: {
    type: String,
    default: 'mindmap'
  }
})

// Emits
const emit = defineEmits(['node-click', 'node-dblclick', 'data-change', 'node-add', 'node-delete', 'node-update'])

// Refs
const containerRef = ref()
const containerId = ref(`mindmap-${Date.now()}`)

// Data
const graph = ref(null)
const currentZoom = ref(1)
const selectedNode = ref(null)
const showEditDialog = ref(false)
const editForm = reactive({
  id: '',
  title: '',
  description: '',
  nodeType: 'normal'
})

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
const createGraph = () => {
  const container = document.getElementById(containerId.value)
  if (!container) return null

  const theme = getMindMapTheme()
  
  const newGraph = new Graph({
    container: containerId.value,
    width: container.offsetWidth,
    height: container.offsetHeight,
    layout: {
      type: 'dagre',
      direction: 'LR',
      rankdir: 'LR',
      nodesep: 30,
      ranksep: 50
    },
    modes: {
      default: ['zoom-canvas', 'drag-canvas', 'drag-node']
    },
    defaultNode: {
      type: 'rect',
      size: [120, 50],
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
        lineWidth: 2,
        fill: theme.brandSoft
      },
      hover: {
        stroke: theme.brand
      }
    }
  })

  return newGraph
}

const initGraph = () => {
  // 销毁之前的图实例
  if (graph.value) {
    try {
      graph.value.destroy()
    } catch (e) {
      console.warn('销毁图表时出错:', e)
    }
    graph.value = null
  }

  if (!containerRef.value) {
    console.warn('容器不存在')
    return
  }

  const formattedData = formatData(props.data)
  if (!formattedData.nodes || formattedData.nodes.length === 0) {
    return
  }

  try {
    graph.value = createGraph()
    
    if (!graph.value) {
      console.error('图表创建失败')
      return
    }

    // 绑定事件
    bindEvents()
    
    // 加载数据
    graph.value.data(formattedData)
    graph.value.render()
    
    // 自适应视图 - 多次尝试确保正确定位
    setTimeout(() => {
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
    }, 100)
    
  } catch (error) {
    console.error('创建图表失败:', error)
    ElMessage.error('图表初始化失败')
  }
}

const bindEvents = () => {
  if (!graph.value) return

  graph.value.on('node:click', (event) => {
    try {
      const { item } = event
      const nodeData = item.getModel()
      selectedNode.value = item
      
      // 清除所有选中状态
      graph.value.clearItemStates()
      // 设置当前节点为选中状态
      graph.value.setItemState(item, 'selected', true)
      
      emit('node-click', nodeData)
    } catch (e) {
      console.error('处理节点点击事件失败:', e)
    }
  })

  graph.value.on('node:dblclick', (event) => {
    if (props.editable) {
      try {
        const { item } = event
        const nodeData = item.getModel()
        editNode(nodeData.id, nodeData)
      } catch (e) {
        console.error('处理节点双击事件失败:', e)
      }
    }
  })

  graph.value.on('canvas:click', () => {
    if (selectedNode.value) {
      graph.value.clearItemStates()
      selectedNode.value = null
    }
  })

  graph.value.on('viewportchange', () => {
    try {
      currentZoom.value = graph.value.getZoom() || 1
    } catch (e) {
      console.warn('获取缩放级别失败:', e)
    }
  })
}

const formatData = (rawData) => {
  if (!rawData) return { nodes: [], edges: [] }
  
  const nodes = (rawData.nodes || []).map(node => {
    const nodeId = String(node.id || node.nodeId)
    const title = node.title || node.name || node.label || `节点${node.id}`
    const nodeType = node.nodeType || node.type || 'normal'
    const colorInfo = getNodeColor(nodeType, nodeId)
    
    return {
      id: nodeId,
      label: title,
      type: 'rect',
      size: [Math.max(title.length * 10 + 40, 120), 50],
      style: {
        fill: colorInfo.fill,
        stroke: colorInfo.stroke,
        lineWidth: 2,
        radius: 12,
        opacity: colorInfo.opacity,
        shadowColor: colorInfo.shadow,
        shadowBlur: 6,
        shadowOffsetX: 2,
        shadowOffsetY: 2
      },
      // 保存原始数据
      title: title,
      description: node.description || node.content || '',
      nodeType: nodeType
    }
  })

  const edges = (rawData.edges || []).map(edge => ({
    id: edge.id || `${edge.source}-${edge.target}`,
    source: String(edge.source),
    target: String(edge.target),
    type: 'line'
  }))

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

// 编辑方法
const addNode = () => {
  if (!selectedNode.value) {
    ElMessage.warning('请先选择一个父节点')
    return
  }
  
  editForm.id = ''
  editForm.title = ''
  editForm.description = ''
  editForm.nodeType = 'normal'
  showEditDialog.value = true
}

const editNode = (nodeId, nodeData) => {
  editForm.id = nodeId
  editForm.title = nodeData.title || ''
  editForm.description = nodeData.description || ''
  editForm.nodeType = nodeData.nodeType || 'normal'
  showEditDialog.value = true
}

const deleteNode = () => {
  if (!selectedNode.value) {
    ElMessage.warning('请先选择要删除的节点')
    return
  }
  
  try {
    const nodeData = selectedNode.value.getModel()
    emit('node-delete', nodeData)
    selectedNode.value = null
  } catch (e) {
    console.error('删除节点失败:', e)
  }
}

const confirmEdit = () => {
  if (!editForm.title.trim()) {
    ElMessage.warning('请输入节点名称')
    return
  }
  
  const nodeData = {
    id: editForm.id || `node_${Date.now()}`,
    title: editForm.title,
    description: editForm.description,
    nodeType: editForm.nodeType
  }
  
  if (editForm.id) {
    emit('node-update', nodeData)
  } else {
    if (selectedNode.value) {
      const parentData = selectedNode.value.getModel()
      nodeData.parentId = parentData.id
    }
    emit('node-add', nodeData)
  }
  
  showEditDialog.value = false
}

const cancelEdit = () => {
  showEditDialog.value = false
}

// Watch数据变化（只监听数据引用变化，不深度监听）
let updateTimer = null
const lastDataHash = ref('')
const watchEnabled = ref(true) // 添加开关控制

const getDataHash = (data) => {
  if (!data || !data.nodes) return ''
  return `${data.nodes.length}-${data.edges?.length || 0}`
}

watch(() => props.data, (newData) => {
  if (!watchEnabled.value) {
    return
  }
  
  const newHash = getDataHash(newData)
  
  // 只有数据实际变化时才更新
  if (newHash !== lastDataHash.value) {
    lastDataHash.value = newHash
    
    if (updateTimer) clearTimeout(updateTimer)
    updateTimer = setTimeout(() => {
      initGraph()
    }, 300)
  }
}, { immediate: false }) // 移除deep监听，提高性能

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initGraph()
  })
})

onUnmounted(() => {
  if (updateTimer) clearTimeout(updateTimer)
  if (graph.value) {
    try {
      graph.value.destroy()
    } catch (e) {
      console.warn('卸载时销毁图表失败:', e)
    }
  }
})

// 暴露方法
defineExpose({
  getGraph: () => graph.value,
  fitView,
  zoomIn,
  zoomOut,
  resetZoom,
  refresh: initGraph,
  enableWatch: () => { watchEnabled.value = true },
  disableWatch: () => { watchEnabled.value = false }
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

.node-type-select {
  width: 100%;
}

.el-button-group {
  vertical-align: top;
}

:deep(.el-button--small) {
  padding: var(--cn-space-1) var(--cn-space-2);
  font-size: 12px;
}

:deep(.el-dialog__body) {
  padding: var(--cn-space-5);
}

:deep(.el-dialog) {
  border-radius: var(--cn-radius-panel);
  background: var(--cn-color-bg-elevated);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid var(--cn-color-border-subtle);
  margin-right: 0;
}

:deep(.el-form-item__label) {
  color: var(--cn-color-text-secondary);
}
</style> 
