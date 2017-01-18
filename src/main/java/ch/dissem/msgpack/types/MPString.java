package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static ch.dissem.msgpack.types.Utils.bytes;

public class MPString implements MPType<String> {
    private String value;

    public MPString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void pack(OutputStream out) throws IOException {
        int size = value.length();
        if (size < 16) {
            out.write(0b10100000 + size);
        } else if (size < 256) {
            out.write(0xD9);
            out.write((byte) size);
        } else if (size < 65536) {
            out.write(0xDA);
            out.write(ByteBuffer.allocate(2).putShort((short) size).array());
        } else {
            out.write(0xDB);
            out.write(ByteBuffer.allocate(4).putInt(size).array());
        }
        out.write(value.getBytes("UTF-8"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPString mpString = (MPString) o;
        return Objects.equals(value, mpString.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return '"' + value + '"'; // FIXME: escape value
    }

    public static class Unpacker implements MPType.Unpacker<MPString> {
        public boolean is(int firstByte) {
            return firstByte == 0xD9 || firstByte == 0xDA || firstByte == 0xDB || (firstByte & 0b11100000) == 0b10100000;
        }

        public MPString unpack(int firstByte, InputStream in) throws IOException {
            int size;
            if ((firstByte & 0b11100000) == 0b10100000) {
                size = firstByte & 0b00011111;
            } else if (firstByte == 0xD9) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == 0xDA) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == 0xDB) {
                size = in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read();
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
            return new MPString(new String(bytes(in, size).array(), "UTF-8"));
        }
    }
}
