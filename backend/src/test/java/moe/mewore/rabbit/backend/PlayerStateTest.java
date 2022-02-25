package moe.mewore.rabbit.backend;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.geometry.Vector2;
import moe.mewore.rabbit.geometry.Vector3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStateTest {

    @Test
    void testReEncode() throws IOException {
        final PlayerState playerState = new PlayerState(new Vector3(.1, .2, .3), new Vector3(.4, .5, .6),
            new Vector2(.7, .8));

        final byte[] encoded = playerState.encodeToBinary();

        final PlayerState decoded = PlayerState.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(encoded)));

        final byte[] reEncoded = decoded.encodeToBinary();
        assertArrayEquals(encoded, reEncoded);
    }

    @Test
    void testEncode() {
        assertEquals(64, new PlayerState().encodeToBinary().length);
    }
}