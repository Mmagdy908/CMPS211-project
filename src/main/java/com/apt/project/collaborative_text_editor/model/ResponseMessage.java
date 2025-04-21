package com.apt.project.collaborative_text_editor.model;

import com.apt.project.collaborative_text_editor.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor
public class ResponseMessage {
    private MessageType type;    
    private String senderId;
    private String content;
}
