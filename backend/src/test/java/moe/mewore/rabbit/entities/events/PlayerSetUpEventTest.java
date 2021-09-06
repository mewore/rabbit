package moe.mewore.rabbit.entities.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSetUpEventTest {

    @Test
    void testEncode() {
        assertEquals(6, new PlayerSetUpEvent(1, true).encodeToBinary().length);
    }
}
