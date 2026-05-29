package com.xiaou.points.dto.lottery.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 概率调整历史响应
 * 
 * @author xiaou
 */
@Data
public class AdjustHistoryResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * ID
     */
    private Long id;
    
    /**
     * 奖品ID
     */
    private Long prizeId;
    
    /**
     * 奖品名称
     */
    private String prizeName;
    
    /**
     * 调整前概率
     */
    private BigDecimal oldProbability;
    
    /**
     * 调整后概率
     */
    private BigDecimal newProbability;
    
    /**
     * 调整原因
     */
    private String reason;
    
    /**
     * 调整时实际回报率
     */
    private BigDecimal actualReturnRate;
    
    /**
     * 操作人ID（0表示系统自动）
     */
    private Long operatorId;
    
    /**
     * 操作人姓名
     */
    private String operatorName;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    public String getAdjustReason() {
        return reason;
    }
}

