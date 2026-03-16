package com.xiaou.techbriefing.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TechBriefingSchemaSqlTest {

    @Test
    void techBriefingSqlShouldDefineCoreTables() throws Exception {
        String sql = Files.readString(Path.of("../sql/v1.8.5/tech_briefing.sql"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_source`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_article`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_article_content`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_article_ai`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_subscription`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `tech_briefing_fetch_log`"));
    }
}
