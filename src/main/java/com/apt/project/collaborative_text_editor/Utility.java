package com.apt.project.collaborative_text_editor;
import java.util.UUID;

public class Utility {
    public String generateUniqueId(){
        return UUID.randomUUID().toString();
    }
}
