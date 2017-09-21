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
 * Representation of a msgpack encoded integer. The encoding is automatically selected according to the value's size.
 * Uses long due to the fact that the msgpack integer implementation may contain up to 64 bit numbers, corresponding
 * to Java long values. Also note that uint64 values may be too large for signed long (thanks Java for not supporting
 * unsigned values) and end in a negative value.
 */
data class MPInteger(override val value: Long) : MPType<Long> {

    constructor(value: Byte) : this(value.toLong())
    constructor(value: Short) : this(value.toLong())
    constructor(value: Int) : this(value.toLong())

    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        when (value) {
            in -32..127 -> out.write(value.toInt())
            in 0..0xFF -> {
                out.write(0xCC)
                out.write(value.toInt())
            }
            in 0..0xFFFF -> {
                out.write(0xCD)
                out.write(ByteBuffer.allocate(2).putShort(value.toShort()).array())
            }
            in 0..0xFFFFFFFFL -> {
                out.write(0xCE)
                out.write(ByteBuffer.allocate(4).putInt(value.toInt()).array())
            }
            in 0..Long.MAX_VALUE -> {
                out.write(0xCF)
                out.write(ByteBuffer.allocate(8).putLong(value).array())
            }
            in Byte.MIN_VALUE..0 -> {
                out.write(0xD0)
                out.write(ByteBuffer.allocate(1).put(value.toByte()).array())
            }
            in Short.MIN_VALUE..0 -> {
                out.write(0xD1)
                out.write(ByteBuffer.allocate(2).putShort(value.toShort()).array())
            }
            in Int.MIN_VALUE..0 -> {
                out.write(0xD2)
                out.write(ByteBuffer.allocate(4).putInt(value.toInt()).array())
            }
            in Long.MIN_VALUE..0 -> {
                out.write(0xD3)
                out.write(ByteBuffer.allocate(8).putLong(value).array())
            }
        }
    }

    override fun toString() = value.toString()

    override fun toJson() = value.toString()

    class Unpacker : MPType.Unpacker<MPInteger> {
        override fun doesUnpack(firstByte: Int): Boolean {
            return when (firstByte) {
                0xCC, 0xCD, 0xCE, 0xCF, 0xD0, 0xD1, 0xD2, 0xD3 -> true
                else -> firstByte and 128 == 0 || firstByte and 224 == 224
            }
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream): MPInteger {
            if (firstByte and 128 == 0 || firstByte and 224 == 224) {
                // The cast needs to happen for the MPInteger to have the correct sign
                return MPInteger(firstByte.toByte())
            } else {
                return when (firstByte) {
                    0xCC -> MPInteger(readLong(input, 1))
                    0xCD -> MPInteger(readLong(input, 2))
                    0xCE -> MPInteger(readLong(input, 4))
                    0xCF -> MPInteger(readLong(input, 8))
                    0xD0 -> MPInteger(bytes(input, 1).get())
                    0xD1 -> MPInteger(bytes(input, 2).short)
                    0xD2 -> MPInteger(bytes(input, 4).int)
                    0xD3 -> MPInteger(bytes(input, 8).long)
                    else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
                }
            }
        }

        private fun readLong(input: InputStream, length: Int): Long {
            var value: Long = 0
            for (i in 0 until length) {
                value = value shl 8 or input.read().toLong()
            }
            return value
        }
    }
}
