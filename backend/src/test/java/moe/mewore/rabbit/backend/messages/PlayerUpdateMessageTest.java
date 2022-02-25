package moe.mewore.rabbit.backend.messages;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.PlayerState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlayerUpdateMessageTest {

    @Test
    void testEncode() {
        final PlayerState playerState = mock(PlayerState.class);
        assertEquals(5, new PlayerUpdateMessage(1, playerState).encodeToBinary().length);
        verify(playerState).appendToBinaryOutput(any());
    }
}
