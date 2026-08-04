package com.xiaou.sre.service;

/**
 * 固定只读调查工具的一轮执行结果。
 *
 * @author xiaou
 */
public record SreReadOnlyToolResult(
        String toolKey,
        Long evidenceId,
        boolean created,
        String status
) {
}
