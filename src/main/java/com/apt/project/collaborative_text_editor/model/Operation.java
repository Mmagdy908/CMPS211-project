package com.apt.project.collaborative_text_editor.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an edit operation (insertion or deletion).
 */
public class Operation implements Serializable {

    public enum Type {
        INSERT, DELETE
    }

    private Type type;
    private String parentId;
    private Character ch; // For insertions
    private int userId;
    private long timestamp;
    private String operationId;
    private String characterId; // ID provided by frontend

    // Updated constructor to include characterId
    public Operation(Type type, String parentId, Character ch, int userId, long timestamp, String characterId) {
        this.type = type;
        this.parentId = parentId;
        this.ch = ch;
        this.userId = userId;
        this.timestamp = timestamp;
        this.characterId = characterId != null ? characterId : generateDefaultId(userId, timestamp);
        this.operationId = this.characterId; // Use the character ID as the operation ID
    }
    
    // For backward compatibility
    public Operation(Type type, String parentId, Character ch, int userId, long timestamp) {
        this(type, parentId, ch, userId, timestamp, null);
    }

    public Operation() {
    }

    public Type getType() {
        return type;
    }

    public String getPosition() {
        return parentId;
    }

    public Character getText() {
        return ch;
    }

    public int getuserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getOperationId() {
        return operationId;
    }
    
    public String getCharacterId() {
        return characterId;
    }

    // Generate a default ID if none provided from frontend
    private String generateDefaultId(int userId, long timestamp) {
        return userId + "-" + timestamp + "-" + (ch != null ? ch : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Operation)) {
            return false;
        }
        Operation operation = (Operation) o;
        return Objects.equals(operationId, operation.operationId);
    }

    @Override
    public int hashCode() {
        return operationId.hashCode();
    }

    @Override
    public String toString() {
        return "Operation{"
                + "type=" + type
                + ", parentId=" + parentId
                + ", ch=" + ch
                + ", userId=" + userId
                + ", timestamp=" + timestamp
                + ", operationId=" + operationId
                + ", characterId=" + characterId
                + '}';
    }
}
