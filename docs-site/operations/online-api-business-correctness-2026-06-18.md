# 线上接口业务正确性测试（2026-06-18）

本次测试目标不是确认接口“能不能连通”，而是验证线上接口在当前生产数据和部署状态下的业务正确性：包括成功路径的写入与读回、状态变化、权限边界、参数校验、预期业务错误，以及 OpenAPI 全量路由在真实服务上的返回分类。

## 测试结论

结论：**部分通过，发现 2 个线上数据或迁移问题**。

| 项目 | 结果 |
| --- | --- |
| 服务健康 | 通过，Actuator 返回 `UP`，MySQL 与 Redis 均为 `UP` |
| 公开入口 | 通过，用户侧入口 `http://36.140.150.167:81`，API 入口 `http://36.140.150.167:81/api` |
| OpenAPI 全量扫描 | 757 个 operation 中 752 个按预期分类，5 个文件存储相关 operation 失败 |
| 服务级业务用例 | 384 个业务步骤中 381 个通过，敏感词新增链路 1 个业务断言失败 |
| 主要问题 | 文件存储表结构与代码期望不一致；敏感词分类种子数据缺失 |

## 环境与入口

| 项目 | 值 |
| --- | --- |
| 测试日期 | `2026-06-18` |
| 服务器 | `36.140.150.167` |
| 主机名 | `ecs-65178302` |
| 后端进程 | Spring Boot，监听 `127.0.0.1:9999` |
| Nginx 入口 | `80`、`81`、`82` |
| API 入口 | `http://36.140.150.167:81/api` |
| OpenAPI | `/api/v3/api-docs` |
| 健康检查 | `/api/actuator/health` |

执行 harness 时使用服务器本机 `http://127.0.0.1:81/api` 作为 base URL，这样仍经过 Nginx 代理和真实后端，同时可以读取本机验证码、日志和数据库证据来完成业务断言。

## 测试方法

| 层级 | 验证内容 |
| --- | --- |
| 健康检查 | 服务可用性、数据库和 Redis 依赖状态 |
| OpenAPI 全量扫描 | 从线上 `/v3/api-docs` 读取 757 个 operation，按无需登录、用户登录、管理员登录和安全跳过分类执行 |
| 服务级业务 harness | 对用户、博客、团队、AI 配置、学习资产、积分抽奖、通知、摸鱼、敏感词等模块做业务步骤断言 |
| 数据库与日志核验 | 对失败链路补充表结构、种子数据和后端异常日志证据 |

服务级 harness 不是单纯请求接口，而是覆盖了创建、查询、更新、删除、权限拒绝、重复提交、非法参数、预期业务错误等业务属性。

## 全量接口结果

OpenAPI 全量扫描共识别 `757` 个 operation。

| 分类 | 数量 | 含义 |
| --- | ---: | --- |
| `PASS_AUTH` | 419 | 鉴权或权限边界按预期拒绝 |
| `PASS_BUSINESS_ERROR` | 119 | 业务错误按预期返回 |
| `PASS_SUCCESS` | 195 | 成功路径返回正常 |
| `PASS_VALIDATION` | 19 | 参数校验按预期触发 |
| `FAIL_BUSINESS_500` | 4 | HTTP 200 但业务码为 500 |
| `FAIL_HTTP_5XX` | 1 | HTTP 500 |

失败 operation 集中在文件存储模块：

| 方法 | 路径 | 结果 | 现象 |
| --- | --- | --- | --- |
| `POST` | `/file/urls` | `FAIL_BUSINESS_500` | 查询 `file_info.storage_config_id` 报未知字段 |
| `GET` | `/file/url/{id}` | `FAIL_BUSINESS_500` | 查询 `file_info.storage_config_id` 报未知字段 |
| `GET` | `/file/info/{id}` | `FAIL_BUSINESS_500` | 查询 `file_info.storage_config_id` 报未知字段 |
| `GET` | `/file/download/{id}` | `FAIL_HTTP_5XX` | 下载链路返回 HTTP 500 |
| `DELETE` | `/file/{id}` | `FAIL_BUSINESS_500` | 返回“文件删除失败” |

## 服务级业务正确性结果

| 模块 | 步骤数 | 失败数 | 结论 |
| --- | ---: | ---: | --- |
| 用户、鉴权、管理员用户 | 57 | 0 | 通过 |
| 博客 | 54 | 0 | 通过 |
| 团队 | 64 | 0 | 通过 |
| 管理端 AI 配置 | 17 | 0 | 通过；线上未配置真实 AI 调用，已按配置态验证 |
| 学习资产 | 27 | 0 | 通过 |
| 积分与抽奖 | 45 | 0 | 通过 |
| 通知 | 48 | 0 | 通过 |
| 摸鱼工具 | 69 | 0 | 通过 |
| 敏感词风控 | 3 | 1 | 失败；管理员新增敏感词被业务码 `409` 拒绝 |

合计：`384` 个服务级业务步骤，`381` 个步骤通过。敏感词 harness 在失败点停止，因此敏感词模块后续链路没有继续展开。

## 发现的问题

### P0：文件存储表结构与代码期望不一致

影响范围：文件 URL 获取、文件详情、文件下载、文件删除等线上文件能力。

证据：

- OpenAPI 全量扫描中 5 个失败 operation 全部集中在 `/file/**`。
- 后端返回的数据库异常指向 `Unknown column 'storage_config_id' in 'SELECT'`。
- 线上 `file_info` 表字段不包含 `storage_config_id`。
- 当前代码 `xiaou-filestorage/src/main/resources/mapper/FileInfoMapper.xml` 的 `Base_Column_List`、插入和更新语句均引用 `storage_config_id`。

判断：线上数据库迁移版本落后于当前后端代码，或文件存储模块迁移脚本未完整执行。

建议：

1. 先备份线上数据库。
2. 对比当前仓库 SQL/migration 与线上 `file_info`、`file_storage`、`storage_config` 表结构。
3. 补齐缺失字段或回滚代码到匹配 schema 的版本。
4. 修复后重跑 OpenAPI 全量扫描和文件存储专项业务 harness。

### P1：敏感词分类种子数据缺失

影响范围：管理端新增敏感词、敏感词分类关联、敏感词风控配置初始化。

证据：

- 业务 harness 在 `POST /admin/sensitive/words` 新增敏感词时失败。
- 接口 HTTP 状态为 `200`，业务码为 `409`，提示“敏感词已存在或添加失败”。
- 线上 `sensitive_category` 表为空，`sensitive_word` 计数为 `0`。
- 后端日志显示新增时触发外键约束失败，原因是写入的 `category_id` 找不到对应分类。

判断：敏感词基础分类数据没有初始化，接口把底层外键失败包装成了模糊的 `409`。

建议：

1. 补齐敏感词分类种子数据，并确认默认分类 ID 与代码或前端表单一致。
2. 接口层把外键或分类缺失错误改成更明确的业务提示，例如“敏感词分类不存在”。
3. 修复后重跑敏感词风控业务 harness，继续覆盖新增、查询、启停、删除和预期错误路径。

## 证据与复核路径

本次测试产物保存在仓库工作目录的 `output/api-test/online-20260618-production/` 下：

| 文件 | 说明 |
| --- | --- |
| `openapi-online.json` | 从线上 OpenAPI 拉取的接口清单 |
| `runs__full-api__summary.json` | OpenAPI 全量扫描汇总 |
| `runs-admin-window__combined-summary.json` | 管理窗口内服务级业务 harness 汇总 |
| `runs__team-correctness__summary.json` | 团队模块业务正确性结果 |
| `runs-sensitive-rerun__sensitive-correctness__summary.json` | 敏感词失败链路复测结果 |
| `db-evidence.json` | 表结构、种子数据和账号状态核验证据 |

## 安全说明

- 测试报告和产物不保存服务器口令、登录 token、验证码、数据库密码或密码哈希。
- 为运行管理端业务 harness，测试期间只在受控窗口内临时恢复了可自动化登录的管理态，完成后已恢复原管理账号密码哈希，并验证默认测试口令无法登录。
- 线上数据写入类用例使用带时间戳的测试数据，并优先通过业务接口清理；失败问题保留为证据，不扩大修改范围。

## 后续复测清单

修复后建议按以下顺序复测：

1. `/api/actuator/health`。
2. `/api/v3/api-docs` 拉取和 OpenAPI 全量扫描。
3. 文件存储专项业务 harness。
4. 敏感词风控专项业务 harness。
5. 当前通过的服务级业务 harness 抽样回归。
