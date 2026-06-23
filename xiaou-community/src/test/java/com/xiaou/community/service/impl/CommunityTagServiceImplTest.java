package com.xiaou.community.service.impl;

import com.xiaou.community.domain.CommunityTag;
import com.xiaou.community.mapper.CommunityPostTagMapper;
import com.xiaou.community.mapper.CommunityTagMapper;
import com.xiaou.community.service.CommunityCacheService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityTagServiceImplTest {

    @Test
    void toggleTagStatusShouldFlipStatusAndClearTagCache() {
        CommunityTagMapper tagMapper = mock(CommunityTagMapper.class);
        CommunityPostTagMapper postTagMapper = mock(CommunityPostTagMapper.class);
        CommunityCacheService cacheService = mock(CommunityCacheService.class);
        CommunityTagServiceImpl service = new CommunityTagServiceImpl(tagMapper, postTagMapper, cacheService);

        CommunityTag tag = new CommunityTag();
        tag.setId(42L);
        tag.setStatus(1);

        when(tagMapper.selectById(42L)).thenReturn(tag);
        when(tagMapper.update(argThat(updated -> {
            assertEquals(42L, updated.getId());
            assertEquals(0, updated.getStatus());
            return true;
        }))).thenReturn(1);

        service.toggleTagStatus(42L);

        verify(cacheService).evictTags();
    }
}
