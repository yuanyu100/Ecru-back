package com.ecru.user.dto.response;

import lombok.Data;

@Data
public class StyleLearningProgressVO {
    private Integer progressPercent;
    private Integer coveredTagCount;
    private Integer totalTagCount;
}
