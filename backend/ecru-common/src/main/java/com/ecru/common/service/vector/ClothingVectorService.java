package com.ecru.common.service.vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClothingVectorService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PgVectorService pgVectorService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 为衣物生成并存储向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @param name 衣物名称
     * @param category 衣物类别
     * @param primaryColor 主颜色
     * @param styleTags 风格标签
     * @param occasionTags 场合标签
     * @param seasonTags 季节标签
     */
    public void generateAndStoreVector(Long clothingId, Long userId, String name, String category, String primaryColor, String styleTags, String occasionTags, String seasonTags) {
        executorService.submit(() -> {
            try {
                String clothingDescription = buildClothingDescription(name, category, primaryColor, styleTags, occasionTags, seasonTags);
                float[] embedding = embeddingService.generateEmbedding(clothingDescription);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", name);
                metadata.put("category", category);
                metadata.put("primaryColor", primaryColor);
                metadata.put("styleTags", styleTags);
                metadata.put("occasionTags", occasionTags);
                metadata.put("seasonTags", seasonTags);

                boolean success = pgVectorService.storeVector(clothingId, userId, embedding, metadata);
                if (!success) {
                    log.warn("存储向量失败，clothingId={}", clothingId);
                }
            } catch (Exception e) {
                log.error("为衣物生成向量失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            }
        });
    }

    /**
     * 更新衣物向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @param name 衣物名称
     * @param category 衣物类别
     * @param primaryColor 主颜色
     * @param styleTags 风格标签
     * @param occasionTags 场合标签
     * @param seasonTags 季节标签
     */
    public void updateClothingVector(Long clothingId, Long userId, String name, String category, String primaryColor, String styleTags, String occasionTags, String seasonTags) {
        executorService.submit(() -> {
            try {
                pgVectorService.deleteVector(clothingId, userId);
                generateAndStoreVector(clothingId, userId, name, category, primaryColor, styleTags, occasionTags, seasonTags);
            } catch (Exception e) {
                log.error("更新衣物向量失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            }
        });
    }

    /**
     * 删除衣物向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     */
    public void deleteClothingVector(Long clothingId, Long userId) {
        executorService.submit(() -> {
            try {
                pgVectorService.deleteVector(clothingId, userId);
            } catch (Exception e) {
                log.error("删除衣物向量失败，clothingId={}: {}", clothingId, e.getMessage(), e);
            }
        });
    }

    /**
     * 构建衣物描述文本
     * @param name 衣物名称
     * @param category 衣物类别
     * @param primaryColor 主颜色
     * @param styleTags 风格标签
     * @param occasionTags 场合标签
     * @param seasonTags 季节标签
     * @return 衣物描述文本
     */
    private String buildClothingDescription(String name, String category, String primaryColor, String styleTags, String occasionTags, String seasonTags) {
        StringBuilder description = new StringBuilder();
        
        if (name != null && !name.isEmpty()) {
            description.append(name).append(", ");
        }
        if (category != null && !category.isEmpty()) {
            description.append(category).append(", ");
        }
        if (primaryColor != null && !primaryColor.isEmpty()) {
            description.append(primaryColor).append(", ");
        }
        if (styleTags != null && !styleTags.isEmpty()) {
            description.append("风格: ").append(styleTags).append(", ");
        }
        if (occasionTags != null && !occasionTags.isEmpty()) {
            description.append("场合: ").append(occasionTags).append(", ");
        }
        if (seasonTags != null && !seasonTags.isEmpty()) {
            description.append("季节: ").append(seasonTags);
        }
        
        // 移除末尾的逗号和空格
        String result = description.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }
        
        return result;
    }

    /**
     * 批量为衣物生成并存储向量
     */
    public void batchGenerateAndStoreVectors() {
        executorService.submit(() -> {
            try {
                pgVectorService.initTable();
                log.info("批量生成向量任务已提交，正在异步处理中");
            } catch (Exception e) {
                log.error("批量生成向量失败: {}", e.getMessage(), e);
            }
        });
    }
}
