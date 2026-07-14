package com.xiaou.system.agent;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 已解析出的工具调用。
 *
 * @author xiaou
 */
@Data
public class AgentToolCall {

    private String toolName;

    private String summary;

    private Map<String, Object> input = new LinkedHashMap<>();
}
