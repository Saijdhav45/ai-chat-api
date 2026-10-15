package com.ai_chat_api.controller;

import com.ai_chat_api.dto.ChatRequest;
import com.ai_chat_api.dto.ChatResponse;
import com.ai_chat_api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/chat")
public class ChatController {


    private final ChatService chatService;
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Mono<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request) {

        return chatService.generateResponse(request)
                .map(response -> {
                    ChatResponse chatResponse = new ChatResponse();
                    chatResponse.setResponse(response);
                    return chatResponse;
                });
    }
}
