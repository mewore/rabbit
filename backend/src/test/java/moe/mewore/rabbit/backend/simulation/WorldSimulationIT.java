package moe.mewore.rabbit.backend.simulation;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.noise.DiamondSquareNoise;
import moe.mewore.rabbit.world.MazeMap;
import moe.mewore.rabbit.world.WorldProperties;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSimulationIT {

    private static final WorldProperties WORLD_PROPERTIES = new WorldProperties("seed", 1, 1, 1, 1, 1, 0, "");

    private static final MazeMap MAP = MazeMap.createSeamless(WORLD_PROPERTIES, new Random(),
        DiamondSquareNoise.createSeamless(1, new Random(), null, 0));

    private WorldState worldState;

    private Player player;

    @BeforeEach
    void setUp() {
        worldState = new WorldState(1, MAP);
        player = worldState.createPlayer(true);
    }

    @Test
    void testAcceptInput() {
        final var input = new PlayerInputMutation(1, 0, 0f, (byte) 0xaa);
        new WorldSimulation(worldState).acceptInput(player, input);
    }

    @Test
    void testAcceptInput_unreasonableFrame() {
        final var input = new PlayerInputMutation(-1000, 0, 0f, (byte) 0xaa);
        new WorldSimulation(worldState).acceptInput(player, input);
    }

    @Test
    void testUpdate() {
        final WorldSimulation simulation = new WorldSimulation(worldState);
        simulation.update(System.currentTimeMillis() + 100);
        assertTrue(worldState.getFrameId() > 0);
    }

    @Test
    void testUpdate_withPastInput() {
        final WorldSimulation simulation = new WorldSimulation(worldState);
        final long createdAt = System.currentTimeMillis();
        simulation.update(createdAt + 100);

        final var input = new PlayerInputMutation(0, 0, 0f, (byte) 0xaa);
        simulation.acceptInput(player, input);
        simulation.update(createdAt + 200);
    }
}
