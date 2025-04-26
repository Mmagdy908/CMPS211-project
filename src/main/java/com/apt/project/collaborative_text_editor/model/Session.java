package com.apt.project.collaborative_text_editor.model;

import java.util.List;
import java.util.Vector;

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
public class Session {
    private String id;
    private String CRDT;
    private Vector<String> editors;
    private Vector<String> viewers;
    private static int MAX_EDITORS=4;

    public void addEditor(String userId) throws Exception{
        if(editors.size()==MAX_EDITORS){
            throw new Exception("Max number of editors is reached");
        }else{
            editors.add(userId);
        }
    }
}
