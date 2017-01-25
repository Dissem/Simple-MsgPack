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
 * Representation of a msgpack encoded float32 or float64 number.
 */
public class MPFloat implements MPType<Double> {

    public enum Precision {FLOAT32, FLOAT64}

    private final double value;
    private final Precision precision;

    public MPFloat(float value) {
        this.value = value;
        this.precision = Precision.FLOAT32;
    }

    public MPFloat(double value) {
        this.value = value;
        this.precision = Precision.FLOAT64;
    }

    @Override
    public Double getValue() {
        return value;
    }

    public Precision getPrecision() {
        return precision;
    }

    public void pack(OutputStream out) throws IOException {
        switch (precision) {
            case FLOAT32:
                out.write(0xCA);
                out.write(ByteBuffer.allocate(4).putFloat((float) value).array());
                break;
            case FLOAT64:
                out.write(0xCB);
                out.write(ByteBuffer.allocate(8).putDouble(value).array());
                break;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MPFloat mpFloat = (MPFloat) o;
        return Double.compare(mpFloat.value, value) == 0;
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

    public static class Unpacker implements MPType.Unpacker<MPFloat> {
        public boolean is(int firstByte) {
            return firstByte == 0xCA || firstByte == 0xCB;
        }

        public MPFloat unpack(int firstByte, InputStream in) throws IOException {
            switch (firstByte) {
                case 0xCA:
                    return new MPFloat(bytes(in, 4).getFloat());
                case 0xCB:
                    return new MPFloat(bytes(in, 8).getDouble());
                default:
                    throw new IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte));
            }
        }
    }
}
