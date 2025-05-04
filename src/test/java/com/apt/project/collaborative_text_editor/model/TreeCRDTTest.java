package com.apt.project.collaborative_text_editor.model;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeCRDTTest {

    private TreeCRDT crdt;

    @BeforeEach
    void setUp() {
        crdt = new TreeCRDT();
        crdt.initialize();
    }

    @Test
    void testInsertSingleCharacter() {
        // Insert a single character
        Operation op = crdt.insert(-1, 'H', 1);

        // Verify operation properties
        assertNotNull(op);
        assertEquals(Operation.Type.INSERT, op.getType());
        assertEquals(-1, op.getPosition());
        assertEquals('H', op.getText());
        assertEquals(1, op.getuserId());

        // Verify document content
        assertEquals("H", crdt.getDocument());
    }

    @Test
    void testInsertMultipleCharacters() {
        // Insert "Hello"
        crdt.insert(-1, 'A', 1);
        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException er) {
        }
        Operation h = crdt.insert(-1, 'h', 1);
        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException er) {
        }
        crdt.insert(-1, 'm', 1);
        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException er) {
        }
        crdt.insert(-1, 'e', 1);
        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException er) {
        }
        crdt.insert(-1, 'd', 1);

        // Document should be "demhA" (in reverse because of tree structure)
        assertEquals("demhA", crdt.getDocument());

        // Now insert at a specific position to test the parent-child relationship
        int hId = Objects.hash(1, h.getTimestamp());
        crdt.insert(hId, '!', 1);

        // The document should now be "demh!A"
        assertEquals("demh!A", crdt.getDocument());
    }

    @Test
    void testInsertText() {
        // Insert text "Hello" at once
        List<Operation> ops = crdt.insertText(-1, "Ahmed", 1);

        assertEquals(5, ops.size());
        assertEquals("Ahmed", crdt.getDocument());
    }

    @Test
    void testDeleteCharacter() {
        // Insert and then delete characters
        Operation h = crdt.insert(-1, 'H', 1);
        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        crdt.insert(-1, 'i', 1);
        // Delete 'H'
        int hId = Objects.hash(1, h.getTimestamp());
        Operation deleteOp = crdt.delete(hId, 1);

        // Verify operation
        assertNotNull(deleteOp);
        assertEquals(Operation.Type.DELETE, deleteOp.getType());
        assertEquals(hId, deleteOp.getPosition());

        // Verify document content
        assertEquals("i", crdt.getDocument());
    }

    @Test
    void testUndoRedo() {
        // Insert some text
        crdt.insert(-1, 'a', 1);

        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        crdt.insert(-1, 'b', 1);

        //simulate delay 
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        crdt.insert(-1, 'c', 1);

        // Document should be "cba"
        assertEquals("cba", crdt.getDocument());

        // Undo last insertion (c)
        assertTrue(crdt.undo("1"));

        // Document should be "ba"
        assertEquals("ba", crdt.getDocument());

        // Redo the undone operation
        assertTrue(crdt.redo("1"));

        // Document should be "cba" again
        assertEquals("cba", crdt.getDocument());
    }

    @Test
    void testConcurrentEdits() {
        // Simulate concurrent edits
        // 6 users inserting at the same time
        crdt.insert(-1, 'a', 1);
        crdt.insert(-1, '1', 2);
        crdt.insert(-1, 'b', 3);
        crdt.insert(-1, '2', 4);
        crdt.insert(-1, '3', 5);
        crdt.insert(-1, 'c', 6);


        // /ba  

        // Document should contain all characters, with ordering based on timestamp
        // Since timestamps are generated at runtime, we can only check the length
        String result = crdt.getDocument();
        assertEquals(6, result.length());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("3"));
    }

    @Test
    void testApplyOperation() {
        // Create an operation
        Operation op = new Operation(Operation.Type.INSERT, -1, 'X', 3, System.currentTimeMillis());

        // Apply the operation
        crdt.apply(op);

        // Verify document
        assertEquals("X", crdt.getDocument());
    }

    @Test
    void testCharacterIds() {
        // Insert some characters
        crdt.insert(-1, 'a', 1);
        //simulate delay
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        crdt.insert(-1, 'b', 1);
        //simulate delay
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }
        crdt.insert(-1, 'c', 1);

        // Get character IDs
        List<Integer> ids = crdt.getCharacterIds();

        // Should have 3 IDs
        assertEquals(3, ids.size());
    }

    @Test
    void testInsertWithSpecificParentChildRelationships() {
        // Insert "Ahmed Hesham Sayed" with specific parent-child relationships
        String text = "Ahmed Hesham Sayed";

        // Insert first character 'A' with root as parent
        Operation opA = crdt.insert(-1, text.charAt(0), 1);
        int parentId = Objects.hash(1, opA.getTimestamp());

        // Insert each subsequent character with the previous character as parent
        for (int i = 1; i < text.length(); i++) {
            // Simulate delay
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            Operation op = crdt.insert(parentId, text.charAt(i), 1);
            parentId = Objects.hash(1, op.getTimestamp());

            // Print for debugging
            System.out.println("Inserted " + text.charAt(i) + " with parent " + parentId);
        }

        // Verify the document content
        assertEquals("Ahmed Hesham Sayed", crdt.getDocument());

        // Verify the number of character IDs matches the text length
        List<Integer> ids = crdt.getCharacterIds();
        assertEquals(text.length(), ids.size());
        
        // Test deleting a character in the middle ('H' in "Ahmed Hesham Sayed")
        // First, get all character IDs in order
        List<Integer> characterIds = crdt.getCharacterIds();
        // Find the ID for 'H' (should be at position 6)
        int deletePosition = 6;
        if (characterIds.size() > deletePosition) {
            int charIdToDelete = characterIds.get(deletePosition);
            crdt.delete(charIdToDelete, 1);

            // Expected text after deletion: "Ahmed esham Sayed"
            assertEquals("Ahmed esham Sayed", crdt.getDocument());
        }
    }

    @Test
    void testInsertWithFrontendProvidedCharacterId() {
        // Test inserting a character with a frontend-provided ID
        String frontendCharId = "frontend-char-id-1";
        Operation op = crdt.insert("-1", 'A', 1, frontendCharId);
        
        // Verify operation properties
        assertNotNull(op);
        assertEquals(Operation.Type.INSERT, op.getType());
        assertEquals("-1", op.getPosition());
        assertEquals('A', op.getText());
        assertEquals(1, op.getuserId());
        assertEquals(frontendCharId, op.getCharacterId());
        
        // Verify document content
        assertEquals("A", crdt.getDocument());
        
        // Verify the character ID is in the list of IDs
        List<String> ids = crdt.getCharacterIds();
        assertEquals(1, ids.size());
        assertEquals(frontendCharId, ids.get(0));
    }
    
    @Test
    void testInsertWithPreviousFrontendIdAsParent() {
        // First insertion with frontend ID
        String firstCharId = "first-char-id";
        Operation firstOp = crdt.insert("-1", 'A', 1, firstCharId);
        
        String secondCharId = "second-char-id";
        Operation secondOp = crdt.insert(firstCharId, 'B', 1, secondCharId);
        
        assertEquals(firstCharId, secondOp.getPosition());
        assertEquals(secondCharId, secondOp.getCharacterId());
        
        assertEquals("AB", crdt.getDocument());
    }
    
    @Test
    void testSequentialInsertionsWithFrontendIds() {
        // Create a series of frontend character IDs
        String[] charIds = {"char-1", "char-2", "char-3", "char-4", "char-5"};
        String text = "Hello";
        
        // Start with the root as parent
        String parentId = "-1";
        
        // Insert each character with its own ID and the previous character as parent
        for (int i = 0; i < text.length(); i++) {
            Operation op = crdt.insert(parentId, text.charAt(i), 1, charIds[i]);
            parentId = op.getCharacterId(); // Use the current character as parent for the next insertion
        }
        
        // Verify the document content - should be in correct order because of parent-child relationships
        assertEquals("Hello", crdt.getDocument());
        
        // Verify all character IDs are present
        List<String> ids = crdt.getCharacterIds();
        assertEquals(text.length(), ids.size());
        for (String charId : charIds) {
            assertTrue(ids.contains(charId));
        }
    }
    
    @Test
    void testBatchInsertWithFrontendIds() {
        // Create a list of character IDs for batch insertion
        List<String> charIds = new ArrayList<>();
        charIds.add("batch-1");
        charIds.add("batch-2");
        charIds.add("batch-3");
        charIds.add("batch-4");
        charIds.add("batch-5");
        
        // Insert text with provided character IDs
        List<Operation> ops = crdt.insertText("-1", "World", 1, charIds);
        
        // Verify operations
        assertEquals(5, ops.size());
        for (int i = 0; i < ops.size(); i++) {
            assertEquals(charIds.get(i), ops.get(i).getCharacterId());
        }
        
        // Verify document content
        assertEquals("World", crdt.getDocument());
        
        // Verify character IDs are in the document
        List<String> documentIds = crdt.getCharacterIds();
        for (String charId : charIds) {
            assertTrue(documentIds.contains(charId));
        }
    }
    
    @Test
    void testMixedAutomaticAndFrontendIds() {
        // Insert first character with automatic ID
        Operation auto1 = crdt.insert("-1", 'A', 1);
        
        // Insert second character with frontend ID using first character as parent
        String frontendId = "frontend-id-1";
        Operation frontend1 = crdt.insert(auto1.getCharacterId(), 'B', 1, frontendId);
        
        // Insert third character with automatic ID using frontend character as parent
        Operation auto2 = crdt.insert(frontendId, 'C', 1);
        
        // Verify the chain of parent-child relationships
        assertEquals("-1", auto1.getPosition());
        assertEquals(auto1.getCharacterId(), frontend1.getPosition());
        assertEquals(frontendId, auto2.getPosition());
        
        // The traversal order should reflect the parent-child relationships
        assertEquals("ABC", crdt.getDocument());
    }
}
