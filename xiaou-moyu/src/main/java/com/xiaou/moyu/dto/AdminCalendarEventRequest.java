package com.xiaou.moyu.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * 管理后台日历事件请求对象
 *
 * @author xiaou
 */
@Data
public class AdminCalendarEventRequest {
    
    /**
     * 事件ID（更新时使用）
     */
    private Long id;
    
    /**
     * 事件日期
     */
    @NotBlank(message = "事件日期不能为空")
    @Pattern(regexp = "^(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$", message = "事件日期格式应为MM-dd")
    private String eventDate;
    
    /**
     * 事件名称
     */
    @NotBlank(message = "事件名称不能为空")
    private String eventName;
    
    /**
     * 事件类型：1-程序员节日，2-技术纪念日，3-开源节日
     */
    @NotNull(message = "事件类型不能为空")
    @Min(value = 1, message = "事件类型值无效")
    @Max(value = 3, message = "事件类型值无效")
    private Integer eventType;
    
    /**
     * 事件描述
     */
    private String description;
    
    /**
     * 节日祝福语
     */
    private String blessingText;
    
    /**
     * 相关链接列表
     */
    private List<String> relatedLinks;
    
    /**
     * 事件图标
     */
    private String icon;
    
    /**
     * 事件颜色
     */
    private String color;
    
    /**
     * 是否重要事件：0-否，1-是
     */
    @Min(value = 0, message = "重要事件标志值无效")
    @Max(value = 1, message = "重要事件标志值无效")
    private Integer isMajor;
    
    /**
     * 排序值
     */
    private Integer sortOrder;
    
    /**
     * 状态：0-禁用，1-启用
     */
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;
}
