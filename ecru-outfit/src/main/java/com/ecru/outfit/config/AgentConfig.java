package com.ecru.outfit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    /**
     * 工作流配置
     */
    private WorkflowConfig workflow;

    /**
     * 提示模板配置
     */
    private PromptConfig prompt;

    @Data
    public static class WorkflowConfig {
        /**
         * 超时时间(毫秒)
         */
        private Integer timeout = 600000;

        /**
         * 最大步骤数
         */
        private Integer maxSteps = 50;

        /**
         * 是否启用工具
         */
        private Boolean enableTools = true;

        /**
         * 工具调用超时时间(毫秒)
         */
        private Integer toolTimeout = 30000;

        /**
         * 最大工具调用次数
         */
        private Integer maxToolCalls = 5;
    }

    @Data
    public static class PromptConfig {
        /**
         * 搭配建议提示模板
         */
        private String outfitAdvice = "你是一位专业的时尚搭配顾问，根据用户提供的信息，生成个性化的搭配建议。请考虑以下因素：\n1. 天气和季节因素\n2. 用户的风格偏好\n3. 场合需求\n4. 现有衣物的搭配可能性\n5. 时尚趋势和专业建议\n\n请提供详细的搭配方案，包括衣物选择、颜色搭配、配饰建议等，并给出专业的时尚分析。";

        /**
         * 图像分析提示模板
         */
        private String imageAnalysis = "你是一位专业的时尚分析师，请对提供的穿搭照片进行详细分析，包括：\n1. 颜色搭配分析\n2. 款式风格分析\n3. 场合适用性\n4. 气质匹配度\n5. 改进建议\n\n请提供专业、详细的分析结果。";

        /**
         * 面料分析提示模板
         */
        private String fabricAnalysis = "你是一位专业的面料分析师，请对提供的面料照片进行详细分析，包括：\n1. 面料材质识别\n2. 质量评估\n3. 护理建议\n4. 适用季节\n5. 搭配建议\n\n请提供专业、详细的分析结果。";
    }

}
