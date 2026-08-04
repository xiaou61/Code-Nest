package com.xiaou.ai.service;

import com.xiaou.ai.dto.codereview.CodePenReviewResult;

/**
 * 用户自有 CodePen 的 AI 审查能力。
 */
public interface AiCodeReviewService {

    CodePenReviewResult reviewCodePen(String title, String htmlCode, String cssCode, String javascriptCode);
}
