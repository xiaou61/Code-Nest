# 文件存储

文件存储模块是全站“图片、附件、导出文件、头像”的底座。它屏蔽了本地磁盘、OSS、COS、Kodo、OBS 这些存储差异，让业务模块只关心“上传后拿到 URL”和“文件记录能不能查到”。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 用户端/业务端 | `/file/**` | 上传、下载、查 URL、删除、检查存在 |
| 管理端 | `/filestorage/storage-config` | 存储配置 |
| 管理端 | `/filestorage/file-management` | 文件管理和强制删除 |
| 管理端 | `/filestorage/migration` | 文件迁移任务 |
| 管理端 | `/filestorage/system-settings` | 文件类型、大小等系统设置 |
| 本地目录 | `uploads/` | 默认本地存储目录 |

## 读源码先看哪里

| 位置 | 作用 |
| --- | --- |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/controller/pub/FileController.java` | 业务侧文件接口 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/controller/admin/AdminFileController.java` | 后台文件管理 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/controller/admin/AdminStorageController.java` | 存储配置管理 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/controller/admin/AdminMigrationController.java` | 文件迁移任务 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/service/impl/FileStorageServiceImpl.java` | 上传、下载、URL、删除主逻辑 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/factory/StorageStrategyFactory.java` | 存储策略工厂 |
| `xiaou-filestorage/src/main/java/com/xiaou/filestorage/strategy/impl/LocalStorageStrategy.java` | 本地存储实现 |
| `xiaou-common/src/main/java/com/xiaou/common/config/LocalFileResourceConfig.java` | `/files/**` 静态资源映射 |

## 公开文件接口

| 接口 | 说明 |
| --- | --- |
| `POST /file/upload/single` | 单文件上传，参数 `file`、`moduleName`、`businessType` |
| `POST /file/upload/batch` | 批量上传 |
| `GET /file/download/{id}` | 下载文件流 |
| `GET /file/info/{id}` | 获取文件基础信息 |
| `GET /file/url/{id}` | 获取访问 URL，可传 `expireHours` |
| `POST /file/urls` | 批量获取 URL |
| `DELETE /file/{id}` | 按模块逻辑删除文件 |
| `GET /file/list` | 按模块和业务类型查询文件 |
| `POST /file/exists` | 批量检查文件是否存在 |

这些接口叫“公开接口”，不是说完全无权限。当前项目的 Sa-Token 拦截器主要保护 `/auth/**` 和 `/user/**`，所以业务模块调用 `/file/**` 时要特别注意：文件归属和模块边界主要靠 `moduleName`、业务侧鉴权和文件记录状态保证。新增敏感文件能力时，应把权限校验前移到业务模块或文件接口本身。

## 上传主流程

`FileStorageServiceImpl.uploadSingle` 的主线如下：

| 步骤 | 说明 |
| --- | --- |
| 1 | 校验文件不能为空，`moduleName` 不能为空 |
| 2 | 读取原始文件名、大小、MIME 类型 |
| 3 | 通过系统设置校验文件扩展名是否允许 |
| 4 | 通过系统设置校验文件大小是否超限 |
| 5 | 计算 MD5 |
| 6 | 如果相同 MD5 的正常文件已存在，直接返回已有文件 URL |
| 7 | 获取默认存储配置 |
| 8 | 默认存储可用时优先上传到默认存储 |
| 9 | 默认存储失败或不存在时降级到本地存储 |
| 10 | 写入 `file_info` |
| 11 | 发布上传事件 |
| 12 | 异步创建本地备份 |

这条链路里最重要的点是“主存储失败后降级本地”。它保证业务上传不容易被云存储配置问题完全打断，但也意味着运维要关注文件最终落到了哪里。

## 存储策略

| 类型 | 实现类 | 说明 |
| --- | --- | --- |
| `LOCAL` | `LocalStorageStrategy` | 本地磁盘，默认兜底 |
| `OSS` | `OssStorageStrategy` | 阿里云 OSS |
| `COS` | `CosStorageStrategy` | 腾讯云 COS |
| `KODO` | `KodoStorageStrategy` | 七牛云 |
| `OBS` | `ObsStorageStrategy` | 华为云 OBS |

`StorageStrategyFactory` 启动时会自动收集所有 `FileStorageStrategy` Bean，并按 `getStorageType()` 注册。后台新增存储类型时，要新增策略实现，并确保 `storage_config.storage_type` 与 `getStorageType()` 返回值一致。

## 本地存储规则

当前本地兜底配置在代码中生成：

| 参数 | 默认值 |
| --- | --- |
| `basePath` | `System.getProperty("user.dir") + "/uploads"` |
| `urlPrefix` | `http://localhost:8080/files` |

`LocalFileResourceConfig` 会把本地 `uploads` 映射成 `/files/**`。本地开发时如果上传成功但图片打不开，先检查三件事：

1. 后端实际工作目录是不是项目根目录。
2. `uploads/` 下文件是否真实存在。
3. 访问 URL 的端口是否和后端端口一致。

## 文件记录字段

| 字段 | 说明 |
| --- | --- |
| `original_name` | 用户上传时的原文件名 |
| `stored_name` | 存储策略生成的实际路径 |
| `file_size` | 字节数 |
| `content_type` | MIME 类型 |
| `md5_hash` | 去重用 MD5，表中有唯一索引 |
| `module_name` | 所属业务模块，如 `user`、`resume` |
| `business_type` | 业务类型，如 `avatar`、`export` |
| `access_url` | 可访问 URL |
| `status` | `0` 已删除、`1` 正常 |
| `is_public` | `0` 私有、`1` 公开 |

文件模块会用 `status = 1` 判断文件是否可用。逻辑删除只改状态，后台强制删除才会尝试删存储对象并删除数据库记录。

## 管理端能力

| 能力 | 接口 | 说明 |
| --- | --- | --- |
| 存储配置列表 | `GET /admin/storage/configs` | 支持按类型和启用状态筛选 |
| 创建配置 | `POST /admin/storage/config` | `config_params` 是 JSON |
| 测试配置 | `POST /admin/storage/config/{id}/test` | 校验连接和参数 |
| 启用/禁用配置 | `PUT /admin/storage/config/{id}/enable` | 控制是否参与上传 |
| 设置默认配置 | `PUT /admin/storage/config/{id}/default` | 默认上传目标 |
| 文件列表 | `GET /admin/file/list` | 后台查看全量文件 |
| 物理删除 | `DELETE /admin/file/{id}/force` | 删除存储对象和数据库记录 |
| 移动文件 | `PUT /admin/file/{id}/move` | 从默认存储移动到目标存储 |
| 文件统计 | `GET /admin/file/statistics` | 文件数量和类型统计 |
| 存储使用 | `GET /admin/file/storage-usage` | 存储占用情况 |

后台接口都应有 `@RequireAdmin`。物理删除、移动文件、设置默认配置属于高风险操作，生产环境建议配合操作日志和二次确认。

## 迁移任务

迁移任务表是 `file_migration`，状态如下：

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 已创建，未执行 |
| `RUNNING` | 执行中 |
| `COMPLETED` | 完成 |
| `FAILED` | 失败 |
| `STOPPED` | 已停止 |

迁移接口：

| 接口 | 说明 |
| --- | --- |
| `POST /admin/file/migrate` | 创建任务 |
| `POST /admin/file/migration/{id}/execute` | 执行任务 |
| `PUT /admin/file/migration/{id}/stop` | 停止任务 |
| `GET /admin/file/migration/{id}/progress` | 查看进度 |
| `DELETE /admin/file/migration/{id}` | 删除非运行中任务 |

当前 `FileMigrationServiceImpl` 里有一部分迁移逻辑仍是示例实现，例如异步使用 `new Thread`，完成后写入模拟成功/失败数量。后续要做生产级迁移，应改为线程池或任务队列，并把每个文件的迁移结果落库。

## 业务模块如何接入

最常见接入方式是：

```text
FileUploadResult result = fileStorageService.uploadSingle(file, moduleName, businessType)
```

推荐的 `moduleName` 和 `businessType` 约定：

| 场景 | moduleName | businessType |
| --- | --- | --- |
| 用户头像 | `user` | `avatar` |
| 简历导出 | `resume` | `export` |
| 聊天图片 | `chat` | `image` |
| 文章封面 | `blog` | `cover` |
| 学习资产附件 | `learning` | `attachment` |

命名要稳定，因为删除接口会用 `moduleName` 判断是否允许删除。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 上传返回成功但数据库没有记录 | 存储上传成功后插入 `file_info` 失败 | 查日志里“建议手动清理文件” |
| 文件重复上传没有新记录 | MD5 命中已有正常文件 | 这是设计行为 |
| 云存储失败后文件到了本地 | 默认存储上传失败，自动降级 | 查 `storage_config.test_status` 和应用日志 |
| 下载失败但 URL 存在 | 默认存储不可读，本地兜底也没有文件 | 检查备份和存储配置 |
| 迁移任务进度不可信 | 当前迁移实现仍有示例逻辑 | 生产化前要补真实迁移明细 |
| 私有文件被直接访问 | `/files/**` 静态映射绕过业务校验 | 私密文件不要直接暴露静态 URL |
