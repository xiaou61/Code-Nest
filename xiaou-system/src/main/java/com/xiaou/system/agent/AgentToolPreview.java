package com.xiaou.system.agent;

import com.xiaou.system.dto.AgentChatArtifact;
import com.xiaou.system.dto.AgentChatDiffItem;
import com.xiaou.system.dto.AgentChatPlanStep;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 写入工具执行前预览。
 *
 * @author xiaou
 */
@Data
public class AgentToolPreview {

    private boolean executable = true;

    private String blockedReason;

    private String summary;

    private List<AgentChatPlanStep> plan = new ArrayList<>();

    private List<AgentChatDiffItem> diff = new ArrayList<>();

    private List<AgentChatArtifact> artifacts = new ArrayList<>();
}
