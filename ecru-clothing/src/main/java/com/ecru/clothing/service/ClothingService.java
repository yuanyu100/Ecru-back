package com.ecru.clothing.service;

import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.RecordWearRequest;
import com.ecru.clothing.dto.request.UpdateClothingRequest;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.dto.response.ClothingStatisticsVO;
import com.github.pagehelper.PageInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClothingService {

    ClothingDetailVO createClothing(Long userId,  CreateClothingRequest request);

    PageInfo<ClothingListVO> getClothingList(
            Long userId,
            ClothingQueryRequest request
    );

    ClothingDetailVO getClothingDetail(Long userId, Long clothingId);

    ClothingDetailVO updateClothing(Long userId, Long clothingId, UpdateClothingRequest request);

    void deleteClothing(Long userId, Long clothingId, Boolean force);

    ClothingDetailVO recognizeClothing(Long userId, Long clothingId);

    void setFrequency(Long userId, Long clothingId, Integer frequencyLevel);

    void recordWear(Long userId, Long clothingId, RecordWearRequest request);

    ClothingStatisticsVO getClothingStatistics(Long userId, String period);

}
