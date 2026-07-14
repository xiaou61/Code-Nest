package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体预览差异项。
 *
 * @author xiaou
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体预览差异项")
public class AgentChatDiffItem {

    private String field;

    private Object beforeValue;

    private Object afterValue;

    private String description;
}
