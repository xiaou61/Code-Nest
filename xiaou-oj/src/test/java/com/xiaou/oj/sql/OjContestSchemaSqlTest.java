package com.xiaou.oj.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OjContestSchemaSqlTest {

    @Test
    void contestSqlShouldDefineRequiredTables() throws IOException {
        String sql = Files.readString(resolveSqlPath());
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest_problem`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `oj_contest_participant`"));
        assertTrue(sql.contains("ALTER TABLE `oj_submission` ADD COLUMN `contest_id`"));
    }

    private Path resolveSqlPath() {
        Path path = Path.of("sql", "v1.8.1", "oj_contest.sql");
        if (Files.exists(path)) {
            return path;
        }
        Path fallback = Path.of("..", "sql", "v1.8.1", "oj_contest.sql");
        if (Files.exists(fallback)) {
            return fallback;
        }
        throw new IllegalStateException("未找到赛事SQL脚本: sql/v1.8.1/oj_contest.sql");
    }
}
