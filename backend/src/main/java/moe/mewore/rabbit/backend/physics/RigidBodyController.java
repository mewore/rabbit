package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.List;
import java.util.function.Consumer;

import com.bulletphysics.collision.broadphase.Dispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.narrowphase.PersistentManifold;
import com.bulletphysics.dynamics.ActionInterface;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.IDebugDraw;
import com.bulletphysics.linearmath.Transform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.FLOAT;
import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.VECTOR3F;

@RequiredArgsConstructor
public class RigidBodyController extends ActionInterface implements FrameSerializableEntity {

    public static final List<FrameDataType> FRAME_DATA_TYPES = List.of(VECTOR3F, VECTOR3F, FLOAT, FLOAT);

    private static final float JUMP_SPEED = 110f;

    private static final float MAX_Y_SPEED = JUMP_SPEED * 2f;

    private static final float ACCELERATION = 400f;

    private static final float GROUND_CHECK_REQUIRED_NORMAL_Y = .3f;

    private final Transform tmpTransform = new Transform();

    private final Vector3f tmpVector3 = new Vector3f();

    public float groundLeniency = 0.1f;

    public float jumpControlLeniency = 0.1f;

    private final RigidBody body;

    private final FrameSection frameSection;

    @Getter
    @Setter
    private Vector2f targetHorizontalMotion = new Vector2f();

    public float groundTimeLeft = -1f;

    public float jumpControlTimeLeft = -1f;

    private Vector3f getPosition() {
        return getPosition(tmpTransform);
    }

    public Vector3f getPosition(final Transform target) {
        return body.getWorldTransform(target).origin;
    }

    public void setPosition(final Vector3f position) {
        if (position != tmpTransform.origin) {
            tmpTransform.setIdentity();
            tmpTransform.origin.set(position);
        }
        body.setWorldTransform(tmpTransform);
    }

    public Vector3f getMotion(final Vector3f target) {
        return body.getLinearVelocity(target);
    }

    private Vector3f getMotion() {
        return getMotion(tmpVector3);
    }

    public void setMotion(final Vector3f motion) {
        body.setLinearVelocity(motion);
    }

    public void jump() {
        jumpControlTimeLeft = jumpControlLeniency;
    }

    boolean onGround() {
        return groundTimeLeft >= 0;
    }

    private boolean wantsToJump() {
        return jumpControlTimeLeft >= 0;
    }

    @Override
    public void updateAction(final CollisionWorld world, final float deltaTimeStep) {
        final var motion = getMotion(tmpVector3);
        final var motionDx = targetHorizontalMotion.x - motion.x;
        final var motionDz = targetHorizontalMotion.y - motion.z;
        final var motionDistanceSquared = motionDx * motionDx + motionDz * motionDz;
        final var maxFrameAcceleration = ACCELERATION * deltaTimeStep;
        if (motionDistanceSquared > maxFrameAcceleration * maxFrameAcceleration) {
            final var multiplier = maxFrameAcceleration / Math.sqrt(motionDistanceSquared);
            motion.x = (float) (motion.x + motionDx * multiplier);
            motion.z = (float) (motion.z + motionDz * multiplier);
        } else {
            motion.x = targetHorizontalMotion.x;
            motion.z = targetHorizontalMotion.y;
        }
        if (onGround() && wantsToJump()) {
            motion.y = JUMP_SPEED;
            groundTimeLeft = jumpControlTimeLeft = 0;
        } else {
            motion.y = Math.max(-MAX_Y_SPEED, Math.min(MAX_Y_SPEED, motion.y));
        }

        body.setLinearVelocity(motion);

        if (Math.abs(motion.x) > 0 || Math.abs(motion.y) > 0 || Math.abs(motion.z) > 0) {
            body.activate();
        }

        jumpControlTimeLeft -= deltaTimeStep;
        groundTimeLeft -= deltaTimeStep;
    }

    @Override
    public void debugDraw(final IDebugDraw debugDrawer) {
        // Do nothing for now
    }

    public void afterPhysics(final CollisionWorld world) {
        forEachGroundCollision(world, collisionNormal -> groundTimeLeft = groundLeniency);
    }

    private void forEachGroundCollision(final CollisionWorld world, final Consumer<Vector3f> callback) {
        final Dispatcher dispatcher = world.getDispatcher();
        for (int i = 0; i < dispatcher.getNumManifolds(); i++) {
            final PersistentManifold manifold = dispatcher.getManifoldByIndexInternal(i);
            final var isFirstBody = manifold.getBody0() == body;
            final var isSecondBody = !isFirstBody && manifold.getBody1() == body;
            if (isFirstBody || isSecondBody) {
                for (int j = 0; j < manifold.getNumContacts(); j++) {
                    final var normal = manifold.getContactPoint(j).normalWorldOnB;
                    if ((isFirstBody ? (1 - normal.y) : (1 + normal.y)) < GROUND_CHECK_REQUIRED_NORMAL_Y) {
                        tmpVector3.set(normal);
                        if (!isFirstBody) {
                            tmpVector3.negate();
                        }
                        callback.accept(tmpVector3);
                    }
                }
            }
        }
    }

    @Override
    public void load(final byte[] frame) {
        frameSection.setFrame(frame);

        tmpTransform.setIdentity();
        frameSection.readIntoVector3f(tmpTransform.origin);
        body.setWorldTransform(tmpTransform);

        body.setLinearVelocity(frameSection.readIntoVector3f(tmpVector3));

        groundTimeLeft = frameSection.readFloat();
        jumpControlTimeLeft = frameSection.readFloat();
    }

    @Override
    public void store(final byte[] frame) {
        frameSection.setFrame(frame);

        frameSection.writeVector3f(getPosition());
        frameSection.writeVector3f(getMotion());

        frameSection.writeFloat(groundTimeLeft);
        frameSection.writeFloat(jumpControlTimeLeft);
    }
}
