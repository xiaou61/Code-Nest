package com.xiaou.learningasset.sql;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningAssetSchemaSqlTest {

    @Test
    void learningAssetSqlShouldDefineCoreTables() throws Exception {
        String sql = Files.readString(Path.of("../sql/v1.8.4/learning_asset.sql"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_record`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_candidate`"));
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS `learning_asset_publish_log`"));
    }
}
