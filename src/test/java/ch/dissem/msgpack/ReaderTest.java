package ch.dissem.msgpack;

import ch.dissem.msgpack.types.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReaderTest {
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
//                new MPBinary(new byte[]{1, 3, 3, 7}),
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
