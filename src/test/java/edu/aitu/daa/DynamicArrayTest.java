package edu.aitu.daa;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicArrayTest {
    @Test void doublesCapacityAndPreservesOrder() {
        DynamicArray array = new DynamicArray();
        for (int i = 0; i < 4097; i++) array.add(i);
        assertEquals(4097, array.size());
        for (int i = 0; i < array.size(); i++) assertEquals(i, array.get(i));
    }

    @Test void countsReadsShiftsAndGrowthExactly() {
        DynamicArray array = new DynamicArray();
        for (int i = 0; i < 8; i++) array.add(i);
        array.metrics().reset();
        array.add(8);
        assertEquals(8, array.metrics().steps());
        assertEquals(8, array.metrics().moves());
        array.metrics().reset();
        assertEquals(3, array.remove(3));
        assertEquals(6, array.metrics().steps()); // returned value + five shifts
        assertEquals(5, array.metrics().moves());
        array.metrics().reset();
        assertEquals(4, array.get(3));
        assertEquals(1, array.metrics().steps());
        assertEquals(0, array.metrics().comparisons());
    }
}
