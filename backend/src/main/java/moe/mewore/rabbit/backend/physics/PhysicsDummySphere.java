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
import moe.mewore.rabbit.backend.simulation.data.FrameCompiler;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializableEntity;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.BYTE;
import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.VECTOR3F;

@RequiredArgsConstructor
public class PhysicsDummySphere implements FrameSerializableEntity {

    private static final FrameDataType[] FRAME_DATA_TYPES = new FrameDataType[]{BYTE, VECTOR3F, VECTOR3F};

    private static final Vector3f OFFSET = new Vector3f(0f, 20f, 0f);

    private static final int BOXES_PER_SPHERE = 6;

    private final Transform tmpTransform = new Transform();

    private final Vector3f tmpVector = new Vector3f();

    @Getter
    private final RigidBody body;

    private final FrameSection frameView;

    public static PhysicsDummySphere[] makeSpheres(final PhysicsDummyBox[] boxes, final FrameCompiler frameCompiler) {
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
                sphere.setFriction(.5f);
                sphere.setRestitution(1f);

                result.add(new PhysicsDummySphere(sphere, frameCompiler.reserve(FRAME_DATA_TYPES)));
            }
        }

        return result.toArray(new PhysicsDummySphere[0]);
    }

    private Vector3f getPosition() {
        return body.getWorldTransform(tmpTransform).origin;
    }

    private Vector3f getMotion() {
        return body.getLinearVelocity(tmpVector);
    }

    public void load(final byte[] frame) {
        frameView.setFrame(frame);
        body.setActivationState(frameView.readByte());

        frameView.readIntoVector3f(tmpTransform.origin);
        body.setWorldTransform(tmpTransform);

        getBody().setLinearVelocity(frameView.readIntoVector3f(tmpVector));
    }

    @Override
    public void store(final byte[] frame) {
        frameView.setFrame(frame);
        frameView.writeByte(body.getActivationState());
        frameView.writeVector3f(getPosition());
        frameView.writeVector3f(getMotion());
    }
}
