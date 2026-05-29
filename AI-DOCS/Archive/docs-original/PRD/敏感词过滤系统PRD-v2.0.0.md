# 敏感词过滤系统优化 PRD v2.0.0

## 一、项目背景

### 1.1 当前问题分析

通过对现有敏感词系统（v1.0.0）的深度分析和线上运行情况的评估，发现以下核心问题：

#### 1.1.1 检测能力不足
- **变形词绕过**：用户通过插入特殊字符（如"敏@感#词"）、使用拼音（"mingan"）、谐音字等方式轻易绕过检测
- **语义理解缺失**：无法识别委婉表达、隐喻等隐性敏感内容
- **组合词检测**：无法检测分散在文本中的组合型敏感词
- **上下文误判**：相同词汇在不同语境下含义不同，导致误判

#### 1.1.2 管理功能薄弱
- **等级策略单一**：虽然定义了低/中/高风险等级，但所有等级词采用统一的处理策略
- **缺乏白名单**：正常词汇被误判后无法加入白名单
- **词库来源单一**：完全依赖手动维护，无法接入第三方权威词库

#### 1.1.3 分析能力欠缺
- **无统计报表**：缺少敏感词命中率统计、热词分析、趋势分析
- **无用户画像**：无法识别频繁违规用户
- **无业务分析**：不知道哪个业务模块敏感词问题最严重

#### 1.1.4 功能覆盖不全
- **仅支持文本**：无法检测图片、视频中的敏感内容
- **单语言支持**：仅支持中文，无法处理英文、繁体等

#### 1.1.5 性能优化空间
- **冷启动慢**：词库大时初始化时间长
- **内存占用高**：AC自动机节点过多时占用内存较大
- **缓存机制缺失**：相同文本重复检测浪费资源

### 1.2 企业级解决方案参考

参考阿里云、腾讯云、百度AI等企业级方案，归纳出以下核心能力：

1. **多策略检测**：DFA算法+AC自动机+正则表达式+NLP语义分析
2. **变形词处理**：拼音匹配、全角半角转换、特殊字符过滤、谐音检测
3. **智能学习**：机器学习模型自动识别新型敏感词
4. **分级管控**：不同等级采用不同策略（替换/拒绝/警告）
5. **多维统计**：命中率、误判率、趋势分析、用户画像
6. **多模态检测**：文本+图片OCR+视频抽帧
7. **动态更新**：词库热更新、无需重启服务

### 1.3 优化目标

| 指标 | v1.0.0现状 | v2.0.0目标 | 提升幅度 |
|------|-----------|-----------|---------|
| 准确率 | 70-80% | 95%+ | 提升15-25% |
| 召回率 | 60-70% | 90%+ | 提升20-30% |
| 误判率 | 10-15% | <3% | 降低70-80% |
| 检测性能 | 50-100ms | <30ms | 提升50%+ |
| 绕过率 | 30-40% | <5% | 降低85%+ |

---

## 二、核心功能优化

### 2.1 变形词检测（新增）★★★★★

#### 2.1.1 特殊字符过滤
```
原文：敏@感#词$汇
处理：敏感词汇
策略：移除所有非字母数字汉字字符
```

**实现方案**：
- 定义特殊字符集：`!@#$%^&*()_+-=[]{}|;:'",.<>?/~`、空格、零宽字符等
- 预处理文本：检测前先过滤特殊字符
- 支持自定义：管理员可配置哪些字符需要过滤

#### 2.1.2 拼音检测
```
原文：mingan词汇、敏感cihu
检测：命中"敏感词汇"
```

**实现方案**：
- 引入拼音库（如pinyin4j）
- 敏感词同时存储汉字+拼音形式
- 文本检测时转换为拼音并匹配
- 支持全拼、简拼、混合模式

#### 2.1.3 全角半角转换
```
原文：ＡＢＣ１２３
转换：ABC123
```

#### 2.1.4 同音字/形似字检测
```
同音字：傻B → 沙比、傻逼
形似字：草泥马 → 艹泥马
```

**实现方案**：
- 维护同音字映射表（数据库表：`sensitive_homophone`）
- 维护形似字映射表（数据库表：`sensitive_similar_char`）
- 检测时对原文进行同音字/形似字替换后再匹配

#### 2.1.5 正则表达式支持
```
示例：\d{11} → 匹配手机号
      .*(微信|VX|V信).* → 匹配联系方式
```

**实现方案**：
- 敏感词表新增字段：`word_type`（1-普通词 2-正则表达式）
- 正则词单独存储，单独匹配
- 限制正则复杂度，防止ReDOS攻击

---

### 2.2 白名单机制（新增）★★★★☆

#### 2.2.1 功能说明
- **目的**：解决正常词汇被误判的问题
- **优先级**：白名单优先级高于敏感词
- **应用场景**：
  - 行业专业术语（如"杀毒软件"的"杀"字）
  - 正常成语、诗词
  - 人名、地名
  - 品牌名称

#### 2.2.2 数据库设计
```sql
CREATE TABLE sensitive_whitelist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(255) NOT NULL COMMENT '白名单词汇',
    category VARCHAR(50) COMMENT '分类（专业术语/成语/人名/品牌等）',
    reason VARCHAR(500) COMMENT '加入白名单的原因',
    scope VARCHAR(50) DEFAULT 'global' COMMENT '作用范围（global-全局/module-模块级）',
    module_name VARCHAR(50) COMMENT '模块名称（scope=module时有效）',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    creator_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word_scope (word, scope, module_name),
    INDEX idx_status (status)
) COMMENT='敏感词白名单表';
```

#### 2.2.3 接口设计
```java
// 管理后台接口
POST /admin/sensitive/whitelist/add        // 新增白名单
POST /admin/sensitive/whitelist/update     // 更新白名单
POST /admin/sensitive/whitelist/delete/{id} // 删除白名单
POST /admin/sensitive/whitelist/list       // 分页查询白名单
POST /admin/sensitive/whitelist/batch/import // 批量导入
```

---

### 2.3 智能分级管控（优化）★★★★☆

#### 2.3.1 敏感词等级细化

**v1.0.0问题**：虽然定义了低/中/高风险等级，但处理策略与词本身强绑定。

**v2.0.0优化**：词的等级与处理策略解耦，支持按业务场景动态配置策略。

#### 2.3.2 策略配置表
```sql
CREATE TABLE sensitive_strategy (
    id INT PRIMARY KEY AUTO_INCREMENT,
    strategy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
    module VARCHAR(50) NOT NULL COMMENT '业务模块（community/interview/moment等）',
    level TINYINT NOT NULL COMMENT '风险等级 1-低 2-中 3-高',
    action VARCHAR(20) NOT NULL COMMENT '处理动作（replace/reject/warn）',
    notify_admin TINYINT DEFAULT 0 COMMENT '是否通知管理员 0-否 1-是',
    limit_user TINYINT DEFAULT 0 COMMENT '是否限制用户 0-否 1-是',
    limit_duration INT COMMENT '限制时长（分钟）',
    description VARCHAR(500) COMMENT '策略描述',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_level (module, level),
    INDEX idx_status (status)
) COMMENT='敏感词处理策略表';
```

**默认策略**：
| 模块 | 等级 | 动作 | 通知管理员 | 限制用户 |
|------|------|------|-----------|---------|
| community | 低 | replace | 否 | 否 |
| community | 中 | replace | 是 | 否 |
| community | 高 | reject | 是 | 是（禁言1小时）|
| interview | 低 | replace | 否 | 否 |
| interview | 中 | replace | 是 | 否 |
| interview | 高 | reject | 是 | 是（禁言24小时）|

#### 2.3.3 接口设计
```java
POST /admin/sensitive/strategy/list         // 查询策略列表
POST /admin/sensitive/strategy/update       // 更新策略配置
POST /admin/sensitive/strategy/reset/{id}   // 重置为默认策略
```

---

### 2.4 词库管理增强（优化）★★★★☆

#### 2.4.1 第三方词库接入（新增）

**支持词库来源**：
- 本地词库（现有方式）
- 第三方API词库（阿里云、百度AI等）
- 开源词库导入（GitHub敏感词词库）
- 定时同步（每日自动同步）

```sql
CREATE TABLE sensitive_source (
    id INT PRIMARY KEY AUTO_INCREMENT,
    source_name VARCHAR(100) NOT NULL COMMENT '词库来源名称',
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型（local/api/github）',
    api_url VARCHAR(500) COMMENT 'API地址',
    api_key VARCHAR(255) COMMENT 'API密钥',
    sync_interval INT DEFAULT 24 COMMENT '同步间隔（小时）',
    last_sync_time DATETIME COMMENT '最后同步时间',
    sync_status TINYINT COMMENT '同步状态 0-失败 1-成功',
    word_count INT DEFAULT 0 COMMENT '词汇数量',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) COMMENT='敏感词来源管理表';
```

#### 2.4.2 词库版本管理（新增）

**目的**：记录词库变更历史，支持回滚。

```sql
CREATE TABLE sensitive_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_no VARCHAR(50) NOT NULL COMMENT '版本号（如v1.0.1）',
    change_type VARCHAR(20) COMMENT '变更类型（add/update/delete/import）',
    change_count INT COMMENT '变更数量',
    change_detail JSON COMMENT '变更详情',
    operator_id BIGINT COMMENT '操作人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500) COMMENT '备注',
    INDEX idx_create_time (create_time)
) COMMENT='敏感词版本历史表';
```

#### 2.4.3 批量导入增强

**v1.0.0问题**：只支持TXT文件，每行一个词，功能单一。

**v2.0.0增强**：
- 支持多格式：TXT、CSV、Excel
- CSV/Excel支持多列：词汇、分类、等级、动作、备注
- 导入预览：导入前预览数据，确认后再导入
- 导入验证：检查格式、去重、过滤无效词
- 导入报告：详细的导入结果报告

**CSV格式示例**：
```csv
word,category,level,action,remark
敏感词1,政治敏感,3,reject,高风险词汇
敏感词2,广告推广,1,replace,低风险词汇
```

---

### 2.5 统计分析功能（新增）★★★★☆

#### 2.5.1 命中统计表
```sql
CREATE TABLE sensitive_hit_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL COMMENT '统计日期',
    word VARCHAR(255) NOT NULL COMMENT '敏感词',
    category_id INT COMMENT '分类ID',
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    module VARCHAR(50) COMMENT '业务模块',
    UNIQUE KEY uk_date_word_module (stat_date, word, module),
    INDEX idx_date (stat_date),
    INDEX idx_hit_count (hit_count DESC)
) COMMENT='敏感词命中统计表';
```

#### 2.5.2 用户违规统计表
```sql
CREATE TABLE sensitive_user_violation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    violation_count INT DEFAULT 0 COMMENT '违规次数',
    last_violation_time DATETIME COMMENT '最后违规时间',
    is_restricted TINYINT DEFAULT 0 COMMENT '是否被限制 0-否 1-是',
    restrict_end_time DATETIME COMMENT '限制结束时间',
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_violation_count (violation_count DESC),
    INDEX idx_user_id (user_id)
) COMMENT='用户违规统计表';
```

#### 2.5.3 统计报表接口
```java
POST /admin/sensitive/statistics/hit-trend        // 命中趋势（折线图）
POST /admin/sensitive/statistics/hot-words        // 热门敏感词（TOP 20）
POST /admin/sensitive/statistics/category-dist    // 分类分布（饼图）
POST /admin/sensitive/statistics/module-dist      // 模块分布（柱状图）
POST /admin/sensitive/statistics/violation-users  // 违规用户列表
POST /admin/sensitive/statistics/accuracy         // 准确率统计（误判率、拦截率等）
```

---

### 2.6 性能优化（优化）★★★★☆

#### 2.6.1 缓存机制（新增）

**策略**：
- **词库缓存**：词库加载后缓存到内存，避免频繁查询数据库
- **结果缓存**：相同文本检测结果缓存（Redis，过期时间5分钟）
- **白名单缓存**：白名单列表缓存到本地内存
- **策略缓存**：处理策略配置缓存

**缓存Key设计**：
```
sensitive:wordlib:{version}           # 词库缓存
sensitive:check:{md5(text)}           # 检测结果缓存
sensitive:whitelist:all               # 白名单缓存
sensitive:strategy:{module}:{level}   # 策略缓存
```

#### 2.6.2 异步处理（优化）

**v1.0.0现状**：日志记录是异步的，但检测是同步的。

**v2.0.0优化**：
- 检测仍保持同步（业务需要实时结果）
- 统计数据更新异步化（使用消息队列）
- 词库刷新异步化（后台任务）
- 第三方词库同步异步化

#### 2.6.3 分布式支持（新增）

**场景**：多节点部署时，词库更新需要通知所有节点刷新。

**实现方案**：
- 使用Redis Pub/Sub或消息队列
- 节点A更新词库 → 发布刷新消息 → 所有节点订阅并刷新词库

---

### 2.7 多模态检测（规划）★★★☆☆

#### 2.7.1 图片OCR检测（v2.1.0规划）

**流程**：
```
上传图片 → OCR识别文字 → 敏感词检测 → 返回结果
```

**技术方案**：
- 集成第三方OCR服务（百度OCR、腾讯OCR、阿里OCR）
- 提取图片中的文字
- 对文字进行敏感词检测
- 支持中文、英文、数字

#### 2.7.2 视频检测（v2.2.0规划）

**流程**：
```
上传视频 → 抽帧 → OCR识别 → 敏感词检测
         → 语音转文字 → 敏感词检测
```

---

## 三、数据库设计

### 3.1 新增表

#### 3.1.1 白名单表（sensitive_whitelist）
见 2.2.2 节

#### 3.1.2 策略配置表（sensitive_strategy）
见 2.3.2 节

#### 3.1.3 词库来源表（sensitive_source）
见 2.4.1 节

#### 3.1.4 版本历史表（sensitive_version）
见 2.4.2 节

#### 3.1.5 同音字映射表（sensitive_homophone）
```sql
CREATE TABLE sensitive_homophone (
    id INT PRIMARY KEY AUTO_INCREMENT,
    original_char VARCHAR(10) NOT NULL COMMENT '原始字符',
    homophone_chars VARCHAR(100) NOT NULL COMMENT '同音字（逗号分隔）',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_original (original_char)
) COMMENT='同音字映射表';

-- 初始化数据示例
INSERT INTO sensitive_homophone (original_char, homophone_chars) VALUES
('傻', '沙,煞,啥'),
('逼', '比,逼,币,鄙'),
('操', '草,曹,槽');
```

#### 3.1.6 形似字映射表（sensitive_similar_char）
```sql
CREATE TABLE sensitive_similar_char (
    id INT PRIMARY KEY AUTO_INCREMENT,
    original_char VARCHAR(10) NOT NULL COMMENT '原始字符',
    similar_chars VARCHAR(100) NOT NULL COMMENT '形似字（逗号分隔）',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_original (original_char)
) COMMENT='形似字映射表';

-- 初始化数据示例
INSERT INTO sensitive_similar_char (original_char, similar_chars) VALUES
('草', '艹,屮'),
('日', '曰,囗');
```

#### 3.1.7 命中统计表（sensitive_hit_statistics）
见 2.5.1 节

#### 3.1.8 用户违规统计表（sensitive_user_violation）
见 2.5.2 节

### 3.2 表结构优化

#### 3.2.1 敏感词表（sensitive_word）优化
```sql
ALTER TABLE sensitive_word
ADD COLUMN word_type TINYINT DEFAULT 1 COMMENT '词类型 1-普通词 2-正则表达式' AFTER word,
ADD COLUMN pinyin VARCHAR(500) COMMENT '拼音（用于拼音检测）' AFTER word,
ADD COLUMN enable_variant_check TINYINT DEFAULT 1 COMMENT '启用变形词检测 0-否 1-是' AFTER action,
ADD COLUMN remark VARCHAR(500) COMMENT '备注' AFTER creator_id,
ADD INDEX idx_word_type (word_type),
ADD INDEX idx_pinyin (pinyin);
```

---

## 四、接口设计

### 4.1 检测接口（API）- 保持兼容

```java
// v1.0.0接口保持不变，确保兼容性
POST /sensitive/check                 // 单个文本检测
POST /sensitive/check/batch           // 批量文本检测

// v2.0.0新增接口
POST /sensitive/check/advanced        // 高级检测（支持更多参数）
{
    "text": "要检测的文本",
    "module": "community",
    "checkVariant": true,            // 是否检查变形词（默认true）
    "checkPinyin": true,             // 是否检查拼音（默认true）
    "useWhitelist": true,            // 是否使用白名单（默认true）
    "returnDetails": true,           // 是否返回详细信息（默认false）
    "userId": 123,
    "businessId": 456
}

// 响应
{
    "code": 200,
    "data": {
        "hit": true,
        "hitWords": ["敏感词1", "敏感词2"],
        "processedText": "处理后文本",
        "riskLevel": 2,
        "action": "replace",
        "allowed": true,
        "details": [                      // 详细信息（returnDetails=true时返回）
            {
                "word": "敏感词1",
                "position": 5,
                "category": "政治敏感",
                "level": 3,
                "matchType": "exact"      // 匹配类型（exact-精确/variant-变形/pinyin-拼音）
            }
        ]
    }
}
```

### 4.2 白名单管理接口（Admin）

```java
POST /admin/sensitive/whitelist/add
POST /admin/sensitive/whitelist/update
POST /admin/sensitive/whitelist/delete/{id}
POST /admin/sensitive/whitelist/delete/batch
POST /admin/sensitive/whitelist/list
POST /admin/sensitive/whitelist/import
POST /admin/sensitive/whitelist/export
```

### 4.3 策略管理接口（Admin）

```java
POST /admin/sensitive/strategy/list
POST /admin/sensitive/strategy/update
POST /admin/sensitive/strategy/reset/{id}
POST /admin/sensitive/strategy/export
```

### 4.4 统计分析接口（Admin）

```java
POST /admin/sensitive/statistics/overview          // 总览（今日命中/拦截率/误判率等）
POST /admin/sensitive/statistics/hit-trend         // 命中趋势
POST /admin/sensitive/statistics/hot-words         // 热门敏感词
POST /admin/sensitive/statistics/category-dist     // 分类分布
POST /admin/sensitive/statistics/module-dist       // 模块分布
POST /admin/sensitive/statistics/violation-users   // 违规用户
POST /admin/sensitive/statistics/accuracy          // 准确率统计
POST /admin/sensitive/statistics/export            // 导出统计报表
```

### 4.5 词库管理接口增强（Admin）

```java
// v1.0.0原有接口保持不变
POST /admin/sensitive/words/list
POST /admin/sensitive/words
POST /admin/sensitive/words/update
POST /admin/sensitive/words/delete/{id}
POST /admin/sensitive/words/delete/batch
POST /admin/sensitive/words/import
POST /admin/sensitive/refresh

// v2.0.0新增接口
POST /admin/sensitive/words/export                 // 导出词库
POST /admin/sensitive/words/preview-import         // 预览导入数据
POST /admin/sensitive/words/confirm-import         // 确认导入
POST /admin/sensitive/words/version/list           // 版本历史
POST /admin/sensitive/words/version/rollback/{id}  // 回滚到指定版本

// 词库来源管理
POST /admin/sensitive/source/list
POST /admin/sensitive/source/add
POST /admin/sensitive/source/update
POST /admin/sensitive/source/delete/{id}
POST /admin/sensitive/source/sync/{id}              // 手动同步
POST /admin/sensitive/source/test-connection/{id}   // 测试连接
```

### 4.6 配置管理接口（Admin）

```java
// 同音字管理
POST /admin/sensitive/homophone/list
POST /admin/sensitive/homophone/add
POST /admin/sensitive/homophone/update
POST /admin/sensitive/homophone/delete/{id}
POST /admin/sensitive/homophone/import

// 形似字管理
POST /admin/sensitive/similar/list
POST /admin/sensitive/similar/add
POST /admin/sensitive/similar/update
POST /admin/sensitive/similar/delete/{id}
POST /admin/sensitive/similar/import
```

---

## 五、前端界面设计

### 5.1 页面结构

```
敏感词管理（/sensitive）
├─ 词库管理（/sensitive/words）           - v1.0.0已有，需优化
├─ 白名单管理（/sensitive/whitelist）      - v2.0.0新增
├─ 策略配置（/sensitive/strategy）         - v2.0.0新增
├─ 统计分析（/sensitive/statistics）       - v2.0.0新增
│  ├─ 数据总览（dashboard）
│  ├─ 命中趋势（trend）
│  ├─ 热词分析（hotwords）
│  └─ 违规用户（users）
├─ 词库来源（/sensitive/source）           - v2.0.0新增
└─ 配置管理（/sensitive/config）           - v2.0.0新增
   ├─ 同音字管理
   └─ 形似字管理
```

### 5.2 词库管理页面优化

#### 5.2.1 新增功能
- **类型标签**：显示词类型（普通词/正则表达式）
- **变形检测开关**：每个词可以单独控制是否启用变形检测
- **拼音显示**：列表显示拼音，方便查看
- **批量编辑**：批量修改分类、等级、动作
- **导入预览**：导入前预览数据，支持编辑后再导入
- **版本历史**：查看词库变更历史，支持回滚

#### 5.2.2 页面布局
```
┌─────────────────────────────────────────────────────┐
│ 搜索栏                                               │
│ [敏感词] [分类▼] [等级▼] [状态▼] [类型▼] [搜索]     │
├─────────────────────────────────────────────────────┤
│ 操作栏                                               │
│ [+新增] [批量删除] [导入▼] [导出] [刷新词库] [版本历史]│
├─────────────────────────────────────────────────────┤
│ 数据表格                                             │
│ ┌──┬─────┬────┬────┬────┬────┬────┬────┬────────┐ │
│ │☑│敏感词│类型│拼音│分类│等级│动作│状态│操作    │ │
│ ├──┼─────┼────┼────┼────┼────┼────┼────┼────────┤ │
│ │☑│词1  │普通│pin │政治│高  │拒绝│启用│编辑|删除│ │
│ │☑│^w+$ │正则│-   │广告│低  │替换│启用│编辑|删除│ │
│ └──┴─────┴────┴────┴────┴────┴────┴────┴────────┘ │
├─────────────────────────────────────────────────────┤
│ 分页                                                 │
│ 共100条 [<] 1 2 3 4 5 [>] 每页[10▼]条               │
└─────────────────────────────────────────────────────┘
```

### 5.3 白名单管理页面（新增）

**路径**：`/sensitive/whitelist`
**文件**：`vue3-admin-front/src/views/sensitive/whitelist/index.vue`

#### 5.3.1 功能
- 新增/编辑白名单
- 批量删除
- 批量导入
- 分类筛选
- 作用范围筛选（全局/模块级）

#### 5.3.2 页面布局
```
┌─────────────────────────────────────────────────────┐
│ [白名单词汇] [分类▼] [作用范围▼] [搜索]             │
├─────────────────────────────────────────────────────┤
│ [+新增] [批量删除] [导入] [导出]                     │
├─────────────────────────────────────────────────────┤
│ ┌──┬──────┬────┬────┬────────┬────┬────────┐      │
│ │☑│词汇  │分类│范围│模块    │状态│操作    │      │
│ ├──┼──────┼────┼────┼────────┼────┼────────┤      │
│ │☑│杀毒  │术语│全局│-       │启用│编辑删除│      │
│ │☑│敏捷  │术语│模块│community│启用│编辑删除│      │
│ └──┴──────┴────┴────┴────────┴────┴────────┘      │
└─────────────────────────────────────────────────────┘
```

### 5.4 策略配置页面（新增）

**路径**：`/sensitive/strategy`
**文件**：`vue3-admin-front/src/views/sensitive/strategy/index.vue`

#### 5.4.1 页面布局
```
┌─────────────────────────────────────────────────────┐
│ 策略配置                                             │
├─────────────────────────────────────────────────────┤
│ 社区模块（community）                                │
│ ┌────┬────┬────┬────────┬────────┬────┬──────┐   │
│ │等级│动作│通知│限制用户│限制时长│状态│操作  │   │
│ ├────┼────┼────┼────────┼────────┼────┼──────┤   │
│ │低  │替换│否  │否      │-       │启用│编辑  │   │
│ │中  │审核│是  │否      │-       │启用│编辑  │   │
│ │高  │拒绝│是  │是      │60分钟  │启用│编辑  │   │
│ └────┴────┴────┴────────┴────────┴────┴──────┘   │
├─────────────────────────────────────────────────────┤
│ 面试模块（interview）                                │
│ ┌────┬────┬────┬────────┬────────┬────┬──────┐   │
│ │等级│动作│通知│限制用户│限制时长│状态│操作  │   │
│ ├────┼────┼────┼────────┼────────┼────┼──────┤   │
│ │低  │替换│否  │否      │-       │启用│编辑  │   │
│ │中  │替换│是  │否      │-       │启用│编辑  │   │
│ │高  │拒绝│是  │是      │1440分钟│启用│编辑  │   │
│ └────┴────┴────┴────────┴────────┴────┴──────┘   │
└─────────────────────────────────────────────────────┘
```

### 5.5 统计分析页面（新增）

**路径**：`/sensitive/statistics`
**文件**：`vue3-admin-front/src/views/sensitive/statistics/index.vue`

#### 5.5.1 数据总览
```
┌─────────────────────────────────────────────────────┐
│ 敏感词统计总览                    [今日|本周|本月▼]  │
├─────────────────────────────────────────────────────┤
│ ┌──────┬──────┬──────┬──────┬──────┐              │
│ │总检测 │命中数 │拦截率 │误判率 │审核中 │              │
│ │10,245│  856 │ 8.4% │ 2.1% │  25  │              │
│ └──────┴──────┴──────┴──────┴──────┘              │
├─────────────────────────────────────────────────────┤
│ 命中趋势图（折线图）                                 │
│ ┌─────────────────────────────────────────────────┐│
│ │                                        ╱╲       ││
│ │                                  ╱╲   ╱  ╲      ││
│ │                      ╱╲      ╱╲ ╱  ╲╱    ╲     ││
│ │            ╱╲    ╱╲ ╱  ╲  ╱╲╱  ╲╱           ╲   ││
│ │      ╱╲   ╱  ╲╱╲╱  ╲╱    ╲╱                  ╲╱ ││
│ │ ────┴────┴────┴────┴────┴────┴────┴────┴────  ││
│ │ 1日  2日  3日  4日  5日  6日  7日  8日  9日  10日││
│ └─────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────┤
│ 热门敏感词 TOP 10                分类分布（饼图）    │
│ ┌─────────┬────┐              ┌──────────────┐    │
│ │敏感词   │次数│              │   政治敏感35%│    │
│ ├─────────┼────┤              │ 色情低俗25%  │    │
│ │词1      │ 45 │              │ 人身攻击20%  │    │
│ │词2      │ 38 │              │ 广告推广15%  │    │
│ │词3      │ 32 │              │ 其他5%       │    │
│ └─────────┴────┘              └──────────────┘    │
└─────────────────────────────────────────────────────┘
```

### 5.6 导航菜单配置

在 `vue3-admin-front/src/router/index.js` 中更新敏感词模块路由：

```javascript
{
  path: '/sensitive',
  name: 'Sensitive',
  meta: { title: '敏感词管理', icon: 'shield' },
  redirect: '/sensitive/words',
  children: [
    {
      path: 'words',
      name: 'SensitiveWords',
      component: () => import('@/views/sensitive/words/index.vue'),
      meta: { title: '词库管理', requiresAuth: true }
    },
    {
      path: 'whitelist',
      name: 'SensitiveWhitelist',
      component: () => import('@/views/sensitive/whitelist/index.vue'),
      meta: { title: '白名单管理', requiresAuth: true }
    },
    {
      path: 'strategy',
      name: 'SensitiveStrategy',
      component: () => import('@/views/sensitive/strategy/index.vue'),
      meta: { title: '策略配置', requiresAuth: true }
    },
    {
      path: 'statistics',
      name: 'SensitiveStatistics',
      component: () => import('@/views/sensitive/statistics/index.vue'),
      meta: { title: '统计分析', requiresAuth: true }
    },
    {
      path: 'source',
      name: 'SensitiveSource',
      component: () => import('@/views/sensitive/source/index.vue'),
      meta: { title: '词库来源', requiresAuth: true }
    },
    {
      path: 'config',
      name: 'SensitiveConfig',
      component: () => import('@/views/sensitive/config/index.vue'),
      meta: { title: '配置管理', requiresAuth: true }
    }
  ]
}
```

---

## 六、技术实现方案

### 6.1 变形词检测引擎

#### 6.1.1 文本预处理器
```java
public class TextPreprocessor {
    /**
     * 文本预处理（移除特殊字符、转换全角半角等）
     */
    public String preprocess(String text, PreprocessConfig config) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        
        String result = text;
        
        // 1. 全角转半角
        if (config.isConvertFullWidth()) {
            result = convertFullWidthToHalfWidth(result);
        }
        
        // 2. 移除特殊字符
        if (config.isRemoveSpecialChars()) {
            result = removeSpecialChars(result);
        }
        
        // 3. 转小写
        if (config.isToLowerCase()) {
            result = result.toLowerCase();
        }
        
        // 4. 同音字替换
        if (config.isReplaceHomophone()) {
            result = replaceHomophone(result);
        }
        
        // 5. 形似字替换
        if (config.isReplaceSimilarChar()) {
            result = replaceSimilarChar(result);
        }
        
        return result;
    }
}
```

#### 6.1.2 拼音检测器
```java
public class PinyinDetector {
    private final PinyinHelper pinyinHelper;
    
    /**
     * 检测拼音敏感词
     */
    public List<String> detectPinyin(String text, Set<String> pinyinSensitiveWords) {
        List<String> hitWords = new ArrayList<>();
        
        // 将文本转换为拼音
        String pinyin = pinyinHelper.toPinyin(text, "");
        
        // 在拼音中查找敏感词
        for (String word : pinyinSensitiveWords) {
            if (pinyin.contains(word)) {
                hitWords.add(word);
            }
        }
        
        return hitWords;
    }
}
```

#### 6.1.3 正则检测器
```java
public class RegexDetector {
    private final List<Pattern> patterns;
    
    /**
     * 正则表达式检测
     */
    public List<String> detectRegex(String text, List<SensitiveWord> regexWords) {
        List<String> hitWords = new ArrayList<>();
        
        for (SensitiveWord word : regexWords) {
            try {
                Pattern pattern = Pattern.compile(word.getWord());
                Matcher matcher = pattern.matcher(text);
                
                if (matcher.find()) {
                    hitWords.add(word.getWord());
                }
            } catch (PatternSyntaxException e) {
                log.error("正则表达式错误：{}", word.getWord(), e);
            }
        }
        
        return hitWords;
    }
}
```

### 6.2 多策略检测引擎

```java
public class EnhancedSensitiveEngine {
    private final AhoCorasickEngine acEngine;          // AC自动机（精确匹配）
    private final PinyinDetector pinyinDetector;       // 拼音检测
    private final RegexDetector regexDetector;         // 正则检测
    private final TextPreprocessor preprocessor;       // 文本预处理
    private final WhitelistService whitelistService;   // 白名单
    
    /**
     * 综合检测
     */
    public EnhancedCheckResult check(String text, CheckOptions options) {
        EnhancedCheckResult result = new EnhancedCheckResult();
        
        // 1. 白名单检查（优先级最高）
        if (options.isUseWhitelist() && whitelistService.contains(text)) {
            result.setHit(false);
            result.setWhitelistMatch(true);
            return result;
        }
        
        // 2. 文本预处理
        String preprocessedText = preprocessor.preprocess(text, options.getPreprocessConfig());
        
        // 3. AC自动机精确匹配
        List<MatchDetail> exactMatches = acEngine.findWithDetails(preprocessedText);
        result.addMatches(exactMatches, MatchType.EXACT);
        
        // 4. 拼音检测（如果开启）
        if (options.isCheckPinyin()) {
            List<MatchDetail> pinyinMatches = pinyinDetector.detect(text);
            result.addMatches(pinyinMatches, MatchType.PINYIN);
        }
        
        // 5. 正则检测（如果有正则词）
        if (options.isCheckRegex()) {
            List<MatchDetail> regexMatches = regexDetector.detect(text);
            result.addMatches(regexMatches, MatchType.REGEX);
        }
        
        // 6. 计算风险等级和处理策略
        result.calculateRiskLevel();
        result.determineAction(options.getModule());
        
        return result;
    }
}
```

### 6.3 白名单服务

```java
@Service
public class WhitelistService {
    private final SensitiveWhitelistMapper whitelistMapper;
    
    // 本地缓存（Caffeine）
    private Cache<String, Boolean> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    
    /**
     * 检查是否在白名单中
     */
    public boolean contains(String word) {
        return contains(word, "global", null);
    }
    
    public boolean contains(String word, String scope, String module) {
        String cacheKey = buildCacheKey(word, scope, module);
        
        return cache.get(cacheKey, key -> {
            // 先查全局白名单
            if (whitelistMapper.existsInWhitelist(word, "global", null)) {
                return true;
            }
            
            // 再查模块白名单
            if (StrUtil.isNotBlank(module)) {
                return whitelistMapper.existsInWhitelist(word, "module", module);
            }
            
            return false;
        });
    }
    
    /**
     * 刷新缓存
     */
    public void refreshCache() {
        cache.invalidateAll();
    }
}
```

### 6.4 策略管理服务

```java
@Service
public class SensitiveStrategyService {
    private final SensitiveStrategyMapper strategyMapper;
    
    // 策略缓存
    private Map<String, SensitiveStrategy> strategyCache = new ConcurrentHashMap<>();
    
    /**
     * 获取处理策略
     */
    public SensitiveStrategy getStrategy(String module, int level) {
        String key = module + ":" + level;
        
        return strategyCache.computeIfAbsent(key, k -> {
            SensitiveStrategy strategy = strategyMapper.selectByModuleAndLevel(module, level);
            
            // 如果没有配置，使用默认策略
            if (strategy == null) {
                strategy = getDefaultStrategy(level);
            }
            
            return strategy;
        });
    }
    
    /**
     * 更新策略
     */
    public void updateStrategy(SensitiveStrategy strategy) {
        strategyMapper.updateStrategy(strategy);
        
        // 清除缓存
        String key = strategy.getModule() + ":" + strategy.getLevel();
        strategyCache.remove(key);
        
        // 发布策略更新消息（通知其他节点）
        publishStrategyUpdateEvent(strategy);
    }
}
```

### 6.5 统计服务

```java
@Service
public class SensitiveStatisticsService {
    private final SensitiveHitStatisticsMapper hitStatisticsMapper;
    private final SensitiveUserViolationMapper userViolationMapper;
    
    /**
     * 记录命中统计（异步）
     */
    @Async
    public void recordHit(String word, String module, Integer categoryId) {
        LocalDate today = LocalDate.now();
        
        SensitiveHitStatistics stat = hitStatisticsMapper.selectByDateAndWord(today, word, module);
        
        if (stat == null) {
            // 新增记录
            stat = new SensitiveHitStatistics();
            stat.setStatDate(today);
            stat.setWord(word);
            stat.setCategoryId(categoryId);
            stat.setModule(module);
            stat.setHitCount(1);
            hitStatisticsMapper.insert(stat);
        } else {
            // 更新计数
            hitStatisticsMapper.incrementHitCount(stat.getId());
        }
    }
    
    /**
     * 记录用户违规（异步）
     */
    @Async
    public void recordUserViolation(Long userId) {
        LocalDate today = LocalDate.now();
        
        SensitiveUserViolation violation = userViolationMapper.selectByUserAndDate(userId, today);
        
        if (violation == null) {
            violation = new SensitiveUserViolation();
            violation.setUserId(userId);
            violation.setStatDate(today);
            violation.setViolationCount(1);
            violation.setLastViolationTime(LocalDateTime.now());
            userViolationMapper.insert(violation);
        } else {
            violation.setViolationCount(violation.getViolationCount() + 1);
            violation.setLastViolationTime(LocalDateTime.now());
            userViolationMapper.updateById(violation);
        }
        
        // 检查是否需要限制用户（如24小时内违规超过5次）
        checkAndRestrictUser(userId, violation.getViolationCount());
    }
    
    /**
     * 获取命中趋势
     */
    public List<TrendData> getHitTrend(LocalDate startDate, LocalDate endDate, String module) {
        return hitStatisticsMapper.selectTrend(startDate, endDate, module);
    }
    
    /**
     * 获取热门敏感词
     */
    public List<HotWord> getHotWords(LocalDate startDate, LocalDate endDate, int limit) {
        return hitStatisticsMapper.selectHotWords(startDate, endDate, limit);
    }
}
```

---

## 七、性能指标与监控

### 7.1 性能指标

| 指标 | 目标值 | 测试条件 |
|------|--------|---------|
| 单次检测响应时间 | <30ms | 文本长度1000字符，词库10000词 |
| 批量检测吞吐量 | >1000 TPS | 100个文本并发检测 |
| 词库初始化时间 | <3s | 50000个敏感词 |
| 内存占用 | <500MB | 50000个敏感词 |
| 缓存命中率 | >70% | 线上真实流量 |

### 7.2 监控指标

```java
@Component
public class SensitiveMetrics {
    private final MeterRegistry meterRegistry;
    
    // 检测次数计数器
    private final Counter checkCounter;
    
    // 命中次数计数器
    private final Counter hitCounter;
    
    // 检测耗时计时器
    private final Timer checkTimer;
    
    // 缓存命中率
    private final Gauge cacheHitRate;
    
    /**
     * 记录检测指标
     */
    public void recordCheck(String module, boolean hit, long duration) {
        checkCounter.increment();
        
        if (hit) {
            hitCounter.increment();
        }
        
        checkTimer.record(duration, TimeUnit.MILLISECONDS);
        
        Tags tags = Tags.of("module", module, "hit", String.valueOf(hit));
        meterRegistry.counter("sensitive.check", tags).increment();
    }
}
```

---

## 八、部署与升级方案

### 8.1 数据库升级脚本

```sql
-- v2.0.0升级脚本
-- 文件：sql/v2.0.0/upgrade.sql

-- 1. 新增表
SOURCE sql/v2.0.0/sensitive_whitelist.sql;
SOURCE sql/v2.0.0/sensitive_strategy.sql;
SOURCE sql/v2.0.0/sensitive_source.sql;
SOURCE sql/v2.0.0/sensitive_version.sql;
SOURCE sql/v2.0.0/sensitive_homophone.sql;
SOURCE sql/v2.0.0/sensitive_similar_char.sql;
SOURCE sql/v2.0.0/sensitive_hit_statistics.sql;
SOURCE sql/v2.0.0/sensitive_user_violation.sql;

-- 2. 修改现有表
ALTER TABLE sensitive_word
ADD COLUMN word_type TINYINT DEFAULT 1 COMMENT '词类型 1-普通词 2-正则表达式' AFTER word,
ADD COLUMN pinyin VARCHAR(500) COMMENT '拼音' AFTER word,
ADD COLUMN enable_variant_check TINYINT DEFAULT 1 COMMENT '启用变形词检测' AFTER action,
ADD COLUMN remark VARCHAR(500) COMMENT '备注' AFTER creator_id,
ADD INDEX idx_word_type (word_type),
ADD INDEX idx_pinyin (pinyin);

-- 3. 初始化默认策略
INSERT INTO sensitive_strategy (strategy_name, module, level, action, notify_admin, limit_user, limit_duration, description) VALUES
('社区低风险', 'community', 1, 'replace', 0, 0, NULL, '社区低风险敏感词-替换处理'),
('社区中风险', 'community', 2, 'replace', 1, 0, NULL, '社区中风险敏感词-替换处理并通知管理员'),
('社区高风险', 'community', 3, 'reject', 1, 1, 60, '社区高风险敏感词-拒绝发布并禁言1小时'),
('面试低风险', 'interview', 1, 'replace', 0, 0, NULL, '面试低风险敏感词-替换处理'),
('面试中风险', 'interview', 2, 'replace', 1, 0, NULL, '面试中风险敏感词-替换处理并通知管理员'),
('面试高风险', 'interview', 3, 'reject', 1, 1, 1440, '面试高风险敏感词-拒绝发布并禁言24小时');

-- 4. 初始化同音字映射（示例数据）
INSERT INTO sensitive_homophone (original_char, homophone_chars) VALUES
('傻', '沙,煞,啥'),
('逼', '比,逼,币,鄙'),
('操', '草,曹,槽');

-- 5. 为现有敏感词生成拼音（需要应用层执行）
-- UPDATE sensitive_word SET pinyin = ... WHERE pinyin IS NULL;
```

### 8.2 兼容性保证

1. **接口兼容**：v1.0.0所有接口保持不变，确保现有业务不受影响
2. **功能开关**：新功能通过配置开关控制，可以逐步启用
3. **数据兼容**：新字段设置默认值，不影响现有数据
4. **渐进式升级**：可以先升级后端，再升级前端

### 8.3 配置项

```yaml
# application.yml
sensitive:
  # 变形词检测配置
  variant:
    enabled: true                    # 是否启用变形词检测
    remove-special-chars: true       # 是否移除特殊字符
    check-pinyin: true               # 是否检测拼音
    check-homophone: true            # 是否检测同音字
    check-similar-char: true         # 是否检测形似字
    
  # 白名单配置
  whitelist:
    enabled: true                    # 是否启用白名单
    cache-size: 10000                # 白名单缓存大小
    cache-ttl: 300                   # 白名单缓存过期时间（秒）
    
  # 缓存配置
  cache:
    enabled: true                    # 是否启用结果缓存
    ttl: 300                         # 缓存过期时间（秒）
    max-size: 100000                 # 最大缓存条数
    
  # 统计配置
  statistics:
    enabled: true                    # 是否启用统计
    async: true                      # 是否异步记录统计
    
  # 性能配置
  performance:
    max-text-length: 10000           # 最大文本长度
    max-batch-size: 100              # 最大批量检测数量
    max-processing-time: 5000        # 最大处理时间（毫秒）
```

---

## 九、测试方案

### 9.1 功能测试

#### 9.1.1 变形词检测测试用例

| 测试用例 | 输入 | 期望输出 | 优先级 |
|---------|------|---------|-------|
| 特殊字符绕过 | "敏@感#词" | 命中"敏感词" | P0 |
| 拼音绕过 | "mingan词" | 命中"敏感词" | P0 |
| 全角字符 | "ＡＢＣ" | 命中"ABC" | P1 |
| 同音字 | "沙比" | 命中"傻逼" | P1 |
| 组合绕过 | "敏@gan词" | 命中"敏感词" | P0 |

#### 9.1.2 白名单测试用例

| 测试用例 | 输入 | 期望输出 | 优先级 |
|---------|------|---------|-------|
| 全局白名单 | "杀毒软件" | 不命中 | P0 |
| 模块白名单 | "敏捷开发"(community) | 不命中 | P1 |
| 非白名单 | "敏感词" | 命中 | P0 |

#### 9.1.3 策略测试用例

| 测试用例 | 模块 | 等级 | 期望动作 | 优先级 |
|---------|------|------|---------|-------|
| 社区低风险 | community | 低 | replace | P0 |
| 社区中风险 | community | 中 | review | P0 |
| 社区高风险 | community | 高 | reject | P0 |

### 9.2 性能测试

#### 9.2.1 单词库性能测试
- 词库规模：10000、30000、50000
- 文本长度：100、1000、5000字符
- 测试指标：响应时间、内存占用

#### 9.2.2 并发性能测试
- 并发用户：100、500、1000
- 测试时长：10分钟
- 测试指标：TPS、响应时间、错误率

### 9.3 压力测试

- 词库极限：100000个敏感词
- 文本极限：10000字符
- 并发极限：5000并发
- 测试指标：系统稳定性、资源占用

---

## 十、风险评估与应对

### 10.1 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 性能下降 | 高 | 中 | 充分测试，分阶段上线，保留降级开关 |
| 内存溢出 | 高 | 低 | 限制词库大小，监控内存，及时告警 |
| 缓存穿透 | 中 | 低 | 布隆过滤器，空值缓存 |
| 分布式一致性 | 中 | 中 | 使用分布式锁，消息队列确保最终一致性 |

### 10.2 业务风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 误判率上升 | 高 | 中 | 完善白名单，提供用户反馈渠道，人工复核 |
| 审核队列积压 | 中 | 中 | 增加审核人力，优化审核流程，自动化审核 |
| 用户体验下降 | 中 | 低 | 优化提示文案，提供修改建议 |

### 10.3 数据风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 词库泄露 | 高 | 低 | 权限控制，审计日志，加密存储 |
| 数据库压力 | 中 | 中 | 读写分离，缓存优化，异步写入 |

---

## 十一、迭代规划

### 11.1 v2.0.0（核心优化）- 开发周期：4周

**Week 1-2：基础功能**
- 变形词检测引擎
- 白名单机制
- 策略管理
- 数据库表设计与创建

**Week 3：统计与前端**
- 统计分析功能
- 前端页面开发
- 接口联调

**Week 4：测试与优化**
- 统计分析
- 性能优化
- 测试与修复
- 文档编写

### 11.2 v2.1.0（多模态检测）- 开发周期：2周

- 图片OCR检测
- 图片敏感词识别
- 前端图片上传与检测
- 测试与优化

### 11.3 v2.2.0（智能学习）- 开发周期：4周

- 机器学习模型训练
- 语义分析引擎
- 自动词库更新
- 模型评估与优化

### 11.4 v2.3.0（多语言支持）- 开发周期：2周

- 英文敏感词检测
- 繁体字检测
- 多语言词库管理
- 国际化配置

---

## 十二、成功标准

### 12.1 技术指标

- ✅ 准确率达到95%+
- ✅ 召回率达到90%+
- ✅ 误判率降低到3%以下
- ✅ 检测性能<30ms
- ✅ 绕过率<5%

### 12.2 业务指标

- ✅ 敏感内容拦截率提升50%+
- ✅ 用户投诉减少60%+
- ✅ 管理效率提升80%+
- ✅ 系统稳定性99.9%+

### 12.3 用户满意度

- ✅ 管理员满意度>90%
- ✅ 用户满意度>85%
- ✅ 误判申诉处理及时率>95%

---

## 附录

### 附录A：参考资料

1. 《敏感词匹配系统设计》- 阿里云开发者社区
2. 《AC自动机算法详解》- 算法导论
3. 《企业内容安全最佳实践》- 腾讯云
4. 《NLP在内容审核中的应用》- 百度AI

### 附录B：术语表

| 术语 | 解释 |
|------|------|
| AC自动机 | Aho-Corasick自动机，多模式字符串匹配算法 |
| DFA | 确定有限状态自动机（Deterministic Finite Automaton）|
| OCR | 光学字符识别（Optical Character Recognition）|
| NLP | 自然语言处理（Natural Language Processing）|
| TPS | 每秒事务数（Transactions Per Second）|
| ReDOS | 正则表达式拒绝服务攻击 |

### 附录C：FAQ

**Q1：为什么不使用第三方敏感词服务（如阿里云、腾讯云）？**
A1：可以结合使用。自建系统作为基础能力，第三方服务作为补充。自建系统的优势是：成本低、可定制、数据安全、无依赖。

**Q2：变形词检测会不会导致误判率上升？**
A2：有可能。通过白名单机制可以降低误判率。同时，每个词可以单独控制是否启用变形检测。

**Q3：如何保证多节点部署时词库一致性？**
A3：使用Redis Pub/Sub或消息队列，在词库更新时通知所有节点刷新。

**Q4：如何评估系统效果？**
A4：通过统计分析功能，查看准确率、召回率、误判率等指标。同时，收集用户反馈数据进行持续优化。

---

**文档版本**：v2.0.0
**编写日期**：2025-10-04
**编写人**：AI助手
**审核人**：待定
**状态**：草稿

---

