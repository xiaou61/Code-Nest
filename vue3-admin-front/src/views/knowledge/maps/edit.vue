<template>
  <CnPage class="knowledge-editor-page" surface="transparent" max-width="100%" dense full-height>
    <CnPageHeader
      :title="mapInfo.title || '知识图谱'"
      description="编辑知识图谱节点结构、节点链接和可视化画布。"
      eyebrow="Knowledge Map Editor"
      :breadcrumbs="breadcrumbs"
      compact
    >
      <template #meta>
        <CnStatusTag v-if="mapInfo.status !== undefined" :type="getStatusTone(mapInfo.status)" size="sm">
          {{ getStatusText(mapInfo.status) }}
        </CnStatusTag>
        <CnStatusTag type="info" size="sm">节点 {{ nodeCount }} 个</CnStatusTag>
        <CnStatusTag :type="selectedNodeId ? 'brand' : 'neutral'" size="sm">
          已选择 {{ selectedNodeId ? '1' : '0' }} 个
        </CnStatusTag>
      </template>

      <template #actions>
        <el-button :icon="ArrowLeft" @click="goBack">返回列表</el-button>
        <el-button-group>
          <el-button :icon="ZoomOut" @click="handleZoomOut" title="缩小">缩小</el-button>
          <el-button @click="handleResetZoom" title="适应画布">{{ Math.round(zoomLevel * 100) }}%</el-button>
          <el-button :icon="ZoomIn" @click="handleZoomIn" title="放大">放大</el-button>
        </el-button-group>
        <el-button :icon="Search" @click="showSearchDialog = true">搜索节点</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存</el-button>
      </template>
    </CnPageHeader>

    <div class="editor-content">
      <!-- 左侧节点树面板 -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <div class="sidebar-header">
          <h3 v-if="!sidebarCollapsed">节点树</h3>
          <el-button
            text
            :icon="sidebarCollapsed ? Expand : Fold"
            @click="sidebarCollapsed = !sidebarCollapsed"
          />
        </div>

        <div v-if="!sidebarCollapsed" class="sidebar-content">
          <!-- 节点树 -->
          <div class="node-tree">
            <div class="tree-toolbar">
              <el-button size="small" :icon="Plus" @click="handleAddRootNode">
                添加根节点
              </el-button>
            </div>

            <el-tree
              ref="treeRef"
              :data="nodeTree"
              node-key="id"
              :props="treeProps"
              :expand-on-click-node="false"
              :highlight-current="true"
              @node-click="handleNodeClick"
              @node-contextmenu="handleNodeRightClick"
            >
              <template #default="{ data }">
                <div class="tree-node" :class="{ active: selectedNodeId === data.id }">
                  <el-icon class="node-icon" :class="`node-type-${data.nodeType}`">
                    <component :is="getNodeIcon(data.nodeType)" />
                  </el-icon>
                  <span class="node-label">{{ data.title }}</span>
                  <div class="node-actions">
                    <el-button
                      size="small"
                      text
                      :icon="Plus"
                      @click.stop="handleAddChildNode(data)"
                      title="添加子节点"
                    />
                    <el-button
                      size="small"
                      text
                      :icon="Edit"
                      @click.stop="handleEditNode(data)"
                      title="编辑节点"
                    />
                    <el-button
                      size="small"
                      text
                      :icon="Delete"
                      @click.stop="handleDeleteNode(data)"
                      title="删除节点"
                    />
                  </div>
                </div>
              </template>
            </el-tree>
          </div>

          <!-- 节点详情编辑 -->
          <div v-if="selectedNode" class="node-editor">
            <div class="node-editor-header">
              <h4>节点编辑</h4>
            </div>
            <el-form
              ref="nodeFormRef"
              :model="nodeForm"
              :rules="nodeFormRules"
              label-width="80px"
              size="small"
            >
              <el-form-item label="标题" prop="title">
                <el-input v-model="nodeForm.title" placeholder="请输入节点标题" />
              </el-form-item>

              <el-form-item label="类型" prop="nodeType">
                <el-select v-model="nodeForm.nodeType" class="full-width">
                  <el-option label="普通" :value="1" />
                  <el-option label="重点" :value="2" />
                  <el-option label="难点" :value="3" />
                </el-select>
              </el-form-item>

              <el-form-item label="展开" prop="isExpanded">
                <el-switch v-model="nodeForm.isExpanded" />
              </el-form-item>

              <el-form-item label="链接" prop="url">
                <el-input
                  v-model="nodeForm.url"
                  placeholder="请输入飞书云文档链接"
                  clearable
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" size="small" @click="handleSaveNode">
                  保存节点
                </el-button>
                <el-button size="small" @click="handleCancelEdit">
                  取消
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </aside>

      <!-- 主编辑画布 -->
      <section class="canvas-container" v-loading="loading">
        <div class="canvas-toolbar">
          <div class="canvas-info">
            <CnStatusTag type="info" size="sm">节点总数 {{ nodeCount }}</CnStatusTag>
            <CnStatusTag :type="selectedNodeId ? 'brand' : 'neutral'" size="sm">
              已选择 {{ selectedNodeId ? '1' : '0' }} 个节点
            </CnStatusTag>
          </div>
        </div>

        <!-- G6思维导图组件 -->
        <MindMap
          ref="mindMapRef"
          :data="mindMapData"
          :editable="true"
          :show-toolbar="false"
          height="100%"
          @node-click="handleMindMapNodeClick"
          @node-dblclick="handleEditNode"
          @node-add="handleAddNodeFromMindMap"
          @node-delete="handleDeleteNodeFromMindMap"
          @node-update="handleUpdateNodeFromMindMap"
        />

        <CnEmptyState
          v-if="!nodeTree.length"
          class="empty-canvas"
          title="暂无节点"
          description="点击添加根节点开始创建知识图谱结构。"
          icon="KM"
          surface="transparent"
        >
          <template #actions>
            <el-button type="primary" :icon="Plus" @click="handleAddRootNode">添加根节点</el-button>
          </template>
        </CnEmptyState>
      </section>
    </div>

    <!-- 搜索节点对话框 -->
    <el-dialog
      v-model="showSearchDialog"
      title="搜索节点"
      width="500px"
    >
      <el-input
        v-model="searchKeyword"
        placeholder="输入关键词搜索节点"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />

      <div v-if="searchResults.length" class="search-results">
        <div
          v-for="result in searchResults"
          :key="result.id"
          class="search-result-item"
          @click="handleSelectSearchResult(result)"
        >
          <div class="result-title">{{ result.title }}</div>
          <div class="result-content">{{ result.url || '暂无链接' }}</div>
        </div>
      </div>
      <CnEmptyState
        v-else-if="searchKeyword"
        class="search-empty"
        title="暂无搜索结果"
        description="换一个关键词再试试。"
        icon="SE"
        size="sm"
        surface="transparent"
      />

      <template #footer>
        <el-button @click="showSearchDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </template>
    </el-dialog>

    <!-- 右键菜单 -->
    <el-menu
      v-show="contextMenuVisible"
      class="context-menu"
      :style="contextMenuStyle"
      mode="vertical"
    >
      <el-menu-item index="add-child" @click="handleContextMenuAdd">
        <el-icon><Plus /></el-icon>
        添加子节点
      </el-menu-item>
      <el-menu-item index="edit" @click="handleContextMenuEdit">
        <el-icon><Edit /></el-icon>
        编辑节点
      </el-menu-item>
      <el-menu-item index="delete" @click="handleContextMenuDelete">
        <el-icon><Delete /></el-icon>
        删除节点
      </el-menu-item>
    </el-menu>
  </CnPage>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick, watch, onUnmounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, ZoomIn, ZoomOut, Search, Plus, Edit, Delete,
  Expand, Fold, Document, Star, Warning
} from '@element-plus/icons-vue'
import {
  getKnowledgeMapById,
  getKnowledgeNodeTree,
  createKnowledgeNode,
  updateKnowledgeNode,
  deleteKnowledgeNode,
  searchKnowledgeNodes
} from '@/api/knowledge'
import { CnEmptyState, CnPage, CnPageHeader, CnStatusTag } from '@/design-system'
import type { CnBreadcrumbItem, CnTone } from '@/design-system'

const MindMap = defineAsyncComponent(() => import('@/components/MindMap.vue'))

const route = useRoute()
const router = useRouter()
const breadcrumbs: CnBreadcrumbItem[] = [{ label: '管理后台' }, { label: '知识图谱', to: '/knowledge/maps' }, { label: '结构编辑' }]

// 基础数据
const mapId = ref(route.params.id)
const mapInfo = ref({})
const nodeTree = ref([])
const selectedNode = ref(null)
const selectedNodeId = ref(null)

// UI状态
const loading = ref(false)
const sidebarCollapsed = ref(false)
const zoomLevel = ref(1)
const showSearchDialog = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])

// 上下文菜单
const contextMenuVisible = ref(false)
const contextMenuStyle = ref({})
const contextMenuNode = ref(null)

// 表单相关
const nodeForm = reactive({
  title: '',
  url: '',
  nodeType: 1,
  isExpanded: true
})

const nodeFormRef = ref()

const nodeFormRules = {
  title: [
    { required: true, message: '请输入节点标题', trigger: 'blur' },
    { max: 200, message: '标题长度不能超过200个字符', trigger: 'blur' }
  ]
}

// 树配置
const treeProps = {
  children: 'children',
  label: 'title'
}

const treeRef = ref()
const mindMapRef = ref()

// 计算属性
const nodeCount = computed(() => {
  const countNodes = (nodes) => {
    let count = 0
    for (const node of nodes) {
      count++
      if (node.children && node.children.length) {
        count += countNodes(node.children)
      }
    }
    return count
  }
  return countNodes(nodeTree.value)
})

// 转换为G6格式的思维导图数据 - 添加更强的缓存机制
let cachedMindMapData = null
let lastNodeTreeHash = ''

const mindMapData = computed(() => {
  if (!nodeTree.value || nodeTree.value.length === 0) {
    return { nodes: [], edges: [] }
  }

  // 简单的哈希检查，避免不必要的重计算
  const currentHash = JSON.stringify(nodeTree.value.map(n => ({ id: n.id, title: n.title, children: n.children?.length || 0 })))

  if (currentHash === lastNodeTreeHash && cachedMindMapData) {
    return cachedMindMapData
  }

  lastNodeTreeHash = currentHash

  const nodes = []
  const edges = []

  const processNode = (node, parent = null) => {
    nodes.push({
      id: node.id.toString(),
      title: node.title,
      description: node.url || '',
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

  try {
    nodeTree.value.forEach(rootNode => processNode(rootNode))
    cachedMindMapData = { nodes, edges }
    return cachedMindMapData
  } catch (error) {
    console.error('📊 mindMapData计算出错:', error)
    return { nodes: [], edges: [] }
  }
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
  } catch (error) {
    ElMessage.error('获取图谱信息失败')
  }
}

const fetchNodeTree = async () => {
  try {
    loading.value = true
    const data = await getKnowledgeNodeTree(mapId.value)
    nodeTree.value = data

    // 清理缓存，确保数据更新
    cachedMindMapData = null
    lastNodeTreeHash = ''

    // 更新节点Map缓存
    refreshNodeMap()

  } catch (error) {
    ElMessage.error('获取节点树失败')
  } finally {
    loading.value = false
  }
}

// 优化节点查找性能 - 使用Map缓存
const nodeMap = new Map()
const updateNodeMap = () => {
  nodeMap.clear()
  const buildMap = (nodes) => {
    for (const node of nodes) {
      // 存储数据副本，避免直接引用
      nodeMap.set(node.id.toString(), { ...node })
      if (node.children && node.children.length) {
        buildMap(node.children)
      }
    }
  }
  buildMap(nodeTree.value)
}

// 手动更新缓存，移除深度监听避免性能问题
const refreshNodeMap = () => {
  updateNodeMap()
}

const handleNodeClick = (data) => {
  selectedNodeId.value = data.id
  selectedNode.value = { ...data } // 使用副本避免引用问题

  // 使用Map快速查找，避免递归遍历
  const fullNodeData = nodeMap.get(data.id.toString())

  if (fullNodeData) {
    // 填充编辑表单 - 使用数据副本
    Object.assign(nodeForm, {
      title: fullNodeData.title,
      url: fullNodeData.url,
      nodeType: fullNodeData.nodeType,
      isExpanded: fullNodeData.isExpanded
    })
  }
}

// 专门处理MindMap组件中节点点击的方法
const handleMindMapNodeClick = (nodeData) => {
  // 简化处理，只更新选中状态，避免复杂操作
  if (nodeData.id) {
    selectedNodeId.value = nodeData.id.toString()

    // 使用Map快速查找完整数据
    const fullNodeData = nodeMap.get(nodeData.id.toString())
    if (fullNodeData) {
      selectedNode.value = { ...fullNodeData } // 使用副本

      // 填充编辑表单
      Object.assign(nodeForm, {
        title: fullNodeData.title,
        url: fullNodeData.url,
        nodeType: fullNodeData.nodeType,
        isExpanded: fullNodeData.isExpanded
      })

      // 同步左侧树的选中状态
      nextTick(() => {
        treeRef.value?.setCurrentKey(fullNodeData.id)
      })
    }
  }
}

// 处理思维导图组件的节点添加事件
const handleAddNodeFromMindMap = async (nodeData) => {
  try {
    const createData = {
      parentId: nodeData.parentId || 0,
      title: nodeData.title,
      url: nodeData.description || '',
      nodeType: getNodeTypeNumber(nodeData.nodeType),
      sortOrder: 0,
      isExpanded: true
    }

    await createKnowledgeNode(mapId.value, createData)
    ElMessage.success('节点创建成功')

    // 刷新节点树
    await fetchNodeTree()

  } catch (error) {
    ElMessage.error('创建节点失败')
  }
}

// 处理思维导图组件的节点删除事件
const handleDeleteNodeFromMindMap = async (nodeData) => {
  try {
    await ElMessageBox.confirm(`确认删除节点"${nodeData.title}"？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })

    await deleteKnowledgeNode(parseInt(nodeData.id))
    ElMessage.success('节点删除成功')

    // 清空选中状态
    if (selectedNodeId.value === nodeData.id) {
      selectedNodeId.value = null
      selectedNode.value = null
      resetNodeForm()
    }

    // 刷新节点树
    await fetchNodeTree()

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除节点失败')
    }
  }
}

// 处理思维导图组件的节点更新事件
const handleUpdateNodeFromMindMap = async (nodeData) => {
  try {
    const updateData = {
      title: nodeData.title,
      url: nodeData.description || '',
      nodeType: getNodeTypeNumber(nodeData.nodeType),
      isExpanded: true
    }

    await updateKnowledgeNode(parseInt(nodeData.id), updateData)
    ElMessage.success('节点更新成功')

    // 刷新节点树
    await fetchNodeTree()

  } catch (error) {
    ElMessage.error('更新节点失败')
  }
}

const getNodeTypeNumber = (nodeTypeString) => {
  const typeMap = { 'normal': 1, 'important': 2, 'category': 3 }
  return typeMap[nodeTypeString] || 1
}

const handleAddRootNode = () => {
  handleAddChildNode({ id: 0, title: '根节点' })
}

const handleAddChildNode = async (parentNode) => {
  try {
    const { value: title } = await ElMessageBox.prompt('请输入节点标题', '添加节点', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPlaceholder: '节点标题',
      inputValidator: (value) => {
        if (!value || !value.trim()) {
          return '请输入节点标题'
        }
        if (value.length > 200) {
          return '标题长度不能超过200个字符'
        }
        return true
      }
    })

    const nodeData = {
      parentId: parentNode.id,
      title: title.trim(),
      url: '',
      nodeType: 1,
      sortOrder: 0,
      isExpanded: true
    }

    await createKnowledgeNode(mapId.value, nodeData)
    ElMessage.success('节点创建成功')

    // 刷新节点树
    await fetchNodeTree()

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('创建节点失败')
    }
  }
}

const handleEditNode = (data) => {
  handleNodeClick(data)
}

const handleDeleteNode = async (data) => {
  try {
    await ElMessageBox.confirm(`确认删除节点"${data.title}"？`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })

    await deleteKnowledgeNode(data.id)
    ElMessage.success('节点删除成功')

    // 清空选中状态
    if (selectedNodeId.value === data.id) {
      selectedNodeId.value = null
      selectedNode.value = null
      resetNodeForm()
    }

    // 刷新节点树
    await fetchNodeTree()

  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除节点失败')
    }
  }
}

const handleSaveNode = async () => {
  if (!selectedNode.value) {
    return
  }

  try {
    // 临时禁用MindMap的watch监听
    mindMapRef.value?.disableWatch()

    // 临时跳过表单验证，直接保存
    await updateKnowledgeNode(selectedNode.value.id, nodeForm)

    ElMessage.success('节点保存成功')

    // 重新启用MindMap监听
    setTimeout(() => {
      mindMapRef.value?.enableWatch()
    }, 1000)

  } catch (error) {
    console.error('❌ 保存节点失败:', error)
    ElMessage.error('保存节点失败: ' + (error.message || '未知错误'))

    // 确保在错误情况下也重新启用监听
    mindMapRef.value?.enableWatch()
  }
}

const handleCancelEdit = () => {
  selectedNodeId.value = null
  selectedNode.value = null
  resetNodeForm()
}

const handleSave = () => {
  ElMessage.success('图谱保存成功')
}

let refreshTimer = null

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  try {
    const data = await searchKnowledgeNodes(mapId.value, searchKeyword.value)
    searchResults.value = data

    if (!data.length) {
      ElMessage.info('未找到相关节点')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  }
}

const handleSelectSearchResult = (result) => {
  showSearchDialog.value = false
  searchKeyword.value = ''
  searchResults.value = []

  // 选中节点并定位
  handleNodeClick(result)

  // 展开树到该节点
  treeRef.value?.setCurrentKey(result.id)
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
  try {
    // 主动清理定时器
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }

    // 禁用MindMap监听
    if (mindMapRef.value) {
      mindMapRef.value.disableWatch?.()
    }

    // 清理缓存
    nodeMap.clear()
    cachedMindMapData = null
    lastNodeTreeHash = ''

    // 延迟跳转，确保清理完成
    setTimeout(() => {
      router.push('/knowledge/maps')
    }, 100)

  } catch (error) {
    console.error('清理资源时出错:', error)
    // 即使出错也要跳转
    router.push('/knowledge/maps')
  }
}

// 右键菜单相关
const handleNodeRightClick = (event, data) => {
  event.preventDefault()
  contextMenuNode.value = data
  showContextMenu(event)
}

const showContextMenu = (event) => {
  contextMenuVisible.value = true
  contextMenuStyle.value = {
    left: `${event.clientX}px`,
    top: `${event.clientY}px`,
    position: 'fixed',
    zIndex: 9999
  }

  // 点击其他地方隐藏菜单
  const hideMenu = () => {
    contextMenuVisible.value = false
    document.removeEventListener('click', hideMenu)
  }

  setTimeout(() => {
    document.addEventListener('click', hideMenu)
  }, 100)
}

const handleContextMenuAdd = () => {
  if (contextMenuNode.value) {
    handleAddChildNode(contextMenuNode.value)
  }
  contextMenuVisible.value = false
}

const handleContextMenuEdit = () => {
  if (contextMenuNode.value) {
    handleEditNode(contextMenuNode.value)
  }
  contextMenuVisible.value = false
}

const handleContextMenuDelete = () => {
  if (contextMenuNode.value) {
    handleDeleteNode(contextMenuNode.value)
  }
  contextMenuVisible.value = false
}

// 辅助方法
const getNodeIcon = (nodeType) => {
  const iconMap = {
    1: Document,  // 普通
    2: Star,      // 重点
    3: Warning    // 难点
  }
  return iconMap[nodeType] || Document
}

const getStatusTone = (status): CnTone => {
  const typeMap: Record<number, CnTone> = {
    0: 'info',     // 草稿
    1: 'success',  // 已发布
    2: 'warning'   // 已隐藏
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    0: '草稿',
    1: '已发布',
    2: '已隐藏'
  }
  return textMap[status] || '未知'
}

// 节点高亮由MindMap组件自动处理

const resetNodeForm = () => {
  Object.assign(nodeForm, {
    title: '',
    url: '',
    nodeType: 1,
    isExpanded: true
  })
  nodeFormRef.value?.resetFields()
}

// 生命周期
onMounted(async () => {
  await fetchMapInfo()
  await fetchNodeTree()
  // 初始化节点Map缓存
  refreshNodeMap()
})

// 组件销毁时清理资源
onUnmounted(() => {
  // 清理定时器
  if (refreshTimer) {
    clearTimeout(refreshTimer)
    refreshTimer = null
  }

  // 清理MindMap相关资源
  if (mindMapRef.value) {
    try {
      mindMapRef.value.disableWatch?.()
    } catch (e) {
      console.warn('清理MindMap时出错:', e)
    }
  }

  // 清理缓存
  nodeMap.clear()
  cachedMindMapData = null
  lastNodeTreeHash = ''

  // 重置状态
  selectedNodeId.value = null
  selectedNode.value = null
  nodeTree.value = []
})

// 监听路由变化
watch(() => route.params.id, (newId) => {
  if (newId && newId !== mapId.value) {
    mapId.value = newId
    fetchMapInfo()
    fetchNodeTree()
  }
})
</script>

<style scoped>
.knowledge-editor-page {
  min-height: calc(100vh - var(--cn-space-6));
}

.knowledge-editor-page :deep(.cn-page__body) {
  grid-template-rows: auto minmax(0, 1fr);
  height: calc(100vh - 2 * var(--cn-space-4));
}

.editor-content {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--cn-panel-border);
  border-radius: var(--cn-radius-panel);
  background: var(--cn-panel-bg);
  box-shadow: var(--cn-shadow-card);
}

.sidebar {
  width: 320px;
  background: var(--cn-color-bg-surface);
  border-right: 1px solid var(--cn-color-border-subtle);
  display: flex;
  flex-direction: column;
  transition: width var(--cn-motion-base) var(--cn-ease-out);
}

.sidebar.collapsed {
  width: 48px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  color: var(--cn-color-text-primary);
}

.sidebar-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.node-tree {
  border-bottom: 1px solid var(--cn-color-border-subtle);
  max-height: 50%;
  overflow-y: auto;
}

.tree-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--cn-color-border-subtle);
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  padding: 4px 0;
  border-radius: var(--cn-radius-card);
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    color var(--cn-motion-fast) var(--cn-ease-out);
}

.tree-node:hover {
  background: var(--cn-color-bg-surface-muted);
}

.tree-node.active {
  background: var(--cn-color-brand-soft);
  color: var(--cn-color-brand-primary);
}

.node-icon {
  font-size: 16px;
}

.node-icon.node-type-1 { color: var(--cn-color-text-tertiary); }
.node-icon.node-type-2 { color: var(--cn-color-danger); }
.node-icon.node-type-3 { color: var(--cn-color-warning); }

.node-label {
  flex: 1;
  font-size: 13px;
  line-height: 1.4;
}

.node-actions {
  display: none;
  gap: 2px;
}

.tree-node:hover .node-actions {
  display: flex;
}

.node-editor {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.node-editor-header {
  margin-bottom: 16px;
  padding: 0;
  border: none;
  box-shadow: none;
}

.node-editor h4 {
  margin: 0;
  font-size: 14px;
  color: var(--cn-color-text-primary);
}

.canvas-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--cn-color-bg-surface);
  position: relative;
  min-width: 0;
}

.canvas-toolbar {
  padding: 12px 20px;
  border-bottom: 1px solid var(--cn-color-border-subtle);
  background: var(--cn-color-bg-surface-muted);
}

.canvas-info {
  display: flex;
  flex-wrap: wrap;
  gap: var(--cn-space-2);
  font-size: 12px;
  color: var(--cn-color-text-secondary);
}

.empty-canvas {
  position: absolute;
  inset: var(--cn-space-6);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--cn-color-text-secondary);
  pointer-events: auto;
}

.search-results {
  max-height: 300px;
  margin-top: var(--cn-space-4);
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  margin-bottom: 8px;
  cursor: pointer;
  transition:
    background-color var(--cn-motion-fast) var(--cn-ease-out),
    border-color var(--cn-motion-fast) var(--cn-ease-out);
}

.search-result-item:hover {
  border-color: var(--cn-color-brand-primary);
  background: var(--cn-color-brand-soft);
}

.result-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--cn-color-text-primary);
  margin-bottom: 4px;
}

.result-content {
  font-size: 12px;
  color: var(--cn-color-text-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.search-empty {
  margin-top: var(--cn-space-4);
}

.context-menu {
  position: fixed;
  background: var(--cn-color-bg-surface);
  border: 1px solid var(--cn-color-border-subtle);
  border-radius: var(--cn-radius-card);
  box-shadow: var(--cn-shadow-popover);
  padding: 4px 0;
  min-width: 120px;
}

.context-menu .el-menu-item {
  height: 32px;
  line-height: 32px;
  padding: 0 16px;
  font-size: 13px;
}

/* Element Plus 样式覆盖 */
:deep(.el-tree-node__content) {
  height: auto;
  padding: 4px 8px;
}

:deep(.el-tree-node__expand-icon) {
  padding: 2px;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-form-item__label) {
  font-size: 12px;
}

.full-width {
  width: 100%;
}
</style>
