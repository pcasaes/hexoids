package me.pcasaes.bbop.util.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleMutatorMultipleAccessorConcurrentHashMapTest {

    private static class KeyClass {
        private final int id;

        private KeyClass(int id) {
            this.id = id;
        }

        public static KeyClass of(int id) {
            return new KeyClass(id);
        }

        public static boolean is(int id, KeyClass obj) {
            return obj.id == id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyClass keyClass = (KeyClass) o;
            return id == keyClass.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @Test
    void testNewEmpty() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(16, 0.5f);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    void testAddTwiceSameKey() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(16, 0.5f);

        KeyClass first = KeyClass.of(1);
        assertNull(map.put(1, first));
        assertFalse(map.isEmpty());

        assertEquals(KeyClass.of(1), map.get(1));
        assertEquals(1, map.size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.keySet().size());
        assertEquals(1, map.entrySet().size());

        KeyClass second = KeyClass.of(1);
        assertSame(first, map.put(1, second));
        assertSame(second, map.get(1));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.keySet().size());
        assertEquals(1, map.entrySet().size());
    }

    @Test
    void testNegativeKey() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(16, 0.5f);

        KeyClass first = KeyClass.of(-1);
        assertNull(map.put(-1, first));
        assertFalse(map.isEmpty());

        assertEquals(KeyClass.of(-1), map.get(-1));
        assertEquals(1, map.size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.keySet().size());
        assertEquals(1, map.entrySet().size());

    }

    @Test
    void testAddAndRemove() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(16, 0.5f);

        assertNull(map.remove(1));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());

        assertNull(map.put(1, KeyClass.of(1)));
        assertFalse(map.isEmpty());
        assertEquals(KeyClass.of(1), map.get(1));
        assertEquals(1, map.size());
        assertEquals(1, map.values().size());
        assertEquals(1, map.keySet().size());
        assertEquals(1, map.entrySet().size());

        assertEquals(KeyClass.of(1), map.remove(1));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    void testRehash() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(2, 0.5f);

        for (int i = 0; i < 64; i++) {
            map.put(i, KeyClass.of(i));
        }

        assertFalse(map.isEmpty());
        assertEquals(64, map.size());
        assertEquals(64, map.values().size());
        assertEquals(64, map.keySet().size());
        assertEquals(64, map.entrySet().size());


        for (int i = 0; i < 64; i++) {
            assertEquals(KeyClass.of(i), map.get(i));
        }
    }

    @Test
    void testEntrySet() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(256, 0.5f);

        for (int i = 0; i < 64; i++) {
            map.put(i, KeyClass.of(i));
        }

        assertFalse(map.isEmpty());
        assertEquals(64, map.size());
        assertEquals(64, map.values().size());
        assertEquals(64, map.keySet().size());
        assertEquals(64, map.entrySet().size());

        final Set<Integer> keys = new HashSet<>();
        map.entrySet()
                .forEach(entry -> {
                    assertTrue(keys.add(entry.getKey()));
                    assertTrue(KeyClass.is(entry.getKey(), entry.getValue()));
                });

        assertEquals(64, keys.size());

        Iterator<Map.Entry<Integer, KeyClass>> iterator = map.entrySet().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            removed++;
        }

        assertTrue(map.isEmpty());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    void testKeySet() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(256, 0.5f);

        for (int i = 0; i < 64; i++) {
            map.put(i, KeyClass.of(i));
        }

        assertFalse(map.isEmpty());
        assertEquals(64, map.size());
        assertEquals(64, map.values().size());
        assertEquals(64, map.keySet().size());
        assertEquals(64, map.entrySet().size());

        final Set<Integer> keys = new HashSet<>();
        map.keySet()
                .forEach(key -> {
                    assertTrue(keys.add(key));
                });

        assertEquals(64, keys.size());


        Iterator<Integer> iterator = map.keySet().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            removed++;
        }

        assertEquals(64, removed);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());

    }

    @Test
    void testValue() {
        Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(256, 0.5f);

        for (int i = 0; i < 64; i++) {
            map.put(i, KeyClass.of(i));
        }

        assertFalse(map.isEmpty());
        assertEquals(64, map.size());
        assertEquals(64, map.values().size());
        assertEquals(64, map.keySet().size());
        assertEquals(64, map.entrySet().size());

        final Set<KeyClass> values = new HashSet<>();
        map.values()
                .forEach(value -> {
                    assertTrue(values.add(value));
                });

        assertEquals(64, values.size());


        Iterator<KeyClass> iterator = map.values().iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            removed++;
        }

        assertEquals(64, removed);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());

    }

    @Test
    void testMultipleAccessorThreads() {

        final Map<Integer, KeyClass> map = new SingleMutatorMultipleAccessorConcurrentHashMap<>(8192, 0.5f);

        final AtomicBoolean stop = new AtomicBoolean(false);

        int numRunners = 10;

        class Runner implements Runnable {

            Map<Integer, KeyClass> found = new HashMap<>();
            volatile boolean stopped = false;

            @Override
            public void run() {
                while (!stop.get()) {
                    found.putAll(map);
                }
                stopped = true;
            }

            int size() {
                return found.size();
            }

            public Map<Integer, KeyClass> getFound() {
                return found;
            }

            public boolean isStopped() {
                return stopped;
            }
        }

        List<Runner> runners = new ArrayList<>(numRunners);
        for (int i = 0; i < numRunners; i++) {
            Runner runner = new Runner();
            runners.add(runner);
            new Thread(runner).start();
        }

        for (int i = 0; i < 8192; i++) {
            map.put(i, KeyClass.of(i));
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> runners
                .stream()
                .map(Runner::size)
                .filter(s -> s == 8192)
                .count() == numRunners);

        stop.set(true);
        await().atMost(5, TimeUnit.SECONDS).until(() -> runners
                .stream()
                .filter(Runner::isStopped)
                .count() == numRunners);

        runners
                .stream()
                .map(Runner::size)
                .forEach(s -> assertEquals(8192, s));

        runners
                .forEach(r -> {
                    for (int i = 0; i < 8192; i++) {
                        KeyClass inMap = map.get(i);
                        assertNotNull(inMap);
                        assertEquals(KeyClass.of(i), inMap);
                    }
                });

        runners
                .stream()
                .map(Runner::getFound)
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .forEach(entry -> {
                    KeyClass inMap = map.get(entry.getKey());
                    assertNotNull(inMap);
                    assertEquals(entry.getValue(), inMap);
                });

    }
}