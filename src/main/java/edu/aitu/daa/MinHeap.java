package edu.aitu.daa;

public final class MinHeap {
    private int[] elements = new int[8];
    private int size;
    private final Metrics metrics = new Metrics();

    public int size() { return size; }
    public Metrics metrics() { return metrics; }

    private int read(int index) {
        metrics.step();
        return elements[index];
    }

    private boolean less(int left, int right) {
        int a = read(left);
        int b = read(right);
        metrics.compare();
        return a < b;
    }

    private void swap(int left, int right) {
        int value = read(left);
        elements[left] = read(right);
        metrics.move();
        elements[right] = value;
        metrics.move();
    }

    private void ensureCapacity() {
        if (size < elements.length) return;
        int[] expanded = new int[Math.multiplyExact(elements.length, 2)];
        for (int i = 0; i < size; i++) {
            expanded[i] = read(i);
            metrics.move();
        }
        elements = expanded;
    }

    public void insert(int value) {
        ensureCapacity();
        int child = size++;
        elements[child] = value;
        while (child > 0) {
            int parent = (child - 1) / 2;
            if (!less(child, parent)) break;
            swap(child, parent);
            child = parent;
        }
    }

    public int peekMin() {
        checkNotEmpty();
        return read(0);
    }

    public int extractMin() {
        checkNotEmpty();
        int minimum = read(0);
        size--;
        if (size > 0) {
            elements[0] = read(size);
            metrics.move();
            bubbleDown(0);
        }
        return minimum;
    }

    private void bubbleDown(int parent) {
        // parent < size/2 means it has a left child; also avoids index overflow.
        while (parent < size / 2) {
            int left = 2 * parent + 1;
            int right = left + 1;
            int smallest = left;
            if (right < size && less(right, left)) smallest = right;
            if (!less(smallest, parent)) break;
            swap(parent, smallest);
            parent = smallest;
        }
    }

    /** Floyd build: copies input (caller retains ownership), then repairs bottom up. */
    public static MinHeap buildHeap(int[] input) {
        if (input == null) throw new NullPointerException("input");
        MinHeap heap = new MinHeap();
        heap.elements = new int[Math.max(8, input.length)];
        heap.size = input.length;
        for (int i = 0; i < input.length; i++) {
            heap.metrics.step();
            heap.elements[i] = input[i];
            heap.metrics.move();
        }
        for (int parent = heap.size / 2 - 1; parent >= 0; parent--) heap.bubbleDown(parent);
        return heap;
    }

    /** Diagnostic copy: deliberately excluded from algorithm counters and timings. */
    public int[] snapshot() {
        int[] copy = new int[size];
        System.arraycopy(elements, 0, copy, 0, size);
        return copy;
    }

    private void checkNotEmpty() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
    }
}
