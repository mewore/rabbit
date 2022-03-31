package moe.mewore.rabbit.backend.game;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.RigidBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitPlayerTest {

    private RabbitPlayer player;

    private CollisionWorld world;

    private RigidBodyController controller;

    private static String formatPlayerTargetMotion(final RabbitPlayer player) {
        return String.format("(%.2f, %.2f)", player.getTargetHorizontalMotion().x,
            player.getTargetHorizontalMotion().y);
    }

    @BeforeEach
    void setUp() {
        world = mock(CollisionWorld.class);
        controller = mock(RigidBodyController.class);
        player = new RabbitPlayer(25, 0, "Player", true, world, mock(RigidBody.class), controller);
    }

    @Test
    void testSettersAndGetters() {
        assertEquals(25, player.getUid());
        assertEquals(100, player.getLatency());
        player.setLatency(250);
        assertEquals(250, player.getLatency());
    }

    @Test
    void testBeforePhysics() {
        player.beforePhysics(1f);
        verify(controller).setTargetHorizontalMotion(any());
        verify(controller, never()).jump();
        verify(controller).updateAction(same(world), eq(1f));
    }

    @Test
    void testBeforePhysics_jumping() {
        final var input = new RabbitPlayerInput(1, RabbitPlayerInput.INPUT_JUMP_BIT, 0f, 0L);
        player.applyInput(input);
        player.beforePhysics(1f);
        verify(controller).jump();
    }

    @Test
    void testAfterPhysics() {
        final var position = new Vector3f(0f, -100f, 0f);
        when(controller.getPosition(any())).thenReturn(position);

        final var map = mock(MazeMap.class);
        when(map.wrapX(anyDouble())).thenReturn(2.0);
        when(map.wrapZ(anyDouble())).thenReturn(3.0);
        player.afterPhysics(map);

        verify(controller).afterPhysics(same(world));
        verify(controller).setPosition(same(position));
        assertEquals("(2.00, 100.00, 3.00)", String.format("(%.2f, %.2f, %.2f)", position.x, position.y, position.z));
    }

    @Test
    void testApplyInput() {
        final var input = new RabbitPlayerInput(1, RabbitPlayerInput.INPUT_UP_BIT, 0f, 0L);
        player.applyInput(input);
        assertEquals("(-0.00, -100.00)", formatPlayerTargetMotion(player));
        assertEquals(1, player.getInputId());
    }

    @Test
    void testLoad() {
        final var frame = new byte[0];
        player.load(frame);
        verify(controller).load(same(frame));
    }

    @Test
    void testStore() {
        final var frame = new byte[0];
        player.store(frame);
        verify(controller).store(same(frame));
    }
}
