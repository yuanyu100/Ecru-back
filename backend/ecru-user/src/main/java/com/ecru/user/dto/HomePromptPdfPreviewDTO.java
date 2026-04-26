package com.ecru.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HomePromptPdfPreviewDTO {

    private String sourceLabel;

    private List<HomePromptItemDTO> items = new ArrayList<>();
}
