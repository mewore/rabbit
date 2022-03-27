package moe.mewore.rabbit.backend;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.RigidBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.mutations.PlayerInputMutation;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerTest {

    private Player player;

    private CollisionWorld world;

    private RigidBodyController controller;

    @BeforeEach
    void setUp() {
        world = mock(CollisionWorld.class);
        controller = mock(RigidBodyController.class);
        player = new Player(0, 0, "Player", true, world, mock(RigidBody.class), controller);
    }

    @Test
    void testBeforePhysics() {
        player.beforePhysics(1f);
        verify(controller).setTargetHorizontalMotion(same(player.getInputState().getTargetHorizontalMotion()));
        verify(controller, never()).jump();
        verify(controller).updateAction(same(world), eq(1f));
    }

    @Test
    void testBeforePhysics_jumping() {
        player.getInputState().applyInput(1, 0f, PlayerInputMutation.INPUT_JUMP_BIT);
        player.beforePhysics(1f);
        verify(controller).jump();
    }

    @Test
    void testAfterPhysics() {
        final var position = new Vector3f(0f, -100f, 0f);
        when(controller.getPosition()).thenReturn(position);

        final var map = mock(MazeMap.class);
        when(map.wrapX(anyDouble())).thenReturn(2.0);
        when(map.wrapZ(anyDouble())).thenReturn(3.0);
        player.afterPhysics(map);

        verify(controller).afterPhysics(same(world));
        verify(controller).setPosition(same(position));
        assertEquals("(2.00, 100.00, 3.00)", String.format("(%.2f, %.2f, %.2f)", position.x, position.y, position.z));
    }
}
