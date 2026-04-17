package com.ecru.web.controller;

import com.ecru.common.service.ai.AiApiMonitorService;
import com.ecru.common.vo.ai.AiApiCallRecordVO;
import com.ecru.common.vo.ai.AiApiDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI API监控控制器
 * 提供AI API监控数据的查询接口
 */
@RestController
@RequestMapping("/ai-monitor")
@Tag(name = "AI API监控", description = "AI API监控数据查询")
public class AiApiMonitorController {

    @Autowired
    private AiApiMonitorService monitorService;

    /**
     * 获取仪表盘数据
     * 包含今日统计、趋势、场景/模型分布、最近调用等
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘数据", description = "获取AI API监控仪表盘数据，包含今日统计、趋势、分布等")
    public Map<String, Object> getDashboard() {
        Map<String, Object> response = new HashMap<>();
        try {
            AiApiDashboardVO dashboard = monitorService.getDashboardData();
            response.put("success", true);
            response.put("data", dashboard);
            response.put("message", "获取仪表盘数据成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取仪表盘数据失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取最近调用记录
     */
    @GetMapping("/recent-calls")
    @Operation(summary = "获取最近调用记录", description = "获取最近的AI API调用记录")
    public Map<String, Object> getRecentCalls(
            @Parameter(description = "返回数量限制，默认20")
            @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<AiApiCallRecordVO> records = monitorService.getRecentCalls(limit);
            response.put("success", true);
            response.put("data", records);
            response.put("message", "获取最近调用记录成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取最近调用记录失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 获取实时统计
     */
    @GetMapping("/realtime-stats")
    @Operation(summary = "获取实时统计", description = "获取AI API实时统计数据")
    public Map<String, Object> getRealtimeStats(
            @Parameter(description = "调用场景")
            @RequestParam(required = false) String scene,
            @Parameter(description = "模型名称")
            @RequestParam(required = false) String model) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = monitorService.getRealtimeStats(scene, model);
            response.put("success", true);
            response.put("data", stats);
            response.put("message", "获取实时统计成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取实时统计失败: " + e.getMessage());
        }
        return response;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "AI监控服务健康检查")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "UP");
        response.put("message", "AI监控服务运行正常");
        return response;
    }
}
