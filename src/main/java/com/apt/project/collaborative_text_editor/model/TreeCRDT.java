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

        CharacterID(int userId, long timestamp) {
            this.userId = userId;
            this.timestamp = timestamp;
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
            return userId == other.userId && timestamp == other.timestamp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, timestamp);
        }
    }

    static class CRDTNode implements Comparable<CRDTNode> {

        CharacterID id;
        Character ch;
        boolean isDeleted = false;
        TreeSet<CRDTNode> children = new TreeSet<>();
        int nodeId; // A unique identifier for the node

        CRDTNode(CharacterID id, Character ch) {
            this.id = id;
            this.ch = ch;
            this.nodeId = Objects.hash(id.userId, id.timestamp);
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
            return nodeId == node.nodeId;
        }

        @Override
        public int hashCode() {
            return nodeId;
        }
    }

    private final Map<CharacterID, CRDTNode> idNodeMap = new ConcurrentHashMap<>();
    private final CRDTNode root = new CRDTNode(new CharacterID(-1, Long.MAX_VALUE), null);
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
    public Operation insert(int parentId, Character ch, int userId) {
        lock.lock();
        try {
            long timestamp = System.currentTimeMillis();
            CharacterID id = new CharacterID(userId, timestamp);
            CRDTNode node = new CRDTNode(id, ch);

            CRDTNode parent = parentId == -1 ? root : findNodeById(parentId);
            if (parent == null) {
                parent = root; // Default to root if parent not found
            }

            parent.children.add(node);
            idNodeMap.put(id, node);

            Operation op = new Operation(Operation.Type.INSERT, parentId, ch, userId, timestamp);
            addToHistory(userId, op);
            return op;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Operation> insertText(int parentId, String text, int userId) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Operation> operations = new ArrayList<>();
        int currentParentId = parentId;

        lock.lock();
        try {
            for (int i = 0; i < text.length(); i++) {
                Operation op = insert(currentParentId, text.charAt(i), userId);
                operations.add(op);
                currentParentId = Objects.hash(userId, op.getTimestamp()); // Use the new node as parent for next character
            }
            return operations;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Operation delete(int charId, int userId) {
        lock.lock();
        try {
            CRDTNode node = findNodeById(charId);
            if (node != null && !node.isDeleted) {
                System.out.println("Before: "+getDocument());
                node.isDeleted = true;
                Operation op = new Operation(Operation.Type.DELETE, charId, node.ch, userId, System.currentTimeMillis());
                addToHistory(userId, op);
                System.out.println("After: "+getDocument());
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
            insert(op.getPosition(), op.getText(), op.getuserId());
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
    public List<Integer> getCharacterIds() {
        lock.lock();
        try {
            List<Integer> ids = new ArrayList<>();
            collectIds(root, ids);
            return ids;
        } finally {
            lock.unlock();
        }
    }

    private void collectIds(CRDTNode node, List<Integer> ids) {
        if (node != root && !node.isDeleted) {
            ids.add(node.nodeId);
        }

        for (CRDTNode child : node.children) {
            collectIds(child, ids);
        }
    }

    private void traverseTree(CRDTNode node, StringBuilder sb) {
        // No need to sort since the TreeSet already sorts by the compareTo method
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
                // Find the node and mark it as deleted
                CRDTNode node = findNodeById(Objects.hash(userId, op.getTimestamp()));
                if (node != null) {
                    node.isDeleted = true;
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                // Find the node and unmark it as deleted
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    node.isDeleted = false;
                }
            }

            // Push to redo stack
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
                // Find the node and unmark it as deleted
                CRDTNode node = findNodeById(Objects.hash(userId, op.getTimestamp()));
                if (node != null) {
                    node.isDeleted = false;
                }
            } else if (op.getType() == Operation.Type.DELETE) {
                // Find the node and mark it as deleted
                CRDTNode node = findNodeById(op.getPosition());
                if (node != null) {
                    node.isDeleted = true;
                }
            }

            // Push back to history
            addToHistory(userId, op);
            return true;
        } finally {
            lock.unlock();
        }
    }

    // Helper methods
    private CRDTNode findNodeById(int nodeId) {
        return idNodeMap.values().stream()
                .filter(n -> n.nodeId == nodeId)
                .findFirst()
                .orElse(null);
    }

    private void addToHistory(int userId, Operation operation) {
        Deque<Operation> userHistory = history.computeIfAbsent(userId, k -> new ArrayDeque<>());
        userHistory.push(operation);

        // Keep only MAX_HISTORY operations per user
        while (userHistory.size() > MAX_HISTORY) {
            userHistory.removeLast();
        }
    }
}
