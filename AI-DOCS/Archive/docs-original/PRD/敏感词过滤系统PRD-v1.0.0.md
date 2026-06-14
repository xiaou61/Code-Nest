# 敏感词过滤系统 PRD v1.0.0

## 一、项目背景与目标

### 1.1 项目背景
随着 Code-Nest 平台用户量和内容交互频率的快速增长，社区帖子、评论、面试题解答、朋友圈动态等用户生成内容（UGC）数量激增。为保障平台内容健康、符合法规要求，已建设一套高效、准确的敏感词过滤系统。

### 1.2 功能目标
- **文本过滤**：支持对用户提交的文本内容进行敏感词检测和过滤
- **词库管理**：提供完整的敏感词词库管理功能，支持分类管理
- **批量操作**：支持敏感词的批量导入和删除操作
- **实时生效**：词库更新后可立即生效
- **业务集成**：可与社区、面试、朋友圈等模块无缝集成

## 二、功能需求

### 2.1 核心功能

#### 2.1.1 敏感词检测服务
- **检测接口**：提供单个文本和批量文本的敏感词检测
- **检测结果**：返回是否命中敏感词、命中词列表、处理后文本、风险等级等
- **处理策略**：支持替换、拒绝、审核三种处理动作
- **性能保护**：支持最大文本长度10000字符，最大敏感词数量50000个

#### 2.1.2 敏感词管理
- **基础管理**：新增、修改、删除、查询敏感词
- **分类管理**：支持政治敏感、色情低俗、人身攻击、广告推广、其他违规五个分类
- **风险等级**：低风险、中风险、高风险三个等级
- **处理动作**：替换（***）、拒绝（禁止发布）、审核（进入审核队列）
- **状态管理**：启用/禁用状态控制

#### 2.1.3 批量操作
- **批量导入**：支持TXT文件批量导入敏感词（每行一个或逗号分隔）
- **批量删除**：支持选择多个敏感词进行批量删除
- **重复处理**：导入时自动跳过重复的敏感词
- **词库刷新**：手动刷新内存中的敏感词库

### 2.2 管理功能

#### 2.2.1 查询筛选
- **关键词搜索**：支持按敏感词内容模糊查询
- **分类筛选**：按敏感词分类进行筛选
- **等级筛选**：按风险等级进行筛选
- **状态筛选**：按启用/禁用状态筛选
- **分页显示**：支持分页查询，默认每页10条

#### 2.2.2 数据展示
- **列表显示**：表格形式展示敏感词信息
- **标签展示**：风险等级、处理动作、状态以标签形式显示
- **操作记录**：显示创建人和创建时间信息

## 三、技术架构设计

### 3.1 整体架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   业务模块       │    │   敏感词服务     │    │   管理后台       │
│                 │    │                 │    │                 │
│ xiaou-community │    │ xiaou-sensitive │    │ vue3-admin-front│
│ xiaou-interview │────│                 │    │                 │
│ xiaou-moment    │    │ AC自动机引擎     │    │ 词库管理界面     │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                         │
                              │                         │
                       ┌─────────────────┐    
                       │   MySQL存储     │    
                       │                 │    
                       │ 敏感词数据      │    
                       │ 分类数据        │    
                       │ 操作日志        │    
                       └─────────────────┘    
```

### 3.2 核心技术

#### 3.2.1 算法实现
- **Aho-Corasick 自动机**：多模式字符串匹配算法
- **读写锁机制**：保证线程安全的词库更新
- **性能优化**：限制遍历深度，防止性能问题
- **内存保护**：设置最大文本长度和敏感词数量限制

#### 3.2.2 技术栈
- **后端框架**：Spring Boot 3.x + MyBatis
- **数据库**：MySQL 8.0
- **前端框架**：Vue 3 + Element Plus
- **文件处理**：支持TXT文件上传和解析

### 3.3 模块结构

#### 3.3.1 xiaou-sensitive模块
```
xiaou-sensitive/
├── controller/
│   ├── admin/
│   │   └── SensitiveWordAdminController.java    # 管理后台控制器
│   └── api/
│       └── SensitiveWordController.java         # API控制器
├── domain/
│   ├── SensitiveWord.java                       # 敏感词实体
│   ├── SensitiveCategory.java                   # 分类实体
│   └── SensitiveLog.java                        # 日志实体
├── dto/
│   ├── SensitiveCheckRequest.java               # 检测请求DTO
│   ├── SensitiveCheckResponse.java              # 检测响应DTO
│   ├── SensitiveWordDTO.java                    # 敏感词DTO
│   └── SensitiveWordQuery.java                  # 查询DTO
├── mapper/
│   ├── SensitiveWordMapper.java                 # 敏感词数据访问
│   ├── SensitiveCategoryMapper.java             # 分类数据访问
│   └── SensitiveLogMapper.java                  # 日志数据访问
├── service/
│   ├── SensitiveWordService.java                # 敏感词服务接口
│   ├── SensitiveCheckService.java               # 检测服务接口
│   └── impl/                                    # 服务实现
└── engine/
    ├── AhoCorasickEngine.java                   # AC自动机实现
    └── SensitiveEngine.java                     # 引擎接口
```

## 四、数据库设计

### 4.1 敏感词分类表（sensitive_category）
```sql
CREATE TABLE sensitive_category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(255) COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);
```

### 4.2 敏感词表（sensitive_word）
```sql
CREATE TABLE sensitive_word (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(255) NOT NULL COMMENT '敏感词',
    category_id INT NOT NULL COMMENT '分类ID',
    level TINYINT DEFAULT 1 COMMENT '风险等级 1-低 2-中 3-高',
    action TINYINT DEFAULT 1 COMMENT '处理动作 1-替换 2-拒绝 3-审核',
    status TINYINT DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    creator_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_status (category_id, status),
    INDEX idx_update_time (update_time),
    INDEX idx_word (word),
    FOREIGN KEY (category_id) REFERENCES sensitive_category(id)
);
```

### 4.3 敏感词检测日志表（sensitive_log）
```sql
CREATE TABLE sensitive_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '用户ID',
    module VARCHAR(50) NOT NULL COMMENT '模块名称',
    business_id BIGINT COMMENT '业务ID',
    original_text TEXT COMMENT '原始文本',
    hit_words JSON COMMENT '命中的敏感词',
    action VARCHAR(20) COMMENT '执行的动作',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_time (user_id, create_time),
    INDEX idx_module_time (module, create_time)
);
```

### 4.4 初始化数据
```sql
-- 默认分类
INSERT INTO sensitive_category VALUES
(1, '政治敏感', '涉及政治敏感内容的词汇', 1, 1, NOW()),
(2, '色情低俗', '涉及色情、低俗内容的词汇', 2, 1, NOW()),
(3, '人身攻击', '涉及人身攻击、辱骂的词汇', 3, 1, NOW()),
(4, '广告推广', '涉及广告推广、营销的词汇', 4, 1, NOW()),
(5, '其他违规', '其他违规内容的词汇', 5, 1, NOW());
```

## 五、接口设计

### 5.1 API接口（内部调用）

#### 5.1.1 单个文本检测
```java
POST /sensitive/check
Content-Type: application/json

// 请求参数
{
    "text": "要检测的文本内容",
    "module": "community",
    "businessId": 123456,
    "userId": 789
}

// 响应结果
{
    "code": 200,
    "data": {
        "hit": true,
        "hitWords": ["敏感词1", "敏感词2"],
        "processedText": "处理后的文本内容",
        "riskLevel": 2,
        "action": "replace",
        "allowed": true
    }
}
```

#### 5.1.2 批量文本检测
```java
POST /sensitive/check/batch
Content-Type: application/json

// 请求参数
[
    {
        "text": "文本1",
        "module": "community",
        "businessId": 123
    },
    {
        "text": "文本2",
        "module": "interview",
        "businessId": 456
    }
]

// 响应结果
{
    "code": 200,
    "data": [
        {
            "hit": true,
            "hitWords": ["敏感词"],
            "processedText": "处理后文本1",
            "riskLevel": 1,
            "action": "replace",
            "allowed": true
        },
        {
            "hit": false,
            "hitWords": [],
            "processedText": "文本2",
            "riskLevel": 0,
            "action": "none",
            "allowed": true
        }
    ]
}
```

### 5.2 管理后台接口

#### 5.2.1 敏感词管理
```java
// 分页查询敏感词列表
POST /admin/sensitive/words/list
{
    "pageNum": 1,
    "pageSize": 10,
    "word": "搜索关键词",
    "categoryId": 1,
    "level": 2,
    "status": 1
}

// 新增敏感词
POST /admin/sensitive/words
{
    "word": "新敏感词",
    "categoryId": 1,
    "level": 1,
    "action": 1,
    "status": 1
}

// 更新敏感词
POST /admin/sensitive/words/update
{
    "id": 123,
    "word": "更新后的敏感词",
    "categoryId": 2,
    "level": 2,
    "action": 2,
    "status": 1
}

// 删除敏感词
POST /admin/sensitive/words/delete/{id}

// 批量删除敏感词
POST /admin/sensitive/words/delete/batch
[1, 2, 3, 4, 5]

// 批量导入敏感词
POST /admin/sensitive/words/import
Content-Type: multipart/form-data
file: [TXT文件]

// 刷新词库
POST /admin/sensitive/refresh

// 查询分类列表
POST /admin/sensitive/categories
```

## 六、前端界面设计

### 6.1 敏感词管理页面
- **路径**：`/sensitive/words`
- **文件**：`vue3-admin-front/src/views/sensitive/words/index.vue`

#### 6.1.1 页面功能
- **搜索栏**：敏感词、分类、风险等级、状态筛选
- **操作栏**：新增、批量删除、导入、刷新词库
- **数据表格**：分页展示敏感词列表
- **编辑对话框**：新增/编辑敏感词
- **导入对话框**：批量导入敏感词

#### 6.1.2 字段映射
- **风险等级**：1-低风险(success)、2-中风险(warning)、3-高风险(danger)
- **处理动作**：1-替换(info)、2-拒绝(danger)、3-审核(warning)
- **状态**：0-禁用(danger)、1-启用(success)

### 6.2 导航菜单配置
需要在 `vue3-admin-front/src/router/index.js` 中添加敏感词管理路由：

```javascript
{
  path: '/sensitive',
  name: 'Sensitive',
  meta: { title: '敏感词管理', icon: 'shield' },
  children: [
    {
      path: 'words',
      name: 'SensitiveWords',
      component: () => import('@/views/sensitive/words/index.vue'),
      meta: { title: '敏感词列表', requiresAuth: true }
    }
  ]
}
```

## 七、核心流程设计

### 7.1 敏感词检测流程
```
用户提交文本
     ↓
参数校验（长度≤10000字符）
     ↓
获取读锁，确保线程安全
     ↓
AC自动机算法匹配
     ↓
是否命中敏感词？
  ┌─ 是 ─→ 根据action处理
  │         ├─ 1：替换为***
  │         ├─ 2：拒绝发布  
  │         └─ 3：进入审核
  │         ↓
  │       记录日志（可选）
  │         ↓
  └─ 否 ─→ 返回原文本
           ↓
       返回检测结果
```

### 7.2 词库更新流程
```
管理员操作词库
     ↓
数据库更新
     ↓
获取写锁
     ↓
重新构建AC自动机
  ├─ 构建Trie树
  ├─ 构建failure指针
  └─ 性能保护检查
     ↓
释放写锁
     ↓
更新完成
```

### 7.3 批量导入流程
```
上传TXT文件
     ↓
文件内容解析
  ├─ 按行分割
  ├─ 逗号分割处理
  └─ 去重过滤
     ↓
逐个插入数据库
  ├─ 检查重复
  ├─ 设置默认值
  └─ 统计结果
     ↓
刷新词库
     ↓
返回导入结果
```

## 八、性能与限制

### 8.1 性能参数
- **最大文本长度**：10,000字符
- **最大敏感词数量**：50,000个
- **最大敏感词长度**：100字符
- **failure链遍历深度**：限制为10层

### 8.2 线程安全
- **读写锁**：使用ReentrantReadWriteLock保证线程安全
- **读锁**：检测时获取读锁，支持并发读取
- **写锁**：更新词库时获取写锁，确保数据一致性

### 8.3 性能优化
- **文本预处理**：转换为小写进行匹配
- **防止循环**：failure指针循环检测
- **深度限制**：限制遍历深度防止性能问题
- **快速检查**：containsSensitiveWords方法一旦发现就返回

## 九、部署说明

### 9.1 数据库初始化
1. 执行 `sql/v1.2.1/sensitive_words.sql` 创建表结构
2. 插入默认分类数据
3. 可根据需要插入初始敏感词数据

### 9.2 模块部署
1. 确保 `xiaou-sensitive` 模块已正确打包
2. 配置数据库连接信息
3. 启动应用服务

### 9.3 前端部署
1. 确保敏感词管理页面已部署
2. 在导航菜单中添加相应路由
3. 配置相关权限控制

## 十、集成说明

### 10.1 业务模块集成
各业务模块需要在表单提交前调用敏感词检测接口：

```java
// 检测文本
SensitiveCheckRequest request = new SensitiveCheckRequest();
request.setText(userInput);
request.setModule("community");
request.setUserId(userId);

SensitiveCheckResponse response = sensitiveCheckService.checkText(request);

if (!response.getAllowed()) {
    // 拒绝发布
    throw new BusinessException("内容包含敏感词，无法发布");
}

// 使用处理后的文本
String cleanText = response.getProcessedText();
```

### 10.2 前端集成示例
```javascript
// 在表单提交前检测
import { checkSensitiveWords } from '@/api/sensitive'

const validateContent = async (content) => {
  try {
    const response = await checkSensitiveWords({
      text: content,
      module: 'community'
    })
    
    if (!response.allowed) {
      ElMessage.error('内容包含敏感词，请修改后重试')
      return false
    }
    
    // 使用处理后的内容
    return response.processedText
  } catch (error) {
    ElMessage.error('内容检测失败')
    return false
  }
}
```

---