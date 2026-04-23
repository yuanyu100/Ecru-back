package com.ecru.outfit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * pgvector 向量数据库配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "pgvector")
public class PgVectorConfig {

    /**
     * 向量维度
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

    /**
     * 索引类型
     */
    private String indexType = "ivfflat";

    /**
     * 索引列表大小
     */
    private Integer lists = 100;

}