/*
 * Causam - A maximally decoupled event system for Java
 * Copyright (C) 2016 Foundry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pw.stamina.causam.internal.util;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentWeakHashMap<K, V> extends AbstractMap<K, V>
        implements java.util.concurrent.ConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;


    static final int DEFAULT_INITIAL_CAPACITY = 16;

    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    static final int MAXIMUM_CAPACITY = 1 << 30;

    static final int MAX_SEGMENTS = 1 << 16; 

    static final int RETRIES_BEFORE_LOCK = 2;


    final int segmentMask;

    final int segmentShift;

    final Segment<K,V>[] segments;

    transient Set<K> keySet;
    transient Set<Entry<K,V>> entrySet;
    transient Collection<V> values;


    private static int hash(int h) {
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }

    final Segment<K,V> segmentFor(int hash) {
        return segments[(hash >>> segmentShift) & segmentMask];
    }


    static final class WeakKeyReference<K> extends WeakReference<K> {
        final int hash;
        WeakKeyReference(K key, int hash, ReferenceQueue<K> refQueue) {
            super(key, refQueue);
            this.hash = hash;
        }
    }

    static final class HashEntry<K,V> {
        final WeakReference<K> keyRef;
        final int hash;
        volatile V value;
        final HashEntry<K,V> next;

        HashEntry(K key, int hash, HashEntry<K,V> next, V value, ReferenceQueue<K> refQueue) {
            this.keyRef = new WeakKeyReference<K>(key, hash, refQueue);
            this.hash = hash;
            this.next = next;
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        static <K,V> HashEntry<K,V>[] newArray(int i) {
            return new HashEntry[i];
        }
    }

    static final class Segment<K,V> extends ReentrantLock implements Serializable {

        private static final long serialVersionUID = 2249069246763182397L;

        transient volatile int count;

        transient int modCount;

        transient int threshold;

        transient volatile HashEntry<K,V>[] table;

        final float loadFactor;

        transient volatile ReferenceQueue<K> refQueue;

        Segment(int initialCapacity, float lf) {
            loadFactor = lf;
            setTable(HashEntry.<K,V>newArray(initialCapacity));
        }

        @SuppressWarnings("unchecked")
        static <K,V> Segment<K,V>[] newArray(int i) {
            return new Segment[i];
        }

        void setTable(HashEntry<K,V>[] newTable) {
            threshold = (int)(newTable.length * loadFactor);
            table = newTable;
            refQueue = new ReferenceQueue<K>();
        }

        HashEntry<K,V> getFirst(int hash) {
            HashEntry<K,V>[] tab = table;
            return tab[hash & (tab.length - 1)];
        }

        V readValueUnderLock(HashEntry<K,V> e) {
            lock();
            try {
                removeStale();
                return e.value;
            } finally {
                unlock();
            }
        }

        V get(Object key, int hash) {
            if (count != 0) { 
                HashEntry<K,V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.keyRef.get())) {
                        V v = e.value;
                        if (v != null)
                            return v;
                        return readValueUnderLock(e); 
                    }
                    e = e.next;
                }
            }
            return null;
        }

        boolean containsKey(Object key, int hash) {
            if (count != 0) { 
                HashEntry<K,V> e = getFirst(hash);
                while (e != null) {
                    if (e.hash == hash && key.equals(e.keyRef.get()))
                        return true;
                    e = e.next;
                }
            }
            return false;
        }

        boolean containsValue(Object value) {
            if (count != 0) { 
                HashEntry<K,V>[] tab = table;
                int len = tab.length;
                for (HashEntry<K, V> aTab : tab) {
                    for (HashEntry<K, V> e = aTab; e != null; e = e.next) {
                        V v = e.value;
                        if (v == null)
                            v = readValueUnderLock(e);
                        if (value.equals(v))
                            return true;
                    }
                }
            }
            return false;
        }

        boolean replace(K key, int hash, V oldValue, V newValue) {
            lock();
            try {
                removeStale();
                HashEntry<K,V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.keyRef.get())))
                    e = e.next;

                boolean replaced = false;
                if (e != null && oldValue.equals(e.value)) {
                    replaced = true;
                    e.value = newValue;
                }
                return replaced;
            } finally {
                unlock();
            }
        }

        V replace(K key, int hash, V newValue) {
            lock();
            try {
                removeStale();
                HashEntry<K,V> e = getFirst(hash);
                while (e != null && (e.hash != hash || !key.equals(e.keyRef.get())))
                    e = e.next;

                V oldValue = null;
                if (e != null) {
                    oldValue = e.value;
                    e.value = newValue;
                }
                return oldValue;
            } finally {
                unlock();
            }
        }


        V put(K key, int hash, V value, boolean onlyIfAbsent) {
            lock();
            try {
                removeStale();
                int c = count;
                if (c++ > threshold) {
                    int reduced = rehash();
                    if (reduced > 0)  
                        count = (c -= reduced) - 1; 
                }

                HashEntry<K,V>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<K,V> first = tab[index];
                HashEntry<K,V> e = first;
                while (e != null && (e.hash != hash || !key.equals(e.keyRef.get())))
                    e = e.next;

                V oldValue;
                if (e != null) {
                    oldValue = e.value;
                    if (!onlyIfAbsent)
                        e.value = value;
                }
                else {
                    oldValue = null;
                    ++modCount;
                    tab[index] = new HashEntry<K,V>(key, hash, first, value, refQueue);
                    count = c; 
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        int rehash() {
            HashEntry<K,V>[] oldTable = table;
            int oldCapacity = oldTable.length;
            if (oldCapacity >= MAXIMUM_CAPACITY)
                return 0;


            HashEntry<K,V>[] newTable = HashEntry.newArray(oldCapacity<<1);
            threshold = (int)(newTable.length * loadFactor);
            int sizeMask = newTable.length - 1;
            int reduce = 0;
            for (HashEntry<K, V> e : oldTable) {

                if (e != null) {
                    HashEntry<K, V> next = e.next;
                    int idx = e.hash & sizeMask;


                    if (next == null)
                        newTable[idx] = e;

                    else {

                        HashEntry<K, V> lastRun = e;
                        int lastIdx = idx;
                        for (HashEntry<K, V> last = next;
                             last != null;
                             last = last.next) {
                            int k = last.hash & sizeMask;
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        newTable[lastIdx] = lastRun;

                        for (HashEntry<K, V> p = e; p != lastRun; p = p.next) {

                            K key = p.keyRef.get();
                            if (key == null) {
                                reduce++;
                                continue;
                            }
                            int k = p.hash & sizeMask;
                            HashEntry<K, V> n = newTable[k];
                            newTable[k] = new HashEntry<K, V>(key, p.hash, n, p.value, refQueue);
                        }
                    }
                }
            }
            table = newTable;
            return reduce;
        }

        V remove(Object key, int hash, Object value, boolean weakRemove) {
            lock();
            try {
                if (!weakRemove)
                    removeStale();
                int c = count - 1;
                HashEntry<K,V>[] tab = table;
                int index = hash & (tab.length - 1);
                HashEntry<K,V> first = tab[index];
                HashEntry<K,V> e = first;
                
                while (e != null && (!weakRemove || key != e.keyRef)
                        && (e.hash != hash || !key.equals(e.keyRef.get())))
                    e = e.next;

                V oldValue = null;
                if (e != null) {
                    V v = e.value;
                    if (value == null || value.equals(v)) {
                        oldValue = v;
                        
                        
                        
                        ++modCount;
                        HashEntry<K,V> newFirst = e.next;
                        for (HashEntry<K,V> p = first; p != e; p = p.next) {
                            K pKey = p.keyRef.get();
                            if (pKey == null) { 
                                c--;
                                continue;
                            }

                            newFirst = new HashEntry<K,V>(pKey, p.hash,
                                    newFirst, p.value, refQueue);
                        }
                        tab[index] = newFirst;
                        count = c; 
                    }
                }
                return oldValue;
            } finally {
                unlock();
            }
        }

        @SuppressWarnings("unchecked")
        void removeStale() {
            WeakKeyReference<K> ref;
            while ((ref = (WeakKeyReference<K>) refQueue.poll()) != null) {
                remove(ref, ref.hash, null, true);
            }
        }

        void clear() {
            if (count != 0) {
                lock();
                try {
                    HashEntry<K,V>[] tab = table;
                    for (int i = 0; i < tab.length ; i++)
                        tab[i] = null;
                    ++modCount;
                    
                    refQueue = new ReferenceQueue<K>();
                    count = 0; 
                } finally {
                    unlock();
                }
            }
        }
    }


    public ConcurrentWeakHashMap(int initialCapacity,
                                 float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();

        if (concurrencyLevel > MAX_SEGMENTS)
            concurrencyLevel = MAX_SEGMENTS;

        
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        segmentShift = 32 - sshift;
        segmentMask = ssize - 1;
        this.segments = Segment.newArray(ssize);

        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity)
            ++c;
        int cap = 1;
        while (cap < c)
            cap <<= 1;

        for (int i = 0; i < this.segments.length; ++i)
            this.segments[i] = new Segment<K,V>(cap, loadFactor);
    }

    public ConcurrentWeakHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentWeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentWeakHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentWeakHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
        putAll(m);
    }

    public boolean isEmpty() {
        final Segment<K,V>[] segments = this.segments;

        int[] mc = new int[segments.length];
        int mcsum = 0;
        for (int i = 0; i < segments.length; ++i) {
            if (segments[i].count != 0)
                return false;
            else
                mcsum += mc[i] = segments[i].modCount;
        }
        
        
        
        if (mcsum != 0) {
            for (int i = 0; i < segments.length; ++i) {
                if (segments[i].count != 0 ||
                        mc[i] != segments[i].modCount)
                    return false;
            }
        }
        return true;
    }

    public int size() {
        final Segment<K,V>[] segments = this.segments;
        long sum = 0;
        long check = 0;
        int[] mc = new int[segments.length];
        
        
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            check = 0;
            sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                sum += segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
            }
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    check += segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        check = -1; 
                        break;
                    }
                }
            }
            if (check == sum)
                break;
        }
        if (check != sum) { 
            sum = 0;
            for (Segment<K, V> segment : segments) segment.lock();
            for (Segment<K, V> segment : segments) sum += segment.count;
            for (Segment<K, V> segment : segments) segment.unlock();
        }
        if (sum > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else
            return (int)sum;
    }

    public V get(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).get(key, hash);
    }

    public boolean containsKey(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).containsKey(key, hash);
    }

    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();

        

        final Segment<K,V>[] segments = this.segments;
        int[] mc = new int[segments.length];

        
        for (int k = 0; k < RETRIES_BEFORE_LOCK; ++k) {
            int sum = 0;
            int mcsum = 0;
            for (int i = 0; i < segments.length; ++i) {
                int c = segments[i].count;
                mcsum += mc[i] = segments[i].modCount;
                if (segments[i].containsValue(value))
                    return true;
            }
            boolean cleanSweep = true;
            if (mcsum != 0) {
                for (int i = 0; i < segments.length; ++i) {
                    int c = segments[i].count;
                    if (mc[i] != segments[i].modCount) {
                        cleanSweep = false;
                        break;
                    }
                }
            }
            if (cleanSweep)
                return false;
        }

        for (Segment<K, V> segment : segments) segment.lock();
        boolean found = false;
        try {
            for (Segment<K, V> segment : segments) {
                if (segment.containsValue(value)) {
                    found = true;
                    break;
                }
            }
        } finally {
            for (Segment<K, V> segment : segments) segment.unlock();
        }
        return found;
    }

    public boolean contains(Object value) {
        return containsValue(value);
    }

    public V put(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, value, false);
    }

    public V putIfAbsent(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key.hashCode());
        return segmentFor(hash).put(key, hash, value, true);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    public V remove(Object key) {
        int hash = hash(key.hashCode());
        return segmentFor(hash).remove(key, hash, null, false);
    }

    public boolean remove(Object key, Object value) {
        int hash = hash(key.hashCode());
        return value != null && segmentFor(hash).remove(key, hash, value, false) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (oldValue == null || newValue == null)
            throw new NullPointerException();
        int hash = hash(key.hashCode());
        return segmentFor(hash).replace(key, hash, oldValue, newValue);
    }

    public V replace(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        int hash = hash(key.hashCode());
        return segmentFor(hash).replace(key, hash, value);
    }

    public void clear() {
        for (Segment<K, V> segment : segments) segment.clear();
    }

    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    public Set<Entry<K,V>> entrySet() {
        Set<Entry<K,V>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    public Enumeration<K> keys() {
        return new KeyIterator();
    }

    public Enumeration<V> elements() {
        return new ValueIterator();
    }


    abstract class HashIterator {
        int nextSegmentIndex;
        int nextTableIndex;
        HashEntry<K,V>[] currentTable;
        HashEntry<K, V> nextEntry;
        HashEntry<K, V> lastReturned;
        K currentKey; 

        HashIterator() {
            nextSegmentIndex = segments.length - 1;
            nextTableIndex = -1;
            advance();
        }

        public boolean hasMoreElements() { return hasNext(); }

        final void advance() {
            if (nextEntry != null && (nextEntry = nextEntry.next) != null)
                return;

            while (nextTableIndex >= 0) {
                if ( (nextEntry = currentTable[nextTableIndex--]) != null)
                    return;
            }

            while (nextSegmentIndex >= 0) {
                Segment<K,V> seg = segments[nextSegmentIndex--];
                if (seg.count != 0) {
                    currentTable = seg.table;
                    for (int j = currentTable.length - 1; j >= 0; --j) {
                        if ( (nextEntry = currentTable[j]) != null) {
                            nextTableIndex = j - 1;
                            return;
                        }
                    }
                }
            }
        }

        public boolean hasNext() {
            while (nextEntry != null) {
                if (nextEntry.keyRef.get() != null)
                    return true;
                advance();
            }

            return false;
        }

        HashEntry<K,V> nextEntry() {
            do {
                if (nextEntry == null)
                    throw new NoSuchElementException();

                lastReturned = nextEntry;
                currentKey = lastReturned.keyRef.get();
                advance();
            } while (currentKey == null); 

            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            ConcurrentWeakHashMap.this.remove(currentKey);
            lastReturned = null;
        }
    }

    final class KeyIterator
            extends HashIterator
            implements Iterator<K>, Enumeration<K>
    {
        public K next()        { return super.nextEntry().keyRef.get(); }
        public K nextElement() { return super.nextEntry().keyRef.get(); }
    }

    final class ValueIterator
            extends HashIterator
            implements Iterator<V>, Enumeration<V>
    {
        public V next()        { return super.nextEntry().value; }
        public V nextElement() { return super.nextEntry().value; }
    }

    static class SimpleEntry<K, V> implements Entry<K, V>,
            Serializable {
        private static final long serialVersionUID = -8499721149061103585L;

        private final K key;
        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(Entry<? extends K, ? extends V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Entry))
                return false;
            @SuppressWarnings("unchecked")
            Entry e = (Entry) o;
            return eq(key, e.getKey()) && eq(value, e.getValue());
        }

        public int hashCode() {
            return (key == null ? 0 : key.hashCode())
                    ^ (value == null ? 0 : value.hashCode());
        }

        public String toString() {
            return key + "=" + value;
        }

        private static boolean eq(Object o1, Object o2) {
            return o1 == null ? o2 == null : o1.equals(o2);
        }
    }

    final class WriteThroughEntry extends SimpleEntry<K,V>
    {
        private static final long serialVersionUID = -7900634345345313646L;

        WriteThroughEntry(K k, V v) {
            super(k,v);
        }

        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = super.setValue(value);
            ConcurrentWeakHashMap.this.put(getKey(), value);
            return v;
        }
    }

    final class EntryIterator
            extends HashIterator
            implements Iterator<Entry<K,V>>
    {
        public Entry<K,V> next() {
            HashEntry<K,V> e = super.nextEntry();
            return new WriteThroughEntry(e.keyRef.get(), e.value);
        }
    }

    final class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }
        public int size() {
            return ConcurrentWeakHashMap.this.size();
        }
        public boolean isEmpty() {
            return ConcurrentWeakHashMap.this.isEmpty();
        }
        public boolean contains(Object o) {
            return ConcurrentWeakHashMap.this.containsKey(o);
        }
        public boolean remove(Object o) {
            return ConcurrentWeakHashMap.this.remove(o) != null;
        }
        public void clear() {
            ConcurrentWeakHashMap.this.clear();
        }
    }

    final class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }
        public int size() {
            return ConcurrentWeakHashMap.this.size();
        }
        public boolean isEmpty() {
            return ConcurrentWeakHashMap.this.isEmpty();
        }
        public boolean contains(Object o) {
            return ConcurrentWeakHashMap.this.containsValue(o);
        }
        public void clear() {
            ConcurrentWeakHashMap.this.clear();
        }
    }

    final class EntrySet extends AbstractSet<Entry<K,V>> {
        public Iterator<Entry<K,V>> iterator() {
            return new EntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            V v = ConcurrentWeakHashMap.this.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?,?> e = (Entry<?,?>)o;
            return ConcurrentWeakHashMap.this.remove(e.getKey(), e.getValue());
        }
        public int size() {
            return ConcurrentWeakHashMap.this.size();
        }
        public boolean isEmpty() {
            return ConcurrentWeakHashMap.this.isEmpty();
        }
        public void clear() {
            ConcurrentWeakHashMap.this.clear();
        }
    }


    private void writeObject(java.io.ObjectOutputStream s) throws IOException  {
        s.defaultWriteObject();

        for (Segment<K, V> seg : segments) {
            seg.lock();
            try {
                HashEntry<K, V>[] tab = seg.table;
                for (HashEntry<K, V> aTab : tab) {
                    for (HashEntry<K, V> e = aTab; e != null; e = e.next) {
                        K key = e.keyRef.get();
                        if (key == null)
                            continue;

                        s.writeObject(key);
                        s.writeObject(e.value);
                    }
                }
            } finally {
                seg.unlock();
            }
        }
        s.writeObject(null);
        s.writeObject(null);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException  {
        s.defaultReadObject();


        for (Segment<K, V> segment : segments) {
            segment.setTable(new HashEntry[1]);
        }

        
        for (;;) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            if (key == null)
                break;
            put(key, value);
        }
    }
}

