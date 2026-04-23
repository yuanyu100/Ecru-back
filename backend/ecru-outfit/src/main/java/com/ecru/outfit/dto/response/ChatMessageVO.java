package com.ecru.outfit.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息VO
 */
@Data
public class ChatMessageVO {

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 角色: user-用户, assistant-AI助手, system-系统
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 推荐衣物信息
     */
    private List<Map<String, Object>> recommendations;

    /**
     * 上下文快照
     */
    private Map<String, Object> contextSnapshot;

    /**
     * 扩展信息
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
