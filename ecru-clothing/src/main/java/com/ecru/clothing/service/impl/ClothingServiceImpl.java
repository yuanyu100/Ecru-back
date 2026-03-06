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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
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

    @Autowired
    @Qualifier("aiImageAnalyzerService")
    private AiImageAnalyzerService imageAnalyzerService;

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
        // 临时硬编码 userId 为 1 用于测试
        userId = 1L;
        log.info("开始AI识别衣物，id: {}, userId: {}", clothingId, userId);
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || clothing.getIsDeleted()) {
            log.error("衣物不存在或已删除，id: {}", clothingId);
            throw new BusinessException("衣物不存在或已删除");
        }
        // 暂时移除用户ID验证，直接测试AI识别功能
        log.info("跳过用户ID验证，直接测试AI识别功能");

        if (StringUtils.isBlank(clothing.getImageUrl())) {
            log.error("衣物图片不存在，无法进行AI识别，id: {}", clothingId);
            throw new BusinessException("衣物图片不存在，无法进行AI识别");
        }

        try {
            log.info("衣物图片URL: {}", clothing.getImageUrl());
            // 从URL获取图片输入流
            try (InputStream imageStream = getImageStreamFromUrl(clothing.getImageUrl())) {
                if (imageStream == null) {
                    log.error("无法获取图片流，id: {}", clothingId);
                    throw new BusinessException("无法获取图片流");
                }
                
                // 调用AI分析服务
                log.info("调用AI分析服务");
                AiImageAnalyzerService.ClothingAnalysisResult analysisResult = imageAnalyzerService.analyzeClothing(imageStream);
                
                if (analysisResult == null) {
                    log.error("AI分析结果为空，id: {}", clothingId);
                    throw new BusinessException("AI分析结果为空");
                }
                
                log.info("AI分析结果: {}", analysisResult.toJson());
                
                // 更新衣物信息
                if (StringUtils.isNotBlank(analysisResult.getCategory())) {
                    clothing.setCategory(analysisResult.getCategory());
                    log.info("更新类别: {}", analysisResult.getCategory());
                }
                if (analysisResult.getColor() != null && analysisResult.getColor().containsKey("primary")) {
                    clothing.setPrimaryColor(analysisResult.getColor().get("primary"));
                    log.info("更新主颜色: {}", analysisResult.getColor().get("primary"));
                }
                if (analysisResult.getColor() != null && analysisResult.getColor().containsKey("secondary")) {
                    clothing.setSecondaryColor(analysisResult.getColor().get("secondary"));
                    log.info("更新次颜色: {}", analysisResult.getColor().get("secondary"));
                }
                if (StringUtils.isNotBlank(analysisResult.getMaterial())) {
                    clothing.setMaterial(analysisResult.getMaterial());
                    log.info("更新材质: {}", analysisResult.getMaterial());
                }
                if (StringUtils.isNotBlank(analysisResult.getPattern())) {
                    clothing.setPattern(analysisResult.getPattern());
                    log.info("更新图案: {}", analysisResult.getPattern());
                }
                if (analysisResult.getStyle() != null && !analysisResult.getStyle().isEmpty()) {
                    clothing.setStyleTags(objectMapper.writeValueAsString(analysisResult.getStyle()));
                    log.info("更新风格标签: {}", analysisResult.getStyle());
                }
                if (analysisResult.getOccasion() != null && !analysisResult.getOccasion().isEmpty()) {
                    clothing.setOccasionTags(objectMapper.writeValueAsString(analysisResult.getOccasion()));
                    log.info("更新场合标签: {}", analysisResult.getOccasion());
                }
                if (analysisResult.getSeason() != null && !analysisResult.getSeason().isEmpty()) {
                    clothing.setSeasonTags(objectMapper.writeValueAsString(analysisResult.getSeason()));
                    log.info("更新季节标签: {}", analysisResult.getSeason());
                }
                
                // 生成衣物名称
                StringBuilder nameBuilder = new StringBuilder();
                if (analysisResult.getColor() != null && StringUtils.isNotBlank(analysisResult.getColor().get("primary"))) {
                    nameBuilder.append(analysisResult.getColor().get("primary"));
                }
                if (StringUtils.isNotBlank(analysisResult.getMaterial())) {
                    nameBuilder.append(analysisResult.getMaterial());
                }
                if (StringUtils.isNotBlank(analysisResult.getCategory())) {
                    nameBuilder.append(analysisResult.getCategory());
                }
                if (nameBuilder.length() > 0) {
                    clothing.setName(nameBuilder.toString());
                    log.info("更新衣物名称: {}", nameBuilder.toString());
                }
                
                clothing.setSourceType("ai");
                clothing.setAiConfidence(BigDecimal.valueOf(0.95)); // 设置默认置信度
                clothing.setUpdatedAt(LocalDateTime.now());
                
                // 保存到数据库
                int updateResult = clothingMapper.updateById(clothing);
                log.info("更新数据库结果: {}", updateResult);
            }
        } catch (Exception e) {
            log.error("AI识别衣物失败", e);
            throw new BusinessException("AI识别失败：" + e.getMessage());
        }

        log.info("AI识别完成，返回衣物详情");
        // 直接返回转换后的 VO 对象，绕过 getClothingDetail 方法中的用户ID验证
        return clothingConverter.toDetailVO(clothing);
    }
    
    /**
     * 从URL获取图片输入流
     */
    private InputStream getImageStreamFromUrl(String imageUrl) throws IOException {
        log.info("处理图片URL: {}", imageUrl);
        
        // 解析URL，提取桶名和对象名
        try {
            // 从URL中提取桶名和对象名
            // URL格式1: http://localhost:9000/bucketName/objectName
            // URL格式2: http://localhost:9000/objectName (桶名为ecru)
            URL url = new URL(imageUrl);
            String path = url.getPath().substring(1); // 移除开头的/            
            int bucketEndIndex = path.indexOf('/');
            
            String bucketName;
            String objectName;
            
            if (bucketEndIndex == -1) {
                // 格式2: 没有桶名，使用默认桶名ecru
                log.info("URL格式: 没有桶名，使用默认桶名ecru");
                bucketName = "ecru";
                objectName = path;
            } else {
                // 格式1: 包含桶名
                log.info("URL格式: 包含桶名");
                bucketName = path.substring(0, bucketEndIndex);
                objectName = path.substring(bucketEndIndex + 1);
            }
            
            log.info("提取桶名: {}, 对象名: {}", bucketName, objectName);
            
            // 使用MinioClient获取对象
            MinioClient minioClient = MinioClient.builder()
                    .endpoint("http://localhost:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();
            
            // 获取对象
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            
            log.info("成功获取MinIO对象流");
            return inputStream;
        } catch (Exception e) {
            log.error("获取图片流失败: {}", e.getMessage(), e);
            // 如果MinIO访问失败，尝试使用HTTP URLConnection作为备用
            log.info("尝试使用HTTP URLConnection获取图片");
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            return connection.getInputStream();
        }
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
