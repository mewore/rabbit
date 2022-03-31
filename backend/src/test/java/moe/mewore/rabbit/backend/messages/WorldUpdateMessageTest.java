package moe.mewore.rabbit.backend.messages;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.game.RabbitPlayer;
import moe.mewore.rabbit.backend.game.RabbitPlayerInput;
import moe.mewore.rabbit.backend.game.RabbitWorld;
import moe.mewore.rabbit.backend.physics.PhysicsDummySphere;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldUpdateMessageTest {

    @Test
    void testEncode() {
        final var world = mock(RabbitWorld.class);

        final var player = mock(RabbitPlayer.class);
        when(player.getIndex()).thenReturn(1);
        when(player.getLatency()).thenReturn(100);
        when(world.getPlayersAsMap()).thenReturn(Map.of(1, player, 2, player));

        when(world.getMaxPlayerCount()).thenReturn(2);
        when(world.getSpheres()).thenReturn(new PhysicsDummySphere[10]);

        assertEquals(59,
            new WorldUpdateMessage(world, List.of(new PlayerInputEvent<>(1, 125, new RabbitPlayerInput(4, 0, 0, 124L))),
                new byte[1]).encodeToBinary().length);
    }
}
