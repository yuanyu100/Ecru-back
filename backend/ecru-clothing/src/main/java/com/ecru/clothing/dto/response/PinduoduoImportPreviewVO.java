package com.ecru.clothing.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PinduoduoImportPreviewVO {

    private String mode;
    private Integer totalDetected;
    private Integer matchedCount;
    private List<PinduoduoImportItemVO> items;
}
