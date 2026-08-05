package com.xiaou.system.service.impl;

import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.points.dto.AdminPointsStatisticsResponse;
import com.xiaou.points.service.PointsService;
import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.resilience.ResilientResult;
import com.xiaou.system.dto.DashboardOverviewResponse;
import com.xiaou.system.dto.LoginLogQueryRequest;
import com.xiaou.system.dto.LoginLogResponse;
import com.xiaou.system.dto.OperationLogQueryRequest;
import com.xiaou.system.dto.OperationLogResponse;
import com.xiaou.system.service.SysDashboardService;
import com.xiaou.system.service.SysLoginLogService;
import com.xiaou.system.service.SysOperationLogService;
import com.xiaou.user.dto.UserInfoResponse;
import com.xiaou.user.dto.UserQueryRequest;
import com.xiaou.user.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 仪表板服务实现
 *
 * @author xiaou
 */
@Service
@RequiredArgsConstructor
public class SysDashboardServiceImpl implements SysDashboardService {

    private static final long WARNING_THRESHOLD_MS = 800L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserInfoService userInfoService;
    private final PointsService pointsService;
    private final ChatRoomService chatRoomService;
    private final ChatOnlineUserService chatOnlineUserService;
    private final SysLoginLogService loginLogService;
    private final SysOperationLogService operationLogService;
    private final ResilientExecutor resilientExecutor;

    @Override
    public DashboardOverviewResponse getOverview() {
        ResilientResult<Long> totalUsersTimed = resilientExecutor.execute(
                "dashboard.total-users", this::queryTotalUsers);
        ResilientResult<AdminPointsStatisticsResponse> pointsTimed = resilientExecutor.execute(
                "dashboard.points", pointsService::getAdminStatistics);
        ResilientResult<Integer> onlineUsersTimed = resilientExecutor.execute(
                "dashboard.online-users", this::queryOnlineUsers);
        ResilientResult<Long> todayLoginTimed = resilientExecutor.execute(
                "dashboard.today-logins", this::queryTodayLoginCount);
        ResilientResult<Long> todayFailedOpsTimed = resilientExecutor.execute(
                "dashboard.failed-operations", this::queryTodayFailedOperationCount);
        ResilientResult<List<DashboardOverviewResponse.RecentOperationItem>> recentOpsTimed =
                resilientExecutor.execute("dashboard.recent-operations", this::queryRecentOperations);

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalUsers(totalUsersTimed.valueOr(0L));
        response.setTodayLoginCount(todayLoginTimed.valueOr(0L));
        response.setOnlineUserCount(onlineUsersTimed.valueOr(0));
        response.setTodayFailedOperationCount(todayFailedOpsTimed.valueOr(0L));

        AdminPointsStatisticsResponse pointsData = pointsTimed.value();
        response.setTotalPointsIssued(pointsData != null && pointsData.getTotalPointsIssued() != null
                ? pointsData.getTotalPointsIssued()
                : 0L);
        response.setActivePointUsers(pointsData != null && pointsData.getActiveUserCount() != null
                ? pointsData.getActiveUserCount()
                : 0);

        response.setModuleHealthList(buildModuleHealth(totalUsersTimed, pointsTimed, onlineUsersTimed, todayLoginTimed, todayFailedOpsTimed));
        response.setRecentOperations(recentOpsTimed.valueOr(Collections.emptyList()));
        return response;
    }

    private Long queryTotalUsers() {
        UserQueryRequest request = new UserQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        PageResult<UserInfoResponse> result = userInfoService.getUserList(request);
        return result != null && result.getTotal() != null ? result.getTotal() : 0L;
    }

    private Integer queryOnlineUsers() {
        Long roomId = chatRoomService.getOfficialRoom().getId();
        Integer count = chatOnlineUserService.getOnlineCount(roomId);
        return count == null ? 0 : count;
    }

    private Long queryTodayLoginCount() {
        LocalDate today = LocalDate.now();
        LoginLogQueryRequest request = new LoginLogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        request.setStartTime(today.atStartOfDay());
        request.setEndTime(today.atTime(23, 59, 59));
        request.setLoginStatus(0);
        PageResult<LoginLogResponse> result = loginLogService.getLoginLogPage(request);
        return result != null && result.getTotal() != null ? result.getTotal() : 0L;
    }

    private Long queryTodayFailedOperationCount() {
        LocalDate today = LocalDate.now();
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        request.setStartTime(today.atStartOfDay());
        request.setEndTime(today.atTime(23, 59, 59));
        request.setStatus(1);
        PageResult<OperationLogResponse> result = operationLogService.getOperationLogPage(request);
        return result != null && result.getTotal() != null ? result.getTotal() : 0L;
    }

    private List<DashboardOverviewResponse.RecentOperationItem> queryRecentOperations() {
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setPageNum(1);
        request.setPageSize(4);
        PageResult<OperationLogResponse> result = operationLogService.getOperationLogPage(request);
        if (result == null || result.getRecords() == null) {
            return Collections.emptyList();
        }

        List<DashboardOverviewResponse.RecentOperationItem> items = new ArrayList<>();
        for (OperationLogResponse operation : result.getRecords()) {
            DashboardOverviewResponse.RecentOperationItem item = new DashboardOverviewResponse.RecentOperationItem();
            item.setId(operation.getId());
            item.setTime(formatOperationTime(operation.getOperationTime()));

            String module = StringUtils.hasText(operation.getModule()) ? operation.getModule() : "系统";
            String typeText = StringUtils.hasText(operation.getOperationTypeText())
                    ? operation.getOperationTypeText()
                    : (StringUtils.hasText(operation.getOperationType()) ? operation.getOperationType() : "操作");
            item.setTitle(module + " · " + typeText);

            String desc = StringUtils.hasText(operation.getDescription())
                    ? operation.getDescription()
                    : (StringUtils.hasText(operation.getRequestUri()) ? operation.getRequestUri() : "无描述信息");
            item.setDesc(desc);
            items.add(item);
        }
        return items;
    }

    private List<DashboardOverviewResponse.ModuleHealthItem> buildModuleHealth(
            ResilientResult<Long> totalUsersTimed,
            ResilientResult<AdminPointsStatisticsResponse> pointsTimed,
            ResilientResult<Integer> onlineUsersTimed,
            ResilientResult<Long> todayLoginTimed,
            ResilientResult<Long> todayFailedOpsTimed
    ) {
        List<DashboardOverviewResponse.ModuleHealthItem> list = new ArrayList<>();
        list.add(buildHealthItem("用户服务", totalUsersTimed));
        list.add(buildHealthItem("积分服务", pointsTimed));
        list.add(buildHealthItem("聊天室服务", onlineUsersTimed));
        list.add(buildHealthItem("登录日志服务", todayLoginTimed));
        list.add(buildHealthItem("操作日志服务", todayFailedOpsTimed));
        return list;
    }

    private <T> DashboardOverviewResponse.ModuleHealthItem buildHealthItem(
            String name, ResilientResult<T> timedResult) {
        DashboardOverviewResponse.ModuleHealthItem item = new DashboardOverviewResponse.ModuleHealthItem();
        item.setName(name);

        if (!timedResult.succeeded()) {
            item.setLatency("--");
            item.setStatus("danger");
            item.setStatusText("异常");
            item.setStatusType("danger");
            return item;
        }

        item.setLatency(timedResult.durationMillis() + "ms");
        if (timedResult.durationMillis() > WARNING_THRESHOLD_MS) {
            item.setStatus("warning");
            item.setStatusText("较慢");
            item.setStatusType("warning");
        } else {
            item.setStatus("healthy");
            item.setStatusText("正常");
            item.setStatusType("success");
        }
        return item;
    }

    private String formatOperationTime(LocalDateTime operationTime) {
        if (operationTime == null) {
            return "--:--";
        }
        return operationTime.format(TIME_FORMATTER);
    }

}
