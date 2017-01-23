/*
 * Copyright 2017 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.msgpack.types;

import ch.dissem.msgpack.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Representation of a msgpack encoded map. It is recommended to use a {@link LinkedHashMap} to ensure the order
 * of entries. For convenience, it also implements the {@link Map} interface.
 *
 * @param <K>
 * @param <V>
 */
public class MPMap<K extends MPType, V extends MPType> implements MPType<Map<K, V>>, Map<K, V> {
    private Map<K, V> map;

    public MPMap() {
        this.map = new LinkedHashMap<>();
    }

    public MPMap(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public Map<K, V> getValue() {
        return map;
    }

    public void pack(OutputStream out) throws IOException {
        int size = map.size();
        if (size < 16) {
            out.write(0x80 + size);
        } else if (size < 65536) {
            out.write(0xDE);
            out.write(ByteBuffer.allocate(2).putShort((short) size).array());
        } else {
            out.write(0xDF);
            out.write(ByteBuffer.allocate(4).putInt(size).array());
        }
        for (Map.Entry<K, V> e : map.entrySet()) {
            e.getKey().pack(out);
            e.getValue().pack(out);
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return map.containsValue(o);
    }

    @Override
    public V get(Object o) {
        return map.get(o);
    }

    @Override
    public V put(K k, V v) {
        return map.put(k, v);
    }

    @Override
    public V remove(Object o) {
        return map.remove(o);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        this.map.putAll(map);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPMap<?, ?> mpMap = (MPMap<?, ?>) o;
        return Objects.equals(map, mpMap.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        return toJson();
    }

    String toJson(String indent) {
        StringBuilder result = new StringBuilder();
        result.append("{\n");
        Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
        String indent2 = indent + "  ";
        while (iterator.hasNext()) {
            Map.Entry<K, V> item = iterator.next();
            result.append(indent2);
            result.append(Utils.toJson(item.getKey(), indent2));
            result.append(": ");
            result.append(Utils.toJson(item.getValue(), indent2));
            if (iterator.hasNext()) {
                result.append(',');
            }
            result.append('\n');
        }
        result.append(indent).append("}");
        return result.toString();
    }

    @Override
    public String toJson() {
        return toJson("");
    }

    public static class Unpacker implements MPType.Unpacker<MPMap> {
        private final Reader reader;

        public Unpacker(Reader reader) {
            this.reader = reader;
        }

        public boolean is(int firstByte) {
            return firstByte == 0xDE || firstByte == 0xDF || (firstByte & 0xF0) == 0x80;
        }

        public MPMap<MPType<?>, MPType<?>> unpack(int firstByte, InputStream in) throws IOException {
            int size;
            if ((firstByte & 0xF0) == 0x80) {
                size = firstByte & 0x0F;
            } else if (firstByte == 0xDE) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == 0xDF) {
                size = in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read();
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
            Map<MPType<?>, MPType<?>> map = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                MPType key = reader.read(in);
                MPType value = reader.read(in);
                map.put(key, value);
            }
            return new MPMap<>(map);
        }
    }
}
