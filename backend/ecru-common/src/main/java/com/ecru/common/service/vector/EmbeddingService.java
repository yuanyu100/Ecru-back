package com.ecru.common.service.vector;

import okhttp3.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url}")
    private String baseUrl;

    @Value("${ai.siliconflow.embedding.model}")
    private String modelName;

    private final OkHttpClient okHttpClient;

    public EmbeddingService() {
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .writeTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 生成文本嵌入向量
     * @param text 文本内容
     * @return 向量数组
     */
    public float[] generateEmbedding(String text) {
        try {
            // 调用硅基流动API生成真实嵌入
            String endpoint = "/embeddings";
            String fullUrl = baseUrl + endpoint;

            // 构建请求体
            com.alibaba.fastjson2.JSONObject requestBody = new com.alibaba.fastjson2.JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");

            // 创建请求
            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }
                String responseBody = response.body().string();
                return parseEmbeddingResponse(responseBody);
            }
        } catch (Exception e) {
            log.error("生成文本嵌入失败: {}", e.getMessage(), e);
            float[] embedding = new float[1024];
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] = (float) (text.length() % (i + 1)) / (i + 1);
            }
            return embedding;
        }
    }

    /**
     * 生成衣物描述的嵌入向量
     * @param clothingDescription 衣物描述
     * @return 向量数组
     */
    public float[] generateClothingEmbedding(String clothingDescription) {
        return generateEmbedding(clothingDescription);
    }

    /**
     * 解析嵌入响应
     * @param responseBody 响应体
     * @return 嵌入向量
     */
    private float[] parseEmbeddingResponse(String responseBody) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(responseBody);
            com.alibaba.fastjson2.JSONArray data = json.getJSONArray("data");
            if (data.isEmpty()) {
                return new float[1024];
            }
            com.alibaba.fastjson2.JSONObject item = data.getJSONObject(0);
            com.alibaba.fastjson2.JSONArray embeddingArray = item.getJSONArray("embedding");
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.getFloat(i);
            }
            return embedding;
        } catch (Exception e) {
            log.error("解析嵌入响应失败: {}", e.getMessage(), e);
            return new float[1024];
        }
    }

    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        try {
            // 调用硅基流动API批量生成真实嵌入
            String endpoint = "/embeddings";
            String fullUrl = baseUrl + endpoint;

            // 构建请求体
            com.alibaba.fastjson2.JSONObject requestBody = new com.alibaba.fastjson2.JSONObject();
            requestBody.put("model", modelName);
            requestBody.put("input", texts);
            requestBody.put("encoding_format", "float");

            // 创建请求
            RequestBody body = RequestBody.create(
                    requestBody.toJSONString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response.code() + " " + response.message());
                }
                String responseBody = response.body().string();
                return parseBatchEmbeddingResponse(responseBody);
            }
        } catch (Exception e) {
            log.error("批量生成文本嵌入失败: {}", e.getMessage(), e);
            List<float[]> embeddings = new ArrayList<>();
            for (String text : texts) {
                float[] embedding = new float[1024];
                for (int i = 0; i < embedding.length; i++) {
                    int hash = text.hashCode() + i;
                    embedding[i] = (float) (Math.abs(hash) % 100) / 100.0f;
                }
                embeddings.add(embedding);
            }
            return embeddings;
        }
    }

    /**
     * 解析批量嵌入响应
     * @param responseBody 响应体
     * @return 嵌入向量列表
     */
    private List<float[]> parseBatchEmbeddingResponse(String responseBody) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(responseBody);
            com.alibaba.fastjson2.JSONArray data = json.getJSONArray("data");
            List<float[]> embeddings = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                com.alibaba.fastjson2.JSONObject item = data.getJSONObject(i);
                com.alibaba.fastjson2.JSONArray embeddingArray = item.getJSONArray("embedding");
                float[] embedding = new float[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    embedding[j] = embeddingArray.getFloat(j);
                }
                embeddings.add(embedding);
            }
            return embeddings;
        } catch (Exception e) {
            log.error("解析批量嵌入响应失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
