package com.apt.project.collaborative_text_editor.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

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
import com.apt.project.collaborative_text_editor.model.Session;
import com.apt.project.collaborative_text_editor.model.User;
import com.apt.project.collaborative_text_editor.service.SessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private SessionService sessionService=new SessionService();
    private final SimpMessagingTemplate messagingTemplate;

    // takes user id and sends the new session id
    @MessageMapping("/session/create")
    public void createSession(@RequestBody User user) {

        try {
            String sessionId = sessionService.createSession(user);
            Message message= Message.builder().type(MessageType.CREATE).sender(user).sessionId(sessionId).editors( Arrays.asList(user)).build();
            messagingTemplate.convertAndSend("/topic/user/" + user.getId(), message);
        } catch (Exception e) {
            Message message=Message.builder().type(MessageType.ERROR).sender(user).error(e.getMessage()).build();
            messagingTemplate.convertAndSend("/topic/user/" + user.getId(), message);
        }
    }

    // TODO
    // COMPLETE LOGIC
    // takes shareable code (either editor or viewer) and returns session id
    @MessageMapping("/session/join")
    public void joinSession(@RequestBody Message message) {
        User user=message.getSender();
        try {
            Session session=sessionService.joinSession(user, null);

            Message responseMessage= Message.builder().type(MessageType.JOIN).sender(user)
            .sessionId(session.getId()).viewers(session.getViewers()).editors(session.getEditors())
            .build();

            messagingTemplate.convertAndSend("/topic/session/"+session.getId(), responseMessage);
            messagingTemplate.convertAndSend("/topic/user/" + user.getId(), responseMessage);
        } catch (Exception e) {
            Message errorMessage=Message.builder().type(MessageType.ERROR).sender(user).error(e.getMessage()).build();
            messagingTemplate.convertAndSend("/topic/user/" + user.getId(), errorMessage);
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
        User user=message.getSender();
        try{
            Message serviceMessage = sessionService.editDocument(message.getOperation(),sessionId);
            Message responseMessage=Message.builder()
                                            .type(MessageType.UPDATE)
                                            .sender(user)
                                            .content(serviceMessage.getContent())
                                            .characterIds(serviceMessage.getCharacterIds())
                                            .editors(serviceMessage.getEditors())
                                            .viewers(serviceMessage.getViewers())
                                            .build();
            // return responseMessage;
            messagingTemplate.convertAndSend("/topic/session/"+sessionId, responseMessage);

        }catch(Exception e){
            Message errorMessage=Message.builder().type(MessageType.ERROR).sender(user).error(e.getMessage()).build();
            // return errorMessage;
            messagingTemplate.convertAndSend("/topic/session/"+sessionId , errorMessage);

        }
    }
   
}
