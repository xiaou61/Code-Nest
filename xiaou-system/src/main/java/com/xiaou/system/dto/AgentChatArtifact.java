package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 智能体结构化产物。
 *
 * @author xiaou
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能体结构化产物")
public class AgentChatArtifact {

    private String type;

    private String title;

    private Map<String, Object> data;
}
