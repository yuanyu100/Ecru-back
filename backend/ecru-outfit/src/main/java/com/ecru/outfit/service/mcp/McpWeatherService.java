package com.ecru.outfit.service.mcp;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class McpWeatherService {

    private static final DateTimeFormatter REPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${mcp.weather.api-key:}")
    private String apiKey;

    public WeatherInfo getWeatherByLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }

        if (!hasValidApiKey()) {
            log.warn("高德天气 API Key 未配置，跳过天气查询");
            return null;
        }

        try {
            String normalizedLocation = location.trim();
            String cacheKey = "weather:" + normalizedLocation;

            if (redisTemplate != null) {
                try {
                    String cachedData = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedData != null) {
                        return WeatherInfo.fromJson(cachedData);
                    }
                } catch (Exception e) {
                    log.warn("读取天气缓存失败: {}", e.getMessage());
                }
            }

            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("city", normalizedLocation);
            params.put("extensions", "base");
            params.put("output", "JSON");

            String url = buildUrl("https://restapi.amap.com/v3/weather/weatherInfo", params);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            WeatherInfo weatherInfo = parseWeatherResponse(response);

            if (weatherInfo != null && redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, weatherInfo.toJson(), 3600, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("写入天气缓存失败: {}", e.getMessage());
                }
            }

            return weatherInfo;
        } catch (Exception e) {
            log.warn("按城市查询天气失败: {}", e.getMessage());
            return null;
        }
    }

    public WeatherInfo getWeatherByCoordinates(double longitude, double latitude) {
        try {
            String location = getLocationByCoordinates(longitude, latitude);
            if (location == null || location.trim().isEmpty()) {
                return null;
            }

            return getWeatherByLocation(location);
        } catch (Exception e) {
            log.warn("按坐标查询天气失败: {}", e.getMessage());
            return null;
        }
    }

    private String getLocationByCoordinates(double longitude, double latitude) {
        if (!hasValidApiKey()) {
            return null;
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("key", apiKey);
            params.put("location", longitude + "," + latitude);
            params.put("extensions", "base");
            params.put("output", "JSON");

            String url = buildUrl("https://restapi.amap.com/v3/geocode/regeo", params);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !"1".equals(String.valueOf(response.get("status")))) {
                return null;
            }

            Map<String, Object> regeocode = asMap(response.get("regeocode"));
            Map<String, Object> addressComponent = asMap(regeocode.get("addressComponent"));
            if (addressComponent.isEmpty()) {
                return null;
            }

            String city = asString(addressComponent.get("city"));
            if (city != null && !city.isBlank()) {
                return city;
            }

            return asString(addressComponent.get("province"));
        } catch (Exception e) {
            log.warn("坐标逆地理解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String buildUrl(String baseUrl, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return url.substring(0, url.length() - 1);
    }

    private WeatherInfo parseWeatherResponse(Map<String, Object> response) {
        if (response == null || !"1".equals(String.valueOf(response.get("status")))) {
            return null;
        }

        List<?> lives = response.get("lives") instanceof List<?> list ? list : List.of();
        if (lives.isEmpty()) {
            return null;
        }

        Map<String, Object> first = asMap(lives.get(0));
        if (first.isEmpty()) {
            return null;
        }

        try {
            WeatherInfo weatherInfo = new WeatherInfo();
            weatherInfo.setLocation(asString(first.get("city")));
            weatherInfo.setTemperature(parseDouble(first.get("temperature")));
            weatherInfo.setWeatherCondition(asString(first.get("weather")));
            weatherInfo.setHumidity(asString(first.get("humidity")));
            weatherInfo.setWindDirection(asString(first.get("winddirection")));
            weatherInfo.setWindPower(asString(first.get("windpower")));
            weatherInfo.setReportTime(asString(first.get("reporttime")));

            LocalDateTime reportDateTime = parseReportDateTime(weatherInfo.getReportTime());
            if (reportDateTime != null) {
                weatherInfo.setSeason(getSeasonByMonth(reportDateTime.getMonthValue()));
                weatherInfo.setTimeOfDay(getTimeOfDay(reportDateTime.getHour()));
            }

            return weatherInfo;
        } catch (Exception e) {
            log.warn("解析天气响应失败: {}", e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseReportDateTime(String reportTime) {
        if (reportTime == null || reportTime.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(reportTime.trim(), REPORT_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("解析天气上报时间失败: {}", reportTime);
            return null;
        }
    }

    private boolean hasValidApiKey() {
        return apiKey != null && !apiKey.isBlank() && !"your-api-key".equals(apiKey);
    }

    private Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getSeasonByMonth(int month) {
        if (month >= 3 && month <= 5) {
            return "春";
        }
        if (month >= 6 && month <= 8) {
            return "夏";
        }
        if (month >= 9 && month <= 11) {
            return "秋";
        }
        return "冬";
    }

    private String getTimeOfDay(int hour) {
        if (hour >= 6 && hour < 9) {
            return "早晨";
        }
        if (hour >= 9 && hour < 12) {
            return "上午";
        }
        if (hour >= 12 && hour < 18) {
            return "下午";
        }
        return "晚上";
    }

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

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public String getWeatherCondition() {
            return weatherCondition;
        }

        public void setWeatherCondition(String weatherCondition) {
            this.weatherCondition = weatherCondition;
        }

        public String getHumidity() {
            return humidity;
        }

        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }

        public String getWindDirection() {
            return windDirection;
        }

        public void setWindDirection(String windDirection) {
            this.windDirection = windDirection;
        }

        public String getWindPower() {
            return windPower;
        }

        public void setWindPower(String windPower) {
            this.windPower = windPower;
        }

        public String getReportTime() {
            return reportTime;
        }

        public void setReportTime(String reportTime) {
            this.reportTime = reportTime;
        }

        public String getSeason() {
            return season;
        }

        public void setSeason(String season) {
            this.season = season;
        }

        public String getTimeOfDay() {
            return timeOfDay;
        }

        public void setTimeOfDay(String timeOfDay) {
            this.timeOfDay = timeOfDay;
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }

        public static WeatherInfo fromJson(String json) {
            return JSON.parseObject(json, WeatherInfo.class);
        }
    }
}
