package moe.mewore.rabbit.geometry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Vector3Test {

    @Test
    void decodeFromBinary() throws IOException {
        final Vector3 vector = new Vector3(.1, .2, .3);

        final byte[] encoded = vector.encodeToBinary();

        final Vector3 decoded = Vector3.decodeFromBinary(new DataInputStream(new ByteArrayInputStream(encoded)));

        final byte[] reEncoded = decoded.encodeToBinary();
        assertArrayEquals(encoded, reEncoded);
    }

    @Test
    void testEncode() {
        assertEquals(24, Vector3.ZERO.encodeToBinary().length);
    }
}
