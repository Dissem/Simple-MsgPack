package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Representation of msgpack encoded nil / null.
 */
public class MPNil implements MPType<Void> {
    private final static int NIL = 0xC0;

    public Void getValue() {
        return null;
    }

    public void pack(OutputStream out) throws IOException {
        out.write(NIL);
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MPNil;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public String toJson() {
        return "null";
    }

    public static class Unpacker implements MPType.Unpacker<MPNil> {

        public boolean is(int firstByte) {
            return firstByte == NIL;
        }

        public MPNil unpack(int firstByte, InputStream in) {
            if (firstByte != NIL) {
                throw new IllegalArgumentException(String.format("0xC0 expected but was 0x%02x", firstByte));
            }
            return new MPNil();
        }
    }
}
