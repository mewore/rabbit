package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import moe.mewore.rabbit.data.BinaryEntity;
import moe.mewore.rabbit.data.SafeDataOutput;

@RequiredArgsConstructor
public class PhysicsDummySphere extends BinaryEntity {

    public static final int FLOAT_DATA_PER_SPHERE = 6;

    private static final Vector3f OFFSET = new Vector3f(0f, 20f, 0f);

    private static final int BOXES_PER_SPHERE = 3;

    private final Transform tmpTransform = new Transform();

    private final Vector3f tmpVector = new Vector3f();

    @Getter
    private final RigidBody body;

    public static PhysicsDummySphere[] makeSpheres(final PhysicsDummyBox[] boxes) {
        final List<PhysicsDummySphere> result = new ArrayList<>();

        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = boxes.length - 1; i >= 0; i--) {
            if (counter.decrementAndGet() <= 0) {
                counter.set(BOXES_PER_SPHERE);

                final var sphereShape = new SphereShape(10f);
                final var sphereConstructionInfo = new RigidBodyConstructionInfo(1, new DefaultMotionState(),
                    sphereShape);
                final RigidBody sphere = new RigidBody(sphereConstructionInfo);
                sphere.translate(boxes[i].getPosition());
                sphere.translate(OFFSET);

                result.add(new PhysicsDummySphere(sphere));
            }
        }

        return result.toArray(new PhysicsDummySphere[0]);
    }

    private static void appendVector3fToBinaryOutput(final Vector3f vector3f, final SafeDataOutput output) {
        output.writeFloat(vector3f.x);
        output.writeFloat(vector3f.y);
        output.writeFloat(vector3f.z);
    }

    private Vector3f getPosition() {
        return body.getWorldTransform(tmpTransform).origin;
    }

    private Vector3f getMotion() {
        return body.getLinearVelocity(tmpVector);
    }

    public void load(final float[] data, int index) {
        tmpTransform.origin.set(data[index], data[++index], data[++index]);
        body.setWorldTransform(tmpTransform);

        tmpVector.set(data[++index], data[++index], data[++index]);
        getBody().setLinearVelocity(tmpVector);
    }

    public void store(final float[] data, int index) {
        final Vector3f position = getPosition();
        data[index] = position.x;
        data[++index] = position.y;
        data[++index] = position.z;

        final Vector3f motion = getMotion();
        data[++index] = motion.x;
        data[++index] = motion.y;
        data[++index] = motion.z;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        appendVector3fToBinaryOutput(getPosition(), output);
        appendVector3fToBinaryOutput(getMotion(), output);
    }
}
