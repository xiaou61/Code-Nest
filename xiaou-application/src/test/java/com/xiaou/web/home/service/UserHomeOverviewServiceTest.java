package com.xiaou.web.home.service;

import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.community.service.CommunityHotPostService;
import com.xiaou.interview.service.InterviewLearnRecordService;
import com.xiaou.knowledge.service.KnowledgeMapService;
import com.xiaou.mockinterview.service.MockInterviewService;
import com.xiaou.moment.service.MomentService;
import com.xiaou.oj.service.OjProblemService;
import com.xiaou.plan.service.PlanService;
import com.xiaou.points.service.PointsService;
import com.xiaou.version.service.VersionHistoryService;
import com.xiaou.web.home.dto.UserHomeOverviewResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserHomeOverviewServiceTest {

    @Mock
    private InterviewLearnRecordService interviewLearnRecordService;
    @Mock
    private KnowledgeMapService knowledgeMapService;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatOnlineUserService chatOnlineUserService;
    @Mock
    private CommunityHotPostService communityHotPostService;
    @Mock
    private MomentService momentService;
    @Mock
    private OjProblemService ojProblemService;
    @Mock
    private MockInterviewService mockInterviewService;
    @Mock
    private PlanService planService;
    @Mock
    private PointsService pointsService;
    @Mock
    private VersionHistoryService versionHistoryService;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(10);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void shouldReturnPartialOverviewWhenGrowthSourcesAreUnavailable() {
        when(interviewLearnRecordService.getTotalLearnedCount(7L)).thenReturn(12);
        ChatRoom room = new ChatRoom();
        room.setId(1L);
        when(chatRoomService.getOfficialRoom()).thenReturn(room);
        when(chatOnlineUserService.getOnlineCount(1L)).thenReturn(3);
        when(planService.getStatsOverview(7L)).thenThrow(new IllegalStateException("plan unavailable"));
        when(mockInterviewService.getStats(7L)).thenThrow(new IllegalStateException("mock unavailable"));
        when(pointsService.getPointsBalance(7L)).thenThrow(new IllegalStateException("points unavailable"));

        UserHomeOverviewService service = new UserHomeOverviewService(
                interviewLearnRecordService,
                knowledgeMapService,
                chatRoomService,
                chatOnlineUserService,
                communityHotPostService,
                momentService,
                ojProblemService,
                mockInterviewService,
                planService,
                pointsService,
                versionHistoryService,
                executor
        );

        UserHomeOverviewResponse overview = service.getOverview(7L);

        assertThat(overview.getHeroMetrics().getLearnedCount()).isEqualTo(12);
        assertThat(overview.getHeroMetrics().getOnlineCount()).isEqualTo(3);
        assertThat(overview.getSections().get("hero").isAvailable()).isTrue();
        assertThat(overview.getSections().get("growth").isAvailable()).isFalse();
    }
}
