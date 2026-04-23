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
 * 用户风格档案表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_style_profiles")
public class UserStyleProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 档案ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 气质类型: 优雅型/自然型/浪漫型/古典型/前卫型/戏剧型
     */
    private String temperamentType;

    /**
     * 各气质类型得分 {"优雅型": 85, "自然型": 60}
     */
    private String temperamentScores;

    /**
     * 身高(cm)
     */
    private Double heightCm;

    /**
     * 体重(kg)
     */
    private Double weightKg;

    /**
     * 体型: 沙漏型/梨型/苹果型/矩形/倒三角
     */
    private String bodyType;

    /**
     * 肤色: 冷白皮/暖黄皮/小麦色/深色
     */
    private String skinTone;

    /**
     * 偏好风格标签列表
     */
    private String preferredStyles;

    /**
     * 回避风格标签列表
     */
    private String avoidedStyles;

    /**
     * 偏好颜色列表
     */
    private String preferredColors;

    /**
     * 回避颜色列表
     */
    private String avoidedColors;

    /**
     * 职业
     */
    private String occupation;

    /**
     * 生活方式标签 ["职场精英", "运动爱好者"]
     */
    private String lifestyleTags;

    /**
     * 是否完成测试
     */
    private Boolean isTestCompleted;

    /**
     * 测试完成时间
     */
    private LocalDateTime testCompletedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
