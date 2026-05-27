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

---

## 敏感词风控模块深度拆解

> 以下内容基于 `xiaou-sensitive` + `xiaou-sensitive-api` 全部源码逐行拆解，覆盖 6 个 ServiceImpl、1 个 Engine 实现、1 个 Preprocessor、4 个 Domain、4 个 Mapper、1 个 API 接口 + 2 个 DTO。

### 一、检测主链路深度分析

**源码**：`SensitiveCheckServiceImpl.checkText`

```
checkText(request):
  1. request == null || text == null || text.trim().isEmpty → 直接返回 allowed=true
  2. text.length > 10000 → 截取前 10000 字符, 并修改 request.setText()
  3. textPreprocessor.preprocess(text, true, true, true, true)
     → 全角转半角 + 去特殊字符 + 转小写 + 同音字 + 形似字
  4. sensitiveEngine.findSensitiveWords(text)          → 原文匹配
  5. sensitiveEngine.findSensitiveWords(preprocessedText) → 预处理文匹配
  6. 合并: variantHitWords 中的词不在 hitWords → 追加
  7. 白名单过滤: 逐词检查 whitelistService.isInWhitelist(word, module)
  8. hitWords 非空 → calculateRiskLevel(hitWords.size())
  9. strategyService.getStrategy(module, riskLevel) → 缓存 → DB → 默认策略
  10. processText(text, action) → replace 用 *** 替换 / reject 返回空
  11. isAllowed(action) → action == "reject" 返回 false
  12. logSensitiveDetectionAsync + recordStatisticsAsync → 异步
```

**关键发现 1**：步骤 2 截断文本后，`request.setText(text)` 修改了入参对象。这意味着调用方如果后续还使用 request 对象，会拿到被截断后的文本。这是一个隐含的副作用。

**关键发现 2**：步骤 6 合并命中词时使用 `hitWords.contains(variantWord)` 做去重。`hitWords` 是 `ArrayList`，`contains` 是 O(n) 操作。如果命中词很多，这里的性能不佳。但因为命中词通常很少，实际影响不大。

**关键发现 3**：异常处理返回 `allowed=false`，`action="error"`。这意味着**检测服务自身异常时，内容发布会被阻断**。这违背了"风控不影响主流程"的设计原则——正常情况下即使检测服务不可用，也不应该阻止用户发布内容。

### 二、批量检测双模式策略

**源码**：`SensitiveCheckServiceImpl.checkTextBatch`

```
checkTextBatch(requests):
  1. requests.size() > 100 → 截取前 100 条
  2. requests.size() ≤ 10 → 串行逐条 checkText
  3. requests.size() > 10 → 并行处理:
     - batchExecutor.submit(() -> checkText(request)) 提交所有任务
     - future.get(10, TimeUnit.SECONDS) 逐条等待结果
     - TimeoutException → createDefaultResponse() + cancel
     - 整体异常 → 退回串行处理
```

**关键发现**：并行模式下的 `createDefaultResponse()` 返回 `hit=false, allowed=true`，与 `checkText` 异常时返回 `allowed=false` 不一致。同样是异常场景，单条检测阻断发布，批量检测却放行——行为不统一。

**线程池配置**：

| 参数 | 值 | 说明 |
| --- | --- | --- |
| 核心线程数 | 2 | 日常并发 |
| 最大线程数 | 8 | 高峰扩容 |
| 队列容量 | 200 | 缓冲任务 |
| KeepAlive | 60s | 扩容线程回收 |
| 拒绝策略 | CallerRunsPolicy | 队列满时由调用线程执行 |
| 线程名 | `SensitiveCheck-{n}` | 日志辨识 |
| Daemon | true | 不阻止 JVM 退出 |

### 三、AC 自动机实现详解

**源码**：`AhoCorasickEngine`

```
TrieNode:
  - children: Map<Character, TrieNode>  ← 每个节点一个 HashMap
  - failure: TrieNode                    ← 失败指针
  - isEndOfWord: boolean
  - pattern: String                      ← 只存储一个模式字符串

initialize(words):
  1. 过滤无效词: null, 空, 长度 > 100
  2. trim + toLowerCase
  3. 构建 Trie: 逐字符插入
  4. 构建失败指针: BFS, 步数限制 100, 循环引用保护

findSensitiveWords(text):
  1. lock.readLock().lock()
  2. text.toLowerCase()
  3. 逐字符遍历, 跟随 Trie + failure 指针
  4. failure 链遍历深度 ≤ 10
  5. 命中时添加 pattern 到结果集
  6. lock.readLock().unlock()

replaceSensitiveWords(text):
  1. findSensitiveWords 获取匹配位置列表
  2. 按 start 降序排序
  3. StringBuilder 从后往前替换为 "***"
```

**关键发现 1**：TrieNode 使用 `HashMap<Character, TrieNode>` 存储子节点。对于大量敏感词（万级以上），每个中间节点都分配一个 HashMap 对象，内存开销较大。生产环境如果词库达到 50000，可以考虑用数组或双数组 Trie 优化。

**关键发现 2**：`TrieNode.pattern` 只存储一个模式字符串，`setPattern` 会覆盖之前的值。如果两个敏感词共享路径（如"敏感"和"敏感词"），较短的那个的 pattern 会被覆盖。但 `isEndOfWord` 仍然为 true，`findSensitiveWords` 在遍历时会检查每个 `isEndOfWord` 节点的 pattern，所以如果 pattern 被覆盖，可能会漏掉较短的匹配词。

**关键发现 3**：失败指针构建使用 BFS，步数限制 100 和循环引用保护是防御性编程。正常情况下不会触发这些限制。

**线程安全**：使用 `ReentrantReadWriteLock`。`initialize` 获取写锁重建整个 Trie，`findSensitiveWords` 获取读锁。写锁会阻塞所有读操作，词库刷新期间检测不可用。

### 四、文本预处理器线程安全问题

**源码**：`TextPreprocessor`

```java
private Map<Character, String> homophoneMap = new HashMap<>();  // ← 普通 HashMap
private Map<Character, String> similarCharMap = new HashMap<>(); // ← 普通 HashMap

refreshMappings(homophones, similarChars):
  // 构建新 Map
  Map<Character, String> newHomophoneMap = new HashMap<>();
  Map<Character, String> newSimilarCharMap = new HashMap<>();
  // 填充映射...
  // 替换引用
  this.homophoneMap = newHomophoneMap;  // ← 非原子操作
  this.similarCharMap = newSimilarCharMap;
```

**关键发现**：`homophoneMap` 和 `similarCharMap` 使用普通 `HashMap`，不是 `ConcurrentHashMap`。`refreshMappings` 方法替换引用不是原子操作——在替换 `homophoneMap` 和 `similarCharMap` 之间存在时间窗口，此时 `preprocess` 方法可能读取到一个新 map 和一个旧 map 的混合状态。

更严重的是：如果 `preprocess` 正在遍历旧 map 的 entrySet，而 `refreshMappings` 替换了引用，由于旧 map 对象仍然存在，不会被 GC，所以不会抛 `ConcurrentModificationException`。但 `preprocess` 在两次替换之间可能使用新的 homophoneMap + 旧的 similarCharMap，造成预处理结果不一致。

### 五、策略缓存与默认策略

**源码**：`SensitiveStrategyServiceImpl`

```
策略缓存: ConcurrentHashMap<"module:level", SensitiveStrategy>
无 TTL、无容量上限

getStrategy(module, level):
  1. 缓存命中 → 返回
  2. 缓存未命中 → DB 查询 selectByModuleAndLevel
  3. DB 未找到 → getDefaultStrategy(level)
  4. 放入缓存

refreshCache():
  1. strategyCache.clear()      ← 清空所有缓存
  2. selectEnabledStrategies()   ← 重新加载
  3. 逐条 put

默认策略:
  level=1: replace, notifyAdmin=0, limitUser=0
  level=2: replace, notifyAdmin=1, limitUser=0
  level=3: reject, notifyAdmin=1, limitUser=1, limitDuration=60分钟
```

**关键发现 1**：`refreshCache()` 先 `clear()` 再逐条 `put`，存在**缓存雪崩窗口**——在 clear 和加载完成之间，所有策略请求都会穿透到数据库。如果此时 DB 压力大，可能导致雪崩。

**关键发现 2**：默认策略中 `level=3` 的 `limitUser=1, limitDuration=60` 表示"限制用户 60 分钟"。但 `checkText` 中并没有检查用户是否被限制——这个策略字段只在统计服务中被设置（`isRestricted`），但从未在检测流程中被查询和执行。

### 六、白名单缓存策略

**源码**：`SensitiveWhitelistServiceImpl`

```
缓存: Caffeine, max 10000, TTL 5min

isInWhitelist(word, module):
  cacheKey = "whitelist:module:{module}:{word}" 或 "whitelist:global:{word}"
  cache.get(cacheKey, key → {
    module 非空 → 先查模块白名单 existsInWhitelist(word, "module", module)
    → 再查全局白名单 existsInWhitelist(word, "global", null)
  })

refreshCache():
  1. cache.invalidateAll()
  2. selectEnabledWords() → 只预加载全局白名单
  3. 预加载 key 格式: "whitelist:global:{word}" (module=null)
```

**关键发现**：`refreshCache` 只预加载了 `module=null` 的全局白名单条目（key 格式 `whitelist:global:{word}`），模块白名单（key 格式 `whitelist:module:{module}:{word}`）完全依赖 Caffeine 的 loading cache 机制按需加载。这意味着刷新缓存后，模块白名单会全部穿透到数据库，直到被 Caffeine 逐条加载。

### 七、统计总览的估算数字

**源码**：`SensitiveStatisticsServiceImpl.getOverview`

```java
return StatisticsOverviewVO.builder()
    .totalCheck(hitCount * 10)    // 简化估算: 总检测数 = 命中数 × 10
    .hitCount(hitCount)
    .hitRate(hitCount / 100.0)    // 简化计算
    .rejectCount(hitCount / 3)    // 简化估算: 拒绝数 = 命中数 / 3
    .replaceCount(hitCount * 2 / 3) // 简化估算: 替换数 = 命中数 × 2/3
    .todayNewWords(0)             // 需要额外查询
    .violationUserCount(violationUserCount)
    .build();
```

**关键发现**：`totalCheck`、`rejectCount`、`replaceCount` 三个字段是**基于 hitCount 的数学估算**，不是真实统计值。总检测数假设"命中率 10%"，拒绝和替换按"1:2"比例分配。这些数字在管理端展示时看起来像真实数据，但实际是公式推算。如果运营依赖这些指标做决策，会得到不准确的结论。

`todayNewWords` 直接硬编码为 0，注释说"需要额外查询"但未实现。

### 八、用户违规限制：写而不查

**源码**：`SensitiveStatisticsServiceImpl.recordUserViolation`

```
recordUserViolation(userId):
  @Async
  查询用户今日违规记录
  → 不存在: INSERT, violationCount=1, isRestricted=0
  → 已存在: UPDATE, violationCount+1
     → 如果 violationCount >= 5 且 isRestricted=0:
       → isRestricted=1, restrictEndTime=NOW()+1小时
       → UPDATE
```

**关键发现**：违规限制逻辑只在"记录"阶段执行——当用户累计 5 次违规时设置 `isRestricted=1`。但 `checkText` 方法中**从未查询** `isRestricted` 状态。也就是说，被限制的用户仍然可以正常发布内容，限制功能形同虚设。

此外，`recordUserViolation` 标注了 `@Async`，这意味着违规计数和限制判定是异步执行的。在高频发布场景下，用户可能在 5 次违规被记录之前就已经发布了更多内容。

### 九、来源同步的事务与网络问题

**源码**：`SensitiveSourceServiceImpl.syncSource`

```
syncSource(sourceId):
  @Transactional(rollbackFor = Exception.class)
  1. 获取来源配置
  2. HTTP 请求外部 API 或 GitHub          ← 网络调用在事务内
  3. 解析内容（JSON 递归提取或逐行解析）
  4. Base64 解码（GitHub blob 响应）
  5. 新增词 / 重新启用已有词, 500/批
  6. 更新来源同步状态
  7. 记录版本
```

**关键发现 1**：`syncSource` 标注了 `@Transactional`，但步骤 2-4 包含 HTTP 网络调用。如果外部 API 响应慢（几十秒），事务会一直持有数据库连接，导致连接池耗尽。正确做法是先在事务外完成网络请求，再开启事务写入数据。

**关键发现 2**：`apiKey` 字段以明文存储在 `sensitive_source` 表中。如果数据库被拖库，攻击者可以获得所有外部 API 的密钥。

### 十、未使用字段分析

`SensitiveWord` 实体中有三个字段在检测流程中未被使用：

| 字段 | 数据库定义 | 实际用途 |
| --- | --- | --- |
| `word_type` | `1` 普通词、`2` 正则表达式 | AC 自动机只加载 `selectEnabledWords()` 返回的词字符串，不区分 `word_type=2` 的正则模式 |
| `enable_variant_check` | 是否启用变形词检测 | 从未在 `checkText` 中读取，变形词检测对所有词一视同仁 |
| `pinyin` | 拼音 | 从未在检测流程中用于拼音匹配 |

这些字段在管理端可以编辑，数据库中有值，但不影响检测行为。运营如果配置了 `word_type=2` 的正则词或关闭了某个词的变形词检测，预期行为不会生效。

### 十一、深度发现与坑点

#### 11.1 已确认的代码问题

| 编号 | 问题 | 位置 | 影响 |
| --- | --- | --- | --- |
| BUG-1 | 检测异常返回 `allowed=false` | `SensitiveCheckServiceImpl.checkText:201-211` | 检测服务异常阻断内容发布 |
| BUG-2 | 批量检测异常返回 `allowed=true` | `SensitiveCheckServiceImpl.createDefaultResponse` | 与单条检测异常行为不一致 |
| BUG-3 | 白名单缓存只预加载全局条目 | `SensitiveWhitelistServiceImpl.refreshCache` | 刷新后模块白名单穿透到 DB |
| BUG-4 | 统计总览使用估算数字 | `SensitiveStatisticsServiceImpl.getOverview:115-119` | totalCheck/rejectCount/replaceCount 不可信 |
| BUG-5 | 用户违规限制不执行 | `SensitiveCheckServiceImpl.checkText` | `isRestricted` 设置了但从未被检查 |
| BUG-6 | `word_type=2` 正则词不生效 | `AhoCorasickEngine.initialize` | 正则模式词被当作普通词处理 |
| BUG-7 | `enableVariantCheck` 字段未使用 | `SensitiveCheckServiceImpl.checkText` | 关闭变形词检测不生效 |
| BUG-8 | `TrieNode.pattern` 只存一个模式 | `AhoCorasickEngine.TrieNode` | 共享路径的短词可能被覆盖 |

#### 11.2 设计层面的潜在风险

| 编号 | 风险 | 说明 |
| --- | --- | --- |
| RISK-1 | TextPreprocessor 使用普通 HashMap | 同音字/形似字映射非线程安全，刷新时可能读取不一致状态 |
| RISK-2 | 策略缓存 clear-then-load 雪崩窗口 | `refreshCache` 清空后重新加载期间，策略请求全部穿透到 DB |
| RISK-3 | syncSource 事务内包含 HTTP 调用 | 外部 API 响应慢时事务长时间持有数据库连接 |
| RISK-4 | apiKey 明文存储 | 数据库拖库后密钥泄露 |
| RISK-5 | TrieNode 使用 HashMap 存子节点 | 词库万级以上时内存开销大 |
| RISK-6 | 违规计数异步执行 | 高频发布场景下限制判定滞后于实际违规 |
| RISK-7 | 检测服务单点 | AC 引擎是 Spring 单例 Bean，读锁并发由 ReentrantReadWriteLock 控制，写锁重建时检测完全不可用 |

#### 11.3 架构设计亮点

| 编号 | 亮点 | 说明 |
| --- | --- | --- |
| H-1 | AC 自动机多模式匹配 | 一次遍历匹配所有敏感词，O(n) 复杂度 |
| H-2 | 双轮检测（原文 + 预处理文） | 全角、符号、同音字、形似字绕过都能识别 |
| H-3 | ReentrantReadWriteLock 保护引擎刷新 | 读不阻塞，写互斥，保证线程安全 |
| H-4 | 白名单模块 + 全局分层 | 先查模块白名单再查全局，支持差异化放行 |
| H-5 | 批量检测双模式 | 小批量串行避免线程开销，大批量并行提高吞吐 |
| H-6 | CallerRunsPolicy 拒绝策略 | 队列满时由调用线程执行，保证检测不丢失 |
| H-7 | 来源同步多通道支持 | 本地 / API / GitHub 三种来源，GitHub 支持 Base64 解码 |
| H-8 | 性能保护参数齐全 | 文本长度、批量大小、处理超时、failure 链深度等限制完善 |

#### 11.4 源码导航速查

| 想了解 | 读什么 |
| --- | --- |
| 检测主链路 | `SensitiveCheckServiceImpl.java` — checkText 11 步 + checkTextBatch 双模式 |
| AC 自动机 | `AhoCorasickEngine.java` — Trie 构建 + failure 指针 BFS + 读写锁 |
| 文本预处理 | `TextPreprocessor.java` — 全角转换 + 同音字/形似字 HashMap |
| 策略缓存 | `SensitiveStrategyServiceImpl.java` — ConcurrentHashMap + 默认策略 + clear-then-load |
| 白名单缓存 | `SensitiveWhitelistServiceImpl.java` — Caffeine loading cache + 模块/全局分层 |
| 词库管理 | `SensitiveWordServiceImpl.java` — 导入预览 + 500/批 + 版本记录 |
| 来源同步 | `SensitiveSourceServiceImpl.java` — @Transactional + HTTP + Base64 + GitHub |
| 统计服务 | `SensitiveStatisticsServiceImpl.java` — 估算数字 + 违规限制 + CSV 导出 |
| 跨模块 API | `SensitiveCheckService.java` (xiaou-sensitive-api) — 检测接口定义 |

## 相关模块

| 模块 | 关系 | 说明 |
| --- | --- | --- |
| [公共底座](/modules/common) | 强依赖 | 敏感词模块依赖公共底座的并发工具、Redis 和异常处理 |
| [鉴权与用户体系](/modules/auth) | 强依赖 | 管理端词库管理需要管理员权限 |
| [社区帖子](/modules/community) | 被依赖 | 发帖和评论必须经过敏感词检测 |
| [动态广场](/modules/moments) | 被依赖 | 动态发布需要敏感词检测 |
| [博客](/modules/blog) | 被依赖 | 博客文章发布需要敏感词检测 |
| [IM 聊天室](/modules/chat) | 被依赖 | 聊天消息需要敏感词检测 |
| [系统运营后台](/modules/system-ops) | 被依赖 | 敏感词管理界面在管理端 |
