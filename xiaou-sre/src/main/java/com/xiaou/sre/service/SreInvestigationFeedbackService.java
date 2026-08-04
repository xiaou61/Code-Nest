package com.xiaou.sre.service;

import com.xiaou.sre.domain.SreInvestigationFeedback;

import java.util.Optional;

/**
 * SRE 调查人工反馈的追加式审计边界。
 *
 * @author xiaou
 */
public interface SreInvestigationFeedbackService {

    Optional<SreInvestigationFeedback> save(Long incidentId,
                                            Long runId,
                                            String accuracy,
                                            String gapType,
                                            String note,
                                            String expectedConclusion,
                                            Long reviewedBy);

    Optional<SreInvestigationFeedback> findLatest(Long incidentId, Long runId);
}
