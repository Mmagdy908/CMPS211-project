package com.apt.project.collaborative_text_editor.model;

import java.util.List;
import java.util.Vector;

import com.apt.project.collaborative_text_editor.Utility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@AllArgsConstructor 
@Builder
public class Session {
    private String id;
    private Document document;
    private Vector<String> editors;
    private Vector<String> viewers;
    private static int MAX_EDITORS=4;

    public Session(){
        id=new Utility().generateUniqueId();
        document=new Document();
        editors=new Vector<String>();
        viewers=new Vector<String>();
    }

    public void addEditor(String userId) throws Exception{
        if(editors.size()==MAX_EDITORS){
            throw new Exception("Max number of editors is reached");
        }else{
            editors.add(userId);
        }
    }

    public void addViewer(String userId) throws Exception{
           // TODO
    }

    public void edit(Operation op){
        document.applyOperation(op);
    }

    public String getDocumentContent(){
       return document.getText();
    }
    public List<Integer> getCharacterIds(){
       return document.getCharacterIds();
    }
}
