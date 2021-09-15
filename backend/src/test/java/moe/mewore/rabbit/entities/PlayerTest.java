package moe.mewore.rabbit.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {

    @Test
    void testEncode() {
        assertEquals(82, new Player(18, "Player 18", true).encodeToBinary().length);
    }
}
