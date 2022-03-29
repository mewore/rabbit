package moe.mewore.rabbit.backend.messages;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.simulation.WorldState;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldUpdateMessageTest {

    @Test
    void testEncode() {
        final var worldState = mock(WorldState.class);

        final var player = mock(Player.class);
        when(player.getId()).thenReturn(1);
        when(player.getLatency()).thenReturn(100);
        when(worldState.getPlayers()).thenReturn(Map.of(1, player, 2, player));

        when(worldState.getMaxPlayerCount()).thenReturn(2);
        when(worldState.getSpheres()).thenReturn(new PhysicsDummySphere[10]);

        assertEquals(59,
            new WorldUpdateMessage(worldState, List.of(new PlayerInputEvent(1, 125, new PlayerInput(4, 124L, 0, 0))),
                new byte[1]).encodeToBinary().length);
    }
}
