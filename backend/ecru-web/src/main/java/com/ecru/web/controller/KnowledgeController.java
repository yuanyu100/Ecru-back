package com.ecru.web.controller;

import com.ecru.common.result.Result;
import com.ecru.web.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库", description = "服装面料知识与搭配指南接口")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/search")
    @Operation(summary = "搜索知识库", description = "支持面料与搭配指南搜索")
    public Result<Map<String, Object>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(knowledgeBaseService.search(query, type, limit));
    }

    @GetMapping("/fabric/{fabricId}")
    @Operation(summary = "获取面料详情", description = "根据面料ID获取详细知识内容")
    public Result<Map<String, Object>> getFabricDetail(@PathVariable Long fabricId) {
        return Result.success(knowledgeBaseService.getFabricDetail(fabricId));
    }

    @GetMapping("/guides/{guideId}")
    @Operation(summary = "获取搭配指南详情", description = "根据指南ID获取详细内容")
    public Result<Map<String, Object>> getGuideDetail(@PathVariable Long guideId) {
        return Result.success(knowledgeBaseService.getGuideDetail(guideId));
    }
}
