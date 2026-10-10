-- 把两张账号表的字符集与排序规则显式钉死。
--
-- 大小写不敏感（utf8mb4_0900_ai_ci）是 users 的唯一性语义所依赖的：Alice 与 alice 必须算
-- 同一个用户名、同一个邮箱。V1 的建表语句没写 CHARSET/COLLATE，实际取值取决于目标库的默认值——
-- 换一个默认排序规则不同的实例，"防混淆"这条就会静默失效（唯一索引会把两者当成两条记录）。
--
-- 这里用 CONVERT TO 而不是改 V1：V1 已经在各环境执行过，改它会触发 Flyway 校验和不匹配，
-- 反而让所有环境的上下文起不来。两张表都很小，这次重建的成本可以忽略。

ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE refresh_tokens CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
