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
import java.nio.ByteBuffer;

public class Utils {
    private static final char[] BASE64_CODES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();
    private static final MPNil NIL = new MPNil();

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

    /**
     * Helper method to decide which types support extra indention (for pretty printing JSON)
     */
    static String toJson(MPType<?> type, String indent) {
        if (type instanceof MPMap) {
            return ((MPMap) type).toJson(indent);
        }
        if (type instanceof MPArray) {
            return ((MPArray) type).toJson(indent);
        }
        return type.toJson();
    }

    /**
     * Slightly improved code from https://en.wikipedia.org/wiki/Base64
     */
    static String base64(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 4) / 3 + 3);
        int b;
        for (int i = 0; i < data.length; i += 3) {
            b = (data[i] & 0xFC) >> 2;
            result.append(BASE64_CODES[b]);
            b = (data[i] & 0x03) << 4;
            if (i + 1 < data.length) {
                b |= (data[i + 1] & 0xF0) >> 4;
                result.append(BASE64_CODES[b]);
                b = (data[i + 1] & 0x0F) << 2;
                if (i + 2 < data.length) {
                    b |= (data[i + 2] & 0xC0) >> 6;
                    result.append(BASE64_CODES[b]);
                    b = data[i + 2] & 0x3F;
                    result.append(BASE64_CODES[b]);
                } else {
                    result.append(BASE64_CODES[b]);
                    result.append('=');
                }
            } else {
                result.append(BASE64_CODES[b]);
                result.append("==");
            }
        }

        return result.toString();
    }

    public static MPString mp(String value) {
        return new MPString(value);
    }

    public static MPBoolean mp(boolean value) {
        return new MPBoolean(value);
    }

    public static MPFloat mp(double value) {
        return new MPFloat(value);
    }

    public static MPFloat mp(float value) {
        return new MPFloat(value);
    }

    public static MPInteger mp(int value) {
        return new MPInteger(value);
    }

    public static MPBinary mp(byte... data) {
        return new MPBinary(data);
    }

    public static MPNil nil() {
        return NIL;
    }
}
