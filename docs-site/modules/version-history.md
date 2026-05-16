# 版本历史

版本历史用于展示产品发布记录、里程碑和更新说明。

## 功能入口

| 端 | 页面 |
| --- | --- |
| 用户端 | `/version-history` |
| 管理端 | `/system/version` |
| 后端模块 | `xiaou-version` |

## 用户侧接口域

| 接口 | 能力 |
| --- | --- |
| `/version/timeline` | 时间线 |
| `/version/{id}` | 版本详情 |
| `/version/view` | 浏览记录 |
| `/version/search` | 搜索 |
| `/version/latest` | 最新版本 |

## 管理侧接口域

| 接口 | 能力 |
| --- | --- |
| `/admin/version/list` | 版本列表 |
| `/admin/version/{id}` | 版本详情 |
| `/admin/version/create` | 创建 |
| `/admin/version/update` | 更新 |
| `/admin/version/delete` | 删除 |
| `/admin/version/publish` | 发布 |
| `/admin/version/hide` | 隐藏 |
| `/admin/version/unpublish` | 取消发布 |
| `/admin/version/batch/*` | 批量发布、隐藏、删除 |
| `/admin/version/check-version/{versionNumber}` | 版本号检查 |

## 写作建议

每个版本建议包含：

- 版本号。
- 发布时间。
- 版本主题。
- 新增功能。
- 安全和稳定性修复。
- 升级注意事项。
- 数据库脚本。
- 前后端部署影响。

