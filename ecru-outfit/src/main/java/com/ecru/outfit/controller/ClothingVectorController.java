package com.ecru.outfit.controller;

import com.ecru.common.service.vector.ClothingVectorService;
import com.ecru.outfit.service.rag.VectorSearchServiceV3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

/**
 * 衣物向量控制器
 */
@RestController
@RequestMapping("/outfit/clothing-vector")
@Tag(name = "衣物向量化", description = "衣物向量控制器")
public class ClothingVectorController {

    @Autowired
    private ClothingVectorService clothingVectorService;

    @Autowired
    private VectorSearchServiceV3 vectorSearchService;

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

    /**
     * 语义检索衣物
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @return 检索结果
     */
    @GetMapping("/search")
    @Operation(summary = "语义检索衣物",description = "语义检索衣物")
    public Map<String, Object> searchClothes(@RequestParam Long userId, @RequestParam String query, @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            var results = vectorSearchService.searchClothes(userId, query, limit);
            response.put("success", true);
            response.put("data", results);
            response.put("message", "语义检索成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "语义检索失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 测试端点
     * @return 测试结果
     */
    @GetMapping("/test")
    @Operation(summary = "测试端点",description = "测试端点")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "测试成功");
        return response;
    }
}

