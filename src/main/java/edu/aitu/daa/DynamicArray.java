package edu.aitu.daa;

public final class DynamicArray implements IntSequence {
    private int[] elements = new int[8];
    private int size;
    private final Metrics metrics = new Metrics();

    @Override public int size() { return size; }
    @Override public Metrics metrics() { return metrics; }

    private int read(int index) {
        metrics.step();
        return elements[index];
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

    @Override public void add(int value) {
        ensureCapacity();
        elements[size++] = value;
    }

    @Override public void add(int index, int value) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
        ensureCapacity();
        for (int i = size; i > index; i--) {
            elements[i] = read(i - 1);
            metrics.move();
        }
        elements[index] = value;
        size++;
    }

    @Override public int remove(int index) {
        checkElementIndex(index);
        int removed = read(index);
        // Before iteration i, [index,i) already contains the next original values.
        for (int i = index; i < size - 1; i++) {
            elements[i] = read(i + 1);
            metrics.move();
        }
        size--;
        return removed;
    }

    @Override public int get(int index) {
        checkElementIndex(index);
        return read(index);
    }

    @Override public boolean contains(int value) {
        // Before iteration i, no element in [0,i) equals value.
        for (int i = 0; i < size; i++) {
            int current = read(i);
            metrics.compare();
            if (current == value) return true;
        }
        return false;
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }
}
