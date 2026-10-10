-- 知识库的五张表。
--
-- 三条约定与 V1 一致（改动前先读）：
-- 1. 字符集与库一致（utf8mb4 / utf8mb4_0900_ai_ci），排序规则大小写不敏感。
-- 2. 审计时间走 MySQL 列默认值并存 UTC，不调用 NOW()/SYSDATE() 取业务时间。
-- 3. SQL 只写在 mapper（本项目用 @Mapper 接口 + 文本块），不在 Java 里拼接。
--
-- 两处刻意的取舍：
-- * 正文用 MEDIUMTEXT 存 markdown 源码，不另存渲染后的 HTML——渲染在前端做，
--   库里只保留内容的唯一形态。
-- * FULLTEXT 必须带 WITH PARSER ngram：默认分词器按空格切词，中文没有空格，
--   不加这一句中文检索基本不召回。代价是小于分词长度的短查询无召回，
--   由 SearchQuery 判定退回 LIKE。

CREATE TABLE knowledge_categories (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  -- NULL = 根分类；自引用即层级树
  parent_id  BIGINT UNSIGNED NULL,
  name       VARCHAR(64)     NOT NULL,
  slug       VARCHAR(64)     NOT NULL,
  sort_order INT             NOT NULL DEFAULT 0,
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_categories_slug (slug),
  KEY idx_knowledge_categories_parent (parent_id),
  CONSTRAINT fk_knowledge_categories_parent FOREIGN KEY (parent_id)
    REFERENCES knowledge_categories (id)
) ENGINE = InnoDB;

CREATE TABLE knowledge_entries (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  -- 条目归属**单个**分类（WORK-004 决定 10）；NULL = 未分类
  category_id  BIGINT UNSIGNED NULL,
  title        VARCHAR(200)    NOT NULL,
  body         MEDIUMTEXT      NOT NULL,
  -- draft | published；学习者只读查询在 SQL 层就带上 status = 'published'
  status       VARCHAR(16)     NOT NULL DEFAULT 'draft',
  published_at DATETIME(3)     NULL,
  created_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at   DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_knowledge_entries_category (category_id),
  KEY idx_knowledge_entries_status (status),
  CONSTRAINT fk_knowledge_entries_category FOREIGN KEY (category_id)
    REFERENCES knowledge_categories (id),
  FULLTEXT KEY ft_knowledge_entries_title_body (title, body) WITH PARSER ngram
) ENGINE = InnoDB;

CREATE TABLE knowledge_entry_relations (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  from_entry_id BIGINT UNSIGNED NOT NULL,
  to_entry_id   BIGINT UNSIGNED NOT NULL,
  -- prerequisite（有向：from 是 to 的前置）| related（按无向处理）
  relation_type VARCHAR(32)     NOT NULL,
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_relations_edge (from_entry_id, to_entry_id, relation_type),
  KEY idx_knowledge_relations_to (to_entry_id),
  -- 删条目不留悬空边
  CONSTRAINT fk_knowledge_relations_from FOREIGN KEY (from_entry_id)
    REFERENCES knowledge_entries (id) ON DELETE CASCADE,
  CONSTRAINT fk_knowledge_relations_to FOREIGN KEY (to_entry_id)
    REFERENCES knowledge_entries (id) ON DELETE CASCADE,
  CONSTRAINT ck_knowledge_relations_not_self CHECK (from_entry_id <> to_entry_id)
) ENGINE = InnoDB;

CREATE TABLE knowledge_files (
  -- UUID 而不是自增：读取端点免鉴权（用户裁决），自增等于把全部附件按 /files/1、/files/2 列出来
  id            CHAR(36)        NOT NULL,
  original_name VARCHAR(255)    NOT NULL,
  -- 入库时嗅探所得的类型，不是客户端声明的；返回时也用它
  content_type  VARCHAR(64)     NOT NULL,
  size_bytes    BIGINT UNSIGNED NOT NULL,
  -- 上传者账号 id，取自令牌
  uploader_id   BIGINT UNSIGNED NOT NULL,
  created_at    DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_knowledge_files_uploader (uploader_id)
) ENGINE = InnoDB;

CREATE TABLE knowledge_self_assessments (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  -- 来自令牌，不接受客户端传入
  user_id    BIGINT UNSIGNED NOT NULL,
  entry_id   BIGINT UNSIGNED NOT NULL,
  -- understood | unsure
  level      VARCHAR(16)     NOT NULL,
  created_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  -- upsert 的唯一落点；"取消标记"就是一次 delete
  UNIQUE KEY uk_knowledge_self_assessments (user_id, entry_id),
  KEY idx_knowledge_self_assessments_entry (entry_id),
  KEY idx_knowledge_self_assessments_user_level (user_id, level),
  CONSTRAINT fk_knowledge_self_assessments_entry FOREIGN KEY (entry_id)
    REFERENCES knowledge_entries (id) ON DELETE CASCADE
) ENGINE = InnoDB;
