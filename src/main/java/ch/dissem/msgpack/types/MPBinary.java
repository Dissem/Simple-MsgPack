package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static ch.dissem.msgpack.types.Utils.bytes;

/**
 * Representation of msgpack encoded binary data a.k.a. byte array.
 */
public class MPBinary implements MPType<byte[]> {
    private byte[] value;

    public MPBinary(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public void pack(OutputStream out) throws IOException {
        int size = value.length;
        if (size < 256) {
            out.write(0xC4);
            out.write((byte) size);
        } else if (size < 65536) {
            out.write(0xC5);
            out.write(ByteBuffer.allocate(2).putShort((short) size).array());
        } else {
            out.write(0xC6);
            out.write(ByteBuffer.allocate(4).putInt(size).array());
        }
        out.write(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPBinary mpBinary = (MPBinary) o;
        return Arrays.equals(value, mpBinary.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return null; // TODO base64
    }

    public static class Unpacker implements MPType.Unpacker<MPBinary> {
        public boolean is(int firstByte) {
            return firstByte == 0xC4 || firstByte == 0xC5 || firstByte == 0xC6;
        }

        public MPBinary unpack(int firstByte, InputStream in) throws IOException {
            int size;
            if (firstByte == 0xC4) {
                size = in.read();
            } else if (firstByte == 0xC5) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == 0xC6) {
                size = in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read();
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
            return new MPBinary(bytes(in, size).array());
        }
    }
}
