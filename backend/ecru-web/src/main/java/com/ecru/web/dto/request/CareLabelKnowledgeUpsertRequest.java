package com.ecru.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CareLabelKnowledgeUpsertRequest {

    @NotBlank(message = "洗护标编码不能为空")
    private String symbolCode;

    @NotBlank(message = "洗护标名称不能为空")
    private String symbolName;

    @NotBlank(message = "洗护分类不能为空")
    private String category;

    @NotBlank(message = "洗护说明不能为空")
    private String instruction;

    private String explanation;

    private String doText;

    private String dontText;

    private String keywords;

    private String source;

    private Boolean active;
}
