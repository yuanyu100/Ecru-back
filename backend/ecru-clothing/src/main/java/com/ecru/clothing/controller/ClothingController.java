package com.ecru.clothing.controller;

import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.ImageUploadRequest;
import com.ecru.clothing.dto.request.PinduoduoImportCommitRequest;
import com.ecru.clothing.dto.request.PinduoduoImportPreviewRequest;
import com.ecru.clothing.dto.request.RecordWearRequest;
import com.ecru.clothing.dto.request.SetFrequencyRequest;
import com.ecru.clothing.dto.request.UpdateClothingRequest;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.dto.response.ClothingStatisticsVO;
import com.ecru.clothing.dto.response.PinduoduoImportPreviewVO;
import com.ecru.clothing.dto.response.PinduoduoImportResultVO;
import com.ecru.clothing.service.ClothingImportService;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.result.Result;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.util.UserContext;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

@RestController
@RequestMapping("/clothings")
@Tag(name = "衣物管理", description = "衣物相关 RESTful 接口")
public class ClothingController {

    @Autowired
    private ClothingService clothingService;

    @Autowired
    private ClothingImportService clothingImportService;

    @Autowired
    private ImageStorageService imageStorageService;

    @PostMapping
    @Operation(summary = "创建衣物", description = "创建新的衣物记录，支持图片 URL")
    public Result<ClothingDetailVO> createClothing(@RequestBody CreateClothingRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.createClothing(userId, request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "创建衣物并上传图片", description = "创建新的衣物记录并上传图片")
    public Result<ClothingDetailVO> createClothingWithImage(@org.springframework.web.bind.annotation.RequestPart("image") MultipartFile image,
                                                            @RequestParam("name") String name,
                                                            @RequestParam(value = "category", required = false) String category,
                                                            @RequestParam(value = "primaryColor", required = false) String primaryColor,
                                                            @RequestParam(value = "autoRecognize", defaultValue = "false") Boolean autoRecognize) {
        Long userId = UserContext.getCurrentUserId();

        CreateClothingRequest request = new CreateClothingRequest();
        request.setName(name);
        request.setCategory(category);
        request.setPrimaryColor(primaryColor);
        request.setAutoRecognize(autoRecognize);

        return Result.success(clothingService.createClothing(userId, request, image));
    }

    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传衣物图片", description = "仅上传图片并返回可访问 URL")
    public Result<String> uploadClothingImage(@org.springframework.web.bind.annotation.RequestPart("file") Part file) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(imageStorageService.uploadImage(file, userId));
    }

    @PostMapping("/images/upload-base64")
    @Operation(summary = "上传衣物图片 Base64", description = "移动端通过 Base64 上传图片并返回可访问 URL")
    public Result<String> uploadClothingImageBase64(@RequestBody ImageUploadRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(imageStorageService.uploadBase64Image(
                request.getData(),
                request.getFilename(),
                request.getContentType(),
                userId
        ));
    }

    @GetMapping
    @Operation(summary = "查询衣物列表", description = "分页查询当前用户的衣物列表")
    public Result<PageInfo<ClothingListVO>> getClothingList(ClothingQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingList(userId, request));
    }

    @PostMapping("/list")
    @Operation(summary = "查询衣物列表", description = "POST 方式分页查询当前用户的衣物列表")
    public Result<PageInfo<ClothingListVO>> getClothingListPost(@RequestBody ClothingQueryRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingList(userId, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取衣物详情", description = "根据 ID 获取衣物详情")
    public Result<ClothingDetailVO> getClothingDetail(@Parameter(description = "衣物 ID") @PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingDetail(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新衣物", description = "更新衣物的详细信息")
    public Result<ClothingDetailVO> updateClothing(@Parameter(description = "衣物 ID") @PathVariable Long id,
                                                   @RequestBody UpdateClothingRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.updateClothing(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除衣物", description = "删除衣物记录")
    public Result<Void> deleteClothing(@Parameter(description = "衣物 ID") @PathVariable Long id,
                                       @Parameter(description = "是否强制删除")
                                       @RequestParam(value = "force", defaultValue = "false") Boolean force) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.deleteClothing(userId, id, force);
        return Result.success();
    }

    @PostMapping("/{id}/recognize")
    @Operation(summary = "AI 识别衣物", description = "使用 AI 重新识别衣物属性")
    public Result<ClothingDetailVO> recognizeClothing(@Parameter(description = "衣物 ID") @PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.recognizeClothing(userId, id));
    }

    @PutMapping("/{id}/frequency")
    @Operation(summary = "设置搭配频率", description = "设置衣物搭配频率（1-5）")
    public Result<Void> setFrequency(@Parameter(description = "衣物 ID") @PathVariable Long id,
                                     @RequestBody SetFrequencyRequest request) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.setFrequency(userId, id, request.getFrequencyLevel());
        return Result.success();
    }

    @PostMapping("/{id}/wear")
    @Operation(summary = "记录穿着", description = "记录衣物穿着情况")
    public Result<Void> recordWear(@Parameter(description = "衣物 ID") @PathVariable Long id,
                                   @RequestBody RecordWearRequest request) {
        Long userId = UserContext.getCurrentUserId();
        clothingService.recordWear(userId, id, request);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取衣物统计", description = "获取当前用户衣物统计信息")
    public Result<ClothingStatisticsVO> getClothingStatistics(@Parameter(description = "统计周期")
                                                              @RequestParam(value = "period", defaultValue = "all") String period) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingService.getClothingStatistics(userId, period));
    }
    @PostMapping("/imports/pinduoduo/preview")
    @Operation(summary = "预览拼多多订单导入", description = "解析浏览器提取 JSON、HAR 或页面 HTML，返回候选衣物")
    public Result<PinduoduoImportPreviewVO> previewPinduoduoImport(@RequestBody PinduoduoImportPreviewRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingImportService.previewPinduoduoImport(userId, request));
    }

    @PostMapping("/imports/pinduoduo")
    @Operation(summary = "导入拼多多订单衣物", description = "将预览后选中的拼多多订单商品批量导入到个人衣橱")
    public Result<PinduoduoImportResultVO> importPinduoduoItems(@RequestBody PinduoduoImportCommitRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(clothingImportService.importPinduoduoItems(userId, request));
    }
}
