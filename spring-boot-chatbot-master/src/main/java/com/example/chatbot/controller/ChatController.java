package com.example.chatbot.controller;

import com.example.chatbot.model.ChatRequest;
import com.example.chatbot.model.ChatResponse;
import com.example.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Controller
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

   
     // @return The index.html template
   
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * REST endpoint to handle chat messages.
     * Receives a POST request with a message and returns the chatbot response.
     *
     * @param request The chat request containing the user's message
     * @return ResponseEntity containing the chat response
     */
    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a valid message."));
        }

        ChatResponse response = chatService.getChatResponse(request.getMessage());
        return ResponseEntity.ok(response);
    }
}

