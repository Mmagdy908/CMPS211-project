// package com.apt.project.collaborative_text_editor.model;

// import java.util.Objects;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// public class TreeCRDTOrderingTest {

//     private TreeCRDT crdt;

//     @BeforeEach
//     void setUp() {
//         crdt = new TreeCRDT();
//         crdt.initialize();
//     }

//     @Test
//     void testInsertionOrder() {
//         // Insert 'a'
//         crdt.insert(-1, 'a', 1);
//         assertEquals("a", crdt.getDocument());

//         // Insert 'b' after delay
//         try {
//             Thread.sleep(50);
//         } catch (InterruptedException e) {
//             // Ignore
//         }
//         crdt.insert(-1, 'b', 1);
//         assertEquals("ba", crdt.getDocument());

//         // Insert 'c' after delay
//         try {
//             Thread.sleep(50);
//         } catch (InterruptedException e) {
//             // Ignore
//         }
//         crdt.insert(-1, 'c', 1);
//         assertEquals("cba", crdt.getDocument());
//     }

//     @Test
//     void testInsertAtSameTime() {
//         // Create operations with the same timestamp for testing
//         long timestamp = System.currentTimeMillis();

//         // Create three operations manually with the same timestamp
//         Operation op1 = new Operation(Operation.Type.INSERT, -1, 'a', 1, timestamp);
//         Operation op2 = new Operation(Operation.Type.INSERT, -1, 'b', 2, timestamp);
//         Operation op3 = new Operation(Operation.Type.INSERT, -1, 'c', 3, timestamp);

//         // Apply operations
//         crdt.apply(op1);
//         crdt.apply(op2);
//         crdt.apply(op3);

//         // With same timestamp, userId should break ties (ascending order)
//         assertEquals("abc", crdt.getDocument());
//     }

//     @Test
//     void testDifferentParentsSameTime() {
//         // Insert 'a'
//         Operation opA = crdt.insert(-1, 'a', 1);

//         // Get ID for 'a'
//         int aId = Objects.hash(1, opA.getTimestamp());

//         // Insert 'b' and 'c' under 'a'
//         crdt.insert(aId, 'b', 1);
//         crdt.insert(aId, 'c', 1);

//         // Document should include parent-child relationships
//         assertEquals("acb", crdt.getDocument());
//     }
// }
