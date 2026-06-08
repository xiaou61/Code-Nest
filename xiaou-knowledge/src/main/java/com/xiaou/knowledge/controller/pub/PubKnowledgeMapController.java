package com.xiaou.knowledge.controller.pub;

import com.xiaou.common.annotation.Log;
import com.xiaou.common.core.domain.Result;
import com.xiaou.common.core.domain.PageResult;
import com.xiaou.common.core.domain.ResultCode;
import com.xiaou.knowledge.domain.KnowledgeMap;
import com.xiaou.knowledge.dto.request.PublishedKnowledgeMapQueryRequest;
import com.xiaou.knowledge.dto.response.KnowledgeMapListResponse;
import com.xiaou.knowledge.dto.response.KnowledgeNodeTreeResponse;
import com.xiaou.knowledge.service.KnowledgeMapService;
import com.xiaou.knowledge.service.KnowledgeNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端知识图谱控制器
 * 
 * @author xiaou
 */
@Slf4j
@RestController
@RequestMapping("/pub/knowledge/maps")
@RequiredArgsConstructor
@Tag(name = "用户端知识图谱", description = "用户端知识图谱相关接口")
@Validated
public class PubKnowledgeMapController {
    
    private final KnowledgeMapService knowledgeMapService;
    private final KnowledgeNodeService knowledgeNodeService;
    
    @Operation(summary = "获取已发布的知识图谱列表")
    @Log(module = "知识图谱", type = Log.OperationType.SELECT, description = "分页查询已发布知识图谱列表")
    @PostMapping("/list")
    public Result<PageResult<KnowledgeMapListResponse>> getPublishedMapList(@RequestBody PublishedKnowledgeMapQueryRequest request) {
        log.info("Controller收到已发布知识图谱列表查询请求: {}", request);
        PageResult<KnowledgeMapListResponse> result = knowledgeMapService.getPublishedList(request);
        log.info("Controller返回结果，total: {}", result.getTotal());
        return Result.success(result);
    }
    
    @Operation(summary = "根据ID获取知识图谱详情")
    @GetMapping("/{id}")
    public Result<KnowledgeMap> getMapById(
            @Parameter(description = "图谱ID") @PathVariable Long id) {
        
        KnowledgeMap knowledgeMap = knowledgeMapService.getById(id);
        // 只返回已发布的图谱
        if (knowledgeMap == null || !knowledgeMap.getStatus().equals(KnowledgeMap.Status.PUBLISHED.getCode())) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "知识图谱未发布或不存在");
        }
        
        // 增加查看次数
        knowledgeMapService.incrementViewCount(id);
        
        return Result.success(knowledgeMap);
    }
    
    @Operation(summary = "获取图谱节点树")
    @GetMapping("/{mapId}/nodes")
    public Result<List<KnowledgeNodeTreeResponse>> getNodeTree(
            @Parameter(description = "图谱ID") @PathVariable Long mapId) {
        
        // 检查图谱是否已发布
        KnowledgeMap knowledgeMap = knowledgeMapService.getById(mapId);
        if (knowledgeMap == null || !knowledgeMap.getStatus().equals(KnowledgeMap.Status.PUBLISHED.getCode())) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "知识图谱未发布或不存在");
        }
        
        List<KnowledgeNodeTreeResponse> nodeTree = knowledgeNodeService.getTreeByMapId(mapId);
        return Result.success(nodeTree);
    }
    
    @Operation(summary = "搜索节点")
    @GetMapping("/{mapId}/nodes/search")
    public Result<List<KnowledgeNodeTreeResponse>> searchNodes(
            @Parameter(description = "图谱ID") @PathVariable Long mapId,
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        
        // 检查图谱是否已发布
        KnowledgeMap knowledgeMap = knowledgeMapService.getById(mapId);
        if (knowledgeMap == null || !knowledgeMap.getStatus().equals(KnowledgeMap.Status.PUBLISHED.getCode())) {
            return Result.error(ResultCode.DATA_NOT_EXIST.getCode(), "知识图谱未发布或不存在");
        }
        
        List<KnowledgeNodeTreeResponse> nodes = knowledgeNodeService.searchNodes(mapId, keyword);
        return Result.success(nodes);
    }
    
    @Operation(summary = "记录节点查看")
    @PostMapping("/nodes/{nodeId}/view")
    public Result<Boolean> recordNodeView(
            @Parameter(description = "节点ID") @PathVariable Long nodeId) {
        
        boolean success = knowledgeNodeService.incrementViewCount(nodeId);
        return Result.success(success);
    }
} 
