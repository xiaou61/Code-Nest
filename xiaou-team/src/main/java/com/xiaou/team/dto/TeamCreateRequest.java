package com.xiaou.team.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建小组请求DTO
 * 
 * @author xiaou
 */
@Data
public class TeamCreateRequest {
    
    /**
     * 小组名称（2-50字符）
     */
    @NotBlank(message = "小组名称不能为空")
    @Size(min = 2, max = 50, message = "小组名称长度需要在2-50个字符之间")
    private String teamName;
    
    /**
     * 小组简介（最多500字）
     */
    @Size(max = 500, message = "小组简介不能超过500个字符")
    private String teamDesc;
    
    /**
     * 小组头像
     */
    @Size(max = 255, message = "小组头像地址不能超过255个字符")
    private String teamAvatar;
    
    /**
     * 类型：1目标型 2学习型 3打卡型
     */
    @NotNull(message = "小组类型不能为空")
    @Min(value = 1, message = "小组类型不合法")
    @Max(value = 3, message = "小组类型不合法")
    private Integer teamType;
    
    /**
     * 标签，逗号分隔（最多5个）
     */
    @Size(max = 100, message = "标签长度不能超过100个字符")
    private String tags;
    
    /**
     * 最大成员数（2-50）
     */
    @Min(value = 2, message = "最大成员数最小为2")
    @Max(value = 50, message = "最大成员数最大为50")
    private Integer maxMembers;
    
    /**
     * 加入方式：1公开 2申请 3邀请
     */
    @Min(value = 1, message = "加入方式不合法")
    @Max(value = 3, message = "加入方式不合法")
    private Integer joinType;
    
    /**
     * 目标标题
     */
    @Size(max = 100, message = "目标标题不能超过100个字符")
    private String goalTitle;
    
    /**
     * 目标描述
     */
    @Size(max = 500, message = "目标描述不能超过500个字符")
    private String goalDesc;
    
    /**
     * 目标开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate goalStartDate;
    
    /**
     * 目标结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate goalEndDate;
    
    /**
     * 每日目标量
     */
    @Min(value = 1, message = "每日目标量必须大于0")
    private Integer dailyTarget;
}
