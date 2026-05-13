package com.example.chatbot.service;

import com.example.chatbot.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ChatService {
    private static final int MAX_GEMINI_RETRIES = 3;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);

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
        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse("Please provide a valid message.");
        }

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return new ChatResponse(
                    "Gemini API key is required. Please set the GEMINI_API_KEY environment variable.");
        }

        try {
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
                            "If the user asks technical questions, answer clearly with short explanations and examples when useful. "
                            +
                            "Do not give the same reply for different messages. " +
                            "Keep casual responses short, natural, and conversational. " +
                            "If you are unsure, say so honestly instead of inventing facts.");
            systemInstruction.put("parts", new Object[] { systemPart });

            part.put("text", message.trim());
            content.put("parts", new Object[] { part });
            generationConfig.put("temperature", 0.8);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 300);

            requestBody.put("system_instruction", systemInstruction);
            requestBody.put("contents", new Object[] { content });
            requestBody.put("generationConfig", generationConfig);

            Map<String, Object> apiResponse = webClient.post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", apiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .retryWhen(Retry.backoff(MAX_GEMINI_RETRIES, RETRY_DELAY)
                            .filter(this::isRetryableGeminiError)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure()))
                    .block();

            String responseText = extractGeminiResponseText(apiResponse);
            return new ChatResponse(responseText != null ? responseText : "Sorry, I couldn't generate a response.");
        } catch (WebClientResponseException e) {
            e.printStackTrace();
            return new ChatResponse(buildErrorMessage(e));
        } catch (WebClientRequestException e) {
            e.printStackTrace();
            return new ChatResponse(buildRequestErrorMessage(e));
        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean isRetryableGeminiError(Throwable throwable) {
        if (!(throwable instanceof WebClientResponseException exception)) {
            return false;
        }

        HttpStatusCode statusCode = exception.getStatusCode();
        return statusCode.value() == 429 || statusCode.is5xxServerError();
    }

    private String buildErrorMessage(WebClientResponseException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode == 400) {
            return "Gemini rejected the request (400). Check the model name and request format.";
        }

        if (statusCode == 401 || statusCode == 403) {
            return "Gemini denied access (" + statusCode
                    + "). Check whether your GEMINI_API_KEY is valid and allowed to use the Generative Language API.";
        }

        if (statusCode == 404) {
            return "Gemini endpoint not found (404). Check the configured model URL: " + apiUrl;
        }

        if (statusCode == 503) {
            return "Gemini is currently under high demand. I retried automatically, but it is still temporarily unavailable. Please try the same question again in a few seconds.";
        }

        if (statusCode == 429) {
            return "Gemini request limit reached for now. Please wait a bit and try again.";
        }

        String responseBody = e.getResponseBodyAsString();
        return "Gemini API error (" + statusCode + "): " +
                (responseBody == null || responseBody.isBlank() ? e.getStatusText() : responseBody);
    }

    private String buildRequestErrorMessage(WebClientRequestException e) {
        Throwable rootCause = e.getMostSpecificCause();
        String rootMessage = rootCause != null && rootCause.getMessage() != null
                ? rootCause.getMessage()
                : e.getMessage();

        return "Could not reach Gemini API at " + apiUrl + ". " +
                "This is usually a network, DNS, proxy, firewall, or TLS issue. " +
                "Details: " + rootMessage;
    }

    private String extractGeminiResponseText(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("candidates")) {
                Object candidatesObj = response.get("candidates");
                if (candidatesObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> candidates = (java.util.List<Map<String, Object>>) candidatesObj;
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate.containsKey("content")) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                            if (content.containsKey("parts")) {
                                Object partsObj = content.get("parts");
                                if (partsObj instanceof java.util.List) {
                                    @SuppressWarnings("unchecked")
                                    java.util.List<Map<String, Object>> parts = (java.util.List<Map<String, Object>>) partsObj;
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
