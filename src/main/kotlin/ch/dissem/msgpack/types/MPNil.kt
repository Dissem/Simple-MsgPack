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
 * Representation of msgpack encoded nil / null.
 */
object MPNil : MPType<Void?> {

    override val value: Void? = null

    @Throws(IOException::class)
    override fun pack(out: OutputStream) = out.write(NIL)

    override fun toString() = "null"

    override fun toJson() = "null"

    class Unpacker : MPType.Unpacker<MPNil> {

        override fun doesUnpack(firstByte: Int) = firstByte == NIL

        override fun unpack(firstByte: Int, input: InputStream): MPNil {
            return when (firstByte) {
                NIL -> MPNil
                else -> throw IllegalArgumentException(String.format("0xC0 expected but was 0x%02x", firstByte))
            }
        }
    }

    private val NIL = 0xC0
}
