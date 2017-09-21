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

package ch.dissem.msgpack.types

import ch.dissem.msgpack.types.Utils.bytes
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Representation of a msgpack encoded float32 or float64 number.
 */
data class MPFloat(override val value: Double, val precision: Precision) : MPType<Double> {

    enum class Precision {
        FLOAT32, FLOAT64
    }

    constructor(value: Float) : this(value.toDouble(), Precision.FLOAT32)
    constructor(value: Double) : this(value, Precision.FLOAT64)

    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        when (precision) {
            MPFloat.Precision.FLOAT32 -> {
                out.write(0xCA)
                out.write(ByteBuffer.allocate(4).putFloat(value.toFloat()).array())
            }
            MPFloat.Precision.FLOAT64 -> {
                out.write(0xCB)
                out.write(ByteBuffer.allocate(8).putDouble(value).array())
            }
        }
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun toJson(): String {
        return value.toString()
    }

    class Unpacker : MPType.Unpacker<MPFloat> {
        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == 0xCA || firstByte == 0xCB
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream) = when (firstByte) {
            0xCA -> MPFloat(bytes(input, 4).float)
            0xCB -> MPFloat(bytes(input, 8).double)
            else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
        }
    }
}
