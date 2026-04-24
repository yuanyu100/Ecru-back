package com.ecru.outfit.service.rag;

import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.common.service.vector.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG检索服务
 */
@Service
public class RagService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorSearchServiceV3 vectorSearchService;

    @Autowired
    private ClothingMapper clothingMapper;

    /**
     * 语义检索衣物
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @param negativePreferences 负面偏好
     * @return 检索结果
     */
    public List<VectorSearchServiceV3.VectorSearchResult> searchClothes(Long userId, String query, Integer limit, List<String> negativePreferences) {
        try {
            return vectorSearchService.searchClothes(userId, query, limit, negativePreferences);
        } catch (Exception e) {
            System.err.println("语义检索衣物失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 语义检索衣物（兼容旧接口）
     * @param userId 用户ID
     * @param query 查询文本
     * @param limit 限制数量
     * @return 检索结果
     */
    public List<VectorSearchServiceV3.VectorSearchResult> searchClothes(Long userId, String query, Integer limit) {
        return searchClothes(userId, query, limit, null);
    }

    /**
     * 为衣物生成嵌入
     * @param clothingId 衣物ID
     * @param clothingText 衣物文本描述
     * @return 是否成功
     */
    public boolean generateClothingEmbedding(Long clothingId, String clothingText) {
        try {
            return vectorSearchService.generateAndStoreEmbedding(clothingId, clothingText);
        } catch (Exception e) {
            System.err.println("为衣物生成嵌入失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 生成文本嵌入
     * @param text 文本
     * @return 嵌入向量
     */
    public float[] generateEmbedding(String text) {
        try {
            return embeddingService.generateEmbedding(text);
        } catch (Exception e) {
            System.err.println("生成文本嵌入失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 批量生成文本嵌入
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        try {
            return embeddingService.generateBatchEmbeddings(texts);
        } catch (Exception e) {
            System.err.println("批量生成文本嵌入失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取用户衣橱统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    public java.util.Map<String, Object> getClothingStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();
        try {
            statistics.put("overview", clothingMapper.selectClothingStatistics(userId, null));
            statistics.put("byCategory", clothingMapper.selectClothingCountByCategory(userId));
            statistics.put("byColor", clothingMapper.selectClothingCountByColor(userId));
            statistics.put("byFrequency", clothingMapper.selectClothingCountByFrequency(userId));
            statistics.put("wearTrend", clothingMapper.selectWearTrend(userId, null));
            return statistics;
        } catch (Exception e) {
            System.err.println("鑾峰彇琛ｆ┍缁熻淇℃伅澶辫触: " + e.getMessage());
            return new HashMap<>();
        }
    }

}
