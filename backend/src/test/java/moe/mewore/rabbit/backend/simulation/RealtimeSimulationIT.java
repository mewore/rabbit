package moe.mewore.rabbit.backend.simulation;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.RabbitPlayer;
import moe.mewore.rabbit.noise.DiamondSquareNoise;
import moe.mewore.rabbit.world.MazeMap;
import moe.mewore.rabbit.world.WorldProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeSimulationIT {

    private static final WorldProperties WORLD_PROPERTIES = new WorldProperties("seed", 1, 1, 1, 1, 1, 0, "");

    private static final MazeMap MAP = MazeMap.createSeamless(WORLD_PROPERTIES, new Random(),
        DiamondSquareNoise.createSeamless(1, new Random(), null, 0));

    private RabbitWorldState worldState;

    private RabbitPlayer player;

    @BeforeEach
    void setUp() {
        worldState = new RabbitWorldState(1, MAP);
        player = worldState.createPlayer(true);
    }

    @Test
    void testAcceptInput() throws InterruptedException {
        final var input = new PlayerInput(1, 0, 0xaa, 0f);
        final var simulation = new RealtimeSimulation(worldState);

        simulation.acceptInput(player, input);
        assertNull(simulation.getLastAppliedInputs());
    }

    @Test
    void testAcceptInput_unreasonableFrame() throws InterruptedException {
        final var input = new PlayerInput(-1000, -1000L, 0xaa, 0f);
        final var simulation = new RealtimeSimulation(worldState);
        simulation.acceptInput(player, input);

        simulation.update(-1);
        assertNotNull(simulation.getLastAppliedInputs());
        assertEquals(1, simulation.getLastAppliedInputs().size());
        assertEquals(1L, simulation.getLastAppliedInputs().get(0).getFrameId());
        assertNotSame(input, simulation.getLastAppliedInputs().get(0).getInput());
    }

    @Test
    void testUpdate() {
        final RealtimeSimulation simulation = new RealtimeSimulation(worldState);
        simulation.update(System.currentTimeMillis() + 100);
        assertTrue(worldState.getFrameId() > 0);
        assertNull(simulation.getLastAppliedInputs());
    }

    @Test
    void testUpdate_withPastInput() throws InterruptedException {
        final RealtimeSimulation simulation = new RealtimeSimulation(worldState);
        final long createdAt = System.currentTimeMillis();
        simulation.update(createdAt + 100);

        final var input = new PlayerInput(0, 1, 0xaa, 0f);
        simulation.acceptInput(player, input);

        simulation.update(createdAt + 200);
        assertNotNull(simulation.getLastAppliedInputs());
        assertEquals(1, simulation.getLastAppliedInputs().size());
        assertSame(input, simulation.getLastAppliedInputs().get(0).getInput());
    }
}
