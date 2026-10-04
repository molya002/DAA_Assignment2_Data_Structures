package edu.aitu.daa;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SequenceTest {
    private IntSequence create(boolean linked) {
        return linked ? new MyLinkedList() : new DynamicArray();
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void edgeCasesAndBounds(boolean linked) {
        IntSequence sequence = create(linked);
        assertFalse(sequence.contains(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.add(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.add(1, 2));
        sequence.add(0, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, sequence.remove(0));
        assertEquals(0, sequence.size());
        sequence.add(7); sequence.add(0, 7); sequence.add(2, Integer.MAX_VALUE);
        assertEquals(7, sequence.get(0));
        assertEquals(Integer.MAX_VALUE, sequence.get(2));
        assertTrue(sequence.contains(7));
        assertFalse(sequence.contains(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.remove(3));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.add(4, 0));
        assertEquals(3, sequence.size());
        assertEquals(Integer.MAX_VALUE, sequence.remove(2));
        sequence.add(9); // catches a stale tail after last-index removal
        assertEquals(9, sequence.get(2));
        while (sequence.size() != 0) sequence.remove(0);
        sequence.add(11); // catches a stale tail after singleton removal
        assertEquals(11, sequence.get(0));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void randomOperationsMatchReference(boolean linked) {
        for (int seed = 0; seed < 8; seed++) {
            Random random = new Random(seed);
            IntSequence actual = create(linked);
            ArrayList<Integer> expected = new ArrayList<>();
            for (int operation = 0; operation < 3000; operation++) {
                int value = random.nextInt(41) - 20;
                int choice = random.nextInt(5);
                if (choice == 0 || expected.isEmpty()) {
                    actual.add(value); expected.add(value);
                } else if (choice == 1) {
                    int index = random.nextInt(expected.size() + 1);
                    actual.add(index, value); expected.add(index, value);
                } else if (choice == 2) {
                    int index = random.nextInt(expected.size());
                    assertEquals(expected.remove(index).intValue(), actual.remove(index));
                } else if (choice == 3) {
                    int index = random.nextInt(expected.size());
                    assertEquals(expected.get(index).intValue(), actual.get(index));
                } else {
                    assertEquals(expected.contains(value), actual.contains(value));
                }
                assertEquals(expected.size(), actual.size());
                if (operation % 100 == 0) {
                    for (int i = 0; i < expected.size(); i++)
                        assertEquals(expected.get(i).intValue(), actual.get(i));
                }
            }
        }
    }
}
