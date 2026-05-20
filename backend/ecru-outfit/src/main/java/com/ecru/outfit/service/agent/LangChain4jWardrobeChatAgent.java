package com.ecru.outfit.service.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LangChain4jWardrobeChatAgent {

    @SystemMessage("""
            You are a wardrobe assistant inside a fashion application.
            {{customPrompt}}

            You can use tools to:
            - get weather context
            - search the user's wardrobe
            - inspect wardrobe statistics

            Rules:
            1. Give helpful, concise, practical answers.
            2. If clothing recommendation is relevant, ground it in real wardrobe items from tool results.
            3. Do not invent clothing IDs.
            4. If the question is mainly knowledge, explanation, or small talk, answer directly without forcing recommendations.
            5. Keep the final response user-facing and natural.

            Return only valid JSON:
            {
              "reply": "string",
              "recommendedItemIds": [1, 2, 3]
            }

            Constraints:
            - recommendedItemIds can be empty
            - If no grounded item should be recommended, return an empty array
            - No markdown, no code fences, JSON only
            """)
    @UserMessage("""
            User ID: {{userId}}
            Location: {{location}}
            Occasion: {{occasion}}
            Weather summary: {{weatherInfo}}
            Wardrobe search query: {{wardrobeQuery}}
            Existing context JSON: {{contextJson}}
            Recent chat history JSON: {{chatHistoryJson}}

            User message:
            {{userMessage}}
            """)
    String chat(
            @V("customPrompt") String customPrompt,
            @V("userId") Long userId,
            @V("location") String location,
            @V("occasion") String occasion,
            @V("weatherInfo") String weatherInfo,
            @V("wardrobeQuery") String wardrobeQuery,
            @V("contextJson") String contextJson,
            @V("chatHistoryJson") String chatHistoryJson,
            @V("userMessage") String userMessage
    );
}
