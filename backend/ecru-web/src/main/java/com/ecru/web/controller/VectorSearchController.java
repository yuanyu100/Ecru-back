package com.ecru.web.controller;

import com.ecru.outfit.service.rag.VectorSearchServiceV3;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量搜索控制器
 */
@RestController
@RequestMapping("/vector-search")
@Tag(name = "向量搜索", description = "向量搜索控制器")
public class VectorSearchController {

    @Autowired
    private VectorSearchServiceV3 vectorSearchServiceV3;

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
            var results = vectorSearchServiceV3.searchClothes(userId, query, limit);
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
