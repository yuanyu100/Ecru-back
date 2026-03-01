package com.ecru.outfit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG检索配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    /**
     * 向量搜索配置
     */
    private VectorConfig vector;

    /**
     * 缓存配置
     */
    private CacheConfig cache;

    @Data
    public static class VectorConfig {
        /**
         * 嵌入维度
         */
        private Integer dimension = 1024;

        /**
         * 相似度阈值
         */
        private Double similarityThreshold = 0.7;

        /**
         * 最大检索结果数
         */
        private Integer maxResults = 10;
    }

    @Data
    public static class CacheConfig {
        /**
         * 是否启用缓存
         */
        private Boolean enabled = true;

        /**
         * 缓存时间(秒)
         */
        private Integer ttl = 3600;
    }

}
