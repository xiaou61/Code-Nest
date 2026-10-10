-- 仅供开发与端到端测试使用的种子账号。
--
-- **默认不加载**：本目录（classpath:db/devdata）不在任何 profile 的默认 locations 里，
-- 只有端到端测试显式加上它才会执行：
--   SPRING_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/devdata
-- 生产迁移目录（db/migration）里永远没有本文件。
--
-- **口令是固定的明文对应值**，因此这是本仓库里唯一已知口令的账号：
--   seed-learner / seed-learner-password
--   seed-admin   / seed-admin-password
-- 仅用于本地与测试库（paideia_test）。任何他人可访问的环境都不得加载本迁移。
--
-- 时间用固定字面量而不是 UTC_TIMESTAMP()：种子数据要可复现，不该随执行时刻变化。

INSERT INTO users (username, email, password_hash, role, email_verified_at, created_at, updated_at)
VALUES
  ('seed-learner',
   'seed-learner@example.test',
   '$2a$10$yeTcR4AZnZhA.xO4T.rK4.PDy8qpFeYgwxpsjMOAAKby3AQ6/AlNy',
   'learner',
   '2026-01-01 00:00:00.000',
   '2026-01-01 00:00:00.000',
   '2026-01-01 00:00:00.000'),
  ('seed-admin',
   'seed-admin@example.test',
   '$2a$10$aOkw09eMRJIoRyeZ7FsCSu40lueJ4vZ2lFwtbCxcZxeYWpE3/WkRm',
   'admin',
   '2026-01-01 00:00:00.000',
   '2026-01-01 00:00:00.000',
   '2026-01-01 00:00:00.000');
