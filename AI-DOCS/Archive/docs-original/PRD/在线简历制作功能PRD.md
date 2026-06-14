# 在线简历制作功能 PRD (产品需求文档)

## 1. 产品概述

### 1.1 产品定位
在线简历制作功能是Code-Nest程序员社区系统的重要扩展，为用户提供专业的简历创建、编辑、管理和导出服务。该功能将集成现有的用户系统和文件存储系统，为程序员群体提供定制化的简历解决方案。

### 1.2 产品价值
- **用户价值**：提供便捷的简历制作工具，提升求职竞争力
- **平台价值**：完善社区生态，增强用户粘性，打造程序员工具集
- **商业价值**：拓展服务范围，为后续增值服务奠定基础

### 1.3 目标用户
- **主要用户**：求职中的程序员、开发者
- **次要用户**：技术从业者、IT相关专业学生
- **潜在用户**：需要技术简历���其他岗位从业者

## 2. 功能需求

### 2.1 核心功能模块

#### 2.1.1 简历模板管理
- **模板分类**：按职业方向（前端、后端、全栈、移动端、算法等）分类
- **模板预览**：支持模板缩略图预览和详细信息展示
- **模板标签**：技术栈标签、经验等级标签、行业标签
- **模板评分**：用户可对使用过的模板进行评分和评论

#### 2.1.2 简历编辑器
- **所见即所得编辑**：实时预览编辑效果
- **模块化编辑**：个人信息、教育背景、工作经验、项目经验、技能特长、证书等模块
- **智能提示**：技术栈自动补全、工作经历智能描述建议
- **版本管理**：支持简历版本历史和回滚

#### 2.1.3 简历生成与导出
- **多格式导出**：PDF、Word、HTML、图片格式
- **在线分享**：生成分享链接，支持访问统计
- **打印优化**：针对不同纸张尺寸的打印优化
- **批量操作**：支持批量导出和格式转换

#### 2.1.4 个人简历管理
- **简历列表**：个人简历管理中心，支持搜索和分类
- **简历复制**：基于现有简历创建副本并修改
- **访问统计**：查看分享链接的访问次数和来源
- **隐私控制**：设置简历的可见性和访问权限

### 2.2 高级功能

#### 2.2.1 AI智能优化
- **内容优化建议**：基于岗位需求的简历内容优化建议
- **技能匹配**：根据目标职位自动调整技能展示优先级
- **语言润色**：简历描述的语言优化和语法检查

#### 2.2.2 数据统计分析
- **简历完整性检查**：自动检查简历信息完整性并给出建议
- **竞争力分析**：与同岗位简历进行对比分析
- **热门技能统计**：展示当前市场热门技术栈

#### 2.2.3 社区功能集成
- **简历展示**：优秀简历公开展示（需用户授权）
- **经验分享**：简历制作经验和技巧分享
- **互助评审**：用户间简历评审和建议功能

## 3. 技术架构

### 3.1 后端架构
- **新增模块**：`xiaou-resume` 简历模块
- **依赖模块**：`xiaou-user`、`xiaou-filestorage`、`xiaou-common`
- **技术栈**：Spring Boot 3.4.4 + MyBatis + MySQL + Redis

### 3.2 前端架构
- **管理端**：基于现有vue3-admin-front扩展
- **用户端**：基于现有vue3-user-front扩展
- **编辑器**：集成富文本编辑器（如Quill.js或TinyMCE）

### 3.3 数据库设计
新增数据表：
- `resume_templates` - 简历模板表
- `resume_info` - 简历基本信息表
- `resume_sections` - 简历模块内容表
- `resume_versions` - 简历版本历史表
- `resume_shares` - 简历分享记录表
- `resume_analytics` - 简历访问统计表

## 4. 接口设计

### 4.1 用户端接口
```
GET    /api/resume/templates          # 获取模板列表
POST   /api/resume                   # 创建简历
GET    /api/resume                   # 获取个人简历列表
PUT    /api/resume/{id}              # 更新简历
DELETE /api/resume/{id}              # 删除简历
GET    /api/resume/{id}/preview      # 预览简历
POST   /api/resume/{id}/export       # 导出简历
POST   /api/resume/{id}/share        # 创建分享链接
GET    /api/resume/{id}/analytics    # 获取访问统计
```

### 4.2 管理端接口
```
GET    /api/admin/resume/templates   # 模板管理
POST   /api/admin/resume/templates   # 创建模板
PUT    /api/admin/resume/templates/{id} # 更新模板
DELETE /api/admin/resume/templates/{id} # 删除模板
GET    /api/admin/resume/analytics   # 平台统计数据
GET    /api/admin/resume/reports     # 异常报告
```

## 5. 开发计划

### 5.1 第一阶段（基础功能）
- [x] 模块结构搭建
- [x] 数据库设计实现
- [x] 基础CRUD功能
- [x] 简单模板系统

### 5.2 第二阶段（核心编辑）
- [ ] 富文本编辑器集成
- [ ] 模板渲染引擎
- [ ] PDF导出功能
- [ ] 用户界面优化

### 5.3 第三阶段（高级功能）
- [ ] AI优化建议
- [ ] 在线分享功能
- [ ] 数据统计分析
- [ ] 社区功能集成

### 5.4 第四阶段（优化完善）
- [ ] 性能优化
- [ ] 安全加固
- [ ] 用户体验提升
- [ ] 全面测试

## 6. 风险评估

### 6.1 技术风险
- **PDF生成性能**：大量并发导出可能影响系统性能
- **存储空间**：简历文件和图片���要合理规划存储策略
- **编辑器兼容性**：不同浏览器编辑器兼容性问题

### 6.2 业务风险
- **用户接受度**：需要确保功能易用性和实用性
- **模板版权**：模板设计需要确保版权合规
- **数据安全**：简历包含个人信息，需要严格的数据保护

### 6.3 运营风险
- **维护成本**：模板和功能需要持续更新维护
- **技术支持**：可能需要专门的用户支持团队

## 7. 成功指标

### 7.1 功能指标
- 简历创建成功率 > 99%
- PDF导出成功率 > 98%
- 页面加载时间 < 3秒

### 7.2 业务指标
- 月活跃用户数 > 1000
- 简历创建数量月增长 > 20%
- 用户满意度 > 4.5/5.0

### 7.3 技术指标
- 系统可用性 > 99.9%
- 并发支持 > 500用户
- 数据完整性 > 99.99%

## 8. 详细数据库设计

### 8.1 简历模板表 (resume_templates)

```sql
CREATE TABLE `resume_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `description` text COMMENT '模板描述',
  `category` varchar(50) NOT NULL COMMENT '模板分类(frontend/backend/fullstack/mobile/algorithm)',
  `thumbnail` varchar(500) COMMENT '模板缩略图URL',
  `preview_url` varchar(500) COMMENT '模板预览图URL',
  `template_content` longtext NOT NULL COMMENT '模板内容(JSON格式)',
  `tags` varchar(200) COMMENT '模板标签(逗号分隔)',
  `is_premium` tinyint(1) DEFAULT 0 COMMENT '是否为付费模板',
  `download_count` int DEFAULT 0 COMMENT '下载次数',
  `rating` decimal(3,2) DEFAULT 0.00 COMMENT '平均评分',
  `rating_count` int DEFAULT 0 COMMENT '评分人数',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
  `created_by` bigint COMMENT '创建者ID',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模板表';
```

### 8.2 简历信息表 (resume_info)

```sql
CREATE TABLE `resume_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '简历ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(200) NOT NULL COMMENT '简历标题',
  `template_id` bigint COMMENT '使用的模板ID',
  `target_position` varchar(100) COMMENT '目标职位',
  `summary` text COMMENT '个人简介',
  `is_public` tinyint(1) DEFAULT 0 COMMENT '是否公开',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否为默认简历',
  `view_count` int DEFAULT 0 COMMENT '查看次数',
  `download_count` int DEFAULT 0 COMMENT '下载次数',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态(0:删除 1:草稿 2:完成)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_user_default` (`user_id`, `is_default`) WHERE is_default = 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历基本信息表';
```

### 8.3 简历模块内容表 (resume_sections)

```sql
CREATE TABLE `resume_sections` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `section_type` varchar(50) NOT NULL COMMENT '模块类型(personal/education/experience/project/skill/certificate)',
  `section_title` varchar(100) NOT NULL COMMENT '模块标题',
  `section_content` longtext NOT NULL COMMENT '模块内容(JSON格式)',
  `sort_order` int DEFAULT 0 COMMENT '排序顺序',
  `is_visible` tinyint(1) DEFAULT 1 COMMENT '是否显示',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_section_type` (`section_type`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历模块内容表';
```

### 8.4 简历版本历史表 (resume_versions)

```sql
CREATE TABLE `resume_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `version_number` int NOT NULL COMMENT '版本号',
  `version_name` varchar(100) COMMENT '版本名称',
  `change_description` text COMMENT '变更描述',
  `snapshot_data` longtext NOT NULL COMMENT '快照数据(JSON格式)',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_version_number` (`version_number`),
  UNIQUE KEY `uk_resume_version` (`resume_id`, `version_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历版本历史表';
```

### 8.5 简历分享记录表 (resume_shares)

```sql
CREATE TABLE `resume_shares` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分享ID',
  `resume_id` bigint NOT NULL COMMENT '简��ID',
  `share_code` varchar(32) NOT NULL COMMENT '分享码',
  `share_url` varchar(500) NOT NULL COMMENT '分享链接',
  `password` varchar(20) COMMENT '访问密码',
  `expire_time` datetime COMMENT '过期时间',
  `access_count` int DEFAULT 0 COMMENT '访问次数',
  `max_access` int COMMENT '最大访问次数限制',
  `is_enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_share_code` (`share_code`),
  UNIQUE KEY `uk_share_code` (`share_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历分享记录表';
```

### 8.6 简历访问统计表 (resume_analytics)

```sql
CREATE TABLE `resume_analytics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `resume_id` bigint NOT NULL COMMENT '简历ID',
  `share_id` bigint COMMENT '分享记录ID',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `ip_address` varchar(45) COMMENT 'IP地址',
  `user_agent` varchar(500) COMMENT '用户代理',
  `referrer` varchar(500) COMMENT '来源页面',
  `location` varchar(100) COMMENT '地理位置',
  `device_type` varchar(20) COMMENT '设备类型',
  `browser` varchar(50) COMMENT '浏览器',
  `os` varchar(50) COMMENT '操作系统',
  PRIMARY KEY (`id`),
  KEY `idx_resume_id` (`resume_id`),
  KEY `idx_share_id` (`share_id`),
  KEY `idx_access_time` (`access_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历访问统计表';
```

## 9. 界面设计规范

### 9.1 设计原则
- **一致性**：与现有Code-Nest系统保持UI/UX一致性
- **简洁性**：界面简洁明了，操作流程清晰
- **响应式**：支持不同屏幕尺寸的设备访问
- **专业性**：体现程序员社区的专业特色

### 9.2 主要页面设计

#### 9.2.1 简历管理页面
- 左侧：简历列表（支持搜索、筛选、排序）
- 右侧：简历预览和操作按钮
- 顶部：创建简历、导入简历等快捷操作

#### 9.2.2 简历编辑页面
- 左侧：模块导航（可拖拽排序）
- 中间：编辑区域（所见即所得）
- 右侧：属性面板和样式设置
- 顶部：保存、预览、导出等操作

#### 9.2.3 模板选择页面
- 网格布局展示模板
- 支持分类筛选和搜索
- 模板预览悬浮效果
- 快速应用模板功能

## 10. 安全与隐私

### 10.1 数据安全
- **传输加密**：全程HTTPS传输
- **存储加密**：敏感信息加密存储
- **访问控制**：严格的权限验证机制
- **数据备份**：定期数据备份和恢复测试

### 10.2 隐私保护
- **用户授权**：简历分享需要用户明确授权
- **数据最小化**：只收集必要的用户信息
- **访问记录**：详细的操作日志记录
- **数据删除**：支持用户彻底删除个人数据

---

**PRD版本**：v1.0
**创建日期**：2025-11-12
**最后更新**：2025-11-12
**负责人**：产品团队
**审核人**：技术团队