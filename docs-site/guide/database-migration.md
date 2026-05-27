# 数据库迁移指南

本文档说明如何进行数据库版本升级、表结构变更和数据迁移。

## 版本管理策略

Code Nest 使用**增量 SQL 脚本**管理数据库版本：

```
sql/
├── init.sql                    # 初始化脚本（全新安装）
├── v1.0.0/
│   ├── V1.0.0__init.sql       # 基础表结构
│   └── V1.0.0__data.sql       # 初始数据
├── v2.0.0/
│   ├── V2.0.0__alter_user.sql # 用户表变更
│   └── V2.0.0__add_points.sql # 新增积分表
└── v2.1.0/
    └── V2.1.0__ai_tables.sql  # AI 相关表
```

## 命名规范

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 初始化脚本 | `init.sql` | `init.sql` |
| 版本目录 | `v{major}.{minor}.{patch}/` | `v2.0.0/` |
| 变更脚本 | `V{version}__{description}.sql` | `V2.0.0__alter_user.sql` |
| 回滚脚本 | `V{version}__rollback_{description}.sql` | `V2.0.0__rollback_user.sql` |

**注意**：版本号使用双下划线 `__` 分隔。

## 新增表

### 1. 创建表脚本

```sql
-- V2.2.0__create_new_feature.sql
CREATE TABLE IF NOT EXISTS `new_feature` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT COMMENT '内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新功能表';
```

### 2. 字段命名规范

| 规则 | 说明 | 示例 |
| --- | --- | --- |
| 主键 | `id` BIGINT 自增 | `id BIGINT NOT NULL AUTO_INCREMENT` |
| 外键 | `{table}_id` | `user_id`, `article_id` |
| 状态 | `status` TINYINT | `status TINYINT DEFAULT 1` |
| 时间 | `create_time`, `update_time` | `DATETIME DEFAULT CURRENT_TIMESTAMP` |
| 删除 | `deleted` TINYINT | `deleted TINYINT DEFAULT 0` |
| 金额 | `{name}_amount` DECIMAL | `price_amount DECIMAL(10,2)` |

### 3. 索引命名规范

| 类型 | 格式 | 示例 |
| --- | --- | --- |
| 主键索引 | `PRIMARY` | 自动创建 |
| 唯一索引 | `uk_{field}` | `uk_username` |
| 普通索引 | `idx_{field}` | `idx_user_id` |
| 联合索引 | `idx_{field1}_{field2}` | `idx_user_id_status` |

## 修改表结构

### 新增字段

```sql
-- V2.2.0__add_avatar_to_user.sql
ALTER TABLE `user_info`
ADD COLUMN `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER `nickname`;
```

### 修改字段

```sql
-- V2.2.0__modify_title_length.sql
ALTER TABLE `new_feature`
MODIFY COLUMN `title` VARCHAR(500) NOT NULL COMMENT '标题';
```

### 删除字段

```sql
-- V2.2.0__drop_old_field.sql
ALTER TABLE `new_feature`
DROP COLUMN `old_field`;
```

### 新增索引

```sql
-- V2.2.0__add_index.sql
ALTER TABLE `new_feature`
ADD INDEX `idx_user_status` (`user_id`, `status`);
```

## 数据迁移

### 批量更新

```sql
-- V2.2.0__migrate_avatar.sql
UPDATE `user_info`
SET `avatar` = CONCAT('/files/avatar/', `id`, '.png')
WHERE `avatar` IS NULL;
```

### 数据复制

```sql
-- V2.2.0__copy_data.sql
INSERT INTO `new_table` (`user_id`, `title`, `content`)
SELECT `user_id`, `title`, `content`
FROM `old_table`
WHERE `status` = 1;
```

## 迁移检查清单

### 执行前

- [ ] 备份当前数据库
- [ ] 在测试环境验证脚本
- [ ] 确认脚本语法正确
- [ ] 检查是否有数据依赖

### 执行中

- [ ] 使用事务包裹（如支持）
- [ ] 记录执行日志
- [ ] 监控执行进度

### 执行后

- [ ] 验证表结构正确
- [ ] 验证数据完整性
- [ ] 测试应用功能
- [ ] 更新文档

## 回滚策略

### 保留回滚脚本

```sql
-- V2.2.0__rollback_new_feature.sql
DROP TABLE IF EXISTS `new_feature`;
```

### 回滚步骤

1. 停止应用服务
2. 执行回滚脚本
3. 恢复数据库备份（如需要）
4. 重启应用服务

## Flyway 集成（可选）

如果使用 Flyway 管理迁移：

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1.0.0
```

## 常见问题

### Q: 脚本执行失败怎么办？

**解决**：
1. 检查错误信息
2. 确认表是否存在
3. 确认字段类型是否匹配
4. 使用回滚脚本恢复

### Q: 如何处理大表变更？

**建议**：
1. 使用 `pt-online-schema-change` 工具
2. 分批执行（如更新 1000 条/次）
3. 在低峰期执行
4. 监控数据库负载

### Q: 如何同步开发和生产环境？

**流程**：
1. 开发环境执行并测试
2. 提交 SQL 脚本到 Git
3. 测试环境验证
4. 生产环境执行

## 相关文档

| 文档 | 说明 |
| --- | --- |
| [数据库与脚本](/architecture/database) | 数据库设计文档 |
| [数据表索引](/reference/database-tables) | 全部表清单 |
| [数据库字段阅读指南](/reference/database-field-guide) | 字段命名规范 |
| [Docker 与服务部署](/operations/docker) | 部署配置 |
