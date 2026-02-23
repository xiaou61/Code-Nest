package com.xiaou.system.service.impl;

import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.points.dto.AdminPointsStatisticsResponse;
import com.xiaou.points.service.PointsService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * 仪表板服务实现
 *
 * @author xiaou
 */
@Slf4j
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

    @Override
    public DashboardOverviewResponse getOverview() {
        TimedResult<Long> totalUsersTimed = timed(this::queryTotalUsers);
        TimedResult<AdminPointsStatisticsResponse> pointsTimed = timed(pointsService::getAdminStatistics);
        TimedResult<Integer> onlineUsersTimed = timed(this::queryOnlineUsers);
        TimedResult<Long> todayLoginTimed = timed(this::queryTodayLoginCount);
        TimedResult<Long> todayFailedOpsTimed = timed(this::queryTodayFailedOperationCount);
        TimedResult<List<DashboardOverviewResponse.RecentOperationItem>> recentOpsTimed = timed(this::queryRecentOperations);

        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setTotalUsers(totalUsersTimed.getValueOrDefault(0L));
        response.setTodayLoginCount(todayLoginTimed.getValueOrDefault(0L));
        response.setOnlineUserCount(onlineUsersTimed.getValueOrDefault(0));
        response.setTodayFailedOperationCount(todayFailedOpsTimed.getValueOrDefault(0L));

        AdminPointsStatisticsResponse pointsData = pointsTimed.getValue();
        response.setTotalPointsIssued(pointsData != null && pointsData.getTotalPointsIssued() != null
                ? pointsData.getTotalPointsIssued()
                : 0L);
        response.setActivePointUsers(pointsData != null && pointsData.getActiveUserCount() != null
                ? pointsData.getActiveUserCount()
                : 0);

        response.setModuleHealthList(buildModuleHealth(totalUsersTimed, pointsTimed, onlineUsersTimed, todayLoginTimed, todayFailedOpsTimed));
        response.setRecentOperations(recentOpsTimed.getValueOrDefault(Collections.emptyList()));
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
            TimedResult<Long> totalUsersTimed,
            TimedResult<AdminPointsStatisticsResponse> pointsTimed,
            TimedResult<Integer> onlineUsersTimed,
            TimedResult<Long> todayLoginTimed,
            TimedResult<Long> todayFailedOpsTimed
    ) {
        List<DashboardOverviewResponse.ModuleHealthItem> list = new ArrayList<>();
        list.add(buildHealthItem("用户服务", totalUsersTimed));
        list.add(buildHealthItem("积分服务", pointsTimed));
        list.add(buildHealthItem("聊天室服务", onlineUsersTimed));
        list.add(buildHealthItem("登录日志服务", todayLoginTimed));
        list.add(buildHealthItem("操作日志服务", todayFailedOpsTimed));
        return list;
    }

    private <T> DashboardOverviewResponse.ModuleHealthItem buildHealthItem(String name, TimedResult<T> timedResult) {
        DashboardOverviewResponse.ModuleHealthItem item = new DashboardOverviewResponse.ModuleHealthItem();
        item.setName(name);

        if (!timedResult.isSuccess()) {
            item.setLatency("--");
            item.setStatus("danger");
            item.setStatusText("异常");
            item.setStatusType("danger");
            return item;
        }

        item.setLatency(timedResult.getCostMs() + "ms");
        if (timedResult.getCostMs() > WARNING_THRESHOLD_MS) {
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

    private <T> TimedResult<T> timed(Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        try {
            T value = supplier.get();
            return new TimedResult<>(value, System.currentTimeMillis() - start, true);
        } catch (Exception e) {
            log.warn("仪表板子查询失败: {}", e.getMessage());
            return new TimedResult<>(null, System.currentTimeMillis() - start, false);
        }
    }

    private static class TimedResult<T> {
        private final T value;
        private final long costMs;
        private final boolean success;

        private TimedResult(T value, long costMs, boolean success) {
            this.value = value;
            this.costMs = costMs;
            this.success = success;
        }

        private T getValue() {
            return value;
        }

        private long getCostMs() {
            return costMs;
        }

        private boolean isSuccess() {
            return success;
        }

        private T getValueOrDefault(T defaultValue) {
            if (!success || value == null) {
                return defaultValue;
            }
            return value;
        }
    }
}
