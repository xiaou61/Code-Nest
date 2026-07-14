package com.xiaou.chat.service.impl;

import com.xiaou.chat.domain.ChatMessage;
import com.xiaou.chat.domain.ChatRoom;
import com.xiaou.chat.dto.ChatHistoryRequest;
import com.xiaou.chat.dto.ChatHistoryResponse;
import com.xiaou.chat.dto.ChatMessageRequest;
import com.xiaou.chat.mapper.ChatMessageMapper;
import com.xiaou.chat.service.ChatRoomService;
import com.xiaou.chat.service.ChatUserBanService;
import com.xiaou.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    private static final long ROOM_ID = 10L;
    private static final long USER_ID = 42L;
    private static final long MESSAGE_ID = 1001L;
    private static final long REPLY_ID = 900L;
    private static final String IP_ADDRESS = "203.0.113.42";
    private static final String DEVICE_INFO = "chat-service-test";

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMessageRequests")
    void shouldRejectInvalidInputBeforeExternalLookups(
            String scenario, ChatMessageRequest request, String expectedMessage) {
        Fixture fixture = new Fixture();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendMessage(request, USER_ID, IP_ADDRESS, DEVICE_INFO)
        );

        assertEquals(expectedMessage, exception.getMessage(), scenario);
        verifyNoInteractions(fixture.roomService, fixture.banService, fixture.messageMapper);
    }

    @Test
    void shouldStopBeforePersistenceWhenUserIsBanned() {
        Fixture fixture = new Fixture();
        fixture.prepareRoom();
        when(fixture.banService.isUserBanned(USER_ID, ROOM_ID)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendMessage(
                        textRequest("hello"), USER_ID, IP_ADDRESS, DEVICE_INFO)
        );

        assertEquals("您已被禁言，无法发送消息", exception.getMessage());
        verify(fixture.roomService).getOfficialRoom();
        verify(fixture.banService).isUserBanned(USER_ID, ROOM_ID);
        verifyNoInteractions(fixture.messageMapper);
    }

    @Test
    void shouldNormalizeAndPersistDefaultTextMessageInOrder() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        ChatMessage persisted = persistedMessage(1, "hello", null);
        fixture.persistAndReload(persisted);
        ChatMessageRequest request = textRequest("  hello  ");
        request.setMessageType(null);
        request.setImageUrl("https://example.com/ignored.png");

        ChatMessage result = fixture.service.sendMessage(
                request, USER_ID, IP_ADDRESS, DEVICE_INFO);

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        InOrder order = inOrder(fixture.roomService, fixture.banService, fixture.messageMapper);
        order.verify(fixture.roomService).getOfficialRoom();
        order.verify(fixture.banService).isUserBanned(USER_ID, ROOM_ID);
        order.verify(fixture.messageMapper).insert(messageCaptor.capture());
        order.verify(fixture.messageMapper).selectById(MESSAGE_ID);

        ChatMessage inserted = messageCaptor.getValue();
        assertAll(
                () -> assertEquals(MESSAGE_ID, inserted.getId()),
                () -> assertEquals(ROOM_ID, inserted.getRoomId()),
                () -> assertEquals(USER_ID, inserted.getUserId()),
                () -> assertEquals(1, inserted.getMessageType()),
                () -> assertEquals("hello", inserted.getContent()),
                () -> assertNull(inserted.getImageUrl()),
                () -> assertEquals(0, inserted.getIsDeleted()),
                () -> assertEquals(IP_ADDRESS, inserted.getIpAddress()),
                () -> assertEquals(DEVICE_INFO, inserted.getDeviceInfo()),
                () -> assertSame(persisted, result)
        );
    }

    @Test
    void shouldNormalizeImageMessageAndCaption() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        ChatMessage persisted = persistedMessage(2, "screenshot", "/api/files/demo.png");
        fixture.persistAndReload(persisted);
        ChatMessageRequest request = imageRequest(
                "  screenshot  ", "  /api/files/demo.png  ");

        ChatMessage result = fixture.service.sendMessage(
                request, USER_ID, IP_ADDRESS, DEVICE_INFO);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertAll(
                () -> assertEquals(2, captor.getValue().getMessageType()),
                () -> assertEquals("screenshot", captor.getValue().getContent()),
                () -> assertEquals("/api/files/demo.png", captor.getValue().getImageUrl()),
                () -> assertSame(persisted, result)
        );
    }

    @Test
    void shouldUseImagePlaceholderForBlankCaption() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        fixture.persistAndReload(persistedMessage(2, "[图片]", "https://example.com/a.png"));

        fixture.service.sendMessage(
                imageRequest("   ", "https://example.com/a.png"),
                USER_ID,
                IP_ADDRESS,
                DEVICE_INFO
        );

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertEquals("[图片]", captor.getValue().getContent());
    }

    @Test
    void shouldCopyValidReplyMetadataAndTruncateLongText() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        ChatMessage reply = replyMessage(ROOM_ID, 0);
        reply.setContent("x".repeat(60));
        reply.setUserNickname("Alice");
        when(fixture.messageMapper.selectById(REPLY_ID)).thenReturn(reply);
        fixture.persistAndReload(persistedMessage(1, "reply", null));
        ChatMessageRequest request = textRequest("reply");
        request.setReplyToId(REPLY_ID);

        fixture.service.sendMessage(request, USER_ID, IP_ADDRESS, DEVICE_INFO);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertAll(
                () -> assertEquals(REPLY_ID, captor.getValue().getReplyToId()),
                () -> assertEquals("Alice", captor.getValue().getReplyToUser()),
                () -> assertEquals("x".repeat(50) + "...",
                        captor.getValue().getReplyToContent())
        );
    }

    @Test
    void shouldUsePlaceholderWhenReplyingToImage() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        ChatMessage reply = replyMessage(ROOM_ID, 0);
        reply.setMessageType(2);
        reply.setContent(null);
        when(fixture.messageMapper.selectById(REPLY_ID)).thenReturn(reply);
        fixture.persistAndReload(persistedMessage(1, "reply", null));
        ChatMessageRequest request = textRequest("reply");
        request.setReplyToId(REPLY_ID);

        fixture.service.sendMessage(request, USER_ID, IP_ADDRESS, DEVICE_INFO);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertEquals("[图片]", captor.getValue().getReplyToContent());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidReplyMessages")
    void shouldIgnoreReplyMetadataOutsideTheActiveRoom(
            String scenario, ChatMessage invalidReply) {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        when(fixture.messageMapper.selectById(REPLY_ID)).thenReturn(invalidReply);
        fixture.persistAndReload(persistedMessage(1, "message", null));
        ChatMessageRequest request = textRequest("message");
        request.setReplyToId(REPLY_ID);

        fixture.service.sendMessage(request, USER_ID, IP_ADDRESS, DEVICE_INFO);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertAll(scenario,
                () -> assertNull(captor.getValue().getReplyToId()),
                () -> assertNull(captor.getValue().getReplyToUser()),
                () -> assertNull(captor.getValue().getReplyToContent())
        );
    }

    @Test
    void shouldFailWithoutReloadWhenInsertIsRejected() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        when(fixture.messageMapper.insert(any(ChatMessage.class))).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendMessage(
                        textRequest("hello"), USER_ID, IP_ADDRESS, DEVICE_INFO)
        );

        assertEquals("发送消息失败", exception.getMessage());
        verify(fixture.messageMapper, never()).selectById(anyLong());
    }

    @Test
    void shouldFailWhenInsertedMessageCannotBeReloaded() {
        Fixture fixture = new Fixture();
        fixture.allowSending();
        when(fixture.messageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(MESSAGE_ID);
            return 1;
        });
        when(fixture.messageMapper.selectById(MESSAGE_ID)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendMessage(
                        textRequest("hello"), USER_ID, IP_ADDRESS, DEVICE_INFO)
        );

        assertEquals("发送消息失败", exception.getMessage());
    }

    @Test
    void shouldDeclareTransactionForInsertAndReloadConsistency() throws NoSuchMethodException {
        Method sendMessage = ChatMessageServiceImpl.class.getMethod(
                "sendMessage", ChatMessageRequest.class, Long.class, String.class, String.class);
        AnnotationTransactionAttributeSource attributes =
                new AnnotationTransactionAttributeSource();

        assertNotNull(attributes.getTransactionAttribute(sendMessage, ChatMessageServiceImpl.class));
    }

    @Test
    void shouldCapConfiguredImageUrlLimitAtDatabaseColumnLength() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "maxImageUrlLength", 1024);
        ChatMessageRequest request = imageRequest(
                null, "https://example.com/" + "x".repeat(481));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendMessage(request, USER_ID, IP_ADDRESS, DEVICE_INFO)
        );

        assertEquals("图片地址过长", exception.getMessage());
        verifyNoInteractions(fixture.roomService, fixture.banService, fixture.messageMapper);
    }

    @Test
    void shouldReturnHistoryInChronologicalOrderWithRecallState() {
        Fixture fixture = new Fixture();
        fixture.prepareRoom();
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setLastMessageId(200L);
        request.setPageSize(2);
        ChatMessage newer = historyMessage(2L, 30_000L);
        ChatMessage older = historyMessage(1L, 180_000L);
        when(fixture.messageMapper.selectHistory(ROOM_ID, 200L, 2))
                .thenReturn(new ArrayList<>(List.of(newer, older)));

        ChatHistoryResponse response = fixture.service.getHistory(request);

        assertAll(
                () -> assertEquals(List.of(1L, 2L), response.getMessages().stream()
                        .map(message -> message.getId())
                        .toList()),
                () -> assertFalse(response.getMessages().get(0).getCanRecall()),
                () -> assertTrue(response.getMessages().get(1).getCanRecall()),
                () -> assertTrue(response.getHasMore())
        );
        verify(fixture.messageMapper).selectHistory(ROOM_ID, 200L, 2);
    }

    @Test
    void shouldReportNoMoreHistoryWhenPageIsNotFull() {
        Fixture fixture = new Fixture();
        fixture.prepareRoom();
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setPageSize(2);
        when(fixture.messageMapper.selectHistory(ROOM_ID, 0L, 2))
                .thenReturn(new ArrayList<>(List.of(historyMessage(1L, 30_000L))));

        ChatHistoryResponse response = fixture.service.getHistory(request);

        assertFalse(response.getHasMore());
    }

    @Test
    void shouldRejectRecallWhenMessageDoesNotExist() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.selectById(MESSAGE_ID)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.recallMessage(MESSAGE_ID, USER_ID)
        );

        assertEquals("消息不存在", exception.getMessage());
        verify(fixture.messageMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectRecallWhenMessageWasAlreadyDeleted() {
        Fixture fixture = new Fixture();
        ChatMessage message = recallableMessage(USER_ID, 30_000L);
        message.setIsDeleted(1);
        when(fixture.messageMapper.selectById(MESSAGE_ID)).thenReturn(message);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.recallMessage(MESSAGE_ID, USER_ID)
        );

        assertEquals("消息不存在", exception.getMessage());
        verify(fixture.messageMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectRecallFromAnotherUser() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.selectById(MESSAGE_ID))
                .thenReturn(recallableMessage(USER_ID + 1, 30_000L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.recallMessage(MESSAGE_ID, USER_ID)
        );

        assertEquals("只能撤回自己的消息", exception.getMessage());
        verify(fixture.messageMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectRecallOutsideTimeWindow() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.selectById(MESSAGE_ID))
                .thenReturn(recallableMessage(USER_ID, 121_000L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.recallMessage(MESSAGE_ID, USER_ID)
        );

        assertEquals("消息发送超过2分钟，无法撤回", exception.getMessage());
        verify(fixture.messageMapper, never()).deleteById(anyLong());
    }

    @Test
    void shouldRejectRecallWhenDeleteAffectsNoRows() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.selectById(MESSAGE_ID))
                .thenReturn(recallableMessage(USER_ID, 30_000L));
        when(fixture.messageMapper.deleteById(MESSAGE_ID)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.recallMessage(MESSAGE_ID, USER_ID)
        );

        assertEquals("撤回消息失败", exception.getMessage());
    }

    @Test
    void shouldRecallRecentOwnedMessage() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.selectById(MESSAGE_ID))
                .thenReturn(recallableMessage(USER_ID, 30_000L));
        when(fixture.messageMapper.deleteById(MESSAGE_ID)).thenReturn(1);

        fixture.service.recallMessage(MESSAGE_ID, USER_ID);

        InOrder order = inOrder(fixture.messageMapper);
        order.verify(fixture.messageMapper).selectById(MESSAGE_ID);
        order.verify(fixture.messageMapper).deleteById(MESSAGE_ID);
    }

    @Test
    void shouldDeleteMessageWhenMapperUpdatesARow() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.deleteById(MESSAGE_ID)).thenReturn(1);

        fixture.service.deleteMessage(MESSAGE_ID);

        verify(fixture.messageMapper).deleteById(MESSAGE_ID);
    }

    @Test
    void shouldRejectDeleteWhenMapperUpdatesNoRows() {
        Fixture fixture = new Fixture();
        when(fixture.messageMapper.deleteById(MESSAGE_ID)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.deleteMessage(MESSAGE_ID)
        );

        assertEquals("删除消息失败", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyBatchIds")
    void shouldRejectEmptyBatchDelete(String scenario, List<Long> ids) {
        Fixture fixture = new Fixture();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.batchDeleteMessages(ids)
        );

        assertEquals("请选择要删除的消息", exception.getMessage(), scenario);
        verifyNoInteractions(fixture.messageMapper);
    }

    @Test
    void shouldRejectBatchDeleteWhenMapperUpdatesNoRows() {
        Fixture fixture = new Fixture();
        List<Long> ids = List.of(1L, 2L);
        when(fixture.messageMapper.deleteBatch(ids)).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.batchDeleteMessages(ids)
        );

        assertEquals("批量删除消息失败", exception.getMessage());
    }

    @Test
    void shouldBatchDeleteMessages() {
        Fixture fixture = new Fixture();
        List<Long> ids = List.of(1L, 2L);
        when(fixture.messageMapper.deleteBatch(ids)).thenReturn(2);

        fixture.service.batchDeleteMessages(ids);

        verify(fixture.messageMapper).deleteBatch(ids);
    }

    @Test
    void shouldPersistSystemAnnouncement() {
        Fixture fixture = new Fixture();
        fixture.prepareRoom();
        when(fixture.messageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        fixture.service.sendAnnouncement("maintenance notice");

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(fixture.messageMapper).insert(captor.capture());
        assertAll(
                () -> assertEquals(ROOM_ID, captor.getValue().getRoomId()),
                () -> assertEquals(0L, captor.getValue().getUserId()),
                () -> assertEquals(3, captor.getValue().getMessageType()),
                () -> assertEquals("maintenance notice", captor.getValue().getContent()),
                () -> assertEquals(0, captor.getValue().getIsDeleted())
        );
    }

    @Test
    void shouldRejectAnnouncementWhenInsertFails() {
        Fixture fixture = new Fixture();
        fixture.prepareRoom();
        when(fixture.messageMapper.insert(any(ChatMessage.class))).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> fixture.service.sendAnnouncement("maintenance notice")
        );

        assertEquals("发送系统公告失败", exception.getMessage());
    }

    private static Stream<Arguments> invalidMessageRequests() {
        return Stream.of(
                Arguments.of("null request", null, "消息不能为空"),
                Arguments.of("unsupported type", request(3, "hello", null), "不支持的消息类型"),
                Arguments.of("missing text", request(1, null, null), "消息内容不能为空"),
                Arguments.of("blank text", request(1, "   ", null), "消息内容不能为空"),
                Arguments.of("long text", request(1, "x".repeat(101), null),
                        "消息内容过长，最多100个字符"),
                Arguments.of("missing image URL", request(2, null, null), "图片消息地址不能为空"),
                Arguments.of("long image URL",
                        request(2, null, "https://example.com/" + "x".repeat(481)),
                        "图片地址过长"),
                Arguments.of("invalid image scheme", request(2, null, "javascript:alert(1)"),
                        "图片地址格式不合法"),
                Arguments.of("long image caption",
                        request(2, "x".repeat(101), "https://example.com/a.png"),
                        "图片说明过长，最多100个字符")
        );
    }

    private static Stream<Arguments> invalidReplyMessages() {
        ChatMessage otherRoom = replyMessage(ROOM_ID + 1, 0);
        ChatMessage deleted = replyMessage(ROOM_ID, 1);
        return Stream.of(
                Arguments.of("reply does not exist", null),
                Arguments.of("reply belongs to another room", otherRoom),
                Arguments.of("reply has been deleted", deleted)
        );
    }

    private static Stream<Arguments> emptyBatchIds() {
        return Stream.of(
                Arguments.of("null list", null),
                Arguments.of("empty list", List.of())
        );
    }

    private static ChatMessageRequest textRequest(String content) {
        return request(1, content, null);
    }

    private static ChatMessageRequest imageRequest(String content, String imageUrl) {
        return request(2, content, imageUrl);
    }

    private static ChatMessageRequest request(Integer type, String content, String imageUrl) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessageType(type);
        request.setContent(content);
        request.setImageUrl(imageUrl);
        return request;
    }

    private static ChatMessage persistedMessage(int type, String content, String imageUrl) {
        ChatMessage message = new ChatMessage();
        message.setId(MESSAGE_ID);
        message.setRoomId(ROOM_ID);
        message.setUserId(USER_ID);
        message.setMessageType(type);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setIsDeleted(0);
        return message;
    }

    private static ChatMessage replyMessage(long roomId, int deleted) {
        ChatMessage message = new ChatMessage();
        message.setId(REPLY_ID);
        message.setRoomId(roomId);
        message.setMessageType(1);
        message.setContent("original message");
        message.setUserNickname("reply-user");
        message.setIsDeleted(deleted);
        return message;
    }

    private static ChatMessage historyMessage(long id, long ageMillis) {
        ChatMessage message = persistedMessage(1, "message-" + id, null);
        message.setId(id);
        message.setCreateTime(new Date(System.currentTimeMillis() - ageMillis));
        return message;
    }

    private static ChatMessage recallableMessage(long ownerId, long ageMillis) {
        ChatMessage message = historyMessage(MESSAGE_ID, ageMillis);
        message.setUserId(ownerId);
        message.setIsDeleted(0);
        return message;
    }

    private static final class Fixture {

        private final ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
        private final ChatRoomService roomService = mock(ChatRoomService.class);
        private final ChatUserBanService banService = mock(ChatUserBanService.class);
        private final ChatMessageServiceImpl service =
                new ChatMessageServiceImpl(messageMapper, roomService, banService);

        private Fixture() {
            ReflectionTestUtils.setField(service, "maxTextLength", 100);
            ReflectionTestUtils.setField(service, "maxImageUrlLength", 500);
        }

        private void prepareRoom() {
            ChatRoom room = new ChatRoom();
            room.setId(ROOM_ID);
            when(roomService.getOfficialRoom()).thenReturn(room);
        }

        private void allowSending() {
            prepareRoom();
            when(banService.isUserBanned(USER_ID, ROOM_ID)).thenReturn(false);
        }

        private void persistAndReload(ChatMessage persisted) {
            when(messageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
                ChatMessage message = invocation.getArgument(0);
                message.setId(MESSAGE_ID);
                return 1;
            });
            when(messageMapper.selectById(MESSAGE_ID)).thenReturn(persisted);
        }
    }
}
