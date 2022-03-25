package moe.mewore.rabbit.backend;

import javax.vecmath.Tuple3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.backend.net.Heart;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;
import moe.mewore.rabbit.world.MazeMap;

@RequiredArgsConstructor
public class Player extends BinaryEntity {

    private static final float MIN_Y = 0f;

    private static final float MAX_Y = 200f;

    @Getter
    private final int uid;

    @Getter
    private final int id;

    @Getter
    private final String username;

    @Getter
    private final boolean isReisen;

    @Getter
    private final CollisionWorld world;

    @Getter
    private final CollisionObject body;

    @Getter
    private final RigidBodyController characterController;

    @Getter
    private final PlayerInputState inputState = new PlayerInputState();

    @Getter
    @Setter
    private int latency = Heart.DEFAULT_LATENCY;

    private static void appendTuple3fToBinaryOutput(final Tuple3f vector, final SafeDataOutput output) {
        output.writeFloat(vector.x);
        output.writeFloat(vector.y);
        output.writeFloat(vector.z);
    }

    public void beforePhysics(final float dt) {
        characterController.targetHorizontalMotion = inputState.getTargetHorizontalMotion();
        if (inputState.isJumping()) {
            characterController.jump();
        }
        characterController.updateAction(world, dt);
    }

    public void afterPhysics(final MazeMap map) {
        characterController.afterPhysics(world);
        final var position = characterController.getPosition();
        position.x = (float) map.wrapX(position.x);
        position.z = (float) map.wrapZ(position.z);
        if (position.y < MIN_Y || position.y > MAX_Y) {
            position.y = (MIN_Y + MAX_Y) * .5f;
            characterController.setPosition(position);
        }
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        output.writeInt(id);
        output.writeInt(getLatency());
        inputState.appendToBinaryOutput(output);
        appendTuple3fToBinaryOutput(characterController.getPosition(), output);
        appendTuple3fToBinaryOutput(characterController.getMotion(), output);
        output.writeFloat(characterController.groundTimeLeft);
        output.writeFloat(characterController.jumpControlTimeLeft);
    }
}
