package com.example.chatbot.service;

import com.example.chatbot.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;

    public ChatService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public ChatResponse getChatResponse(String message) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return new ChatResponse("Please provide a valid message.");
            }

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> systemInstruction = new HashMap<>();
            Map<String, Object> systemPart = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            Map<String, Object> generationConfig = new HashMap<>();

            systemPart.put("text",
                    "You are Chunnu-Boot, a helpful AI chatbot inside a Spring Boot web application. " +
                    "Your job is to give clear, natural, human-like responses. " +
                    "Always respond according to the user's exact intent. " +
                    "If the user greets you with hello, hi, or hey, reply with a friendly greeting. " +
                    "If the user says bye, goodbye, or see you, reply with a short farewell. " +
                    "If the user asks technical questions, answer clearly with short explanations and examples when useful. " +
                    "Do not give the same reply for different messages. " +
                    "Keep casual responses short, natural, and conversational. " +
                    "If you are unsure, say so honestly instead of inventing facts.");
            systemInstruction.put("parts", new Object[]{systemPart});

            part.put("text", message.trim());
            content.put("parts", new Object[]{part});
            generationConfig.put("temperature", 0.8);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 300);

            requestBody.put("systemInstruction", systemInstruction);
            requestBody.put("contents", new Object[]{content});
            requestBody.put("generationConfig", generationConfig);

            Map<String, Object> apiResponse = webClient.post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String responseText = extractResponseText(apiResponse);
            return new ChatResponse(responseText != null ? responseText : "Sorry, I couldn't generate a response.");
        } catch (WebClientResponseException e) {
            e.printStackTrace();
            String responseBody = e.getResponseBodyAsString();
            return new ChatResponse(
                    "Gemini API error (" + e.getStatusCode().value() + "): " +
                            (responseBody == null || responseBody.isBlank() ? e.getStatusText() : responseBody));
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse(
                    "The chatbot could not get a response from Gemini. Please check your API key, billing, or network connection.");
        }
    }

    private String extractResponseText(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("candidates")) {
                Object candidatesObj = response.get("candidates");
                if (candidatesObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> candidates =
                            (java.util.List<Map<String, Object>>) candidatesObj;
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate.containsKey("content")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            if (content.containsKey("parts")) {
                                Object partsObj = content.get("parts");
                                if (partsObj instanceof java.util.List) {
                                    @SuppressWarnings("unchecked")
                                    java.util.List<Map<String, Object>> parts =
                                            (java.util.List<Map<String, Object>>) partsObj;
                                    if (!parts.isEmpty()) {
                                        Map<String, Object> part = parts.get(0);
                                        return (String) part.get("text");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
