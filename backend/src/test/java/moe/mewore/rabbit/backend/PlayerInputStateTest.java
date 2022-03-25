package moe.mewore.rabbit.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerInputStateTest {

    @Test
    void testApplyInput() {
        assertEquals(9, new PlayerInputState().encodeToBinary().length);
    }
}
