package com.xiaou.oj.controller;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.oj.controller.admin.OjContestController;
import com.xiaou.oj.controller.pub.OjContestPublicController;
import com.xiaou.oj.domain.OjContest;
import com.xiaou.oj.dto.contest.ContestRankingItem;
import com.xiaou.oj.service.OjContestRankingService;
import com.xiaou.oj.service.OjContestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OjContestControllerWebTest {

    @Mock
    private OjContestService contestService;

    @Mock
    private OjContestRankingService contestRankingService;

    @Test
    void shouldExposeContestRankingEndpoint() throws Exception {
        when(contestRankingService.getContestRanking(1L))
                .thenReturn(List.of(new ContestRankingItem()));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OjContestPublicController(contestService, contestRankingService))
                .build();

        mockMvc.perform(get("/oj/contests/1/ranking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldExposeAdminContestListEndpoint() throws Exception {
        when(contestService.getContests(any()))
                .thenReturn(PageResult.of(1, 10, 0L, List.of(new OjContest())));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OjContestController(contestService))
                .build();

        mockMvc.perform(post("/admin/oj/contests/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
