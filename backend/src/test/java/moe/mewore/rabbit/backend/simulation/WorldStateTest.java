package moe.mewore.rabbit.backend.simulation;

import javax.vecmath.Vector3f;
import java.util.Collections;

import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializationTestUtil;
import moe.mewore.rabbit.backend.simulation.player.FakePlayerInputEvent;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorldStateTest {

    private MazeMap map;

    @BeforeEach
    void setUp() {
        map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(1f);
        when(map.getDepth()).thenReturn(1f);
        when(map.getWalls()).thenReturn(Collections.emptyList());
    }

    @Test
    void testHasPlayers() {
        assertFalse(new WorldState(1, map).hasPlayers());
    }

    @Test
    void testGetPlayers() {
        assertEquals(Collections.emptyMap(), new WorldState(1, map).getPlayers());
    }

    @Test
    void testCreatePlayer() {
        final Player result = new WorldState(1, map).createPlayer(true);
        assertNotNull(result);
        assertEquals("Player 1", result.getUsername());
        assertEquals(0, result.getId());
        assertTrue(result.isReisen());
        assertEquals(new Vector3f(0f, 5f, 0f), result.getBody().getWorldTransform(new Transform()).origin);
    }

    @Test
    void testCreatePlayer_full() {
        final var worldState = new WorldState(1, map);
        assertNotNull(worldState.createPlayer(true));
        assertNull(worldState.createPlayer(true));
    }

    @Test
    void testRemovePlayer() {
        final var worldState = new WorldState(1, map);
        final Player player = worldState.createPlayer(true);
        assertNotNull(player);
        assertTrue(worldState.hasPlayers());

        worldState.removePlayer(player);
        assertFalse(worldState.hasPlayers());
    }

    @Test
    void testGetFrameSize() {
        assertEquals(204, new WorldState(3, map).getFrameSize());
    }

    @Test
    void testDoStep() {
        new WorldState(1, map).doStep();
    }

    @Test
    void testDoStep_brokenWorld() {
        new WorldState(1, map).doStep();
    }

    @Test
    void testDoStep_withPlayer() {
        final var worldState = new WorldState(1, map);
        final Player player = worldState.createPlayer(true);
        assertNotNull(player);

        assertEquals(0, worldState.getFrameId());
        worldState.doStep();
        assertEquals(1, worldState.getFrameId());
    }

    @Test
    void testLoad() {
        final var worldState = new WorldState(1, map);
        final Player player = worldState.createPlayer(true);
        assertNotNull(player);
        final byte[] frame = new byte[worldState.getFrameSize()];
        frame[7] = 10;
        worldState.load(frame);
        assertEquals(10L, worldState.getFrameId());
    }

    @Test
    void testSerialization() {
        final var worldState = new WorldState(1, map);
        worldState.createPlayer(true);

        final var otherWorldState = new WorldState(1, map);
        final var player = otherWorldState.createPlayer(false);
        assertNotNull(player);
        final var inputEvent = new FakePlayerInputEvent(player,
            new PlayerInput(1, 0, (byte) (PlayerInput.INPUT_UP_BIT | PlayerInput.INPUT_JUMP_BIT), 0f));
        otherWorldState.loadInput(new PlayerInputEvent[]{inputEvent}, true);
        for (int i = 0; i < 5; i++) {
            otherWorldState.doStep();
        }

        FrameSerializationTestUtil.testSerialization(() -> new byte[worldState.getFrameSize()],
            worldState.getHeaderFrameSection(), otherWorldState.getHeaderFrameSection(), worldState, otherWorldState);
    }
}
