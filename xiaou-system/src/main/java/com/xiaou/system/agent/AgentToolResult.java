package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatDiffItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行结果。
 *
 * @author xiaou
 */
@Data
public class AgentToolResult {

    private boolean success;

    private String summary;

    private String errorMessage;

    private List<AgentChatDiffItem> diff = new ArrayList<>();

    private List<AgentChatArtifact> artifacts = new ArrayList<>();

    private List<String> nextActions = new ArrayList<>();
}
