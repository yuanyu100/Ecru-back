package com.ecru.outfit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 搭配反馈表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("outfit_feedback")
public class OutfitFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 反馈ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 搭配建议记录ID
     */
    private Long outfitAdviceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 整体评分 1-5星
     */
    private Integer overallRating;

    /**
     * 风格匹配度评分 1-5星
     */
    private Integer styleRating;

    /**
     * 实用性评分 1-5星
     */
    private Integer practicalityRating;

    /**
     * 天气适配度评分 1-5星
     */
    private Integer weatherRating;

    /**
     * 是否实际穿着
     */
    private Boolean isWorn;

    /**
     * 穿着日期
     */
    private LocalDate wornAt;

    /**
     * 文字反馈
     */
    private String feedbackText;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
