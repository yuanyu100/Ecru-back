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
 * AI对话会话
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_conversations")
public class AiConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话唯一标识
     */
    private String sessionId;

    /**
     * 会话标题(自动生成或用户设置)
     */
    private String title;

    /**
     * 对话场景: outfit-搭配建议, general-一般咨询, style-风格建议
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
     * 扩展信息: 地理位置、场合、天气等
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
