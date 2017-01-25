/*
 * Copyright 2017 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private List<MPType.Unpacker<?>> unpackers = new LinkedList<>();

    private static final Reader instance = new Reader();

    private Reader() {
        unpackers.add(new MPNil.Unpacker());
        unpackers.add(new MPBoolean.Unpacker());
        unpackers.add(new MPInteger.Unpacker());
        unpackers.add(new MPFloat.Unpacker());
        unpackers.add(new MPString.Unpacker());
        unpackers.add(new MPBinary.Unpacker());
        unpackers.add(new MPMap.Unpacker(this));
        unpackers.add(new MPArray.Unpacker(this));
    }

    public static Reader getInstance() {
        return instance;
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
