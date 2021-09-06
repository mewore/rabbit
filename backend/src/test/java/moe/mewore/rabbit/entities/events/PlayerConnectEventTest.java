package moe.mewore.rabbit.entities.events;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerConnectEventTest {

    @Test
    void testEncode() {
        assertEquals(85, new PlayerConnectEvent(new Player(1, "User")).encodeToBinary().length);
    }
}
