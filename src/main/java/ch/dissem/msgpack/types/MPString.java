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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

import static ch.dissem.msgpack.types.Utils.bytes;

/**
 * Representation of a msgpack encoded string. The encoding is automatically selected according to the string's length.
 * <p>
 * The default encoding is UTF-8.
 * </p>
 */
public class MPString implements MPType<String>, CharSequence {
    private static final int FIXSTR_PREFIX = 0b10100000;
    private static final int FIXSTR_PREFIX_FILTER = 0b11100000;
    private static final int STR8_PREFIX = 0xD9;
    private static final int STR8_LIMIT = 256;
    private static final int STR16_PREFIX = 0xDA;
    private static final int STR16_LIMIT = 65536;
    private static final int STR32_PREFIX = 0xDB;
    private static final int FIXSTR_FILTER = 0b00011111;

    private static Charset encoding = Charset.forName("UTF-8");

    /**
     * Use this method if for some messed up reason you really need to use something else than UTF-8.
     * Ask yourself: why should I? Is this really necessary?
     * <p>
     * It will set the encoding for all {@link MPString}s, but if you have inconsistent encoding in your
     * format you're lost anyway.
     * </p>
     */
    public static void setEncoding(Charset encoding) {
        MPString.encoding = encoding;
    }

    private final String value;

    public MPString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void pack(OutputStream out) throws IOException {
        int size = value.length();
        if (size < 32) {
            out.write(FIXSTR_PREFIX + size);
        } else if (size < STR8_LIMIT) {
            out.write(STR8_PREFIX);
            out.write(size);
        } else if (size < STR16_LIMIT) {
            out.write(STR16_PREFIX);
            out.write(ByteBuffer.allocate(2).putShort((short) size).array());
        } else {
            out.write(STR32_PREFIX);
            out.write(ByteBuffer.allocate(4).putInt(size).array());
        }
        out.write(value.getBytes(encoding));
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
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int i) {
        return value.charAt(i);
    }

    @Override
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return value.subSequence(beginIndex, endIndex);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String toJson() {
        StringBuilder result = new StringBuilder(value.length() + 4);
        result.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                case '/':
                    result.append('\\').append(c);
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        result.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int j = 0; j - hex.length() < 4; j++) {
                            result.append('0');
                        }
                        result.append(hex);
                    } else {
                        result.append(c);
                    }
            }
        }
        result.append('"');
        return result.toString();
    }

    public static class Unpacker implements MPType.Unpacker<MPString> {
        public boolean is(int firstByte) {
            return firstByte == STR8_PREFIX || firstByte == STR16_PREFIX || firstByte == STR32_PREFIX
                    || (firstByte & FIXSTR_PREFIX_FILTER) == FIXSTR_PREFIX;
        }

        public MPString unpack(int firstByte, InputStream in) throws IOException {
            int size;
            if ((firstByte & FIXSTR_PREFIX_FILTER) == FIXSTR_PREFIX) {
                size = firstByte & FIXSTR_FILTER;
            } else if (firstByte == STR8_PREFIX) {
                size = in.read();
            } else if (firstByte == STR16_PREFIX) {
                size = in.read() << 8 | in.read();
            } else if (firstByte == STR32_PREFIX) {
                size = in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read();
            } else {
                throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
            return new MPString(new String(bytes(in, size).array(), encoding));
        }
    }
}
