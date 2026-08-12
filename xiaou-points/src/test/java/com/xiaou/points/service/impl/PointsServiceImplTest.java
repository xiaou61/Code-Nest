package com.xiaou.points.service.impl;

import com.github.pagehelper.Page;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.points.domain.UserCheckinBitmap;
import com.xiaou.points.domain.UserPointsBalance;
import com.xiaou.points.dto.UserPointsListRequest;
import com.xiaou.points.dto.UserPointsRankingResponse;
import com.xiaou.points.mapper.UserCheckinBitmapMapper;
import com.xiaou.points.mapper.UserPointsBalanceMapper;
import com.xiaou.points.mapper.UserPointsDetailMapper;
import com.xiaou.user.api.UserInfoApiService;
import com.xiaou.user.api.dto.SimpleUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private UserPointsBalanceMapper pointsBalanceMapper;

    @Mock
    private UserPointsDetailMapper pointsDetailMapper;

    @Mock
    private UserCheckinBitmapMapper checkinBitmapMapper;

    @Mock
    private UserInfoApiService userInfoApiService;

    private PointsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PointsServiceImpl(
                pointsBalanceMapper,
                pointsDetailMapper,
                checkinBitmapMapper,
                userInfoApiService
        );
    }

    @Test
    void shouldKeepPagingTotalAndUseBatchUserLookups() {
        UserPointsBalance first = balance(1L, 100);
        UserPointsBalance second = balance(2L, 50);
        Page<UserPointsBalance> page = new Page<>(1, 2);
        page.setTotal(5);
        page.addAll(List.of(first, second));

        UserCheckinBitmap latest = new UserCheckinBitmap();
        latest.setUserId(1L);
        latest.setYearMonth("2026-08");
        latest.setLastCheckinDate(LocalDate.now());
        latest.setContinuousDays(3);

        when(pointsBalanceMapper.selectBalancePage(any(), any(), any(), any(), any())).thenReturn(page);
        when(userInfoApiService.getSimpleUserInfoBatch(anyList())).thenReturn(Map.of(
                1L, SimpleUserInfo.builder().id(1L).nickname("u1").build(),
                2L, SimpleUserInfo.builder().id(2L).nickname("u2").build()
        ));
        when(checkinBitmapMapper.selectLatestByUserIds(anyList())).thenReturn(List.of(latest));

        PageResult<UserPointsRankingResponse> result = service.getUserPointsList(new UserPointsListRequest());

        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getRecords().get(0).getRanking()).isEqualTo(1);
        assertThat(result.getRecords().get(1).getRanking()).isEqualTo(2);
        verify(pointsBalanceMapper).selectBalancePage(eq(null), eq(null), eq(null), eq("points"), eq("desc"));
        verify(userInfoApiService, never()).getSimpleUserInfo(any());
        verify(checkinBitmapMapper, never()).selectLatestByUserId(any());
    }

    private UserPointsBalance balance(Long userId, int points) {
        UserPointsBalance balance = new UserPointsBalance();
        balance.setUserId(userId);
        balance.setTotalPoints(points);
        return balance;
    }
}
