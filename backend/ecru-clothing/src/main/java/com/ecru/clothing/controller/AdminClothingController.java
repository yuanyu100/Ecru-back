package com.ecru.clothing.controller;

import com.ecru.clothing.dto.request.AdminClothingQueryRequest;
import com.ecru.clothing.dto.response.AdminClothingListVO;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.result.ErrorCode;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/clothings")
@RequiredArgsConstructor
@Tag(name = "管理员衣物管理", description = "管理员全局衣物管理接口")
public class AdminClothingController {

    private final ClothingService clothingService;

    @GetMapping
    @Operation(summary = "获取全局衣物列表")
    public Result<PageInfo<AdminClothingListVO>> getAdminClothings(AdminClothingQueryRequest request) {
        requireAdmin();
        return Result.success(clothingService.getAdminClothingList(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "管理员删除衣物")
    public Result<Void> deleteClothing(@PathVariable Long id,
                                       @RequestParam(value = "force", defaultValue = "false") Boolean force) {
        requireAdmin();
        clothingService.adminDeleteClothing(id, force);
        return Result.success();
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
