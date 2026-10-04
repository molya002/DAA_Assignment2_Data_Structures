package edu.aitu.daa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyLinkedListTest {
    @Test void countsTraversalAndHeadPointerUpdates() {
        MyLinkedList list = new MyLinkedList();
        list.add(1); list.add(2); list.add(3);
        list.metrics().reset();
        assertEquals(3, list.get(2));
        assertEquals(2, list.metrics().steps());
        list.metrics().reset();
        list.add(0, 9);
        assertEquals(0, list.metrics().steps());
        assertEquals(2, list.metrics().moves());
        assertEquals(9, list.remove(0));
        assertEquals(1, list.metrics().steps());
        assertEquals(3, list.metrics().moves());
        list.metrics().reset();
        assertFalse(list.contains(99));
        assertEquals(3, list.metrics().steps());
        assertEquals(3, list.metrics().comparisons());
    }
}
