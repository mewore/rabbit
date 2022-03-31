package moe.mewore.rabbit.backend.simulation.player;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.game.RabbitPlayerInput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerInputEventTest {

    @Test
    void testGetFrameId() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        assertEquals(121535L, inputEvent.getFrameId());
    }

    @Test
    void testCanReplace_differentPlayerUid() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        final var otherInputEvent = new PlayerInputEvent<>(1, 12, new RabbitPlayerInput(1, 0, 0, 121535L));
        assertTrue(inputEvent.canReplace(otherInputEvent));
    }

    @Test
    void testCanReplace_moreRecent() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(2, 0, 0, 121535L));
        final var otherInputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        assertTrue(inputEvent.canReplace(otherInputEvent));
    }

    @Test
    void testCanReplace_null() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        assertTrue(inputEvent.canReplace(null));
    }

    @Test
    void testCanReplace_same() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        final var otherInputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        assertFalse(inputEvent.canReplace(otherInputEvent));
    }

    @Test
    void testCanReplace_older() {
        final var inputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(1, 0, 0, 121535L));
        final var otherInputEvent = new PlayerInputEvent<>(1, 135, new RabbitPlayerInput(2, 0, 0, 121535L));
        assertFalse(inputEvent.canReplace(otherInputEvent));
    }
}
