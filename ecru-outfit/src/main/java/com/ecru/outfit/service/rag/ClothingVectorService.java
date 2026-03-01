package com.ecru.outfit.service.rag;

import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.mapper.ClothingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 衣物向量服务
 */
@Service
public class ClothingVectorService {

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private PgVectorService pgVectorService;

    @Resource
    private ClothingMapper clothingMapper;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 为单个衣物生成并存储向量
     * @param clothing 衣物实体
     */
    @Transactional
    public void generateAndStoreVector(Clothing clothing) {
        try {
            // 检查是否已有向量
            if (hasVector(clothing.getId(), clothing.getUserId())) {
                System.out.println("衣物ID: " + clothing.getId() + " 已有向量，跳过生成");
                return;
            }
            
            // 构建衣物描述文本
            String clothingDescription = buildClothingDescription(clothing);
            
            // 生成向量
            float[] embedding = embeddingService.generateEmbedding(clothingDescription);
            
            // 构建元数据
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("name", clothing.getName());
            metadata.put("category", clothing.getCategory());
            metadata.put("primaryColor", clothing.getPrimaryColor());
            metadata.put("styleTags", clothing.getStyleTags());
            metadata.put("occasionTags", clothing.getOccasionTags());
            metadata.put("seasonTags", clothing.getSeasonTags());
            
            // 存储向量
            boolean success = pgVectorService.storeVector(
                    clothing.getId(),
                    clothing.getUserId(),
                    embedding,
                    metadata
            );
            
            if (success) {
                System.out.println("成功为衣物ID: " + clothing.getId() + " 生成并存储向量");
            } else {
                System.err.println("存储向量失败，衣物ID: " + clothing.getId());
            }
        } catch (Exception e) {
            System.err.println("为衣物生成向量失败，衣物ID: " + clothing.getId() + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 批量为衣物生成并存储向量
     */
    public void batchGenerateAndStoreVectors() {
        // 获取所有衣物
        List<Clothing> clothingList = clothingMapper.selectClothingList(null, null, null, null, null, null, null, null, null, null, null, null);
        
        System.out.println("开始为 " + clothingList.size() + " 件衣物生成向量");
        
        for (Clothing clothing : clothingList) {
            executorService.submit(() -> {
                generateAndStoreVector(clothing);
            });
        }
        
        System.out.println("批量任务已提交，正在异步处理中");
    }

    /**
     * 检查衣物是否已有向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     * @return 是否已有向量
     */
    public boolean hasVector(Long clothingId, Long userId) {
        try {
            // 直接查询指定衣物ID和用户ID的向量是否存在
            return pgVectorService.vectorExists(clothingId, userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 构建衣物描述文本
     * @param clothing 衣物实体
     * @return 衣物描述文本
     */
    private String buildClothingDescription(Clothing clothing) {
        StringBuilder description = new StringBuilder();
        
        description.append("衣物名称: ").append(clothing.getName()).append("。");
        description.append("类别: ").append(clothing.getCategory());
        if (clothing.getSubCategory() != null) {
            description.append("，子类别: ").append(clothing.getSubCategory());
        }
        description.append("。");
        
        if (clothing.getPrimaryColor() != null) {
            description.append("主颜色: ").append(clothing.getPrimaryColor());
            if (clothing.getSecondaryColor() != null) {
                description.append("，次颜色: ").append(clothing.getSecondaryColor());
            }
            description.append("。");
        }
        
        if (clothing.getMaterial() != null) {
            description.append("材质: ").append(clothing.getMaterial());
            if (clothing.getMaterialDetails() != null) {
                description.append("，材质详情: ").append(clothing.getMaterialDetails());
            }
            description.append("。");
        }
        
        if (clothing.getPattern() != null) {
            description.append("图案: ").append(clothing.getPattern()).append("。");
        }
        
        if (clothing.getFit() != null) {
            description.append("版型: ").append(clothing.getFit()).append("。");
        }
        
        if (clothing.getStyleTags() != null) {
            description.append("风格标签: ").append(clothing.getStyleTags()).append("。");
        }
        
        if (clothing.getOccasionTags() != null) {
            description.append("场合标签: ").append(clothing.getOccasionTags()).append("。");
        }
        
        if (clothing.getSeasonTags() != null) {
            description.append("季节标签: ").append(clothing.getSeasonTags()).append("。");
        }
        
        return description.toString();
    }

    /**
     * 更新衣物向量
     * @param clothing 衣物实体
     */
    @Transactional
    public void updateClothingVector(Clothing clothing) {
        try {
            // 先删除旧向量
            pgVectorService.deleteVector(clothing.getId(), clothing.getUserId());
            // 生成并存储新向量
            generateAndStoreVector(clothing);
        } catch (Exception e) {
            System.err.println("更新衣物向量失败，衣物ID: " + clothing.getId() + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 删除衣物向量
     * @param clothingId 衣物ID
     * @param userId 用户ID
     */
    public void deleteClothingVector(Long clothingId, Long userId) {
        try {
            pgVectorService.deleteVector(clothingId, userId);
        } catch (Exception e) {
            System.err.println("删除衣物向量失败，衣物ID: " + clothingId + ", 错误: " + e.getMessage());
        }
    }
}
