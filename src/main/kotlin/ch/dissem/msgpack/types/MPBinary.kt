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
import java.util.*

/**
 * Representation of msgpack encoded binary data a.k.a. byte array.
 */
data class MPBinary(override val value: ByteArray) : MPType<ByteArray> {

    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        val size = value.size
        when {
            size < 256 -> {
                out.write(0xC4)
                out.write(size.toByte().toInt())
            }
            size < 65536 -> {
                out.write(0xC5)
                out.write(ByteBuffer.allocate(2).putShort(size.toShort()).array())
            }
            else -> {
                out.write(0xC6)
                out.write(ByteBuffer.allocate(4).putInt(size).array())
            }
        }
        out.write(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MPBinary) return false
        return Arrays.equals(value, other.value)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(value)
    }

    override fun toString(): String {
        return toJson()
    }

    override fun toJson(): String {
        return Utils.base64(value)
    }

    class Unpacker : MPType.Unpacker<MPBinary> {
        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == 0xC4 || firstByte == 0xC5 || firstByte == 0xC6
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream): MPBinary {
            val size: Int = when (firstByte) {
                0xC4 -> input.read()
                0xC5 -> input.read() shl 8 or input.read()
                0xC6 -> input.read() shl 24 or (input.read() shl 16) or (input.read() shl 8) or input.read()
                else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
            }
            return MPBinary(bytes(input, size).array())
        }
    }
}
