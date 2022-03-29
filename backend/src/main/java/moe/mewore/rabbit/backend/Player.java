package moe.mewore.rabbit.backend;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.linearmath.Transform;

import org.checkerframework.checker.nullness.qual.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.backend.net.Heart;
import moe.mewore.rabbit.backend.physics.RigidBodyController;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;
import moe.mewore.rabbit.backend.simulation.player.PlayerInput;
import moe.mewore.rabbit.backend.simulation.player.PlayerInputEvent;
import moe.mewore.rabbit.world.MazeMap;

@RequiredArgsConstructor
public class Player implements FrameSerializableEntity {

    private static final float MAX_SPEED = 100f;

    @Getter
    private final Vector2f targetHorizontalMotion = new Vector2f();

    private final Transform tmpTransform = new Transform();

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

    private final CollisionWorld world;

    @Getter
    private final CollisionObject body;

    @Getter
    private final RigidBodyController characterController;

    private boolean jumping;

    @Getter
    @Setter
    private int latency = Heart.DEFAULT_LATENCY;

    @Getter
    private @Nullable PlayerInputEvent lastInputEvent = null;

    public void beforePhysics(final float dt) {
        characterController.setTargetHorizontalMotion(targetHorizontalMotion);
        if (jumping) {
            characterController.jump();
        }
        characterController.updateAction(world, dt);
    }

    public void afterPhysics(final MazeMap map) {
        characterController.afterPhysics(world);
        final var position = characterController.getPosition(tmpTransform);
        position.x = (float) map.wrapX(position.x);
        position.z = (float) map.wrapZ(position.z);
        if (position.y < MIN_Y || position.y > MAX_Y) {
            position.y = (MIN_Y + MAX_Y) * .5f;
        }
        characterController.setPosition(position);
    }

    public void applyInput(final PlayerInputEvent inputEvent) {
        lastInputEvent = inputEvent;
        inputEvent.getInput().applyToTargetHorizontalMotion(targetHorizontalMotion, MAX_SPEED);
        jumping = inputEvent.getInput().isJumping();
    }

    public void clearInput() {
        lastInputEvent = null;
        PlayerInput.EMPTY.applyToTargetHorizontalMotion(targetHorizontalMotion, 0f);
        jumping = false;
    }

    @Override
    public void load(final byte[] frame) {
        characterController.load(frame);
    }

    @Override
    public void store(final byte[] frame) {
        characterController.store(frame);
    }

    public Vector3f getPosition(final Transform transform) {
        return characterController.getPosition(transform);
    }

    public Vector3f getMotion(final Vector3f target) {
        return characterController.getMotion(target);
    }
}
