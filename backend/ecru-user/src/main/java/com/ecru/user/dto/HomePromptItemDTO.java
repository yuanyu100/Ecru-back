package com.ecru.user.dto;

import lombok.Data;

@Data
public class HomePromptItemDTO {

    private String id;

    private String text;

    private String sourceType;

    private String sourceLabel;

    private Boolean enabled;

    private Boolean selected;
}
