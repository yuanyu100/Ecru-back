package com.ecru.outfit.controller;

import com.ecru.outfit.service.rag.ClothingVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 衣物向量控制器
 */
@RestController
@RequestMapping("/outfit/clothing-vector")
@Tag(name = "衣物向量化", description = "衣物向量控制器")
public class ClothingVectorController {

    @Resource
    private ClothingVectorService clothingVectorService;

    /**
     * 批量为衣物生成向量
     * @return 响应结果
     */
    @PostMapping("/batch-generate")
    @Operation(summary = "批量生成向量",description = "批量生成向量")
    public Map<String, Object> batchGenerateVectors() {
        Map<String, Object> response = new HashMap<>();
        try {
            clothingVectorService.batchGenerateAndStoreVectors();
            response.put("success", true);
            response.put("message", "批量生成向量任务已提交，正在异步处理中");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "批量生成向量失败: " + e.getMessage());
        }
        return response;
    }
}
