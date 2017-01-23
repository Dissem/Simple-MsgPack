package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static ch.dissem.msgpack.types.Utils.bytes;

/**
 * Representation of a msgpack encoded float32 number.
 */
public class MPFloat implements MPType<Float> {
    private float value;

    public MPFloat(float value) {
        this.value = value;
    }

    @Override
    public Float getValue() {
        return value;
    }

    public void pack(OutputStream out) throws IOException {
        out.write(0xCA);
        out.write(ByteBuffer.allocate(4).putFloat(value).array());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPFloat mpFloat = (MPFloat) o;
        return Float.compare(mpFloat.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public String toJson() {
        return String.valueOf(value);
    }

    public static class Unpacker implements MPType.Unpacker<MPFloat> {
        public boolean is(int firstByte) {
            return firstByte == 0xCA;
        }

        public MPFloat unpack(int firstByte, InputStream in) throws IOException {
            if (firstByte == 0xCA) {
                return new MPFloat(bytes(in, 4).getFloat());
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
        }
    }
}
