package com.apt.project.collaborative_text_editor.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
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

    private SessionService sessionService = new SessionService();
    private final SimpMessagingTemplate messagingTemplate;

    // takes user id and sends the new session id
    @MessageMapping("/session/create")
    public void createSession(@RequestBody User user) {

        try {
           user.setCursorPosition(0);
            String sessionId = sessionService.createSession(user);
            
            Session session=sessionService.getSession(sessionId);

            Message message= 
            Message.builder()
            .type(MessageType.CREATE)
            .sender(user)
            .sessionId(sessionId)
            .editors( new Vector<>(Arrays.asList(user)))
            .editorCode(session.getEditorCode())
            .viewerCode(session.getViewerCode())
            .build();
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
            String code = message.getCode();
            if (code == null || code.isBlank()) {
                throw new Exception("No join‑code provided");
            }
            String sessionId=sessionService.joinSession(user, code);
            Session session = sessionService.getSession(sessionId);
            Message responseMessage= 
                Message.builder()
                .type(MessageType.JOIN)
                .sender(user)
                .sessionId(session.getId())
                .viewers(session.getViewers())
                .editors(session.getEditors())
                .content(session.getDocumentContent())
                .characterIds(session.getCharacterIds()).isEditor(session.isEditor(user))
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
    public void leaveSession(@RequestBody User user, @DestinationVariable String sessionId) {
        // try {
        // } catch (Exception e) {
        // }
        // Optionally implement session cleanup or user removal logic here
        // For now, just notify others that the user left
        try {
            Message leaveMessage = Message.builder()
                    .type(MessageType.LEAVE)
                    .sender(user)
                    .sessionId(sessionId)
                    .content(user.getId() + " left the session.")
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, leaveMessage);

            // broadcast updated presence
            Vector<User> editors = sessionService.getEditors(sessionId);
            Vector<User> viewers = sessionService.getViewers(sessionId);
            Message pres = Message.builder()
                    .type(MessageType.PRESENCE)
                    .sessionId(sessionId)
                    .editors(editors)
                    .viewers(viewers)
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, pres);
        } catch (Exception e) {
            Message errorMessage = Message.builder()
                    .type(MessageType.ERROR)
                    .sender(user)
                    .error(e.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);
        }
    }

    // takes crdt operation and update it then sends back new content
    @MessageMapping("/session/{sessionId}/edit")
    public void editDocument(@RequestBody Message message, @DestinationVariable String sessionId) {
        User user=message.getSender();

        try{
            Message serviceMessage = sessionService
                                    .editDocument(message,sessionId);

            Message responseMessage=Message.builder()
                                            .type(MessageType.UPDATE)
                                            .sender(user)
                                            .content(serviceMessage.getContent())
                                            .characterIds(serviceMessage.getCharacterIds())
                                            .editors(serviceMessage.getEditors())
                                            .viewers(serviceMessage.getViewers())
                                            .build();
            messagingTemplate.convertAndSend("/topic/session/"+sessionId, responseMessage);

        }catch(Exception e){
            Message errorMessage=Message.builder().type(MessageType.ERROR).sender(user).error(e.getMessage()).build();
            // return errorMessage;
            messagingTemplate.convertAndSend("/topic/session/"+sessionId , errorMessage);

        }
    }

    @MessageMapping("/session/{sessionId}/update-cursor")
    public void updateCursors(@RequestBody Message message, @DestinationVariable String sessionId) {
        User user=message.getSender();
        try{
            Message serviceMessage = sessionService
                                    .updateCursors(sessionId,message.getEditors());

            Message responseMessage=Message.builder()
                                            .type(MessageType.CURSOR)
                                            .sender(user)
                                            .content(serviceMessage.getContent())
                                            .characterIds(serviceMessage.getCharacterIds())
                                            .editors(serviceMessage.getEditors())
                                            .viewers(serviceMessage.getViewers())
                                            .build();
            // return responseMessage;
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, responseMessage);

        }catch(Exception e){
            Message errorMessage=Message.builder().type(MessageType.ERROR).sender(user).error(e.getMessage()).build();
            // return errorMessage;
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);

        }
    }

    @MessageMapping("/session/{sessionId}/undo")
    public void undoOperation(@RequestBody Message message, @DestinationVariable String sessionId) {
        User user = message.getSender();
        try {
            Message serviceMessage = sessionService.undoOperation(sessionId, user);
            
            Message responseMessage = Message.builder()
                    .type(MessageType.UPDATE)
                    .sender(user)
                    .content(serviceMessage.getContent())
                    .characterIds(serviceMessage.getCharacterIds())
                    .editors(serviceMessage.getEditors())
                    .viewers(serviceMessage.getViewers())
                    .build();
                    
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, responseMessage);
        } catch (Exception e) {
            Message errorMessage = Message.builder()
                    .type(MessageType.ERROR)
                    .sender(user)
                    .error(e.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);
        }
    }

    @MessageMapping("/session/{sessionId}/redo")
    public void redoOperation(@RequestBody Message message, @DestinationVariable String sessionId) {
        User user = message.getSender();
        try {
            Message serviceMessage = sessionService.redoOperation(sessionId, user);
            
            Message responseMessage = Message.builder()
                    .type(MessageType.UPDATE)
                    .sender(user)
                    .content(serviceMessage.getContent())
                    .characterIds(serviceMessage.getCharacterIds())
                    .editors(serviceMessage.getEditors())
                    .viewers(serviceMessage.getViewers())
                    .build();
                    
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, responseMessage);
        } catch (Exception e) {
            Message errorMessage = Message.builder()
                    .type(MessageType.ERROR)
                    .sender(user)
                    .error(e.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, errorMessage);
        }
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



