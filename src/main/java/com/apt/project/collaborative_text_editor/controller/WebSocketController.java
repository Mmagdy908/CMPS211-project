package com.apt.project.collaborative_text_editor.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.apt.project.collaborative_text_editor.enums.MessageType;
import com.apt.project.collaborative_text_editor.model.ResponseMessage;
import com.apt.project.collaborative_text_editor.service.SessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private SessionService sessionService=new SessionService();
    private final SimpMessagingTemplate messagingTemplate;

    // takes user id and sends the new session id
    @MessageMapping("/session/create")
    public void createSession(@RequestBody String userId) {

        try {
            String sessionId = sessionService.createSession(userId);
            ResponseMessage message=new ResponseMessage(MessageType.CREATE, userId, sessionId);
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        } catch (Exception e) {
            ResponseMessage message=new ResponseMessage(MessageType.ERROR, userId, e.getMessage());
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        }
    }

    // takes shareable code (either editor or viewer) and returns session id
    @MessageMapping("/session/join")
    public void joinSession(@RequestBody String userId) {

        try {
            
        } catch (Exception e) {
           
        }
    }

    // takes user id 
    @MessageMapping("/session/{sessionId}/leave")
    public void leaveSession(@RequestBody String userId) {

        try {
            
        } catch (Exception e) {
           
        }
    }

    // takes crdt operation and update it then sends back new content
    @MessageMapping("/session/{sessionId}/update")
    @SendTo("/topic/session/{sessionId}")
    public void handleMessage(String message, @DestinationVariable String sessionId) {
        System.out.println("Received message in room " + sessionId + ": " + message);
    }
   
}
