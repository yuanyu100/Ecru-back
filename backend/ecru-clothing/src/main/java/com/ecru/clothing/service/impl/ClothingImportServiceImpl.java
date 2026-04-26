package com.ecru.clothing.service.impl;

import com.ecru.clothing.dto.request.CreateClothingRequest;
import com.ecru.clothing.dto.request.PinduoduoImportCommitRequest;
import com.ecru.clothing.dto.request.PinduoduoImportItemRequest;
import com.ecru.clothing.dto.request.PinduoduoImportPreviewRequest;
import com.ecru.clothing.dto.response.ClothingDetailVO;
import com.ecru.clothing.dto.response.PinduoduoImportItemVO;
import com.ecru.clothing.dto.response.PinduoduoImportPreviewVO;
import com.ecru.clothing.dto.response.PinduoduoImportResultVO;
import com.ecru.clothing.entity.Clothing;
import com.ecru.clothing.mapper.ClothingMapper;
import com.ecru.clothing.service.ClothingImportService;
import com.ecru.clothing.service.ClothingService;
import com.ecru.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClothingImportServiceImpl implements ClothingImportService {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?is)<script[^>]*>(.*?)</script>");
    private static final Pattern JSON_PARSE_PATTERN = Pattern.compile("JSON\\.parse\\((['\"])(.*?)\\1\\)", Pattern.DOTALL);
    private static final Pattern SIZE_PATTERN = Pattern.compile("(?i)\\b(XXXL|XXL|XL|XS|S|M|L|均码|均碼|34|35|36|37|38|39|40|41|42|43|44|45)\\b");

    private static final List<String> NAME_KEYS = Arrays.asList(
            "goodsName", "goods_name", "itemName", "item_name", "productName", "product_name",
            "goodsTitle", "goods_title", "title", "name"
    );
    private static final List<String> IMAGE_KEYS = Arrays.asList(
            "goodsImg", "goods_img", "goodsImage", "goods_image", "goodsImageUrl", "goods_image_url",
            "thumbUrl", "thumb_url", "hdThumbUrl", "hd_thumb_url", "imageUrl", "image_url",
            "coverUrl", "cover_url", "image", "thumbnail"
    );
    private static final List<String> LINK_KEYS = Arrays.asList(
            "goodsLink", "goods_link", "goodsUrl", "goods_url", "productUrl", "product_url",
            "detailUrl", "detail_url", "linkUrl", "link_url", "url", "pageUrl", "page_url"
    );
    private static final List<String> ORDER_TIME_KEYS = Arrays.asList(
            "orderTime", "order_time", "createTime", "create_time", "createdAt", "created_at",
            "payTime", "pay_time", "orderDate", "order_date"
    );
    private static final List<String> PRICE_KEYS = Arrays.asList(
            "price", "goodsPrice", "goods_price", "payAmount", "pay_amount", "amount",
            "orderAmount", "order_amount", "totalAmount", "total_amount"
    );
    private static final List<String> SHOP_KEYS = Arrays.asList(
            "mallName", "mall_name", "shopName", "shop_name", "storeName", "store_name", "merchantName"
    );
    private static final List<String> SKU_KEYS = Arrays.asList(
            "skuText", "sku_text", "skuSpec", "sku_spec", "goodsSpec", "goods_spec",
            "spec", "specs", "specDesc", "spec_desc", "sizeDesc", "size_desc"
    );
    private static final List<String> ORDER_ID_KEYS = Arrays.asList(
            "orderSn", "order_sn", "orderId", "order_id", "groupOrderId", "group_order_id"
    );
    private static final List<String> BRAND_KEYS = Arrays.asList(
            "brand", "brandName", "brand_name"
    );

    private static final String[] CLOTHING_KEYWORDS = {
            "衣", "衣服", "上衣", "下装", "裙", "裤", "外套", "大衣", "风衣", "夹克", "西装", "卫衣", "衬衫", "T恤", "t恤",
            "短袖", "长袖", "打底衫", "打底", "底衫", "毛衣", "针织", "羽绒", "棉服", "马甲", "背心", "吊带", "牛仔", "阔腿裤", "短裤", "长裤",
            "半裙", "半身裙", "连衣裙", "西裤", "打底裤", "睡衣", "家居服", "鞋", "靴", "包", "帽",
            "围巾", "袜", "bra", "shirt", "pants", "jeans", "coat", "jacket", "dress", "skirt",
            "hoodie", "sweater", "cardigan", "shoes", "boots", "bag", "hat", "sock"
    };

    private final ClothingService clothingService;
    private final ClothingMapper clothingMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PinduoduoImportPreviewVO previewPinduoduoImport(Long userId, PinduoduoImportPreviewRequest request) {
        if (request == null || StringUtils.isBlank(request.getPayload())) {
            throw new BusinessException("导入内容不能为空");
        }

        String mode = detectMode(request.getMode(), request.getPayload());
        List<PinduoduoImportItemVO> parsedItems = parseItems(mode, request.getPayload(), Boolean.TRUE.equals(request.getOnlyClothing()));
        parsedItems.forEach(item -> item.setDuplicate(isDuplicate(userId, item)));

        PinduoduoImportPreviewVO response = new PinduoduoImportPreviewVO();
        response.setMode(mode);
        response.setTotalDetected(parsedItems.size());
        response.setMatchedCount((int) parsedItems.stream().filter(item -> !Boolean.TRUE.equals(item.getDuplicate())).count());
        response.setItems(parsedItems);
        return response;
    }

    @Override
    public PinduoduoImportResultVO importPinduoduoItems(Long userId, PinduoduoImportCommitRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("至少选择一条订单商品再导入");
        }

        boolean autoRecognize = Boolean.TRUE.equals(request.getAutoRecognize());
        boolean skipExisting = !Boolean.FALSE.equals(request.getSkipExisting());

        List<ClothingDetailVO> createdItems = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (PinduoduoImportItemRequest itemRequest : request.getItems()) {
            PinduoduoImportItemVO item = normalizeItem(toItemVO(itemRequest), "structured_json");
            if (StringUtils.isBlank(item.getProductName())) {
                failed++;
                messages.add("跳过 1 条记录：商品名称为空");
                continue;
            }

            if (skipExisting && isDuplicate(userId, item)) {
                skipped++;
                messages.add("已跳过重复商品：" + item.getProductName());
                continue;
            }

            try {
                CreateClothingRequest createRequest = toCreateRequest(item, autoRecognize);
                ClothingDetailVO createdDetail = clothingService.createClothing(userId, createRequest);

                Clothing update = new Clothing();
                update.setId(createdDetail.getId());
                update.setSourceType("import");
                update.setSourcePlatform(StringUtils.defaultIfBlank(item.getSource(), "pinduoduo"));
                update.setSourceOrderId(item.getOrderId());
                update.setSourceShopName(item.getShopName());
                update.setSourceSkuText(item.getSkuText());
                clothingMapper.updateById(update);

                createdItems.add(clothingService.getClothingDetail(userId, createdDetail.getId()));
                created++;
            } catch (Exception e) {
                failed++;
                log.error("Import pinduoduo item failed: {}", item.getProductName(), e);
                messages.add("导入失败：" + item.getProductName());
            }
        }

        PinduoduoImportResultVO result = new PinduoduoImportResultVO();
        result.setCreated(created);
        result.setSkipped(skipped);
        result.setFailed(failed);
        result.setCreatedItems(createdItems);
        result.setMessages(messages);
        return result;
    }

    private List<PinduoduoImportItemVO> parseItems(String mode, String payload, boolean onlyClothing) {
        LinkedHashMap<String, PinduoduoImportItemVO> items = new LinkedHashMap<>();
        String trimmedPayload = payload.trim();

        switch (mode) {
            case "structured_json", "auto" -> collectFromJsonText(trimmedPayload, items, "pinduoduo");
            case "har" -> collectFromHar(trimmedPayload, items);
            case "html" -> collectFromHtml(trimmedPayload, items);
            default -> throw new BusinessException("暂不支持的导入模式：" + mode);
        }

        List<PinduoduoImportItemVO> result = new ArrayList<>(items.values());
        if (onlyClothing) {
            List<PinduoduoImportItemVO> clothingOnly = result.stream().filter(this::looksLikeClothing).toList();
            if (!clothingOnly.isEmpty()) {
                result = clothingOnly;
            }
        }
        return result;
    }

    private void collectFromHar(String payload, Map<String, PinduoduoImportItemVO> items) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entries = root.path("log").path("entries");
            if (!entries.isArray()) {
                collectFromJsonNode(root, new ImportContext(), items, "pinduoduo");
                return;
            }

            for (JsonNode entry : entries) {
                JsonNode textNode = entry.path("response").path("content").path("text");
                if (textNode.isTextual()) {
                    String content = textNode.asText();
                    if (StringUtils.isBlank(content)) {
                        continue;
                    }
                    if (looksLikeJson(content)) {
                        collectFromJsonText(content, items, "pinduoduo");
                    } else if (looksLikeHtml(content)) {
                        collectFromHtml(content, items);
                    }
                }
            }
        } catch (Exception e) {
            throw new BusinessException("HAR 内容解析失败，请确认导出的是标准 HAR JSON");
        }
    }

    private void collectFromHtml(String html, Map<String, PinduoduoImportItemVO> items) {
        Matcher scriptMatcher = SCRIPT_PATTERN.matcher(html);
        while (scriptMatcher.find()) {
            String scriptContent = scriptMatcher.group(1);
            extractJsonPayloads(scriptContent).forEach(json -> collectFromJsonText(json, items, "pinduoduo"));

            Matcher parseMatcher = JSON_PARSE_PATTERN.matcher(scriptContent);
            while (parseMatcher.find()) {
                String unescaped = parseMatcher.group(2)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\/", "/");
                if (looksLikeJson(unescaped)) {
                    collectFromJsonText(unescaped, items, "pinduoduo");
                }
            }
        }

        extractJsonPayloads(html).forEach(json -> collectFromJsonText(json, items, "pinduoduo"));
    }

    private void collectFromJsonText(String payload, Map<String, PinduoduoImportItemVO> items, String source) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode directItems = root.path("items");
            if (directItems.isArray()) {
                for (JsonNode itemNode : directItems) {
                    PinduoduoImportItemVO directCandidate = extractStructuredItem(itemNode, source);
                    if (directCandidate != null) {
                        PinduoduoImportItemVO normalized = normalizeItem(directCandidate, source);
                        items.putIfAbsent(buildFingerprint(normalized), normalized);
                    }
                }
            }
            collectFromJsonNode(root, new ImportContext(), items, source);
        } catch (Exception e) {
            log.debug("Skip non-json payload fragment");
        }
    }

    private PinduoduoImportItemVO extractStructuredItem(JsonNode node, String source) {
        if (node == null || !node.isObject()) {
            return null;
        }

        PinduoduoImportItemVO item = new PinduoduoImportItemVO();
        item.setProductName(firstText(node, NAME_KEYS));
        item.setProductUrl(firstText(node, LINK_KEYS));
        item.setImageUrl(firstText(node, IMAGE_KEYS));
        item.setOrderTime(firstText(node, ORDER_TIME_KEYS));
        item.setPrice(firstNumber(node, PRICE_KEYS));
        item.setShopName(firstText(node, SHOP_KEYS));
        item.setSkuText(firstText(node, SKU_KEYS));
        item.setOrderId(firstText(node, ORDER_ID_KEYS));
        item.setBrand(firstText(node, BRAND_KEYS));
        item.setSource(source);

        if (StringUtils.isBlank(item.getProductName())) {
            return null;
        }
        return item;
    }

    private void collectFromJsonNode(JsonNode node, ImportContext context, Map<String, PinduoduoImportItemVO> items, String source) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectFromJsonNode(child, context, items, source);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }

        ImportContext nextContext = context.merge(
                firstText(node, ORDER_TIME_KEYS),
                firstText(node, ORDER_ID_KEYS),
                firstText(node, SHOP_KEYS),
                firstText(node, BRAND_KEYS)
        );

        PinduoduoImportItemVO candidate = extractCandidate(node, nextContext, source);
        if (candidate != null) {
            PinduoduoImportItemVO normalized = normalizeItem(candidate, source);
            items.putIfAbsent(buildFingerprint(normalized), normalized);
        }

        node.fields().forEachRemaining(entry -> collectFromJsonNode(entry.getValue(), nextContext, items, source));
    }

    private PinduoduoImportItemVO extractCandidate(JsonNode node, ImportContext context, String source) {
        String productName = firstText(node, NAME_KEYS);
        String imageUrl = firstText(node, IMAGE_KEYS);
        String productUrl = firstText(node, LINK_KEYS);
        Double price = firstNumber(node, PRICE_KEYS);
        String skuText = firstText(node, SKU_KEYS);

        boolean enoughSignals = StringUtils.isNotBlank(productName)
                && (StringUtils.isNotBlank(imageUrl) || StringUtils.isNotBlank(productUrl)
                || StringUtils.isNotBlank(skuText) || price != null);
        if (!enoughSignals) {
            return null;
        }

        PinduoduoImportItemVO item = new PinduoduoImportItemVO();
        item.setProductName(productName);
        item.setImageUrl(imageUrl);
        item.setProductUrl(productUrl);
        item.setPrice(price);
        item.setSkuText(skuText);
        item.setOrderTime(StringUtils.defaultIfBlank(firstText(node, ORDER_TIME_KEYS), context.orderTime()));
        item.setOrderId(StringUtils.defaultIfBlank(firstText(node, ORDER_ID_KEYS), context.orderId()));
        item.setShopName(StringUtils.defaultIfBlank(firstText(node, SHOP_KEYS), context.shopName()));
        item.setBrand(StringUtils.defaultIfBlank(firstText(node, BRAND_KEYS), context.brand()));
        item.setSource(source);
        return item;
    }

    private PinduoduoImportItemVO normalizeItem(PinduoduoImportItemVO item, String source) {
        PinduoduoImportItemVO normalized = new PinduoduoImportItemVO();
        normalized.setProductName(cleanText(item.getProductName()));
        normalized.setProductUrl(cleanUrl(item.getProductUrl()));
        normalized.setImageUrl(cleanUrl(item.getImageUrl()));
        normalized.setOrderTime(normalizeOrderTime(item.getOrderTime()));
        normalized.setPrice(item.getPrice());
        normalized.setShopName(cleanText(item.getShopName()));
        normalized.setSkuText(cleanText(item.getSkuText()));
        normalized.setOrderId(cleanText(item.getOrderId()));
        normalized.setBrand(cleanText(item.getBrand()));
        normalized.setSource(source);

        String category = StringUtils.defaultIfBlank(cleanText(item.getCategory()), inferCategory(normalized.getProductName()));
        normalized.setCategory(category);
        normalized.setMaterial(StringUtils.defaultIfBlank(cleanText(item.getMaterial()), inferMaterial(normalized.getProductName(), normalized.getSkuText())));
        normalized.setSize(StringUtils.defaultIfBlank(cleanText(item.getSize()), inferSize(normalized.getSkuText(), normalized.getProductName())));
        return normalized;
    }

    private CreateClothingRequest toCreateRequest(PinduoduoImportItemVO item, boolean autoRecognize) {
        CreateClothingRequest request = new CreateClothingRequest();
        request.setName(item.getProductName());
        request.setCategory(item.getCategory());
        request.setMaterial(item.getMaterial());
        request.setSize(item.getSize());
        request.setImageUrl(item.getImageUrl());
        request.setPurchaseDate(toPurchaseDate(item.getOrderTime()));
        request.setPurchaseLink(item.getProductUrl());
        request.setPurchasePrice(item.getPrice());
        request.setBrand(item.getBrand());
        request.setSourcePlatform("pinduoduo");
        request.setSourceOrderId(item.getOrderId());
        request.setSourceShopName(item.getShopName());
        request.setSourceSkuText(item.getSkuText());
        request.setAutoRecognize(autoRecognize && StringUtils.isNotBlank(item.getImageUrl()));
        return request;
    }

    private boolean isDuplicate(Long userId, PinduoduoImportItemVO item) {
        if (StringUtils.isNotBlank(item.getProductUrl())
                && clothingMapper.selectPossibleDuplicate(userId, item.getProductUrl(), null, null) != null) {
            return true;
        }
        if (StringUtils.isNotBlank(item.getOrderId())
                && clothingMapper.selectPossibleDuplicate(userId, null, item.getOrderId(), null) != null) {
            return true;
        }
        return StringUtils.isNotBlank(item.getImageUrl())
                && clothingMapper.selectPossibleDuplicate(userId, null, null, item.getImageUrl()) != null;
    }

    private PinduoduoImportItemVO toItemVO(PinduoduoImportItemRequest request) {
        PinduoduoImportItemVO item = new PinduoduoImportItemVO();
        item.setProductName(request.getProductName());
        item.setProductUrl(request.getProductUrl());
        item.setImageUrl(request.getImageUrl());
        item.setOrderTime(request.getOrderTime());
        item.setPrice(request.getPrice());
        item.setShopName(request.getShopName());
        item.setSkuText(request.getSkuText());
        item.setOrderId(request.getOrderId());
        item.setBrand(request.getBrand());
        item.setCategory(request.getCategory());
        item.setMaterial(request.getMaterial());
        item.setSize(request.getSize());
        item.setSource("pinduoduo");
        return item;
    }

    private String detectMode(String rawMode, String payload) {
        String mode = StringUtils.defaultIfBlank(rawMode, "auto").trim().toLowerCase(Locale.ROOT);
        if (!"auto".equals(mode)) {
            return mode;
        }

        String trimmed = payload.trim();
        if (looksLikeHtml(trimmed)) {
            return "html";
        }
        if (trimmed.startsWith("{") && trimmed.contains("\"log\"") && trimmed.contains("\"entries\"")) {
            return "har";
        }
        return "structured_json";
    }

    private boolean looksLikeClothing(PinduoduoImportItemVO item) {
        String haystack = (StringUtils.defaultString(item.getProductName()) + " " + StringUtils.defaultString(item.getSkuText()))
                .toLowerCase(Locale.ROOT);
        for (String keyword : CLOTHING_KEYWORDS) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String inferCategory(String productName) {
        String value = StringUtils.defaultString(productName).toLowerCase(Locale.ROOT);
        if (containsAny(value, "鞋", "靴", "shoes", "boots")) {
            return "鞋履";
        }
        if (containsAny(value, "包", "bag")) {
            return "包袋";
        }
        if (containsAny(value, "帽", "围巾", "袜", "配饰", "hat", "sock")) {
            return "配饰";
        }
        if (containsAny(value, "裙", "dress", "skirt")) {
            return "裙装";
        }
        if (containsAny(value, "裤", "jeans", "pants", "shorts")) {
            return "下装";
        }
        if (containsAny(value, "外套", "大衣", "风衣", "夹克", "coat", "jacket")) {
            return "外套";
        }
        if (containsAny(value, "衬衫", "卫衣", "毛衣", "针织", "背心", "吊带", "shirt", "hoodie", "sweater")) {
            return "上装";
        }
        return "上装";
    }

    private String inferMaterial(String productName, String skuText) {
        String value = (StringUtils.defaultString(productName) + " " + StringUtils.defaultString(skuText)).toLowerCase(Locale.ROOT);
        if (containsAny(value, "羊毛", "wool")) {
            return "羊毛";
        }
        if (containsAny(value, "牛仔", "denim")) {
            return "牛仔";
        }
        if (containsAny(value, "棉", "cotton")) {
            return "棉";
        }
        if (containsAny(value, "麻", "linen")) {
            return "麻";
        }
        if (containsAny(value, "皮", "leather")) {
            return "皮革";
        }
        if (containsAny(value, "丝", "silk")) {
            return "丝";
        }
        return null;
    }

    private String inferSize(String skuText, String productName) {
        String value = StringUtils.defaultIfBlank(skuText, productName);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Matcher matcher = SIZE_PATTERN.matcher(value);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : null;
    }

    private String toPurchaseDate(String orderTime) {
        if (StringUtils.isBlank(orderTime)) {
            return null;
        }
        String value = orderTime.trim();
        if (value.matches("\\d{13}")) {
            return Instant.ofEpochMilli(Long.parseLong(value)).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }
        if (value.matches("\\d{10}")) {
            return Instant.ofEpochSecond(Long.parseLong(value)).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        }

        List<String> patterns = Arrays.asList(
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX"
        );
        for (String pattern : patterns) {
            try {
                if (pattern.contains("H")) {
                    return java.time.LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern)).toLocalDate().toString();
                }
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern)).toString();
            } catch (DateTimeParseException ignored) {
                // Keep trying.
            }
        }
        return value.length() >= 10 ? value.substring(0, 10).replace('/', '-') : null;
    }

    private String normalizeOrderTime(String rawValue) {
        String purchaseDate = toPurchaseDate(rawValue);
        return StringUtils.defaultIfBlank(purchaseDate, cleanText(rawValue));
    }

    private String firstText(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            String text = asText(value);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        }
        return null;
    }

    private Double firstNumber(JsonNode node, List<String> keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.asDouble();
            }
            String text = asText(value);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            text = text.replace("¥", "").replace("￥", "").replace(",", "").trim();
            if (text.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(text);
            }
        }
        return null;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode child : node) {
                String childText = asText(child);
                if (StringUtils.isNotBlank(childText)) {
                    parts.add(childText);
                }
            }
            return parts.isEmpty() ? null : String.join(" / ", parts);
        }
        return null;
    }

    private List<String> extractJsonPayloads(String text) {
        List<String> payloads = new ArrayList<>();
        int limit = Math.min(text.length(), 1_500_000);
        boolean inString = false;
        boolean escape = false;
        int depth = 0;
        int start = -1;

        for (int index = 0; index < limit; index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (current == '\\') {
                    escape = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }

            if (depth == 0 && (current == '{' || current == '[')) {
                start = index;
                depth = 1;
                continue;
            }

            if (depth > 0) {
                if (current == '{' || current == '[') {
                    depth++;
                } else if (current == '}' || current == ']') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        String candidate = text.substring(start, index + 1).trim();
                        if (candidate.length() >= 32) {
                            payloads.add(candidate);
                            if (payloads.size() >= 24) {
                                return payloads;
                            }
                        }
                        start = -1;
                    }
                }
            }
        }
        return payloads;
    }

    private String buildFingerprint(PinduoduoImportItemVO item) {
        return String.join("|",
                StringUtils.defaultString(item.getOrderId()),
                StringUtils.defaultString(item.getProductUrl()),
                StringUtils.defaultString(item.getImageUrl()),
                StringUtils.defaultString(item.getProductName())
        );
    }

    private boolean looksLikeHtml(String text) {
        return text.startsWith("<!DOCTYPE html") || text.startsWith("<html") || text.contains("<script");
    }

    private boolean looksLikeJson(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replace("\\/", "/")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
        return StringUtils.defaultIfBlank(cleaned, null);
    }

    private String cleanUrl(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.startsWith("//")) {
            return "https:" + cleaned;
        }
        return cleaned;
    }

    private boolean containsAny(String value, String... keywords) {
        return Arrays.stream(keywords).filter(Objects::nonNull).anyMatch(value::contains);
    }

    private record ImportContext(String orderTime, String orderId, String shopName, String brand) {
        private ImportContext() {
            this(null, null, null, null);
        }

        private ImportContext merge(String nextOrderTime, String nextOrderId, String nextShopName, String nextBrand) {
            return new ImportContext(
                    StringUtils.defaultIfBlank(nextOrderTime, orderTime),
                    StringUtils.defaultIfBlank(nextOrderId, orderId),
                    StringUtils.defaultIfBlank(nextShopName, shopName),
                    StringUtils.defaultIfBlank(nextBrand, brand)
            );
        }
    }
}
