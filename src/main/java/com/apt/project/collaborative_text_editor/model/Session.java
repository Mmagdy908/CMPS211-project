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
        } else if (!editors.contains(userId)) {
            editors.add(user);
        }
    }

    public void addViewer(User user) throws Exception{
           // TODO 
           if (!viewers.contains(userId)) {
            viewers.add(userId);
        }
    }

    public boolean isEditor(String userId) {
        return editors.contains(userId);
    }

    public boolean isViewer(String userId) {
        return viewers.contains(userId);
    }

    public void edit(Operation op,User sender){

        document.applyOperation(op);
       
        
        for(int i=0;i<editors.size();i++){
            int currentCursorPosition = editors.elementAt(i).getCursorPosition();
            if(op.getType()==Type.INSERT && currentCursorPosition>=sender.getCursorPosition()){
                editors.elementAt(i).setCursorPosition(currentCursorPosition+1);
            }
            else if (op.getType()==Type.DELETE && currentCursorPosition>=sender.getCursorPosition() && sender.getCursorPosition()>0)
                editors.elementAt(i).setCursorPosition(currentCursorPosition-1);

        }        

    }

    public String getDocumentContent(){
       return document.getText();
    }
    public List<Integer> getCharacterIds(){
       return document.getCharacterIds();
    }
    public Vector<User> getEditors(){
        // System.out.println("from getter: "+editors.elementAt(0).getCursorPosition());
        return editors;
    }
}
