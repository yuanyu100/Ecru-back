package com.ecru.outfit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_style_archives")
public class UserStyleArchive {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String temperamentType;

    private String temperamentScores;

    private Integer heightCm;

    private Integer weightKg;

    private String bodyType;

    private String skinTone;

    private String preferredStyles;

    private String avoidedStyles;

    private String preferredColors;

    private String avoidedColors;

    private String occupation;

    private String lifestyleTags;

    private Boolean isTestCompleted;

    private LocalDateTime testCompletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
