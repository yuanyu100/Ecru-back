package com.ecru.common.service.vector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 衣物向量服务
 */
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
        System.out.println("开始为衣物ID: " + clothingId + " 生成向量");
        executorService.submit(() -> {
            try {
                System.out.println("线程开始执行向量化处理，衣物ID: " + clothingId);
                // 构建衣物描述文本
                String clothingDescription = buildClothingDescription(name, category, primaryColor, styleTags, occasionTags, seasonTags);
                System.out.println("衣物描述: " + clothingDescription);
                
                // 生成向量
                System.out.println("开始生成向量");
                float[] embedding = embeddingService.generateEmbedding(clothingDescription);
                System.out.println("向量生成成功，长度: " + embedding.length);
                
                // 构建元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", name);
                metadata.put("category", category);
                metadata.put("primaryColor", primaryColor);
                metadata.put("styleTags", styleTags);
                metadata.put("occasionTags", occasionTags);
                metadata.put("seasonTags", seasonTags);
                
                // 存储向量
                System.out.println("开始存储向量");
                boolean success = pgVectorService.storeVector(
                        clothingId,
                        userId,
                        embedding,
                        metadata
                );
                
                if (success) {
                    System.out.println("成功为衣物ID: " + clothingId + " 生成并存储向量");
                } else {
                    System.err.println("存储向量失败，衣物ID: " + clothingId);
                }
            } catch (Exception e) {
                System.err.println("为衣物生成向量失败，衣物ID: " + clothingId + ", 错误: " + e.getMessage());
                e.printStackTrace();
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
                // 先删除旧向量
                pgVectorService.deleteVector(clothingId, userId);
                // 生成并存储新向量
                generateAndStoreVector(clothingId, userId, name, category, primaryColor, styleTags, occasionTags, seasonTags);
            } catch (Exception e) {
                System.err.println("更新衣物向量失败，衣物ID: " + clothingId + ", 错误: " + e.getMessage());
                e.printStackTrace();
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
                System.err.println("删除衣物向量失败，衣物ID: " + clothingId + ", 错误: " + e.getMessage());
                e.printStackTrace();
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
        System.out.println("开始批量生成向量任务");
        executorService.submit(() -> {
            try {
                // 初始化表结构
                pgVectorService.initTable();
                
                // 这里需要注入ClothingMapper来获取所有衣物
                // 由于我们在common模块中，不应该依赖clothing模块
                // 所以这里只做初始化，具体的批量处理逻辑需要在业务模块中实现
                System.out.println("批量生成向量任务已提交，正在异步处理中");
            } catch (Exception e) {
                System.err.println("批量生成向量失败: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
