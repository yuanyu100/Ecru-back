package com.ecru.clothing.service.impl;

import com.ecru.clothing.converter.ClothingConverter;
import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.RecordWearRequest;
import com.ecru.clothing.dto.request.UpdateClothingRequest;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.dto.response.ClothingStatisticsVO;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.entity.ClothingWearLog;
import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.clothing.mapper.ClothingWearLogMapper;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ecru.common.service.storage.ImageStorageService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClothingServiceImpl implements ClothingService {

    @Autowired
    private ClothingMapper clothingMapper;

    @Autowired
    private ClothingWearLogMapper clothingWearLogMapper;

    @Autowired
    private ClothingConverter clothingConverter;

    @Autowired
    private ImageStorageService imageStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ClothingDetailVO createClothing(Long userId,  CreateClothingRequest request) {
        return createClothing(userId, request, null);
    }

    @Override
    public ClothingDetailVO createClothing(Long userId,  CreateClothingRequest request, MultipartFile image) {
        Clothing clothing = new Clothing();
        clothing.setUserId(userId);
        
        // 处理图片上传
        if (image != null && !image.isEmpty()) {
            String imageUrl = imageStorageService.uploadImage(image, userId);
            clothing.setImageUrl(imageUrl);
        } else if (request != null && StringUtils.isNotBlank(request.getImageUrl())) {
            clothing.setImageUrl(request.getImageUrl());
        } else {
            throw new BusinessException("请上传衣物图片或提供图片URL");
        }

        // 处理其他字段
        if (request != null) {
            if (StringUtils.isNotBlank(request.getName())) {
                clothing.setName(request.getName());
            } else {
                clothing.setName("未命名衣物");
            }
            
            if (StringUtils.isNotBlank(request.getCategory())) {
                clothing.setCategory(request.getCategory());
            }
            if (StringUtils.isNotBlank(request.getSubCategory())) {
                clothing.setSubCategory(request.getSubCategory());
            }
            if (StringUtils.isNotBlank(request.getPrimaryColor())) {
                clothing.setPrimaryColor(request.getPrimaryColor());
            }
            if (StringUtils.isNotBlank(request.getSecondaryColor())) {
                clothing.setSecondaryColor(request.getSecondaryColor());
            }
            if (StringUtils.isNotBlank(request.getMaterial())) {
                clothing.setMaterial(request.getMaterial());
            }
            if (StringUtils.isNotBlank(request.getMaterialDetails())) {
                clothing.setMaterialDetails(request.getMaterialDetails());
            }
            if (StringUtils.isNotBlank(request.getPattern())) {
                clothing.setPattern(request.getPattern());
            }
            if (StringUtils.isNotBlank(request.getFit())) {
                clothing.setFit(request.getFit());
            }
            if (StringUtils.isNotBlank(request.getSize())) {
                clothing.setSize(request.getSize());
            }
            if (request.getStyleTags() != null && !request.getStyleTags().isEmpty()) {
                try {
                    clothing.setStyleTags(objectMapper.writeValueAsString(request.getStyleTags()));
                } catch (JsonProcessingException e) {
                    log.error("序列化styleTags失败", e);
                }
            }
            if (request.getOccasionTags() != null && !request.getOccasionTags().isEmpty()) {
                try {
                    clothing.setOccasionTags(objectMapper.writeValueAsString(request.getOccasionTags()));
                } catch (JsonProcessingException e) {
                    log.error("序列化occasionTags失败", e);
                }
            }
            if (request.getSeasonTags() != null && !request.getSeasonTags().isEmpty()) {
                try {
                    clothing.setSeasonTags(objectMapper.writeValueAsString(request.getSeasonTags()));
                } catch (JsonProcessingException e) {
                    log.error("序列化seasonTags失败", e);
                }
            }
            if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
                try {
                    clothing.setImageUrls(objectMapper.writeValueAsString(request.getImageUrls()));
                } catch (JsonProcessingException e) {
                    log.error("序列化imageUrls失败", e);
                }
            }
            if (request.getPurchasePrice() != null) {
                clothing.setPurchasePrice(BigDecimal.valueOf(request.getPurchasePrice()));
            }
            if (StringUtils.isNotBlank(request.getPurchaseDate())) {
                try {
                    clothing.setPurchaseDate(LocalDate.parse(request.getPurchaseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    log.error("解析购买日期失败", e);
                }
            }
            if (StringUtils.isNotBlank(request.getPurchaseLink())) {
                clothing.setPurchaseLink(request.getPurchaseLink());
            }
            if (StringUtils.isNotBlank(request.getBrand())) {
                clothing.setBrand(request.getBrand());
            }
        }

        // 设置默认值
        clothing.setFrequencyLevel(3);
        clothing.setWearCount(0);
        clothing.setIsDeleted(false);
        clothing.setSourceType("manual");
        clothing.setCreatedAt(LocalDateTime.now());
        clothing.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        clothingMapper.insert(clothing);

        // 模拟AI识别
        if (request != null && Boolean.TRUE.equals(request.getAutoRecognize())) {
            return recognizeClothing(userId, clothing.getId());
        }

        return clothingConverter.toDetailVO(clothing);
    }

    @Override
    public PageInfo<ClothingListVO> getClothingList(Long userId, ClothingQueryRequest request) {
        Integer page = request.getPage() != null ? request.getPage() : 1;
        Integer size = request.getSize() != null ? request.getSize() : 20;
        
        // 使用PageHelper进行分页
        PageHelper.startPage(page, size);
        List<Clothing> clothings = clothingMapper.selectClothingList(
                userId, request.getCategory(), request.getPrimaryColor(), request.getMaterial(), 
                request.getStyleTag(), request.getOccasionTag(), request.getSeasonTag(), 
                request.getKeyword(), request.getSortBy(), request.getSortOrder(), 
                request.getMinFrequency(), request.getMaxFrequency()
        );
        
        // 转换为VO并包装为PageInfo
        List<ClothingListVO> voList = clothings.stream()
                .map(clothingConverter::toListVO)
                .collect(Collectors.toList());
        
        return new PageInfo<>(voList);
    }

    @Override
    public ClothingDetailVO getClothingDetail(Long userId, Long clothingId) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted() || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        ClothingDetailVO vo = clothingConverter.toDetailVO(clothing);
        
        // 添加穿着历史
        List<ClothingWearLog> logs = clothingWearLogMapper.selectRecentWearLogs(clothingId, 10);
        ClothingDetailVO.WearHistory wearHistory = new ClothingDetailVO.WearHistory();
        wearHistory.setTotal(clothing.getWearCount());
        wearHistory.setRecent(logs.stream()
                .map(log -> {
                    ClothingDetailVO.WearHistory.WearRecord record = new ClothingDetailVO.WearHistory.WearRecord();
                    record.setDate(log.getWornAt().toString());
                    record.setOutfitId(log.getOutfitId());
                    return record;
                })
                .collect(Collectors.toList())
        );
        vo.setWearHistory(wearHistory);

        return vo;
    }

    @Override
    public ClothingDetailVO updateClothing(Long userId, Long clothingId, UpdateClothingRequest request) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted() || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        // 更新字段
        if (StringUtils.isNotBlank(request.getName())) {
            clothing.setName(request.getName());
        }
        if (StringUtils.isNotBlank(request.getCategory())) {
            clothing.setCategory(request.getCategory());
        }
        if (StringUtils.isNotBlank(request.getSubCategory())) {
            clothing.setSubCategory(request.getSubCategory());
        }
        if (StringUtils.isNotBlank(request.getPrimaryColor())) {
            clothing.setPrimaryColor(request.getPrimaryColor());
        }
        if (StringUtils.isNotBlank(request.getSecondaryColor())) {
            clothing.setSecondaryColor(request.getSecondaryColor());
        }
        if (StringUtils.isNotBlank(request.getMaterial())) {
            clothing.setMaterial(request.getMaterial());
        }
        if (StringUtils.isNotBlank(request.getMaterialDetails())) {
            clothing.setMaterialDetails(request.getMaterialDetails());
        }
        if (StringUtils.isNotBlank(request.getPattern())) {
            clothing.setPattern(request.getPattern());
        }
        if (StringUtils.isNotBlank(request.getFit())) {
            clothing.setFit(request.getFit());
        }
        if (StringUtils.isNotBlank(request.getSize())) {
            clothing.setSize(request.getSize());
        }
        if (request.getStyleTags() != null) {
            try {
                clothing.setStyleTags(objectMapper.writeValueAsString(request.getStyleTags()));
            } catch (JsonProcessingException e) {
                log.error("序列化styleTags失败", e);
            }
        }
        if (request.getOccasionTags() != null) {
            try {
                clothing.setOccasionTags(objectMapper.writeValueAsString(request.getOccasionTags()));
            } catch (JsonProcessingException e) {
                log.error("序列化occasionTags失败", e);
            }
        }
        if (request.getSeasonTags() != null) {
            try {
                clothing.setSeasonTags(objectMapper.writeValueAsString(request.getSeasonTags()));
            } catch (JsonProcessingException e) {
                log.error("序列化seasonTags失败", e);
            }
        }
        if (request.getImageUrls() != null) {
            try {
                clothing.setImageUrls(objectMapper.writeValueAsString(request.getImageUrls()));
            } catch (JsonProcessingException e) {
                log.error("序列化imageUrls失败", e);
            }
        }
        if (request.getPurchasePrice() != null) {
            clothing.setPurchasePrice(BigDecimal.valueOf(request.getPurchasePrice()));
        }
        if (StringUtils.isNotBlank(request.getPurchaseDate())) {
            try {
                clothing.setPurchaseDate(LocalDate.parse(request.getPurchaseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception e) {
                log.error("解析购买日期失败", e);
            }
        }
        if (StringUtils.isNotBlank(request.getPurchaseLink())) {
            clothing.setPurchaseLink(request.getPurchaseLink());
        }
        if (StringUtils.isNotBlank(request.getBrand())) {
            clothing.setBrand(request.getBrand());
        }
        if (request.getFrequencyLevel() != null) {
            clothing.setFrequencyLevel(request.getFrequencyLevel());
        }

        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);

        return getClothingDetail(userId, clothingId);
    }

    @Override
    public void deleteClothing(Long userId, Long clothingId, Boolean force) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        if (force) {
            // 硬删除
            clothingMapper.deleteById(clothingId);
        } else {
            // 软删除
            clothing.setIsDeleted(true);
            clothing.setUpdatedAt(LocalDateTime.now());
            clothingMapper.updateById(clothing);
        }
    }

    @Override
    public ClothingDetailVO recognizeClothing(Long userId, Long clothingId) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted() || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        // 模拟AI识别结果
        clothing.setCategory("上装");
        clothing.setSubCategory("T恤");
        clothing.setPrimaryColor("白色");
        clothing.setMaterial("棉");
        clothing.setPattern("纯色");
        clothing.setFit("宽松");
        
        try {
            clothing.setStyleTags(objectMapper.writeValueAsString(Collections.singletonList("简约")));
            clothing.setOccasionTags(objectMapper.writeValueAsString(Collections.singletonList("日常")));
            clothing.setSeasonTags(objectMapper.writeValueAsString(List.of("春", "夏", "秋")));
        } catch (JsonProcessingException e) {
            log.error("序列化标签失败", e);
        }

        clothing.setName("白色纯棉T恤");
        clothing.setSourceType("ai");
        clothing.setAiConfidence(BigDecimal.valueOf(0.92));
        clothing.setUpdatedAt(LocalDateTime.now());

        clothingMapper.updateById(clothing);
        return getClothingDetail(userId, clothingId);
    }

    @Override
    public void setFrequency(Long userId, Long clothingId, Integer frequencyLevel) {
        if (frequencyLevel < 1 || frequencyLevel > 5) {
            throw new BusinessException("搭配频率必须在1-5之间");
        }

        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted() || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        clothing.setFrequencyLevel(frequencyLevel);
        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);
    }

    @Override
    public void recordWear(Long userId, Long clothingId, RecordWearRequest request) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted() || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权限访问");
        }

        // 创建穿着记录
        ClothingWearLog wearLog = new ClothingWearLog();
        wearLog.setClothingId(clothingId);
        wearLog.setUserId(userId);
        
        if (request != null && StringUtils.isNotBlank(request.getWornAt())) {
            try {
                wearLog.setWornAt(LocalDate.parse(request.getWornAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception e) {
                log.error("解析穿着日期失败", e);
                wearLog.setWornAt(LocalDate.now());
            }
        } else {
            wearLog.setWornAt(LocalDate.now());
        }

        if (request != null && request.getOutfitId() != null) {
            wearLog.setOutfitId(request.getOutfitId());
        }
        if (request != null && StringUtils.isNotBlank(request.getWeatherCondition())) {
            wearLog.setWeatherCondition(request.getWeatherCondition());
        }
        if (request != null && request.getTemperature() != null) {
            wearLog.setTemperature(BigDecimal.valueOf(request.getTemperature()));
        }
        if (request != null && StringUtils.isNotBlank(request.getNotes())) {
            wearLog.setNotes(request.getNotes());
        }
        wearLog.setCreatedAt(LocalDateTime.now());

        clothingWearLogMapper.insert(wearLog);

        // 更新衣物的穿着次数和最后穿着时间
        clothing.setWearCount(clothing.getWearCount() + 1);
        clothing.setLastWornAt(LocalDateTime.now());
        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);
    }

    @Override
    public ClothingStatisticsVO getClothingStatistics(Long userId, String period) {
        ClothingStatisticsVO vo = new ClothingStatisticsVO();

        // 概览统计
        Map<String, Object> overviewMap = clothingMapper.selectClothingStatistics(userId, period);
        ClothingStatisticsVO.Overview overview = new ClothingStatisticsVO.Overview();
        if (overviewMap != null) {
            overview.setTotalClothings(overviewMap.get("totalClothings") != null ? ((Number) overviewMap.get("totalClothings")).intValue() : 0);
            overview.setTotalWornThisPeriod(overviewMap.get("totalWorn") != null ? ((Number) overviewMap.get("totalWorn")).intValue() : 0);
            overview.setMostWornClothingId(overviewMap.get("mostWornClothingId") != null ? ((Number) overviewMap.get("mostWornClothingId")).longValue() : null);
            overview.setMostWornCount(overviewMap.get("mostWornCount") != null ? ((Number) overviewMap.get("mostWornCount")).intValue() : 0);
        }
        vo.setOverview(overview);

        // 按类别统计
        List<Map<String, Object>> categoryMaps = clothingMapper.selectClothingCountByCategory(userId);
        List<ClothingStatisticsVO.CategoryStat> categoryStats = categoryMaps.stream()
                .map(map -> {
                    ClothingStatisticsVO.CategoryStat stat = new ClothingStatisticsVO.CategoryStat();
                    stat.setCategory(map.get("category") != null ? map.get("category").toString() : "");
                    stat.setCount(map.get("count") != null ? ((Number) map.get("count")).intValue() : 0);
                    stat.setPercentage(map.get("percentage") != null ? ((Number) map.get("percentage")).doubleValue() : 0);
                    return stat;
                })
                .collect(Collectors.toList());
        vo.setByCategory(categoryStats);

        // 按颜色统计
        List<Map<String, Object>> colorMaps = clothingMapper.selectClothingCountByColor(userId);
        List<ClothingStatisticsVO.ColorStat> colorStats = colorMaps.stream()
                .map(map -> {
                    ClothingStatisticsVO.ColorStat stat = new ClothingStatisticsVO.ColorStat();
                    stat.setColor(map.get("color") != null ? map.get("color").toString() : "");
                    stat.setCount(map.get("count") != null ? ((Number) map.get("count")).intValue() : 0);
                    stat.setPercentage(map.get("percentage") != null ? ((Number) map.get("percentage")).doubleValue() : 0);
                    return stat;
                })
                .collect(Collectors.toList());
        vo.setByColor(colorStats);

        // 按频率统计
        List<Map<String, Object>> frequencyMaps = clothingMapper.selectClothingCountByFrequency(userId);
        List<ClothingStatisticsVO.FrequencyStat> frequencyStats = frequencyMaps.stream()
                .map(map -> {
                    ClothingStatisticsVO.FrequencyStat stat = new ClothingStatisticsVO.FrequencyStat();
                    stat.setLevel(map.get("level") != null ? ((Number) map.get("level")).intValue() : 0);
                    stat.setLabel(map.get("label") != null ? map.get("label").toString() : "");
                    stat.setCount(map.get("count") != null ? ((Number) map.get("count")).intValue() : 0);
                    return stat;
                })
                .collect(Collectors.toList());
        vo.setByFrequency(frequencyStats);

        // 穿着趋势
        List<Map<String, Object>> trendMaps = clothingMapper.selectWearTrend(userId, period);
        List<ClothingStatisticsVO.WearTrend> wearTrends = trendMaps.stream()
                .map(map -> {
                    ClothingStatisticsVO.WearTrend trend = new ClothingStatisticsVO.WearTrend();
                    trend.setDate(map.get("date") != null ? map.get("date").toString() : "");
                    trend.setCount(map.get("count") != null ? ((Number) map.get("count")).intValue() : 0);
                    return trend;
                })
                .collect(Collectors.toList());
        vo.setWearTrend(wearTrends);

        return vo;
    }

    private String saveImage(MultipartFile image) {
        try {
            // 创建保存目录
            String uploadDir = "uploads/clothings/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名
            String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
            File dest = new File(dir, fileName);

            // 保存文件
            image.transferTo(dest);

            // 返回相对路径
            return "/" + uploadDir + "/" + fileName;
        } catch (IOException e) {
            log.error("保存图片失败", e);
            throw new BusinessException("保存图片失败");
        }
    }

}
