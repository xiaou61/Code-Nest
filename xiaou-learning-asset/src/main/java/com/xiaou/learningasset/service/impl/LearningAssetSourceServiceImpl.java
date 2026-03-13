package com.xiaou.learningasset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.xiaou.blog.dto.ArticleDetailResponse;
import com.xiaou.blog.service.BlogArticleService;
import com.xiaou.codepen.dto.CodePenDetailResponse;
import com.xiaou.codepen.service.CodePenService;
import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.dto.CommunityPostResponse;
import com.xiaou.community.service.CommunityAiSummaryService;
import com.xiaou.community.service.CommunityPostService;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.learningasset.dto.request.LearningAssetConvertRequest;
import com.xiaou.learningasset.dto.source.LearningAssetSourceSnapshot;
import com.xiaou.learningasset.service.LearningAssetSourceService;
import com.xiaou.mockinterview.dto.response.InterviewReportResponse;
import com.xiaou.mockinterview.service.MockInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 来源快照服务实现
 */
@Service
@RequiredArgsConstructor
public class LearningAssetSourceServiceImpl implements LearningAssetSourceService {

    private final BlogArticleService blogArticleService;
    private final CommunityPostService communityPostService;
    private final CommunityAiSummaryService communityAiSummaryService;
    private final CodePenService codePenService;
    private final MockInterviewService mockInterviewService;

    @Override
    public LearningAssetSourceSnapshot loadSnapshot(Long userId, LearningAssetConvertRequest request) {
        return switch (request.getSourceType()) {
            case "blog" -> loadBlogSnapshot(request);
            case "community" -> loadCommunitySnapshot(request);
            case "codepen" -> loadCodePenSnapshot(userId, request);
            case "mock_interview" -> loadMockInterviewSnapshot(userId, request);
            default -> throw new BusinessException("暂不支持的来源类型");
        };
    }

    private LearningAssetSourceSnapshot loadBlogSnapshot(LearningAssetConvertRequest request) {
        ArticleDetailResponse detail = blogArticleService.getArticleDetail(request.getSourceId());
        return LearningAssetSourceSnapshot.builder()
                .sourceType("blog")
                .sourceId(detail.getId())
                .title(detail.getTitle())
                .summary(detail.getSummary())
                .content(detail.getContent())
                .tags(mergeTags(detail.getTags(), request.getExtraTags()))
                .build();
    }

    private LearningAssetSourceSnapshot loadCommunitySnapshot(LearningAssetConvertRequest request) {
        CommunityPostResponse detail = communityPostService.getPostDetail(request.getSourceId());
        String summary = detail.getAiSummary();
        if (Boolean.TRUE.equals(request.getUseExistingSummary()) && StrUtil.isBlank(summary)) {
            Map<String, Object> summaryResult = communityAiSummaryService.getSummary(request.getSourceId());
            if (summaryResult != null && summaryResult.get("summary") != null) {
                summary = String.valueOf(summaryResult.get("summary"));
            }
        }
        List<String> tags = detail.getTags() == null ? List.of() :
                detail.getTags().stream().map(CommunityTag::getName).toList();
        return LearningAssetSourceSnapshot.builder()
                .sourceType("community")
                .sourceId(detail.getId())
                .title(detail.getTitle())
                .summary(summary)
                .content(detail.getContent())
                .tags(mergeTags(tags, request.getExtraTags()))
                .build();
    }

    private LearningAssetSourceSnapshot loadCodePenSnapshot(Long userId, LearningAssetConvertRequest request) {
        CodePenDetailResponse detail = codePenService.getDetail(request.getSourceId(), userId);
        String content = StrUtil.join("\n", List.of(
                StrUtil.blankToDefault(detail.getDescription(), ""),
                StrUtil.maxLength(StrUtil.blankToDefault(detail.getHtmlCode(), ""), 400),
                StrUtil.maxLength(StrUtil.blankToDefault(detail.getCssCode(), ""), 400),
                StrUtil.maxLength(StrUtil.blankToDefault(detail.getJsCode(), ""), 400)
        ));
        return LearningAssetSourceSnapshot.builder()
                .sourceType("codepen")
                .sourceId(detail.getId())
                .title(detail.getTitle())
                .summary(detail.getDescription())
                .content(content)
                .tags(mergeTags(detail.getTags(), request.getExtraTags()))
                .build();
    }

    private LearningAssetSourceSnapshot loadMockInterviewSnapshot(Long userId, LearningAssetConvertRequest request) {
        InterviewReportResponse report = mockInterviewService.getReport(userId, request.getSourceId());
        List<String> segments = new ArrayList<>();
        segments.add(StrUtil.blankToDefault(report.getAiSummary(), ""));
        if (report.getAiSuggestion() != null) {
            segments.addAll(report.getAiSuggestion());
        }
        if (report.getWeakPoints() != null && !report.getWeakPoints().isEmpty()) {
            segments.add("薄弱点：" + String.join("、", report.getWeakPoints()));
        }
        return LearningAssetSourceSnapshot.builder()
                .sourceType("mock_interview")
                .sourceId(report.getSessionId())
                .title(StrUtil.blankToDefault(report.getDirectionName(), "模拟面试报告"))
                .summary(report.getAiSummary())
                .content(String.join("\n", segments))
                .tags(mergeTags(report.getWeakPoints(), request.getExtraTags()))
                .build();
    }

    private List<String> mergeTags(List<String> baseTags, List<String> extraTags) {
        Set<String> merged = new LinkedHashSet<>();
        if (baseTags != null) {
            merged.addAll(baseTags);
        }
        if (extraTags != null) {
            merged.addAll(extraTags);
        }
        return new ArrayList<>(merged);
    }
}
