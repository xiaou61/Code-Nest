# 学习小组模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
独自学习往往难以坚持，缺乏动力和反馈。研究表明，组队学习可以显著提高学习效率和完成率。Code Nest 作为开发者成长平台，需要提供一个让用户能够组队学习、互相监督、共同进步的功能模块，帮助用户建立学习社交圈，提升学习粘性和平台活跃度。

### 💡 核心价值
- **社交学习**: 打破独自学习的孤独感，建立学习伙伴关系
- **互相监督**: 组内成员互相监督打卡，提高学习自律性
- **竞争激励**: 组内排行榜激发良性竞争，提升学习动力
- **资源共享**: 组内分享学习资料、经验心得，加速成长
- **目标驱动**: 共同目标让学习更有方向和意义

### 🏠 模块定位
作为独立的 `xiaou-team` 模块，在用户端 `vue3-user-front` 提供完整的组队学习体验。该模块与以下模块存在关联：
- **xiaou-interview**: 组队刷题、题目讨论
- **xiaou-plan**: 小组打卡与个人计划联动
- **xiaou-points**: 小组活动获得积分奖励
- **xiaou-notification**: 小组消息通知
- **xiaou-chat**: 小组内部聊天（可选扩展）
- **xiaou-flashcard**: 小组共享闪卡学习

## 🚀 功能需求

### 1. 小组管理

#### 1.1 创建小组
- **基本信息**:
  - 小组名称（2-20字符）
  - 小组简介（最多200字）
  - 小组头像（可上传或选择预设）
  - 小组标签（最多5个，如：Java、前端、秋招、转行等）
- **小组类型**:
  - 🎯 目标型：为特定目标组建（如：秋招备战、转行前端）
  - 📚 学习型：长期学习交流（如：Java进阶、算法修炼）
  - 🏃 打卡型：互相监督打卡（如：百日刷题、早起计划）
- **人数设置**:
  - 最小人数：2人
  - 最大人数：2-50人可选
  - 达到上限后自动关闭申请
- **加入方式**:
  - 公开加入：任何人可直接加入
  - 申请加入：需组长审批
  - 邀请加入：仅限邀请码加入
- **小组目标**（可选）:
  - 目标描述：如"30天刷完LeetCode热题100"
  - 目标周期：开始日期 - 结束日期
  - 每日任务量：如"每天3道题"

#### 1.2 小组发现/广场
- **推荐小组**: 根据用户学习偏好智能推荐
- **热门小组**: 按活跃度、人数排序的热门小组
- **最新小组**: 按创建时间排序
- **分类筛选**: 按小组类型、标签筛选
- **搜索功能**: 按名称、标签搜索小组
- **小组卡片展示**:
  - 小组头像、名称、简介
  - 成员数/上限
  - 活跃度指标（7天打卡率）
  - 小组标签
  - 组长信息

#### 1.3 小组详情
- **基本信息展示**: 名称、简介、标签、创建时间
- **成员列表**: 头像墙、成员角色（组长/管理员/成员）
- **小组目标**: 目标描述、进度展示
- **小组动态**: 最近的打卡、讨论等活动
- **数据统计**: 总打卡次数、平均完成率、活跃天数
- **操作按钮**: 申请加入/退出小组/邀请好友

#### 1.4 我的小组
- **已加入小组列表**: 展示所有已加入的小组
- **我创建的小组**: 展示自己创建的小组
- **申请记录**: 待审核的加入申请
- **邀请记录**: 收到的小组邀请

### 2. 成员管理

#### 2.1 角色体系
- **组长**: 创建者，拥有最高权限
  - 解散小组
  - 转让组长
  - 设置/取消管理员
  - 修改小组信息
  - 审批加入申请
  - 移除成员
- **管理员**: 组长指定，协助管理
  - 审批加入申请
  - 移除普通成员
  - 发布小组公告
- **成员**: 普通参与者
  - 参与打卡
  - 发布讨论
  - 查看排行榜

#### 2.2 成员操作
- **申请加入**: 填写申请理由，等待审批
- **邀请成员**: 生成邀请链接/邀请码
- **退出小组**: 普通成员可自由退出
- **移除成员**: 组长/管理员可移除成员（需确认）
- **成员禁言**: 临时禁止成员发言（管理员功能）

### 3. 小组打卡

#### 3.1 打卡任务
- **创建打卡任务**（组长/管理员）:
  - 任务名称：如"每日刷题"
  - 任务类型：刷题/学习时长/阅读/自定义
  - 目标数量：如3道题、2小时
  - 打卡周期：每日/工作日/自定义
  - 开始结束日期
  - 是否必须附带内容（文字/图片）
- **任务列表**: 展示小组当前进行中的打卡任务
- **任务详情**: 任务规则、参与人数、完成情况

#### 3.2 执行打卡
- **今日打卡**: 展示今日需要完成的任务
- **打卡表单**:
  - 完成数量（如刷了几道题）
  - 学习内容（文字描述）
  - 学习截图（可选）
  - 心得感悟（可选）
  - 关联题目（如关联面试题ID）
- **打卡状态**: 未打卡/已打卡/补卡
- **补卡机制**: 允许补打最近3天的卡（每月限5次）

#### 3.3 打卡展示
- **打卡动态流**: 时间线展示成员打卡记录
- **点赞功能**: 为他人打卡点赞鼓励
- **评论功能**: 评论他人打卡内容
- **打卡日历**: 日历视图展示打卡情况
- **打卡热力图**: GitHub风格热力图展示

### 4. 小组排行榜

#### 4.1 成员排行
- **本周排行**: 本周学习数据排名
- **本月排行**: 本月累计数据排名
- **总榜**: 加入以来累计数据
- **排行维度**:
  - 打卡天数
  - 刷题数量
  - 学习时长
  - 连续打卡天数
  - 获得点赞数
- **排行展示**: 前三名特殊样式，自己排名高亮

#### 4.2 小组排行（全平台）
- **活跃小组榜**: 按小组活跃度排名
- **打卡率榜**: 按成员平均打卡率排名
- **成长榜**: 按成员进步幅度排名
- **人气榜**: 按成员数量排名

### 5. 小组讨论

#### 5.1 讨论区
- **发布讨论**: 标题+内容+图片
- **讨论分类**:
  - 📢 公告（仅管理员）
  - ❓ 问题求助
  - 📝 学习笔记
  - 💡 经验分享
  - 💬 闲聊灌水
- **讨论列表**: 按分类、时间筛选
- **置顶功能**: 管理员可置顶重要讨论
- **精华功能**: 管理员可设为精华

#### 5.2 互动功能
- **评论回复**: 支持多级回复
- **点赞**: 讨论和评论点赞
- **收藏**: 收藏有价值的讨论
- **@提及**: @某个成员

### 6. 组队刷题（特色功能）

#### 6.1 同步刷题
- **创建刷题房间**: 选择题单，设置时间
- **实时进度**: 显示每个成员的做题进度
- **答题状态**: 已完成/进行中/未开始
- **用时对比**: 每道题的用时对比

#### 6.2 题目讨论
- **题目关联讨论**: 在特定题目下发起讨论
- **解法分享**: 分享自己的解题思路
- **代码互评**: 互相查看和评价代码

### 7. 数据统计

#### 7.1 小组统计
- **概览数据**:
  - 总成员数
  - 活跃成员数（7天内有打卡）
  - 总打卡次数
  - 平均打卡率
  - 累计学习时长
- **趋势图表**:
  - 每日打卡人数趋势
  - 每周活跃度趋势
  - 成员成长曲线

#### 7.2 个人在组内统计
- **我的排名**: 各维度在组内排名
- **我的贡献**: 发起讨论数、帮助他人次数
- **成长曲线**: 加入小组后的学习数据变化

### 8. 消息通知

#### 8.1 通知类型
- **申请通知**: 新成员申请加入
- **审批通知**: 申请被通过/拒绝
- **邀请通知**: 被邀请加入小组
- **打卡提醒**: 今日打卡提醒
- **排名变化**: 排名上升/下降通知
- **小组公告**: 管理员发布的公告
- **@提及**: 被@的通知
- **点赞评论**: 打卡/讨论被点赞评论

#### 8.2 通知设置
- **免打扰**: 可设置特定小组免打扰
- **通知频率**: 实时/汇总（每日一次）

### 9. 积分激励

#### 9.1 积分获取
- **创建小组**: +50积分（每人限3个）
- **每日打卡**: +5积分
- **连续打卡7天**: +30积分
- **帮助他人**: +10积分（回答被采纳）
- **优质讨论**: +20积分（被设为精华）
- **排行榜周冠军**: +100积分

#### 9.2 小组积分池
- **贡献积分**: 成员可向小组积分池贡献
- **积分奖励**: 组长可用积分池奖励优秀成员

## 🔧 技术实现方案

### 1. 数据库设计

#### 1.1 小组表（study_team）
```sql
CREATE TABLE study_team (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '小组ID',
    team_name VARCHAR(50) NOT NULL COMMENT '小组名称',
    team_desc VARCHAR(500) COMMENT '小组简介',
    team_avatar VARCHAR(255) COMMENT '小组头像',
    team_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1目标型 2学习型 3打卡型',
    tags VARCHAR(255) COMMENT '标签，逗号分隔',
    max_members INT NOT NULL DEFAULT 20 COMMENT '最大成员数',
    current_members INT NOT NULL DEFAULT 1 COMMENT '当前成员数',
    join_type TINYINT NOT NULL DEFAULT 1 COMMENT '加入方式：1公开 2申请 3邀请',
    invite_code VARCHAR(20) COMMENT '邀请码',
    
    -- 小组目标
    goal_title VARCHAR(100) COMMENT '目标标题',
    goal_desc VARCHAR(500) COMMENT '目标描述',
    goal_start_date DATE COMMENT '目标开始日期',
    goal_end_date DATE COMMENT '目标结束日期',
    daily_target INT COMMENT '每日目标量',
    
    -- 统计数据
    total_checkins INT DEFAULT 0 COMMENT '总打卡次数',
    total_discussions INT DEFAULT 0 COMMENT '总讨论数',
    active_days INT DEFAULT 0 COMMENT '活跃天数',
    
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已解散 1正常 2已满员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_invite_code (invite_code),
    INDEX idx_creator_id (creator_id),
    INDEX idx_team_type (team_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习小组表';
```

#### 1.2 小组成员表（study_team_member）
```sql
CREATE TABLE study_team_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role TINYINT NOT NULL DEFAULT 3 COMMENT '角色：1组长 2管理员 3成员',
    nickname VARCHAR(50) COMMENT '组内昵称',
    join_reason VARCHAR(200) COMMENT '加入理由',
    
    -- 个人统计
    total_checkins INT DEFAULT 0 COMMENT '总打卡次数',
    current_streak INT DEFAULT 0 COMMENT '当前连续打卡',
    max_streak INT DEFAULT 0 COMMENT '最长连续打卡',
    total_likes_received INT DEFAULT 0 COMMENT '获得点赞数',
    contribution_points INT DEFAULT 0 COMMENT '贡献积分',
    
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已退出 1正常 2禁言中',
    mute_end_time DATETIME COMMENT '禁言结束时间',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    last_active_time DATETIME COMMENT '最后活跃时间',
    
    UNIQUE KEY uk_team_user (team_id, user_id),
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role (role),
    INDEX idx_join_time (join_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组成员表';
```

#### 1.3 小组申请表（study_team_application）
```sql
CREATE TABLE study_team_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    apply_reason VARCHAR(200) COMMENT '申请理由',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1已通过 2已拒绝 3已取消',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_time DATETIME COMMENT '审核时间',
    reject_reason VARCHAR(200) COMMENT '拒绝原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组申请表';
```

#### 1.4 打卡任务表（study_team_task）
```sql
CREATE TABLE study_team_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    task_name VARCHAR(100) NOT NULL COMMENT '任务名称',
    task_desc VARCHAR(500) COMMENT '任务描述',
    task_type TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1刷题 2学习时长 3阅读 4自定义',
    target_value INT NOT NULL COMMENT '目标数量',
    target_unit VARCHAR(20) COMMENT '目标单位',
    repeat_type TINYINT NOT NULL DEFAULT 1 COMMENT '重复：1每日 2工作日 3自定义',
    repeat_days VARCHAR(20) COMMENT '自定义重复日',
    require_content TINYINT DEFAULT 0 COMMENT '是否必须附带内容',
    require_image TINYINT DEFAULT 0 COMMENT '是否必须附带图片',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    creator_id BIGINT NOT NULL COMMENT '创建人ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0已结束 1进行中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_team_id (team_id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组打卡任务表';
```

#### 1.5 打卡记录表（study_team_checkin）
```sql
CREATE TABLE study_team_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '打卡ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    complete_value INT NOT NULL COMMENT '完成数量',
    content VARCHAR(1000) COMMENT '学习内容',
    images VARCHAR(2000) COMMENT '图片URL，逗号分隔',
    feeling VARCHAR(500) COMMENT '心得感悟',
    related_question_ids VARCHAR(500) COMMENT '关联题目ID',
    is_supplement TINYINT DEFAULT 0 COMMENT '是否补卡',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '打卡时间',
    
    UNIQUE KEY uk_task_user_date (task_id, user_id, checkin_date),
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id),
    INDEX idx_checkin_date (checkin_date),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组打卡记录表';
```

#### 1.6 打卡点赞表（study_team_checkin_like）
```sql
CREATE TABLE study_team_checkin_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    checkin_id BIGINT NOT NULL COMMENT '打卡ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    
    UNIQUE KEY uk_checkin_user (checkin_id, user_id),
    INDEX idx_checkin_id (checkin_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡点赞表';
```

#### 1.7 打卡评论表（study_team_checkin_comment）
```sql
CREATE TABLE study_team_checkin_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    checkin_id BIGINT NOT NULL COMMENT '打卡ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID',
    reply_user_id BIGINT COMMENT '回复用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    status TINYINT DEFAULT 1 COMMENT '状态：0已删除 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    
    INDEX idx_checkin_id (checkin_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡评论表';
```

#### 1.8 小组讨论表（study_team_discussion）
```sql
CREATE TABLE study_team_discussion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '讨论ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    category TINYINT NOT NULL DEFAULT 5 COMMENT '分类：1公告 2问题 3笔记 4经验 5闲聊',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    images VARCHAR(2000) COMMENT '图片URL',
    is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶',
    is_essence TINYINT DEFAULT 0 COMMENT '是否精华',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    status TINYINT DEFAULT 1 COMMENT '状态：0已删除 1正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组讨论表';
```

#### 1.9 小组每日统计表（study_team_daily_stats）
```sql
CREATE TABLE study_team_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '统计ID',
    team_id BIGINT NOT NULL COMMENT '小组ID',
    stat_date DATE NOT NULL COMMENT '统计日期',
    checkin_count INT DEFAULT 0 COMMENT '打卡人数',
    member_count INT DEFAULT 0 COMMENT '当日成员数',
    checkin_rate DECIMAL(5,2) DEFAULT 0 COMMENT '打卡率',
    discussion_count INT DEFAULT 0 COMMENT '讨论数',
    new_member_count INT DEFAULT 0 COMMENT '新加入成员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    UNIQUE KEY uk_team_date (team_id, stat_date),
    INDEX idx_team_id (team_id),
    INDEX idx_stat_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小组每日统计表';
```

### 2. 后端接口设计

#### 2.1 小组管理接口（/team）
```
POST   /team/create                    # 创建小组
PUT    /team/{id}                      # 更新小组信息
DELETE /team/{id}                      # 解散小组
GET    /team/{id}                      # 获取小组详情
POST   /team/list                      # 获取小组列表（广场）
GET    /team/my                        # 获取我的小组列表
GET    /team/recommend                 # 获取推荐小组
POST   /team/{id}/join                 # 申请加入小组
POST   /team/{id}/quit                 # 退出小组
GET    /team/{id}/invite-code          # 获取邀请码
POST   /team/join-by-code              # 通过邀请码加入
PUT    /team/{id}/transfer             # 转让组长
```

#### 2.2 成员管理接口（/team/member）
```
GET    /team/{id}/members              # 获取成员列表
PUT    /team/{id}/member/{userId}/role # 设置成员角色
DELETE /team/{id}/member/{userId}      # 移除成员
POST   /team/{id}/member/{userId}/mute # 禁言成员
DELETE /team/{id}/member/{userId}/mute # 解除禁言
GET    /team/{id}/applications         # 获取申请列表
POST   /team/{id}/application/{appId}/approve  # 审批申请
POST   /team/{id}/application/{appId}/reject   # 拒绝申请
```

#### 2.3 打卡任务接口（/team/task）
```
POST   /team/{id}/task                 # 创建打卡任务
PUT    /team/{id}/task/{taskId}        # 更新任务
DELETE /team/{id}/task/{taskId}        # 删除任务
GET    /team/{id}/tasks                # 获取任务列表
GET    /team/{id}/task/{taskId}        # 获取任务详情
```

#### 2.4 打卡接口（/team/checkin）
```
POST   /team/{id}/checkin              # 执行打卡
GET    /team/{id}/checkin/today        # 获取今日打卡状态
GET    /team/{id}/checkin/list         # 获取打卡动态流
GET    /team/{id}/checkin/calendar     # 获取打卡日历
POST   /team/{id}/checkin/{checkinId}/like    # 点赞打卡
DELETE /team/{id}/checkin/{checkinId}/like    # 取消点赞
POST   /team/{id}/checkin/{checkinId}/comment # 评论打卡
```

#### 2.5 排行榜接口（/team/rank）
```
GET    /team/{id}/rank/weekly          # 获取周排行
GET    /team/{id}/rank/monthly         # 获取月排行
GET    /team/{id}/rank/total           # 获取总排行
GET    /team/rank/teams                # 获取小组排行榜（全平台）
```

#### 2.6 讨论接口（/team/discussion）
```
POST   /team/{id}/discussion           # 发布讨论
PUT    /team/{id}/discussion/{did}     # 编辑讨论
DELETE /team/{id}/discussion/{did}     # 删除讨论
GET    /team/{id}/discussions          # 获取讨论列表
GET    /team/{id}/discussion/{did}     # 获取讨论详情
PUT    /team/{id}/discussion/{did}/pin # 置顶/取消置顶
PUT    /team/{id}/discussion/{did}/essence # 设为/取消精华
POST   /team/{id}/discussion/{did}/like    # 点赞
POST   /team/{id}/discussion/{did}/comment # 评论
```

#### 2.7 统计接口（/team/stats）
```
GET    /team/{id}/stats                # 获取小组统计
GET    /team/{id}/stats/trend          # 获取趋势数据
GET    /team/{id}/stats/my             # 获取我在组内的统计
```

### 3. 前端页面设计

#### 3.1 小组广场页面
```
┌──────────────────────────────────────────────────────┐
│  🏠 学习小组                           [+ 创建小组]  │
├──────────────────────────────────────────────────────┤
│  [热门] [最新] [我的]     🔍 搜索小组...             │
├──────────────────────────────────────────────────────┤
│  标签筛选: [全部] [Java] [前端] [算法] [秋招] ...    │
├──────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │ 🖼️              │  │ 🖼️              │           │
│  │ Java面试突击营   │  │ 前端进阶之路     │           │
│  │ 30天冲刺秋招... │  │ 一起学习React.. │           │
│  │ 👥 45/50  🔥98% │  │ 👥 23/30  🔥95% │           │
│  │ [Java][面试]    │  │ [前端][React]   │           │
│  │ 组长: 小明      │  │ 组长: 小红      │           │
│  │      [申请加入] │  │      [申请加入] │           │
│  └─────────────────┘  └─────────────────┘           │
│                                                      │
│  ┌─────────────────┐  ┌─────────────────┐           │
│  │ ...             │  │ ...             │           │
│  └─────────────────┘  └─────────────────┘           │
├──────────────────────────────────────────────────────┤
│  < 1  2  3  4  5  ... 10 >                          │
└──────────────────────────────────────────────────────┘
```

#### 3.2 小组详情页面
```
┌──────────────────────────────────────────────────────┐
│  ← 返回                                              │
├──────────────────────────────────────────────────────┤
│  ┌─────┐                                             │
│  │ 🖼️ │  Java面试突击营                             │
│  └─────┘  30天冲刺秋招，每天3道算法题               │
│           👥 45/50成员  📅 创建于2025-12-01          │
│           标签: [Java] [面试] [算法]                 │
├──────────────────────────────────────────────────────┤
│  [打卡] [讨论] [排行] [成员] [设置]                  │
├──────────────────────────────────────────────────────┤
│  📊 小组目标                           进度: 60%     │
│  ████████████░░░░░░░░ 18/30天                        │
│  每日目标: 3道算法题                                 │
├──────────────────────────────────────────────────────┤
│  📈 今日打卡 (32/45人已打卡)                         │
│  ┌──────────────────────────────────────────┐       │
│  │ 🧑 小明  刚刚                             │       │
│  │ 完成3道题：两数之和、三数之和、最长子串   │       │
│  │ 今天状态不错，继续加油！                  │       │
│  │ ❤️ 12  💬 3                    [查看详情] │       │
│  └──────────────────────────────────────────┘       │
│  ┌──────────────────────────────────────────┐       │
│  │ 🧑 小红  30分钟前                         │       │
│  │ 完成2道题：反转链表、合并链表             │       │
│  │ ❤️ 8   💬 1                               │       │
│  └──────────────────────────────────────────┘       │
├──────────────────────────────────────────────────────┤
│  [我要打卡]                                          │
└──────────────────────────────────────────────────────┘
```

#### 3.3 打卡页面
```
┌──────────────────────────────────────────────────────┐
│  ← 返回           今日打卡                           │
├──────────────────────────────────────────────────────┤
│  📋 任务: 每日刷题                                   │
│  🎯 目标: 3道算法题                                  │
├──────────────────────────────────────────────────────┤
│  完成数量:                                           │
│  ┌────────────────────────────────────────┐         │
│  │  [-]     3     [+]                     │         │
│  └────────────────────────────────────────┘         │
├──────────────────────────────────────────────────────┤
│  学习内容:                                           │
│  ┌────────────────────────────────────────┐         │
│  │ 今天做了两数之和、三数之和...          │         │
│  │                                        │         │
│  └────────────────────────────────────────┘         │
├──────────────────────────────────────────────────────┤
│  心得感悟:                                           │
│  ┌────────────────────────────────────────┐         │
│  │ 双指针技巧很实用，需要多练习...        │         │
│  └────────────────────────────────────────┘         │
├──────────────────────────────────────────────────────┤
│  上传截图: [+ 添加图片]                              │
├──────────────────────────────────────────────────────┤
│  关联题目: [选择题目]                                │
│  已选: 两数之和、三数之和                            │
├──────────────────────────────────────────────────────┤
│              [提交打卡]                              │
└──────────────────────────────────────────────────────┘
```

#### 3.4 排行榜页面
```
┌──────────────────────────────────────────────────────┐
│  🏆 小组排行榜                                       │
├──────────────────────────────────────────────────────┤
│  [本周] [本月] [总榜]     维度: [打卡天数 ▼]         │
├──────────────────────────────────────────────────────┤
│  🥇 小明     打卡28天  连续14天  刷题89道            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━         │
│  🥈 小红     打卡26天  连续10天  刷题76道            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━         │
│  🥉 小李     打卡25天  连续8天   刷题72道            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━         │
│  4  小王     打卡23天  连续5天   刷题65道            │
│  5  小张     打卡22天  连续7天   刷题60道            │
│  ...                                                 │
├──────────────────────────────────────────────────────┤
│  📍 我的排名                                         │
│  ┌──────────────────────────────────────────┐       │
│  │  #8  你      打卡18天  连续3天  刷题45道  │       │
│  │  距离上一名还差2天打卡                    │       │
│  └──────────────────────────────────────────┘       │
└──────────────────────────────────────────────────────┘
```

## ✅ 实现范围

### 第一期功能（v1.0.0）
- ✅ **小组CRUD**: 创建、编辑、解散小组
- ✅ **小组广场**: 小组列表、搜索、筛选
- ✅ **成员管理**: 加入、退出、角色管理
- ✅ **打卡功能**: 创建任务、执行打卡、打卡展示
- ✅ **基础排行**: 组内成员排行榜
- ✅ **消息通知**: 基础的加入、打卡通知

### 第二期功能（v1.1.0）
- 🔲 **讨论功能**: 小组讨论区完整实现
- 🔲 **邀请机制**: 邀请码、邀请链接
- 🔲 **补卡功能**: 补打卡机制
- 🔲 **数据统计**: 完整的统计报表和图表
- 🔲 **小组排行**: 全平台小组排行榜

### 第三期功能（v1.2.0）
- 🔲 **组队刷题**: 同步刷题房间
- 🔲 **积分激励**: 小组积分池、奖励机制
- 🔲 **小组聊天**: 与IM模块打通，小组内实时聊天
- 🔲 **成就系统**: 小组成就、个人在组内成就

## 📝 附录

### A. 小组类型枚举
```java
public enum TeamType {
    GOAL(1, "目标型", "🎯"),
    STUDY(2, "学习型", "📚"),
    CHECKIN(3, "打卡型", "🏃");
}
```

### B. 成员角色枚举
```java
public enum MemberRole {
    LEADER(1, "组长"),
    ADMIN(2, "管理员"),
    MEMBER(3, "成员");
}
```

### C. 加入方式枚举
```java
public enum JoinType {
    PUBLIC(1, "公开加入"),
    APPLY(2, "申请加入"),
    INVITE(3, "邀请加入");
}
```

### D. 积分规则
```java
public class TeamPointsRule {
    // 创建小组
    public static final int CREATE_TEAM = 50;
    // 每日打卡
    public static final int DAILY_CHECKIN = 5;
    // 连续打卡7天
    public static final int STREAK_7_DAYS = 30;
    // 帮助他人（回答被采纳）
    public static final int HELP_OTHERS = 10;
    // 优质讨论（被设为精华）
    public static final int ESSENCE_DISCUSSION = 20;
    // 周冠军
    public static final int WEEKLY_CHAMPION = 100;
}
```

### E. 通知模板
```
【申请通知】
标题：📨 收到新的加入申请
内容：用户「{用户名}」申请加入小组「{小组名}」，申请理由：{理由}

【审批通过】
标题：🎉 恭喜你加入「{小组名}」
内容：你的加入申请已通过，快去小组打卡吧！

【打卡提醒】
标题：⏰ 今日打卡提醒
内容：小组「{小组名}」的任务「{任务名}」还未完成，别忘了打卡哦！

【排名变化】
标题：📈 排名上升通知
内容：你在小组「{小组名}」的排名上升到第{排名}名，继续加油！

【被@提及】
标题：💬 {用户名} 在讨论中提到了你
内容："{讨论内容前50字}..."
```

### F. 业务规则
1. **创建限制**: 每个用户最多创建3个小组
2. **加入限制**: 每个用户最多加入10个小组
3. **补卡限制**: 每月最多补卡5次，只能补最近3天
4. **禁言限制**: 禁言时长最长7天
5. **解散规则**: 小组成员超过10人时不可直接解散，需先移除成员
6. **转让规则**: 组长转让后降为普通成员
7. **退出规则**: 组长不能直接退出，需先转让组长
