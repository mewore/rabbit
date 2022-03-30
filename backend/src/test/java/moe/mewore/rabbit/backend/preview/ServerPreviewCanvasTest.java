package moe.mewore.rabbit.backend.preview;

import javax.vecmath.Vector3f;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.DynamicsWorld;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.backend.simulation.RabbitWorldState;
import moe.mewore.rabbit.backend.simulation.player.FakePlayerInputEvent;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.world.MazeMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerPreviewCanvasTest {

    private static MazeMap makeMap(final int columns, final int rows) {
        final var map = mock(MazeMap.class);
        when(map.getColumnCount()).thenReturn(columns);
        when(map.getRowCount()).thenReturn(rows);
        when(map.render(anyInt(), anyInt())).thenReturn(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        return map;
    }

    private static RabbitWorldState makeWorldState(final Map<Integer, Player> players) {
        return makeWorldState(players, mock(DynamicsWorld.class));
    }

    private static RabbitWorldState makeWorldState(final Map<Integer, Player> players, final DynamicsWorld world) {
        final var worldState = mock(RabbitWorldState.class);
        when(worldState.getWorld()).thenReturn(world);
        when(worldState.getPlayers()).thenReturn(players);
        return worldState;
    }

    @Test
    void testUpdateOverlay() {
        final var world = mock(DynamicsWorld.class);
        final var worldState = makeWorldState(Collections.emptyMap(), world);

        final var map = makeMap(2, 1);
        final var canvas = new ServerPreviewCanvas(map, worldState);
        verify(map).render(200, 100);
        verify(world).setDebugDrawer(any());

        canvas.updateOverlay();
        verify(world).debugDrawWorld();
    }

    @Test
    void testUpdateOverlay_noChanges() {
        final var world = mock(DynamicsWorld.class);
        final var worldState = makeWorldState(Collections.emptyMap(), world);
        final var canvas = new ServerPreviewCanvas(makeMap(1, 1), worldState);
        canvas.updateOverlay();
        canvas.updateOverlay();
    }

    @Test
    void testUpdateOverlay_players() {
        final var world = mock(DynamicsWorld.class);
        final var body = mock(CollisionObject.class);
        final var controller = mock(RigidBodyController.class);
        when(controller.getPosition(any())).thenReturn(new Vector3f());
        when(controller.getMotion(any())).thenReturn(new Vector3f());
        final var playerWithTargetMotion = new Player(0, 0, "Player 1", false, world, body, controller);
        final var input = new PlayerInput(1, 1, PlayerInput.INPUT_UP_BIT, 1f);
        playerWithTargetMotion.applyInput(new FakePlayerInputEvent(playerWithTargetMotion, input));

        final var controller2 = mock(RigidBodyController.class);
        when(controller2.getPosition(any())).thenReturn(new Vector3f());
        when(controller2.getMotion(any())).thenReturn(new Vector3f(1, 1, 1));
        final var playerWithMotion = new Player(1, 1, "Player 2", true, world, body, controller2);

        final var worldState = makeWorldState(Map.of(0, playerWithTargetMotion, 1, playerWithMotion));
        final var map = makeMap(1, 2);
        final var canvas = new ServerPreviewCanvas(map, worldState);
        when(map.getWidth()).thenReturn(1f);
        when(map.getDepth()).thenReturn(1f);
        canvas.updateOverlay();
    }

    @Test
    void testPaint() {
        final var worldState = makeWorldState(Collections.emptyMap());
        final var map = makeMap(1, 2);
        final var canvas = new ServerPreviewCanvasWithSize(map, worldState);
        final var graphics = mock(Graphics.class);
        canvas.paint(graphics);
        verify(graphics, times(4)).drawImage(any(), anyInt(), anyInt(), same(canvas));
    }

    private static class ServerPreviewCanvasWithSize extends ServerPreviewCanvas {

        public ServerPreviewCanvasWithSize(final MazeMap map, final RabbitWorldState worldState) {
            super(map, worldState);
        }

        @Override
        public int getWidth() {
            return 2;
        }

        @Override
        public int getHeight() {
            return 2;
        }
    }
}
