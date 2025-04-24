package com.apt.project.collaborative_text_editor.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an edit operation (insertion or deletion).
 */
public class Operation implements Serializable {
    public enum Type { INSERT, DELETE }

    private Type type;
    private int parentId;
    private Character ch; // For insertions
    private int userId;
    private long timestamp;
    private int operationId; // Unique identifier for this operation
    
    // Constructors, getters, and setters
    public Operation(Type type, int parentId, Character ch, int userId, long timestamp) {
        this.type = type;
        this.parentId = parentId;
        this.ch = ch;
        this.userId = userId;
        this.timestamp = timestamp;
        this.operationId = generateOperationId(userId, timestamp);
    }

    public Type getType() {
        return type;
    }

    public int getPosition() {
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
    
    public int getOperationId() {
        return operationId;
    }
    
    // Generate a unique operation ID based on user ID and timestamp
    private int generateOperationId(int userId, long timestamp) {
        return Objects.hash(userId, timestamp, ch);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Operation)) return false;
        Operation operation = (Operation) o;
        return operationId == operation.operationId;
    }
    
    @Override
    public int hashCode() {
        return operationId;
    }
    
    @Override
    public String toString() {
        return "Operation{" +
                "type=" + type +
                ", parentId=" + parentId +
                ", ch=" + ch +
                ", userId=" + userId +
                ", timestamp=" + timestamp +
                ", operationId=" + operationId +
                '}';
    }
}
