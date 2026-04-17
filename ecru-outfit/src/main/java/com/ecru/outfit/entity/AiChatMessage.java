package com.ecru.outfit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI对话消息
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_chat_messages")
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long conversationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色: user-用户, assistant-AI助手, system-系统
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型: text-文本, image-图片, recommendation-推荐, error-错误
     */
    private String messageType;

    /**
     * 推荐衣物信息 [{"clothingId": 1, "name": "", "reason": ""}]
     */
    private String recommendations;

    /**
     * 上下文快照: 天气、场合等
     */
    private String contextSnapshot;

    /**
     * 扩展信息: 意图分析、token消耗等
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
