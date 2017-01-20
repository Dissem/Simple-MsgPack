package ch.dissem.msgpack.types;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class Utils {
    /**
     * Returns a {@link ByteBuffer} containing the next <code>count</code> bytes from the {@link InputStream}.
     */
    static ByteBuffer bytes(InputStream in, int count) throws IOException {
        byte[] result = new byte[count];
        int off = 0;
        while (off < count) {
            int read = in.read(result, off, count - off);
            if (read < 0) {
                throw new IOException("Unexpected end of stream, wanted to read " + count + " bytes but only got " + off);
            }
            off += read;
        }
        return ByteBuffer.wrap(result);
    }
}
