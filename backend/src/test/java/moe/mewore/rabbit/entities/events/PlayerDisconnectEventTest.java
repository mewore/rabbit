package moe.mewore.rabbit.entities.events;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDisconnectEventTest {

    @Test
    void testEncode() {
        assertEquals(5, new PlayerDisconnectEvent(new Player(1, "User", true)).encodeToBinary().length);
    }
}
