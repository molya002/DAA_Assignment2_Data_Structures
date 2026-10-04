package edu.aitu.daa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Hand-calculated event traces, independent of the benchmark's aggregate totals. */
class MetricsContractTest {
    private void counts(Metrics m, long steps, long moves, long comparisons) {
        assertEquals(steps, m.steps(), "steps");
        assertEquals(moves, m.moves(), "moves");
        assertEquals(comparisons, m.comparisons(), "comparisons");
    }

    @Test void searchStopsAtFirstDuplicateAndCountsMisses() {
        for (IntSequence s : new IntSequence[]{new DynamicArray(), new MyLinkedList()}) {
            for (int x : new int[]{4, 7, 7}) s.add(x);
            boolean linked = s instanceof MyLinkedList;
            s.metrics().reset();
            assertTrue(s.contains(7));
            counts(s.metrics(), linked ? 1 : 2, 0, 2);
            s.metrics().reset();
            assertFalse(s.contains(99));
            counts(s.metrics(), 3, 0, 3);
        }
    }

    @Test void indexedInsertAndRemoveCountPhysicalEvents() {
        for (IntSequence s : new IntSequence[]{new DynamicArray(), new MyLinkedList()}) {
            for (int x : new int[]{10, 20, 30}) s.add(x);
            boolean linked = s instanceof MyLinkedList;
            s.metrics().reset();
            s.add(1, 99);
            counts(s.metrics(), linked ? 1 : 2, 2, 0);
            s.metrics().reset();
            assertEquals(99, s.remove(1));
            counts(s.metrics(), linked ? 2 : 3, linked ? 1 : 2, 0);
            assertEquals(3, s.size());
            assertEquals(10, s.get(0));
            assertEquals(20, s.get(1));
            assertEquals(30, s.get(2));
        }
    }

    @Test void invalidCallsLeaveContentsAndCountersUnchanged() {
        for (IntSequence s : new IntSequence[]{new DynamicArray(), new MyLinkedList()}) {
            s.add(12); s.add(34); s.metrics().reset();
            assertThrows(IndexOutOfBoundsException.class, () -> s.add(3, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> s.remove(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> s.get(2));
            counts(s.metrics(), 0, 0, 0);
            assertEquals(2, s.size());
            assertEquals(12, s.get(0)); assertEquals(34, s.get(1));
        }
        MinHeap h = new MinHeap();
        assertThrows(IllegalStateException.class, h::peekMin);
        assertThrows(IllegalStateException.class, h::extractMin);
        counts(h.metrics(), 0, 0, 0);
    }

    @Test void heapGrowthCountsCopyAndParentComparison() {
        MinHeap h = new MinHeap();
        for (int i = 0; i < 8; i++) h.insert(i);
        h.metrics().reset(); h.insert(8);
        counts(h.metrics(), 10, 8, 1);
        assertArrayEquals(new int[]{0,1,2,3,4,5,6,7,8}, h.snapshot());
    }

    @Test void bubbleDownChoosesRightChildThenSingleLeftChild() {
        MinHeap h = MinHeap.buildHeap(new int[]{1,4,2,8,5,3,9});
        h.metrics().reset();
        assertEquals(1, h.extractMin());
        counts(h.metrics(), 12, 5, 3);
        assertArrayEquals(new int[]{2,4,3,8,5,9}, h.snapshot());
    }

    @Test void floydCountsInputCopyAndDoesNotAliasCaller() {
        int[] input = {3,1,2};
        MinHeap h = MinHeap.buildHeap(input);
        counts(h.metrics(), 9, 5, 2);
        input[0] = Integer.MIN_VALUE;
        assertArrayEquals(new int[]{1,3,2}, h.snapshot());
        int[] snapshot = h.snapshot(); snapshot[0] = 99;
        assertEquals(1, h.peekMin());
    }
}
