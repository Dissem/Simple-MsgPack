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
import java.nio.charset.Charset

/**
 * Representation of a msgpack encoded string. The encoding is automatically selected according to the string's length.
 *
 * The default encoding is UTF-8.
 */
data class MPString(override val value: String) : MPType<String>, CharSequence {

    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        val bytes = value.toByteArray(encoding)
        val size = bytes.size
        when {
            size < 32 -> out.write(FIXSTR_PREFIX + size)
            size < STR8_LIMIT -> {
                out.write(STR8_PREFIX)
                out.write(size)
            }
            size < STR16_LIMIT -> {
                out.write(STR16_PREFIX)
                out.write(ByteBuffer.allocate(2).putShort(size.toShort()).array())
            }
            else -> {
                out.write(STR32_PREFIX)
                out.write(ByteBuffer.allocate(4).putInt(size).array())
            }
        }
        out.write(bytes)
    }

    override val length: Int = value.length

    override fun get(index: Int): Char {
        return value[index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return value.subSequence(startIndex, endIndex)
    }

    override fun toString(): String {
        return value
    }

    override fun toJson(): String {
        val result = StringBuilder(value.length + 4)
        result.append('"')
        value.forEach {
            when (it) {
                '\\', '"', '/' -> result.append('\\').append(it)
                '\b' -> result.append("\\b")
                '\t' -> result.append("\\t")
                '\n' -> result.append("\\n")
                '\r' -> result.append("\\r")
                else -> if (it < ' ') {
                    result.append("\\u")
                    val hex = Integer.toHexString(it.toInt())
                    var j = 0
                    while (j + hex.length < 4) {
                        result.append('0')
                        j++
                    }
                    result.append(hex)
                } else {
                    result.append(it)
                }
            }
        }
        result.append('"')
        return result.toString()
    }

    class Unpacker : MPType.Unpacker<MPString> {
        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == STR8_PREFIX || firstByte == STR16_PREFIX || firstByte == STR32_PREFIX
                    || firstByte and FIXSTR_PREFIX_FILTER == FIXSTR_PREFIX
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream): MPString {
            val size: Int = when {
                firstByte and FIXSTR_PREFIX_FILTER == FIXSTR_PREFIX -> firstByte and FIXSTR_FILTER
                firstByte == STR8_PREFIX -> input.read()
                firstByte == STR16_PREFIX -> input.read() shl 8 or input.read()
                firstByte == STR32_PREFIX -> input.read() shl 24 or (input.read() shl 16) or (input.read() shl 8) or input.read()
                else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
            }
            return MPString(String(bytes(input, size).array(), encoding))
        }
    }

    companion object {
        private val FIXSTR_PREFIX = 160
        private val FIXSTR_PREFIX_FILTER = 224
        private val STR8_PREFIX = 0xD9
        private val STR8_LIMIT = 256
        private val STR16_PREFIX = 0xDA
        private val STR16_LIMIT = 65536
        private val STR32_PREFIX = 0xDB
        private val FIXSTR_FILTER = 31

        /**
         * Use this if for some messed up reason you really need to use something else than UTF-8.
         * Ask yourself: why should I? Is this really necessary?
         *
         * It will set the encoding for all [MPString]s, but if you have inconsistent encoding in your
         * format you're lost anyway.
         */
        var encoding = Charset.forName("UTF-8")
    }
}
