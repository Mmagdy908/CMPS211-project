package com.apt.project.collaborative_text_editor.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class ChatMessage {

    private String username;

    private String content;

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    // @Override
    // public String toString() {
    //     return "@" + username + ":" + content;
    // }
    @Override
    public String toString() {
        // String json="";
        // try {
        //     json =new ObjectMapper().writeValueAsString(this); 
             
        // } catch (Exception e) {
        //     System.err.println("Can't convert to json");
        // }
        return "{\"username\":\""+username+"\",\"content\":\""+content+"\"}";
    }
}
