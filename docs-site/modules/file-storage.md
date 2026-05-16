# 文件存储

文件存储模块负责上传、删除、列表、存在性检查、公开/私有访问、存储配置和迁移能力。

## 主要入口

| 端 | 页面 |
| --- | --- |
| 管理端 | `/filestorage/storage-config`、`/filestorage/file-management`、`/filestorage/migration`、`/filestorage/system-settings` |
| 后端 | `xiaou-filestorage`、`xiaou-common` |
| 本地目录 | `uploads/` |

## 本地资源映射

项目保留了本地文件资源映射配置：

```text
xiaou-common/src/main/java/com/xiaou/common/config/LocalFileResourceConfig.java
```

该配置将本地 `uploads` 目录映射为 `/files/**`。`uploads/` 属于运行期文件，已经在 `.gitignore` 中忽略。

## 访问控制

v2.1.1 之后，文件接口权限做了收口：

- 上传、删除、列表、存在性检查需要登录。
- 文件信息、下载与 URL 获取需要按公开/私有状态校验。
- 不建议通过静态路径绕过业务权限。

## 后续补齐

- 本地存储和云存储适配。
- 文件可见性模型。
- 分片上传和大文件策略。
- 迁移任务状态。
- 图片、附件、导出文件的生命周期。

