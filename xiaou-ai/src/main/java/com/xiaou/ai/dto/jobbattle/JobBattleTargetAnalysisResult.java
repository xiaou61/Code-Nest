package com.xiaou.ai.dto.jobbattle;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 求职作战台-单岗位综合分析结果。
 *
 * @author xiaou
 */
@Data
@Accessors(chain = true)
public class JobBattleTargetAnalysisResult {

    /**
     * JD 解析结果
     */
    private JobBattleJdParseResult jdParse;

    /**
     * 简历匹配结果
     */
    private JobBattleResumeMatchResult resumeMatch;

    /**
     * 是否包含降级结果
     */
    private boolean fallback;
}
