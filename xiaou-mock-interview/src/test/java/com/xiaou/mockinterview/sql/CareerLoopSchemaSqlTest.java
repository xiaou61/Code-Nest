package com.xiaou.mockinterview.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerLoopSchemaSqlTest {

    @Test
    void careerLoopSqlShouldDefineRequiredTables() throws Exception {
        String sql = Files.readString(resolveSqlPath());
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_session`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_stage_log`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_action`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_snapshot`"));
    }

    private Path resolveSqlPath() {
        Path path = Path.of("sql", "v1.8.2", "career_loop.sql");
        if (Files.exists(path)) {
            return path;
        }
        Path fallback = Path.of("..", "sql", "v1.8.2", "career_loop.sql");
        if (Files.exists(fallback)) {
            return fallback;
        }
        throw new IllegalStateException("Could not find SQL script: sql/v1.8.2/career_loop.sql");
    }
}
