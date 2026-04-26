package com.ecru.outfit.controller;

import com.ecru.common.result.Result;
import com.ecru.outfit.service.mcp.McpWeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/weather")
@Tag(name = "天气", description = "首页天气与对话天气上下文")
public class WeatherController {

    @Autowired
    private McpWeatherService weatherService;

    @GetMapping("/current")
    @Operation(summary = "查询当前天气", description = "支持按城市或经纬度查询当前天气")
    public Result<Map<String, Object>> getCurrentWeather(
            @Parameter(description = "城市名，如上海") @RequestParam(required = false) String location,
            @Parameter(description = "经度") @RequestParam(required = false) Double longitude,
            @Parameter(description = "纬度") @RequestParam(required = false) Double latitude) {

        McpWeatherService.WeatherInfo weatherInfo;
        if (longitude != null && latitude != null) {
            weatherInfo = weatherService.getWeatherByCoordinates(longitude, latitude);
        } else {
            weatherInfo = weatherService.getWeatherByLocation(location);
        }

        if (weatherInfo == null) {
            return Result.error(500, "天气查询失败");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("location", weatherInfo.getLocation());
        payload.put("temperature", weatherInfo.getTemperature());
        payload.put("weatherCondition", weatherInfo.getWeatherCondition());
        payload.put("humidity", weatherInfo.getHumidity());
        payload.put("windDirection", weatherInfo.getWindDirection());
        payload.put("windPower", weatherInfo.getWindPower());
        payload.put("reportTime", weatherInfo.getReportTime());
        payload.put("season", weatherInfo.getSeason());
        payload.put("timeOfDay", weatherInfo.getTimeOfDay());
        payload.put("summary", buildSummary(weatherInfo));
        return Result.success(payload);
    }

    private String buildSummary(McpWeatherService.WeatherInfo weatherInfo) {
        String location = weatherInfo.getLocation() == null ? "" : weatherInfo.getLocation();
        String temperature = weatherInfo.getTemperature() == null ? "" : Math.round(weatherInfo.getTemperature()) + "°C";
        String condition = weatherInfo.getWeatherCondition() == null ? "" : weatherInfo.getWeatherCondition();
        return String.join(" · ", java.util.List.of(location, temperature, condition).stream().filter(item -> item != null && !item.isBlank()).toList());
    }
}
