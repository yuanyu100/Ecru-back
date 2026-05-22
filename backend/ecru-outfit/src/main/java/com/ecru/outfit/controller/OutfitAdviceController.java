package com.ecru.outfit.controller;

import com.ecru.common.result.Result;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.common.util.UserContext;
import com.ecru.outfit.dto.response.HomeRecommendationLookDTO;
import com.ecru.outfit.dto.response.HomeRecommendationResponseDTO;
import com.ecru.outfit.dto.response.OutfitAdviceDetailDTO;
import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.ecru.outfit.entity.OutfitFeedback;
import com.ecru.outfit.entity.UserStyleArchive;
import com.ecru.outfit.service.HomeRecommendationService;
import com.ecru.outfit.service.OutfitAdviceService;
import com.ecru.outfit.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/outfit")
@Tag(name = "穿搭建议", description = "穿搭建议、历史记录、风格档案与首页推荐接口")
@RequiredArgsConstructor
public class OutfitAdviceController {

    private final OutfitAdviceService outfitAdviceService;

    @Qualifier("aiImageAnalyzerService")
    private final AiImageAnalyzerService imageAnalyzerService;

    private final RagService ragService;
    private final HomeRecommendationService homeRecommendationService;

    @PostMapping("/advice")
    @Operation(summary = "生成穿搭建议", description = "支持图片或文字输入，生成并保存穿搭方案")
    public Result<?> getOutfitAdvice(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "occasion", required = false) String occasion) {
        try (InputStream imageStream = image != null ? image.getInputStream() : null) {
            Long userId = UserContext.getCurrentUserId();
            if (image == null && (description == null || description.trim().isEmpty())) {
                return Result.error(400, "请提供穿搭图片或文字描述");
            }
            OutfitAdviceRecord record = outfitAdviceService.getOutfitAdvice(userId, imageStream, description, location, occasion);
            return Result.success(record);
        } catch (IOException e) {
            log.error("处理穿搭图片失败", e);
            return Result.error(400, "处理穿搭图片失败");
        } catch (Exception e) {
            log.error("生成穿搭建议失败", e);
            return Result.error(500, "生成穿搭建议失败");
        }
    }

    @PostMapping("/analyze")
    @Operation(summary = "分析穿搭图片", description = "只分析图片，不保存记录")
    public Result<?> analyzeOutfit(@RequestParam("image") MultipartFile image) {
        try {
            Object result = imageAnalyzerService.analyzeOutfit(image.getInputStream());
            return result == null ? Result.error(500, "分析穿搭图片失败") : Result.success(result);
        } catch (Exception e) {
            log.error("分析穿搭图片失败", e);
            return Result.error(500, "分析穿搭图片失败");
        }
    }

    @GetMapping("/history")
    @Operation(summary = "获取穿搭历史", description = "分页查询当前用户的手动生成穿搭记录")
    public Result<List<OutfitAdviceRecord>> getHistory(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            Long userId = UserContext.getCurrentUserId();
            return Result.success(outfitAdviceService.getHistory(userId, page, size));
        } catch (Exception e) {
            log.error("获取穿搭历史失败", e);
            return Result.error(500, "获取穿搭历史失败");
        }
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "获取穿搭详情", description = "获取指定穿搭记录的详情、单品与反馈")
    public Result<?> getAdviceDetail(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            OutfitAdviceDetailDTO detail = outfitAdviceService.getAdviceById(id, userId);
            return detail == null ? Result.error(404, "穿搭记录不存在") : Result.success(detail);
        } catch (Exception e) {
            log.error("获取穿搭详情失败", e);
            return Result.error(500, "获取穿搭详情失败");
        }
    }

    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除穿搭记录", description = "删除当前用户的穿搭记录")
    public Result<?> deleteAdvice(@PathVariable Long id) {
        try {
            Long userId = UserContext.getCurrentUserId();
            return outfitAdviceService.deleteAdvice(id, userId)
                    ? Result.success("删除成功")
                    : Result.error(404, "穿搭记录不存在");
        } catch (Exception e) {
            log.error("删除穿搭记录失败", e);
            return Result.error(500, "删除穿搭记录失败");
        }
    }

    @PostMapping("/history/{id}/favorite")
    @Operation(summary = "收藏穿搭记录", description = "更新收藏状态")
    public Result<?> toggleFavorite(@PathVariable Long id, @RequestParam("isFavorite") Boolean isFavorite) {
        try {
            Long userId = UserContext.getCurrentUserId();
            return outfitAdviceService.toggleFavorite(id, userId, isFavorite)
                    ? Result.success("操作成功")
                    : Result.error(404, "穿搭记录不存在");
        } catch (Exception e) {
            log.error("更新穿搭收藏状态失败", e);
            return Result.error(500, "更新穿搭收藏状态失败");
        }
    }

    @PostMapping("/{id}/feedback")
    @Operation(summary = "提交穿搭反馈", description = "提交评分与文字反馈")
    public Result<?> submitFeedback(@PathVariable Long id, @RequestBody OutfitFeedback feedback) {
        try {
            Long userId = UserContext.getCurrentUserId();
            OutfitFeedback savedFeedback = outfitAdviceService.submitFeedback(id, userId, feedback);
            return savedFeedback == null ? Result.error(404, "穿搭记录不存在") : Result.success(savedFeedback);
        } catch (Exception e) {
            log.error("提交穿搭反馈失败", e);
            return Result.error(500, "提交穿搭反馈失败");
        }
    }

    @PostMapping("/search")
    @Operation(summary = "语义检索衣物", description = "基于自然语言搜索衣橱中的相关衣物")
    public Result<?> searchClothes(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        try {
            Long userId = UserContext.getCurrentUserId();
            return Result.success(ragService.searchClothes(userId, query, limit));
        } catch (Exception e) {
            log.error("语义检索衣物失败", e);
            return Result.error(500, "语义检索衣物失败");
        }
    }

    @GetMapping("/style-profile")
    @Operation(summary = "获取风格档案", description = "获取当前用户的体型、气质与偏好档案")
    public Result<?> getStyleProfile() {
        try {
            Long userId = UserContext.getCurrentUserId();
            return Result.success(outfitAdviceService.getStyleProfile(userId));
        } catch (Exception e) {
            log.error("获取风格档案失败", e);
            return Result.error(500, "获取风格档案失败");
        }
    }

    @PutMapping("/style-profile")
    @Operation(summary = "更新风格档案", description = "创建或更新当前用户的风格档案")
    public Result<?> updateStyleProfile(@RequestBody UserStyleArchive profile) {
        try {
            profile.setUserId(UserContext.getCurrentUserId());
            return outfitAdviceService.updateStyleProfile(profile)
                    ? Result.success("更新成功")
                    : Result.error(500, "更新失败");
        } catch (Exception e) {
            log.error("更新风格档案失败", e);
            return Result.error(500, "更新风格档案失败");
        }
    }

    @GetMapping("/home-recommendations")
    @Operation(summary = "获取首页推荐搭配", description = "根据衣橱、风格档案和天气信息生成首页搭配")
    public Result<HomeRecommendationResponseDTO> getHomeRecommendations(
            @RequestParam(value = "refresh", defaultValue = "false") Boolean refresh,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "temperature", required = false) Double temperature,
            @RequestParam(value = "weatherCondition", required = false) String weatherCondition) {
        try {
            // `/outfit/home-recommendations` 是首页推荐主入口。
            // 控制器只负责接收前端传来的刷新标记和环境参数，
            // 真正的搭配生成逻辑全部下沉到 HomeRecommendationService。
            Long userId = UserContext.getCurrentUserId();
            return Result.success(homeRecommendationService.getRecommendations(
                    userId,
                    Boolean.TRUE.equals(refresh),
                    location,
                    temperature,
                    weatherCondition
            ));
        } catch (Exception e) {
            log.error("获取首页推荐搭配失败", e);
            return Result.error(500, "获取首页推荐搭配失败");
        }
    }

    @GetMapping("/home-recommendations/{id}")
    @Operation(summary = "Home recommendation detail", description = "Return the selected homepage look with item-level wardrobe metadata.")
    public Result<HomeRecommendationLookDTO> getHomeRecommendationDetail(@PathVariable Long id) {
        try {
            // 详情接口不会重新生成推荐，只是读取前面已经生成并保存好的某一套首页 look。
            Long userId = UserContext.getCurrentUserId();
            HomeRecommendationLookDTO detail = homeRecommendationService.getRecommendationDetail(userId, id);
            return detail == null ? Result.error(404, "Recommendation not found") : Result.success(detail);
        } catch (Exception e) {
            log.error("Load home recommendation detail failed", e);
            return Result.error(500, "Load home recommendation detail failed");
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "穿搭对话", description = "结合天气、场景与衣橱内容生成推荐")
    public Result<?> chatWithAgent(@RequestBody OutfitAdviceService.ChatRequest chatRequest) {
        try {
            Long userId = UserContext.getCurrentUserId();
            if (userId == null) {
                userId = 1L;
            }
            if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
                return Result.error(400, "请输入聊天内容");
            }
            return Result.success(outfitAdviceService.chatWithAgent(userId, chatRequest));
        } catch (Exception e) {
            log.error("穿搭对话失败", e);
            return Result.error(500, "穿搭对话失败");
        }
    }
}
