# 知识图谱模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
为了帮助用户更好地整理和复习面试题知识点，提供一个类似思维导图的可视化知识管理系统。采用**官方创建、用户学习**的模式，管理员在后台通过可视化界面创建和管理知识图谱，用户在前端查看和学习。通过树形结构展示知识点层级关系，支持节点展开/收缩，点击节点可查看详细的Markdown格式解释内容。

### 💡 核心价值
- **官方内容保障**: 管理员统一创建和维护，确保内容质量和权威性
- **可视化知识结构**: 通过现代化思维导图直观展示知识点层级关系
- **分层次学习**: 支持从概览到细节的渐进式学习方式  
- **高效复习**: 快速定位知识点，弹窗查看详细内容
- **知识沉淀**: 系统化整理面试相关知识点

## 🚀 功能需求

### 1. 管理员端功能 (后台创建管理)

#### 1.1 知识图谱管理
- **图谱创建**: 支持创建新的知识图谱主题，设置标题、描述、封面等
- **图谱列表**: 展示所有创建的知识图谱，支持搜索、筛选、排序
- **图谱设置**: 配置图谱可见性、分类、标签等属性
- **批量操作**: 支持批量发布、隐藏、删除图谱

#### 1.2 可视化节点编辑
- **拖拽创建**: 通过拖拽方式添加新节点到指定位置
- **可视化编辑**: 在思维导图上直接编辑节点内容和层级关系
- **节点管理**: 添加/编辑/删除知识点节点，支持批量操作
- **层级调整**: 支持拖拽调整节点层级和位置关系
- **节点样式**: 设置不同类型节点的颜色、图标、样式

#### 1.3 内容编辑器
- **Markdown编辑器**: 富文本编辑器支持Markdown语法
- **实时预览**: 编辑时实时预览渲染效果
- **模板功能**: 提供常用内容模板快速填充
- **媒体管理**: 支持图片、代码块等多媒体内容插入

### 2. 用户端功能 (前端学习查看)

#### 2.1 思维导图浏览
- **现代化渲染**: 使用G6引擎渲染美观的思维导图
- **节点展开/收缩**: 支持逐级展开查看，记住展开状态
- **节点样式**: 不同类型节点显示不同颜色/图标
- **缩放平移**: 支持画布缩放和平移操作，触摸屏友好

#### 2.2 内容详情查看
- **弹窗展示**: 点击节点弹出详情弹窗，支持键盘快捷键
- **Markdown渲染**: 支持完整Markdown格式内容渲染
- **代码高亮**: 支持多语言代码块语法高亮
- **图片支持**: 支持图片查看、缩放功能

#### 2.3 学习辅助功能
- **全文搜索**: 支持在图谱中搜索关键词
- **节点定位**: 搜索结果快速定位到相关节点
- **搜索高亮**: 搜索关键词在节点中高亮显示
- **收藏功能**: 支持收藏感兴趣的知识图谱

## 🎨 界面设计

### 1. 管理员端 - 图谱管理页面
```
+--------------------------------------------------+
|  📊 知识图谱管理                    [+ 新建图谱] |
|                                                  |
|  🔍 [搜索框] [筛选▼] [排序▼]          [批量操作▼] |
|                                                  |
|  📚 Java面试知识点                  [已发布] ✅  |
|  └─ 47个节点 | 235次查看 | 2024-01-15           |
|       [编辑结构][编辑信息][复制][删除]           |
|                                                  |
|  🌐 前端技术栈                      [草稿] 📝    |
|  └─ 23个节点 | 0次查看 | 2024-01-16             |
|       [编辑结构][编辑信息][发布][删除]           |
|                                                  |
|  🔒 数据结构与算法                  [已隐藏] 👁    |
|  └─ 89个节点 | 456次查看 | 2024-01-10           |
|       [编辑结构][编辑信息][显示][删除]           |
+--------------------------------------------------+
```

### 2. 管理员端 - 可视化编辑页面
```
+--------------------------------------------------+
|  🔙 返回列表  | [📝 编辑: Java面试知识点]  💾 保存 |
+--------------------------------------------------+
| 工具栏: [➕节点] [🗑️删除] [📋复制] [📝编辑] [🎨样式] |
+--------------------------------------------------+
|  左侧面板          |           编辑画布            |
|                   |                              |
| 📁 节点树形列表    |     📊 Java面试知识点        |
| ├─ 📝 基础语法     |      ├─ 📝 基础语法 ●        |  
| │  ├─ 数据类型 ●   |      │   ├─ 数据类型 ●       |
| │  ├─ 运算符 ●     |      │   ├─ 运算符 ●         |
| │  └─ 控制结构 ●   |      │   └─ 控制结构 ●       |
| ├─ 🗂️ 集合框架     |      ├─ 🗂️ 集合框架          |
| └─ ⚡ 多线程        |      │   ├─ List ●          |
|                   |      │   ├─ Set ●           |
| 📝 内容编辑器      |      │   └─ Map ●           |
| [Markdown编辑器]   |      └─ ⚡ 多线程             |
| [实时预览]         |          ├─ 线程创建 ●       |
|                   |          └─ 同步机制 ●       |
+--------------------------------------------------+
```

### 3. 用户端 - 图谱浏览页面
```
+--------------------------------------------------+
|  🏠 首页  | 知识图谱 |  🔍 搜索  ⭐ 收藏  👤 个人 |
+--------------------------------------------------+
|                                                  |
|  📚 推荐学习                                     |
|                                                  |
|  📊 Java面试知识点            👀 235  ⭐ 收藏    |
|  └─ 全面的Java面试题知识整理                     |
|      [开始学习] [查看详情]                       |
|                                                  |
|  🌐 前端技术栈                👀 189  ⭐ 收藏    |
|  └─ 前端面试必备知识点汇总                       |
|      [开始学习] [查看详情]                       |
|                                                  |
|  🔒 数据结构与算法            👀 456  ⭐ 收藏    |
|  └─ 算法面试核心知识点梳理                       |
|      [开始学习] [查看详情]                       |
+--------------------------------------------------+
```

### 4. 用户端 - 思维导图学习页面
```
+--------------------------------------------------+
|  🔙 返回  | Java面试知识点 |  🔍 搜索  ⭐ 收藏夹 |
+--------------------------------------------------+
|                                                  |
|          📊 Java面试知识点                      |
|           ├─ 📝 基础语法                        |
|           │   ├─ 数据类型 ●                     |
|           │   ├─ 运算符 ●                       |
|           │   └─ 控制结构 ●                     |
|           ├─ 🗂️ 集合框架                        |
|           │   ├─ List ●                         |
|           │   ├─ Set ●                          |
|           │   └─ Map ●                          |
|           └─ ⚡ 多线程                           |
|               ├─ 线程创建 ●                     |
|               ├─ 同步机制 ●                     |
|               └─ 线程池 ●                       |
|                                                  |
| 💡 提示: 点击节点查看详细内容，支持键盘方向键导航 |
+--------------------------------------------------+
```

### 5. 节点详情弹窗 (用户端只读)
```
+--------------------------------------------------+
|  🔷 HashMap实现原理                        ✕     |
+--------------------------------------------------+
|                                                  |
|  ## HashMap底层实现                             |
|                                                  |
|  HashMap在Java8之前采用**数组+链表**的数据结构:   |
|                                                  |
|  ```java                                        |
|  public class HashMap<K,V> {                    |
|      transient Node<K,V>[] table;              |
|      // ...                                     |
|  }                                               |
|  ```                                            |
|                                                  |
|  ### 关键特性                                   |
|  - 初始容量: 16                                 |
|  - 负载因子: 0.75                               |
|  - 扩容机制: 容量翻倍                            |
|                                                  |
|                    [⬅️ 上一个] [下一个 ➡️] [关闭]  |
+--------------------------------------------------+
```

## 🗄️ 数据库设计

### 1. 知识图谱表 (knowledge_map)
```sql
CREATE TABLE knowledge_map (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '图谱标题',
    description TEXT COMMENT '图谱描述', 
    user_id BIGINT NOT NULL COMMENT '创建用户ID',
    cover_image VARCHAR(500) COMMENT '封面图片',
    node_count INT DEFAULT 0 COMMENT '节点总数',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
);
```

### 2. 知识节点表 (knowledge_node)
```sql
CREATE TABLE knowledge_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    map_id BIGINT NOT NULL COMMENT '所属图谱ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父节点ID，0为根节点',
    title VARCHAR(200) NOT NULL COMMENT '节点标题',
    content LONGTEXT COMMENT '节点详细内容(Markdown)',
    node_type TINYINT(1) DEFAULT 1 COMMENT '节点类型: 1-普通 2-重点 3-难点',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    level_depth INT DEFAULT 1 COMMENT '层级深度',
    is_expanded TINYINT(1) DEFAULT 1 COMMENT '是否默认展开',
    view_count INT DEFAULT 0 COMMENT '查看次数',
    last_view_time DATETIME COMMENT '最后查看时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_map_id (map_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order),
    FOREIGN KEY (map_id) REFERENCES knowledge_map(id) ON DELETE CASCADE
);
```

### 3. 图谱收藏表 (knowledge_favorite)
```sql
CREATE TABLE knowledge_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    map_id BIGINT NOT NULL COMMENT '图谱ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_map_id (map_id),
    UNIQUE KEY uk_user_map (user_id, map_id),
    FOREIGN KEY (map_id) REFERENCES knowledge_map(id) ON DELETE CASCADE
);
```

## 🔌 API接口设计

### 1. 管理员端接口

#### 1.1 图谱管理
```http
# 获取管理员图谱列表
GET /api/admin/knowledge/maps?page=1&size=10&keyword=Java&status=published

# 创建图谱
POST /api/admin/knowledge/maps
Content-Type: application/json
{
  "title": "Java面试知识点",
  "description": "Java相关面试题整理",
  "coverImage": "https://...",
  "status": "draft"
}

# 更新图谱信息
PUT /api/admin/knowledge/maps/{mapId}

# 发布图谱
POST /api/admin/knowledge/maps/{mapId}/publish

# 删除图谱
DELETE /api/admin/knowledge/maps/{mapId}
```

#### 1.2 节点管理
```http
# 获取图谱节点树 (管理员版，包含所有信息)
GET /api/admin/knowledge/maps/{mapId}/nodes

# 创建节点
POST /api/admin/knowledge/maps/{mapId}/nodes
Content-Type: application/json
{
  "parentId": 1,
  "title": "HashMap实现原理", 
  "content": "## HashMap底层实现\n...",
  "nodeType": 2,
  "sortOrder": 1
}

# 批量更新节点顺序
PUT /api/admin/knowledge/maps/{mapId}/nodes/sort
Content-Type: application/json
{
  "nodeOrders": [
    {"nodeId": 1, "parentId": 0, "sortOrder": 1},
    {"nodeId": 2, "parentId": 1, "sortOrder": 1}
  ]
}

# 更新节点
PUT /api/admin/knowledge/nodes/{nodeId}

# 删除节点
DELETE /api/admin/knowledge/nodes/{nodeId}
```

### 2. 用户端接口

#### 2.1 图谱浏览
```http
# 获取已发布的图谱列表
GET /api/pub/knowledge/maps?page=1&size=10&keyword=Java

# 获取图谱详情
GET /api/pub/knowledge/maps/{mapId}

# 获取图谱节点树 (用户版，只显示已发布内容)
GET /api/pub/knowledge/maps/{mapId}/nodes
```

#### 2.2 收藏功能
```http
# 收藏图谱
POST /api/knowledge/favorites
Content-Type: application/json
{
  "mapId": 1
}

# 取消收藏
DELETE /api/knowledge/favorites/{mapId}

# 获取收藏列表
GET /api/knowledge/favorites?page=1&size=10
```

#### 2.3 搜索功能
```http
# 搜索图谱
GET /api/pub/knowledge/search?q=HashMap&mapId=1

# 搜索结果高亮
GET /api/pub/knowledge/search/highlight?q=HashMap&nodeId=10
```

## ⚙️ 技术实现方案

### 1. 前端技术栈
- **基础框架**: Vue 3 + Composition API
- **图形渲染**: G6 (AntV) - 现代化图形渲染引擎
- **Markdown渲染**: markdown-it + highlight.js + KaTeX (数学公式)
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **编辑器**: Monaco Editor (代码编辑) + Markdown编辑器

### 2. 核心组件设计

#### 2.1 管理员端可视化编辑器 (AdminMindMapEditor.vue)
```vue
<template>
  <div class="admin-mind-map-editor">
    <div class="editor-toolbar">
      <el-button-group>
        <el-button :icon="Plus" @click="addNode">新增节点</el-button>
        <el-button :icon="Edit" @click="editNode" :disabled="!selectedNode">编辑</el-button>
        <el-button :icon="Delete" @click="deleteNode" :disabled="!selectedNode">删除</el-button>
      </el-button-group>
      <el-button type="primary" @click="saveMap">保存</el-button>
    </div>
    
    <div class="editor-content">
      <!-- 左侧节点树 -->
      <div class="sidebar">
        <NodeTreePanel v-model:selected="selectedNode" :nodes="nodes" />
        <NodeEditor v-model:node="selectedNode" @save="onNodeSave" />
      </div>
      
      <!-- 主编辑画布 -->
      <div class="canvas-container">
        <div ref="mindMapRef" class="mind-map-canvas"></div>
      </div>
    </div>
  </div>
</template>
```

#### 2.2 用户端思维导图查看器 (UserMindMapViewer.vue)
```vue
<template>
  <div class="user-mind-map-viewer">
    <div class="viewer-toolbar">
      <el-input 
        v-model="searchKeyword" 
        placeholder="搜索知识点..." 
        :prefix-icon="Search"
        @input="onSearch"
      />
      <el-button :icon="Star" @click="toggleFavorite">
        {{ isFavorited ? '取消收藏' : '收藏' }}
      </el-button>
    </div>
    
    <div ref="mindMapRef" class="mind-map-canvas"></div>
    
    <!-- 节点详情弹窗 (只读) -->
    <NodeDetailDialog 
      v-model="dialogVisible"
      :node="selectedNode"
      :readonly="true"
      @navigate="onNavigate"
    />
  </div>
</template>
```

#### 2.3 节点详情弹窗 (NodeDetailDialog.vue)
```vue
<template>
  <el-dialog 
    v-model="visible"
    :title="node?.title"
    width="900px"
    class="node-detail-dialog"
    :before-close="onClose"
  >
    <div class="markdown-content" v-html="renderedContent"></div>
    
    <template #footer>
      <div class="dialog-footer">
        <div class="navigation" v-if="readonly">
          <el-button :disabled="!hasPrev" @click="$emit('navigate', 'prev')">
            <ArrowLeft /> 上一个
          </el-button>
          <el-button :disabled="!hasNext" @click="$emit('navigate', 'next')">
            下一个 <ArrowRight />
          </el-button>
        </div>
        <div class="actions">
          <el-button @click="visible = false">关闭</el-button>
          <el-button v-if="!readonly" type="primary" @click="editNode">编辑</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>
```

### 3. 后端模块结构 (xiaou-knowledge)
```
xiaou-knowledge/
├── controller/
│   ├── admin/                         # 管理员端接口
│   │   ├── AdminKnowledgeMapController.java
│   │   └── AdminKnowledgeNodeController.java  
│   └── pub/                           # 公开接口 (用户端)
│       ├── PubKnowledgeMapController.java
│       └── PubKnowledgeViewController.java
│   └── app/                           # 用户功能接口
│       └── KnowledgeFavoriteController.java
├── domain/
│   ├── KnowledgeMap.java              # 知识图谱实体
│   ├── KnowledgeNode.java             # 知识节点实体
│   └── KnowledgeFavorite.java         # 图谱收藏实体
├── dto/
│   ├── request/
│   │   ├── CreateKnowledgeMapRequest.java
│   │   ├── CreateKnowledgeNodeRequest.java
│   │   └── SortNodesRequest.java
│   ├── response/
│   │   ├── KnowledgeMapListResponse.java
│   │   ├── KnowledgeNodeTreeResponse.java
│   │   └── KnowledgeSearchResponse.java
│   └── KnowledgeMapDto.java
├── service/
│   ├── KnowledgeMapService.java       # 图谱管理服务
│   ├── KnowledgeNodeService.java      # 节点管理服务
│   ├── KnowledgeFavoriteService.java  # 收藏功能服务
│   └── impl/
│       ├── KnowledgeMapServiceImpl.java
│       ├── KnowledgeNodeServiceImpl.java
│       └── KnowledgeFavoriteServiceImpl.java
└── mapper/
    ├── KnowledgeMapMapper.java
    ├── KnowledgeNodeMapper.java
    └── KnowledgeFavoriteMapper.java
```

## 📋 开发计划

### Phase 1: 基础架构 (1周)
- [ ] 创建xiaou-knowledge独立模块
- [ ] 数据库设计与创建
- [ ] 后端基础架构搭建
- [ ] 前端路由和基础页面框架

### Phase 2: 管理员端功能 (3周)  
- [ ] 管理员图谱管理界面
- [ ] G6思维导图可视化编辑器集成
- [ ] 拖拽式节点创建和编辑功能
- [ ] Markdown编辑器和实时预览
- [ ] 图谱发布和状态管理

### Phase 3: 用户端功能 (2周)
- [ ] 用户端图谱浏览界面
- [ ] G6思维导图展示优化
- [ ] 节点详情弹窗和导航
- [ ] 收藏功能实现
- [ ] 搜索和高亮功能

### Phase 4: 优化完善 (1周)
- [ ] 性能优化和缓存策略
- [ ] 移动端适配优化
- [ ] 用户体验细节优化  
- [ ] 测试与bug修复

## 🎯 成功指标

### 功能指标
- 支持创建无限层级的知识图谱  
- 单个图谱支持500+节点流畅展示
- 节点详情加载时间 < 200ms
- 图谱渲染时间 < 800ms
- 管理员编辑操作响应时间 < 300ms

### 用户体验指标
- 管理员编辑界面操作流畅度 > 95%
- 用户端图谱浏览流畅度 > 98%
- 移动端适配良好，支持触摸操作
- 搜索响应时间 < 500ms

## 🔮 未来规划

### v1.1.0 计划
- **模板系统**: 提供常用知识图谱模板
- **批量导入**: 支持从Excel、JSON等格式批量导入
- **图谱复制**: 支持图谱克隆和复制功能

### v1.2.0 计划  
- **协作功能**: 支持多管理员协作编辑图谱
- **版本管理**: 支持图谱版本控制和历史回滚
- **数据统计**: 添加图谱查看统计和热点分析

### v2.0.0 计划
- **AI智能推荐**: 基于用户行为推荐相关知识点
- **学习路径**: 智能生成个性化学习路径
- **离线功能**: 支持用户端离线查看功能

---

*文档版本: v1.0.0*  
*创建时间: 2025-01-16*  
*最后更新: 2025-01-16*
