package com.ecru.web.controller;

import com.ecru.common.result.Result;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.common.util.UserContext;
import com.ecru.web.dto.request.MaterialQuestionRequest;
import com.ecru.web.service.KnowledgeBaseService;
import com.ecru.web.service.MaterialKnowledgeAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库", description = "服装面料、材质与洗护知识接口")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final MaterialKnowledgeAssistantService materialKnowledgeAssistantService;
    private final AiImageAnalyzerService aiImageAnalyzerService;

    public KnowledgeController(KnowledgeBaseService knowledgeBaseService,
                               MaterialKnowledgeAssistantService materialKnowledgeAssistantService,
                               @Qualifier("aiImageAnalyzerService") AiImageAnalyzerService aiImageAnalyzerService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.materialKnowledgeAssistantService = materialKnowledgeAssistantService;
        this.aiImageAnalyzerService = aiImageAnalyzerService;
    }

    @GetMapping("/search")
    @Operation(summary = "搜索知识库", description = "支持面料、指南和水洗标搜索")
    public Result<Map<String, Object>> search(
            @RequestParam("query") String query,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return Result.success(knowledgeBaseService.search(query, type, limit));
    }

    @GetMapping("/fabric/{fabricId}")
    @Operation(summary = "获取面料详情", description = "根据面料 ID 获取详细知识内容")
    public Result<Map<String, Object>> getFabricDetail(@PathVariable Long fabricId) {
        return Result.success(knowledgeBaseService.getFabricDetail(fabricId));
    }

    @GetMapping("/guides/{guideId}")
    @Operation(summary = "获取搭配指南详情", description = "根据指南 ID 获取详细内容")
    public Result<Map<String, Object>> getGuideDetail(@PathVariable Long guideId) {
        return Result.success(knowledgeBaseService.getGuideDetail(guideId));
    }

    @GetMapping("/care-labels/{symbolCode}")
    @Operation(summary = "获取洗护标详情", description = "根据洗护标编码获取洗护说明")
    public Result<Map<String, Object>> getCareLabelDetail(@PathVariable String symbolCode) {
        return Result.success(knowledgeBaseService.getCareLabelDetail(symbolCode));
    }

    @PostMapping("/materials/ask")
    @Operation(summary = "材质问答", description = "围绕材质、面料和洗护知识进行问答，AI 不可用时自动回退知识库总结")
    public Result<Map<String, Object>> askMaterialQuestion(@Valid @RequestBody MaterialQuestionRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(materialKnowledgeAssistantService.askMaterialQuestion(
                request.getMaterial(),
                request.getQuestion(),
                userId));
    }

    @PostMapping("/materials/analyze")
    @Operation(summary = "分析材质和洗护标签图片", description = "识别图片中的材质、成分和洗护信息，可附带提问")
    public Result<Map<String, Object>> analyzeMaterialLabel(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "question", required = false) String question,
            @RequestParam(value = "materialHint", required = false) String materialHint,
            HttpServletRequest request) {
        try {
            Part imagePart = null;
            if ((image == null || image.isEmpty()) && request instanceof MultipartHttpServletRequest multipartRequest) {
                image = multipartRequest.getFile("image");
                if ((image == null || image.isEmpty()) && !multipartRequest.getFileMap().isEmpty()) {
                    image = multipartRequest.getFileMap().values().iterator().next();
                }
            }
            if ((image == null || image.isEmpty()) && request != null) {
                imagePart = request.getPart("image");
                if ((imagePart == null || imagePart.getSize() <= 0) && request.getParts() != null) {
                    for (Part part : request.getParts()) {
                        if (part != null && part.getSize() > 0 && part.getSubmittedFileName() != null) {
                            imagePart = part;
                            break;
                        }
                    }
                }
            }
            if ((image == null || image.isEmpty()) && (imagePart == null || imagePart.getSize() <= 0)) {
                return Result.error(400, "请上传图片");
            }
            Long userId = UserContext.getCurrentUserId();
            AiImageAnalyzerService.MaterialLabelAnalysisResult analysis;
            if (image != null && !image.isEmpty()) {
                analysis = aiImageAnalyzerService.analyzeMaterialLabel(image.getInputStream());
            } else {
                try (var inputStream = imagePart.getInputStream()) {
                    analysis = aiImageAnalyzerService.analyzeMaterialLabel(inputStream);
                }
            }
            return Result.success(materialKnowledgeAssistantService.buildMaterialAnalysisResponse(
                    analysis,
                    materialHint,
                    question,
                    userId));
        } catch (Exception e) {
            log.error("分析材质/洗护图片失败", e);
            return Result.error(500, "分析材质/洗护图片失败: " + e.getMessage());
        }
    }
}
