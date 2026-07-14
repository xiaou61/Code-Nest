package com.xiaou.system.service.impl;

import com.xiaou.common.core.domain.PageResult;
import com.xiaou.system.domain.SysAgentAudit;
import com.xiaou.system.dto.AgentAuditPreviewRequest;
import com.xiaou.system.dto.AgentAuditQueryRequest;
import com.xiaou.system.dto.AgentAuditResponse;
import com.xiaou.system.dto.AgentAuditResultRequest;
import com.xiaou.system.mapper.SysAgentAuditMapper;
import com.xiaou.system.service.SysAgentAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysAgentAuditServiceImplTest {

    @Mock
    private SysAgentAuditMapper agentAuditMapper;

    @Test
    void shouldCreatePreviewAuditRecord() {
        when(agentAuditMapper.insert(any(SysAgentAudit.class))).thenAnswer(invocation -> {
            SysAgentAudit audit = invocation.getArgument(0);
            audit.setId(1001L);
            return 1;
        });

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);
        AgentAuditPreviewRequest request = new AgentAuditPreviewRequest();
        request.setConfirmationId("agent-confirmation-abc");
        request.setUserMessage("创建社区标签 AI问答 描述 AI 相关讨论 排序 15");
        request.setIntent("community.tag.create");
        request.setActionId("admin.community.createTag");
        request.setRoute("/community/tags");
        request.setRiskLevel("medium");
        request.setRiskCategory("L1_LOW_RISK_WRITE");
        request.setSummary("已生成低风险写入预览：创建社区标签“AI问答”。确认前不会调用写入接口。");
        request.setPayloadJson("{\"name\":\"AI问答\"}");
        request.setDiffJson("[{\"field\":\"name\",\"before\":null,\"after\":\"AI问答\"}]");
        request.setPlanJson("[{\"step\":\"等待确认\",\"status\":\"blocked\"}]");
        request.setIdempotencyKey("agent-idempotency-abc");

        AgentAuditResponse response = service.createPreview(request, 7L, "admin");

        ArgumentCaptor<SysAgentAudit> captor = ArgumentCaptor.forClass(SysAgentAudit.class);
        verify(agentAuditMapper).insert(captor.capture());
        SysAgentAudit saved = captor.getValue();
        assertNotNull(saved.getAuditId());
        assertTrue(saved.getAuditId().startsWith("agent-audit-"));
        assertEquals("agent-confirmation-abc", saved.getConfirmationId());
        assertEquals("agent-idempotency-abc", saved.getIdempotencyKey());
        assertEquals("PREVIEW", saved.getStatus());
        assertEquals("community.tag.create", saved.getIntent());
        assertEquals("admin.community.createTag", saved.getActionId());
        assertEquals("/community/tags", saved.getRoute());
        assertEquals("L1_LOW_RISK_WRITE", saved.getRiskCategory());
        assertEquals(7L, saved.getOperatorId());
        assertEquals("admin", saved.getOperatorName());
        assertNotNull(saved.getCreatedTime());
        assertNotNull(saved.getUpdatedTime());

        assertEquals(saved.getAuditId(), response.getAuditId());
        assertEquals("PREVIEW", response.getStatus());
        assertEquals("agent-confirmation-abc", response.getConfirmationId());
        assertEquals("agent-idempotency-abc", response.getIdempotencyKey());
    }

    @Test
    void shouldConfirmAndRecordExecutionResult() {
        SysAgentAudit existing = createExistingAudit("PREVIEW");
        when(agentAuditMapper.selectByAuditId("agent-audit-1")).thenReturn(existing);
        when(agentAuditMapper.updateByAuditId(any(SysAgentAudit.class))).thenReturn(1);

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);

        AgentAuditResponse confirmed = service.confirm("agent-audit-1");
        assertEquals("CONFIRMED", confirmed.getStatus());

        ArgumentCaptor<SysAgentAudit> confirmCaptor = ArgumentCaptor.forClass(SysAgentAudit.class);
        verify(agentAuditMapper).updateByAuditId(confirmCaptor.capture());
        SysAgentAudit confirmedAudit = confirmCaptor.getValue();
        assertEquals("agent-audit-1", confirmedAudit.getAuditId());
        assertEquals("CONFIRMED", confirmedAudit.getStatus());
        assertEquals("PREVIEW", confirmedAudit.getExpectedStatus());
        assertNotNull(confirmedAudit.getConfirmedTime());

        AgentAuditResultRequest resultRequest = new AgentAuditResultRequest();
        resultRequest.setSuccess(true);
        resultRequest.setResultJson("{\"id\":42}");

        AgentAuditResponse executed = service.recordResult("agent-audit-1", resultRequest);
        assertEquals("EXECUTED", executed.getStatus());
        assertEquals("{\"id\":42}", executed.getResultJson());
    }

    @Test
    void shouldCancelPreviewAudit() {
        SysAgentAudit existing = createExistingAudit("PREVIEW");
        when(agentAuditMapper.selectByAuditId("agent-audit-1")).thenReturn(existing);
        when(agentAuditMapper.updateByAuditId(any(SysAgentAudit.class))).thenReturn(1);

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);

        AgentAuditResponse cancelled = service.cancel("agent-audit-1", "管理员取消");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("管理员取消", cancelled.getErrorMessage());
    }

    @Test
    void shouldRejectInvalidAuditStateTransitions() {
        SysAgentAudit existing = createExistingAudit("EXECUTED");
        when(agentAuditMapper.selectByAuditId("agent-audit-1")).thenReturn(existing);

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);

        assertThrows(IllegalStateException.class, () -> service.confirm("agent-audit-1"));
        assertThrows(IllegalStateException.class, () -> service.cancel("agent-audit-1", "管理员取消"));
        verify(agentAuditMapper, never()).updateByAuditId(any(SysAgentAudit.class));
    }

    @Test
    void shouldRejectConcurrentAuditStateChange() {
        SysAgentAudit existing = createExistingAudit("PREVIEW");
        when(agentAuditMapper.selectByAuditId("agent-audit-1")).thenReturn(existing);
        when(agentAuditMapper.updateByAuditId(any(SysAgentAudit.class))).thenReturn(0);

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);

        assertThrows(IllegalStateException.class, () -> service.confirm("agent-audit-1"));
    }

    @Test
    void shouldReturnPagedAuditResponses() {
        AgentAuditQueryRequest query = new AgentAuditQueryRequest();
        query.setIntent("community.tag.create");
        query.setPageNum(1);
        query.setPageSize(10);

        when(agentAuditMapper.selectList(query)).thenReturn(List.of(createExistingAudit("EXECUTED")));

        SysAgentAuditService service = new SysAgentAuditServiceImpl(agentAuditMapper);
        PageResult<AgentAuditResponse> page = service.getAuditPage(query);

        assertEquals(1, page.getPageNum());
        assertEquals(1, page.getRecords().size());
        assertEquals("agent-audit-1", page.getRecords().get(0).getAuditId());
        assertEquals("EXECUTED", page.getRecords().get(0).getStatus());
    }

    private SysAgentAudit createExistingAudit(String status) {
        SysAgentAudit audit = new SysAgentAudit();
        audit.setId(1L);
        audit.setAuditId("agent-audit-1");
        audit.setConfirmationId("agent-confirmation-1");
        audit.setIdempotencyKey("agent-idempotency-1");
        audit.setIntent("community.tag.create");
        audit.setActionId("admin.community.createTag");
        audit.setRoute("/community/tags");
        audit.setRiskLevel("medium");
        audit.setRiskCategory("L1_LOW_RISK_WRITE");
        audit.setStatus(status);
        audit.setSummary("已生成低风险写入预览");
        audit.setPayloadJson("{\"name\":\"AI问答\"}");
        audit.setDiffJson("[]");
        audit.setPlanJson("[]");
        audit.setCreatedTime(LocalDateTime.now());
        audit.setUpdatedTime(LocalDateTime.now());
        return audit;
    }
}
