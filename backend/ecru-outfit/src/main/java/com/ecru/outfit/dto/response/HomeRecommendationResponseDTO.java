package com.ecru.outfit.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class HomeRecommendationResponseDTO {

    private LocalDateTime generatedAt;

    private String emptyReason;

    private List<HomeRecommendationLookDTO> looks = new ArrayList<>();
}
