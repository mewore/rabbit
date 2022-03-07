package moe.mewore.rabbit.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {

    @Test
    void testEncode() {
        assertEquals(70, new Player(51, 18, "Player 18", true).encodeToBinary().length);
    }
}
