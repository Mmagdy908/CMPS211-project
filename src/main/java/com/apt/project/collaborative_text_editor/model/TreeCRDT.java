package com.apt.project.collaborative_text_editor.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TreeCRDT implements CRDT {

    static class CharacterID implements Comparable<CharacterID> {

        final int userId;
        final long timestamp;
        final String id; // Added field for explicit ID from frontend

        CharacterID(int userId, long timestamp, String id) {
            this.userId = userId;
            this.timestamp = timestamp;
            this.id = id != null ? id : (userId + "-" + timestamp);
        }

        @Override
        public int compareTo(CharacterID other) {
            // Newer timestamps should appear first (descending timestamp order)
            int timeCmp = Long.compare(other.timestamp, this.timestamp);
            return timeCmp != 0 ? timeCmp : Integer.compare(this.userId, other.userId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CharacterID)) {
                return false;
            }
            CharacterID other = (CharacterID) o;
            return Objects.equals(id, other.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    static class CRDTNode implements Comparable<CRDTNode> {

        CharacterID id;
        Character ch;
        boolean isDeleted = false;
        TreeSet<CRDTNode> children = new TreeSet<>();
        String nodeId; // Changed from int to String

        CRDTNode(CharacterID id, Character ch) {
            this.id = id;
            this.ch = ch;
            this.nodeId = id.id;
        }

        @Override
        public int compareTo(CRDTNode other) {
            return this.id.compareTo(other.id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CRDTNode)) {
                return false;
            }
            CRDTNode node = (CRDTNode) o;
            return Objects.equals(nodeId, node.nodeId);
        }

        @Override
        public int hashCode() {
            return nodeId.hashCode();
        }
    }

    private final Map<String, CRDTNode> idNodeMap = new ConcurrentHashMap<>();
    private final CRDTNode root = new CRDTNode(new CharacterID(-1, Long.MAX_VALUE, "-1"), null);
    private final ReentrantLock lock = new ReentrantLock();

    // Track history for each user (max 10 operations per user for undo)
    private final Map<Integer, Deque<Operation>> history = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Operation>> redoStack = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 10;

    // Enhanced debugging methods
    private static final boolean DEBUG = true; // Toggle for enabling/disabling debug
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private void debug(String method, String message) {
        if (DEBUG) {
            String timestamp = LocalDateTime.now().format(timeFormatter);
            System.out.println("[DEBUG][" + timestamp + "][TreeCRDT:" + method + "] " + message);
        }
    }

    private void logIdMapState(String operation, String id) {
        if (DEBUG) {
            debug(operation, "Looking for ID: " + id);
            debug(operation, "Map contains " + idNodeMap.size() + " keys: " + idNodeMap.keySet());
            if (idNodeMap.size() < 10) { // Only dump full contents for small maps
                for (Map.Entry<String, CRDTNode> entry : idNodeMap.entrySet()) {
                    debug(operation, "  Key: " + entry.getKey() + ", Node ID: " + entry.getValue().nodeId +
                            ", Char: " + entry.getValue().ch + ", Deleted: " + entry.getValue().isDeleted);
                }
            }
        }
    }

    @Override
    public void initialize() {
        lock.lock();
        try {
            debug("initialize", "Initializing CRDT");
            idNodeMap.clear();
            root.children.clear();
            history.clear();
            redoStack.clear();
            debug("initialize", "CRDT initialized");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Operation insert(String parentId, Character ch, int userId) {
        return insert(parentId, ch, userId, null);
    }

    public Operation insert(String parentId, Character ch, int userId, String characterId) {
        lock.lock();
        try {
            long timestamp = System.currentTimeMillis();
            String charId = characterId != null ? characterId : userId + "-" + timestamp;
            debug("insert", "Inserting character '" + ch + "' with ID: " + charId +
                    ", parent: " + parentId + ", userId: " + userId);

            CharacterID id = new CharacterID(userId, timestamp, charId);
            CRDTNode node = new CRDTNode(id, ch);

            CRDTNode parent = "-1".equals(parentId) ? root : findNodeById(parentId);
            if (parent == null) {
                debug("insert", "WARNING: Parent node not found for ID: " + parentId + ". Using root instead.");
                parent = root; // Default to root if parent not found
            } else {
                debug("insert", "Found parent node with ID: " + parent.nodeId);
            }

            parent.children.add(node);
            // Ensure we use the exact same ID string that the node has
            idNodeMap.put(node.nodeId, node);
            debug("insert", "Node added to map with ID: " + node.nodeId +
                    ", Map size now: " + idNodeMap.size());

            Operation op = new Operation(Operation.Type.INSERT, parentId, ch, userId, timestamp, charId);
            addToHistory(userId, op);
            debug("insert", "Operation added to history: " + op);
            return op;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Operation> insertText(String parentId, String text, int userId, List<String> characterIds) {
        if (text == null || text.isEmpty()) {
            debug("insertText", "Text is null or empty, returning empty list");
            return Collections.emptyList();
        }

        debug("insertText", "Inserting text of length " + text.length() +
                " with parent: " + parentId + ", userId: " + userId);

        List<Operation> operations = new ArrayList<>();
        String currentParentId = parentId;

        lock.lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                // Use provided character ID if available, otherwise generate one
                String characterId = (characterIds != null && i < characterIds.size())
                        ? characterIds.get(i)
                        : userId + "-" + System.currentTimeMillis() + "-" + i;

                debug("insertText", "Inserting character '" + text.charAt(i) +
                        "' at position " + i + " with ID: " + characterId);

                Operation op = insert(currentParentId, text.charAt(i), userId, characterId);
                operations.add(op);
                currentParentId = op.getCharacterId();

                debug("insertText", "Next parent ID will be: " + currentParentId);
            }
            debug("insertText", "Completed text insertion, created " + operations.size() + " operations");
            return operations;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Operation> insertText(String parentId, String text, int userId) {
        debug("insertText", "Called without character IDs, delegating to main method");
        // Use the new method with null characterIds for backward compatibility
        return insertText(parentId, text, userId, null);
    }

    @Override
    public Operation delete(String charId, int userId) {
        lock.lock();
        try {
            debug("delete", "Attempting to delete char with ID: " + charId + ", userId: " + userId);

            CRDTNode node = findNodeById(charId);
            if (node != null && !node.isDeleted) {
                debug("delete", "Found node to delete: " + node.nodeId + ", char: '" + node.ch + "'");

                node.isDeleted = true;
                long timestamp = System.currentTimeMillis();
                Operation op = new Operation(Operation.Type.DELETE, charId, node.ch, userId, timestamp, charId);
                addToHistory(userId, op);

                debug("delete", "Node marked as deleted and operation recorded: " + op);
                return op;
            } else {
                if (node == null) {
                    logIdMapState("delete", charId);
                    debug("delete", "ERROR: Cannot delete - Node not found for ID: " + charId);
                } else {
                    debug("delete", "WARNING: Cannot delete - Node already deleted for ID: " + charId);
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void apply(Operation op) {
        if (op == null) {
            debug("apply", "Operation is null, ignoring");
            return;
        }

        debug("apply", "Applying operation: " + op.getType() +
                ", position: " + op.getPosition() +
                ", char: " + op.getText() +
                ", userId: " + op.getuserId() +
                ", characterId: " + op.getCharacterId());

        if (op.getType() == Operation.Type.INSERT) {
            insert(op.getPosition(), op.getText(), op.getuserId(), op.getCharacterId());
        } else if (op.getType() == Operation.Type.DELETE) {
            delete(op.getPosition(), op.getuserId());
        }

        debug("apply", "Operation applied, document is now: " + getDocument());
    }

    @Override
    public String getDocument() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            traverseTree(root, sb);
            String result = sb.toString();
            debug("getDocument", "Retrieved document: '" + result + "', length: " + result.length());
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getCharacterIds() {
        lock.lock();
        try {
            List<String> ids = new ArrayList<>();
            collectIds(root, ids);
            debug("getCharacterIds", "Retrieved " + ids.size() + " character IDs");
            return ids;
        } finally {
            lock.unlock();
        }
    }

    private void collectIds(CRDTNode node, List<String> ids) {
        if (node != root && !node.isDeleted) {
            ids.add(node.nodeId);
        }

        for (CRDTNode child : node.children) {
            collectIds(child, ids);
        }
    }

    private void traverseTree(CRDTNode node, StringBuilder sb) {
        for (CRDTNode child : node.children) {
            if (!child.isDeleted) {
                sb.append(child.ch);
            }
            traverseTree(child, sb);
        }
    }

    @Override
    public boolean undo(String userIdStr) {
        int userId = Integer.parseInt(userIdStr);
        lock.lock();
        try {
            debug("undo", "Attempting undo for userId: " + userId);

            Deque<Operation> ops = history.getOrDefault(userId, new ArrayDeque<>());
            if (ops.isEmpty()) {
                debug("undo", "No operations to undo for userId: " + userId);
                return false;
            }

            Operation op = ops.pop();
            debug("undo", "Undoing operation: " + op.getType() +
                    ", char: " + op.getText() +
                    ", characterId: " + op.getCharacterId());

            if (op.getType() == Operation.Type.INSERT) {
                CRDTNode node = findNodeById(op.getCharacterId());
                if (node != null) {
                    debug("undo", "Marking node as deleted: " + node.nodeId);
                    node.isDeleted = true;
                } else {
                    debug("undo", "ERROR: Node not found for ID: " + op.getCharacterId());
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    debug("undo", "Unmarking node as deleted: " + node.nodeId);
                    node.isDeleted = false;
                } else {
                    debug("undo", "ERROR: Node not found for ID: " + op.getPosition());
                }
            }

            redoStack.computeIfAbsent(userId, k -> new ArrayDeque<>()).push(op);
            debug("undo", "Added operation to redo stack, document is now: " + getDocument());
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean redo(String userIdStr) {
        int userId = Integer.parseInt(userIdStr);
        lock.lock();
        try {
            debug("redo", "Attempting redo for userId: " + userId);

            Deque<Operation> redo = redoStack.getOrDefault(userId, new ArrayDeque<>());
            if (redo.isEmpty()) {
                debug("redo", "No operations to redo for userId: " + userId);
                return false;
            }

            Operation op = redo.pop();
            debug("redo", "Redoing operation: " + op.getType() +
                    ", char: " + op.getText() +
                    ", characterId: " + op.getCharacterId());

            if (op.getType() == Operation.Type.INSERT) {
                CRDTNode node = findNodeById(op.getCharacterId());
                if (node != null) {
                    debug("redo", "Unmarking node as deleted: " + node.nodeId);
                    node.isDeleted = false;
                } else {
                    debug("redo", "ERROR: Node not found for ID: " + op.getCharacterId());
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    debug("redo", "Marking node as deleted: " + node.nodeId);
                    node.isDeleted = true;
                } else {
                    debug("redo", "ERROR: Node not found for ID: " + op.getPosition());
                }
            }

            addToHistory(userId, op);
            debug("redo", "Added operation back to history, document is now: " + getDocument());
            return true;
        } finally {
            lock.unlock();
        }
    }

    private CRDTNode findNodeById(String nodeId) {
        CRDTNode node = idNodeMap.get(nodeId);
        if (node == null && nodeId != null && !"-1".equals(nodeId)) {
            debug("findNodeById", "Node not found for ID: " + nodeId);
            logIdMapState("findNodeById", nodeId);
        } else if (node != null) {
            debug("findNodeById", "Found node: " + node.nodeId + ", char: '" + node.ch +
                    "', deleted: " + node.isDeleted);
        }
        return node;
    }

    private void addToHistory(int userId, Operation operation) {
        Deque<Operation> userHistory = history.computeIfAbsent(userId, k -> new ArrayDeque<>());
        userHistory.push(operation);
        debug("addToHistory", "Added operation to history for userId: " + userId +
                ", history size: " + userHistory.size());

        while (userHistory.size() > MAX_HISTORY) {
            Operation removed = userHistory.removeLast();
            debug("addToHistory", "Removed oldest operation from history: " + removed);
        }
    }
}
