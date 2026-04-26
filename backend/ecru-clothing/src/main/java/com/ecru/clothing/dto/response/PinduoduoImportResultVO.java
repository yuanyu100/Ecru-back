package com.ecru.clothing.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PinduoduoImportResultVO {

    private Integer created;
    private Integer skipped;
    private Integer failed;
    private List<ClothingDetailVO> createdItems;
    private List<String> messages;
}
