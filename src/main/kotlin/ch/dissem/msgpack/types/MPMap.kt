package ch.dissem.msgpack.types

import ch.dissem.msgpack.Reader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Representation of a msgpack encoded map. It is recommended to use a [LinkedHashMap] to ensure the order
 * of entries. For convenience, it also implements the [Map] interface.
 *
 * @param <K> key type
 * @param <V> value type
 */
data class MPMap<K : MPType<*>, V : MPType<*>>(override val value: MutableMap<K, V> = LinkedHashMap<K, V>()) : MPType<Map<K, V>>, MutableMap<K, V> {


    @Throws(IOException::class)
    override fun pack(out: OutputStream) {
        val size = value.size
        when {
            size < 16 -> {
                out.write(0x80 + size)
            }
            size < 65536 -> {
                out.write(0xDE)
                out.write(ByteBuffer.allocate(2).putShort(size.toShort()).array())
            }
            else -> {
                out.write(0xDF)
                out.write(ByteBuffer.allocate(4).putInt(size).array())
            }
        }
        for ((key, value1) in value) {
            key.pack(out)
            value1.pack(out)
        }
    }

    override val size: Int
        get() = value.size

    override fun isEmpty() = value.isEmpty()

    override fun containsKey(key: K) = value.containsKey(key)

    override fun containsValue(value: V) = this.value.containsValue(value)

    override fun get(key: K) = value[key]

    override fun putIfAbsent(key: K, value: V) = this.value.putIfAbsent(key, value)

    override fun put(key: K, value: V): V? = this.value.put(key, value)

    override fun remove(key: K): V? = value.remove(key)

    override fun putAll(from: Map<out K, V>) = value.putAll(from)

    override fun clear() = value.clear()

    override val keys: MutableSet<K>
        get() = value.keys

    override val values: MutableCollection<V>
        get() = value.values

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = value.entries

    override fun toString() = toJson()

    internal fun toJson(indent: String) = if (value.entries.isEmpty()) {
        "{}"
    } else {
        value.entries.joinToString(
                separator = ",\n",
                prefix = "{\n",
                postfix = "\n$indent}") { i ->
            "$indent  ${Utils.toJson(i.key, "$indent  ")}: ${Utils.toJson(i.value, "$indent  ")}"
        }
    }

    override fun toJson(): String {
        return toJson("")
    }

    class Unpacker(private val reader: Reader) : MPType.Unpacker<MPMap<*, *>> {

        override fun doesUnpack(firstByte: Int): Boolean {
            return firstByte == 0xDE || firstByte == 0xDF || firstByte and 0xF0 == 0x80
        }

        @Throws(IOException::class)
        override fun unpack(firstByte: Int, input: InputStream): MPMap<MPType<*>, MPType<*>> {
            val size: Int = when {
                firstByte and 0xF0 == 0x80 -> firstByte and 0x0F
                firstByte == 0xDE -> input.read() shl 8 or input.read()
                firstByte == 0xDF -> input.read() shl 24 or (input.read() shl 16) or (input.read() shl 8) or input.read()
                else -> throw IllegalArgumentException(String.format("Unexpected first byte 0x%02x", firstByte))
            }
            val map = LinkedHashMap<MPType<*>, MPType<*>>()
            for (i in 0 until size) {
                val key = reader.read(input)
                val value = reader.read(input)
                map.put(key, value)
            }
            return MPMap(map)
        }
    }
}
