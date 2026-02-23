package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 仪表板总览响应
 *
 * @author xiaou
 */
@Data
@Schema(description = "管理端仪表板总览响应")
public class DashboardOverviewResponse {

    @Schema(description = "用户总量")
    private Long totalUsers;

    @Schema(description = "今日登录数")
    private Long todayLoginCount;

    @Schema(description = "在线会话数")
    private Integer onlineUserCount;

    @Schema(description = "今日失败操作数")
    private Long todayFailedOperationCount;

    @Schema(description = "累计积分发放")
    private Long totalPointsIssued;

    @Schema(description = "活跃积分用户数")
    private Integer activePointUsers;

    @Schema(description = "模块健康")
    private List<ModuleHealthItem> moduleHealthList;

    @Schema(description = "最近操作")
    private List<RecentOperationItem> recentOperations;

    @Data
    @Schema(description = "模块健康项")
    public static class ModuleHealthItem {
        @Schema(description = "模块名称")
        private String name;

        @Schema(description = "耗时，如 52ms")
        private String latency;

        @Schema(description = "状态标识：healthy/warning/danger")
        private String status;

        @Schema(description = "状态文案")
        private String statusText;

        @Schema(description = "Element Plus 标签类型：success/warning/danger")
        private String statusType;
    }

    @Data
    @Schema(description = "最近操作项")
    public static class RecentOperationItem {
        @Schema(description = "日志ID")
        private Long id;

        @Schema(description = "时间，HH:mm")
        private String time;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "描述")
        private String desc;
    }
}
