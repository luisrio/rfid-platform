package org.example.rfid.scenario.engine;

import java.util.ArrayDeque;
import java.util.List;

public final class TestRandomSource implements RandomSource {

    private final ArrayDeque<Double> doubles;
    private final ArrayDeque<Integer> ints;

    public TestRandomSource(List<Double> doubles, List<Integer> ints) {
        this.doubles = new ArrayDeque<>(doubles);
        this.ints = new ArrayDeque<>(ints);
    }

    @Override
    public double nextDouble() {
        if (doubles.isEmpty()) {
            return 0.0;
        }
        return doubles.removeFirst();
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be > 0");
        }
        if (ints.isEmpty()) {
            return 0;
        }
        return Math.floorMod(ints.removeFirst(), bound);
    }
}
