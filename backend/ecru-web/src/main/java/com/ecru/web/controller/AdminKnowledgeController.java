package com.ecru.web.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.web.dto.request.AdminKnowledgeListRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.CareLabelKnowledgeUpsertRequest;
import com.ecru.web.dto.request.FabricKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.FabricKnowledgeUpsertRequest;
import com.ecru.web.dto.request.GuideKnowledgeBatchImportRequest;
import com.ecru.web.dto.request.GuideKnowledgeUpsertRequest;
import com.ecru.web.service.KnowledgeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/fabrics/import")
    @Operation(summary = "批量导入面料知识")
    public Result<Map<String, Object>> importFabrics(@RequestBody FabricKnowledgeBatchImportRequest request) {
        requireAdmin();
        return Result.success("面料知识批量导入成功", knowledgeAdminService.importFabrics(request));
    }

    @PutMapping("/fabrics/{id}")
    @Operation(summary = "更新面料知识")
    public Result<Map<String, Object>> updateFabric(@PathVariable Long id,
                                                    @Valid @RequestBody FabricKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("面料知识更新成功", knowledgeAdminService.updateFabric(id, request));
    }

    @DeleteMapping("/fabrics/{id}")
    @Operation(summary = "删除面料知识")
    public Result<Void> deleteFabric(@PathVariable Long id) {
        requireAdmin();
        knowledgeAdminService.deleteFabric(id);
        return Result.success("面料知识删除成功", null);
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

    @PostMapping("/guides/import")
    @Operation(summary = "批量导入指南知识")
    public Result<Map<String, Object>> importGuides(@RequestBody GuideKnowledgeBatchImportRequest request) {
        requireAdmin();
        return Result.success("指南知识批量导入成功", knowledgeAdminService.importGuides(request));
    }

    @PostMapping(value = "/guides/import-pdf", consumes = "multipart/form-data")
    @Operation(summary = "从PDF导入指南知识", description = "上传PDF文件，自动解析文本内容并写入指南知识库")
    public Result<Map<String, Object>> importGuideFromPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateExisting", defaultValue = "true") boolean updateExisting) {
        requireAdmin();
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请上传PDF文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            return Result.error(400, "仅支持PDF格式文件");
        }
        return Result.success("PDF导入成功", knowledgeAdminService.importGuidesFromPdf(file, updateExisting));
    }

    @PutMapping("/guides/{id}")
    @Operation(summary = "更新指南知识")
    public Result<Map<String, Object>> updateGuide(@PathVariable Long id,
                                                   @Valid @RequestBody GuideKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("指南知识更新成功", knowledgeAdminService.updateGuide(id, request));
    }

    @DeleteMapping("/guides/{id}")
    @Operation(summary = "删除指南知识")
    public Result<Void> deleteGuide(@PathVariable Long id) {
        requireAdmin();
        knowledgeAdminService.deleteGuide(id);
        return Result.success("指南知识删除成功", null);
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

    @PostMapping("/care-labels/import")
    @Operation(summary = "批量导入洗护知识")
    public Result<Map<String, Object>> importCareLabels(@RequestBody CareLabelKnowledgeBatchImportRequest request) {
        requireAdmin();
        return Result.success("洗护知识批量导入成功", knowledgeAdminService.importCareLabels(request));
    }

    @PutMapping("/care-labels/{id}")
    @Operation(summary = "更新洗护知识")
    public Result<Map<String, Object>> updateCareLabel(@PathVariable Long id,
                                                       @Valid @RequestBody CareLabelKnowledgeUpsertRequest request) {
        requireAdmin();
        return Result.success("洗护知识更新成功", knowledgeAdminService.updateCareLabel(id, request));
    }

    @DeleteMapping("/care-labels/{id}")
    @Operation(summary = "删除洗护知识")
    public Result<Void> deleteCareLabel(@PathVariable Long id) {
        requireAdmin();
        knowledgeAdminService.deleteCareLabel(id);
        return Result.success("洗护知识删除成功", null);
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
