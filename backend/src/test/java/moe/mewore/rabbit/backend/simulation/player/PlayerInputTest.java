package moe.mewore.rabbit.backend.simulation.player;

import javax.vecmath.Vector2f;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInputTest {

    @Test
    void testDecodeEncode() throws IOException {
        final byte[] initialData = new byte[Integer.BYTES + Long.BYTES + 1 + Float.BYTES];
        Arrays.fill(initialData, (byte) 25);
        final PlayerInput decoded = PlayerInput.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(initialData)));
        final byte[] encoded = decoded.encodeToBinary();
        assertArrayEquals(encoded, initialData);
    }

    @Test
    void testApplyToTargetHorizontalMotion() {
        final PlayerInput input = new PlayerInput(0, 0, PlayerInput.INPUT_UP_BIT, 1f);
        final var targetMotion = new Vector2f();
        input.applyToTargetHorizontalMotion(targetMotion, 100f);
        assertEquals("(-84.15, -54.03)", String.format("(%.2f, %.2f)", targetMotion.x, targetMotion.y));
    }

    @Test
    void testIsJumping() {
        final PlayerInput input = new PlayerInput(0, 0, PlayerInput.INPUT_UP_BIT, 0f);
        assertFalse(input.isJumping());
    }

    @Test
    void testIsJumping_jumping() {
        final PlayerInput input = new PlayerInput(0, 0, PlayerInput.INPUT_UP_BIT | PlayerInput.INPUT_JUMP_BIT, 0f);
        assertTrue(input.isJumping());
    }
}
