package com.ecru.outfit.dto.response;

import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.ecru.outfit.entity.OutfitFeedback;
import com.ecru.outfit.entity.OutfitItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutfitAdviceDetailDTO {

    private OutfitAdviceRecord record;

    private List<OutfitItem> items = new ArrayList<>();

    private OutfitFeedback feedback;
}
