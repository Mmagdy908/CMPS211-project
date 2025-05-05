package com.apt.project.collaborative_text_editor.enums;

public enum MessageType {
    REGISTER, 
    CURSOR, 
    LEAVE, 
    ERROR, 
    UPDATE, 
    JOIN, 
    PRESENCE, 
    CREATE,
    UNDO,   // Add this for undo operation
    REDO    // Add this for redo operation
}