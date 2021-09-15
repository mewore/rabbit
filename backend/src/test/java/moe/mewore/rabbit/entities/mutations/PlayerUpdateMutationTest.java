package moe.mewore.rabbit.entities.mutations;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PlayerUpdateMutationTest {

    @Test
    void testDecodeEncode() throws IOException {
        final byte[] initialData = new byte[8 * 8];
        Arrays.fill(initialData, (byte) 25);
        final PlayerUpdateMutation decoded = PlayerUpdateMutation.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(initialData)));

        final byte[] encoded = decoded.encodeToBinary();
        assertArrayEquals(Arrays.copyOfRange(encoded, 1, encoded.length), initialData);
    }
}
