package com.ecru.outfit.dto.request;

import lombok.Data;

import java.util.Map;

/**
 * 创建会话请求DTO
 */
@Data
public class CreateConversationRequestDTO {

    /**
     * 会话标题(可选)
     */
    private String title;

    /**
     * 对话场景: outfit-搭配建议, general-一般咨询, style-风格建议
     */
    private String context;

    /**
     * 地理位置(可选)
     */
    private String location;

    /**
     * 场合(可选)
     */
    private String occasion;

    /**
     * 扩展信息
     */
    private Map<String, Object> metadata;

}
