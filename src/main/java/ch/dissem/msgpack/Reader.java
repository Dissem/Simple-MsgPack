package ch.dissem.msgpack;

import ch.dissem.msgpack.types.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Reads MPType object from an {@link InputStream}.
 */
public class Reader {
    private List<MPType.Unpacker<?>> unpackers = new LinkedList<MPType.Unpacker<?>>();

    public Reader() {
        unpackers.add(new MPNil.Unpacker());
        unpackers.add(new MPBoolean.Unpacker());
        unpackers.add(new MPInteger.Unpacker());
        unpackers.add(new MPFloat.Unpacker());
        unpackers.add(new MPDouble.Unpacker());
        unpackers.add(new MPString.Unpacker());
        unpackers.add(new MPBinary.Unpacker());
        unpackers.add(new MPMap.Unpacker(this));
        unpackers.add(new MPArray.Unpacker(this));
    }

    /**
     * Register your own extensions
     */
    public void register(MPType.Unpacker<?> unpacker) {
        unpackers.add(unpacker);
    }

    public MPType read(InputStream in) throws IOException {
        int firstByte = in.read();
        for (MPType.Unpacker<?> unpacker : unpackers) {
            if (unpacker.is(firstByte)) {
                return unpacker.unpack(firstByte, in);
            }
        }
        throw new IOException(String.format("Unsupported input, no reader for 0x%02x", firstByte));
    }
}
