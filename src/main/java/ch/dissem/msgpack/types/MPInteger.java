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
import java.util.Objects;

import static ch.dissem.msgpack.types.Utils.bytes;

/**
 * Representation of a msgpack encoded integer. The encoding is automatically selected according to the value's size.
 * Uses long due to the fact that the msgpack integer implementation may contain up to 64 bit numbers, corresponding
 * to Java long values. Also note that uint64 values may be too large for signed long (thanks Java for not supporting
 * unsigned values) and end in a negative value.
 */
public class MPInteger implements MPType<Long> {
    private long value;

    public MPInteger(long value) {
        this.value = value;
    }

    @Override
    public Long getValue() {
        return value;
    }

    public void pack(OutputStream out) throws IOException {
        if ((value > ((byte) 0b11100000) && value < 0x80)) {
            out.write((int) value);
        } else if (value > 0) {
            if (value <= 0xFF) {
                out.write(0xCC);
                out.write((int) value);
            } else if (value <= 0xFFFF) {
                out.write(0xCD);
                out.write(ByteBuffer.allocate(2).putShort((short) value).array());
            } else if (value < 0xFFFFFFFFL) {
                out.write(0xCE);
                out.write(ByteBuffer.allocate(4).putInt((int) value).array());
            } else {
                out.write(0xCF);
                out.write(ByteBuffer.allocate(8).putLong(value).array());
            }
        } else {
            if (value >= Byte.MIN_VALUE) {
                out.write(0xD0);
                out.write(ByteBuffer.allocate(1).put((byte) value).array());
            } else if (value >= Short.MIN_VALUE) {
                out.write(0xD1);
                out.write(ByteBuffer.allocate(2).putShort((short) value).array());
            } else if (value >= Integer.MIN_VALUE) {
                out.write(0xD2);
                out.write(ByteBuffer.allocate(4).putInt((int) value).array());
            } else {
                out.write(0xD3);
                out.write(ByteBuffer.allocate(8).putLong(value).array());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPInteger mpInteger = (MPInteger) o;
        return value == mpInteger.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public String toJson() {
        return String.valueOf(value);
    }

    public static class Unpacker implements MPType.Unpacker<MPInteger> {
        public boolean is(int firstByte) {
            switch (firstByte) {
                case 0xCC:
                case 0xCD:
                case 0xCE:
                case 0xCF:
                case 0xD0:
                case 0xD1:
                case 0xD2:
                case 0xD3:
                    return true;
                default:
                    return (firstByte & 0b10000000) == 0 || (firstByte & 0b11100000) == 0b11100000;
            }
        }

        public MPInteger unpack(int firstByte, InputStream in) throws IOException {
            if ((firstByte & 0b10000000) == 0 || (firstByte & 0b11100000) == 0b11100000) {
                return new MPInteger((byte) firstByte);
            } else {
                switch (firstByte) {
                    case 0xCC:
                        return new MPInteger(in.read());
                    case 0xCD:
                        return new MPInteger(in.read() << 8 | in.read());
                    case 0xCE:
                        return new MPInteger(in.read() << 24 | in.read() << 16 | in.read() << 8 | in.read());
                    case 0xCF: {
                        long value = 0;
                        for (int i = 0; i < 8; i++) {
                            value = value << 8 | in.read();
                        }
                        return new MPInteger(value);
                    }
                    case 0xD0:
                        return new MPInteger(bytes(in, 1).get());
                    case 0xD1:
                        return new MPInteger(bytes(in, 2).getShort());
                    case 0xD2:
                        return new MPInteger(bytes(in, 4).getInt());
                    case 0xD3:
                        return new MPInteger(bytes(in, 8).getLong());
                    default:
                        throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
                }
            }
        }
    }
}
