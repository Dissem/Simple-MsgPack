package ch.dissem.msgpack.types;

import ch.dissem.msgpack.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MPMap<K extends MPType, V extends MPType> implements MPType<Map<K, V>> {
    private Map<K, V> map;

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
        StringBuilder result = new StringBuilder();
        result.append('{');
        Iterator<Map.Entry<K, V>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<K, V> item = iterator.next();
            result.append(item.getKey().toString());
            result.append(": ");
            result.append(item.getValue().toString());
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        result.append('}');
        return result.toString();
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
