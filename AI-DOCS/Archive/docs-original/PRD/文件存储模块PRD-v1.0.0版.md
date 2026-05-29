# 文件存储模块PRD-v1.0.0

## 一、背景与目标

Code-Nest项目文件存储模块是一个功能完整的统一文件管理平台，基于策略模式设计，支持多云存储无缝切换、智能文件迁移、本地存档备份、统计分析等企业级功能。为整个系统提供稳定、高效、可扩展的文件存储服务。

**技术架构**：Spring Boot + MyBatis + 策略模式 + 事件驱动
**存储支持**：本地存储、阿里云OSS、腾讯云COS、七牛云KODO、华为云OBS

------

## 二、核心功能实现

### 2.1 文件存储服务 (FileStorageService)

#### 2.1.1 文件上传功能
- **单文件上传** (`uploadSingle`)
  - 支持模块标识和业务类型分类
  - 自动文件类型检测（基于Apache Tika）
  - MD5重复检测机制
  - 文件大小和类型校验
  - 返回文件ID、访问URL等完整信息

- **批量文件上传** (`uploadBatch`)
  - 支持多文件并行处理
  - 统一的模块和业务类型管理
  - 批量结果返回和错误处理

#### 2.1.2 文件访问功能
- **文件下载** (`downloadFile`)
  - 返回InputStream文件流
  - 支持多存储源自动切换
  - 故障转移机制（主存储→本地存储）

- **文件信息获取** (`getFileInfo`)
  - 返回完整文件元数据
  - 包含文件名、大小、类型、上传时间等

- **文件URL生成** (`getFileUrl`)
  - 支持临时链接生成
  - 可配置有效期（小时）
  - 支持批量URL获取 (`getFileUrls`)

#### 2.1.3 文件管理功能
- **逻辑删除** (`deleteFile`)
  - 基于模块权限验证
  - 软删除机制，支持恢复
  
- **文件列表查询** (`listFiles`)
  - 支持模块和业务类型筛选
  - 分页查询支持
  - 多维度搜索功能

### 2.2 多云存储策略 (Strategy Pattern)

#### 2.2.1 已实现存储策略
- **LocalStorageStrategy** - 本地文件系统存储
- **OssStorageStrategy** - 阿里云对象存储
- **CosStorageStrategy** - 腾讯云对象存储  
- **KodoStorageStrategy** - 七牛云对象存储
- **ObsStorageStrategy** - 华为云对象存储

#### 2.2.2 统一接口设计 (FileStorageStrategy)
```java
// 核心接口方法
- uploadFile(InputStream, String, Map<String, Object>)  // 文件上传
- downloadFile(String)                                   // 文件下载  
- deleteFile(String)                                     // 文件删除
- generateUrl(String, Integer)                           // URL生成
- testConnection()                                       // 连接测试
```

#### 2.2.3 策略工厂 (StorageStrategyFactory)
- 动态策略选择和创建
- 配置驱动的存储后端切换
- 运行时策略实例化管理

### 2.3 文件迁移服务 (FileMigrationService)

#### 2.3.1 迁移任务管理
- **创建迁移任务** (`createMigrationTask`)
  - 支持源存储→目标存储配置
  - 多种迁移类型：FULL、INCREMENTAL、TIME_RANGE、FILE_TYPE
  - 可配置筛选参数和任务名称

- **执行控制**
  - 异步任务执行 (`executeMigration`)
  - 实时停止功能 (`stopMigration`)
  - 任务删除管理 (`deleteMigrationTask`)

#### 2.3.2 进度监控
- **实时进度查询** (`getMigrationProgress`)
  - 总文件数、成功数、失败数统计
  - 任务状态跟踪：PENDING、RUNNING、COMPLETED、FAILED、STOPPED
  
- **任务列表管理** (`listMigrationTasks`)
  - 按状态筛选任务
  - 限制查询数量控制

### 2.4 存储配置服务 (StorageConfigService)

#### 2.4.1 配置管理功能
- 存储配置CRUD操作
- 配置参数JSON存储和解析
- 启用/禁用状态控制
- 默认存储配置设置

#### 2.4.2 配置验证功能
- **连接测试** (`testConfig`)
  - 实时验证存储配置可用性
  - 返回测试状态和错误信息
  
- **支持的存储类型** (`getSupportedStorageTypes`)
  - 动态获取可用存储类型列表

### 2.5 统计分析服务 (FileStatisticsService)

#### 2.5.1 基础统计功能
- **文件统计** (`getFileStatistics`)
  - 总文件数、已删除文件数
  - 总存储大小、今日新增文件
  - 存储配置数量统计

- **存储使用分析** (`getStorageUsage`)
  - 各模块存储使用情况
  - 存储平台使用分布
  - 文件类型分布统计

#### 2.5.2 高级统计功能
- **模块统计** (`getModuleStatistics`)
- **文件类型统计** (`getFileTypeStatistics`)
- **上传趋势分析** (`getUploadTrend`)
- **热门文件统计** (`getHotFiles`)

### 2.6 系统设置服务 (FileSystemSettingService)

#### 2.6.1 系统配置管理
- **设置CRUD** (`getSystemSettings`/`updateSystemSettings`)
- **单项设置管理** (`getSettingValue`/`updateSetting`)

#### 2.6.2 文件限制管理
- **文件类型白名单** (`getAllowedFileTypes`/`updateFileTypes`)
- **文件大小限制** (`getMaxFileSize`/`isFileSizeExceeded`)
- **类型检查** (`isFileTypeAllowed`)

#### 2.6.3 配额与安全设置
- **模块存储配额** (`getModuleStorageQuota`)
- **临时链接有效期** (`getTempLinkExpireHours`)
- **自动备份配置** (`isAutoBackupEnabled`)

------

## 三、数据模型设计

### 3.1 核心实体

#### FileInfo（文件信息实体）
```sql
-- 字段设计
id: BIGINT                    -- 文件ID
original_name: VARCHAR(255)   -- 原始文件名
stored_name: VARCHAR(255)     -- 存储文件名
file_size: BIGINT            -- 文件大小(字节)
content_type: VARCHAR(100)    -- MIME类型
md5_hash: VARCHAR(32)         -- MD5校验值
module_name: VARCHAR(50)      -- 所属模块名称
business_type: VARCHAR(50)    -- 业务类型
upload_time: TIMESTAMP        -- 上传时间
access_url: VARCHAR(500)      -- 访问URL
status: TINYINT              -- 文件状态(0=已删除,1=正常)
is_public: TINYINT           -- 访问权限(0=私有,1=公开)
delete_time: TIMESTAMP        -- 删除时间
create_time: TIMESTAMP        -- 创建时间
update_time: TIMESTAMP        -- 更新时间
```

#### StorageConfig（存储配置实体）
```sql
-- 字段设计
id: BIGINT                    -- 配置ID
storage_type: VARCHAR(20)     -- 存储类型(LOCAL,OSS,COS,KODO,OBS)
config_name: VARCHAR(100)     -- 配置名称
config_params: LONGTEXT       -- 配置参数JSON
is_default: TINYINT          -- 是否默认存储(0=否,1=是)
is_enabled: TINYINT          -- 是否启用(0=禁用,1=启用)
test_status: TINYINT         -- 测试状态(0=失败,1=成功,NULL=未测试)
create_time: TIMESTAMP        -- 创建时间
update_time: TIMESTAMP        -- 更新时间
```

#### FileMigration（文件迁移实体）
```sql
-- 字段设计
id: BIGINT                    -- 迁移任务ID
task_name: VARCHAR(100)       -- 任务名称
source_storage_id: BIGINT     -- 源存储配置ID
target_storage_id: BIGINT     -- 目标存储配置ID
migration_type: VARCHAR(20)   -- 迁移类型(FULL,INCREMENTAL,TIME_RANGE,FILE_TYPE)
filter_params: TEXT          -- 筛选参数JSON
total_files: INT             -- 总文件数
success_count: INT           -- 成功数量
fail_count: INT              -- 失败数量
status: VARCHAR(20)          -- 任务状态(PENDING,RUNNING,COMPLETED,FAILED,STOPPED)
start_time: TIMESTAMP        -- 开始时间
end_time: TIMESTAMP          -- 结束时间
error_message: TEXT          -- 错误信息
create_time: TIMESTAMP       -- 创建时间
update_time: TIMESTAMP       -- 更新时间
```

### 3.2 辅助实体

#### FileAccess（文件访问记录）
```sql
-- 用于访问日志记录
id, file_id, access_time, access_ip, module_name
```

#### FileStorage（文件存储记录）
```sql
-- 用于多存储副本管理
id, file_id, storage_config_id, storage_path, is_primary, sync_status
```

#### FileSystemSetting（系统设置）
```sql
-- 用于系统配置管理
id, setting_key, setting_value, description, create_time, update_time
```

------

## 四、API接口设计

### 4.1 公开接口 (/file)

#### 文件操作接口
- `POST /file/upload/single` - 单文件上传
- `POST /file/upload/batch` - 批量文件上传
- `GET /file/download/{id}` - 文件下载
- `GET /file/info/{id}` - 获取文件信息
- `GET /file/url/{id}` - 获取文件访问URL
- `POST /file/urls` - 批量获取文件URL
- `DELETE /file/{id}` - 逻辑删除文件
- `GET /file/list` - 查询文件列表
- `POST /file/exists` - 检查文件存在性

### 4.2 管理员接口

#### 文件管理 (/admin/file)
- `GET /admin/file/list` - 管理员文件列表（支持全量查看）
- `DELETE /admin/file/{id}/force` - 物理删除文件
- `PUT /admin/file/{id}/move` - 移动文件到其他存储
- `GET /admin/file/statistics` - 获取文件统计信息
- `GET /admin/file/storage-usage` - 获取存储使用情况

#### 存储配置 (/admin/storage)
- `GET /admin/storage/configs` - 存储配置列表
- `POST /admin/storage/config` - 创建存储配置
- `GET /admin/storage/config/{id}` - 获取存储配置详情
- `PUT /admin/storage/config/{id}` - 更新存储配置
- `DELETE /admin/storage/config/{id}` - 删除存储配置
- `POST /admin/storage/config/{id}/test` - 测试存储配置
- `PUT /admin/storage/config/{id}/enable` - 启用/禁用存储配置
- `PUT /admin/storage/config/{id}/default` - 设置默认存储配置
- `GET /admin/storage/types` - 获取支持的存储类型

#### 文件迁移 (/admin/file)
- `POST /admin/file/migrate` - 创建迁移任务
- `GET /admin/file/migration/{id}` - 查询迁移任务状态
- `GET /admin/file/migrations` - 迁移任务列表
- `POST /admin/file/migration/{id}/execute` - 执行迁移任务
- `PUT /admin/file/migration/{id}/stop` - 停止迁移任务
- `DELETE /admin/file/migration/{id}` - 删除迁移任务
- `GET /admin/file/migration/{id}/progress` - 获取迁移进度

#### 系统设置 (/admin/system)
- `GET /admin/system/settings` - 获取系统设置
- `PUT /admin/system/settings` - 更新系统设置
- `GET /admin/system/file-types` - 获取允许的文件类型
- `PUT /admin/system/file-types` - 更新文件类型白名单
- `GET /admin/system/summary` - 获取系统配置摘要

------

## 五、技术特性与架构

### 5.1 设计模式应用

#### 策略模式 (Strategy Pattern)
- **FileStorageStrategy** 抽象策略接口
- **AbstractFileStorageStrategy** 抽象实现基类
- 5种具体策略实现：Local、OSS、COS、KODO、OBS
- **StorageStrategyFactory** 策略工厂

#### 事件驱动架构
- **FileOperationEventPublisher** 文件操作事件发布
- 异步事件处理机制
- 解耦文件操作与后续处理逻辑

### 5.2 安全与可靠性

#### 数据安全
- MD5文件完整性校验
- 配置参数加密存储
- 基于模块的权限控制
- 文件访问日志记录

#### 容错机制
- 存储健康检查 (StorageHealthService)
- 自动故障转移
- 本地备份服务 (FileBackupService)
- 异步备份机制

### 5.3 性能优化

#### 存储优化
- 策略模式支持动态切换
- 连接池复用
- 文件元信息缓存
- 异步处理机制

#### 查询优化
- 分页查询支持
- 索引优化设计
- 批量操作接口
- 统计信息缓存

------

## 六、核心业务流程

### 6.1 文件上传流程
1. **接收请求** → 模块调用上传接口
2. **参数校验** → 文件大小、类型、模块权限验证
3. **重复检测** → MD5校验，避免重复存储
4. **存储选择** → 根据默认配置选择存储策略
5. **文件存储** → 调用具体存储策略上传文件
6. **记录保存** → 保存文件元信息到数据库
7. **异步备份** → 创建本地副本（如启用）
8. **结果返回** → 返回文件ID、URL等信息

### 6.2 文件迁移流程
1. **任务创建** → 配置源存储、目标存储、迁移策略
2. **文件扫描** → 根据筛选条件查找待迁移文件
3. **任务执行** → 异步执行迁移任务
4. **文件迁移** → 下载→上传→校验→更新记录
5. **进度更新** → 实时更新成功/失败计数
6. **完成处理** → 更新任务状态，清理临时数据

### 6.3 存储切换流程
1. **配置验证** → 测试新存储配置可用性
2. **设置默认** → 更新默认存储配置
3. **迁移提示** → 可选择是否迁移现有文件
4. **自动迁移** → 创建迁移任务（如选择）
5. **切换完成** → 新文件使用新存储

------

## 七、功能特色

### 7.1 企业级特性
- **多云统一管理** - 一套系统管理多个云存储平台
- **智能故障转移** - 主存储不可用时自动切换备用存储  
- **本地双重备份** - 云存储+本地存档双重保障
- **完善监控统计** - 存储使用情况全面监控分析

### 7.2 开发友好
- **统一接口设计** - 屏蔽底层存储差异，提供一致的API
- **模块化架构** - 基于Spring Boot的模块化设计
- **策略模式扩展** - 易于新增存储类型支持
- **事件驱动机制** - 松耦合的异步处理架构

### 7.3 运维管理
- **可视化管理界面** - 完整的Web管理后台
- **实时迁移监控** - 迁移任务进度实时监控
- **配置热更新** - 无需重启即可更新存储配置
- **详细操作日志** - 完整的文件操作审计日志

------

## 八、版本说明

**版本**：v2.0.0（实际实现版）  
**基于**：xiaou-filestorage 模块实际代码实现  
**技术栈**：Spring Boot 2.x + MyBatis + MySQL + Redis  
**存储支持**：本地存储、阿里云OSS、腾讯云COS、七牛云KODO、华为云OBS  
**架构模式**：策略模式 + 事件驱动 + 分层架构  

**功能完整度**：✅ 100%已实现，生产就绪 