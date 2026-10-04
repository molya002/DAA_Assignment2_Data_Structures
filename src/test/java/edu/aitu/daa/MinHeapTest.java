package edu.aitu.daa;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {
    private void assertHeap(MinHeap heap) {
        int[] a = heap.snapshot();
        for (int i = 1; i < a.length; i++)
            assertTrue(a[(i - 1) / 2] <= a[i], "heap property at child " + i);
    }

    @Test void emptySingletonDuplicatesAndExtremes() {
        MinHeap heap = new MinHeap();
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
        heap.insert(7); assertHeap(heap);
        assertEquals(7, heap.peekMin());
        assertEquals(1, heap.size());
        assertEquals(7, heap.extractMin()); assertHeap(heap);
        assertThrows(IllegalStateException.class, heap::extractMin);
        for (int value : new int[]{7, 7, Integer.MAX_VALUE, Integer.MIN_VALUE, 0}) {
            heap.insert(value); assertHeap(heap);
        }
        for (int value : new int[]{Integer.MIN_VALUE, 0, 7, 7, Integer.MAX_VALUE}) {
            assertEquals(value, heap.extractMin()); assertHeap(heap);
        }
    }

    @Test void randomMixedOperationsMatchPriorityQueue() {
        for (int seed = 0; seed < 8; seed++) {
            Random random = new Random(seed);
            MinHeap heap = new MinHeap();
            PriorityQueue<Integer> expected = new PriorityQueue<>();
            for (int i = 0; i < 2500; i++) {
                if (expected.isEmpty() || random.nextInt(3) != 0) {
                    int value = random.nextInt(101) - 50;
                    heap.insert(value); expected.add(value);
                } else {
                    assertEquals(expected.remove().intValue(), heap.extractMin());
                }
                assertHeap(heap);
                assertEquals(expected.size(), heap.size());
                if (!expected.isEmpty()) assertEquals(expected.peek().intValue(), heap.peekMin());
            }
            while (!expected.isEmpty()) {
                assertEquals(expected.remove().intValue(), heap.extractMin()); assertHeap(heap);
            }
        }
    }

    @Test void floydBuildAndRepeatedInsertProduceSortedOutput() {
        Random random = new Random(42);
        for (int n : new int[]{0, 1, 2, 3, 8, 9, 100, 1001}) {
            int[] input = new int[n];
            for (int i = 0; i < n; i++) input[i] = random.nextInt();
            int[] original = input.clone();
            MinHeap built = MinHeap.buildHeap(input);
            assertArrayEquals(original, input);
            assertHeap(built);
            MinHeap inserted = new MinHeap();
            for (int value : input) { inserted.insert(value); assertHeap(inserted); }
            Arrays.sort(original);
            for (int value : original) {
                assertEquals(value, built.extractMin()); assertHeap(built);
                assertEquals(value, inserted.extractMin()); assertHeap(inserted);
            }
        }
        assertThrows(NullPointerException.class, () -> MinHeap.buildHeap(null));
    }

    @Test void countsHeapEventsExactly() {
        MinHeap heap = new MinHeap();
        heap.insert(3);
        heap.metrics().reset();
        heap.insert(1);
        assertEquals(4, heap.metrics().steps());
        assertEquals(2, heap.metrics().moves());
        assertEquals(1, heap.metrics().comparisons());
        heap.metrics().reset();
        assertEquals(1, heap.extractMin());
        assertEquals(2, heap.metrics().steps());
        assertEquals(1, heap.metrics().moves());
        assertEquals(0, heap.metrics().comparisons());
        heap.metrics().reset();
        assertEquals(3, heap.peekMin());
        assertEquals(1, heap.metrics().steps());
    }
}
