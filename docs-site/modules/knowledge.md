# 知识图谱

知识图谱用于把技术知识组织成树状结构，帮助用户从“零散文章”进入“体系化学习”。当前实现更接近“知识树 + 外部文档链接”，节点可以标记普通、重点和难点，前端负责可视化展示。

## 推荐学习顺序

知识图谱适合按“图谱主表 -> 节点树 -> 发布状态 -> 学习资产联动”来读：

1. 先看 `knowledge_map`，理解一个图谱的标题、状态、节点数和浏览数。
2. 再看 `knowledge_node`，理解 `parent_id = 0` 的根节点和子节点深度。
3. 接着看管理端节点新增、排序、删除，理解为什么有子节点时不能直接删除。
4. 然后看用户端树渲染，理解后端返回的是树，不是通用图数据库关系。
5. 最后看学习资产联动，理解内容候选如何审核后沉淀为知识节点。

## 功能入口

| 端 | 页面 | 说明 |
| --- | --- | --- |
| 用户端 | `/knowledge` | 已发布图谱列表 |
| 用户端 | `/knowledge/maps/:id` | 图谱查看器 |
| 管理端 | `/knowledge/maps` | 图谱列表和管理 |
| 管理端 | `/knowledge/maps/:id/edit` | 图谱编辑和节点维护 |

## 源码地图

| 层级 | 文件或目录 |
| --- | --- |
| 用户前端 | `vue3-user-front/src/views/knowledge/` |
| 用户 API | `vue3-user-front/src/api/knowledge.js` |
| 管理前端 | `vue3-admin-front/src/views/knowledge/` |
| 管理 API | `vue3-admin-front/src/api/knowledge.js` |
| 公开 Controller | `xiaou-knowledge/src/main/java/com/xiaou/knowledge/controller/pub/PubKnowledgeMapController.java` |
| 管理 Controller | `xiaou-knowledge/src/main/java/com/xiaou/knowledge/controller/admin/` |
| Service | `xiaou-knowledge/src/main/java/com/xiaou/knowledge/service/impl/` |
| 实体 | `xiaou-knowledge/src/main/java/com/xiaou/knowledge/domain/` |

## 数据模型

| 表 | 说明 |
| --- | --- |
| `knowledge_map` | 图谱主表 |
| `knowledge_node` | 图谱节点表 |

`knowledge_map` 关键字段：

| 字段 | 说明 |
| --- | --- |
| `title` | 图谱标题 |
| `description` | 图谱描述 |
| `cover_image` | 封面图 |
| `user_id` | 创建管理员 ID |
| `node_count` | 节点总数 |
| `view_count` | 查看次数 |
| `status` | 状态 |
| `sort_order` | 排序权重 |

图谱状态：

| 值 | 含义 |
| --- | --- |
| `0` | 草稿 |
| `1` | 已发布 |
| `2` | 已隐藏 |

`knowledge_node` 关键字段：

| 字段 | 说明 |
| --- | --- |
| `map_id` | 所属图谱 |
| `parent_id` | 父节点，`0` 表示根节点 |
| `title` | 节点标题 |
| `url` | 飞书云文档或外部资料链接 |
| `node_type` | 节点类型 |
| `sort_order` | 同级排序 |
| `level_depth` | 节点深度 |
| `is_expanded` | 是否默认展开 |
| `view_count` | 查看次数 |
| `last_view_time` | 最后查看时间 |

节点类型：

| 值 | 含义 |
| --- | --- |
| `1` | 普通 |
| `2` | 重点 |
| `3` | 难点 |

## 用户侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/pub/knowledge/maps/list` | `POST` | 分页查询已发布图谱 |
| `/pub/knowledge/maps/{id}` | `GET` | 图谱详情 |
| `/pub/knowledge/maps/{mapId}/nodes` | `GET` | 图谱节点树 |
| `/pub/knowledge/maps/{mapId}/nodes/search` | `GET` | 节点搜索 |
| `/pub/knowledge/maps/nodes/{nodeId}/view` | `POST` | 记录节点浏览 |

用户端只读取已发布图谱。草稿和隐藏图谱只能在管理端维护。

## 管理侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/knowledge/maps/list` | `POST` | 图谱列表 |
| `/admin/knowledge/maps/{id}` | `GET` | 图谱详情 |
| `/admin/knowledge/maps` | `POST` | 创建图谱 |
| `/admin/knowledge/maps/{id}` | `PUT` | 更新图谱 |
| `/admin/knowledge/maps/{id}/publish` | `POST` | 发布 |
| `/admin/knowledge/maps/{id}/hide` | `POST` | 隐藏 |
| `/admin/knowledge/maps/{id}` | `DELETE` | 删除 |
| `/admin/knowledge/maps/batch` | `DELETE` | 批量删除 |
| `/admin/knowledge/maps/{mapId}/nodes` | `GET` | 节点列表 |
| `/admin/knowledge/maps/{mapId}/nodes` | `POST` | 新增节点 |
| `/admin/knowledge/nodes/{id}` | `GET` | 节点详情 |
| `/admin/knowledge/nodes/{id}` | `PUT` | 更新节点 |
| `/admin/knowledge/nodes/{id}` | `DELETE` | 删除节点 |
| `/admin/knowledge/maps/{mapId}/nodes/sort` | `PUT` | 节点排序 |
| `/admin/knowledge/maps/{mapId}/nodes/search` | `GET` | 节点搜索 |

## 图谱维护流程

1. 管理员创建图谱，默认状态为草稿，节点数和浏览数为 0。
2. 管理员新增根节点，`parent_id = 0`，`level_depth = 1`。
3. 新增子节点时，服务会校验父节点存在，并且父节点属于当前图谱。
4. 子节点深度等于父节点深度加 1。
5. 如果 `sort_order = 0`，服务会取同级最大排序值再加 1。
6. 新增或删除节点后，更新图谱节点总数。
7. 图谱发布后，用户端列表可见。
8. 用户查看图谱或节点时，浏览计数增加。

## 节点树构建

`KnowledgeNodeServiceImpl#buildTree` 的逻辑很清楚：

1. 查询某个图谱下所有节点。
2. 按 `parent_id` 分组。
3. 取 `parent_id = 0` 的节点作为根节点。
4. 递归把子节点挂到父节点的 `children` 里。

这意味着数据正确性非常依赖 `parent_id`：

| 情况 | 结果 |
| --- | --- |
| 根节点 `parent_id = 0` | 能出现在树第一层 |
| 父节点不存在 | 新增时会被拒绝 |
| 父节点属于别的图谱 | 新增时会被拒绝 |
| 节点有子节点 | 删除时会被拒绝 |

删除节点前必须先删除子节点，这可以避免误删整棵知识树。

## 和学习资产的关系

学习资产模块可以把外部内容转化为知识节点候选。一个完整闭环可以这样理解：

1. 用户或系统沉淀学习资产。
2. 学习资产生成知识节点候选。
3. 管理端审核候选内容。
4. 审核通过后写入某个 `knowledge_map` 下的 `knowledge_node`。
5. 用户端在知识图谱中看到体系化节点。

当前知识图谱服务只负责图谱和节点本身，候选生成、审核状态和发布动作在学习资产模块中维护。

## 前端渲染要点

用户端 `MapViewer.vue` 会读取图谱详情和节点树，再交给可视化组件展示。后端返回的是树结构，不是任意图数据库关系。

前端需要关注：

| 数据 | 用途 |
| --- | --- |
| `id` | 节点唯一标识 |
| `parentId` | 构建树关系 |
| `title` | 节点标题 |
| `url` | 点击后打开外部文档 |
| `nodeType` | 控制普通、重点、难点样式 |
| `isExpanded` | 初始展开状态 |
| `children` | 子节点列表 |

## 验证清单

- 草稿图谱不会出现在用户端 `/knowledge`。
- 发布图谱后，用户端列表可见。
- 新增根节点时 `parent_id = 0`。
- 新增子节点时，`level_depth` 会自动比父节点大 1。
- 删除有子节点的节点会失败。
- 节点排序接口只允许排序当前图谱内的节点。
- 用户搜索节点时返回树结构，前端能定位节点。
- 用户浏览节点后，节点浏览数和最后浏览时间更新。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 用户端看不到图谱 | 图谱未发布或已隐藏 | 检查 `knowledge_map.status` |
| 节点不显示在树里 | `parent_id` 不正确 | 根节点必须是 0，子节点必须指向同图谱父节点 |
| 删除节点失败 | 节点还有子节点 | 先删除或迁移子节点 |
| 节点顺序混乱 | 同级 `sort_order` 重复或未维护 | 使用排序接口批量更新 |
| 外部文档打不开 | `url` 权限不足或格式不对 | 发布前用普通用户身份验证 |

---

## 深度拆解

### 一、图谱创建与状态管理深度分析

`KnowledgeMapServiceImpl.createMap`：

```text
1. userId null → 抛"用户ID不能为空"
2. 构建 KnowledgeMap:
   ├─ nodeCount=0, viewCount=0
   ├─ status = request.status ?? DRAFT(0)
   ├─ sortOrder = request.sortOrder ?? 0
3. insert → result<=0 抛"创建失败"
4. return id
```

**发布/隐藏**：`publishMap` → `updateStatus(id, PUBLISHED(1))`，`hideMap` → `updateStatus(id, HIDDEN(2))`。没有校验当前状态——可以从 HIDDEN 直接 PUBLISHED，也可以从 DRAFT 直接 HIDDEN。**没有不允许从 DRAFT 跳到 HIDDEN 的限制**。

**删除图谱级联**：`deleteMap` 先 `knowledgeNodeMapper.deleteByMapId(id)` 删除所有节点，再删图谱。**物理删除**——不走软删除。这意味着删除后节点和图谱数据不可恢复。

**批量删除**：`deleteBatch` 先 `knowledgeNodeMapper.deleteByMapIds(ids)` 批量删节点，再 `deleteBatchIds(ids)` 批量删图谱。注释说"避免N+1问题"——确实一次性删除而非逐个。

### 二、节点创建与树构建深度分析

`KnowledgeNodeServiceImpl.createNode`：

```text
1. 校验图谱存在 → knowledgeMapMapper.selectById(mapId)
2. parentId != 0:
   ├─ selectById(parentId) → 不存在抛"父节点不存在"
   ├─ parentNode.mapId != mapId → 抛"父节点不属于当前图谱"
   └─ levelDepth = parentNode.levelDepth + 1
3. parentId == 0 → levelDepth = 1
4. sortOrder == 0 → selectMaxSortOrder(mapId, parentId) + 1
5. 构建 KnowledgeNode (viewCount=0)
6. insert → result<=0 抛"创建失败"
7. updateMapNodeCount → countByMapId 重算
```

**深度无上限**：`levelDepth = parent.levelDepth + 1` 没有最大深度限制。理论上可以创建无限深度的节点，但前端渲染深度过大的树会性能恶化。

**排序自增逻辑**：`sortOrder == 0` 时取同级最大排序值 +1。但如果多个节点并发创建且都 sortOrder=0，`selectMaxSortOrder` 可能返回相同值，导致排序冲突。

**树构建 `buildTree`**：

```text
1. 按 parentId 分组 → groupedNodes
2. 取 parentId=0 的根节点
3. 递归 buildChildren: groupedNodes.get(parentNode.id) → setChildren
```

**无环检测**：如果数据中存在 parentId 循环引用（A.parentId=B, B.parentId=A），buildChildren 会无限递归导致 StackOverflow。但新增节点时校验了父节点存在且属于同图谱，降低了环出现的概率——**但编辑节点 parentId 时没有同样的校验**。

### 三、节点删除与保护深度分析

`deleteNode`：

```text
1. selectById(id) → 校验存在
2. selectByParentId(id) → 有子节点 → 抛"存在子节点，无法删除"
3. deleteById(id) → 物理删除
4. updateMapNodeCount → 重算图谱节点数
```

**物理删除 + 子节点保护**：不允许有子节点时删除，但没有提供"级联删除子节点"选项。管理端需要先手动逐个删除叶子节点，再删父节点——**对于深度较大的树操作繁琐**。

**删除不清理浏览记录**：节点被物理删除后，如果有外部系统引用该节点 URL，会产生 404。

### 四、节点排序深度分析

`sortNodes`：

```text
1. 校验 mapId 非空
2. 逐个校验 nodeId 存在且属于当前图谱 → getById + mapId 匹配
3. batchUpdateOrder → 批量更新 sortOrder
```

**N+1 校验**：每个 nodeOrder 都调用 `getById(nodeOrder.getNodeId())`，等于逐个 SELECT。对于排序 50 个节点，会执行 50 次查询。

**没有校验 parentId 一致**：排序请求中只传 nodeId 和 sortOrder，不传 parentId。如果排序不同父节点下的子节点混在一起，会导致排序混乱。前端应确保只排序同级节点。

### 五、搜索与浏览深度分析

**搜索 `searchNodes`**：`knowledgeNodeMapper.searchNodes(mapId, keyword)` → 返回匹配节点 → `buildTree`。搜索结果也是树结构，而非扁平列表——这意味着只显示匹配节点的祖先路径，不显示匹配节点的未匹配兄弟。

**浏览计数 `incrementViewCount`**：图谱浏览 → `knowledgeMapMapper.incrementViewCount(mapId)`，节点浏览 → `knowledgeNodeMapper.incrementViewCount(id)` + `knowledgeNodeMapper.updateLastViewTime(id, now())`。都是 `SET view_count = view_count + 1`，并发安全。

### 六、深度发现与坑点

#### 6.1 确认 BUG

| 编号 | BUG | 位置 | 说明 |
| --- | --- | --- | --- |
| BUG-1 | 编辑节点 parentId 无校验 | `KnowledgeNodeServiceImpl.updateNode` | 可以将节点移到别的图谱下，或设置循环引用 |
| BUG-2 | 排序 N+1 校验 | `KnowledgeNodeServiceImpl.sortNodes` | 逐个 getById 查询，排序50节点=50次SELECT |
| BUG-3 | 状态无约束 | `publishMap / hideMap` | 可以从 HIDDEN 直接 PUBLISHED，也可以 DRAFT→HIDDEN |
| BUG-4 | 物理删除不可恢复 | `deleteMap / deleteNode` | 不走软删除，删除后数据永久丢失 |
| BUG-5 | 深度无上限 | `createNode` | levelDepth 无最大值限制，前端可能渲染失败 |

#### 6.2 设计风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 并发 sortOrder 冲突 | 多节点同时 sortOrder=0，selectMaxSortOrder 返回相同值 |
| RISK-2 | buildTree 无环检测 | 循环引用导致 StackOverflow（概率低但需防御） |
| RISK-3 | 删除子节点操作繁琐 | 不提供级联删除，管理端需逐个删叶子 |

#### 6.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | nodeCount COUNT 重算 | 每次 create/delete 后 countByMapId 重算，避免增量漂移 |
| H-2 | parentId 归属校验 | createNode 校验父节点属于同图谱，防止跨图谱引用 |
| H-3 | 子节点删除保护 | 有子节点不允许删除，防止误删整棵子树 |
| H-4 | 搜索返回树结构 | 保持可视化上下文，而非扁平列表 |
| H-5 | 批量删除避免 N+1 | deleteByMapIds 一次性删除所有节点 |
| H-6 | sortOrder 自增 | sortOrder=0 自动取同级最大值+1，减少手动维护 |

#### 6.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 图谱创建 | `KnowledgeMapServiceImpl.java` — createMap + userId + status/sortOrder 默认值 |
| 图谱发布/隐藏 | `KnowledgeMapServiceImpl.java` — publishMap + hideMap + updateStatus |
| 图谱删除 | `KnowledgeMapServiceImpl.java` — deleteMap + deleteByMapId 级联 |
| 批量删除 | `KnowledgeMapServiceImpl.java` — deleteBatch + deleteByMapIds |
| 节点创建 | `KnowledgeNodeServiceImpl.java` — createNode + levelDepth + sortOrder 自增 |
| 节点删除 | `KnowledgeNodeServiceImpl.java` — deleteNode + 子节点保护 |
| 树构建 | `KnowledgeNodeServiceImpl.java` — buildTree + buildChildren 递归 |
| 节点排序 | `KnowledgeNodeServiceImpl.java` — sortNodes + N+1 校验 |
| 搜索 | `KnowledgeNodeServiceImpl.java` — searchNodes + buildTree |
| 浏览计数 | `KnowledgeNodeServiceImpl.java` — incrementViewCount |
| 图谱域 | `KnowledgeMap.java` — Status(DRAFT/PUBLISHED/HIDDEN) + nodeCount + viewCount |
| 节点域 | `KnowledgeNode.java` — parentId + levelDepth + sortOrder + nodeType |


## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 知识图谱模块依赖公共底座的统一响应、分页和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 图谱创建和管理需要用户登录态 |
| [用户账户与个人中心](/modules/user-account) | 强依赖 | 用户图谱数据依赖用户信息 |
| [题库与成长闭环](/modules/interview-and-growth) | 强依赖 | 知识图谱与学习成长紧密关联 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 图谱管理界面在管理端 |
