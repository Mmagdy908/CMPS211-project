package com.apt.project.collaborative_text_editor.service;

import java.util.ArrayList;
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

    // TODO optional
    // MAP editor and viewer codes to sessions

    // REMOVE THIS

    // public String createSession(String userId) throws Exception{
    // Session session=new Session();
    // String sessionId=session.getId();
    // session.addEditor(userId);
    // activeSessions.put(sessionId, session);
    // lastSession=sessionId; // REMOVE
    // return sessionId;
    // }
    String lastSession;
    private final Map<String, String> editorCodeToSession = new ConcurrentHashMap<>();
    private final Map<String, String> viewerCodeToSession = new ConcurrentHashMap<>();

    public String createSession(String userId) throws Exception {
        Session session = new Session();
        String sessionId = session.getId();
        session.addEditor(userId);
        activeSessions.put(sessionId, session);
        editorCodeToSession.put(session.getDocument().getEditorCode(), sessionId);
        viewerCodeToSession.put(session.getDocument().getViewerCode(), sessionId);
        lastSession = sessionId; // REMOVE
        return sessionId;
    }

    // TODO
    // COMPLETE LOGIC
    public String joinSession(String userId, String code) throws Exception {
        // Session session=activeSessions.get(lastSession);
        // session.addEditor(userId);
        // return lastSession;

        String sessionId = editorCodeToSession.get(code);
        boolean isEditor = true;
        if (sessionId == null) {
            sessionId = viewerCodeToSession.get(code);
            isEditor = false;
        }
        if (sessionId == null)
            throw new Exception("Invalid code");
        Session session = activeSessions.get(sessionId);
        if (isEditor)
            session.addEditor(userId);
        else
            session.addViewer(userId);
        return sessionId;
    }

    public Message editDocument(Operation op, String sessionId) throws Exception {
        Session session = activeSessions.get(sessionId);
        session.edit(op);
        Message message = Message.builder()
                .content(session.getDocumentContent())
                .characterIds(session.getCharacterIds()).build();
        return message;
    }

    public Session getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public List<String> getParticipants(String sessionId) {
        Session s = activeSessions.get(sessionId);
        List<String> all = new ArrayList<>();
        all.addAll(s.getEditors());
        all.addAll(s.getViewers());
        return all;
    }

}
