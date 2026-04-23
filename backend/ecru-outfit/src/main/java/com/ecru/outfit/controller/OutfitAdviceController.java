package com.ecru.outfit.controller;

import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.ecru.outfit.entity.OutfitFeedback;
import com.ecru.outfit.entity.UserStyleProfile;
import com.ecru.outfit.service.OutfitAdviceService;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.outfit.service.rag.RagService;
import com.ecru.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 搭配建议控制器
 */
@Slf4j
@RestController
@RequestMapping("/outfit")
@Tag(name = "搭配建议", description = "智能搭配建议相关接口")
public class OutfitAdviceController {

    @Autowired
    private OutfitAdviceService outfitAdviceService;

    @Autowired
    @Qualifier("aiImageAnalyzerService")
    private AiImageAnalyzerService imageAnalyzerService;

    @Autowired
    private RagService ragService;

    /**
     * 获取搭配建议
     * @param request HTTP请求
     * @param image 穿搭照片
     * @param description 文字描述
     * @param location 地理位置
     * @param occasion 场合
     * @return 搭配建议
     */
    @PostMapping("/advice")
    @Operation(summary = "获取搭配建议", description = "核心接口，调用Agent工作流生成搭配建议")
    public Result<?> getOutfitAdvice(
            HttpServletRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "occasion", required = false) String occasion
    ) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");

            // 验证参数
            if (image == null && description == null) {
                return Result.error(400, "请提供穿搭照片或文字描述");
            }

            // 处理图片
            java.io.InputStream imageStream = image != null ? image.getInputStream() : null;

            // 获取搭配建议
            var record = outfitAdviceService.getOutfitAdvice(
                    userId,
                    imageStream,
                    description,
                    location,
                    occasion
            );

            return Result.success(record);
        } catch (IOException e) {
            log.error("处理图片失败: {}", e.getMessage());
            return Result.error(400, "处理图片失败");
        } catch (Exception e) {
            log.error("获取搭配建议失败: {}", e.getMessage());
            return Result.error(500, "获取搭配建议失败");
        }
    }

    /**
     * 分析穿搭照片
     * @param request HTTP请求
     * @param image 穿搭照片
     * @return 分析结果
     */
    @PostMapping("/analyze")
    @Operation(summary = "分析穿搭照片", description = "仅分析照片，不生成搭配建议")
    public Result<?> analyzeOutfit(
            HttpServletRequest request,
            @RequestParam("image") MultipartFile image
    ) {
        try {
            // 分析图片
            var result = imageAnalyzerService.analyzeOutfit(image.getInputStream());

            if (result == null) {
                return Result.error(500, "分析照片失败");
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("分析穿搭照片失败: {}", e.getMessage());
            return Result.error(500, "分析穿搭照片失败");
        }
    }

    /**
     * 获取历史搭配记录
     * @param request HTTP请求
     * @param page 页码
     * @param size 每页大小
     * @return 搭配记录列表
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史搭配记录", description = "分页查询用户的搭配历史")
    public Result<?> getHistory(
            HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");

            // 获取历史记录
            List<OutfitAdviceRecord> records = outfitAdviceService.getHistory(userId, page, size);

            return Result.success(records);
        } catch (Exception e) {
            log.error("获取历史搭配记录失败: {}", e.getMessage());
            return Result.error(500, "获取历史搭配记录失败");
        }
    }

    /**
     * 获取搭配详情
     * @param id 搭配记录ID
     * @return 搭配详情
     */
    @GetMapping("/history/{id}")
    @Operation(summary = "获取搭配详情", description = "获取单条搭配建议的完整信息")
    public Result<?> getAdviceDetail(@PathVariable Long id) {
        try {
            var record = outfitAdviceService.getAdviceById(id);
            if (record == null) {
                return Result.error(404, "搭配记录不存在");
            }
            return Result.success(record);
        } catch (Exception e) {
            log.error("获取搭配详情失败: {}", e.getMessage());
            return Result.error(500, "获取搭配详情失败");
        }
    }

    /**
     * 删除搭配记录
     * @param id 搭配记录ID
     * @return 操作结果
     */
    @DeleteMapping("/history/{id}")
    @Operation(summary = "删除搭配记录", description = "软删除搭配记录")
    public Result<?> deleteAdvice(@PathVariable Long id) {
        try {
            boolean success = outfitAdviceService.deleteAdvice(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error(404, "搭配记录不存在");
            }
        } catch (Exception e) {
            log.error("删除搭配记录失败: {}", e.getMessage());
            return Result.error(500, "删除搭配记录失败");
        }
    }

    /**
     * 收藏/取消收藏搭配
     * @param id 搭配记录ID
     * @param isFavorite 是否收藏
     * @return 操作结果
     */
    @PostMapping("/history/{id}/favorite")
    @Operation(summary = "收藏/取消收藏搭配", description = "切换搭配记录的收藏状态")
    public Result<?> toggleFavorite(
            @PathVariable Long id,
            @RequestParam("isFavorite") Boolean isFavorite
    ) {
        try {
            boolean success = outfitAdviceService.toggleFavorite(id, isFavorite);
            if (success) {
                return Result.success("操作成功");
            } else {
                return Result.error(404, "搭配记录不存在");
            }
        } catch (Exception e) {
            log.error("操作收藏状态失败: {}", e.getMessage());
            return Result.error(500, "操作收藏状态失败");
        }
    }

    /**
     * 提交搭配反馈
     * @param request HTTP请求
     * @param id 搭配记录ID
     * @param feedback 反馈信息
     * @return 操作结果
     */
    @PostMapping("/{id}/feedback")
    @Operation(summary = "提交搭配反馈", description = "对搭配方案进行评分和反馈")
    public Result<?> submitFeedback(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody OutfitFeedback feedback
    ) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");

            // 提交反馈
            var savedFeedback = outfitAdviceService.submitFeedback(id, userId, feedback);

            return Result.success(savedFeedback);
        } catch (Exception e) {
            log.error("提交搭配反馈失败: {}", e.getMessage());
            return Result.error(500, "提交搭配反馈失败");
        }
    }

    /**
     * 语义检索衣物
     * @param request HTTP请求
     * @param query 查询文本
     * @param limit 限制数量
     * @return 检索结果
     */
    @PostMapping("/search")
    @Operation(summary = "语义检索衣物", description = "基于自然语言查询衣物")
    public Result<?> searchClothes(
            HttpServletRequest request,
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit
    ) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");

            // 搜索衣物
            var results = ragService.searchClothes(userId, query, limit);

            return Result.success(results);
        } catch (Exception e) {
            log.error("语义检索衣物失败: {}", e.getMessage());
            return Result.error(500, "语义检索衣物失败");
        }
    }

    /**
     * 获取用户风格档案
     * @param request HTTP请求
     * @return 风格档案
     */
    @GetMapping("/style-profile")
    @Operation(summary = "获取用户风格档案", description = "获取用户的风格档案信息")
    public Result<?> getStyleProfile(HttpServletRequest request) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");

            // 获取风格档案
            var profile = outfitAdviceService.getStyleProfile(userId);

            return Result.success(profile);
        } catch (Exception e) {
            log.error("获取用户风格档案失败: {}", e.getMessage());
            return Result.error(500, "获取用户风格档案失败");
        }
    }

    /**
     * 更新用户风格档案
     * @param request HTTP请求
     * @param profile 风格档案
     * @return 操作结果
     */
    @PutMapping("/style-profile")
    @Operation(summary = "更新用户风格档案", description = "更新用户的风格档案信息")
    public Result<?> updateStyleProfile(
            HttpServletRequest request,
            @RequestBody UserStyleProfile profile
    ) {
        try {
            // 获取用户ID
            Long userId = (Long) request.getAttribute("userId");
            profile.setUserId(userId);

            // 更新档案
            boolean success = outfitAdviceService.updateStyleProfile(profile);

            if (success) {
                return Result.success("更新成功");
            } else {
                return Result.error(500, "更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户风格档案失败: {}", e.getMessage());
            return Result.error(500, "更新用户风格档案失败");
        }
    }

    /**
     * Agent聊天接口
     * @param request HTTP请求
     * @param chatRequest 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/chat")
    @Operation(summary = "Agent聊天", description = "智能聊天接口，可根据天气和需求推荐衣物")
    public Result<?> chatWithAgent(
            HttpServletRequest request,
            @RequestBody OutfitAdviceService.ChatRequest chatRequest
    ) {
        try {
            // 获取用户ID，默认值为1，方便测试
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = 1L; // 默认用户ID，方便测试
            }

            // 验证参数
            if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
                return Result.error(400, "请输入聊天内容");
            }

            // 处理聊天请求
            OutfitAdviceService.ChatResponse response = outfitAdviceService.chatWithAgent(userId, chatRequest);

            return Result.success(response);
        } catch (Exception e) {
            log.error("Agent聊天失败: {}", e.getMessage());
            return Result.error(500, "Agent聊天失败");
        }
    }

}
