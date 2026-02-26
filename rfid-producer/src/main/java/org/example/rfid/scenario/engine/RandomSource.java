package org.example.rfid.scenario.engine;

import java.util.concurrent.ThreadLocalRandom;

interface RandomSource {

    double nextDouble();

    int nextInt(int bound);

    static RandomSource threadLocal() {
        return new RandomSource() {
            @Override
            public double nextDouble() {
                return ThreadLocalRandom.current().nextDouble();
            }

            @Override
            public int nextInt(int bound) {
                return ThreadLocalRandom.current().nextInt(bound);
            }
        };
    }
}
