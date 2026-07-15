package com.xiaou.moment.service.impl;

import com.xiaou.common.satoken.StpUserUtil;
import com.xiaou.moment.domain.Moment;
import com.xiaou.moment.domain.MomentFavorite;
import com.xiaou.moment.domain.MomentLike;
import com.xiaou.moment.mapper.MomentCommentMapper;
import com.xiaou.moment.mapper.MomentFavoriteMapper;
import com.xiaou.moment.mapper.MomentLikeMapper;
import com.xiaou.moment.mapper.MomentMapper;
import com.xiaou.moment.service.MomentViewService;
import com.xiaou.user.api.UserInfoApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MomentServiceImplTest {

    private static final long MOMENT_ID = 11L;
    private static final long USER_ID = 22L;

    @Mock
    private MomentMapper momentMapper;
    @Mock
    private MomentLikeMapper momentLikeMapper;
    @Mock
    private MomentCommentMapper momentCommentMapper;
    @Mock
    private MomentFavoriteMapper momentFavoriteMapper;
    @Mock
    private UserInfoApiService userInfoApiService;
    @Mock
    private MomentViewService momentViewService;

    private MomentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MomentServiceImpl(
                momentMapper,
                momentLikeMapper,
                momentCommentMapper,
                momentFavoriteMapper,
                userInfoApiService,
                momentViewService
        );
    }

    @Test
    void shouldIncrementLikeCountOnlyWhenLikeRowWasInserted() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentLikeMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(null);
        when(momentLikeMapper.insertIfAbsent(any(MomentLike.class))).thenReturn(0);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleLike(MOMENT_ID)).isTrue();
        }

        verify(momentMapper, never()).incrementLikeCount(MOMENT_ID);
    }

    @Test
    void shouldDecrementLikeCountOnlyWhenLikeRowWasDeleted() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentLikeMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(new MomentLike());
        when(momentLikeMapper.deleteByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(0);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleLike(MOMENT_ID)).isFalse();
        }

        verify(momentMapper, never()).decrementLikeCount(MOMENT_ID);
    }

    @Test
    void shouldUpdateLikeCountWhenRowMutationSucceeds() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentLikeMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(null);
        when(momentLikeMapper.insertIfAbsent(any(MomentLike.class))).thenReturn(1);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleLike(MOMENT_ID)).isTrue();
        }

        verify(momentMapper).incrementLikeCount(MOMENT_ID);
    }

    @Test
    void shouldIncrementFavoriteCountOnlyWhenFavoriteRowWasInserted() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentFavoriteMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(null);
        when(momentFavoriteMapper.insertIfAbsent(any(MomentFavorite.class))).thenReturn(0);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleFavorite(MOMENT_ID)).isTrue();
        }

        verify(momentMapper, never()).incrementFavoriteCount(MOMENT_ID);
    }

    @Test
    void shouldDecrementFavoriteCountOnlyWhenFavoriteRowWasDeleted() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentFavoriteMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(new MomentFavorite());
        when(momentFavoriteMapper.delete(MOMENT_ID, USER_ID)).thenReturn(0);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleFavorite(MOMENT_ID)).isFalse();
        }

        verify(momentMapper, never()).decrementFavoriteCount(MOMENT_ID);
    }

    @Test
    void shouldUpdateFavoriteCountWhenRowMutationSucceeds() {
        Moment moment = moment();
        when(momentMapper.selectById(MOMENT_ID)).thenReturn(moment);
        when(momentFavoriteMapper.selectByMomentIdAndUserId(MOMENT_ID, USER_ID)).thenReturn(new MomentFavorite());
        when(momentFavoriteMapper.delete(MOMENT_ID, USER_ID)).thenReturn(1);

        try (MockedStatic<StpUserUtil> login = loggedInUser()) {
            assertThat(service.toggleFavorite(MOMENT_ID)).isFalse();
        }

        verify(momentMapper).decrementFavoriteCount(MOMENT_ID);
    }

    private MockedStatic<StpUserUtil> loggedInUser() {
        MockedStatic<StpUserUtil> login = mockStatic(StpUserUtil.class);
        login.when(StpUserUtil::getLoginIdAsLong).thenReturn(USER_ID);
        return login;
    }

    private Moment moment() {
        Moment moment = new Moment();
        moment.setId(MOMENT_ID);
        moment.setUserId(USER_ID);
        return moment;
    }
}
