package moe.mewore.rabbit.entities.events;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.entities.PlayerState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerUpdateEventTest {

    @Test
    void testEncode() {
        final PlayerState playerState = mock(PlayerState.class);
        assertEquals(5, new PlayerUpdateEvent(1, playerState).encodeToBinary().length);
        verify(playerState).appendToBinaryOutput(any());
    }
}
