package me.pcasaes.hexoids.core.domain.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.LongConsumer;

public class PhysicsMetrics {

    private static final PhysicsMetrics instance = new PhysicsMetrics();

    public static PhysicsMetrics get() {
        return instance;
    }

    private PhysicsMetrics() {
    }

    private final Map<String, List<Long>> measurements = new HashMap<>();

    public LongConsumer intercept(LongConsumer execution, String name) {
        List<Long> measures = new ArrayList<>();
        measurements.put(name, measures);
        return t -> {
            long start = System.nanoTime();
            try {
                execution.accept(t);
            } finally {
                measures.add(System.nanoTime() - start);
            }
        };
    }

    public void flush(BiConsumer<String, List<Long>> consumeMeasurements) {
        measurements
                .forEach((n, list) -> {
                    consumeMeasurements.accept(n, new ArrayList<>(list));
                    list.clear();
                });
    }
}
