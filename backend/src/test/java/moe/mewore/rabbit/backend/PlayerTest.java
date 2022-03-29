package moe.mewore.rabbit.backend;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.RigidBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.backend.simulation.player.FakePlayerInputEvent;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.world.MazeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
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

    private static String formatPlayerTargetMotion(final Player player) {
        return String.format("(%.2f, %.2f)", player.getTargetHorizontalMotion().x,
            player.getTargetHorizontalMotion().y);
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
        final var input = new PlayerInput(1, 0L, PlayerInput.INPUT_JUMP_BIT, 0f);
        player.applyInput(new FakePlayerInputEvent(player, input));
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
        final var input = new PlayerInput(1, 0L, PlayerInput.INPUT_UP_BIT, 0f);
        final var inputEvent = new FakePlayerInputEvent(player, input);
        player.applyInput(inputEvent);
        assertEquals("(-0.00, -100.00)", formatPlayerTargetMotion(player));
        assertSame(inputEvent, player.getLastInputEvent());
    }

    @Test
    void testClearInput() {
        final var input = new PlayerInput(1, 0L, PlayerInput.INPUT_UP_BIT, 0f);
        player.applyInput(new FakePlayerInputEvent(player, input));
        player.clearInput();
        assertEquals("(0.00, 0.00)", formatPlayerTargetMotion(player));
        assertNull(player.getLastInputEvent());
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
