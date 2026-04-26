package com.ecru.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HomePromptSettingsDTO {

    private List<HomePromptItemDTO> items = new ArrayList<>();

    private String selectedPromptId;

    private Boolean homeFlowDefaultVisible;

    private Integer homePromptStayMs;

    private Integer homePromptFadeMs;
}
