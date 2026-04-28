package com.ecru.outfit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.outfit.dto.response.HomeRecommendationLookDTO;
import com.ecru.outfit.dto.response.HomeRecommendationResponseDTO;
import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.ecru.outfit.entity.OutfitItem;
import com.ecru.outfit.entity.UserStyleArchive;
import com.ecru.outfit.mapper.OutfitAdviceRecordMapper;
import com.ecru.outfit.mapper.OutfitItemMapper;
import com.ecru.outfit.mapper.UserStyleArchiveMapper;
import com.ecru.user.entity.StyleTag;
import com.ecru.user.entity.UserStyleProfile;
import com.ecru.user.mapper.StyleTagMapper;
import com.ecru.user.mapper.UserStyleProfileMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeRecommendationService {

    private static final String HOME_OCCASION = "HOME_DAILY";
    private static final int MAX_LOOKS = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ClothingMapper clothingMapper;
    private final OutfitAdviceRecordMapper outfitAdviceRecordMapper;
    private final OutfitItemMapper outfitItemMapper;
    private final UserStyleArchiveMapper userStyleArchiveMapper;
    private final UserStyleProfileMapper userStyleProfileMapper;
    private final StyleTagMapper styleTagMapper;

    @Transactional
    public HomeRecommendationResponseDTO getRecommendations(Long userId,
                                                            boolean refresh,
                                                            String location,
                                                            Double temperature,
                                                            String weatherCondition) {
        if (!refresh) {
            List<OutfitAdviceRecord> existing = outfitAdviceRecordMapper.selectHomeRecommendations(userId, MAX_LOOKS);
            if (!existing.isEmpty()) {
                return buildResponse(existing);
            }
        }

        List<Clothing> clothings = clothingMapper.selectList(new LambdaQueryWrapper<Clothing>()
                .eq(Clothing::getUserId, userId)
                .eq(Clothing::getIsDeleted, false)
                .orderByDesc(Clothing::getFrequencyLevel)
                .orderByAsc(Clothing::getWearCount)
                .orderByDesc(Clothing::getUpdatedAt));

        if (clothings.isEmpty()) {
            HomeRecommendationResponseDTO empty = new HomeRecommendationResponseDTO();
            empty.setGeneratedAt(LocalDateTime.now());
            empty.setEmptyReason("衣橱里还没有可用于搭配的衣物。");
            return empty;
        }

        RecommendationContext context = buildContext(userId, location, temperature, weatherCondition);
        List<GeneratedLook> looks = generateLooks(clothings, context, refresh);
        if (looks.isEmpty()) {
            HomeRecommendationResponseDTO empty = new HomeRecommendationResponseDTO();
            empty.setGeneratedAt(LocalDateTime.now());
            empty.setEmptyReason("当前衣橱里暂时无法组成完整搭配，至少需要上装和下装，或一件连衣裙。");
            return empty;
        }

        List<OutfitAdviceRecord> existing = outfitAdviceRecordMapper.selectHomeRecommendations(userId, 20);
        if (!existing.isEmpty()) {
            List<Long> adviceIds = existing.stream().map(OutfitAdviceRecord::getId).toList();
            outfitItemMapper.deleteByOutfitAdviceIds(adviceIds);
            outfitAdviceRecordMapper.deleteHomeRecommendations(userId);
        }

        List<OutfitAdviceRecord> saved = persistLooks(userId, looks, context);
        return buildResponse(saved);
    }

    public HomeRecommendationLookDTO getRecommendationDetail(Long userId, Long recommendationId) {
        OutfitAdviceRecord record = outfitAdviceRecordMapper.selectById(recommendationId);
        if (record == null) {
            return null;
        }
        if (!userId.equals(record.getUserId())
                || !HOME_OCCASION.equals(record.getOccasion())
                || record.getInputType() == null
                || record.getInputType() != 3
                || Boolean.TRUE.equals(record.getIsDeleted())) {
            return null;
        }
        return toLookDTO(record);
    }

    private RecommendationContext buildContext(Long userId, String location, Double temperature, String weatherCondition) {
        RecommendationContext context = new RecommendationContext();
        context.location = StringUtils.hasText(location) ? location : null;
        context.temperature = temperature;
        context.weatherCondition = StringUtils.hasText(weatherCondition) ? weatherCondition : null;
        context.season = resolveSeason(temperature, LocalDateTime.now().getMonthValue());
        context.timeOfDay = resolveTimeOfDay(LocalDateTime.now().getHour());

        UserStyleArchive archive = userStyleArchiveMapper.selectByUserId(userId);
        if (archive != null) {
            context.preferredStyles.addAll(parseListField(archive.getPreferredStyles()));
            context.avoidedStyles.addAll(parseListField(archive.getAvoidedStyles()));
            context.preferredColors.addAll(parseListField(archive.getPreferredColors()));
            context.avoidedColors.addAll(parseListField(archive.getAvoidedColors()));
        }

        List<UserStyleProfile> preferenceProfiles = userStyleProfileMapper.selectTopPreferences(userId, 8);
        if (!preferenceProfiles.isEmpty()) {
            Map<Long, StyleTag> tagMap = styleTagMapper.selectBatchIds(preferenceProfiles.stream()
                            .map(UserStyleProfile::getStyleTagId)
                            .collect(Collectors.toSet()))
                    .stream()
                    .collect(Collectors.toMap(StyleTag::getId, item -> item));

            for (UserStyleProfile profile : preferenceProfiles) {
                StyleTag tag = tagMap.get(profile.getStyleTagId());
                if (tag == null || !StringUtils.hasText(tag.getName())) {
                    continue;
                }
                if (profile.getPreferenceScore() != null && profile.getPreferenceScore().compareTo(BigDecimal.ZERO) > 0) {
                    context.preferredStyles.add(tag.getName());
                }
                if (profile.getPreferenceScore() != null && profile.getPreferenceScore().compareTo(BigDecimal.ZERO) < 0) {
                    context.avoidedStyles.add(tag.getName());
                }
            }
        }

        return context;
    }

    private List<GeneratedLook> generateLooks(List<Clothing> clothings, RecommendationContext context) {
        return generateLooks(clothings, context, false);
    }

    private List<GeneratedLook> generateLooks(List<Clothing> clothings, RecommendationContext context, boolean shuffle) {
        List<Clothing> tops = new ArrayList<>();
        List<Clothing> bottoms = new ArrayList<>();
        List<Clothing> outers = new ArrayList<>();
        List<Clothing> dresses = new ArrayList<>();

        for (Clothing clothing : clothings) {
            switch (normalizeCategory(clothing.getCategory(), clothing.getSubCategory(), clothing.getName())) {
                case "TOP" -> tops.add(clothing);
                case "BOTTOM" -> bottoms.add(clothing);
                case "OUTER" -> outers.add(clothing);
                case "DRESS" -> dresses.add(clothing);
                default -> {
                }
            }
        }

        java.util.Random rng = shuffle ? new java.util.Random() : null;
        Comparator<Clothing> ranking = Comparator.comparingDouble(
                (Clothing item) -> scoreClothing(item, context) + (rng != null ? rng.nextDouble() * 1.2 - 0.6 : 0D)
        ).reversed();
        tops.sort(ranking);
        bottoms.sort(ranking);
        outers.sort(ranking);
        dresses.sort(ranking);

        List<GeneratedLook> candidates = new ArrayList<>();
        Set<String> signatures = new HashSet<>();

        for (Clothing dress : dresses.stream().limit(shuffle ? 6 : 4).toList()) {
            GeneratedLook dressLook = buildDressLook(dress, outers, context);
            if (dressLook != null && signatures.add(dressLook.signature())) {
                candidates.add(dressLook);
            }
        }

        for (Clothing top : tops.stream().limit(shuffle ? 8 : 6).toList()) {
            for (Clothing bottom : bottoms.stream().limit(shuffle ? 8 : 6).toList()) {
                GeneratedLook look = buildTwoPieceLook(top, bottom, outers, context);
                if (look != null && signatures.add(look.signature())) {
                    candidates.add(look);
                }
            }
        }

        return candidates.stream()
                .sorted(Comparator.comparingDouble((GeneratedLook item) -> item.score).reversed())
                .limit(MAX_LOOKS)
                .toList();
    }

    private GeneratedLook buildDressLook(Clothing dress, List<Clothing> outers, RecommendationContext context) {
        List<Clothing> items = new ArrayList<>();
        items.add(dress);
        Clothing outer = pickOuter(dress, null, outers, context);
        if (outer != null) {
            items.add(outer);
        }
        return composeLook(items, context);
    }

    private GeneratedLook buildTwoPieceLook(Clothing top, Clothing bottom, List<Clothing> outers, RecommendationContext context) {
        List<Clothing> items = new ArrayList<>();
        items.add(top);
        items.add(bottom);
        Clothing outer = pickOuter(top, bottom, outers, context);
        if (outer != null) {
            items.add(outer);
        }
        return composeLook(items, context);
    }

    private Clothing pickOuter(Clothing top, Clothing bottom, List<Clothing> outers, RecommendationContext context) {
        if (outers.isEmpty()) {
            return null;
        }
        if (context.temperature != null && context.temperature >= 24) {
            return null;
        }
        return outers.stream()
                .filter(item -> seasonFits(item, context.season))
                .max(Comparator.comparingDouble(item -> scoreClothing(item, context) + colorHarmony(item, top, bottom)))
                .orElse(null);
    }

    private GeneratedLook composeLook(List<Clothing> items, RecommendationContext context) {
        if (items.isEmpty()) {
            return null;
        }

        boolean hasTop = items.stream().anyMatch(item -> "TOP".equals(normalizeCategory(item.getCategory(), item.getSubCategory(), item.getName())));
        boolean hasBottom = items.stream().anyMatch(item -> "BOTTOM".equals(normalizeCategory(item.getCategory(), item.getSubCategory(), item.getName())));
        boolean hasDress = items.stream().anyMatch(item -> "DRESS".equals(normalizeCategory(item.getCategory(), item.getSubCategory(), item.getName())));
        if (!hasDress && !(hasTop && hasBottom)) {
            return null;
        }

        GeneratedLook look = new GeneratedLook();
        look.items = items;
        look.score = items.stream().mapToDouble(item -> scoreClothing(item, context)).sum() + comboHarmony(items);
        look.tags = buildTags(items, context);
        look.mood = buildMood(look.tags, context);
        look.title = look.mood + "穿搭";
        look.note = buildNote(items, context);
        look.reasoning = buildReasoning(items, context);
        look.palette = items.stream()
                .map(this::resolvePaletteColor)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();
        return look;
    }

    private List<OutfitAdviceRecord> persistLooks(Long userId, List<GeneratedLook> looks, RecommendationContext context) {
        LocalDateTime now = LocalDateTime.now();
        List<OutfitAdviceRecord> saved = new ArrayList<>();

        for (GeneratedLook look : looks) {
            OutfitAdviceRecord record = new OutfitAdviceRecord();
            record.setUserId(userId);
            record.setInputType(3);
            record.setLocation(context.location);
            record.setTemperature(context.temperature == null ? null : BigDecimal.valueOf(context.temperature));
            record.setWeatherCondition(context.weatherCondition);
            record.setSeason(context.season);
            record.setTimeOfDay(context.timeOfDay);
            record.setOccasion(HOME_OCCASION);
            record.setOutfitName(look.title);
            record.setOutfitDescription(look.note);
            record.setReasoning(look.reasoning);
            record.setFashionSuggestions("优先从现有衣橱直接组合，减少重复购买。");
            record.setSuitabilityScore(BigDecimal.valueOf(Math.min(0.99D, Math.max(0.55D, look.score / 10D))));
            record.setIsFavorite(false);
            record.setIsDeleted(false);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            outfitAdviceRecordMapper.insert(record);

            List<OutfitItem> itemEntities = new ArrayList<>();
            for (int i = 0; i < look.items.size(); i++) {
                Clothing clothing = look.items.get(i);
                OutfitItem item = new OutfitItem();
                item.setOutfitAdviceId(record.getId());
                item.setClothingId(clothing.getId());
                item.setItemName(clothing.getName());
                item.setItemCategory(clothing.getCategory());
                item.setItemColor(firstNonBlank(clothing.getPrimaryColor(), clothing.getSecondaryColor(), "未标注"));
                item.setItemImageUrl(clothing.getImageUrl());
                item.setIsRecommended(false);
                item.setReason(buildItemReason(clothing, context));
                item.setSortOrder(i);
                item.setCreatedAt(now);
                itemEntities.add(item);
            }
            outfitItemMapper.batchInsert(itemEntities);
            saved.add(record);
        }

        return saved;
    }

    private HomeRecommendationResponseDTO buildResponse(List<OutfitAdviceRecord> records) {
        HomeRecommendationResponseDTO response = new HomeRecommendationResponseDTO();
        response.setGeneratedAt(records.isEmpty() ? LocalDateTime.now() : records.get(0).getCreatedAt());
        response.setLooks(records.stream().map(this::toLookDTO).toList());
        return response;
    }

    private HomeRecommendationLookDTO toLookDTO(OutfitAdviceRecord record) {
        List<OutfitItem> items = outfitItemMapper.selectByOutfitAdviceId(record.getId());
        Map<Long, Clothing> clothingMap = clothingMapper.selectBatchIds(items.stream()
                        .map(OutfitItem::getClothingId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Clothing::getId, item -> item));
        HomeRecommendationLookDTO dto = new HomeRecommendationLookDTO();
        dto.setId(record.getId());
        dto.setMood(buildMood(buildTagsFromItems(items, record.getSeason()), null));
        dto.setTitle(firstNonBlank(record.getOutfitName(), "今日穿搭"));
        dto.setNote(firstNonBlank(record.getOutfitDescription(), record.getReasoning(), "基于当前衣橱生成的一组推荐。"));
        dto.setTags(buildTagsFromItems(items, record.getSeason()));
        dto.setPalette(items.stream().map(this::resolvePaletteColor).filter(StringUtils::hasText).distinct().limit(3).toList());
        dto.setItems(items.stream().map(item -> toLookItemDTO(item, clothingMap.get(item.getClothingId()))).toList());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    private HomeRecommendationLookDTO.LookItem toLookItemDTO(OutfitItem item, Clothing clothing) {
        HomeRecommendationLookDTO.LookItem dto = new HomeRecommendationLookDTO.LookItem();
        dto.setClothingId(item.getClothingId());
        dto.setName(item.getItemName());
        dto.setCategory(item.getItemCategory());
        dto.setColor(item.getItemColor());
        dto.setImageUrl(firstNonBlank(item.getItemImageUrl(), clothing != null ? clothing.getImageUrl() : null));
        dto.setReason(item.getReason());
        dto.setFrequencyLevel(clothing != null ? clothing.getFrequencyLevel() : null);
        dto.setWearCount(clothing != null ? clothing.getWearCount() : null);
        dto.setSourceType(clothing != null ? clothing.getSourceType() : null);
        dto.setSourcePlatform(clothing != null ? clothing.getSourcePlatform() : null);
        dto.setFromWardrobe(clothing != null || item.getClothingId() != null);
        return dto;
    }

    private List<String> buildTags(List<Clothing> items, RecommendationContext context) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(context.season)) {
            tags.add(context.season);
        }
        items.stream()
                .flatMap(item -> parseListField(item.getStyleTags()).stream())
                .limit(3)
                .forEach(tags::add);
        items.stream()
                .map(item -> firstNonBlank(item.getPrimaryColor(), item.getSecondaryColor(), null))
                .filter(StringUtils::hasText)
                .limit(2)
                .forEach(tags::add);
        return new ArrayList<>(tags).stream().limit(4).toList();
    }

    private List<String> buildTagsFromItems(List<OutfitItem> items, String season) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (StringUtils.hasText(season)) {
            tags.add(season);
        }
        items.stream()
                .map(OutfitItem::getItemCategory)
                .filter(StringUtils::hasText)
                .limit(2)
                .forEach(tags::add);
        items.stream()
                .map(OutfitItem::getItemColor)
                .filter(StringUtils::hasText)
                .limit(2)
                .forEach(tags::add);
        return new ArrayList<>(tags);
    }

    private String buildMood(List<String> tags, RecommendationContext context) {
        if (!CollectionUtils.isEmpty(tags)) {
            return tags.get(0);
        }
        if (context != null && StringUtils.hasText(context.season)) {
            return context.season;
        }
        return "安静";
    }

    private String buildNote(List<Clothing> items, RecommendationContext context) {
        String names = items.stream()
                .map(Clothing::getName)
                .map(this::trimName)
                .limit(3)
                .collect(Collectors.joining(" + "));
        if (context.temperature != null) {
            return names + "，适合当前约 " + Math.round(context.temperature) + "°C 的体感。";
        }
        return names + "，直接从现有衣橱组合出一套完整搭配。";
    }

    private String buildReasoning(List<Clothing> items, RecommendationContext context) {
        List<String> reasons = new ArrayList<>();
        if (StringUtils.hasText(context.season)) {
            reasons.add("季节匹配");
        }
        if (!context.preferredStyles.isEmpty()) {
            reasons.add("贴合你的风格偏好");
        }
        reasons.add("优先复用高频单品");
        return String.join("，", reasons);
    }

    private String buildItemReason(Clothing clothing, RecommendationContext context) {
        List<String> reasons = new ArrayList<>();
        if (seasonFits(clothing, context.season)) {
            reasons.add("季节合适");
        }
        if (styleMatches(clothing, context.preferredStyles)) {
            reasons.add("风格贴合");
        }
        if (colorMatches(clothing, context.preferredColors)) {
            reasons.add("颜色偏好吻合");
        }
        if (reasons.isEmpty()) {
            reasons.add("适合作为当前搭配的一部分");
        }
        return String.join(" / ", reasons);
    }

    private double scoreClothing(Clothing clothing, RecommendationContext context) {
        double score = 0D;
        score += safeNumber(clothing.getFrequencyLevel()) * 0.9D;
        score += Math.max(0D, 4D - safeNumber(clothing.getWearCount()) * 0.12D);
        if (seasonFits(clothing, context.season)) {
            score += 1.2D;
        }
        if (styleMatches(clothing, context.preferredStyles)) {
            score += 1.4D;
        }
        if (styleMatches(clothing, context.avoidedStyles)) {
            score -= 1.8D;
        }
        if (colorMatches(clothing, context.preferredColors)) {
            score += 0.8D;
        }
        if (colorMatches(clothing, context.avoidedColors)) {
            score -= 1.2D;
        }
        return score;
    }

    private double comboHarmony(List<Clothing> items) {
        if (items.size() < 2) {
            return 0D;
        }
        double score = 0D;
        for (int i = 0; i < items.size() - 1; i++) {
            score += colorHarmony(items.get(i), items.get(i + 1), null);
        }
        return score;
    }

    private double colorHarmony(Clothing primary, Clothing secondary, Clothing fallback) {
        String left = normalizeColorName(firstNonBlank(primary != null ? primary.getPrimaryColor() : null, primary != null ? primary.getSecondaryColor() : null, null));
        String right = normalizeColorName(firstNonBlank(secondary != null ? secondary.getPrimaryColor() : null, secondary != null ? secondary.getSecondaryColor() : null,
                fallback != null ? firstNonBlank(fallback.getPrimaryColor(), fallback.getSecondaryColor(), null) : null));
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0.3D;
        }
        if (left.equals(right)) {
            return 1.1D;
        }
        if (isNeutral(left) || isNeutral(right)) {
            return 0.8D;
        }
        if ((left.contains("blue") && right.contains("white")) || (left.contains("white") && right.contains("blue"))) {
            return 0.9D;
        }
        return 0.4D;
    }

    private boolean seasonFits(Clothing clothing, String season) {
        if (!StringUtils.hasText(season)) {
            return true;
        }
        List<String> tags = parseListField(clothing.getSeasonTags());
        if (tags.isEmpty()) {
            return true;
        }
        String normalizedSeason = normalizeSeasonTag(season);
        return tags.stream().map(this::normalizeSeasonTag).anyMatch(normalizedSeason::equals);
    }

    private boolean styleMatches(Clothing clothing, Collection<String> targetStyles) {
        if (CollectionUtils.isEmpty(targetStyles)) {
            return false;
        }
        List<String> clothingStyles = parseListField(clothing.getStyleTags()).stream()
                .map(this::normalizeKeyword)
                .toList();
        return targetStyles.stream()
                .map(this::normalizeKeyword)
                .anyMatch(clothingStyles::contains);
    }

    private boolean colorMatches(Clothing clothing, Collection<String> targetColors) {
        if (CollectionUtils.isEmpty(targetColors)) {
            return false;
        }
        String color = normalizeKeyword(firstNonBlank(clothing.getPrimaryColor(), clothing.getSecondaryColor(), ""));
        return targetColors.stream().map(this::normalizeKeyword).anyMatch(color::contains);
    }

    private List<String> parseListField(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            if (raw.trim().startsWith("[")) {
                return OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() {
                }).stream().filter(StringUtils::hasText).toList();
            }
        } catch (Exception ex) {
            log.debug("Parse list field failed, fallback to split: {}", raw, ex);
        }
        return List.of(raw.split("[,，/]")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalizeCategory(String category, String subCategory, String name) {
        String value = normalizeKeyword(firstNonBlank(category, subCategory, name));
        if (containsAny(value, "dress", "连衣裙", "裙装")) {
            return "DRESS";
        }
        if (containsAny(value, "outer", "外套", "coat", "jacket", "西装")) {
            return "OUTER";
        }
        if (containsAny(value, "bottom", "下装", "裤", "skirt", "trousers", "jeans", "shorts", "裙")) {
            return "BOTTOM";
        }
        if (containsAny(value, "top", "上装", "上衣", "shirt", "t恤", "tee", "hoodie", "sweater", "衬衫", "毛衣", "卫衣")) {
            return "TOP";
        }
        return "OTHER";
    }

    private String resolveSeason(Double temperature, int month) {
        if (temperature != null) {
            if (temperature >= 26) {
                return "夏";
            }
            if (temperature >= 18) {
                return "春";
            }
            if (temperature >= 10) {
                return "秋";
            }
            return "冬";
        }
        if (month >= 3 && month <= 5) {
            return "春";
        }
        if (month >= 6 && month <= 8) {
            return "夏";
        }
        if (month >= 9 && month <= 11) {
            return "秋";
        }
        return "冬";
    }

    private String resolveTimeOfDay(int hour) {
        if (hour < 11) {
            return "上午";
        }
        if (hour < 17) {
            return "下午";
        }
        return "晚上";
    }

    private String normalizeSeasonTag(String value) {
        String normalized = normalizeKeyword(value);
        if (containsAny(normalized, "spring", "春")) {
            return "春";
        }
        if (containsAny(normalized, "summer", "夏")) {
            return "夏";
        }
        if (containsAny(normalized, "autumn", "fall", "秋")) {
            return "秋";
        }
        if (containsAny(normalized, "winter", "冬")) {
            return "冬";
        }
        return normalized;
    }

    private String normalizeKeyword(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeColorName(String value) {
        String normalized = normalizeKeyword(value);
        if (normalized.contains("白")) {
            return "white";
        }
        if (normalized.contains("黑")) {
            return "black";
        }
        if (normalized.contains("灰")) {
            return "gray";
        }
        if (normalized.contains("蓝") || normalized.contains("navy")) {
            return "blue";
        }
        if (normalized.contains("棕") || normalized.contains("卡其") || normalized.contains("米") || normalized.contains("杏")) {
            return "beige";
        }
        if (normalized.contains("粉")) {
            return "pink";
        }
        return normalized;
    }

    private boolean isNeutral(String value) {
        return containsAny(value, "white", "black", "gray", "beige", "blue");
    }

    private String resolvePaletteColor(Clothing clothing) {
        if (StringUtils.hasText(clothing.getPrimaryColorHex())) {
            return clothing.getPrimaryColorHex();
        }
        return mapColorNameToHex(firstNonBlank(clothing.getPrimaryColor(), clothing.getSecondaryColor(), null));
    }

    private String resolvePaletteColor(OutfitItem item) {
        return mapColorNameToHex(item.getItemColor());
    }

    private String mapColorNameToHex(String colorName) {
        String normalized = normalizeColorName(colorName);
        return switch (normalized) {
            case "white" -> "#F4EFE6";
            case "black" -> "#3A3836";
            case "gray" -> "#B8B4AE";
            case "blue" -> "#7D93AE";
            case "beige" -> "#D7C1A1";
            case "pink" -> "#DAB2B6";
            default -> "#D7C1A1";
        };
    }

    private String trimName(String name) {
        if (!StringUtils.hasText(name)) {
            return "单品";
        }
        return name.length() <= 14 ? name : name.substring(0, 14) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean containsAny(String raw, String... keywords) {
        if (!StringUtils.hasText(raw)) {
            return false;
        }
        for (String keyword : keywords) {
            if (raw.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private static class RecommendationContext {
        private String location;
        private Double temperature;
        private String weatherCondition;
        private String season;
        private String timeOfDay;
        private final Set<String> preferredStyles = new LinkedHashSet<>();
        private final Set<String> avoidedStyles = new LinkedHashSet<>();
        private final Set<String> preferredColors = new LinkedHashSet<>();
        private final Set<String> avoidedColors = new LinkedHashSet<>();
    }

    private static class GeneratedLook {
        private List<Clothing> items = new ArrayList<>();
        private double score;
        private String mood;
        private String title;
        private String note;
        private String reasoning;
        private List<String> tags = new ArrayList<>();
        private List<String> palette = new ArrayList<>();

        private String signature() {
            return items.stream().map(item -> String.valueOf(item.getId())).sorted().collect(Collectors.joining("-"));
        }
    }
}
