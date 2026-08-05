package com.xiaou.common.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * P6Spy SQL日志格式化
 * 
 * @author xiaou
 */
public class P6SpyLogger implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, 
                                String category, String prepared, String sql, String url) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        
        // 格式化SQL，去除多余空白
        String formattedSql = sql.replaceAll("\\s+", " ").trim();
        
        // 根据执行时间显示不同颜色标识
        String timeIndicator;
        if (elapsed > 1000) {
            timeIndicator = "🐢 SLOW";  // 慢查询 > 1s
        } else if (elapsed > 500) {
            timeIndicator = "⚠️ ";      // 较慢 > 500ms
        } else if (elapsed > 100) {
            timeIndicator = "⏱️ ";      // 一般 > 100ms
        } else {
            timeIndicator = "⚡";       // 快速 < 100ms
        }
        
        // 美化输出格式
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("┌──────────────────────────────────────────────────────────────\n");
        sb.append("│ ").append(timeIndicator).append(" 耗时: ").append(elapsed).append(" ms\n");
        sb.append("│ SQL : ").append(formattedSql).append("\n");
        sb.append("└──────────────────────────────────────────────────────────────");
        
        return sb.toString();
    }
}
