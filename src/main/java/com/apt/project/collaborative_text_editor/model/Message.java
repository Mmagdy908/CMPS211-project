package com.apt.project.collaborative_text_editor.model;

import java.util.List;
import java.util.Vector;

import com.apt.project.collaborative_text_editor.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message {
    private MessageType type;    
    private User sender;
    private String sessionId;
    private String error;
    private String content;
    private List<String> characterIds; // Changed from List<Integer> to List<String>
    private Operation operation;
    private Vector<User> editors;
    private Vector<User> viewers;
    private String editorCode;
    private String viewerCode;
    private String code;
    //private List<String> activeUsers;
    private Integer cursorPosition;
    private String text;
    private List<String> characterIdList;
}
