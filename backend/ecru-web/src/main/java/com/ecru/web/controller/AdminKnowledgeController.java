package com.ecru.web.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.web.dto.request.AdminKnowledgeListRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeUpsertRequest;
import com.ecru.web.dto.request.FabricKnowledgeUpsertRequest;
import com.ecru.web.dto.request.GuideKnowledgeUpsertRequest;
import com.ecru.web.service.KnowledgeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
@Tag(name = "管理员知识库管理", description = "面料、指南、洗护知识的后台维护接口")
public class AdminKnowledgeController {

    private final KnowledgeAdminService knowledgeAdminService;

    @GetMapping("/overview")
    @Operation(summary = "获取知识库概览")
    public Result<Map<String, Object>> getOverview() {
        requireAdmin();
        return Result.success(knowledgeAdminService.getOverview());
    }

    @GetMapping("/fabrics")
    @Operation(summary = "获取面料知识列表")
    public Result<Map<String, Object>> listFabrics(AdminKnowledgeListRequest request) {
        requireAdmin();
        return Result.success(knowledgeAdminService.listFabrics(request));
    }

    @PostMapping("/fabrics")
    @Operation(summary = "新增面料知识")
    public Result<Map<String, Object>> createFabric(@Valid @RequestBody FabricKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("面料知识新增成功", knowledgeAdminService.createFabric(request));
    }

    @PutMapping("/fabrics/{id}")
    @Operation(summary = "更新面料知识")
    public Result<Map<String, Object>> updateFabric(@PathVariable Long id,
                                                    @Valid @RequestBody FabricKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("面料知识更新成功", knowledgeAdminService.updateFabric(id, request));
    }

    @GetMapping("/guides")
    @Operation(summary = "获取指南知识列表")
    public Result<Map<String, Object>> listGuides(AdminKnowledgeListRequest request) {
        requireAdmin();
        return Result.success(knowledgeAdminService.listGuides(request));
    }

    @PostMapping("/guides")
    @Operation(summary = "新增指南知识")
    public Result<Map<String, Object>> createGuide(@Valid @RequestBody GuideKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("指南知识新增成功", knowledgeAdminService.createGuide(request));
    }

    @PutMapping("/guides/{id}")
    @Operation(summary = "更新指南知识")
    public Result<Map<String, Object>> updateGuide(@PathVariable Long id,
                                                   @Valid @RequestBody GuideKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("指南知识更新成功", knowledgeAdminService.updateGuide(id, request));
    }

    @GetMapping("/care-labels")
    @Operation(summary = "获取洗护知识列表")
    public Result<Map<String, Object>> listCareLabels(AdminKnowledgeListRequest request) {
        requireAdmin();
        return Result.success(knowledgeAdminService.listCareLabels(request));
    }

    @PostMapping("/care-labels")
    @Operation(summary = "新增洗护知识")
    public Result<Map<String, Object>> createCareLabel(@Valid @RequestBody CareLabelKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("洗护知识新增成功", knowledgeAdminService.createCareLabel(request));
    }

    @PutMapping("/care-labels/{id}")
    @Operation(summary = "更新洗护知识")
    public Result<Map<String, Object>> updateCareLabel(@PathVariable Long id,
                                                       @Valid @RequestBody CareLabelKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("洗护知识更新成功", knowledgeAdminService.updateCareLabel(id, request));
    }

    private void requireAdmin() {
        if (!UserContext.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!UserContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
