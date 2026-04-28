package com.ecru.clothing.service.impl;

import com.ecru.clothing.converter.ClothingConverter;
import com.ecru.clothing.dto.request.AdminClothingQueryRequest;
import com.ecru.clothing.dto.request.ClothingQueryRequest;
import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.RecordWearRequest;
import com.ecru.clothing.dto.request.UpdateClothingRequest;
import com.ecru.clothing.dto.response.AdminClothingListVO;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.ClothingListVO;
import com.ecru.clothing.dto.response.ClothingStatisticsVO;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.entity.ClothingWearLog;
import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.clothing.mapper.ClothingWearLogMapper;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.config.MinioConfig;
import com.ecru.common.exception.BusinessException;
import com.ecru.common.service.ai.AiImageAnalyzerService;
import com.ecru.common.service.storage.ImageStorageService;
import com.ecru.common.service.vector.ClothingVectorService;
import com.ecru.common.util.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClothingServiceImpl implements ClothingService {

    private final ClothingMapper clothingMapper;
    private final ClothingWearLogMapper clothingWearLogMapper;
    private final ClothingConverter clothingConverter;
    private final ImageStorageService imageStorageService;
    private final MinioConfig minioConfig;

    @Qualifier("aiImageAnalyzerService")
    private final AiImageAnalyzerService imageAnalyzerService;

    private final ClothingVectorService clothingVectorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ClothingDetailVO createClothing(Long userId, CreateClothingRequest request) {
        return createClothing(userId, request, null);
    }

    @Override
    public ClothingDetailVO createClothing(Long userId, CreateClothingRequest request, MultipartFile image) {
        Clothing clothing = new Clothing();
        clothing.setUserId(userId);

        if (image != null && !image.isEmpty()) {
            clothing.setImageUrl(imageStorageService.uploadImage(image, userId));
        } else if (request != null && StringUtils.isNotBlank(request.getImageUrl())) {
            clothing.setImageUrl(request.getImageUrl());
        }

        applyCreateRequest(clothing, request);
        clothing.setFrequencyLevel(3);
        clothing.setWearCount(0);
        clothing.setIsDeleted(false);
        clothing.setSourceType("manual");
        clothing.setCreatedAt(LocalDateTime.now());
        clothing.setUpdatedAt(LocalDateTime.now());

        clothingMapper.insert(clothing);
        createVector(clothing);

        if (request != null && Boolean.TRUE.equals(request.getAutoRecognize())) {
            return recognizeClothing(userId, clothing.getId());
        }

        return clothingConverter.toDetailVO(clothing);
    }

    @Override
    public PageInfo<ClothingListVO> getClothingList(Long userId, ClothingQueryRequest request) {
        ClothingQueryRequest safeRequest = request != null ? request : new ClothingQueryRequest();
        int pageNum = safeRequest.getPage() != null && safeRequest.getPage() > 0 ? safeRequest.getPage() : 1;
        int pageSize = safeRequest.getSize() != null && safeRequest.getSize() > 0 ? safeRequest.getSize() : 20;
        long offset = (long) (pageNum - 1) * pageSize;
        long total = clothingMapper.countClothingList(userId, safeRequest);

        List<ClothingListVO> voList = clothingMapper.selectClothingList(userId, safeRequest, offset, pageSize).stream()
                .map(clothingConverter::toListVO)
                .collect(Collectors.toList());

        return buildPageInfo(voList, pageNum, pageSize, total);
    }

    @Override
    public PageInfo<AdminClothingListVO> getAdminClothingList(AdminClothingQueryRequest request) {
        AdminClothingQueryRequest safeRequest = request != null ? request : new AdminClothingQueryRequest();
        int pageNum = safeRequest.getPage() != null && safeRequest.getPage() > 0 ? safeRequest.getPage() : 1;
        int pageSize = safeRequest.getSize() != null && safeRequest.getSize() > 0 ? safeRequest.getSize() : 20;
        long offset = (long) (pageNum - 1) * pageSize;
        long total = clothingMapper.countAdminClothingList(safeRequest);

        List<AdminClothingListVO> records = clothingMapper.selectAdminClothingList(safeRequest, offset, pageSize);
        return buildPageInfo(records, pageNum, pageSize, total);
    }

    @Override
    public ClothingDetailVO getClothingDetail(Long userId, Long clothingId) {
        Clothing clothing = requireOwnedClothing(userId, clothingId);
        ClothingDetailVO vo = clothingConverter.toDetailVO(clothing);

        List<ClothingWearLog> logs = clothingWearLogMapper.selectRecentWearLogs(clothingId, 10);
        ClothingDetailVO.WearHistory wearHistory = new ClothingDetailVO.WearHistory();
        wearHistory.setTotal(clothing.getWearCount());
        wearHistory.setRecent(clothingConverter.toWearRecords(logs));
        vo.setWearHistory(wearHistory);
        return vo;
    }

    @Override
    public ClothingDetailVO updateClothing(Long userId, Long clothingId, UpdateClothingRequest request) {
        Clothing clothing = requireOwnedClothing(userId, clothingId);

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
            clothing.setStyleTags(writeJson(request.getStyleTags()));
        }
        if (request.getOccasionTags() != null) {
            clothing.setOccasionTags(writeJson(request.getOccasionTags()));
        }
        if (request.getSeasonTags() != null) {
            clothing.setSeasonTags(writeJson(request.getSeasonTags()));
        }
        if (request.getImageUrls() != null) {
            clothing.setImageUrls(writeJson(request.getImageUrls()));
        }
        if (StringUtils.isNotBlank(request.getImageUrl())) {
            clothing.setImageUrl(request.getImageUrl());
        }
        if (request.getPurchasePrice() != null) {
            clothing.setPurchasePrice(BigDecimal.valueOf(request.getPurchasePrice()));
        }
        if (StringUtils.isNotBlank(request.getPurchaseDate())) {
            try {
                clothing.setPurchaseDate(LocalDate.parse(request.getPurchaseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception e) {
                log.error("Parse purchase date failed", e);
            }
        }
        if (StringUtils.isNotBlank(request.getPurchaseLink())) {
            clothing.setPurchaseLink(request.getPurchaseLink());
        }
        if (StringUtils.isNotBlank(request.getBrand())) {
            clothing.setBrand(request.getBrand());
        }
        if (StringUtils.isNotBlank(request.getSourcePlatform())) {
            clothing.setSourcePlatform(request.getSourcePlatform());
        }
        if (StringUtils.isNotBlank(request.getSourceOrderId())) {
            clothing.setSourceOrderId(request.getSourceOrderId());
        }
        if (StringUtils.isNotBlank(request.getSourceShopName())) {
            clothing.setSourceShopName(request.getSourceShopName());
        }
        if (StringUtils.isNotBlank(request.getSourceSkuText())) {
            clothing.setSourceSkuText(request.getSourceSkuText());
        }
        if (request.getFrequencyLevel() != null) {
            clothing.setFrequencyLevel(request.getFrequencyLevel());
        }

        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);
        updateVector(clothing);
        return getClothingDetail(userId, clothingId);
    }

    @Override
    public void deleteClothing(Long userId, Long clothingId, Boolean force) {
        Clothing clothing = requireOwnedClothing(userId, clothingId);
        removeClothing(clothing, force);
    }

    @Override
    public void adminDeleteClothing(Long clothingId, Boolean force) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || Boolean.TRUE.equals(clothing.getIsDeleted())) {
            throw new BusinessException("衣物不存在");
        }
        removeClothing(clothing, force);
    }

    @Override
    public ClothingDetailVO recognizeClothing(Long userId, Long clothingId) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || Boolean.TRUE.equals(clothing.getIsDeleted())) {
            throw new BusinessException("衣物不存在");
        }
        if (!clothing.getUserId().equals(userId) && !UserContext.isAdmin()) {
            throw new BusinessException("无权访问该衣物");
        }
        if (StringUtils.isBlank(clothing.getImageUrl())) {
            throw new BusinessException("衣物缺少图片，无法执行 AI 识别");
        }

        try (InputStream imageStream = getImageStreamFromUrl(clothing.getImageUrl())) {
            AiImageAnalyzerService.ClothingAnalysisResult analysisResult = imageAnalyzerService.analyzeClothing(imageStream);
            if (analysisResult == null) {
                throw new BusinessException("AI 分析结果为空");
            }

            if (StringUtils.isNotBlank(analysisResult.getCategory())) {
                clothing.setCategory(analysisResult.getCategory());
            }
            if (analysisResult.getColor() != null) {
                clothing.setPrimaryColor(analysisResult.getColor().get("primary"));
                clothing.setSecondaryColor(analysisResult.getColor().get("secondary"));
            }
            if (StringUtils.isNotBlank(analysisResult.getMaterial())) {
                clothing.setMaterial(analysisResult.getMaterial());
            }
            if (StringUtils.isNotBlank(analysisResult.getPattern())) {
                clothing.setPattern(analysisResult.getPattern());
            }
            if (analysisResult.getStyle() != null && !analysisResult.getStyle().isEmpty()) {
                clothing.setStyleTags(writeJson(analysisResult.getStyle()));
            }
            if (analysisResult.getOccasion() != null && !analysisResult.getOccasion().isEmpty()) {
                clothing.setOccasionTags(writeJson(analysisResult.getOccasion()));
            }
            if (analysisResult.getSeason() != null && !analysisResult.getSeason().isEmpty()) {
                clothing.setSeasonTags(writeJson(analysisResult.getSeason()));
            }

            String generatedName = buildRecognizedName(analysisResult);
            if (StringUtils.isNotBlank(generatedName)) {
                clothing.setName(generatedName);
            }

            clothing.setSourceType("ai");
            clothing.setAiConfidence(BigDecimal.valueOf(0.95));
            clothing.setUpdatedAt(LocalDateTime.now());
            clothingMapper.updateById(clothing);
            updateVector(clothing);
            return clothingConverter.toDetailVO(clothing);
        } catch (IOException e) {
            log.error("Read clothing image failed", e);
            throw new BusinessException("读取衣物图片失败");
        } catch (Exception e) {
            log.error("Recognize clothing failed", e);
            throw new BusinessException("AI 识别失败: " + e.getMessage());
        }
    }

    @Override
    public void setFrequency(Long userId, Long clothingId, Integer frequencyLevel) {
        if (frequencyLevel == null || frequencyLevel < 1 || frequencyLevel > 5) {
            throw new BusinessException("搭配频率必须在 1-5 之间");
        }

        Clothing clothing = requireOwnedClothing(userId, clothingId);
        clothing.setFrequencyLevel(frequencyLevel);
        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);
    }

    @Override
    @Transactional
    public void recordWear(Long userId, Long clothingId, RecordWearRequest request) {
        Clothing clothing = requireOwnedClothing(userId, clothingId);

        ClothingWearLog wearLog = new ClothingWearLog();
        wearLog.setClothingId(clothingId);
        wearLog.setUserId(userId);
        wearLog.setWornAt(parseWearDate(request != null ? request.getWornAt() : null));
        if (request != null) {
            wearLog.setOutfitId(request.getOutfitId());
            wearLog.setWeatherCondition(request.getWeatherCondition());
            if (request.getTemperature() != null) {
                wearLog.setTemperature(BigDecimal.valueOf(request.getTemperature()));
            }
            wearLog.setNotes(request.getNotes());
        }
        wearLog.setCreatedAt(LocalDateTime.now());
        clothingWearLogMapper.insert(wearLog);

        clothing.setWearCount((clothing.getWearCount() == null ? 0 : clothing.getWearCount()) + 1);
        clothing.setLastWornAt(LocalDateTime.now());
        clothing.setUpdatedAt(LocalDateTime.now());
        clothingMapper.updateById(clothing);
    }

    @Override
    public ClothingStatisticsVO getClothingStatistics(Long userId, String period) {
        ClothingStatisticsVO vo = new ClothingStatisticsVO();

        Map<String, Object> overviewMap = clothingMapper.selectClothingStatistics(userId, period);
        ClothingStatisticsVO.Overview overview = new ClothingStatisticsVO.Overview();
        if (overviewMap != null) {
            overview.setTotalClothings(numberToInt(overviewMap.get("totalClothings")));
            overview.setTotalWornThisPeriod(numberToInt(overviewMap.get("totalWorn")));
            overview.setMostWornClothingId(numberToLong(overviewMap.get("mostWornClothingId")));
            overview.setMostWornCount(numberToInt(overviewMap.get("mostWornCount")));
        }
        vo.setOverview(overview);

        vo.setByCategory(clothingMapper.selectClothingCountByCategory(userId).stream().map(map -> {
            ClothingStatisticsVO.CategoryStat stat = new ClothingStatisticsVO.CategoryStat();
            stat.setCategory(stringValue(map.get("category")));
            stat.setCount(numberToInt(map.get("count")));
            stat.setPercentage(numberToDouble(map.get("percentage")));
            return stat;
        }).toList());

        vo.setByColor(clothingMapper.selectClothingCountByColor(userId).stream().map(map -> {
            ClothingStatisticsVO.ColorStat stat = new ClothingStatisticsVO.ColorStat();
            stat.setColor(stringValue(map.get("color")));
            stat.setCount(numberToInt(map.get("count")));
            stat.setPercentage(numberToDouble(map.get("percentage")));
            return stat;
        }).toList());

        vo.setByFrequency(clothingMapper.selectClothingCountByFrequency(userId).stream().map(map -> {
            ClothingStatisticsVO.FrequencyStat stat = new ClothingStatisticsVO.FrequencyStat();
            stat.setLevel(numberToInt(map.get("level")));
            stat.setLabel(stringValue(map.get("label")));
            stat.setCount(numberToInt(map.get("count")));
            return stat;
        }).toList());

        vo.setWearTrend(clothingMapper.selectWearTrend(userId, period).stream().map(map -> {
            ClothingStatisticsVO.WearTrend trend = new ClothingStatisticsVO.WearTrend();
            trend.setDate(stringValue(map.get("date")));
            trend.setCount(numberToInt(map.get("count")));
            return trend;
        }).toList());

        return vo;
    }

    private Clothing requireOwnedClothing(Long userId, Long clothingId) {
        Clothing clothing = clothingMapper.selectById(clothingId);
        if (clothing == null || Boolean.TRUE.equals(clothing.getIsDeleted()) || !clothing.getUserId().equals(userId)) {
            throw new BusinessException("衣物不存在或无权访问");
        }
        return clothing;
    }

    private void removeClothing(Clothing clothing, Boolean force) {
        if (Boolean.TRUE.equals(force)) {
            clothingMapper.deleteById(clothing.getId());
        } else {
            clothing.setIsDeleted(true);
            clothing.setUpdatedAt(LocalDateTime.now());
            clothingMapper.updateById(clothing);
        }
        clothingVectorService.deleteClothingVector(clothing.getId(), clothing.getUserId());
    }

    private void updateVector(Clothing clothing) {
        clothingVectorService.updateClothingVector(
                clothing.getId(),
                clothing.getUserId(),
                clothing.getName(),
                clothing.getCategory(),
                clothing.getPrimaryColor(),
                clothing.getStyleTags(),
                clothing.getOccasionTags(),
                clothing.getSeasonTags()
        );
    }

    private void createVector(Clothing clothing) {
        clothingVectorService.generateAndStoreVector(
                clothing.getId(),
                clothing.getUserId(),
                clothing.getName(),
                clothing.getCategory(),
                clothing.getPrimaryColor(),
                clothing.getStyleTags(),
                clothing.getOccasionTags(),
                clothing.getSeasonTags()
        );
    }

    private void applyCreateRequest(Clothing clothing, CreateClothingRequest request) {
        if (request == null) {
            clothing.setName("未命名衣物");
            return;
        }

        clothing.setName(StringUtils.isNotBlank(request.getName()) ? request.getName() : "未命名衣物");
        clothing.setCategory(request.getCategory());
        clothing.setSubCategory(request.getSubCategory());
        clothing.setPrimaryColor(request.getPrimaryColor());
        clothing.setSecondaryColor(request.getSecondaryColor());
        clothing.setMaterial(request.getMaterial());
        clothing.setMaterialDetails(request.getMaterialDetails());
        clothing.setPattern(request.getPattern());
        clothing.setFit(request.getFit());
        clothing.setSize(request.getSize());
        clothing.setStyleTags(writeJson(request.getStyleTags()));
        clothing.setOccasionTags(writeJson(request.getOccasionTags()));
        clothing.setSeasonTags(writeJson(request.getSeasonTags()));
        clothing.setImageUrls(writeJson(request.getImageUrls()));
        clothing.setPurchaseLink(request.getPurchaseLink());
        clothing.setBrand(request.getBrand());
        clothing.setSourcePlatform(request.getSourcePlatform());
        clothing.setSourceOrderId(request.getSourceOrderId());
        clothing.setSourceShopName(request.getSourceShopName());
        clothing.setSourceSkuText(request.getSourceSkuText());
        if (request.getPurchasePrice() != null) {
            clothing.setPurchasePrice(BigDecimal.valueOf(request.getPurchasePrice()));
        }
        if (StringUtils.isNotBlank(request.getPurchaseDate())) {
            try {
                clothing.setPurchaseDate(LocalDate.parse(request.getPurchaseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } catch (Exception e) {
                log.error("Parse purchase date failed", e);
            }
        }
    }

    private String writeJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            log.error("Serialize json field failed", e);
            return null;
        }
    }

    private String buildRecognizedName(AiImageAnalyzerService.ClothingAnalysisResult result) {
        StringBuilder builder = new StringBuilder();
        if (result.getColor() != null && StringUtils.isNotBlank(result.getColor().get("primary"))) {
            builder.append(result.getColor().get("primary"));
        }
        if (StringUtils.isNotBlank(result.getMaterial())) {
            builder.append(result.getMaterial());
        }
        if (StringUtils.isNotBlank(result.getCategory())) {
            builder.append(result.getCategory());
        }
        return builder.toString();
    }

    private InputStream getImageStreamFromUrl(String imageUrl) throws IOException {
        try {
            URL url = new URL(imageUrl);
            String path = url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath();
            int bucketEndIndex = path.indexOf('/');

            String bucketName;
            String objectName;
            if (bucketEndIndex == -1) {
                bucketName = minioConfig.getBucketName();
                objectName = path;
            } else {
                bucketName = path.substring(0, bucketEndIndex);
                objectName = path.substring(bucketEndIndex + 1);
            }

            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception ex) {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            return connection.getInputStream();
        }
    }

    private LocalDate parseWearDate(String value) {
        if (StringUtils.isBlank(value)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            log.error("Parse wear date failed", e);
            return LocalDate.now();
        }
    }

    private Integer numberToInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Long numberToLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Double numberToDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private <T> PageInfo<T> buildPageInfo(List<T> records, int pageNum, int pageSize, long total) {
        Page<T> page = new Page<>(pageNum, pageSize);
        page.setTotal(total);
        page.addAll(records);
        return page.toPageInfo();
    }
}
