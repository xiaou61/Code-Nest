package com.xiaou.points.dto.lottery.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 统计查询请求
 * 
 * @author xiaou
 */
@Data
public class StatisticsQueryRequest {
    
    /**
     * 开始日期 (格式: yyyy-MM-dd)
     */
    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "开始日期格式必须为yyyy-MM-dd")
    private String startDate;
    
    /**
     * 结束日期 (格式: yyyy-MM-dd)
     */
    @NotBlank(message = "结束日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "结束日期格式必须为yyyy-MM-dd")
    private String endDate;
}

