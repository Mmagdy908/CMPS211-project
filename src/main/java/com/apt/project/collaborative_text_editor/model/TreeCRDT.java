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

    @Override
    public void initialize() {
        lock.lock();
        try {
            idNodeMap.clear();
            root.children.clear();
            history.clear();
            redoStack.clear();
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
            CharacterID id = new CharacterID(userId, timestamp, charId);
            CRDTNode node = new CRDTNode(id, ch);

            CRDTNode parent = "-1".equals(parentId) ? root : findNodeById(parentId);
            if (parent == null) {
                parent = root; // Default to root if parent not found
            }

            parent.children.add(node);
            idNodeMap.put(charId, node);

            Operation op = new Operation(Operation.Type.INSERT, parentId, ch, userId, timestamp, charId);
            addToHistory(userId, op);
            return op;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Operation> insertText(String parentId, String text, int userId, List<String> characterIds) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Operation> operations = new ArrayList<>();
        String currentParentId = parentId;

        lock.lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                // Use provided character ID if available, otherwise generate one
                String characterId = (characterIds != null && i < characterIds.size()) 
                    ? characterIds.get(i) 
                    : userId + "-" + System.currentTimeMillis() + "-" + i;
                
                Operation op = insert(currentParentId, text.charAt(i), userId, characterId);
                operations.add(op);
                currentParentId = op.getCharacterId();
            }
            return operations;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Operation> insertText(String parentId, String text, int userId) {
        // Use the new method with null characterIds for backward compatibility
        return insertText(parentId, text, userId, null);
    }

    @Override
    public Operation delete(String charId, int userId) {
        lock.lock();
        try {
            CRDTNode node = findNodeById(charId);
            if (node != null && !node.isDeleted) {
                node.isDeleted = true;
                Operation op = new Operation(Operation.Type.DELETE, charId, node.ch, userId, System.currentTimeMillis());
                addToHistory(userId, op);
                return op;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void apply(Operation op) {
        if (op == null) {
            return;
        }

        if (op.getType() == Operation.Type.INSERT) {
            insert(op.getPosition(), op.getText(), op.getuserId(), op.getCharacterId());
        } else if (op.getType() == Operation.Type.DELETE) {
            delete(op.getPosition(), op.getuserId());
        }
    }

    @Override
    public String getDocument() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            traverseTree(root, sb);
            return sb.toString();
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
            Deque<Operation> ops = history.getOrDefault(userId, new ArrayDeque<>());
            if (ops.isEmpty()) {
                return false;
            }

            Operation op = ops.pop();
            if (op.getType() == Operation.Type.INSERT) {
                CRDTNode node = findNodeById(op.getCharacterId());
                if (node != null) {
                    node.isDeleted = true;
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    node.isDeleted = false;
                }
            }

            redoStack.computeIfAbsent(userId, k -> new ArrayDeque<>()).push(op);
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
            Deque<Operation> redo = redoStack.getOrDefault(userId, new ArrayDeque<>());
            if (redo.isEmpty()) {
                return false;
            }

            Operation op = redo.pop();
            if (op.getType() == Operation.Type.INSERT) {
                CRDTNode node = findNodeById(op.getCharacterId());
                if (node != null) {
                    node.isDeleted = false;
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    node.isDeleted = true;
                }
            }

            addToHistory(userId, op);
            return true;
        } finally {
            lock.unlock();
        }
    }

    private CRDTNode findNodeById(String nodeId) {
        return idNodeMap.get(nodeId);
    }

    private void addToHistory(int userId, Operation operation) {
        Deque<Operation> userHistory = history.computeIfAbsent(userId, k -> new ArrayDeque<>());
        userHistory.push(operation);

        while (userHistory.size() > MAX_HISTORY) {
            userHistory.removeLast();
        }
    }
}
