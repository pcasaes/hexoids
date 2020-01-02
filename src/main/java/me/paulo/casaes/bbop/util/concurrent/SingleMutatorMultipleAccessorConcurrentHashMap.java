package me.paulo.casaes.bbop.util.concurrent;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class SingleMutatorMultipleAccessorConcurrentHashMap<K, V> implements Map<K, V> {

    private static final int DEFAULT_CAPACITY = 512;

    private static final float DEFAULT_LOAD_FACTOR = 0.5f;

    private static final int DEFAULT_MAX_HASH_LIMIT = 50;

    private static final int MAXIMUM_CAPACITY = 1 << 30;

    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    private MyEntry<K, V>[] entries;
    private int wrapMask;
    private int size;
    private final int hashAttemptsLimit;
    private int currentMaxHashAttempts;
    private MyEntry<K, V> head;
    private MyEntry<K, V> tail;

    private final Collection<V> values;
    private final Set<K> keySet;
    private final Set<Map.Entry<K, V>> entrySet;

    public SingleMutatorMultipleAccessorConcurrentHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_MAX_HASH_LIMIT);
    }

    public SingleMutatorMultipleAccessorConcurrentHashMap(int initialCapacity,
                                                          float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_MAX_HASH_LIMIT);
    }

    public SingleMutatorMultipleAccessorConcurrentHashMap(int initialCapacity,
                                                          float loadFactor,
                                                          int hashAttemptsLimit) {
        this(getNearestPowerOfTwo(1 + (int) Math.ceil(initialCapacity / loadFactor)), hashAttemptsLimit);
    }

    private SingleMutatorMultipleAccessorConcurrentHashMap(int entriesSize,
                                                           int hashAttemptsLimit) {
        setEntriesSize(entriesSize);
        this.hashAttemptsLimit = hashAttemptsLimit;
        this.currentMaxHashAttempts = 0;

        this.size = 0;

        this.values = new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new ValueIterator();
            }

            @Override
            public int size() {
                return SingleMutatorMultipleAccessorConcurrentHashMap.this.size;
            }
        };

        this.keySet = new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                return new KeyIterator();
            }

            @Override
            public int size() {
                return SingleMutatorMultipleAccessorConcurrentHashMap.this.size;
            }
        };


        this.entrySet = new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new EntryIterator();
            }

            @Override
            public int size() {
                return SingleMutatorMultipleAccessorConcurrentHashMap.this.size;
            }
        };
    }

    private void setEntriesSize(int entriesSize) {
        this.wrapMask = entriesSize - 1;
        this.entries = new MyEntry[entriesSize];
        this.size = 0;
        this.head = null;
        this.tail = null;
    }

    private static int getNearestPowerOfTwo(int v) {
        v--;
        v |= v >>> 1;
        v |= v >>> 2;
        v |= v >>> 4;
        v |= v >>> 8;
        v |= v >>> 16;
        v++;
        return v;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    /**
     * Taken from {@link java.util.concurrent.ConcurrentHashMap}
     */
    private static int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    private static int hashCode(int attempt, int hashedKey) {
        return 31 * (31 + attempt) + hashedKey;
    }

    private int getHash(int attempt, int hashedKey) {
        return spread(hashCode(attempt, hashedKey)) % this.wrapMask;

    }

    @Override
    public V get(Object key) {
        MyEntry<K, V> entry = getEntry(key);
        return entry == null ? null : entry.value;
    }

    private MyEntry<K, V> getEntry(Object key) {
        int hashedKey = key.hashCode();
        for (int i = 1; i <= this.currentMaxHashAttempts; i++) {
            int hash = getHash(i, hashedKey);

            MyEntry<K, V> entry = entries[hash];
            if (entry != null && entry.key.equals(key)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        while (true) {
            try {
                return putAttempt(key, value);
            } catch (HashLimitReachedException ex) {
                rehash();
            }
        }
    }

    private void rehash() {
        while (this.entries.length < MAXIMUM_CAPACITY) {
            if (rehash(this.entries.length * 2)) {
                return;
            }
        }
        throw new HashLimitReachedException();
    }

    public int getHashAttemptsLimit() {
        return hashAttemptsLimit;
    }

    public int getCurrentMaxHashAttempts() {
        return currentMaxHashAttempts;
    }

    public int getCapacity() {
        return entries.length;
    }

    private boolean rehash(int newSize) {
        SingleMutatorMultipleAccessorConcurrentHashMap<K, V> newMap = new SingleMutatorMultipleAccessorConcurrentHashMap<>(newSize, hashAttemptsLimit);
        try {
            this.forEach(newMap::put);

            this.entries = newMap.entries;
            this.wrapMask = newMap.wrapMask;
            this.currentMaxHashAttempts = newMap.currentMaxHashAttempts;
            this.head = newMap.head;
            this.tail = newMap.tail;
            this.size = newMap.size;
        } catch (HashLimitReachedException ex) {
            return false;
        }
        return true;
    }

    private void updateCurrentMaxHashAttempts(int i) {
        if (i > this.currentMaxHashAttempts) {
            this.currentMaxHashAttempts = i;
        }
    }

    private V putAttempt(K key, V value) {
        int hashedKey = key.hashCode();
        for (int i = 1; i <= this.hashAttemptsLimit; i++) {
            int hash = getHash(i, hashedKey);

            MyEntry<K, V> entry = entries[hash];
            if (entry == null || entry.key.equals(key)) {
                updateCurrentMaxHashAttempts(i);
                if (entry == null) {
                    entry = new MyEntry<>(hash, key, value, tail);
                    this.entries[hash] = entry;

                    tail = entry;
                    if (this.head == null) {
                        this.head = entry;
                    }
                    this.size++;
                    return null;
                }
                return entry.setValue(value);
            }
        }
        throw new HashLimitReachedException();
    }

    @Override
    public V remove(Object key) {
        MyEntry<K, V> entry = getEntry(key);
        if (entry != null) {
            V value = entry.value;
            removeEntry(entry);
            return value;
        }
        return null;
    }

    private void removeEntry(MyEntry<K, V> entry) {
        if (entry == head) {
            head = entry.next;
        }
        if (entry == tail) {
            tail = entry.prev;
        }
        if (entry.prev != null) {
            entry.prev.next = entry.next;
        } else {
            head = entry.next;
        }

        if (entry.next != null) {
            entry.next.prev = entry.prev;
        } else {
            tail = entry.prev;
        }

        entries[entry.usedHash] = null;
        this.size--;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        Arrays.fill(entries, null);
        this.size = 0;
        this.currentMaxHashAttempts = 0;
        this.tail = null;
        this.head = null;
    }

    @Override
    public Set<K> keySet() {
        return keySet;
    }

    @Override
    public Collection<V> values() {
        return values;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return entrySet;
    }

    private static class MyEntry<K, V> implements Map.Entry<K, V> {

        private final int usedHash;
        private final K key;
        private V value;
        private MyEntry<K, V> prev;
        private MyEntry<K, V> next;

        private MyEntry(int usedHash, K key, V value, MyEntry<K, V> prev) {
            this.usedHash = usedHash;
            this.key = key;
            this.value = value;
            link(prev);
        }

        private void link(MyEntry<K, V> prev) {
            this.prev = prev;
            if (prev != null) {
                prev.next = this;
            }
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V v = this.value;
            this.value = value;
            return v;
        }
    }


    private static class HashLimitReachedException extends RuntimeException {
        private HashLimitReachedException() {
            super("Reached limit of hash attempts");
        }
    }

    private class EntryIterator implements Iterator<Map.Entry<K, V>> {

        private MyEntry<K, V> nextEntry;
        private MyEntry<K, V> currentEntry;
        private K nextKey;
        private V nextValue;

        public EntryIterator() {
            prepareNext();
        }

        @Override
        public boolean hasNext() {
            return nextKey != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            this.currentEntry = this.nextEntry;
            prepareNext();
            return this.currentEntry;
        }

        @Override
        public void remove() {
            if (currentEntry == null) {
                throw new IllegalStateException("Must call next before remove. Remove can only be called once per next");
            }
            MyEntry<K, V> toRemove = currentEntry;
            currentEntry = null;

            removeEntry(toRemove);
        }

        private void prepareNext() {
            while ((nextEntry = nextEntry == null ? head : nextEntry.next) != null) {
                K key = nextEntry.key;
                V value = nextEntry.value;

                if (key != null && value != null) {
                    nextKey = key;
                    nextValue = value;
                    break;
                }
            }
            if (nextEntry == null) {
                nextKey = null;
                nextValue = null;
            }
        }
    }

    private class ValueIterator implements Iterator<V> {

        private final EntryIterator entryIterator = new EntryIterator();

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @Override
        public V next() {
            V result = entryIterator.nextValue;
            entryIterator.next();
            return result;
        }

        @Override
        public void remove() {
            entryIterator.remove();
        }
    }

    private class KeyIterator implements Iterator<K> {

        private final EntryIterator entryIterator = new EntryIterator();

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @Override
        public K next() {
            K result = entryIterator.nextKey;
            entryIterator.next();
            return result;
        }

        @Override
        public void remove() {
            entryIterator.remove();
        }
    }
}
