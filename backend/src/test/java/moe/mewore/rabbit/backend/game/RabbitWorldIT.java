package moe.mewore.rabbit.backend.game;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.simulation.data.FrameSerializationTestUtil;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RabbitWorldIT {

    private MazeMap map;

    @BeforeEach
    void setUp() {
        map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(1f);
        when(map.getDepth()).thenReturn(1f);
        when(map.getWalls()).thenReturn(Collections.emptyList());
    }

    @Test
    void testSerialization() {
        final var world = new RabbitWorld(1, map, RabbitWorld.createPhysicsWorld());
        world.createPlayer(true);

        final var otherWorld = new RabbitWorld(1, map, RabbitWorld.createPhysicsWorld());
        final var player = otherWorld.createPlayer(false);
        assertNotNull(player);
        final var input = new RabbitPlayerInput(1,
            (byte) (RabbitPlayerInput.INPUT_UP_BIT | RabbitPlayerInput.INPUT_JUMP_BIT), 0f, 0L);
        otherWorld.applyInputs(List.of(new ArrayDeque<>(List.of(input))), false);
        for (int i = 0; i < 5; i++) {
            otherWorld.doStep(1f);
        }

        FrameSerializationTestUtil.testSerialization(() -> new byte[world.getFrameSize()],
            world.getHeaderFrameSection(), otherWorld.getHeaderFrameSection(), world, otherWorld);
    }
}
