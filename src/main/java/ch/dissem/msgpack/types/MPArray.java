package ch.dissem.msgpack.types;

import ch.dissem.msgpack.Reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Representation of a msgpack encoded array. Uses a list to represent data internally, and implements the {@link List}
 * interface for your convenience.
 *
 * @param <T>
 */
public class MPArray<T extends MPType> implements MPType<List<T>>, List<T> {
    private List<T> array;

    public MPArray() {
        this.array = new LinkedList<>();
    }

    public MPArray(List<T> array) {
        this.array = array;
    }

    @SafeVarargs
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
    public int size() {
        return array.size();
    }

    @Override
    public boolean isEmpty() {
        return array.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return array.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return array.iterator();
    }

    @Override
    public Object[] toArray() {
        return array.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        return array.toArray(t1s);
    }

    @Override
    public boolean add(T t) {
        return array.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return array.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return array.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        return array.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        return array.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return array.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return array.retainAll(collection);
    }

    @Override
    public void clear() {
        array.clear();
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
    public T get(int i) {
        return array.get(i);
    }

    @Override
    public T set(int i, T t) {
        return array.set(i, t);
    }

    @Override
    public void add(int i, T t) {
        array.add(i, t);
    }

    @Override
    public T remove(int i) {
        return array.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return array.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return array.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return array.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return array.listIterator(i);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return array.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public String toJson() {
        return toJson("");
    }

    String toJson(String indent) {
        StringBuilder result = new StringBuilder();
        result.append("[\n");
        Iterator<T> iterator = array.iterator();
        String indent2 = indent + "  ";
        while (iterator.hasNext()) {
            T item = iterator.next();
            result.append(indent2);
            result.append(Utils.toJson(item, indent2));
            if (iterator.hasNext()) {
                result.append(',');
            }
            result.append('\n');
        }
        result.append("]");
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
