# 个人博客模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
为了提升用户的内容创作体验和个人品牌建设，Code-Nest平台需要为每个用户提供独立的个人博客空间。用户可以在自己的博客中发布技术文章、学习笔记、项目总结等内容，打造专属的个人技术博客站点。

### 💡 核心价值
- **个人品牌**: 每个用户拥有独立的博客空间，展示个人技术能力
- **内容沉淀**: 支持长文创作，沉淀技术知识和学习笔记
- **专业展示**: 提供美观的博客主页，支持自定义配置
- **积分消耗**: 开通博客需消耗50积分，发布文章需消耗20积分，鼓励高质量内容产出
- **SEO优化**: 独立URL访问，利于搜索引擎收录
- **便捷分享**: 支持博客链接分享，展示个人作品

### 🎨 产品定位
- **目标用户**: 技术博主、知识分享者、学习记录者
- **使用场景**: 技术文章发布、学习笔记整理、项目经验总结
- **差异化**: 独立博客空间 + 积分激励机制 + 简洁展示

------

## 二、用户角色

### 1. 博客作者（前台用户）
- 开通个人博客空间（消耗50积分，一次性）
- 发布、编辑、删除博客文章（发布消耗20积分）
- 配置博客基本信息（昵称、简介、头像、背景）
- 管理文章分类和标签
- 分享博客链接

### 2. 博客访客（游客/登录用户）
- 访问已开通博客的用户主页
- 浏览博客文章列表
- 阅读文章详情

### 3. 管理员（后台）
- 博客内容审核管理
- 文章列表管理（置顶、下架、删除）
- 分类标签管理
- 违规内容处理

------

## 三、功能需求

### 1. 前台（用户端）功能

#### 1.1 博客空间管理

##### 1.1.1 博客开通
- **主动开通**: 用户点击"开通博客"按钮主动申请开通
- **积分扣除**: 开通博客需扣除50积分
- **积分检查**: 开通前检查用户积分是否≥50
- **开通失败**: 积分不足时提示充值或打卡获取积分
- **博客域名**: 每个用户拥有唯一的博客访问地址 `/blog/{userId}` 或 `/blog/{username}`
- **初始配置**: 开通后自动初始化默认博客名称、简介、头像
- **一次性操作**: 每个用户只能开通一次，开通后不可关闭

##### 1.1.2 博客配置
- **基本信息**:
  - 博客名称（默认用户昵称，可修改）
  - 个人简介（最多200字）
  - 头像设置（继承用户头像，可单独设置）
  - 博客背景图（支持自定义上传）
  
- **展示配置**:
  - 社交链接（Github、CSDN、掘金等）
  - 个人标签（最多10个）
  - 博客公告（最多100字）
  - 是否公开博客（默认公开）

#### 1.2 文章管理

##### 1.2.1 文章创建
- **编辑器**: Markdown编辑器（支持实时预览）
- **基本信息**:
  - 文章标题（必填，1-100字符）
  - 文章封面（选填，支持上传图片）
  - 文章分类（必选，单选）
  - 文章标签（选填，最多5个）
  - 文章摘要（选填，自动提取或手动输入，最多200字）
  
- **内容编辑**:
  - Markdown语法支持
  - 代码高亮
  - 图片上传（支持拖拽、粘贴）
  - 表格支持
  - 公式支持（LaTeX）
  - 自动保存草稿

- **发布设置**:
  - 立即发布
  - 保存草稿
  - 是否原创（默认是）

##### 1.2.2 积分消耗机制
- **发布扣费**: 发布文章扣除20积分
- **草稿免费**: 保存草稿不扣除积分
- **积分检查**: 发布前检查用户积分是否充足
- **扣费失败**: 积分不足时提示充值或打卡获取积分
- **已发布文章**: 编辑已发布文章不重复扣费

##### 1.2.3 文章编辑
- 编辑已发布文章（不重复扣积分）
- 编辑草稿文章
- 修改文章分类、标签
- 更新文章封面
- 修改发布时间

##### 1.2.4 文章删除
- 软删除机制（可恢复）
- 删除确认提示
- 删除后文章不再展示

##### 1.2.5 文章状态管理
- **草稿**: 未发布的文章
- **已发布**: 正常展示的文章
- **已下架**: 被管理员下架或用户自己下架
- **已删除**: 软删除状态

#### 1.3 文章展示

##### 1.3.1 博客主页
- **头部区域**:
  - 博客背景图
  - 博主头像、昵称
  - 个人简介
  - 社交链接

- **统计信息**:
  - 文章总数

- **文章列表**:
  - 文章卡片（封面、标题、摘要、分类、标签、发布时间）
  - 分页展示（每页10篇）
  - 按发布时间倒序

##### 1.3.2 文章详情页
- **文章头部**:
  - 文章标题
  - 作者信息（头像、昵称）
  - 发布时间、更新时间
  - 分类、标签

- **文章内容**:
  - Markdown渲染
  - 代码高亮显示
  - 图片预览放大
  - 目录导航（自动提取标题生成）

- **功能按钮**:
  - 分享按钮（复制文章链接）
  - 返回博客主页

- **侧边栏**:
  - 博主信息卡片
  - 相关文章推荐

##### 1.3.3 文章分类/标签筛选
- 按分类筛选文章
- 按标签筛选文章
- 支持多标签组合筛选

#### 1.4 个人中心

##### 1.4.1 我的博客管理
- **文章管理**:
  - 已发布文章列表
  - 草稿箱
  - 已删除文章（可恢复）
  - 批量操作（删除、下架）

- **博客信息**:
  - 文章总数
  - 博客开通时间
  - 博客链接（一键复制）

------

### 2. 后台（管理端）功能

#### 2.1 博客管理

##### 2.1.1 博客列表
- 分页显示所有用户博客
- 按用户ID、博客名称筛选
- 按创建时间、文章数量排序
- 查看博客详细信息

##### 2.1.2 博客审核
- 审核博客配置信息
- 处理违规博客（封禁、下架）
- 博客数据统计

#### 2.2 文章管理

##### 2.2.1 文章列表管理
- **列表展示**:
  - 分页显示所有文章
  - 多维度筛选（用户、分类、标签、状态、时间范围）
  - 关键词搜索（标题、内容）
  - 敏感词检测标记

- **文章操作**:
  - 置顶设置（支持时长配置）
  - 状态管理（正常/下架/删除）
  - 查看文章详情
  - 强制删除文章

##### 2.2.2 内容审核
- 查看待审核文章
- 审核通过/拒绝
- 违规内容处理
- 审核记录查看

#### 2.3 分类标签管理

##### 2.3.1 分类管理
- 创建、编辑、删除分类
- 分类排序设置
- 分类文章数量统计
- 分类图标设置

##### 2.3.2 标签管理
- 标签库管理
- 热门标签统计
- 标签合并
- 无用标签清理

#### 2.4 基础数据查看

##### 2.4.1 概览数据
- 博客总数
- 文章总数
- 活跃博主数（近30天发布过文章）

------

## 四、核心流程

### 1. 博客开通流程
1. **进入开通页**: 用户点击"开通博客"按钮
2. **积分检查**: 检查用户积分是否≥50
3. **确认开通**: 用户确认开通并同意扣除50积分
4. **扣除积分**: 积分充足，扣除50积分
5. **创建博客**: 创建博客配置记录，初始化默认信息
6. **开通成功**: 提示开通成功，跳转到博客配置页面

### 2. 文章创作流程
1. **检查博客**: 检查用户是否已开通博客，未开通则引导开通
2. **进入创作**: 点击"写文章"按钮
3. **编辑内容**: 填写标题、内容、封面、分类、标签
4. **保存草稿**: 随时保存草稿（不扣积分）
5. **积分检查**: 点击发布时检查积分是否≥20
6. **扣除积分**: 积分充足，扣除20积分
7. **文章发布**: 文章状态改为已发布，展示在博客主页

### 3. 用户访问流程
1. **进入博客**: 访问 `/blog/{userId}` 或 `/blog/{username}`
2. **浏览主页**: 查看博主信息、文章列表
3. **筛选文章**: 按分类、标签筛选
4. **阅读文章**: 点击文章进入详情页
5. **分享文章**: 复制文章链接进行分享

### 4. 管理员审核流程
1. **内容监控**: 系统自动检测敏感词
2. **待审核列表**: 查看需要审核的文章
3. **内容审查**: 查看文章详细内容
4. **审核决策**: 通过、拒绝或下架
5. **通知用户**: 审核结果通知作者

------

## 五、技术方案

### 1. 数据库设计

#### 1.1 博客配置表（blog_config）
```sql
CREATE TABLE blog_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    blog_name VARCHAR(100) COMMENT '博客名称',
    blog_description VARCHAR(500) COMMENT '博客简介',
    blog_avatar VARCHAR(255) COMMENT '博客头像URL',
    blog_cover VARCHAR(255) COMMENT '博客背景图URL',
    blog_notice VARCHAR(200) COMMENT '博客公告',
    personal_tags VARCHAR(500) COMMENT '个人标签，JSON格式',
    social_links VARCHAR(1000) COMMENT '社交链接，JSON格式',
    is_public TINYINT DEFAULT 1 COMMENT '是否公开：1-公开 0-私密',
    total_articles INT DEFAULT 0 COMMENT '文章总数',
    points_cost INT DEFAULT 50 COMMENT '开通消耗积分',
    status TINYINT DEFAULT 1 COMMENT '博客状态：1-正常 0-封禁',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_total_articles (total_articles DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '博客配置表';
```

#### 1.2 博客文章表（blog_article）
```sql
CREATE TABLE blog_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '作者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    cover_image VARCHAR(255) COMMENT '文章封面图',
    summary VARCHAR(500) COMMENT '文章摘要',
    content LONGTEXT NOT NULL COMMENT '文章内容（Markdown格式）',
    category_id BIGINT COMMENT '文章分类ID',
    tags VARCHAR(200) COMMENT '文章标签，JSON数组格式',
    status TINYINT DEFAULT 0 COMMENT '文章状态：0-草稿 1-已发布 2-已下架 3-已删除',
    is_original TINYINT DEFAULT 1 COMMENT '是否原创：1-原创 0-转载',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶：1-置顶 0-普通',
    top_expire_time DATETIME COMMENT '置顶过期时间',
    points_cost INT DEFAULT 20 COMMENT '发布消耗积分',
    publish_time DATETIME COMMENT '发布时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status_publish_time (status, publish_time DESC),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_is_top (is_top, publish_time DESC),
    FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '博客文章表';
```

#### 1.3 文章分类表（blog_category）
```sql
CREATE TABLE blog_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_icon VARCHAR(255) COMMENT '分类图标URL',
    category_description VARCHAR(200) COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    article_count INT DEFAULT 0 COMMENT '文章数量',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_category_name (category_name),
    INDEX idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '文章分类表';
```

#### 1.4 文章标签表（blog_tag）
```sql
CREATE TABLE blog_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tag_name (tag_name),
    INDEX idx_use_count (use_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '文章标签表';
```



### 2. 后端接口设计

#### 2.1 用户端博客接口（/user/blog）

##### 博客配置
```
POST /user/blog/open                     # 开通博客（扣除50积分）
GET /user/blog/check-status              # 检查博客开通状态
GET /user/blog/config/{userId}           # 获取博客配置
POST /user/blog/config/update            # 更新博客配置
GET /user/blog/stats                     # 获取博客统计数据
```

##### 文章管理
```
POST /user/blog/article/create          # 创建文章（草稿）
POST /user/blog/article/publish         # 发布文章（扣除积分）
POST /user/blog/article/update          # 更新文章
DELETE /user/blog/article/{id}           # 删除文章
POST /user/blog/article/list             # 获取文章列表
GET /user/blog/article/{id}              # 获取文章详情
POST /user/blog/article/my-list          # 我的文章列表
POST /user/blog/article/draft-list       # 我的草稿列表
```

##### 分类标签
```
GET /user/blog/categories                # 获取分类列表
GET /user/blog/tags                      # 获取标签列表
POST /user/blog/article/by-category      # 按分类获取文章
POST /user/blog/article/by-tag           # 按标签获取文章
```


#### 2.2 管理端博客接口（/admin/blog）

##### 博客管理
```
POST /admin/blog/list                    # 获取博客列表
GET /admin/blog/{id}                     # 获取博客详情
POST /admin/blog/update-status           # 更新博客状态
```

##### 文章管理
```
POST /admin/blog/article/list            # 获取文章列表（管理端）
POST /admin/blog/article/audit           # 审核文章
POST /admin/blog/article/top             # 置顶文章
POST /admin/blog/article/update-status   # 更新文章状态
DELETE /admin/blog/article/{id}          # 删除文章（管理端）
```

##### 分类标签管理
```
POST /admin/blog/category/create         # 创建分类
POST /admin/blog/category/update         # 更新分类
DELETE /admin/blog/category/{id}         # 删除分类
POST /admin/blog/category/list           # 分类列表

POST /admin/blog/tag/merge               # 合并标签
DELETE /admin/blog/tag/{id}              # 删除标签
POST /admin/blog/tag/list                # 标签列表
```

##### 基础数据
```
GET /admin/blog/statistics               # 获取基础统计数据（博客总数、文章总数、活跃博主数）
```

#### 2.3 请求/响应示例

**开通博客（扣除积分）：**
```json
// 请求
POST /user/blog/open

// 响应
{
    "code": 200,
    "message": "博客开通成功，扣除50积分",
    "data": {
        "blogId": 123,
        "userId": 12345,
        "pointsRemaining": 950,
        "createTime": "2025-10-02 14:30:00"
    }
}

// 积分不足时
{
    "code": 400,
    "message": "积分不足，当前积分：30，开通博客需要50积分",
    "data": null
}

// 已开通时
{
    "code": 400,
    "message": "您已开通博客，无需重复开通",
    "data": {
        "blogId": 123,
        "createTime": "2025-09-20 10:00:00"
    }
}
```

**检查博客开通状态：**
```json
// 请求
GET /user/blog/check-status

// 响应（已开通）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "isOpened": true,
        "blogId": 123,
        "createTime": "2025-09-20 10:00:00"
    }
}

// 响应（未开通）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "isOpened": false,
        "currentPoints": 850,
        "requiredPoints": 50,
        "canOpen": true
    }
}
```

**发布文章（扣除积分）：**
```json
// 请求
POST /user/blog/article/publish
{
    "id": 123,  // 草稿ID，首次发布可为空
    "title": "Spring Boot 实战指南",
    "coverImage": "https://example.com/cover.jpg",
    "summary": "这是一篇关于Spring Boot的实战教程",
    "content": "# Spring Boot 实战\n\n## 简介\n...",
    "categoryId": 1,
    "tags": ["Spring Boot", "Java", "后端"],
    "isOriginal": 1,
    "allowComment": 1
}

// 响应
{
    "code": 200,
    "message": "发布成功，扣除20积分",
    "data": {
        "articleId": 456,
        "pointsRemaining": 980,
        "publishTime": "2025-10-02 14:30:00"
    }
}

// 积分不足时
{
    "code": 400,
    "message": "积分不足，当前积分：10，发布文章需要20积分",
    "data": null
}
```

**获取博客主页：**
```json
// 请求
GET /user/blog/config/12345

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "userId": 12345,
        "blogName": "张三的技术博客",
        "blogDescription": "专注于Java后端技术分享",
        "blogAvatar": "https://example.com/avatar.jpg",
        "blogCover": "https://example.com/cover.jpg",
        "blogNotice": "欢迎来到我的博客，持续更新中...",
        "personalTags": ["Java", "Spring", "MySQL"],
        "socialLinks": {
            "github": "https://github.com/zhangsan",
            "csdn": "https://blog.csdn.net/zhangsan"
        },
        "totalArticles": 25
    }
}
```

**获取文章列表：**
```json
// 请求
POST /user/blog/article/list
{
    "userId": 12345,
    "categoryId": null,
    "tags": null,
    "keyword": null,
    "pageNum": 1,
    "pageSize": 10
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "records": [
            {
                "id": 456,
                "userId": 12345,
                "userNickname": "张三",
                "userAvatar": "https://example.com/avatar.jpg",
                "title": "Spring Boot 实战指南",
                "coverImage": "https://example.com/cover.jpg",
                "summary": "这是一篇关于Spring Boot的实战教程",
                "categoryId": 1,
                "categoryName": "后端开发",
                "tags": ["Spring Boot", "Java", "后端"],
                "isOriginal": 1,
                "isTop": 0,
                "publishTime": "2025-10-01 10:30:00"
            }
        ],
        "total": 25,
        "current": 1,
        "size": 10
    }
}
```

**文章详情：**
```json
// 请求
GET /user/blog/article/456

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "id": 456,
        "userId": 12345,
        "userNickname": "张三",
        "userAvatar": "https://example.com/avatar.jpg",
        "title": "Spring Boot 实战指南",
        "coverImage": "https://example.com/cover.jpg",
        "summary": "这是一篇关于Spring Boot的实战教程",
        "content": "# Spring Boot 实战\n\n## 简介\n...",
        "categoryId": 1,
        "categoryName": "后端开发",
        "tags": ["Spring Boot", "Java", "后端"],
        "isOriginal": 1,
        "publishTime": "2025-10-01 10:30:00",
        "updateTime": "2025-10-01 10:30:00",
        "canEdit": false,
        "canDelete": false,
        "relatedArticles": [
            {
                "id": 457,
                "title": "Spring Cloud 微服务实战"
            }
        ]
    }
}
```

**管理端文章列表：**
```json
// 请求
POST /admin/blog/article/list
{
    "userId": null,
    "categoryId": null,
    "status": null,
    "keyword": "Spring",
    "startTime": "2025-01-01 00:00:00",
    "endTime": "2025-12-31 23:59:59",
    "pageNum": 1,
    "pageSize": 20
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "records": [
            {
                "id": 456,
                "userId": 12345,
                "userNickname": "张三",
                "title": "Spring Boot 实战指南",
                "categoryName": "后端开发",
                "status": 1,
                "statusDesc": "已发布",
                "hasSensitiveWord": false,
                "isTop": 0,
                "publishTime": "2025-10-01 10:30:00",
                "createTime": "2025-10-01 09:15:00"
            }
        ],
        "total": 100,
        "current": 1,
        "size": 20
    }
}
```

### 3. DTO结构设计

#### 3.1 请求DTO

**文章发布请求（ArticlePublishRequest）：**
```java
{
    "id": "Long（草稿ID，选填）",
    "title": "String（标题，必填，1-100字符）",
    "coverImage": "String（封面图URL，选填）",
    "summary": "String（摘要，选填，最多200字符）",
    "content": "String（Markdown内容，必填）",
    "categoryId": "Long（分类ID，必填）",
    "tags": "List<String>（标签列表，选填，最多5个）",
    "isOriginal": "Integer（是否原创，默认1）"
}
```

**文章列表查询请求（ArticleListRequest）：**
```java
{
    "userId": "Long（作者ID，选填）",
    "categoryId": "Long（分类ID，选填）",
    "tags": "List<String>（标签筛选，选填）",
    "keyword": "String（关键词搜索，选填）",
    "status": "Integer（状态筛选，选填）",
    "pageNum": "Integer（页码，默认1）",
    "pageSize": "Integer（每页大小，默认10）"
}
```

**博客配置更新请求（BlogConfigUpdateRequest）：**
```java
{
    "blogName": "String（博客名称，选填）",
    "blogDescription": "String（博客简介，选填）",
    "blogAvatar": "String（博客头像，选填）",
    "blogCover": "String（博客背景图，选填）",
    "blogNotice": "String（博客公告，选填）",
    "personalTags": "List<String>（个人标签，选填）",
    "socialLinks": "Map<String, String>（社交链接，选填）",
    "isPublic": "Integer（是否公开，选填）"
}
```

#### 3.2 响应DTO

**文章发布响应（ArticlePublishResponse）：**
```java
{
    "articleId": "Long（文章ID）",
    "pointsRemaining": "Integer（剩余积分）",
    "publishTime": "String（发布时间）"
}
```

**文章详情响应（ArticleDetailResponse）：**
```java
{
    "id": "Long（文章ID）",
    "userId": "Long（作者ID）",
    "userNickname": "String（作者昵称）",
    "userAvatar": "String（作者头像）",
    "title": "String（文章标题）",
    "coverImage": "String（封面图）",
    "summary": "String（摘要）",
    "content": "String（内容）",
    "categoryId": "Long（分类ID）",
    "categoryName": "String（分类名称）",
    "tags": "List<String>（标签列表）",
    "isOriginal": "Integer（是否原创）",
    "publishTime": "String（发布时间）",
    "updateTime": "String（更新时间）",
    "canEdit": "Boolean（是否可编辑）",
    "canDelete": "Boolean（是否可删除）",
    "relatedArticles": "List<ArticleSimple>（相关文章）"
}
```

**博客统计响应（BlogStatisticsResponse）：**
```java
{
    "totalBlogs": "Long（博客总数）",
    "totalArticles": "Long（文章总数）",
    "activeBloggers": "Long（活跃博主数，近30天发布过文章）"
}
```

### 4. 前端组件设计

#### 4.1 核心组件
- `BlogHome`：博客主页组件
- `ArticleEditor`：Markdown编辑器组件
- `ArticleCard`：文章卡片组件
- `ArticleDetail`：文章详情组件
- `BlogSidebar`：博客侧边栏组件
- `CategoryFilter`：分类筛选组件
- `TagFilter`：标签筛选组件

#### 4.2 状态管理
使用Pinia管理全局状态：
- `blogStore`：博客配置和文章数据管理
- `editorStore`：编辑器状态管理
- `userStore`：用户信息管理

### 5. 业务规则与验证

#### 5.1 开通限制
- **积分检查**: 开通前检查用户积分≥50
- **唯一性检查**: 每个用户只能开通一次
- **账号验证**: 必须是已登录的正常账号

#### 5.2 发布限制
- **博客检查**: 发布前检查用户是否已开通博客
- **标题验证**: 1-100字符，不能为空
- **内容验证**: 不能为空，最少50字符
- **积分检查**: 发布前检查用户积分≥20
- **分类必选**: 必须选择一个分类
- **标签限制**: 最多选择5个标签
- **敏感词过滤**: 发布前检查敏感词

#### 5.3 积分扣除规则
- **开通博客**: 扣除50积分（一次性）
- **开通失败**: 积分不扣除，回滚事务
- **首次发布文章**: 扣除20积分
- **编辑已发布**: 不重复扣除积分
- **草稿保存**: 不扣除积分
- **发布失败**: 积分不扣除，回滚事务
- **删除文章**: 不退还积分
- **关闭博客**: 不支持关闭，积分不退还

#### 5.4 权限控制
- **博客开通**: 仅登录用户可开通，且未开通过
- **文章创建**: 仅已开通博客的用户可创建
- **文章编辑**: 仅作者本人可编辑
- **文章删除**: 仅作者本人或管理员可删除
- **博客配置**: 仅博主本人可修改

------

## 六、功能特色

1. **主动开通机制**: 用户主动开通博客，避免资源浪费，开通需50积分
2. **独立博客空间**: 每个用户拥有独立的博客展示空间
3. **Markdown编辑**: 强大的Markdown编辑器，支持实时预览
4. **双重积分激励**: 开通博客扣除50积分，发布文章扣除20积分，鼓励高质量内容
5. **完善的分类标签**: 支持文章分类和多标签管理
6. **便捷分享**: 支持博客和文章链接分享
7. **SEO友好**: 独立URL访问，利于搜索引擎收录
8. **双端分离设计**: 前台用户端和后台管理端分离
9. **内容安全保障**: 敏感词检测、内容审核机制

------

## 七、技术实现亮点

1. **积分扣除事务**: 开通博客/发布文章和扣除积分在同一事务中，保证数据一致性
2. **博客状态检查**: 所有文章操作前先检查博客开通状态
3. **自动提取摘要**: 未填写摘要时自动提取内容前200字
4. **草稿自动保存**: 编辑器定时自动保存草稿
5. **图片懒加载**: 文章列表图片懒加载，提升性能
6. **代码高亮**: 支持多语言代码高亮显示
7. **目录自动生成**: 根据Markdown标题自动生成文章目录
8. **相关文章推荐**: 根据分类和标签推荐相关文章
9. **全文搜索**: 基于MySQL全文索引的文章搜索
10. **敏感词检测**: 发布前自动检测敏感词并标记

