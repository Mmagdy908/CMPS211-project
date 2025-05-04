package com.apt.project.collaborative_text_editor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apt.project.collaborative_text_editor.model.Document;
import com.apt.project.collaborative_text_editor.model.Operation;
import com.apt.project.collaborative_text_editor.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public ResponseEntity<Document> createDocument(@RequestParam String title) {
        Document document = documentService.createDocument(title);
        return ResponseEntity.ok(document);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        Document document = documentService.getDocumentById(id);
        if (document != null) {
            return ResponseEntity.ok(document);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/editor/{code}")
    public ResponseEntity<Document> getDocumentByEditorCode(@PathVariable String code) {
        Document document = documentService.getDocumentByEditorCode(code);
        if (document != null) {
            return ResponseEntity.ok(document);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/viewer/{code}")
    public ResponseEntity<Document> getDocumentByViewerCode(@PathVariable String code) {
        Document document = documentService.getDocumentByViewerCode(code);
        if (document != null) {
            return ResponseEntity.ok(document);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<String> getDocumentText(@PathVariable String id) {
        String text = documentService.getDocumentText(id);
        return ResponseEntity.ok(text);
    }

    @PostMapping("/{id}/insert")
    public ResponseEntity<Operation> insertCharacter(
            @PathVariable String id,
            @RequestParam String parentId,
            @RequestParam Character ch,
            @RequestParam int userId,
            @RequestParam(required = false) String characterId) {
        
        Operation operation;
        if (characterId != null && !characterId.isEmpty()) {
            operation = documentService.insertCharacter(id, parentId, ch, userId, characterId);
        } else {
            operation = documentService.insertCharacter(id, parentId, ch, userId);
        }
        
        if (operation != null) {
            return ResponseEntity.ok(operation);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Operation> deleteCharacter(
            @PathVariable String id,
            @RequestParam String charId,
            @RequestParam int userId) {
        Operation operation = documentService.deleteCharacter(id, charId, userId);
        if (operation != null) {
            return ResponseEntity.ok(operation);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<Void> applyOperation(
            @PathVariable String id,
            @RequestBody Operation operation) {
        documentService.applyOperation(id, operation);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/undo")
    public ResponseEntity<Void> undo(
            @PathVariable String id,
            @RequestParam String userId) {
        documentService.undo(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/redo")
    public ResponseEntity<Void> redo(
            @PathVariable String id,
            @RequestParam String userId) {
        documentService.redo(id, userId);
        return ResponseEntity.ok().build();
    }
}
