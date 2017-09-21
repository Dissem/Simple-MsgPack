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

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

object Utils {
    private val BASE64_CODES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray()

    /**
     * Returns a [ByteBuffer] containing the next `count` bytes from the [InputStream].
     */
    @Throws(IOException::class)
    internal fun bytes(`in`: InputStream, count: Int): ByteBuffer {
        val result = ByteArray(count)
        var off = 0
        while (off < count) {
            val read = `in`.read(result, off, count - off)
            if (read < 0) {
                throw IOException("Unexpected end of stream, wanted to read $count bytes but only got $off")
            }
            off += read
        }
        return ByteBuffer.wrap(result)
    }

    /**
     * Helper method to decide which types support extra indention (for pretty printing JSON)
     */
    internal fun toJson(type: MPType<*>, indent: String): String {
        if (type is MPMap<*, *>) {
            return type.toJson(indent)
        }
        if (type is MPArray<*>) {
            return type.toJson(indent)
        }
        return type.toJson()
    }

    /**
     * Slightly improved code from https://en.wikipedia.org/wiki/Base64
     */
    internal fun base64(data: ByteArray): String {
        val result = StringBuilder(data.size * 4 / 3 + 3)
        var b: Int
        var i = 0
        while (i < data.size) {
            b = data[i].toInt() and 0xFC shr 2
            result.append(BASE64_CODES[b])
            b = data[i].toInt() and 0x03 shl 4
            if (i + 1 < data.size) {
                b = b or (data[i + 1].toInt() and 0xF0 shr 4)
                result.append(BASE64_CODES[b])
                b = data[i + 1].toInt() and 0x0F shl 2
                if (i + 2 < data.size) {
                    b = b or (data[i + 2].toInt() and 0xC0 shr 6)
                    result.append(BASE64_CODES[b])
                    b = data[i + 2].toInt() and 0x3F
                    result.append(BASE64_CODES[b])
                } else {
                    result.append(BASE64_CODES[b])
                    result.append('=')
                }
            } else {
                result.append(BASE64_CODES[b])
                result.append("==")
            }
            i += 3
        }

        return result.toString()
    }

    @JvmStatic
    val String.mp
        @JvmName("mp")
        get() = MPString(this)

    @JvmStatic
    val Boolean.mp
        @JvmName("mp")
        get() = MPBoolean(this)

    @JvmStatic
    val Float.mp
        @JvmName("mp")
        get() = MPFloat(this)

    @JvmStatic
    val Double.mp
        @JvmName("mp")
        get() = MPFloat(this)

    @JvmStatic
    val Int.mp
        @JvmName("mp")
        get() = MPInteger(this.toLong())

    @JvmStatic
    val Long.mp
        @JvmName("mp")
        get() = MPInteger(this)

    @JvmStatic
    val ByteArray.mp
        get() = MPBinary(this)

    @JvmStatic
    fun mp(vararg data: Byte): MPBinary {
        return MPBinary(data)
    }

    @JvmStatic
    fun nil(): MPNil {
        return MPNil
    }
}
