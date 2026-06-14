## 1. 项目基本信息

**项目名称：** Web朋友圈（简版动态广场）  
**版本：** v1.0.0  
**技术栈：** Spring Boot + MyBatis + Vue3  
**适用端：** PC端（优先 

## 2. 背景与目标

### 2.1 背景
平台需要一个轻量级的用户互动区，支持用户发动态、看动态、互动（点赞、评论）。类似朋友圈，但去掉好友关系，只保留核心功能，营造轻社交氛围。

### 2.2 产品目标
- **核心目标：** 提供简洁的动态发布与浏览功能
- **用户目标：** 用户可以便捷地分享生活动态，与其他用户互动
- **业务目标：** 提升用户活跃度和粘性，增加平台社交属性

## 3. 功能需求

### 3.1 动态管理

#### 3.1.1 发布动态
**基础功能：**
- 文本输入：支持100字以内文本
- 表情支持：内置emoji表情包
- 图片上传：最多9张图片，支持jpg/png/gif格式
- 发布权限：仅登录用户可发布

**交互细节：**
- 文本计数器实时显示剩余字数
- 图片支持拖拽上传和点击上传
- 图片预览功能，支持删除已选图片
- 发布前内容校验

**限制规则：**
- 文本：1-100字符
- 图片：单张最大5MB，总数最多9张
- 发布频率：同一用户5分钟内最多发布3条动态

#### 3.1.2 动态展示
**显示内容：**
- 用户信息：头像、昵称
- 发布时间：相对时间显示（如"3分钟前"）
- 动态内容：文本、表情、图片九宫格
- 互动数据：点赞数、评论数
- 操作按钮：点赞、评论、删除（仅自己的动态）

**排序规则：**
- 按发布时间倒序排列
- 支持分页加载（每页20条）

#### 3.1.3 动态删除
- 用户只能删除自己发布的动态
- 删除需二次确认
- 删除后相关点赞、评论数据一并删除

### 3.2 点赞功能

#### 3.2.1 点赞操作
- 登录用户可对任意动态进行点赞/取消点赞
- 同一用户对同一动态只能点赞一次
- 点赞状态实时更新，支持乐观锁

#### 3.2.2 点赞展示
- 显示点赞总数
- 显示当前用户是否已点赞（高亮状态）
- 点击查看点赞用户列表（显示前10个用户头像）

### 3.3 评论功能

#### 3.3.1 评论发表
- 支持文字评论（最多200字）
- 支持emoji表情
- 单层评论结构（不支持回复评论）

#### 3.3.2 评论展示
- 显示评论者头像、昵称、时间
- 默认显示最新3条评论
- 支持展开查看全部评论
- 评论按时间正序排列

#### 3.3.3 评论管理
- 用户可删除自己的评论
- 动态发布者可删除自己动态下的任意评论

### 3.4 管理端功能

#### 3.4.1 动态管理
**动态列表管理：**
- 分页显示所有动态，支持按状态筛选
- 支持按用户ID、关键词、时间范围筛选
- 显示敏感词检测结果标记
- 支持单个删除和批量删除操作

**内容审核：**
- 查看动态详细信息（内容、图片、发布时间等）
- 对违规动态进行删除处理
- 支持按状态（正常、删除、审核中）过滤

#### 3.4.2 评论管理
**评论列表管理：**
- 分页显示所有评论，支持多维度筛选
- 支持按动态ID、用户ID、内容关键词筛选
- 支持按时间范围筛选评论
- 管理员可删除任意违规评论

#### 3.4.3 数据统计
**基础统计：**
- 动态总数统计
- 点赞总数统计
- 评论总数统计
- 活跃用户数统计

**时间维度统计：**
- 支持按日期范围查询统计数据
- 每日发布动态数量趋势
- 每日点赞、评论数量统计
- 用户活跃度分析

## 4. 非功能需求

### 4.1 性能要求
- 动态列表加载时间 < 2秒
- 图片上传响应时间 < 5秒
- 支持并发用户数 > 1000

### 4.2 安全要求
- 内容审核：敏感词过滤
- 图片安全：病毒扫描、格式验证
- 防刷保护：发布频率限制、IP限制

### 4.3 兼容性要求
- PC端：Chrome 80+、Firefox 75+、Safari 13+
- 移动端：iOS 12+、Android 8+

## 5. 页面设计

### 5.1 动态广场页面（用户端）

#### 5.1.1 页面布局
```
┌─────────────────────────────────────────┐
│ 导航栏                                     │
├─────────────────────────────────────────┤
│ 发布动态区域                               │
│ ┌─────────┐ 想说点什么... [发布]          │
│ │ 头像    │ [📷] [😊]                      │
│ └─────────┘                              │
├─────────────────────────────────────────┤
│ 动态列表                                   │
│ ┌─────────┐ 用户A · 2小时前               │
│ │ 头像    │ 今天天气真好啊~ [图片]         │
│ └─────────┘ 👍 12  💬 5                    │
│             └─ 用户B：是的呢              │
│             └─ 用户C：+1                  │
├─────────────────────────────────────────┤
│ [加载更多]                                │
└─────────────────────────────────────────┘
```

#### 5.1.2 交互规范
- 发布区域：点击文本框自动展开，显示字数统计
- 图片上传：支持拖拽和点击上传，显示进度条
- 动态卡片：hover效果，操作按钮高亮
- 无限滚动：滚动到底部自动加载更多

### 5.2 管理后台页面（管理端）

#### 5.2.1 动态管理页面
**筛选功能：**
- 用户ID筛选器（支持用户昵称搜索）
- 状态筛选器（正常/删除/审核中）
- 关键词搜索（动态内容模糊搜索）
- 时间范围选择器

**列表功能：**
- 动态内容预览（支持图片缩略图）
- 用户信息显示（头像、昵称）
- 点赞数、评论数统计
- 敏感词检测标记
- 状态标签显示
- 操作按钮：删除、批量删除

#### 5.2.2 评论管理页面
**筛选功能：**
- 动态ID筛选（支持跳转到对应动态）
- 用户ID筛选
- 评论内容关键词搜索
- 时间范围筛选

**列表功能：**
- 评论内容完整显示
- 所属动态信息预览
- 评论者信息显示
- 删除操作按钮

#### 5.2.3 数据统计页面
**概览面板：**
- 核心指标卡片：动态总数、点赞总数、评论总数、活跃用户数
- 支持时间范围筛选统计数据

**趋势图表：**
- 发布动态数量趋势图（按日统计）
- 点赞评论互动趋势图
- 用户活跃度分析图表

**数据导出：**
- 支持统计数据Excel导出
- 支持自定义时间范围导出

## 6. 技术方案

### 6.1 数据库设计

#### 6.1.1 动态表（moments）
```sql
CREATE TABLE moments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT COMMENT '动态内容(最多100字)',
    images TEXT COMMENT '图片URLs，JSON格式存储',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0删除 2审核中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_status (status),
    INDEX idx_status_time (status, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '动态表';
```

#### 6.1.2 点赞表（moment_likes）
```sql
CREATE TABLE moment_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    moment_id BIGINT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_moment_user (moment_id, user_id),
    INDEX idx_moment_id (moment_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '动态点赞表';
```

#### 6.1.3 评论表（moment_comments）
```sql
CREATE TABLE moment_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    moment_id BIGINT NOT NULL COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content TEXT NOT NULL COMMENT '评论内容(最多200字)',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_moment_id (moment_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '动态评论表';
```

### 6.2 接口设计

#### 6.2.1 用户端动态接口（/user/moments）
```
POST /user/moments/publish             # 发布动态
POST /user/moments/list                # 获取动态列表
DELETE /user/moments/{id}              # 删除动态
POST /user/moments/{momentId}/like     # 点赞/取消点赞
POST /user/moments/comment             # 发表评论
POST /user/moments/comments            # 获取动态完整评论列表
DELETE /user/moments/comments/{id}     # 删除评论
```

#### 6.2.2 管理端动态接口（/admin/moments）
```
POST /admin/moments/list               # 获取动态列表（管理端）
POST /admin/moments/batch-delete       # 批量删除动态
POST /admin/moments/comments/list      # 获取评论列表（管理端）
DELETE /admin/moments/comments/{id}    # 删除评论（管理端）
POST /admin/moments/statistics         # 获取统计数据
```

#### 6.2.3 请求/响应示例
**发布动态：**
```json
// 请求
POST /user/moments/publish
{
    "content": "今天天气真好！",
    "images": ["url1", "url2"]
}

// 响应
{
    "code": 200,
    "message": "发布成功",
    "data": 123  // 返回动态ID
}
```

**获取动态列表：**
```json
// 请求
POST /user/moments/list
{
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
                "id": 123,
                "userId": 456,
                "userNickname": "用户A",
                "userAvatar": "avatar_url",
                "content": "今天天气真好！",
                "images": ["url1", "url2"],
                "likeCount": 12,
                "commentCount": 5,
                "isLiked": true,
                "canDelete": false,
                "createTime": "2024-01-01 12:00:00",
                "recentComments": [
                    {
                        "id": 789,
                        "userId": 101,
                        "userNickname": "用户B",
                        "userAvatar": "avatar_url",
                        "content": "是的呢",
                        "canDelete": false,
                        "createTime": "2024-01-01 12:05:00"
                    }
                ]
            }
        ],
        "total": 100,
        "current": 1,
        "size": 10
    }
}
```

**点赞动态：**
```json
// 请求
POST /user/moments/123/like

// 响应
{
    "code": 200,
    "message": "操作成功",
    "data": true  // true表示已点赞，false表示取消点赞
}
```

**发布评论：**
```json
// 请求
POST /user/moments/comment
{
    "momentId": 123,
    "content": "太棒了！"
}

// 响应
{
    "code": 200,
    "message": "评论成功",
    "data": 789  // 返回评论ID
}
```

**获取完整评论列表：**
```json
// 请求
POST /user/moments/comments
{
    "momentId": 123,
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
                "id": 789,
                "userId": 101,
                "userNickname": "用户B",
                "userAvatar": "avatar_url",
                "content": "太棒了！",
                "canDelete": false,
                "createTime": "2024-01-01 12:05:00"
            }
        ],
        "total": 50,
        "current": 1,
        "size": 20
    }
}
```

**管理端获取动态列表：**
```json
// 请求
POST /admin/moments/list
{
    "pageNum": 1,
    "pageSize": 20,
    "userId": null,
    "status": null,
    "keyword": null,
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-12-31 23:59:59"
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "records": [
            {
                "id": 123,
                "userId": 456,
                "userNickname": "用户A",
                "content": "今天天气真好！",
                "images": ["url1", "url2"],
                "likeCount": 12,
                "commentCount": 5,
                "status": 1,
                "statusDesc": "正常",
                "hasSensitiveWord": false,
                "createTime": "2024-01-01 12:00:00"
            }
        ],
        "total": 100,
        "current": 1,
        "size": 20
    }
}
```

**获取统计数据：**
```json
// 请求
POST /admin/moments/statistics
{
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-01-31 23:59:59"
}

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "totalMoments": 1000,
        "totalLikes": 5000,
        "totalComments": 3000,
        "activeUsers": 500,
        "dailyStats": [
            {
                "date": "2024-01-01",
                "momentCount": 50,
                "likeCount": 200,
                "commentCount": 100
            }
        ]
    }
}
```

### 6.3 DTO结构设计

#### 6.3.1 请求DTO
**动态发布请求（MomentPublishRequest）：**
```java
{
    "content": "String(1-100字符，必填)",
    "images": "List<String>（最多9个URL，选填）"
}
```

**动态列表查询请求（UserMomentListRequest）：**
```java
{
    "pageNum": "Integer（页码，默认1）",
    "pageSize": "Integer（每页大小，默认10）"
}
```

**评论发布请求（CommentPublishRequest）：**
```java
{
    "momentId": "Long（动态ID，必填）",
    "content": "String（评论内容，必填）"
}
```

**管理端动态查询请求（AdminMomentListRequest）：**
```java
{
    "pageNum": "Integer",
    "pageSize": "Integer", 
    "userId": "Long（用户ID筛选，选填）",
    "status": "Integer（状态筛选，选填）",
    "keyword": "String（关键词搜索，选填）",
    "startTime": "String（开始时间，选填）",
    "endTime": "String（结束时间，选填）"
}
```

#### 6.3.2 响应DTO
**动态列表响应（MomentListResponse）：**
```java
{
    "id": "Long（动态ID）",
    "userId": "Long（用户ID）", 
    "userNickname": "String（用户昵称）",
    "userAvatar": "String（用户头像URL）",
    "content": "String（动态内容）",
    "images": "List<String>（图片URL列表）",
    "likeCount": "Integer（点赞数）",
    "commentCount": "Integer（评论数）",
    "isLiked": "Boolean（是否已点赞）",
    "canDelete": "Boolean（是否可删除）",
    "createTime": "String（创建时间）",
    "recentComments": "List<CommentResponse>（最新评论列表）"
}
```

**统计数据响应（MomentStatisticsResponse）：**
```java
{
    "totalMoments": "Long（动态总数）",
    "totalLikes": "Long（点赞总数）",
    "totalComments": "Long（评论总数）", 
    "activeUsers": "Long（活跃用户数）",
    "dailyStats": "List<DailyStatistics>（每日统计数据）"
}
```

### 6.4 前端组件设计

#### 6.4.1 核心组件
- `MomentPublisher`：动态发布组件
- `MomentCard`：动态卡片组件  
- `CommentList`：评论列表组件
- `EmojiPicker`：表情选择器组件
- `ImageUploader`：图片上传组件

#### 6.4.2 状态管理
使用Pinia管理全局状态：
- `momentStore`：动态数据管理
- `userStore`：用户信息管理

### 6.5 业务规则与验证

#### 6.5.1 发布限制
- **内容验证：** 动态内容1-100字符，不能为空
- **图片限制：** 最多上传9张图片，单张图片最大5MB
- **频率控制：** 同一用户5分钟内最多发布3条动态
- **敏感词过滤：** 发布前检查敏感词，标记异常内容

#### 6.5.2 状态管理
- **动态状态：** 1-正常、0-已删除、2-审核中
- **评论状态：** 1-正常、0-已删除
- **软删除：** 删除操作仅更新状态，不物理删除数据

#### 6.5.3 权限控制
- **用户端权限：** 登录用户可发布动态、点赞、评论
- **删除权限：** 用户只能删除自己的动态和评论
- **管理端权限：** 管理员可查看、删除所有内容，查看统计数据

## 7. 实现状态

### 7.1 已完成功能 ✅

#### 后端实现（xiaou-moment模块）
- ✅ 数据库表结构（moments、moment_likes、moment_comments）
- ✅ 用户端完整接口（发布、列表、删除、点赞、评论）
- ✅ 管理端完整接口（列表管理、批量删除、评论管理、统计）
- ✅ DTO结构设计（请求/响应对象）
- ✅ 业务规则验证（发布频率限制、内容验证）
- ✅ 分页查询支持
- ✅ 敏感词检测标记

#### 前端实现
- ✅ 用户端API封装（vue3-user-front/src/api/moment.js）
- ✅ 管理端API封装（vue3-admin-front/src/api/moment.js）
- ✅ 统一请求响应处理
- ✅ 分页组件支持

#### 核心特性
- ✅ 动态发布（文字+图片，最多9张）
- ✅ 动态列表（分页展示，包含点赞评论数）
- ✅ 点赞功能（防重复点赞）
- ✅ 评论功能（支持完整评论列表查询）
- ✅ 权限控制（用户只能删除自己的内容）
- ✅ 管理端批量操作
- ✅ 数据统计分析

### 7.2 技术实现亮点
- **接口设计**：用户端(/user/moments)和管理端(/admin/moments)分离
- **数据分页**：统一使用PageResult分页响应结构
- **状态管理**：软删除设计，支持审核状态
- **权限控制**：@RequireAdmin注解实现管理员权限控制
- **参数验证**：使用Jakarta Validation进行请求参数校验
- **业务规则**：发布频率限制、敏感词检测等业务逻辑

## 8. 风险与应对

### 8.1 技术风险
**风险：** 图片上传性能问题  
**应对：** 使用CDN加速，图片压缩处理

**风险：** 并发点赞数据一致性  
**应对：** 使用Redis分布式锁，乐观锁机制

### 8.2 业务风险
**风险：** 用户发布不当内容  
**应对：** 敏感词过滤，举报机制，人工审核

**风险：** 用户活跃度不高  
**应对：** 增加激励机制，推送提醒
