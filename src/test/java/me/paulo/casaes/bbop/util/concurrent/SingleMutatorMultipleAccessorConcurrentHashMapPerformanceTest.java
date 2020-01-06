package me.paulo.casaes.bbop.util.concurrent;

import me.paulo.casaes.bbop.model.Clock;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@EnabledIfEnvironmentVariable(named = "TEST.BENCHMARK", matches = "true")
public class SingleMutatorMultipleAccessorConcurrentHashMapPerformanceTest {

    private static final Logger LOGGER = Logger.getLogger(SingleMutatorMultipleAccessorConcurrentHashMapPerformanceTest.class.getName());

    private static final int INITIAL_CAPACITY = 8192;

    private static final Random RNG = new Random(9756395730485L);

    private static final Long[] INPUT_DATA = new Long[INITIAL_CAPACITY * 4];

    private static final int INPUT_DATA_WRAP_MASK = INPUT_DATA.length - 1;

    static {
        for (int i = 0; i < INPUT_DATA.length; i++) {
            INPUT_DATA[i] = RNG.nextLong();
        }
    }

    enum Type {
        HASH_MAP(() -> new HashMap<>(INITIAL_CAPACITY, 0.5F)),
        SINGLE_MUTATOR_MULTIPLE_ACCESSOR_CONCURRENT_HASH_MAP(() -> new SingleMutatorMultipleAccessorConcurrentHashMap<>(INITIAL_CAPACITY, 0.5F, 50)),
        CONCURRENT_HASH_MAP(() -> new ConcurrentHashMap<>(INITIAL_CAPACITY, 0.5F)),
        ;

        private final Supplier<Map<Long, Long>> createInstance;
        private final List<Map<Long, Long>> maps = new ArrayList<>(2_000);

        Type(Supplier<Map<Long, Long>> createInstance) {
            this.createInstance = createInstance;
        }

        Map<Long, Long> newMapInstance() {
            Map<Long, Long> map = createInstance.get();
            maps.add(map);
            return map;
        }

        void reset() {
            maps.clear();
        }

        boolean isThreadSafe() {
            return ordinal() > 0;
        }
    }

    private Type type;


    private int n = 0;

    @BeforeEach
    void setup(RepetitionInfo repetitionInfo) {
        type = Type.values()[repetitionInfo.getCurrentRepetition() - 1];
        LOGGER.info("Testing " + type);
        n = 0;
        System.gc();
    }

    @AfterEach
    void wrapUp(RepetitionInfo repetitionInfo) {
        if (type == Type.SINGLE_MUTATOR_MULTIPLE_ACCESSOR_CONCURRENT_HASH_MAP) {
            type.maps
                    .stream()
                    .map(m -> (SingleMutatorMultipleAccessorConcurrentHashMap<Long, Long>) m)
                    .forEach(m -> {
                        //make sure we don't rehash
                        assertEquals(INITIAL_CAPACITY * 4, m.getCapacity());

                        // shouldn't raise hash limit too much
                        assertTrue(m.getCurrentMaxHashAttempts() < 10);
                    });
        }
        type.reset();
    }

    private Long getLong() {
        try {
            return INPUT_DATA[n];
        } finally {
            n = (n + 1) & INPUT_DATA_WRAP_MASK;
        }
    }

    private void profiled(Runnable runnable) {
        long start = Clock.get().getTime();
        runnable.run();
        LOGGER.info(type + " ran in\t" + (Clock.get().getTime() - start) + "ms");
    }

    @RepeatedTest(3)
    void testPut() {

        Map<Long, Long>[] maps = new Map[1_000];
        for (int a = 0; a < 1_000; a++) {
            maps[a] = type.newMapInstance();
        }


        profiled(() -> {
            for (Map<Long, Long> map : maps) {

                for (int i = 0; i < INITIAL_CAPACITY; i++) {
                    Long val = getLong();
                    map.put(val, val);
                }
                assertEquals(INITIAL_CAPACITY, map.size());
            }
        });
    }

    @RepeatedTest(3)
    void testGet() {
        Map<Long, Long> map = type.newMapInstance();
        List<Long> keys = new ArrayList<>();
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            Long val = getLong();
            map.put(val, val);
            keys.add(val);
        }

        profiled(() -> {
            for (int a = 0; a < 15_000; a++) {

                for (Long key : keys) {
                    map.get(key);
                }

            }
        });
        assertEquals(INITIAL_CAPACITY, map.size());
    }


    @RepeatedTest(3)
    void testConcurrency() {
        if (!type.isThreadSafe()) {
            return;
        }

        final long TIME = 5_000L;

        final Long[] data = new Long[INITIAL_CAPACITY];

        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            long val = getLong();
            data[i] = val;
        }

        final Map<Long, Long> map = type.newMapInstance();

        int numRunners = 10;

        class Runner implements Runnable {

            volatile boolean stopped = false;

            volatile long ops = 0;

            @Override
            public void run() {
                long o = 0;
                long start = Clock.get().getTime();
                outer:
                while (true) {
                    for (Long val : data) {
                        if (val != null) {
                            map.get(val);
                            o++;
                        }
                        if (Clock.get().getTime() - start >= TIME) {
                            break outer;
                        }
                    }
                }
                ops = o;
                stopped = true;
            }

            public boolean isStopped() {
                return stopped;
            }

            public long getOps() {
                return ops;
            }
        }

        List<Runner> runners = new ArrayList<>(numRunners);
        for (int i = 0; i < numRunners; i++) {
            Runner runner = new Runner();
            runners.add(runner);
            new Thread(runner).start();
        }

        long start = Clock.get().getTime();
        long writeOps = 0;
        for (int i = 0; i < 10_00_000; i++) {
            for (Long val : data) {
                map.put(val, val);
                writeOps++;
                if (Clock.get().getTime() - start >= TIME) {
                    i = 10_00_000;
                    break;
                }
            }
            for (Long val : data) {
                map.remove(val);
                writeOps++;
                if (Clock.get().getTime() - start >= TIME) {
                    i = 10_00_000;
                    break;
                }
            }
        }

        LOGGER.info(type + " write ops " + (writeOps / (TIME / 1000.0)));


        try {
            await().atMost(5, TimeUnit.SECONDS).until(() -> runners
                    .stream()
                    .filter(Runner::isStopped)
                    .count() == numRunners);
        } catch (ConditionTimeoutException ex) {
            fail(ex);
        }

        runners
                .stream()
                .map(Runner::getOps)
                .forEach(o -> LOGGER.info(type + " read ops " + (o / (TIME / 1000.0))));


    }
}
