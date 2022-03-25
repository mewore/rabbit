package moe.mewore.rabbit.backend;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.physics.RigidBodyController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerTest {

    @Test
    void testEncode() {
        final var controller = mock(RigidBodyController.class);
        when(controller.getPosition()).thenReturn(new Vector3f());
        when(controller.getMotion()).thenReturn(new Vector3f());

        final var player = new Player(51, 18, "Player 18", true, mock(CollisionWorld.class),
            mock(CollisionObject.class), controller);

        assertEquals(49, player.encodeToBinary().length);
    }
}
