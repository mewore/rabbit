package moe.mewore.rabbit.entities.events;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerJoinEventTest {

    @Test
    void testEncode() {
        assertEquals(86, new PlayerJoinEvent(new Player(1, "User", true)).encodeToBinary().length);
    }
}
