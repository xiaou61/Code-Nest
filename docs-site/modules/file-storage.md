# 文件存储

文件存储模块是全站“图片、附件、导出文件、头像”的底座。它屏蔽了本地磁盘、OSS、COS、Kodo、OBS 这些存储差异，让业务模块只关心“上传后拿到 URL”和“文件记录能不能查到”。

这一页要重点区分三件事：文件怎么存、文件怎么读、谁能读。当前实现已经在 `/file/**` 层补了基础登录和公开文件判断，但它不是完整的业务归属系统。也就是说，文件服务负责存储和基础访问门槛，头像、聊天图片、简历导出这些业务仍然要自己决定“这个文件是不是当前用户有权使用”。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 用户端/业务端 | `/file/**` | 上传、下载、查 URL、删除、检查存在；上传和变更类接口要求用户端或管理端已登录 |
| 管理端 | `/filestorage/storage-config` | 存储配置 |
| 管理端 | `/filestorage/file-management` | 文件管理和强制删除 |
| 管理端 | `/filestorage/migration` | 文件迁移任务 |
| 管理端 | `/filestorage/system-settings` | 文件类型、大小等系统设置 |
| 本地目录 | `uploads/` | 默认本地存储目录 |

## 推荐学习顺序

如果你第一次看文件存储，不建议先从云存储 SDK 开始。先用本地上传跑通主链路，再理解云存储只是策略实现。

1. 先看 `/file/upload/single`，理解业务模块怎样把文件交给文件服务。
2. 再看 `FileStorageServiceImpl.uploadSingle`，把校验、MD5 去重、默认存储、本地兜底和落库串起来。
3. 接着看 `StorageStrategyFactory` 和几个 `FileStorageStrategy` 实现，理解“同一个上传流程，换不同存储后端”。
4. 然后看 `LocalFileResourceConfig`，确认后端 `/files/**` 静态映射和外部 `/api/files/**` URL 的关系。
5. 最后看后台配置、文件管理和迁移任务，这部分属于运维和生产治理。

学习时可以用“头像上传”做最小案例，因为它只需要一个图片文件、一个用户资料字段和一条 `file_info` 记录，就能把文件模块的主要能力串起来。

## 源码地图

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

## 业务侧文件接口和访问边界

| 接口 | 登录和读取规则 | 说明 |
| --- | --- | --- |
| `POST /file/upload/single` | 必须登录用户端或管理端 | 单文件上传，参数 `file`、`moduleName`、`businessType` |
| `POST /file/upload/batch` | 必须登录用户端或管理端 | 批量上传 |
| `GET /file/download/{id}` | 公开文件可匿名读；私有文件必须登录 | 下载文件流 |
| `GET /file/info/{id}` | 公开文件可匿名读；私有文件必须登录 | 获取文件基础信息 |
| `GET /file/url/{id}` | 公开文件可匿名读；私有文件必须登录 | 获取访问 URL，可传 `expireHours` |
| `POST /file/urls` | 如果请求里包含私有文件，则必须登录 | 批量获取 URL |
| `DELETE /file/{id}` | 必须登录用户端或管理端 | 按 `moduleName` 逻辑删除文件 |
| `GET /file/list` | 必须登录用户端或管理端 | 按模块和业务类型查询文件 |
| `POST /file/exists` | 必须登录用户端或管理端 | 批量检查文件是否存在 |

这里的“公开文件”由 `file_info.is_public = 1` 决定。`FileController.canReadFile` 的判断很直接：公开文件允许读，私有文件要求当前请求存在用户端或管理端登录态。

这个判断只解决“是否登录”和“是否公开”的问题，不解决“是不是文件所有者”的问题。比如某个私有简历附件只要拿到文件 ID，任意已登录账号理论上都可能通过 `/file/url/{id}` 读取，所以敏感业务不要只依赖通用文件接口。更稳的做法是业务模块先做归属校验，再由业务接口返回文件 URL，或者在文件接口里补充更细的 owner/module 权限模型。

## 上传主流程

`FileStorageServiceImpl.uploadSingle` 的主线如下：

| 步骤 | 说明 |
| --- | --- |
| 1 | 校验文件不能为空，`moduleName` 不能为空 |
| 2 | 读取原始文件名、大小、MIME 类型；MIME 优先取请求头，缺失时用 Apache Tika 探测 |
| 3 | 通过系统设置校验文件扩展名是否允许 |
| 4 | 通过系统设置校验文件大小是否超限 |
| 5 | 计算 MD5 |
| 6 | 如果相同 MD5 的正常文件已存在，直接返回已有文件 URL |
| 7 | 获取默认存储配置 |
| 8 | 默认存储可用时优先上传到默认存储 |
| 9 | 默认存储失败或不存在时降级到本地存储 |
| 10 | 写入 `file_info`，默认 `is_public = 0` |
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
| `urlPrefix` | `http://localhost:9999/api/files` |

`LocalFileResourceConfig` 会把本地 `uploads` 映射成后端资源路径 `/files/**`。因为主应用配置了 `/api` 上下文，本地策略返回给前端的外部 URL 通常是 `http://localhost:9999/api/files/...`。生产 Nginx 也可以像 `deploy/nginx/code-nest-113.44.190.45.conf` 那样额外把 `/files/` 直接 alias 到上传目录。

本地开发时如果上传成功但图片打不开，先检查三件事：

1. 后端实际工作目录是不是项目根目录。
2. `uploads/` 下文件是否真实存在。
3. 访问 URL 是走 `http://localhost:9999/api/files/...`，还是被 Nginx 或前端代理改成了 `/files/...`。

## 系统设置和默认值

文件类型、大小和临时链接等配置来自 `file_system_setting`。`FileSystemSettingServiceImpl` 会做 5 分钟内存缓存，更新设置后会清缓存。

| setting_key | 默认值 | 说明 |
| --- | --- | --- |
| `MAX_FILE_SIZE` | `104857600` | 单文件最大 100MB |
| `ALLOWED_FILE_TYPES` | `jpg`、`jpeg`、`png`、`gif`、`bmp`、`webp`、`pdf`、`doc`、`docx`、`xls`、`xlsx`、`ppt`、`pptx`、`txt`、`zip`、`rar`、`7z`、`mp4`、`avi`、`mov`、`mp3`、`wav` | 文件扩展名白名单 |
| `AUTO_BACKUP_ENABLED` | `true` | 是否启用本地备份配置项；当前上传主链路会调用异步备份 |
| `TEMP_LINK_EXPIRE_HOURS` | `24` | 临时链接默认有效期 |
| `STORAGE_QUOTA_PER_MODULE` | `10737418240` | 每个模块默认 10GB 配额 |

上传前的类型检查看的是扩展名白名单，不是只看 MIME。头像等业务模块还会在自己的 Controller 里做更窄的二次校验，例如只允许 `jpg`、`jpeg`、`png`、`gif` 且不超过 5MB。

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
| `is_public` | `0` 私有、`1` 公开；新上传默认私有 |

文件模块会用 `status = 1` 判断文件是否可用。逻辑删除只改状态，后台强制删除才会尝试删存储对象并删除数据库记录。

读取接口会用 `is_public` 判断匿名用户能不能访问；静态资源映射本身不会理解业务权限，所以私密文件不要把最终静态 URL 当作长期公开地址到处传播。

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
| 系统设置 | `GET/PUT /admin/system/settings` | 查看和更新文件系统设置 |
| 文件类型白名单 | `GET/PUT /admin/system/file-types` | 查看和更新允许上传的扩展名 |
| 配置摘要 | `GET /admin/system/summary` | 查看最大文件、配额、临时链接和备份开关 |

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

业务接入时建议按这四步走：

1. 业务接口先做登录态和归属校验，不要把敏感场景完全交给 `/file/**`。
2. 调用文件服务上传，保存返回的 `accessUrl` 或 `storagePath` 到业务表。
3. 如果页面需要重新读取文件，优先由业务接口返回 URL，而不是让前端随意枚举文件 ID。
4. 删除业务对象时同步调用文件逻辑删除，后台强制删除前确认没有业务表仍在引用。

## 验证清单

| 验证点 | 怎么验证 | 预期结果 |
| --- | --- | --- |
| 未登录上传 | 不带用户端和管理端 Token 调用 `POST /file/upload/single` | 返回 `401`，不会写入文件记录 |
| 本地单文件上传 | 调用 `POST /file/upload/single`，传 `moduleName=user`、`businessType=avatar` | 返回 `storagePath`、`accessUrl`、`fileSize`，并在 `file_info` 写入正常记录 |
| MD5 去重 | 重复上传同一个文件 | 不新增重复正常记录，返回已有文件信息 |
| 文件类型限制 | 上传系统设置不允许的扩展名 | 接口返回失败，不写入文件记录 |
| 文件大小限制 | 上传超过限制的文件 | 接口返回失败，存储目录不应残留业务可见文件 |
| 私有文件匿名读取 | 不带 Token 访问 `is_public = 0` 的文件信息或 URL | 返回 `403` |
| 公开文件匿名读取 | 访问 `is_public = 1` 的文件信息或 URL | 可以读取文件信息或 URL |
| 本地静态映射 | 打开返回的 `/api/files/**` URL，或生产 Nginx 暴露的 `/files/**` URL | 浏览器能访问文件，路径对应 `uploads/` 下真实文件 |
| 云存储降级 | 配一个不可用默认存储后上传 | 日志记录默认存储失败，并降级到本地存储 |
| 逻辑删除 | 调用 `DELETE /file/{id}` | `file_info.status` 变为删除态，普通查询不再把它当可用文件 |
| 强制删除 | 管理端执行物理删除 | 存储对象和数据库记录按预期删除，业务引用需要提前确认 |
| 迁移任务 | 创建并执行迁移任务 | 状态按 `PENDING`、`RUNNING`、结束态流转；当前实现要注意模拟迁移逻辑 |

开发新业务接入文件模块时，至少要补测“上传成功、重复上传、删除后业务页面表现、URL 访问权限”四件事。文件服务只负责存储，不应该替业务模块兜底所有权限判断。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 上传返回成功但数据库没有记录 | 存储上传成功后插入 `file_info` 失败 | 查日志里“建议手动清理文件” |
| 文件重复上传没有新记录 | MD5 命中已有正常文件 | 这是设计行为 |
| 云存储失败后文件到了本地 | 默认存储上传失败，自动降级 | 查 `storage_config.test_status` 和应用日志 |
| 下载失败但 URL 存在 | 默认存储不可读，本地兜底也没有文件 | 检查备份和存储配置 |
| 上传 URL 是 `/api/files`，部署里却配置了 `/files` | 本地策略、后端 context-path、Nginx alias 三层前缀没有统一 | 明确浏览器最终访问哪个入口，生产环境统一成一个公开前缀 |
| 迁移任务进度不可信 | 当前迁移实现仍有示例逻辑 | 生产化前要补真实迁移明细 |
| 私有文件被直接访问 | `/files/**` 或 `/api/files/**` 静态映射绕过业务归属校验 | 私密文件不要直接暴露静态 URL，优先走业务接口校验后下发 |
