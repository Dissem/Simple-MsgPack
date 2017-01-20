package ch.dissem.msgpack;

import ch.dissem.msgpack.types.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReaderTest {
    private static final Random RANDOM = new Random();
    private Reader reader = new Reader();

    @Test
    public void ensureDemoJsonIsParsedCorrectly() throws Exception {
        MPType read = reader.read(stream("demo.mp"));
        assertThat(read, instanceOf(MPMap.class));
        assertThat(read.toString(), is(string("demo.json")));
    }

    @Test
    public void ensureDemoJsonIsEncodedCorrectly() throws Exception {
        MPMap<MPString, MPType<?>> object = new MPMap<>(new LinkedHashMap<MPString, MPType<?>>());
        object.getValue().put(new MPString("compact"), new MPBoolean(true));
        object.getValue().put(new MPString("schema"), new MPInteger(0));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        object.pack(out);
        assertThat(out.toByteArray(), is(bytes("demo.mp")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ensureMPArrayIsEncodedAndDecodedCorrectly() throws Exception {
        MPArray<MPType<?>> array = new MPArray<>(
                new MPBinary(new byte[]{1, 3, 3, 7}),
                new MPBoolean(false),
                new MPDouble(Math.PI),
                new MPFloat(1.5f),
                new MPInteger(42),
                new MPMap<>(new HashMap<MPNil, MPNil>()),
                new MPNil(),
                new MPString("yay!") // TODO: emoji
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        array.pack(out);
        MPType read = reader.read(new ByteArrayInputStream(out.toByteArray()));
        assertThat(read, instanceOf(MPArray.class));
        assertThat((MPArray<MPType<?>>) read, is(array));
    }

    @Test
    public void ensureStringsAreEncodedAndDecodedCorrectly() throws Exception {
        ensureStringIsEncodedAndDecodedCorrectly(0);
        ensureStringIsEncodedAndDecodedCorrectly(31);
        ensureStringIsEncodedAndDecodedCorrectly(32);
        ensureStringIsEncodedAndDecodedCorrectly(255);
        ensureStringIsEncodedAndDecodedCorrectly(256);
        ensureStringIsEncodedAndDecodedCorrectly(65535);
        ensureStringIsEncodedAndDecodedCorrectly(65536);
    }

    private void ensureStringIsEncodedAndDecodedCorrectly(int length) throws Exception {
        MPString value = new MPString(stringWithLength(length));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        value.pack(out);
        MPType read = reader.read(new ByteArrayInputStream(out.toByteArray()));
        assertThat(read, instanceOf(MPString.class));
        assertThat((MPString) read, is(value));
    }

    @Test
    public void ensureBinariesAreEncodedAndDecodedCorrectly() throws Exception {
        ensureBinaryIsEncodedAndDecodedCorrectly(0);
        ensureBinaryIsEncodedAndDecodedCorrectly(255);
        ensureBinaryIsEncodedAndDecodedCorrectly(256);
        ensureBinaryIsEncodedAndDecodedCorrectly(65535);
        ensureBinaryIsEncodedAndDecodedCorrectly(65536);
    }

    private void ensureBinaryIsEncodedAndDecodedCorrectly(int length) throws Exception {
        MPBinary value = new MPBinary(new byte[length]);
        RANDOM.nextBytes(value.getValue());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        value.pack(out);
        MPType read = reader.read(new ByteArrayInputStream(out.toByteArray()));
        assertThat(read, instanceOf(MPBinary.class));
        assertThat((MPBinary) read, is(value));
    }

    @Test
    public void ensureArraysAreEncodedAndDecodedCorrectly() throws Exception {
        ensureArrayIsEncodedAndDecodedCorrectly(0);
        ensureArrayIsEncodedAndDecodedCorrectly(15);
        ensureArrayIsEncodedAndDecodedCorrectly(16);
        ensureArrayIsEncodedAndDecodedCorrectly(65535);
        ensureArrayIsEncodedAndDecodedCorrectly(65536);
    }

    @SuppressWarnings("unchecked")
    private void ensureArrayIsEncodedAndDecodedCorrectly(int length) throws Exception {
        MPNil nil = new MPNil();
        ArrayList<MPNil> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(nil);
        }
        MPArray<MPNil> value = new MPArray<>(list);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        value.pack(out);
        MPType read = reader.read(new ByteArrayInputStream(out.toByteArray()));
        assertThat(read, instanceOf(MPArray.class));
        assertThat((MPArray<MPNil>) read, is(value));
    }

    @Test
    public void ensureMapsAreEncodedAndDecodedCorrectly() throws Exception {
        ensureMapIsEncodedAndDecodedCorrectly(0);
        ensureMapIsEncodedAndDecodedCorrectly(15);
        ensureMapIsEncodedAndDecodedCorrectly(16);
        ensureMapIsEncodedAndDecodedCorrectly(65535);
        ensureMapIsEncodedAndDecodedCorrectly(65536);
    }

    @SuppressWarnings("unchecked")
    private void ensureMapIsEncodedAndDecodedCorrectly(int length) throws Exception {
        MPNil nil = new MPNil();
        HashMap<MPNil, MPNil> map = new HashMap<>(length);
        for (int i = 0; i < length; i++) {
            map.put(nil, nil);
        }
        MPMap<MPNil, MPNil> value = new MPMap<>(map);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        value.pack(out);
        MPType read = reader.read(new ByteArrayInputStream(out.toByteArray()));
        assertThat(read, instanceOf(MPMap.class));
        assertThat((MPMap<MPNil, MPNil>) read, is(value));
    }

    private String stringWithLength(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append('a');
        }
        return result.toString();
    }

    private InputStream stream(String resource) {
        return getClass().getClassLoader().getResourceAsStream(resource);
    }

    private byte[] bytes(String resource) throws IOException {
        InputStream in = stream(resource);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[100];
        for (int size = in.read(buffer); size >= 0; size = in.read(buffer)) {
            out.write(buffer, 0, size);
        }
        return out.toByteArray();
    }

    private String string(String resource) throws IOException {
        return new String(bytes(resource), "UTF-8");
    }
}
