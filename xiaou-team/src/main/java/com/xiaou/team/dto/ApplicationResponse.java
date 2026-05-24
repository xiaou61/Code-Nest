package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请响应DTO
 * 
 * @author xiaou
 */
@Data
public class ApplicationResponse {
    
    /**
     * 申请ID
     */
    private Long id;
    
    /**
     * 小组ID
     */
    private Long teamId;
    
    /**
     * 小组名称
     */
    private String teamName;

    /**
     * 小组头像
     */
    private String teamAvatar;
    
    /**
     * 申请人ID
     */
    private Long userId;
    
    /**
     * 申请人用户名
     */
    private String userName;
    
    /**
     * 申请人头像
     */
    private String userAvatar;
    
    /**
     * 申请理由
     */
    private String applyReason;
    
    /**
     * 状态：0待审核 1已通过 2已拒绝 3已取消
     */
    private Integer status;
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 审核人ID
     */
    private Long reviewerId;
    
    /**
     * 审核人用户名
     */
    private String reviewerName;
    
    /**
     * 审核时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reviewTime;
    
    /**
     * 拒绝原因
     */
    private String rejectReason;
    
    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
