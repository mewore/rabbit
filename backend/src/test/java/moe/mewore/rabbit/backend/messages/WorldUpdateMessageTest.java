package moe.mewore.rabbit.backend.messages;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.simulation.WorldState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorldUpdateMessageTest {

    @Test
    void testEncode() {
        final WorldState worldState = mock(WorldState.class);
        assertEquals(1, new WorldUpdateMessage(worldState).encodeToBinary().length);
        verify(worldState).appendToBinaryOutput(any());
    }
}
