package com.ecru.outfit.service.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * MCP天气工具服务
 */
@Slf4j
@Service
public class McpWeatherService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 根据地理位置获取天气信息
     * @param location 地理位置
     * @return 天气信息
     */
    public WeatherInfo getWeatherByLocation(String location) {
        try {
            // 先尝试从缓存获取
            String cacheKey = "weather:" + location;
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return WeatherInfo.fromJson(cachedData);
            }

            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("key", "test-api-key");
            params.put("city", location);
            params.put("extensions", "base");
            params.put("output", "JSON");

            // 调用高德天气API
            String url = buildUrl("https://restapi.amap.com/v3/weather/weatherInfo", params);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // 解析响应
            WeatherInfo weatherInfo = parseWeatherResponse(response);

            // 缓存结果
            if (weatherInfo != null) {
                redisTemplate.opsForValue().set(
                        cacheKey, 
                        weatherInfo.toJson(), 
                        3600, 
                        TimeUnit.SECONDS
                );
            }

            return weatherInfo;
        } catch (Exception e) {
            System.err.println("获取天气信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据经纬度获取天气信息
     * @param longitude 经度
     * @param latitude 纬度
     * @return 天气信息
     */
    public WeatherInfo getWeatherByCoordinates(double longitude, double latitude) {
        try {
            // 先根据经纬度获取地理位置
            String location = getLocationByCoordinates(longitude, latitude);
            if (location == null) {
                return null;
            }

            // 再根据地理位置获取天气
            return getWeatherByLocation(location);
        } catch (Exception e) {
            log.error("根据经纬度获取天气信息失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 根据经纬度获取地理位置
     * @param longitude 经度
     * @param latitude 纬度
     * @return 地理位置
     */
    private String getLocationByCoordinates(double longitude, double latitude) {
        try {
            // 构建请求参数
            Map<String, String> params = new HashMap<>();
            params.put("key", "test-api-key");
            params.put("location", longitude + "," + latitude);
            params.put("extensions", "base");
            params.put("output", "JSON");

            // 调用高德地理编码API
            String url = buildUrl("https://restapi.amap.com/v3/geocode/regeo", params);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // 解析响应
            if (response != null && "1".equals(response.get("status"))) {
                Map<String, Object> regeocode = (Map<String, Object>) response.get("regeocode");
                if (regeocode != null) {
                    Map<String, Object> addressComponent = (Map<String, Object>) regeocode.get("addressComponent");
                    if (addressComponent != null) {
                        return (String) addressComponent.get("city");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("根据经纬度获取地理位置失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建完整的URL
     * @param baseUrl 基础URL
     * @param params 参数
     * @return 完整URL
     */
    private String buildUrl(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return url.toString().endsWith("&") ? url.toString().substring(0, url.length() - 1) : url.toString();
    }

    /**
     * 解析天气响应
     * @param response 响应
     * @return 天气信息
     */
    private WeatherInfo parseWeatherResponse(Map<String, Object> response) {
        if (response == null || !"1".equals(response.get("status"))) {
            return null;
        }

        try {
            Map<String, Object> lives = (Map<String, Object>) ((java.util.List<?>) response.get("lives")).get(0);
            WeatherInfo weatherInfo = new WeatherInfo();
            weatherInfo.setLocation((String) lives.get("city"));
            weatherInfo.setTemperature(Double.parseDouble((String) lives.get("temperature")));
            weatherInfo.setWeatherCondition((String) lives.get("weather"));
            weatherInfo.setHumidity((String) lives.get("humidity"));
            weatherInfo.setWindDirection((String) lives.get("winddirection"));
            weatherInfo.setWindPower((String) lives.get("windpower"));
            weatherInfo.setReportTime((String) lives.get("reporttime"));

            // 根据月份判断季节
            int month = Integer.parseInt(weatherInfo.getReportTime().substring(4, 6));
            weatherInfo.setSeason(getSeasonByMonth(month));

            // 根据时间判断时段
            int hour = Integer.parseInt(weatherInfo.getReportTime().substring(8, 10));
            weatherInfo.setTimeOfDay(getTimeOfDay(hour));

            return weatherInfo;
        } catch (Exception e) {
            System.err.println("解析天气响应失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据月份获取季节
     * @param month 月份
     * @return 季节
     */
    private String getSeasonByMonth(int month) {
        if (month >= 3 && month <= 5) {
            return "春";
        } else if (month >= 6 && month <= 8) {
            return "夏";
        } else if (month >= 9 && month <= 11) {
            return "秋";
        } else {
            return "冬";
        }
    }

    /**
     * 根据小时获取时段
     * @param hour 小时
     * @return 时段
     */
    private String getTimeOfDay(int hour) {
        if (hour >= 6 && hour < 9) {
            return "早晨";
        } else if (hour >= 9 && hour < 12) {
            return "上午";
        } else if (hour >= 12 && hour < 18) {
            return "下午";
        } else {
            return "晚上";
        }
    }

    /**
     * 天气信息类
     */
    public static class WeatherInfo {
        private String location;
        private Double temperature;
        private String weatherCondition;
        private String humidity;
        private String windDirection;
        private String windPower;
        private String reportTime;
        private String season;
        private String timeOfDay;

        // Getters and Setters
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public String getWeatherCondition() { return weatherCondition; }
        public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }
        public String getHumidity() { return humidity; }
        public void setHumidity(String humidity) { this.humidity = humidity; }
        public String getWindDirection() { return windDirection; }
        public void setWindDirection(String windDirection) { this.windDirection = windDirection; }
        public String getWindPower() { return windPower; }
        public void setWindPower(String windPower) { this.windPower = windPower; }
        public String getReportTime() { return reportTime; }
        public void setReportTime(String reportTime) { this.reportTime = reportTime; }
        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }
        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }

        // JSON 转换方法
        public String toJson() {
            return com.alibaba.fastjson2.JSON.toJSONString(this);
        }

        public static WeatherInfo fromJson(String json) {
            return com.alibaba.fastjson2.JSON.parseObject(json, WeatherInfo.class);
        }
    }

}
