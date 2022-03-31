package moe.mewore.rabbit.backend.game;

import javax.vecmath.Vector3f;
import java.util.Collections;

import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitWorldTest {

    private MazeMap map;

    private DynamicsWorld physicsWorld;

    private RabbitWorld world;

    @BeforeEach
    void setUp() {
        map = mock(MazeMap.class);
        when(map.getWidth()).thenReturn(1f);
        when(map.getDepth()).thenReturn(1f);
        when(map.getWalls()).thenReturn(Collections.emptyList());

        physicsWorld = mock(DynamicsWorld.class);

        world = new RabbitWorld(2, map, physicsWorld);
    }

    @Test
    void testInitialize() {
        verify(physicsWorld, never()).addRigidBody(any());
        world.initialize();
        verify(physicsWorld, times(25)).addRigidBody(any());
    }

    @Test
    void testGetPlayers() {
        assertEquals(Collections.emptyMap(), new RabbitWorld(1, map, physicsWorld).getPlayersAsMap());
    }

    @Test
    void testCreatePlayer() {
        final RabbitPlayer player = new RabbitWorld(1, map, physicsWorld).createPlayer(true);
        assertNotNull(player);
        assertEquals("Player 1", player.getUsername());
        assertEquals(0, player.getIndex());
        assertTrue(player.isReisen());
        assertEquals(new Vector3f(0f, 5f, 0f), player.getBody().getWorldTransform(new Transform()).origin);
        verify(physicsWorld, only()).addRigidBody(same((RigidBody) player.getBody()));
    }

    @Test
    void testRemovePlayer() {
        final RabbitPlayer player = world.createPlayer(true);
        assertNotNull(player);
        assertFalse(world.getPlayersAsMap().isEmpty());

        world.removePlayer(player);
        assertTrue(world.getPlayersAsMap().isEmpty());
        verify(physicsWorld).removeCollisionObject(same(player.getBody()));
    }

    @Test
    void testGetFrameSize() {
        assertEquals(172, new RabbitWorld(2, map, physicsWorld).getFrameSize());
    }

    @Test
    void testDoStep() {
        world.doStep(.25f);
        verify(physicsWorld, only()).stepSimulation(.25f, 0, .25f);
    }

    @Test
    void testDoStep_brokenWorld() {
        doThrow(new NullPointerException("oof")).when(physicsWorld).stepSimulation(.25f, 0, .25f);
        world.doStep(.25f);
    }
}
