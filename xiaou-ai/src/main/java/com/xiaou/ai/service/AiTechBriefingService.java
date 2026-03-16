package com.xiaou.ai.service;

import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryResult;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateResult;

/**
 * 科技热点 AI 服务
 */
public interface AiTechBriefingService {

    TechBriefingTranslateResult translate(TechBriefingTranslateRequest request);

    TechBriefingSummaryResult summarize(TechBriefingSummaryRequest request);
}
