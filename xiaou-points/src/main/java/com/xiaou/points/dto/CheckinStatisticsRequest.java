package com.xiaou.points.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打卡统计查询请求DTO
 * 
 * @author xiaou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinStatisticsRequest {
    
    /**
     * 查询最近几个月的数据，默认为3个月
     */
    @Builder.Default
    private Integer months = 3;
}
