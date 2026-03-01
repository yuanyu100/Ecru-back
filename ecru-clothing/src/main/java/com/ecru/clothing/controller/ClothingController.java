package com.ecru.clothing.controller;

import com.ecru.clothing.dto.request.*;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.dto.response.ClothingStatisticsVO;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.result.Result;
import com.ecru.common.util.UserContext;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clothings")
@Tag(name = "衣物管理", description = "衣物相关的RESTful接口")
public class ClothingController {

    @Autowired
    private ClothingService clothingService;

    @PostMapping()
    @Operation(summary = "创建衣物", description = "创建新的衣物记录，支持图片上传和AI识别")
    public Result<ClothingDetailVO> createClothing(@RequestBody CreateClothingRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.createClothing(userId,  request));
    }

    @GetMapping
    @Operation(summary = "查询衣物列表", description = "分页查询用户的所有衣物，支持多条件筛选和排序")
    public Result<PageInfo<ClothingListVO>> getClothingList(ClothingQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingList(userId, request));
    }

    @PostMapping("/list")
    @Operation(summary = "查询衣物列表（POST）", description = "分页查询用户的所有衣物，支持多条件筛选和排序")
    public Result<PageInfo<ClothingListVO>> getClothingListPost(@RequestBody ClothingQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingList(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取衣物详情", description = "根据ID获取衣物的详细信息")
    public Result<ClothingDetailVO> getClothingDetail(
            @Parameter(description = "衣物ID") @PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingDetail(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新衣物", description = "更新衣物的详细信息")
    public Result<ClothingDetailVO> updateClothing(
            @Parameter(description = "衣物ID") @PathVariable Long id,
            @RequestBody UpdateClothingRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.updateClothing(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除衣物", description = "删除衣物记录（软删除）")
    public Result<Void> deleteClothing(
            @Parameter(description = "衣物ID") @PathVariable Long id,
            @Parameter(description = "是否硬删除") @RequestParam(value = "force", defaultValue = "false") Boolean force) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.deleteClothing(userId, id, force);
        return Result.success();
    }

    @PostMapping("/{id}/recognize")
    @Operation(summary = "AI识别衣物", description = "使用AI重新识别衣物的属性")
    public Result<ClothingDetailVO> recognizeClothing(
            @Parameter(description = "衣物ID") @PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.recognizeClothing(userId, id));
    }

    @PutMapping("/{id}/frequency")
    @Operation(summary = "设置搭配频率", description = "为衣物设置搭配频率（1-5）")
    public Result<Void> setFrequency(
            @Parameter(description = "衣物ID") @PathVariable Long id,
            @RequestBody SetFrequencyRequest request) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.setFrequency(userId, id, request.getFrequencyLevel());
        return Result.success();
    }

    @PostMapping("/{id}/wear")
    @Operation(summary = "记录穿着", description = "记录衣物的穿着情况")
    public Result<Void> recordWear(
            @Parameter(description = "衣物ID") @PathVariable Long id,
            @RequestBody RecordWearRequest request) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.recordWear(userId, id, request);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取衣物统计", description = "获取用户的衣物统计信息")
    public Result<ClothingStatisticsVO> getClothingStatistics(
            @Parameter(description = "统计周期") @RequestParam(value = "period", defaultValue = "all") String period) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingStatistics(userId, period));
    }

}
