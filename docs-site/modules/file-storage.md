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

---

## 文件存储模块深度拆解

> 以下内容基于 `xiaou-filestorage` 全部源码逐行拆解，覆盖 4 个 Controller、6 个 ServiceImpl、5 个 Domain、7 个 Mapper XML、1 个 Factory、1 个 Event 体系、1 个 DTO、1 个 Strategy 接口 + 1 个 Abstract + 2 个实现。

### 一、策略模式与模板方法架构详解

文件存储采用 **工厂 + 策略 + 模板方法** 三层设计：

```
FileStorageStrategy (接口)
  └── getStorageType(), initialize(), testConnection()
  └── uploadFile(), downloadFile(), deleteFile(), existsFile()
  └── getFileUrl(), copyFile(), getFileSize()

AbstractFileStorageStrategy (模板方法)
  ├── initialize() → 调 doInitialize() + 设 initialized=true
  ├── testConnection() → 先检查 initialized → 调 doTestConnection()
  ├── generateStoragePath() → moduleName/businessType/yyyy/MM/dd/时间戳_随机8位.ext
  ├── calculateMd5() → DigestUtil.md5Hex(inputStream)
  ├── getConfigParam(key) / getConfigParam(key, default)
  └── 抽象方法: doInitialize(), doTestConnection()

LocalStorageStrategy (本地实现)
  ├── basePath, urlPrefix
  ├── doInitialize() → 从 configParams 读 basePath 和 urlPrefix, 创建目录
  ├── doTestConnection() → Files.exists(baseDir) && Files.isWritable(baseDir)
  └── uploadFile() → Files.copy + 生成 urlPrefix + "/" + storagePath

StorageStrategyFactory (工厂)
  ├── strategyMap: ConcurrentHashMap<storageType, FileStorageStrategy> — Spring Bean 注册
  ├── initializedStrategies: ConcurrentHashMap<configId, FileStorageStrategy> — 按配置缓存
  ├── 构造注入所有 FileStorageStrategy Bean → 按 getStorageType() 注册
  ├── createAndInitialize(configId, type, params) → 反射创建新实例 + initialize + 缓存
  └── removeInitializedStrategy(configId) → 清除缓存（配置更新/禁用时调用）
```

**关键设计**：`StorageStrategyFactory` 在构造函数中自动收集所有 `FileStorageStrategy` Spring Bean。这意味着新增存储类型只需添加策略实现类并标注 `@Component`，无需修改工厂代码。

**关键发现 1**：`createAndInitialize` 使用**反射创建新实例**（`template.getClass().getDeclaredConstructor().newInstance()`），而不是直接使用 Spring Bean。这是因为每个存储配置需要独立的参数——两个 OSS 配置（不同 bucket）不能共享同一个策略实例。但反射创建意味着新实例**不再是 Spring Bean**，无法享受依赖注入、AOP 等特性。

**关键发现 2**：`initializedStrategies` 使用 `ConcurrentHashMap` 缓存，但没有 TTL 或容量上限。如果管理员频繁创建和测试存储配置，缓存会无限增长。`removeInitializedStrategy` 只在配置更新/禁用时被调用，正常使用中缓存不会主动清理。

### 二、上传主链路深度分析

**源码**：`FileStorageServiceImpl.uploadSingle`

```
uploadSingle(file, moduleName, businessType):
  1. file != null && moduleName != null → 否则 throw
  2. 读取 originalName, size, contentType
     contentType 优先取 file.getContentType()
     缺失时用 Apache Tika 探测 (DetectUtil.detect)
  3. 校验扩展名: fileSystemSettingService.isAllowedFileType(originalName)
  4. 校验大小: size <= fileSystemSettingService.getMaxFileSize()
  5. 计算 MD5: DigestUtil.md5Hex(inputStream)
     注意: 这会消耗 InputStream, 后续上传需要重新获取流
  6. MD5 去重: fileInfoMapper.selectByMd5Hash(md5Hash)
     如果找到 status=1 的相同 MD5 → 直接返回已有 FileInfo
  7. 获取默认存储: storageConfigMapper.selectDefault()
  8. 默认存储存在且可用 → 尝试上传到默认存储
     失败 → 降级到本地存储 (storageType = "LOCAL")
  9. 两个都失败 → 返回 FileUploadResult.failure
  10. 写入 file_info:
      - storedName = uploadResult.storagePath
      - accessUrl = uploadResult.accessUrl
      - md5Hash = md5
      - moduleName, businessType
      - isPublic = 0 (默认私有)
      - status = 1
  11. 发布上传事件: FileOperationEventPublisher.publishUploadEvent
  12. 异步备份: fileBackupService.createLocalBackupAsync(fileInfo)
```

**关键发现 1**：步骤 5 计算 MD5 时会消耗 `InputStream`。后续步骤 8 上传到存储策略时需要新的 `InputStream`。这意味着 MD5 计算和实际上传使用的是**两个不同的流**——MultipartFile 的流可以被重复获取，但代码中的实现细节值得注意。

**关键发现 2**：MD5 去重查询 `selectByMd5Hash` 只匹配 `status = 1` 的记录。如果一个文件被逻辑删除（`status = 0`），相同内容的文件会再次上传。这是合理的设计——已删除的文件不应阻止重新上传。

**关键发现 3**：降级机制是"默认存储失败 → 本地存储"，而不是"默认存储失败 → 下一个可用存储"。`StorageHealthServiceImpl.getNextAvailableStorage` 实现了寻找下一个 LOCAL 存储的逻辑，但 `uploadSingle` 的降级路径没有调用它。

### 三、文件访问权限双轨判定

**源码**：`FileController.canReadFile`

```java
private boolean canReadFile(FileInfo fileInfo) {
    return Integer.valueOf(1).equals(fileInfo.getIsPublic()) || isAuthenticated();
}
```

这个判断只解决两种场景：
- `isPublic = 1`：任何人都能读（包括匿名）
- `isPublic = 0`：只要登录就能读（不管是谁）

**关键发现**：私有文件的访问控制**只看登录态，不看归属**。这意味着任意已登录用户都可以读取任意私有文件的 URL、信息和下载流。这对头像、公开图片没问题，但对简历附件、聊天图片等敏感场景是权限漏洞。

**对比**：通知模块的双轨机制（个人消息看 `receiverId`）有明确的归属检查，但文件模块没有。文档中已经提示了这个问题，源码验证了这一点。

### 四、事件驱动访问记录

文件模块使用 Spring 事件机制记录访问日志：

```
FileController → fileStorageService.downloadFile(id)
  → FileOperationEventPublisher.publishDownloadEvent(fileId, fileName, ip)
    → ApplicationEventPublisher.publishEvent(FileOperationEvent)
      → FileOperationEventListener.handleFileOperationEvent(event)  // @Async
        → recordFileAccess(event) → INSERT file_access
        → handleDownloadEvent(event) → log.debug(...)
```

**事件类型与访问类型映射**：

| EventType | file_access.access_type | 说明 |
| --- | --- | --- |
| DOWNLOAD | `"DOWNLOAD"` | 下载 |
| ACCESS | `"VIEW"` | 查看 |
| UPLOAD | `EventType.name()` = `"UPLOAD"` | 上传 |
| DELETE | `EventType.name()` = `"DELETE"` | 删除 |

**关键发现**：UPLOAD 和 DELETE 事件也会写入 `file_access` 表，但它们的 `accessType` 是枚举名称字符串。而 DOWNLOAD 和 ACCESS 有专门的映射（`"DOWNLOAD"` 和 `"VIEW"`）。这意味着 `file_access` 表既是访问日志又是操作日志——语义不够清晰。

**关键发现 2**：事件监听器使用 `@Async`，访问日志是异步写入的。如果数据库连接异常，访问日志可能丢失。与通知模块的异步写入问题一致。

### 五、存储配置管理生命周期

**源码**：`StorageConfigServiceImpl`

```
createConfig(storageConfig):
  1. 检查名称唯一 → selectByConfigName
  2. 检查存储类型支持 → strategyFactory.isSupported
  3. 如果 isDefault=1 → clearAllDefaultStatus
  4. insert → 成功后自动 testConfig

updateConfig(storageConfig):
  1. 检查配置存在
  2. 检查名称不与其他冲突
  3. 如果设为默认 → clearAllDefaultStatus
  4. update → 成功后:
     - removeInitializedStrategy(id)  // 清除缓存实例
     - 如果启用 → testConfig(id)      // 重新测试

deleteConfig(id):
  1. 不允许删除默认配置 (isDefault=1 → return false)
  2. 删除记录 + removeInitializedStrategy(id)

setDefaultConfig(id):
  1. 配置必须存在且启用
  2. clearAllDefaultStatus → updateDefaultStatus(id, 1)

toggleConfig(id, isEnabled):
  如果禁用默认配置 → 先取消默认状态 (updateDefaultStatus(id, 0))
  然后更新启用状态 + removeInitializedStrategy(id)
```

**关键发现**：配置创建后会自动执行 `testConfig`，这意味着创建配置时如果存储连接不通，配置仍然会写入数据库（只是 `test_status=0`）。配置创建和测试不在同一个事务里——`testConfig` 在 `insert` 之后执行，且 `testConfig` 本身不是事务方法。

### 六、物理删除与文件移动

**源码**：`AdminFileController.forceDeleteFile`

```
forceDeleteFile(id):
  1. selectById → 文件不存在视为删除成功
  2. 获取默认存储配置
  3. 创建策略实例 → strategy.deleteFile(storedName)
  4. fileInfoMapper.deleteById(id) → 物理删除数据库记录
```

**关键发现 1**：物理删除只尝试删除**默认存储**上的文件。如果文件实际存储在本地（因为上传时默认存储失败降级了），物理删除会尝试从默认存储（可能是 OSS）删除一个根本不存在的文件，而本地磁盘上的文件不会被清理。

**关键发现 2**：物理删除没有发布删除事件，也没有删除 `file_storage` 和 `file_access` 中的关联记录。这会导致"孤儿数据"——文件主记录删了，但存储位置记录和访问日志还在。

**源码**：`AdminFileController.moveFile`

```
moveFile(id, targetStorageId):
  1. 获取文件信息
  2. 获取"源存储"配置 → selectDefault()  ← 注意：不是文件实际存储
  3. 获取目标存储配置
  4. 初始化两个策略
  5. 从源存储下载 → 上传到目标存储
  6. 更新 file_info 的 storedName 和 accessUrl
  7. 删除源存储文件
  8. 如果更新数据库失败 → 删除目标存储中的文件（回滚）
```

**关键发现 3**：移动文件的源存储配置使用 `selectDefault()`，而不是文件实际所在的存储。如果文件存储在本地（降级上传的），移动操作会尝试从默认存储（可能是 OSS）下载一个不存在的文件。

### 七、迁移任务状态机与半成品实现

**源码**：`FileMigrationServiceImpl`

```
迁移状态:
  PENDING → RUNNING → COMPLETED
                      → FAILED
  PENDING/RUNNING → STOPPED (手动停止)
  RUNNING 不可删除, 其余状态可删除
```

**异步执行**：

```java
private void executeMigrationAsync(FileMigration migration) {
    new Thread(() -> {  // ← 警告：使用裸 Thread，不是线程池
        try {
            performMigration(migration);
            migration.setStatus("COMPLETED");
            migration.setTotalFiles(100);   // ← 模拟数据
            migration.setSuccessCount(98);  // ← 模拟数据
            migration.setFailCount(2);      // ← 模拟数据
            fileMigrationMapper.updateById(migration);
        } catch (Exception e) {
            migration.setStatus("FAILED");
            ...
        }
    }).start();
}
```

**关键发现 1**：异步执行使用 `new Thread().start()`，没有使用线程池。这与文档提示一致——注释明确写了"在实际项目中，这里应该使用异步线程池来执行迁移任务"。裸 Thread 无法被监控、无法限制并发、无法优雅停机。

**关键发现 2**：迁移完成后写入的是**模拟数据**（`totalFiles=100, successCount=98, failCount=2`）。`performMigration` 方法虽然有真实迁移框架（查询文件列表、逐个处理），但核心迁移逻辑（下载+上传+验证）被 `Thread.sleep(100)` 模拟替代。

**关键发现 3**：迁移方法标注了 `@Transactional(rollbackFor = Exception.class)`，但 `executeMigrationAsync` 在新线程中执行。事务只在主线程有效，异步线程中的 `fileMigrationMapper.updateById` 不在事务保护范围内。这意味着如果异步线程执行到一半崩溃，迁移状态不会自动回滚到 PENDING。

### 八、本地备份机制详解

**源码**：`FileBackupServiceImpl`

```
createLocalBackupAsync(fileInfo):  // @Async("fileBackupExecutor")
  1. hasLocalBackup(fileId) → 已有则跳过
  2. 备份目录: /data/file-backup/{moduleName}
  3. 备份文件名: {fileId}_{storedName}
  4. 源路径: System.getProperty("user.dir") + "/uploads/" + storedName
  5. 源文件存在 → FileUtil.copy(源, 目标, true)
  6. 源文件不存在 → 跳过备份（只 warn）
  7. 写入 file_storage: storageConfigId=1L(硬编码本地), isPrimary=0, syncStatus=1
```

**关键发现 1**：备份路径 `/data/file-backup` 是硬编码常量，不是从配置读取。如果部署环境不支持这个路径（如 Windows 系统或容器化部署），备份会失败。

**关键发现 2**：`storageConfigId = 1L` 是硬编码。代码注释写着"本地存储配置ID假设为1"。如果管理员创建了新的存储配置导致 ID 不是 1，备份记录会指向错误的配置。

**关键发现 3**：备份从 `System.getProperty("user.dir") + "/uploads/"` 复制文件。这假设文件一定在本地上传目录，但如果文件上传到了 OSS（成功上传到默认存储），本地根本没有源文件，备份会被跳过——只打了一条 warn 日志。

**过期清理**：

```
cleanExpiredBackups():
  retentionDays = 30  // 硬编码，不从配置读取
  cutoffTime = 当前时间 - 30天
  → 递归遍历 /data/file-backup 目录
  → 文件最后修改时间 < cutoffTime → 删除
  → 空子目录 → 也删除
```

**关键发现 4**：清理只删除物理文件，**不删除 `file_storage` 表中的记录**。这会导致数据库里仍有指向已删除文件的备份记录。

### 九、系统设置缓存策略

**源码**：`FileSystemSettingServiceImpl`

```
getSystemSettings() → 查询所有 file_system_setting → 5分钟内存缓存
getMaxFileSize() → 从缓存读 "MAX_FILE_SIZE" → 默认 104857600 (100MB)
isAllowedFileType(name) → 从缓存读 "ALLOWED_FILE_TYPES" →逗号分隔白名单
updateSystemSettings(settings) → 逐项更新 → 清缓存
```

**关键发现**：设置缓存是**内存缓存**（ConcurrentHashMap + TTL），不是 Redis。这意味着多实例部署时，更新设置后只有当前实例的缓存被清理，其他实例需要等 TTL 过期才能读到新值。

### 十、健康检查简化实现

**源码**：`StorageHealthServiceImpl`

```java
public boolean isStorageHealthy(StorageConfig config) {
    // 直接执行健康检查，不使用缓存
    return performHealthCheck(config);
}

public StorageConfig getNextAvailableStorage(Long excludeConfigId) {
    // 简化逻辑：故障转移时直接返回本地存储配置
    return enabledConfigs.stream()
        .filter(c -> !c.getId().equals(excludeConfigId))
        .filter(c -> "LOCAL".equals(c.getStorageType()))
        .findFirst().orElse(null);
}

public void checkAllStorageHealth() {
    // 简化版本：不再进行定时批量检查
    log.debug("简化版本已移除定时批量健康检查，改为按需检查");
}
```

**关键发现**：健康检查被大幅简化。注释明确写着"简化版本已移除定时批量健康检查"。这意味着：
1. 每次上传都要实时执行健康检查（创建策略实例 + testConnection），开销较大
2. 故障转移只找 LOCAL 类型，不会跳到其他云存储
3. 没有定时巡检，存储故障只能在上传时被动发现

### 十一、深度发现与坑点

#### 11.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 物理删除只删默认存储上的文件 | `AdminFileController.forceDeleteFile` | 降级到本地的文件不会被清理 |
| BUG-2 | 移动文件使用 selectDefault 获取源存储 | `AdminFileController.moveFile` | 实际在本地存储的文件无法移动 |
| BUG-3 | 迁移使用裸 Thread | `FileMigrationServiceImpl.executeMigrationAsync` | 无法监控、限流和优雅停机 |
| BUG-4 | 迁移进度写入模拟数据 | `FileMigrationServiceImpl.executeMigrationAsync:249-251` | 进度数字不可信 |
| BUG-5 | 备份 storageConfigId 硬编码为 1L | `FileBackupServiceImpl:84` | 配置 ID 变化后备份记录指向错误 |
| BUG-6 | 备份清理只删文件不删数据库记录 | `FileBackupServiceImpl.cleanExpiredBackups` | 数据库残留孤儿备份记录 |
| BUG-7 | 物理删除不清理关联记录 | `AdminFileController.forceDeleteFile` | file_storage/file_access 残留 |
| BUG-8 | 私有文件只看登录不看归属 | `FileController.canReadFile` | 任意登录用户可读任意私有文件 |

#### 11.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | 策略实例缓存无容量限制 | initializedStrategies 是 ConcurrentHashMap，无 TTL 和上限 |
| RISK-2 | 系统设置内存缓存多实例不同步 | 5 分钟 TTL，多实例部署时更新延迟 |
| RISK-3 | 反射创建策略实例脱离 Spring 管理 | 新实例无 AOP、无依赖注入 |
| RISK-4 | 备份路径硬编码 /data/file-backup | Windows 和容器环境可能不兼容 |
| RISK-5 | 健康检查无定时巡检 | 存储故障只在上传时被动发现 |
| RISK-6 | 迁移事务与异步线程不一致 | @Transactional 只在主线程有效，异步线程不回滚 |
| RISK-7 | 静态资源映射绕过权限 | `/files/**` 或 `/api/files/**` 直接暴露物理文件 |

#### 11.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | 策略模式屏蔽存储差异 | 同一上传流程支持 LOCAL/OSS/COS/KODO/OBS |
| H-2 | 模板方法统一公共逻辑 | MD5 计算、路径生成、配置读取由抽象类处理 |
| H-3 | 工厂自动注册策略 Bean | 新增存储类型只需加策略实现类 |
| H-4 | 上传降级本地兜底 | 默认存储失败时自动降级到本地，保证上传不中断 |
| H-5 | MD5 去重 | 相同内容文件不重复存储 |
| H-6 | 事件驱动访问记录 | 上传/下载/访问/删除事件异步记录到 file_access |
| H-7 | 系统设置内存缓存 | 5 分钟 TTL 减少频繁查 DB |
| H-8 | 配置更新自动清除缓存实例 | 更新/禁用配置后 removeInitializedStrategy 保证一致性 |
| H-9 | 不允许删除默认配置 | 防止误删导致上传无目标 |
| H-10 | 配置创建后自动测试 | 确保新配置可用性 |

#### 11.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 上传主链路 | `FileStorageServiceImpl.java` — 校验→MD5去重→上传→降级→落库→事件→备份 |
| 策略模式 | `FileStorageStrategy.java` + `AbstractFileStorageStrategy.java` + `LocalStorageStrategy.java` |
| 策略工厂 | `StorageStrategyFactory.java` — 自动注册+反射创建+缓存 |
| 文件访问权限 | `FileController.java:canReadFile` — isPublic || isAuthenticated |
| 事件体系 | `FileOperationEvent.java` + `Publisher` + `Listener` — 5 种事件类型 |
| 存储配置管理 | `StorageConfigServiceImpl.java` — 创建/更新/删除/默认/测试/启禁 |
| 物理删除 | `AdminFileController.forceDeleteFile` — 只删默认存储+数据库记录 |
| 文件移动 | `AdminFileController.moveFile` — 下载→上传→更新→删源 |
| 迁移任务 | `FileMigrationServiceImpl.java` — 裸Thread+模拟数据+无事务保护 |
| 本地备份 | `FileBackupServiceImpl.java` — 硬编码路径+硬编码ID+文件复制 |
| 统计服务 | `FileStatisticsServiceImpl.java` — 模块/类型/趋势/热门 |
| 健康检查 | `StorageHealthServiceImpl.java` — 简化版无缓存无定时 |
| 系统设置 | `FileSystemSettingServiceImpl.java` — 内存5分钟缓存 |
