package com.ecru.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FabricKnowledgeUpsertRequest {

    @NotBlank(message = "面料名称不能为空")
    private String name;

    private String alias;

    private String fabricType;

    private Integer warmthScore;

    private Integer breathabilityScore;

    private Integer comfortScore;

    private Integer durabilityScore;

    private String summary;

    private String properties;

    private String careGuide;

    private String suitableSeasons;

    private String suitableOccasions;

    private String keywords;

    private String source;

    private Boolean active;
}
