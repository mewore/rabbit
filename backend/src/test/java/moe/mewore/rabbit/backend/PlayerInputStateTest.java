package moe.mewore.rabbit.backend;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInputStateTest {

    @Test
    void testApplyInput() {
        final var inputState = new PlayerInputState();
        assertEquals(0, inputState.getInputId());
        assertFalse(inputState.isJumping());
        assertEquals(0, inputState.getInputKeys());
        assertEquals("(0.00, 0.00)", String.format("(%.2f, %.2f)", inputState.getTargetHorizontalMotion().x,
            inputState.getTargetHorizontalMotion().y));

        inputState.applyInput(5, 1f,
            PlayerInputMutation.INPUT_LEFT_BIT | PlayerInputMutation.INPUT_UP_BIT | PlayerInputMutation.INPUT_JUMP_BIT);
        assertEquals(5, inputState.getInputId());
        assertTrue(inputState.isJumping());
        assertNotEquals(0, inputState.getInputKeys());
        assertEquals("(-97.71, 21.30)", String.format("(%.2f, %.2f)", inputState.getTargetHorizontalMotion().x,
            inputState.getTargetHorizontalMotion().y));
        assertEquals(100, Math.round(inputState.getTargetHorizontalMotion().length()));
    }
}
