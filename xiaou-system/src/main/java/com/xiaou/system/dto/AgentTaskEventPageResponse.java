package com.xiaou.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Cursor page for a durable task event feed.
 */
@Data
@Schema(description = "管理员智能体任务事件分页")
public class AgentTaskEventPageResponse {

    private List<AgentTaskEventResponse> events = new ArrayList<>();
    private Long nextCursor;
    private boolean hasMore;
}
