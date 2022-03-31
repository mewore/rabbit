package moe.mewore.rabbit.backend.game;

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

class RabbitPlayerInputTest {

    @Test
    void testSetFrameId() {
        final RabbitPlayerInput input = new RabbitPlayerInput(0, RabbitPlayerInput.INPUT_UP_BIT, 0f, 5L);
        assertEquals(5L, input.getFrameId());
        input.setFrameId(25L);
        assertEquals(25L, input.getFrameId());
    }

    @Test
    void testDecodeEncode() throws IOException {
        final byte[] initialData = new byte[Integer.BYTES + Long.BYTES + 1 + Float.BYTES];
        Arrays.fill(initialData, (byte) 25);
        final RabbitPlayerInput decoded = RabbitPlayerInput.decodeFromBinary(
            new DataInputStream(new ByteArrayInputStream(initialData)));
        final byte[] encoded = decoded.encodeToBinary();
        assertArrayEquals(encoded, initialData);
    }

    @Test
    void testApplyToTargetHorizontalMotion() {
        final RabbitPlayerInput input = new RabbitPlayerInput(0, RabbitPlayerInput.INPUT_UP_BIT, 1f, 0);
        final var targetMotion = new Vector2f();
        input.applyToTargetHorizontalMotion(targetMotion, 100f);
        assertEquals("(-84.15, -54.03)", String.format("(%.2f, %.2f)", targetMotion.x, targetMotion.y));
    }

    @Test
    void testIsJumping() {
        final RabbitPlayerInput input = new RabbitPlayerInput(0, RabbitPlayerInput.INPUT_UP_BIT, 0f, 0);
        assertFalse(input.isJumping());
    }

    @Test
    void testIsJumping_jumping() {
        final RabbitPlayerInput input = new RabbitPlayerInput(0,
            RabbitPlayerInput.INPUT_UP_BIT | RabbitPlayerInput.INPUT_JUMP_BIT, 0f, 0L);
        assertTrue(input.isJumping());
    }
}
