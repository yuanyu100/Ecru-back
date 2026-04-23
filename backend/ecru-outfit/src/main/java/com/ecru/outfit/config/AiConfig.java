package com.ecru.outfit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * Qwen3-VL 配置
     */
    private QwenConfig qwen;

    /**
     * LangChain4j 配置
     */
    private LangChain4jConfig langchain4j;

    @Data
    public static class QwenConfig {
        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 基础URL
         */
        private String baseUrl = "https://ark-cn-beijing.bytedance.net/api/v3";

        /**
         * 模型名称
         */
        private String model = "qwen3-vl-32b-instruct";

        /**
         * 超时时间(毫秒)
         */
        private Integer timeout = 300000;

        /**
         * 温度参数
         */
        private Double temperature = 0.7;

        /**
         * 最大令牌数
         */
        private Integer maxTokens = 2048;
    }

    @Data
    public static class LangChain4jConfig {
        /**
         * 嵌入模型配置
         */
        private EmbeddingConfig embedding;

        @Data
        public static class EmbeddingConfig {
            /**
             * 模型名称
             */
            private String model = "BAAI/bge-large-zh-v1.5";

            /**
             * 基础URL
             */
            private String baseUrl = "https://ark-cn-beijing.bytedance.net/api/v3";

            /**
             * 超时时间(毫秒)
             */
            private Integer timeout = 60000;
        }
    }

}
