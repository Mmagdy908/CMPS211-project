package com.apt.project.collaborative_text_editor.model;

import java.util.List;

public interface CRDT {

    /**
     * Initializes the CRDT instance with an empty document or initial content.
     */
    void initialize();

    /**
     * Inserts text at the specified position in the document.
     *
     * @param parentId The id of parent character where the text should be inserted. null or -1 if inserting at beginning
     * @param ch    The character to insert.
     * @param userId   The unique identifier of the user performing the operation.
     * @return Represents a single edit (insertion) with metadata 
     *         such as parent id, ch, user id, and timestamp.
     */
    Operation insert(int parentId, Character ch, int userId);

    /**
     * Inserts multiple characters (for paste operations) at the specified position.
     *
     * @param parentId The id of parent character where the text should be inserted
     * @param text     The text to insert
     * @param userId   The unique identifier of the user performing the operation
     * @return List of operations representing the paste action
     */
    List<Operation> insertText(int parentId, String text, int userId);

    /**
     * Deletes a specified number of characters starting from the given position.
     *
     * @param charId The id of character to delete 
     * @param userId   The unique identifier of the user performing the operation.
     * @return An Operation object representing the deletion with metadata 
     *         such as parent id, ch, user id, and timestamp.
     */
    Operation delete(int charId, int userId);

    /**
     * Applies the given operation to the local replica.
     *
     * @param op The operation to apply.
     */
    void apply(Operation op);

    /**
     * Returns the current state of the document as a string.
     *
     * @return The current document content.
     */
    String getDocument();

    /**
     * Reverts the last operation performed by the specified user.
     *
     * @param userId The unique identifier of the user.
     * @return true if the undo was successful, false otherwise
     */
    boolean undo(String userId);

    /**
     * Reapplies the last undone operation for the specified user.
     *
     * @param userId The unique identifier of the user.
     * @return true if the redo was successful, false otherwise
     */
    boolean redo(String userId);
    
    /**
     * Get a list of character IDs and their positions to support cursor positioning
     * 
     * @return Map of character IDs to their positions in the document
     */
    List<Integer> getCharacterIds();
}
