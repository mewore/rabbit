package moe.mewore.rabbit.entities.messages;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerJoinMessageTest {

    @Test
    void testEncode() {
        assertEquals(86, new PlayerJoinMessage(new Player(1, "User", true)).encodeToBinary().length);
    }
}