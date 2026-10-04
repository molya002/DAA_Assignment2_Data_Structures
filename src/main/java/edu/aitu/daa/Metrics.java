package edu.aitu.daa;

/** Physical events, accumulated inside the data structure (never inferred from n). */
public final class Metrics {
    private long steps;
    private long moves;
    private long comparisons;

    public void step() { steps++; }
    public void move() { moves++; }
    public void compare() { comparisons++; }
    public long steps() { return steps; }
    public long moves() { return moves; }
    public long comparisons() { return comparisons; }
    public void reset() { steps = moves = comparisons = 0; }
}
