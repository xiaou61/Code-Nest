package com.xiaou.techbriefing.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingSummaryResult;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateRequest;
import com.xiaou.ai.dto.techbriefing.TechBriefingTranslateResult;
import com.xiaou.ai.service.AiTechBriefingService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.exception.BusinessException;
import com.xiaou.techbriefing.domain.TechBriefingArticle;
import com.xiaou.techbriefing.domain.TechBriefingArticleAi;
import com.xiaou.techbriefing.domain.TechBriefingArticleContent;
import com.xiaou.techbriefing.domain.TechBriefingFetchLog;
import com.xiaou.techbriefing.domain.TechBriefingSource;
import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import com.xiaou.techbriefing.dto.model.TechBriefingFeedItem;
import com.xiaou.techbriefing.dto.request.TechBriefingArticleQueryRequest;
import com.xiaou.techbriefing.dto.request.TechBriefingSubscriptionCreateRequest;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleCardResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleDetailResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingSubscriptionResponse;
import com.xiaou.techbriefing.mapper.TechBriefingArticleAiMapper;
import com.xiaou.techbriefing.mapper.TechBriefingArticleContentMapper;
import com.xiaou.techbriefing.mapper.TechBriefingArticleMapper;
import com.xiaou.techbriefing.mapper.TechBriefingFetchLogMapper;
import com.xiaou.techbriefing.mapper.TechBriefingSourceMapper;
import com.xiaou.techbriefing.mapper.TechBriefingSubscriptionMapper;
import com.xiaou.techbriefing.service.TechBriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 科技热点服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechBriefingServiceImpl implements TechBriefingService {

    private final TechBriefingSourceMapper sourceMapper;
    private final TechBriefingArticleMapper articleMapper;
    private final TechBriefingArticleContentMapper articleContentMapper;
    private final TechBriefingArticleAiMapper articleAiMapper;
    private final TechBriefingSubscriptionMapper subscriptionMapper;
    private final TechBriefingFetchLogMapper fetchLogMapper;
    private final TechBriefingRssClient rssClient;
    private final TechBriefingWebhookClient webhookClient;
    private final AiTechBriefingService aiTechBriefingService;

    @Override
    public PageResult<TechBriefingArticleCardResponse> listArticles(TechBriefingArticleQueryRequest request) {
        TechBriefingArticleQueryRequest query = normalizeQuery(request);
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        long total = articleMapper.countPublic(query);
        if (total <= 0) {
            return PageResult.of(query.getPageNum(), query.getPageSize(), 0L, List.of());
        }
        List<TechBriefingArticle> articles = articleMapper.selectPublicList(query, offset, query.getPageSize());
        if (articles == null || articles.isEmpty()) {
            return PageResult.of(query.getPageNum(), query.getPageSize(), total, List.of());
        }
        List<Long> articleIds = articles.stream().map(TechBriefingArticle::getId).filter(Objects::nonNull).toList();
        Map<Long, TechBriefingArticleAi> aiMap = articleAiMapper.selectByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(TechBriefingArticleAi::getArticleId, item -> item, (left, right) -> left));
        List<TechBriefingArticleCardResponse> records = articles.stream()
                .map(article -> toCardResponse(article, aiMap.get(article.getId())))
                .toList();
        return PageResult.of(query.getPageNum(), query.getPageSize(), total, records);
    }

    @Override
    public TechBriefingArticleDetailResponse getArticleDetail(Long articleId) {
        TechBriefingArticle article = articleMapper.selectById(articleId);
        if (article == null || Boolean.TRUE.equals(article.getDeleted())) {
            throw new BusinessException("文章不存在");
        }
        TechBriefingArticleContent content = articleContentMapper.selectByArticleId(articleId);
        TechBriefingArticleAi ai = articleAiMapper.selectByArticleId(articleId);
        TechBriefingArticleDetailResponse response = new TechBriefingArticleDetailResponse();
        response.setId(article.getId());
        response.setTitle(StrUtil.blankToDefault(article.getTitleZh(), article.getTitle()));
        response.setOriginalTitle(article.getTitle());
        response.setSummary(StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary()));
        response.setAiSummary(extractAiSummary(ai, article));
        response.setWhyImportant(ai == null ? null : ai.getWhyImportant());
        response.setImpactScope(ai == null ? null : ai.getImpactScope());
        response.setTranslatedContentZh(content == null ? null : StrUtil.blankToDefault(content.getTranslatedContentZh(), content.getRawContent()));
        response.setRawContent(content == null ? null : content.getRawContent());
        response.setSourceUrl(article.getSourceUrl());
        response.setSourceName(article.getSourceName());
        response.setTranslationStatus(article.getTranslationStatus());
        response.setTags(parseTags(article.getTagsJson()));
        response.setPublishTime(article.getPublishTime());
        return response;
    }

    @Override
    public List<TechBriefingArticleCardResponse> getHighlights() {
        List<TechBriefingArticle> articles = articleMapper.selectHighlights(3);
        if (articles == null || articles.isEmpty()) {
            return List.of();
        }
        List<Long> articleIds = articles.stream().map(TechBriefingArticle::getId).toList();
        Map<Long, TechBriefingArticleAi> aiMap = articleAiMapper.selectByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(TechBriefingArticleAi::getArticleId, item -> item, (left, right) -> left));
        return articles.stream().map(article -> toCardResponse(article, aiMap.get(article.getId()))).toList();
    }

    @Override
    public List<String> getCategories() {
        List<String> categories = articleMapper.selectDistinctCategories();
        return categories == null ? List.of() : categories;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TechBriefingSubscriptionResponse createSubscription(Long userId, TechBriefingSubscriptionCreateRequest request) {
        validateWebhookRequest(request);
        TechBriefingSubscription existed = subscriptionMapper.selectActiveByChannelAndWebhook(request.getChannelType(), request.getWebhookUrl());
        if (existed != null) {
            throw new BusinessException("该渠道已经订阅过科技热点速览");
        }
        TechBriefingSubscription subscription = new TechBriefingSubscription()
                .setUserId(userId)
                .setChannelType(request.getChannelType())
                .setWebhookUrl(request.getWebhookUrl())
                .setWebhookSecret(request.getWebhookSecret())
                .setTargetName(StrUtil.blankToDefault(request.getTargetName(), request.getChannelType() + " 订阅"))
                .setConfigHash(buildConfigHash(request))
                .setTopicPreferences(JSONUtil.toJsonStr(request.getTopicPreferences() == null ? List.of() : request.getTopicPreferences()))
                .setFrequency(StrUtil.blankToDefault(request.getFrequency(), "DAILY"))
                .setStatus("ENABLED")
                .setSource("PAGE");
        subscriptionMapper.insert(subscription);
        return toSubscriptionResponse(subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void testSubscription(Long userId, Long subscriptionId) {
        TechBriefingSubscription subscription = subscriptionMapper.selectById(subscriptionId);
        if (subscription == null) {
            throw new BusinessException("订阅配置不存在");
        }
        if (userId != null && subscription.getUserId() != null && !Objects.equals(subscription.getUserId(), userId)) {
            throw new BusinessException("无权操作该订阅配置");
        }
        webhookClient.sendTestMessage(subscription, "科技热点速览订阅测试成功，你将收到后续的每日科技简报。");
        subscriptionMapper.updateTestResult(subscriptionId, LocalDateTime.now(), "SUCCESS");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSubscription(Long userId, Long subscriptionId) {
        TechBriefingSubscription subscription = subscriptionMapper.selectById(subscriptionId);
        if (subscription == null) {
            throw new BusinessException("订阅配置不存在");
        }
        if (userId != null && subscription.getUserId() != null && !Objects.equals(subscription.getUserId(), userId)) {
            throw new BusinessException("无权取消该订阅");
        }
        subscriptionMapper.updateStatusById(subscriptionId, "DISABLED");
    }

    @Override
    public List<TechBriefingSource> listSources() {
        List<TechBriefingSource> sources = sourceMapper.selectAll();
        return sources == null ? List.of() : sources;
    }

    @Override
    public List<TechBriefingSubscription> listSubscriptions() {
        List<TechBriefingSubscription> subscriptions = subscriptionMapper.selectAll();
        return subscriptions == null ? List.of() : subscriptions;
    }

    @Override
    public Map<String, Object> getSubscriptionStatistics() {
        List<TechBriefingSubscription> subscriptions = listSubscriptions();
        long total = subscriptions.size();
        long enabled = subscriptions.stream().filter(item -> "ENABLED".equalsIgnoreCase(item.getStatus())).count();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("enabled", enabled);
        result.put("disabled", Math.max(0, total - enabled));
        return result;
    }

    @Override
    public List<TechBriefingFetchLog> listFetchLogs(int limit) {
        return fetchLogMapper.selectLatest(limit <= 0 ? 20 : limit);
    }

    @Override
    public List<TechBriefingArticle> listAdminArticles() {
        List<TechBriefingArticle> articles = articleMapper.selectAdminList();
        return articles == null ? List.of() : articles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSource(TechBriefingSource source) {
        if (source == null || StrUtil.isBlank(source.getSourceName())) {
            throw new BusinessException("来源名称不能为空");
        }
        source.setStatus(StrUtil.blankToDefault(source.getStatus(), "ENABLED"));
        source.setFetchType(StrUtil.blankToDefault(source.getFetchType(), "RSS"));
        source.setSourceType(StrUtil.blankToDefault(source.getSourceType(), "GLOBAL"));
        source.setWeight(source.getWeight() == null ? 50 : source.getWeight());
        if (source.getId() == null) {
            sourceMapper.insert(source);
        } else {
            sourceMapper.update(source);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(Long sourceId) {
        sourceMapper.deleteById(sourceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticlePin(Long articleId, Boolean isPinned) {
        articleMapper.updatePinStatus(articleId, Boolean.TRUE.equals(isPinned));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineArticle(Long articleId) {
        articleMapper.updateStatus(articleId, "OFFLINE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryTranslate(Long articleId) {
        TechBriefingArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        TechBriefingArticleContent content = articleContentMapper.selectByArticleId(articleId);
        TechBriefingTranslateRequest request = new TechBriefingTranslateRequest();
        request.setTitle(article.getTitle());
        request.setSummary(article.getSummary());
        request.setContent(content == null ? null : content.getRawContent());
        request.setSourceName(article.getSourceName());
        TechBriefingTranslateResult result = aiTechBriefingService.translate(request);
        if (result == null) {
            throw new BusinessException("AI 翻译暂不可用");
        }
        article.setTitleZh(StrUtil.blankToDefault(result.getTitleZh(), article.getTitleZh()));
        article.setSummaryZh(StrUtil.blankToDefault(result.getSummaryZh(), article.getSummaryZh()));
        article.setTranslationStatus("READY");
        articleMapper.updateById(article);
        if (content != null) {
            content.setTranslatedContentZh(StrUtil.blankToDefault(result.getContentZh(), content.getTranslatedContentZh()));
            content.setTranslationStatus("READY");
            articleContentMapper.updateByArticleId(content);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retrySummary(Long articleId) {
        TechBriefingArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        TechBriefingArticleContent content = articleContentMapper.selectByArticleId(articleId);
        TechBriefingSummaryRequest request = new TechBriefingSummaryRequest();
        request.setTitle(article.getTitle());
        request.setTitleZh(StrUtil.blankToDefault(article.getTitleZh(), article.getTitle()));
        request.setSummary(article.getSummary());
        request.setSummaryZh(StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary()));
        request.setContentZh(content == null ? null : StrUtil.blankToDefault(content.getTranslatedContentZh(), content.getRawContent()));
        request.setSourceName(article.getSourceName());
        TechBriefingSummaryResult result = aiTechBriefingService.summarize(request);
        if (result == null) {
            throw new BusinessException("AI 摘要暂不可用");
        }
        TechBriefingArticleAi ai = new TechBriefingArticleAi()
                .setArticleId(articleId)
                .setSummaryJson(JSONUtil.toJsonStr(Map.of("summary", StrUtil.blankToDefault(result.getSummary(), article.getSummaryZh()))))
                .setKeywordsJson(JSONUtil.toJsonStr(result.getKeywords()))
                .setWhyImportant(result.getWhyImportant())
                .setImpactScope(result.getImpactScope())
                .setModelName(result.getModelName())
                .setStatus("READY")
                .setFailReason(null);
        TechBriefingArticleAi existedAi = articleAiMapper.selectByArticleId(articleId);
        if (existedAi == null) {
            articleAiMapper.insert(ai);
        } else {
            articleAiMapper.updateByArticleId(ai);
        }
        article.setAiSummaryStatus("READY");
        articleMapper.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshAllSources() {
        List<TechBriefingSource> sources = sourceMapper.selectEnabledSources();
        if (sources == null || sources.isEmpty()) {
            return;
        }
        for (TechBriefingSource source : sources) {
            refreshSource(source);
        }
    }

    private void refreshSource(TechBriefingSource source) {
        LocalDateTime start = LocalDateTime.now();
        int successCount = 0;
        int failCount = 0;
        String status = "SUCCESS";
        String message = "ok";
        try {
            List<TechBriefingFeedItem> items = rssClient.fetch(source);
            for (TechBriefingFeedItem item : items) {
                if (StrUtil.isBlank(item.getLink()) || StrUtil.isBlank(item.getTitle())) {
                    continue;
                }
                persistFeedItem(source, item);
                successCount++;
            }
        } catch (Exception ex) {
            log.error("刷新来源失败: source={}", source.getSourceName(), ex);
            failCount++;
            status = "FAILED";
            message = StrUtil.blankToDefault(ex.getMessage(), "refresh failed");
        } finally {
            fetchLogMapper.insert(new TechBriefingFetchLog()
                    .setSourceId(source.getId())
                    .setTaskType("REFRESH")
                    .setSuccessCount(successCount)
                    .setFailCount(failCount)
                    .setStatus(status)
                    .setMessage(message)
                    .setStartTime(start)
                    .setEndTime(LocalDateTime.now()));
        }
    }

    private void persistFeedItem(TechBriefingSource source, TechBriefingFeedItem item) {
        TechBriefingArticle existed = articleMapper.selectBySourceUrl(item.getLink());
        TechBriefingTranslateResult translateResult = buildTranslateResult(source, item);
        TechBriefingSummaryResult summaryResult = buildSummaryResult(source, item, translateResult);

        TechBriefingArticle article = (existed == null ? new TechBriefingArticle() : existed)
                .setSourceId(source.getId())
                .setSourceName(source.getSourceName())
                .setTitle(item.getTitle())
                .setTitleZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getTitleZh(), item.getTitle()))
                .setSummary(item.getSummary())
                .setSummaryZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getSummaryZh(), item.getSummary()))
                .setSourceUrl(item.getLink())
                .setCoverImage(item.getImageUrl())
                .setLanguage("GLOBAL".equalsIgnoreCase(source.getSourceType()) ? "en" : "zh")
                .setRegionType(source.getSourceType())
                .setCategory(firstCategory(item.getCategories()))
                .setTagsJson(JSONUtil.toJsonStr(item.getCategories() == null ? List.of() : item.getCategories()))
                .setHotScore(Math.max(1, source.getWeight() == null ? 1 : source.getWeight()))
                .setPublishTime(item.getPublishTime() == null ? LocalDateTime.now() : item.getPublishTime())
                .setStatus("READY")
                .setIsPinned(Boolean.FALSE)
                .setTranslationStatus("READY")
                .setAiSummaryStatus("READY")
                .setDeleted(Boolean.FALSE);
        if (existed == null) {
            articleMapper.insert(article);
        } else {
            articleMapper.updateById(article);
        }

        TechBriefingArticleContent content = new TechBriefingArticleContent()
                .setArticleId(article.getId())
                .setRawContent(item.getContent())
                .setTranslatedContentZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getContentZh(), item.getContent()))
                .setContentExtractStatus("READY")
                .setTranslationStatus("READY")
                .setTokenUsage(0)
                .setCopyrightMode("SUMMARY_ONLY");
        TechBriefingArticleContent existedContent = articleContentMapper.selectByArticleId(article.getId());
        if (existedContent == null) {
            articleContentMapper.insert(content);
        } else {
            articleContentMapper.updateByArticleId(content);
        }

        TechBriefingArticleAi ai = new TechBriefingArticleAi()
                .setArticleId(article.getId())
                .setSummaryJson(JSONUtil.toJsonStr(Map.of("summary", summaryText(summaryResult, article))))
                .setKeywordsJson(JSONUtil.toJsonStr(summaryResult == null || summaryResult.getKeywords() == null ? item.getCategories() : summaryResult.getKeywords()))
                .setWhyImportant(summaryWhy(summaryResult, article))
                .setImpactScope(summaryImpact(summaryResult))
                .setModelName(summaryResult == null ? "rule" : summaryResult.getModelName())
                .setStatus("READY")
                .setFailReason(null);
        TechBriefingArticleAi existedAi = articleAiMapper.selectByArticleId(article.getId());
        if (existedAi == null) {
            articleAiMapper.insert(ai);
        } else {
            articleAiMapper.updateByArticleId(ai);
        }
    }

    private TechBriefingTranslateResult buildTranslateResult(TechBriefingSource source, TechBriefingFeedItem item) {
        if (!"GLOBAL".equalsIgnoreCase(source.getSourceType())) {
            return new TechBriefingTranslateResult()
                    .setTitleZh(item.getTitle())
                    .setSummaryZh(item.getSummary())
                    .setContentZh(item.getContent())
                    .setModelName("rule");
        }
        TechBriefingTranslateRequest request = new TechBriefingTranslateRequest();
        request.setTitle(item.getTitle());
        request.setSummary(item.getSummary());
        request.setContent(item.getContent());
        request.setSourceName(source.getSourceName());
        return aiTechBriefingService.translate(request);
    }

    private TechBriefingSummaryResult buildSummaryResult(TechBriefingSource source,
                                                         TechBriefingFeedItem item,
                                                         TechBriefingTranslateResult translateResult) {
        TechBriefingSummaryRequest request = new TechBriefingSummaryRequest();
        request.setTitle(item.getTitle());
        request.setTitleZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getTitleZh(), item.getTitle()));
        request.setSummary(item.getSummary());
        request.setSummaryZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getSummaryZh(), item.getSummary()));
        request.setContentZh(StrUtil.blankToDefault(translateResult == null ? null : translateResult.getContentZh(), item.getContent()));
        request.setSourceName(source.getSourceName());
        TechBriefingSummaryResult result = aiTechBriefingService.summarize(request);
        if (result != null) {
            return result;
        }
        return new TechBriefingSummaryResult()
                .setSummary(buildFallbackSummary(item))
                .setWhyImportant(buildFallbackWhy(item, source))
                .setImpactScope("适合关注 " + StrUtil.blankToDefault(firstCategory(item.getCategories()), "科技趋势") + " 的用户快速浏览。")
                .setKeywords(item.getCategories())
                .setModelName("rule");
    }

    private TechBriefingArticleCardResponse toCardResponse(TechBriefingArticle article, TechBriefingArticleAi ai) {
        TechBriefingArticleCardResponse response = new TechBriefingArticleCardResponse();
        response.setId(article.getId());
        response.setTitle(StrUtil.blankToDefault(article.getTitleZh(), article.getTitle()));
        response.setSummary(StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary()));
        response.setAiSummary(extractAiSummary(ai, article));
        response.setSourceName(article.getSourceName());
        response.setRegionType(article.getRegionType());
        response.setCategory(article.getCategory());
        response.setSourceUrl(article.getSourceUrl());
        response.setCoverImage(article.getCoverImage());
        response.setTranslationStatus(article.getTranslationStatus());
        response.setPublishTime(article.getPublishTime());
        return response;
    }

    private String extractAiSummary(TechBriefingArticleAi ai, TechBriefingArticle article) {
        if (ai != null) {
            if (StrUtil.isNotBlank(ai.getWhyImportant())) {
                return ai.getWhyImportant();
            }
            if (StrUtil.isNotBlank(ai.getSummaryJson())) {
                cn.hutool.json.JSONObject json = JSONUtil.parseObj(ai.getSummaryJson());
                String summary = json.getStr("summary");
                if (StrUtil.isNotBlank(summary)) {
                    return summary;
                }
            }
        }
        return StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary());
    }

    private TechBriefingArticleQueryRequest normalizeQuery(TechBriefingArticleQueryRequest request) {
        TechBriefingArticleQueryRequest query = request == null ? new TechBriefingArticleQueryRequest() : request;
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
        query.setPageSize(Math.min(query.getPageSize(), 50));
        query.setScope(StrUtil.blankToDefault(query.getScope(), "all"));
        query.setSortBy(StrUtil.blankToDefault(query.getSortBy(), "latest"));
        return query;
    }

    private void validateWebhookRequest(TechBriefingSubscriptionCreateRequest request) {
        if (request == null) {
            throw new BusinessException("订阅配置不能为空");
        }
        if ("FEISHU".equalsIgnoreCase(request.getChannelType())
                && !StrUtil.containsIgnoreCase(request.getWebhookUrl(), "open.feishu.cn/open-apis/bot/v2/hook/")) {
            throw new BusinessException("飞书 Webhook 地址格式不正确");
        }
        if ("DINGTALK".equalsIgnoreCase(request.getChannelType())
                && !StrUtil.containsIgnoreCase(request.getWebhookUrl(), "dingtalk.com/robot/send")) {
            throw new BusinessException("钉钉 Webhook 地址格式不正确");
        }
    }

    private String buildConfigHash(TechBriefingSubscriptionCreateRequest request) {
        return DigestUtil.sha256Hex(StrUtil.blankToDefault(request.getChannelType(), "")
                + "|"
                + StrUtil.blankToDefault(request.getWebhookUrl(), "")
                + "|"
                + StrUtil.blankToDefault(request.getWebhookSecret(), ""));
    }

    private TechBriefingSubscriptionResponse toSubscriptionResponse(TechBriefingSubscription subscription) {
        TechBriefingSubscriptionResponse response = new TechBriefingSubscriptionResponse();
        response.setId(subscription.getId());
        response.setChannelType(subscription.getChannelType());
        response.setTargetName(subscription.getTargetName());
        response.setMaskedWebhookUrl(maskWebhookUrl(subscription.getWebhookUrl()));
        response.setFrequency(subscription.getFrequency());
        response.setStatus(subscription.getStatus());
        return response;
    }

    private String maskWebhookUrl(String webhookUrl) {
        if (StrUtil.isBlank(webhookUrl) || webhookUrl.length() < 12) {
            return "***";
        }
        return webhookUrl.substring(0, 8) + "***" + webhookUrl.substring(webhookUrl.length() - 4);
    }

    private String buildFallbackSummary(TechBriefingFeedItem item) {
        String text = StrUtil.blankToDefault(item.getSummary(), item.getContent());
        if (StrUtil.isBlank(text)) {
            return item.getTitle();
        }
        return StrUtil.maxLength(text, 120);
    }

    private String buildFallbackWhy(TechBriefingFeedItem item, TechBriefingSource source) {
        return StrUtil.format("来自 {} 的 {} 动态，适合用来快速判断今天的技术趋势。",
                source.getSourceName(),
                StrUtil.blankToDefault(firstCategory(item.getCategories()), "科技"));
    }

    private String summaryText(TechBriefingSummaryResult result, TechBriefingArticle article) {
        return result == null || StrUtil.isBlank(result.getSummary())
                ? StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary())
                : result.getSummary();
    }

    private String summaryWhy(TechBriefingSummaryResult result, TechBriefingArticle article) {
        return result == null || StrUtil.isBlank(result.getWhyImportant())
                ? StrUtil.blankToDefault(article.getSummaryZh(), article.getSummary())
                : result.getWhyImportant();
    }

    private String summaryImpact(TechBriefingSummaryResult result) {
        return result == null ? null : result.getImpactScope();
    }

    private String firstCategory(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return "科技";
        }
        return categories.get(0);
    }

    private List<String> parseTags(String tagsJson) {
        if (StrUtil.isBlank(tagsJson)) {
            return List.of();
        }
        try {
            List<String> tags = JSONUtil.toList(tagsJson, String.class);
            return tags == null ? List.of() : tags;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}
