package com.example.chatbot.controller;

import com.example.chatbot.model.ChatResponse;
import com.example.chatbot.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ChatControllerTest {

    private MockMvc mockMvc;
    private StubChatService chatService;

    @BeforeEach
    public void setup() {
        chatService = new StubChatService();
        ChatController chatController = new ChatController(chatService);
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
    }

    @Test
    public void testIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    public void testChat_Success() throws Exception {
        chatService.setResponse("Hello user");

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Hello user"));
    }

    @Test
    public void testChat_EmptyMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    // Stub class to replace Mockito mock to avoid Java 25 ByteBuddy incompatibility
    static class StubChatService extends ChatService {
        private String response;

        public StubChatService() {
            super(WebClient.builder());
        }

        public void setResponse(String response) {
            this.response = response;
        }

        @Override
        public ChatResponse getChatResponse(String message) {
            return new ChatResponse(response);
        }
    }
}
