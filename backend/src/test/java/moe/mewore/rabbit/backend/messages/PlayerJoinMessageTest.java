package moe.mewore.rabbit.backend.messages;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerJoinMessageTest {

    @Test
    void testEncode() {
        assertEquals(15, new PlayerJoinMessage(new Player(1, 1, "User", true), false).encodeToBinary().length);
    }
}
