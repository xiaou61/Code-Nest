package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bounded structured information supplied to a task waiting for planner input.
 */
@Data
@Schema(description = "管理员智能体任务补充输入")
public class AgentTaskInputRequest {

    @NotEmpty(message = "补充输入不能为空")
    @Size(max = 20, message = "补充输入最多包含20个字段")
    @Schema(description = "仅作为 planner 上下文的结构化补充信息")
    private Map<String, Object> input = new LinkedHashMap<>();
}
