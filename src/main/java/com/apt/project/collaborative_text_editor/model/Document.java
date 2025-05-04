package com.apt.project.collaborative_text_editor.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Document implements Serializable {
    
    private String id;
    private String title;
    private String editorCode;
    private String viewerCode;
    private TreeCRDT content;
    
    public Document() {
        this.id = UUID.randomUUID().toString();
        this.editorCode = generateCode();
        this.viewerCode = generateCode();
        this.content = new TreeCRDT();
        this.content.initialize();
    }
    
    public Document(String title) {
        this();
        this.title = title;
    }
    
    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    // Document editing operations
    
    // For backward compatibility 
    public Operation insertCharacter(String parentId, Character ch, int userId) {
        return content.insert(parentId, ch, userId);
    }
    
    // New method with characterId parameter
    public Operation insertCharacter(String parentId, Character ch, int userId, String characterId) {
        return content.insert(parentId, ch, userId, characterId);
    }
    
    // For backward compatibility
    public List<Operation> insertText(String parentId, String text, int userId) {
        return content.insertText(parentId, text, userId);
    }
    
    // New method with characterIds parameter
    public List<Operation> insertText(String parentId, String text, int userId, List<String> characterIds) {
        return content.insertText(parentId, text, userId, characterIds);
    }
    
    public Operation deleteCharacter(String charId, int userId) {
        return content.delete(charId, userId);
    }
    
    public boolean undo(String userId) {
        return content.undo(userId);
    }
    
    public boolean redo(String userId) {
        return content.redo(userId);
    }
    
    public void applyOperation(Operation operation) {
        content.apply(operation);
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getEditorCode() {
        return editorCode;
    }
    
    public String getViewerCode() {
        return viewerCode;
    }
    
    public TreeCRDT getContent() {
        return content;
    }
    
    public String getText() {
        return content.getDocument();
    }
    
    public List<String> getCharacterIds() {
        return content.getCharacterIds();
    }
}
