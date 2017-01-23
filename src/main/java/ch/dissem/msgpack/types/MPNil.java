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
