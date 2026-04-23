package com.ecru.clothing.converter;

import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.UpdateClothingRequest;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.entity.ClothingWearLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服装转换工具类
 */
@Component
public class ClothingConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将Clothing实体转换为ClothingDetailVO
     */
    public ClothingDetailVO toDetailVO(Clothing clothing) {
        if (clothing == null) {
            return null;
        }
        
        ClothingDetailVO vo = new ClothingDetailVO();
        vo.setId(clothing.getId());
        vo.setName(clothing.getName());
        vo.setBrand(clothing.getBrand());
        vo.setCategory(clothing.getCategory());
        vo.setSubCategory(clothing.getSubCategory());
        vo.setMaterial(clothing.getMaterial());
        vo.setMaterialDetails(clothing.getMaterialDetails());
        vo.setPattern(clothing.getPattern());
        vo.setPrimaryColor(clothing.getPrimaryColor());
        vo.setPrimaryColorHex(clothing.getPrimaryColorHex());
        vo.setSecondaryColor(clothing.getSecondaryColor());
        vo.setSecondaryColorHex(clothing.getSecondaryColorHex());
        vo.setSize(clothing.getSize());
        vo.setFit(clothing.getFit());
        vo.setStyleTags(stringToList(clothing.getStyleTags()));
        vo.setOccasionTags(stringToList(clothing.getOccasionTags()));
        vo.setSeasonTags(stringToList(clothing.getSeasonTags()));
        vo.setImageUrl(clothing.getImageUrl());
        vo.setImageUrls(stringToList(clothing.getImageUrls()));
        vo.setPurchaseDate(clothing.getPurchaseDate() != null ? clothing.getPurchaseDate().toString() : null);
        vo.setPurchaseLink(clothing.getPurchaseLink());
        vo.setPurchasePrice(clothing.getPurchasePrice() != null ? clothing.getPurchasePrice().doubleValue() : null);
        vo.setUserId(clothing.getUserId());
        vo.setFrequencyLevel(clothing.getFrequencyLevel());
        vo.setWearCount(clothing.getWearCount());
        vo.setLastWornAt(clothing.getLastWornAt());
        vo.setThumbnailUrl(clothing.getThumbnailUrl());
        vo.setSourceType(clothing.getSourceType());
        vo.setAiConfidence(clothing.getAiConfidence() != null ? clothing.getAiConfidence().doubleValue() : null);
        vo.setCreatedAt(clothing.getCreatedAt());
        vo.setUpdatedAt(clothing.getUpdatedAt());
        
        return vo;
    }

    /**
     * 将Clothing实体转换为ClothingListVO
     */
    public ClothingListVO toListVO(Clothing clothing) {
        if (clothing == null) {
            return null;
        }
        
        ClothingListVO vo = new ClothingListVO();
        vo.setId(clothing.getId());
        vo.setName(clothing.getName());
        vo.setCategory(clothing.getCategory());
        vo.setSubCategory(clothing.getSubCategory());
        vo.setPrimaryColor(clothing.getPrimaryColor());
        vo.setPrimaryColorHex(clothing.getPrimaryColorHex());
        vo.setMaterial(clothing.getMaterial());
        vo.setPattern(clothing.getPattern());
        vo.setStyleTags(stringToList(clothing.getStyleTags()));
        vo.setFrequencyLevel(clothing.getFrequencyLevel());
        vo.setWearCount(clothing.getWearCount());
        vo.setImageUrl(clothing.getImageUrl());
        vo.setThumbnailUrl(clothing.getThumbnailUrl());
        vo.setCreatedAt(clothing.getCreatedAt());
        
        return vo;
    }

    /**
     * 将CreateClothingRequest转换为Clothing实体
     */
    public Clothing toEntity(CreateClothingRequest request) {
        if (request == null) {
            return null;
        }
        
        Clothing clothing = new Clothing();
        clothing.setName(request.getName());
        clothing.setBrand(request.getBrand());
        clothing.setCategory(request.getCategory());
        clothing.setSubCategory(request.getSubCategory());
        clothing.setMaterial(request.getMaterial());
        clothing.setMaterialDetails(request.getMaterialDetails());
        clothing.setPattern(request.getPattern());
        clothing.setPrimaryColor(request.getPrimaryColor());
        clothing.setSecondaryColor(request.getSecondaryColor());
        clothing.setSize(request.getSize());
        clothing.setFit(request.getFit());
        clothing.setStyleTags(listToString(request.getStyleTags()));
        clothing.setOccasionTags(listToString(request.getOccasionTags()));
        clothing.setSeasonTags(listToString(request.getSeasonTags()));
        clothing.setImageUrl(request.getImageUrl());
        clothing.setImageUrls(listToString(request.getImageUrls()));
        if (request.getPurchaseDate() != null) {
            try {
                clothing.setPurchaseDate(java.time.LocalDate.parse(request.getPurchaseDate()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        clothing.setPurchaseLink(request.getPurchaseLink());
        if (request.getPurchasePrice() != null) {
            clothing.setPurchasePrice(java.math.BigDecimal.valueOf(request.getPurchasePrice()));
        }
        
        return clothing;
    }

    /**
     * 使用UpdateClothingRequest更新Clothing实体
     */
    public Clothing updateEntity(UpdateClothingRequest request, Clothing clothing) {
        if (request == null || clothing == null) {
            return clothing;
        }
        
        if (request.getName() != null) {
            clothing.setName(request.getName());
        }
        if (request.getBrand() != null) {
            clothing.setBrand(request.getBrand());
        }
        if (request.getCategory() != null) {
            clothing.setCategory(request.getCategory());
        }
        if (request.getSubCategory() != null) {
            clothing.setSubCategory(request.getSubCategory());
        }
        if (request.getMaterial() != null) {
            clothing.setMaterial(request.getMaterial());
        }
        if (request.getMaterialDetails() != null) {
            clothing.setMaterialDetails(request.getMaterialDetails());
        }
        if (request.getPattern() != null) {
            clothing.setPattern(request.getPattern());
        }
        if (request.getPrimaryColor() != null) {
            clothing.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            clothing.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getSize() != null) {
            clothing.setSize(request.getSize());
        }
        if (request.getFit() != null) {
            clothing.setFit(request.getFit());
        }
        if (request.getStyleTags() != null) {
            clothing.setStyleTags(listToString(request.getStyleTags()));
        }
        if (request.getOccasionTags() != null) {
            clothing.setOccasionTags(listToString(request.getOccasionTags()));
        }
        if (request.getSeasonTags() != null) {
            clothing.setSeasonTags(listToString(request.getSeasonTags()));
        }
        if (request.getImageUrls() != null) {
            clothing.setImageUrls(listToString(request.getImageUrls()));
        }
        if (request.getPurchaseDate() != null) {
            try {
                clothing.setPurchaseDate(java.time.LocalDate.parse(request.getPurchaseDate()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (request.getPurchaseLink() != null) {
            clothing.setPurchaseLink(request.getPurchaseLink());
        }
        if (request.getPurchasePrice() != null) {
            clothing.setPurchasePrice(java.math.BigDecimal.valueOf(request.getPurchasePrice()));
        }
        if (request.getFrequencyLevel() != null) {
            clothing.setFrequencyLevel(request.getFrequencyLevel());
        }
        
        return clothing;
    }

    /**
     * 将字符串转换为列表
     */
    private List<String> stringToList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(value, List.class);
        } catch (JsonProcessingException e) {
            return Collections.singletonList(value);
        }
    }

    /**
     * 将列表转换为字符串
     */
    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return list.toString();
        }
    }

    /**
     * 将ClothingWearLog列表转换为ClothingDetailVO.WearHistory.WearRecord列表
     */
    public List<ClothingDetailVO.WearHistory.WearRecord> toWearRecords(List<ClothingWearLog> logs) {
        if (logs == null) {
            return Collections.emptyList();
        }
        return logs.stream()
                .map(log -> {
                    ClothingDetailVO.WearHistory.WearRecord record = new ClothingDetailVO.WearHistory.WearRecord();
                    record.setDate(log.getWornAt().toString());
                    record.setOutfitId(log.getOutfitId());
                    return record;
                })
                .collect(Collectors.toList());
    }
}
