package moe.mewore.rabbit.backend.physics;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.TriangleShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import org.junit.jupiter.api.Test;

import moe.mewore.rabbit.backend.simulation.data.FrameCompiler;
import moe.mewore.rabbit.backend.simulation.data.FrameDataType;
import moe.mewore.rabbit.backend.simulation.data.FrameSection;
import moe.mewore.rabbit.backend.simulation.data.FrameSerializationTestUtil;

import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.BYTE;
import static moe.mewore.rabbit.backend.simulation.data.FrameDataType.VECTOR3F;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PhysicsDummySphereTest {

    private static final FrameDataType[] FRAME_DATA_TYPES = new FrameDataType[]{BYTE, VECTOR3F, VECTOR3F};

    @Test
    void testMakeSpheres() {
        final RigidBody mockBody = mock(RigidBody.class);
        final PhysicsDummyBox[] boxes = new PhysicsDummyBox[]{new PhysicsDummyBox(mockBody, 1f, 1f,
            new Vector3f(1f, 1f, 1f), 1f), new PhysicsDummyBox(mockBody, 1f, 1f, new Vector3f(1f, 1f, 1f), 1f)};

        final var frameCompiler = mock(FrameCompiler.class);
        final PhysicsDummySphere[] result = PhysicsDummySphere.makeSpheres(boxes, frameCompiler);
        assertEquals(1, result.length);
        assertEquals(new Vector3f(1f, 21f, 1f), result[0].getBody().getWorldTransform(new Transform()).origin);
        verify(frameCompiler, times(1)).reserve(FRAME_DATA_TYPES[0], FRAME_DATA_TYPES[1], FRAME_DATA_TYPES[2]);
    }

    @Test
    void testSerialization() {
        final FrameCompiler frameCompiler = new FrameCompiler();
        final FrameSection frameSection = frameCompiler.reserve(FRAME_DATA_TYPES);
        final var shape = new TriangleShape();
        final Transform transform = new Transform();

        transform.origin.set(2f, 2f, 2f);
        final var firstSphere = new PhysicsDummySphere(new RigidBody(1, new DefaultMotionState(transform), shape),
            frameSection);

        transform.origin.set(3f, 3f, 3f);
        final var secondSphere = new PhysicsDummySphere(new RigidBody(1, new DefaultMotionState(transform), shape),
            frameSection);

        FrameSerializationTestUtil.testSerialization(frameCompiler, frameSection, firstSphere, secondSphere);
    }
}
