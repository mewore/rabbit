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

    private static final Vector3f OFFSET = new Vector3f(0f, 20f, 0f);

    private static final int BOXES_PER_SPHERE = 3;

    private static final Transform TRANSFORM = new Transform();

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

    private Vector3f getPosition() {
        return body.getWorldTransform(TRANSFORM).origin;
    }

    public void load(final float[] data, final int index) {
        TRANSFORM.origin.set(data[index], data[index + 1], data[index + 2]);
        body.setWorldTransform(TRANSFORM);
    }

    public void store(final float[] data, final int index) {
        final Vector3f position = getPosition();
        data[index] = position.x;
        data[index + 1] = position.y;
        data[index + 2] = position.z;
    }

    @Override
    public void appendToBinaryOutput(final SafeDataOutput output) {
        final Vector3f position = getPosition();
        output.writeFloat(position.x);
        output.writeFloat(position.y);
        output.writeFloat(position.z);
    }
}
