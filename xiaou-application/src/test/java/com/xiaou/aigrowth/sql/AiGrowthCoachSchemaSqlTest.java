package com.xiaou.aigrowth.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiGrowthCoachSchemaSqlTest {

    @Test
    void aiGrowthCoachSqlShouldDefineCoreTables() throws Exception {
        String sql = Files.readString(Path.of("../sql/v1.8.5/ai_growth_coach.sql"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_snapshot`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_action`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_chat_session`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_chat_message`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_config`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `ai_growth_coach_replan_log`"));
    }
}
