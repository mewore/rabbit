package moe.mewore.rabbit.backend.simulation;

import javax.vecmath.Vector3f;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.data.ByteArrayDataOutput;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void testRegisterInput() {
        final WorldSnapshot snapshot = new WorldSnapshot(10, 10);
        final var player = new Player(0, 0, "Player", false, mock(CollisionWorld.class), mock(CollisionObject.class),
            mock(RigidBodyController.class));

        final int[] emptyIntArray = new int[10];
        final float[] emptyFloatArray = new float[10];
        assertArrayEquals(emptyIntArray, snapshot.getIntData());
        assertArrayEquals(emptyFloatArray, snapshot.getFloatData());

        final var input = new PlayerInputMutation(1, 0, 0f, (byte) 0xaa);
        WorldState.registerInput(snapshot, player, input);
        assertNotEquals(List.of(emptyIntArray), List.of(snapshot.getIntData()));
        assertNotEquals(List.of(emptyFloatArray), List.of(snapshot.getFloatData()));
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
    void testCreateEmptySnapshot() {
        final WorldSnapshot snapshot = new WorldState(3, map).createEmptySnapshot();
        assertEquals(13, snapshot.getIntData().length);
        assertEquals(51, snapshot.getFloatData().length);
    }

    @Test
    void testDoStep() {
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
        assertNotNull(worldState.createPlayer(true));
        worldState.load(worldState.createEmptySnapshot(), 18);
        assertEquals(18, worldState.getFrameId());
    }

    @Test
    void testLoadInput() {
        final var worldState = new WorldState(1, map);
        final Player player = worldState.createPlayer(true);
        assertNotNull(player);

        final WorldSnapshot snapshot = worldState.createEmptySnapshot();
        final var input = new PlayerInputMutation(1, 0, (float) Math.PI, (byte) 0xaa);
        WorldState.registerInput(snapshot, player, input);
        worldState.loadInput(snapshot);
        assertEquals(0, player.getInputState().getInputKeys());
    }

    @Test
    void testLoadInput_matchingUid() {
        final var worldState = new WorldState(1, map);
        final Player player = worldState.createPlayer(true);
        assertNotNull(player);

        final WorldSnapshot snapshot = worldState.createEmptySnapshot();
        final var input = new PlayerInputMutation(1, 0, (float) Math.PI, (byte) 0xa);
        WorldState.registerInput(snapshot, player, input);
        snapshot.getIntData()[0] = player.getUid();
        worldState.loadInput(snapshot);
        assertEquals(0xa, player.getInputState().getInputKeys());
    }

    @Test
    void testStore() {
        final var worldState = new WorldState(1, map);
        worldState.store(worldState.createEmptySnapshot());
    }

    @Test
    void testAppendToBinaryOutput() {
        final var byteStream = new ByteArrayOutputStream();
        final SafeDataOutput dataOutput = new ByteArrayDataOutput(byteStream);

        final var worldState = new WorldState(1, map);
        assertNotNull(worldState.createPlayer(true));

        worldState.appendToBinaryOutput(dataOutput);
        assertEquals(161, byteStream.toByteArray().length);
    }
}
