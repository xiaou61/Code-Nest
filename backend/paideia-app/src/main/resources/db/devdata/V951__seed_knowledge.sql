-- 仅供开发与端到端测试使用的知识库种子数据。
--
-- **默认不加载**：本目录（classpath:db/devdata）不在任何 profile 的默认 locations 里，
-- 只有端到端测试显式加上它才会执行：
--   SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/devdata
-- 生产迁移目录（db/migration）里永远没有本文件。
--
-- 时间用固定字面量而不是 UTC_TIMESTAMP()：种子数据要可复现，不该随执行时刻变化。
--
-- 这些数据是给 e2e 用的，因此**刻意覆盖到每条查询路径**：
-- 两棵子树（验证分类树的展开与筛选）、三条已发布 + 一条草稿（验证草稿不可见）、
-- 两条关系（验证邻域视图与反向链接）、正文带二级标题（验证正文目录）。
-- 附件不在这里造：上传路径由集成测试自己走 multipart 覆盖。

INSERT INTO knowledge_categories (id, parent_id, name, slug, sort_order, created_at, updated_at)
VALUES
  (1, NULL, '后端基础', 'backend-basics',  10, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000'),
  (2, 1,    'Java 并发', 'java-concurrency', 10, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000'),
  (3, NULL, '架构',      'architecture',    20, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000');

INSERT INTO knowledge_entries (id, category_id, title, body, status, published_at, created_at, updated_at)
VALUES
  (1, 1, '什么是依赖注入',
   '## 它解决什么\n\n把"谁来造对象"从使用方手里拿走，交给装配层。\n\n## 常见误解\n\n依赖注入不等于必须用框架——构造器传参也是注入，只是没有容器帮你拼。\n',
   'published', '2026-01-02 00:00:00.000', '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000'),
  (2, 2, '线程与锁的基本认识',
   '## 共享可变状态才是问题\n\n线程本身不是问题，被多个线程同时读写的**状态**才是。\n\n## 锁的代价\n\n加锁会把并行变回串行，所以先想能不能不共享。\n',
   'published', '2026-01-03 00:00:00.000', '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000'),
  (3, 3, '模块化单体的边界',
   '## 边界靠什么保证\n\n约定会被违反，所以边界要由**构建期检查**来守。\n\n| 手段 | 违约时 |\n| --- | --- |\n| 模块依赖声明 | 编译失败 |\n| 内部包访问 | 构建期校验失败 |\n\n```java\nApplicationModules.of(App.class).verify();\n```\n',
   'published', '2026-01-04 00:00:00.000', '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000'),
  (4, 2, '虚拟线程的落地注意（草稿）',
   '## 待补\n\n这条是草稿，用来验证学习者端看不到它、而管理员能预览。\n',
   'draft', NULL, '2026-01-01 00:00:00.000', '2026-01-01 00:00:00.000');

INSERT INTO knowledge_entry_relations (id, from_entry_id, to_entry_id, relation_type, created_at)
VALUES
  (1, 1, 3, 'prerequisite', '2026-01-01 00:00:00.000'),
  (2, 2, 3, 'related',      '2026-01-01 00:00:00.000');
