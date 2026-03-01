package com.xiaou.mockinterview.dto.response;

import com.xiaou.mockinterview.domain.CareerLoopAction;
import com.xiaou.mockinterview.domain.CareerLoopSession;
import com.xiaou.mockinterview.domain.CareerLoopSnapshot;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 求职闭环当前状态响应
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class CareerLoopCurrentResponse {

    private CareerLoopSession session;

    private CareerLoopSnapshot snapshot;

    private List<CareerLoopAction> actions;

    private List<String> riskFlags;

    private List<String> nextSuggestions;
}

