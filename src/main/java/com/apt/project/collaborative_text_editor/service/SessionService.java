package com.apt.project.collaborative_text_editor.service;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.apt.project.collaborative_text_editor.Utility;
import com.apt.project.collaborative_text_editor.model.Message;
import com.apt.project.collaborative_text_editor.model.Operation;
import com.apt.project.collaborative_text_editor.model.Session;
import com.apt.project.collaborative_text_editor.model.User;

public class SessionService {
    // maps session id -> session
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    //TODO optional
    // MAP editor and viewer codes to sessions

    //REMOVE THIS
    Session lastSession;
    public String createSession(User user) throws Exception{
        Session session=new Session();
        String sessionId=session.getId();
        session.addEditor(user);
        activeSessions.put(sessionId, session);
        lastSession=session; // REMOVE
        return sessionId;
    }

    // TODO 
    // COMPLETE LOGIC
    public Session joinSession(User user,String code) throws Exception{
        Session session=activeSessions.get(lastSession.getId());
        session.addEditor(user);
        return session;
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
