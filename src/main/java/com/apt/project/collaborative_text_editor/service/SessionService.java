package com.apt.project.collaborative_text_editor.service;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.apt.project.collaborative_text_editor.Utility;
import com.apt.project.collaborative_text_editor.model.Message;
import com.apt.project.collaborative_text_editor.model.Operation;
import com.apt.project.collaborative_text_editor.model.Session;

public class SessionService {
    // maps session id -> session
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    //TODO optional
    // MAP editor and viewer codes to sessions

    //REMOVE THIS
    String lastSession;
    public String createSession(String userId) throws Exception{
        Session session=new Session();
        String sessionId=session.getId();
        session.addEditor(userId);
        activeSessions.put(sessionId, session);
        lastSession=sessionId; // REMOVE
        return sessionId;
    }

    // TODO 
    // COMPLETE LOGIC
    public String joinSession(String userId,String code) throws Exception{
        Session session=activeSessions.get(lastSession);
        session.addEditor(userId);
        return lastSession;
    }

    public Message editDocument(Operation op, String sessionId) throws Exception{
        Session session=activeSessions.get(sessionId);
        session.edit(op);
        Message message=Message.builder()
        .content(session.getDocumentContent())
        .characterIds(session.getCharacterIds()).build();
        return message;
    }

}
