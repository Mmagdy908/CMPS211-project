package com.apt.project.collaborative_text_editor.model;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import com.apt.project.collaborative_text_editor.Utility;
import com.apt.project.collaborative_text_editor.model.Operation.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Getter 
@Setter 
@Builder
public class Session {
    private String id;
    private Document document;
    private Vector<User> editors;
    private Vector<User> viewers;
    private static int MAX_EDITORS=4;
    private String editorCode;
    private String viewerCode;

    private final ReentrantLock lock = new ReentrantLock();

    // TODO
    // editor, viewer codes
    public Session(){
        id=new Utility().generateUniqueId();
        document=new Document();
        editors=new Vector<User>();
        viewers=new Vector<User>();

        // Assign codes from the document
        this.editorCode = document.getEditorCode();
        this.viewerCode = document.getViewerCode();
    }
 

    //TODO add logic
    public void addEditor(User user) throws Exception{
        // if(editors.size()==MAX_EDITORS){
        //     throw new Exception("Max number of editors is reached");
        // }else{
        //     editors.add(userId);
        // }

        if (editors.size() == MAX_EDITORS) {
            throw new Exception("Max number of editors is reached");
        }
        boolean exists = editors.stream().anyMatch(u -> u.getId().equals(user.getId())); 
        if (!exists) {
            editors.add(user);
        }
    }

    public void addViewer(User user) throws Exception{
           // TODO 
           boolean exists = viewers.stream().anyMatch(u -> u.getId().equals(user.getId()));
           if (!exists) {
            viewers.add(user);
        }
    }

    public boolean isEditor(User user) {
        return editors.stream().anyMatch(u -> u.getId().equals(user.getId()));
    }

    public boolean isViewer(User user) {
        return viewers.stream().anyMatch(u -> u.getId().equals(user.getId()));
    }

    public void edit(Operation op,User sender){

        document.applyOperation(op);
    //    System.out.println("sender cursor before: " +sender.getCursorPosition());
        
        for(int i=0;i<editors.size();i++){
            int currentCursorPosition = editors.elementAt(i).getCursorPosition();
            if(op.getType()==Type.INSERT && currentCursorPosition>=sender.getCursorPosition()){
                editors.elementAt(i).setCursorPosition(currentCursorPosition+1);
            }
            else if (op.getType()==Type.DELETE && currentCursorPosition>=sender.getCursorPosition() && sender.getCursorPosition()>0)
            {
                // System.out.println("user: " +editors.elementAt(i).getUsername());
                // System.out.println("user cursor before: " +editors.elementAt(i).getCursorPosition());

                editors.elementAt(i).setCursorPosition(currentCursorPosition-1);
                // System.out.println("user cursor after: " +editors.elementAt(i).getCursorPosition());

            }

        }        
        // System.out.println("sender cursor after: " +sender.getCursorPosition());


    }

    public String getEditorCode(){
        return this.editorCode;
    }

    public String getViewerCode(){
        return this.viewerCode;
    }

    public String getDocumentContent(){
       return document.getText();
    }
    public List<String> getCharacterIds(){
       return document.getCharacterIds();
    }
    public Vector<User> getEditors(){
        // System.out.println("from getter: "+editors.elementAt(0).getCursorPosition());
        return editors;
    }
}
