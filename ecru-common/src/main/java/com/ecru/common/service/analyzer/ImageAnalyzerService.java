package com.ecru.common.service.analyzer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 图像分析服务
 */
@Service
public class ImageAnalyzerService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private OkHttpClient okHttpClient;

    public void init() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(300000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(300000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(300000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 分析穿搭照片
     * @param imageStream 图片流
     * @return 分析结果
     */
    public ImageAnalysisResult analyzeOutfit(InputStream imageStream) {
        try {
            // 将图片转换为Base64
            byte[] imageBytes = inputStreamToByteArray(imageStream);
            String base64Image = Base64Utils.encodeToString(imageBytes);

            // 生成图片哈希作为缓存键
            String imageHash = Base64Utils.encodeToString(Objects.requireNonNull(imageBytes));
            String cacheKey = "image_analysis:" + imageHash;

            // 尝试从缓存获取
            boolean enableCache = true;
            if (enableCache && redisTemplate != null) {
                try {
                    String cachedData = redisTemplate.opsForValue().get(cacheKey);
                    if (cachedData != null) {
                        return ImageAnalysisResult.fromJson(cachedData);
                    }
                } catch (Exception e) {
                    System.err.println("Redis缓存获取失败: " + e.getMessage());
                    // 继续执行，不使用缓存
                }
            }

            // 构建分析提示
            String prompt = getOutfitAnalysisPrompt();

            // 调用Qwen3-VL API
            ImageAnalysisResult result = callQwenVlApi(base64Image, prompt);

            // 缓存结果
            if (result != null && enableCache && redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(
                            cacheKey,
                            result.toJson(),
                            86400,
                            java.util.concurrent.TimeUnit.SECONDS
                    );
                } catch (Exception e) {
                    System.err.println("Redis缓存设置失败: " + e.getMessage());
                    // 继续执行，不使用缓存
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("分析穿搭照片失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 分析衣物图片
     * @param imageStream 图片流
     * @return 分析结果
     */
    public ClothingAnalysisResult analyzeClothing(InputStream imageStream) {
        try {
            // 将图片转换为Base64
            byte[] imageBytes = inputStreamToByteArray(imageStream);
            String base64Image = Base64Utils.encodeToString(imageBytes);

            // 构建分析提示
            String prompt = getClothingAnalysisPrompt();

            // 调用Qwen3-VL API
            JSONObject response = callQwenVlApiRaw(base64Image, prompt);

            // 解析结果
            return parseClothingAnalysisResponse(response);
        } catch (Exception e) {
            System.err.println("分析衣物图片失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 调用Qwen3-VL API
     * @param base64Image Base64编码的图片
     * @param prompt 提示文本
     * @return 分析结果
     */
    private ImageAnalysisResult callQwenVlApi(String base64Image, String prompt) throws IOException {
        JSONObject response = callQwenVlApiRaw(base64Image, prompt);
        return parseImageAnalysisResponse(response);
    }

    /**
     * 调用Qwen3-VL API（原始响应）
     * @param base64Image Base64编码的图片
     * @param prompt 提示文本
     * @return 原始响应
     */
    private JSONObject callQwenVlApiRaw(String base64Image, String prompt) throws IOException {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen3-vl-32b-instruct");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);

        // 构建消息
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        // 构建内容（多模态）
        JSONArray content = new JSONArray();

        // 添加文本提示
        JSONObject textContent = new JSONObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);
        content.add(textContent);

        // 添加图片
        JSONObject imageContent = new JSONObject();
        imageContent.put("type", "image_url");
        JSONObject imageUrl = new JSONObject();
        imageUrl.put("url", "data:image/jpeg;base64," + base64Image);
        imageContent.put("image_url", imageUrl);
        content.add(imageContent);

        message.put("content", content);
        messages.add(message);
        requestBody.put("messages", messages);

        // 构建请求
        Request request = new Request.Builder()
                .url("https://api.siliconflow.cn/v1/chat/completions")
                .header("Authorization", "Bearer test-api-key")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                .build();

        // 执行请求
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }
            String responseBody = response.body().string();
            return JSON.parseObject(responseBody);
        }
    }

    /**
     * 解析图像分析响应
     * @param response API响应
     * @return 分析结果
     */
    private ImageAnalysisResult parseImageAnalysisResponse(JSONObject response) {
        try {
            JSONArray choices = response.getJSONArray("choices");
            if (choices.isEmpty()) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");

            // 解析JSON格式的内容
            JSONObject analysisJson = JSON.parseObject(content);
            ImageAnalysisResult result = new ImageAnalysisResult();
            result.setDetectedItems(new ArrayList<>());
            result.setStyle(analysisJson.getString("style"));
            result.setColorAnalysis(analysisJson.getString("colorAnalysis"));
            result.setOccasion(analysisJson.getString("occasion"));
            result.setSeason(analysisJson.getString("season"));
            result.setSuggestions(analysisJson.getString("suggestions"));

            return result;
        } catch (Exception e) {
            System.err.println("解析图像分析响应失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析衣物分析响应
     * @param response API响应
     * @return 分析结果
     */
    private ClothingAnalysisResult parseClothingAnalysisResponse(JSONObject response) {
        try {
            JSONArray choices = response.getJSONArray("choices");
            if (choices.isEmpty()) {
                return null;
            }

            JSONObject choice = choices.getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");

            // 解析JSON格式的内容
            JSONObject analysisJson = JSON.parseObject(content);
            ClothingAnalysisResult result = new ClothingAnalysisResult();
            result.setCategory(analysisJson.getString("category"));
            result.setColor(new HashMap<>());
            if (analysisJson.containsKey("color")) {
                result.setColor(analysisJson.getJSONObject("color").toJavaObject(Map.class));
            }
            result.setStyle(new ArrayList<>());
            if (analysisJson.containsKey("style")) {
                result.setStyle(analysisJson.getJSONArray("style").toJavaList(String.class));
            }
            result.setMaterial(analysisJson.getString("material"));
            result.setPattern(analysisJson.getString("pattern"));
            result.setOccasion(new ArrayList<>());
            if (analysisJson.containsKey("occasion")) {
                result.setOccasion(analysisJson.getJSONArray("occasion").toJavaList(String.class));
            }
            result.setSeason(new ArrayList<>());
            if (analysisJson.containsKey("season")) {
                result.setSeason(analysisJson.getJSONArray("season").toJavaList(String.class));
            }

            return result;
        } catch (Exception e) {
            System.err.println("解析衣物分析响应失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取穿搭分析提示
     * @return 提示文本
     */
    private String getOutfitAnalysisPrompt() {
        return "请分析这张穿搭照片，输出以下信息（JSON格式）：\n" +
                "{\n" +
                "  \"detectedItems\": [\n" +
                "    {\"item\": \"上衣\", \"color\": \"白色\", \"category\": \"上装\", \"confidence\": 0.95}\n" +
                "  ],\n" +
                "  \"style\": \"韩系休闲风\",\n" +
                "  \"colorAnalysis\": \"灰黑配色，低调简约\",\n" +
                "  \"occasion\": \"日常休闲\",\n" +
                "  \"season\": \"秋冬\",\n" +
                "  \"suggestions\": \"oversize版型适合梨型身材，建议将上衣前摆塞入裤腰提升比例\"\n" +
                "}";
    }

    /**
     * 获取衣物分析提示
     * @return 提示文本
     */
    private String getClothingAnalysisPrompt() {
        return "请分析这张衣物图片，输出以下信息（JSON格式）：\n" +
                "{\n" +
                "  \"category\": \"上装\",\n" +
                "  \"color\": {\n" +
                "    \"primary\": \"白色\",\n" +
                "    \"secondary\": \"浅蓝色\"\n" +
                "  },\n" +
                "  \"style\": [\"简约\", \"通勤\"],\n" +
                "  \"material\": \"棉\",\n" +
                "  \"pattern\": \"条纹\",\n" +
                "  \"occasion\": [\"日常\", \"通勤\"],\n" +
                "  \"season\": [\"春\", \"秋\"]\n" +
                "}";
    }

    /**
     * 将输入流转换为字节数组
     * @param inputStream 输入流
     * @return 字节数组
     */
    private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    /**
     * 图像分析结果
     */
    public static class ImageAnalysisResult {
        private List<Map<String, Object>> detectedItems;
        private String style;
        private String colorAnalysis;
        private String occasion;
        private String season;
        private String suggestions;

        // Getters and Setters
        public List<Map<String, Object>> getDetectedItems() { return detectedItems; }
        public void setDetectedItems(List<Map<String, Object>> detectedItems) { this.detectedItems = detectedItems; }
        public String getStyle() { return style; }
        public void setStyle(String style) { this.style = style; }
        public String getColorAnalysis() { return colorAnalysis; }
        public void setColorAnalysis(String colorAnalysis) { this.colorAnalysis = colorAnalysis; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }
        public String getSuggestions() { return suggestions; }
        public void setSuggestions(String suggestions) { this.suggestions = suggestions; }

        // JSON 转换方法
        public String toJson() { return JSON.toJSONString(this); }
        public static ImageAnalysisResult fromJson(String json) { return JSON.parseObject(json, ImageAnalysisResult.class); }
    }

    /**
     * 衣物分析结果
     */
    public static class ClothingAnalysisResult {
        private String category;
        private Map<String, String> color;
        private List<String> style;
        private String material;
        private String pattern;
        private List<String> occasion;
        private List<String> season;

        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Map<String, String> getColor() { return color; }
        public void setColor(Map<String, String> color) { this.color = color; }
        public List<String> getStyle() { return style; }
        public void setStyle(List<String> style) { this.style = style; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public List<String> getOccasion() { return occasion; }
        public void setOccasion(List<String> occasion) { this.occasion = occasion; }
        public List<String> getSeason() { return season; }
        public void setSeason(List<String> season) { this.season = season; }

        // JSON 转换方法
        public String toJson() { return JSON.toJSONString(this); }
        public static ClothingAnalysisResult fromJson(String json) { return JSON.parseObject(json, ClothingAnalysisResult.class); }
    }

}