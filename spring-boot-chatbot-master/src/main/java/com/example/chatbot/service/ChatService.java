package com.example.chatbot.service;

import com.example.chatbot.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

    /**
     * Sends a message to the Gemini API and returns the response.
     * Includes hardcoded responses for testing common queries.
     *
     * @param message The user's message to send to the API
     * @return ChatResponse containing the API's response
     */
    public ChatResponse getChatResponse(String message) {
        try {
          
            String response = getHardcodedResponse(message);
            if (response != null) {
                return new ChatResponse(response);
            }

            // Prepare the request body for Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();

            part.put("text", message);
            content.put("parts", new Object[] { part });
            requestBody.put("contents", new Object[] { content });

            // Make API call
            String fullUrl = apiUrl + "?key=" + apiKey;

            Map<String, Object> apiResponse = webClient.post()
                    .uri(fullUrl)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // Extract the response text from the API response
            String responseText = extractResponseText(apiResponse);

            return new ChatResponse(responseText != null ? responseText : "Sorry, I couldn't generate a response.");

        } catch (Exception e) {
            e.printStackTrace();
            return new ChatResponse(
                    "I'm currently working offline with limited knowledge. Please try asking about:\n" +
                    "• DevOps\n" +
                    "• Maven\n" +
                    "• Spring Boot\n" +
                    "• Greetings (hello, hi, hey)\n" +
                    "For real API integration, please provide a valid Gemini API key with billing enabled.");
        }
    }

    /**
     * Returns hardcoded responses for common test queries.
     * This allows testing without a valid Gemini API key.
     *
     * @param message The user's message
     * @return Hardcoded response or null if no match found
     */
    private String getHardcodedResponse(String message) {
        String lowerMessage = message.toLowerCase().trim();

        // Greeting responses
        if (lowerMessage.equals("hello") || lowerMessage.equals("hi") || lowerMessage.equals("hii") 
            || lowerMessage.equals("hey") || lowerMessage.equals("greetings")) {
            return "Hello! 👋 Welcome to the Web-Based Chatbot. How can I help you today?";
        }

        // How are you responses
        if (lowerMessage.equals("how are you") || lowerMessage.equals("how are you?") 
            || lowerMessage.contains("how are you doing")) {
            return "I'm doing great, thank you for asking! 😊 I'm here to help you with any questions about DevOps, Maven, or anything else. What would you like to know?";
        }

        // What is DevOps response
        if (lowerMessage.contains("what is devops") || lowerMessage.contains("devops") 
            && (lowerMessage.contains("what") || lowerMessage.contains("explain"))) {
            return "DevOps is a set of practices, tools, and a culture that combines software development (Dev) and IT operations (Ops). " +
                    "Key aspects of DevOps include:\n\n" +
                    "• Automation of deployment and infrastructure management\n" +
                    "• Continuous Integration/Continuous Deployment (CI/CD)\n" +
                    "• Infrastructure as Code (IaC)\n" +
                    "• Monitoring and logging\n" +
                    "• Collaboration between development and operations teams\n\n" +
                    "DevOps aims to shorten the development lifecycle and deliver updates more frequently with better quality.";
        }

        // What is Maven response
        if (lowerMessage.contains("what is maven") || (lowerMessage.contains("maven") 
            && (lowerMessage.contains("what") || lowerMessage.contains("explain")))) {
            return "Maven is a build automation and project management tool primarily used in Java development. Key features include:\n\n" +
                    "• Dependency Management - Automatically downloads and manages project dependencies\n" +
                    "• Build Automation - Standardized build process through build lifecycle phases\n" +
                    "• Project Structure - Enforces a standard directory structure\n" +
                    "• Plugin System - Extensible through plugins for various tasks\n" +
                    "• POM (Project Object Model) - XML file that describes the project\n\n" +
                    "Maven simplifies Java project management and is widely used in enterprise development.";
        }

        // What is Spring Boot response
        if (lowerMessage.contains("what is spring boot") || (lowerMessage.contains("spring boot") 
            && (lowerMessage.contains("what") || lowerMessage.contains("explain")))) {
            return "Spring Boot is a framework that simplifies the development of standalone, production-ready Spring applications. Key features:\n\n" +
                    "• Auto-configuration - Automatically configures Spring application based on classpath\n" +
                    "• Embedded servers - Includes embedded Tomcat, Jetty, or Undertow\n" +
                    "• Standalone JAR - Package application as an executable JAR file\n" +
                    "• Starter Dependencies - Simplified dependency management\n" +
                    "• Actuator - Built-in monitoring and metrics\n\n" +
                    "Spring Boot is ideal for building microservices and RESTful APIs quickly.";
        }

        // What is this chatbot response
        if (lowerMessage.contains("what is this") || lowerMessage.contains("tell me about this app") 
            || lowerMessage.contains("what does this chatbot do")) {
            return "This is a Web-Based Chatbot built with Java Spring Boot and integrated with the Gemini API. " +
                    "The application demonstrates:\n\n" +
                    "• Spring Boot backend architecture\n" +
                    "• REST API communication\n" +
                    "• Integration with third-party APIs\n" +
                    "• Layered architecture (Controller → Service → Model)\n" +
                    "• Maven project structure\n\n" +
                    "You can ask me anything, and I'll try to help! For testing, I have hardcoded responses for common questions like 'What is DevOps?', 'What is Maven?', etc.";
        }

        // Thank you responses
        if (lowerMessage.contains("thank") || lowerMessage.equals("thanks") || lowerMessage.equals("thank you")) {
            return "You're welcome! 😊 Feel free to ask me more questions anytime. I'm here to help!";
        }

        // Help/assistance response
        if (lowerMessage.equals("help") || lowerMessage.equals("help me") || lowerMessage.equals("?") 
            || lowerMessage.contains("what can you do")) {
            return "I can help you with questions about:\n\n" +
                    "• DevOps and best practices\n" +
                    "• Maven and Java build tools\n" +
                    "• Spring Boot framework\n" +
                    "• REST APIs and backend development\n" +
                    "• Web application architecture\n\n" +
                    "Try asking me: 'What is DevOps?', 'What is Maven?', 'How are you?', or any other question!";
        }

        // Default response for common queries
        if (lowerMessage.equals("yes") || lowerMessage.equals("yeah") || lowerMessage.equals("ok") || lowerMessage.equals("okay")) {
            return "Great! 👍 Is there anything else you'd like to know?";
        }

        if (lowerMessage.equals("no") || lowerMessage.equals("nope")) {
            return "Alright! Feel free to ask me anything if you need help. I'm always here!";
        }

        // No match found
        return null;
    }

    /**
     * Extracts the response text from the Gemini API response structure.
     *
     * @param response The response map from the API
     * @return The extracted text response
     */
    private String extractResponseText(Map<String, Object> response) {
        try {
            if (response != null && response.containsKey("candidates")) {
                Object candidatesObj = response.get("candidates");
                if (candidatesObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> candidates = (java.util.List<Map<String, Object>>) candidatesObj;
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        if (candidate.containsKey("content")) {
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
