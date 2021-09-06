package moe.mewore.rabbit.entities.mutations;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSetUpMutationTest {

    @Test
    void testDecodeEncode() throws IOException {
        final byte[] initialData = new byte[]{1};

        final PlayerSetUpMutation decoded = PlayerSetUpMutation.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(initialData)));

        final byte[] encoded = decoded.encodeToBinary();
        assertArrayEquals(Arrays.copyOfRange(encoded, 1, encoded.length), initialData);
    }

    @Test
    void testEncode() {
        assertEquals(2, new PlayerSetUpMutation(true).encodeToBinary().length);
    }
}
