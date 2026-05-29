package com.xiaou.points.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 积分明细查询请求DTO
 * 
 * @author xiaou
 */
@Data
public class PointsDetailQueryRequest {
    
    /**
     * 用户ID（用户端查询时必填）
     */
    @Positive(message = "用户ID必须为正数")
    private Long userId;
    
    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页数量必须大于等于1")
    private Integer pageSize = 20;
    
    /**
     * 积分类型筛选：1-后台发放 2-打卡积分
     */
    @Min(value = 1, message = "积分类型不合法")
    @Max(value = 4, message = "积分类型不合法")
    private Integer pointsType;
    
    /**
     * 开始时间
     */
    private String startTime;
    
    /**
     * 结束时间
     */
    private String endTime;
    
    /**
     * 管理员ID（管理端查询时使用）
     */
    @Positive(message = "管理员ID必须为正数")
    private Long adminId;
    
    /**
     * 用户名模糊搜索（管理端使用）
     */
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String userName;
}
