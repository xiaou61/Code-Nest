# 版本历史

版本历史用于展示产品发布记录、里程碑和更新说明。它连接两类读者：用户端读者需要知道“这个版本更新了什么”，管理端维护者需要知道“版本记录如何创建、发布、隐藏和回滚到草稿”。

## 推荐学习顺序

版本历史模块不复杂，但很适合学习“后台运营内容如何发布到用户端”：

1. 先看数据模型，理解版本号、更新类型、状态、发布时间和逻辑删除。
2. 再看管理端创建和发布流程，理解草稿、发布、隐藏、取消发布的状态变化。
3. 接着看用户端接口，确认前台只读取已发布版本。
4. 最后看文案规范，理解产品内版本历史和 VitePress 文档站的分工。

## 功能入口

| 端 | 页面 | 说明 |
| --- | --- | --- |
| 用户端 | `/version-history` | 查看时间线、搜索版本、打开版本详情 |
| 管理端 | `/system/version` | 创建、编辑、发布、隐藏、取消发布、批量操作 |
| 后端用户接口 | `/version` | 公开版本历史 |
| 后端管理接口 | `/admin/version` | 后台版本管理 |

## 源码地图

| 层级 | 文件 |
| --- | --- |
| 用户 API | `vue3-user-front/src/api/version.js` |
| 管理页面 | `vue3-admin-front/src/views/system/version/index.vue` |
| 管理 API | `vue3-admin-front/src/api/version.js` |
| 公开 Controller | `xiaou-version/src/main/java/com/xiaou/version/controller/pub/VersionHistoryController.java` |
| 管理 Controller | `xiaou-version/src/main/java/com/xiaou/version/controller/admin/VersionHistoryAdminController.java` |
| Service | `xiaou-version/src/main/java/com/xiaou/version/service/impl/VersionHistoryServiceImpl.java` |
| 实体 | `xiaou-version/src/main/java/com/xiaou/version/domain/VersionHistory.java` |

## 数据模型

核心表是 `version_history`：

| 字段 | 说明 |
| --- | --- |
| `version_number` | 版本号，例如 `v2.2.0` |
| `title` | 更新标题 |
| `update_type` | 更新类型 |
| `description` | 版本简要描述 |
| `prd_url` | PRD 或详细文档链接 |
| `release_time` | 发布时间 |
| `status` | 发布状态 |
| `sort_order` | 排序权重，数字越大越靠前 |
| `view_count` | 浏览次数 |
| `is_featured` | 是否重点推荐 |
| `deleted` | 逻辑删除标记 |

更新类型：

| 值 | 含义 |
| --- | --- |
| `1` | 重大更新 |
| `2` | 功能更新 |
| `3` | 修复更新 |
| `4` | 其他 |

发布状态：

| 值 | 含义 | 用户端是否可见 |
| --- | --- | --- |
| `0` | 草稿 | 否 |
| `1` | 已发布 | 是 |
| `2` | 已隐藏 | 否 |

## 用户侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/version/timeline` | `POST` | 分页查看已发布版本 |
| `/version/{id}` | `GET` | 查看版本详情 |
| `/version/view` | `POST` | 增加浏览次数 |
| `/version/search` | `POST` | 搜索已发布版本 |
| `/version/latest` | `GET` | 获取最新若干版本，默认 5 条 |

用户侧只读已发布版本。这里不要把草稿和隐藏版本泄露到前台。

## 管理侧接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/admin/version/list` | `POST` | 管理端分页查询，支持关键字、类型、状态、推荐和发布时间范围 |
| `/admin/version/{id}` | `GET` | 版本详情 |
| `/admin/version/create` | `POST` | 创建版本 |
| `/admin/version/update` | `PUT` | 更新版本 |
| `/admin/version/delete` | `DELETE` | 逻辑删除 |
| `/admin/version/publish` | `POST` | 发布 |
| `/admin/version/hide` | `POST` | 隐藏 |
| `/admin/version/unpublish` | `POST` | 取消发布，回到草稿 |
| `/admin/version/batch/publish` | `POST` | 批量发布 |
| `/admin/version/batch/hide` | `POST` | 批量隐藏 |
| `/admin/version/batch/delete` | `POST` | 批量逻辑删除 |
| `/admin/version/check-version/{versionNumber}` | `GET` | 检查版本号是否重复 |

## 创建和发布流程

1. 管理员在 `/system/version` 填写版本号、标题、类型、说明、PRD 链接和发布时间。
2. 后端检查版本号是否已存在。唯一约束是 `version_number + deleted`。
3. 发布时间必须能按 `yyyy-MM-dd HH:mm:ss` 解析。
4. 创建记录时写入创建人、初始浏览次数 0。
5. 草稿状态下用户端不可见。
6. 点击发布后，状态改为 `1`。
7. 用户端时间线和最新版本接口开始返回该记录。
8. 用户查看详情后，可调用 `/version/view` 增加浏览次数。

## 状态流转

```text
草稿(0) -> 已发布(1)
已发布(1) -> 已隐藏(2)
已隐藏(2) -> 已发布(1)
已发布(1) -> 草稿(0)
任意状态 -> 逻辑删除(deleted = 1)
```

文档写作建议：

| 场景 | 推荐状态 |
| --- | --- |
| 版本还没上线 | 草稿 |
| 上线并希望用户看到 | 已发布 |
| 发现描述有误但不想删除 | 已隐藏 |
| 发布后需要撤回重新整理 | 取消发布回草稿 |
| 确认不再需要 | 逻辑删除 |

## 一条好版本记录应该包含什么

建议每个版本记录至少包含：

| 内容 | 说明 |
| --- | --- |
| 版本号 | 和 Git tag、部署包、数据库脚本保持一致 |
| 发布时间 | 明确到秒，方便排查线上问题 |
| 更新类型 | 重大、功能、修复或其他 |
| 一句话标题 | 让用户马上知道主题 |
| 简要描述 | 不要只写“优化若干问题” |
| 重点功能 | 用户能感知的变化 |
| 修复项 | 影响稳定性、兼容性和数据的修复 |
| 升级注意事项 | 是否需要执行 SQL、重启服务、清缓存 |
| PRD 或文档链接 | 方便维护者追溯 |

## 和文档站的关系

版本历史是产品内展示，VitePress 文档站是长期学习资料。两者建议这样配合：

| 产品版本历史 | VitePress |
| --- | --- |
| 面向最终用户和管理员 | 面向开发者、学习者和维护者 |
| 讲“这版更新了什么” | 讲“这个功能怎么实现、怎么维护” |
| 简洁、可快速浏览 | 详细、可复现、可验证 |
| 可以链接到 PRD 或文档 | 可以反向引用版本号和变更背景 |

## 验证清单

- 创建版本时，重复版本号会被拒绝。
- 发布时间格式不对时，会返回明确错误。
- 草稿和隐藏版本不会出现在用户端时间线。
- 发布后能在 `/version/timeline` 查到。
- 打开详情并调用 `/version/view` 后，`view_count` 会增加。
- 批量发布、隐藏、删除只影响选中的 ID。
- 逻辑删除后，同版本号可重新创建，但要确认这是否符合运营预期。

## 常见坑

| 问题 | 原因 | 处理 |
| --- | --- | --- |
| 用户端看不到版本 | 状态不是已发布或已逻辑删除 | 检查 `status` 和 `deleted` |
| 创建失败提示时间格式错误 | 发布时间不是 `yyyy-MM-dd HH:mm:ss` | 管理端提交前格式化 |
| 版本号重复 | `version_number + deleted` 已存在未删除记录 | 编辑原记录或改版本号 |
| 最新版本排序不对 | 发布时间和排序权重混用 | 明确列表排序规则，优先按发布时间或权重 |
| PRD 链接打不开 | 链接只在内网或权限不足 | 发布前用普通用户权限验证 |
