package com.apt.project.collaborative_text_editor.service;

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

    public Message editDocument(Operation op, String sessionId, User sender) throws Exception{
        lock.lock();
        try{
            Session session=activeSessions.get(sessionId);
           
           
            session.edit(op,sender);
            Message message=Message.builder()
            .content(session.getDocumentContent())
            .characterIds(session.getCharacterIds()).editors(session.getEditors())
            .build();
            return message;
        }
        finally{
            lock.unlock();
        }
    }

    public Message updateCursors( String sessionId, Vector<User> editors) throws Exception{
        Session session=activeSessions.get(sessionId);
        session.setEditors(editors);
        Message message=Message.builder()
        .content(session.getDocumentContent())
        .characterIds(session.getCharacterIds()).editors(session.getEditors())
        .viewers(session.getViewers())
        .build();
        return message;
    }

}
