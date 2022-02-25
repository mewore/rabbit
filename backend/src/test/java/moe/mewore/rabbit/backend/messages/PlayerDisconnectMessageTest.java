package moe.mewore.rabbit.backend.messages;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDisconnectMessageTest {

    @Test
    void testEncode() {
        assertEquals(5, new PlayerDisconnectMessage(new Player(1, "User", true)).encodeToBinary().length);
    }
}
