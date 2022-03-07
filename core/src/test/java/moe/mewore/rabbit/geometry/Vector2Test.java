package moe.mewore.rabbit.geometry;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Vector2Test {

    @Test
    void decodeFromBinary() throws IOException {
        final Vector2 vector = new Vector2(.1, .2);

        final byte[] encoded = vector.encodeToBinary();

        final Vector2 decoded = Vector2.decodeFromBinary(new DataInputStream(new ByteArrayInputStream(encoded)));

        final byte[] reEncoded = decoded.encodeToBinary();
        assertArrayEquals(encoded, reEncoded);
    }

    @Test
    void testEncode() {
        assertEquals(16, new Vector2().encodeToBinary().length);
    }
}
