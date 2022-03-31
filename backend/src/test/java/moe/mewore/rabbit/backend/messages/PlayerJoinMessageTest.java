package moe.mewore.rabbit.backend.messages;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.game.RabbitPlayer;
import moe.mewore.rabbit.backend.physics.RigidBodyController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PlayerJoinMessageTest {

    @Test
    void testEncode() {
        final var player = new RabbitPlayer(1, 1, "User", true, mock(CollisionWorld.class), mock(CollisionObject.class),
            mock(RigidBodyController.class));
        assertEquals(15, new PlayerJoinMessage(player, false).encodeToBinary().length);
    }
}
