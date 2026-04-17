package com.ecru.outfit.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话信息VO
 */
@Data
public class ConversationVO {

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 对话场景
     */
    private String context;

    /**
     * 是否活跃
     */
    private Boolean isActive;

    /**
     * 消息数量
     */
    private Integer messageCount;

    /**
     * 扩展信息
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 最后一条消息预览
     */
    private String lastMessagePreview;

}
