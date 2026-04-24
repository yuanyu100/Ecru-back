package com.ecru.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuideKnowledgeUpsertRequest {

    @NotBlank(message = "指南标题不能为空")
    private String title;

    private String subtitle;

    private String guideType;

    private String summary;

    private String content;

    private String author;

    private String publishDate;

    private String tags;

    private String coverImageUrl;

    private String coverImageCaption;

    private String keywords;

    private String source;

    private Boolean active;
}
