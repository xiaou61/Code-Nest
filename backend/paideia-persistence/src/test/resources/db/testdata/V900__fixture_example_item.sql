-- 测试作用域的示例表：仅用于验证分页与授权隔离，不进入生产 schema。
-- 版本号从 900 起，避免与生产迁移（从 1 起）冲突。
CREATE TABLE IF NOT EXISTS t_example_item
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id   VARCHAR(64)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_example_item_owner (owner_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
