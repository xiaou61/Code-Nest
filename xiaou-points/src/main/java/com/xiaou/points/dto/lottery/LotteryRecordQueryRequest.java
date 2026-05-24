package com.xiaou.points.dto.lottery;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * 抽奖记录查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class LotteryRecordQueryRequest {
    
    /**
     * 页码
     */
    @JsonAlias("page")
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;
    
    /**
     * 每页数量
     */
    @JsonAlias("size")
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer pageSize = 20;
    
    /**
     * 用户ID（内部使用）
     */
    @Positive(message = "用户ID必须大于0")
    private Long userId;

    /**
     * 奖品ID
     */
    @Positive(message = "奖品ID必须大于0")
    private Long prizeId;
    
    public Integer getPage() {
        return pageNum;
    }
    
    public Integer getSize() {
        return pageSize;
    }
    
    /**
     * 奖品等级筛选
     */
    private Integer prizeLevel;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 管理端日期筛选开始日期
     */
    private String startDate;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 管理端日期筛选结束日期
     */
    private String endDate;

    public LocalDateTime getStartTime() {
        if (startTime != null) {
            return startTime;
        }
        return parseDateBoundary(startDate, true);
    }

    public LocalDateTime getEndTime() {
        if (endTime != null) {
            return endTime;
        }
        return parseDateBoundary(endDate, false);
    }

    private LocalDateTime parseDateBoundary(String date, boolean startOfDay) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            return startOfDay ? localDate.atStartOfDay() : localDate.atTime(LocalTime.MAX);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}

