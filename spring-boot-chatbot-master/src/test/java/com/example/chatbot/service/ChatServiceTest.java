package com.example.chatbot.service;

import com.example.chatbot.model.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ChatServiceTest {

    private static MockWebServer mockWebServer;
    private ChatService chatService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());


        WebClient.Builder builder = WebClient.builder();
        chatService = new ChatService(builder);

        ReflectionTestUtils.setField(chatService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(chatService, "apiUrl", baseUrl);
    }

    @Test
    void testGetChatResponse_Success() throws JsonProcessingException {
        // Mock API Response
        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"Hello! How can I help you?\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        ChatResponse response = chatService.getChatResponse("Hi");

        assertEquals("Hello! How can I help you?", response.getResponse());
    }

    @Test
    void testGetChatResponse_Error() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        ChatResponse response = chatService.getChatResponse("Hi");

        // The service catches exceptions and returns an error message
        assertTrue(response.getResponse().contains("Error"));
    }
}
