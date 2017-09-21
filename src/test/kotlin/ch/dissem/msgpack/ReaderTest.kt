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

package ch.dissem.msgpack

import ch.dissem.msgpack.types.*
import ch.dissem.msgpack.types.Utils.mp
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*

class ReaderTest {
    @Test
    fun `ensure demo json is parsed correctly`() {
        val read = Reader.read(stream("demo.mp"))
        assertThat(read, instanceOf(MPMap::class.java))
        assertThat(read.toString(), equalTo(string("demo.json")))
    }

    @Test
    fun `ensure demo json is encoded correctly`() {
        val obj = MPMap<MPString, MPType<*>>()
        obj.put("compact".mp, true.mp)
        obj.put("schema".mp, 0.mp)
        val out = ByteArrayOutputStream()
        obj.pack(out)
        assertThat(out.toByteArray(), equalTo(bytes("demo.mp")))
    }

    @Test
    fun `ensure mpArray is encoded and decoded correctly`() {
        val array = MPArray(
                byteArrayOf(1, 3, 3, 7).mp,
                false.mp,
                Math.PI.mp,
                1.5f.mp,
                42.mp,
                MPMap<MPNil, MPNil>(),
                MPNil,
                "yay! \uD83E\uDD13".mp
        )
        val out = ByteArrayOutputStream()
        array.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf(MPArray::class.java))
        @Suppress("UNCHECKED_CAST")
        assertThat(read as MPArray<MPType<*>>, equalTo(array))
        assertThat(read.toJson(), equalTo("[\n  AQMDBw==,\n  false,\n  3.141592653589793,\n  1.5,\n  42,\n  {\n  },\n  null,\n  \"yay! 🤓\"\n]"))
    }

    @Test
    fun `ensure float is encoded and decoded correctly`() {
        val expected = MPFloat(1.5f)
        val out = ByteArrayOutputStream()
        expected.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf<Any>(MPFloat::class.java))
        val actual = read as MPFloat
        assertThat(actual, equalTo(expected))
        assertThat(actual.precision, equalTo(MPFloat.Precision.FLOAT32))
    }

    @Test
    fun `ensure double is encoded and decoded correctly`() {
        val expected = MPFloat(Math.PI)
        val out = ByteArrayOutputStream()
        expected.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf<Any>(MPFloat::class.java))
        val actual = read as MPFloat
        assertThat(actual, equalTo(expected))
        assertThat(actual.value, equalTo(Math.PI))
        assertThat(actual.precision, equalTo(MPFloat.Precision.FLOAT64))
    }

    @Test
    fun `ensure longs are encoded and decoded correctly`() {
        // positive fixnum
        ensureLongIsEncodedAndDecodedCorrectly(0, 1)
        ensureLongIsEncodedAndDecodedCorrectly(127, 1)
        // negative fixnum
        ensureLongIsEncodedAndDecodedCorrectly(-1, 1)
        ensureLongIsEncodedAndDecodedCorrectly(-32, 1)
        // uint 8
        ensureLongIsEncodedAndDecodedCorrectly(128, 2)
        ensureLongIsEncodedAndDecodedCorrectly(255, 2)
        // uint 16
        ensureLongIsEncodedAndDecodedCorrectly(256, 3)
        ensureLongIsEncodedAndDecodedCorrectly(65535, 3)
        // uint 32
        ensureLongIsEncodedAndDecodedCorrectly(65536, 5)
        ensureLongIsEncodedAndDecodedCorrectly(4294967295L, 5)
        // uint 64
        ensureLongIsEncodedAndDecodedCorrectly(4294967296L, 9)
        ensureLongIsEncodedAndDecodedCorrectly(java.lang.Long.MAX_VALUE, 9)
        // int 8
        ensureLongIsEncodedAndDecodedCorrectly(-33, 2)
        ensureLongIsEncodedAndDecodedCorrectly(-128, 2)
        // int 16
        ensureLongIsEncodedAndDecodedCorrectly(-129, 3)
        ensureLongIsEncodedAndDecodedCorrectly(-32768, 3)
        // int 32
        ensureLongIsEncodedAndDecodedCorrectly(-32769, 5)
        ensureLongIsEncodedAndDecodedCorrectly(Integer.MIN_VALUE.toLong(), 5)
        // int 64
        ensureLongIsEncodedAndDecodedCorrectly(-2147483649L, 9)
        ensureLongIsEncodedAndDecodedCorrectly(java.lang.Long.MIN_VALUE, 9)
    }

    private fun ensureLongIsEncodedAndDecodedCorrectly(`val`: Long, bytes: Int) {
        val value = `val`.mp
        val out = ByteArrayOutputStream()
        value.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(out.size(), equalTo(bytes))
        assertThat(read, instanceOf<Any>(MPInteger::class.java))
        assertThat(read as MPInteger, equalTo(value))
    }

    @Test
    fun `ensure strings are encoded and decoded correctly`() {
        ensureStringIsEncodedAndDecodedCorrectly(0)
        ensureStringIsEncodedAndDecodedCorrectly(31)
        ensureStringIsEncodedAndDecodedCorrectly(32)
        ensureStringIsEncodedAndDecodedCorrectly(255)
        ensureStringIsEncodedAndDecodedCorrectly(256)
        ensureStringIsEncodedAndDecodedCorrectly(65535)
        ensureStringIsEncodedAndDecodedCorrectly(65536)
    }

    @Test
    fun `ensure json strings are escaped correctly`() {
        val builder = StringBuilder()
        var c = '\u0001'
        while (c < ' ') {
            builder.append(c)
            c++
        }
        val string = MPString(builder.toString())
        assertThat(string.toJson(), equalTo("\"\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\b\\t\\n\\u000b\\u000c\\r\\u000e\\u000f\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b\\u001c\\u001d\\u001e\\u001f\""))
    }

    private fun ensureStringIsEncodedAndDecodedCorrectly(length: Int) {
        val value = MPString(stringWithLength(length))
        val out = ByteArrayOutputStream()
        value.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf<Any>(MPString::class.java))
        assertThat(read as MPString, equalTo(value))
    }

    @Test
    fun `ensure binaries are encoded and decoded correctly`() {
        ensureBinaryIsEncodedAndDecodedCorrectly(0)
        ensureBinaryIsEncodedAndDecodedCorrectly(255)
        ensureBinaryIsEncodedAndDecodedCorrectly(256)
        ensureBinaryIsEncodedAndDecodedCorrectly(65535)
        ensureBinaryIsEncodedAndDecodedCorrectly(65536)
    }

    private fun ensureBinaryIsEncodedAndDecodedCorrectly(length: Int) {
        val value = MPBinary(ByteArray(length))
        RANDOM.nextBytes(value.value)
        val out = ByteArrayOutputStream()
        value.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf<Any>(MPBinary::class.java))
        assertThat(read as MPBinary, equalTo(value))
    }

    @Test
    fun `ensure arrays are encoded and decoded correctly`() {
        ensureArrayIsEncodedAndDecodedCorrectly(0)
        ensureArrayIsEncodedAndDecodedCorrectly(15)
        ensureArrayIsEncodedAndDecodedCorrectly(16)
        ensureArrayIsEncodedAndDecodedCorrectly(65535)
        ensureArrayIsEncodedAndDecodedCorrectly(65536)
    }

    private fun ensureArrayIsEncodedAndDecodedCorrectly(length: Int) {
        val nil = MPNil
        val list = ArrayList<MPNil>(length)
        for (i in 0 until length) {
            list.add(nil)
        }
        val value = MPArray(list)
        val out = ByteArrayOutputStream()
        value.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf(MPArray::class.java))
        @Suppress("UNCHECKED_CAST")
        assertThat(read as MPArray<MPNil>, equalTo(value))
    }

    @Test
    fun `ensure maps are encoded and decoded correctly`() {
        ensureMapIsEncodedAndDecodedCorrectly(0)
        ensureMapIsEncodedAndDecodedCorrectly(15)
        ensureMapIsEncodedAndDecodedCorrectly(16)
        ensureMapIsEncodedAndDecodedCorrectly(65535)
        ensureMapIsEncodedAndDecodedCorrectly(65536)
    }

    private fun ensureMapIsEncodedAndDecodedCorrectly(size: Int) {
        val nil = MPNil
        val map = HashMap<MPInteger, MPNil>(size)
        for (i in 0..size - 1) {
            map.put(i.mp, nil)
        }
        val value = MPMap(map)
        val out = ByteArrayOutputStream()
        value.pack(out)
        val read = Reader.read(ByteArrayInputStream(out.toByteArray()))
        assertThat(read, instanceOf<Any>(MPMap::class.java))
        @Suppress("UNCHECKED_CAST")
        assertThat(read as MPMap<MPInteger, MPNil>, equalTo(value))
    }

    private fun stringWithLength(length: Int): String {
        val result = StringBuilder(length)
        for (i in 0..length - 1) {
            result.append('a')
        }
        return result.toString()
    }

    private fun stream(resource: String): InputStream {
        return javaClass.classLoader.getResourceAsStream(resource)
    }

    private fun bytes(resource: String): ByteArray {
        val `in` = stream(resource)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(100)
        var size = `in`.read(buffer)
        while (size >= 0) {
            out.write(buffer, 0, size)
            size = `in`.read(buffer)
        }
        return out.toByteArray()
    }

    private fun string(resource: String): String {
        return String(bytes(resource))
    }

    companion object {
        private val RANDOM = Random()
    }
}
