package com.ecru.common.service.vector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    private final OpenAiEmbeddingModel embeddingModel;

    public EmbeddingService(OpenAiEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] generateEmbedding(String text) {
        String safeText = text == null ? "" : text;
        try {
            Response<Embedding> response = embeddingModel.embed(safeText);
            Embedding embedding = response.content();
            if (embedding == null || embedding.vector() == null || embedding.vector().length == 0) {
                throw new IllegalStateException("Empty embedding response");
            }
            return embedding.vector();
        } catch (Exception e) {
            log.error("Failed to generate embedding: {}", e.getMessage(), e);
            return buildFallbackEmbedding(safeText);
        }
    }


    public List<float[]> generateBatchEmbeddings(List<String> texts) {
        List<String> safeTexts = texts == null ? List.of() : texts;
        try {
            List<TextSegment> segments = new ArrayList<>(safeTexts.size());
            for (String text : safeTexts) {
                segments.add(TextSegment.from(text == null ? "" : text));
            }

            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            List<Embedding> embeddings = response.content();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new IllegalStateException("Empty embedding batch response");
            }

            List<float[]> results = new ArrayList<>(embeddings.size());
            for (Embedding embedding : embeddings) {
                results.add(embedding.vector());
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings: {}", e.getMessage(), e);
            List<float[]> fallback = new ArrayList<>(safeTexts.size());
            for (String text : safeTexts) {
                fallback.add(buildFallbackEmbedding(text == null ? "" : text));
            }
            return fallback;
        }
    }

    private float[] buildFallbackEmbedding(String text) {
        float[] embedding = new float[1024];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) (text.length() % (i + 1)) / (i + 1);
        }
        return embedding;
    }
}
