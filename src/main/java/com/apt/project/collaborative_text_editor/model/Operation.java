package com.apt.project.collaborative_text_editor.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an edit operation (insertion or deletion).
 */
public class Operation implements Serializable {

    public enum Type {
        INSERT, DELETE
    }

    private Type type;
    private String parentId;
    private Character ch; // For single character insertions
    private String text; // For multi-character insertions
    private int userId;
    private long timestamp;
    private String operationId;
    private String characterId; // ID provided by frontend
    private boolean isTextOperation = false; // Flag to indicate if this is a text batch operation

    // Add default constructor for Jackson deserialization
    public Operation() {
        // Default constructor required for Jackson
        this.operationId = "op-" + UUID.randomUUID().toString();
    }

    // Single character constructor
    public Operation(Type type, String parentId, Character ch, int userId, long timestamp, String characterId) {
        this.type = type;
        this.parentId = parentId;
        this.ch = ch;
        this.userId = userId;
        this.timestamp = timestamp;
        this.characterId = characterId != null ? characterId : generateDefaultId(userId, timestamp);
        // Generate a unique operation ID 
        this.operationId = "op-" + UUID.randomUUID().toString();
        this.isTextOperation = false;
    }

    // New constructor for text operations
    public Operation(Type type, String parentId, String text, int userId, long timestamp, String operationId) {
        this.type = type;
        this.parentId = parentId;
        this.text = text;
        this.userId = userId;
        this.timestamp = timestamp;
        this.operationId = operationId != null ? operationId : "op-" + UUID.randomUUID().toString();
        this.isTextOperation = true;
    }

    // Add getters and setters for all fields so Jackson can access them
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPosition() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Character getText() {
        return ch;
    }

    public void setCh(Character ch) {
        this.ch = ch;
    }

    public String getTextString() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        if (text != null) {
            this.isTextOperation = true;
            s        }
    }

    public boolean isTextOperation() {
        return isTextOperation;
    }

    public void setTextOperation(boolean isTextOperation) {
        this.isTextOperation = isTextOperation;
    }

    public int getuserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCharacterId() {
        return characterId;
    }

    public void setCharacterId(String characterId) {
        this.characterId = characterId;
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
        return Objects.hashCode(operationId);
    }

    @Override
    public String toString() {
        return "Operation{"
                + "type=" + type
                + ", parentId=" + parentId
                + (isTextOperation ? ", text='" + text + "'" : ", ch=" + ch)
                + ", userId=" + userId
                + ", timestamp=" + timestamp
                + ", operationId=" + operationId
                + (isTextOperation ? "" : ", characterId=" + characterId)
                + ", isTextOperation=" + isTextOperation
                + '}';
    }
}
