-- 账号与认证的两张表。
--
-- 三条约定（改动前先读）：
-- 1. 字符集与库一致（utf8mb4 / utf8mb4_0900_ai_ci）。该排序规则**大小写不敏感**，
--    因此 Alice 与 alice 视为同一用户名、同一邮箱。这是刻意的（防混淆），
--    但它是库的行为而不是应用写死的规则，所以显式记在这里。
-- 2. 审计时间走 MySQL 列默认值并存 UTC，不调用 NOW()/SYSDATE() 取业务时间。
-- 3. SQL 只写在 mapper（本项目用 @Mapper 接口 + 注解/文本块），不在 Java 里拼接。
--
-- refresh token 存的是 SHA-256 而不是随机串本身：令牌明文只在响应里出现一次，
-- 库被读走也无法直接冒用。

CREATE TABLE users (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username          VARCHAR(64)     NOT NULL,
  email             VARCHAR(254)    NOT NULL,
  -- 100 而不是 60：容纳后续算法升级（BCrypt 恰好 60 字符）
  password_hash     VARCHAR(100)    NOT NULL,
  role              VARCHAR(16)     NOT NULL,
  email_verified_at DATETIME(3)     NOT NULL,
  created_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at        DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE refresh_tokens (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id    BIGINT UNSIGNED NOT NULL,
  token_hash CHAR(64)        NOT NULL,
  -- 一条链一个 family：检测到重用时就吊销该 family 下全部记录
  family_id  CHAR(36)        NOT NULL,
  expires_at DATETIME(3)     NOT NULL,
  revoked_at DATETIME(3)     NULL,
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_refresh_token_hash (token_hash),
  KEY idx_refresh_family (family_id),
  KEY idx_refresh_expires (expires_at),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;
