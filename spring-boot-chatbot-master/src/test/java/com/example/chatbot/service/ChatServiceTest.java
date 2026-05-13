package com.example.chatbot.service;

import com.example.chatbot.model.ChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        // Mock Gemini API Response
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

        ChatResponse response = chatService.getChatResponse("Tell me something about testing");

        assertEquals("Hello! How can I help you?", response.getResponse());
    }

    @Test
    void testGetChatResponse_UsesGeminiMessagesPayload() throws InterruptedException {
        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"Payload accepted\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        chatService.getChatResponse("Hello there");

        RecordedRequest request = mockWebServer.takeRequest(2, TimeUnit.SECONDS);
        assertNotNull(request);
        String body = request.getBody().readUtf8();

        assertTrue(body.contains("\"contents\""));
        assertTrue(body.contains("\"parts\""));
        assertTrue(body.contains("\"text\""));
    }

    @Test
    void testGetChatResponse_Error() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        ChatResponse response = chatService.getChatResponse("Tell me something about deployment");

        // The service catches exceptions and returns an error message
        assertTrue(response.getResponse().contains("Gemini API error (500)"));
    }

    @Test
    void testGetChatResponse_MissingApiKey() {
        ReflectionTestUtils.setField(chatService, "apiKey", "");

        ChatResponse response = chatService.getChatResponse("Hello");

        assertTrue(response.getResponse().contains("Gemini API key is required"));
    }

    @Test
    void testGetChatResponse_401ShowsHelpfulMessage() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401));

        ChatResponse response = chatService.getChatResponse("Why unauthorized?");

        assertTrue(response.getResponse().contains("Gemini denied access (401)"));
    }

    @Test
    void testGetChatResponse_RetriesAfter503AndSucceeds() {
        String jsonResponse = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\n" +
                "            \"text\": \"Recovered after retry\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        ChatResponse response = chatService.getChatResponse("Why did you fail once?");

        assertEquals("Recovered after retry", response.getResponse());
    }

    @Test
    void testGetChatResponse_503ShowsFriendlyMessageAfterRetries() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        ChatResponse response = chatService.getChatResponse("Still unavailable?");

        assertTrue(response.getResponse().contains("currently under high demand"));
    }
}
