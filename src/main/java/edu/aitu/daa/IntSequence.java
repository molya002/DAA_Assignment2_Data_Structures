package edu.aitu.daa;

/** add(index,x) permits 0..size; get/remove permit 0..size-1. */
public interface IntSequence {
    void add(int value);
    void add(int index, int value);
    int remove(int index);
    int get(int index);
    boolean contains(int value);
    int size();
    Metrics metrics();
}
