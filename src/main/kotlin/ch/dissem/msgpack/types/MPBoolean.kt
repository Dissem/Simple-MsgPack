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
import java.io.OutputStream

/**
 * Representation of a msgpack encoded boolean.
 */
data class MPBoolean(override val value: Boolean) : MPType<Boolean> {

    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        out.write(if (value) {
            TRUE
        } else {
            FALSE
        })
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun toJson(): String {
        return value.toString()
    }

    class Unpacker : MPType.Unpacker<MPBoolean> {

        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == TRUE || firstByte == FALSE
        }

        override fun unpack(firstByte: Int, input: InputStream) = when (firstByte) {
            TRUE -> MPBoolean(true)
            FALSE -> MPBoolean(false)
            else -> throw IllegalArgumentException(String.format("0xC2 or 0xC3 expected but was 0x%02x", firstByte))
        }
    }

    companion object {
        private const val FALSE = 0xC2
        private const val TRUE = 0xC3
    }
}
