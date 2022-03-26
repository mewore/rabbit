package moe.mewore.rabbit.backend.messages;

import java.util.Map;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.simulation.WorldSnapshot;
import moe.mewore.rabbit.backend.simulation.WorldState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorldUpdateMessageTest {

    @Test
    void testEncode() {
        final var worldState = mock(WorldState.class);
        final var snapshot = mock(WorldSnapshot.class);

        final var player = mock(Player.class);
        when(player.getId()).thenReturn(1);
        when(player.getLatency()).thenReturn(100);
        when(worldState.getPlayers()).thenReturn(Map.of(1, player, 2, player));

        when(worldState.getMaxPlayerCount()).thenReturn(2);
        when(worldState.getSpheres()).thenReturn(new PhysicsDummySphere[10]);

        assertEquals(29, new WorldUpdateMessage(worldState, snapshot).encodeToBinary().length);
        verify(snapshot).appendToBinaryOutput(any());
    }
}
