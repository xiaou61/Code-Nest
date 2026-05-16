# 知识图谱

知识图谱用于把技术知识组织成树状结构，帮助用户从“零散文章”进入“体系化学习”。当前实现更接近“知识树 + 外部文档链接”，节点可以标记普通、重点和难点，前端负责可视化展示。

## 功能入口

| 端 | 页面 | 说明 |
| --- | --- | --- |
| 用户端 | `/knowledge` | 已发布图谱列表 |
| 用户端 | `/knowledge/maps/:id` | 图谱查看器 |
| 管理端 | `/knowledge/maps` | 图谱列表和管理 |
| 管理端 | `/knowledge/maps/:id/edit` | 图谱编辑和节点维护 |

源码位置：

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
