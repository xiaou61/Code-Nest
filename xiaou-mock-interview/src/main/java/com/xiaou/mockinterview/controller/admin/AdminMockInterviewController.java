package com.xiaou.mockinterview.controller.admin;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.utils.PageHelper;
import com.xiaou.mockinterview.domain.MockInterviewDirection;
import com.xiaou.mockinterview.domain.MockInterviewQA;
import com.xiaou.mockinterview.domain.MockInterviewSession;
import com.xiaou.mockinterview.dto.request.InterviewHistoryRequest;
import com.xiaou.mockinterview.dto.response.admin.AdminInterviewSessionDetailResponse;
import com.xiaou.mockinterview.dto.response.admin.AdminInterviewStatsOverviewResponse;
import com.xiaou.mockinterview.enums.InterviewLevelEnum;
import com.xiaou.mockinterview.enums.InterviewStyleEnum;
import com.xiaou.mockinterview.enums.QAStatusEnum;
import com.xiaou.mockinterview.enums.QuestionTypeEnum;
import com.xiaou.mockinterview.mapper.MockInterviewDirectionMapper;
import com.xiaou.mockinterview.mapper.MockInterviewQAMapper;
import com.xiaou.mockinterview.mapper.MockInterviewSessionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端模拟面试控制器
 *
 * @author xiaou
 */
@Tag(name = "模拟面试-管理端", description = "管理端模拟面试相关接口")
@RestController
@RequestMapping("/admin/mock-interview")
@RequiredArgsConstructor
public class AdminMockInterviewController {

    private final MockInterviewDirectionMapper directionMapper;
    private final MockInterviewSessionMapper sessionMapper;
    private final MockInterviewQAMapper qaMapper;

    // =================== 方向配置管理 ===================

    @Operation(summary = "获取方向配置列表")
    @GetMapping("/directions")
    public Result<List<MockInterviewDirection>> getDirections() {
        List<MockInterviewDirection> directions = directionMapper.selectAll();
        return Result.success(directions);
    }

    @Operation(summary = "新增方向配置")
    @PostMapping("/directions")
    public Result<Long> addDirection(@RequestBody DirectionRequest request) {
        MockInterviewDirection direction = new MockInterviewDirection()
                .setDirectionCode(request.getDirectionCode())
                .setDirectionName(request.getDirectionName())
                .setIcon(request.getIcon())
                .setDescription(request.getDescription())
                .setCategoryIds(request.getCategoryIds())
                .setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .setStatus(1)
                .setCreateTime(LocalDateTime.now());
        directionMapper.insert(direction);
        return Result.success("新增成功", direction.getId());
    }

    @Operation(summary = "更新方向配置")
    @PutMapping("/directions/{id}")
    public Result<Void> updateDirection(@PathVariable Long id, @RequestBody DirectionRequest request) {
        MockInterviewDirection direction = directionMapper.selectById(id);
        if (direction == null) {
            return Result.error("方向配置不存在");
        }

        direction.setDirectionName(request.getDirectionName())
                .setIcon(request.getIcon())
                .setDescription(request.getDescription())
                .setCategoryIds(request.getCategoryIds())
                .setSortOrder(request.getSortOrder());
        directionMapper.updateById(direction);
        return Result.success();
    }

    @Operation(summary = "删除方向配置")
    @DeleteMapping("/directions/{id}")
    public Result<Void> deleteDirection(@PathVariable Long id) {
        directionMapper.deleteById(id);
        return Result.success();
    }

    @Operation(summary = "更新方向状态")
    @PutMapping("/directions/{id}/status")
    public Result<Void> updateDirectionStatus(@PathVariable Long id, @RequestParam Integer status) {
        directionMapper.updateStatus(id, status);
        return Result.success();
    }

    // =================== 面试记录管理 ===================

    @Operation(summary = "获取面试记录列表")
    @PostMapping("/sessions")
    public Result<PageResult<MockInterviewSession>> getSessions(@RequestBody(required = false) InterviewHistoryRequest request) {
        if (request == null) {
            request = new InterviewHistoryRequest();
        }
        InterviewHistoryRequest finalRequest = request;
        PageResult<MockInterviewSession> result = PageHelper.doPage(request.getPageNum(), request.getPageSize(),
                () -> sessionMapper.selectPageAll(finalRequest));

        Map<String, String> directionNameMap = buildDirectionNameMap();
        if (result.getRecords() != null) {
            result.getRecords().forEach(session -> {
                fillSessionDisplayFields(session, directionNameMap);
                session.setAnsweredCount(qaMapper.countAnsweredBySessionId(session.getId()));
            });
        }
        return Result.success(result);
    }

    @Operation(summary = "获取面试会话详情")
    @GetMapping("/sessions/{id}")
    public Result<AdminInterviewSessionDetailResponse> getSessionDetail(@PathVariable Long id) {
        MockInterviewSession session = sessionMapper.selectById(id);
        if (session == null) {
            return Result.error("面试会话不存在");
        }

        fillSessionDisplayFields(session, buildDirectionNameMap());

        List<MockInterviewQA> qaList = qaMapper.selectMainQuestionsBySessionId(id);
        qaList.forEach(mainQa -> mainQa.setFollowUps(qaMapper.selectFollowUpsByParentId(mainQa.getId())));

        List<MockInterviewQA> allQa = qaMapper.selectBySessionId(id);
        int answeredCount = (int) allQa.stream()
                .filter(qa -> Objects.equals(qa.getStatus(), QAStatusEnum.ANSWERED.getCode()))
                .count();
        int skippedCount = (int) allQa.stream()
                .filter(qa -> Objects.equals(qa.getStatus(), QAStatusEnum.SKIPPED.getCode()))
                .count();
        int pendingCount = (int) allQa.stream()
                .filter(qa -> Objects.equals(qa.getStatus(), QAStatusEnum.PENDING.getCode()))
                .count();
        int followUpCount = (int) allQa.stream()
                .filter(qa -> Objects.equals(qa.getQuestionType(), QuestionTypeEnum.FOLLOW_UP.getCode()))
                .count();

        AdminInterviewSessionDetailResponse response = new AdminInterviewSessionDetailResponse();
        response.setSession(session);
        response.setQaList(qaList);
        response.setAnsweredCount(answeredCount);
        response.setSkippedCount(skippedCount);
        response.setPendingCount(pendingCount);
        response.setFollowUpCount(followUpCount);
        return Result.success(response);
    }

    @Operation(summary = "获取运营统计概览")
    @GetMapping("/stats/overview")
    public Result<AdminInterviewStatsOverviewResponse> getStatsOverview(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Map<String, Object> overviewMap = sessionMapper.selectAdminOverviewStats(startTime, endTime);
        if (overviewMap == null) {
            overviewMap = Collections.emptyMap();
        }

        long totalSessions = toLong(overviewMap.get("totalSessions"));
        long completedSessions = toLong(overviewMap.get("completedSessions"));
        long ongoingSessions = toLong(overviewMap.get("ongoingSessions"));
        long interruptedSessions = toLong(overviewMap.get("interruptedSessions"));
        BigDecimal avgScore = toDecimal(overviewMap.get("avgScore"));
        int avgDurationMinutes = toInt(overviewMap.get("avgDurationMinutes"));
        long activeUsers = toLong(overviewMap.get("activeUsers"));

        AdminInterviewStatsOverviewResponse response = new AdminInterviewStatsOverviewResponse();
        response.setTotalSessions(totalSessions);
        response.setCompletedSessions(completedSessions);
        response.setOngoingSessions(ongoingSessions);
        response.setInterruptedSessions(interruptedSessions);
        response.setAvgScore(avgScore);
        response.setAvgDurationMinutes(avgDurationMinutes);
        response.setActiveUsers(activeUsers);
        response.setCompletionRate(calcRate(completedSessions, totalSessions));

        List<Map<String, Object>> directionRows = sessionMapper.selectDirectionDistributionStats(startTime, endTime);
        if (directionRows == null) {
            directionRows = Collections.emptyList();
        }
        List<AdminInterviewStatsOverviewResponse.DirectionDistribution> distributions = directionRows.stream()
                .map(row -> {
                    AdminInterviewStatsOverviewResponse.DirectionDistribution item =
                            new AdminInterviewStatsOverviewResponse.DirectionDistribution();
                    long sessionCount = toLong(row.get("sessionCount"));
                    long completedCount = toLong(row.get("completedCount"));
                    item.setDirection(String.valueOf(row.get("direction")));
                    item.setDirectionName(String.valueOf(row.get("directionName")));
                    item.setSessionCount(sessionCount);
                    item.setAvgScore(toDecimal(row.get("avgScore")));
                    item.setCompletionRate(calcRate(completedCount, sessionCount));
                    return item;
                })
                .collect(Collectors.toList());
        response.setDirectionDistributions(distributions);
        return Result.success(response);
    }

    // =================== 内部类 ===================

    private void fillSessionDisplayFields(MockInterviewSession session, Map<String, String> directionNameMap) {
        if (session == null) {
            return;
        }
        session.setDirectionName(directionNameMap.getOrDefault(session.getDirection(), session.getDirection()));

        if (session.getLevel() != null) {
            InterviewLevelEnum levelEnum = InterviewLevelEnum.getByCode(session.getLevel());
            session.setLevelName(levelEnum != null ? levelEnum.getName() : "未知");
        }

        if (session.getStyle() != null) {
            session.setStyleName(InterviewStyleEnum.getByCode(session.getStyle()).getName());
        }
    }

    private Map<String, String> buildDirectionNameMap() {
        Map<String, String> map = new HashMap<>();
        List<MockInterviewDirection> directions = directionMapper.selectAll();
        directions.forEach(direction -> map.put(direction.getDirectionCode(), direction.getDirectionName()));
        return map;
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignore) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    @Data
    public static class DirectionRequest {
        private String directionCode;
        private String directionName;
        private String icon;
        private String description;
        private String categoryIds;
        private Integer sortOrder;
    }
}
