package com.apt.project.collaborative_text_editor.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.apt.project.collaborative_text_editor.model.Document;
import com.apt.project.collaborative_text_editor.model.Operation;

@Service
public class DocumentService {
    
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    
    public Document createDocument(String title) {
        Document document = new Document(title);
        documents.put(document.getId(), document);
        return document;
    }
    
    public Document getDocumentById(String id) {
        return documents.get(id);
    }
    
    public Document getDocumentByEditorCode(String code) {
        return documents.values().stream()
                .filter(doc -> doc.getEditorCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    public Document getDocumentByViewerCode(String code) {
        return documents.values().stream()
                .filter(doc -> doc.getViewerCode().equals(code))
                .findFirst()
                .orElse(null);
    }
    
    // Character-based editing methods
    
    // For backward compatibility
    public Operation insertCharacter(String documentId, String parentId, Character ch, int userId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.insertCharacter(parentId, ch, userId);
        }
        return null;
    }
    
    // New method with characterId parameter
    public Operation insertCharacter(String documentId, String parentId, Character ch, int userId, String characterId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.insertCharacter(parentId, ch, userId, characterId);
        }
        return null;
    }
    
    // For backward compatibility
    public List<Operation> insertText(String documentId, String parentId, String text, int userId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.insertText(parentId, text, userId);
        }
        return null;
    }
    
    // New method with characterIds parameter
    public List<Operation> insertText(String documentId, String parentId, String text, int userId, List<String> characterIds) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.insertText(parentId, text, userId, characterIds);
        }
        return null;
    }
    
    public Operation deleteCharacter(String documentId, String charId, int userId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.deleteCharacter(charId, userId);
        }
        return null;
    }
    
    // Undo/Redo operations
    
    public boolean undo(String documentId, String userId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.undo(userId);
        }
        return false;
    }
    
    public boolean redo(String documentId, String userId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.redo(userId);
        }
        return false;
    }
    
    // Apply operations from remote users
    
    public void applyOperation(String documentId, Operation operation) {
        Document document = documents.get(documentId);
        if (document != null) {
            document.applyOperation(operation);
        }
    }
    
    public String getDocumentText(String documentId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.getText();
        }
        return "";
    }

    public List<String> getCharacterIds(String documentId) {
        Document document = documents.get(documentId);
        if (document != null) {
            return document.getCharacterIds();
        }
        return Collections.emptyList();
    }
}
