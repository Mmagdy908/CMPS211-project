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

    public String createSession(String userId) throws Exception{
        String sessionId=new Utility().generateUniqueId();
        Session session=new Session(sessionId, "", new Vector<String>(), new Vector<String>());
        session.addEditor(userId);
        activeSessions.put(sessionId, session);
        return sessionId;
    }

}
