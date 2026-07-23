package com.xiaou.sre.service;

import com.xiaou.sre.dto.response.SreInvestigationContext;

import java.util.Optional;

/**
 * SRE 事故只读调查入口。
 *
 * <p>系统 Agent 或 AI 只能通过该边界读取已裁剪的调查数据，不能直接查询事故表、
 * 原始告警载荷或外部可观测性系统。</p>
 *
 * @author xiaou
 */
public interface SreInvestigationFacade {

    Optional<SreInvestigationContext> findByIncidentId(Long incidentId);
}
