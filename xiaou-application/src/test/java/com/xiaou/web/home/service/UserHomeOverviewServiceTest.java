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
import com.xiaou.web.growthcoach.dto.GrowthCoachBriefingResponse;
import com.xiaou.web.growthcoach.service.GrowthCoachBriefingService;
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
    @Mock
    private GrowthCoachBriefingService growthCoachBriefingService;

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

        UserHomeOverviewService service = newService();

        UserHomeOverviewResponse overview = service.getOverview(7L);

        assertThat(overview.getHeroMetrics().getLearnedCount()).isEqualTo(12);
        assertThat(overview.getHeroMetrics().getOnlineCount()).isEqualTo(3);
        assertThat(overview.getSections().get("hero").isAvailable()).isTrue();
        assertThat(overview.getSections().get("growth").isAvailable()).isFalse();
    }

    @Test
    void shouldExposeTheStableGrowthCoachActionInTheHomeOverview() {
        GrowthCoachBriefingResponse source = new GrowthCoachBriefingResponse();
        GrowthCoachBriefingResponse.PrimaryAction action = new GrowthCoachBriefingResponse.PrimaryAction();
        action.setActionType("APPLICATION_FOLLOW_UP");
        action.setActionId(42L);
        action.setTitle("更新字节跳动投递进展");
        action.setExpectedMinutes(15);
        action.setReason("这条投递已到跟进时间");
        action.setExpectedChange("更新后将重新计算求职下一步");
        action.setRoutePath("/career-loop?focus=applications");
        action.setSource("application_outcomes");
        action.setRiskLevel("attention");
        action.setPrefillMessage("记录本次沟通结果");
        action.setTrackingId("gpa-20260729-a1b2c3");
        source.setPrimaryAction(action);
        when(growthCoachBriefingService.getForUser(7L)).thenReturn(source);

        UserHomeOverviewResponse overview = newService().getOverview(7L);

        assertThat(overview.getTodayAction().isAvailable()).isTrue();
        assertThat(overview.getTodayAction().getActionType()).isEqualTo("APPLICATION_FOLLOW_UP");
        assertThat(overview.getTodayAction().getActionId()).isEqualTo(42L);
        assertThat(overview.getTodayAction().getTitle()).isEqualTo("更新字节跳动投递进展");
        assertThat(overview.getTodayAction().getEstimatedMinutes()).isEqualTo(15);
        assertThat(overview.getTodayAction().getStartRoute()).isEqualTo("/career-loop?focus=applications");
        assertThat(overview.getTodayAction().getSource()).isEqualTo("application_outcomes");
        assertThat(overview.getTodayAction().getTrackingId()).isEqualTo("gpa-20260729-a1b2c3");
    }

    private UserHomeOverviewService newService() {
        return new UserHomeOverviewService(
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
                growthCoachBriefingService,
                executor
        );
    }
}
