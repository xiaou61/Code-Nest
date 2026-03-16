package com.xiaou.techbriefing.service;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.techbriefing.domain.TechBriefingFetchLog;
import com.xiaou.techbriefing.domain.TechBriefingSource;
import com.xiaou.techbriefing.domain.TechBriefingArticle;
import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import com.xiaou.techbriefing.dto.request.TechBriefingArticleQueryRequest;
import com.xiaou.techbriefing.dto.request.TechBriefingSubscriptionCreateRequest;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleCardResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleDetailResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingSubscriptionResponse;

import java.util.List;
import java.util.Map;

/**
 * 科技热点服务
 */
public interface TechBriefingService {

    PageResult<TechBriefingArticleCardResponse> listArticles(TechBriefingArticleQueryRequest request);

    TechBriefingArticleDetailResponse getArticleDetail(Long articleId);

    List<TechBriefingArticleCardResponse> getHighlights();

    List<String> getCategories();

    TechBriefingSubscriptionResponse createSubscription(Long userId, TechBriefingSubscriptionCreateRequest request);

    void testSubscription(Long userId, Long subscriptionId);

    void cancelSubscription(Long userId, Long subscriptionId);

    List<TechBriefingSource> listSources();

    List<TechBriefingSubscription> listSubscriptions();

    Map<String, Object> getSubscriptionStatistics();

    List<TechBriefingFetchLog> listFetchLogs(int limit);

    List<TechBriefingArticle> listAdminArticles();

    void saveSource(TechBriefingSource source);

    void deleteSource(Long sourceId);

    void updateArticlePin(Long articleId, Boolean isPinned);

    void offlineArticle(Long articleId);

    void retryTranslate(Long articleId);

    void retrySummary(Long articleId);

    void refreshAllSources();
}
