package com.apt.project.collaborative_text_editor.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.apt.project.collaborative_text_editor.enums.MessageType;
import com.apt.project.collaborative_text_editor.model.Message;
import com.apt.project.collaborative_text_editor.model.Operation;
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
            Message message= Message.builder().type(MessageType.CREATE).senderId(userId).sessionId(sessionId).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        } catch (Exception e) {
            Message message=Message.builder().type(MessageType.ERROR).senderId(userId).error(e.getMessage()).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        }
    }

    // TODO
    // COMPLETE LOGIC
    // takes shareable code (either editor or viewer) and returns session id
    @MessageMapping("/session/join")
    public void joinSession(@RequestBody Message message) {
        String userId=message.getSenderId();
        try {
            String sessionId=sessionService.joinSession(userId, null);
            Message responseMessage= Message.builder().type(MessageType.JOIN).senderId(userId).sessionId(sessionId).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, responseMessage);
        } catch (Exception e) {
            Message errorMessage=Message.builder().type(MessageType.ERROR).senderId(userId).error(e.getMessage()).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, errorMessage);
        }
    }

    // TODO
    // takes user id 
    @MessageMapping("/session/{sessionId}/leave")
    public void leaveSession(@RequestBody String userId) {

        try {
            
        } catch (Exception e) {
           
        }
    }

    // takes crdt operation and update it then sends back new content
    @MessageMapping("/session/{sessionId}/edit")
    public void editDocument(@RequestBody Message message, @DestinationVariable String sessionId) {
        String userId=message.getSenderId();
        try{
            Message serviceMessage = sessionService.editDocument(message.getOperation(),sessionId);
            Message responseMessage=Message.builder()
                                            .type(MessageType.UPDATE)
                                            .senderId(userId)
                                            .content(serviceMessage.getContent())
                                            .characterIds(serviceMessage.getCharacterIds())
                                            .build();
            // return responseMessage;
            messagingTemplate.convertAndSend("/topic/session/"+sessionId, responseMessage);

        }catch(Exception e){
            Message errorMessage=Message.builder().type(MessageType.ERROR).senderId(userId).error(e.getMessage()).build();
            // return errorMessage;
            messagingTemplate.convertAndSend("/topic/session/"+sessionId , errorMessage);

        }
    }
   
}
