package com.ecru.outfit.controller;

import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.ecru.outfit.dto.request.AdminOutfitRecordQueryRequest;
import com.ecru.outfit.service.AdminOutfitRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/outfits")
@RequiredArgsConstructor
@Tag(name = "管理员穿搭记录管理", description = "管理员查看和清理全局穿搭记录")
public class AdminOutfitRecordController {

    private final AdminOutfitRecordService adminOutfitRecordService;

    @GetMapping("/overview")
    @Operation(summary = "获取穿搭记录概览")
    public Result<Map<String, Object>> getOverview() {
        requireAdmin();
        return Result.success(adminOutfitRecordService.getOverview());
    }

    @GetMapping("/records")
    @Operation(summary = "获取全局穿搭记录列表")
    public Result<Map<String, Object>> getRecords(AdminOutfitRecordQueryRequest request) {
        requireAdmin();
        return Result.success(adminOutfitRecordService.listRecords(request));
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "获取穿搭记录详情")
    public Result<Map<String, Object>> getRecordDetail(@PathVariable Long id) {
        requireAdmin();
        return Result.success(adminOutfitRecordService.getRecordDetail(id));
    }

    @DeleteMapping("/records/{id}")
    @Operation(summary = "管理员删除穿搭记录")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        requireAdmin();
        adminOutfitRecordService.deleteRecord(id);
        return Result.success("穿搭记录删除成功", null);
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
