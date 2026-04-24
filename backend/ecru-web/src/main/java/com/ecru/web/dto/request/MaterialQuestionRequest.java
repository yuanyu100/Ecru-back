package com.ecru.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaterialQuestionRequest {

    @NotBlank(message = "材质不能为空")
    private String material;

    @NotBlank(message = "问题不能为空")
    private String question;
}
