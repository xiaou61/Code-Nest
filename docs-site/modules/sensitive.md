# 敏感词风控

敏感词风控是内容安全的基础能力。社区帖子、动态、博客、聊天、评论等内容型模块，都可以在发布前调用它，判断文本是否允许通过、是否需要替换，或者是否应该拒绝。

## 功能入口

| 端 | 页面或接口 | 说明 |
| --- | --- | --- |
| 管理端 | `/sensitive/words` | 敏感词库 |
| 管理端 | `/sensitive/whitelist` | 白名单 |
| 管理端 | `/sensitive/strategy` | 处理策略 |
| 管理端 | `/sensitive/statistics` | 命中统计 |
| 管理端 | `/sensitive/source` | 词库来源和同步 |
| 管理端 | `/sensitive/version` | 版本历史和回滚记录 |
| 管理端 | `/sensitive/config` | 配置入口 |
| 后端模块 | `xiaou-sensitive`、`xiaou-sensitive-api` | 检测实现和跨模块 API |

## 推荐学习顺序

敏感词模块最好按“检测主链路 -> 词库治理 -> 运营闭环”的顺序学习。这样不会一开始就被导入、同步、版本这些后台能力绕晕。

1. 先看 `SensitiveCheckServiceImpl.checkText`，理解一段文本从输入到允许/拒绝的全过程。
2. 再看 `AhoCorasickEngine`，知道项目为什么不用循环 `contains` 匹配大量敏感词。
3. 接着看 `TextPreprocessor`，理解全角、符号、同音字、形似字这些绕过处理。
4. 然后看白名单和策略服务，理解“命中词”为什么不一定等于“拒绝发布”。
5. 最后看导入、来源同步和版本记录，这些是运营维护词库时才会频繁使用的能力。

如果只是业务模块要接入风控，先不用读完整后台页面。先会调用 `isAllowed` 或 `checkText`，并知道 `processedText` 什么时候要入库，就已经能完成 80% 的接入工作。

## 源码地图

| 位置 | 作用 |
| --- | --- |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/controller/api/SensitiveWordController.java` | 对外检测接口 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/service/impl/SensitiveCheckServiceImpl.java` | 检测主流程 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/engine/AhoCorasickEngine.java` | AC 自动机匹配引擎 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/engine/TextPreprocessor.java` | 文本预处理、变形词处理 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/service/impl/SensitiveWordServiceImpl.java` | 敏感词管理、导入、导出、版本记录 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/service/impl/SensitiveStrategyServiceImpl.java` | 策略缓存和默认策略 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/service/impl/SensitiveWhitelistServiceImpl.java` | 白名单缓存 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/service/impl/SensitiveSourceServiceImpl.java` | 本地/API/GitHub 词库同步 |
| `xiaou-sensitive/src/main/java/com/xiaou/sensitive/scheduler/SensitiveSourceSyncScheduler.java` | 定时同步来源 |

## 对外检测接口

| 接口 | 能力 |
| --- | --- |
| `POST /sensitive/check` | 单条文本检测 |
| `POST /sensitive/check/batch` | 批量文本检测 |

检测请求核心字段：

| 字段 | 说明 |
| --- | --- |
| `text` | 待检测文本 |
| `module` | 业务模块，如 `community`、`moment`、`blog` |
| `businessId` | 业务数据 ID |
| `userId` | 用户 ID，用于日志和违规统计 |

检测响应会告诉调用方：是否命中、命中了哪些词、处理后的文本、风险等级、动作，以及是否允许通过。

## 检测主流程

`SensitiveCheckServiceImpl.checkText` 可以按下面理解：

| 步骤 | 说明 |
| --- | --- |
| 1 | 空文本直接通过 |
| 2 | 文本超过 10000 字符时截断检测 |
| 3 | 使用 `TextPreprocessor` 做全角转半角、去特殊字符、转小写、同音字/形似字替换 |
| 4 | 原文本跑一次 AC 自动机 |
| 5 | 预处理后的文本再跑一次 AC 自动机 |
| 6 | 合并命中词 |
| 7 | 按模块和全局白名单过滤 |
| 8 | 根据命中数量计算风险等级 |
| 9 | 根据模块和风险等级取策略 |
| 10 | 执行 `replace`、`reject` 或 `none` |
| 11 | 异步记录命中日志、命中统计和用户违规统计 |

一句话概括：先“标准化文本”，再“快速匹配”，再“白名单排除”，最后“按策略处理”。

## AC 自动机怎么理解

`AhoCorasickEngine` 使用 Aho-Corasick 算法。它适合一次性匹配大量词，而不是对每个词都 `contains` 一遍。

项目里的保护参数：

| 参数 | 值 | 说明 |
| --- | --- | --- |
| `MAX_TEXT_LENGTH` | 10000 | 检测文本最大长度 |
| `MAX_PATTERN_COUNT` | 50000 | 敏感词数量提示阈值 |
| `MAX_PATTERN_LENGTH` | 100 | 单个敏感词最大长度 |
| failure 链遍历深度 | 10 | 避免异常词库导致性能问题 |

引擎初始化时会把有效词统一 `trim` 并转小写，然后构建 Trie 树和 failure 指针。词库刷新时直接重建引擎。

## 文本预处理

`TextPreprocessor` 处理绕过方式：

| 能力 | 示例思路 |
| --- | --- |
| 全角转半角 | `ＡＢＣ` 转成 `ABC` |
| 移除特殊字符 | 去掉空格、零宽字符和常见符号 |
| 转小写 | 英文字母统一小写 |
| 同音字替换 | 使用 `sensitive_homophone` 映射 |
| 形似字替换 | 使用 `sensitive_similar_char` 映射 |

这一步是为了处理“中间插符号”“全角字符”“形似字替换”等绕过行为。不是所有绕过都能完全识别，所以运营上仍要靠命中日志和词库迭代。

## 风险等级和动作

默认风险等级按命中词数量计算：

| 命中数量 | 风险等级 |
| --- | --- |
| 1 个 | `1` 低风险 |
| 2 到 3 个 | `2` 中风险 |
| 4 个及以上 | `3` 高风险 |

默认处理动作：

| 风险等级 | 默认动作 | 结果 |
| --- | --- | --- |
| 1 | `replace` | 敏感词替换为 `***`，允许通过 |
| 2 | `replace` | 替换后允许通过 |
| 3 | `reject` | 返回空文本，不允许通过 |

如果 `sensitive_strategy` 中配置了模块策略，会优先使用配置策略。策略缓存 key 是 `module:level`。

## 白名单

白名单用于降低误判。逻辑顺序是：

1. 如果传了 `module`，先查模块白名单。
2. 模块白名单未命中，再查全局白名单。
3. 命中的词从结果中移除。

白名单使用 Caffeine 本地缓存，最大 10000 条，5 分钟过期。新增、更新、删除或导入白名单后会刷新缓存。

## 词库管理

敏感词主表是 `sensitive_word`：

| 字段 | 说明 |
| --- | --- |
| `word` | 敏感词 |
| `word_type` | `1` 普通词、`2` 正则表达式 |
| `category_id` | 分类 |
| `level` | 风险等级 `1`、`2`、`3` |
| `action` | `1` 替换、`2` 拒绝、`3` 审核 |
| `enable_variant_check` | 是否启用变形词检测 |
| `status` | `0` 禁用、`1` 启用 |

导入前可以预览。预览会统计：

| 指标 | 含义 |
| --- | --- |
| 总行数 | 原始导入条数 |
| 有效词 | 通过空值和长度校验的词 |
| 文件内重复 | 导入文件自身重复 |
| 数据库重复 | 已存在的词 |
| 可导入 | 真正会插入的词 |
| 无效样本 | 空白、超长等样例 |

批量导入按 500 条一批写入。导入成功后刷新词库，并写入版本记录。

## 来源同步

词库来源表是 `sensitive_source`，支持：

| 来源类型 | 说明 |
| --- | --- |
| `local` | 本地来源，无需远程同步 |
| `api` | 从 API 拉取词库 |
| `github` | 支持 GitHub blob/raw 地址，必要时解析 Base64 content |

定时任务 `SensitiveSourceSyncScheduler` 默认每小时扫描一次到期来源。每个来源根据 `sync_interval` 判断是否需要同步，默认 24 小时。

同步保护：

| 机制 | 说明 |
| --- | --- |
| `AtomicBoolean running` | 避免上一轮未结束时重复执行 |
| 重试 | 默认最多重试 2 次 |
| 连续失败计数 | 达到阈值后可发告警 |
| `alert-webhook` | 配置后发送同步失败告警 |

同步成功后会新增词、重新启用被禁用的已有词，最后刷新检测引擎。

## 版本记录

`sensitive_version` 记录词库变更：

| changeType | 场景 |
| --- | --- |
| `add` | 新增敏感词 |
| `update` | 更新敏感词 |
| `delete` | 删除敏感词 |
| `import` | 批量导入 |
| `sync` | 来源同步 |
| `rollback` | 回滚记录 |

当前回滚服务会创建回滚版本记录，但不是完整的“按快照恢复词库”。如果要做真正回滚，需要保存词库快照或变更前后明细。

## 核心数据表

| 表 | 说明 |
| --- | --- |
| `sensitive_word` | 敏感词 |
| `sensitive_category` | 分类 |
| `sensitive_whitelist` | 白名单 |
| `sensitive_strategy` | 模块策略 |
| `sensitive_log` | 检测日志 |
| `sensitive_hit_statistics` | 命中统计 |
| `sensitive_user_violation` | 用户违规统计 |
| `sensitive_homophone` | 同音字映射 |
| `sensitive_similar_char` | 形似字映射 |
| `sensitive_source` | 词库来源 |
| `sensitive_version` | 版本记录 |

## 接入内容发布流程

推荐做法：

1. 发布前组装待检测文本，如标题、正文、评论内容。
2. 调用 `SensitiveCheckService.isAllowed(text, module, businessId, userId)`。
3. 如果 `allowed = false`，直接阻止发布并给用户清晰提示。
4. 如果动作是 `replace`，使用 `processedText` 入库。
5. 业务发布成功后，保留 `businessId`，方便日志追踪。

## 验证清单

| 验证点 | 怎么验证 | 预期结果 |
| --- | --- | --- |
| 普通文本通过 | 调用 `POST /sensitive/check`，传不含敏感词文本 | `allowed = true`，命中列表为空 |
| 低风险替换 | 加一个启用敏感词后检测单词命中内容 | 返回 `replace` 类处理结果，`processedText` 中命中词被替换 |
| 高风险拒绝 | 构造 4 个以上有效命中词 | 风险等级为高，默认动作应拒绝 |
| 变形词识别 | 使用全角、符号分隔、同音字或形似字样例 | 预处理后仍能命中对应词 |
| 模块白名单 | 给某个模块配置白名单，再用同词检测 | 该模块命中被过滤，其他模块仍按敏感词处理 |
| 批量检测 | 调用批量检测接口传多条文本 | 每条文本都有独立响应，超大批量要观察降级和耗时 |
| 导入预览 | 上传包含重复、空行、超长词的词库 | 预览能区分有效词、文件内重复、数据库重复和无效样本 |
| 来源同步 | 配置 API 或 GitHub 来源并触发同步 | 新词入库、旧词重新启用，失败时累计失败次数 |
| 日志统计 | 发布内容触发命中后查看统计 | 主流程不被异步日志影响，日志和命中统计最终写入 |

新增内容模块时，最小验证路径是：标题命中、正文命中、评论命中、白名单放行、拒绝提示。不要只测一个“正常发布”，否则上线后最容易漏掉异常路径。

## 常见坑

| 问题 | 原因 | 建议 |
| --- | --- | --- |
| 明明加了词却检测不到 | 词未启用，或词库没有刷新 | 调用刷新接口或检查 `status` |
| 某些词命中后又消失 | 被白名单过滤 | 查模块白名单和全局白名单 |
| 长文本只检测前半部分 | 超过 10000 字符被截断 | 长文发布可分段检测 |
| 来源同步失败很多次 | API 地址、GitHub 地址或 token 错误 | 用测试连接接口先验证 |
| 高风险内容没有进入人工审核 | 当前默认策略高风险是 `reject`，中风险也是 `replace` | 若需要审核流，要补业务审核状态 |
| 日志没有写入但发布被处理了 | 日志和统计是异步记录，失败不影响主流程 | 查线程池和应用日志 |
