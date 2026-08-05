package com.xiaou.system.service.impl;

import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.points.service.PointsService;
import com.xiaou.resilience.ResilientExecutor;
import com.xiaou.system.dto.DashboardOverviewResponse;
import com.xiaou.system.service.SysLoginLogService;
import com.xiaou.system.service.SysOperationLogService;
import com.xiaou.user.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysDashboardServiceImplTest {

    @Mock
    private UserInfoService userInfoService;
    @Mock
    private PointsService pointsService;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatOnlineUserService chatOnlineUserService;
    @Mock
    private SysLoginLogService loginLogService;
    @Mock
    private SysOperationLogService operationLogService;
    @Spy
    private ResilientExecutor resilientExecutor = new ResilientExecutor();

    @InjectMocks
    private SysDashboardServiceImpl service;

    @Test
    void shouldDegradeOnlyTheFailedDashboardSource() {
        ChatRoom officialRoom = new ChatRoom();
        officialRoom.setId(1L);
        when(chatRoomService.getOfficialRoom()).thenReturn(officialRoom);
        when(chatOnlineUserService.getOnlineCount(1L)).thenReturn(3);
        when(userInfoService.getUserList(any())).thenThrow(new IllegalStateException("user unavailable"));

        DashboardOverviewResponse response = service.getOverview();

        assertThat(response.getTotalUsers()).isZero();
        assertThat(response.getOnlineUserCount()).isEqualTo(3);
        assertThat(response.getModuleHealthList())
                .filteredOn(item -> "用户服务".equals(item.getName()))
                .singleElement()
                .extracting(DashboardOverviewResponse.ModuleHealthItem::getStatus)
                .isEqualTo("danger");
        assertThat(response.getModuleHealthList())
                .filteredOn(item -> "聊天室服务".equals(item.getName()))
                .singleElement()
                .extracting(DashboardOverviewResponse.ModuleHealthItem::getStatus)
                .isNotEqualTo("danger");
    }
}
