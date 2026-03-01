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
 * 搭配单品关联表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("outfit_items")
public class OutfitItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 搭配建议记录ID
     */
    private Long outfitAdviceId;

    /**
     * 衣物ID(来自用户衣橱,为空表示推荐新品)
     */
    private Long clothingId;

    /**
     * 单品名称
     */
    private String itemName;

    /**
     * 单品类别
     */
    private String itemCategory;

    /**
     * 单品颜色
     */
    private String itemColor;

    /**
     * 单品图片URL
     */
    private String itemImageUrl;

    /**
     * 是否为系统推荐新品
     */
    private Boolean isRecommended;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
