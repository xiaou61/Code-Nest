# 社区模块PRD-v1.1.0

> 历史说明：本文档中的 Coze AI 摘要描述属于旧版接入方式。当前社区摘要能力已迁移到统一 AI Runtime，Prompt 与结构化输出规范见 `docs/plans/2026-04-20-ai-prompt-governance.md`。

## 版本信息
- **版本号**: v1.1.0
- **发布日期**: 2025年10月
- **基础版本**: v1.0.0
- **主要更新**: 智能推荐 + 话题标签 + 性能优化 + AI摘要

---

## 1. 版本概述

### 1.1 升级背景
社区模块v1.0.0已上线并稳定运行,实现了帖子发布、评论互动、分类管理等基础功能。通过用户反馈和数据分析发现，当前版本存在以下待优化的问题：

- **内容发现难**：用户难以发现优质内容，缺少智能推荐机制
- **组织不清晰**：缺少话题标签体系，内容归类不够精细
- **交互体验弱**：评论功能较简单，缺少二级回复
- **性能待优化**：长列表加载较慢，缺少缓存优化
- **创作体验差**：缺少草稿保存，编辑中断内容丢失
- **个性化不足**：无法查看其他用户的个人主页和发帖历史

### 1.2 核心更新

#### 功能增强
- **热门帖子推荐**：智能推荐优质内容，提升内容曝光
- **话题标签系统**：支持多标签管理，精细化内容分类
- **二级评论回复**：支持评论的评论，增强互动体验
- **草稿保存功能**：自动保存草稿，避免内容丢失
- **用户个人主页**：查看用户资料和发帖历史
- **帖子AI摘要**：AI自动生成帖子摘要，提升阅读体验

#### 性能优化
- **Redis缓存优化**：热门帖子、用户信息等数据缓存
- **分页加载优化**：无限滚动+虚拟列表，提升加载速度
- **图片懒加载**：图片延迟加载，减少首屏时间
- **搜索性能提升**：全文索引优化，搜索速度提升

#### 用户体验
- **相对时间显示**：友好的时间展示（如"3分钟前"）
- **表情选择器**：支持emoji表情输入
- **Markdown编辑器增强**：实时预览、工具栏优化
- **响应式优化**：更好的移动端适配

### 1.3 技术集成
- **Redis缓存**: 7天TTL，热门帖子、用户信息缓存
- **Coze AI工作流**: 帖子摘要生成（可选功能）
- **全文索引**: MySQL全文索引，提升搜索性能
- **虚拟滚动**: 前端虚拟列表，支持长列表流畅滚动

---

## 2. v1.0.0问题分析

### 2.1 功能缺失问题

| 问题 | 影响 | 严重程度 |
|------|------|---------|
| 无热门推荐机制 | 优质内容曝光不足，用户粘性低 | 🔴 高 |
| 缺少话题标签 | 内容组织混乱，查找困难 | 🔴 高 |
| 评论仅支持一级 | 互动层级单一，讨论深度不够 | 🟡 中 |
| 无草稿保存 | 编辑中断内容丢失，用户体验差 | 🟡 中 |
| 无用户个人主页 | 无法了解其他用户，社交性不足 | 🟡 中 |
| 无AI辅助功能 | 创作和阅读体验待提升 | 🟢 低 |

### 2.2 性能问题

| 问题 | 影响 | 严重程度 |
|------|------|---------|
| 无缓存机制 | 热门数据频繁查询数据库，性能差 | 🔴 高 |
| 列表加载慢 | 超过100条帖子时卡顿明显 | 🟡 中 |
| 搜索速度慢 | Like查询性能差，用户体验不佳 | 🟡 中 |
| 图片加载慢 | 原图直接加载，首屏加载慢 | 🟢 低 |

### 2.3 用户体验问题

| 问题 | 影响 | 严重程度 |
|------|------|---------|
| 时间显示不友好 | 显示绝对时间，不够直观 | 🟡 中 |
| Markdown编辑器简单 | 创作体验不佳 | 🟡 中 |
| 无表情支持 | 表达受限，互动性弱 | 🟢 低 |
| 移动端适配一般 | 移动端体验待优化 | 🟢 低 |

---

## 3. v1.1.0优化方案

### 3.1 热门帖子推荐系统

#### 3.1.1 功能说明
- **位置**：社区首页顶部展示热门帖子轮播/卡片
- **展示形式**：横向轮播或网格卡片，展示3-5个热门帖子
- **更新频率**：每10分钟更新一次
- **推荐规则**：综合点赞、评论、收藏、浏览量等维度计算热度

#### 3.1.2 热度算法
```
热度分数 = 点赞数 × 3 + 评论数 × 5 + 收藏数 × 8 + 浏览数 × 0.1 - 时间衰减
时间衰减 = (当前时间 - 发布时间) / 24小时 × 10

筛选条件：
- 发布时间：72小时内
- 状态：正常（未下架、未删除）
- 热度分数：>= 30
- 数量限制：TOP 10
```

#### 3.1.3 数据结构设计

**Redis缓存设计**：
```
Key: community:hot:posts
Type: Sorted Set
Score: 热度分数
Value: 帖子ID
TTL: 10分钟
```

#### 3.1.4 接口设计

**获取热门帖子**
```
GET /community/posts/hot
参数：
{
  "limit": 5  // 获取数量，默认5
}

响应：
{
  "code": 200,
  "data": [
    {
      "id": 123,
      "title": "帖子标题",
      "content": "帖子内容摘要...",
      "authorName": "张三",
      "categoryName": "技术分享",
      "tags": ["Java", "Spring"],
      "likeCount": 45,
      "commentCount": 23,
      "collectCount": 18,
      "viewCount": 189,
      "hotScore": 285.5,
      "createTime": "2025-10-01 12:00:00"
    }
  ]
}
```

#### 3.1.5 前端展示

**热门帖子卡片**：
```
┌─────────────────────────────────────────────┐
│ 🔥 热门推荐                                  │
├─────────────────────────────────────────────┤
│ ┌──────────┬──────────┬──────────┐         │
│ │ [卡片1]  │ [卡片2]  │ [卡片3]  │         │
│ │ 标题     │ 标题     │ 标题     │         │
│ │ #标签    │ #标签    │ #标签    │         │
│ │ 👍45 💬23│ 👍38 💬15│ 👍52 💬30│         │
│ └──────────┴──────────┴──────────┘         │
└─────────────────────────────────────────────┘
```

### 3.2 话题标签系统

#### 3.2.1 功能说明
- **标签管理**：支持创建、编辑、删除标签
- **多标签绑定**：每个帖子可关联1-5个标签
- **标签筛选**：支持按标签筛选帖子
- **标签统计**：展示标签下的帖子数量
- **热门标签**：自动统计热门标签

#### 3.2.2 数据库设计

**标签表（community_tag）**：
```sql
CREATE TABLE community_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    description VARCHAR(200) COMMENT '标签描述',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    icon VARCHAR(50) COMMENT '标签图标',
    post_count INT DEFAULT 0 COMMENT '帖子数量',
    follow_count INT DEFAULT 0 COMMENT '关注数量',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name (name),
    INDEX idx_post_count (post_count DESC),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区标签表';
```

**帖子标签关联表（community_post_tag）**：
```sql
CREATE TABLE community_post_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_post_tag (post_id, tag_id),
    INDEX idx_post_id (post_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子标签关联表';
```

#### 3.2.3 接口设计

**标签管理接口**：
```
# 前台
GET  /community/tags                    # 获取启用的标签列表
GET  /community/tags/hot                # 获取热门标签（按帖子数排序）
GET  /community/tags/{id}/posts         # 获取标签下的帖子

# 后台
POST   /admin/community/tags            # 创建标签
PUT    /admin/community/tags/{id}       # 更新标签
DELETE /admin/community/tags/{id}       # 删除标签
POST   /admin/community/tags/list       # 标签列表（分页）
```

**帖子创建/编辑增加标签字段**：
```json
{
  "title": "帖子标题",
  "content": "帖子内容",
  "categoryId": 1,
  "tagIds": [1, 2, 3]  // 新增：标签ID数组
}
```

#### 3.2.4 前端展示

**标签选择器**（发帖时）：
```
┌─────────────────────────────────────┐
│ 选择标签（最多5个）                  │
│ ┌─────────────────────────────────┐ │
│ │ #Java  #Spring  #MySQL  #Redis  │ │
│ │ #Vue   #算法    #架构   #面试   │ │
│ └─────────────────────────────────┘ │
│ 已选择：#Java #Spring #MySQL        │
└─────────────────────────────────────┘
```

**帖子卡片展示标签**：
```
┌──────────────────────────────────┐
│ 标题：Spring Boot最佳实践        │
│ #Java #Spring #后端              │
│ 张三 · 3分钟前 · 技术分享        │
│ 👍 12  💬 5  ⭐ 3                │
└──────────────────────────────────┘
```

### 3.3 二级评论回复

#### 3.3.1 功能说明
- **评论层级**：支持一级评论和二级回复
- **回复展示**：展开/收起二级回复
- **回复通知**：被回复用户收到消息通知
- **@功能**：支持@提及用户

#### 3.3.2 数据库调整

**评论表增加字段**：
```sql
ALTER TABLE community_comment 
ADD COLUMN reply_to_id BIGINT COMMENT '回复的评论ID，NULL表示一级评论',
ADD COLUMN reply_to_user_id BIGINT COMMENT '回复的用户ID',
ADD COLUMN reply_to_user_name VARCHAR(50) COMMENT '回复的用户名',
ADD COLUMN reply_count INT DEFAULT 0 COMMENT '回复数量（仅一级评论有效）',
ADD INDEX idx_parent_id (parent_id),
ADD INDEX idx_reply_to_id (reply_to_id);
```

#### 3.3.3 接口调整

**评论列表接口增强**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1,
        "postId": 100,
        "content": "这篇文章写得很好！",
        "authorId": 10,
        "authorName": "张三",
        "likeCount": 5,
        "replyCount": 2,
        "createTime": "2025-10-01 12:00:00",
        "replies": [  // 二级回复（最多展示2条）
          {
            "id": 2,
            "content": "同意，学到了很多",
            "authorId": 11,
            "authorName": "李四",
            "replyToUserId": 10,
            "replyToUserName": "张三",
            "createTime": "2025-10-01 12:05:00"
          }
        ],
        "hasMoreReplies": true  // 是否有更多回复
      }
    ],
    "total": 50
  }
}
```

**新增接口**：
```
POST /community/comments/{id}/reply      # 回复评论
GET  /community/comments/{id}/replies    # 获取评论的回复列表（分页）
```

#### 3.3.4 前端展示

**评论区结构**：
```
┌──────────────────────────────────────────┐
│ 💬 评论 (5)                               │
├──────────────────────────────────────────┤
│ [头像] 张三  3分钟前                      │
│        这篇文章写得很好！                 │
│        👍 5  [回复]                       │
│   └─ [头像] 李四 回复 张三  2分钟前      │
│            同意，学到了很多               │
│            👍 2  [回复]                   │
│   └─ [头像] 王五 回复 张三  1分钟前      │
│            赞同                           │
│            👍 1  [回复]                   │
│        [展开更多2条回复 ▼]                │
├──────────────────────────────────────────┤
│ [头像] 赵六  5分钟前                      │
│        有疑问，能详细说说吗？             │
│        👍 3  [回复]                       │
└──────────────────────────────────────────┘
```

### 3.4 草稿保存功能

#### 3.4.1 功能说明
- **自动保存**：编辑5秒后自动保存到localStorage
- **草稿恢复**：打开编辑页面时提示恢复草稿
- **多草稿管理**：支持多个草稿（创建帖子、编辑帖子）
- **草稿清理**：发布成功后自动清理草稿

#### 3.4.2 实现方案

**前端LocalStorage设计**：
```javascript
// 草稿Key格式
const DRAFT_KEY = {
  CREATE: 'community_draft_create',      // 创建帖子草稿
  EDIT: 'community_draft_edit_{postId}'  // 编辑帖子草稿
}

// 草稿数据结构
{
  "title": "帖子标题",
  "content": "帖子内容",
  "categoryId": 1,
  "tagIds": [1, 2],
  "saveTime": "2025-10-01 12:00:00",
  "version": "1.1.0"
}
```

**自动保存逻辑**：
```javascript
// 使用防抖，5秒后保存
const saveDraft = debounce(() => {
  const draft = {
    title: form.title,
    content: form.content,
    categoryId: form.categoryId,
    tagIds: form.tagIds,
    saveTime: new Date().toISOString(),
    version: '1.1.0'
  }
  localStorage.setItem(DRAFT_KEY.CREATE, JSON.stringify(draft))
  ElMessage.success('草稿已自动保存')
}, 5000)
```

**草稿恢复提示**：
```
┌─────────────────────────────────────┐
│ 💡 检测到未发布的草稿                │
│                                     │
│ 保存时间：2025-10-01 12:00:00       │
│                                     │
│ [恢复草稿]  [放弃草稿]              │
└─────────────────────────────────────┘
```

### 3.5 用户个人主页

#### 3.5.1 功能说明
- **个人信息**：展示用户基本信息（头像、昵称、简介）
- **统计数据**：发帖数、获赞数、评论数、收藏数
- **帖子列表**：展示用户发布的所有帖子（分页）
- **活跃标签**：展示用户常用的标签
- **访问入口**：点击用户头像/昵称进入

#### 3.5.2 接口设计

**用户主页接口**：
```
GET /community/users/{userId}/profile

响应：
{
  "code": 200,
  "data": {
    "userId": 123,
    "userName": "张三",
    "avatar": "https://...",
    "bio": "热爱技术的程序员",
    "stats": {
      "postCount": 28,
      "likeCount": 156,
      "commentCount": 89,
      "collectCount": 45
    },
    "activeTags": [
      {"id": 1, "name": "Java", "count": 15},
      {"id": 2, "name": "Spring", "count": 12}
    ],
    "recentPosts": [...]  // 最近3篇帖子
  }
}
```

**用户帖子列表**：
```
POST /community/users/{userId}/posts
参数：
{
  "pageNum": 1,
  "pageSize": 10,
  "categoryId": null,  // 可选：按分类筛选
  "tagId": null        // 可选：按标签筛选
}
```

#### 3.5.3 前端展示

**用户主页布局**：
```
┌──────────────────────────────────────────┐
│ [返回]                                    │
├──────────────────────────────────────────┤
│ ┌───────┐                                 │
│ │ 头像  │  张三                            │
│ └───────┘  热爱技术的程序员               │
│                                           │
│ 📝 28 篇帖子  👍 156 获赞  💬 89 评论     │
│                                           │
│ 常用标签：#Java #Spring #MySQL            │
├──────────────────────────────────────────┤
│ 📋 Ta的帖子                               │
├──────────────────────────────────────────┤
│ [帖子卡片1]                               │
│ [帖子卡片2]                               │
│ [帖子卡片3]                               │
│ ...                                       │
└──────────────────────────────────────────┘
```

### 3.6 帖子AI摘要（可选功能）

#### 3.6.1 功能说明
- **AI摘要生成**：自动为长文帖子生成摘要
- **触发时机**：帖子内容超过500字时自动生成
- **展示位置**：帖子列表显示摘要，详情页显示完整内容
- **手动刷新**：支持重新生成摘要

#### 3.6.2 Coze工作流集成

**工作流配置**：

**输入变量**：
- `title` (string): 帖子标题
- `content` (string): 帖子完整内容

**输出变量**：
- `summary` (string): 帖子摘要（200字以内）
- `keywords` (array): 关键词列表（3-5个）

**提示词**：
```
你是一个专业的技术内容分析助手。请为以下技术社区帖子生成摘要和关键词：

标题：{{title}}
内容：{{content}}

任务要求：
1. 生成200字以内的摘要，提取核心技术点和关键信息
2. 提取3-5个技术关键词（如编程语言、框架、技术概念等）
3. 严格按照以下JSON格式输出：

{
  "summary": "摘要内容...",
  "keywords": ["关键词1", "关键词2", "关键词3"]
}

注意：摘要要简洁专业，关键词要准确反映技术主题。
```

**示例**：
```
输入：
title: "Spring Boot集成Redis实现分布式缓存"
content: "本文介绍如何在Spring Boot中集成Redis..."

输出：
{
  "summary": "本文介绍了在Spring Boot项目中集成Redis实现分布式缓存的完整流程，包括依赖配置、连接设置和使用方法，适合微服务架构性能优化。",
  "keywords": ["Spring Boot", "Redis", "分布式缓存", "微服务"]
}
```

#### 3.6.3 数据库设计

**帖子表增加字段**：
```sql
ALTER TABLE community_post
ADD COLUMN ai_summary TEXT COMMENT 'AI生成的摘要',
ADD COLUMN ai_keywords VARCHAR(200) COMMENT 'AI提取的关键词，逗号分隔',
ADD COLUMN ai_generate_time DATETIME COMMENT 'AI生成时间';
```

#### 3.6.4 接口设计

```
POST /community/posts/{id}/generate-summary  # 生成AI摘要（手动触发）
GET  /community/posts/{id}/summary           # 获取帖子摘要
```

#### 3.6.5 缓存策略

**Redis缓存**：
```
Key: community:post:summary:{postId}
Value: {"summary": "...", "keywords": [...]}
TTL: 30天
```

### 3.7 搜索功能增强

#### 3.7.1 全文索引优化

**添加全文索引**：
```sql
ALTER TABLE community_post
ADD FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram;
```

**搜索查询优化**：
```sql
SELECT * FROM community_post
WHERE MATCH(title, content) AGAINST('关键词' IN NATURAL LANGUAGE MODE)
AND status = 1
ORDER BY (
    likeCount * 3 + commentCount * 5 + collectCount * 8 + viewCount * 0.1
) DESC
LIMIT 20;
```

#### 3.7.2 搜索建议

**热门搜索词**：
```
Redis存储：
Key: community:hot:keywords
Type: Sorted Set
Score: 搜索次数
TTL: 7天
```

#### 3.7.3 搜索历史

**用户搜索历史**（前端LocalStorage）：
```javascript
{
  "userId": 123,
  "keywords": [
    {"keyword": "Spring Boot", "time": "2025-10-01 12:00:00"},
    {"keyword": "Redis", "time": "2025-10-01 11:30:00"}
  ]
}
```

### 3.8 性能优化方案

#### 3.8.1 Redis缓存策略

**缓存内容**：
| 缓存项 | Key格式 | TTL | 说明 |
|--------|---------|-----|------|
| 热门帖子列表 | community:hot:posts | 10分钟 | Sorted Set |
| 帖子详情 | community:post:{id} | 30分钟 | String |
| 用户信息 | community:user:{id} | 1小时 | Hash |
| 分类列表 | community:categories | 1天 | List |
| 标签列表 | community:tags | 1天 | List |
| 帖子点赞状态 | community:post:like:{userId} | 1小时 | Set |
| 帖子收藏状态 | community:post:collect:{userId} | 1小时 | Set |

**缓存更新策略**：
- **主动更新**：数据变更时立即更新缓存
- **被动失效**：TTL过期自动失效
- **穿透防护**：空值缓存，防止缓存穿透

#### 3.8.2 数据库优化

**索引优化**：
```sql
-- 帖子表
ALTER TABLE community_post
ADD INDEX idx_status_top_time (status, is_top DESC, create_time DESC),
ADD INDEX idx_category_status_time (category_id, status, create_time DESC),
ADD INDEX idx_author_status_time (author_id, status, create_time DESC),
ADD INDEX idx_hot_score (status, create_time, like_count, comment_count);

-- 评论表
ALTER TABLE community_comment
ADD INDEX idx_post_parent_time (post_id, parent_id, create_time DESC),
ADD INDEX idx_author_time (author_id, create_time DESC);

-- 点赞表
ALTER TABLE community_post_like
ADD INDEX idx_user_time (user_id, create_time DESC);

-- 收藏表
ALTER TABLE community_post_collect
ADD INDEX idx_user_time (user_id, create_time DESC);
```

**查询优化**：
- 避免SELECT *，只查询需要的字段
- 使用覆盖索引，减少回表
- 批量查询用户信息，减少N+1查询

#### 3.8.3 前端性能优化

**虚拟滚动**：
```vue
<template>
  <RecycleScroller
    :items="postList"
    :item-size="300"
    :buffer="600"
    key-field="id"
    class="scroller"
  >
    <template #default="{ item }">
      <PostCard :post="item" />
    </template>
  </RecycleScroller>
</template>
```

**图片懒加载**：
```vue
<el-image
  :src="post.coverImage"
  loading="lazy"
  :preview-src-list="[post.coverImage]"
/>
```

**无限滚动**：
```javascript
const handleScroll = () => {
  const scrollTop = document.documentElement.scrollTop
  const clientHeight = document.documentElement.clientHeight
  const scrollHeight = document.documentElement.scrollHeight
  
  if (scrollTop + clientHeight >= scrollHeight - 200 && !loading.value) {
    loadMorePosts()
  }
}
```

### 3.9 用户体验优化

#### 3.9.1 相对时间显示

**时间格式化规则**：
```javascript
const formatRelativeTime = (dateStr) => {
  const now = new Date()
  const date = new Date(dateStr)
  const diff = (now - date) / 1000  // 秒
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  
  // 超过7天显示具体日期
  return date.toLocaleDateString('zh-CN')
}
```

#### 3.9.2 表情选择器

**使用emoji-picker-element**：
```vue
<template>
  <emoji-picker
    @emoji-click="onEmojiClick"
    class="emoji-picker"
  />
</template>

<script setup>
const onEmojiClick = (event) => {
  const emoji = event.detail.unicode
  form.content += emoji
}
</script>
```

#### 3.9.3 Markdown编辑器增强

**功能增强**：
- 实时预览
- 工具栏（加粗、斜体、链接、图片、代码块等）
- 代码高亮
- 表格支持
- 快捷键支持

**推荐组件**：使用`v-md-editor`或`mavon-editor`

### 3.10 移动端适配优化

#### 3.10.1 响应式布局

**断点设计**：
```scss
// 移动端
@media (max-width: 768px) {
  .post-card {
    padding: 12px;
    margin: 10px;
  }
  
  .post-title {
    font-size: 16px;
  }
}

// 平板端
@media (min-width: 769px) and (max-width: 1199px) {
  .post-card {
    padding: 16px;
    margin: 15px;
  }
}

// 桌面端
@media (min-width: 1200px) {
  .post-card {
    padding: 20px;
    margin: 20px;
  }
}
```

#### 3.10.2 移动端交互优化

- **下拉刷新**：支持下拉刷新帖子列表
- **触摸优化**：增大点击区域（最小44px）
- **输入优化**：移动端弹出全屏编辑器
- **图片查看**：支持手势缩放

---

## 4. 数据库变更

### 4.1 新增表

**标签表（community_tag）**：
```sql
CREATE TABLE community_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    description VARCHAR(200) COMMENT '标签描述',
    color VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色',
    icon VARCHAR(50) COMMENT '标签图标',
    post_count INT DEFAULT 0 COMMENT '帖子数量',
    follow_count INT DEFAULT 0 COMMENT '关注数量',
    sort_order INT DEFAULT 0 COMMENT '排序顺序',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_name (name),
    INDEX idx_post_count (post_count DESC),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区标签表';
```

**帖子标签关联表（community_post_tag）**：
```sql
CREATE TABLE community_post_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_post_tag (post_id, tag_id),
    INDEX idx_post_id (post_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子标签关联表';
```

### 4.2 字段调整

**帖子表（community_post）**：
```sql
ALTER TABLE community_post
ADD COLUMN ai_summary TEXT COMMENT 'AI生成的摘要',
ADD COLUMN ai_keywords VARCHAR(200) COMMENT 'AI提取的关键词，逗号分隔',
ADD COLUMN ai_generate_time DATETIME COMMENT 'AI生成时间',
ADD FULLTEXT INDEX ft_title_content (title, content) WITH PARSER ngram;
```

**评论表（community_comment）**：
```sql
ALTER TABLE community_comment
ADD COLUMN reply_to_id BIGINT COMMENT '回复的评论ID',
ADD COLUMN reply_to_user_id BIGINT COMMENT '回复的用户ID',
ADD COLUMN reply_to_user_name VARCHAR(50) COMMENT '回复的用户名',
ADD COLUMN reply_count INT DEFAULT 0 COMMENT '回复数量（仅一级评论有效）',
ADD INDEX idx_reply_to_id (reply_to_id);
```

### 4.3 索引优化

```sql
-- 帖子表索引
ALTER TABLE community_post
ADD INDEX idx_status_top_time (status, is_top DESC, create_time DESC),
ADD INDEX idx_category_status_time (category_id, status, create_time DESC),
ADD INDEX idx_author_status_time (author_id, status, create_time DESC),
ADD INDEX idx_hot_score (status, create_time, like_count, comment_count);

-- 评论表索引
ALTER TABLE community_comment
ADD INDEX idx_post_parent_time (post_id, parent_id, create_time DESC);

-- 点赞表索引
ALTER TABLE community_post_like
ADD INDEX idx_user_time (user_id, create_time DESC);

-- 收藏表索引
ALTER TABLE community_post_collect
ADD INDEX idx_user_time (user_id, create_time DESC);
```

---

## 5. 接口调整

### 5.1 新增接口

#### 5.1.1 热门推荐
```
GET  /community/posts/hot                     # 获取热门帖子
```

#### 5.1.2 标签管理
```
# 前台
GET  /community/tags                          # 获取启用的标签列表
GET  /community/tags/hot                      # 获取热门标签
GET  /community/tags/{id}/posts               # 获取标签下的帖子

# 后台
POST   /admin/community/tags                  # 创建标签
PUT    /admin/community/tags/{id}             # 更新标签
DELETE /admin/community/tags/{id}             # 删除标签
POST   /admin/community/tags/list             # 标签列表（分页）
```

#### 5.1.3 二级评论
```
POST /community/comments/{id}/reply           # 回复评论
GET  /community/comments/{id}/replies         # 获取评论的回复列表
```

#### 5.1.4 用户主页
```
GET  /community/users/{userId}/profile        # 获取用户主页信息
POST /community/users/{userId}/posts          # 获取用户帖子列表
```

#### 5.1.5 AI摘要（可选）
```
POST /community/posts/{id}/generate-summary   # 生成AI摘要
GET  /community/posts/{id}/summary            # 获取帖子摘要
```

### 5.2 接口优化

**帖子列表接口增强**：
```json
{
  "pageNum": 1,
  "pageSize": 10,
  "categoryId": null,
  "tagId": null,        // 新增：按标签筛选
  "keyword": null,
  "sortBy": "hot"       // 新增：排序方式（hot-热度，time-时间）
}
```

**帖子响应增加字段**：
```json
{
  "id": 123,
  "title": "标题",
  "content": "内容",
  "summary": "AI生成的摘要",    // 新增
  "tags": [                      // 新增
    {"id": 1, "name": "Java", "color": "#409EFF"}
  ],
  "hotScore": 285.5,             // 新增：热度分数
  "categoryName": "技术分享",
  "authorId": 10,
  "authorName": "张三",
  "viewCount": 189,
  "likeCount": 45,
  "commentCount": 23,
  "collectCount": 18,
  "isLiked": true,
  "isCollected": false,
  "createTime": "2025-10-01 12:00:00"
}
```

---

## 6. 技术实现

### 6.1 后端技术栈

#### 6.1.1 新增依赖
```xml
<!-- pom.xml -->
<!-- Redis支持已有，无需新增 -->
<!-- Coze工作流调用（如需AI功能） -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.10.0</version>
</dependency>
```

#### 6.1.2 配置文件
```yaml
# application.yml
community:
  cache:
    hot-posts-ttl: 600          # 热门帖子缓存10分钟
    post-detail-ttl: 1800       # 帖子详情缓存30分钟
    user-info-ttl: 3600         # 用户信息缓存1小时
  hot:
    time-range: 72              # 热门帖子时间范围（小时）
    min-score: 30               # 最低热度分数
    limit: 10                   # 热门帖子数量
  ai:
    enabled: false              # 是否启用AI功能
    coze-workflow-id: ""        # Coze工作流ID
```

### 6.2 前端技术栈

#### 6.2.1 新增依赖
```json
{
  "dependencies": {
    "vue-virtual-scroller": "^2.0.0-beta.8",  // 虚拟滚动
    "emoji-picker-element": "^1.18.0",         // 表情选择器
    "@kangc/v-md-editor": "^2.3.15",          // Markdown编辑器
    "dayjs": "^1.11.10",                       // 时间处理
    "lodash-es": "^4.17.21"                    // 工具函数
  }
}
```

#### 6.2.2 组件封装

**新增组件**：
```
src/views/community/
├── components/
│   ├── HotPosts.vue            # 热门帖子展示
│   ├── TagSelector.vue         # 标签选择器
│   ├── CommentTree.vue         # 评论树（支持二级回复）
│   ├── UserProfile.vue         # 用户主页
│   ├── DraftManager.vue        # 草稿管理
│   └── MarkdownEditor.vue      # Markdown编辑器封装
```

---

## 7. 实施计划

### 7.1 开发阶段

| 阶段 | 内容 | 预计时间 | 优先级 |
|------|------|----------|--------|
| 第一阶段 | 热门推荐+话题标签 | 3天 | P0 |
| 第二阶段 | 二级评论+用户主页 | 3天 | P0 |
| 第三阶段 | 性能优化+缓存 | 2天 | P0 |
| 第四阶段 | 草稿保存+体验优化 | 2天 | P1 |
| 第五阶段 | AI摘要（可选） | 2天 | P2 |
| 测试上线 | 测试+部署 | 1天 | - |

**总计**：13天

### 7.2 测试要点

#### 7.2.1 功能测试
- ✅ 热门帖子推荐准确性
- ✅ 标签管理和筛选功能
- ✅ 二级评论显示和回复
- ✅ 草稿保存和恢复
- ✅ 用户主页数据展示
- ✅ AI摘要生成（可选）

#### 7.2.2 性能测试
- ✅ 缓存命中率 >= 80%
- ✅ 热门帖子查询 < 100ms
- ✅ 帖子列表加载 < 500ms
- ✅ 长列表（1000+）流畅滚动

#### 7.2.3 兼容性测试
- ✅ Chrome 80+
- ✅ Firefox 75+
- ✅ Safari 13+
- ✅ Edge 80+
- ✅ 移动端浏览器

---

## 8. 风险评估与应对

### 8.1 技术风险

| 风险 | 影响程度 | 应对策略 |
|------|----------|----------|
| Redis缓存失效 | 中 | 数据库兜底，自动重建缓存 |
| 全文索引性能 | 中 | 分表分库，ElasticSearch方案备选 |
| AI调用失败 | 低 | 熔断降级，不影响核心功能 |
| 虚拟滚动兼容性 | 低 | 充分测试，准备降级方案 |

### 8.2 业务风险

| 风险 | 影响程度 | 应对策略 |
|------|----------|----------|
| 热门算法不准确 | 中 | A/B测试，数据驱动优化 |
| 标签体系混乱 | 中 | 运营引导，标签审核机制 |
| 二级评论滥用 | 低 | 敏感词过滤，举报机制 |

---

## 9. 成功指标

### 9.1 性能指标
- 帖子列表首屏加载时间 < **500ms**
- 热门帖子查询响应时间 < **100ms**
- Redis缓存命中率 >= **80%**
- 长列表（1000+）滚动帧率 >= **55fps**

### 9.2 用户体验指标
- 用户平均停留时间增加 **30%**
- 帖子发布量增加 **25%**
- 互动率（点赞+评论+收藏）提升 **40%**
- 用户日活跃度提升 **20%**

### 9.3 功能使用率
- 热门帖子点击率 >= **35%**
- 标签使用率 >= **60%**
- 二级评论占比 >= **30%**
- 草稿恢复率 >= **40%**
- 用户主页访问率 >= **25%**


**文档版本**：v1.1.0  
**更新日期**：2025年10月3日  
**维护团队**：Code-Nest开发团队

