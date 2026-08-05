package com.xiaou.web.home.adapter.module;

import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.service.ChatOnlineUserService;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.web.home.port.UserHomePresencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 通过聊天模块的公开服务提供首页在线人数。
 */
@Component
@RequiredArgsConstructor
public class ChatUserHomePresenceAdapter implements UserHomePresencePort {

    private final ChatRoomService chatRoomService;
    private final ChatOnlineUserService chatOnlineUserService;

    @Override
    public int onlineUserCount() {
        ChatRoom room = chatRoomService.getOfficialRoom();
        if (room == null || room.getId() == null) {
            return 0;
        }
        Integer count = chatOnlineUserService.getOnlineCount(room.getId());
        return count == null ? 0 : count;
    }
}
