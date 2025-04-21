package com.apt.project.collaborative_text_editor.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/session/{sessionId}")
    @SendTo("/topic/session/{sessionId}")
    public ChatMessage handleMessage(ChatMessage message, @DestinationVariable String sessionId) {
        System.out.println("Received message in room " + sessionId + ": " + message);
        return message;
    }
}
