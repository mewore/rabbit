package moe.mewore.rabbit.backend.mutations;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PlayerInputMutationTest {

    @Test
    void testDecodeEncode() throws IOException {
        final byte[] initialData = new byte[2 * 4 + 1];
        Arrays.fill(initialData, (byte) 1);
        Arrays.fill(initialData, 0, 2 * 4, (byte) 25);
        final PlayerInputMutation decoded = PlayerInputMutation.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(initialData)));

        final byte[] encoded = decoded.encodeToBinary();
        assertArrayEquals(Arrays.copyOfRange(encoded, 1, encoded.length), initialData);
    }
}
