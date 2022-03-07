package moe.mewore.rabbit.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStateTest {

    @Test
    void testEncode() {
        assertEquals(48, new PlayerState().encodeToBinary().length);
    }
}
