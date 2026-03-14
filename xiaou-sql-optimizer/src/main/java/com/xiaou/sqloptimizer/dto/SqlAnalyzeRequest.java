package com.xiaou.sqloptimizer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * SQL分析请求DTO
 *
 * @author xiaou
 */
@Data
public class SqlAnalyzeRequest {

    /**
     * 待优化的SQL语句
     */
    @NotBlank(message = "SQL语句不能为空")
    @Size(max = 20000, message = "SQL语句长度不能超过20000个字符")
    private String sql;

    /**
     * EXPLAIN执行计划结果
     */
    @NotBlank(message = "EXPLAIN结果不能为空")
    @Size(max = 50000, message = "EXPLAIN结果长度不能超过50000个字符")
    private String explainResult;

    /**
     * EXPLAIN格式（TABLE/JSON）
     */
    @Pattern(regexp = "^(TABLE|JSON)$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "EXPLAIN格式仅支持TABLE或JSON")
    private String explainFormat = "TABLE";

    /**
     * 表结构列表
     */
    @Valid
    @NotEmpty(message = "表结构不能为空")
    @Size(max = 20, message = "表结构最多支持20项")
    private List<TableStructure> tableStructures;

    /**
     * MySQL版本
     */
    @Size(max = 20, message = "MySQL版本长度不能超过20个字符")
    private String mysqlVersion = "8.0";

    /**
     * 表结构
     */
    @Data
    public static class TableStructure {
        /**
         * 表名
         */
        @Size(max = 64, message = "表名长度不能超过64个字符")
        private String tableName;

        /**
         * DDL语句
         */
        @NotBlank(message = "表结构DDL不能为空")
        @Size(max = 10000, message = "表结构DDL长度不能超过10000个字符")
        private String ddl;
    }
}
