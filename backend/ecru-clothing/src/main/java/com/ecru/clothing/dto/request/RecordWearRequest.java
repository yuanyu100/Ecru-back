package com.ecru.clothing.dto.request;

import lombok.Data;

@Data
public class RecordWearRequest {

    private String wornAt;
    private Long outfitId;
    private String weatherCondition;
    private Double temperature;
    private String notes;

}
