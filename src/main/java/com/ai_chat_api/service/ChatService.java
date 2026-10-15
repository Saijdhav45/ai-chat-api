package com.ai_chat_api.service;

import com.ai_chat_api.dto.ChatRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
@Service
public class ChatService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String geminiApiUrl;
    private final String geminiApiKey;
    private final String geminiModel;

    public ChatService(
            WebClient webClient,
            ObjectMapper objectMapper,
            @Value("${gemini.api.url}") String geminiApiUrl,
            @Value("${gemini.api.key}") String geminiApiKey,
            @Value("${gemini.api.model:gemini-3.8-flash}") String geminiModel) {

        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.geminiApiUrl = geminiApiUrl;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
    }

    public Mono<String> generateResponse(ChatRequest chatRequest) {

        String prompt = buildPrompt(chatRequest);

        Map<String, Object> requestBody = Map.of(
                "model", geminiModel,
                "input", prompt,
                "store", false
        );

        return webClient.post()
                .uri(geminiApiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-goog-api-key", geminiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractResponse);
    }

    private String buildPrompt(ChatRequest chatRequest) {

        if (chatRequest == null
                || chatRequest.getMessage() == null
                || chatRequest.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message must not be empty.");
        }

        return chatRequest.getMessage().trim();
    }

    private String extractResponse(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode steps = root.path("steps");

            if (!steps.isArray()) {
                throw new IllegalStateException(
                        "Gemini response does not contain a steps array.");
            }

            StringBuilder output = new StringBuilder();

            for (JsonNode step : steps) {

                if (!"model_output".equals(step.path("type").asText())) {
                    continue;
                }

                JsonNode content = step.path("content");

                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode item : content) {

                    if ("text".equals(item.path("type").asText())) {

                        String text = item.path("text").asText("");

                        if (!text.isBlank()) {
                            if (output.length() > 0) {
                                output.append("\n");
                            }

                            output.append(text);
                        }
                    }
                }
            }

            if (output.length() == 0) {
                throw new IllegalStateException(
                        "Gemini response does not contain generated text.");
            }

            return output.toString();

        } catch (JsonParseException e) {
            throw new IllegalStateException(
                    "Failed to parse Gemini response JSON.", e);
        }
    }
}