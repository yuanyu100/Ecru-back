package com.ecru.outfit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搭配建议记录
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("outfit_advice_records")
public class OutfitAdviceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 输入类型: 1-上传照片, 2-文字描述, 3-智能推荐
     */
    private Integer inputType;

    /**
     * 用户上传的穿搭照片URL
     */
    private String inputImageUrl;

    /**
     * 用户文字描述
     */
    private String inputDescription;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 温度(摄氏度)
     */
    private BigDecimal temperature;

    /**
     * 天气状况: 晴/多云/雨/雪等
     */
    private String weatherCondition;

    /**
     * 季节: 春/夏/秋/冬
     */
    private String season;

    /**
     * 时段: 早晨/上午/下午/晚上
     */
    private String timeOfDay;

    /**
     * 识别到的单品列表 [{'item': '上衣', 'color': '白色', 'category': '上装'}]
     */
    private String detectedItems;

    /**
     * 识别到的风格
     */
    private String detectedStyle;

    /**
     * 色彩分析结果
     */
    private String colorAnalysis;

    /**
     * 搭配方案名称
     */
    private String outfitName;

    /**
     * 搭配方案描述
     */
    private String outfitDescription;

    /**
     * 搭配思路说明
     */
    private String reasoning;

    /**
     * 时尚建议
     */
    private String fashionSuggestions;

    /**
     * 推荐购买商品 [{'name': '', 'reason': '', 'link': ''}]
     */
    private String purchaseRecommendations;

    /**
     * 适用场合
     */
    private String occasion;

    /**
     * 场合适配度评分 0.00-1.00
     */
    private BigDecimal suitabilityScore;

    /**
     * 是否收藏
     */
    private Boolean isFavorite;

    /**
     * 是否删除
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
