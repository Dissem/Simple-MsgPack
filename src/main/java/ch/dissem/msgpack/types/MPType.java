package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Representation of some msgpack encoded data.
 */
public interface MPType<T> {
    interface Unpacker<M extends MPType> {
        boolean is(int firstByte);

        M unpack(int firstByte, InputStream in) throws IOException;
    }

    T getValue();

    void pack(OutputStream out) throws IOException;
}
