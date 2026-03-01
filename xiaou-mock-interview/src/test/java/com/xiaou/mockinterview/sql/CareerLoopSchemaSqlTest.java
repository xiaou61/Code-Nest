package com.xiaou.mockinterview.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CareerLoopSchemaSqlTest {

    @Test
    void careerLoopSqlShouldDefineRequiredTables() throws Exception {
        String sql = Files.readString(Path.of("../sql/v1.8.3/career_loop.sql"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_session`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_stage_log`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_action`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `career_loop_snapshot`"));
    }
}
