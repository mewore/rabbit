package moe.mewore.rabbit.backend.messages;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.Player;
import moe.mewore.rabbit.backend.physics.RigidBodyController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PlayerDisconnectMessageTest {

    @Test
    void testEncode() {
        final var player = new Player(1, 1, "User", true, mock(CollisionWorld.class), mock(CollisionObject.class),
            mock(RigidBodyController.class));
        assertEquals(5, new PlayerDisconnectMessage(player).encodeToBinary().length);
    }
}
