package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体计划步骤。
 *
 * @author xiaou
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体计划步骤")
public class AgentChatPlanStep {

    private String title;

    private String status;

    private String detail;
}
