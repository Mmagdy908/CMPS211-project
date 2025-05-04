package com.apt.project.collaborative_text_editor.controller;

import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import com.apt.project.collaborative_text_editor.enums.MessageType;
import com.apt.project.collaborative_text_editor.model.Message;
import com.apt.project.collaborative_text_editor.model.Session;
import com.apt.project.collaborative_text_editor.service.SessionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private SessionService sessionService = new SessionService();
    private final SimpMessagingTemplate messagingTemplate;

    // takes user id and sends the new session id
    @MessageMapping("/session/create")
    public void createSession(@RequestBody String userId) {

        try {
            String sessionId = sessionService.createSession(userId);
            Session session = sessionService.getSession(sessionId);
            Message message = Message.builder()
            .type(MessageType.CREATE)
            .senderId(userId)
            .sessionId(sessionId)
            .editorCode(session.getEditorCode())
            .viewerCode(session.getViewerCode())
            .build();
            //Message message = Message.builder().type(MessageType.CREATE).senderId(userId).sessionId(sessionId).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        } catch (Exception e) {
            Message message = Message.builder().type(MessageType.ERROR).senderId(userId).error(e.getMessage()).build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, message);
        }
    }

    // TODO
    // COMPLETE LOGIC
    // takes shareable code (either editor or viewer) and returns session id
    @MessageMapping("/session/join")
    public void joinSession(@RequestBody Message message) {
        String userId = message.getSenderId();
        String code = message.getContent(); // Expecting the shareable code in content
        try {
            String sessionId = sessionService.joinSession(userId, code);
            Message responseMessage = Message.builder()
                    .type(MessageType.JOIN)
                    .senderId(userId)
                    .sessionId(sessionId)
                    .build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, responseMessage);

                      // broadcast new presence to all in session
            List<String> participants = sessionService.getParticipants(sessionId);
            Message pres = Message.builder()
                    .type(MessageType.PRESENCE)
                    .sessionId(sessionId)
                    .activeUsers(participants)
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, pres);
        } catch (Exception e) {
            Message errorMessage = Message.builder()
                    .type(MessageType.ERROR)
                    .senderId(userId)
                    .error(e.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/user/" + userId, errorMessage);
        }
    }

    // TODO
    // takes user id
    @MessageMapping("/session/{sessionId}/leave")
    public void leaveSession(@RequestBody String userId, @DestinationVariable String sessionId) {
        // try {
        // } catch (Exception e) {
        // }
        // Optionally implement session cleanup or user removal logic here
        // For now, just notify others that the user left
        try {
            Message leaveMessage = Message.builder()
                    .type(MessageType.LEAVE)
                    .senderId(userId)
                    .sessionId(sessionId)
                    .content(userId + " left the session.")
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, leaveMessage);

            // broadcast updated presence
            List<String> participants = sessionService.getParticipants(sessionId);
            Message pres = Message.builder()
                    .type(MessageType.PRESENCE)
                    .sessionId(sessionId)
                    .activeUsers(participants)
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, pres);
        } catch (Exception e) {
            Message errorMessage = Message.builder()
                    .type(MessageType.ERROR)
                    .senderId(userId)
                    .error(e.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);
        }
    }

    // takes crdt operation and update it then sends back new content
    @MessageMapping("/session/{sessionId}/edit")
    public void editDocument(@RequestBody Message message, @DestinationVariable String sessionId) {
        String userId = message.getSenderId();
        try {
            Message serviceMessage = sessionService.editDocument(message.getOperation(), sessionId);
            Message responseMessage = Message.builder()
                    .type(MessageType.UPDATE)
                    .senderId(userId)
                    .content(serviceMessage.getContent())
                    .characterIds(serviceMessage.getCharacterIds())
                    .build();
            // return responseMessage;
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, responseMessage);

        } catch (Exception e) {
            Message errorMessage = Message.builder().type(MessageType.ERROR).senderId(userId).error(e.getMessage())
                    .build();
            // return errorMessage;
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);

        }
    }
    
//========================================================================================================//
//                         Suggested editOrCursor to relay cursor position                                //
//========================================================================================================//        


    // @MessageMapping("/session/{sessionId}/edit")
    // public void editOrCursor(Message msg, @DestinationVariable String sessionId) {
    //     String userId = msg.getSenderId();

    //     // CURSOR messages just echo back out
    //     if (msg.getType() == MessageType.CURSOR) {
    //         messagingTemplate.convertAndSend("/topic/session/" + sessionId, msg);
    //         return;
    //     }

    //     // Otherwise it’s a text‐edit → apply CRDT and broadcast UPDATE
    //     try {
    //         Message svc = sessionService.editDocument(msg.getOperation(), sessionId);

    //         Message upd = Message.builder()
    //                 .type(MessageType.UPDATE)
    //                 .senderId(userId)
    //                 .sessionId(sessionId)
    //                 .content(svc.getContent())
    //                 .characterIds(svc.getCharacterIds())
    //                 .build();

    //         messagingTemplate.convertAndSend("/topic/session/" + sessionId, upd);

    //     } catch (Exception ex) {
    //         Message err = Message.builder()
    //                 .type(MessageType.ERROR)
    //                 .senderId(userId)
    //                 .sessionId(sessionId)
    //                 .error(ex.getMessage())
    //                 .build();
    //         messagingTemplate.convertAndSend("/topic/session/" + sessionId, err);
    //     }
    // }


}
