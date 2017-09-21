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

import ch.dissem.msgpack.Reader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Representation of a msgpack encoded array. Uses a list to represent data internally, and implements the [List]
 * interface for your convenience.

 * @param <E> content type
 */
data class MPArray<E : MPType<*>>(override val value: MutableList<E> = mutableListOf()) : MPType<MutableList<E>>, MutableList<E> {

    @SafeVarargs
    constructor(vararg objects: E) : this(mutableListOf(*objects))


    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        val size = value.size
        when {
            size < 16 -> out.write(144 + size)
            size < 65536 -> {
                out.write(0xDC)
                out.write(ByteBuffer.allocate(2).putShort(size.toShort()).array())
            }
            else -> {
                out.write(0xDD)
                out.write(ByteBuffer.allocate(4).putInt(size).array())
            }
        }
        for (o in value) {
            o.pack(out)
        }
    }

    override val size: Int
        get() = value.size

    override fun isEmpty(): Boolean {
        return value.isEmpty()
    }

    override fun contains(element: E) = value.contains(element)

    override fun iterator() = value.iterator()

    override fun add(element: E) = value.add(element)

    override fun remove(element: E) = value.remove(element)

    override fun containsAll(elements: Collection<E>) = value.containsAll(elements)

    override fun addAll(elements: Collection<E>) = value.addAll(elements)

    override fun addAll(index: Int, elements: Collection<E>) = value.addAll(index, elements)

    override fun removeAll(elements: Collection<E>) = value.removeAll(elements)

    override fun retainAll(elements: Collection<E>) = value.retainAll(elements)

    override fun clear() = value.clear()

    override fun get(index: Int) = value[index]

    override operator fun set(index: Int, element: E) = value.set(index, element)

    override fun add(index: Int, element: E) = value.add(index, element)

    override fun removeAt(index: Int) = value.removeAt(index)

    override fun indexOf(element: E) = value.indexOf(element)

    override fun lastIndexOf(element: E) = value.lastIndexOf(element)

    override fun listIterator() = value.listIterator()

    override fun listIterator(index: Int) = value.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = value.subList(fromIndex, toIndex)

    override fun toString() = toJson()

    override fun toJson() = toJson("")

    internal fun toJson(indent: String): String {
        val result = StringBuilder()
        result.append("[\n")
        val iterator = value.iterator()
        val indent2 = indent + "  "
        while (iterator.hasNext()) {
            val item = iterator.next()
            result.append(indent2)
            result.append(Utils.toJson(item, indent2))
            if (iterator.hasNext()) {
                result.append(',')
            }
            result.append('\n')
        }
        result.append("]")
        return result.toString()
    }

    class Unpacker(private val reader: Reader) : MPType.Unpacker<MPArray<*>> {

        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == 0xDC || firstByte == 0xDD || firstByte and 240 == 144
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream): MPArray<MPType<*>> {
            val size: Int = when {
                firstByte and 240 == 144 -> firstByte and 15
                firstByte == 0xDC -> input.read() shl 8 or input.read()
                firstByte == 0xDD -> input.read() shl 24 or (input.read() shl 16) or (input.read() shl 8) or input.read()
                else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
            }
            val list = mutableListOf<MPType<*>>()
            for (i in 0 until size) {
                val value = reader.read(input)
                list.add(value)
            }
            return MPArray(list)
        }
    }
}
