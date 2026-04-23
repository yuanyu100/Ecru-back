package com.ecru.outfit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MCP工具配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "mcp")
public class McpConfig {

    /**
     * 天气服务配置
     */
    private WeatherConfig weather;

    @Data
    public static class WeatherConfig {
        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 基础URL
         */
        private String baseUrl = "https://restapi.amap.com/v3";

        /**
         * 超时时间(毫秒)
         */
        private Integer timeout = 10000;

        /**
         * 天气查询URL
         */
        private String weatherUrl = "/weather/weatherInfo";

        /**
         * 地理编码URL
         */
        private String geoUrl = "/geocode/regeo";

        /**
         * 缓存时间(秒)
         */
        private Integer cacheTime = 3600;
    }

}
