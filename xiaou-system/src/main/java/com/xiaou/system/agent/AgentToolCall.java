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
public class AgentToolCall implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String toolName;

    private String summary;

    private Map<String, Object> input = new LinkedHashMap<>();
}
