package com.xiaou.points.dto.lottery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 风险用户查询请求
 * 
 * @author xiaou
 */
@Data
public class RiskUserQueryRequest {
    
    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;
    
    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量必须大于0")
    private Integer size = 20;
    
    /**
     * 风险等级：0-正常 1-低风险 2-中风险 3-高风险
     */
    @Min(value = 0, message = "风险等级不能小于0")
    @Max(value = 3, message = "风险等级不能大于3")
    private Integer riskLevel;
    
    /**
     * 是否黑名单：0-否 1-是
     */
    @Min(value = 0, message = "黑名单状态不能小于0")
    @Max(value = 1, message = "黑名单状态不能大于1")
    private Integer isBlacklist;
    
    /**
     * 最小连续未中奖次数
     */
    @Min(value = 0, message = "连续未中奖次数不能小于0")
    private Integer minContinuousNoWin;
}

