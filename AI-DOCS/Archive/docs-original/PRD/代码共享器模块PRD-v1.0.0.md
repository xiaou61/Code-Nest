# 代码共享器模块 PRD v1.0.0

## 📋 项目概述

### 🎯 项目背景
为了提升程序员的代码分享和学习体验,Code-Nest平台需要提供一个在线代码编辑和分享工具。该模块类似于CodePen,允许用户在浏览器中直接编写HTML、CSS、JavaScript代码,并实时预览效果,同时支持代码作品的分享、浏览和互动,打造一个活跃的前端开发者社区。

### 💡 核心价值
- **在线编辑**: 无需本地环境,浏览器中直接编写代码并实时预览
- **即时分享**: 一键分享代码作品,展示个人技术能力
- **学习交流**: 浏览他人优秀作品,学习前端开发技巧
- **代码收藏**: 收藏优秀代码片段,建立个人代码库
- **社区互动**: 点赞、评论、收藏,构建活跃的开发者社区
- **模板库**: 丰富的代码模板,快速开始项目开发
- **创作激励**: 创建作品奖励10积分,鼓励用户持续创作
- **知识变现**: 作者可设置作品为付费,通过优质内容获得积分收益

### 🎨 产品定位
- **目标用户**: 前端开发者、代码学习者、技术分享者
- **使用场景**: 代码练习、效果演示、技术分享、Bug复现、面试展示
- **差异化**: 在线编辑 + 实时预览 + 社区互动 + 积分激励

------

## 二、用户角色

### 1. 代码创作者（前台用户）
- 创建和编辑代码作品（奖励10积分）
- 实时预览代码效果
- 保存、更新、删除作品
- 配置作品信息（标题、描述、标签、可见性、定价）
- 设置作品为免费或付费（付费作品隐藏源码）
- 设置Fork价格（免费或指定积分数）
- 分享作品链接
- 管理个人代码库
- 查看作品收益统计

### 2. 代码浏览者（游客/登录用户）
- 浏览代码广场
- 查看代码详情和预览效果
- 免费作品：可查看源码并免费Fork
- 付费作品：只能预览效果,源码隐藏,Fork需付费
- 点赞优秀作品
- 评论交流（需登录）
- 收藏代码作品（需登录）
- 按标签/分类筛选作品
- 购买付费作品的Fork权限（支付积分）

### 3. 管理员（后台）
- 代码内容审核管理
- 作品列表管理（推荐、下架、删除）
- 模板管理（创建、编辑系统模板）
- 标签分类管理
- 违规内容处理
- 数据统计分析

------

## 三、功能需求

### 1. 前台（用户端）功能

#### 1.1 在线代码编辑器

##### 1.1.1 编辑器布局
- **三栏编辑**: HTML、CSS、JavaScript独立编辑区
- **预览窗口**: 实时显示代码运行效果
- **布局模式**: 
  - 经典模式（上下分栏：编辑器在上,预览在下）
  - 左右模式（左右分栏：编辑器在左,预览在右）
  - 全屏预览（隐藏编辑器,只显示预览）
  - 全屏编辑（隐藏预览,专注编辑）
- **拖拽调整**: 支持分栏大小拖拽调整

##### 1.1.2 编辑器功能
- **代码编辑**: 基于Monaco Editor（VS Code编辑器内核）
- **语法高亮**: 自动识别HTML、CSS、JS语法
- **代码补全**: 智能代码提示和自动补全
- **代码格式化**: 一键格式化代码
- **多光标编辑**: 支持多光标同时编辑
- **代码折叠**: 支持代码块折叠展开
- **错误提示**: 实时显示语法错误
- **主题切换**: 支持浅色/深色主题

##### 1.1.3 实时预览
- **自动刷新**: 代码修改后自动更新预览（可配置延迟时间）
- **手动刷新**: 支持手动刷新预览
- **控制台输出**: 显示JavaScript的console输出和错误信息
- **响应式预览**: 支持预览不同设备尺寸效果
- **全屏预览**: 支持全屏查看预览效果

##### 1.1.4 外部资源
- **外部库引入**: 
  - 支持CDN链接引入外部CSS库（如Bootstrap、Tailwind CSS）
  - 支持CDN链接引入外部JS库（如jQuery、Vue、React）
  - 提供常用库的快速添加选项
- **预处理器支持**:
  - CSS预处理器（Sass、Less、Stylus）
  - JS转译器（Babel、TypeScript）
  - HTML模板引擎（Pug、Markdown）

#### 1.2 作品管理

##### 1.2.1 作品创建
- **新建作品**: 点击"新建代码"按钮创建空白作品
- **模板创建**: 从系统模板快速创建
- **积分奖励**: 首次保存作品奖励10积分
- **自动保存**: 编辑过程中自动保存到草稿
- **基本信息**:
  - 作品标题（必填,1-100字符）
  - 作品描述（选填,最多500字符）
  - 作品标签（选填,最多5个）
  - 可见性设置（公开/私密）
- **定价设置**:
  - 作品类型（免费/付费）
  - Fork价格（0积分或自定义积分数，1-1000积分）
  - 免费作品：源码公开，可免费Fork
  - 付费作品：源码隐藏，Fork需支付设定的积分

##### 1.2.2 作品保存
- **草稿保存**: 自动保存到草稿（不奖励积分）
- **首次发布**: 首次正式发布作品奖励10积分
- **保存提示**: 显示保存状态、时间和积分奖励信息
- **版本记录**: 自动记录保存历史版本

##### 1.2.3 作品编辑
- **编辑权限**: 只有作者可编辑自己的作品
- **编辑规则**: 编辑已发布作品不重复奖励积分
- **更新信息**: 支持修改标题、描述、标签、定价设置
- **价格调整**: 可随时调整Fork价格（免费↔付费）
- **更新代码**: 支持修改HTML、CSS、JS代码
- **更新时间**: 自动更新最后修改时间

##### 1.2.4 作品删除
- **软删除**: 删除后可在回收站恢复
- **删除确认**: 删除前弹窗确认
- **彻底删除**: 回收站中可彻底删除（不可恢复）
- **积分规则**: 删除作品不扣除已获得的奖励积分

##### 1.2.5 作品分享
- **分享链接**: 生成唯一的作品访问链接
- **二维码**: 生成作品二维码
- **嵌入代码**: 生成iframe嵌入代码
- **社交分享**: 支持分享到微信、微博等
- **复制功能**: 一键复制分享链接

#### 1.3 代码广场

##### 1.3.1 作品列表
- **卡片展示**: 
  - 作品预览图（代码运行效果截图）
  - 作品标题
  - 作者信息（头像、昵称）
  - 标签标识
  - 互动数据（点赞数、评论数、收藏数、浏览数）
  - 创建时间
- **分页加载**: 瀑布流或分页展示
- **排序方式**: 
  - 最新发布
  - 最热门（按点赞数）
  - 最多评论
  - 最多收藏
  - 最多浏览

##### 1.3.2 筛选功能
- **标签筛选**: 按标签筛选作品
- **分类筛选**: 按作品类型分类（动画特效、组件库、游戏、工具等）
- **作者筛选**: 查看指定作者的作品
- **时间筛选**: 按发布时间筛选
- **搜索功能**: 关键词搜索作品标题和描述

##### 1.3.3 推荐位
- **编辑推荐**: 管理员推荐的优秀作品
- **热门作品**: 自动计算的热门作品
- **新手教程**: 适合新手学习的作品
- **每日精选**: 每日推荐一个优秀作品

#### 1.4 作品详情

##### 1.4.1 详情展示
- **预览区域**: 全屏展示代码运行效果
- **代码查看**: 
  - 免费作品：展示HTML、CSS、JS完整代码（可折叠）
  - 付费作品：代码区域显示"付费作品，Fork后可查看源码"
- **作品信息**:
  - 作品标题、描述
  - 作者信息（头像、昵称、作品数）
  - 标签列表
  - 创建时间、更新时间
  - 定价信息（免费/Fork价格）
  - 互动数据（点赞、评论、收藏、浏览、Fork数）

##### 1.4.2 互动功能
- **点赞**: 点赞作品（登录用户）
- **收藏**: 收藏到个人收藏夹（登录用户）
- **评论**: 发表评论和回复（登录用户）
- **分享**: 分享作品链接
- **举报**: 举报违规内容

##### 1.4.3 操作功能
- **在线编辑**: 
  - 免费作品：游客和登录用户可在线编辑查看效果（不能保存）
  - 付费作品：只能预览，不能编辑
- **Fork作品**: 
  - 免费作品：登录用户可免费Fork
  - 付费作品：需支付作者设定的积分，积分转给作者
  - Fork后获得完整源码副本，可自由编辑
- **积分检查**: Fork前检查用户积分是否充足
- **嵌入展示**: 支持iframe嵌入到其他网站（只显示预览）
- **全屏预览**: 全屏查看运行效果
- **下载代码**: 
  - 免费作品：可直接下载HTML文件
  - 付费作品：需Fork后才能下载

#### 1.5 个人中心

##### 1.5.1 我的作品
- **作品列表**: 显示所有个人作品
- **草稿箱**: 未发布的草稿作品
- **回收站**: 已删除的作品（可恢复）
- **分类管理**: 按标签、时间分类查看
- **批量操作**: 批量删除、设置可见性

##### 1.5.2 我的收藏
- **收藏列表**: 显示收藏的作品
- **收藏夹管理**: 创建多个收藏夹分类
- **取消收藏**: 移除收藏
- **收藏排序**: 按收藏时间、作品热度排序

##### 1.5.3 我的互动
- **点赞记录**: 我点赞过的作品
- **评论记录**: 我发表的评论
- **收到的评论**: 我的作品收到的评论
- **粉丝关注**: 关注我的用户列表

##### 1.5.4 统计数据
- **作品总数**: 已发布作品数量
- **总点赞数**: 作品累计获得的点赞
- **总收藏数**: 作品累计获得的收藏
- **总浏览数**: 作品累计浏览量
- **总Fork数**: 作品被Fork的总次数
- **积分收益**: 通过付费作品获得的积分总额
- **收益明细**: 查看每个作品的收益详情

------

### 2. 后台（管理端）功能

#### 2.1 作品管理

##### 2.1.1 作品列表
- **列表展示**:
  - 分页显示所有作品
  - 多维度筛选（作者、标签、状态、时间范围）
  - 关键词搜索（标题、描述）
  - 敏感词检测标记
- **作品操作**:
  - 推荐设置（设为编辑推荐）
  - 状态管理（正常/下架/删除）
  - 查看作品详情和代码
  - 强制删除作品

##### 2.1.2 内容审核
- **待审核列表**: 查看待审核作品（可选功能）
- **审核通过/拒绝**: 审核决策
- **违规处理**: 处理违规内容
- **审核记录**: 查看审核历史

##### 2.1.3 推荐管理
- **编辑推荐**: 设置编辑推荐作品
- **推荐位管理**: 管理各个推荐位
- **推荐排序**: 调整推荐作品顺序
- **推荐时效**: 设置推荐过期时间

#### 2.2 模板管理

##### 2.2.1 系统模板
- **模板列表**: 显示所有系统模板
- **创建模板**: 管理员创建系统模板
- **编辑模板**: 修改模板代码和信息
- **删除模板**: 删除不需要的模板
- **模板分类**: 按用途分类（基础模板、动画模板、组件模板等）

##### 2.2.2 模板属性
- **模板名称**: 模板标题
- **模板描述**: 模板说明
- **模板代码**: HTML、CSS、JS代码
- **模板标签**: 标签分类
- **使用次数**: 统计模板使用次数
- **模板预览**: 预览模板效果

#### 2.3 标签管理

##### 2.3.1 标签库
- **标签列表**: 显示所有标签
- **创建标签**: 添加新标签
- **编辑标签**: 修改标签名称和描述
- **删除标签**: 删除无用标签
- **标签合并**: 合并相似标签

##### 2.3.2 标签统计
- **使用次数**: 统计标签使用频率
- **热门标签**: 显示最热门的标签
- **标签趋势**: 标签使用趋势分析

#### 2.4 评论管理

##### 2.4.1 评论列表
- **所有评论**: 显示所有评论
- **筛选功能**: 按作品、用户、时间筛选
- **敏感词检测**: 自动标记敏感评论
- **评论操作**: 删除、隐藏评论

#### 2.5 基础数据统计

##### 2.5.1 概览数据
- **作品总数**: 平台所有作品数量
- **用户总数**: 创建过作品的用户数
- **今日新增**: 今日新增作品数
- **总浏览量**: 所有作品累计浏览量
- **总点赞量**: 所有作品累计点赞数
- **总评论量**: 所有作品累计评论数

##### 2.5.2 趋势分析
- **作品增长趋势**: 按天/周/月统计作品增长
- **用户活跃度**: 活跃用户数趋势
- **热门标签**: 最受欢迎的标签排行

------

## 四、核心流程

### 1. 作品创建流程
1. **进入编辑器**: 点击"新建代码"按钮或选择系统模板
2. **编写代码**: 在编辑器中编写HTML、CSS、JS
3. **实时预览**: 预览窗口实时显示效果
4. **配置信息**: 填写标题、描述、标签
5. **设置定价**: 选择免费或付费，设置Fork价格（0或1-1000积分）
6. **保存发布**: 确认保存并发布
7. **积分奖励**: 首次发布奖励10积分
8. **生成链接**: 生成唯一的作品访问链接

### 2. 作品浏览流程
1. **进入广场**: 访问代码广场页面
2. **浏览列表**: 查看作品卡片列表
3. **筛选排序**: 按标签、热度、价格等筛选排序
4. **点击作品**: 进入作品详情页
5. **查看效果**: 查看预览效果
6. **查看代码**: 
   - 免费作品：直接查看完整源码
   - 付费作品：代码区域显示付费提示
7. **互动操作**: 点赞、评论、收藏、分享

### 3. 免费作品Fork流程
1. **浏览作品**: 查看免费作品
2. **查看源码**: 查看完整的HTML、CSS、JS代码
3. **点击Fork**: 点击Fork按钮
4. **确认Fork**: 确认Fork操作（免费）
5. **复制作品**: 作品复制到个人账号
6. **二次创作**: 可以修改和保存
7. **积分奖励**: Fork后的作品首次发布也获得10积分

### 4. 付费作品Fork流程
1. **浏览作品**: 查看付费作品
2. **只能预览**: 只能看到运行效果，看不到源码
3. **点击Fork**: 点击Fork按钮，显示价格
4. **积分检查**: 检查用户积分是否足够支付
5. **确认支付**: 确认支付指定积分
6. **积分转账**: 
   - 扣除用户积分
   - 积分转给作品作者
   - 记录交易明细
7. **获得源码**: Fork成功后可查看完整源码
8. **二次创作**: 可以修改和保存到自己账号
9. **积分奖励**: Fork后的作品首次发布也获得10积分

### 5. 管理员审核流程
1. **内容监控**: 系统自动检测敏感词
2. **待审核列表**: 查看需要审核的作品
3. **内容审查**: 查看作品代码和效果
4. **审核决策**: 通过、拒绝或下架
5. **通知用户**: 审核结果通知作者

------

## 五、技术方案

### 1. 数据库设计

#### 1.1 代码作品表（code_pen）
```sql
CREATE TABLE code_pen (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '作者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '作品标题',
    description VARCHAR(1000) COMMENT '作品描述',
    html_code LONGTEXT COMMENT 'HTML代码',
    css_code LONGTEXT COMMENT 'CSS代码',
    js_code LONGTEXT COMMENT 'JavaScript代码',
    preview_image VARCHAR(255) COMMENT '预览图URL',
    external_css TEXT COMMENT '外部CSS库链接，JSON数组格式',
    external_js TEXT COMMENT '外部JS库链接，JSON数组格式',
    preprocessor_config TEXT COMMENT '预处理器配置，JSON格式',
    tags VARCHAR(500) COMMENT '标签，JSON数组格式',
    category VARCHAR(50) COMMENT '分类（动画、组件、游戏、工具等）',
    is_public TINYINT DEFAULT 1 COMMENT '可见性：1-公开 0-私密',
    is_free TINYINT DEFAULT 1 COMMENT '是否免费：1-免费 0-付费',
    fork_price INT DEFAULT 0 COMMENT 'Fork价格（积分），0表示免费，1-1000表示付费',
    is_template TINYINT DEFAULT 0 COMMENT '是否系统模板：1-是 0-否',
    status TINYINT DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已下架 3-已删除',
    is_recommend TINYINT DEFAULT 0 COMMENT '是否推荐：1-推荐 0-普通',
    recommend_expire_time DATETIME COMMENT '推荐过期时间',
    forked_from BIGINT COMMENT 'Fork来源作品ID',
    fork_count INT DEFAULT 0 COMMENT 'Fork次数',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    collect_count INT DEFAULT 0 COMMENT '收藏数',
    view_count INT DEFAULT 0 COMMENT '浏览数',
    points_reward INT DEFAULT 10 COMMENT '创建奖励积分',
    total_income INT DEFAULT 0 COMMENT '累计收益积分（通过付费Fork获得）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    publish_time DATETIME COMMENT '发布时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_status_publish_time (status, publish_time DESC),
    INDEX idx_is_recommend (is_recommend, publish_time DESC),
    INDEX idx_category (category),
    INDEX idx_like_count (like_count DESC),
    INDEX idx_view_count (view_count DESC),
    INDEX idx_is_free (is_free),
    INDEX idx_fork_price (fork_price),
    FULLTEXT INDEX ft_title_description (title, description) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '代码作品表';
```

#### 1.2 作品点赞表（code_pen_like）
```sql
CREATE TABLE code_pen_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '点赞ID',
    pen_id BIGINT NOT NULL COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    
    UNIQUE KEY uk_pen_user (pen_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_pen_id (pen_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '作品点赞表';
```

#### 1.3 作品收藏表（code_pen_collect）
```sql
CREATE TABLE code_pen_collect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    pen_id BIGINT NOT NULL COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_id BIGINT COMMENT '收藏夹ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    
    UNIQUE KEY uk_pen_user (pen_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_pen_id (pen_id),
    INDEX idx_folder_id (folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '作品收藏表';
```

#### 1.4 收藏夹表（code_pen_folder）
```sql
CREATE TABLE code_pen_folder (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏夹ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    folder_name VARCHAR(100) NOT NULL COMMENT '收藏夹名称',
    folder_description VARCHAR(500) COMMENT '收藏夹描述',
    collect_count INT DEFAULT 0 COMMENT '收藏数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '收藏夹表';
```

#### 1.5 作品评论表（code_pen_comment）
```sql
CREATE TABLE code_pen_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    pen_id BIGINT NOT NULL COMMENT '作品ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT COMMENT '父评论ID（回复）',
    reply_to_user_id BIGINT COMMENT '回复目标用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常 2-已隐藏 3-已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_pen_id (pen_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '作品评论表';
```

#### 1.6 作品标签表（code_pen_tag）
```sql
CREATE TABLE code_pen_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_description VARCHAR(200) COMMENT '标签描述',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE KEY uk_tag_name (tag_name),
    INDEX idx_use_count (use_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '作品标签表';
```

#### 1.7 Fork交易记录表（code_pen_fork_transaction）
```sql
CREATE TABLE code_pen_fork_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '交易ID',
    original_pen_id BIGINT NOT NULL COMMENT '原作品ID',
    forked_pen_id BIGINT NOT NULL COMMENT 'Fork后的作品ID',
    original_author_id BIGINT NOT NULL COMMENT '原作者ID',
    fork_user_id BIGINT NOT NULL COMMENT 'Fork用户ID',
    fork_price INT DEFAULT 0 COMMENT 'Fork价格（积分），0表示免费',
    transaction_type TINYINT DEFAULT 0 COMMENT '交易类型：0-免费Fork 1-付费Fork',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    
    INDEX idx_original_pen_id (original_pen_id),
    INDEX idx_forked_pen_id (forked_pen_id),
    INDEX idx_original_author_id (original_author_id),
    INDEX idx_fork_user_id (fork_user_id),
    INDEX idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fork交易记录表';
```

### 2. 后端接口设计

#### 2.1 用户端代码编辑器接口（/user/code-pen）

##### 作品管理
```
POST /user/code-pen/create              # 创建作品（首次发布奖励10积分）
POST /user/code-pen/save                # 保存作品
POST /user/code-pen/update              # 更新作品
DELETE /user/code-pen/{id}              # 删除作品
POST /user/code-pen/fork                # Fork作品（免费或付费）
GET /user/code-pen/{id}                 # 获取作品详情
POST /user/code-pen/my-list             # 我的作品列表
POST /user/code-pen/draft-list          # 我的草稿列表
POST /user/code-pen/check-fork-price    # 检查Fork价格和用户积分
POST /user/code-pen/income-stats        # 查看收益统计
```

##### 作品广场
```
POST /user/code-pen/list                # 获取作品列表（代码广场）
POST /user/code-pen/recommend-list      # 获取推荐作品列表
POST /user/code-pen/search              # 搜索作品
POST /user/code-pen/by-tag              # 按标签获取作品
POST /user/code-pen/by-category         # 按分类获取作品
POST /user/code-pen/by-user             # 获取指定用户的作品
GET /user/code-pen/hot                  # 获取热门作品
```

##### 互动功能
```
POST /user/code-pen/like                # 点赞作品
POST /user/code-pen/unlike              # 取消点赞
POST /user/code-pen/collect             # 收藏作品
POST /user/code-pen/uncollect           # 取消收藏
POST /user/code-pen/comment             # 发表评论
DELETE /user/code-pen/comment/{id}      # 删除评论
POST /user/code-pen/comment-list        # 获取评论列表
POST /user/code-pen/view                # 增加浏览数
```

##### 收藏夹管理
```
POST /user/code-pen/folder/create       # 创建收藏夹
POST /user/code-pen/folder/update       # 更新收藏夹
DELETE /user/code-pen/folder/{id}       # 删除收藏夹
POST /user/code-pen/folder/list         # 我的收藏夹列表
POST /user/code-pen/folder/items        # 收藏夹内容列表
```

##### 模板管理
```
GET /user/code-pen/templates            # 获取系统模板列表
GET /user/code-pen/template/{id}        # 获取模板详情
```

##### 标签管理
```
GET /user/code-pen/tags                 # 获取所有标签
GET /user/code-pen/tags/hot             # 获取热门标签
```

#### 2.2 管理端代码编辑器接口（/admin/code-pen）

##### 作品管理
```
POST /admin/code-pen/list               # 获取作品列表（管理端）
GET /admin/code-pen/{id}                # 获取作品详情
POST /admin/code-pen/update-status      # 更新作品状态
POST /admin/code-pen/recommend          # 设置推荐
POST /admin/code-pen/cancel-recommend   # 取消推荐
DELETE /admin/code-pen/{id}             # 删除作品（管理端）
```

##### 模板管理
```
POST /admin/code-pen/template/create    # 创建系统模板
POST /admin/code-pen/template/update    # 更新模板
DELETE /admin/code-pen/template/{id}    # 删除模板
POST /admin/code-pen/template/list      # 模板列表
```

##### 标签管理
```
POST /admin/code-pen/tag/create         # 创建标签
POST /admin/code-pen/tag/update         # 更新标签
DELETE /admin/code-pen/tag/{id}         # 删除标签
POST /admin/code-pen/tag/merge          # 合并标签
POST /admin/code-pen/tag/list           # 标签列表
```

##### 评论管理
```
POST /admin/code-pen/comment/list       # 评论列表
POST /admin/code-pen/comment/hide       # 隐藏评论
DELETE /admin/code-pen/comment/{id}     # 删除评论
```

##### 基础数据
```
GET /admin/code-pen/statistics          # 获取统计数据
GET /admin/code-pen/trends              # 获取趋势数据
```

#### 2.3 请求/响应示例

**创建作品（奖励积分）：**
```json
// 请求
POST /user/code-pen/create
{
    "title": "炫酷的CSS动画效果",
    "description": "使用纯CSS实现的炫酷动画特效",
    "htmlCode": "<!DOCTYPE html>\n<html>\n<body>\n<div class=\"box\"></div>\n</body>\n</html>",
    "cssCode": ".box {\n  width: 100px;\n  height: 100px;\n  background: linear-gradient(45deg, #ff0000, #00ff00);\n  animation: rotate 2s infinite;\n}\n\n@keyframes rotate {\n  from { transform: rotate(0deg); }\n  to { transform: rotate(360deg); }\n}",
    "jsCode": "",
    "externalCss": [],
    "externalJs": [],
    "tags": ["CSS", "动画", "特效"],
    "category": "动画",
    "isPublic": 1,
    "isFree": 1,
    "forkPrice": 0
}

// 响应（首次发布）
{
    "code": 200,
    "message": "作品发布成功，奖励10积分",
    "data": {
        "penId": 12345,
        "pointsAdded": 10,
        "pointsTotal": 1010,
        "shareUrl": "https://code-nest.com/pen/12345",
        "createTime": "2025-10-11 14:30:00"
    }
}

// 响应（保存草稿）
{
    "code": 200,
    "message": "草稿保存成功",
    "data": {
        "penId": 12345,
        "status": 0,
        "createTime": "2025-10-11 14:30:00"
    }
}
```

**检查Fork价格：**
```json
// 请求
POST /user/code-pen/check-fork-price
{
    "penId": 12345
}

// 响应（免费作品）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "penId": 12345,
        "isFree": true,
        "forkPrice": 0,
        "currentPoints": 150,
        "canFork": true
    }
}

// 响应（付费作品-积分充足）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "penId": 12345,
        "isFree": false,
        "forkPrice": 50,
        "currentPoints": 150,
        "canFork": true,
        "authorName": "张三"
    }
}

// 响应（付费作品-积分不足）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "penId": 12345,
        "isFree": false,
        "forkPrice": 50,
        "currentPoints": 30,
        "canFork": false,
        "message": "积分不足，还需要20积分"
    }
}
```

**Fork作品：**
```json
// 请求
POST /user/code-pen/fork
{
    "penId": 12345
}

// 响应（免费Fork）
{
    "code": 200,
    "message": "Fork成功",
    "data": {
        "newPenId": 12346,
        "originalPenId": 12345,
        "forkPrice": 0,
        "createTime": "2025-10-11 14:35:00"
    }
}

// 响应（付费Fork）
{
    "code": 200,
    "message": "Fork成功，支付50积分给作者",
    "data": {
        "newPenId": 12346,
        "originalPenId": 12345,
        "forkPrice": 50,
        "pointsRemaining": 100,
        "authorName": "张三",
        "createTime": "2025-10-11 14:35:00"
    }
}

// 积分不足时
{
    "code": 400,
    "message": "积分不足，当前积分：30，Fork需要50积分",
    "data": null
}
```

**作品列表：**
```json
// 请求
POST /user/code-pen/list
{
    "category": "动画",
    "tags": ["CSS"],
    "keyword": "动画",
    "sortBy": "hot",
    "isFree": null,
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
                "id": 12345,
                "userId": 1001,
                "userNickname": "张三",
                "userAvatar": "https://example.com/avatar.jpg",
                "title": "炫酷的CSS动画效果",
                "description": "使用纯CSS实现的炫酷动画特效",
                "previewImage": "https://example.com/preview.jpg",
                "tags": ["CSS", "动画", "特效"],
                "category": "动画",
                "isFree": true,
                "forkPrice": 0,
                "likeCount": 256,
                "commentCount": 32,
                "collectCount": 89,
                "viewCount": 1520,
                "forkCount": 15,
                "createTime": "2025-10-10 10:30:00",
                "isLiked": false,
                "isCollected": false
            },
            {
                "id": 12346,
                "userId": 1002,
                "userNickname": "李四",
                "userAvatar": "https://example.com/avatar2.jpg",
                "title": "高级动画组件库",
                "description": "企业级动画组件库，包含20+动画效果",
                "previewImage": "https://example.com/preview2.jpg",
                "tags": ["CSS", "组件", "动画"],
                "category": "组件",
                "isFree": false,
                "forkPrice": 100,
                "likeCount": 589,
                "commentCount": 78,
                "collectCount": 245,
                "viewCount": 3520,
                "forkCount": 45,
                "createTime": "2025-10-09 15:20:00",
                "isLiked": true,
                "isCollected": false
            }
        ],
        "total": 150,
        "current": 1,
        "size": 20
    }
}
```

**作品详情：**
```json
// 请求
GET /user/code-pen/12345

// 响应（免费作品）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "id": 12345,
        "userId": 1001,
        "userNickname": "张三",
        "userAvatar": "https://example.com/avatar.jpg",
        "title": "炫酷的CSS动画效果",
        "description": "使用纯CSS实现的炫酷动画特效",
        "htmlCode": "<!DOCTYPE html>\n<html>\n<body>\n<div class=\"box\"></div>\n</body>\n</html>",
        "cssCode": ".box {...}",
        "jsCode": "",
        "externalCss": [],
        "externalJs": [],
        "previewImage": "https://example.com/preview.jpg",
        "tags": ["CSS", "动画", "特效"],
        "category": "动画",
        "isPublic": 1,
        "isFree": true,
        "forkPrice": 0,
        "forkedFrom": null,
        "likeCount": 256,
        "commentCount": 32,
        "collectCount": 89,
        "viewCount": 1520,
        "forkCount": 15,
        "totalIncome": 0,
        "createTime": "2025-10-10 10:30:00",
        "updateTime": "2025-10-10 15:20:00",
        "isLiked": false,
        "isCollected": false,
        "canEdit": false,
        "canDelete": false,
        "canViewCode": true,
        "shareUrl": "https://code-nest.com/pen/12345"
    }
}

// 响应（付费作品-未购买）
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "id": 12346,
        "userId": 1002,
        "userNickname": "李四",
        "userAvatar": "https://example.com/avatar2.jpg",
        "title": "高级动画组件库",
        "description": "企业级动画组件库，包含20+动画效果",
        "htmlCode": null,
        "cssCode": null,
        "jsCode": null,
        "externalCss": [],
        "externalJs": [],
        "previewImage": "https://example.com/preview2.jpg",
        "tags": ["CSS", "组件", "动画"],
        "category": "组件",
        "isPublic": 1,
        "isFree": false,
        "forkPrice": 100,
        "forkedFrom": null,
        "likeCount": 589,
        "commentCount": 78,
        "collectCount": 245,
        "viewCount": 3520,
        "forkCount": 45,
        "totalIncome": 4500,
        "createTime": "2025-10-09 15:20:00",
        "updateTime": "2025-10-09 15:20:00",
        "isLiked": true,
        "isCollected": false,
        "canEdit": false,
        "canDelete": false,
        "canViewCode": false,
        "codeViewMessage": "付费作品，Fork后可查看源码",
        "shareUrl": "https://code-nest.com/pen/12346"
    }
}
```

**管理端统计数据：**
```json
// 请求
GET /admin/code-pen/statistics

// 响应
{
    "code": 200,
    "message": "获取成功",
    "data": {
        "totalPens": 5420,
        "totalUsers": 1230,
        "todayNewPens": 45,
        "totalViews": 152380,
        "totalLikes": 32580,
        "totalComments": 8920,
        "totalCollects": 15680,
        "totalForks": 3250
    }
}
```

### 3. DTO结构设计

#### 3.1 请求DTO

**作品创建请求（CodePenCreateRequest）：**
```java
{
    "title": "String（标题，必填，1-100字符）",
    "description": "String（描述，选填，最多500字符）",
    "htmlCode": "String（HTML代码，选填）",
    "cssCode": "String（CSS代码，选填）",
    "jsCode": "String（JavaScript代码，选填）",
    "externalCss": "List<String>（外部CSS库链接，选填）",
    "externalJs": "List<String>（外部JS库链接，选填）",
    "preprocessorConfig": "Map<String, Object>（预处理器配置，选填）",
    "tags": "List<String>（标签列表，选填，最多5个）",
    "category": "String（分类，选填）",
    "isPublic": "Integer（可见性，默认1）",
    "isFree": "Integer（是否免费，1-免费 0-付费，默认1）",
    "forkPrice": "Integer（Fork价格，0或1-1000积分，默认0）"
}
```

**作品列表查询请求（CodePenListRequest）：**
```java
{
    "userId": "Long（作者ID，选填）",
    "category": "String（分类，选填）",
    "tags": "List<String>（标签筛选，选填）",
    "keyword": "String（关键词搜索，选填）",
    "sortBy": "String（排序方式：latest/hot/most_liked/most_viewed，默认latest）",
    "status": "Integer（状态筛选，选填）",
    "pageNum": "Integer（页码，默认1）",
    "pageSize": "Integer（每页大小，默认20）"
}
```

**评论发表请求（CommentCreateRequest）：**
```java
{
    "penId": "Long（作品ID，必填）",
    "content": "String（评论内容，必填，1-500字符）",
    "parentId": "Long（父评论ID，选填）",
    "replyToUserId": "Long（回复目标用户ID，选填）"
}
```

#### 3.2 响应DTO

**作品创建响应（CodePenCreateResponse）：**
```java
{
    "penId": "Long（作品ID）",
    "pointsAdded": "Integer（奖励积分，首次发布为10）",
    "pointsTotal": "Integer（当前总积分）",
    "shareUrl": "String（分享链接）",
    "createTime": "String（创建时间）"
}
```

**作品详情响应（CodePenDetailResponse）：**
```java
{
    "id": "Long（作品ID）",
    "userId": "Long（作者ID）",
    "userNickname": "String（作者昵称）",
    "userAvatar": "String（作者头像）",
    "title": "String（作品标题）",
    "description": "String（作品描述）",
    "htmlCode": "String（HTML代码，付费作品未购买时为null）",
    "cssCode": "String（CSS代码，付费作品未购买时为null）",
    "jsCode": "String（JavaScript代码，付费作品未购买时为null）",
    "externalCss": "List<String>（外部CSS库）",
    "externalJs": "List<String>（外部JS库）",
    "preprocessorConfig": "Map<String, Object>（预处理器配置）",
    "previewImage": "String（预览图）",
    "tags": "List<String>（标签）",
    "category": "String（分类）",
    "isPublic": "Integer（可见性）",
    "isFree": "Boolean（是否免费）",
    "forkPrice": "Integer（Fork价格）",
    "forkedFrom": "Long（Fork来源）",
    "likeCount": "Integer（点赞数）",
    "commentCount": "Integer（评论数）",
    "collectCount": "Integer（收藏数）",
    "viewCount": "Integer（浏览数）",
    "forkCount": "Integer（Fork数）",
    "totalIncome": "Integer（累计收益积分）",
    "createTime": "String（创建时间）",
    "updateTime": "String（更新时间）",
    "isLiked": "Boolean（是否已点赞）",
    "isCollected": "Boolean（是否已收藏）",
    "canEdit": "Boolean（是否可编辑）",
    "canDelete": "Boolean（是否可删除）",
    "canViewCode": "Boolean（是否可查看源码）",
    "codeViewMessage": "String（源码查看提示信息）",
    "shareUrl": "String（分享链接）"
}
```

**统计数据响应（CodePenStatisticsResponse）：**
```java
{
    "totalPens": "Long（作品总数）",
    "totalUsers": "Long（用户总数）",
    "todayNewPens": "Long（今日新增）",
    "totalViews": "Long（总浏览量）",
    "totalLikes": "Long（总点赞数）",
    "totalComments": "Long（总评论数）",
    "totalCollects": "Long（总收藏数）",
    "totalForks": "Long（总Fork数）"
}
```

### 4. 前端组件设计

#### 4.1 核心组件
- `CodeEditor`：代码编辑器组件（基于Monaco Editor）
- `CodePreview`：代码预览组件（iframe）
- `CodePenList`：作品列表组件
- `CodePenCard`：作品卡片组件
- `CodePenDetail`：作品详情组件
- `CommentList`：评论列表组件
- `TagFilter`：标签筛选组件
- `TemplateSelector`：模板选择器组件

#### 4.2 编辑器布局组件
- `EditorLayout`：编辑器整体布局
- `EditorHeader`：编辑器头部（标题、保存按钮等）
- `EditorToolbar`：编辑器工具栏（格式化、设置等）
- `EditorPanel`：编辑器面板（HTML、CSS、JS）
- `PreviewPanel`：预览面板
- `ConsolePanel`：控制台面板

#### 4.3 状态管理
使用Pinia管理全局状态：
- `codePenStore`：作品数据管理
- `editorStore`：编辑器状态管理（代码、配置、布局）
- `userStore`：用户信息管理
- `interactionStore`：互动数据管理（点赞、收藏等）

#### 4.4 技术选型
- **编辑器**: Monaco Editor（VS Code编辑器内核）
- **预览**: iframe沙箱环境
- **预处理器**: 
  - CSS: sass.js、less.js、stylus
  - JS: babel-standalone、typescript
  - HTML: pug、markdown-it
- **代码格式化**: prettier
- **代码高亮**: highlight.js

### 5. 业务规则与验证

#### 5.1 创建限制
- **标题验证**: 1-100字符，不能为空
- **描述验证**: 最多500字符
- **标签限制**: 最多选择5个标签
- **账号验证**: 必须是已登录的正常账号
- **代码验证**: 至少有一种代码（HTML/CSS/JS）不为空
- **定价验证**: Fork价格范围0-1000积分

#### 5.2 积分规则
- **创建作品**: 首次发布奖励10积分（status从0变为1）
- **编辑作品**: 不重复奖励积分
- **草稿保存**: 不奖励积分（status=0）
- **免费Fork**: 不需要积分
- **付费Fork**: 
  - Fork前检查用户积分是否充足
  - 扣除Fork用户的积分
  - 积分转给作品作者
  - 作品的total_income增加
  - 记录交易明细
- **删除作品**: 不扣除已获得的奖励积分
- **交易失败**: 积分不扣除，回滚事务
- **Fork后发布**: Fork的作品首次发布也奖励10积分

#### 5.3 权限控制
- **作品创建**: 仅登录用户
- **作品编辑**: 仅作者本人可编辑
- **作品删除**: 仅作者本人或管理员可删除
- **免费Fork**: 仅登录用户
- **付费Fork**: 仅登录用户且积分充足
- **点赞评论**: 仅登录用户
- **作品预览**: 所有人可查看（公开作品）
- **源码查看**: 
  - 免费作品：所有人可查看
  - 付费作品：仅作者本人和已Fork用户可查看
  - 私密作品：仅作者和管理员可查看

#### 5.4 安全限制
- **XSS防护**: 对用户输入的代码进行沙箱隔离
- **iframe沙箱**: 预览使用sandbox属性限制权限
- **敏感词过滤**: 标题、描述进行敏感词检测
- **恶意代码**: 禁止执行危险操作（如无限循环、大量请求）
- **频率限制**: 限制短时间内的创建/评论频率

#### 5.5 互动规则
- **点赞**: 每个用户对每个作品只能点赞一次
- **收藏**: 每个用户对每个作品只能收藏一次
- **评论**: 评论内容1-500字符，支持回复
- **浏览**: 同一用户24小时内重复浏览不重复计数

------

## 六、功能特色

1. **在线编辑**: 无需本地环境，浏览器中直接编写代码
2. **实时预览**: 代码修改实时显示效果，所见即所得
3. **创作激励**: 创建作品奖励10积分，鼓励用户持续创作
4. **知识变现**: 支持设置付费作品，通过优质内容获得积分收益
5. **灵活定价**: 作者可自定义Fork价格（0-1000积分）
6. **源码保护**: 付费作品隐藏源码，Fork后可查看
7. **专业编辑器**: 基于Monaco Editor，提供VS Code级别的编辑体验
8. **预处理器支持**: 支持Sass、Less、TypeScript等预处理器
9. **外部库引入**: 支持引入CDN库，快速开发
10. **模板系统**: 丰富的系统模板，快速开始
11. **Fork机制**: 支持Fork他人作品进行二次创作
12. **社区互动**: 点赞、评论、收藏，构建活跃社区
13. **多维度筛选**: 按标签、分类、热度、价格等多维度筛选
14. **收藏夹管理**: 支持创建多个收藏夹分类管理
15. **分享嵌入**: 支持分享链接、二维码、iframe嵌入
16. **响应式预览**: 支持预览不同设备尺寸效果
17. **代码下载**: 支持下载HTML文件到本地
18. **收益统计**: 查看作品获得的积分收益明细

------

## 七、技术实现亮点

1. **积分交易事务**: 创作奖励/Fork付费和积分变动在同一事务中，保证数据一致性
2. **源码隐藏机制**: 付费作品源码字段返回null，保护作者权益
3. **交易记录追踪**: 完整记录Fork交易明细，支持收益统计
4. **Monaco Editor集成**: 集成VS Code编辑器内核，提供专业编辑体验
5. **iframe沙箱**: 使用sandbox属性隔离预览环境，确保安全性
6. **实时编译**: 支持预处理器实时编译（Sass、Less、TypeScript等）
7. **代码格式化**: 集成Prettier实现一键代码格式化
8. **自动保存**: 编辑器自动保存草稿，防止数据丢失
9. **延迟刷新**: 预览窗口延迟刷新，避免频繁更新影响性能
10. **懒加载**: 作品列表瀑布流懒加载，提升性能
11. **全文搜索**: 基于MySQL全文索引的作品搜索
12. **敏感词检测**: 标题和描述自动检测敏感词
13. **防重复浏览**: 24小时内重复浏览不重复计数
14. **热度计算**: 综合点赞、评论、收藏、浏览计算热度
15. **预览图生成**: 自动生成作品预览缩略图（可选）
16. **版本控制**: 保存历史版本，支持回溯（可选）
17. **收益分析**: 统计每个作品的Fork次数和收益金额

------

## 八、后续扩展方向

### 1. 协作功能
- **多人协作**: 支持多人同时编辑一个作品
- **实时同步**: 实时同步编辑内容
- **权限管理**: 作品协作权限管理

### 2. 增强功能
- **代码片段库**: 个人代码片段库管理
- **AI辅助**: AI代码补全和优化建议
- **性能分析**: 代码性能分析工具
- **调试工具**: 集成调试工具

### 3. 社区功能
- **作品挑战**: 定期发起代码挑战活动
- **排行榜**: 作者排行榜、作品排行榜
- **勋章系统**: 成就勋章激励体系
- **关注功能**: 关注喜欢的作者

### 4. 商业功能
- **付费模板**: 优质付费模板库
- **专业版**: 高级功能订阅服务
- **代码市场**: 作品交易市场

------

## 九、实施计划

### 第一期：核心编辑器（MVP）
- ✅ 在线代码编辑器（HTML、CSS、JS）
- ✅ 实时预览功能
- ✅ 作品保存和管理
- ✅ 积分奖励机制（创建奖励10积分）
- ✅ 基础的作品列表和详情

### 第二期：社区功能
- ✅ 代码广场和作品展示
- ✅ 点赞、评论、收藏功能
- ✅ 标签分类筛选
- ✅ 作品搜索
- ✅ 作品分享

### 第三期：知识变现功能
- ✅ 付费作品机制（免费/付费设置）
- ✅ 源码隐藏保护
- ✅ Fork付费功能
- ✅ 积分交易系统
- ✅ 收益统计功能
- ✅ 交易记录管理

### 第四期：增强功能
- ✅ 系统模板库
- ✅ 收藏夹管理
- ✅ 外部库引入
- ✅ 推荐机制
- ✅ 免费/付费筛选

### 第五期：高级功能
- ⏳ 预处理器支持（Sass、Less、TypeScript）
- ⏳ 代码格式化
- ⏳ 响应式预览
- ⏳ 控制台输出
- ⏳ 代码下载

### 第六期：管理和优化
- ⏳ 后台管理功能完善
- ⏳ 数据统计分析
- ⏳ 性能优化
- ⏳ 安全加固

------

## 十、风险评估

### 1. 技术风险
- **XSS攻击**: 用户代码可能包含恶意脚本
  - **解决方案**: iframe沙箱隔离，CSP策略
- **性能问题**: 复杂代码实时编译可能卡顿
  - **解决方案**: 延迟编译，Web Worker处理
- **浏览器兼容**: Monaco Editor对浏览器有要求
  - **解决方案**: 降级方案，使用简单编辑器

### 2. 业务风险
- **版权问题**: 用户可能上传侵权代码
  - **解决方案**: 版权声明，举报机制
- **恶意代码**: 用户可能编写恶意代码
  - **解决方案**: 代码审查，沙箱隔离
- **存储成本**: 大量代码作品占用存储
  - **解决方案**: 代码压缩，定期清理
- **价格滥用**: 作者可能设置不合理的高价
  - **解决方案**: 限制价格范围（1-1000积分），推荐合理定价
- **积分通货膨胀**: 大量奖励积分可能导致积分贬值
  - **解决方案**: 平衡积分产出和消耗，监控积分流通

### 3. 运营风险
- **内容质量**: 低质量作品影响平台形象
  - **解决方案**: 推荐机制，审核机制
- **定价争议**: 用户对付费作品价格不满
  - **解决方案**: 市场化定价，用户评价系统
- **收益分配**: 积分收益可能引发纠纷
  - **解决方案**: 明确交易规则，完整的交易记录

------

## 十一、总结

代码共享器模块是Code-Nest平台的重要组成部分，通过提供在线代码编辑和分享功能，构建活跃的前端开发者社区。该模块结合了在线编辑、实时预览、社区互动、创作激励和知识变现等特色功能，为用户提供了一个便捷、专业、有价值的代码创作和学习平台。

**核心创新点：**
1. **创作激励机制**: 发布作品奖励积分，鼓励用户持续创作优质内容
2. **知识变现体系**: 支持付费作品，让优质内容创作者获得积分收益
3. **源码保护机制**: 付费作品隐藏源码，Fork后可查看，保护作者权益
4. **灵活定价系统**: 作者可自定义Fork价格，市场化定价
5. **完整交易系统**: 积分交易、收益统计、交易记录一应俱全

通过分阶段实施，从核心编辑器到社区功能，再到知识变现功能和高级功能，逐步完善整个模块，最终打造一个功能完善、体验优秀、可持续发展的代码共享和变现平台。

