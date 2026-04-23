package com.ecru.outfit.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * AI聊天响应DTO
 */
@Data
public class ChatResponseDTO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * AI回复内容
     */
    private String response;

    /**
     * 推荐衣物列表
     */
    private List<Map<String, Object>> recommendedClothes;

    /**
     * 天气信息
     */
    private String weatherInfo;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 会话标题
     */
    private String conversationTitle;

    /**
     * 是否是新会话
     */
    private Boolean isNewConversation;

}
