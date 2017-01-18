package ch.dissem.msgpack.types;

import ch.dissem.msgpack.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class MPArray<T extends MPType> implements MPType<List<T>> {
    private List<T> array;

    public MPArray(List<T> array) {
        this.array = array;
    }

    public MPArray(T... objects) {
        this.array = Arrays.asList(objects);
    }

    public List<T> getValue() {
        return array;
    }

    public void pack(OutputStream out) throws IOException {
        int size = array.size();
        if (size < 16) {
            out.write(0b10010000 + size);
        } else if (size < 65536) {
            out.write(0xDC);
            out.write(ByteBuffer.allocate(2).putShort((short) size).array());
        } else {
            out.write(0xDD);
            out.write(ByteBuffer.allocate(4).putInt(size).array());
        }
        for (MPType o : array) {
            o.pack(out);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPArray<?> mpArray = (MPArray<?>) o;
        return Objects.equals(array, mpArray.array);
    }

    @Override
    public int hashCode() {
        return Objects.hash(array);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('[');
        Iterator<T> iterator = array.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            result.append(item.toString());
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        result.append(']');
        return result.toString();
    }

    public static class Unpacker implements MPType.Unpacker<MPArray> {
        private final Reader reader;

        public Unpacker(Reader reader) {
            this.reader = reader;
        }

        public boolean is(int firstByte) {
            return firstByte == 0xDC || firstByte == 0xDD || (firstByte & 0b11110000) == 0b10010000;
        }

        public MPArray<MPType<?>> unpack(int firstByte, InputStream in) throws IOException {
            int size;
            if ((firstByte & 0b11110000) == 0b10010000) {
                size = firstByte & 0b00001111;
            } else if (firstByte == 0xDC) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == 0xDD) {
                size = in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read();
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
            List<MPType<?>> list = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                MPType value = reader.read(in);
                list.add(value);
            }
            return new MPArray<>(list);
        }
    }
}
