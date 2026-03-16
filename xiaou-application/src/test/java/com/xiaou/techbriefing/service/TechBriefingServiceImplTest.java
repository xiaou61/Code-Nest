package com.xiaou.techbriefing.service;

import com.xiaou.ai.service.AiTechBriefingService;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.techbriefing.domain.TechBriefingArticle;
import com.xiaou.techbriefing.domain.TechBriefingArticleAi;
import com.xiaou.techbriefing.domain.TechBriefingSource;
import com.xiaou.techbriefing.domain.TechBriefingSubscription;
import com.xiaou.techbriefing.dto.model.TechBriefingFeedItem;
import com.xiaou.techbriefing.dto.request.TechBriefingArticleQueryRequest;
import com.xiaou.techbriefing.dto.request.TechBriefingSubscriptionCreateRequest;
import com.xiaou.techbriefing.dto.response.TechBriefingArticleCardResponse;
import com.xiaou.techbriefing.dto.response.TechBriefingSubscriptionResponse;
import com.xiaou.techbriefing.mapper.TechBriefingArticleAiMapper;
import com.xiaou.techbriefing.mapper.TechBriefingArticleContentMapper;
import com.xiaou.techbriefing.mapper.TechBriefingArticleMapper;
import com.xiaou.techbriefing.mapper.TechBriefingFetchLogMapper;
import com.xiaou.techbriefing.mapper.TechBriefingSourceMapper;
import com.xiaou.techbriefing.mapper.TechBriefingSubscriptionMapper;
import com.xiaou.techbriefing.service.impl.TechBriefingRssClient;
import com.xiaou.techbriefing.service.impl.TechBriefingServiceImpl;
import com.xiaou.techbriefing.service.impl.TechBriefingWebhookClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechBriefingServiceImplTest {

    @Mock
    private TechBriefingSourceMapper sourceMapper;

    @Mock
    private TechBriefingArticleMapper articleMapper;

    @Mock
    private TechBriefingArticleContentMapper articleContentMapper;

    @Mock
    private TechBriefingArticleAiMapper articleAiMapper;

    @Mock
    private TechBriefingSubscriptionMapper subscriptionMapper;

    @Mock
    private TechBriefingFetchLogMapper fetchLogMapper;

    @Mock
    private TechBriefingRssClient rssClient;

    @Mock
    private TechBriefingWebhookClient webhookClient;

    @Mock
    private AiTechBriefingService aiTechBriefingService;

    @InjectMocks
    private TechBriefingServiceImpl service;

    @Test
    void listArticlesShouldReturnPagedCards() {
        TechBriefingArticleQueryRequest request = new TechBriefingArticleQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setScope("all");

        TechBriefingArticle article = new TechBriefingArticle()
                .setId(101L)
                .setSourceId(11L)
                .setTitle("OpenAI launches a new research stack")
                .setTitleZh("OpenAI 发布新的研究技术栈")
                .setSummaryZh("一套面向推理和智能体的新研究栈。")
                .setSourceName("TechCrunch")
                .setRegionType("GLOBAL")
                .setCategory("AI")
                .setPublishTime(LocalDateTime.of(2026, 3, 16, 10, 30))
                .setTranslationStatus("READY")
                .setSourceUrl("https://example.com/article/101");
        TechBriefingArticleAi articleAi = new TechBriefingArticleAi()
                .setArticleId(101L)
                .setWhyImportant("它会影响后续 AI 研究基础设施与开发者工具链。");

        when(articleMapper.countPublic(any(TechBriefingArticleQueryRequest.class))).thenReturn(1L);
        when(articleMapper.selectPublicList(any(TechBriefingArticleQueryRequest.class), eq(0), eq(10)))
                .thenReturn(List.of(article));
        when(articleAiMapper.selectByArticleIds(List.of(101L))).thenReturn(List.of(articleAi));

        PageResult<TechBriefingArticleCardResponse> result = service.listArticles(request);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("OpenAI 发布新的研究技术栈", result.getRecords().get(0).getTitle());
        assertTrue(result.getRecords().get(0).getAiSummary().contains("影响后续 AI 研究基础设施"));
    }

    @Test
    void refreshSourcesShouldPersistParsedFeedItems() {
        TechBriefingSource source = new TechBriefingSource()
                .setId(1L)
                .setSourceName("TechCrunch")
                .setSourceType("GLOBAL")
                .setFetchType("RSS")
                .setRssUrl("https://techcrunch.com/feed/")
                .setStatus("ENABLED");
        TechBriefingFeedItem feedItem = new TechBriefingFeedItem()
                .setTitle("ByteDance pauses Seedance rollout")
                .setLink("https://techcrunch.com/2026/03/15/bytedance-seedance/")
                .setSummary("The company is reportedly delaying the launch as its engineers work through legal issues.")
                .setContent("The company is reportedly delaying the launch as its engineers work through legal issues.")
                .setImageUrl("https://techcrunch.com/img.png")
                .setPublishTime(LocalDateTime.of(2026, 3, 15, 21, 1))
                .setCategories(List.of("AI", "Video"));

        when(sourceMapper.selectEnabledSources()).thenReturn(List.of(source));
        when(rssClient.fetch(source)).thenReturn(List.of(feedItem));
        when(articleMapper.selectBySourceUrl(feedItem.getLink())).thenReturn(null);
        doAnswer(invocation -> {
            TechBriefingArticle article = invocation.getArgument(0);
            article.setId(501L);
            return 1;
        }).when(articleMapper).insert(any(TechBriefingArticle.class));

        service.refreshAllSources();

        ArgumentCaptor<TechBriefingArticle> articleCaptor = ArgumentCaptor.forClass(TechBriefingArticle.class);
        verify(articleMapper).insert(articleCaptor.capture());
        verify(articleContentMapper).insert(any());
        verify(articleAiMapper).insert(any());
        verify(fetchLogMapper).insert(any());

        TechBriefingArticle persisted = articleCaptor.getValue();
        assertEquals("ByteDance pauses Seedance rollout", persisted.getTitle());
        assertEquals("ByteDance pauses Seedance rollout", persisted.getTitleZh());
        assertEquals("GLOBAL", persisted.getRegionType());
        assertEquals("READY", persisted.getStatus());
        assertFalse(Boolean.TRUE.equals(persisted.getDeleted()));
    }

    @Test
    void createSubscriptionShouldValidateAndMaskWebhook() {
        TechBriefingSubscriptionCreateRequest request = new TechBriefingSubscriptionCreateRequest();
        request.setChannelType("FEISHU");
        request.setWebhookUrl("https://open.feishu.cn/open-apis/bot/v2/hook/1234567890abcdef");
        request.setWebhookSecret("secret-token");
        request.setTargetName("AI 快讯群");
        request.setTopicPreferences(List.of("AI", "芯片"));
        request.setFrequency("DAILY");

        when(subscriptionMapper.selectActiveByChannelAndWebhook(eq("FEISHU"), eq(request.getWebhookUrl()))).thenReturn(null);
        doAnswer(invocation -> {
            TechBriefingSubscription subscription = invocation.getArgument(0);
            subscription.setId(9001L);
            return 1;
        }).when(subscriptionMapper).insert(any(TechBriefingSubscription.class));

        TechBriefingSubscriptionResponse response = service.createSubscription(1001L, request);

        ArgumentCaptor<TechBriefingSubscription> captor = ArgumentCaptor.forClass(TechBriefingSubscription.class);
        verify(subscriptionMapper).insert(captor.capture());
        TechBriefingSubscription persisted = captor.getValue();
        assertEquals("FEISHU", persisted.getChannelType());
        assertEquals("AI 快讯群", persisted.getTargetName());
        assertNotNull(persisted.getConfigHash());
        assertEquals(9001L, response.getId());
        assertTrue(response.getMaskedWebhookUrl().contains("***"));
    }

    @Test
    void testSubscriptionShouldSendChannelMessage() {
        TechBriefingSubscription subscription = new TechBriefingSubscription()
                .setId(9001L)
                .setUserId(1001L)
                .setChannelType("DINGTALK")
                .setWebhookUrl("https://oapi.dingtalk.com/robot/send?access_token=test")
                .setWebhookSecret("ding-secret")
                .setTargetName("研发群")
                .setStatus("ENABLED");
        when(subscriptionMapper.selectById(9001L)).thenReturn(subscription);

        service.testSubscription(1001L, 9001L);

        verify(webhookClient).sendTestMessage(eq(subscription), eq("科技热点速览订阅测试成功，你将收到后续的每日科技简报。"));
    }
}
