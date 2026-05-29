# 面试题模块PRD

## 1. 产品概述

### 1.1 产品定位
面试题管理和学习平台，通过分类+题单+题目的三级结构组织内容，支持官方题单和用户自建题单，提供多种学习模式和个性化功能。

### 1.2 核心价值
- **内容管理**：分层级管理面试题内容，支持官方题单和用户创建
- **学习体验**：提供背题模式和做题模式，满足不同学习需求
- **便捷导入**：支持Markdown格式批量导入，简化内容管理
- **个性化功能**：收藏系统、随机抽题、学习进度追踪

### 1.3 用户群体
- **系统管理员**：管理官方题单、分类和系统配置
- **普通用户**：学习面试题，创建个人题单，收藏管理

## 2. 功能架构

### 2.1 系统架构图

```
面试题模块
├── 分类管理（Categories）
│   ├── 分类CRUD
│   ├── 状态控制
│   └── 排序管理
├── 题单管理（Question Sets）
│   ├── 官方题单（管理端）
│   ├── 用户题单（用户端）
│   ├── 状态管理
│   └── 可见性控制
├── 题目管理（Questions）
│   ├── Markdown导入
│   ├── 题目导航
│   └── 浏览统计
├── 收藏系统（Favorites）
│   ├── 题单收藏
│   ├── 题目收藏
│   └── 收藏管理
└── 学习功能
    ├── 双模式学习
    ├── 随机抽题
    ├── 搜索功能
    └── 学习统计
```

## 3. 详细功能说明

### 3.1 管理端功能

#### 3.1.1 分类管理
**页面路径**: `/admin/interview/categories`

**核心功能**:
- **分类列表**：显示所有分类，包含题单数量统计
- **新增分类**：创建新的题目分类
- **编辑分类**：修改分类名称、描述、排序
- **状态控制**：启用/禁用分类（开关切换）
- **删除保护**：有关联题单的分类不能删除
- **搜索过滤**：按分类名称和状态筛选

**数据模型**:
```sql
interview_category {
  id: 分类ID
  name: 分类名称
  description: 分类描述  
  sort_order: 排序序号
  question_set_count: 题单数量（冗余字段）
  status: 状态（0-禁用 1-启用）
  create_time: 创建时间
  update_time: 更新时间
}
```

#### 3.1.2 题单管理
**页面路径**: `/admin/interview/question-sets`

**核心功能**:
- **题单列表**：分页展示所有官方题单
- **新增题单**：创建官方题单
- **编辑题单**：修改题单基本信息
- **题目管理**：跳转到题目管理页面
- **状态管理**：草稿/发布/下线状态控制
- **批量操作**：支持批量删除

#### 3.1.3 题目管理
**页面路径**: `/admin/interview/questions`

**核心功能**:
- **题目列表**：按题单展示题目，支持分页
- **题目详情**：查看题目内容和答案
- **排序管理**：题单内题目排序
- **批量删除**：删除选中题目
- **统计信息**：浏览量、收藏量统计

### 3.2 用户端功能

#### 3.2.1 题库首页
**页面路径**: `/interview`

**核心功能**:
- **分类导航**：按分类浏览题单
- **题单展示**：卡片式展示题单信息
- **搜索功能**：关键词搜索题单和题目
- **快捷操作**：随机抽题、我的收藏入口

**展示信息**:
- 题单标题、描述、分类
- 题目数量、浏览量、收藏量
- 创建者信息、创建时间
- 题单类型（官方/用户）

#### 3.2.2 题单详情页
**页面路径**: `/interview/question-sets/:id`

**核心功能**:
- **题单信息展示**：完整题单信息
- **题目列表**：题单内所有题目
- **学习模式选择**：背题模式/做题模式
- **收藏功能**：收藏/取消收藏题单
- **学习入口**：开始学习、继续学习

#### 3.2.3 题目学习页
**页面路径**: `/interview/questions/:setId/:questionId`

**双模式学习**:

1. **背题模式**（默认显示答案）
   - 题目和答案同时显示
   - 适合复习和记忆
   - 支持Markdown渲染

2. **做题模式**（隐藏答案）
   - 初始只显示题目
   - 点击"查看答案"按钮显示答案
   - 支持隐藏答案重新做题

**导航功能**:
- 上一题/下一题按钮
- 学习进度条显示
- 返回题单按钮
- 题目位置显示（第X题/共Y题）

**其他功能**:
- 题目收藏功能
- 浏览量统计
- 模式切换（保存用户偏好）

#### 3.2.4 随机抽题
**页面路径**: `/interview/random`

**核心功能**:
- **题单选择**：多选题单进行抽题
- **数量设置**：设定抽题数量
- **随机算法**：打乱顺序随机抽取
- **学习模式**：支持双模式学习
- **重新抽题**：快速重新生成题目

#### 3.2.5 收藏管理
**页面路径**: `/interview/favorites`

**功能特性**:
- **分类查看**：题单收藏、题目收藏分开展示
- **收藏统计**：显示收藏总数
- **快速访问**：点击直接跳转到对应内容
- **取消收藏**：支持批量取消收藏
- **收藏时间**：显示收藏时间记录

**题单收藏**:
- 卡片式展示
- 显示题单基本信息和统计数据
- 支持直接跳转学习

**题目收藏**:
- 列表式展示
- 显示题目标题和所属题单
- 支持直接跳转到题目

## 4. 核心特色功能

### 4.1 Markdown批量导入

**实现原理**:
- 使用`## `作为题目分割标识
- 标题后的所有内容作为答案
- 支持Markdown语法渲染
- 批量解析和导入

**导入格式示例**:
```markdown
## 什么是Java多态？

多态是面向对象编程的重要特性，指同一个接口可以有不同的实现方式。

在Java中，多态主要体现在：
- 方法重载（编译时多态）
- 方法重写（运行时多态）

## Spring Bean生命周期

Spring Bean的生命周期包括以下阶段：
1. 实例化
2. 属性注入
3. 初始化
4. 使用
5. 销毁
```

**功能特性**:
- 实时预览解析结果
- 错误提示和验证
- 支持覆盖导入
- 批量插入性能优化

### 4.2 智能收藏系统

**收藏类型**:
- 题单收藏（targetType=1）
- 题目收藏（targetType=2）

**功能特性**:
- 收藏状态实时同步
- 收藏数量统计
- 防重复收藏
- 收藏时间记录

### 4.3 双模式学习体验

**模式对比**:

| 功能 | 背题模式 | 做题模式 |
|------|----------|----------|
| 答案显示 | 默认显示 | 点击显示 |
| 适用场景 | 复习记忆 | 自测练习 |
| 用户操作 | 直接浏览 | 思考后查看 |
| 学习效果 | 加强记忆 | 检验掌握 |

**技术实现**:
- 前端状态切换
- 用户偏好保存（localStorage）
- 页面间状态保持

### 4.4 高级搜索功能

**搜索范围**:
- 题单标题、描述
- 题目标题、答案内容
- 创建者信息

**搜索特性**:
- 模糊匹配
- 分页结果
- 高亮显示
- 多条件组合

## 5. 数据模型设计

### 5.1 核心实体关系

```
InterviewCategory (分类)
    ↓ 1:N
InterviewQuestionSet (题单)
    ↓ 1:N  
InterviewQuestion (题目)

User (用户)
    ↓ 1:N
InterviewFavorite (收藏)
    → 关联到题单或题目
```

### 5.2 表结构详情

#### 题单表 (interview_question_set)
```sql
CREATE TABLE interview_question_set (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '题单标题',
    description TEXT COMMENT '题单描述',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    type TINYINT DEFAULT 1 COMMENT '类型 1-官方 2-用户创建',
    visibility TINYINT DEFAULT 1 COMMENT '可见性 1-公开 2-私有',
    question_count INT DEFAULT 0 COMMENT '题目数量',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏次数',
    status TINYINT DEFAULT 1 COMMENT '状态 0-草稿 1-发布 2-下线',
    creator_id BIGINT COMMENT '创建人ID',
    creator_name VARCHAR(50) COMMENT '创建人姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 题目表 (interview_question)
```sql
CREATE TABLE interview_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_set_id BIGINT NOT NULL COMMENT '所属题单ID',
    title VARCHAR(500) NOT NULL COMMENT '题目标题',
    answer TEXT NOT NULL COMMENT '参考答案（Markdown格式）',
    sort_order INT DEFAULT 1 COMMENT '题单内排序',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 收藏表 (interview_favorite)
```sql
CREATE TABLE interview_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    target_type TINYINT NOT NULL COMMENT '收藏类型 1-题单 2-题目',
    target_id BIGINT NOT NULL COMMENT '目标ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
);
```

## 6. 技术实现要点

### 6.1 后端技术栈
- **框架**: Spring Boot + MyBatis
- **数据库**: MySQL 8.0
- **缓存**: Redis（热点数据缓存）
- **工具**: Hutool（对象转换、工具类）

### 6.2 前端技术栈
- **框架**: Vue 3 + Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **Markdown渲染**: markdown-it
- **代码高亮**: highlight.js

### 6.3 性能优化

#### 缓存策略
- 分类列表：长期缓存（1小时）
- 题单列表：中期缓存（30分钟）
- 题目内容：短期缓存（10分钟）
- 用户收藏状态：内存缓存

#### 分页优化
- 题单列表：默认10条/页
- 题目列表：默认20条/页
- 搜索结果：支持自定义分页大小

#### 前端优化
- 页面路由缓存（keep-alive）
- 图片懒加载
- 组件按需加载
- 防抖搜索

### 6.4 安全控制

#### 权限管理
- 管理员：所有功能权限
- 普通用户：只能管理自己创建的内容
- 游客：只读权限

#### 数据校验
- 前端表单验证
- 后端参数校验
- 防XSS攻击（Markdown内容）
- SQL注入防护

## 7. API接口设计

### 7.1 管理端接口

#### 分类管理
```
GET    /admin/interview/categories          # 获取分类列表
POST   /admin/interview/categories          # 创建分类
PUT    /admin/interview/categories/{id}     # 更新分类
DELETE /admin/interview/categories/{id}     # 删除分类
```

#### 题单管理
```
GET    /admin/interview/question-sets       # 分页查询题单
POST   /admin/interview/question-sets       # 创建题单
PUT    /admin/interview/question-sets/{id}  # 更新题单
DELETE /admin/interview/question-sets/{id}  # 删除题单
```

#### 题目管理
```
GET    /admin/interview/questions           # 分页查询题目
POST   /admin/interview/questions           # 创建题目
PUT    /admin/interview/questions/{id}      # 更新题目
DELETE /admin/interview/questions/{id}      # 删除题目
POST   /admin/interview/questions/batch-delete  # 批量删除
```

### 7.2 用户端接口

#### 公开浏览
```
GET    /interview/categories                # 获取启用分类
GET    /interview/question-sets             # 分页查询公开题单
GET    /interview/question-sets/{id}        # 获取题单详情
GET    /interview/question-sets/{id}/questions  # 获取题单题目列表
GET    /interview/questions/{setId}/{id}    # 获取题目详情
GET    /interview/questions/{setId}/{id}/prev   # 获取上一题
GET    /interview/questions/{setId}/{id}/next   # 获取下一题
```

#### 搜索功能
```
GET    /interview/search/question-sets      # 搜索题单
GET    /interview/search/questions          # 搜索题目
```

#### 随机抽题
```
POST   /interview/question-sets/questions/random  # 随机抽题
```

#### 收藏管理
```
POST   /interview/favorites/add             # 添加收藏
POST   /interview/favorites/remove          # 取消收藏
POST   /interview/favorites/check           # 检查收藏状态
GET    /interview/favorites/my              # 获取我的收藏
```

