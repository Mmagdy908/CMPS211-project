package com.apt.project.collaborative_text_editor.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.apt.project.collaborative_text_editor.Utility;
import com.apt.project.collaborative_text_editor.model.Message;
import com.apt.project.collaborative_text_editor.model.Operation;
import com.apt.project.collaborative_text_editor.model.Session;
import com.apt.project.collaborative_text_editor.model.User;

public class SessionService {
    // maps session id -> session
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

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
    Session lastSession;
    private final Map<String, String> editorCodeToSession = new ConcurrentHashMap<>();
    private final Map<String, String> viewerCodeToSession = new ConcurrentHashMap<>();

    public String createSession(User user) throws Exception {
        Session session = new Session();
        String sessionId = session.getId();
        session.addEditor(user);
        
        activeSessions.put(sessionId, session);
        editorCodeToSession.put(session.getDocument().getEditorCode(), sessionId);
        viewerCodeToSession.put(session.getDocument().getViewerCode(), sessionId);
        lastSession = session; // REMOVE
        return sessionId;
    }

    // TODO
    // COMPLETE LOGIC
    public String joinSession(User user, String code) throws Exception {
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
            session.addEditor(user);
        else
            session.addViewer(user);
        return sessionId;
    }



    public Message editDocument(Message message,String sessionId) throws Exception{
        lock.lock();
        try{
            Session session=activeSessions.get(sessionId);
           
           
            session.edit(message);
            
            Message responseMessage=Message.builder()
            .content(session.getDocumentContent())
            .characterIds(session.getCharacterIds()).editors(session.getEditors())
            .build();
            return responseMessage;
        }
        finally{
            lock.unlock();
        }
    }

    public Message updateCursors( String sessionId, Vector<User> editors) throws Exception{
        lock.lock();
        try{
        Session session=activeSessions.get(sessionId);
        session.setEditors(editors);
        Message message=Message.builder()
        .content(session.getDocumentContent())
        .characterIds(session.getCharacterIds()).editors(session.getEditors())
        .viewers(session.getViewers())
        .build();
        return message;
        }
        finally{
            lock.unlock();
        }
    }

    public Session getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public List<User> getParticipants(String sessionId) {
        Session s = activeSessions.get(sessionId);
        List<User> all = new ArrayList<>();
        all.addAll(s.getEditors());
        all.addAll(s.getViewers());
        return all;
    }

    public Vector<User> getEditors(String sessionId) {
        Session s = activeSessions.get(sessionId);
        return s.getEditors();
    }
    public Vector<User> getViewers(String sessionId) {
        Session s = activeSessions.get(sessionId);
        return s.getViewers();
    }

}
