package com.xiaou.system.agent;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可被智能体调用的后端工具定义。
 *
 * @author xiaou
 */
@Data
public class AgentToolDefinition {

    private String name;

    private String title;

    private String description;

    private String intent;

    private String route;

    private String riskLevel = "readonly";

    private String riskCategory = "READONLY";

    private boolean destructive;

    private boolean confirmationRequired;

    private String confirmationText;

    private Map<String, Object> inputSchema = new LinkedHashMap<>();

    private List<String> requiredInputKeys = new ArrayList<>();

    private List<String> requiredPermissions = new ArrayList<>();

    private List<String> requiredRoles = new ArrayList<>();

    private String tenantScope = "ANY";
}
