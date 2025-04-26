package com.apt.project.collaborative_text_editor.service;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.apt.project.collaborative_text_editor.Utility;
import com.apt.project.collaborative_text_editor.model.Session;

public class SessionService {
    // maps session id -> session
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    //REMOVE THIS
    String lastSession;
    public String createSession(String userId) throws Exception{
        String sessionId=new Utility().generateUniqueId();
        Session session=new Session(sessionId, "", new Vector<String>(), new Vector<String>());
        session.addEditor(userId);
        activeSessions.put(sessionId, session);
        lastSession=sessionId;
        return sessionId;
    }

    public String joinSession(String userId,String code) throws Exception{
        Session session=activeSessions.get(lastSession);
        session.addEditor(userId);
        return lastSession;
    }

}
