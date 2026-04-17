package com.ecru.outfit.dto.request;

import lombok.Data;

import java.util.Map;

/**
 * AI聊天请求DTO
 */
@Data
public class ChatRequestDTO {

    /**
     * 用户消息
     */
    private String message;

    /**
     * 会话ID(可选,不传则创建新会话)
     */
    private String sessionId;

    /**
     * 地理位置(可选)
     */
    private String location;

    /**
     * 场合(可选)
     */
    private String occasion;

    /**
     * 对话场景: outfit-搭配建议, general-一般咨询, style-风格建议
     */
    private String context;

    /**
     * 扩展信息
     */
    private Map<String, Object> metadata;

}
